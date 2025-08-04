package com.pickyboy.guardian.core;

import cn.hutool.core.util.StrUtil;
import com.pickyboy.guardian.anotation.GuardianCheck;
import com.pickyboy.guardian.anotation.Rule;
import com.pickyboy.guardian.config.GuardianProperties;
import com.pickyboy.guardian.core.counter.Counter;
import com.pickyboy.guardian.core.key.KeyProvider;
import com.pickyboy.guardian.core.key.KeyProviderManager;
import com.pickyboy.guardian.model.counter.CounterParam;
import com.pickyboy.guardian.model.GuardianDefinition;
import com.pickyboy.guardian.model.response.AlertInfo;
import com.pickyboy.guardian.model.response.GuardianCheckResult;
import com.pickyboy.guardian.model.rule.GuardianRule;
import com.pickyboy.guardian.model.strategy.ActionContext;
import com.pickyboy.guardian.strategy.ActionStrategy;
import com.pickyboy.guardian.strategy.ActionStrategyManager;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
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

    // 关键改动：根据配置，容器中只会有一个 Counter Bean，因此直接注入单个实例
    @Resource
    private Counter counter;
    // --- 智能线程池注入 ---
    @Autowired(required = false)
    @Qualifier("guardianExecutor")
    private Executor guardianExecutor; // 优先注入名为 guardianExecutor 的 Bean

    @Autowired(required = false)
    private Executor defaultExecutor; // 其次注入任意一个 Executor Bean

    private Executor executor; // 最终使用的线程池

    @PostConstruct
    public void initExecutor() {
        if (guardianExecutor != null) {
            this.executor = guardianExecutor;
            log.info("Guardian 使用了指定的 'guardianExecutor' 线程池。");
        } else if (defaultExecutor != null) {
            this.executor = defaultExecutor;
            log.info("Guardian 使用了项目中默认的 Executor 线程池。");
        } else {
            this.executor = ForkJoinPool.commonPool();
            log.warn("Guardian: 未找到名为 'guardianExecutor' 或其他 Executor 类型的 Bean，将回退到使用 ForkJoinPool.commonPool()。" +
                    "对于 I/O 密集型策略，强烈建议在您的项目中配置一个专用的线程池 Bean。");
        }
    }

    /**
     * 执行Guardian检查
     */
    public GuardianCheckResult check(String finalKey, ProceedingJoinPoint point, GuardianDefinition definition) {
        if (!guardianProperties.isEnabled()) {
            return GuardianCheckResult.allowed(definition.getCounterParam().getScene(),0);
        }

        try {
            // 1. 递增获取当前计数
            long currentCount = counter.incrementAndGet(definition.getCounterParam());

            // 2. 检查阈值并处理告警
            List<GuardianRule> matchedRules = definition.getRules().stream()
                    .filter(rule -> {
                        if (rule.isContinuous()) {
                            // 持续模式，使用 >=
                            return currentCount >= rule.getCount();
                        } else {
                            // 单次模式，使用 ==
                            return currentCount == rule.getCount();
                        }
                    })
                    .toList();

            // 3. 如果没有规则匹配，则允许通过
            if (matchedRules.isEmpty()) {
                return GuardianCheckResult.allowed(definition.getCounterParam().getScene(), currentCount);
            }

            // 4. 如果有规则匹配，则执行处置
            // 4.1 收集所有需要执行的策略
            List<ActionStrategy> strategiesToExecute = matchedRules.stream()
                    .flatMap(rule -> ActionStrategyManager.getActionStrategies(rule.getStrategies()).stream())
                    .distinct()
                    .collect(Collectors.toList());

            // 4.2 创建上下文
            ActionContext context = ActionContext.builder()
                    .definition(definition)
                    .triggeredRules(matchedRules) // 传递所有触发的规则
                    .currentCount(currentCount)
                    .key(finalKey)
                    .joinPoint(point)
                    .build();

            // 4.3 异步执行所有策略
            strategiesToExecute.forEach(strategy ->
                    CompletableFuture.runAsync(() -> strategy.execute(context), executor)
                            .exceptionally(e -> {
                                log.error("异步执行策略 '{}' 失败", strategy.getType(), e);
                                return null;
                            })
            );

            // 4.4 判断是否需要快速失败（中断请求）
            boolean shouldFailFast = strategiesToExecute.stream().anyMatch(ActionStrategy::failFast);

            // 4.5 构建并返回最终结果
            AlertInfo alertInfo = buildAggregatedAlertInfo(context);
            if (shouldFailFast) {
                return GuardianCheckResult.denied(definition.getCounterParam().getScene(), definition.getErrorMessage(), currentCount, alertInfo);
            } else {
                GuardianCheckResult result = GuardianCheckResult.allowed(definition.getCounterParam().getScene(), currentCount);
                result.setTriggeredAlert(alertInfo); // 即使不中断，也附上告警信息供日志等使用
                return result;
            }

        } catch (Exception e) {
            log.error("Guardian检查异常", e);
            // 异常时允许通过，避免影响业务
            return GuardianCheckResult.allowed(definition.getCounterParam().getScene(),0);
        }
    }

    /**
     * 构建Guardian规则（从注解和配置创建）
     */
    public GuardianDefinition buildDef(GuardianCheck guardianCheck) {

        // 构建计数器参数
        CounterParam counterParam = CounterParam.builder()
                .scene(guardianCheck.scene())
                .key(guardianCheck.key())
                .windowSize(guardianCheck.windowSize() > 0 ? guardianCheck.windowSize() : guardianProperties.getDefaultWindowSize())
                .timeUnit(guardianCheck.timeUnit() != null ? guardianCheck.timeUnit() : guardianProperties.getDefaultTimeUnit())
                .build();


        // 构建规则对象列表
        List<GuardianRule> rules = (guardianCheck.rules() != null && guardianCheck.rules().length > 0)
                ? Arrays.stream(guardianCheck.rules()).map(this::convertAnnotationToRule).collect(Collectors.toList())
                : guardianProperties.getDefaultRules();

        // 错误信息
        String errorMessage = guardianCheck.errorMessage()!=null?guardianCheck.errorMessage() :
                guardianProperties.getDefaultErrorMessage();

        // 构造
        return GuardianDefinition.builder()
                .counterParam(counterParam)
                .rules(rules)
                .errorMessage(errorMessage)
                .build();

    }



    /**
     * 构建限流键
     */
    public String buildLimitKey(ProceedingJoinPoint point, GuardianCheck guardianCheck) {
        String keyExpression = guardianCheck.key();

        try {
            KeyProvider keyProvider = KeyProviderManager.autoSelectKeyProvider(keyExpression);
            String key = keyProvider.generateKey(point, keyExpression);

            if (StrUtil.isBlank(key)) {
                log.warn("KeyProvider生成的key为空: provider={}, expression={}",
                        keyProvider.getName(), keyExpression);
                return null;
            }

            return key;

        } catch (Exception e) {
            log.error("构建Guardian Key失败:  expression={}", keyExpression, e);
            throw e;
        }
    }

    /**
     * 将注解 @Rule 转换为实体 GuardianRule
     */
    private GuardianRule convertAnnotationToRule(Rule rule) {
        return GuardianRule.builder()
                .count(rule.count())
                .level(rule.level())
                .strategies(Arrays.asList(rule.strategy()))
                .description(rule.description())
                .continuous(rule.continuous())
                .build();
    }


    /**
     * 根据上下文构建聚合的告警信息对象
     */
    private AlertInfo buildAggregatedAlertInfo(ActionContext context) {
        List<GuardianRule> triggeredRules = context.getTriggeredRules();
        // 以最高级别的规则为准
        GuardianRule primaryRule = triggeredRules.stream()
                .max(Comparator.comparing(rule -> rule.getCount()))
                .orElse(null);
        if (primaryRule == null) return null;

        // 关键改动：构建包含所有触发规则描述的 details 字符串
        String baseDetails = String.format("Scene: %s, Key: %s", context.getDefinition().getCounterParam().getScene(), context.getKey());
        String triggeredRulesDetails = triggeredRules.stream()
                .map(rule -> String.format("Rule(count=%d, desc='%s')", rule.getCount(), rule.getDescription()))
                .collect(Collectors.joining("; "));
        String finalDetails = baseDetails + " | Triggered Rules: [ " + triggeredRulesDetails + " ]";

        return AlertInfo.builder()
                .level(primaryRule.getLevel())
                .threshold(primaryRule.getCount())
                .strategies(triggeredRules.stream().flatMap(r -> r.getStrategies().stream()).distinct().collect(Collectors.toList()))
                .alertTime(LocalDateTime.now())
                .message("触发 " + triggeredRules.size() + " 条规则")
                .details(finalDetails) // 使用我们新构建的、更详细的 details
                .build();
    }

}
