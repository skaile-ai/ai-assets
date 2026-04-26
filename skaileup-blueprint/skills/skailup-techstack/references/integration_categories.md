# Additional Integration Categories

These are the categories of additional integrations a project may need beyond
its core stack. Use this reference during the integration consultation step.

## External APIs

- **Payment gateways** — Stripe, Mollie, PayPal, Adyen
- **Email services** — SendGrid, Resend, Postmark, AWS SES
- **SMS / messaging** — Twilio, MessageBird, Vonage
- **Maps / geocoding** — Google Maps, Mapbox, OpenStreetMap

## File Storage

- **S3-compatible** — AWS S3, MinIO (self-hosted), DigitalOcean Spaces
- **Local filesystem** — for development or simple deployments
- **Cloud providers** — Azure Blob Storage, Google Cloud Storage

## Analytics & Monitoring

- **Product analytics** — PostHog, Mixpanel, Amplitude
- **Error tracking** — Sentry, Bugsnag
- **Infrastructure monitoring** — Grafana, Prometheus
- **Logging** — Loki, ELK stack

## Auth Providers

- **Social logins** — Google, GitHub, Apple, Microsoft
- **Enterprise SSO** — SAML, OIDC providers
- **Directory services** — LDAP, Active Directory

## Domain-Specific Services

- **AI / ML** — OpenAI, Anthropic, Hugging Face, Replicate
- **Search** — Meilisearch, Typesense, Algolia, Elasticsearch
- **Geolocation** — IP geolocation, reverse geocoding
- **PDF generation** — Puppeteer, wkhtmltopdf, Gotenberg
- **Real-time** — WebSockets, Server-Sent Events
- **Task queues** — BullMQ, Redis-backed job queues

## Consultation Approach

Ask the user one round of questions covering all categories above.
Frame questions around the features already identified (if `_concept/experience/features/`
exists). For example:

- "I see a billing feature — do you need a payment gateway like Stripe?"
- "The user profile has an avatar field — do you need file storage?"
- "You have a notification feature — email (SendGrid) or SMS (Twilio)?"

If the user has no additional integrations, document "None identified"
and move on. Do not push unnecessary complexity.
