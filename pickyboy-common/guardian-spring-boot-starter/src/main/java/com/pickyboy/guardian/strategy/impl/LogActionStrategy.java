package com.pickyboy.guardian.strategy.impl;

import com.pickyboy.guardian.model.constant.GuardianConstants;
import com.pickyboy.guardian.model.rule.GuardianRule;
import com.pickyboy.guardian.model.strategy.ActionContext;
import com.pickyboy.guardian.strategy.ActionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;

/**
 * 日志记录动作策略
 *
 * @author pickyboy
 */
@Slf4j
@Component
@Order(100)
public class LogActionStrategy implements ActionStrategy {

    @Override
    public void execute(ActionContext context) {
        // 1. 从触发的多个规则中，找到阈值最高的那一个作为本次告警的主要代表
        GuardianRule primaryRule = context.getTriggeredRules().stream()
                .max(Comparator.comparing(GuardianRule::getCount))
                .orElse(null);

        if (primaryRule == null) {
            log.warn("[Guardian] LogActionStrategy executed but no primary rule found in context for key: {}", context.getKey());
            return;
        }

        // 2. 从上下文中提取日志所需的核心信息
        String level = primaryRule.getLevel();
        String scene = context.getDefinition().getCounterParam().getScene();
        String key = context.getKey();
        long currentCount = context.getCurrentCount();
        long threshold = primaryRule.getCount();

        // 3. 使用 SLF4J 的参数化日志，性能更优
        String logTemplate = "[Guardian] Alert Triggered | Level: {} | Scene: {} | Key: {} | Count: {} | Threshold: {}";

        // 4. 根据规则级别，选择不同的日志输出级别
        switch (level.toUpperCase()) {
            case GuardianConstants.ALERT_LEVEL_INFO:
                log.info(logTemplate, level, scene, key, currentCount, threshold);
                break;
            case GuardianConstants.ALERT_LEVEL_WARNING:
                log.warn(logTemplate, level, scene, key, currentCount, threshold);
                break;
            case GuardianConstants.ALERT_LEVEL_CRITICAL:
                log.error(logTemplate, level, scene, key, currentCount, threshold);
                break;
            default:
                log.info(logTemplate, level, scene, key, currentCount, threshold);
        }
    }

    @Override
    public String getType() {
        return GuardianConstants.STRATEGY_TYPE_LOG_ONLY;
    }

    /**
     * 日志记录策略不应中断原始方法的执行。
     */
    @Override
    public boolean failFast() {
        return false;
    }

}