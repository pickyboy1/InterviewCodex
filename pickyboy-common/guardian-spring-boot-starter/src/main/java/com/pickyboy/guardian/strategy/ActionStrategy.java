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
     * 是否快速失败。
     * 如果返回 true，表示执行此策略后应立即中断原始方法的执行。
     *
     * @return 默认为 false (不中断)
     */
    default boolean failFast() {
        return false;
    }
}
