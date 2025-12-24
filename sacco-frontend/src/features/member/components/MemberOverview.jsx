import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { Wallet, AlertCircle, TrendingUp, CreditCard, Bell } from 'lucide-react';
import ShareCapitalCard from '../../../components/ShareCapitalCard';

export default function MemberOverview({ user }) {
    const [balanceData, setBalanceData] = useState({ balance: 0, accounts: [] });
    const [loans, setLoans] = useState([]);
    const [notifications, setNotifications] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const loadData = async () => {
            try {
                // 1. Get Savings Balance (Self-Service Endpoint)
                const balRes = await api.get('/api/savings/my-balance');
                if (balRes.data.success) setBalanceData(balRes.data);

                // 2. Get Notifications
                const notifRes = await api.get('/api/notifications');
                if (notifRes.data.success) setNotifications(notifRes.data.data.slice(0, 5));

                // 3. Get Loans (Requires Id - typically attached to User or fetched separately)
                // Assuming user object has Id, or we rely on the backend finding it via context
                if (user?.Id) {
                    const loanRes = await api.get(`/api/loans/member/${user.Id}`);
                    if (loanRes.data.success) setLoans(loanRes.data.data);
                }

                setLoading(false);
            } catch (e) {
                console.error("Dashboard Load Error", e);
                setLoading(false);
            }
        };
        loadData();
    }, [user]);

    // Calculate Loan Totals
    const totalLoanBalance = loans.reduce((acc, loan) => acc + (loan.loanBalance || 0), 0);
    const activeLoansCount = loans.filter(l => l.status === 'DISBURSED' || l.status === 'APPROVED').length;

    if (loading) return <div className="p-8 text-center text-slate-400">Loading your dashboard...</div>;

    return (
        <div className="space-y-6 animate-in fade-in">

            {/* Welcome Banner */}
            <div className="bg-indigo-900 text-white p-6 rounded-2xl shadow-lg flex flex-col md:flex-row justify-between items-center gap-4">
                <div>
                    <h2 className="text-2xl font-bold">Welcome back, {user?.firstName}!</h2>
                    <p className="text-indigo-200 text-sm">Here is what is happening with your account today.</p>
                </div>
                <div className="bg-white/10 px-4 py-2 rounded-lg text-center backdrop-blur-sm border border-white/10">
                    <p className="text-xs text-indigo-200 uppercase font-bold">Total Savings</p>
                    <p className="text-xl font-bold font-mono">KES {Number(balanceData.balance).toLocaleString()}</p>
                </div>
            </div>

            {/* Stats Grid */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div className="bg-white p-5 rounded-xl shadow-sm border border-slate-200">
                    <div className="flex justify-between items-start mb-2">
                        <div className="p-2 bg-emerald-100 text-emerald-600 rounded-lg"><Wallet size={20} /></div>
                        <span className="text-xs font-bold text-slate-400 uppercase">Accounts</span>
                    </div>
                    <h3 className="text-2xl font-bold text-slate-800">{balanceData.accounts?.length || 0}</h3>
                    <p className="text-xs text-slate-500">Active Savings Accounts</p>
                </div>

                <div className="bg-white p-5 rounded-xl shadow-sm border border-slate-200">
                    <div className="flex justify-between items-start mb-2">
                        <div className="p-2 bg-purple-100 text-purple-600 rounded-lg"><CreditCard size={20} /></div>
                        <span className="text-xs font-bold text-slate-400 uppercase">Loan Balance</span>
                    </div>
                    <h3 className="text-2xl font-bold text-slate-800">KES {Number(totalLoanBalance).toLocaleString()}</h3>
                    <p className="text-xs text-slate-500">{activeLoansCount} Active Loans</p>
                </div>

                <div className="bg-white p-5 rounded-xl shadow-sm border border-slate-200">
                    <div className="flex justify-between items-start mb-2">
                        <div className="p-2 bg-amber-100 text-amber-600 rounded-lg"><TrendingUp size={20} /></div>
                        <span className="text-xs font-bold text-slate-400 uppercase">Interest Earned</span>
                    </div>
                    <h3 className="text-2xl font-bold text-slate-800">
                        KES {Number(balanceData.interestEarned || 0).toLocaleString()}
                    </h3>
                    <p className="text-xs text-slate-500">Total Accrued</p>
                </div>

                {/* Share Capital Card */}
                <ShareCapitalCard forCurrentUser={true} showOwnershipPercentage={true} />
            </div>

            {/* Recent Notifications */}
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                <div className="p-4 border-b bg-slate-50 flex items-center gap-2">
                    <Bell size={16} className="text-slate-500"/>
                    <h3 className="font-bold text-slate-700 text-sm">Recent Alerts</h3>
                </div>
                <div className="divide-y">
                    {notifications.length === 0 ? (
                        <div className="p-6 text-center text-slate-400 text-sm italic">No new notifications.</div>
                    ) : (
                        notifications.map(n => (
                            <div key={n.id} className="p-4 hover:bg-slate-50 flex gap-3">
                                <div className="mt-1"><AlertCircle size={16} className="text-blue-500"/></div>
                                <div>
                                    <p className="text-sm font-bold text-slate-800">{n.title}</p>
                                    <p className="text-xs text-slate-600 mt-1">{n.message}</p>
                                    <p className="text-[10px] text-slate-400 mt-2">{new Date(n.createdAt).toLocaleString()}</p>
                                </div>
                            </div>
                        ))
                    )}
                </div>
            </div>
        </div>
    );
}