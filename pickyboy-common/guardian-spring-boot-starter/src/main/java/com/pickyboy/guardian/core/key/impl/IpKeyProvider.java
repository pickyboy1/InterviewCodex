package com.pickyboy.guardian.core.key.impl;

import cn.hutool.core.util.StrUtil;
import com.pickyboy.guardian.core.key.KeyProvider;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * IP地址 Key生成器
 * 自动获取客户端真实IP地址
 *
 * 支持以下几种方式获取IP：
 * 1. 从方法参数中的HttpServletRequest获取
 * 2. 从Spring RequestContextHolder获取
 * 3. 处理代理服务器的X-Forwarded-For等头部
 *
 * @author pickyboy
 */
@Slf4j
@Component
public class IpKeyProvider implements KeyProvider {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "X-Original-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    @Override
    public String generateKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        try {
            String ip = null;

            // 1. 从keyExpression中解析（如果指定了具体IP）
            if (StrUtil.isNotBlank(keyExpression) && !keyExpression.equals("ip")) {
                ip = parseIpFromExpression(keyExpression);
            }

            // 2. 从方法参数中的HttpServletRequest获取
            if (StrUtil.isBlank(ip)) {
                ip = extractIpFromArgs(joinPoint);
            }

            // 3. 从Spring RequestContextHolder获取
            if (StrUtil.isBlank(ip)) {
                ip = extractIpFromRequestContext();
            }

            if (StrUtil.isBlank(ip)) {
                log.warn("IpKeyProvider无法获取客户端IP，使用默认值");
                ip = "unknown";
            }

            return "ip:" + ip;

        } catch (Exception e) {
            log.error("IpKeyProvider生成key失败", e);
            return "ip:error";
        }
    }

    @Override
    public String getName() {
        return "ip";
    }

    @Override
    public String getDescription() {
        return "IP地址 Key生成器，自动获取客户端真实IP";
    }

    @Override
    public boolean supports(String keyExpression) {
        return StrUtil.isBlank(keyExpression) ||
               keyExpression.equals("ip") ||
               keyExpression.startsWith("ip:");
    }

    /**
     * 从keyExpression中解析IP
     */
    private String parseIpFromExpression(String keyExpression) {
        if (keyExpression.startsWith("ip:")) {
            return keyExpression.substring(3);
        }
        return null;
    }

    /**
     * 从方法参数中提取IP
     */
    private String extractIpFromArgs(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length && i < args.length; i++) {
            Object arg = args[i];

            if (arg != null && arg instanceof HttpServletRequest) {
                return getRealIpAddress((HttpServletRequest) arg);
            }
        }

        return null;
    }

    /**
     * 从Spring RequestContextHolder获取IP
     */
    private String extractIpFromRequestContext() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return getRealIpAddress(request);
            }
        } catch (Exception e) {
            log.debug("从RequestContextHolder获取IP失败", e);
        }
        return null;
    }

    /**
     * 获取真实IP地址
     * 处理代理服务器的情况
     */
    private String getRealIpAddress(HttpServletRequest request) {
        String ip = null;

        // 依次检查各种可能的IP头部
        for (String header : IP_HEADERS) {
            ip = request.getHeader(header);
            if (isValidIp(ip)) {
                break;
            }
        }

        // 如果都没有获取到，使用getRemoteAddr
        if (!isValidIp(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多IP的情况（X-Forwarded-For可能包含多个IP）
        if (StrUtil.isNotBlank(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        // IPv6本地地址转换为IPv4
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip;
    }

    /**
     * 验证IP是否有效
     */
    private boolean isValidIp(String ip) {
        return StrUtil.isNotBlank(ip) &&
               !"unknown".equalsIgnoreCase(ip) &&
               !"null".equalsIgnoreCase(ip);
    }
}