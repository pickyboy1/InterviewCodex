package com.pickyboy.guardian.core.key.impl;

import cn.hutool.core.util.StrUtil;
import com.pickyboy.guardian.core.key.KeyProvider;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 用户ID Key生成器
 * 自动从方法参数中寻找用户对象并提取ID
 *
 * 支持以下几种方式获取用户ID：
 * 1. 参数中包含User对象，且有getId()方法
 * 2. 参数名包含userId的Long/String类型参数
 * 3. 从Sa-Token获取当前登录用户ID（如果可用）
 *
 * @author pickyboy
 */
@Slf4j
@Component
public class UserKeyProvider implements KeyProvider {

    @Override
    public String generateKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        try {
            // 1. 优先从keyExpression中解析
            if (StrUtil.isNotBlank(keyExpression) && !keyExpression.equals("user")) {
                String userId = parseUserIdFromExpression(keyExpression);
                if (StrUtil.isNotBlank(userId)) {
                    return "user:" + userId;
                }
            }

            // 2. 从方法参数中自动寻找用户ID
            String userId = extractUserIdFromArgs(joinPoint);
            if (StrUtil.isNotBlank(userId)) {
                return "user:" + userId;
            }

            // 3. 尝试从Sa-Token获取当前登录用户ID
            userId = getCurrentUserIdFromSaToken();
            if (StrUtil.isNotBlank(userId)) {
                return "user:" + userId;
            }

            log.warn("UserKeyProvider无法获取用户ID，使用默认值");
            return "user:unknown";

        } catch (Exception e) {
            log.error("UserKeyProvider生成key失败", e);
            return "user:error";
        }
    }

    @Override
    public String getName() {
        return "user";
    }

    @Override
    public String getDescription() {
        return "用户ID Key生成器，自动提取用户ID";
    }

    @Override
    public boolean supports(String keyExpression) {
        return StrUtil.isBlank(keyExpression) ||
               keyExpression.equals("user") ||
               keyExpression.startsWith("user:");
    }

    /**
     * 从keyExpression中解析用户ID
     */
    private String parseUserIdFromExpression(String keyExpression) {
        if (keyExpression.startsWith("user:")) {
            return keyExpression.substring(5);
        }
        return null;
    }

    /**
     * 从方法参数中提取用户ID
     */
    private String extractUserIdFromArgs(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length && i < args.length; i++) {
            Parameter parameter = parameters[i];
            Object arg = args[i];

            if (arg == null) {
                continue;
            }

            // 1. 检查参数名是否包含userId
            String paramName = parameter.getName().toLowerCase();
            if (paramName.contains("userid") || paramName.equals("id")) {
                if (arg instanceof Long || arg instanceof String || arg instanceof Integer) {
                    return arg.toString();
                }
            }

            // 2. 检查是否是User类型对象
            String className = arg.getClass().getSimpleName().toLowerCase();
            if (className.contains("user")) {
                String userId = extractIdFromUserObject(arg);
                if (StrUtil.isNotBlank(userId)) {
                    return userId;
                }
            }
        }

        return null;
    }

    /**
     * 从用户对象中提取ID
     */
    private String extractIdFromUserObject(Object userObj) {
        try {
            // 尝试调用getId()方法
            Method getIdMethod = userObj.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(userObj);
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            try {
                // 尝试调用id字段
                java.lang.reflect.Field idField = userObj.getClass().getDeclaredField("id");
                idField.setAccessible(true);
                Object id = idField.get(userObj);
                return id != null ? id.toString() : null;
            } catch (Exception ex) {
                log.debug("无法从用户对象中提取ID: {}", userObj.getClass().getName());
                return null;
            }
        }
    }

    /**
     * 从Sa-Token获取当前登录用户ID
     */
    private String getCurrentUserIdFromSaToken() {
        try {
            // 使用反射调用Sa-Token，避免直接依赖
            Class<?> stpUtilClass = Class.forName("cn.dev33.satoken.stp.StpUtil");
            Method getLoginIdMethod = stpUtilClass.getMethod("getLoginId");
            Object loginId = getLoginIdMethod.invoke(null);
            return loginId != null ? loginId.toString() : null;
        } catch (Exception e) {
            // Sa-Token不可用或未登录，返回null
            return null;
        }
    }
}