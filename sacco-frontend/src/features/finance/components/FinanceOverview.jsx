import React, { useState, useEffect } from 'react';
import {
    TrendingUp, FileText, Printer,
    Wallet, X, Download
} from 'lucide-react';
import api from '../../../api';
import { useSettings } from '../../../context/SettingsContext';

export default function FinanceOverview() {
    const [transactions, setTransactions] = useState([]);
    const [stats, setStats] = useState({
        totalIn: 0, deposits: 0, regFees: 0, fines: 0
    });
    const [loading, setLoading] = useState(true);
    const [showPrintView, setShowPrintView] = useState(false);

    // Get Organization Settings for the Print Header
    const { settings, getImageUrl } = useSettings();
    const logoUrl = getImageUrl(settings.SACCO_LOGO);
    const orgName = settings.SACCO_NAME || "Sacco System";
    const orgAddress = settings.SACCO_ADDRESS || "Nairobi, Kenya";

    const loadData = async () => {
        try {
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

    const handlePrint = () => {
        // 1. Show the print view first
        setShowPrintView(true);
        // 2. Small delay to ensure render, then trigger print dialog
        setTimeout(() => {
            window.print();
        }, 500);
    };

    if (loading) return <div className="p-12 text-center text-slate-400">Loading Finance Data...</div>;

    return (
        <div className="animate-in fade-in relative">
            
            {/* --- MAIN DASHBOARD CONTENT (Hidden when printing) --- */}
            <div className={`grid grid-cols-1 lg:grid-cols-3 gap-8 ${showPrintView ? 'hidden' : 'block'} print:hidden`}>
                {/* LEFT COLUMN */}
                <div className="lg:col-span-2 space-y-6">

                    {/* HERO CARD */}
                    <div className="bg-white rounded-2xl border border-slate-200 shadow-xl overflow-hidden">
                        <div className="bg-gradient-to-r from-emerald-900 to-teal-700 p-8 text-white relative">
                            <div className="relative z-10">
                                <p className="text-emerald-300 font-bold text-xs uppercase tracking-widest mb-1">Total Verified Inflows</p>
                                <h2 className="text-4xl font-extrabold mb-2">KES {stats.totalIn.toLocaleString()}</h2>
                                <p className="text-emerald-100/80 text-sm">Accumulated cash from all revenue streams.</p>
                            </div>
                            <div className="absolute right-0 bottom-0 p-6 opacity-10"><TrendingUp size={140} /></div>
                        </div>

                        {/* Breakdown Grid */}
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

                    {/* RECENT ACTIVITY TABLE */}
                    <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
                        <div className="p-5 border-b border-slate-100 bg-slate-50 flex justify-between items-center">
                            <h3 className="font-bold text-slate-800 flex items-center gap-2"><FileText size={18} className="text-slate-400"/> Recent Activity</h3>
                            <button onClick={handlePrint} className="p-2 hover:bg-white rounded-lg transition text-slate-500 flex items-center gap-2 text-xs font-bold border border-transparent hover:border-slate-200">
                                <Printer size={16}/> Print Report
                            </button>
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

            {/* --- PRINT OVERLAY (Only visible when printing or when state is true) --- */}
            {showPrintView && (
                <div className="fixed inset-0 bg-white z-[9999] overflow-auto animate-in fade-in">
                    {/* Floating Controls (Hidden on Paper) */}
                    <div className="fixed top-4 right-4 flex gap-2 print:hidden z-50">
                        <button onClick={() => window.print()} className="bg-slate-800 text-white px-4 py-2 rounded-lg font-bold shadow-lg flex items-center gap-2 hover:bg-slate-700">
                            <Printer size={16}/> Print Now
                        </button>
                        <button onClick={() => setShowPrintView(false)} className="bg-white text-slate-700 border border-slate-200 px-4 py-2 rounded-lg font-bold shadow-lg hover:bg-slate-50 flex items-center gap-2">
                            <X size={16}/> Close
                        </button>
                    </div>

                    {/* Paper Layout */}
                    <div className="max-w-[210mm] mx-auto bg-white p-10 min-h-screen">
                        
                        {/* Print Styles */}
                        <style>{`
                            @media print {
                                @page { size: A4; margin: 10mm; }
                                body { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
                                /* Explicitly hide everything else */
                                body > *:not(#root) { display: none; }
                            }
                        `}</style>

                        {/* Report Header */}
                        <div className="flex justify-between items-start border-b-2 border-slate-800 pb-6 mb-8">
                            <div>
                                {logoUrl && <img src={logoUrl} alt="Logo" className="h-16 object-contain mb-2" />}
                                <h1 className="text-2xl font-black text-slate-900 uppercase">FINANCIAL ACTIVITY REPORT</h1>
                                <p className="text-sm text-slate-600 mt-1">Generated: {new Date().toLocaleString()}</p>
                            </div>
                            <div className="text-right">
                                <h2 className="text-lg font-bold text-slate-800">{orgName}</h2>
                                <p className="text-xs text-slate-600 whitespace-pre-line">{orgAddress}</p>
                            </div>
                        </div>

                        {/* Summary Section */}
                        <div className="grid grid-cols-4 gap-4 mb-8 border border-slate-200 rounded-lg p-4 bg-slate-50 print:bg-slate-50">
                            <div>
                                <p className="text-[10px] uppercase font-bold text-slate-500">Total Inflows</p>
                                <p className="text-xl font-bold text-slate-900">{formatMoney(stats.totalIn)}</p>
                            </div>
                            <div>
                                <p className="text-[10px] uppercase font-bold text-slate-500">Deposits</p>
                                <p className="text-lg font-bold text-slate-900">{formatMoney(stats.deposits)}</p>
                            </div>
                            <div>
                                <p className="text-[10px] uppercase font-bold text-slate-500">Reg Fees</p>
                                <p className="text-lg font-bold text-slate-900">{formatMoney(stats.regFees)}</p>
                            </div>
                            <div>
                                <p className="text-[10px] uppercase font-bold text-slate-500">Fines</p>
                                <p className="text-lg font-bold text-slate-900">{formatMoney(stats.fines)}</p>
                            </div>
                        </div>

                        {/* Detailed Table */}
                        <h3 className="font-bold text-slate-800 mb-4 uppercase text-sm border-b pb-1">Recent Transactions</h3>
                        <table className="w-full text-sm text-left">
                            <thead className="bg-slate-100 text-slate-600 font-bold border-b border-slate-300">
                                <tr>
                                    <th className="px-4 py-2">Date</th>
                                    <th className="px-4 py-2">Member</th>
                                    <th className="px-4 py-2">Type</th>
                                    <th className="px-4 py-2 text-right">Amount (KES)</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-200">
                                {transactions.map((t, idx) => (
                                    <tr key={idx} className="break-inside-avoid">
                                        <td className="px-4 py-2 text-slate-600">{new Date(t.transactionDate).toLocaleDateString()}</td>
                                        <td className="px-4 py-2 font-medium text-slate-900">
                                            {t.member ? `${t.member.firstName} ${t.member.lastName}` : 'System'}
                                            <div className="text-[10px] text-slate-400 font-mono">{t.referenceCode}</div>
                                        </td>
                                        <td className="px-4 py-2 text-xs uppercase">{t.type}</td>
                                        <td className="px-4 py-2 text-right font-mono font-bold">{parseFloat(t.amount).toLocaleString()}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>

                        {/* Footer */}
                        <div className="mt-12 pt-4 border-t border-slate-200 text-center text-[10px] text-slate-400">
                            <p>End of Report • {orgName}</p>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

// Helper
const formatMoney = (amount) => Number(amount).toLocaleString(undefined, {minimumFractionDigits: 2});