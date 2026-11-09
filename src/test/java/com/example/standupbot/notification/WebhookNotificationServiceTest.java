package com.example.standupbot.notification;

import com.example.standupbot.exception.SlackDeliveryException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;


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

    @Test
        void shouldSendWebhookSuccessfully() {

        RestClient.Builder builder = RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        WebhookNotificationService service =
                new WebhookNotificationService(builder);

        server.expect(requestTo("https://example.com/webhook"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(
                        "{\"text\":\"Test message\"}"
                ))
                .andRespond(withSuccess());

        service.send(
                "https://example.com/webhook",
                "Test message"
        );

        server.verify();
        }
        @Test
void shouldThrowExceptionWhenWebhookReturnsServerError() {

    RestClient.Builder builder = RestClient.builder();

    MockRestServiceServer server =
            MockRestServiceServer.bindTo(builder).build();

    WebhookNotificationService service =
            new WebhookNotificationService(builder);

    server.expect(requestTo("https://example.com/webhook"))
            .andRespond(withServerError());

    assertThrows(
            SlackDeliveryException.class,
            () -> service.send(
                    "https://example.com/webhook",
                    "Test message"
            )
    );

    server.verify();
}
}