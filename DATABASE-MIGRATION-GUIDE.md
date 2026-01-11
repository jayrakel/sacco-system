# 🔧 DATABASE MIGRATION GUIDE - Fix Existing Disbursed Loans

## ❓ WHY IS THIS NEEDED?

**The Problem:**
- The code fix for loan disbursement calculations only affects **NEW loans** disbursed after the fix
- **OLD loans** that were disbursed BEFORE the fix still have:
  - ❌ `outstanding_principal` = 0.00
  - ❌ `outstanding_interest` = 0.00
  - ❌ `total_outstanding_amount` = 0.00
  - ❌ `weekly_repayment_amount` = NULL
  - ❌ `maturity_date` = NULL
  - ❌ `created_by` = NULL

**The Solution:**
We need to **update the database** to recalculate these fields for existing loans.

---

## 🎯 THREE WAYS TO FIX EXISTING LOANS

### **Method 1: SQL Migration Script (Recommended for Production)**

**File:** `src/main/resources/db/migration/V1.1__Fix_Existing_Disbursed_Loans.sql`

**When to use:** Production deployment, Flyway/Liquibase migrations

**Database:** PostgreSQL

**Steps:**
```bash
# 1. Connect to your database
psql -U postgres -d sacco_db

# 2. Run the migration script
\i src/main/resources/db/migration/V1.1__Fix_Existing_Disbursed_Loans.sql

# 3. Verify the results
SELECT 
    loan_number,
    outstanding_principal,
    outstanding_interest,
    total_outstanding_amount,
    weekly_repayment_amount,
    maturity_date
FROM loans
WHERE loan_status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC;
```

---

### **Method 2: REST API Endpoint (Recommended for Testing)**

**Endpoint:** `POST /api/finance/admin/migrate-loans`

**Who can use:** ADMIN users only

**How to use:**

**Option A: Using Postman/Insomnia:**
```http
POST http://localhost:8082/api/finance/admin/migrate-loans
Authorization: Bearer {admin_token}
Content-Type: application/json
```

**Option B: Using cURL:**
```bash
curl -X POST http://localhost:8082/api/finance/admin/migrate-loans \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json"
```

**Option C: Browser Console (if logged in as Admin):**
```javascript
fetch('/api/finance/admin/migrate-loans', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('sacco_token')}`,
    'Content-Type': 'application/json'
  }
})
.then(res => res.json())
.then(data => console.log(data));
```

**Response:**
```json
{
  "success": true,
  "message": "Migration completed",
  "data": {
    "loansNeedingFix": 5,
    "loansFixed": 5,
    "message": "Successfully fixed 5 out of 5 loans"
  }
}
```

---

### **Method 3: Check First, Then Migrate**

**Step 1: Check how many loans need fixing**

```http
GET http://localhost:8082/api/finance/admin/loans-needing-fix
Authorization: Bearer {admin_token}
```

**Response:**
```json
{
  "success": true,
  "message": "Loans needing fix retrieved",
  "data": {
    "count": 5,
    "needsMigration": true
  }
}
```

**Step 2: If count > 0, run migration**

```http
POST http://localhost:8082/api/finance/admin/migrate-loans
Authorization: Bearer {admin_token}
```

---

## 📋 WHAT THE MIGRATION DOES

For each loan with `loan_status` = 'DISBURSED' or 'ACTIVE' that has missing fields:

### **1. Calculates Interest**
```
Formula: (Principal × Annual Rate × Duration in Weeks) / (100 × 52)

Example:
Principal: 50,000 KES
Rate: 10% per annum
Duration: 52 weeks

Interest = (50,000 × 10 × 52) / 5,200
         = 5,000 KES
```

### **2. Calculates Total Repayable**
```
Total = Principal + Interest
      = 50,000 + 5,000
      = 55,000 KES
```

### **3. Calculates Weekly Payment**
```
Weekly = Total / Duration
       = 55,000 / 52
       = 1,057.69 KES
```

### **4. Calculates Maturity Date**
```
Maturity = Disbursement Date + Duration in Weeks
         = Jan 11, 2026 + 52 weeks
         = Jan 10, 2027
```

### **5. Sets Audit Fields**
```
created_by = "SYSTEM_MIGRATION" (if NULL)
updated_by = "SYSTEM_MIGRATION"
updated_at = Current timestamp
```

---

## 🧪 TESTING THE MIGRATION

### **Before Migration:**

```sql
SELECT * FROM loans WHERE loan_number = 'LN-123456';

-- Results:
loan_status               = 'DISBURSED'
disbursed_amount         = 50000.00
outstanding_principal    = 0.00         ❌
outstanding_interest     = 0.00         ❌
total_outstanding_amount = 0.00         ❌
weekly_repayment_amount  = NULL         ❌
maturity_date            = NULL         ❌
```

### **After Migration:**

```sql
SELECT * FROM loans WHERE loan_number = 'LN-123456';

-- Results:
loan_status               = 'DISBURSED'
disbursed_amount         = 50000.00
outstanding_principal    = 50000.00     ✅
outstanding_interest     = 5000.00      ✅
total_outstanding_amount = 55000.00     ✅
weekly_repayment_amount  = 1057.69      ✅
maturity_date            = '2027-01-10' ✅
created_by               = 'SYSTEM_MIGRATION' ✅
updated_by               = 'SYSTEM_MIGRATION' ✅
```

---

## 🔍 VERIFICATION QUERIES

### **1. Count loans that need fixing:**
```sql
SELECT COUNT(*) as loans_needing_fix
FROM loans
WHERE loan_status IN ('DISBURSED', 'ACTIVE')
  AND (
    outstanding_principal IS NULL 
    OR outstanding_principal = 0.00
    OR outstanding_interest IS NULL 
    OR outstanding_interest = 0.00
    OR total_outstanding_amount IS NULL 
    OR total_outstanding_amount = 0.00
    OR weekly_repayment_amount IS NULL
    OR maturity_date IS NULL
  );
