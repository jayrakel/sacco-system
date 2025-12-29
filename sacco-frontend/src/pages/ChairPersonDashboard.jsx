import React, { useState, useEffect } from 'react';
import api from '../api'; // ✅ FIXED PATH
import { Users, TrendingUp, CheckCircle, Gavel } from 'lucide-react';
import DashboardHeader from '../components/DashboardHeader'; // ✅ FIXED PATH
import BrandedSpinner from '../components/BrandedSpinner'; // ✅ FIXED PATH
import ShareCapitalCard from '../components/ShareCapitalCard'; // ✅ FIXED PATH

export default function ChairpersonDashboard() {
    const [user, setUser] = useState(null);
    const [approvalQueue, setApprovalQueue] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
        fetchAgenda();
    }, []);

    const fetchAgenda = async () => {
        setLoading(true);
        try {
            const res = await api.get('/api/loans/admin/pending');
            if (res.data.success) {
                // ✅ LOGIC: Chair sees loans PASSED by Secretary (SECRETARY_DECISION)
                setApprovalQueue(res.data.data.filter(l => l.status === 'SECRETARY_DECISION'));
            }
        } catch (e) {
            console.error("Failed to load dashboard", e);
        } finally {
            setLoading(false);
        }
    };

    const handleFinalApproval = async (loan) => {
        if (!window.confirm(`Grant Final Executive Approval for Loan ${loan.loanNumber}? This authorizes disbursement.`)) return;

        try {
            // ✅ CALLS FINAL APPROVAL ENDPOINT
            await api.post(`/api/loans/chairperson/${loan.id}/final-approval`);
            alert("Final Approval Granted. Loan forwarded to Treasurer.");
            fetchAgenda();
        } catch (error) {
            alert(error.response?.data?.message || "Action failed");
        }
    };

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800 pb-12">
            <DashboardHeader user={user} title="Executive Dashboard" />

            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8 space-y-8 animate-in fade-in">

                {/* 1. Static Overview Cards */}
                <div>
                    <h1 className="text-2xl font-bold text-slate-800">Executive Overview</h1>
                    <p className="text-slate-500 text-sm">High-level insights and system health status.</p>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                    <div className="bg-white p-8 rounded-2xl shadow-sm border border-indigo-100 flex flex-col items-center text-center">
                        <div className="p-4 bg-indigo-50 text-indigo-600 rounded-full mb-4"><Users size={32}/></div>
                        <h3 className="font-bold text-slate-700 text-lg">System Health</h3>
                        <div className="text-3xl font-bold text-green-600 mt-2">Good</div>
                    </div>
                    <div className="bg-white p-8 rounded-2xl shadow-sm border border-emerald-100 flex flex-col items-center text-center">
                        <div className="p-4 bg-emerald-50 text-emerald-600 rounded-full mb-4"><TrendingUp size={32}/></div>
                        <h3 className="font-bold text-slate-700 text-lg">Liquidity Status</h3>
                        <div className="text-3xl font-bold text-slate-900 mt-2">Stable</div>
                    </div>
                    <div className="bg-white p-8 rounded-2xl shadow-sm border border-blue-100 flex flex-col items-center text-center">
                        <div className="p-4 bg-blue-50 text-blue-600 rounded-full mb-4"><Gavel size={32}/></div>
                        <h3 className="font-bold text-slate-700 text-lg">Pending Sign-offs</h3>
                        <div className="text-3xl font-bold text-indigo-600 mt-2">
                            {approvalQueue.length}
                        </div>
                    </div>

                    {/* Share Capital Card */}
                    <ShareCapitalCard />
                </div>

                {/* 2. FINAL APPROVAL TABLE */}
                <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
                    <div className="p-6 border-b border-slate-100 bg-slate-50/50 flex justify-between items-center">
                        <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                            <CheckCircle size={20} className="text-indigo-600"/> Executive Sign-Off Required
                        </h2>
                        <span className="text-xs bg-indigo-100 text-indigo-700 px-3 py-1 rounded-full font-bold">
                            {approvalQueue.length} Pending
                        </span>
                    </div>

                    <table className="w-full text-sm text-left">
                        <thead className="bg-slate-50 text-slate-500 font-bold border-b border-slate-100 uppercase text-xs">
                            <tr>
                                <th className="p-4">Ref</th>
                                <th className="p-4">Applicant</th>
                                <th className="p-4 text-right">Amount</th>
                                <th className="p-4">Committee Status</th>
                                <th className="p-4 text-center">Action</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {loading ? (
                                <tr><td colSpan="5" className="p-12 text-center"><BrandedSpinner size="medium"/></td></tr>
                            ) : approvalQueue.length === 0 ? (
                                <tr><td colSpan="5" className="p-8 text-center text-slate-400 italic">No loans pending final executive approval.</td></tr>
                            ) : approvalQueue.map(loan => (
                                <tr key={loan.id} className="hover:bg-slate-50 transition">
                                    <td className="p-4 font-mono text-slate-500 text-xs">{loan.loanNumber}</td>
                                    <td className="p-4">
                                        <p className="font-bold text-slate-700">{loan.memberName}</p>
                                    </td>
                                    <td className="p-4 text-right font-mono font-bold text-slate-700">
                                        KES {Number(loan.principalAmount).toLocaleString()}
                                    </td>

                                    {/* ✅ SHOW STATUS FROM SECRETARY */}
                                    <td className="p-4 text-xs">
                                        <span className="bg-emerald-50 text-emerald-700 px-2 py-1 rounded font-bold border border-emerald-100">
                                            Passed Vote
                                        </span>
                                    </td>

                                    <td className="p-4 flex justify-center">
                                        <button
                                            onClick={() => handleFinalApproval(loan)}
                                            className="flex items-center gap-2 px-6 py-2 bg-indigo-900 text-white rounded-lg text-xs font-bold hover:bg-indigo-800 transition shadow-lg shadow-indigo-900/20"
                                        >
                                            <Gavel size={14}/> Grant Final Approval
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </main>
        </div>
    );
}