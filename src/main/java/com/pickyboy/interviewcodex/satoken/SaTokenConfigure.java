package com.pickyboy.interviewcodex.satoken;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token  全局拦截器
 *
 * @author pickyboy
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry){
        // 注册 Sa-Token 拦截器，打开注解式鉴权功能
        registry.addInterceptor(new SaInterceptor(handle-> {
            StpUtil.checkLogin();
                }))
                .addPathPatterns("/**")
                .excludePathPatterns("/user/login", "/user/register",
                        "/doc.html",          // Knife4j UI 页面
                        "/webjars/**",        // Knife4j 静态资源
                        "/swagger-resources/**", // Swagger 资源
                        "/v3/api-docs/**",      // OpenAPI v3 定义
                        "/v2/api-docs/**",
                        "/error"
                        );      // OpenAPI v2 定义 (兼容));
    }
}
