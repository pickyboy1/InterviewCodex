package com.pickyboy.guardian.core;

import com.pickyboy.guardian.model.strategy.ActionContext;
import org.aspectj.lang.ProceedingJoinPoint;

public interface UserIdInterface {
    String getUserId(ProceedingJoinPoint joinPoint, String keyExpression);


}
