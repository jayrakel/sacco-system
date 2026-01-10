import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { useSettings } from '../../../context/SettingsContext';
import { AlertCircle, Loader2 } from 'lucide-react';

// Widgets
import ActiveLoanCard from '../../loans/components/dashboard/ActiveLoanCard';
import LoanVotingWidget from '../../loans/components/dashboard/LoanVotingWidget';
import LoanEligibilityWidget from '../../loans/components/dashboard/LoanEligibilityWidget';
import DraftLoanWidget from '../../loans/components/dashboard/DraftLoanWidget'; // ✅ New Widget
import LoanHistoryList from '../../loans/components/dashboard/LoanHistoryList';

// Modals
import LoanApplicationModal from '../../loans/components/dashboard/LoanApplicationModal';
import LoanFeePaymentModal from './LoanFeePaymentModal';

export default function MemberLoans({ user, onUpdate, onVoteCast }) {
    const { settings } = useSettings();
    const [loans, setLoans] = useState([]);
    const [activeVotes, setActiveVotes] = useState([]);

    // ✅ STATE: Tracks if a draft exists (Temporal State)
    const [activeDraft, setActiveDraft] = useState(null);

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
            // ✅ READ-ONLY: Check if draft exists, but DO NOT create one yet.
            const [loansRes, eligRes, votesRes, draftRes] = await Promise.all([
                api.get('/api/loans/my-loans').catch(e => ({ error: e })),
                api.get('/api/loans/eligibility/check').catch(e => ({ error: e })),
                api.get('/api/loans/voting/active').catch(e => ({ error: e })),
                api.get('/api/loans/draft').catch(e => ({ error: e }))
            ]);

            if (loansRes.data?.success) setLoans(loansRes.data.data);
            if (eligRes.data?.success) setEligibilityData(eligRes.data.data);
            if (votesRes.data?.success) setActiveVotes(votesRes.data.data);

            // If a draft exists, load it. If not, this remains null.
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

    // ✅ ACTION: User Clicks "Start Application"
    // This creates the Draft Entity and switches the view
    const handleStartApplication = async () => {
        setSelectedHistoryLoan(null);
        try {
            // 1. Create the Draft (Backend Write)
            const res = await api.post('/api/loans/start');

            if (res.data.success) {
                const newDraft = res.data.data;
                setActiveDraft(newDraft); // Switch UI to Draft Mode

                // 2. Immediately Prompt for Fee
                setIsPayFeeModalOpen(true);
            }
        } catch (e) {
            alert(e.response?.data?.message || "Failed to start application. Please check eligibility.");
        }
    };

    // ✅ ACTION: User Clicks "Resume" on the Draft Widget
    const handleResumeDraft = () => {
        if (!activeDraft) return;

        if (activeDraft.status === 'PENDING_FEE') {
            setIsPayFeeModalOpen(true);
        } else if (activeDraft.status === 'FEE_PAID') {
            setIsApplyModalOpen(true);
        }
    };

    // ✅ CALLBACK: Fee Paid Successfully
    const handleFeeSuccess = () => {
        setIsPayFeeModalOpen(false);

        // Reload to update draft status to FEE_PAID, then open form
        loadDashboardData().then(() => {
            setIsApplyModalOpen(true);
        });
    };

    // Determine Primary View
    const activeLoan = loans.find(l => ['PENDING_GUARANTORS', 'ACTIVE', 'IN_ARREARS', 'DISBURSED'].includes(l.status));

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

            {activeLoan ? (
                // CASE A: Active Loan exists -> Show Status Card
                <ActiveLoanCard loan={activeLoan} />
            ) : activeDraft ? (
                // CASE B: Draft exists -> Show Draft Widget (Resume/Pay)
                <DraftLoanWidget
                    draftLoan={activeDraft}
                    onResume={handleResumeDraft}
                />
            ) : (
                // CASE C: Nothing exists -> Show Eligibility & Start Button
                <LoanEligibilityWidget
                    eligibilityData={eligibilityData}
                    settings={settings}
                    onApply={handleStartApplication} // Triggers Draft Creation
                />
            )}

            {/* --- 2. SECONDARY WIDGETS --- */}
            <LoanVotingWidget activeVotes={activeVotes} onVote={handleCastVote} />

            <LoanHistoryList
                loans={loans}
                onSelect={(loan) => {
                    setSelectedHistoryLoan(loan); // View old loan
                    setIsApplyModalOpen(true);
                }}
            />

            {/* --- MODALS --- */}

            {/* 1. Fee Payment (Uses Active Draft) */}
            <LoanFeePaymentModal
                isOpen={isPayFeeModalOpen}
                onClose={() => setIsPayFeeModalOpen(false)}
                onSuccess={handleFeeSuccess}
                loan={activeDraft} // Passes the temporal draft
            />

            {/* 2. Application Form (Uses Paid Draft OR History Loan) */}
            <LoanApplicationModal
                isOpen={isApplyModalOpen}
                onClose={() => {
                    setIsApplyModalOpen(false);
                    loadDashboardData();
                }}
                draft={activeDraft} // Pass draft for conversion
                resumeLoan={selectedHistoryLoan} // Pass history if viewing old
            />
        </div>
    );
}