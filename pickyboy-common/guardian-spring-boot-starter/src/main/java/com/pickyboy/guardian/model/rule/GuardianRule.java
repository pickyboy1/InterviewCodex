package com.pickyboy.guardian.model.rule;

import com.pickyboy.guardian.model.constant.GuardianConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 阈值配置
 *
 * @author pickyboy
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardianRule {
    /**
     * 触发此规则的计数值(阈值)
     */
    private long count;

    /**
     * 告警级别
     */
    private String level = GuardianConstants.ALERT_LEVEL_WARNING;

    /**
     * 达到阈值后的处理策略列表
     */
    private List<String> strategies;


    /**
     * 描述信息
     */
    private String description;
}