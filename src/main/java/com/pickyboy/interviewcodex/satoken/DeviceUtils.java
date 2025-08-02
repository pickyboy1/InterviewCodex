package com.pickyboy.interviewcodex.satoken;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.pickyboy.interviewcodex.common.ErrorCode;
import com.pickyboy.interviewcodex.exception.ThrowUtils;
import org.apache.http.HttpHeaders;


import javax.servlet.http.HttpServletRequest;

/**
 * 获取用户登录设备
 *
 * @author pickyboy
 */
public class DeviceUtils {
    public static String getRequestDevice(HttpServletRequest request){
        String UA =  request.getHeader(HttpHeaders.USER_AGENT);
        // 使用hutool解析userAgent
        UserAgent userAgent = UserAgentUtil.parse(UA);
        ThrowUtils.throwIf(userAgent == null, ErrorCode.OPERATION_ERROR,"非法请求");
        String device = "pc";
        // 判断设备类型
        // 是否为小程序
        if(isMiniProgram(UA)){
           device = "miniProgram";
        }
        // 是否为pad
        else if(isPad(UA)){
            device = "pad";
        }
        // 是否为手机
        else if (userAgent.isMobile()) {
            device = "mobile";
        }
        return device;
    }

    /**
     * 判断是否为小程序
     * @param UA
     * @return
     */
    private static boolean isMiniProgram(String UA){
        return StrUtil.containsIgnoreCase(UA, "miniProgram")
                && StrUtil.containsIgnoreCase(UA, "MicroMessenger");
    }
    /**
     * 判断是否为平板设备
     */
    private  static boolean isPad(String UA){
        return StrUtil.containsIgnoreCase(UA, "iPad")
                || (StrUtil.containsIgnoreCase(UA, "Android")
                && !StrUtil.containsIgnoreCase(UA, "Mobile"));
    }
}
