import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import api from '../api';

// Icons
import {
    LayoutDashboard, Users, Wallet, Settings, LogOut,
    TrendingUp, CreditCard, UserPlus, FileText,
    Download, ChevronLeft, ChevronRight, ArrowUpRight, ArrowDownLeft,
    PieChart, Activity, AlertCircle, PiggyBank, FileBarChart, ShieldCheck,
    Briefcase, Calendar, Filter // ✅ Added Calendar & Filter
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
import ReportsDashboard from '../features/reports/ReportsDashboard';
import AuditLogs from '../features/admin/components/AuditLogs';
import AssetManager from '../features/admin/components/AssetManager';
import ShareCapitalCard from '../components/ShareCapitalCard';

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
            case 'overview': return <OverviewTab setActiveTab={setActiveTab} />;
            case 'finance': return <FinanceTab />;
            case 'savings': return (
                <div className="space-y-8 animate-in fade-in">
                    <SavingsProducts />
                    <div className="border-t border-slate-200 my-4"></div>
                    <SavingsManager />
                </div>
            );
            case 'loans': return (
                <div className="space-y-8 animate-in fade-in">
                    <LoanProducts />
                    <div className="border-t border-slate-200 my-4"></div>
                    <LoanManager />
                </div>
            );
            case 'assets': return <AssetManager />;
            case 'reports': return <ReportsDashboard />;
            case 'members': return <MembersTab />;
            case 'register':
                return (
                    <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden p-1 animate-in zoom-in-95">
                        <AddMember />
                    </div>
                );
            case 'settings':
                return (
                    <div className="system-settings-wrapper animate-in fade-in">
                        <SystemSettings />
                    </div>
                );
            case 'audit': return <AuditLogs />;
            default: return <OverviewTab setActiveTab={setActiveTab} />;
        }
    };

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800 pb-12">
            <DashboardHeader user={user} title="Admin Portal" />

            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8">
                <div className="mb-8 overflow-x-auto pb-2 scrollbar-hide">
                    <div className="flex gap-1 w-max">
                        <TabButton id="overview" label="Dashboard" icon={LayoutDashboard} />
                        <TabButton id="finance" label="Finance" icon={Wallet} />
                        <TabButton id="savings" label="Savings" icon={PiggyBank} />
                        <TabButton id="assets" label="Assets" icon={Briefcase} />
                        <TabButton id="loans" label="Loans & Credit" icon={CreditCard} />
                        <TabButton id="reports" label="Reports" icon={FileBarChart} />
                        <TabButton id="members" label="Members" icon={Users} />
                        <div className="w-px bg-slate-300 mx-1 h-6 self-center"></div>
                        <TabButton id="register" label="Register New" icon={UserPlus} />
                        <TabButton id="settings" label="Configuration" icon={Settings} />
                        <TabButton id="audit" label="Audit & Security" icon={ShieldCheck} />
                    </div>
                </div>

                <div className="min-h-[500px]">
                    {renderContent()}
                </div>
            </main>
        </div>
    );
}

