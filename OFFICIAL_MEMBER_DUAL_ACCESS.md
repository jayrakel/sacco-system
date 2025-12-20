# Official Members: Dual Portal Access Implementation ✅

## Problem Solved

**Issue:** Officials (Chairperson, Secretary, Treasurer, etc.) are also SACCO members, but they couldn't access the member portal to vote on loans.

**Solution:** Implemented view switching that allows officials to toggle between their administrative role and member role.

---

## How It Works

### Concept:
```
Official = Member + Administrative Privileges

Example:
- John Doe is a SACCO member (MEM000001)
- John is also the Chairperson (administrative role)
- John should be able to:
  ✅ Access Chairperson Dashboard (for approvals)
  ✅ Access Member Dashboard (for voting on loans)
```

---

## Implementation Details

### 1. New API Endpoints

#### **Switch View**
```http
POST /api/auth/switch-view?viewMode=MEMBER
POST /api/auth/switch-view?viewMode=ADMIN
```

**What it does:**
- Generates a new JWT token with the selected view mode
- Returns new authentication credentials
- Allows seamless switching

**Request:**
```javascript
POST /api/auth/switch-view?viewMode=MEMBER
Headers: Authorization: Bearer <current-token>
```

**Response:**
```json
{
  "success": true,
  "message": "Switched to MEMBER view",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "viewMode": "MEMBER",
  "userId": "uuid",
  "username": "john.doe",
  "firstName": "John",
  "lastName": "Doe",
  "memberNumber": "MEM000001",
  "role": "CHAIRPERSON"
}
```

#### **Get Available Views**
```http
GET /api/auth/available-views
```

**What it does:**
- Shows which portals the user can access
- Tells if user is both member and official

**Response for Official Member:**
```json
{
  "success": true,
  "canAccessMemberPortal": true,
  "canAccessAdminPortal": true,
  "memberNumber": "MEM000001",
  "role": "CHAIRPERSON",
  "isBothMemberAndOfficial": true,
  "message": "You can access both Member and CHAIRPERSON portals"
}
```

**Response for Regular Member:**
```json
{
  "success": true,
  "canAccessMemberPortal": true,
  "canAccessAdminPortal": false,
  "memberNumber": "MEM000002",
  "role": "MEMBER",
  "isBothMemberAndOfficial": false,
  "message": "You have access to Member portal"
}
```

---

## Frontend Implementation Guide

### 1. Add View Switcher Component

Create a component that shows when user can access multiple views:

```javascript
// ViewSwitcher.jsx
import { useState, useEffect } from 'react';
import api from '../api';

export default function ViewSwitcher() {
    const [availableViews, setAvailableViews] = useState(null);
    const [currentView, setCurrentView] = useState(
        localStorage.getItem('viewMode') || 'ADMIN'
    );

    useEffect(() => {
        checkAvailableViews();
    }, []);

    const checkAvailableViews = async () => {
        try {
            const response = await api.get('/auth/available-views');
            setAvailableViews(response.data);
        } catch (error) {
            console.error('Error checking views:', error);
        }
    };

    const switchView = async (viewMode) => {
        try {
            const response = await api.post(`/auth/switch-view?viewMode=${viewMode}`);
            
            if (response.data.success) {
                // Update token
                localStorage.setItem('token', response.data.token);
                localStorage.setItem('viewMode', viewMode);
                
                // Update current view
                setCurrentView(viewMode);
                
                // Reload to show correct dashboard
                window.location.href = viewMode === 'MEMBER' ? '/dashboard' : `/${viewMode.toLowerCase()}-dashboard`;
            }
        } catch (error) {
            console.error('Error switching view:', error);
            alert('Failed to switch view');
        }
    };

    // Only show if user has access to multiple views
    if (!availableViews?.isBothMemberAndOfficial) {
        return null;
    }

    return (
        <div className="view-switcher bg-white p-4 rounded-lg shadow-md">
            <p className="text-sm text-gray-600 mb-2">Current View:</p>
            <div className="flex gap-2">
                <button
                    onClick={() => switchView('MEMBER')}
                    className={`px-4 py-2 rounded-lg font-medium transition ${
                        currentView === 'MEMBER'
                            ? 'bg-emerald-600 text-white'
                            : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
                    }`}
                >
                    👤 Member View
                </button>
                <button
                    onClick={() => switchView('ADMIN')}
                    className={`px-4 py-2 rounded-lg font-medium transition ${
                        currentView === 'ADMIN'
                            ? 'bg-blue-600 text-white'
                            : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
                    }`}
                >
                    👔 {availableViews.role} View
                </button>
            </div>
            <p className="text-xs text-gray-500 mt-2">
                {currentView === 'MEMBER' 
                    ? '🗳️ You can vote on loans in Member view'
                    : '📋 You can approve/manage in ' + availableViews.role + ' view'
                }
            </p>
        </div>
    );
}
```

### 2. Add to Dashboard Header

```javascript
// DashboardHeader.jsx
import ViewSwitcher from './ViewSwitcher';

export default function DashboardHeader() {
    return (
        <header className="bg-white shadow-sm p-4">
            <div className="flex justify-between items-center">
                <h1>Dashboard</h1>
                
                {/* Add view switcher */}
                <ViewSwitcher />
                
                <UserMenu />
            </div>
        </header>
    );
}
```

