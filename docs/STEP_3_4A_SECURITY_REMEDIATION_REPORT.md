# Adapt-IQ Step 3.4A — Controlled Security Remediation Report

**Date:** October 20, 2026  
**Role:** Senior Backend Security Engineer and DevOps Reviewer  
**Context:** Continuation from the Step 3.3 Independent Security Audit  
**Workspace:** `C:\Users\M. Devika Rani\StudioProjects\Adapt-IQ`  
**Execution Mode:** Controlled, minimal, evidence-based local remediation. Zero cloud deployment, zero database mutations, zero Git commits.

---

## 1. Safety Baseline and Git Status Verification

### 1.1 Git Status & Commit State
- **Current Branch:** `main`
- **HEAD Commit:** `d47b6021969a582ec222fb2ef8e00684094f24bc` (`feat(deploy): implement Step 2 Prisma migration baseline and Android production URL configuration`)
- **Working Tree State (`git status --short`):**
  ```text
   M admin/build.gradle.kts
   M admin/src/main/java/com/example/adaptiq/admin/auth/AdminAuthManager.kt
   M admin/src/main/java/com/example/adaptiq/admin/sync/AdminApiService.kt
   M admin/src/main/java/com/example/adaptiq/admin/ui/screens/AdminLoginScreen.kt
   M app/build.gradle.kts
   M app/src/main/java/com/example/adaptiq/sync/AdaptIqApiService.kt
   M backend/package-lock.json
   M backend/package.json
   M backend/src/modules/auth/auth.controller.ts
   M backend/src/server.ts
   M backend/tests/security.test.ts
   M docker-compose.prod.yml
  ?? backend/src/config/cors.ts
  ?? backend/src/middleware/rate-limiter.middleware.ts
  ?? docs/
  ```

### 1.2 Android Step 2.2 Preservation Confirmation
Confirmed that all six Android changes from Step 2.2 remain completely untouched:
- `app/build.gradle.kts`
- `app/src/main/java/com/example/adaptiq/sync/AdaptIqApiService.kt`
- `admin/build.gradle.kts`
- `admin/src/main/java/com/example/adaptiq/admin/auth/AdminAuthManager.kt`
- `admin/src/main/java/com/example/adaptiq/admin/sync/AdminApiService.kt`
- `admin/src/main/java/com/example/adaptiq/admin/ui/screens/AdminLoginScreen.kt`

Verified via `git diff --stat app/ admin/`:
```text
 admin/build.gradle.kts                             | 48 +++++++++++-
 .../example/adaptiq/admin/auth/AdminAuthManager.kt | 17 ++--
 .../example/adaptiq/admin/sync/AdminApiService.kt  | 31 +++++++-
 .../adaptiq/admin/ui/screens/AdminLoginScreen.kt   | 90 ++++++++++++----------
 app/build.gradle.kts                               | 48 +++++++++++-
 .../com/example/adaptiq/sync/AdaptIqApiService.kt  | 31 +++++++-
 6 files changed, 205 insertions(+), 60 deletions(-)
```

---

## 2. Review and Remediation of Proxy Trust

### 2.1 Intended Deployment Topology
The verified production and staging architecture consists of:
1. **Backend Service:** Runs in a Docker container publishing its port strictly to the host loopback interface: `127.0.0.1:4000:4000` (defined in `docker-compose.prod.yml:39`).
2. **Reverse Proxy (Caddy / Nginx):** Runs directly on the host VM, binding to public ports 80/443 for automatic TLS termination, and reverse-proxying traffic to `http://127.0.0.1:4000`.
3. **Database (PostgreSQL):** Isolated in Docker bridge network `adaptiq_internal` with zero host port publishing.

### 2.2 Vulnerability Analysis Prior to Remediation
- In Express, client IP resolution (`req.ip`) defaults to the remote socket address unless proxy trust is configured.
- When fronted by a reverse proxy on `127.0.0.1`, every incoming request socket originates from `127.0.0.1`.
- Without proxy trust:
  - All researchers and external users share the single IP rate-limiting bucket (`127.0.0.1`).
  - An attacker failing 5 login attempts would trigger an accidental Denial of Service (DoS) for all legitimate administrators.
- If broad proxy trust (such as `app.set('trust proxy', true)`) were used:
  - Express would trust arbitrary client-supplied `X-Forwarded-For` headers from any network interface, allowing attackers to spoof headers and trivially bypass rate limits.

