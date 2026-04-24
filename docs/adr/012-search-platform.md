# ADR-012: Search Platform Architecture

**Status:** Accepted
**Date:** 2026-04-24
**Context:** Smart Search & Discovery Platform mega-ticket

## Decision

Build the search platform using PostgreSQL full-text search with PostGIS for geospatial queries, backed by a materialized view for denormalized search-optimized data. Use Leaflet with marker clusters for the interactive map experience.

## Alternatives Considered

### PostgreSQL Full-Text vs Elasticsearch

**Decision: PostgreSQL full-text search (tsvector/tsquery)**

Rationale:
- PostgreSQL full-text search is sufficient for MVP scale (<10K listings)
- Eliminates operational complexity of running Elasticsearch cluster
- Single database simplifies deployment and backup strategy
- GIN indexes on tsvector provide sub-200ms query performance
- `websearch_to_tsquery` supports natural language queries with boolean operators
- `pg_trgm` extension enables autocomplete via trigram similarity
- Can migrate to Elasticsearch later if scale demands it

### Materialized View vs Denormalized Table

**Decision: Materialized view with trigger-based refresh**

Rationale:
- Materialized view automatically stays in sync with source tables
- `REFRESH MATERIALIZED VIEW CONCURRENTLY` allows reads during refresh
- Trigger-based refresh on listing/location/policy/amenity changes keeps data fresh
- Denormalizes listing, location, category, policy, and amenity data into single queryable row
- PostGIS geography column enables efficient ST_DWithin radius queries
- GIN index on tsvector, GiST index on geog point
- Trigram GIN indexes enable autocomplete suggestions

### Cursor vs Offset Pagination

**Decision: Offset pagination with cursor support**

Rationale:
- Offset pagination is simpler to implement and understand
- Supports arbitrary page jumps (useful for SEO and deep linking)
- Cursor included in response for future migration to cursor-based pagination
- Acceptable for search results where total count is meaningful to users
- Can switch to cursor pagination if deep pagination performance becomes an issue

### Leaflet vs Mapbox

**Decision: Leaflet with markercluster plugin**

Rationale:
- Leaflet is open-source with no usage-based pricing
- Sufficient for our map requirements (markers, clusters, bounds events)
- `leaflet.markercluster` provides performant clustering for 1000+ markers
- Custom HTML markers support price badges
- OpenStreetMap tiles are free
- Mapbox would add cost and vendor lock-in
- Can upgrade to Mapbox GL later if advanced styling is needed

## Architecture

### Database Layer
```
search_listings_materialized (materialized view)
├── listing fields (id, title, description, price, etc.)
├── location fields (city, country, lat, lng)
├── category_name, instant_book, policy fields
├── amenity_ids (JSONB array)
├── cover_url (from listing_photos)
├── search_vector (tsvector for full-text)
└── geog (PostGIS geography point)

search_queries (table)
└── Analytics: query hash, filters, result count, response time

search_clicks (table)
└── Analytics: CTR tracking, result position, device type
```

### Backend Layer
```
SearchController (POST /api/listings/search)
├── SearchService
│   ├── search() -> materialized view query + saved status enrichment
│   ├── getSuggestions() -> pg_trgm similarity
│   ├── recordSearchClick() -> analytics persistence
│   └── recordSearchQueryAsync() -> fire-and-forget analytics
└── Caffeine cache (5 min TTL, invalidated on listing updates)
```

### Frontend Layer
```
SearchResultsComponent (split-view: list + map)
├── SearchMapComponent (Leaflet + markercluster)
├── SearchFiltersComponent (price, bedrooms, amenities, etc.)
├── SearchStateService (URL-synced state, map bounds)
└── SearchApiService (HTTP client, result caching)
```

## Consequences

### Positive
- Single database simplifies operations
- Materialized view provides fast search queries (<200ms p95)
- Full-text search supports natural language queries
- PostGIS enables efficient geospatial radius queries
- Click tracking enables CTR analytics
- Marker clusters handle large result sets on map
- URL-synced state enables bookmarkable searches

### Negative
- Materialized view refresh has latency (trigger-based, not instantaneous)
- PostgreSQL full-text search lacks advanced features like fuzzy matching, synonyms
- Scaling beyond 100K listings may require Elasticsearch migration
- Trigger-based refresh adds write overhead to listing operations

## Migration Strategy
- V16 migration creates materialized view, search_queries, search_clicks tables
- Enables PostGIS and pg_trgm extensions
- Sets up trigger-based refresh on listing/location/policy/amenity changes
- Existing GET /api/listings/search endpoint remains for backward compatibility
- New POST /api/listings/search endpoint uses materialized view
