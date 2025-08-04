package com.pickyboy.guardian.strategy;

import com.pickyboy.guardian.model.strategy.ActionContext;

/**
 * 动作策略接口
 *
 * @author pickyboy
 */
public interface ActionStrategy {
    /**
     * 执行动作
     *
     * @param context 动作上下文
     */
    void execute(ActionContext context);

    /**
     * 获取策略类型
     *
     * @return 动作类型
     */
    String getType();

    /**
     * 获取策略描述
     *
     * @return 策略描述
     */
    default String getDescription() {
        return getType();
    }

    /**
     * 是否启用异步执行
     *
     * @return 是否异步
     */
    default boolean isAsync() {
        return false;
    }
}
