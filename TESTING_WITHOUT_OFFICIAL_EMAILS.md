# Testing Guide - Login & Voting Without Official Emails ✅

## Problem Solved

1. ✅ **Can't login to admin portals** - No official SACCO emails configured yet
2. ✅ **Applicant voting on own loan** - System now prevents this

---

## Solution 1: Flexible Login (Temporary for Testing)

### What Changed:

**Before:**
- Officials MUST have official email to access admin portal
- Personal email → Member portal only
- Official email → Admin portal only

**Now (Testing Mode):**
- ✅ If `officialEmail` is NULL → Personal email grants admin access
- ✅ Once `officialEmail` is set → Dual login enforced

### How It Works:

```java
// Login Logic (Updated)

if (user.officialEmail == null && user.role != MEMBER) {
    // No official email set yet
    // → Allow admin access with personal email
    isOfficialLogin = true;
}
```

---

## Testing Scenarios

### Scenario 1: Admin User (No Official Email Set)

**User Data:**
```
email: admin@test.com
officialEmail: NULL
role: ADMIN
```

**Login:**
```
Email: admin@test.com
Password: ********
```

**Result:**
- ✅ Logs in successfully
- ✅ `isOfficialLogin = true`
- ✅ Frontend shows Admin Dashboard

---

### Scenario 2: Chairperson (No Official Email Set)

**User Data:**
```
email: john@gmail.com
officialEmail: NULL
role: CHAIRPERSON
memberNumber: MEM000001
```

**Login:**
```
Email: john@gmail.com
Password: ********
```

**Result:**
- ✅ Logs in successfully
- ✅ `isOfficialLogin = true`
- ✅ Frontend shows Chairperson Dashboard
- ✅ Can perform admin duties

**To vote as member:**
- Currently: Can't (will show admin dashboard)
- Solution: Will need to set up official email first

---

### Scenario 3: Secretary (Official Email Set)

**User Data:**
```
email: alice@yahoo.com
officialEmail: secretary@sacco.com
role: SECRETARY
memberNumber: MEM000002
```

**Login as Admin:**
```
Email: secretary@sacco.com
Password: ********
```

**Result:**
- ✅ `isOfficialLogin = true`
- ✅ Shows Secretary Dashboard

**Login as Member:**
```
Email: alice@yahoo.com
Password: ********
```

**Result:**
- ✅ `isMemberLogin = true`
- ✅ Shows Member Dashboard
- ✅ Can vote on loans

---

## Solution 2: Applicant Cannot Vote on Own Loan

### Prevention Strategy:

**1. Notification Exclusion (Primary):**
- ✅ Loan applicant is NOT notified about voting on their own loan
- ✅ Receives special "Your loan is on agenda" notification instead
- ✅ Other members receive voting notification

**2. Backend Validation (Safety Net):**
- ✅ If applicant somehow tries to vote, error is shown
- ✅ "You cannot vote on your own loan application"

### Implementation:

```java
// MeetingService.notifyMembersAboutVoting()

// Get all members
List<Member> allMembers = memberRepository.findAll();

// Get applicant ID
UUID applicantId = agenda.getLoan().getMember().getId();

// Send notifications
for (Member member : allMembers) {
    if (member.getId().equals(applicantId)) {
        // SKIP APPLICANT - Don't send voting notification
        log.info("Skipping voting notification for applicant");
        continue;
    }
    
    // Send voting notification to all other members
    sendVotingNotification(member, agenda);
}
```

### Validation (Safety Net):

```java
// MeetingService.castVote()

// Check 1: Voting is open
if (agenda.status != OPEN_FOR_VOTE) {
    throw new RuntimeException("Voting is not currently open");
}

// Check 2: Haven't voted already
if (hasVotedAlready(agenda, member)) {
    throw new RuntimeException("You have already voted on this agenda");
}

// Check 3: Not voting on own loan ✅ (Safety net)
if (agenda.loan != null && agenda.loan.member.id == memberId) {
    throw new RuntimeException("You cannot vote on your own loan application");
}

// All checks passed → Cast vote
```

### Testing:

**Setup:**
```
Member: John Doe (MEM000001)
Loan: LN12345 (Applied by John Doe)
Meeting: MTG-001
Agenda: Loan Application - John Doe
Status: OPEN_FOR_VOTE
```

**Test 1: Applicant notification check**
```
Member: John Doe (MEM000001)
Action: Check notifications when voting opens
```

**Result:**
```
✅ No voting notification received
✅ Received: "Your loan application is on the agenda. You will be notified of results."
✅ Cannot see voting interface in member portal
```

**Test 2: Other member notification**
```
Member: Alice Smith (MEM000002)
Action: Check notifications when voting opens
```

**Result:**
```
✅ Voting notification received
✅ "Voting Now Open - Loan Application - John Doe"
✅ Can vote in member portal
```

**Test 3: Applicant tries to vote (edge case)**
```
Member: John Doe (MEM000001)
Action: Directly access voting API
```

**Result:**
```
❌ Error: "You cannot vote on your own loan application"
(Safety net validation)
```

**Test 4: Other member votes**
```
Member: Alice Smith (MEM000002)
Action: Cast vote on Agenda (Loan LN12345)
Vote: YES
```

**Result:**
```
✅ Success: Vote recorded
```

---

## Current Testing Workflow

### For Officials Without Official Emails:

**1. Login:**
```
Email: [your-personal-email]
Password: [your-password]
```

**2. System Behavior:**
- ✅ Checks if `officialEmail` is NULL
- ✅ Since it's NULL, grants admin access
- ✅ Returns `isOfficialLogin = true`
- ✅ Frontend shows admin dashboard

