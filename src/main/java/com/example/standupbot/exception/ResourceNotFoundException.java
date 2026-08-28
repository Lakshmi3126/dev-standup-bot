package com.example.standupbot.exception;

public class ResourceNotFoundException extends StandupBotException {

    public ResourceNotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}
