package com.example.standupbot.exception;

public class ValidationException extends StandupBotException {

    public ValidationException(String message) {
        super("BAD_REQUEST", message);
    }
}