**3. You Can:**
- ✅ Access admin portal
- ✅ Perform administrative duties
- ✅ Approve loans
- ✅ Manage members
- ⚠️ Cannot vote as member (until official email is set)

---

## Setting Up Official Emails (When Ready)

### Step 1: Admin Assigns Official Email

**Endpoint:**
```http
POST /api/admin/official-emails/assign
{
  "userId": "uuid-of-official",
  "officialEmail": "chairperson@yoursacco.com"
}
```

### Step 2: Test Dual Login

**Admin Access:**
```
Email: chairperson@yoursacco.com
Password: [same-password]
→ Admin Dashboard
```

**Member Access:**
```
Email: john@gmail.com
Password: [same-password]
→ Member Dashboard (can vote!)
```

---

## Voting Restrictions Summary

### ✅ Can Vote:
- Member who is NOT the loan applicant
- Member logged in with personal email (member portal)
- Voting is currently open
- Haven't voted on this agenda already

### ❌ Cannot Vote:
- ⛔ Loan applicant on their own loan
- ⛔ Already voted on this agenda
- ⛔ Voting is closed
- ⛔ Logged in with official email (admin portal)
- ⛔ Not a member (no member number)

---

## Frontend Implementation Guide

### Login Response Handling:

```javascript
// Login.jsx
const handleLogin = async (email, password) => {
    const response = await api.post('/auth/login', {
        username: email,
        password: password
    });

    const { data } = response;

    // Store token and user data
    localStorage.setItem('token', data.token);
    localStorage.setItem('user', JSON.stringify(data));

    // Determine which dashboard to show
    if (data.isOfficialLogin) {
        // Admin portal
        const dashboard = `/${data.role.toLowerCase()}-dashboard`;
        navigate(dashboard);
    } else if (data.isMemberLogin) {
        // Member portal
        navigate('/dashboard');
    } else {
        // Fallback
        navigate('/dashboard');
    }
};
```

### Voting UI - Hide Vote Button for Applicant:

```javascript
// LoanVotingCard.jsx
const LoanVotingCard = ({ agenda, currentMember }) => {
    const isOwnLoan = agenda.loan?.member?.id === currentMember.id;

    if (isOwnLoan) {
        return (
            <div className="alert alert-info">
                <p>This is your loan application.</p>
                <p>You cannot vote on your own loan.</p>
            </div>
        );
    }

    return (
        <div className="voting-card">
            <h3>{agenda.agendaTitle}</h3>
            <p>Loan Amount: KES {agenda.loan?.principalAmount}</p>
            
            <div className="vote-buttons">
                <button onClick={() => vote('YES')}>✅ YES</button>
                <button onClick={() => vote('NO')}>❌ NO</button>
                <button onClick={() => vote('ABSTAIN')}>⚪ ABSTAIN</button>
            </div>
        </div>
    );
};
```

---

## Migration Path

### Current State (No Official Emails):
```
✅ All users login with personal email
✅ Officials see admin dashboards
✅ Members see member dashboards
⚠️ Officials can't vote as members yet
```

### Future State (With Official Emails):
```
✅ Officials login with official email → Admin portal
✅ Officials login with personal email → Member portal
✅ Officials can vote on loans (as members)
✅ Clear separation of duties
✅ Better audit trail
```

### When to Set Up Official Emails:
- When you have a domain (e.g., yoursacco.com)
- When you want officials to vote as members
- When you need stricter separation of duties
- When required for compliance/auditing

### For Now:
- ✅ Keep testing with personal emails
- ✅ Officials can access admin portals
- ✅ System works fully except dual-role voting
- ✅ Can set up official emails later without code changes

---

## Quick Reference

### Login Behavior Matrix:

| User Role | Email Type | Official Email Set? | Dashboard Shown | Can Vote? |
|-----------|-----------|---------------------|-----------------|-----------|
| ADMIN | Personal | No | Admin | No* |
| ADMIN | Personal | Yes | Member | No (not a member) |
| ADMIN | Official | Yes | Admin | No* |
| CHAIRPERSON | Personal | No | Chairperson | No* |
| CHAIRPERSON | Personal | Yes | Member | Yes ✅ |
| CHAIRPERSON | Official | Yes | Chairperson | No* |
| MEMBER | Personal | N/A | Member | Yes ✅ |

*Not a member = no member number

---

## Testing Checklist

### Login Testing:
- [ ] Admin logs in with personal email → Admin dashboard
- [ ] Chairperson logs in with personal email → Chairperson dashboard
- [ ] Secretary logs in with personal email → Secretary dashboard
- [ ] Treasurer logs in with personal email → Treasurer dashboard
- [ ] Member logs in with personal email → Member dashboard

### Voting Testing:
- [ ] Member can vote on other member's loan
- [ ] Member CANNOT vote on own loan (error shown)
- [ ] Member cannot vote twice on same agenda
- [ ] Member cannot vote when voting is closed
- [ ] Vote is recorded correctly (YES/NO/ABSTAIN)

### Official Email Testing (When Ready):
- [ ] Assign official email to user
- [ ] Login with official email → Admin dashboard
- [ ] Login with personal email → Member dashboard
- [ ] Can vote in member portal
- [ ] Cannot vote in admin portal

---

## Summary

✅ **Login Issue Fixed:**
- Officials can now login with personal email (admin access granted)
- No official email required for testing
- Can set up official emails later without breaking anything

✅ **Voting Restriction Enforced:**
- Applicants CANNOT vote on their own loans
- Error message: "You cannot vote on your own loan application"
- Backend validation prevents this completely

✅ **Testing Ready:**
- You can test all admin features now
- Use personal emails for all logins
- Set up official emails when ready
- System will work both ways!

**You're all set to test the complete meeting and voting workflow!** 🎉

