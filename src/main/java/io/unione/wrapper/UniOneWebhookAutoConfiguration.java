package io.unione.wrapper;

import io.unione.client.Configuration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Webhook endpoint of a servlet web application, see {@link UniOneWebhookController}.
 */
@AutoConfiguration(after = UniOneAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
@ConditionalOnProperty(prefix = "unione", name = "webhook.enabled", havingValue = "true", matchIfMissing = true)
public class UniOneWebhookAutoConfiguration {
    /**
     * The webhook controller.
     *
     * @param properties {@code unione.*} properties
     * @param environment environment of the application
     * @param config connection settings (API key)
     * @param publisher publisher of the webhook events
     * @return the controller
     */
    @Bean
    @ConditionalOnMissingBean
    public UniOneWebhookController unioneWebhookController(UniOneProperties properties,
            Environment environment, Configuration config, ApplicationEventPublisher publisher) {
        String key = UniOneAutoConfiguration.setting(properties.getWebhook().getKey(), environment,
            "UNIONE_WEBHOOK_KEY");
        return new UniOneWebhookController(key == null ? config.getApiKey() : key, publisher);
    }
}
