package com.pickyboy.guardian.model;

import com.pickyboy.guardian.model.rule.AlertInfo;
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
     * 触发检查的防护定义名称 (对应 @GuardianCheck 的 scene)。
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
     * 距离下一个最高阈值还剩余的次数。
     * 如果已达到最高阈值，则为 0。
     */
    private long remainingCount;

    /**
     * 计数器重置的剩余时间
     * -1表示不需要重置
     */
    private long timeToReset;

    /**
     * 当 allowed 为 false 时，触发的最高级别告警信息。
     */
    private AlertInfo triggeredAlert;


    /**
     * 错误消息
     */
    private String errorMessage;


    /**
     * 快速创建允许通过的结果
     */
    public static GuardianCheckResult allowed(String scene , long currentCount, long remainingCount) {
        return GuardianCheckResult.builder()
                .scene(scene)
                .allowed(true)
                .currentCount(currentCount)
                .remainingCount(remainingCount)
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
}