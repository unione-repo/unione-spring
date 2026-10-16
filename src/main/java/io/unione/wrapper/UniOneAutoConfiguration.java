package io.unione.wrapper;

import io.unione.UniOneClient;
import io.unione.client.Configuration;
import java.time.Duration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * UniOne API client: {@link Configuration} and {@link UniOneClient} beans.
 */
@AutoConfiguration
@EnableConfigurationProperties(UniOneProperties.class)
public class UniOneAutoConfiguration {
    /**
     * A property, or the environment variable when the property is empty.
     *
     * @param value value of the property
     * @param environment environment of the application
     * @param variable name of the environment variable
     * @return the value, null when both are empty
     */
    static String setting(String value, Environment environment, String variable) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        String env = environment.getProperty(variable);
        return env == null || env.isBlank() ? null : env.trim();
    }

    /**
     * Connection settings of the API.
     *
     * @param properties {@code unione.*} properties
     * @param environment environment of the application
     * @return the configuration
     */
    @Bean
    @ConditionalOnMissingBean
    public Configuration unioneConfiguration(UniOneProperties properties, Environment environment) {
        String apiKey = setting(properties.getApiKey(), environment, "UNIONE_API_KEY");
        if (apiKey == null) {
            throw new IllegalStateException("UniOne: set the API key (unione.api-key or UNIONE_API_KEY)");
        }
        Configuration config = new Configuration(apiKey).setPlatform("spring.2.1.2");
        String host = setting(properties.getHost(), environment, "UNIONE_HOST");
        if (host != null) {
            config.setBaseUrl(Configuration.baseUrlFor(host));
        }
        String timeout = setting(properties.getTimeout() == null ? null : properties.getTimeout().toString(),
            environment, "UNIONE_TIMEOUT");
        if (timeout != null) {
            config.setTimeout(Duration.ofMillis((long) (Double.parseDouble(timeout) * 1000)));
        }
        return config;
    }

    /**
     * The API client.
     *
     * @param config connection settings
     * @return the client
     */
    @Bean
    @ConditionalOnMissingBean
    public UniOneClient unioneClient(Configuration config) {
        return new UniOneClient(config);
    }
}
