package com.pickyboy.guardian.model;

import com.pickyboy.guardian.model.constant.GuardianConstants;
import com.pickyboy.guardian.model.counter.CounterParam;
import com.pickyboy.guardian.model.rule.GuardianRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Guardian防护定义
 *对应@GuardianCheck注解
 *
 * @author pickyboy
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardianDefinition {
    /**
     * 规则名称
     */
    private String name;

    /**
     * 计数器参数
     */
    private CounterParam counterParam;

    /**
     * 防护规则列表
     */
    private List<GuardianRule> rules;


    /**
     * 失败时的错误消息
     */
    @Builder.Default
    private String errorMessage = GuardianConstants.DEFAULT_ERROR_MESSAGE;

    /**
     * 是否启用
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * 描述信息
     */
    private String description;
}
