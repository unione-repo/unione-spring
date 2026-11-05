package io.unione.wrapper;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring Security of the webhook endpoint: the API calls it without a session, a CSRF token or credentials (the
 * notifications are checked by their signature), so its path is excluded from CSRF and open to everyone. The chain
 * matches the webhook path only and goes first; the other requests keep the security of the application. To change
 * it, define a {@link SecurityFilterChain} bean named {@code unioneWebhookSecurityFilterChain}.
 */
@AutoConfiguration(after = UniOneWebhookAutoConfiguration.class, afterName = {
    "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration",
    "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = {"org.springframework.web.servlet.DispatcherServlet",
    "org.springframework.security.web.SecurityFilterChain",
    "org.springframework.security.config.annotation.web.builders.HttpSecurity"})
@ConditionalOnProperty(prefix = "unione", name = "webhook.enabled", havingValue = "true", matchIfMissing = true)
public class UniOneWebhookSecurityAutoConfiguration {
    /**
     * The security filter chain of the webhook path: CSRF off, every request permitted.
     *
     * @param http security builder of the chain
     * @param environment environment of the application
     * @return the chain
     * @throws Exception when the chain can not be built
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnMissingBean(name = "unioneWebhookSecurityFilterChain")
    public SecurityFilterChain unioneWebhookSecurityFilterChain(HttpSecurity http, Environment environment)
            throws Exception {
        String path = environment.resolvePlaceholders("${unione.webhook.path:${UNIONE_WEBHOOK_PATH:/unione/webhook}}");
        return http
            .securityMatcher(AntPathRequestMatcher.antMatcher(path))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
            .build();
    }
}
