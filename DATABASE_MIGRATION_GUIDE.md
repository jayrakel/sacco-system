# Database Migration & Deployment Guide

## Overview
This document explains the database migration strategy for the SACCO System to ensure smooth deployments without schema conflicts.

---

## What Changed?

### Code Changes
The `DepositProduct` entity now allows `created_by` to be `NULL`:
```java
@JoinColumn(name = "created_by", nullable = true)
private Member createdBy;  // Null for system/admin users
```

This change allows ADMIN users (who don't have Member records) to create deposit products.

### Database Schema Change Required
```sql
ALTER TABLE deposit_products ALTER COLUMN created_by DROP NOT NULL;
```

---

## Migration Strategy Implemented

### 1. **Flyway Database Migrations (RECOMMENDED)**

We've configured Flyway to handle database schema changes automatically:

#### Configuration Added to `application.properties`:
```properties
# Flyway Migration
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration

# Changed from 'update' to 'validate' for production safety
spring.jpa.hibernate.ddl-auto=validate
```

#### Migration File Created:
**Location:** `src/main/resources/db/migration/V1__fix_deposit_products_created_by.sql`

```sql
-- Migration to fix deposit_products.created_by to allow NULL for admin users
ALTER TABLE deposit_products ALTER COLUMN created_by DROP NOT NULL;
```

### 2. **How It Works**

When you deploy the system online:

1. **First Time Setup:**
   - Flyway creates a `flyway_schema_history` table in your database
   - The `baseline-on-migrate=true` setting allows Flyway to work with existing databases
   - It marks all existing tables as "baseline" (V0)

2. **Running Migrations:**
   - On application startup, Flyway automatically checks for new migration files
   - It executes `V1__fix_deposit_products_created_by.sql` if not already applied
   - The migration is tracked in `flyway_schema_history` table
   - Future restarts won't re-run the same migration

3. **Production Safety:**
   - Changed `spring.jpa.hibernate.ddl-auto=validate` (was `update`)
   - This means Hibernate will only **validate** the schema, not modify it
   - All schema changes MUST go through Flyway migrations
   - Prevents accidental schema changes in production

---

## Deployment Instructions

### Option A: Using Flyway (AUTOMATED - Recommended)

This is now configured automatically. Just deploy your application normally:

```bash
# 1. Build the application
mvn clean package

# 2. Run the application (Flyway runs automatically on startup)
java -jar target/sacco-system.jar

# Or using Maven
mvn spring-boot:run
```

**What happens:**
- Flyway detects the migration file
- Executes the SQL to make `created_by` nullable
- Records the migration in the database
- Application starts successfully

### Option B: Manual Migration (If Flyway Disabled)

If you need to run the migration manually:

```bash
# Connect to your PostgreSQL database
psql -U your_username -d your_database_name

# Run the migration
ALTER TABLE deposit_products ALTER COLUMN created_by DROP NOT NULL;

# Verify
\d deposit_products
```

---

## For Online Deployment (Heroku, AWS, Azure, etc.)

### Pre-Deployment Checklist

1. ✅ **Flyway is enabled** in `application.properties`
2. ✅ **Migration file exists** at `src/main/resources/db/migration/V1__fix_deposit_products_created_by.sql`
3. ✅ **Hibernate DDL is set to validate** (not update)
4. ✅ **Database credentials** are set via environment variables:
   - `DB_URL`
   - `DB_USERNAME`
   - `DB_PASSWORD`

### Deployment Steps

```bash
# 1. Ensure environment variables are set on your hosting platform
export DB_URL=jdbc:postgresql://your-db-host:5432/sacco_db
export DB_USERNAME=your_db_user
export DB_PASSWORD=your_db_password
export JWT_SECRET=your_secret_key
# ... other environment variables

# 2. Build and deploy
mvn clean package
# Deploy the JAR to your hosting platform

# 3. Start the application
# Flyway will automatically run migrations on first startup
```

### Verifying Migration Success

Check the logs on startup. You should see:

```
INFO  FlywayExecutor : Database: jdbc:postgresql://...
INFO  FlywayExecutor : Successfully validated 1 migration
INFO  FlywayExecutor : Migrating schema "public" to version "1 - fix deposit products created by"
INFO  FlywayExecutor : Successfully applied 1 migration
```

---

## Future Database Changes

### Adding New Migrations

When you need to make future schema changes:

1. Create a new migration file with the next version number:
   - `V2__add_new_column.sql`
   - `V3__create_new_table.sql`
   - etc.

2. Follow the naming convention:
   - `V{version}__{description}.sql`
   - Version must be unique and sequential
   - Use double underscore `__` before description
   - Use underscores in description (no spaces)

3. Example:
   ```sql
   -- V2__add_deposit_limit.sql
   ALTER TABLE deposit_products ADD COLUMN max_deposit_amount DECIMAL(15,2);
   ```

4. Flyway will automatically apply new migrations on next deployment

---

## Rollback Strategy

If you need to undo a migration:

1. Create a new "undo" migration (Flyway Community doesn't support automatic rollback):
   ```sql
   -- V2__rollback_created_by_change.sql
   ALTER TABLE deposit_products ALTER COLUMN created_by SET NOT NULL;
   ```

2. Or manually run the undo SQL:
   ```bash
   psql -U user -d db -c "ALTER TABLE deposit_products ALTER COLUMN created_by SET NOT NULL;"
   ```

---

## Important Notes

### ⚠️ Breaking Changes from Previous Setup

**Before:**
- `spring.jpa.hibernate.ddl-auto=update` (Hibernate auto-modifies schema)
- No migration tracking
- Risk of schema drift between environments

**After:**
- `spring.jpa.hibernate.ddl-auto=validate` (Hibernate only validates)
- Flyway tracks all migrations
- Consistent schema across all environments
- Safer for production deployments

### 🔒 Production Safety

- Always test migrations on a staging database first
- Back up your database before deploying
- Never edit migration files after they've been applied
- Keep migration files in version control (Git)

### 📝 Migration File Rules

1. **Never modify** a migration file after it's been applied
2. **Always create new** migration files for changes
3. **Use sequential version numbers** (V1, V2, V3, ...)
4. **Test locally first** before deploying to production
5. **Commit migration files** to your Git repository

---

## Troubleshooting

### Issue: "Migration checksum mismatch"
**Cause:** Migration file was edited after being applied
**Solution:** 
```sql
DELETE FROM flyway_schema_history WHERE version = '1';
```
Then restart the application.

### Issue: "Schema validation failed"
**Cause:** Database schema doesn't match entity definitions
**Solution:** 
1. Check if all migrations have been applied
2. Compare database schema with entity classes
3. Create a migration to fix the difference

### Issue: Flyway disabled in production
**Solution:** Check that `spring.flyway.enabled=true` in production properties

---

## Summary

✅ **Your system is now deployment-ready!**

When you deploy online:
1. Flyway will automatically detect the migration
2. It will execute the `ALTER TABLE` command
3. The `created_by` column will become nullable
4. Admin users can create deposit products without errors
5. Future deployments will not re-run this migration

**No manual SQL execution needed when deploying!** 🎉

The migration is tracked in your codebase (`src/main/resources/db/migration/V1__fix_deposit_products_created_by.sql`) and will be applied automatically on first deployment.
