package com.pickyboy.guardian.core;

import com.pickyboy.guardian.model.strategy.ActionContext;

/**
 * 用户封禁服务接口
 * <p>
 * 这是一个回调接口，应由引入 Guardian 的业务系统来实现，
 * 以便 Guardian 框架能够调用业务系统的特定封号逻辑。
 *
 * @author pickyboy
 */
public interface UserBanService {

    /**
     * 执行封号操作。
     *
     * @param userId  需要被封禁的用户ID
     * @param context 触发时的上下文信息
     * @return 如果封禁成功，返回 true
     */
    boolean banUser(Long userId, ActionContext context);
}