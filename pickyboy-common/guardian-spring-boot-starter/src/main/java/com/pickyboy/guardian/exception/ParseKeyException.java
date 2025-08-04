package com.pickyboy.guardian.exception;

public class ParseKeyException extends GuardianException{
    public ParseKeyException(String message) {
        super(message);
    }
    public ParseKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
