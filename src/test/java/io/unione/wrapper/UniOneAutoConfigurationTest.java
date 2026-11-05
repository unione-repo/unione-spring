package io.unione.wrapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.unione.UniOneClient;
import io.unione.client.ApiClient;
import io.unione.client.Configuration;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;

/** The beans of the auto-configuration: the client from the properties or the environment, the mail sender. */
class UniOneAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withInitializer(new WithoutSystemEnvironment())
        .withConfiguration(AutoConfigurations.of(
            UniOneAutoConfiguration.class,
            UniOneMailAutoConfiguration.class));

    @Test
    void clientFromTheProperties() {
        runner.withPropertyValues("unione.api-key=test-key", "unione.host=http://127.0.0.1:9/api",
                "unione.timeout=2.5")
            .run(context -> {
                assertThat(context).hasSingleBean(UniOneClient.class);
                Configuration config = context.getBean(Configuration.class);
                assertThat(config.getApiKey()).isEqualTo("test-key");
                assertThat(config.getBaseUrl()).isEqualTo("http://127.0.0.1:9/api");
                assertThat(config.getTimeout()).isEqualTo(Duration.ofMillis(2500));
                assertThat(config.getPlatform()).isEqualTo("spring.2.3.2," + ApiClient.PLATFORM);
            });
    }

    @Test
    void environmentVariablesWhenThePropertiesAreEmpty() {
        runner.withPropertyValues("UNIONE_API_KEY=env-key", "UNIONE_HOST=http://127.0.0.1:9/env",
                "UNIONE_TIMEOUT=3")
            .run(context -> {
                Configuration config = context.getBean(Configuration.class);
                assertThat(config.getApiKey()).isEqualTo("env-key");
                assertThat(config.getBaseUrl()).isEqualTo("http://127.0.0.1:9/env");
                assertThat(config.getTimeout()).isEqualTo(Duration.ofSeconds(3));
            });
    }

    @Test
    void propertiesWinOverTheEnvironment() {
        runner.withPropertyValues("unione.api-key=property-key", "UNIONE_API_KEY=env-key",
                "unione.host=http://127.0.0.1:9/property", "UNIONE_HOST=http://127.0.0.1:9/env")
            .run(context -> {
                Configuration config = context.getBean(Configuration.class);
                assertThat(config.getApiKey()).isEqualTo("property-key");
                assertThat(config.getBaseUrl()).isEqualTo("http://127.0.0.1:9/property");
            });
    }

    @Test
    void noApiKeyFailsTheStartup() {
        runner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("unione.api-key");
        });
    }

    @Test
    void ownClientIsKept() {
        UniOneClient own = new UniOneClient("own-key");
        runner.withPropertyValues("unione.api-key=test-key")
            .withBean(UniOneClient.class, () -> own)
            .run(context -> assertThat(context.getBean(UniOneClient.class)).isSameAs(own));
    }

    @Test
    void mailSenderIsTheApiOne() {
        runner.withPropertyValues("unione.api-key=test-key")
            .run(context -> assertThat(context.getBean(JavaMailSender.class))
                .isInstanceOf(UniOneMailSender.class));
    }

    @Test
    void mailSenderCanBeDisabled() {
        runner.withPropertyValues("unione.api-key=test-key", "unione.mail.enabled=false")
            .run(context -> {
                assertThat(context).hasSingleBean(UniOneClient.class);
                assertThat(context).doesNotHaveBean(JavaMailSender.class);
            });
    }
}
