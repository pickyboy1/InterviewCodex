package com.pickyboy.interviewcodex.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.pickyboy.interviewcodex.common.BaseResponse;
import com.pickyboy.interviewcodex.common.ResultUtils;
import com.pickyboy.interviewcodex.service.QuestionService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 *  测试Satoken框架
 *  实现用户登录
 *
 * @author pickyboy
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private QuestionService questionService;

    //测试登陆
    @GetMapping("/doLogin")
    public String doLogin(String username, String password) {
        //这里仅做模拟，真实项目需要从数据库中查询用户信息
        if ("admin".equals(username) && "123456".equals(password)) {
            //登录
            StpUtil.login(10001);
            //返回 token
            return "登录成功：" + StpUtil.getTokenInfo();
        }
        return "登录失败";
    }
    // 查询登录状态
    @GetMapping("/isLogin")
    public String isLogin() {
        return "当前会话是否登录：" + StpUtil.isLogin();
    }

    // 测试Es补全建议
    @GetMapping("/suggest")
    public BaseResponse<List<String>> suggest(String prefix) {
        if (StringUtils.isBlank(prefix)) {
            return ResultUtils.success(Collections.emptyList());
        }
        return ResultUtils.success(questionService.getSuggestions(prefix));
    }
}