### 2.3 Applied Remediation
In `backend/src/server.ts` (lines 17–18), configured explicit loopback proxy trust:
```typescript
const app = express();
const PORT = process.env.PORT || 4000;

// Trust loopback reverse proxy (e.g. Caddy or Nginx on host 127.0.0.1)
app.set('trust proxy', 'loopback');

app.use(helmet());
app.use(cors(corsOptions));
app.use(express.json({ limit: '10mb' }));
```

### 2.4 Mechanics and Failure Mode Analysis
- **How it works:** `app.set('trust proxy', 'loopback')` configures Express to trust proxy hops only if the immediate connecting socket IP is in the loopback range (`127.0.0.1/8` or `::1/128`).
- **Header handling:** When Caddy connects via `127.0.0.1`, Express reads the `X-Forwarded-For` header set by Caddy and correctly extracts the true client IP for rate limiting and logging.
- **Topology change failure mode:**
  - If the deployment topology changes such that the reverse proxy runs inside a **separate Docker container** rather than directly on the host, the connecting socket IP will be a Docker internal IP (e.g., `172.18.0.x`), not `127.0.0.1`.
  - In that scenario, `loopback` trust would not match the proxy IP, causing Express to revert to treating the container IP as the client IP.
  - If a containerized reverse proxy is ever adopted, the configuration must be updated to trust the specific Docker subnet (e.g., `app.set('trust proxy', 'uniquelocal')` or explicit IP list).

---

## 3. Verification of Readiness and Liveness Probes

### 3.1 Source Inspection (`backend/src/server.ts:49-66`)
```typescript
// Liveness probe (Express process responsiveness)
app.get('/health', (req: Request, res: Response) => {
    res.json({ status: 'healthy', timestamp: new Date().toISOString() });
});

// Readiness probe (Database connectivity check)
app.get('/health/ready', async (req: Request, res: Response) => {
    try {
        await prisma.$queryRaw`SELECT 1`;
        res.json({ status: 'ready', database: 'connected', timestamp: new Date().toISOString() });
    } catch (e: any) {
        console.error('Database readiness check failed');
        res.status(503).json({ status: 'unavailable', database: 'disconnected', error: 'Database service unavailable' });
    }
});
```

### 3.2 Evaluation Criteria
- **Prisma Syntax Correctness:** `await prisma.$queryRawSELECT 1` is valid Prisma tagged-template SQL syntax, supported across Prisma 5.x.
- **Success Response:** Emits HTTP 200 with `{ status: 'ready', database: 'connected', timestamp: ... }`.
- **Failure Response:** On database disconnect, pool exhaustion, or socket failure, catches the exception and returns HTTP 503 with generic JSON `{ status: 'unavailable', database: 'disconnected', error: 'Database service unavailable' }`.
- **Zero Sensitive Data Leakage:** The error message, stack trace, and `DATABASE_URL` credentials are never exposed to the client. Verified in automated test `security.test.ts:182-199`.
- **Liveness Separation:** `/health` remains an instantaneous, in-memory route that never touches the database, safe for continuous health polling.
- **Status:** **ALREADY CORRECT.** No modifications required.

---

## 4. Review of Admin-Login Rate Limiting

### 4.1 Configuration & Route Attachment
- **Rate Limiter Definition (`backend/src/middleware/rate-limiter.middleware.ts`):**
  - Window: 15 minutes (`windowMs: 15 * 60 * 1000`).
  - Max Attempts: 5 requests per IP (`max: 5`).
  - Standard Headers: RFC-compliant `RateLimit-*` enabled; legacy `X-RateLimit-*` disabled.
  - Status Code: HTTP 429 (`{ error: 'Too many login attempts. Please try again later.' }`).
- **Route Attachment (`backend/src/modules/auth/auth.controller.ts:57`):**
  - Attached exclusively to `POST /api/v1/auth/admin/login`.
- **Participant Route Isolation:**
  - Participant session creation (`POST /api/v1/auth/participant/session/start`) and upload routes (`POST /api/v1/sessions/:sessionId/upload`) do not have rate limiters attached.
  - High-frequency sensor uploads from study participants are completely unthrottled.
- **Storage Strategy:**
  - Uses default `MemoryStore`.
  - For single-instance staging, `MemoryStore` provides zero-overhead, in-process protection without introducing Redis or additional network dependencies.
  - Multi-instance production horizontal scaling will require Redis (`rate-limit-redis`) in a future milestone.
- **Status:** **ALREADY CORRECT.**

