package com.pickyboy.interviewcodex.satoken;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.pickyboy.interviewcodex.model.entity.User;
import org.springframework.stereotype.Component;

import cn.dev33.satoken.stp.StpInterface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.pickyboy.interviewcodex.constant.UserConstant.USER_LOGIN_STATE;

@Component
public class StpInterfaceImpl implements StpInterface {
    /**
     * 返回一个账号所拥有的权限码集合
     * 比如 [ "user.*", "user.add", "user.update" ]
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType){
        return  new ArrayList<>();
    }

    /**
     *
     * @param loginId
     * @param loginType
     * @return
     * 账号所属角色列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType){
        // 从当前登陆用户信息获取角色
        User user = (User) StpUtil.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);
        return Collections.singletonList(user.getUserRole());

    }
}
