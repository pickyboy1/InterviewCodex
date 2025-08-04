package com.pickyboy.guardian.strategy.impl;

import com.pickyboy.guardian.model.constant.GuardianConstants;
import com.pickyboy.guardian.model.strategy.ActionContext;
import com.pickyboy.guardian.strategy.ActionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 邮件告警动作策略（模拟实现）
 * 实际只记录日志，模拟邮件发送过程
 *
 * @author pickyboy
 */
@Slf4j
@Component
public class EmailActionStrategy implements ActionStrategy {

    @Override
    public void execute(ActionContext context) {
        String subject = String.format("Guardian告警 - %s", context.getLevel());
        String content = buildEmailContent(context);

        // 模拟邮件发送，实际只记录日志
        log.warn("【邮件告警模拟】Subject: {}, Content: {}", subject, content);
        log.warn("【邮件告警模拟】如需实际发送邮件，请集成邮件服务组件");
    }

    private String buildEmailContent(ActionContext context) {
        return String.format(
                "Guardian系统检测到异常访问行为:\n\n" +
                "场景: %s\n" +
                "告警级别: %s\n" +
                "限流键: %s\n" +
                "当前计数: %d\n" +
                "触发阈值: %d\n" +
                "目标对象: %s\n" +
                "业务类型: %s\n" +
                "原因: %s\n\n" +
                "请及时处理！",
                context.getScene(),
                context.getLevel(),
                context.getKey(),
                context.getCurrentCount(),
                context.getThreshold(),
                context.getTarget(),
                context.getBusinessType(),
                context.getReason()
        );
    }

    @Override
    public String getType() {
        return GuardianConstants.ACTION_TYPE_EMAIL;
    }



    @Override
    public String getDescription() {
        return "发送邮件告警（模拟实现）";
    }
}