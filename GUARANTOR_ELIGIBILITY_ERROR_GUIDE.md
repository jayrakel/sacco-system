# Guarantor Eligibility Error - Troubleshooting Guide

## Your Error Message
```
Guarantor not eligible:
- Membership too recent to guarantee. Required: 1 months, Current: 0 months
- Exceeds guarantor limit. Available to guarantee: KES -7000.000, Requested: KES 10000
```

---

## What This Means

### Issue 1: Membership Too Recent (0 months)
- **Problem:** The guarantor account was created less than 1 month ago (possibly today or this week)
- **Requirement:** System is set to require at least 1 month of membership
- **Current:** 0 months (account is brand new)

### Issue 2: Already Over-Committed (KES -7,000)
- **Problem:** The guarantor has NEGATIVE available capacity
- **What this means:** They've already guaranteed MORE money than they're allowed to
- **Available:** -KES 7,000 (they're KES 7,000 OVER their limit)
- **You're requesting:** KES 10,000 more (which would make it even worse)

---

## Why Negative Capacity Happens

The system calculates available guarantee capacity as:

```
Available = (Total Savings × MAX_RATIO) - Current Guarantees

Example:
- Guarantor's Savings: KES 5,000
- MAX_GUARANTOR_LIMIT_RATIO: 2
- Maximum they can guarantee: 5,000 × 2 = KES 10,000

If they've already guaranteed:
- Current guarantees: KES 17,000
- Available: (5,000 × 2) - 17,000 = -KES 7,000
```

This means this person has already guaranteed KES 17,000 but only has KES 5,000 in savings!

---

## Solutions

### Option 1: Wait for Membership Duration ⏰
**When:** If you really need this specific guarantor
**Action:** Wait until they've been a member for at least 1 month

**Timeline:**
- Account created: [Check member's created date]
- Eligible on: [1 month from creation date]

---

### Option 2: Lower Membership Requirement ⚙️
**When:** You want to allow newer members to guarantee
**Action:** Change system setting

1. Login as **Admin**
2. Go to **Configuration → System Parameters**
3. Find **MIN MONTHS TO GUARANTEE**
4. Change from: `1` to: `0`
5. Save

⚠️ **Warning:** This allows brand new members to guarantee loans, which increases risk!

---

### Option 3: Choose Different Guarantor (RECOMMENDED) ✅
**When:** You need to proceed now
**Action:** Select a guarantor who:

✅ Has been a member for at least 1 month  
✅ Has positive available guarantee capacity  
✅ Has sufficient savings  

**How to check before selecting:**
- The system shows eligibility when you search for guarantors
- Look for the "Available to guarantee" amount
- Make sure it's POSITIVE and greater than your requested amount

---

### Option 4: Fix Over-Committed Guarantor 🔧
**When:** You must use this guarantor
**Action:** They need to increase their savings OR reduce existing guarantees

**Path A - Increase Savings:**
1. Guarantor deposits more money into savings
2. This increases their maximum guarantee capacity
3. Example: If they deposit KES 10,000, their capacity increases by KES 20,000

**Path B - Reduce Existing Guarantees:**
1. Review their existing guarantor commitments
2. See if any loans can be completed or released
3. Each completed loan frees up that guarantee amount

---

## How to Check Guarantor Details

### As Admin:
1. Go to **Members**
2. Search for the guarantor
3. View their profile
4. Check:
   - **Created Date** (for membership duration)
   - **Total Savings** (to calculate capacity)
   - **Existing Guarantees** (to see commitments)

### Expected Data:
```
Member: John Doe
Created: December 19, 2024 (1 day ago = 0 months)
Total Savings: KES 5,000
Current Guarantees: KES 17,000
Maximum Allowed: 5,000 × 2 = KES 10,000
Available: 10,000 - 17,000 = -KES 7,000 ❌
```

---

## System Settings Reference

Current thresholds (check in System Parameters):
- `MIN_MONTHS_TO_GUARANTEE`: **1 month** (appears to be lowered from default 6)
- `MIN_SAVINGS_TO_GUARANTEE`: **KES 10,000**
- `MAX_GUARANTOR_LIMIT_RATIO`: **2** (can guarantee up to 2× savings)

**Recommendation:** Review if 1 month is too lenient for your SACCO's risk profile.

---

## Quick Decision Tree

```
Can I use this guarantor?
│
├─ Membership < 1 month? → NO ❌ Wait or choose another
│
├─ Available capacity negative? → NO ❌ They're over-committed
│
├─ Available capacity < requested? → NO ❌ Not enough capacity
│
└─ All checks pass? → YES ✅ Can proceed
```

---

## Preventing This in Future

1. **Set appropriate membership duration requirements**
   - Default: 6 months (safer)
   - Minimum: 3 months (balanced)
   - Current: 1 month (risky)

2. **Educate members on guarantee limits**
   - Show available capacity before committing
   - Warn when approaching limits
   - Display existing commitments clearly

3. **Monitor over-committed guarantors**
   - Run reports on guarantors with negative capacity
   - Require them to increase savings
   - Consider system alerts

---

## Need More Help?

**If you want to see the exact details of why this guarantor failed:**

1. Open browser console (F12)
2. Try adding the guarantor again
3. Look for the API response showing:
   - Exact savings amount
   - Exact guarantee exposure
   - Calculated available capacity

**Or contact support with:**
- Guarantor member number
- Loan ID you're working on
- Screenshot of the error

---

## Status

❌ **Current:** Cannot use this guarantor  
✅ **Recommended Action:** Choose a different guarantor with positive capacity  
⚙️ **Alternative:** Lower MIN_MONTHS_TO_GUARANTEE to 0 (if acceptable risk)


