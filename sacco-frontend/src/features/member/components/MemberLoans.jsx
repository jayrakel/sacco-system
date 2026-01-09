import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { useSettings } from '../../../context/SettingsContext';
import { AlertCircle, Loader2 } from 'lucide-react';

// Widgets
import ActiveLoanCard from '../../loans/components/dashboard/ActiveLoanCard';
import LoanVotingWidget from '../../loans/components/dashboard/LoanVotingWidget';
import LoanEligibilityWidget from '../../loans/components/dashboard/LoanEligibilityWidget';
import LoanHistoryList from '../../loans/components/dashboard/LoanHistoryList';

// Modals
import LoanApplicationModal from '../../loans/components/dashboard/LoanApplicationModal';
// ✅ IMPORT FROM LOCAL (This is the one we created for member fee payment)
import LoanFeePaymentModal from './LoanFeePaymentModal';

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

    // We maintain 'selectedLoan' just in case we need to resume an old draft
    // But for a NEW application, this will be null.
    const [selectedLoan, setSelectedLoan] = useState(null);

    useEffect(() => {
        loadDashboardData();
    }, []);

    const loadDashboardData = async () => {
        setLoading(true);
        setError(null);
        try {
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

    // ✅ STEP 1: START -> OPEN PAYMENT MODAL FIRST
    // This is triggered by the "Apply Now" button in LoanEligibilityWidget
    const handleStartApplication = () => {
        setSelectedLoan(null); // Ensure we are starting fresh
        setIsPayFeeModalOpen(true);
    };

    // ✅ STEP 2: PAYMENT SUCCESS -> OPEN APPLICATION FORM
    // This is called by LoanFeePaymentModal when MPESA transaction is COMPLETED
    const handleFeeSuccess = () => {
        setIsPayFeeModalOpen(false);

        // Slight delay for smooth UX transition
        setTimeout(() => {
            setIsApplyModalOpen(true);
        }, 500);
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
                    // For existing loans that need repayment (different from application fee)
                    alert("Loan Repayment Modal would open here.");
                }}
            />

            {/* 2. Show Voting Actions */}
            <LoanVotingWidget activeVotes={activeVotes} onVote={handleCastVote} />

            {/* 3. Show Eligibility Card (Only if no active loan) */}
            {!activeLoan && (
                <LoanEligibilityWidget
                    eligibilityData={eligibilityData}
                    settings={settings}
                    onApply={handleStartApplication} // ✅ Triggers Fee Payment Modal
                />
            )}

            {/* 4. History Table */}
            <LoanHistoryList
                loans={loans}
                onSelect={(loan) => {
                    // If viewing an old draft, we might skip fee if already paid?
                    // For now, let's just open the application modal to view details
                    setSelectedLoan(loan);
                    setIsApplyModalOpen(true);
                }}
            />

            {/* --- MODALS --- */}

            {/* 1. Fee Payment (First Step) */}
            <LoanFeePaymentModal
                isOpen={isPayFeeModalOpen}
                onClose={() => setIsPayFeeModalOpen(false)}
                onSuccess={handleFeeSuccess} // ✅ Moves to Step 2
            />

            {/* 2. Application Form (Second Step) */}
            <LoanApplicationModal
                isOpen={isApplyModalOpen}
                onClose={() => {
                    setIsApplyModalOpen(false);
                    loadDashboardData(); // Refresh list on close
                }}
                resumeLoan={selectedLoan} // Used if editing an old draft
            />
        </div>
    );
}