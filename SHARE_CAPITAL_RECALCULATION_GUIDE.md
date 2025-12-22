# Share Capital Recalculation Guide

## Problem Overview

Your share capital data has inconsistencies because:
1. **Old deposits** were created with `shareValue = KES 1 per share`
2. **New system setting** uses `SHARE_VALUE = KES 100 per share`
3. Some records show 0 shares (calculation never ran)

### Current Data State
- **Account 1**: KES 10,000 paid, 0 shares, shareValue=1, ownership=62.50%
- **Account 2**: KES 6,000 paid, 6,000 shares, shareValue=1, ownership=37.50%
- **Admin view**: Total=16,000, shares=160, shareValue=100

## Solution Implemented

### Backend Changes

#### 1. ShareCapitalService.java
Added two new features:

**`getShareValue()` - Now reads from system settings**
```java
public BigDecimal getShareValue() {
    return BigDecimal.valueOf(systemSettingService.getDouble("SHARE_VALUE", 100.0));
}
```

**`recalculateAllShares()` - Fixes all share capital records**
```java
public int recalculateAllShares() {
    BigDecimal currentShareValue = getShareValue(); // Get current SHARE_VALUE setting
    List<ShareCapital> allRecords = shareCapitalRepository.findAll();
    
    for (ShareCapital shareCapital : allRecords) {
        // Update share value to current setting
        shareCapital.setShareValue(currentShareValue);
        
        // Recalculate shares: paidAmount ÷ shareValue
        BigDecimal sharesOwned = shareCapital.getPaidAmount()
                .divide(currentShareValue, 2, RoundingMode.DOWN);
        
        shareCapital.setPaidShares(sharesOwned);
        shareCapital.setTotalShares(sharesOwned);
        
        shareCapitalRepository.save(shareCapital);
    }
    
    return updatedCount;
}
```

#### 2. ShareCapitalController.java
Added admin endpoint:
```
POST /api/shares/admin/recalculate
```

### Frontend Changes

#### System Settings (Admin Dashboard)
Added new **Maintenance** tab with:
- Share Capital Recalculation tool
- Clear instructions on when to use it
- Success/error feedback

## How to Fix Your Data

### Step 1: Access System Settings
1. Login as **Admin**
2. Navigate to **Admin Dashboard**
3. Click **System Settings**

### Step 2: Go to Maintenance Tab
1. Click the **Maintenance** tab (with wrench icon)
2. Find "Recalculate Share Capital" section

### Step 3: Run Recalculation
1. Click **Recalculate All Shares** button
2. Confirm the action in the popup
3. Wait for completion message

### What Happens
The system will:
1. Read current `SHARE_VALUE` from settings (KES 100)
2. Update ALL share capital records with this value
3. Recalculate shares for each member: `shares = paidAmount ÷ 100`

### Expected Result After Recalculation

**Account 1**:
- Paid Amount: KES 10,000 ✓ (unchanged)
- Share Value: KES 100 ✓ (updated from 1)
- Shares Owned: **100 shares** ✓ (was 0, now 10,000 ÷ 100 = 100)
- Ownership: 62.50% ✓ (unchanged, based on amount)

**Account 2**:
- Paid Amount: KES 6,000 ✓ (unchanged)
- Share Value: KES 100 ✓ (updated from 1)
- Shares Owned: **60 shares** ✓ (was 6,000, now 6,000 ÷ 100 = 60)
- Ownership: 37.50% ✓ (unchanged)

**Admin View**:
- Total Share Capital: KES 16,000 ✓ (unchanged)
- Total Shares: **160 shares** ✓ (100 + 60 = 160)
- Share Value: KES 100 ✓

## When to Use This Tool

### ✅ Use recalculation when:
- Share value has been updated in System Parameters
- Share counts appear incorrect or inconsistent
- After migrating data from another system
- Old deposits show wrong share values

### ⚠️ Important Notes:
- **This is safe** - It only recalculates, doesn't change paid amounts
- **Ownership percentages** are based on amounts, so they stay correct
- **Run this ONCE** after fixing the SHARE_VALUE setting
- **Future deposits** will automatically use the correct share value

## Technical Details

### How Shares Are Calculated
```
Shares Owned = Paid Amount ÷ Share Value
```

Examples:
- KES 10,000 ÷ KES 100 = 100 shares
- KES 6,000 ÷ KES 100 = 60 shares
- KES 500 ÷ KES 100 = 5 shares

### How Ownership Percentage Is Calculated
```
Ownership % = (Member's Paid Amount ÷ Total SACCO Share Capital) × 100
```

Example:
- Account 1: (10,000 ÷ 16,000) × 100 = 62.50%
- Account 2: (6,000 ÷ 16,000) × 100 = 37.50%

### Future Deposits
All new share capital deposits will:
1. Read current `SHARE_VALUE` from system settings
2. Calculate shares automatically: `amount ÷ share value`
3. Create transaction record with SHARE_PURCHASE type
4. Update member's share capital immediately

No manual recalculation needed for new deposits!

## Configuration

### Change Share Value (Admin Only)
1. Go to **System Settings** → **System Parameters** tab
2. Find `SHARE_VALUE` setting
3. Update to desired value (e.g., 50, 100, 200)
4. Click **Save Configuration**
5. Go to **Maintenance** tab and run recalculation
6. All existing records will update to new value

### Example Scenarios

**Scenario 1: Increase share value to KES 200**
- Current: Member has 10,000 paid = 100 shares @ KES 100
- After: Member has 10,000 paid = 50 shares @ KES 200
- Amount unchanged, shares adjusted

**Scenario 2: Decrease share value to KES 50**
- Current: Member has 10,000 paid = 100 shares @ KES 100
- After: Member has 10,000 paid = 200 shares @ KES 50
- Amount unchanged, shares adjusted

## Troubleshooting

### Issue: Button doesn't work
- **Check**: Are you logged in as Admin?
- **Check**: Do you have network connection?
- **Check**: Look at browser console for errors (F12)

### Issue: Shows 0 records updated
- **Cause**: No share capital records exist in database
- **Solution**: Make at least one share capital deposit first

### Issue: Numbers still look wrong
- **Check**: Did the recalculation succeed? Look for ✅ message
- **Refresh**: Refresh your browser and check the cards again
- **Verify**: Check System Parameters to confirm SHARE_VALUE is correct

## Summary

This fix ensures all share capital data is consistent:
- ✅ All records use current SHARE_VALUE from settings
- ✅ Shares calculated correctly: amount ÷ share value  
- ✅ Ownership percentages remain accurate
- ✅ New deposits automatically use correct calculations
- ✅ Simple one-click fix available in admin panel

Just run the recalculation tool once, and your data will be fixed!
