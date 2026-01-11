# ✅ SQL SYNTAX FIXED FOR POSTGRESQL

## 🐛 THE ERROR

```
ERROR:  syntax error at or near "duration_weeks"
LINE 30: DATE_ADD(disbursement_date, INTERVAL duration_we...
```

**Root Cause:** The SQL migration script was using **MySQL syntax** but your database is **PostgreSQL**.

---

## 🔧 WHAT WAS FIXED

### **1. Date Arithmetic**

**MySQL (WRONG):**
```sql
DATE_ADD(disbursement_date, INTERVAL duration_weeks WEEK)
```

**PostgreSQL (CORRECT):**
```sql
disbursement_date + (duration_weeks || ' weeks')::INTERVAL
```

---

### **2. ROUND Function with CAST**

**MySQL (Works without CAST):**
```sql
ROUND((amount * rate * weeks) / 5200.0, 2)
```

**PostgreSQL (Needs CAST to NUMERIC):**
```sql
ROUND(CAST((amount * rate * weeks) / 5200.0 AS NUMERIC), 2)
```

---

### **3. Division by Zero Protection**

**Added NULLIF to prevent division by zero:**
```sql
weekly_repayment_amount = ROUND(
    CAST(total / NULLIF(duration_weeks, 0) AS NUMERIC),
    2
)
```

---

### **4. Date Difference**

**MySQL:**
```sql
DATEDIFF(maturity_date, CURDATE()) as days_to_maturity
```

**PostgreSQL:**
```sql
(maturity_date - CURRENT_DATE) as days_to_maturity
```

---

## 📊 COMPLETE FIXED SQL

The migration script now uses proper PostgreSQL syntax:

```sql
UPDATE loans
SET 
    outstanding_principal = COALESCE(disbursed_amount, approved_amount),
    
    outstanding_interest = ROUND(
        CAST((COALESCE(disbursed_amount, approved_amount) * interest_rate * duration_weeks) / 5200.0 AS NUMERIC), 
        2
    ),
    
    total_outstanding_amount = ROUND(
        CAST(COALESCE(disbursed_amount, approved_amount) + 
        ((COALESCE(disbursed_amount, approved_amount) * interest_rate * duration_weeks) / 5200.0) AS NUMERIC),
        2
    ),
    
    weekly_repayment_amount = ROUND(
        CAST((COALESCE(disbursed_amount, approved_amount) + 
        ((COALESCE(disbursed_amount, approved_amount) * interest_rate * duration_weeks) / 5200.0)) / 
        NULLIF(duration_weeks, 0) AS NUMERIC),
        2
    ),
    
    -- PostgreSQL interval syntax
    maturity_date = CASE 
        WHEN disbursement_date IS NOT NULL THEN 
            disbursement_date + (duration_weeks || ' weeks')::INTERVAL
        ELSE 
            CURRENT_DATE + (duration_weeks || ' weeks')::INTERVAL
    END,
    
    created_by = COALESCE(created_by, 'SYSTEM_MIGRATION'),
    updated_by = 'SYSTEM_MIGRATION',
    updated_at = CURRENT_TIMESTAMP

WHERE 
    loan_status IN ('DISBURSED', 'ACTIVE')
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

---

## ✅ NOW YOU CAN RUN THE MIGRATION

**Method 1: Via psql**
```bash
psql -U postgres -d sacco_db
\i src/main/resources/db/migration/V1.1__Fix_Existing_Disbursed_Loans.sql
```

**Method 2: Via Admin API**
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

---

## 🧪 VERIFY IT WORKED

```sql
-- Check if loans were updated
SELECT 
    loan_number,
    outstanding_principal,
    outstanding_interest,
    total_outstanding_amount,
    weekly_repayment_amount,
    maturity_date,
    updated_by
FROM loans
WHERE loan_status IN ('DISBURSED', 'ACTIVE')
  AND updated_by = 'SYSTEM_MIGRATION';
```

**Expected Results:**
- ✅ All fields should have values (not NULL or 0.00)
- ✅ `updated_by` = 'SYSTEM_MIGRATION'
- ✅ `maturity_date` = disbursement_date + duration in weeks

---

## 📝 KEY POSTGRESQL DIFFERENCES

| Feature | MySQL | PostgreSQL |
|---------|-------|------------|
| **Date Add** | `DATE_ADD(date, INTERVAL n WEEK)` | `date + (n \|\| ' weeks')::INTERVAL` |
| **Current Date** | `CURDATE()` | `CURRENT_DATE` |
| **Date Diff** | `DATEDIFF(date1, date2)` | `date1 - date2` |
| **Rounding** | `ROUND(value, 2)` works on floats | `ROUND(CAST(value AS NUMERIC), 2)` |
| **Concat** | `CONCAT(a, b)` | `a \|\| b` |
| **Div by Zero** | Throws error | Use `NULLIF(divisor, 0)` |

---

**The SQL migration script is now fixed and ready to run on PostgreSQL!** ✅

