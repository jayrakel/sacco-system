# ⚠️ DATABASE MIGRATION REQUIRED

## Issue Summary
The dictionary refactoring is **CODE-COMPLETE** and **COMPILES SUCCESSFULLY**, but the application cannot start due to **database schema mismatch**.

### What Happened
- ✅ All Java code has been refactored to match the dictionary
- ✅ Code compiles with 0 errors
- ❌ Existing database has NULL values in columns that are now NOT NULL
- ❌ Hibernate cannot auto-migrate the data properly

---

## ✅ Solution Provided

I've created a **Flyway migration script** that handles all data migration:

**File:** `src/main/resources/db/migration/V5__dictionary_refactor_phase_a_b.sql`

This script:
1. Adds new columns as nullable first
2. Migrates data from old columns to new columns
3. Sets columns to NOT NULL after data is migrated
4. Adds unique constraints where needed

---

## 🚀 Next Steps (Choose ONE Option)

### Option A: Run Migration on Existing Database (RECOMMENDED FOR DEV)

```bash
# 1. Stop the application if running

# 2. Run Flyway migration manually
./mvnw flyway:migrate

# 3. Start the application
./mvnw spring-boot:run
```

### Option B: Fresh Database (ONLY FOR DEV - DESTROYS ALL DATA)

```sql
-- Connect to PostgreSQL
psql -U postgres

-- Drop and recreate database
DROP DATABASE IF EXISTS sacco_system;
CREATE DATABASE sacco_system;

-- Exit psql
\q
```

Then start the application - Flyway will create everything from scratch.

### Option C: Production Migration (DO NOT USE YET)

**⚠️ FOR PRODUCTION, YOU NEED:**
1. Full database backup
2. Test migration on staging environment first
3. Review the migration script with DBA
4. Plan rollback strategy
5. Schedule maintenance window

---

## 📋 Migration Script Details

The migration handles these table updates:

| Table | Old Field → New Field | Migration Strategy |
|-------|----------------------|-------------------|
| **beneficiaries** | `full_name` → `first_name` + `last_name` | Split on first space |
| **beneficiaries** | `id_number` → `identity_number` | Direct copy |
| **members** | `id_number` → `national_id` | Direct copy + unique constraint |
| **members** | `registration_date` → `membership_date` | Direct copy |
| **users** | `password` → `password_hash` | Direct copy |
| **users** | - | Add `user_id`, `username`, `user_status` |
| **loan_products** | `name` → `product_name` | Direct copy |
| **loan_products** | - | Generate `product_code` (LP000001, etc.) |
| **savings_products** | `name` → `product_name` | Direct copy |
| **savings_products** | - | Generate `product_code` (SP000001, etc.) |
| **savings_accounts** | `balance` → `balance_amount` | Direct copy |
| **savings_accounts** | `status` → `account_status` | Direct copy |
| **loans** | `status` → `loan_status` | Direct copy |
| **loans** | `loan_balance` → `total_outstanding_amount` | Direct copy |
| **employment_details** | `terms` → `employment_terms` | Direct copy |
| **loan_guarantors** | `guarantee_amount` → `guaranteed_amount` | Direct copy |

All tables also get audit fields:
- `active` (default TRUE)
- `created_at` (default CURRENT_TIMESTAMP)
- `updated_at` (default CURRENT_TIMESTAMP)
- `created_by` (default 'SYSTEM_MIGRATION')
- `updated_by` (default 'SYSTEM_MIGRATION')

---

## ⚠️ Important Notes

### What Changed in Configuration
- **Before:** `spring.jpa.hibernate.ddl-auto=update`
- **After:** `spring.jpa.hibernate.ddl-auto=validate`

This change ensures:
- Flyway manages all schema changes
- Hibernate only validates the schema
- No accidental schema modifications

### Beneficiary Name Split Logic
The migration splits `full_name` into `first_name` and `last_name` using this logic:
- `first_name` = everything before the first space
- `last_name` = everything after the first space (or same as full_name if no space)

**Example:**
- `"John Doe"` → `first_name="John"`, `last_name="Doe"`
- `"John Michael Doe"` → `first_name="John"`, `last_name="Michael Doe"`
- `"Madonna"` → `first_name="Madonna"`, `last_name="Madonna"`

If this logic doesn't match your data, you may need to manually fix some records after migration.

---

## 🧪 Verification After Migration

After running the migration, verify:

```sql
-- 1. Check beneficiaries were migrated
SELECT id, full_name, first_name, last_name FROM beneficiaries LIMIT 10;

-- 2. Check members national_id
SELECT id, id_number, national_id FROM members LIMIT 10;

-- 3. Check users
SELECT id, email, username, user_id FROM users LIMIT 10;

-- 4. Check products
SELECT id, name, product_name, product_code FROM loan_products LIMIT 10;
SELECT id, name, product_name, product_code FROM savings_products LIMIT 10;

-- 5. Check accounts
SELECT id, balance, balance_amount, status, account_status FROM savings_accounts LIMIT 10;

-- 6. Check loans
SELECT id, status, loan_status, loan_balance, total_outstanding_amount FROM loans LIMIT 10;
```

---

## 🆘 If Migration Fails

1. **Check Flyway history:**
   ```sql
   SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;
   ```

2. **If migration is marked as failed:**
   ```sql
   -- Remove failed migration record
   DELETE FROM flyway_schema_history WHERE version = '5';
   ```

3. **Fix the issue and re-run:**
   ```bash
   ./mvnw flyway:migrate
   ```

4. **Get help:**
   - Check the error message carefully
   - Review the SQL script for syntax errors
   - Verify database permissions
   - Contact your DBA if needed

---

## ✅ Success Criteria

The migration is successful when:
- [ ] Flyway migration completes without errors
- [ ] Application starts successfully
- [ ] All API endpoints respond correctly
- [ ] Sample data queries return expected results
- [ ] Frontend can load data from backend

---

**Status:** READY FOR MIGRATION  
**Created:** January 7, 2026  
**Migration Script:** `V5__dictionary_refactor_phase_a_b.sql`  
**Configuration Updated:** `application.properties`

