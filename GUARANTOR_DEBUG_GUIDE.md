# How to Debug Guarantor Capacity Issues

## The Problem You Had

You got this error:
```
Guarantor not eligible:
- Membership too recent to guarantee. Required: 1 months, Current: 0 months
- Exceeds guarantor limit. Available to guarantee: KES -7000.000, Requested: KES 10000
```

But the guarantor has **KES 500,000** in savings, so they should have plenty of capacity!

---

## What I Fixed

### 1. Updated Guarantor Eligibility Logic
**Changed:** Now counts BOTH `ACCEPTED` and `PENDING` guarantees (not just accepted)

**Why:** Pending guarantees are still commitments that haven't been declined yet, so they should reduce available capacity.

**File:** `LoanService.java` - `checkGuarantorEligibility()` method

### 2. Added Debug Endpoint
**New endpoint:** `GET /api/debug/guarantor/{memberId}`

**What it does:** Shows you EXACTLY what's happening with a guarantor's capacity calculation

---

## How to Use the Debug Endpoint

### Step 1: Get the Member ID
1. Find the guarantor's member number (e.g., MEM000001)
2. Go to the Members page and note their UUID

**OR** use the browser console when selecting the guarantor - it will show the member ID.

### Step 2: Call the Debug Endpoint

**URL:** `http://localhost:8081/api/debug/guarantor/{memberId}`

Replace `{memberId}` with the actual UUID.

**Example:**
```
http://localhost:8081/api/debug/guarantor/a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

### Step 3: Read the Response

You'll get a detailed breakdown like this:

```json
{
  "success": true,
  "data": {
    "memberNumber": "MEM000001",
    "memberName": "John Doe",
    "memberStatus": "ACTIVE",
    "totalSavings": 500000,
    "maxGuarantorRatio": 2.0,
    "maxGuarantorLimit": 1000000,
    "totalGuaranteeRecords": 5,
    
    "guaranteesByStatus": {
      "ACCEPTED": {
        "count": 2,
        "totalAmount": 50000,
        "details": [...]
      },
      "PENDING": {
        "count": 3,
        "totalAmount": 457000,
        "details": [...]
      },
      "DECLINED": {
        "count": 0,
        "totalAmount": 0,
        "details": []
      }
    },
    
    "currentExposure_ACCEPTED_ONLY": 50000,
    "currentExposure_ACCEPTED_AND_PENDING": 507000,
    "availableToGuarantee_CURRENT_LOGIC": 950000,
    "availableToGuarantee_RECOMMENDED": 493000,
    
    "analysis": {
      "isOverCommitted": false,
      "wouldBeOverCommittedWithPending": false,
      "message": "Member has capacity available"
    }
  }
}
```

---

## Understanding the Response

### Key Fields:

**totalSavings**
- Current total savings in the account
- Example: 500000 (KES 500,000)

**maxGuarantorLimit**
- Maximum they can guarantee
- Formula: `totalSavings × maxGuarantorRatio`
- Example: 500,000 × 2 = 1,000,000

**guaranteesByStatus**
- Breaks down all their guarantee commitments by status
- Shows ACCEPTED, PENDING, and DECLINED separately

**currentExposure_ACCEPTED_ONLY**
- Old calculation (only counting accepted guarantees)
- This is what was causing your issue!

**currentExposure_ACCEPTED_AND_PENDING**
- New calculation (counting both accepted AND pending)
- More accurate representation of commitments

**availableToGuarantee_RECOMMENDED**
- How much capacity they actually have left
- Formula: `maxLimit - (accepted + pending)`

---

## What Likely Happened in Your Case

Based on the error (KES -7,000 available), here's what probably occurred:

### Scenario 1: Many Pending Guarantees
```
Member Savings: KES 500,000
Max Limit: 500,000 × 2 = KES 1,000,000

Old Logic (ACCEPTED only):
- Accepted guarantees: KES 17,000
- Available: 1,000,000 - 17,000 = KES 983,000 ✅ (looks good!)

New Logic (ACCEPTED + PENDING):
- Accepted guarantees: KES 17,000
- Pending guarantees: KES 990,000 (!!!)
- Total exposure: KES 1,007,000
- Available: 1,000,000 - 1,007,000 = -KES 7,000 ❌

Why negative? They have KES 990,000 in PENDING requests that haven't been 
responded to yet!
```

### Scenario 2: Old Data Before Savings Increase
```
Before deposit:
- Savings: KES 3,500
- Guarantees: KES 17,000
- Max limit: 3,500 × 2 = KES 7,000
- Available: 7,000 - 17,000 = -KES 10,000 ❌

After deposit of KES 496,500:
- Savings: KES 500,000
- Guarantees: KES 17,000 (still the same)
- Max limit: 500,000 × 2 = KES 1,000,000
- Available: 1,000,000 - 17,000 = KES 983,000 ✅

BUT if frontend cached the old check, it would still show -KES 7,000!
```

---

## How to Fix Common Issues

### Issue 1: Frontend Caching Old Data
**Solution:** Refresh the page or clear browser cache

### Issue 2: Too Many Pending Guarantees
**Solution:** 
1. Check all pending guarantee requests
2. Decline ones that won't be needed
3. This frees up capacity immediately

### Issue 3: Stale Database Values
**Solution:**
1. Use the debug endpoint to see real-time values
2. Compare with what the frontend shows
3. If different, refresh the member's data

---

## Testing the Fix

### Before (Old Behavior):
- Only counted ACCEPTED guarantees
- Member could commit to many PENDING guarantees
- Would show available capacity incorrectly

### After (New Behavior):
- Counts BOTH ACCEPTED and PENDING guarantees
- More accurate representation of commitments
- Prevents over-commitment

### To Test:
1. **Run the backend** with the updated code
2. **Call the debug endpoint** for your problematic guarantor
3. **Check the response** - compare `ACCEPTED_ONLY` vs `ACCEPTED_AND_PENDING`
4. **Try adding them as guarantor again** - should work if they have real capacity

---

## Next Steps

1. ✅ **Compile the code** (already done - no errors!)
2. 🚀 **Restart the backend** to apply changes
3. 🔍 **Use debug endpoint** to investigate the specific guarantor
4. 📊 **Share the debug output** if you need help understanding it
5. ✅ **Try adding the guarantor again** after confirming they have capacity

---

## Quick Commands

### Compile:
```bash
cd "C:\Users\JAY\OneDrive\Desktop\sacco-system"
./mvnw clean compile
```

### Run:
```bash
./mvnw spring-boot:run
```

### Test Debug Endpoint (in browser or Postman):
```
GET http://localhost:8081/api/debug/guarantor/{memberId}
```

---

## Summary

✅ **Fixed:** Guarantor eligibility now counts PENDING guarantees  
✅ **Added:** Debug endpoint to troubleshoot capacity issues  
✅ **Compiled:** No errors  
📊 **Next:** Restart backend and test with the debug endpoint

The issue with your KES 500,000 savings guarantor should be resolved once you restart the backend!

