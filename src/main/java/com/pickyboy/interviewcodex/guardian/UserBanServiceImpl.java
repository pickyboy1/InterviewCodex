package com.pickyboy.interviewcodex.guardian;

import cn.dev33.satoken.stp.StpUtil;
import com.pickyboy.guardian.core.UserBanService;
import com.pickyboy.guardian.model.strategy.ActionContext;
import org.springframework.stereotype.Component;

@Component
public class UserBanServiceImpl implements UserBanService {
    @Override
    public boolean banUser(Long aLong, ActionContext actionContext) {
        Object userId = actionContext.getKey();
        StpUtil.kickout(userId);
        StpUtil.disable(userId,300);
        return true;
    }
}
