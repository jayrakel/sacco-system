import React, { useState, useEffect } from 'react';
import api from '../api';
import { Briefcase, CheckCircle, XCircle, Clock, Wallet, RefreshCw } from 'lucide-react';
import DashboardHeader from '../components/DashboardHeader';
import LoanManager from '../features/loans/components/LoanManager';

export default function LoansDashboard() {
    const [user, setUser] = useState(null);
    const [loadingStats, setLoadingStats] = useState(false);
    const [stats, setStats] = useState({
        pending: 0,
        active: 0,
        rejected: 0,
        totalDisbursed: 0
    });

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));

        fetchLoanStats();

        // ✅ AUTO-REFRESH: Update stats every 15 seconds
        const interval = setInterval(fetchLoanStats, 15000);
        return () => clearInterval(interval);
    }, []);

    const fetchLoanStats = async () => {
        setLoadingStats(true);
        try {
            const res = await api.get('/api/loans');
            if (res.data.success) {
                const loans = res.data.data;
                setStats({
                    pending: loans.filter(l => l.status === 'PENDING' || l.status === 'SUBMITTED').length,
                    active: loans.filter(l => ['APPROVED', 'DISBURSED', 'ACTIVE'].includes(l.status)).length,
                    rejected: loans.filter(l => l.status === 'REJECTED').length,
                    totalDisbursed: loans
                        .filter(l => l.status === 'DISBURSED' || l.status === 'ACTIVE')
                        .reduce((sum, l) => sum + (l.principalAmount || 0), 0)
                });
            }
        } catch (e) {
            console.error("Failed to load stats", e);
        } finally {
            setLoadingStats(false);
        }
    };

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800 pb-12">
            <DashboardHeader user={user} title="Loan Officer Portal" />

            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8 space-y-8 animate-in fade-in">
                <div className="flex justify-between items-end">
                    <div>
                        <h1 className="text-2xl font-bold text-slate-800">Loan Portfolio Overview</h1>
                        <p className="text-slate-500 text-sm">Manage applications, approvals, and disbursements.</p>
                    </div>
                    <button onClick={fetchLoanStats} className="p-2 bg-white border border-slate-200 rounded-full hover:bg-slate-50 text-slate-400 transition">
                        <RefreshCw size={18} className={loadingStats ? "animate-spin" : ""} />
                    </button>
                </div>

                {/* Dynamic Stat Cards */}
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                    <StatCard icon={<Clock size={24}/>} color="amber" label="Pending Review" value={stats.pending} />
                    <StatCard icon={<CheckCircle size={24}/>} color="emerald" label="Active Loans" value={stats.active} />
                    <StatCard icon={<Wallet size={24}/>} color="indigo" label="Total Disbursed" value={`KES ${(stats.totalDisbursed / 1000000).toFixed(1)}M`} />
                    <StatCard icon={<XCircle size={24}/>} color="red" label="Rejected" value={stats.rejected} />
                </div>

                {/* Main Workspace */}
                <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
                    <div className="p-6 border-b border-slate-100 bg-slate-50/50">
                        <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                            <Briefcase size={20} className="text-slate-400"/> Loan Applications
                        </h2>
                    </div>

                    {/* ✅ PASS REFRESH CALLBACK TO CHILD */}
                    <LoanManager onUpdate={fetchLoanStats} />
                </div>
            </main>
        </div>
    );
}

// Simple Helper Component for Cards
function StatCard({ icon, color, label, value }) {
    const colors = {
        amber: "bg-amber-100 text-amber-600",
        emerald: "bg-emerald-100 text-emerald-600",
        indigo: "bg-indigo-100 text-indigo-600",
        red: "bg-red-100 text-red-600",
    };

    return (
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-200 flex items-center gap-4 transition hover:shadow-md">
            <div className={`p-3 rounded-xl ${colors[color] || colors.indigo}`}>
                {icon}
            </div>
            <div>
                <p className="text-xs font-bold text-slate-400 uppercase">{label}</p>
                <h3 className="text-2xl font-bold text-slate-800">{value}</h3>
            </div>
        </div>
    );
}