import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import api from '../api';

// Icons
import {
    LayoutDashboard, Users, Wallet, Settings, LogOut,
    TrendingUp, CreditCard, UserPlus, FileText,
    Download, ChevronLeft, ChevronRight, ArrowDownLeft,
    Activity, AlertCircle, PiggyBank, FileBarChart, ShieldCheck,
    Briefcase, Filter, UserCog, Key
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
    const [searchParams] = useSearchParams();
    const activeTab = searchParams.get('tab') || 'overview';

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
    }, []);

    const TabButton = ({ id, label, icon: Icon }) => (
        <Link
            to={`?tab=${id}`}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl font-bold text-xs sm:text-sm transition-all duration-200 border whitespace-nowrap ${
                activeTab === id
                ? 'bg-indigo-900 text-white shadow-md border-indigo-900'
                : 'bg-white text-slate-600 hover:bg-slate-50 border-slate-200'
            }`}
        >
            <Icon size={16} className={activeTab === id ? "text-indigo-200" : "text-slate-400"}/>
            {label}
        </Link>
    );

    // --- CONTENT SWITCHER ---
    const renderContent = () => {
        switch(activeTab) {
            case 'overview': return <OverviewTab />;
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
                        <TabButton id="members" label="Sacco Members" icon={Users} />
                        <div className="w-px bg-slate-300 mx-1 h-6 self-center"></div>
                        <TabButton id="users" label="System Users" icon={UserCog} />
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
function OverviewTab() {
    const [stats, setStats] = useState({ totalMembers: 0, totalSavings: 0, totalLoansIssued: 0, netIncome: 0 });
    const [chartData, setChartData] = useState([]);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();
    const [dateRange, setDateRange] = useState({
        start: new Date(new Date().setDate(new Date().getDate() - 7)).toISOString().split('T')[0],
        end: new Date().toISOString().split('T')[0]
    });

    useEffect(() => { loadDashboard(); }, []);

    const loadDashboard = async () => {
        try {
            // ✅ Correct: Includes /api because api.js base is just the host
            const todayRes = await api.get('/api/reports/dashboard-stats');
            if (todayRes.data.success) setStats(todayRes.data.data);
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
            // ✅ FIX: Added /api prefix
            await api.post('/api/reports/generate');
            alert("✅ Report Generated!");
            navigate('?tab=reports');
        } catch (e) {
            console.error("Failed to generate report:", e);
        }
    };

    const handleSystemDiag = () => { alert("System Status: Operational\nDatabase: Connected\nEmail Service: Active"); };

    const StatCard = ({ label, value, icon: Icon, color, subtext }) => (
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-200 hover:shadow-md transition-all group">
            <div className="flex justify-between items-start mb-4">
                <div className={`p-3 rounded-xl ${color} text-white shadow-sm group-hover:scale-110 transition-transform`}>
                    <Icon size={22} />
                </div>
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
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
                <StatCard label="Total Savings" value={stats.totalSavings} icon={Wallet} color="bg-emerald-600" subtext="Member Deposits" />
                <StatCard label="Net Income" value={stats.netIncome} icon={TrendingUp} color="bg-indigo-600" subtext="Fees + Interest - Expenses" />
                <StatCard label="Active Members" value={stats.totalMembers} icon={Users} color="bg-blue-600" subtext="Registered & Verified" />
                <StatCard label="Loans Issued" value={stats.totalLoansIssued} icon={CreditCard} color="bg-purple-600" subtext="Total Disbursed" />
                <ShareCapitalCard />
            </div>

             <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 lg:col-span-2">
                    <div className="flex flex-col xl:flex-row justify-between items-center mb-6 gap-4">
                        <h3 className="font-bold text-slate-800 flex items-center gap-2 whitespace-nowrap">
                            <Activity size={20} className="text-emerald-600"/> Performance
                        </h3>
                        <div className="flex items-center gap-2 bg-slate-50 p-1.5 rounded-lg border border-slate-200">
                            <input type="date" value={dateRange.start} onChange={(e) => setDateRange({...dateRange, start: e.target.value})} className="bg-transparent text-xs font-bold text-slate-600 outline-none w-24 cursor-pointer" />
                            <span className="text-slate-300">|</span>
                            <input type="date" value={dateRange.end} onChange={(e) => setDateRange({...dateRange, end: e.target.value})} className="bg-transparent text-xs font-bold text-slate-600 outline-none w-24 cursor-pointer" />
                            <button onClick={fetchChartData} className="bg-slate-900 text-white p-1.5 rounded-md hover:bg-slate-800 transition"><Filter size={14} /></button>
                        </div>
                    </div>

                    {/* ✅ Chart: Income vs Expenses - Following ReportsDashboard pattern */}
                    <div className="h-80 w-full">
                        {chartData && chartData.length > 0 ? (
                            <ResponsiveContainer width="100%" height="100%">
                                <AreaChart data={chartData} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
                                    <defs>
                                        <linearGradient id="colorIncome" x1="0" y1="0" x2="0" y2="1">
                                            <stop offset="5%" stopColor="#10B981" stopOpacity={0.1}/>
                                            <stop offset="95%" stopColor="#10B981" stopOpacity={0}/>
                                        </linearGradient>
                                        <linearGradient id="colorExpense" x1="0" y1="0" x2="0" y2="1">
                                            <stop offset="5%" stopColor="#EF4444" stopOpacity={0.1}/>
                                            <stop offset="95%" stopColor="#EF4444" stopOpacity={0}/>
                                        </linearGradient>
                                    </defs>
                                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#F1F5F9" />
                                    <XAxis dataKey="name" fontSize={12} tickLine={false} axisLine={false} />
                                    <YAxis fontSize={12} tickLine={false} axisLine={false} tickFormatter={(val) => `${val/1000}k`} />
                                    <Tooltip formatter={(value) => `KES ${Number(value).toLocaleString()}`} />
                                    <Legend iconType="circle" />
                                    <Area type="monotone" dataKey="income" stroke="#10B981" fill="url(#colorIncome)" strokeWidth={2} name="Income" />
                                    <Area type="monotone" dataKey="expense" stroke="#EF4444" fill="url(#colorExpense)" strokeWidth={2} name="Expenses" />
                                </AreaChart>
                            </ResponsiveContainer>
                        ) : (
                            <div className="h-full flex items-center justify-center bg-slate-50 rounded-xl border-2 border-dashed border-slate-200 text-slate-400 text-sm">
                                {loading ? 'Loading chart data...' : 'No data available for selected date range'}
                            </div>
                        )}
                    </div>
                </div>
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                    <h3 className="font-bold text-slate-800 mb-4">Quick Actions</h3>
                    <div className="space-y-3">
                        <button onClick={handleGenerateReport} className="w-full bg-slate-50 hover:bg-emerald-50 hover:text-emerald-700 p-3 rounded-xl flex items-center gap-3 transition text-sm font-bold border border-slate-100 hover:border-emerald-200 group text-left">
                            <div className="bg-white p-1.5 rounded-lg shadow-sm group-hover:shadow text-emerald-600"><FileText size={16}/></div>
                            <div><span className="block">Generate Report</span></div>
                        </button>
                        <button onClick={handleSystemDiag} className="w-full bg-slate-50 hover:bg-amber-50 hover:text-amber-700 p-3 rounded-xl flex items-center gap-3 transition text-sm font-bold border border-slate-100 hover:border-amber-200 group text-left">
                            <div className="bg-white p-1.5 rounded-lg shadow-sm group-hover:shadow text-amber-600"><AlertCircle size={16}/></div>
                            <div><span className="block">System Status</span></div>
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
        window.location.href = 'http://localhost:8082/api/transactions/download';
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
// 4. ✅ NEW: SYSTEM USERS TAB (Manage Staff & Passwords)
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