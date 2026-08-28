package com.example.standupbot.exception;

public class TeamMemberMismatchException extends StandupBotException {

    public TeamMemberMismatchException(String message) {
        super("BAD_REQUEST", message);
    }
}
