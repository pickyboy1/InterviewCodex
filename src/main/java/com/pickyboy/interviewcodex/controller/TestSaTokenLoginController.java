package com.pickyboy.interviewcodex.controller;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *  测试Satoken框架
 *  实现用户登录
 */
@RestController
@RequestMapping("/test/user")
public class TestSaTokenLoginController {
    //测试登陆
    @RequestMapping("/doLogin")
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
    @RequestMapping("/isLogin")
    public String isLogin() {
        return "当前会话是否登录：" + StpUtil.isLogin();
    }

}
