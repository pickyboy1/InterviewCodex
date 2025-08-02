package com.pickyboy.interviewcodex.cache;

/**
 * 自动缓存异常
 *
 * @author pickyboy
 */
public class AutoCacheException extends RuntimeException {

    public AutoCacheException() {
    }

    public AutoCacheException(String message) {
        super(message);
    }

    public AutoCacheException(String message, Throwable cause) {
        super(message, cause);
    }

    public AutoCacheException(Throwable cause) {
        super(cause);
    }

    public AutoCacheException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
