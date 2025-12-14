import React, { useState, useEffect } from 'react';
import {
    TrendingUp, FileText, Printer,
    Wallet
} from 'lucide-react';
import api from '../../../api';

export default function FinanceOverview() {
    const [transactions, setTransactions] = useState([]);
    const [stats, setStats] = useState({
        totalIn: 0, deposits: 0, regFees: 0, fines: 0
    });
    const [loading, setLoading] = useState(true);

    const loadData = async () => {
        try {
            // Fetching transactions using your existing endpoint structure
            const response = await api.get('/api/transactions');
            const data = response.data.data || [];

            setTransactions(data);

            const safeSum = (arr) => arr.reduce((acc, c) => acc + (parseFloat(c.amount) || 0), 0);

            const dep = data.filter(d => d.type === 'DEPOSIT');
            const reg = data.filter(p => p.type === 'REGISTRATION_FEE');
            const fin = data.filter(p => p.type === 'FINE');

            setStats({
                totalIn: safeSum(data),
                deposits: safeSum(dep),
                regFees: safeSum(reg),
                fines: safeSum(fin),
            });
        } catch (error) { console.error("Load Error:", error); } finally { setLoading(false); }
    };

    useEffect(() => { loadData(); }, []);

    if (loading) return <div className="p-12 text-center text-slate-400">Loading Finance Data...</div>;

    return (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 animate-in fade-in">
            {/* LEFT COLUMN */}
            <div className="lg:col-span-2 space-y-6">

                {/* 1. HERO CARD */}
                <div className="bg-white rounded-2xl border border-slate-200 shadow-xl overflow-hidden">
                    <div className="bg-gradient-to-r from-emerald-900 to-teal-700 p-8 text-white relative">
                        <div className="relative z-10">
                            <p className="text-emerald-300 font-bold text-xs uppercase tracking-widest mb-1">Total Verified Inflows</p>
                            <h2 className="text-4xl font-extrabold mb-2">KES {stats.totalIn.toLocaleString()}</h2>
                            <p className="text-emerald-100/80 text-sm">Accumulated cash from all revenue streams.</p>
                        </div>
                        <div className="absolute right-0 bottom-0 p-6 opacity-10"><TrendingUp size={140} /></div>
                    </div>

                    {/* The "Fancy" Grid Breakdown */}
                    <div className="grid grid-cols-3 divide-x divide-y divide-slate-100 bg-white">
                        <div className="p-4 flex flex-col items-center text-center hover:bg-slate-50 transition">
                            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wide mb-1">Deposits</span>
                            <span className="text-lg font-bold text-emerald-700">KES {stats.deposits.toLocaleString()}</span>
                        </div>
                        <div className="p-4 flex flex-col items-center text-center hover:bg-slate-50 transition">
                            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wide mb-1">Reg Fees</span>
                            <span className="text-lg font-bold text-indigo-700">KES {stats.regFees.toLocaleString()}</span>
                        </div>
                        <div className="p-4 flex flex-col items-center text-center hover:bg-slate-50 transition">
                            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wide mb-1">Fines Paid</span>
                            <span className="text-lg font-bold text-amber-700">KES {stats.fines.toLocaleString()}</span>
                        </div>
                    </div>
                </div>

                {/* 2. RECENT ACTIVITY TABLE */}
                <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
                    <div className="p-5 border-b border-slate-100 bg-slate-50 flex justify-between items-center">
                        <h3 className="font-bold text-slate-800 flex items-center gap-2"><FileText size={18} className="text-slate-400"/> Recent Activity</h3>
                        <button onClick={() => window.print()} className="p-2 hover:bg-white rounded-lg transition text-slate-500"><Printer size={18}/></button>
                    </div>
                    <div className="overflow-x-auto">
                        <table className="w-full text-sm text-slate-600 text-left">
                            <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500"><tr><th className="px-6 py-3">Member</th><th className="px-6 py-3">Type</th><th className="px-6 py-3">Amount</th><th className="px-6 py-3">Date</th></tr></thead>
                            <tbody className="divide-y divide-slate-100">
                                {transactions.slice(0, 10).map((t, idx) => (
                                    <tr key={t.id || idx} className="hover:bg-slate-50 transition">
                                        <td className="px-6 py-4 font-medium text-slate-900">{t.member ? `${t.member.firstName} ${t.member.lastName}` : 'System'}</td>
                                        <td className="px-6 py-4"><span className={`px-2 py-1 rounded text-[10px] font-bold uppercase ${t.type === 'DEPOSIT' ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-600'}`}>{t.type}</span></td>
                                        <td className="px-6 py-4 font-mono font-bold text-slate-700">KES {parseFloat(t.amount).toLocaleString()}</td>
                                        <td className="px-6 py-4 text-xs text-slate-400">{new Date(t.transactionDate).toLocaleDateString()}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            {/* RIGHT COLUMN */}
            <div className="space-y-6">
                <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
                    <h3 className="font-bold text-slate-800 mb-4 flex items-center gap-2"><Wallet size={18} className="text-blue-600"/> Actions</h3>
                    <button className="w-full bg-slate-900 text-white py-3 rounded-xl font-bold mb-3 hover:bg-slate-800 transition">Record Transaction</button>
                    <button className="w-full bg-white border border-slate-300 text-slate-700 py-3 rounded-xl font-bold hover:bg-slate-50 transition">Generate Report</button>
                </div>
            </div>
        </div>
    );
}