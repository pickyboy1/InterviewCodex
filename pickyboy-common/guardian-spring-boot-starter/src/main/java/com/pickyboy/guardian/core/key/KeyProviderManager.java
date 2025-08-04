package com.pickyboy.guardian.core.key;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * KeyProvider管理器
 * 管理所有的Key生成器，并提供统一的访问接口
 *
 * @author pickyboy
 */
@Slf4j
@Component
public class KeyProviderManager {

    /**
     * KeyProvider映射（静态成员）
     */
    private static Map<String, KeyProvider> keyProviders;

    /**
     * 默认的KeyProvider（SpEL）
     */
    private static KeyProvider defaultKeyProvider;

    /**
     * 注入的KeyProvider列表
     */
    @Autowired
    private List<KeyProvider> providers;

    /**
     * 初始化KeyProvider映射
     */
    @PostConstruct
    public void initKeyProviders() {
        // 初始化KeyProvider映射
        keyProviders = providers.stream()
                .collect(Collectors.toMap(KeyProvider::getName, Function.identity()));

        // 设置默认的KeyProvider（SpEL）
        defaultKeyProvider = keyProviders.get("spel");

        log.info("KeyProvider管理器初始化完成");
        log.info("可用KeyProvider: {}", keyProviders.keySet());
    }

    /**
     * 根据名称获取KeyProvider
     */
    public static KeyProvider getKeyProvider(String name) {
        if (keyProviders == null) {
            throw new IllegalStateException("KeyProviderManager尚未初始化");
        }

        KeyProvider provider = keyProviders.get(name);
        if (provider == null) {
            log.warn("未找到KeyProvider: {}, 使用默认Provider", name);
            return defaultKeyProvider;
        }
        return provider;
    }

    /**
     * 自动选择合适的KeyProvider
     * 根据keyExpression自动选择最合适的Provider
     */
    public static KeyProvider autoSelectKeyProvider(String keyExpression) {
        if (keyProviders == null) {
            return defaultKeyProvider;
        }

        // 按优先级顺序检查各个Provider
        for (String providerName : new String[]{"user", "ip", "spel"}) {
            KeyProvider provider = keyProviders.get(providerName);
            if (provider != null && provider.supports(keyExpression)) {
                return provider;
            }
        }

        return defaultKeyProvider;
    }

    /**
     * 获取默认KeyProvider
     */
    public static KeyProvider getDefaultKeyProvider() {
        return defaultKeyProvider != null ? defaultKeyProvider : getKeyProvider("spel");
    }

    /**
     * 检查KeyProvider是否可用
     */
    public static boolean isKeyProviderAvailable(String name) {
        return keyProviders != null && keyProviders.containsKey(name);
    }

    /**
     * 获取所有可用的KeyProvider名称
     */
    public static String[] getAvailableKeyProviderNames() {
        return keyProviders != null ?
                keyProviders.keySet().toArray(new String[0]) :
                new String[0];
    }
}