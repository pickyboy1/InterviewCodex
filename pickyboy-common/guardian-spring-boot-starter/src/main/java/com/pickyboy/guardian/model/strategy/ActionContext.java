package com.pickyboy.guardian.model.strategy;

import com.pickyboy.guardian.model.GuardianDefinition;
import com.pickyboy.guardian.model.rule.GuardianRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.List;
import java.util.Map;

/**
 * 动作执行上下文
 *
 * @author pickyboy
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionContext {
    /**
     * 完整的防护定义信息。
     */
    private GuardianDefinition definition;

    /**
     * 本次触发的具体规则。
     */
    private List<GuardianRule> triggeredRules;

    /**
     * 触发时的当前计数值。
     */
    private long currentCount;

    /**
     * 用于计数的 Key。
     */
    private String key;

    /**
     * AOP 切点信息，策略可以访问原始方法参数，或决定是否继续执行。
     */
    private ProceedingJoinPoint joinPoint;

    /**
     * 供策略实现者使用的额外元数据。
     */
    private Map<String, Object> metadata;
}
