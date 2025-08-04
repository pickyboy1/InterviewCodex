package com.pickyboy.guardian.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
public class ActionStrategyManager {

    // 用于按名称快速查找
    private static Map<String, ActionStrategy> actionStrategiesMap = Collections.emptyMap();

    @Autowired(required = false)
    private List<ActionStrategy> strategies = Collections.emptyList();

    @PostConstruct
    public void initStrategies() {
        if (strategies.isEmpty()) {
            log.warn("Guardian: 未找到任何 ActionStrategy 的 Bean 实现。");
            return;
        }
        // 根据 @Order 注解排序
        strategies.sort(AnnotationAwareOrderComparator.INSTANCE);

        // 转换为 Map
        actionStrategiesMap = strategies.stream()
                .collect(Collectors.toMap(ActionStrategy::getType, Function.identity()));

        log.info("ActionStrategyManager 初始化完成");
        log.info("可用处置策略 (按优先级): {}",
                strategies.stream().map(ActionStrategy::getType).collect(Collectors.toList()));
    }

    /**
     * 根据名称获取处置策略。
     */
    public static ActionStrategy getActionStrategy(String name) {
        ActionStrategy strategy = actionStrategiesMap.get(name);
        if (strategy == null) {
            throw new IllegalArgumentException("未找到名为 '" + name + "' 的处置策略");
        }
        return strategy;
    }

    /**
     * 根据名称列表获取一批处置策略。
     */
    public static List<ActionStrategy> getActionStrategies(List<String> names) {
        if (names == null || names.isEmpty()) {
            return Collections.emptyList();
        }
        return names.stream()
                .map(actionStrategiesMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
