# ✅ FIX: Database Constraint Error - UNDER_REVIEW Status

**Issue:** `constraint [loans_loan_status_check]` violation when clicking "Start Review"

---

## 🐛 ROOT CAUSE

The database CHECK constraint on `loans.loan_status` column **did NOT include `UNDER_REVIEW`** as an allowed value!

**What happened:**
1. Java enum `Loan.LoanStatus` has `UNDER_REVIEW` defined ✅
2. Code tries to save loan with status `UNDER_REVIEW` ✅
3. Database rejects it because CHECK constraint doesn't allow it ❌

**Error:**
```sql
could not execute statement...constraint [loans_loan_status_check]
```

---

## ✅ FIX APPLIED

### Created Migration File:
**File:** `V7__add_under_review_status.sql`

**What it does:**
1. Drops the old CHECK constraint
2. Recreates it with ALL valid statuses including `UNDER_REVIEW`

**SQL:**
```sql
ALTER TABLE loans DROP CONSTRAINT IF EXISTS loans_loan_status_check;

ALTER TABLE loans ADD CONSTRAINT loans_loan_status_check CHECK (
    loan_status IN (
        'DRAFT',
        'PENDING_GUARANTORS',
        'AWAITING_GUARANTORS', 
        'SUBMITTED',
        'UNDER_REVIEW',  -- ✅ ADDED
        'APPROVED',
        'REJECTED',
        'CANCELLED',
        'DISBURSED',
        'ACTIVE',
        'IN_ARREARS',
        'DEFAULTED',
        'CLOSED',
        'WRITTEN_OFF'
    )
);
```

---

## 🚀 DEPLOYMENT

### Restart Backend:
```bash
# Stop current backend (Ctrl+C)
mvn spring-boot:run
```

**Flyway will automatically run the migration on startup!**

---

## 🧪 TESTING

After restart:
1. Go to Loan Officer Dashboard
2. Click "Review" on a loan
3. Click "Start Review" button
4. ✅ Should now work - loan moves to `UNDER_REVIEW`
5. ✅ Applicant receives email/SMS
6. ✅ No more constraint error!

---

## 📝 WHAT HAPPENS NOW

When you click "Start Review":
1. Backend changes loan status from `SUBMITTED` → `UNDER_REVIEW`
2. Database accepts it (constraint now allows it) ✅
3. Email sent to member: "Your loan is under review"
4. SMS sent to member
5. Audit log created
6. Modal stays open, status updates

---

## ⚠️ WHY THIS HAPPENED

The initial database schema was created before `UNDER_REVIEW` status was added to the workflow. The enum was updated in code, but the database constraint wasn't updated.

**This is why database migrations are important!**

---

**Status:** ✅ FIXED - Restart backend and test!

