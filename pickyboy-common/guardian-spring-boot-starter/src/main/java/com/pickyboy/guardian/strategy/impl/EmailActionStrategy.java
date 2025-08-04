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
 * 邮件告警动作策略（模拟实现）
 * 实际只记录日志，模拟邮件发送过程
 *
 * @author pickyboy
 */
@Slf4j
@Component
@Order(200) // 赋予一个中等优先级
public class EmailActionStrategy implements ActionStrategy {

    @Override
    public void execute(ActionContext context) {
        // 1. 从触发的多个规则中，找到阈值最高的那一个作为本次告警的主要代表
        GuardianRule primaryRule = context.getTriggeredRules().stream()
                .max(Comparator.comparing(GuardianRule::getCount))
                .orElse(null);

        if (primaryRule == null) {
            log.warn("[Guardian] EmailActionStrategy executed but no primary rule found in context for key: {}", context.getKey());
            return;
        }

        // 2. 构建邮件主题和内容
        String subject = String.format("Guardian 告警 - [%s] 触发场景: %s",
                primaryRule.getLevel(),
                context.getDefinition().getCounterParam().getScene());

        String content = buildEmailContent(context, primaryRule);

        // 3. 模拟邮件发送，实际只记录日志
        log.warn("【邮件告警模拟】收件人: [admin@example.com]");
        log.warn("【邮件告警模拟】主题: {}", subject);
        log.warn("【邮件告警模拟】内容:\n{}", content);
        log.warn("【邮件告警模拟】如需实际发送邮件，请在项目中集成邮件服务，并提供一个自定义的 ActionStrategy Bean。");
    }

    private String buildEmailContent(ActionContext context, GuardianRule primaryRule) {
        return String.format(
                "Guardian 系统检测到异常访问行为:\n\n" +
                        "告警级别: %s\n" +
                        "触发场景: %s\n" +
                        "限流 Key: %s\n" +
                        "当前计数值: %d\n" +
                        "触发阈值: %d\n" +
                        "规则描述: %s\n\n" +
                        "请及时关注！",
                primaryRule.getLevel(),
                context.getDefinition().getCounterParam().getScene(),
                context.getKey(),
                context.getCurrentCount(),
                primaryRule.getCount(),
                primaryRule.getDescription()
        );
    }

    /**
     * 返回此策略的唯一名称。
     */
    @Override
    public String getType() {
        return GuardianConstants.STRATEGY_TYPE_EMAIL;
    }

    /**
     * 发送邮件告警不应中断原始方法的执行。
     */
    @Override
    public boolean failFast() {
        return false;
    }
}