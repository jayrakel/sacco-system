# 🐛 DATABASE CONSTRAINT ERROR: VOTING_CLOSED Status Not Recognized

**Error:** `constraint [meetings_status_check]` - Database rejects VOTING_CLOSED status

**Root Cause:** We added new enum values to Java code but database check constraints weren't updated.

---

## 🔍 THE PROBLEM

### Error Message:
```
could not execute statement [ERROR: new row for relation "meetings" 
violates check constraint "meetings_status_check"]
```

### What Happened:
1. We added `VOTING_CLOSED` to `Meeting.MeetingStatus` enum in Java
2. We added `APPROVED_BY_COMMITTEE` to `Loan.LoanStatus` enum in Java
3. **BUT** database check constraints still have old values
4. When trying to save `meeting.setStatus(VOTING_CLOSED)`, database rejects it

### Database Constraint (OLD):
```sql
CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'POSTPONED'))
```
❌ Missing: `VOTING_CLOSED`

---

## ✅ THE FIX

### Created Database Migration Script

**File:** `V8__add_voting_closed_status_and_approved_by_committee.sql`

```sql
-- Update meetings table to allow VOTING_CLOSED status
ALTER TABLE meetings DROP CONSTRAINT IF EXISTS meetings_status_check;
ALTER TABLE meetings ADD CONSTRAINT meetings_status_check 
    CHECK (status IN (
        'SCHEDULED', 
        'IN_PROGRESS', 
        'VOTING_CLOSED',      -- ✅ NEW!
        'COMPLETED', 
        'CANCELLED', 
        'POSTPONED'
    ));

-- Update loans table to allow APPROVED_BY_COMMITTEE status
ALTER TABLE loans DROP CONSTRAINT IF EXISTS loans_loan_status_check;
ALTER TABLE loans ADD CONSTRAINT loans_loan_status_check 
    CHECK (loan_status IN (
        'DRAFT', 
        'PENDING_GUARANTORS', 
        'AWAITING_GUARANTORS', 
        'SUBMITTED', 
        'UNDER_REVIEW', 
        'APPROVED', 
        'APPROVED_BY_COMMITTEE',  -- ✅ NEW!
        'REJECTED', 
        'CANCELLED', 
        'DISBURSED', 
        'ACTIVE', 
        'IN_ARREARS', 
        'DEFAULTED', 
        'CLOSED', 
        'WRITTEN_OFF'
    ));
```

---

## 🛡️ GUARDRAILS VERIFIED

### Secretary Cannot Finalize Unless Voting is Closed

**Location:** `VotingService.finalizeVotingResults()`

```java
@Transactional
public void finalizeVotingResults(UUID meetingId, String secretaryEmail) {
    Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new ApiException("Meeting not found", 404));

    // ✅ GUARDRAIL: Must be VOTING_CLOSED status
    if (meeting.getStatus() != Meeting.MeetingStatus.VOTING_CLOSED) {
        throw new ApiException("Voting must be closed before finalizing results", 400);
    }

    // ... rest of finalization logic
}
```

**Protection:**
- ❌ Cannot finalize if status = SCHEDULED
- ❌ Cannot finalize if status = IN_PROGRESS (voting still open!)
- ✅ Can only finalize if status = VOTING_CLOSED
- ❌ Cannot finalize if status = COMPLETED (already done)

---

## 🔄 WORKFLOW ENFORCEMENT

### Complete Flow with Guardrails:

**Step 1: Chairperson Opens Voting**
```
Status: SCHEDULED → IN_PROGRESS ✅
Secretary tries to finalize → ❌ ERROR: "Voting must be closed"
```

**Step 2: Members Vote**
```
Status: IN_PROGRESS (voting open)
Secretary tries to finalize → ❌ ERROR: "Voting must be closed"
```

**Step 3: Chairperson Closes Voting**
```
Status: IN_PROGRESS → VOTING_CLOSED ✅
Secretary tries to finalize → ✅ ALLOWED
```

**Step 4: Secretary Finalizes**
```
Status: VOTING_CLOSED → COMPLETED ✅
Secretary tries to finalize again → ❌ ERROR: "Voting must be closed"
```

---

## 🚀 DEPLOYMENT

### Apply Database Migration

**Option 1: Automatic (Spring Boot will run on startup)**
```bash
# Just restart the application
cd C:\Users\JAY\OneDrive\Desktop\sacco-system
mvn spring-boot:run
```

Spring Boot + Flyway will automatically:
1. Detect new migration file `V8__*.sql`
2. Run the ALTER TABLE commands
3. Update database constraints

