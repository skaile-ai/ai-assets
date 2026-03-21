# Additional Integration Categories

The PostXL core stack is fixed. These are the categories of additional
integrations a project may need beyond the core platform.

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

## Third-Party Auth Providers
Keycloak supports federation — these integrate via Keycloak config:
- **Social logins** — Google, GitHub, Apple, Microsoft
- **Enterprise SSO** — SAML, OIDC providers
- **Directory services** — LDAP, Active Directory

## Domain-Specific Services
- **AI / ML** — OpenAI, Anthropic, Hugging Face, Replicate
- **Search** — Meilisearch, Typesense, Algolia, Elasticsearch
- **Geolocation** — IP geolocation, reverse geocoding
- **PDF generation** — Puppeteer, wkhtmltopdf, Gotenberg
- **Real-time** — WebSockets (already in NestJS), Server-Sent Events
- **Task queues** — BullMQ (Redis-backed, common with NestJS)

## Consultation Approach

Ask the user one round of questions covering all categories above.
Frame questions around the features already identified (if 2_experience/2_features/
exists). For example:

- "I see a billing feature — do you need a payment gateway like Stripe?"
- "The user profile has an avatar field — do you need file storage?"

If the user has no additional integrations, document "None identified"
and move on. Do not push unnecessary complexity.
