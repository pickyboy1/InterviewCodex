package com.pickyboy.interviewcodex.sentinel;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * 编码声明资源示例
 */
public class SentinelDemo {

    public static void main(String[] args) {

        // 配置限流规则
        initFlowRules();
        while (true) {
            // 声明被保护的资源
        try(Entry entry = SphU.entry("HelloWorld")){
            // 被保护的资源对应代码段
            System.out.println("hello world");
            Thread.sleep(40);
        }
        catch (BlockException e){
            // 被流控后的处理
            System.out.println("限流了");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        }
    };
// 配置限流规则
    public static void initFlowRules(){
        List<FlowRule> rules = new ArrayList<>();
        FlowRule rule = new FlowRule();
        rule.setResource("HelloWorld");
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        // Set limit QPS to 20
        rule.setCount(20);
        rules.add(rule);
        FlowRuleManager.loadRules(rules);
    }
}
