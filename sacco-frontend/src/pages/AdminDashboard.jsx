import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';

// Icons
import {
    LayoutDashboard, Users, Wallet, Settings, LogOut,
    TrendingUp, CreditCard, UserPlus, FileText,
    Download, ChevronLeft, ChevronRight, ArrowUpRight, ArrowDownLeft,
    PieChart, Activity, AlertCircle, PiggyBank
} from 'lucide-react';

// Components
import DashboardHeader from '../components/DashboardHeader';
import AddMember from './members/AddMember';
import SystemSettings from './admin/SystemSettings';
import TransactionModal from '../components/TransactionModal';
import AccountingReports from '../features/finance/components/AccountingReports';
import LoanManager from '../features/loans/components/LoanManager';
import LoanProducts from '../features/loans/components/LoanProducts';
import SavingsManager from '../features/savings/components/SavingsManager';
import SavingsProducts from '../features/savings/components/SavingsProducts';

export default function AdminDashboard() {
    const [user, setUser] = useState(null);
    const [activeTab, setActiveTab] = useState('overview');

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
    }, []);

    // --- TAB BUTTON COMPONENT ---
    const TabButton = ({ id, label, icon: Icon }) => (
        <button
            onClick={() => setActiveTab(id)}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl font-bold text-xs sm:text-sm transition-all duration-200 border whitespace-nowrap ${
                activeTab === id
                ? 'bg-indigo-900 text-white shadow-md border-indigo-900'
                : 'bg-white text-slate-600 hover:bg-slate-50 border-slate-200'
            }`}
        >
            <Icon size={16} className={activeTab === id ? "text-indigo-200" : "text-slate-400"}/>
            {label}
        </button>
    );

    // --- CONTENT SWITCHER ---
    const renderContent = () => {
        switch(activeTab) {
            case 'overview': return <OverviewTab />;
            case 'finance': return <FinanceTab />;
            case 'savings': return (
                <div className="space-y-8">
                    <SavingsProducts />
                    <div className="border-t border-slate-200 my-4"></div>
                    <SavingsManager />
                </div>
            );
            case 'loans': return (
                            <div className="space-y-8">
                                <LoanProducts />
                                <div className="border-t border-slate-200 my-4"></div>
                                <LoanManager />
                            </div>
                        );
            case 'members': return <MembersTab />;
            case 'register':
                return (
                    <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden p-1">
                        <AddMember />
                    </div>
                );
            case 'settings':
                return (
                    <div className="system-settings-wrapper">
                        <SystemSettings />
                    </div>
                );
            default: return <OverviewTab />;
        }
    };

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800 pb-12">

            {/* SHARED HEADER */}
            <DashboardHeader user={user} title="Admin Portal" />

            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8">

                {/* NAVIGATION BAR */}
                <div className="mb-8 overflow-x-auto pb-2 scrollbar-hide">
                    <div className="flex gap-2 w-max">
                        <TabButton id="overview" label="Dashboard" icon={LayoutDashboard} />
                        <TabButton id="finance" label="Finance" icon={Wallet} />
                        <TabButton id="savings" label="Savings" icon={PiggyBank} />
                        <TabButton id="loans" label="Loans & Credit" icon={CreditCard} />
                        <TabButton id="members" label="Members" icon={Users} />
                        <div className="w-px bg-slate-300 mx-1 h-6 self-center"></div>
                        <TabButton id="register" label="Register New" icon={UserPlus} />
                        <TabButton id="settings" label="Configuration" icon={Settings} />
                    </div>
                </div>

                {/* CONTENT AREA */}
                <div className="animate-in fade-in duration-300">
                    {renderContent()}
                </div>

            </main>
        </div>
    );
}

// ==================================================================================
// 1. OVERVIEW TAB (Real Stats)
// ==================================================================================
function OverviewTab() {
    const [stats, setStats] = useState({ totalMembers: 0, totalSavings: 0, totalLoansIssued: 0, netIncome: 0 });
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        api.get('/api/reports/today').then(res => {
            if (res.data.success) setStats(res.data.data);
            setLoading(false);
        }).catch(() => setLoading(false));
    }, []);

    const StatCard = ({ label, value, icon: Icon, color, subtext }) => (
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-200 hover:shadow-md transition-all group">
            <div className="flex justify-between items-start mb-4">
                <div className={`p-3 rounded-xl ${color} text-white shadow-sm group-hover:scale-110 transition-transform`}>
                    <Icon size={22} />
                </div>
                {/* Optional Trend Indicator */}
                <span className="bg-slate-50 text-slate-400 text-[10px] px-2 py-1 rounded-full font-bold uppercase tracking-wide">Today</span>
            </div>
            <div>
                <h3 className="text-2xl font-bold text-slate-800 tracking-tight mb-1">
                    {typeof value === 'number' && !label.includes('Members') ? `KES ${value.toLocaleString()}` : value}
                </h3>
                <p className="text-slate-500 text-xs font-bold uppercase tracking-wide">{label}</p>
                {subtext && <p className="text-xs text-slate-400 mt-1">{subtext}</p>}
            </div>
        </div>
    );

    if (loading) return <div className="p-10 text-center text-slate-400">Loading Dashboard...</div>;

    return (
        <div className="space-y-6">

            {/* STATS GRID */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <StatCard
                    label="Total Savings"
                    value={stats.totalSavings}
                    icon={Wallet}
                    color="bg-emerald-600"
                    subtext="Member Deposits"
                />
                <StatCard
                    label="Net Income"
                    value={stats.netIncome}
                    icon={TrendingUp}
                    color="bg-indigo-600"
                    subtext="Fees + Interest - Expenses"
                />
                <StatCard
                    label="Active Members"
                    value={stats.totalMembers}
                    icon={Users}
                    color="bg-blue-600"
                    subtext="Registered & Verified"
                />
                <StatCard
                    label="Loans Issued"
                    value={stats.totalLoansIssued}
                    icon={CreditCard}
                    color="bg-purple-600"
                    subtext="Total Disbursed"
                />
            </div>

            {/* SECOND ROW */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

                {/* Financial Health (Placeholder Chart) */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 lg:col-span-2">
                    <div className="flex justify-between items-center mb-6">
                        <h3 className="font-bold text-slate-800 flex items-center gap-2">
                            <Activity size={20} className="text-emerald-600"/> Financial Performance
                        </h3>
                        <button className="text-xs font-bold text-blue-600 bg-blue-50 px-3 py-1 rounded-full hover:bg-blue-100 transition">
                            View Report
                        </button>
                    </div>
                    <div className="h-64 flex items-center justify-center bg-slate-50 rounded-xl border border-dashed border-slate-200 text-slate-400 text-sm">
                        <div className="text-center">
                            <PieChart size={40} className="mx-auto mb-2 opacity-20"/>
                            <p>Analytics Chart Loading...</p>
                        </div>
                    </div>
                </div>

                {/* Quick Actions */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                    <h3 className="font-bold text-slate-800 mb-4">Quick Actions</h3>
                    <div className="space-y-3">
                        <button className="w-full bg-slate-50 hover:bg-emerald-50 hover:text-emerald-700 p-3 rounded-xl flex items-center gap-3 transition text-sm font-bold border border-slate-100 hover:border-emerald-200 group">
                            <div className="bg-white p-1.5 rounded-lg shadow-sm group-hover:shadow text-emerald-600"><FileText size={16}/></div>
                            Generate Monthly Report
                        </button>
                        <button className="w-full bg-slate-50 hover:bg-blue-50 hover:text-blue-700 p-3 rounded-xl flex items-center gap-3 transition text-sm font-bold border border-slate-100 hover:border-blue-200 group">
                            <div className="bg-white p-1.5 rounded-lg shadow-sm group-hover:shadow text-blue-600"><Users size={16}/></div>
                            Review Pending Members
                        </button>
                        <button className="w-full bg-slate-50 hover:bg-amber-50 hover:text-amber-700 p-3 rounded-xl flex items-center gap-3 transition text-sm font-bold border border-slate-100 hover:border-amber-200 group">
                            <div className="bg-white p-1.5 rounded-lg shadow-sm group-hover:shadow text-amber-600"><AlertCircle size={16}/></div>
                            System Diagnostics
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}

// ==================================================================================
// 2. FINANCE TAB (Real Transactions)
// ==================================================================================
function FinanceTab() {
    const [viewMode, setViewMode] = useState('standard');
    const [transactions, setTransactions] = useState([]);
    const [currentPage, setCurrentPage] = useState(1);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const itemsPerPage = 10;

    const fetchTransactions = () => {
        api.get('/api/transactions').then(res => {
            if(res.data.success) setTransactions(res.data.data);
        });
    };

    useEffect(() => { fetchTransactions(); }, []);

    // ✅ NEW: Handle Interest
    const handleRunInterest = async () => {
        const rate = prompt("Enter Annual Interest Rate % (e.g. 5 for 5%):", "5");
        if (rate) {
            try {
                await api.post('/api/transactions/interest', null, { params: { rate } });
                alert("Interest applied successfully to all accounts!");
                fetchTransactions(); // Refresh list to show interest entries
            } catch (error) {
                alert("Failed to apply interest.");
            }
        }
    };

    // ✅ NEW: Handle Reversal
    const handleReverse = async (txId) => {
        if (!window.confirm("Are you sure you want to REVERSE this transaction? This cannot be undone.")) return;

        const reason = prompt("Enter reason for reversal:");
        if (!reason) return;

        try {
            await api.post(`/api/transactions/${txId}/reverse`, null, { params: { reason } });
            alert("Transaction Reversed!");
            fetchTransactions();
        } catch (error) {
            alert(error.response?.data?.message || "Reversal Failed");
        }
    };

    const handleDownload = () => {
        window.location.href = 'http://localhost:8080/api/transactions/download';
    };

    const lastIndex = currentPage * itemsPerPage;
    const firstIndex = lastIndex - itemsPerPage;
    const currentTx = transactions.slice(firstIndex, lastIndex);
    const totalPages = Math.ceil(transactions.length / itemsPerPage);

    return (
        <div className="space-y-6">

            <div className="flex bg-slate-200/50 p-1 rounded-xl w-fit">
                <button onClick={() => setViewMode('standard')} className={`px-4 py-2 text-sm font-bold rounded-lg transition ${viewMode === 'standard' ? 'bg-white shadow text-slate-800' : 'text-slate-500 hover:text-slate-700'}`}>Transactions</button>
                <button onClick={() => setViewMode('accounting')} className={`px-4 py-2 text-sm font-bold rounded-lg transition ${viewMode === 'accounting' ? 'bg-white shadow text-indigo-700' : 'text-slate-500 hover:text-slate-700'}`}>Accounting Books</button>
            </div>

            {viewMode === 'accounting' ? (
                <AccountingReports />
            ) : (
                <>
                    <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
                        <div className="p-5 border-b border-slate-100 flex flex-col md:flex-row justify-between items-center bg-slate-50/50 gap-4">
                            <div>
                                <h2 className="text-lg font-bold text-slate-800">Financial Ledger</h2>
                                <p className="text-slate-500 text-xs">Complete history of all fees, deposits, and transfers.</p>
                            </div>
                            <div className="flex gap-2">
                                <button onClick={handleRunInterest} className="flex items-center gap-2 bg-indigo-600 text-white px-4 py-2 rounded-lg text-xs font-bold hover:bg-indigo-700 transition shadow-lg shadow-indigo-600/20">
                                    <TrendingUp size={14} /> Run Interest
                                </button>
                                <button onClick={() => setIsModalOpen(true)} className="flex items-center gap-2 bg-emerald-600 text-white px-4 py-2 rounded-lg text-xs font-bold hover:bg-emerald-700 transition shadow-lg shadow-emerald-600/20">
                                    <ArrowDownLeft size={14} /> New Transaction
                                </button>
                                <button onClick={handleDownload} className="flex items-center gap-2 bg-slate-900 text-white px-4 py-2 rounded-lg text-xs font-bold hover:bg-slate-800 transition shadow-lg shadow-slate-900/20">
                                    <Download size={14} /> CSV
                                </button>
                            </div>
                        </div>

                        <div className="overflow-x-auto">
                            <table className="w-full text-left text-sm">
                                <thead className="bg-slate-50 text-slate-500 uppercase font-bold text-xs tracking-wider border-b border-slate-100">
                                    <tr>
                                        <th className="p-4">Ref No</th>
                                        <th className="p-4">Date</th>
                                        <th className="p-4">Member</th>
                                        <th className="p-4">Type</th>
                                        <th className="p-4 text-right">Amount</th>
                                        <th className="p-4 text-center">Action</th> {/* New Action Column */}
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-100">
                                    {currentTx.map((tx) => (
                                        <tr key={tx.id} className="hover:bg-slate-50 transition duration-150">
                                            <td className="p-4 font-mono text-xs text-slate-500">{tx.transactionId}</td>
                                            <td className="p-4 text-slate-700">{new Date(tx.transactionDate).toLocaleDateString()}</td>
                                            <td className="p-4 font-medium text-slate-900">
                                                {tx.member ? `${tx.member.firstName} ${tx.member.lastName}` : 'System'}
                                            </td>
                                            <td className="p-4">
                                                <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-bold border ${
                                                    tx.type === 'DEPOSIT' ? 'bg-emerald-50 text-emerald-700 border-emerald-100' :
                                                    tx.type === 'WITHDRAWAL' ? 'bg-amber-50 text-amber-700 border-amber-100' :
                                                    tx.type === 'REVERSAL' ? 'bg-gray-100 text-gray-600 border-gray-200' :
                                                    'bg-blue-50 text-blue-700 border-blue-100'
                                                }`}>
                                                    {tx.type.replace(/_/g, ' ')}
                                                </span>
                                            </td>
                                            <td className="p-4 text-right font-bold text-slate-800">KES {Number(tx.amount).toLocaleString()}</td>

                                            {/* ✅ REVERSE BUTTON */}
                                            <td className="p-4 text-center">
                                                {tx.type !== 'REVERSAL' && (
                                                    <button
                                                        onClick={() => handleReverse(tx.transactionId)}
                                                        className="text-xs text-red-500 hover:text-red-700 font-bold underline decoration-dotted"
                                                        title="Reverse Transaction"
                                                    >
                                                        Reverse
                                                    </button>
                                                )}
                                            </td>
                                        </tr>
                                    ))}
                                    {transactions.length === 0 && <tr><td colSpan="6" className="p-10 text-center text-slate-400 italic">No transactions found.</td></tr>}
                                </tbody>
                            </table>
                        </div>

                        {totalPages > 1 && (
                            <div className="p-3 border-t border-slate-100 flex justify-end gap-2 bg-slate-50">
                                <button onClick={() => setCurrentPage(p => Math.max(1, p-1))} disabled={currentPage===1} className="p-2 rounded-lg bg-white border border-slate-200 hover:bg-slate-100 disabled:opacity-50"><ChevronLeft size={16}/></button>
                                <span className="py-2 px-4 text-xs font-bold text-slate-600 bg-white border border-slate-200 rounded-lg">Page {currentPage} of {totalPages}</span>
                                <button onClick={() => setCurrentPage(p => Math.min(totalPages, p+1))} disabled={currentPage===totalPages} className="p-2 rounded-lg bg-white border border-slate-200 hover:bg-slate-100 disabled:opacity-50"><ChevronRight size={16}/></button>
                            </div>
                        )}
                    </div>

                    <TransactionModal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} onSuccess={fetchTransactions} />
                </>
            )}
        </div>
    );
}