**Option 2: Manual (if needed)**
```sql
-- Connect to your database and run:
-- (Only if automatic migration fails)

ALTER TABLE meetings DROP CONSTRAINT IF EXISTS meetings_status_check;
ALTER TABLE meetings ADD CONSTRAINT meetings_status_check 
    CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'VOTING_CLOSED', 'COMPLETED', 'CANCELLED', 'POSTPONED'));

ALTER TABLE loans DROP CONSTRAINT IF EXISTS loans_loan_status_check;
ALTER TABLE loans ADD CONSTRAINT loans_loan_status_check 
    CHECK (loan_status IN (
        'DRAFT', 'PENDING_GUARANTORS', 'AWAITING_GUARANTORS', 'SUBMITTED', 
        'UNDER_REVIEW', 'APPROVED', 'APPROVED_BY_COMMITTEE', 'REJECTED', 
        'CANCELLED', 'DISBURSED', 'ACTIVE', 'IN_ARREARS', 'DEFAULTED', 
        'CLOSED', 'WRITTEN_OFF'
    ));
```

---

## ✅ VERIFICATION

### After Restarting Backend:

**1. Check Migration Applied:**
```sql
SELECT version, description, installed_on 
FROM flyway_schema_history 
ORDER BY installed_on DESC 
LIMIT 1;
```

**Expected:**
```
version | description                                    | installed_on
--------|------------------------------------------------|---------------
8       | add voting closed status and approved by comm  | 2026-01-10 ...
```

**2. Test Chairperson Close Voting:**
```
1. Login as Chairperson
2. Go to Active Voting section
3. Click "Close Voting"
4. ✅ Should succeed (no constraint error)
5. ✅ Meeting status → VOTING_CLOSED
```

**3. Test Secretary Guardrail:**
```
Before chairperson closes:
  Secretary clicks "Finalize"
  ✅ Error: "Voting must be closed before finalizing results"

After chairperson closes:
  Secretary clicks "Finalize"
  ✅ Success: Results finalized
```

**4. Test Database Constraint:**
```sql
-- Verify constraint allows VOTING_CLOSED
UPDATE meetings 
SET status = 'VOTING_CLOSED' 
WHERE status = 'SCHEDULED' 
LIMIT 1;

-- Should succeed without constraint error
```

---

## 🔍 TROUBLESHOOTING

### If Migration Doesn't Run Automatically:

**Check Flyway Configuration:**
```properties
# application.properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
```

**Check Migration File Naming:**
```
✅ V8__add_voting_closed_status_and_approved_by_committee.sql
❌ v8__add_voting_closed_status.sql (lowercase V)
❌ V8_add_voting_closed_status.sql (single underscore)
```

**Run Migration Manually:**
```bash
mvn flyway:migrate
```

---

### If Constraint Error Persists:

**1. Check Current Constraint:**
```sql
SELECT conname, consrc 
FROM pg_constraint 
WHERE conname = 'meetings_status_check';
```

**2. Drop and Recreate Manually:**
```sql
ALTER TABLE meetings DROP CONSTRAINT meetings_status_check;
ALTER TABLE meetings ADD CONSTRAINT meetings_status_check 
    CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'VOTING_CLOSED', 'COMPLETED', 'CANCELLED', 'POSTPONED'));
```

**3. Restart Application:**
```bash
mvn spring-boot:run
```

---

## 📝 FILES CREATED

### Database Migration:
- `V8__add_voting_closed_status_and_approved_by_committee.sql`
  - Updates `meetings` table constraint
  - Updates `loans` table constraint
  - Adds support for new enum values

### Guardrails (Already Exist):
- `VotingService.finalizeVotingResults()` - Line 222
  - Checks status = VOTING_CLOSED before allowing finalization

---

## ✨ SUMMARY

**Problem:** Database constraint rejects VOTING_CLOSED status

**Root Cause:** Java enums updated but database constraints not updated

**Solution:**
1. ✅ Created migration script to update constraints
2. ✅ Verified guardrail exists (secretary can only finalize when VOTING_CLOSED)
3. ✅ Migration will run automatically on next startup

**Next Steps:**
1. Restart backend → Migration runs automatically
2. Test chairperson close voting → Should work
3. Test secretary finalize → Should only work after chairperson closes

---

**Status:** ✅ FIXED - Restart backend to apply migration!

**Command:**
```bash
cd C:\Users\JAY\OneDrive\Desktop\sacco-system
mvn spring-boot:run
```

**The database constraints will be updated and VOTING_CLOSED status will work!** 🎉

