import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import api from '../api';

// Icons
import {
    LayoutDashboard, Users, Wallet, Settings,
    TrendingUp, CreditCard, UserPlus, FileText,
    Download, ChevronLeft, ChevronRight, ArrowDownLeft, ArrowUpRight,
    Activity, AlertCircle, PiggyBank, FileBarChart, ShieldCheck,
    Briefcase, Filter, UserCog, Key, Menu, X, Calendar, ChevronDown, BookOpen, List
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
import SystemStatusModal from '../components/SystemStatusModal';

// ==================================================================================
// SIDEBAR COMPONENTS
// ==================================================================================

const SidebarItem = ({ icon: Icon, label, to, active, onClick }) => (
    <Link
        to={to}
        onClick={onClick}
        className={`flex items-center gap-3 px-4 py-3 mx-2 rounded-xl transition-all duration-200 group mb-1 ${
            active
            ? 'bg-indigo-600 text-white shadow-md shadow-indigo-200 font-bold'
            : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900 font-medium'
        }`}
    >
        <Icon size={20} className={`transition-colors ${active ? 'text-indigo-100' : 'text-slate-400 group-hover:text-slate-600'}`} />
        <span className="text-sm">{label}</span>
    </Link>
);

// Level 1 Group (e.g. Finance)
const SidebarGroup = ({ icon: Icon, label, active, expanded, onToggle, children }) => (
    <div className="mb-1 mx-2">
        <button
            onClick={onToggle}
            className={`w-full flex items-center justify-between px-4 py-3 rounded-xl transition-all duration-200 group
            ${active ? 'bg-indigo-50 text-indigo-900 font-bold' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900 font-medium'}`}
        >
            <div className="flex items-center gap-3">
                <Icon size={20} className={`transition-colors ${active ? 'text-indigo-600' : 'text-slate-400 group-hover:text-slate-600'}`} />
                <span className="text-sm">{label}</span>
            </div>
            {expanded ? <ChevronDown size={16} className="text-slate-400" /> : <ChevronRight size={16} className="text-slate-400" />}
        </button>
        {expanded && (
            <div className="mt-1 ml-4 pl-4 border-l-2 border-slate-100 space-y-1 animate-in slide-in-from-top-2 duration-200">
                {children}
            </div>
        )}
    </div>
);

// Level 2 Sub-Item (e.g. Transactions)
const SidebarSubItem = ({ label, to, active, onClick }) => (
    <Link
        to={to}
        onClick={onClick}
        className={`block px-4 py-2 rounded-lg text-xs font-bold transition-all duration-200
        ${active ? 'text-indigo-700 bg-indigo-50' : 'text-slate-500 hover:text-slate-700 hover:bg-slate-50'}`}
    >
        {label}
    </Link>
);

// Level 2 Sub-Group (e.g. Accounting Books)
const SidebarSubGroup = ({ label, expanded, onToggle, children }) => (
    <div className="mt-1">
        <button
            onClick={onToggle}
            className="w-full flex items-center justify-between px-4 py-2 rounded-lg text-xs font-bold text-slate-500 hover:text-slate-800 hover:bg-slate-50 transition-all"
        >
            <span>{label}</span>
            {expanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
        </button>
        {expanded && (
            <div className="ml-3 mt-1 pl-3 border-l border-slate-200 space-y-1">
                {children}
            </div>
        )}
    </div>
);

// Level 3 Item (e.g. Balance Sheet)
const SidebarDeepItem = ({ label, to, active, onClick }) => (
    <Link
        to={to}
        onClick={onClick}
        className={`block px-3 py-1.5 rounded-md text-[11px] font-medium transition-all
        ${active ? 'text-indigo-600 bg-indigo-50/50 font-bold' : 'text-slate-400 hover:text-slate-600'}`}
    >
        {label}
    </Link>
);

const AdminSidebar = ({ activeTab, closeMobile }) => {
    // Initialize states based on current tab
    const [financeExpanded, setFinanceExpanded] = useState(activeTab.startsWith('finance'));
    const [booksExpanded, setBooksExpanded] = useState(activeTab.includes('accounting'));

    // ✅ NEW: Automatically sync expanded state with the active tab.
    // This ensures that when you click a new tab (e.g. Savings), Finance collapses.
    useEffect(() => {
        setFinanceExpanded(activeTab.startsWith('finance'));
        setBooksExpanded(activeTab.includes('accounting'));
    }, [activeTab]);

    return (
        <div className="py-6 overflow-y-auto h-full custom-scrollbar">
            <SidebarItem
                to="?tab=overview"
                label="Dashboard"
                icon={LayoutDashboard}
                active={activeTab === 'overview'}
                onClick={closeMobile}
            />

            {/* FINANCE GROUP */}
            <SidebarGroup
                icon={Wallet}
                label="Finance"
                active={activeTab.startsWith('finance')}
                expanded={financeExpanded}
                onToggle={() => setFinanceExpanded(!financeExpanded)}
            >
                <SidebarSubItem
                    label="Transactions"
                    to="?tab=finance-transactions"
                    active={activeTab === 'finance-transactions'}
                    onClick={closeMobile}
                />

                {/* NESTED ACCOUNTING BOOKS */}
                <SidebarSubGroup
                    label="Accounting Books"
                    expanded={booksExpanded}
                    onToggle={() => setBooksExpanded(!booksExpanded)}
                >
                    <SidebarDeepItem label="Overview" to="?tab=finance-accounting-overview" active={activeTab === 'finance-accounting-overview'} onClick={closeMobile} />
                    <SidebarDeepItem label="Chart of Accounts" to="?tab=finance-accounting-coa" active={activeTab === 'finance-accounting-coa'} onClick={closeMobile} />
                    <SidebarDeepItem label="Balance Sheet" to="?tab=finance-accounting-balance-sheet" active={activeTab === 'finance-accounting-balance-sheet'} onClick={closeMobile} />
                    <SidebarDeepItem label="Profit & Loss" to="?tab=finance-accounting-pl" active={activeTab === 'finance-accounting-pl'} onClick={closeMobile} />
                    <SidebarDeepItem label="General Ledger" to="?tab=finance-accounting-ledger" active={activeTab === 'finance-accounting-ledger'} onClick={closeMobile} />
                    <SidebarDeepItem label="Journal Entries" to="?tab=finance-accounting-journal" active={activeTab === 'finance-accounting-journal'} onClick={closeMobile} />
                </SidebarSubGroup>
            </SidebarGroup>

            <SidebarItem
                to="?tab=savings"
                label="Savings"
                icon={PiggyBank}
                active={activeTab === 'savings'}
                onClick={closeMobile}
            />

            <SidebarItem
                to="?tab=assets"
                label="Assets"
                icon={Briefcase}
                active={activeTab === 'assets'}
                onClick={closeMobile}
            />

            <SidebarItem
                to="?tab=loans"
                label="Loans & Credit"
                icon={CreditCard}
                active={activeTab === 'loans'}
                onClick={closeMobile}
            />

            <SidebarItem
                to="?tab=reports"
                label="Reports"
                icon={FileBarChart}
                active={activeTab === 'reports'}
                onClick={closeMobile}
            />

            <SidebarItem
                to="?tab=members"
                label="Sacco Members"
                icon={Users}
                active={activeTab === 'members'}
                onClick={closeMobile}
            />

            <div className="my-4 border-t border-slate-200 mx-4"></div>

            <SidebarItem
                to="?tab=users"
                label="System Users"
                icon={UserCog}
                active={activeTab === 'users'}
                onClick={closeMobile}
            />

            <SidebarItem
                to="?tab=register"
                label="Register New"
                icon={UserPlus}
                active={activeTab === 'register'}
                onClick={closeMobile}
            />

            <SidebarItem
                to="?tab=settings"
                label="Configuration"
                icon={Settings}
                active={activeTab === 'settings'}
                onClick={closeMobile}
            />

            <SidebarItem
                to="?tab=audit"
                label="Audit & Security"
                icon={ShieldCheck}
                active={activeTab === 'audit'}
                onClick={closeMobile}
            />

            {/* Bottom spacer */}
            <div className="h-20"></div>
        </div>
    );
};

// ==================================================================================
// MAIN LAYOUT COMPONENT
// ==================================================================================
export default function AdminDashboard() {
    const [user, setUser] = useState(null);
    const [searchParams] = useSearchParams();
    const activeTab = searchParams.get('tab') || 'overview';
    const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
    }, []);

    // --- CONTENT SWITCHER ---
    const renderContent = () => {
        switch(activeTab) {
            case 'overview': return <OverviewTab />;

            // ✅ Finance - Transactions
            case 'finance-transactions': return <TransactionsView />;

            // ✅ Finance - Accounting Books (Mapping URL to Component Prop)
            case 'finance-accounting-overview': return <AccountingReports activeView="overview" />;
            case 'finance-accounting-coa': return <AccountingReports activeView="accounts" />;
            case 'finance-accounting-balance-sheet': return <AccountingReports activeView="balance-sheet" />;
            case 'finance-accounting-pl': return <AccountingReports activeView="income-statement" />;
            case 'finance-accounting-ledger': return <AccountingReports activeView="ledger" />;
            case 'finance-accounting-journal': return <AccountingReports activeView="journal" />;

            // Fallback for parent tab click
            case 'finance': return <TransactionsView />;

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
            case 'users': return <SystemUsersTab />;
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
            default: return <OverviewTab />;
        }
    };

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800 flex flex-col">
            {/* 1. Sticky Header */}
            <div className="sticky top-0 z-50 bg-white shadow-sm border-b border-slate-200">
                <DashboardHeader user={user} title="Admin Portal" />

                {/* Mobile Menu Toggle Bar */}
                <div className="lg:hidden border-t border-slate-100 p-2 flex items-center gap-3 px-4 bg-white">
                    <button
                        onClick={() => setIsMobileMenuOpen(true)}
                        className="p-2 text-slate-600 hover:bg-slate-100 rounded-lg"
                    >
                        <Menu size={24} />
                    </button>
                    <span className="text-sm font-bold text-slate-700 capitalize">
                        {activeTab.replace(/-/g, ' ')}
                    </span>
                </div>
            </div>

            {/* 2. Main Layout Container */}
            <div className="flex flex-1 max-w-[1920px] mx-auto w-full relative">

                {/* Sidebar (Desktop) - Sticky */}
                <aside className="hidden lg:block w-64 flex-shrink-0 sticky top-[80px] h-[calc(100vh-80px)] border-r border-slate-200 bg-white/50 backdrop-blur-xl">
                    <AdminSidebar activeTab={activeTab} closeMobile={() => {}} />
                </aside>

                {/* Sidebar (Mobile) - Overlay */}
                {isMobileMenuOpen && (
                    <div className="fixed inset-0 z-50 lg:hidden">
                        {/* Backdrop */}
                        <div
                            className="absolute inset-0 bg-slate-900/50 backdrop-blur-sm transition-opacity"
                            onClick={() => setIsMobileMenuOpen(false)}
                        ></div>
                        {/* Drawer */}
                        <div className="absolute left-0 top-0 bottom-0 w-72 bg-white shadow-2xl flex flex-col animate-in slide-in-from-left duration-200">
                            <div className="p-4 flex justify-between items-center border-b border-slate-100">
                                <span className="font-bold text-lg text-slate-800">Menu</span>
                                <button onClick={() => setIsMobileMenuOpen(false)} className="p-2 text-slate-500 hover:bg-slate-100 rounded-full">
                                    <X size={20} />
                                </button>
                            </div>
                            <div className="flex-1 overflow-y-auto">
                                <AdminSidebar activeTab={activeTab} closeMobile={() => setIsMobileMenuOpen(false)} />
                            </div>
                        </div>
                    </div>
                )}

                {/* Main Content Area */}
                <main className="flex-1 min-w-0 p-4 sm:p-6 lg:p-8">
                    <div className="max-w-7xl mx-auto pb-12">
                        {renderContent()}
                    </div>
                </main>
            </div>
        </div>
    );
}

