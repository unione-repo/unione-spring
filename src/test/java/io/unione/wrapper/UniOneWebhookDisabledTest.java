package io.unione.wrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** unione.webhook.enabled=false: no controller, no security chain, the path is not mapped. */
@SpringBootTest(classes = TestApplication.class, properties = {
    "unione.api-key=test-key",
    "unione.host=http://127.0.0.1:9",
    "unione.webhook.path=/unione/webhook",
    "unione.webhook.enabled=false"
})
@ContextConfiguration(initializers = WithoutSystemEnvironment.class)
class UniOneWebhookDisabledTest {
    @Autowired
    private WebApplicationContext context;

    @Test
    void webhookIsNotRegistered() throws Exception {
        assertThat(context.getBeansOfType(UniOneWebhookController.class)).isEmpty();
        assertThat(context.containsBean("unioneWebhookSecurityFilterChain")).isFalse();
        // the DispatcherServlet alone (no security filters): an unmapped path is 404
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(get("/unione/webhook")).andExpect(status().isNotFound());
    }
}
