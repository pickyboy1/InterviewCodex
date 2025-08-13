package com.pickyboy.interviewcodex.guardian;

import cn.dev33.satoken.stp.StpUtil;
import com.pickyboy.guardian.core.UserIdInterface;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;

@Component
public class UserIdInterfaceImpl implements UserIdInterface {

    @Override
    public String getUserId(ProceedingJoinPoint proceedingJoinPoint, String s) {
        return StpUtil.getLoginId().toString();
    }
}
