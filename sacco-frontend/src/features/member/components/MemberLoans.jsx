import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { useSettings } from '../../../context/SettingsContext';
import { AlertCircle, Loader2, ArrowRight, Play } from 'lucide-react';

// Widgets
import ActiveLoanCard from '../../loans/components/dashboard/ActiveLoanCard';
import LoanVotingWidget from '../../loans/components/dashboard/LoanVotingWidget';
import LoanEligibilityWidget from '../../loans/components/dashboard/LoanEligibilityWidget';
import DraftLoanWidget from '../../loans/components/dashboard/DraftLoanWidget';
import LoanHistoryList from '../../loans/components/dashboard/LoanHistoryList';

// Modals
import LoanApplicationModal from '../../loans/components/dashboard/LoanApplicationModal';
import LoanFeePaymentModal from './LoanFeePaymentModal';

export default function MemberLoans({ user, onUpdate, onVoteCast }) {
    const { settings } = useSettings();
    const [loans, setLoans] = useState([]);
    const [activeVotes, setActiveVotes] = useState([]);

    // STATE: Tracks if a draft exists (Phase 1: Details/Fee)
    const [activeDraft, setActiveDraft] = useState(null);

    // STATE: Tracks a loan being resumed (Phase 2: Guarantors)
    const [resumeLoan, setResumeLoan] = useState(null);

    const [loading, setLoading] = useState(true);
    const [eligibilityData, setEligibilityData] = useState(null);
    const [error, setError] = useState(null);

    // Modal States
    const [isApplyModalOpen, setIsApplyModalOpen] = useState(false);
    const [isPayFeeModalOpen, setIsPayFeeModalOpen] = useState(false);

    // Used for viewing history details (Read Only)
    const [selectedHistoryLoan, setSelectedHistoryLoan] = useState(null);

    useEffect(() => {
        loadDashboardData();
    }, []);

    const loadDashboardData = async () => {
        setLoading(true);
        setError(null);
        try {
            const [loansRes, eligRes, votesRes, draftRes] = await Promise.all([
                api.get('/api/loans/my-loans').catch(e => ({ error: e })),
                api.get('/api/loans/eligibility/check').catch(e => ({ error: e })),
                api.get('/api/loans/voting/active').catch(e => ({ error: e })),
                api.get('/api/loans/draft').catch(e => ({ error: e }))
            ]);

            if (loansRes.data?.success) setLoans(loansRes.data.data);
            if (eligRes.data?.success) setEligibilityData(eligRes.data.data);
            if (votesRes.data?.success) setActiveVotes(votesRes.data.data);
            if (draftRes.data?.success) setActiveDraft(draftRes.data.data);

        } catch (e) {
            console.error("Failed to load dashboard data", e);
            setError("System connection error.");
        } finally {
            setLoading(false);
        }
    };

    const handleCastVote = async (loanId, voteYes) => {
        if(!window.confirm(`Confirm Vote: ${voteYes ? "YES (Approve)" : "NO (Reject)"}?`)) return;
        try {
            await api.post(`/api/loans/${loanId}/vote`, { vote: voteYes });
            alert("Vote Cast Successfully!");
            setActiveVotes(prev => prev.filter(l => l.id !== loanId));
            if(onVoteCast) onVoteCast();
        } catch (e) {
            alert(e.response?.data?.message || "Failed to cast vote.");
        }
    };

    const handleStartApplication = async () => {
        setResumeLoan(null); // Clear resume state
        setSelectedHistoryLoan(null);
        try {
            const res = await api.post('/api/loans/start');
            if (res.data.success) {
                const newDraft = res.data.data;
                setActiveDraft(newDraft);
                setIsPayFeeModalOpen(true);
            }
        } catch (e) {
            alert(e.response?.data?.message || "Failed to start application. Please check eligibility.");
        }
    };

    // Phase 1 Resume (Draft)
    const handleResumeDraft = () => {
        setResumeLoan(null); // Ensure we aren't resuming Step 2
        if (!activeDraft) return;

        if (activeDraft.status === 'PENDING_FEE') {
            setIsPayFeeModalOpen(true);
        } else if (activeDraft.status === 'FEE_PAID') {
            setIsApplyModalOpen(true);
        }
    };

    // ✅ Phase 2 Resume (Pending Guarantors)
    const handleResumeIncompleteLoan = (loan) => {
        setResumeLoan(loan); // Set the loan to be passed to the modal
        setIsApplyModalOpen(true); // Open modal (it will auto-jump to Step 2)
    };

    const handleFeeSuccess = () => {
        setIsPayFeeModalOpen(false);
        loadDashboardData().then(() => {
            setIsApplyModalOpen(true);
        });
    };

    // --- LOGIC: FILTER LOANS ---

    // ✅ FIX: Use 'loanStatus' (DTO field) instead of 'status'
    // 1. Loan In Progress (Needs Guarantors)
    const loanInProgress = loans.find(l => (l.loanStatus || l.status) === 'PENDING_GUARANTORS');

    // 2. Truly Active Loan (Running/Disbursed)
    const activeLoan = loans.find(l => ['ACTIVE', 'IN_ARREARS', 'DISBURSED'].includes(l.loanStatus || l.status));

    // 3. Submitted Loans (Waiting for approval)
    const submittedLoans = loans.filter(l => (l.loanStatus || l.status) === 'SUBMITTED');


    if (loading) return <div className="p-10 text-center text-slate-400 font-bold animate-pulse"><Loader2 className="animate-spin mx-auto mb-2"/> Loading Dashboard...</div>;

    return (
        <div className="space-y-8 animate-in fade-in duration-500">

            {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 p-4 rounded-xl flex items-center gap-3">
                    <AlertCircle size={20} />
                    <p className="font-bold text-sm">{error}</p>
                </div>
            )}

            {/* --- 1. PRIMARY STATUS AREA (Mutually Exclusive) --- */}

            {loanInProgress ? (
                // CASE A: Loan Created but needs Guarantors (Step 2)
                <div className="bg-orange-50 border border-orange-200 rounded-2xl p-6 shadow-sm flex flex-col md:flex-row justify-between items-center gap-4">
                    <div>
                        <h3 className="font-bold text-orange-900 text-lg flex items-center gap-2">
                            <AlertCircle size={20}/> Application Incomplete
                        </h3>
                        <p className="text-orange-700 mt-1">
                            Your application for <strong>{loanInProgress.productName || loanInProgress.product?.productName}</strong> is pending guarantors.
                        </p>
                    </div>
                    <button
                        onClick={() => handleResumeIncompleteLoan(loanInProgress)}
                        className="bg-orange-600 hover:bg-orange-700 text-white px-6 py-3 rounded-xl font-bold flex items-center gap-2 transition shadow-md active:scale-95 whitespace-nowrap"
                    >
                        Resume Application <Play size={18} fill="currentColor"/>
                    </button>
                </div>
            ) : activeLoan ? (
                // CASE B: Active Loan exists -> Show Status Card
                <ActiveLoanCard loan={activeLoan} />
            ) : activeDraft ? (
                // CASE C: Draft exists (Step 1) -> Show Draft Widget
                <DraftLoanWidget
                    draftLoan={activeDraft}
                    onResume={handleResumeDraft}
                />
            ) : (
                // CASE D: Nothing exists -> Show Eligibility & Start Button
                <LoanEligibilityWidget
                    eligibilityData={eligibilityData}
                    settings={settings}
                    onApply={handleStartApplication}
                />
            )}

            {/* --- 2. SUBMITTED LOANS (Pending Approval) --- */}
            {submittedLoans.length > 0 && (
                <div className="bg-blue-50 border border-blue-100 rounded-2xl p-6">
                    <h3 className="font-bold text-blue-900 mb-4 flex items-center gap-2">
                        <Loader2 size={18} className="animate-spin"/> Pending Approval
                    </h3>
                    <div className="space-y-3">
                        {submittedLoans.map(loan => (
                            <div key={loan.id} className="bg-white p-4 rounded-xl border border-blue-200 flex justify-between items-center">
                                <div>
                                    <p className="font-bold text-slate-800">{loan.productName || loan.product?.productName}</p>
                                    <p className="text-sm text-slate-500">Amount: KES {Number(loan.principalAmount).toLocaleString()}</p>
                                </div>
                                <span className="bg-blue-100 text-blue-700 text-xs font-bold px-3 py-1 rounded-full">
                                    Submitted
                                </span>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* --- 3. SECONDARY WIDGETS --- */}
            <LoanVotingWidget activeVotes={activeVotes} onVote={handleCastVote} />

            <LoanHistoryList
                loans={loans}
                onSelect={(loan) => {
                    setSelectedHistoryLoan(loan);
                    setIsApplyModalOpen(true);
                }}
            />

            {/* --- MODALS --- */}

            <LoanFeePaymentModal
                isOpen={isPayFeeModalOpen}
                onClose={() => setIsPayFeeModalOpen(false)}
                onSuccess={handleFeeSuccess}
                loan={activeDraft}
            />

            <LoanApplicationModal
                isOpen={isApplyModalOpen}
                onClose={() => {
                    setIsApplyModalOpen(false);
                    setResumeLoan(null);        // ✅ Clear resume state on close
                    setSelectedHistoryLoan(null); // ✅ Clear history selection on close
                    loadDashboardData();
                }}
                onSuccess={() => {
                    setIsApplyModalOpen(false);
                    loadDashboardData();
                }}
                draft={activeDraft}
                // ✅ CRITICAL FIX: Pass the history loan if resumeLoan is empty
                existingLoan={resumeLoan || selectedHistoryLoan}
            />
        </div>
    );
}