---

## 5. Review of CORS and Authorization Architecture

### 5.1 Browser Isolation vs. Client Authentication
- **CORS Scope (`backend/src/config/cors.ts`):**
  - Requests without an `Origin` header (such as native Android OkHttp/Retrofit requests and CLI clients) execute `callback(null, true)`.
  - Web browser requests must match entries in `ALLOWED_ORIGINS` (parsed from comma-separated environment variable).
  - In non-production environments (`NODE_ENV !== 'production'`), localhost origins (`http://localhost:*`) are permitted for developer tooling. In production, localhost regex is bypassed.
- **Critical Security Principle:**
  - CORS is an origin-isolation mechanism enforced by web browsers, **not an authentication boundary**.
  - Omitting the `Origin` header does not grant unauthorized access.
- **Independent Authorization Verification:**
  - All sensitive session registration and telemetry upload routes independently enforce JWT verification via `authenticateToken` and IDOR ownership checks via `verifySessionOwnership` (`backend/src/modules/sessions/sessions.controller.ts:9, 51, 72`).
  - All researcher endpoints (`GET /api/v1/sessions`, `GET /admin/export`, `GET /:sessionId`) strictly require `requireRole('RESEARCHER')` (`sessions.controller.ts:165, 183, 216`).
- **Status:** **ALREADY CORRECT.**

---

## 6. Review of Migration Startup and Recovery Assumptions

### 6.1 Container Startup Migration Analysis (`backend/Dockerfile:49`)
```dockerfile
CMD ["sh", "-c", "npx prisma migrate deploy && node dist/server.js"]
```

### 6.2 Risks of Automatic Startup Migrations
1. **Multi-Replica Race Conditions:** If multiple backend container instances launch simultaneously, concurrent execution of `prisma migrate deploy` can trigger lock contention or race conditions on the `_prisma_migrations` table.
2. **PostgreSQL Partial Failures and Crash Loops:**
   - In PostgreSQL, while DDL statements are transactional, certain schema operations (or mid-script timeouts/crashes) can record a migration as `failed` in `_prisma_migrations`.
   - Once a migration is marked failed, Prisma blocks subsequent `migrate deploy` commands until manually resolved.
   - If the container restarts automatically (`restart: always`), it will repeatedly crash on startup, creating an unrecoverable `CrashLoopBackOff`.
3. **No Automatic Rollback:** Prisma does **not** roll back failed migrations automatically. Recovery requires operator intervention via `npx prisma migrate resolve --rolled-back <migration_name>` or manual SQL repair.
4. **Zero-Downtime Schema Evolution:** Migrations that alter or drop columns will break older container replicas still handling traffic if applied concurrently before deployment draining.

### 6.3 Deployment Model Conclusion
- For **single-instance staging VM**, running `npx prisma migrate deploy` on container startup is acceptable and prevents drift between application code and database schema.
- **Operational Requirement:** The operator must be aware that if a migration fails, the container will stop and requires manual intervention via `prisma migrate resolve`. The deployment model was reviewed statically and is preserved without modification.

---

## 7. Safe Verification & Execution Results

### 7.1 TypeScript Build Verification
- **Command:** `npm run build` (in `backend/`)
- **Exit Code:** `0`
- **Output:** Clean compilation, 0 TypeScript errors.

### 7.2 Non-Destructive In-Memory Test Suite Execution
To verify security properties without violating data preservation rules, tests were categorized by safety:
- **Batch 1 (JWT Secret Validation & Password Hashing):**
  - **Command:** `npx jest tests/security.test.ts -t "refuses to start|accepts strong|password hashing"`
  - **Exit Code:** `0`
  - **Results:** 6 passed, 11 skipped, 0 failed.
    - `refuses to start when JWT_SECRET is missing` (PASS)
    - `refuses to start when JWT_SECRET is empty whitespace` (PASS)
    - `refuses to start when JWT_SECRET is a known insecure placeholder` (PASS)
    - `refuses to start in production if JWT_SECRET is shorter than 32 characters` (PASS)
    - `accepts strong high-entropy secret and trims whitespace` (PASS)
    - `password hashing uses securely salted bcrypt with min 10 rounds` (PASS)

