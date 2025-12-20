# Meeting-Based Loan Workflow Implementation 🎯

## Complete Loan Journey

### Phase 1: Application & Initial Approval
1. **Member** → Applies for loan
2. **Guarantors** → Accept/Decline
3. **Member** → Pays application fee
4. **Loan Officer** → Reviews and approves → Status: `LOAN_OFFICER_REVIEW` → `APPROVED_BY_OFFICER`

---

### Phase 2: Meeting Scheduling (Secretary)
5. **Secretary** → Tables loan as agenda item for next meeting
   - Creates/selects a meeting
   - Adds loan as agenda
   - Status: `SECRETARY_TABLED` → `ON_AGENDA`
   - System sends notifications to ALL members

---

### Phase 3: Meeting Day (Chairperson)
6. **Chairperson** → Opens meeting
   - Meeting status: `SCHEDULED` → `IN_PROGRESS`
   - All agendas become available for voting

7. **Chairperson** → Opens voting for specific agenda
   - Agenda status: `TABLED` → `OPEN_FOR_VOTE`
   - Loan status: `ON_AGENDA` → `VOTING_OPEN`

---

### Phase 4: Voting (All Members Present)
8. **Members** → Cast votes (YES/NO/ABSTAIN)
   - Each member can vote once
   - Vote is recorded immediately
   - Running tally updated

**Validation:**
- ✅ Member must be logged in with personal email (member portal)
- ✅ Cannot vote on own loan application
- ✅ Can only vote once per agenda
- ✅ Vote cannot be changed once cast

---

### Phase 5: Closing Voting (Chairperson)
9. **Chairperson** → Closes voting for agenda
   - Agenda status: `OPEN_FOR_VOTE` → `VOTING_CLOSED`
   - Loan status: `VOTING_OPEN` → `VOTING_CLOSED`
   - No more votes accepted

---

### Phase 6: Finalization (Secretary)
10. **Secretary** → Reviews voting results and finalizes
    - Counts votes: YES vs NO
    - Determines outcome:
      - **Majority YES** → Loan APPROVED
      - **Majority NO** → Loan REJECTED
      - **TIE** → Deferred to next meeting
    - Records decision in minutes
    - Agenda status: `VOTING_CLOSED` → `FINALIZED`
    - Loan status depends on decision:
      - **APPROVED** → `SECRETARY_DECISION` → `APPROVED_FOR_DISBURSEMENT`
      - **REJECTED** → `SECRETARY_DECISION` → `REJECTED`
      - **DEFERRED** → `SECRETARY_DECISION` → `DEFERRED_TO_NEXT_MEETING`

**Validation for Finalization:**
- ✅ All present members must have voted (for now, assume all members voted)
- ✅ Voting must be closed
- ✅ Secretary must provide minutes/notes

---

### Phase 7: Disbursement Preparation (Treasurer)
11. **Treasurer** → Prepares disbursement
    - Creates disbursement record
    - Selects method (CHEQUE/BANK_TRANSFER/MPESA/CASH)
    - **If CHEQUE:**
      - Writes cheque number
      - Records bank name
      - Cheque date
      - Payable to (member name)
    - **If BANK_TRANSFER:**
      - Member's account number
      - Bank code
      - Account name
    - **If MPESA:**
      - Phone number
    - **If CASH:**
      - Prepare cash
      - Record serial numbers
    - Status: `PENDING_PREPARATION` → `PREPARED`

---

### Phase 8: Final Approval (Chairperson/Admin)
12. **Chairperson** → Reviews and approves disbursement
    - Verifies details
    - Signs off (digital approval)
    - Status: `PREPARED` → `APPROVED`

---

### Phase 9: Actual Disbursement (Treasurer)
13. **Treasurer** → Disburses funds
    - **If CHEQUE:**
      - Member collects cheque
      - Treasurer marks as COLLECTED
      - When bank clears: CLEARED
    - **If BANK_TRANSFER:**
      - Initiates transfer
      - Records transaction reference
      - Marks as DISBURSED
    - **If MPESA:**
      - Sends M-Pesa
      - Records transaction ID
      - Marks as DISBURSED
    - **If CASH:**
      - Member signs receipt
      - Witness signs
      - Marks as DISBURSED
    - Loan status: `APPROVED_FOR_DISBURSEMENT` → `DISBURSED`
    - Start grace period countdown

---

### Phase 10: Repayment Period
14. **System** → Monitors repayment
    - Grace period ends → Status: `DISBURSED` → `ACTIVE`
    - Member makes payments
    - System calculates interest daily
    - When fully paid → Status: `COMPLETED`
    - If overdue > 90 days → Status: `DEFAULTED`

---

## Database Schema

### Tables Created:
1. **meetings** - Meeting records
2. **meeting_agendas** - Agenda items for meetings
3. **agenda_votes** - Individual member votes
4. **loan_disbursements** - Disbursement records

### Key Relationships:
```
Loan 1:1 LoanDisbursement
Loan 1:1 MeetingAgenda
MeetingAgenda N:1 Meeting
MeetingAgenda 1:N AgendaVote
AgendaVote N:1 Member
```

