package com.pickyboy.interviewcodex.constant;

public interface RedisConstant {

    /*
    * 用户签到记录Redis key前缀
    * */
    String USER_SIGN_IN_REDIS_KEY_PREFIX = "user:signins";

    static String getUserSignInRedisKey(int year,long userId){
        return String.format("%s:%d:%d",USER_SIGN_IN_REDIS_KEY_PREFIX,year,userId);
    }
}