// ==================================================================================
// 1. OVERVIEW TAB
// ==================================================================================
function OverviewTab() {
    const [stats, setStats] = useState({
        totalMembers: 0,
        totalSavings: 0,
        totalLoansIssued: 0,
        netIncome: 0,
        savingsTrend: "0% vs last month",
        incomeTrend: "0% vs last month",
        pendingLoansCount: "0 Pending Approval",
        newMembersCount: "0 New this week"
    });
    const [chartData, setChartData] = useState([]);
    const [loading, setLoading] = useState(true);
    const [user, setUser] = useState({});

    // ✅ State for System Status Modal
    const [isSystemStatusOpen, setIsSystemStatusOpen] = useState(false);

    const navigate = useNavigate();

    const [dateRange, setDateRange] = useState({
        start: new Date(new Date().setDate(new Date().getDate() - 30)).toISOString().split('T')[0],
        end: new Date().toISOString().split('T')[0]
    });

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
        loadDashboard();
    }, []);

    const loadDashboard = async () => {
        try {
            const todayRes = await api.get('/api/reports/dashboard-stats');
            if (todayRes.data.success) {
                setStats(todayRes.data.data);
            }
            fetchChartData();
            setLoading(false);
        } catch (e) {
            console.error("Failed to load dashboard stats:", e);
            setLoading(false);
        }
    };

    const fetchChartData = async () => {
        try {
            const chartRes = await api.get(`/api/reports/chart?startDate=${dateRange.start}&endDate=${dateRange.end}`);
            if (chartRes.data.success) setChartData(chartRes.data.data);
        } catch (e) {
            console.error("Failed to load chart data:", e);
        }
    };

    const handleGenerateReport = async () => {
        if(!window.confirm("Generate End-of-Day Financial Report?")) return;
        try {
            await api.post('/api/reports/generate');
            alert("✅ Report Generated Successfully!");
            navigate('?tab=reports');
        } catch (e) {
            console.error("Failed to generate report:", e);
            alert("Failed to generate report.");
        }
    };

    if (loading) return (
        <div className="flex flex-col items-center justify-center h-96 text-slate-400 gap-4 animate-pulse">
            <div className="w-12 h-12 rounded-full bg-slate-200"></div>
            <p>Loading Dashboard insights...</p>
        </div>
    );

    return (
        <div className="space-y-8 animate-in fade-in duration-500">

            {/* 1. Welcome Section */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-slate-900">
                        Welcome back, {user.firstName || 'Admin'} 👋
                    </h1>
                    <p className="text-slate-500 text-sm mt-1 flex items-center gap-2">
                        <Calendar size={14} />
                        {new Date().toLocaleDateString('en-GB', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
                    </p>
                </div>
                <div className="flex gap-3">
                    <button
                        onClick={() => setIsSystemStatusOpen(true)}
                        className="bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 hover:text-slate-900 px-4 py-2 rounded-xl text-sm font-bold transition shadow-sm flex items-center gap-2"
                    >
                        <Activity size={16} className="text-emerald-500" /> System Status
                    </button>
                    <button
                        onClick={handleGenerateReport}
                        className="bg-indigo-900 hover:bg-indigo-800 text-white px-4 py-2 rounded-xl text-sm font-bold transition shadow-md shadow-indigo-200 flex items-center gap-2"
                    >
                        <FileText size={16} /> Generate Report
                    </button>
                </div>
            </div>

            {/* 2. Key Metrics Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
                <MetricCard
                    label="Total Savings"
                    value={stats.totalSavings}
                    icon={Wallet}
                    color="text-emerald-600"
                    bg="bg-emerald-50"
                    trend={stats.savingsTrend}
                />
                <MetricCard
                    label="Net Income"
                    value={stats.netIncome}
                    icon={TrendingUp}
                    color="text-indigo-600"
                    bg="bg-indigo-50"
                    trend={stats.incomeTrend}
                />
                <MetricCard
                    label="Loans Issued"
                    value={stats.totalLoansIssued}
                    icon={CreditCard}
                    color="text-purple-600"
                    bg="bg-purple-50"
                    trend={stats.pendingLoansCount}
                />
                <MetricCard
                    label="Active Members"
                    value={stats.totalMembers}
                    icon={Users}
                    color="text-blue-600"
                    bg="bg-blue-50"
                    trend={stats.newMembersCount}
                    isCount
                />
            </div>

            {/* 3. Main Content: Charts & Secondary Widgets */}
            <div className="grid grid-cols-1 xl:grid-cols-3 gap-8">

                {/* Large Chart Section */}
                <div className="xl:col-span-2 bg-white p-6 rounded-3xl shadow-sm border border-slate-100">
                    <div className="flex justify-between items-center mb-6">
                        <div>
                            <h3 className="font-bold text-slate-800 text-lg">Financial Performance</h3>
                            <p className="text-slate-400 text-xs">Income vs Expenses over time</p>
                        </div>
                        {/* Simple Date Filter */}
                        <div className="flex bg-slate-50 p-1 rounded-lg border border-slate-200">
                            <input
                                type="date"
                                value={dateRange.start}
                                onChange={(e) => setDateRange({...dateRange, start: e.target.value})}
                                className="bg-transparent border-none text-xs font-bold text-slate-600 focus:ring-0 cursor-pointer px-2"
                            />
                            <span className="text-slate-300 self-center">|</span>
                            <input
                                type="date"
                                value={dateRange.end}
                                onChange={(e) => setDateRange({...dateRange, end: e.target.value})}
                                className="bg-transparent border-none text-xs font-bold text-slate-600 focus:ring-0 cursor-pointer px-2"
                            />
                            <button onClick={fetchChartData} className="ml-2 bg-white text-slate-600 hover:text-indigo-600 p-1 rounded-md shadow-sm border border-slate-100">
                                <Filter size={14} />
                            </button>
                        </div>
                    </div>

                    <div className="h-[350px] w-full">
                        {chartData && chartData.length > 0 ? (
                            <ResponsiveContainer width="100%" height="100%">
                                <AreaChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                                    <defs>
                                        <linearGradient id="colorIncome" x1="0" y1="0" x2="0" y2="1">
                                            <stop offset="5%" stopColor="#4F46E5" stopOpacity={0.1}/>
                                            <stop offset="95%" stopColor="#4F46E5" stopOpacity={0}/>
                                        </linearGradient>
                                        <linearGradient id="colorExpense" x1="0" y1="0" x2="0" y2="1">
                                            <stop offset="5%" stopColor="#EF4444" stopOpacity={0.1}/>
                                            <stop offset="95%" stopColor="#EF4444" stopOpacity={0}/>
                                        </linearGradient>
                                    </defs>
                                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#F1F5F9" />
                                    <XAxis
                                        dataKey="name"
                                        fontSize={11}
                                        tickLine={false}
                                        axisLine={false}
                                        tick={{fill: '#94A3B8'}}
                                        dy={10}
                                    />
                                    <YAxis
                                        fontSize={11}
                                        tickLine={false}
                                        axisLine={false}
                                        tick={{fill: '#94A3B8'}}
                                        tickFormatter={(val) => `${val/1000}k`}
                                    />
                                    <Tooltip
                                        contentStyle={{borderRadius: '12px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)'}}
                                        formatter={(value) => [`KES ${Number(value).toLocaleString()}`, '']}
                                    />
                                    <Legend iconType="circle" wrapperStyle={{paddingTop: '20px'}} />
                                    <Area
                                        type="monotone"
                                        dataKey="income"
                                        stroke="#4F46E5"
                                        strokeWidth={3}
                                        fill="url(#colorIncome)"
                                        name="Income"
                                        animationDuration={1500}
                                    />
                                    <Area
                                        type="monotone"
                                        dataKey="expense"
                                        stroke="#EF4444"
                                        strokeWidth={3}
                                        fill="url(#colorExpense)"
                                        name="Expenses"
                                        animationDuration={1500}
                                    />
                                </AreaChart>
                            </ResponsiveContainer>
                        ) : (
                            <div className="h-full flex flex-col items-center justify-center bg-slate-50/50 rounded-2xl border-2 border-dashed border-slate-100 text-slate-400">
                                <FileBarChart size={48} className="mb-2 opacity-20" />
                                <span className="text-sm font-medium">No financial data for this period</span>
                            </div>
                        )}
                    </div>
                </div>

                {/* Right Column: Widgets */}
                <div className="space-y-6">
                    {/* Share Capital */}
                    <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
                       <ShareCapitalCard />
                    </div>

                    {/* Quick Access / Shortcuts */}
                    <div className="bg-gradient-to-br from-slate-900 to-slate-800 p-6 rounded-3xl shadow-lg text-white relative overflow-hidden group">
                        <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity transform group-hover:scale-110">
                            <Wallet size={120} />
                        </div>
                        <h3 className="text-lg font-bold mb-1 relative z-10">Quick Actions</h3>
                        <p className="text-slate-400 text-xs mb-6 relative z-10">Common tasks managed recently</p>

                        <div className="grid grid-cols-2 gap-3 relative z-10">
                            <Link to="?tab=register" className="bg-white/10 hover:bg-white/20 backdrop-blur-sm p-3 rounded-xl flex flex-col items-center gap-2 transition border border-white/5">
                                <UserPlus size={20} className="text-emerald-400" />
                                <span className="text-[10px] font-bold uppercase tracking-wider">New Member</span>
                            </Link>
                            <Link to="?tab=finance-transactions" className="bg-white/10 hover:bg-white/20 backdrop-blur-sm p-3 rounded-xl flex flex-col items-center gap-2 transition border border-white/5">
                                <ArrowDownLeft size={20} className="text-blue-400" />
                                <span className="text-[10px] font-bold uppercase tracking-wider">Deposit</span>
                            </Link>
                            <Link to="?tab=loans" className="bg-white/10 hover:bg-white/20 backdrop-blur-sm p-3 rounded-xl flex flex-col items-center gap-2 transition border border-white/5">
                                <FileText size={20} className="text-purple-400" />
                                <span className="text-[10px] font-bold uppercase tracking-wider">Loan Req</span>
                            </Link>
                            <Link to="?tab=audit" className="bg-white/10 hover:bg-white/20 backdrop-blur-sm p-3 rounded-xl flex flex-col items-center gap-2 transition border border-white/5">
                                <ShieldCheck size={20} className="text-amber-400" />
                                <span className="text-[10px] font-bold uppercase tracking-wider">Audit Log</span>
                            </Link>
                        </div>
                    </div>
                </div>
            </div>

            {/* ✅ MODAL INTEGRATION */}
            <SystemStatusModal isOpen={isSystemStatusOpen} onClose={() => setIsSystemStatusOpen(false)} />
        </div>
    );
}

// Helper Component for the Cards
const MetricCard = ({ label, value, icon: Icon, color, bg, trend, isCount }) => (
    <div className="bg-white p-6 rounded-3xl shadow-sm border border-slate-100 hover:shadow-md transition-all duration-300 group">
        <div className="flex justify-between items-start mb-4">
            <div className={`p-3.5 rounded-2xl ${bg} ${color} group-hover:scale-110 transition-transform duration-300`}>
                <Icon size={24} strokeWidth={2.5} />
            </div>
            {trend && (
                <span className={`flex items-center gap-1 text-[10px] font-bold px-2 py-1 rounded-full border ${
                    trend.includes('+') ? 'bg-emerald-50 text-emerald-600 border-emerald-100' :
                    trend.includes('-') ? 'bg-rose-50 text-rose-600 border-rose-100' :
                    'bg-slate-50 text-slate-500 border-slate-100'
                }`}>
                    {trend.includes('+') ? <ArrowUpRight size={10} className="text-emerald-500"/> : null}
                    {trend}
                </span>
            )}
        </div>
        <div>
            <h3 className="text-3xl font-extrabold text-slate-800 tracking-tight mb-1">
                {isCount ? value : `KES ${Number(value).toLocaleString()}`}
            </h3>
            <p className="text-slate-400 text-xs font-bold uppercase tracking-wide">{label}</p>
        </div>
    </div>
);

// ==================================================================================
// 2. TRANSACTIONS VIEW (Modern Redesign)
// ==================================================================================
function TransactionsView() {
    const [transactions, setTransactions] = useState([]);
    const [currentPage, setCurrentPage] = useState(1);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [loading, setLoading] = useState(true);
    const itemsPerPage = 10;

    const fetchTransactions = async () => {
        setLoading(true);
        try {
            const res = await api.get('/api/transactions');
            if(res.data.success) {
                // Sort by date descending (newest first)
                const sorted = res.data.data.sort((a, b) => new Date(b.transactionDate) - new Date(a.transactionDate));
                setTransactions(sorted);
            }
        } catch (error) {
            console.error("Failed to load transactions", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchTransactions(); }, []);

    const handleRunInterest = async () => {
        if (window.confirm("Are you sure you want to run Monthly Interest? This will calculate interest for all accounts based on their specific Product Rates.")) {
            try {
                await api.post('/api/transactions/interest');
                alert("✅ Interest calculation job started successfully!");
                fetchTransactions();
            } catch (error) {
                alert("❌ Failed to apply interest: " + (error.response?.data?.message || error.message));
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
        window.location.href = 'http://localhost:8082/api/transactions/download';
    };

    // Helper for badges
    const getTypeBadge = (type) => {
        const styles = {
            DEPOSIT: 'bg-emerald-100 text-emerald-700 border-emerald-200',
            WITHDRAWAL: 'bg-amber-100 text-amber-700 border-amber-200',
            LOAN_DISBURSEMENT: 'bg-blue-100 text-blue-700 border-blue-200',
            LOAN_REPAYMENT: 'bg-indigo-100 text-indigo-700 border-indigo-200',
            INTEREST_EARNED: 'bg-purple-100 text-purple-700 border-purple-200',
            REVERSAL: 'bg-slate-100 text-slate-600 border-slate-200 decoration-line-through',
            TRANSFER: 'bg-cyan-100 text-cyan-700 border-cyan-200'
        };
        const defaultStyle = 'bg-slate-50 text-slate-600 border-slate-200';
        return styles[type] || defaultStyle;
    };

    const lastIndex = currentPage * itemsPerPage;
    const firstIndex = lastIndex - itemsPerPage;
    const currentTx = transactions.slice(firstIndex, lastIndex);
    const totalPages = Math.ceil(transactions.length / itemsPerPage);

    return (
        <div className="space-y-6 animate-in fade-in slide-in-from-bottom-2 duration-300">

            {/* Header & Actions Card */}
            <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100 flex flex-col md:flex-row justify-between items-center gap-4">
                <div>
                    <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                        <div className="p-2 bg-indigo-50 text-indigo-600 rounded-lg">
                            <Wallet size={20} />
                        </div>
                        Financial Ledger
                    </h2>
                    <p className="text-slate-500 text-xs mt-1 ml-11">Complete history of all deposits, withdrawals, and system fees.</p>
                </div>

                <div className="flex gap-2">
                    <button onClick={handleRunInterest} className="flex items-center gap-2 bg-indigo-600 text-white px-4 py-2 rounded-xl text-xs font-bold hover:bg-indigo-700 transition shadow-lg shadow-indigo-200">
                        <TrendingUp size={16} /> Run Interest
                    </button>
                    <button onClick={() => setIsModalOpen(true)} className="flex items-center gap-2 bg-emerald-600 text-white px-4 py-2 rounded-xl text-xs font-bold hover:bg-emerald-700 transition shadow-lg shadow-emerald-200">
                        <ArrowDownLeft size={16} /> New Transaction
                    </button>
                    <button onClick={handleDownload} className="flex items-center gap-2 bg-white border border-slate-200 text-slate-700 px-4 py-2 rounded-xl text-xs font-bold hover:bg-slate-50 transition shadow-sm">
                        <Download size={16} /> Export CSV
                    </button>
                </div>
            </div>

            {/* Transactions Table */}
            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead className="bg-slate-50 text-slate-500 font-bold border-b border-slate-100 uppercase text-xs tracking-wider">
                            <tr>
                                <th className="p-4 pl-6">Reference / Date</th>
                                <th className="p-4">Member</th>
                                <th className="p-4">Transaction Type</th>
                                <th className="p-4 text-right">Amount</th>
                                <th className="p-4 text-center">Action</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-50">
                            {loading ? (
                                <tr>
                                    <td colSpan="5" className="p-12 text-center text-slate-400">
                                        <div className="flex flex-col items-center gap-2">
                                            <div className="w-6 h-6 border-2 border-indigo-200 border-t-indigo-600 rounded-full animate-spin"></div>
                                            <span>Loading transactions...</span>
                                        </div>
                                    </td>
                                </tr>
                            ) : currentTx.length > 0 ? (
                                currentTx.map((tx) => (
                                    <tr key={tx.id} className="hover:bg-slate-50 transition duration-150 group">
                                        <td className="p-4 pl-6">
                                            <div className="font-bold text-slate-700">{tx.transactionId}</div>
                                            <div className="text-xs text-slate-400 font-mono mt-0.5 flex items-center gap-1">
                                                <Calendar size={10} />
                                                {new Date(tx.transactionDate).toLocaleString()}
                                            </div>
                                        </td>
                                        <td className="p-4">
                                            {tx.member ? (
                                                <div>
                                                    <div className="font-bold text-slate-800">{tx.member.firstName} {tx.member.lastName}</div>
                                                    <div className="text-xs text-slate-400">{tx.member.memberNumber}</div>
                                                </div>
                                            ) : (
                                                <span className="text-slate-400 italic">System / Anonymous</span>
                                            )}
                                        </td>
                                        <td className="p-4">
                                            <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-bold border uppercase tracking-wide ${getTypeBadge(tx.type)}`}>
                                                {tx.type.replace(/_/g, ' ')}
                                            </span>
                                        </td>
                                        <td className="p-4 text-right">
                                            <div className={`font-bold font-mono ${
                                                ['DEPOSIT', 'LOAN_REPAYMENT', 'INTEREST_EARNED', 'SHARE_CAPITAL'].includes(tx.type)
                                                ? 'text-emerald-600'
                                                : 'text-slate-800'
                                            }`}>
                                                KES {Number(tx.amount).toLocaleString(undefined, {minimumFractionDigits: 2})}
                                            </div>
                                        </td>
                                        <td className="p-4 text-center">
                                            {tx.type !== 'REVERSAL' && (
                                                <button
                                                    onClick={() => handleReverse(tx.transactionId)}
                                                    className="opacity-0 group-hover:opacity-100 transition-opacity text-xs bg-rose-50 text-rose-600 hover:bg-rose-100 hover:text-rose-700 font-bold px-3 py-1.5 rounded-lg border border-rose-100"
                                                    title="Reverse this transaction"
                                                >
                                                    Reverse
                                                </button>
                                            )}
                                        </td>
                                    </tr>
                                ))
                            ) : (
                                <tr>
                                    <td colSpan="5" className="p-12 text-center text-slate-400 italic">
                                        No transactions found. Start by adding a new one.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>

                {/* Pagination */}
                {totalPages > 1 && (
                    <div className="p-4 border-t border-slate-100 flex justify-between items-center bg-slate-50">
                        <button
                            onClick={() => setCurrentPage(p => Math.max(1, p-1))}
                            disabled={currentPage===1}
                            className="p-2 rounded-lg bg-white border border-slate-200 hover:bg-slate-100 text-slate-600 disabled:opacity-50 transition shadow-sm"
                        >
                            <ChevronLeft size={16}/>
                        </button>
                        <span className="text-xs font-bold text-slate-500">
                            Page <span className="text-slate-900">{currentPage}</span> of {totalPages}
                        </span>
                        <button
                            onClick={() => setCurrentPage(p => Math.min(totalPages, p+1))}
                            disabled={currentPage===totalPages}
                            className="p-2 rounded-lg bg-white border border-slate-200 hover:bg-slate-100 text-slate-600 disabled:opacity-50 transition shadow-sm"
                        >
                            <ChevronRight size={16}/>
                        </button>
                    </div>
                )}
            </div>

            <TransactionModal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} onSuccess={fetchTransactions} />
        </div>
    );
}

// ==================================================================================
// 3. MEMBERS TAB (Real Data)
// ==================================================================================
function MembersTab() {
    const [members, setMembers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        api.get('/api/members')
            .then(res => {
                if(res.data.success) {
                    setMembers(res.data.data);
                }
                setLoading(false);
            })
            .catch(err => {
                setError(err.response?.data?.message || 'Failed to load members');
                setLoading(false);
            });
    }, []);

    if (loading) {
        return (
            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-10 text-center">
                <div className="text-slate-400">Loading members...</div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-10 text-center">
                <div className="text-red-600">Error: {error}</div>
            </div>
        );
    }

    if (members.length === 0) {
        return (
            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-10 text-center">
                <div className="text-slate-400">No members found. Register your first member to get started.</div>
            </div>
        );
    }

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
                            <th className="p-4">Email</th>
                            <th className="p-4">Contact</th>
                            <th className="p-4">Status</th>
                            <th className="p-4 text-right">Savings</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                        {members.map((m) => (
                            <tr key={m.id} className="hover:bg-slate-50 transition">
                                <td className="p-4 font-mono text-slate-500 text-xs">{m.memberNumber}</td>
                                <td className="p-4 font-bold text-slate-800">{m.firstName} {m.lastName}</td>
                                <td className="p-4 text-slate-500 text-xs">{m.email}</td>
                                <td className="p-4 text-slate-500 text-xs">{m.phoneNumber}</td>
                                <td className="p-4"><span className="px-2.5 py-0.5 bg-green-50 text-green-700 border border-green-200 rounded-full text-[10px] font-bold uppercase">{m.memberStatus}</span></td>
                                <td className="p-4 text-right font-bold text-slate-800">KES {Number(m.totalSavings || 0).toLocaleString()}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}

// ==================================================================================
// 4. SYSTEM USERS TAB (Manage Staff & Passwords)
// ==================================================================================
function SystemUsersTab() {
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchUsers();
    }, []);

    const fetchUsers = async () => {
        try {
            const res = await api.get('/api/users');
            if(res.data.success) setUsers(res.data.data);
        } catch (e) {
            console.error("Failed to load users", e);
        } finally {
            setLoading(false);
        }
    };

    const handleVerify = async (userId) => {
        if(!window.confirm("Force verify this user's email?")) return;
        try {
            await api.post(`/api/users/${userId}/verify`);
            alert("User verified successfully!");
            fetchUsers();
        } catch (e) {
            alert(e.response?.data?.message || "Verification Failed");
        }
    };

    const handleResetPassword = async (userId) => {
        const newPass = prompt("Enter new password for this user:");
        if (!newPass || newPass.length < 6) {
            if(newPass) alert("Password must be at least 6 characters.");
            return;
        }

        try {
            await api.post(`/api/users/${userId}/reset-password`, { password: newPass });
            alert(`Password reset successfully to: ${newPass}`);
        } catch (e) {
            alert(e.response?.data?.message || "Reset Failed");
        }
    };

    if (loading) return <div className="p-10 text-center text-slate-400">Loading Users...</div>;

    return (
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden animate-in fade-in">
            <div className="p-5 border-b border-slate-100 flex justify-between items-center bg-indigo-50/50">
                <div>
                    <h2 className="text-lg font-bold text-indigo-900">System Users & Staff</h2>
                    <p className="text-slate-500 text-xs">Manage access for Admins, Officers, and Tellers.</p>
                </div>
                <button onClick={fetchUsers} className="text-xs font-bold text-indigo-600 hover:underline">Refresh List</button>
            </div>

            <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                    <thead className="bg-slate-50 text-slate-500 uppercase font-bold text-xs tracking-wider border-b border-slate-100">
                        <tr>
                            <th className="p-4">Name</th>
                            <th className="p-4">Role</th>
                            <th className="p-4">Email</th>
                            <th className="p-4">Status</th>
                            <th className="p-4 text-center">Actions</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                        {users.map((u) => (
                            <tr key={u.id} className="hover:bg-slate-50 transition">
                                <td className="p-4 font-bold text-slate-800">
                                    {u.firstName} {u.lastName}
                                </td>
                                <td className="p-4">
                                    <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase border ${
                                        u.role === 'ADMIN' ? 'bg-purple-50 text-purple-700 border-purple-200' :
                                        u.role === 'MEMBER' ? 'bg-blue-50 text-blue-700 border-blue-200' :
                                        'bg-amber-50 text-amber-700 border-amber-200'
                                    }`}>
                                        {u.role.replace(/_/g, ' ')}
                                    </span>
                                </td>
                                <td className="p-4 text-slate-500 text-xs font-mono">{u.email}</td>
                                <td className="p-4">
                                    {u.emailVerified ? (
                                        <span className="text-emerald-600 text-xs font-bold flex items-center gap-1"><ShieldCheck size={12}/> Verified</span>
                                    ) : (
                                        <span className="text-rose-500 text-xs font-bold flex items-center gap-1"><AlertCircle size={12}/> Unverified</span>
                                    )}
                                </td>
                                <td className="p-4 text-center flex justify-center gap-2">
                                    {!u.emailVerified && (
                                        <button
                                            onClick={() => handleVerify(u.id)}
                                            className="p-1.5 bg-emerald-100 text-emerald-700 rounded-lg hover:bg-emerald-200 transition"
                                            title="Force Verify Email"
                                        >
                                            <ShieldCheck size={16}/>
                                        </button>
                                    )}
                                    <button
                                        onClick={() => handleResetPassword(u.id)}
                                        className="p-1.5 bg-amber-100 text-amber-700 rounded-lg hover:bg-amber-200 transition"
                                        title="Reset Password"
                                    >
                                        <Key size={16}/>
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}