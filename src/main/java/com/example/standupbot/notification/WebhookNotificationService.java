package com.example.standupbot.notification;

import com.example.standupbot.exception.SlackDeliveryException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class WebhookNotificationService {

    private final RestClient restClient;

    public WebhookNotificationService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public void send(String webhookUrl, String message) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new SlackDeliveryException("Slack webhook URL is missing");
        }

        if (message == null || message.isBlank()) {
            throw new SlackDeliveryException("Slack message is empty");
        }

        try {
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", message))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new SlackDeliveryException(
                    "Failed to deliver Slack webhook notification", e);
        }
    }
}