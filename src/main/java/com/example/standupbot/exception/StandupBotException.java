package com.example.standupbot.exception;

public abstract class StandupBotException extends RuntimeException {

    private final String errorCode;

    protected StandupBotException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected StandupBotException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
