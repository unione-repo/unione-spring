package io.unione.wrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The webhook endpoint through the filters of the application: Spring Security is on the classpath, so the request
 * also passes the security chain of the starter (no CSRF token, no credentials). The API is never called.
 */
@SpringBootTest(classes = TestApplication.class, properties = {
    "unione.api-key=test-key",
    "unione.host=http://127.0.0.1:9",
    "unione.webhook.path=/unione/webhook"
})
@ContextConfiguration(initializers = WithoutSystemEnvironment.class)
@AutoConfigureMockMvc
class UniOneWebhookTest {
    /** A webhook notification with two events; sign() replaces its auth placeholder. */
    static final String SAMPLE = """
        {
            "auth": "00000000000000000000000000000000",
            "events_by_user": [
                {
                    "user_id": 1,
                    "project_id": "6432890213745872",
                    "events": [
                        {
                            "event_name": "transactional_email_status",
                            "event_data": {
                                "job_id": "1a3Q2V-0000OZ-S0",
                                "email": "user@example.com",
                                "status": "delivered",
                                "event_time": "2015-11-30 15:09:42"
                            }
                        },
                        {
                            "event_name": "transactional_spam_block",
                            "event_data": {
                                "block_time": "2015-11-30 15:09:42",
                                "block_type": "one_smtp",
                                "domain": "example.com",
                                "SMTP_blocks_count": 1,
                                "domain_status": "blocked"
                            }
                        }
                    ]
                }
            ]
        }""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private TestApplication.WebhookEvents events;

    @BeforeEach
    void clear() {
        events.received.clear();
    }

    @Test
    void getAnswers200() throws Exception {
        mvc.perform(get("/unione/webhook")).andExpect(status().isOk());
    }

    @Test
    void signedNotificationIsDispatched() throws Exception {
        mvc.perform(post("/unione/webhook").contentType(MediaType.APPLICATION_JSON)
                .content(WebhookSignature.sign(SAMPLE, "test-key")))
            .andExpect(status().isOk());

        assertThat(events.received).hasSize(3);
        assertThat(events.received.get(0)).isInstanceOf(WebhookReceivedEvent.class);
        assertThat(((WebhookReceivedEvent) events.received.get(0)).getCallback()).isNotNull();
        WebhookEventReceivedEvent first = (WebhookEventReceivedEvent) events.received.get(1);
        assertThat(first.getEvent().getEventData().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void wrongSignatureIs401() throws Exception {
        String body = WebhookSignature.sign(SAMPLE, "test-key").replace("user@", "evil@");
        mvc.perform(post("/unione/webhook").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/unione/webhook").contentType(MediaType.APPLICATION_JSON)
                .content(WebhookSignature.sign(SAMPLE, "other-key")))
            .andExpect(status().isUnauthorized());
        assertThat(events.received).isEmpty();
    }

    @Test
    void securityChainOfTheWebhookIsRegistered() {
        assertThat(context.containsBean("unioneWebhookSecurityFilterChain")).isTrue();
    }
}
