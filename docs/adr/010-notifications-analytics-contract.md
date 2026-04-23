# ADR 010: Notifications Analytics Contract

## Status
Accepted

## Context
The notifications and messaging platform needs durable analytics for delivery health and recipient engagement. Analytics must continue to come from backend outbox events exported to S3, not from Angular UI state, so reporting reflects committed domain behavior and provider outcomes.

Realtime notifications also require websocket traffic to reach the backend through the production edge. CloudFront must route `/ws/*` requests to the ALB without caching while preserving authorization, cookies, query strings, and websocket handshake headers.

## Decision
Notifications emit versioned `notification_*` outbox events with no message body or recipient email address in the analytics payload:

- `notification_delivery_requested` records an application-level decision to notify a recipient.
- `notification_delivery_succeeded` records provider or in-app transport acceptance.
- `notification_delivery_failed` records provider or transport failure details without PII.
- `notification_engagement_recorded` records recipient engagement such as `OPENED`, `CLICKED`, or `READ`.

The dbt contract is:

- `sr_notifications` normalizes all notification events from `sr_events`.
- `fct_notification_delivery` models one delivery lifecycle event per row.
- `fct_notification_engagement` models one recipient engagement event per row.
- `obt_listing_performance` includes listing-scoped notification delivery and engagement counts when a notification payload carries `listingId`.

The infrastructure contract is:

- CloudFront keeps the default web origin behavior for normal application traffic.
- CloudFront routes `/ws/*` to the ALB origin.
- `/ws/*` uses the managed disabled-cache policy.
- `/ws/*` forwards cookies, query strings, authorization, origin, and websocket handshake headers to the ALB.
- Viewer traffic redirects to HTTPS so browser websocket clients use `wss://` in production.

## Consequences
- Delivery and engagement reporting can be built from warehouse facts without reading UI state.
- Notification payloads stay privacy-safe by using user IDs and provider message IDs instead of email addresses or message content.
- Listing performance can show notification reach and engagement alongside reservations, listing engagement, and trust metrics.
- Environments adopting the Terraform module must pass the web origin and ALB DNS names explicitly, because this branch does not yet include a Terraform root stack.

## Related
- `packages/event-schemas/notification_delivery_requested.avsc`
- `packages/event-schemas/notification_delivery_succeeded.avsc`
- `packages/event-schemas/notification_delivery_failed.avsc`
- `packages/event-schemas/notification_engagement_recorded.avsc`
- `apps/analytics-dbt/models/silver/sr_notifications.sql`
- `apps/analytics-dbt/models/gold/fct_notification_delivery.sql`
- `apps/analytics-dbt/models/gold/fct_notification_engagement.sql`
- `infra/terraform/modules/cloudfront-alb-routing`
