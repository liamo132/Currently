# Security Notes (Sprint 8)

## Implemented Controls

### Authentication and authorization
- JWT-based stateless authentication protects all non-public endpoints.
- Public endpoints are restricted to `/api/auth/**` and `/api/appliances/**`.
- Unauthorized and forbidden responses are standardized as JSON (`UNAUTHORIZED`, `FORBIDDEN`).
- Integration tests verify protected endpoint enforcement.

### Password and PIN protection
- User passwords and vault PINs are hashed with BCrypt (strength 12).
- Raw password/PIN values are never returned in API responses.

### Input validation
- Bean validation is enforced on auth, room, appliance, insight, energy settings, and vault PIN request payloads.
- Service-layer validation still enforces critical domain rules (trust boundary on backend).
- Validation failures return consistent `VALIDATION_ERROR` payloads with field details.

### Error handling
- Global exception handling standardizes backend error responses:
  - `code`, `message`, optional `details`, `timestamp`, `path`.
- Malformed JSON and domain-level invalid input are explicitly handled.
- Rate limiter now returns the same structured error contract (`RATE_LIMITED`).

### CORS and secret handling
- CORS uses an explicit allowlist (`APP_CORS_ALLOWED_ORIGINS`) instead of wildcard controller annotations.
- JWT secret and data-protection keys are environment-driven; production profile fails on missing JWT secret.
- Encrypted PII + lookup hashes use application keys (`APP_DATA_ENCRYPTION_KEY`, `APP_DATA_HASH_KEY`).

### Abuse resistance
- Rate limiting is enabled for `/api/auth/**` and `/api/vault/**` to reduce brute-force attempts.
- Repeated failed logins and vault PIN attempts now trigger temporary in-memory lockouts.
- Rate limiting trusts `X-Forwarded-For` only when explicitly enabled by config.

### Vault and file protection
- Vault bill files are encrypted at rest before being stored in the database.
- Vault uploads enforce a 5 MB maximum size.
- Vault uploads require both PDF metadata and a real PDF file signature (`%PDF-`).
- Download responses use `no-store` cache headers to reduce sensitive file caching.

### HTTP response hardening
- Security headers now include HSTS, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy`, and a restrictive API CSP.
- Default cache-control headers are enabled for API responses.

### Security monitoring
- Security-sensitive events are written to a dedicated audit log (`security-audit.log`) with rolling retention.
- Auth and vault actions are logged without storing raw passwords or PINs.

## Known Limitations
- JWT revocation/blacklisting is not implemented; tokens remain valid until expiry.
- Rate limiting is in-memory and per-instance; not shared across multiple backend replicas.
- No password breach screening or stronger password policy checks are implemented yet.

