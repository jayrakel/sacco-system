import React, { useState, useEffect } from 'react';
import DashboardHeader from '../components/DashboardHeader';
import AccountingReports from '../features/finance/components/AccountingReports';

export default function FinanceDashboard() {
    const [user, setUser] = useState(null);

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
    }, []);

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800 pb-12">
            <DashboardHeader user={user} title="Treasurer Portal" />
            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8 space-y-8 animate-in fade-in">
                <div>
                    <h1 className="text-2xl font-bold text-slate-800">Financial Management</h1>
                    <p className="text-slate-500 text-sm">Ledgers, Reports, and Liquidity Monitoring.</p>
                </div>

                {/* Full Accounting Reports View */}
                <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-1">
                    <AccountingReports />
                </div>
            </main>
        </div>
    );
}