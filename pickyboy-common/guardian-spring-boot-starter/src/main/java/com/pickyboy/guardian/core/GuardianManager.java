package com.pickyboy.guardian.core;

import com.pickyboy.guardian.core.counter.Counter;
import com.pickyboy.guardian.model.constant.GuardianConstants;
import com.pickyboy.guardian.strategy.ActionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Guardian策略管理器
 * 管理所有的计数器策略和动作策略
 * 使用静态成员变量存储，便于全局访问
 *
 * @author pickyboy
 */
@Slf4j
@Component
public class GuardianManager {

    /**
     * 计数器策略映射（静态成员）
     */
    private static Map<String, Counter> counterStrategies;

    /**
     * 动作策略映射（静态成员）
     */
    private static Map<String, ActionStrategy> actionStrategies;

    /**
     * 注入的计数器列表
     */
    @Autowired
    private List<Counter> counters;

    /**
     * 注入的动作策略列表
     */
    @Autowired
    private List<ActionStrategy> actions;

    /**
     * 初始化策略映射
     */
    @PostConstruct
    public void initStrategies() {
        // 初始化计数器策略映射
        counterStrategies = counters.stream()
                .collect(Collectors.toMap(Counter::getType, Function.identity()));

        // 初始化动作策略映射
        actionStrategies = actions.stream()
                .collect(Collectors.toMap(ActionStrategy::getType, Function.identity()));

        log.info("Guardian策略管理器初始化完成");
        log.info("可用计数器策略: {}", counterStrategies.keySet());
        log.info("可用动作策略: {}", actionStrategies.keySet());
    }

    /**
     * 获取计数器策略
     */
    public static Counter getCounter(String type) {
        Counter counter = counterStrategies.get(type);
        if (counter == null) {
            throw new IllegalArgumentException("未找到计数器策略: " + type);
        }
        return counter;
    }

    /**
     * 获取动作策略
     */
    public static ActionStrategy getActionStrategy(String type) {
        ActionStrategy strategy = actionStrategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("未找到动作策略: " + type);
        }
        return strategy;
    }

    /**
     * 获取默认计数器策略
     */
    public static Counter getDefaultCounter() {
        // 优先使用Redis，如果不可用则使用本地缓存
        if (counterStrategies.containsKey(GuardianConstants.COUNTER_TYPE_REDIS)) {
            return counterStrategies.get(GuardianConstants.COUNTER_TYPE_REDIS);
        } else {
            return counterStrategies.get(GuardianConstants.COUNTER_TYPE_LOCAL);
        }
    }

    /**
     * 检查策略是否可用
     */
    public static boolean isCounterAvailable(String type) {
        return counterStrategies != null && counterStrategies.containsKey(type);
    }

    /**
     * 检查动作策略是否可用
     */
    public static boolean isActionStrategyAvailable(String type) {
        return actionStrategies != null && actionStrategies.containsKey(type);
    }

    /**
     * 获取所有可用的计数器类型
     */
    public static String[] getAvailableCounterTypes() {
        return counterStrategies != null ?
                counterStrategies.keySet().toArray(new String[0]) :
                new String[0];
    }

    /**
     * 获取所有可用的动作类型
     */
    public static String[] getAvailableActionTypes() {
        return actionStrategies != null ?
                actionStrategies.keySet().toArray(new String[0]) :
                new String[0];
    }
}