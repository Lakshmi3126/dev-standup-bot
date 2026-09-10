package com.example.standupbot.notification;

/**
 * Channel blocker alert input. Consecutive-day count is supplied by BlockerService later.
 */
public record BlockerAlertContent(
        String teamName, String memberName, String description, int consecutiveDays) {
}
