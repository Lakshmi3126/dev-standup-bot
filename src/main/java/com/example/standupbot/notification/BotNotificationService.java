package com.example.standupbot.notification;

import com.example.standupbot.exception.SlackDeliveryException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class BotNotificationService {

    private static final String SLACK_POST_MESSAGE_URL =
            "https://slack.com/api/chat.postMessage";

    private final RestClient restClient;

    public BotNotificationService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public void send(String botToken, String slackUserId, String message) {
        if (botToken == null || botToken.isBlank()) {
            throw new SlackDeliveryException("Slack bot token is missing");
        }

        if (slackUserId == null || slackUserId.isBlank()) {
            throw new SlackDeliveryException("Slack user ID is missing");
        }

        if (message == null || message.isBlank()) {
            throw new SlackDeliveryException("Slack message is empty");
        }

        try {
            String response = restClient.post()
                    .uri(SLACK_POST_MESSAGE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + botToken)
                    .body(Map.of(
                            "channel", slackUserId,
                            "text", message
                    ))
                    .retrieve()
                    .body(String.class);

            if (response == null || !response.contains("\"ok\":true")) {
                throw new SlackDeliveryException(
                        "Slack rejected the bot notification");
            }

        } catch (SlackDeliveryException e) {
            throw e;
        } catch (Exception e) {
            throw new SlackDeliveryException(
                    "Failed to deliver Slack bot notification", e);
        }
    }
}