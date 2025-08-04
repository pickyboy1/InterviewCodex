package com.pickyboy.guardian.strategy.impl;

import com.pickyboy.guardian.exception.GuardianException;
import com.pickyboy.guardian.model.constant.GuardianConstants;
import com.pickyboy.guardian.model.strategy.ActionContext;
import com.pickyboy.guardian.strategy.ActionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 拒绝请求的处置策略 (最终版)
 * <p>
 * 核心职责：通过抛出异常来中断原始方法的执行。
 *
 * @author pickyboy
 */
@Slf4j
@Component
@Order(100) // 赋予一个高优先级
public class RejectActionStrategy implements ActionStrategy {

    @Override
    public void execute(ActionContext context) {
        // 记录一条简洁的拒绝日志，包含核心上下文信息
        log.warn("[Guardian] 拒绝访问请求. Scene: {}, Key: {}, Count: {}",
                context.getDefinition().getCounterParam().getScene(),
                context.getKey(),
                context.getCurrentCount());

        // 抛出异常，这是实现“快速失败”的关键。
        // AOP 切面将捕获此异常并中断方法执行。
        throw new GuardianException(context.getDefinition().getErrorMessage());
    }

    @Override
    public String getType() {
        return GuardianConstants.STRATEGY_TYPE_REJECT;
    }

    /**
     * 拒绝策略的核心就是快速失败，必须返回 true。
     */
    @Override
    public boolean failFast() {
        return true;
    }
}