package io.unione.wrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import io.unione.client.ApiClient;
import io.unione.client.ApiException;
import io.unione.client.Json;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/** JavaMailSender over email/send: the API is a local HttpServer, never the real one. */
class UniOneMailSenderTest {
    private HttpServer server;
    private final List<String> requests = Collections.synchronizedList(new ArrayList<>());
    private volatile int status = 200;
    private volatile String response = "{\"status\":\"success\",\"job_id\":\"1ZymBc-00041N-9X\",\"emails\":[]}";

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requests.add(exchange.getRequestURI().getPath() + "?" + exchange.getRequestURI().getRawQuery() + " "
                + exchange.getRequestHeaders().getFirst("X-API-KEY") + " " + body);
            byte[] out = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, out.length);
            try (OutputStream stream = exchange.getResponseBody()) {
                stream.write(out);
            }
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
            .withInitializer(new WithoutSystemEnvironment())
            .withConfiguration(AutoConfigurations.of(
                UniOneAutoConfiguration.class,
                UniOneMailAutoConfiguration.class))
            .withPropertyValues("unione.api-key=test-key",
                "unione.host=http://127.0.0.1:" + server.getAddress().getPort());
    }

    /** The "message" of the only email/send request; checks the path, the platform and the API key. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> sentMessage() {
        assertThat(requests).hasSize(1);
        String request = requests.get(0);
        String platform = URLEncoder.encode("spring.2.3.1," + ApiClient.PLATFORM, StandardCharsets.UTF_8);
        assertThat(request).startsWith("/email/send.json?platform=" + platform + " test-key ");
        Map<String, Object> body = Json.asMap(Json.parse(request.substring(request.indexOf(" {") + 1)));
        return (Map<String, Object>) body.get("message");
    }

    @Test
    @SuppressWarnings("unchecked")
    void mimeMessageIsSentThroughTheApi() {
        runner().run(context -> {
            JavaMailSender sender = context.getBean(JavaMailSender.class);
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom("shop@example.com", "Shop");
            helper.setTo(new InternetAddress("user@example.com", "User"));
            helper.setSubject("Order shipped");
            helper.setText("<b>Your order is on the way</b>", true);
            message.setHeader("X-Order", "42");
            sender.send(message);

            Map<String, Object> sent = sentMessage();
            assertThat(sent.get("from_email")).isEqualTo("shop@example.com");
            assertThat(sent.get("from_name")).isEqualTo("Shop");
            assertThat(sent.get("subject")).isEqualTo("Order shipped");
            Map<String, Object> recipient = (Map<String, Object>) ((List<Object>) sent.get("recipients")).get(0);
            assertThat(recipient.get("email")).isEqualTo("user@example.com");
            assertThat((Map<String, Object>) recipient.get("substitutions")).containsEntry("to_name", "User");
            assertThat((Map<String, Object>) sent.get("body")).containsEntry("html", "<b>Your order is on the way</b>");
            assertThat((Map<String, Object>) sent.get("headers")).containsEntry("X-Order", "42");
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void simpleMessageIsSentAsPlainText() {
        runner().run(context -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("shop@example.com");
            message.setTo("user@example.com");
            message.setSubject("Hello");
            message.setText("Plain text");
            context.getBean(JavaMailSender.class).send(message);

            Map<String, Object> sent = sentMessage();
            assertThat(sent.get("subject")).isEqualTo("Hello");
            assertThat((Map<String, Object>) sent.get("body")).containsEntry("plaintext", "Plain text");
        });
    }

    @Test
    void apiErrorIsAMailSendExceptionCausedByTheSdkError() {
        status = 400;
        response = "{\"status\":\"error\",\"message\":\"Unknown recipient\",\"code\":204}";
        runner().run(context -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("shop@example.com");
            message.setTo("user@example.com");
            message.setSubject("Hello");
            message.setText("Plain text");
            assertThatThrownBy(() -> context.getBean(JavaMailSender.class).send(message))
                .isInstanceOf(MailSendException.class)
                .hasMessageContaining("(API error 204, HTTP 400)")
                .cause().isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(204);
                    assertThat(e.isRetryable()).isFalse();
                });
        });
    }
}
