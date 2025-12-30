import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { useSettings } from '../../../context/SettingsContext';
import { AlertCircle, CreditCard, Loader2, Plus } from 'lucide-react';

// ✅ CORRECTED IMPORTS: Widgets are in the loans dashboard
import ActiveLoanCard from '../../loans/components/dashboard/ActiveLoanCard';
import LoanVotingWidget from '../../loans/components/dashboard/LoanVotingWidget';
import LoanEligibilityWidget from '../../loans/components/dashboard/LoanEligibilityWidget';
import LoanHistoryList from '../../loans/components/dashboard/LoanHistoryList';

// ✅ CORRECTED IMPORTS: Modals are also in the loans dashboard
import LoanApplicationModal from '../../loans/components/dashboard/LoanApplicationModal';
// We use the dashboard Fee Modal as it contains the logic we built earlier
import LoanFeePaymentModal from '../../loans/components/dashboard/LoanFeePaymentModal';

export default function MemberLoans({ user, onUpdate, onVoteCast }) {
    const { settings } = useSettings();
    const [loans, setLoans] = useState([]);
    const [activeVotes, setActiveVotes] = useState([]);
    const [loading, setLoading] = useState(true);
    const [eligibilityData, setEligibilityData] = useState(null);
    const [error, setError] = useState(null);

    // Modal States
    const [isApplyModalOpen, setIsApplyModalOpen] = useState(false);
    const [isPayFeeModalOpen, setIsPayFeeModalOpen] = useState(false);
    const [selectedLoan, setSelectedLoan] = useState(null);

    useEffect(() => {
        loadDashboardData();
    }, []);

    const loadDashboardData = async () => {
        setLoading(true);
        setError(null);
        try {
            // Fetch everything in parallel
            const [loansRes, eligRes, votesRes] = await Promise.all([
                api.get('/api/loans/my-loans').catch(e => ({ error: e })),
                api.get('/api/loans/eligibility/check').catch(e => ({ error: e })),
                api.get('/api/loans/voting/active').catch(e => ({ error: e }))
            ]);

            if (loansRes.data?.success) setLoans(loansRes.data.data);
            if (eligRes.data?.success) setEligibilityData(eligRes.data.data);
            if (votesRes.data?.success) setActiveVotes(votesRes.data.data);

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

    // ✅ STEP 1: User clicks "Start Application" -> Open Form to Create Draft
    const handleStartApplication = () => {
        setSelectedLoan(null);
        setIsApplyModalOpen(true);
    };

    // ✅ STEP 2: Draft Created -> Close Form & Check Fee
    const handleDraftCreated = (draftLoan) => {
        setIsApplyModalOpen(false);

        // Only open fee modal if the fee wasn't paid in the wizard
        if (draftLoan.status === 'DRAFT' && !draftLoan.feePaid) {
            setSelectedLoan(draftLoan);
            setIsPayFeeModalOpen(true);
        } else {
            alert(`Application Created Successfully! Ref: ${draftLoan.loanNumber}`);
        }

        loadDashboardData(); // Refresh list
    };

    // ✅ STEP 3: Fee Paid -> Close Everything & Refresh
    const handleFeeSuccess = () => {
        setIsPayFeeModalOpen(false);
        alert("Application Fee Paid Successfully! You can now add guarantors.");
        loadDashboardData();
    };

    const activeLoan = loans.find(l => l.status === 'ACTIVE' || l.status === 'IN_ARREARS');

    if (loading) return <div className="p-10 text-center text-slate-400 font-bold animate-pulse"><Loader2 className="animate-spin mx-auto mb-2"/> Loading Dashboard...</div>;

    return (
        <div className="space-y-8 animate-in fade-in duration-500">

            {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 p-4 rounded-xl flex items-center gap-3">
                    <AlertCircle size={20} />
                    <p className="font-bold text-sm">{error}</p>
                </div>
            )}

            {/* 1. Show Active Loan (Top Priority) */}
            <ActiveLoanCard
                loan={activeLoan}
                onPayFee={(loan) => {
                    setSelectedLoan(loan);
                    setIsPayFeeModalOpen(true);
                }}
            />

            {/* 2. Show Voting Actions */}
            <LoanVotingWidget activeVotes={activeVotes} onVote={handleCastVote} />

            {/* 3. Show Eligibility Card (Only if no active loan) */}
            {!activeLoan && (
                <LoanEligibilityWidget
                    eligibilityData={eligibilityData}
                    settings={settings}
                    onApply={handleStartApplication}
                />
            )}

            {/* 4. History Table */}
            <LoanHistoryList
                loans={loans}
                onSelect={(loan) => { setSelectedLoan(loan); setIsApplyModalOpen(true); }}
                onPayFee={(loan) => { setSelectedLoan(loan); setIsPayFeeModalOpen(true); }}
            />

            {/* Modals */}
            <LoanApplicationModal
                isOpen={isApplyModalOpen}
                onClose={() => setIsApplyModalOpen(false)}
                onSuccess={handleDraftCreated} // Links to Fee Step check
                resumeLoan={selectedLoan}
            />

            <LoanFeePaymentModal
                isOpen={isPayFeeModalOpen}
                onClose={() => setIsPayFeeModalOpen(false)}
                onSuccess={handleFeeSuccess}
                loan={selectedLoan}
            />
        </div>
    );
}