### 3. Update Login to Detect Dual Access

```javascript
// Login.jsx
const handleLogin = async (credentials) => {
    try {
        const response = await api.post('/auth/login', credentials);
        
        // Save token
        localStorage.setItem('token', response.data.token);
        localStorage.setItem('user', JSON.stringify(response.data));
        
        // Check available views
        const viewsResponse = await api.get('/auth/available-views');
        
        if (viewsResponse.data.isBothMemberAndOfficial) {
            // Show modal: "You have access to both Member and Admin portals"
            // Let user choose which dashboard to go to
            showViewSelectionModal(viewsResponse.data);
        } else if (viewsResponse.data.canAccessAdminPortal) {
            // Go to admin dashboard
            navigate(`/${response.data.role.toLowerCase()}-dashboard`);
        } else {
            // Go to member dashboard
            navigate('/dashboard');
        }
    } catch (error) {
        console.error('Login failed:', error);
    }
};
```

---

## Use Cases

### Case 1: Chairperson Wants to Vote

**Scenario:**
- Alice is the Chairperson (CHAIRPERSON role)
- Alice is also a member (MEM000005)
- A loan is open for voting
- Alice wants to vote like other members

**Steps:**
1. Alice logs in → Goes to Chairperson Dashboard
2. Alice sees "View Switcher" in header
3. Alice clicks **"Member View"**
4. System switches to Member Dashboard
5. Alice can now vote on loans
6. After voting, Alice clicks **"Chairperson View"**
7. Alice is back to administrative functions

---

### Case 2: Secretary Participating in AGM

**Scenario:**
- Bob is the Secretary
- AGM voting is happening
- Bob needs to vote as a member

**Steps:**
1. Bob switches to "Member View"
2. Bob participates in voting like any member
3. After AGM, Bob switches back to "Secretary View"

---

### Case 3: Regular Member (No Switching)

**Scenario:**
- Carol is a regular member
- She has no administrative role

**Result:**
- Carol doesn't see the View Switcher
- Carol only has Member Dashboard
- Everything works as normal

---

## Voting Implementation

### Update Loan Voting to Check Member Status

```java
// In LoanService.java
public void castVote(UUID loanId, boolean voteYes) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
    
    // Check if user is a member
    if (user.getMemberNumber() == null) {
        throw new RuntimeException("Only members can vote on loans");
    }
    
    // Check if user hasn't voted before
    // Cast vote...
}
```

### Frontend Voting Component

```javascript
// LoanVoting.jsx
const castVote = async (loanId, voteYes) => {
    try {
        // Check if in member view
        const viewMode = localStorage.getItem('viewMode');
        if (viewMode === 'ADMIN') {
            alert('Please switch to Member View to vote on loans');
            return;
        }
        
        await api.post(`/loans/${loanId}/vote`, { voteYes });
        alert('Vote cast successfully!');
    } catch (error) {
        console.error('Voting failed:', error);
    }
};
```

---

## Security Considerations

### 1. Token Claims
- Each token includes `viewMode` claim
- Backend can verify which view user is in
- Prevents unauthorized actions

### 2. Role Validation
```java
// Example: Approve loan (admin only)
if (viewMode.equals("MEMBER")) {
    throw new RuntimeException("Cannot approve loans in Member view. Switch to Admin view.");
}

// Example: Vote on loan (member only)
if (user.getMemberNumber() == null) {
    throw new RuntimeException("Only members can vote");
}
```

### 3. Audit Trail
- Log when users switch views
- Track which view was active during each action

---

## Benefits

### For Officials:
✅ Can participate in voting as regular members  
✅ Seamless switching between roles  
✅ Clear separation of duties  
✅ No need for multiple accounts  

### For SACCO:
✅ Better governance (officials vote like members)  
✅ Transparent voting process  
✅ Proper audit trail  
✅ No privilege abuse  

### For System:
✅ Single authentication system  
✅ Clean architecture  
✅ Easy to maintain  
✅ Secure implementation  

---

## Testing Checklist

### Backend Tests:
- [ ] Official can switch to MEMBER view
- [ ] Official can switch back to ADMIN view
- [ ] Regular member cannot switch to ADMIN view
- [ ] Token includes viewMode claim
- [ ] Available views endpoint returns correct data

### Frontend Tests:
- [ ] View switcher shows for dual-access users
- [ ] View switcher hidden for regular members
- [ ] Switching updates token correctly
- [ ] Dashboard changes after switch
- [ ] Voting works in MEMBER view
- [ ] Admin functions work in ADMIN view

### Integration Tests:
- [ ] Official votes on loan in MEMBER view
- [ ] Official approves loan in ADMIN view
- [ ] Cannot mix actions (vote in ADMIN, approve in MEMBER)

---

## Summary

✅ **Fixed Compilation Errors** - Changed `getRepaymentDate()` to `getExpectedRepaymentDate()`  
✅ **Implemented View Switching** - Officials can access member portal  
✅ **Added API Endpoints** - `/auth/switch-view` and `/auth/available-views`  
✅ **Updated JWT Service** - Support for viewMode in tokens  
✅ **Documented Frontend Integration** - Complete guide for UI implementation  

**Officials can now vote as members while maintaining their administrative privileges!** 🎉

