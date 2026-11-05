package io.unione.wrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

/** Application of the web tests: the auto-configuration of the starter (and of Spring Security, on the classpath). */
@SpringBootConfiguration
@EnableAutoConfiguration
public class TestApplication {
    /** The webhook events published by the controller. */
    public static class WebhookEvents implements ApplicationListener<ApplicationEvent> {
        public final List<ApplicationEvent> received = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void onApplicationEvent(ApplicationEvent event) {
            if (event instanceof WebhookReceivedEvent || event instanceof WebhookEventReceivedEvent) {
                received.add(event);
            }
        }
    }

    @Bean
    WebhookEvents webhookEvents() {
        return new WebhookEvents();
    }
}
