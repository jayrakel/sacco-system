import React, { useState, useEffect } from 'react';
import api from '../../../api';
import {
    CreditCard, PlusCircle, CheckCircle, Clock, XCircle,
    FileText, Edit, AlertCircle, ChevronRight, Gavel,
    ThumbsUp, ThumbsDown, Calendar, Wallet, TrendingUp, AlertTriangle
} from 'lucide-react';
import LoanApplicationModal from './LoanApplicationModal';
import LoanFeePaymentModal from './LoanFeePaymentModal';

export default function MemberLoans({ user, onUpdate, onVoteCast }) {
    const [loans, setLoans] = useState([]);
    const [activeVotes, setActiveVotes] = useState([]);
    const [loading, setLoading] = useState(true);
    const [eligibilityData, setEligibilityData] = useState(null);
    const [isEligible, setIsEligible] = useState(false);
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
            const loansRes = await api.get('/api/loans/my-loans').catch(e => ({ error: e }));
            const eligRes = await api.get('/api/loans/eligibility/check').catch(e => ({ error: e }));
            const votesRes = await api.get('/api/loans/voting/active').catch(e => ({ error: e }));

            if (loansRes.data?.success) setLoans(loansRes.data.data);

            if (eligRes.data?.success) {
                setIsEligible(eligRes.data.data.eligible);
                setEligibilityData(eligRes.data.data);
            }

            if (votesRes.data?.success) {
                setActiveVotes(votesRes.data.data);
            }

        } catch (e) {
            console.error("Failed to load dashboard data", e);
            setError("System connection error.");
        } finally {
            setLoading(false);
        }
    };

    const handleCastVote = async (loanId, voteYes) => {
        if(!window.confirm(`Confirm Vote: ${voteYes ? "YES (Approve)" : "NO (Reject)"}? This action is final.`)) return;
        try {
            await api.post(`/api/loans/${loanId}/vote`, { vote: voteYes });
            alert("Vote Cast Successfully!");
            setActiveVotes(prev => prev.filter(l => l.id !== loanId));
            if(onVoteCast) onVoteCast();
        } catch (e) {
            alert(e.response?.data?.message || "Failed to cast vote.");
        }
    };

    const handleApplyNewLoan = () => {
        setSelectedLoan(null);
        setIsPayFeeModalOpen(true);
    };

    const handleFeePaymentSuccess = (draftLoan) => {
        setSelectedLoan(draftLoan);
        setIsPayFeeModalOpen(false);
        setIsApplyModalOpen(true);
        loadDashboardData();
    };

    // ✅ HELPER: Calculate Days Remaining (Grace Period)
    const getDaysRemaining = (nextDate) => {
        if (!nextDate) return 0;
        const today = new Date();
        const due = new Date(nextDate);
        const diffTime = due - today;
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        return diffDays;
    };

    const getStatusBadge = (status) => {
        const styles = {
            DRAFT: "bg-slate-100 text-slate-600",
            ACTIVE: "bg-green-100 text-green-700",
            IN_ARREARS: "bg-red-100 text-red-700",
            DISBURSED: "bg-blue-100 text-blue-700",
            COMPLETED: "bg-emerald-100 text-emerald-800 border border-emerald-200"
        };
        return <span className={`px-2 py-1 rounded text-[10px] font-bold uppercase ${styles[status] || 'bg-slate-100'}`}>{status?.replace(/_/g, ' ')}</span>;
    };

    // ✅ FIND ACTIVE LOAN (To show the main card)
    const activeLoan = loans.find(l => l.status === 'ACTIVE' || l.status === 'IN_ARREARS');

    if (loading) return <div className="p-10 text-center text-slate-400 font-bold animate-pulse">Synchronizing Loan Data...</div>;

    return (
        <div className="space-y-8 animate-in fade-in duration-500">

            {/* --- 1. ACTIVE LOAN CARD (THE NEW FEATURE) --- */}
            {activeLoan && (
                <div className="bg-gradient-to-br from-indigo-900 to-slate-900 rounded-3xl p-8 text-white shadow-xl relative overflow-hidden">
                    {/* Background decoration */}
                    <div className="absolute top-0 right-0 w-64 h-64 bg-white/5 rounded-full -mr-16 -mt-16 blur-3xl pointer-events-none"></div>

                    <div className="relative z-10 flex flex-col md:flex-row justify-between gap-8">
                        {/* Left: Balance & Payment Info */}
                        <div className="space-y-6 flex-1">
                            <div>
                                <div className="flex items-center gap-2 mb-1 opacity-80">
                                    <span className="text-xs font-bold uppercase tracking-widest text-indigo-300">Total Outstanding Balance</span>
                                    {activeLoan.totalArrears > 0 && <span className="bg-red-500/20 text-red-300 px-2 py-0.5 rounded text-[10px] font-bold border border-red-500/30">IN ARREARS</span>}
                                </div>
                                <h1 className="text-4xl md:text-5xl font-black tracking-tight">
                                    KES {Number(activeLoan.loanBalance).toLocaleString()}
                                </h1>
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div className="bg-white/10 p-4 rounded-2xl backdrop-blur-sm border border-white/5">
                                    <div className="flex items-center gap-2 mb-2 text-indigo-200">
                                        <Calendar size={16}/>
                                        <span className="text-xs font-bold">Weekly Due</span>
                                    </div>
                                    <p className="text-xl font-bold">KES {Number(activeLoan.weeklyRepaymentAmount).toLocaleString()}</p>
                                </div>
                                <div className={`p-4 rounded-2xl backdrop-blur-sm border ${activeLoan.totalArrears > 0 ? 'bg-red-500/10 border-red-500/30' : 'bg-emerald-500/10 border-emerald-500/30'}`}>
                                    <div className={`flex items-center gap-2 mb-2 ${activeLoan.totalArrears > 0 ? 'text-red-300' : 'text-emerald-300'}`}>
                                        {activeLoan.totalArrears > 0 ? <AlertTriangle size={16}/> : <Wallet size={16}/>}
                                        <span className="text-xs font-bold">{activeLoan.totalArrears > 0 ? "Arrears" : "Prepaid"}</span>
                                    </div>
                                    <p className="text-xl font-bold">
                                        KES {Number(activeLoan.totalArrears > 0 ? activeLoan.totalArrears : activeLoan.totalPrepaid).toLocaleString()}
                                    </p>
                                </div>
                            </div>
                        </div>

                        {/* Right: Timeline & Action */}
                        <div className="flex flex-col justify-between min-w-[280px] bg-white/5 p-6 rounded-2xl border border-white/5">
                            <div>
                                <h3 className="text-lg font-bold mb-4 flex items-center gap-2">
                                    <Clock size={18} className="text-indigo-400"/>
                                    Repayment Countdown
                                </h3>

                                {activeLoan.nextPaymentDate ? (
                                    getDaysRemaining(activeLoan.nextPaymentDate) > 0 ? (
                                        <div className="text-center py-6">
                                            <span className="text-5xl font-black text-indigo-300">{getDaysRemaining(activeLoan.nextPaymentDate)}</span>
                                            <p className="text-sm font-bold uppercase tracking-widest opacity-60 mt-1">Days Remaining</p>
                                            <p className="text-xs text-indigo-200 mt-2">Due on {activeLoan.nextPaymentDate}</p>
                                        </div>
                                    ) : (
                                        <div className="text-center py-6 bg-red-500/10 rounded-xl border border-red-500/30">
                                            <span className="text-xl font-black text-red-300">PAYMENT DUE!</span>
                                            <p className="text-xs text-red-200 mt-1">Immediate action required</p>
                                        </div>
                                    )
                                ) : (
                                    <div className="text-center py-6 opacity-60">
                                        <p className="text-sm">Schedule Generated</p>
                                    </div>
                                )}
                            </div>

                            <button
                                onClick={() => {
                                    // REUSE the Fee Payment Modal logic but adapt it for repayment?
                                    // For now, simpler to just open the modal with specific context
                                    // Or instruct user to pay via MPESA
                                    alert(`Please Pay KES ${activeLoan.weeklyRepaymentAmount} to Paybill: 123456\nAccount: ${activeLoan.loanNumber}`);
                                }}
                                className="w-full bg-emerald-500 hover:bg-emerald-400 text-white font-bold py-3 rounded-xl transition-all shadow-lg shadow-emerald-900/20 flex items-center justify-center gap-2"
                            >
                                <CreditCard size={18}/> Make Repayment
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* --- 2. ACTIVE VOTING SECTION --- */}
            {activeVotes.length > 0 && (
                <div className="bg-purple-50 border border-purple-100 rounded-3xl p-6">
                    <div className="flex items-center gap-3 mb-4">
                        <div className="p-2 bg-purple-100 text-purple-600 rounded-xl"><Gavel size={24}/></div>
                        <div>
                            <h2 className="text-lg font-bold text-slate-800">Active Committee Votes</h2>
                            <p className="text-slate-500 text-sm">Cast your vote on pending loan applications.</p>
                        </div>
                    </div>
                    {/* ... (Existing voting card logic) ... */}
                </div>
            )}

            {/* --- 3. PREREQUISITES & ELIGIBILITY (Hide if they have active loan) --- */}
            {!activeLoan && !isEligible && (
               // ... (Existing prerequisite logic) ...
               <div className="bg-white p-8 rounded-3xl border-2 border-amber-100 shadow-sm">
                   {/* ... Copy existing code ... */}
                   <div className="text-center text-slate-400 py-10">
                       <p>Eligibility check logic here...</p>
                       {/* (I omitted the full code block for brevity, keep your original implementation here) */}
                   </div>
               </div>
            )}

            {/* --- 4. START APPLICATION (Hide if active loan exists) --- */}
            {!activeLoan && isEligible && (
                <div className="bg-indigo-900 rounded-3xl p-10 text-white flex justify-between items-center shadow-2xl relative overflow-hidden">
                    <div className="relative z-10">
                        <h2 className="text-3xl font-black">Eligible for New Loan</h2>
                        <p className="text-indigo-200 text-sm">You currently have no active liabilities.</p>
                    </div>
                    <button
                        onClick={handleApplyNewLoan}
                        className="relative z-10 bg-white text-indigo-900 px-8 py-4 rounded-2xl font-black hover:bg-emerald-400 hover:text-white transition-all flex items-center gap-2"
                    >
                        <PlusCircle size={20}/> Start Application
                    </button>
                </div>
            )}

            {/* --- 5. LOAN HISTORY LIST --- */}
            {loans.length > 0 && (
                <div className="bg-white rounded-3xl shadow-sm border border-slate-200 overflow-hidden">
                    <div className="p-6 bg-slate-50 border-b border-slate-100">
                        <h3 className="font-black text-slate-700 uppercase text-xs tracking-widest">Loan History</h3>
                    </div>
                    <div className="overflow-x-auto">
                        <table className="w-full text-sm text-left">
                            <thead className="bg-slate-50 text-slate-400 font-bold uppercase text-[10px] tracking-widest">
                                <tr>
                                    <th className="p-4">Loan No</th>
                                    <th className="p-4 text-right">Principal</th>
                                    <th className="p-4 text-right">Balance</th>
                                    <th className="p-4">Status</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100">
                                {loans.map(loan => (
                                    <tr key={loan.id} className="hover:bg-slate-50 transition">
                                        <td className="p-4 font-mono font-bold text-slate-600">{loan.loanNumber}</td>
                                        <td className="p-4 text-right font-mono">KES {Number(loan.principalAmount).toLocaleString()}</td>
                                        <td className="p-4 text-right font-mono font-bold text-slate-800">
                                            KES {Number(loan.loanBalance).toLocaleString()}
                                        </td>
                                        <td className="p-4">{getStatusBadge(loan.status)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            <LoanApplicationModal isOpen={isApplyModalOpen} onClose={() => setIsApplyModalOpen(false)} onSuccess={loadDashboardData} resumeLoan={selectedLoan} />
            <LoanFeePaymentModal isOpen={isPayFeeModalOpen} onClose={() => setIsPayFeeModalOpen(false)} onSuccess={handleFeePaymentSuccess} loan={selectedLoan} isNewApplication={!selectedLoan} />
        </div>
    );
}