package io.unione.wrapper;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.StandardEnvironment;

/**
 * Removes the environment variables of the machine from the context of a test: relaxed binding would map
 * UNIONE_API_KEY of a CI onto unione.api-key, so only the properties of the test count.
 */
public class WithoutSystemEnvironment implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
        context.getEnvironment().getPropertySources()
            .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
    }
}
