import React, { useState, useEffect } from 'react';
import api from '../../../api';
import {
    CreditCard, PlusCircle, CheckCircle, Clock, XCircle,
    FileText, Edit, AlertCircle, ChevronRight, Gavel,
    ThumbsUp, ThumbsDown, Calendar, Wallet, AlertTriangle
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
            } else if (eligRes.error) {
                setError(eligRes.error.response?.data?.message || "Could not verify eligibility.");
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
            APPLICATION_FEE_PENDING: "bg-red-50 text-red-600",
            GUARANTORS_PENDING: "bg-amber-50 text-amber-600",
            ACTIVE: "bg-green-100 text-green-700",
            IN_ARREARS: "bg-red-100 text-red-700",
            VOTING_OPEN: "bg-purple-100 text-purple-700",
            SECRETARY_DECISION: "bg-indigo-50 text-indigo-700",
            TREASURER_DISBURSEMENT: "bg-blue-50 text-blue-700",
            DISBURSED: "bg-blue-100 text-blue-700"
        };
        return <span className={`px-2 py-1 rounded text-[10px] font-bold uppercase ${styles[status] || 'bg-slate-100'}`}>{status?.replace(/_/g, ' ')}</span>;
    };

    // ✅ FIND ACTIVE LOAN (To show the main card)
    const activeLoan = loans.find(l => l.status === 'ACTIVE' || l.status === 'IN_ARREARS');

    if (loading) return <div className="p-10 text-center text-slate-400 font-bold animate-pulse">Synchronizing Loan Data...</div>;

    return (
        <div className="space-y-8 animate-in fade-in duration-500">

            {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 p-4 rounded-xl flex items-center gap-3">
                    <AlertCircle size={20} />
                    <div><p className="font-bold text-sm">Synchronization Error</p><p className="text-xs">{error}</p></div>
                </div>
            )}

            {/* --- 1. ACTIVE LOAN CARD (RESTORED) --- */}
            {activeLoan && (
                <div className="bg-gradient-to-br from-indigo-900 to-slate-900 rounded-3xl p-8 text-white shadow-xl relative overflow-hidden">
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
                <div className="bg-gradient-to-r from-purple-50 to-indigo-50 border border-purple-100 rounded-3xl p-6 shadow-sm">
                    <div className="flex items-center gap-3 mb-4">
                        <div className="p-2 bg-purple-100 text-purple-600 rounded-xl"><Gavel size={24}/></div>
                        <div>
                            <h2 className="text-lg font-bold text-slate-800">Active Committee Votes</h2>
                            <p className="text-slate-500 text-sm">Cast your vote on pending loan applications.</p>
                        </div>
                    </div>

                    <div className="grid gap-4 md:grid-cols-2">
                        {activeVotes.map(loan => (
                            <div key={loan.id} className="bg-white p-5 rounded-2xl shadow-sm border border-purple-100 flex flex-col justify-between hover:shadow-md transition">
                                <div className="mb-4">
                                    <div className="flex justify-between items-start">
                                        <h3 className="font-bold text-slate-800">{loan.memberName}</h3>
                                        <span className="text-[10px] font-mono bg-slate-100 text-slate-500 px-2 py-1 rounded">{loan.loanNumber}</span>
                                    </div>
                                    <p className="text-2xl font-black text-slate-700 mt-2">KES {Number(loan.principalAmount).toLocaleString()}</p>
                                    <div className="flex gap-4 mt-2 text-xs text-slate-500">
                                        <span>Duration: {loan.duration} {loan.durationUnit}</span>
                                    </div>
                                    {/* Show Savings if available (Context for voters) */}
                                    <div className="flex justify-between text-sm pt-2 border-t border-slate-50 mt-2">
                                        <span className="text-slate-500">Member Savings</span>
                                        <span className="font-medium text-emerald-600">
                                            KES {Number(loan.memberSavings || 0).toLocaleString()}
                                        </span>
                                    </div>
                                </div>

                                <div className="flex gap-3 mt-auto pt-4 border-t border-slate-50">
                                    <button
                                        onClick={() => handleCastVote(loan.id, false)}
                                        className="flex-1 py-2.5 border border-red-100 bg-red-50 text-red-600 rounded-xl font-bold text-sm hover:bg-red-100 flex items-center justify-center gap-2 transition"
                                    >
                                        <ThumbsDown size={16}/> Decline
                                    </button>
                                    <button
                                        onClick={() => handleCastVote(loan.id, true)}
                                        className="flex-1 py-2.5 bg-emerald-600 text-white rounded-xl font-bold text-sm hover:bg-emerald-700 flex items-center justify-center gap-2 shadow-sm transition"
                                    >
                                        <ThumbsUp size={16}/> Approve
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* --- 3. PREREQUISITES & ELIGIBILITY (Hide if active loan exists) --- */}
            {!activeLoan && !isEligible && (
                <div className="bg-white p-8 rounded-3xl border-2 border-amber-100 shadow-sm overflow-hidden relative">
                    <div className="flex items-center gap-4 mb-8">
                        <div className="p-3 bg-amber-50 text-amber-600 rounded-2xl"><AlertCircle size={28}/></div>
                        <div>
                            <h2 className="text-xl font-black text-slate-800">Loan Application Prerequisites</h2>
                            <p className="text-slate-500 text-sm">Meet the requirements below to unlock applications.</p>
                        </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
                        {/* Savings Check */}
                        <div className={`p-5 rounded-2xl border transition-all ${Number(eligibilityData?.currentSavings || 0) >= Number(eligibilityData?.requiredSavings || 5000) ? 'bg-emerald-50 border-emerald-100' : 'bg-slate-50 border-slate-100 opacity-80'}`}>
                            <div className="flex justify-between items-start mb-2">
                                <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Savings Status</p>
                                {Number(eligibilityData?.currentSavings || 0) >= Number(eligibilityData?.requiredSavings || 5000) ? <CheckCircle size={14} className="text-emerald-500"/> : <Clock size={14} className="text-slate-300"/>}
                            </div>
                            <p className="text-lg font-black text-slate-700">KES {Number(eligibilityData?.currentSavings || 0).toLocaleString()}</p>
                            <p className="text-xs text-slate-400">Target: KES {Number(eligibilityData?.requiredSavings).toLocaleString()}</p>
                        </div>

                        {/* Membership Check */}
                        <div className={`p-5 rounded-2xl border transition-all ${Number(eligibilityData?.membershipMonths || 0) >= Number(eligibilityData?.requiredMonths || 0) ? 'bg-emerald-50 border-emerald-100' : 'bg-slate-50 border-slate-100 opacity-80'}`}>
                            <div className="flex justify-between items-start mb-2">
                                <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Membership</p>
                                {Number(eligibilityData?.membershipMonths || 0) >= Number(eligibilityData?.requiredMonths || 0) ? <CheckCircle size={14} className="text-emerald-500"/> : <Clock size={14} className="text-slate-300"/>}
                            </div>
                            <p className="text-lg font-black text-slate-700">{eligibilityData?.membershipMonths || 0} Months</p>
                            <p className="text-xs text-slate-400">Req: {eligibilityData?.requiredMonths || 0} Months</p>
                        </div>

                        {/* Active Loan Check */}
                        <div className={`p-5 rounded-2xl border transition-all ${Number(eligibilityData?.currentActiveLoans || 0) < Number(eligibilityData?.maxActiveLoans || 1) ? 'bg-emerald-50 border-emerald-100' : 'bg-red-50 border-red-100'}`}>
                            <div className="flex justify-between items-start mb-2">
                                <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Active Loans</p>
                                {Number(eligibilityData?.currentActiveLoans || 0) < Number(eligibilityData?.maxActiveLoans || 1) ? <CheckCircle size={14} className="text-emerald-500"/> : <XCircle size={14} className="text-red-500"/>}
                            </div>
                            <p className="text-lg font-black text-slate-700">{eligibilityData?.currentActiveLoans || 0} / {eligibilityData?.maxActiveLoans || 1}</p>
                            <p className="text-xs text-slate-400">Limit: {eligibilityData?.maxActiveLoans || 1} active</p>
                        </div>
                    </div>

                    {/* Pending Actions */}
                    <div className="bg-amber-50/50 p-4 rounded-xl border border-amber-100/50">
                        <h4 className="text-[10px] font-black text-amber-600 uppercase mb-2 tracking-widest">Pending Actions</h4>
                        <ul className="space-y-2">
                            {eligibilityData?.reasons?.map((reason, idx) => (
                                <li key={idx} className="flex items-center gap-2 text-xs font-bold text-amber-700">
                                    <span className="w-1.5 h-1.5 rounded-full bg-amber-400"/> {reason}
                                </li>
                            ))}
                        </ul>
                    </div>
                </div>
            )}

            {/* --- 4. START APPLICATION (Hide if active loan exists) --- */}
            {!activeLoan && isEligible && (
                <div className="bg-indigo-900 rounded-3xl p-10 text-white flex flex-col md:flex-row justify-between items-center shadow-2xl shadow-indigo-900/20 border border-white/10 group relative overflow-hidden">
                    <div className="absolute top-0 right-0 -mr-16 -mt-16 w-64 h-64 bg-indigo-500/20 rounded-full blur-3xl group-hover:bg-emerald-500/10 transition-colors duration-700" />
                    <div className="relative z-10 space-y-2 text-center md:text-left mb-6 md:mb-0">
                        <h2 className="text-3xl font-black tracking-tight">You are Eligible for a Loan!</h2>
                        <p className="text-indigo-200 text-sm max-w-md">You have met all system requirements.</p>
                    </div>
                    <button
                        onClick={handleApplyNewLoan}
                        className="relative z-10 bg-white text-indigo-900 px-10 py-5 rounded-2xl font-black hover:bg-emerald-400 hover:text-white transition-all transform hover:scale-105 active:scale-95 flex items-center gap-3 shadow-xl"
                    >
                        <PlusCircle size={24}/> Start Application <ChevronRight size={20}/>
                    </button>
                </div>
            )}

            {/* --- 5. LOAN HISTORY LIST --- */}
            {loans.length > 0 && (
                <div className="bg-white rounded-3xl shadow-sm border border-slate-200 overflow-hidden">
                    <div className="p-6 bg-slate-50 border-b border-slate-100">
                        <h3 className="font-black text-slate-700 uppercase text-xs tracking-widest">Your Loan Applications</h3>
                    </div>
                    <div className="overflow-x-auto">
                        <table className="w-full text-sm text-left">
                            <thead className="bg-slate-50/50 text-slate-400 font-bold uppercase text-[10px] tracking-widest border-b border-slate-100">
                                <tr>
                                    <th className="p-4">Loan No</th>
                                    <th className="p-4 text-right">Principal</th>
                                    <th className="p-4">Status</th>
                                    <th className="p-4 text-center">Action</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100">
                                {loans.map(loan => (
                                    <tr key={loan.id} className="hover:bg-indigo-50/30 transition">
                                        <td className="p-4 font-mono text-slate-500 font-bold">{loan.loanNumber || 'NEW DRAFT'}</td>
                                        <td className="p-4 text-right font-black text-slate-900 font-mono">KES {Number(loan.principalAmount || 0).toLocaleString()}</td>
                                        <td className="p-4">{getStatusBadge(loan.status)}</td>
                                        <td className="p-4 text-center">
                                            {loan.status === 'DRAFT' && <button onClick={() => { setSelectedLoan(loan); setIsApplyModalOpen(true); }} className="text-indigo-600 p-2 hover:bg-indigo-50 rounded-lg"><Edit size={16}/></button>}
                                            {loan.status === 'APPLICATION_FEE_PENDING' && <button onClick={() => { setSelectedLoan(loan); setIsPayFeeModalOpen(true); }} className="bg-purple-600 text-white px-3 py-1 text-[10px] rounded hover:bg-purple-700">PAY FEE</button>}
                                        </td>
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