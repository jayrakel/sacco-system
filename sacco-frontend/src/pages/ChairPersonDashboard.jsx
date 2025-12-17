import React, { useState, useEffect } from 'react';
import { Users, TrendingUp, Activity } from 'lucide-react';
import DashboardHeader from '../components/DashboardHeader';

export default function ChairpersonDashboard() {
    const [user, setUser] = useState(null);

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
    }, []);

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800 pb-12">
            <DashboardHeader user={user} title="Executive Dashboard" />
            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8 space-y-8 animate-in fade-in">

                <div>
                    <h1 className="text-2xl font-bold text-slate-800">Executive Overview</h1>
                    <p className="text-slate-500 text-sm">High-level insights and system health status.</p>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <div className="bg-white p-8 rounded-2xl shadow-sm border border-indigo-100 flex flex-col items-center text-center">
                        <div className="p-4 bg-indigo-50 text-indigo-600 rounded-full mb-4"><Users size={32}/></div>
                        <h3 className="font-bold text-slate-700 text-lg">Membership Growth</h3>
                        <div className="text-3xl font-bold text-slate-900 mt-2">Active</div>
                        <p className="text-slate-400 text-sm">View full report in Reports Tab</p>
                    </div>

                    <div className="bg-white p-8 rounded-2xl shadow-sm border border-emerald-100 flex flex-col items-center text-center">
                        <div className="p-4 bg-emerald-50 text-emerald-600 rounded-full mb-4"><TrendingUp size={32}/></div>
                        <h3 className="font-bold text-slate-700 text-lg">Financial Health</h3>
                        <div className="text-3xl font-bold text-slate-900 mt-2">Stable</div>
                        <p className="text-slate-400 text-sm">Liquidity looks good</p>
                    </div>

                    <div className="bg-white p-8 rounded-2xl shadow-sm border border-blue-100 flex flex-col items-center text-center">
                        <div className="p-4 bg-blue-50 text-blue-600 rounded-full mb-4"><Activity size={32}/></div>
                        <h3 className="font-bold text-slate-700 text-lg">System Status</h3>
                        <div className="text-3xl font-bold text-green-600 mt-2">Online</div>
                        <p className="text-slate-400 text-sm">All services operational</p>
                    </div>
                </div>
            </main>
        </div>
    );
}