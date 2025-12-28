import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { CreditCard, PlusCircle, CheckCircle, Clock, XCircle, FileText, ArrowRight, Trash2, Edit, Users, AlertCircle, ChevronRight } from 'lucide-react';
import LoanApplicationModal from './LoanApplicationModal';
import LoanFeePaymentModal from './LoanFeePaymentModal';

export default function MemberLoans({ user }) {
    const [loans, setLoans] = useState([]);
    const [loading, setLoading] = useState(true);
    const [eligibilityData, setEligibilityData] = useState(null);
    const [isEligible, setIsEligible] = useState(false);

    // Modal States
    const [isApplyModalOpen, setIsApplyModalOpen] = useState(false);
    const [isPayFeeModalOpen, setIsPayFeeModalOpen] = useState(false);
    const [selectedLoan, setSelectedLoan] = useState(null);

    useEffect(() => {
        loadDashboardData();
    }, []);

    const loadDashboardData = async () => {
        setLoading(true);
        try {
            const [loansRes, eligRes] = await Promise.all([
                api.get('/api/loans/my-loans'),
                api.get('/api/loans/eligibility/check')
            ]);
            if (loansRes.data.success) setLoans(loansRes.data.data);
            if (eligRes.data.success) {
                setIsEligible(eligRes.data.data.eligible);
                setEligibilityData(eligRes.data.data);
            }
        } catch (e) {
            console.error("Failed to load dashboard data", e);
        } finally {
            setLoading(false);
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

    const getStatusBadge = (status) => {
        const styles = {
            DRAFT: "bg-slate-100 text-slate-600",
            APPLICATION_FEE_PENDING: "bg-red-50 text-red-600",
            GUARANTORS_PENDING: "bg-amber-50 text-amber-600",
            ACTIVE: "bg-green-100 text-green-700",
        };
        return <span className={`px-2 py-1 rounded text-[10px] font-bold uppercase ${styles[status] || 'bg-slate-100'}`}>{status?.replace(/_/g, ' ')}</span>;
    };

    if (loading) return <div className="p-10 text-center text-slate-400 font-bold animate-pulse">Synchronizing Loan Data...</div>;

    return (
        <div className="space-y-8 animate-in fade-in duration-500">

            {/* 1. PREREQUISITES SECTION: Shown only if NOT eligible */}
            {!isEligible && (
                <div className="bg-white p-8 rounded-3xl border-2 border-amber-100 shadow-sm overflow-hidden relative">
                    <div className="flex items-center gap-4 mb-8">
                        <div className="p-3 bg-amber-50 text-amber-600 rounded-2xl">
                            <AlertCircle size={28}/>
                        </div>
                        <div>
                            <h2 className="text-xl font-black text-slate-800">Loan Application Prerequisites</h2>
                            <p className="text-slate-500 text-sm">Meet the requirements below to unlock applications.</p>
                        </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
                        {/* Dynamic Savings Check */}
                        <div className={`p-5 rounded-2xl border transition-all ${Number(eligibilityData?.currentSavings || 0) >= Number(eligibilityData?.requiredSavings || 5000) ? 'bg-emerald-50 border-emerald-100' : 'bg-slate-50 border-slate-100 opacity-80'}`}>
                            <div className="flex justify-between items-start mb-2">
                                <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Savings Status</p>
                                {Number(eligibilityData?.currentSavings || 0) >= Number(eligibilityData?.requiredSavings || 5000) ? <CheckCircle size={14} className="text-emerald-500"/> : <Clock size={14} className="text-slate-300"/>}
                            </div>
                            <p className="text-lg font-black text-slate-700">KES {Number(eligibilityData?.currentSavings || 0).toLocaleString()}</p>
                            <p className="text-xs text-slate-400">Target: KES {Number(eligibilityData?.requiredSavings || 5000).toLocaleString()}</p>
                        </div>

                        {/* Dynamic Membership Check */}
                        <div className={`p-5 rounded-2xl border transition-all ${Number(eligibilityData?.membershipMonths || 0) >= Number(eligibilityData?.requiredMonths || 0) ? 'bg-emerald-50 border-emerald-100' : 'bg-slate-50 border-slate-100 opacity-80'}`}>
                            <div className="flex justify-between items-start mb-2">
                                <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Membership</p>
                                {Number(eligibilityData?.membershipMonths || 0) >= Number(eligibilityData?.requiredMonths || 0) ? <CheckCircle size={14} className="text-emerald-500"/> : <Clock size={14} className="text-slate-300"/>}
                            </div>
                            <p className="text-lg font-black text-slate-700">{eligibilityData?.membershipMonths || 0} Months</p>
                            <p className="text-xs text-slate-400">Req: {eligibilityData?.requiredMonths || 0} Months</p>
                        </div>

                        {/* Dynamic Active Loan Limit Check */}
                        <div className={`p-5 rounded-2xl border transition-all ${Number(eligibilityData?.currentActiveLoans || 0) < Number(eligibilityData?.maxActiveLoans || 1) ? 'bg-emerald-50 border-emerald-100' : 'bg-red-50 border-red-100'}`}>
                            <div className="flex justify-between items-start mb-2">
                                <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Active Loans</p>
                                {Number(eligibilityData?.currentActiveLoans || 0) < Number(eligibilityData?.maxActiveLoans || 1) ? <CheckCircle size={14} className="text-emerald-500"/> : <XCircle size={14} className="text-red-500"/>}
                            </div>
                            <p className="text-lg font-black text-slate-700">{eligibilityData?.currentActiveLoans || 0} / {eligibilityData?.maxActiveLoans || 1}</p>
                            <p className="text-xs text-slate-400">Limit: {eligibilityData?.maxActiveLoans || 1} active</p>
                        </div>
                    </div>

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

            {/* 2. ELIGIBLE ACTION: Card shown only when qualifies */}
            {isEligible && (
                <div className="bg-indigo-900 rounded-3xl p-10 text-white flex flex-col md:flex-row justify-between items-center shadow-2xl shadow-indigo-900/20 border border-white/10 group relative overflow-hidden">
                    <div className="absolute top-0 right-0 -mr-16 -mt-16 w-64 h-64 bg-indigo-500/20 rounded-full blur-3xl group-hover:bg-emerald-500/10 transition-colors duration-700" />
                    <div className="relative z-10 space-y-2 text-center md:text-left mb-6 md:mb-0">
                        <h2 className="text-3xl font-black tracking-tight">You are Eligible for a Loan!</h2>
                        <p className="text-indigo-200 text-sm max-w-md">You have met all system requirements. You can now proceed to submit an application.</p>
                    </div>
                    <button
                        onClick={handleApplyNewLoan}
                        className="relative z-10 bg-white text-indigo-900 px-10 py-5 rounded-2xl font-black hover:bg-emerald-400 hover:text-white transition-all transform hover:scale-105 active:scale-95 flex items-center gap-3 shadow-xl"
                    >
                        <PlusCircle size={24}/> Start Application <ChevronRight size={20}/>
                    </button>
                </div>
            )}

            {/* 3. LOAN HISTORY: Only shown if history exists */}
            {loans.length > 0 && (
                <div className="bg-white rounded-3xl shadow-sm border border-slate-200 overflow-hidden">
                    <div className="p-6 bg-slate-50 border-b border-slate-100 flex justify-between items-center">
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
                                            <div className="flex justify-center gap-2">
                                                {loan.status === 'DRAFT' && (
                                                    <button onClick={() => { setSelectedLoan(loan); setIsApplyModalOpen(true); }} className="text-indigo-600 p-2 hover:bg-indigo-50 rounded-lg transition" title="Complete Application">
                                                        <Edit size={16}/>
                                                    </button>
                                                )}
                                                {loan.status === 'APPLICATION_FEE_PENDING' && (
                                                    <button onClick={() => { setSelectedLoan(loan); setIsPayFeeModalOpen(true); }} className="bg-purple-600 text-white px-3 py-1.5 rounded-lg text-[10px] font-black hover:bg-purple-700 transition shadow-md">
                                                        PAY FEE
                                                    </button>
                                                )}
                                                <button className="text-slate-400 p-2 hover:bg-slate-100 rounded-lg">
                                                    <FileText size={16}/>
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {/* Application Modals */}
            <LoanApplicationModal
                isOpen={isApplyModalOpen}
                onClose={() => setIsApplyModalOpen(false)}
                onSuccess={loadDashboardData}
                resumeLoan={selectedLoan}
            />

            <LoanFeePaymentModal
                isOpen={isPayFeeModalOpen}
                onClose={() => setIsPayFeeModalOpen(false)}
                onSuccess={handleFeePaymentSuccess}
                loan={selectedLoan}
                isNewApplication={!selectedLoan}
            />
        </div>
    );
}