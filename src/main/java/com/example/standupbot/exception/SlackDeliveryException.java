package com.example.standupbot.exception;

public class SlackDeliveryException extends StandupBotException {

    public SlackDeliveryException(String message) {
        super("BAD_GATEWAY", message);
    }

    public SlackDeliveryException(String message, Throwable cause) {
        super("BAD_GATEWAY", message, cause);
    }
}
