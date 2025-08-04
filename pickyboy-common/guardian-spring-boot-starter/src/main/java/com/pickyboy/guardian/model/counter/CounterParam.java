package com.pickyboy.guardian.model.counter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 计数器配置
 *
 * @author pickyboy
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CounterParam {

    /**
     * 业务场景
     */
    private String scene;

    /**
     * 计数器键
     */
    private String key;

    /**
     * 时间窗口大小
     */
    private int windowSize;

    /**
     * 时间单位
     */
    private TimeUnit timeUnit;



    /**
     * 扩展参数
     */
    private Map<String, Object> extraParams;


}