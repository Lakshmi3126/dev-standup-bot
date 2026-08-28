package com.example.standupbot.exception;

public class DuplicateSubmissionException extends StandupBotException {

    public DuplicateSubmissionException(String message) {
        super("CONFLICT", message);
    }
}
