package com.example.standupbot.notification;

import com.example.standupbot.exception.SlackDeliveryException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertThrows;

class WebhookNotificationServiceTest {

    @Test
    void shouldRejectMissingWebhookUrl() {
        WebhookNotificationService service =
                new WebhookNotificationService(RestClient.builder());

        assertThrows(
                SlackDeliveryException.class,
                () -> service.send("", "Test message")
        );
    }

    @Test
    void shouldRejectEmptyMessage() {
        WebhookNotificationService service =
                new WebhookNotificationService(RestClient.builder());

        assertThrows(
                SlackDeliveryException.class,
                () -> service.send(
                        "https://example.com/webhook",
                        ""
                )
        );
    }
}