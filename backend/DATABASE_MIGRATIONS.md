# Adapt-IQ Database Migrations & Operations Guide

This guide documents the procedures for version-controlled database migrations, schema verification, backups, and emergency rollbacks for the Adapt-IQ research platform.

---

## 1. Migration Architecture

Adapt-IQ uses **Prisma Migrate** for deterministic, version-controlled database schema management:
- **Baseline Migration:** `prisma/migrations/0_init/migration.sql` encapsulates the complete schema baseline (7 tables: `admin_users`, `participants`, `sessions`, `action_events`, `sensor_samples`, `upload_batches`, `audit_logs`).
- **Production Execution:** In containerized production, `prisma migrate deploy` executes automatically on startup before Express begins listening. `prisma db push` is strictly prohibited in production.

---

## 2. Backup Procedures (Pre-Migration Safety)

Always take a full transactional database dump before applying any schema modification:

### PostgreSQL pg_dump (Container / Host)
```bash
# 1. From the host (if port is accessible):
pg_dump -h localhost -U postgres -d adaptiq_research -F c -b -v -f "./backup_$(date +%Y%m%d_%H%M%S).dump"

# 2. Or directly inside the running Docker container:
docker exec -t adaptiq_postgres_prod pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -F c > "./backup_$(date +%Y%m%d_%H%M%S).dump"

# 3. Plain SQL format backup:
docker exec -t adaptiq_postgres_prod pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" > "./backup_$(date +%Y%m%d_%H%M%S).sql"
```

---

## 3. Schema Verification & Pre-flight Checks

Before applying new migrations or marking migrations as resolved, verify whether the live database matches the Prisma datamodel:

### Check Schema Drift
```bash
# Compare live database against Prisma schema (expect "No difference detected"):
npx prisma migrate diff \
  --from-schema-datasource ./prisma/schema.prisma \
  --to-schema-datamodel ./prisma/schema.prisma
```

### Inspect Migration Status
```bash
npm run db:status
# Or:
npx prisma migrate status
```

---

## 4. Baselining an Existing Database

If deploying to an existing database created before migrations were introduced:
1. Ensure the database matches the baseline schema:
   ```bash
   npx prisma migrate diff --from-schema-datasource ./prisma/schema.prisma --to-schema-datamodel ./prisma/schema.prisma
   ```
2. Mark the baseline migration as already applied without executing `CREATE TABLE` statements (preventing "relation already exists" errors):
   ```bash
   npx prisma migrate resolve --applied 0_init
   ```
3. Confirm status:
   ```bash
   npx prisma migrate status
   ```

---

## 5. Production Migration Deployment

To deploy pending migrations to production without resetting data:
```bash
npm run db:migrate
# Under the hood:
npx prisma migrate deploy
```
> [!NOTE]
> `prisma migrate deploy` will never prompt for input, will never drop tables, and exits with a non-zero code if a migration fails.

---

## 6. Rollback & Disaster Recovery Procedures

Prisma migrations are forward-only (`migration.sql`). If a migration fails or must be reverted:

### Scenario A: Reverting Before Release (Down-migration)
1. Generate the reversal SQL by diffing the schema:
   ```bash
   npx prisma migrate diff \
     --from-schema-datamodel prisma/schema.prisma \
     --to-schema-datasource prisma/schema.prisma \
     --script > rollback.sql
   ```
2. Review `rollback.sql` carefully to ensure no research data columns are dropped unintentionally.
3. Apply `rollback.sql`:
   ```bash
   psql -h <host> -U <user> -d <dbname> -f rollback.sql
   ```
4. Mark the problematic migration as rolled back in Prisma's `_prisma_migrations` table:
   ```bash
   npx prisma migrate resolve --rolled-back <migration_folder_name>
   ```

### Scenario B: Restoring Full Backup from pg_dump
If an irrecoverable issue occurs:
```bash
# 1. Stop backend service to prevent write conflicts:
docker compose -f docker-compose.prod.yml stop backend

# 2. Restore database from custom-format dump:
docker exec -i adaptiq_postgres_prod pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists < "./backup_timestamp.dump"

# 3. Verify data integrity and restart backend:
docker compose -f docker-compose.prod.yml start backend
```
