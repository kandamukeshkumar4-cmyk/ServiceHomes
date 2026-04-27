# ADR-014: Host Calendar and Seasonal Pricing

**Status:** Accepted
**Date:** 2026-04-27
**Context:** Host Calendar & Seasonal Pricing mega-ticket

## Decision

Build a host-facing calendar and pricing management system that layers on top of the existing availability rules. Introduce four new pricing concepts: seasonal pricing templates (date-range multipliers), length-of-stay discounts (tiered percentage discounts), weekend multipliers (day-of-week premiums), and turnover days (buffer days after confirmed reservations).

## Architecture

### Pricing Rule Stack (precedence, highest to lowest)
1. **Per-date price override** (`ListingAvailabilityRule.PRICE_OVERRIDE`) — absolute nightly price
2. **Weekend multiplier** — applied to base price after seasonal
3. **Seasonal pricing template** — date-range multiplier on base price
4. **Base nightly price** (`Listing.nightlyPrice`)

Length-of-stay discounts are applied to the subtotal after per-night pricing is computed, not to individual nights.

### Domain Model
```
SeasonalPricingTemplate
├── listing_id (FK)
├── name
├── start_date, end_date
├── multiplier (> 0)

LengthOfStayDiscount
├── listing_id (FK)
├── min_nights (> 0)
├── discount_percent (0-100)

WeekendMultiplier
├── listing_id (FK, unique)
├── friday_multiplier, saturday_multiplier, sunday_multiplier (> 0)

TurnoverDay
├── listing_id (FK, unique)
├── buffer_days (>= 0)
```

### Calendar Day Generation
`HostCalendarService.getCalendar()` generates a day-by-day view for a date range by:
1. Loading availability rules, seasonal templates, weekend multipliers, and turnover settings
2. Computing blocked dates from `BLOCKED_DATE` rules
3. Computing turnover dates by querying confirmed reservations and adding buffer days after each checkout
4. For each day, applying the pricing stack: base -> seasonal -> weekend, with price overrides taking precedence

### Integration with Reservations
`AvailabilityService.evaluateStay()` now queries `HostCalendarService.findApplicableLengthOfStayDiscount()` and applies the maximum applicable tier to the stay subtotal. This ensures guests see discounted pricing during booking while hosts manage discounts in the calendar UI.

### API Surface
All endpoints are under `/api/host/listings/{listingId}/calendar`:
- `GET /` — calendar day view
- `GET|POST|PUT|DELETE /seasonal-templates` — CRUD seasonal templates
- `GET|POST|PUT|DELETE /length-of-stay-discounts` — CRUD LOS discounts
- `GET|PUT /weekend-multiplier` — upsert weekend multiplier
- `GET|PUT /turnover-day` — upsert turnover settings

## Consequences

### Positive
- Hosts can optimize revenue with seasonal and weekend pricing
- Length-of-stay discounts incentivize longer bookings
- Turnover days protect hosts from back-to-back bookings without prep time
- Pricing stack is deterministic and well-documented
- Existing availability rules remain untouched; new features are additive

### Negative
- Four new tables increase schema complexity
- Calendar generation requires multiple queries; caching may be needed at scale
- Length-of-stay discounts are computed at evaluation time, not stored per reservation
- Seasonal templates cannot overlap; hosts must manage contiguous date ranges

## Migration Strategy
- V19 migration creates four new tables with indexes and check constraints
- No data migration required; new features are opt-in per listing
- Existing `/listings/{id}/calendar` endpoint remains for basic availability view
- New `/host/listings/{id}/calendar` endpoint provides enriched pricing view