```

### **2. View all disbursed loans with their calculated fields:**
```sql
SELECT 
    loan_number,
    loan_status,
    disbursed_amount,
    outstanding_principal,
    outstanding_interest,
    total_outstanding_amount,
    weekly_repayment_amount,
    disbursement_date,
    maturity_date,
    (maturity_date - CURRENT_DATE) as days_to_maturity,
    updated_by,
    updated_at
FROM loans
WHERE loan_status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC;
```

### **3. Compare principal vs outstanding (should match initially):**
```sql
SELECT 
    loan_number,
    disbursed_amount,
    outstanding_principal,
    (disbursed_amount - outstanding_principal) as difference
FROM loans
WHERE loan_status IN ('DISBURSED', 'ACTIVE')
  AND disbursed_amount != outstanding_principal;
```

---

## ⚠️ IMPORTANT NOTES

### **Safety Measures:**

1. **Backup First:**
```sql
-- Create backup table
CREATE TABLE loans_backup_20260111 AS 
SELECT * FROM loans 
WHERE loan_status IN ('DISBURSED', 'ACTIVE');
```

2. **Test on Staging First:**
   - Run migration on staging/dev environment
   - Verify results
   - Then run on production

3. **Run During Low Traffic:**
   - Best time: After hours or weekends
   - Minimal user impact

### **What Won't Be Affected:**

- ✅ Loan applications in progress
- ✅ Pending approvals
- ✅ Committee voting
- ✅ New disbursements (already fixed by code)

### **What Will Be Updated:**

- ✅ Old loans with missing calculations
- ✅ Disbursed loans without weekly payment amounts
- ✅ Active loans without maturity dates
- ✅ Loans missing audit fields

---

## 🚀 DEPLOYMENT CHECKLIST

### **Pre-Deployment:**
```
☐ Backup database
☐ Test migration on staging
☐ Verify calculations are correct
☐ Check user impact (none expected)
☐ Schedule during low-traffic time
```

### **Deployment:**
```
☐ Deploy backend with new code
☐ Restart application
☐ Run migration (SQL or API)
☐ Verify via SQL queries
☐ Check logs for errors
☐ Test member dashboard
```

### **Post-Deployment:**
```
☐ All disbursed loans show correct amounts
☐ Member dashboard shows "KES X,XXX" not "KES NaN"
☐ Active loan cards display properly
☐ Weekly payments calculated
☐ Maturity dates set
☐ No errors in logs
```

---

## 📊 MIGRATION LOGS

The migration will log each fixed loan:

```
2026-01-11 10:30:15 INFO  🔧 Starting migration to fix existing disbursed loans...
2026-01-11 10:30:15 INFO  ✅ Fixed loan LN-123456: Principal=50000.00, Interest=5000.00, Total=55000.00, Weekly=1057.69, Maturity=2027-01-10
2026-01-11 10:30:15 INFO  ✅ Fixed loan LN-789012: Principal=30000.00, Interest=3000.00, Total=33000.00, Weekly=634.62, Maturity=2026-12-25
2026-01-11 10:30:15 INFO  🎉 Migration complete! Fixed 2 out of 2 loans
```

---

## 🔄 ROLLBACK PROCEDURE (If Needed)

If something goes wrong:

```sql
-- 1. Delete migrated data
DELETE FROM loans WHERE updated_by = 'SYSTEM_MIGRATION';

-- 2. Restore from backup
INSERT INTO loans 
SELECT * FROM loans_backup_20260111;

-- 3. Verify restoration
SELECT COUNT(*) FROM loans WHERE loan_status IN ('DISBURSED', 'ACTIVE');
```

---

## ❓ FAQ

**Q: Will this affect new loans?**
A: No. New loans disbursed after the code fix will automatically have all fields calculated.

**Q: Do I need to run this multiple times?**
A: No. Run it once after deploying the fix. The migration skips loans that are already correct.

**Q: What if a loan is partially repaid?**
A: The migration sets INITIAL outstanding amounts. Repayments will reduce these amounts normally.

**Q: Can regular users run this?**
A: No. The API endpoint is ADMIN only. SQL access is typically restricted to DBAs.

**Q: Will users see any downtime?**
A: No. The migration runs in the background and is very fast (milliseconds per loan).

---

## ✅ SUCCESS CRITERIA

After running the migration, verify:

1. ✅ All DISBURSED/ACTIVE loans have `outstanding_principal` > 0
2. ✅ All DISBURSED/ACTIVE loans have `outstanding_interest` > 0
3. ✅ All DISBURSED/ACTIVE loans have `total_outstanding_amount` > 0
4. ✅ All DISBURSED/ACTIVE loans have `weekly_repayment_amount` NOT NULL
5. ✅ All DISBURSED/ACTIVE loans have `maturity_date` NOT NULL
6. ✅ Member dashboard shows "KES X,XXX" not "KES NaN"
7. ✅ Active loan card displays all amounts correctly
8. ✅ No errors in application logs

---

## 📞 SUPPORT

If you encounter issues:

1. Check application logs: `tail -f app.log`
2. Verify database connection
3. Ensure admin credentials are correct
4. Check backup exists before rollback
5. Contact system administrator

---

**Remember: This migration is REQUIRED to fix existing disbursed loans. The code fix only affects future disbursements!** 🔧✅