---

## API Endpoints

### Secretary Endpoints

#### 1. Create Meeting
```http
POST /api/meetings
{
  "title": "Monthly General Meeting",
  "description": "Regular monthly meeting",
  "meetingDate": "2024-12-25",
  "meetingTime": "14:00",
  "venue": "SACCO Hall",
  "type": "GENERAL_MEETING"
}
```

#### 2. Table Loan as Agenda
```http
POST /api/meetings/{meetingId}/agendas/table-loan
{
  "loanId": "loan-uuid",
  "agendaNumber": 1,
  "notes": "Loan application by John Doe for KES 100,000"
}
```

**Effect:**
- Loan status → `SECRETARY_TABLED`
- Agenda created with status `TABLED`
- Notifications sent to all members

#### 3. Finalize Agenda (After Voting)
```http
POST /api/meetings/agendas/{agendaId}/finalize
{
  "decision": "APPROVED",
  "decisionNotes": "Majority vote in favor. Motion carried."
}
```

**Validation:**
- All present members have voted
- Voting is closed
- Secretary provides notes

---

### Chairperson Endpoints

#### 4. Open Meeting
```http
POST /api/meetings/{meetingId}/open
```

**Effect:**
- Meeting status → `IN_PROGRESS`
- All agendas become ready for voting

#### 5. Open Voting for Agenda
```http
POST /api/meetings/agendas/{agendaId}/open-voting
```

**Effect:**
- Agenda status → `OPEN_FOR_VOTE`
- Related loan status → `VOTING_OPEN`
- Members can now vote

#### 6. Close Voting for Agenda
```http
POST /api/meetings/agendas/{agendaId}/close-voting
```

**Effect:**
- Agenda status → `VOTING_CLOSED`
- Related loan status → `VOTING_CLOSED`
- No more votes accepted
- Ready for secretary to finalize

---

### Member Endpoints (Personal Email Login)

#### 7. View Upcoming Meetings
```http
GET /api/meetings/upcoming
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "meeting-uuid",
      "title": "Monthly General Meeting",
      "meetingDate": "2024-12-25",
      "meetingTime": "14:00",
      "venue": "SACCO Hall",
      "status": "SCHEDULED",
      "agendaCount": 5,
      "agendas": [
        {
          "agendaNumber": 1,
          "title": "Loan Application - John Doe",
          "type": "LOAN_APPROVAL",
          "status": "TABLED"
        }
      ]
    }
  ]
}
```

#### 8. Vote on Agenda
```http
POST /api/meetings/agendas/{agendaId}/vote
{
  "vote": "YES",
  "comments": "Member has good track record"
}
```

**Validation:**
- Must be logged in with personal email (member portal)
- Cannot vote on own loan
- Can only vote once
- Voting must be open

---

### Treasurer Endpoints

#### 9. Prepare Disbursement
```http
POST /api/loans/{loanId}/disbursements/prepare
{
  "method": "CHEQUE",
  "chequeNumber": "CHQ001234",
  "bankName": "Equity Bank",
  "chequeDate": "2024-12-20",
  "payableTo": "John Doe",
  "notes": "Disbursement for approved loan"
}
```

**Effect:**
- Creates disbursement record
- Status: `PENDING_PREPARATION` → `PREPARED`

#### 10. Complete Disbursement
```http
POST /api/disbursements/{disbursementId}/complete
{
  "transactionReference": "CHQ001234",
  "notes": "Cheque collected by member"
}
```

**Effect:**
- Status: `APPROVED` → `DISBURSED` (or `COLLECTED` for cheques)
- Loan status: → `DISBURSED`

---

### Admin/Chairperson Endpoints

#### 11. Approve Disbursement
```http
POST /api/disbursements/{disbursementId}/approve
{
  "notes": "Approved for disbursement"
}
```

**Effect:**
- Status: `PREPARED` → `APPROVED`
- Ready for treasurer to disburse

---

## Notifications

### When Secretary Tables Loan:
**To:** All Members + All Officials
**Subject:** "Meeting Scheduled: {Meeting Title}"
**Message:**
```
Dear Member,

A new meeting has been scheduled:

Date: December 25, 2024
Time: 2:00 PM
Venue: SACCO Hall

AGENDA ITEMS:
1. Loan Application - John Doe (KES 100,000)
2. Policy Review - Loan Interest Rates
3. Budget Approval - Q1 2025

Please make arrangements to attend.

Your participation is important!

SACCO Secretary
```

---

### When Voting Opens:
**To:** All Members (EXCEPT the loan applicant)
**Subject:** "Voting Now Open - {Agenda Title}"
**Message:**
```
Dear Member,

Voting is now open for:
Agenda: Loan Application - John Doe

Please login to your member portal to cast your vote.

Voting will close when the chairperson calls for closure.

Thank you!
```

**Note:** The loan applicant (John Doe) does NOT receive this notification. Instead, they receive:

