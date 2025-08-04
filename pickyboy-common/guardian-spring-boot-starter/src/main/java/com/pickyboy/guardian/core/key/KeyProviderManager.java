package com.pickyboy.guardian.core.key;

import com.pickyboy.guardian.core.key.impl.SpelKeyProvider;
import com.pickyboy.guardian.model.constant.GuardianConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * KeyProvider管理器 (最终版)
 * <p>
 * 管理所有的Key生成器，并提供统一的、按优先级的访问接口。
 *
 * @author pickyboy
 */
@Slf4j
@Component
public class KeyProviderManager {

    // 用于按名称快速查找
    private static Map<String, KeyProvider> keyProvidersMap = Collections.emptyMap();
    // 用于按优先级顺序遍历选择
    private static List<KeyProvider> sortedProviders = Collections.emptyList();
    // 默认的回退 Provider
    private static KeyProvider defaultKeyProvider;

    @Autowired(required = false) // 使用可选注入，即使没有provider也不会报错
    private List<KeyProvider> providers = new ArrayList<>();

    @PostConstruct
    public void initKeyProviders() {
        // 1. 确保 SpelKeyProvider 始终存在
        // 检查 Spring 容器是否已经注入了一个 SpelKeyProvider
        boolean spelProviderExists = providers.stream()
                .anyMatch(p -> p.getName().equals(GuardianConstants.KEY_PROVIDER_SPEL));

        // 如果容器中没有，我们就手动创建一个并添加到列表中
        if (!spelProviderExists) {
            providers.add(new SpelKeyProvider());
            log.info("Guardian: 未找到 SpelKeyProvider Bean，已创建默认实例。");
        }

        // 2. 根据 @Order 注解对注入的 List 进行排序
        providers.sort(AnnotationAwareOrderComparator.INSTANCE);

        // 3. 将排序后的 List 保存起来，用于后续的遍历选择
        sortedProviders = providers;

        // 4. 将 List 转换为 Map，用于按名称的快速查找
        keyProvidersMap = providers.stream()
                .collect(Collectors.toMap(KeyProvider::getName, Function.identity()));

        // 5. 设置默认的回退 Provider，现在我们可以确信它一定存在于 Map 中
        defaultKeyProvider = keyProvidersMap.get(GuardianConstants.KEY_PROVIDER_SPEL);

        log.info("KeyProvider管理器初始化完成");
        log.info("可用KeyProvider (按优先级): {}",
                sortedProviders.stream().map(KeyProvider::getName).collect(Collectors.toList()));
    }

    /**
     * 根据名称精确获取KeyProvider。
     */
    public static KeyProvider getKeyProvider(String name) {
        if (keyProvidersMap.isEmpty()) {
            throw new IllegalStateException("KeyProviderManager尚未初始化或未找到任何Provider");
        }
        return keyProvidersMap.get(name);
    }

    /**
     * 智能选择合适的KeyProvider。
     * 根据keyExpression自动选择优先级最高的Provider。
     */
    public static KeyProvider autoSelectKeyProvider(String keyExpression) {
        // 遍历排序好的 List 来保证优先级
        for (KeyProvider provider : sortedProviders) {
            // SpelKeyProvider 作为最后的选项，这里先跳过
            if (provider.getName().equals(GuardianConstants.KEY_PROVIDER_SPEL)) {
                continue;
            }
            if (provider.supports(keyExpression)) {
                return provider;
            }
        }
        // 如果没有找到其他匹配项，则返回默认的 SpelKeyProvider
        return defaultKeyProvider;
    }

    /**
     * 获取默认的回退KeyProvider (通常是SpelKeyProvider)。
     */
    public static KeyProvider getDefaultKeyProvider() {
        return defaultKeyProvider;
    }

    /**

     * 检查指定名称的KeyProvider是否可用。
     */
    public static boolean isKeyProviderAvailable(String name) {
        return keyProvidersMap.containsKey(name);
    }

    /**
     * 获取所有可用的KeyProvider名称 (已按优先级排序)。
     */
    public static List<String> getAvailableKeyProviderNames() {
        return sortedProviders.stream()
                .map(KeyProvider::getName)
                .collect(Collectors.toList());
    }
}