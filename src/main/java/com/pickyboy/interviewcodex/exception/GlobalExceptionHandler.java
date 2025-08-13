package com.pickyboy.interviewcodex.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.pickyboy.guardian.exception.GuardianException;
import com.pickyboy.interviewcodex.common.BaseResponse;
import com.pickyboy.interviewcodex.common.ErrorCode;
import com.pickyboy.interviewcodex.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author pickyboy
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    // Guardian 异常处理
    @ExceptionHandler(GuardianException.class)
    public BaseResponse<?> guardianExceptionHandler(GuardianException e) {
        log.error("GuardianException",e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR,e.getMessage());
    }
    // Sa-Token相关异常处理
    // 无角色异常处理
    @ExceptionHandler(NotRoleException.class)
    public BaseResponse<?> notRoleExceptionHandler(NotRoleException e) {
        log.error("NotRoleException", e);
        return ResultUtils.error(ErrorCode.NO_AUTH_ERROR, "权限不足");
    }

    // 无权限异常处理
    @ExceptionHandler(NotPermissionException.class)
    public BaseResponse<?> notPermissionExceptionHandler(NotPermissionException e) {
        log.error("NotPermissionException", e);
        return ResultUtils.error(ErrorCode.NO_AUTH_ERROR, "权限不足");
    }

    // 未登录异常处理
    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<?> notLoginExceptionHandler(NotLoginException e) {
        log.error("NotLoginException", e);
        return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR, "当前未登录,请先登录");
    }

    // sentinel 限流异常处理
    @ExceptionHandler(BlockException.class)
    public BaseResponse<?> sentinelExceptionHandler(BlockException e) {
        log.warn("请求被sentinel拦截,规则类型:{},规则详情:{}", e.getClass().getSimpleName(), e.getRule());
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统繁忙,请稍后再试");
    }

}