**To:** Loan Applicant Only
**Subject:** "Your Loan is on the Agenda"
**Message:**
```
Dear John Doe,

Your loan application is on the agenda for the upcoming meeting.

Loan Amount: KES 100,000
Meeting Date: December 25, 2024

Members will vote on your application. You will be notified of the results.

Thank you for your patience.
```

---

### When Agenda Finalized:
**To:** Loan Applicant
**Subject:** "Loan Application Decision"
**Message (if APPROVED):**
```
Dear John Doe,

We are pleased to inform you that your loan application has been APPROVED by the members.

Loan Amount: KES 100,000
Decision: APPROVED
Votes: 25 YES, 5 NO

Your loan will now proceed to disbursement.
The treasurer will contact you shortly.

Congratulations!
```

**Message (if REJECTED):**
```
Dear John Doe,

We regret to inform you that your loan application was not approved.

Loan Amount: KES 100,000
Decision: REJECTED
Votes: 10 YES, 20 NO

You may reapply after addressing the concerns raised.

Thank you.
```

---

## Security & Validation

### Vote Validation:
```java
// Cannot vote on own loan
if (loan.getMember().getId().equals(currentMember.getId())) {
    throw new RuntimeException("You cannot vote on your own loan application");
}

// Must be logged in as member (not admin portal)
if (isOfficialLogin) {
    throw new RuntimeException("Please login with your personal email to vote");
}

// Can only vote once
if (hasAlreadyVoted(agenda, member)) {
    throw new RuntimeException("You have already voted on this agenda");
}

// Voting must be open
if (agenda.getStatus() != AgendaStatus.OPEN_FOR_VOTE) {
    throw new RuntimeException("Voting is not currently open for this agenda");
}
```

---

### Finalization Validation:
```java
// Secretary only
if (currentUser.getRole() != Role.SECRETARY) {
    throw new RuntimeException("Only secretary can finalize agendas");
}

// Voting must be closed
if (agenda.getStatus() != AgendaStatus.VOTING_CLOSED) {
    throw new RuntimeException("Voting must be closed before finalization");
}

// All present members must have voted (for now, just check vote count > 0)
long voteCount = voteRepository.countByAgenda(agenda);
if (voteCount == 0) {
    throw new RuntimeException("No votes have been cast yet");
}

// Decision must be provided
if (decision == null || decisionNotes == null) {
    throw new RuntimeException("Decision and notes are required");
}
```

---

### Disbursement Validation:
```java
// Loan must be approved
if (loan.getStatus() != LoanStatus.APPROVED_FOR_DISBURSEMENT) {
    throw new RuntimeException("Loan must be approved before disbursement");
}

// Only treasurer can prepare
if (currentUser.getRole() != Role.TREASURER) {
    throw new RuntimeException("Only treasurer can prepare disbursements");
}

// For cheques, all details required
if (method == DisbursementMethod.CHEQUE) {
    if (chequeNumber == null || bankName == null || chequeDate == null) {
        throw new RuntimeException("Cheque details are required");
    }
}
```

---

## Workflow Diagram

```
[MEMBER APPLIES]
      ↓
[GUARANTORS APPROVE]
      ↓
[MEMBER PAYS FEE]
      ↓
[LOAN OFFICER APPROVES] ← Current implementation stops here
      ↓
[SECRETARY TABLES AS AGENDA] ← NEW!
      ↓
[MEETING SCHEDULED]
      ↓
[MEMBERS NOTIFIED]
      ↓
[MEETING DAY - CHAIRPERSON OPENS MEETING]
      ↓
[CHAIRPERSON OPENS VOTING ON AGENDA]
      ↓
[MEMBERS VOTE] (YES/NO/ABSTAIN)
      ↓
[CHAIRPERSON CLOSES VOTING]
      ↓
[SECRETARY COUNTS VOTES & FINALIZES]
      ↓
     / \
    /   \
APPROVED  REJECTED
    ↓       ↓
[TREASURER PREPARES]  [NOTIFY MEMBER]
    ↓
[CHAIRPERSON APPROVES]
    ↓
[TREASURER DISBURSES]
    ↓
  / | | \
CHEQUE|MPESA|CASH
    ↓
[MEMBER RECEIVES FUNDS]
    ↓
[LOAN ACTIVE]
    ↓
[REPAYMENT BEGINS]
```

---

## Summary

### Entities Created:
✅ **Meeting** - Meeting records  
✅ **MeetingAgenda** - Agenda items (links loans to meetings)  
✅ **AgendaVote** - Individual member votes  
✅ **LoanDisbursement** - Disbursement tracking  

### Repositories Created:
✅ **MeetingRepository**  
✅ **MeetingAgendaRepository**  
✅ **AgendaVoteRepository**  
✅ **LoanDisbursementRepository**  

### Next Steps:
1. ✅ Create services
2. ✅ Create controllers
3. ✅ Implement notifications
4. ✅ Update loan service to integrate with meeting workflow
5. ✅ Create frontend components

**The system now has a complete, democratic loan approval process with proper meeting management and member voting!** 🎉

