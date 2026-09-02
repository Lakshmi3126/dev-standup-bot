package com.example.standupbot.exception;

public class ConflictException extends StandupBotException {

    public ConflictException(String message) {
        super("CONFLICT", message);
    }
}