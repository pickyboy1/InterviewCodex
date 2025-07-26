package com.pickyboy.interviewcodex.sentinel;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.circuitbreaker.CircuitBreakerStrategy;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class SentinelRulesManager {
    @PostConstruct
    public void initRules() {
        initFlowRules();
        initDegradeRules();
    }

    public static void initFlowRules() {
        // 单ip查看题目列表流控规则
        ParamFlowRule rule = new ParamFlowRule("listQuestionVOByPageSentinel")
                .setParamIdx(0)
                .setCount(10)
                .setDurationInSec(60);
        ParamFlowRuleManager.loadRules(java.util.Collections.singletonList(rule));
    }

    public static void initDegradeRules() {
        // 单ip查看题目列表熔断规则
        DegradeRule slowCallRule = new DegradeRule("listQuestionVOByPageSentinel")
                .setGrade(CircuitBreakerStrategy.SLOW_REQUEST_RATIO.getType())
                .setCount(0.2) // 慢调用占比
                .setTimeWindow(60)// 熔断时间窗口
                .setStatIntervalMs(30*1000)// 统计时间窗口
                .setMinRequestAmount(10)// 最小请求数
                .setSlowRatioThreshold(3);// 慢调用响应时间

        DegradeRule errorRateRule = new DegradeRule("listQuestionVOByPageSentinel")
                .setGrade(CircuitBreakerStrategy.ERROR_RATIO.getType())
                .setCount(0.1)// 错误比例
                .setTimeWindow(60) // 熔断时间窗口
                .setStatIntervalMs(30*1000) // 统计时间窗口
                .setMinRequestAmount(5); // 最小请求数


        // 加载规则
        DegradeRuleManager.loadRules(java.util.Arrays.asList(slowCallRule, errorRateRule));

    }
}
