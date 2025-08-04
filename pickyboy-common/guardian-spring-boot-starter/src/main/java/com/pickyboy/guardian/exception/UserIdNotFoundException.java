package com.pickyboy.guardian.exception;

/**
 * 在无法自动解析用户ID时抛出的特定异常。
 */
public class UserIdNotFoundException extends GuardianException {
    public UserIdNotFoundException(String message) {
        super(message);
    }
}