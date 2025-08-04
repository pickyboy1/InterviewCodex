package com.pickyboy.guardian.aop;

import cn.hutool.core.util.StrUtil;
import com.pickyboy.guardian.anotation.GuardianCheck;
import com.pickyboy.guardian.core.GuardianService;
import com.pickyboy.guardian.exception.GuardianException;
import com.pickyboy.guardian.model.response.GuardianCheckResult;
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
            String key = guardianService.buildLimitKey(point, guardianCheck);
            if (StrUtil.isBlank(key)) {
                log.warn("Guardian键为空，跳过检查");
                return point.proceed();
            }

            // 2. 构建规则
            GuardianDefinition definition = guardianService.buildDef(guardianCheck);

            // 3. 执行检查
            GuardianCheckResult result = guardianService.check(key, point, definition);

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




}
