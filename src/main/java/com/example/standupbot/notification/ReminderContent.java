package com.example.standupbot.notification;

/**
 * Personal pre-deadline reminder input. Delivery uses the bot-token path, not this formatter.
 */
public record ReminderContent(String memberName, String deadline) {
}
