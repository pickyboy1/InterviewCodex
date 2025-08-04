package com.pickyboy.guardian.core.key.impl;

import cn.hutool.core.util.StrUtil;
import com.pickyboy.guardian.core.key.KeyProvider;
import com.pickyboy.guardian.core.UserIdInterface;
import com.pickyboy.guardian.exception.GuardianException;
import com.pickyboy.guardian.exception.UserIdNotFoundException;
import com.pickyboy.guardian.model.constant.GuardianConstants;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 用户ID Key生成器 (最终版)
 * <p>
 * 通过接口回调和智能反射逻辑，灵活地获取用户ID。
 * 当自动反射无法确定唯一ID时，会抛出清晰的异常指导用户进行自定义实现。
 *
 * @author pickyboy
 */
@Slf4j
@Component
@Order(100) // 赋予一个较高的优先级
public class UserKeyProvider implements KeyProvider {

    /**
     * 采用可选注入。
     * 如果使用者在他的项目中定义了 UserIdInterface 的 Bean，Spring 就会将其注入。
     * 如果没有，此字段为 null，我们会回退到默认逻辑。
     */
    @Autowired(required = false)
    private UserIdInterface userIdInterface;

    @Override
    public String generateKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        try {
            String userId = null;

            // 优先级 1: 委托给用户提供的 UserIdInterface 实现
            if (userIdInterface != null) {
                userId = userIdInterface.getUserId(joinPoint, keyExpression);
                log.debug("Using custom UserIdInterface, resolved userId: {}", userId);
            }

            // 优先级 2 (兜底逻辑): 如果没有自定义实现，或自定义实现返回了空值，
            // 则执行默认的智能反射逻辑。
            if (StrUtil.isBlank(userId)) {
                userId = extractUserIdFromArgs(joinPoint);
                log.debug("Using default reflection logic, resolved userId: {}", userId);
            }

            // 最终校验：如果所有尝试都失败了，抛出清晰的指导性异常
            if (StrUtil.isNotBlank(userId)) {
                return userId; // 成功获取，直接返回
            }

            // 抛出我们自定义的、带有明确指导信息的异常
            throw new UserIdNotFoundException(
                    "Guardian 无法自动确定用户ID。请通过实现 'UserIdInterface' 接口并将其注册为 Bean 来提供明确的用户ID获取逻辑。 " +
                            "更多信息请参阅文档：http://example.pickyboy.com"
            );

        } catch (UserIdNotFoundException e) {
            // 直接向上抛出我们的指导性异常，让 AOP 切面能够捕获并处理
            throw e;
        } catch (Exception e) {
            // 对于其他所有未预料到的异常，进行日志记录并包装成通用的 GuardianException
            log.error("UserKeyProvider 在生成 Key 的过程中发生未知异常", e);
            throw new GuardianException("UserKeyProvider failed to generate key", e);
        }
    }

    @Override
    public String getName() {
        return GuardianConstants.KEY_PROVIDER_USER;
    }

    @Override
    public String getDescription() {
        return "用户ID Key生成器，通过接口回调或智能反射提取用户ID";
    }

    @Override
    public boolean supports(String keyExpression) {
        return GuardianConstants.KEY_PROVIDER_USER.equalsIgnoreCase(keyExpression);
    }

    /**
     * 默认的兜底实现：从方法参数中智能提取用户ID。
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

            // 规则 1: 检查参数名是否是 "userId" 或 "id"
            String paramName = parameter.getName().toLowerCase();
            if (paramName.contains("userid") || paramName.equals("id")) {
                if (arg instanceof Long || arg instanceof String || arg instanceof Integer) {
                    return arg.toString();
                }
            }

            // 规则 2: 根据参数的类名进行智能判断
            String className = arg.getClass().getSimpleName();
            if (className.equalsIgnoreCase("User")) {
                // 如果类名正好是 "User"，我们期望找到 "id" 字段/方法
                String userId = extractFieldFromObject(arg, "id");
                if (StrUtil.isNotBlank(userId)) {
                    return userId;
                }
            } else if (className.toLowerCase().contains("user")) {
                // 如果类名包含 "user" (例如 LoginUserVO)，我们期望找到 "userId" 字段/方法
                String userId = extractFieldFromObject(arg, "userId");
                if (StrUtil.isNotBlank(userId)) {
                    return userId;
                }
            }
        }

        return null; // 如果在参数中未找到，返回 null
    }

    /**
     * 尝试从一个对象中通过反射获取指定名称的属性值。
     * 优先尝试 getter 方法，其次尝试直接访问字段。
     *
     * @param targetObj 目标对象
     * @param fieldName 要获取的字段名 (如 "id" 或 "userId")
     * @return 字段值的字符串表示，如果找不到则返回 null
     */
    private String extractFieldFromObject(Object targetObj, String fieldName) {
        try {
            // 优先尝试调用 getter 方法, e.g., getId() or getUserId()
            String getterName = "get" + StrUtil.upperFirst(fieldName);
            Method getMethod = targetObj.getClass().getMethod(getterName);
            Object id = getMethod.invoke(targetObj);
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            try {
                // 如果没有 getter 方法，尝试直接访问同名字段
                java.lang.reflect.Field idField = targetObj.getClass().getDeclaredField(fieldName);
                idField.setAccessible(true);
                Object id = idField.get(targetObj);
                return id != null ? id.toString() : null;
            } catch (Exception ex) {
                // 在这里，我们不抛出异常，而是返回 null，因为这只是多种尝试中的一种。
                // 最终的异常将在 generateKey 方法中，当所有尝试都失败后抛出。
                log.debug("无法从对象 {} 中提取字段 '{}'", targetObj.getClass().getName(), fieldName);
                return null;
            }
        }
    }
}