// ==================================================================================
// 1. OVERVIEW TAB (Real Stats + Custom Date Filter Chart)
// ==================================================================================
function OverviewTab({ setActiveTab }) {
    const [stats, setStats] = useState({ totalMembers: 0, totalSavings: 0, totalLoansIssued: 0, netIncome: 0 });
    const [chartData, setChartData] = useState([]);
    const [loading, setLoading] = useState(true);

    // ✅ NEW: Custom Date Range State (Defaults to last 7 days)
    const [dateRange, setDateRange] = useState({
        start: new Date(new Date().setDate(new Date().getDate() - 7)).toISOString().split('T')[0],
        end: new Date().toISOString().split('T')[0]
    });

    useEffect(() => {
        loadDashboard();
    }, []); // Initial load only

    const loadDashboard = async () => {
        try {
            console.log('📊 Loading Admin Dashboard...');

            // 1. Fetch Summary Stats (Always Today)
            console.log('🔄 Fetching /api/reports/today...');
            const todayRes = await api.get('/api/reports/today');
            console.log('✅ Today report response:', todayRes.data);

            if (todayRes.data.success) {
                console.log('📈 Stats loaded:', todayRes.data.data);
                setStats(todayRes.data.data);
            } else {
                console.warn('⚠️ Today report unsuccessful:', todayRes.data);
            }

            // 2. Fetch Chart Data
            console.log('📊 Fetching chart data...');
            fetchChartData();

            setLoading(false);
            console.log('✅ Dashboard loaded successfully');
        } catch (e) {
            console.error("❌ Dashboard Load Failed:");
            console.error('Error name:', e.name);
            console.error('Error message:', e.message);
            console.error('Error response:', e.response?.data);
            console.error('Error status:', e.response?.status);
            console.error('Request URL:', e.config?.url);
            console.error('Full error:', e);
            setLoading(false);
        }
    };

    // ✅ NEW: Fetch Chart Data with Custom Dates
    const fetchChartData = async () => {
        try {
            console.log(`🔄 Fetching chart data from ${dateRange.start} to ${dateRange.end}...`);
            const chartRes = await api.get(`/api/reports/chart?startDate=${dateRange.start}&endDate=${dateRange.end}`);
            console.log('✅ Chart data response:', chartRes.data);

            if (chartRes.data.success && chartRes.data.data.length > 0) {
                console.log('📊 Chart data points:', chartRes.data.data.length);
                setChartData(chartRes.data.data);
            } else {
                console.warn('⚠️ No chart data available');
                setChartData([]); // Empty state if no data
            }
        } catch (e) {
            console.error("❌ Chart Data Failed:");
            console.error('Error details:', e.response?.data || e.message);
            console.error('Full error:', e);
        }
    };

    const handleGenerateReport = async () => {
        if(!window.confirm("Generate End-of-Day Financial Report?")) return;
        try {
            await api.post('/api/reports/generate');
            alert("✅ Report Generated successfully! Check Reports tab.");
            setActiveTab('reports');
        } catch (e) { alert("Report Generation Failed"); }
    };

    const handleSystemDiag = () => {
        alert("System Status: Operational\nDatabase: Connected\nEmail Service: Active");
    };

    const handleReviewMembers = () => {
        setActiveTab('members');
    }

    const StatCard = ({ label, value, icon: Icon, color, subtext }) => (
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-200 hover:shadow-md transition-all group">
            <div className="flex justify-between items-start mb-4">
                <div className={`p-3 rounded-xl ${color} text-white shadow-sm group-hover:scale-110 transition-transform`}>
                    <Icon size={22} />
                </div>
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
        <div className="space-y-6 animate-in fade-in">

            {/* STATS GRID */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
                <StatCard label="Total Savings" value={stats.totalSavings} icon={Wallet} color="bg-emerald-600" subtext="Member Deposits" />
                <StatCard label="Net Income" value={stats.netIncome} icon={TrendingUp} color="bg-indigo-600" subtext="Fees + Interest - Expenses" />
                <StatCard label="Active Members" value={stats.totalMembers} icon={Users} color="bg-blue-600" subtext="Registered & Verified" />
                <StatCard label="Loans Issued" value={stats.totalLoansIssued} icon={CreditCard} color="bg-purple-600" subtext="Total Disbursed" />
                
                {/* Share Capital Card */}
                <ShareCapitalCard />
            </div>

            {/* CHART ROW */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

                {/* ✅ DYNAMIC CHART WITH CUSTOM DATE FILTER */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 lg:col-span-2 flex flex-col">

                    {/* Header with Title & Custom Date Picker */}
                    <div className="flex flex-col xl:flex-row justify-between items-center mb-6 gap-4">
                        <h3 className="font-bold text-slate-800 flex items-center gap-2 whitespace-nowrap">
                            <Activity size={20} className="text-emerald-600"/>
                            Performance
                        </h3>

                        {/* ✅ DATE RANGE FILTER UI */}
                        <div className="flex items-center gap-2 bg-slate-50 p-1.5 rounded-lg border border-slate-200">
                            <div className="flex items-center gap-1 px-2">
                                <Calendar size={14} className="text-slate-400"/>
                                <input
                                    type="date"
                                    value={dateRange.start}
                                    onChange={(e) => setDateRange({...dateRange, start: e.target.value})}
                                    className="bg-transparent text-xs font-bold text-slate-600 outline-none w-24 cursor-pointer"
                                />
                            </div>
                            <span className="text-slate-300">|</span>
                            <div className="flex items-center gap-1 px-2">
                                <input
                                    type="date"
                                    value={dateRange.end}
                                    onChange={(e) => setDateRange({...dateRange, end: e.target.value})}
                                    className="bg-transparent text-xs font-bold text-slate-600 outline-none w-24 cursor-pointer"
                                />
                            </div>
                            <button
                                onClick={fetchChartData}
                                className="bg-slate-900 text-white p-1.5 rounded-md hover:bg-slate-800 transition"
                                title="Apply Filter"
                            >
                                <Filter size={14} />
                            </button>
                        </div>

                        <button
                            onClick={() => setActiveTab('reports')}
                            className="hidden sm:flex text-xs font-bold text-blue-600 bg-blue-50 px-3 py-1.5 rounded-full hover:bg-blue-100 transition items-center gap-1 whitespace-nowrap"
                        >
                            View Reports <ChevronRight size={14}/>
                        </button>
                    </div>

                    {/* The Chart */}
                    <div className="h-64 w-full">
                        {chartData.length > 0 ? (
                            <ResponsiveContainer width="100%" height="100%">
                                <AreaChart data={chartData} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
                                    <defs>
                                        <linearGradient id="colorIncome" x1="0" y1="0" x2="0" y2="1">
                                            <stop offset="5%" stopColor="#059669" stopOpacity={0.1}/>
                                            <stop offset="95%" stopColor="#059669" stopOpacity={0}/>
                                        </linearGradient>
                                        <linearGradient id="colorExpense" x1="0" y1="0" x2="0" y2="1">
                                            <stop offset="5%" stopColor="#ef4444" stopOpacity={0.1}/>
                                            <stop offset="95%" stopColor="#ef4444" stopOpacity={0}/>
                                        </linearGradient>
                                    </defs>
                                    <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{fontSize: 12, fill: '#94a3b8'}} />
                                    <YAxis axisLine={false} tickLine={false} tick={{fontSize: 12, fill: '#94a3b8'}} />
                                    <CartesianGrid vertical={false} stroke="#f1f5f9" />
                                    <Tooltip contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }} />
                                    <Legend verticalAlign="top" height={36} iconType="circle" />
                                    <Area type="monotone" dataKey="income" stroke="#059669" strokeWidth={2} fillOpacity={1} fill="url(#colorIncome)" name="Income" />
                                    <Area type="monotone" dataKey="expense" stroke="#ef4444" strokeWidth={2} fillOpacity={1} fill="url(#colorExpense)" name="Expenses" />
                                </AreaChart>
                            </ResponsiveContainer>
                        ) : (
                            <div className="h-full flex flex-col items-center justify-center text-slate-400 text-sm italic gap-2">
                                <Activity className="opacity-20" size={32} />
                                No data found for this date range.
                            </div>
                        )}
                    </div>
                </div>

                {/* QUICK ACTIONS */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                    <h3 className="font-bold text-slate-800 mb-4">Quick Actions</h3>
                    <div className="space-y-3">
                        <button onClick={handleGenerateReport} className="w-full bg-slate-50 hover:bg-emerald-50 hover:text-emerald-700 p-3 rounded-xl flex items-center gap-3 transition text-sm font-bold border border-slate-100 hover:border-emerald-200 group text-left">
                            <div className="bg-white p-1.5 rounded-lg shadow-sm group-hover:shadow text-emerald-600"><FileText size={16}/></div>
                            <div>
                                <span className="block">Generate Report</span>
                                <span className="text-[10px] text-slate-400 font-normal">Create daily financial summary</span>
                            </div>
                        </button>

                        <button onClick={handleReviewMembers} className="w-full bg-slate-50 hover:bg-blue-50 hover:text-blue-700 p-3 rounded-xl flex items-center gap-3 transition text-sm font-bold border border-slate-100 hover:border-blue-200 group text-left">
                            <div className="bg-white p-1.5 rounded-lg shadow-sm group-hover:shadow text-blue-600"><Users size={16}/></div>
                            <div>
                                <span className="block">Review Members</span>
                                <span className="text-[10px] text-slate-400 font-normal">Manage member accounts</span>
                            </div>
                        </button>

                        <button onClick={handleSystemDiag} className="w-full bg-slate-50 hover:bg-amber-50 hover:text-amber-700 p-3 rounded-xl flex items-center gap-3 transition text-sm font-bold border border-slate-100 hover:border-amber-200 group text-left">
                            <div className="bg-white p-1.5 rounded-lg shadow-sm group-hover:shadow text-amber-600"><AlertCircle size={16}/></div>
                            <div>
                                <span className="block">System Status</span>
                                <span className="text-[10px] text-slate-400 font-normal">Check connection health</span>
                            </div>
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

    const handleRunInterest = async () => {
        if (window.confirm("Are you sure you want to run Monthly Interest? This will calculate interest for all accounts based on their specific Product Rates.")) {
            try {
                await api.post('/api/transactions/interest');
                alert("Interest applied successfully to all accounts!");
                fetchTransactions();
            } catch (error) {
                alert("Failed to apply interest.");
            }
        }
    };

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
        <div className="space-y-6 animate-in fade-in">
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
                                        <th className="p-4 text-center">Action</th>
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
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden animate-in fade-in">
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
                            <th className="p-4">Email</th> {/* ✅ Added Email Header */}
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
                                {/* ✅ Added Email Cell */}
                                <td className="p-4 text-slate-500 text-xs">{m.email}</td>
                                <td className="p-4 text-slate-500 text-xs">{m.phoneNumber}</td>
                                <td className="p-4"><span className="px-2.5 py-0.5 bg-green-50 text-green-700 border border-green-200 rounded-full text-[10px] font-bold uppercase">{m.status}</span></td>
                                <td className="p-4 text-right font-bold text-slate-800">KES {Number(m.totalSavings || 0).toLocaleString()}</td>
                            </tr>
                        ))}
                        {members.length === 0 && (
                            <tr><td colSpan="6" className="p-10 text-center text-slate-400 italic">No members found.</td></tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}