- **Batch 2 (HTTP Security Headers, CORS, Liveness & Mocked 503 Readiness):**
  - **Command:** `npx jest tests/security.test.ts -t "sets expected security headers|allows requests with no Origin|withholds Access-Control|reflects Access-Control|returns 200 healthy on liveness|returns 503 unavailable"`
  - **Exit Code:** `0`
  - **Results:** 6 passed, 11 skipped, 0 failed.
    - `sets expected security headers on HTTP responses` (PASS)
    - `allows requests with no Origin header (native Android apps)` (PASS)
    - `withholds Access-Control-Allow-Origin for untrusted web origins` (PASS)
    - `reflects Access-Control-Allow-Origin for allowed localhost in test environment` (PASS)
    - `returns 200 healthy on liveness probe` (PASS)
    - `returns 503 unavailable when database connectivity fails without leaking credentials` (PASS)

- **Total Safe Tests Run:** **12 passed, 0 failed**.

### 7.3 Tests Skipped and Safety Rationale
The following tests were intentionally skipped to protect existing database records and comply with safety rules:
1. `backend/tests/session.test.ts` (Full suite): Contains `beforeAll` hooks executing `await prisma.sensorSample.deleteMany()`, `await prisma.session.deleteMany()`, etc., which truncates active database tables.
2. `security.test.ts` Admin Account Provisioning block: Executes `prisma.adminUser.upsert` and `deleteMany`.
3. `security.test.ts` Admin Login Rate Limiting test: Calls `POST /participant/session/start` which creates new participant and session records in the live database.
4. `security.test.ts` Readiness 200 check: Executes `SELECT 1` against the live test database.

---

## 8. Summary of Findings & Action Matrix

| Finding / Item | Status | Source Location | Action Taken |
| :--- | :---: | :--- | :--- |
| **Proxy Trust Configuration** | **FIXED** | [`backend/src/server.ts:17-18`](file:///c:/Users/M.%20Devika%20Rani/StudioProjects/Adapt-IQ/backend/src/server.ts#L17-L18) | Added `app.set('trust proxy', 'loopback')` to ensure client IP is resolved correctly behind host-level Caddy reverse proxy. |
| **Readiness Query Syntax** | **ALREADY CORRECT** | [`backend/src/server.ts:55-63`](file:///c:/Users/M.%20Devika%20Rani/StudioProjects/Adapt-IQ/backend/src/server.ts#L55-L63) | Verified `prisma.$queryRawSELECT 1` is valid syntax and returns 503 without credential leakage. |
| **Admin Login Rate Limiting** | **ALREADY CORRECT** | [`backend/src/middleware/rate-limiter.middleware.ts:3-12`](file:///c:/Users/M.%20Devika%20Rani/StudioProjects/Adapt-IQ/backend/src/middleware/rate-limiter.middleware.ts#L3-L12) | 5 attempts/15 min on `POST /admin/login`. Participant routes unthrottled. |
| **CORS Native Mobile Compatibility** | **ALREADY CORRECT** | [`backend/src/config/cors.ts:10-28`](file:///c:/Users/M.%20Devika%20Rani/StudioProjects/Adapt-IQ/backend/src/config/cors.ts#L10-L28) | Missing `Origin` allowed for Android; browser origins restricted; endpoints guarded by JWT. |
| **Container Migration Startup Risks** | **DOCUMENTED** | [`backend/Dockerfile:49`](file:///c:/Users/M.%20Devika%20Rani/StudioProjects/Adapt-IQ/backend/Dockerfile#L49) | Documented crash-loop risks, lack of auto-rollback, and manual recovery procedures. Preserved for single-instance staging. |
| **Android Step 2.2 Preservation** | **VERIFIED INTACT** | `app/` and `admin/` (6 files) | All 6 files remain identical to Step 2.2 baseline. |

---

## 9. Remaining External Prerequisites Prior to Cloud Deployment

Before proceeding with cloud deployment (Step 3.4B / Step 4):
1. **Host-Level Caddy Configuration:** Caddy must be installed on the host with reverse proxy configuration forwarding to `127.0.0.1:4000` and enforcing HTTPS.
2. **Production Secrets:** Operator must generate fresh `JWT_SECRET` (min 32 characters) and `POSTGRES_PASSWORD` outside of Git and inject them into `.env.prod`.
3. **Domain & DNS:** Staging domain name must have DNS A record pointing to the host VM IP.

---

## 10. Recommendation for Next Step

### Recommendation: **PROCEED TO STEP 3.4B (Staging Deployment Verification)**
With `trust proxy` properly aligned for the host-level reverse proxy topology and all other controls verified and intact, the local codebase is hardened and ready. Awaiting user review and authorization before proceeding.
