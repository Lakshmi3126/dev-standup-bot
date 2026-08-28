package com.example.standupbot.exception;

public class InvalidTimezoneException extends StandupBotException {

    public InvalidTimezoneException(String message) {
        super("BAD_REQUEST", message);
    }
}
