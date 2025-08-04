package com.pickyboy.guardian.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警信息
 *
 * @author pickyboy
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertInfo {

    /**
     * 告警级别
     */
    private String level;

    /**
     * 触发阈值
     */
    private long threshold;


    /**
     * 执行的策略列表
     */
    private List<String> strategies;

    /**
     * 告警时间
     */
    private LocalDateTime alertTime;

    /**
     * 告警消息
     */
    private String message;

    /**
     * 告警详情
     */
    private String details;

    public AlertInfo(String level, long threshold) {
        this.level = level;
        this.threshold = threshold;
        this.alertTime = LocalDateTime.now();
    }
}