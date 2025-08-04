package com.pickyboy.guardian.core.key.impl;

import cn.hutool.core.util.StrUtil;
import com.pickyboy.guardian.core.key.KeyProvider;
import com.pickyboy.guardian.exception.ParseKeyException; // 引入我们自定义的异常
import com.pickyboy.guardian.model.constant.GuardianConstants;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * IP地址 Key生成器 (集成错误处理)
 * <p>
 * 核心职责：自动获取客户端真实IP地址，用于基于IP的访问控制。
 *
 * @author pickyboy
 */
@Slf4j
@Component
@Order(100) // 赋予一个较高的优先级
public class IpKeyProvider implements KeyProvider {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_CLIENT_IP",
            "REMOTE_ADDR"
    };

    @Override
    public String generateKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        try {
            // 直接获取客户端IP并返回
            String clientIp = getClientIp(joinPoint);
            // 增加一次最终校验
            if (StrUtil.isBlank(clientIp)) {
                throw new ParseKeyException("经过所有尝试后，未能获取到有效的客户端IP地址。");
            }
            return clientIp;
        } catch (Exception e) {
            // 如果异常不是我们自定义的 ParseKeyException，就将其包装起来
            if (!(e instanceof ParseKeyException)) {
                throw new ParseKeyException("解析IP地址时发生未知错误", e);
            }
            // 否则直接向上抛出
            throw e;
        }
    }

    @Override
    public String getName() {
        return GuardianConstants.KEY_PROVIDER_IP;
    }

    @Override
    public String getDescription() {
        return "IP地址 Key生成器，自动获取客户端真实IP";
    }

    @Override
    public boolean supports(String keyExpression) {
        return GuardianConstants.KEY_PROVIDER_IP.equalsIgnoreCase(keyExpression);
    }

    /**
     * 统一的IP获取入口，整合了从参数和上下文获取的逻辑。
     *
     * @return 获取到的IP地址，如果找不到则返回 null
     */
    private String getClientIp(ProceedingJoinPoint joinPoint) {
        String ip = extractIpFromArgs(joinPoint);
        if (StrUtil.isBlank(ip)) {
            ip = extractIpFromRequestContext();
        }
        return ip;
    }

    private String extractIpFromArgs(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof HttpServletRequest) {
                return getRealIpAddress((HttpServletRequest) arg);
            }
        }
        return null;
    }

    private String extractIpFromRequestContext() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return getRealIpAddress(request);
            }
        } catch (IllegalStateException e) {
            log.trace("当前非Web请求上下文，无法从 RequestContextHolder 获取IP。");
        } catch (Exception e) {
            log.warn("从 RequestContextHolder 获取IP失败", e);
        }
        return null;
    }

    private String getRealIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = null;
        for (String header : IP_HEADERS) {
            ip = request.getHeader(header);
            if (isValidIp(ip)) {
                break;
            }
        }
        if (!isValidIp(ip)) {
            ip = request.getRemoteAddr();
        }
        if (StrUtil.isNotBlank(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }

    private boolean isValidIp(String ip) {
        return StrUtil.isNotBlank(ip) &&
                !"unknown".equalsIgnoreCase(ip) &&
                !"null".equalsIgnoreCase(ip);
    }
}