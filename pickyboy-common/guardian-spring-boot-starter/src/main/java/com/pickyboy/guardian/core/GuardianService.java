package com.pickyboy.guardian.core;

import cn.hutool.core.util.StrUtil;
import com.pickyboy.guardian.config.GuardianProperties;
import com.pickyboy.guardian.core.counter.Counter;
import com.pickyboy.guardian.model.*;
import com.pickyboy.guardian.model.constant.GuardianConstants;
import com.pickyboy.guardian.model.counter.CounterParam;
import com.pickyboy.guardian.model.rule.AlertInfo;
import com.pickyboy.guardian.model.GuardianDefinition;
import com.pickyboy.guardian.model.rule.GuardianRule;
import com.pickyboy.guardian.model.strategy.ActionContext;
import com.pickyboy.guardian.strategy.ActionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Guardian核心服务
 *
 * @author pickyboy
 */
@Slf4j
@Service
public class GuardianService {

    @Resource
    private GuardianProperties guardianProperties;

    /**
     * 执行Guardian检查
     */
    public GuardianCheckResult checkLimit(String key, String scene, GuardianDefinition rule) {
        if (!guardianProperties.isEnabled()) {
            return GuardianCheckResult.allowed(0, Long.MAX_VALUE);
        }

        try {
            // 1. 获取计数器并递增
            Counter counter = GuardianManager.getCounter(rule.getCounterParam().getStrategy());
            long currentCount = counter.incrementAndGet(rule.getCounterParam());

            // 2. 检查阈值并处理告警
            List<AlertInfo> alerts = new ArrayList<>();
            boolean allowed = true;

            for (GuardianRule threshold : rule.getThresholds()) {
                if (currentCount >= threshold.getThreshold()) {
                    // 创建告警信息
                    AlertInfo alertInfo = AlertInfo.builder()
                            .level(threshold.getLevel())
                            .threshold(threshold.getThreshold())
                            .currentCount(currentCount)
                            .actionTypes(threshold.getActionTypes())
                            .alertTime(LocalDateTime.now())
                            .message(String.format("场景 %s 触发 %s 级别告警", scene, threshold.getLevel()))
                            .build();
                    alerts.add(alertInfo);

                    // 执行动作策略
                    executeActions(key, scene, currentCount, threshold);

                    // 判断是否拒绝
                    if (GuardianConstants.ALERT_LEVEL_CRITICAL.equals(threshold.getLevel()) && rule.isFailFast()) {
                        allowed = false;
                    }
                }
            }

            return GuardianCheckResult.builder()
                    .allowed(allowed)
                    .currentCount(currentCount)
                    .remainingCount(Math.max(0, getMaxThreshold(rule) - currentCount))
                    .alerts(alerts)
                    .ruleName(rule.getName())
                    .errorMessage(allowed ? null : rule.getErrorMessage())
                    .build();

        } catch (Exception e) {
            log.error("Guardian检查异常", e);
            // 异常时允许通过，避免影响业务
            return GuardianCheckResult.allowed(0, Long.MAX_VALUE);
        }
    }

    /**
     * 构建Guardian规则（从注解和配置创建）
     */
    public GuardianDefinition buildRule(String scene, String counterType, int windowSize,
                                        TimeUnit timeUnit, long expiration, int warnCount,
                                        int banCount, String[] warnActions, String[] banActions,
                                        boolean failFast, String errorMessage) {

        // 构建计数器配置
        String finalCounterType = parseCounterType(counterType);
        CounterParam counterParam = CounterParam.builder()
                .strategy(finalCounterType)
                .windowSize(windowSize)
                .timeUnit(timeUnit)
                .expiration(expiration)
                .scene(scene)
                .build();

        // 构建阈值配置
        List<GuardianRule> thresholds = new ArrayList<>();

        // 警告阈值
        int finalWarnCount = warnCount == -1 ? guardianProperties.getDefaultWarnThreshold() : warnCount;
        List<String> finalWarnActions = parseActionTypes(warnActions, guardianProperties.getDefaultWarnActions());
        thresholds.add(GuardianRule.builder()
                .threshold(finalWarnCount)
                .level(GuardianConstants.ALERT_LEVEL_WARNING)
                .actionTypes(finalWarnActions)
                .enabled(true)
                .description("警告阈值")
                .build());

        // 封禁阈值
        int finalBanCount = banCount == -1 ? guardianProperties.getDefaultBanThreshold() : banCount;
        List<String> finalBanActions = parseActionTypes(banActions, guardianProperties.getDefaultBanActions());
        thresholds.add(GuardianRule.builder()
                .threshold(finalBanCount)
                .level(GuardianConstants.ALERT_LEVEL_CRITICAL)
                .actionTypes(finalBanActions)
                .enabled(true)
                .description("封禁阈值")
                .build());

        // 构建规则
        return GuardianDefinition.builder()
                .name(scene + "_rule")
                .counterParam(counterParam)
                .thresholds(thresholds)
                .failFast(failFast)
                .errorMessage(StrUtil.isBlank(errorMessage) ? guardianProperties.getDefaultErrorMessage() : errorMessage)
                .enabled(true)
                .description("自动生成的规则: " + scene)
                .build();
    }

    /**
     * 执行动作策略
     */
    private void executeActions(String key, String scene, long currentCount, GuardianRule threshold) {
        for (String actionType : threshold.getActionTypes()) {
            try {
                ActionStrategy strategy = GuardianManager.getActionStrategy(actionType);
                if (strategy.supports(threshold.getLevel())) {
                    ActionContext context = ActionContext.builder()
                            .key(key)
                            .currentCount(currentCount)
                            .threshold(threshold.getThreshold())
                            .level(threshold.getLevel())
                            .businessType("guardian")
                            .scene(scene)
                            .reason(String.format("触发%s级别阈值%d", threshold.getLevel(), threshold.getThreshold()))
                            .build();

                    strategy.execute(context);
                }
            } catch (Exception e) {
                log.error("执行动作策略失败: {}", actionType, e);
            }
        }
    }

    /**
     * 解析计数器类型
     */
    private String parseCounterType(String counterType) {
        if (StrUtil.isBlank(counterType)) {
            return guardianProperties.getDefaultCounterType();
        }

        // 验证计数器类型是否有效
        if (GuardianConstants.COUNTER_TYPE_REDIS.equals(counterType) ||
            GuardianConstants.COUNTER_TYPE_LOCAL.equals(counterType)) {
            return counterType;
        }

        log.warn("无效的计数器类型: {}, 使用默认类型", counterType);
        return guardianProperties.getDefaultCounterType();
    }

    /**
     * 解析动作类型
     */
    private List<String> parseActionTypes(String[] actionTypes, List<String> defaultActions) {
        if (actionTypes == null || actionTypes.length == 0) {
            return new ArrayList<>(defaultActions);
        }

        return Arrays.stream(actionTypes)
                .map(type -> {
                    // 验证动作类型是否有效
                    if (GuardianConstants.ACTION_TYPE_EMAIL.equals(type) ||
                        GuardianConstants.ACTION_TYPE_LOG.equals(type) ||
                        GuardianConstants.ACTION_TYPE_BAN.equals(type)) {
                        return type;
                    }
                    log.warn("无效的动作类型: {}", type);
                    return null;
                })
                .filter(type -> type != null)
                .collect(Collectors.toList());
    }

    /**
     * 获取最大阈值
     */
    private long getMaxThreshold(GuardianDefinition rule) {
        return rule.getThresholds().stream()
                .mapToLong(GuardianRule::getThreshold)
                .max()
                .orElse(Long.MAX_VALUE);
    }
}
