# Adapt-IQ Backend: Security, Provisioning & Deployment Guide

This document describes the security architecture, environment variable requirements, and administrator provisioning procedures for the Adapt-IQ backend.

---

## 1. Environment Variables Configuration

The production backend strictly requires valid, secure environment variables to start.

| Variable | Required | Description | Example / Recommended Value |
|---|---|---|---|
| `PORT` | Optional | HTTP listener port (defaults to 4000) | `4000` |
| `DATABASE_URL` | **Yes** | PostgreSQL connection URL | `postgresql://<user>:<password>@<host>:5432/<dbname>?schema=public` |
| `JWT_SECRET` | **Yes** | Cryptographically secure secret for JWT signing (min 32 chars in production) | Generate via `openssl rand -base64 32` |
| `SESSION_TOKEN_EXPIRATION` | Optional | Participant token lifespan (defaults to `4h`) | `4h` |
| `NODE_ENV` | **Yes** | Execution mode (`production` or `development`) | `production` |
| `BOOTSTRAP_ADMIN_EMAIL` | Optional | Email for initial admin provisioning on first boot | `<admin-email>` |
| `BOOTSTRAP_ADMIN_PASSWORD` | Optional | Password for initial admin provisioning | `<strong-password-min-8-chars>` |
| `BOOTSTRAP_ADMIN_ROLE` | Optional | Role for initial admin (`RESEARCHER` or `SUPER_ADMIN`) | `RESEARCHER` |

> [!IMPORTANT]
> The backend automatically validates `JWT_SECRET` on startup. If `JWT_SECRET` is missing, empty, matches a known placeholder, or is less than 32 characters in production, the server will terminate immediately with a fatal exit code.

---

## 2. Administrator Provisioning

Hardcoded researcher accounts and passwords have been removed from the server startup flow. Researchers can be provisioned using either of the two explicit methods below:

### Method A: Administrative CLI Command (Recommended)
Run the dedicated provisioning command directly in the backend directory:
```bash
npm run admin:create -- --email researcher@domain.com --password "YourStrongPasswordHere" --role RESEARCHER
```
Options:
* `--email <email>`: Valid email address for the researcher.
* `--password <password>`: Minimum 8 characters. Passwords are securely hashed using `bcrypt` (12 rounds) before storage.
* `--role <role>`: Optional. Either `RESEARCHER` (default) or `SUPER_ADMIN`.

Alternatively, environment variables can be passed to the command:
```bash
ADMIN_EMAIL=researcher@domain.com ADMIN_PASSWORD="YourStrongPasswordHere" npm run admin:create
```

### Method B: Environment Variable Bootstrap on First Run
For automated container deployments, you may specify:
```bash
BOOTSTRAP_ADMIN_EMAIL="researcher@domain.com"
BOOTSTRAP_ADMIN_PASSWORD="YourStrongPasswordHere"
BOOTSTRAP_ADMIN_ROLE="RESEARCHER"
```
When provided, the server checks if the account already exists. If not, it creates it using `bcrypt` (12 rounds). Once provisioned, these variables should be unset.

---

## 3. Docker Production Deployment

In `docker-compose.prod.yml`:
1. **Isolated Database:** PostgreSQL port 5432 is **not published** to the host. It is only accessible to the backend container over the internal Docker bridge network (`adaptiq_internal`).
2. **Mandatory Credentials:** Fallback credentials have been removed. You must specify `POSTGRES_USER`, `POSTGRES_PASSWORD`, and `JWT_SECRET` in a production `.env` file before launching:

```bash
# Example deployment command
docker compose -f docker-compose.prod.yml --env-file .env.production up -d
```

---

## 4. Credential Rotation Advisory

> [!WARNING]
> Because previously used researcher email addresses and passwords were once committed to Git history, simply removing them from the current working tree files does **not** erase them from prior commits.
> 
> **Immediate Action Required:**
> 1. Rotate all researcher passwords in the database using `npm run admin:create`.
> 2. Do not reuse any previously committed credentials.
> 3. If a Git history rewrite is desired to purge historical commit text, it must be coordinated and explicitly authorized by repository owners.
