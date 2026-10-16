# UniOne Spring Boot 2.1.2

Web API 1.90

Quickstart guide: [Send emails with Java](https://docs.unione.io/en/send-with-java) · Docs for LLMs: [llms.txt](https://docs.unione.io/llms.txt) · [API reference](https://docs.unione.io/en/web-api-ref)

Spring Boot integration of the [UniOne Java SDK](https://github.com/unione-repo/unione-java-sdk) (``): the API client in the application context (auto-configuration), a webhook endpoint with signature check, a JavaMailSender transport that sends through `email/send`.

## Install

```bash
# pom.xml
<dependency>
    <groupId>io.unione</groupId>
    <artifactId>unione-spring-boot-starter</artifactId>
    <version>2.1.2</version>
</dependency>
```

Requirements: Java 17+, Spring Boot 3.

## Get an API key

1. Sign up at [cp.unione.io](https://cp.unione.io/en/user/registration/).
2. Copy your API key from [Settings → Security → API](https://cp.unione.io/en/settings/security/api).
3. Put it in an environment variable:

```bash
export UNIONE_API_KEY="your-api-key"
```

The package reads it from `UNIONE_API_KEY` (see the configuration below).

> **Server-side only.** Use this package in backend code: API routes, server functions, workers.
> Never ship the API key to a browser or a mobile app, and never commit it.

## Configuration

Settings are read from the environment by default:

```yaml
# application.yml (or the UNIONE_* environment variables)
unione:
  api-key: ${UNIONE_API_KEY}
  host: us1.unione.io
  timeout: 30
  webhook:
    path: /unione/webhook
  mail:
    enabled: true
```

| Option | Environment variable | Default | Description |
|---|---|---|---|
| `unione.api-key` | `UNIONE_API_KEY` |  | User or project API key (required). |
| `unione.host` | `UNIONE_HOST` | `api.unione.io` | API host (datacenter: `api.unione.io`, `eu1.unione.io`, `us1.unione.io`) or a full base URL. |
| `unione.timeout` | `UNIONE_TIMEOUT` | `30` | Request timeout, seconds. |
| `unione.webhook.key` | `UNIONE_WEBHOOK_KEY` |  | API key the webhooks are signed with (the key of the user/project of the webhook), if it differs from the API key. |
| `unione.webhook.path` | `UNIONE_WEBHOOK_PATH` | `/unione/webhook` | Path of the webhook endpoint. |

The `UNIONE_*` variables are read when the `unione.*` property is not set. `unione.webhook.enabled=false` turns off the webhook controller, `unione.mail.enabled=false` the mail sender (it is registered when `spring-boot-starter-mail` is on the classpath and there is no other `MailSender` bean).

## Send your first email — no DNS setup

Every account can send test email right away from a **sandbox domain**: pre-verified, DKIM and SPF already set.
It delivers only to addresses you confirm.

The examples use the API client of the package (see the configuration above).

**1. Get your sandbox domain** (`domain/get-sandbox-domain`). The call creates the domain on first use and returns the same one afterwards.

```java
import io.unione.UniOneClient;

@Service
public class MailService {
    private final UniOneClient client;

    public MailService(UniOneClient client) {
        this.client = client;
    }

    public String getSandboxDomain() {
        var response = client.domain().getSandboxDomain();
        return response.getDomain();
    }
}
```

**2. Confirm your inbox** (`confirmed-email/create`). UniOne sends a confirmation email; click the link in it within 48 hours.

```java
import io.unione.UniOneClient;
import io.unione.api.confirmedemail.model.ConfirmedEmailRequest;

@Service
public class MailService {
    private final UniOneClient client;

    public MailService(UniOneClient client) {
        this.client = client;
    }

    public String confirmEmail() {
        var response = client.confirmedEmail().create(new ConfirmedEmailRequest()
            .setEmail("you@example.com"));
        return response.getStatus();
    }
}
```

Check the status with `confirmed-email/list` — your address should show `"status": "confirmed"`.

**3. Send the email** (`email/send`) from any address on the sandbox domain to the confirmed address.

```java
import io.unione.UniOneClient;
import io.unione.api.email.model.EmailSendRequest;
import io.unione.api.email.model.MessageObject;
import io.unione.api.email.model.MessageObjectRecipientsItem;
import io.unione.common.model.BodyObject;
import java.util.List;

@Service
public class MailService {
    private final UniOneClient client;

    public MailService(UniOneClient client) {
        this.client = client;
    }

    public String send() {
        var response = client.email().send(new EmailSendRequest()
            .setMessage(new MessageObject()
                    .setRecipients(List.of(
                            new MessageObjectRecipientsItem()
                                .setEmail("you@example.com")))
                    .setBody(new BodyObject()
                            .setHtml("<p>It works!</p>"))
                    .setSubject("Hello from UniOne")
                    .setFromEmail("noreply@<sandbox-domain>")));
        return response.getJobId();
    }
}
```

### What you get back

```json
{
  "status": "success",
  "job_id": "1ZymBc-00041N-9X",
  "emails": [
    "you@example.com"
  ]
}
```

On error the API returns `status: "error"`, a `message` and a numeric `code`:

```json
{
  "status": "error",
  "code": 102,
  "message": "Error ID:83274F98-C7D5-11F1-B221-6214FF8707B1. Can not decode key wrongkey"
}
```

### Same flow with curl

```bash
BASE=https://api.unione.io/en/transactional/api/v1

curl -s -X POST $BASE/domain/get-sandbox-domain.json \
  -H "Content-Type: application/json" -H "X-API-KEY: $UNIONE_API_KEY" -d '{}'

curl -s -X POST $BASE/confirmed-email/create.json \
  -H "Content-Type: application/json" -H "X-API-KEY: $UNIONE_API_KEY" \
  -d '{"email": "you@example.com"}'

curl -s -X POST $BASE/email/send.json \
  -H "Content-Type: application/json" -H "X-API-KEY: $UNIONE_API_KEY" \
  -d '{
        "message": {
          "recipients": [
            {
              "email": "you@example.com"
            }
          ],
          "from_email": "noreply@<sandbox-domain>",
          "subject": "Hello from UniOne",
          "body": {
            "html": "<p>It works!</p>"
          }
        }
      }'
```

## Sending mail

Messages of the JavaMailSender go through [email/send](https://docs.unione.io/en/web-api-ref#email-send): from, to/cc/bcc, reply-to, subject, HTML and text parts, attachments, inline images and `X-*` headers.

```java
@Autowired
private JavaMailSender mailSender;

MimeMessage message = mailSender.createMimeMessage();
MimeMessageHelper helper = new MimeMessageHelper(message, true);
helper.setFrom("shop@example.com", "Shop");
helper.setTo("user@example.com");
helper.setSubject("Order shipped");
helper.setText("<b>Your order is on the way</b>", true);
message.setHeader("X-Order", "42");
mailSender.send(message);
```

## Webhooks

The endpoint answers `GET` with 200 OK (the URL check of [webhook/set](https://docs.unione.io/en/web-api-ref#webhook-set)) and accepts the `POST` notifications: it checks the `auth` field (MD5 of the request body in which the `auth` value is replaced by the API key), answers 401 to a wrong signature, and passes the notification and each of its events to the application.

```java
import io.unione.wrapper.WebhookEventReceivedEvent;

@Component
public class DeliveryListener {
    @EventListener
    public void onEvent(WebhookEventReceivedEvent e) {
        var data = e.getEvent().getEventData();
        log.info("UniOne event {}: {} {}", e.getEvent().getEventName(), data.getEmail(), data.getStatus());
    }
}
```

Register the URL of the endpoint with `webhook/set`, e.g. `https://example.com/unione/webhook`.

## API client

Every section of the API is available on the client, see the [SDK docs](https://github.com/unione-repo/unione-java-sdk).

```java
import io.unione.UniOneClient;

@Service
public class UsageService {
    private final UniOneClient client;

    public UsageService(UniOneClient client) {
        this.client = client;
    }

    public Integer emailsSent() {
        var accounting = client.system().info().getAccounting();
        return accounting == null ? null : accounting.getEmailsSent();
    }
}
```

## Sending through the API

Besides the JavaMailSender, mail can be sent through the API client directly. The client calls [email/send](https://docs.unione.io/en/web-api-ref#email-send) with all of its options: templates, substitutions, metadata, tracking. Fields of the request and other examples: [Java SDK docs](https://github.com/unione-repo/unione-java-sdk/blob/HEAD/docs/Email.md#send).

```java
import io.unione.UniOneClient;
import io.unione.api.email.model.EmailSendRequest;
import io.unione.api.email.model.MessageObject;
import io.unione.api.email.model.MessageObjectRecipientsItem;
import io.unione.common.model.BodyObject;
import java.util.List;
import java.util.Map;

@Service
public class MailService {
    private final UniOneClient client;

    public MailService(UniOneClient client) {
        this.client = client;
    }

    public String send() {
        var response = client.email().send(new EmailSendRequest()
            .setMessage(new MessageObject()
                    .setRecipients(List.of(
                            new MessageObjectRecipientsItem()
                                .setEmail("user@example.com")
                                .setSubstitutions(Map.of(
                                        "CustomerId", "12452",
                                        "to_name", "John Smith"))))
                    .setBody(new BodyObject()
                            .setHtml("<b>Hello, {{to_name}}</b>")
                            .setPlaintext("Hello, {{to_name}}"))
                    .setSubject("UniOne test email ")
                    .setFromEmail("email@example.com")
                    .setFromName("John Smith")));
        return response.getJobId();
    }
}
```

## Sandbox limits

- Sends only to confirmed addresses: up to 10 per account.
- Up to 100 emails a day.
- The sandbox domain expires after 31 days; call `domain/get-sandbox-domain` again to get a new one.
- Works only with the account API key, not a project key.
- Not listed in `domain/list`; the other `domain/*` methods do not apply to it.

The sandbox is for development and testing. Do not use a sandbox domain as the sender in production.

## Before production: verify your domain

1. `domain/get-dns-records` — returns the values for your DNS records.
2. Add these records at your DNS provider (the domain page of the control panel shows them too):

   | Type | Host | Value |
   |---|---|---|
   | TXT | `@` | the `verification-record` value (`unione-validate-hash=…`) |
   | TXT | `<selector>._domainkey` (the selector is on the domain page of the control panel, e.g. `us`) | `v=DKIM1; k=rsa; p=<dkim>` |
   | TXT | `@` | `v=spf1 include:spf.unione.io ~all` |
   | CNAME | `_dmarc` | the `dmarc` value |

3. `domain/validate-verification-record`, then `domain/validate-dkim`.
4. `domain/list` — wait until the verification record is confirmed and DKIM is active.
5. Read `from_email` from your config, so switching from the sandbox needs no code change.

## Sending reliably

- **Retries:** set `message.idempotence_key` on sends that may be retried, so a repeated request does not send twice.
- **Limits:** up to 500 recipients and 10 MB per request; each attachment up to 7 MB.
- **Body:** always include `body.html` or `body.plaintext`.
- **Delivery status:** register a webhook (`webhook/set`) with a fast, idempotent HTTPS handler instead of polling.
- **Defaults:** keep the default unsubscribe footer and tracking; changing them requires UniOne support.
- **Errors:** retry `429` and `5xx` with backoff; do not retry other errors unchanged.

## Links

- [Send emails with Java](https://docs.unione.io/en/send-with-java) and other [integration guides](https://docs.unione.io/en/integrations/)
- [Web API reference](https://docs.unione.io/en/web-api-ref)
- [llms.txt](https://docs.unione.io/llms.txt) for AI coding agents
- [UniOne MCP server and agent setup](https://unione.io/en/agents)
- [Support](https://cp.unione.io/en/support)

## For package contributors

See [AGENTS.md](AGENTS.md): how this package is generated from the OpenAPI spec, built, tested and released.
