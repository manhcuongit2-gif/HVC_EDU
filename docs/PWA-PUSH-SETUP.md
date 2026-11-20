# EduCenter PWA and Web Push setup

## Database

On startup, Flyway applies the new migrations after `V20261124`:

- `V20261125`: push subscriptions, notification preferences, notification center, outbox/delivery tracking, homework, class chat links and mentions.
- `V20261126`: admin broadcasts and scheduled broadcasts.
- `V20261127`: important chat message flag.
- `V20261128`: notification sound preference copied into the push outbox.

Back up the database before the first production startup as usual. Hibernate schema updates remain disabled.

## Configure VAPID keys

Generate a VAPID key pair once and keep the private key in the deployment secret store. For example, with the `web-push` CLI:

```powershell
npx web-push generate-vapid-keys --json
$env:APP_PUSH_ENABLED = 'true'
$env:APP_PUSH_VAPID_PUBLIC_KEY = '<public key from generator>'
$env:APP_PUSH_VAPID_PRIVATE_KEY = '<private key from generator>'
$env:APP_PUSH_VAPID_SUBJECT = 'mailto:admin@your-domain.example'
mvn spring-boot:run
```

Use the same key pair after restarts. Do not commit either key. If VAPID is not configured, the application remains usable and the push worker leaves queued deliveries untouched. Browser and in-app notifications still appear in the center.

For production, serve the application over HTTPS. Browsers allow the service worker and push APIs on `localhost` for local development. Each user must enable push from **Thông báo → Thiết bị**; the browser permission prompt is only shown after that button is pressed. Device keys are tied to the signed-in account and can be removed from that page.

The PWA caches only its offline fallback and icon. Authenticated pages, APIs, chat, reports and uploaded files are not placed in the service-worker cache. Push previews are generic by default; a user can opt into detailed previews in notification settings.

## Supported application behavior

- The existing `GET /api/notifications` response remains the message-unread summary.
- Business notifications use `GET /api/business-notifications`, with a separate badge.
- Web Push is VAPID based. Each notification is persisted before delivery and retried with backoff; expired subscriptions are disabled.
- Quiet hours delay push delivery. In-app notifications remain visible immediately.
- Chat uses the Spring simple STOMP broker at `/ws`, while the existing HTTP polling remains as a fallback. Class group membership is synchronized from active class enrollment and assigned teachers.
- Parent homework submissions are on behalf of their own child. Late submissions are accepted and marked late.

The simple broker and active-chat suppression are in-memory and intended for a single application instance. A multi-instance production deployment needs a shared STOMP broker and shared presence coordination.

On iPhone/iPad, install the site to the Home Screen before enabling push. Web Push for Home Screen web apps requires iOS/iPadOS 16.4 or later. Desktop and Android browser support depends on the browser and its notification permission.
