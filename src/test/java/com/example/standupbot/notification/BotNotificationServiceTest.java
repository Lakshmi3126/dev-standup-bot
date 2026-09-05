package com.example.standupbot.notification;

import com.example.standupbot.exception.SlackDeliveryException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BotNotificationServiceTest {

    private static final String SLACK_URL =
            "https://slack.com/api/chat.postMessage";

    @Test
    void shouldRejectMissingBotToken() {
        BotNotificationService service =
                new BotNotificationService(RestClient.builder());

        assertThrows(
                SlackDeliveryException.class,
                () -> service.send("", "U12345", "Test message")
        );
    }

    @Test
    void shouldRejectMissingSlackUserId() {
        BotNotificationService service =
                new BotNotificationService(RestClient.builder());

        assertThrows(
                SlackDeliveryException.class,
                () -> service.send("xoxb-test-token", "", "Test message")
        );
    }

    @Test
    void shouldRejectEmptyMessage() {
        BotNotificationService service =
                new BotNotificationService(RestClient.builder());

        assertThrows(
                SlackDeliveryException.class,
                () -> service.send(
                        "xoxb-test-token",
                        "U12345",
                        ""
                )
        );
    }

    @Test
    void shouldSendPersonalMessageToSlackUser() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        BotNotificationService service =
                new BotNotificationService(builder);

        server.expect(requestTo(SLACK_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(
                        "Authorization",
                        "Bearer xoxb-test-token"
                ))
                .andExpect(header(
                        "Content-Type",
                        MediaType.APPLICATION_JSON_VALUE
                ))
                .andExpect(content().json("""
                        {
                            "channel": "U12345",
                            "text": "Test message"
                        }
                        """))
                .andRespond(withSuccess(
                        "{\"ok\":true}",
                        MediaType.APPLICATION_JSON
                ));

        assertDoesNotThrow(() ->
                service.send(
                        "xoxb-test-token",
                        "U12345",
                        "Test message"
                )
        );

        server.verify();
    }

    @Test
    void shouldThrowWhenSlackRejectsMessage() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        BotNotificationService service =
                new BotNotificationService(builder);

        server.expect(requestTo(SLACK_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"ok\":false,\"error\":\"invalid_auth\"}",
                        MediaType.APPLICATION_JSON
                ));

        assertThrows(
                SlackDeliveryException.class,
                () -> service.send(
                        "xoxb-test-token",
                        "U12345",
                        "Test message"
                )
        );

        server.verify();
    }
}