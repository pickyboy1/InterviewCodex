package com.pickyboy.guardian.core.key.impl;

import cn.hutool.core.util.StrUtil;
import com.pickyboy.guardian.core.key.KeyProvider;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * SpEL表达式 Key生成器
 * 支持Spring Expression Language表达式解析
 * 这是原有的SpEL功能，保持向后兼容
 *
 * @author pickyboy
 */
@Slf4j
@Component
public class SpelKeyProvider implements KeyProvider {

    private final SpelExpressionParser spelParser = new SpelExpressionParser();

    @Override
    public String generateKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        if (StrUtil.isBlank(keyExpression)) {
            return null;
        }

        try {
            // 如果不是SpEL表达式，直接返回
            if (!isSpelExpression(keyExpression)) {
                return keyExpression;
            }

            // 解析SpEL表达式
            Expression expression = spelParser.parseExpression(keyExpression);
            EvaluationContext context = buildEvaluationContext(joinPoint);
            Object result = expression.getValue(context);

            return result != null ? result.toString() : null;

        } catch (Exception e) {
            log.error("SpelKeyProvider解析表达式失败: {}", keyExpression, e);
            return null;
        }
    }

    @Override
    public String getName() {
        return "spel";
    }

    @Override
    public String getDescription() {
        return "SpEL表达式 Key生成器，支持Spring表达式语言";
    }

    @Override
    public boolean supports(String keyExpression) {
        return isSpelExpression(keyExpression);
    }

    /**
     * 判断是否是SpEL表达式
     */
    private boolean isSpelExpression(String keyExpression) {
        return StrUtil.isNotBlank(keyExpression) &&
               (keyExpression.startsWith("#") ||
                keyExpression.contains(".") ||
                keyExpression.contains("'") ||
                keyExpression.contains("+"));
    }

    /**
     * 构建SpEL表达式上下文
     */
    private EvaluationContext buildEvaluationContext(ProceedingJoinPoint joinPoint) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        // 获取方法参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        // 将参数添加到上下文
        for (int i = 0; i < parameters.length && i < args.length; i++) {
            Parameter parameter = parameters[i];
            Object arg = args[i];

            // 使用参数名作为变量名
            String paramName = parameter.getName();
            context.setVariable(paramName, arg);

            // 特殊处理HttpServletRequest
            if (arg != null && arg.getClass().getName().contains("HttpServletRequest")) {
                context.setVariable("request", arg);
            }

            // 特殊处理用户相关对象
            if (arg != null) {
                String className = arg.getClass().getSimpleName().toLowerCase();
                if (className.contains("user")) {
                    context.setVariable("loginUser", arg);
                    context.setVariable("user", arg);
                }
            }
        }

        return context;
    }
}