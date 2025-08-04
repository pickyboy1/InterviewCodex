package com.pickyboy.guardian.exception;

/**
 * Guardian异常类
 *
 * @author pickyboy
 */
public class GuardianException extends RuntimeException {
    
    public GuardianException(String message) {
        super(message);
    }
    
    public GuardianException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public GuardianException(Throwable cause) {
        super(cause);
    }
}
