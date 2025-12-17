import React, { useState, useEffect } from 'react';
import api from '../api';
import {
    Briefcase, CheckCircle, XCircle, Clock, Wallet
} from 'lucide-react';
import DashboardHeader from '../components/DashboardHeader';
import LoanManager from '../features/loans/components/LoanManager';

export default function LoansDashboard() {
    const [user, setUser] = useState(null);
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
    }, []);

    const fetchLoanStats = async () => {
        try {
            const res = await api.get('/api/loans');
            if (res.data.success) {
                const loans = res.data.data;
                setStats({
                    pending: loans.filter(l => l.status === 'PENDING').length,
                    active: loans.filter(l => l.status === 'APPROVED' || l.status === 'DISBURSED').length,
                    rejected: loans.filter(l => l.status === 'REJECTED').length,
                    totalDisbursed: loans
                        .filter(l => l.status === 'DISBURSED')
                        .reduce((sum, l) => sum + (l.principalAmount || 0), 0)
                });
            }
        } catch (e) {
            console.error("Failed to load stats", e);
        }
    };

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800 pb-12">
            <DashboardHeader user={user} title="Loan Officer Portal" />

            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8 space-y-8 animate-in fade-in">
                <div>
                    <h1 className="text-2xl font-bold text-slate-800">Loan Portfolio Overview</h1>
                    <p className="text-slate-500 text-sm">Manage applications, approvals, and disbursements.</p>
                </div>

                {/* Stat Cards */}
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                    <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-200 flex items-center gap-4">
                        <div className="p-3 bg-amber-100 text-amber-600 rounded-xl"><Clock size={24} /></div>
                        <div>
                            <p className="text-xs font-bold text-slate-400 uppercase">Pending Review</p>
                            <h3 className="text-2xl font-bold text-slate-800">{stats.pending}</h3>
                        </div>
                    </div>
                    <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-200 flex items-center gap-4">
                        <div className="p-3 bg-emerald-100 text-emerald-600 rounded-xl"><CheckCircle size={24} /></div>
                        <div>
                            <p className="text-xs font-bold text-slate-400 uppercase">Active Loans</p>
                            <h3 className="text-2xl font-bold text-slate-800">{stats.active}</h3>
                        </div>
                    </div>
                    <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-200 flex items-center gap-4">
                        <div className="p-3 bg-indigo-100 text-indigo-600 rounded-xl"><Wallet size={24} /></div>
                        <div>
                            <p className="text-xs font-bold text-slate-400 uppercase">Total Disbursed</p>
                            <h3 className="text-2xl font-bold text-slate-800">{(stats.totalDisbursed / 1000000).toFixed(1)}M <span className="text-xs text-slate-400">KES</span></h3>
                        </div>
                    </div>
                    <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-200 flex items-center gap-4">
                        <div className="p-3 bg-red-100 text-red-600 rounded-xl"><XCircle size={24} /></div>
                        <div>
                            <p className="text-xs font-bold text-slate-400 uppercase">Rejected</p>
                            <h3 className="text-2xl font-bold text-slate-800">{stats.rejected}</h3>
                        </div>
                    </div>
                </div>

                {/* Main Workspace */}
                <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
                    <div className="p-6 border-b border-slate-100 bg-slate-50/50">
                        <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                            <Briefcase size={20} className="text-slate-400"/> Loan Applications
                        </h2>
                    </div>
                    <LoanManager />
                </div>
            </main>
        </div>
    );
}