package com.example.standupbot.notification;

import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final WebhookNotificationService webhookNotificationService;
    private final BotNotificationService botNotificationService;

    public NotificationService(
            WebhookNotificationService webhookNotificationService,
            BotNotificationService botNotificationService) {
        this.webhookNotificationService = webhookNotificationService;
        this.botNotificationService = botNotificationService;
    }

    public void sendChannelMessage(String webhookUrl, String message) {
        webhookNotificationService.send(webhookUrl, message);
    }

    public void sendPersonalMessage(
            String botToken,
            String slackUserId,
            String message) {

        botNotificationService.send(botToken, slackUserId, message);
    }
}