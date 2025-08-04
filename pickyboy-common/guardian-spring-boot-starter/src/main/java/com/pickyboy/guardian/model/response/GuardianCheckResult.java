package com.pickyboy.guardian.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Guardian检查结果,将检查和执行解耦
 *
 * @author pickyboy
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardianCheckResult {


    /**
     * 触发场景
     */
    private String scene;


    /**
     * 是否允许通过
     */
    private boolean allowed;

    /**
     * 当前计数
     */
    private long currentCount;



    /**
     * 触发的告警信息。
     */
    private AlertInfo triggeredAlert;


    /**
     * 错误消息
     */
    private String errorMessage;


    /**
     * 快速创建允许通过的结果
     */
    public static GuardianCheckResult allowed(String scene , long currentCount) {
        return GuardianCheckResult.builder()
                .scene(scene)
                .allowed(true)
                .currentCount(currentCount)
                .build();
    }

    /**
     * 快速创建拒绝通过的结果
     */
    public static GuardianCheckResult denied(String scene , String errorMessage, long currentCount) {
        return GuardianCheckResult.builder()
                .scene(scene)
                .allowed(false)
                .currentCount(currentCount)
                .errorMessage(errorMessage)
                .build();
    }

    public static GuardianCheckResult denied(String scene , String errorMessage, long currentCount,AlertInfo alert) {
        return GuardianCheckResult.builder()
                .scene(scene)
                .allowed(false)
                .currentCount(currentCount)
                .errorMessage(errorMessage)
                .triggeredAlert(alert)
                .build();
    }
}