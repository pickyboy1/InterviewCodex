package com.pickyboy.guardian.aop;

import cn.hutool.core.util.StrUtil;
import com.pickyboy.guardian.anotation.GuardianCheck;
import com.pickyboy.guardian.core.GuardianService;
import com.pickyboy.guardian.core.key.KeyProvider;
import com.pickyboy.guardian.core.key.KeyProviderManager;
import com.pickyboy.guardian.exception.GuardianException;
import com.pickyboy.guardian.model.GuardianCheckResult;
import com.pickyboy.guardian.model.GuardianDefinition;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Guardian AOP切面
 *
 * @author pickyboy
 */
@Slf4j
@Aspect
@Component
public class GuardianAspect {

    @Resource
    private GuardianService guardianService;

    @Around("@annotation(guardianCheck)")
    public Object around(ProceedingJoinPoint point, GuardianCheck guardianCheck) throws Throwable {
        try {
            // 1. 构建限流键
            String key = buildLimitKey(point, guardianCheck);
            if (StrUtil.isBlank(key)) {
                log.warn("Guardian键为空，跳过检查");
                return point.proceed();
            }

            // 2. 构建规则
            GuardianDefinition rule = guardianService.buildRule(
                    guardianCheck.scene(),
                    guardianCheck.counterType(),
                    guardianCheck.windowSize(),
                    guardianCheck.timeUnit(),
                    guardianCheck.expiration(),
                    guardianCheck.rules(),
                    guardianCheck.errorMessage()
            );

            // 3. 执行检查
            GuardianCheckResult result = guardianService.checkLimit(key, guardianCheck.scene(), rule);

            // 4. 根据结果决定是否继续执行
            if (!result.isAllowed()) {
                throw new GuardianException(result.getErrorMessage());
            }

            return point.proceed();

        } catch (GuardianException e) {
            // Guardian异常直接抛出
            throw e;
        } catch (Exception e) {
            log.error("Guardian切面执行异常", e);
            // 其他异常时允许通过，避免影响业务
            return point.proceed();
        }
    }

    /**
     * 构建限流键
     */
    private String buildLimitKey(ProceedingJoinPoint point, GuardianCheck guardianCheck) {
        String keyExpression = guardianCheck.key();
        String keyProviderName = guardianCheck.keyProvider();

        try {
            KeyProvider keyProvider = selectKeyProvider(keyProviderName, keyExpression);
            String key = keyProvider.generateKey(point, keyExpression);

            if (StrUtil.isBlank(key)) {
                log.warn("KeyProvider生成的key为空: provider={}, expression={}",
                        keyProvider.getName(), keyExpression);
                return null;
            }

            return key;

        } catch (Exception e) {
            log.error("构建Guardian Key失败: provider={}, expression={}", keyProviderName, keyExpression, e);
            return null;
        }
    }

    /**
     * 选择合适的KeyProvider
     */
    private KeyProvider selectKeyProvider(String keyProviderName, String keyExpression) {
        // 如果明确指定了KeyProvider
        if (StrUtil.isNotBlank(keyProviderName)) {
            if ("auto".equals(keyProviderName)) {
                return KeyProviderManager.autoSelectKeyProvider(keyExpression);
            } else {
                return KeyProviderManager.getKeyProvider(keyProviderName);
            }
        }

        // 如果没有指定，则自动选择
        return KeyProviderManager.autoSelectKeyProvider(keyExpression);
    }
}