// ==================================================================================
// 3. MEMBERS TAB (Real Data)
// ==================================================================================
function MembersTab() {
    const [members, setMembers] = useState([]);

    useEffect(() => {
        api.get('/api/members').then(res => {
            if(res.data.success) setMembers(res.data.data);
        });
    }, []);

    return (
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
            <div className="p-5 border-b border-slate-100 flex justify-between items-center">
                <div>
                    <h2 className="text-lg font-bold text-slate-800">Member Directory</h2>
                    <p className="text-slate-500 text-xs">Active accounts and their current standing.</p>
                </div>
                <span className="bg-slate-100 text-slate-600 text-xs font-bold px-3 py-1 rounded-full border border-slate-200">{members.length} Total</span>
            </div>
            <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                    <thead className="bg-slate-50 text-slate-500 uppercase font-bold text-xs tracking-wider border-b border-slate-100">
                        <tr>
                            <th className="p-4">Member No</th>
                            <th className="p-4">Name</th>
                            <th className="p-4">Contact</th>
                            <th className="p-4">Status</th>
                            <th className="p-4 text-right">Savings</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                        {members.map((m) => (
                            <tr key={m.id} className="hover:bg-slate-50 transition">
                                <td className="p-4 font-mono text-slate-500 text-xs">{m.memberNumber}</td>
                                <td className="p-4 font-bold text-slate-800 flex items-center gap-3">
                                    <div className="w-8 h-8 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center text-xs font-bold border border-emerald-100">
                                        {m.firstName.charAt(0)}{m.lastName.charAt(0)}
                                    </div>
                                    {m.firstName} {m.lastName}
                                </td>
                                <td className="p-4 text-slate-500 text-xs">{m.phoneNumber}</td>
                                <td className="p-4"><span className="px-2.5 py-0.5 bg-green-50 text-green-700 border border-green-200 rounded-full text-[10px] font-bold uppercase">{m.status}</span></td>
                                <td className="p-4 text-right font-bold text-slate-800">KES {Number(m.totalSavings || 0).toLocaleString()}</td>
                            </tr>
                        ))}
                        {members.length === 0 && (
                            <tr><td colSpan="5" className="p-10 text-center text-slate-400 italic">No members found.</td></tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}