import React, { useState, useEffect } from 'react';
import api from '../api';
import {
    Briefcase, CheckCircle, XCircle, Clock, Wallet, RefreshCw,
    FileText, User, Calendar, DollarSign, AlertCircle, Shield, ChevronRight
} from 'lucide-react';
import DashboardHeader from '../components/DashboardHeader';
import BrandedSpinner from '../components/BrandedSpinner';

export default function LoansDashboard() {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [pendingLoans, setPendingLoans] = useState([]);
    const [stats, setStats] = useState({ pending: 0, active: 0, rejected: 0, totalDisbursed: 0 });

    // --- MODAL STATE ---
    const [selectedLoan, setSelectedLoan] = useState(null);
    const [guarantors, setGuarantors] = useState([]);
    const [loadingGuarantors, setLoadingGuarantors] = useState(false);
    const [processing, setProcessing] = useState(false);
    const [remarks, setRemarks] = useState("");

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user') || localStorage.getItem('user');
        if (storedUser) setUser(JSON.parse(storedUser));
        loadAllData();
        const interval = setInterval(loadAllData, 30000);
        return () => clearInterval(interval);
    }, []);

    const loadAllData = async () => {
        try {
            const pendingRes = await api.get('/api/loans/admin/pending');
            if (pendingRes.data.success) {
                const loans = pendingRes.data.data;
                setPendingLoans(loans);
                setStats(prev => ({ ...prev, pending: loans.length }));
            }
        } catch (e) {
            console.error("Failed to load dashboard data", e);
        } finally {
            setLoading(false);
        }
    };

    const openReviewModal = async (loan) => {
        setSelectedLoan(loan);
        setRemarks("");
        setLoadingGuarantors(true);
        try {
            const res = await api.get(`/api/loans/${loan.id}/guarantors`);
            if(res.data.success) setGuarantors(res.data.data);
        } catch (error) {
            console.error(error);
        } finally {
            setLoadingGuarantors(false);
        }
    };

    const handleDecision = async (decision) => {
        if (decision === 'REJECT' && !remarks.trim()) {
            alert("Please provide remarks for rejection.");
            return;
        }
        if (decision === 'APPROVE' && !window.confirm("Confirm: Approve this loan application?")) return;

        setProcessing(true);
        try {
            await api.post(`/api/loans/admin/${selectedLoan.id}/review`, { decision, remarks });
            setPendingLoans(prev => prev.filter(l => l.id !== selectedLoan.id));
            setStats(prev => ({ ...prev, pending: prev.pending - 1 }));
            setSelectedLoan(null);
            alert(`Loan ${decision}ED successfully.`);
        } catch (error) {
            alert(error.response?.data?.message || "Operation failed");
        } finally {
            setProcessing(false);
        }
    };

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800 pb-12">
            <DashboardHeader user={user} title="Loan Officer Portal" />

            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8 space-y-8 animate-in fade-in">
                <div className="flex justify-between items-end">
                    <div>
                        <h1 className="text-2xl font-bold text-slate-800">Technical Review Portfolio</h1>
                        <p className="text-slate-500 text-sm">Validate eligibility & guarantors before approval.</p>
                    </div>
                    <button onClick={loadAllData} className="p-2 bg-white border border-slate-200 rounded-full hover:bg-slate-50 text-slate-400 transition">
                        <RefreshCw size={18} />
                    </button>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                    <StatCard icon={<Clock size={24}/>} color="amber" label="Queue" value={pendingLoans.length} />
                    <StatCard icon={<CheckCircle size={24}/>} color="emerald" label="Active" value={stats.active} />
                    <StatCard icon={<Wallet size={24}/>} color="indigo" label="Volume" value={`KES ${(stats.totalDisbursed/1000).toFixed(0)}k`} />
                    <StatCard icon={<XCircle size={24}/>} color="red" label="Rejected" value={stats.rejected} />
                </div>

                <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
                    <div className="p-6 border-b border-slate-100 bg-slate-50/50 flex justify-between items-center">
                        <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                            <Briefcase size={20} className="text-slate-400"/> Application Queue
                        </h2>
                    </div>

                    {loading ? (
                        <div className="p-12 flex justify-center"><BrandedSpinner /></div>
                    ) : pendingLoans.length === 0 ? (
                        <div className="p-12 text-center text-slate-500">
                            <CheckCircle className="mx-auto h-12 w-12 text-slate-300 mb-4" />
                            <h3 className="text-lg font-medium text-slate-900">Queue Empty</h3>
                            <p>No applications pending review.</p>
                        </div>
                    ) : (
                        <div className="divide-y divide-slate-100">
                            {pendingLoans.map((loan) => (
                                <div key={loan.id} className="p-6 hover:bg-slate-50 transition flex flex-col md:flex-row items-center gap-6">
                                    <div className="flex-1 space-y-1">
                                        <div className="flex items-center gap-3">
                                            <span className="font-mono text-xs font-bold text-slate-500 bg-slate-100 px-2 py-1 rounded">{loan.loanNumber}</span>
                                            <span className="text-[10px] font-bold bg-amber-100 text-amber-700 px-2 py-1 rounded uppercase tracking-wide">{loan.status}</span>
                                        </div>
                                        <h3 className="text-lg font-bold text-slate-900">{loan.memberName}</h3>
                                        <div className="flex items-center gap-4 text-sm text-slate-500">
                                            <span className="flex items-center gap-1"><DollarSign size={14}/> KES {loan.principalAmount?.toLocaleString()}</span>
                                            <span className="flex items-center gap-1"><Calendar size={14}/> {loan.expectedRepaymentDate}</span>
                                        </div>
                                    </div>
                                    <button
                                        onClick={() => openReviewModal(loan)}
                                        className="px-6 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-bold rounded-lg shadow-sm flex items-center gap-2"
                                    >
                                        Review Application <ChevronRight size={16}/>
                                    </button>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </main>

            {/* --- REVIEW MODAL --- */}
            {selectedLoan && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in">
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl overflow-hidden flex flex-col max-h-[90vh]">
                        <div className="p-6 border-b border-slate-100 bg-slate-50 flex justify-between items-center">
                            <div>
                                <h3 className="text-xl font-bold text-slate-800">Review Application</h3>
                                <p className="text-sm text-slate-500 font-mono">{selectedLoan.loanNumber}</p>
                            </div>
                            <button onClick={() => setSelectedLoan(null)} className="text-slate-400 hover:text-red-500"><XCircle size={28}/></button>
                        </div>

                        <div className="p-6 overflow-y-auto custom-scrollbar space-y-6">
                            {/* 1. Applicant Financial Snapshot */}
                            <div className="grid grid-cols-2 gap-4">
                                <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
                                    <p className="text-xs text-slate-400 uppercase font-bold mb-1">Applicant</p>
                                    <p className="font-bold text-slate-800 text-lg flex items-center gap-2">
                                        <User size={18} className="text-indigo-500"/> {selectedLoan.memberName}
                                    </p>
                                </div>
                                <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
                                    <p className="text-xs text-slate-400 uppercase font-bold mb-1">Amount Requested</p>
                                    <p className="font-bold text-slate-800 text-lg flex items-center gap-2">
                                        <DollarSign size={18} className="text-slate-400"/> KES {selectedLoan.principalAmount?.toLocaleString()}
                                    </p>
                                </div>

                                {/* ✅ NEW: Financial Health Check */}
                                <div className="p-4 bg-emerald-50 rounded-xl border border-emerald-100">
                                    <p className="text-xs text-emerald-600 uppercase font-bold mb-1">Total Savings</p>
                                    <p className="font-bold text-emerald-800 text-lg">
                                        KES {selectedLoan.memberSavings?.toLocaleString() || '0'}
                                    </p>
                                </div>
                                <div className="p-4 bg-indigo-50 rounded-xl border border-indigo-100">
                                    <p className="text-xs text-indigo-600 uppercase font-bold mb-1">Net Monthly Income</p>
                                    <p className="font-bold text-indigo-800 text-lg">
                                        KES {selectedLoan.memberNetIncome?.toLocaleString() || '0'}
                                    </p>
                                </div>
                            </div>

                            {/* 2. Guarantors Check */}
                            <div>
                                <h4 className="font-bold text-slate-700 mb-3 flex items-center gap-2">
                                    <Shield size={18} className="text-indigo-500"/> Guarantor Status
                                </h4>
                                {loadingGuarantors ? <p className="text-sm text-slate-400 italic">Checking guarantors...</p> : (
                                    <div className="space-y-2">
                                        {guarantors.map(g => (
                                            <div key={g.id} className="flex justify-between items-center p-3 border border-slate-100 rounded-lg bg-white">
                                                <div>
                                                    <p className="font-bold text-sm text-slate-700">{g.memberName}</p>
                                                    <p className="text-xs text-slate-400">Guaranteed: KES {g.guaranteeAmount.toLocaleString()}</p>
                                                </div>
                                                <span className={`text-[10px] font-bold px-2 py-1 rounded uppercase ${g.status === 'ACCEPTED' ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'}`}>
                                                    {g.status}
                                                </span>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>

                            {/* 3. Officer Remarks */}
                            <div>
                                <label className="block text-sm font-bold text-slate-700 mb-2">Officer Remarks / Rejection Reason</label>
                                <textarea
                                    value={remarks}
                                    onChange={(e) => setRemarks(e.target.value)}
                                    placeholder="Enter comments here (Required for rejection)..."
                                    className="w-full p-3 border border-slate-200 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:outline-none text-sm"
                                    rows="3"
                                ></textarea>
                            </div>
                        </div>

                        <div className="p-6 border-t border-slate-100 bg-slate-50 flex gap-4">
                            <button
                                onClick={() => handleDecision('REJECT')}
                                disabled={processing}
                                className="flex-1 py-3 bg-white border border-slate-200 text-slate-600 font-bold rounded-xl hover:bg-red-50 hover:text-red-600 hover:border-red-200 transition"
                            >
                                Reject Application
                            </button>
                            <button
                                onClick={() => handleDecision('APPROVE')}
                                disabled={processing}
                                className="flex-1 py-3 bg-emerald-600 text-white font-bold rounded-xl hover:bg-emerald-700 shadow-md hover:shadow-lg transition flex justify-center items-center gap-2"
                            >
                                {processing ? <RefreshCw className="animate-spin" size={20}/> : <CheckCircle size={20}/>}
                                Approve Loan
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

function StatCard({ icon, color, label, value }) {
    const colors = {
        amber: "bg-amber-100 text-amber-600",
        emerald: "bg-emerald-100 text-emerald-600",
        indigo: "bg-indigo-100 text-indigo-600",
        red: "bg-red-100 text-red-600",
    };
    return (
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-200 flex items-center gap-4">
            <div className={`p-3 rounded-xl ${colors[color] || colors.indigo}`}>{icon}</div>
            <div>
                <p className="text-xs font-bold text-slate-400 uppercase">{label}</p>
                <h3 className="text-2xl font-bold text-slate-800">{value}</h3>
            </div>
        </div>
    );
}