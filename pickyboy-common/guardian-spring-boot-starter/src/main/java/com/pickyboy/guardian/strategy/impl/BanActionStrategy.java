package com.pickyboy.guardian.strategy.impl;

import com.pickyboy.guardian.model.constant.GuardianConstants;
import com.pickyboy.guardian.model.strategy.ActionContext;
import com.pickyboy.guardian.strategy.ActionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/**
 * 封禁动作策略
 * 包含踢下线和封号两个操作
 * 需要sa-token依赖
 *
 * @author pickyboy
 */
@Slf4j
@Component
@ConditionalOnClass(name = "cn.dev33.satoken.util.SaTokenConsts")
public class BanActionStrategy implements ActionStrategy {

    @Override
    public void execute(ActionContext context) {
        try {
            Long userId = extractUserId(context.getTarget());
            if (userId == null) {
                log.error("封禁失败：无效的用户ID - Context: {}", context);
                return;
            }

            // 1. 踢下线
            kickoutUser(userId);

            // 2. 封号（如果级别是CRITICAL）
            if (GuardianConstants.ALERT_LEVEL_CRITICAL.equals(context.getLevel())) {
                banUser(userId, context);
            }

            log.error("用户被处理 - UserId: {}, Scene: {}, Count: {}, Threshold: {}, Level: {}",
                    userId, context.getScene(), context.getCurrentCount(),
                    context.getThreshold(), context.getLevel());

        } catch (Exception e) {
            log.error("执行封禁动作失败", e);
        }
    }

    /**
     * 踢下线
     */
    private void kickoutUser(Long userId) {
        try {
            // 使用反射调用Sa-Token，避免直接依赖
            Class<?> stpUtilClass = Class.forName("cn.dev33.satoken.util.SaTokenConsts");
            Class<?> stpClass = Class.forName("cn.dev33.satoken.stp.StpUtil");
            stpClass.getMethod("kickout", Object.class).invoke(null, userId);

            log.warn("用户被踢下线 - UserId: {}", userId);
        } catch (Exception e) {
            log.error("踢下线失败 - UserId: {}, Error: {}", userId, e.getMessage());
        }
    }

    /**
     * 封号（需要业务系统自己实现用户服务）
     */
    private void banUser(Long userId, ActionContext context) {
        // TODO: 这里需要业务系统注入UserService来实现封号
        // 由于starter不应该依赖具体的业务逻辑，这里只记录日志
        // 实际使用时可以通过ApplicationContext获取UserService bean

        log.error("用户被封号 - UserId: {}, Reason: {}", userId, context.getReason());
        log.error("注意：封号操作需要在业务系统中实现UserService.banUser()方法");
    }

    /**
     * 提取用户ID
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
                log.warn("无法解析用户ID: {}", target);
                return null;
            }
        }

        return null;
    }

    @Override
    public String getType() {
        return GuardianConstants.ACTION_TYPE_BAN;
    }


    @Override
    public String getDescription() {
        return "踢下线并封禁用户";
    }
}