import React, { useState, useEffect } from 'react';
import api from '../api';
import { Landmark, CheckCircle, PieChart, AlertCircle, DollarSign } from 'lucide-react';
import DashboardHeader from '../components/DashboardHeader';
import BrandedSpinner from '../components/BrandedSpinner';

export default function FinanceDashboard() {
    const [user, setUser] = useState(null);
    const [pendingDisbursements, setPendingDisbursements] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
        fetchPending();
    }, []);

    const fetchPending = async () => {
        setLoading(true);
        try {
            // Fetch loans ready for money movement
            const res = await api.get('/api/loans/admin/pending');
            if (res.data.success) {
                // ✅ LOGIC UPDATE: Treasurer ONLY sees loans approved by Chairperson
                // Status must be: TREASURER_DISBURSEMENT
                setPendingDisbursements(res.data.data.filter(l =>
                    l.status === 'TREASURER_DISBURSEMENT'
                ));
            }
        } catch (e) {
            console.error(e);
        } finally {
            setLoading(false);
        }
    };

    const handleDisburse = async (loanId) => {
        if (!window.confirm("CONFIRM: Release funds to member account? This action moves actual money.")) return;

        try {
            await api.post(`/api/loans/admin/${loanId}/disburse`);
            alert("Disbursement Successful. Funds have been transferred.");
            fetchPending();
        } catch (err) {
            alert("Disbursement Failed: " + (err.response?.data?.message || err.message));
        }
    };

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800 pb-12">
            <DashboardHeader user={user} title="Treasury Portal" />
            <main className="max-w-7xl mx-auto px-4 mt-8 space-y-8">

                {/* Header Stats */}
                <div className="flex items-center gap-4 mb-8">
                    <div className="p-4 bg-emerald-100 text-emerald-600 rounded-full">
                        <PieChart size={32} />
                    </div>
                    <div>
                        <h1 className="text-2xl font-bold text-slate-800">Financial Operations</h1>
                        <p className="text-slate-500">Manage liquidity and finalize loan disbursements.</p>
                    </div>
                </div>

                {/* Disbursement Queue */}
                <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
                    <div className="p-6 border-b border-slate-100 bg-slate-50/50 flex justify-between items-center">
                        <h2 className="font-bold text-slate-800 flex items-center gap-2">
                            <Landmark size={20} className="text-emerald-600"/> Disbursement Queue
                        </h2>
                        <span className="bg-emerald-100 text-emerald-800 text-xs font-bold px-3 py-1 rounded-full">
                            {pendingDisbursements.length} Pending
                        </span>
                    </div>

                    {loading ? <div className="p-12 text-center"><BrandedSpinner /></div> : (
                        <div className="divide-y divide-slate-100">
                            {pendingDisbursements.length === 0 ? (
                                <div className="p-12 text-center text-slate-400 flex flex-col items-center gap-2">
                                    <CheckCircle size={40} className="text-slate-200"/>
                                    <p>All clear. No loans awaiting disbursement.</p>
                                </div>
                            ) : pendingDisbursements.map(loan => (
                                <div key={loan.id} className="p-6 flex flex-col md:flex-row justify-between items-center gap-4 hover:bg-slate-50 transition">
                                    <div>
                                        <div className="flex items-center gap-2">
                                            <h3 className="font-bold text-lg text-slate-800">{loan.memberName}</h3>
                                            <span className="text-[10px] bg-blue-100 text-blue-700 px-2 py-1 rounded font-bold uppercase border border-blue-200">
                                                Executive Approved
                                            </span>
                                        </div>
                                        <p className="text-slate-500 text-sm mt-1">
                                            Ref: <span className="font-mono text-slate-700">{loan.loanNumber}</span>
                                        </p>
                                    </div>

                                    <div className="flex items-center gap-6">
                                        <div className="text-right">
                                            <span className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider">Net Amount</span>
                                            <span className="block font-mono font-black text-emerald-600 text-xl">KES {Number(loan.principalAmount).toLocaleString()}</span>
                                        </div>

                                        <button
                                            onClick={() => handleDisburse(loan.id)}
                                            className="bg-emerald-600 text-white px-6 py-3 rounded-xl font-bold flex items-center gap-2 hover:bg-emerald-700 shadow-lg shadow-emerald-600/20 transition transform active:scale-95"
                                        >
                                            <DollarSign size={20}/> Release Funds
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </main>
        </div>
    );
}