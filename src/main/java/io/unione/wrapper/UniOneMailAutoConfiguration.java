package io.unione.wrapper;

import io.unione.UniOneClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * {@link UniOneMailSender} as the {@link JavaMailSender} of the application (before the SMTP sender of Spring Boot).
 */
@AutoConfiguration(after = UniOneAutoConfiguration.class,
    beforeName = "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration")
@ConditionalOnClass(name = {"jakarta.mail.internet.MimeMessage", "org.springframework.mail.javamail.JavaMailSender"})
@ConditionalOnProperty(prefix = "unione", name = "mail.enabled", havingValue = "true", matchIfMissing = true)
public class UniOneMailAutoConfiguration {
    /**
     * Mail sender over email/send.
     *
     * @param client the API client
     * @return the sender
     */
    @Bean
    @ConditionalOnMissingBean(MailSender.class)
    public JavaMailSender unioneMailSender(UniOneClient client) {
        return new UniOneMailSender(client.getApiClient());
    }
}
