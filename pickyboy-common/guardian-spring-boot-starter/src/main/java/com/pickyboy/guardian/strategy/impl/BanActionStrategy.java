package com.pickyboy.guardian.strategy.impl;

import com.pickyboy.guardian.core.UserBanService;
import com.pickyboy.guardian.model.constant.GuardianConstants;
import com.pickyboy.guardian.model.strategy.ActionContext;
import com.pickyboy.guardian.strategy.ActionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 封禁动作策略
 *
 * @author pickyboy
 */
@Slf4j
@Component
@Order(50) // 赋予一个非常高的优先级
public class BanActionStrategy implements ActionStrategy {


    @Autowired(required = false)
    private UserBanService userBanService;

    @Override
    public void execute(ActionContext context) {
        // 从上下文中提取用户ID，这里假设 Key 就是用户ID
        Long userId = extractUserId(context.getKey());
        if (userId == null) {
            log.warn("[Guardian] BanActionStrategy 无法从 Key '{}' 中提取有效的 UserId。", context.getKey());
            return;
        }


        //  委托封号逻辑
        if (userBanService != null) {
            try {
                boolean success = userBanService.banUser(userId, context);
                if (success) {
                    log.error("用户已被封禁 (通过 UserBanService) - UserId: {}", userId);
                } else {
                    log.error("UserBanService 报告封禁失败 - UserId: {}", userId);
                }
            } catch (Exception e) {
                log.error("执行用户自定义的 UserBanService 时发生错误", e);
            }
        } else {
            log.error("需要执行封号操作，但未找到 UserBanService 的实现。请在您的业务系统中创建一个实现此接口的 Bean。 - UserId: {}", userId);
        }
    }

    /**
     * 提取用户ID的辅助方法
     */
    private Long extractUserId(Object target) {
        if (target == null) {
            return null;
        }
        if (target instanceof Long) {
            return (Long) target;
        }
        if (target instanceof String) {
            try {
                return Long.parseLong((String) target);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        // 可以根据需要扩展其他类型
        return null;
    }

    @Override
    public String getType() {
        return GuardianConstants.STRATEGY_TYPE_BAN;
    }

    /**
     * 封禁策略是最高级别的处置，应该立即中断请求。
     */
    @Override
    public boolean failFast() {
        return true;
    }


}