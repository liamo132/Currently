# Currently Backend

This backend has been switched from SQLite to PostgreSQL with security-focused defaults.

## Environment variables

- `POSTGRES_URL` (required): e.g. `jdbc:postgresql://localhost:5432/currently_db`
- `POSTGRES_USER` (required)
- `POSTGRES_PASSWORD` (required)
- `POSTGRES_SSLMODE` (optional): `disable` for local dev, `verify-full` for production
- `JWT_SECRET` (recommended in all environments, required in production): Base64-encoded 32+ byte key
- `JWT_EXPIRATION_MS` (optional): defaults to `3600000` (1 hour)
- `APP_CORS_ALLOWED_ORIGINS` (optional): comma-separated frontend origin allowlist
- `APP_DATA_ENCRYPTION_KEY` (required): Base64 32-byte key for AES-GCM field encryption
- `APP_DATA_HASH_KEY` (required): Base64 32+ byte key for HMAC lookups (`email_hash`, `username_hash`)
- `RATE_LIMIT_MAX_REQUESTS` (optional): default `30` requests
- `RATE_LIMIT_WINDOW_SECONDS` (optional): default `60` seconds
- `JPA_DDL_AUTO` (optional): `update` (default) or `validate`
- `SPRING_PROFILES_ACTIVE` (optional): `dev` (default) or `prod`

## Start PostgreSQL locally

Create your local `.env` from the template:

```bash
Copy-Item .env.example .env
# Edit .env and set POSTGRES_PASSWORD (and any other values you want)
docker compose up -d
```

Set environment variables (PowerShell) before running the backend if you prefer:

```bash
$env:POSTGRES_URL="jdbc:postgresql://localhost:5432/currently_db"
$env:POSTGRES_USER="currently_user"
$env:POSTGRES_PASSWORD="replace_me_securely"
```

Run with Maven wrapper:

```bash
./mvnw.cmd spring-boot:run
```

The app now auto-loads `.env` using `spring.config.import`, so IntelliJ runs also pick up the same values.
When using `SPRING_PROFILES_ACTIVE=prod`, startup fails if `JWT_SECRET` is missing.

## Production hardening checklist for the new Postgres layer

- Set TLS mode in URL: `sslmode=verify-full`
- Set `POSTGRES_SSLMODE=verify-full`
- Set `spring.jpa.hibernate.ddl-auto=validate`
- Keep DB port bound to localhost unless external access is strictly required
- Use database role least-privilege access
- Enable `pgcrypto` (init script already included) for future field encryption work
- Keep secrets in a manager (not in `.env`, shell history, or source code)

## Current encryption scope

- Passwords and vault PINs: BCrypt hash (one-way)
- Encrypted at rest (AES-GCM via JPA converter): user username/name/email/provider, room labels, user appliance names
- Login/lookup safety: email and username matched by keyed HMAC hash columns (`email_hash`, `username_hash`)

## Security docs

- Error contract for frontend/backend handling: `ERROR_CONTRACT.md`
- Implemented controls and known limitations: `SECURITY_NOTES.md`
