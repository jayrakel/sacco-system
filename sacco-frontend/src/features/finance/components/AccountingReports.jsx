import React, { useState, useEffect, useRef } from 'react';
import api from '../../../api';
import { 
    BookOpen, Loader2, Plus, X, Calendar, Filter, RefreshCw, Trash2, Save,
    LayoutDashboard, TrendingUp, DollarSign, Users, AlertCircle, FileText, PieChart as PieIcon,
    Printer, CheckCircle, AlertTriangle, List, ToggleLeft, ToggleRight, ShieldCheck, Download,
    ArrowRight, ChevronDown
} from 'lucide-react';
import {
    BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts';
import { useSettings } from '../../../context/SettingsContext';

// ✅ Accepts activeView prop from AdminDashboard router
export default function AccountingReports({ activeView = 'overview' }) {
    const [accounts, setAccounts] = useState([]);
    const [journal, setJournal] = useState([]);
    const [activityReport, setActivityReport] = useState([]);
    const [loading, setLoading] = useState(true);

    // Global Settings
    const { settings, getImageUrl } = useSettings();
    const logoUrl = getImageUrl(settings.SACCO_LOGO);
    const orgName = settings.SACCO_NAME || "Sacco System";
    const orgAddress = settings.SACCO_ADDRESS || "Nairobi, Kenya";
    const orgContact = [settings.SACCO_EMAIL, settings.SACCO_PHONE].filter(Boolean).join(' | ');

    // Filters & State
    const [dateRange, setDateRange] = useState({ startDate: '', endDate: '' });
    const [showAddModal, setShowAddModal] = useState(false);
    const [showEntryModal, setShowEntryModal] = useState(false);
    const [printingReport, setPrintingReport] = useState(null);
    const [isDownloading, setIsDownloading] = useState(false);
    const printRef = useRef(null);

    // Forms
    const [newAccount, setNewAccount] = useState({ code: '', name: '', type: 'ASSET' });
    const [entryForm, setEntryForm] = useState({
        description: '',
        reference: '',
        date: new Date().toISOString().split('T')[0],
        lines: [{ accountCode: '', debit: 0, credit: 0 }, { accountCode: '', debit: 0, credit: 0 }]
    });

    const COLORS = ['#10B981', '#3B82F6', '#F59E0B', '#EF4444', '#8B5CF6'];

    // ✅ FIX 1: Reset filters immediately when switching views
    useEffect(() => {
        setDateRange({ startDate: '', endDate: '' });
    }, [activeView]);

    // ✅ FIX 2: Fetch data when view OR dates change
    useEffect(() => {
        fetchData();
    }, [dateRange.endDate, dateRange.startDate, activeView]);

    // Print Trigger
    useEffect(() => {
        if (printingReport && printRef.current) {
            const timer = setTimeout(() => { window.print(); }, 800);
            return () => clearTimeout(timer);
        }
    }, [printingReport]);

    const fetchData = async () => {
        setLoading(true);
        try {
            let accEndpoint = '/api/accounting/accounts';
            let activityEndpoint = null;
            let journalEndpoint = '/api/accounting/journal';

            // Construct Query Params - only add if truly present and not empty
            const queryParams = [];
            if (dateRange.startDate && dateRange.startDate.trim()) {
                queryParams.push(`startDate=${dateRange.startDate}`);
            }
            if (dateRange.endDate && dateRange.endDate.trim()) {
                queryParams.push(`endDate=${dateRange.endDate}`);
            }
            const queryString = queryParams.length > 0 ? `?${queryParams.join('&')}` : '';

            // 1. Determine Accounts/Report Endpoint
            // If filters are active, we switch to the report endpoint to get historical balances
            if (activeView !== 'accounts' && dateRange.endDate && dateRange.endDate.trim()) {
                accEndpoint = `/api/accounting/report${queryString}`;
            }

            // 2. Determine Activity Endpoint (Ledger)
            if (dateRange.startDate && dateRange.startDate.trim()) {
                activityEndpoint = `/api/accounting/report/activity${queryString}`;
            }

            // 3. Determine Journal Endpoint (Apply filters if they exist)
            if (queryString) {
                journalEndpoint += queryString;
            }

            const promises = [api.get(accEndpoint), api.get(journalEndpoint)];
            if (activityEndpoint) promises.push(api.get(activityEndpoint));

            const [accRes, jourRes, actRes] = await Promise.all(promises);

            if(accRes.data.success) setAccounts(accRes.data.data);
            if(jourRes.data.success) setJournal(jourRes.data.data);

            // Only set activity report if the endpoint was actually called
            if (activityEndpoint && actRes?.data?.success) {
                setActivityReport(actRes.data.data.activity || []);
            } else {
                setActivityReport([]);
            }

        } catch (error) {
            console.error("Error fetching data", error);
        } finally {
            setLoading(false);
        }
    };

    // ✅ Explicit Clear Handler
    const handleClearFilters = () => {
        setDateRange({ startDate: '', endDate: '' });
    };

    // --- LOGIC HELPERS ---
    const getTotal = (type) => accounts.filter(a => a.type === type && a.active).reduce((sum, a) => sum + parseFloat(a.balance || 0), 0);
    const getNetIncome = () => getTotal('INCOME') - getTotal('EXPENSE');
    const formatMoney = (amount) => Number(amount).toLocaleString(undefined, {minimumFractionDigits: 2});

    const totalAssets = getTotal('ASSET');
    const totalLiabilities = getTotal('LIABILITY');
    const totalEquity = getTotal('EQUITY') + getNetIncome();
    const balanceDifference = totalAssets - (totalLiabilities + totalEquity);
    const isBalanced = Math.abs(balanceDifference) < 1.0;

    // --- ACTIONS ---
    const handleCreateAccount = async (e) => {
        e.preventDefault();
        try {
            await api.post('/api/accounting/accounts', newAccount);
            alert("Account Created Successfully!");
            setShowAddModal(false);
            setNewAccount({ code: '', name: '', type: 'ASSET' });
            fetchData();
        } catch (err) { alert("Failed to create account: " + (err.response?.data?.message || err.message)); }
    };

    const toggleAccount = async (code) => {
        try {
            await api.put(`/api/accounting/accounts/${code}/toggle`);
            setAccounts(accounts.map(a => a.code === code ? {...a, active: !a.active} : a));
        } catch (error) { alert("Failed to update status"); fetchData(); }
    };

    const submitJournalEntry = async (e) => {
        e.preventDefault();
        const totalDebit = entryForm.lines.reduce((s, l) => s + Number(l.debit), 0);
        const totalCredit = entryForm.lines.reduce((s, l) => s + Number(l.credit), 0);
        if (Math.abs(totalDebit - totalCredit) > 0.01) return alert(`Unbalanced: Debit ${totalDebit} != Credit ${totalCredit}`);
        try {
            await api.post('/api/accounting/journal', entryForm);
            alert("Posted Successfully!");
            setShowEntryModal(false);
            setEntryForm({ description: '', reference: '', date: new Date().toISOString().split('T')[0], lines: [{ accountCode: '', debit: 0, credit: 0 }, { accountCode: '', debit: 0, credit: 0 }] });
            fetchData();
        } catch (error) { alert("Failed to post entry."); }
    };

    // --- CHART DATA ---
    const plData = [{ name: 'Revenue', amount: getTotal('INCOME') }, { name: 'Expenses', amount: getTotal('EXPENSE') }, { name: 'Net Profit', amount: getNetIncome() }];
    const assetData = accounts.filter(a => a.type === 'ASSET' && a.active && parseFloat(a.balance) > 0).map(a => ({ name: a.name, value: parseFloat(a.balance) })).sort((a, b) => b.value - a.value).slice(0, 5);

    // --- FORM HANDLERS ---
    const handleEntryChange = (index, field, value) => {
        const newLines = [...entryForm.lines];
        newLines[index][field] = value;
        if (field === 'debit' && value > 0) newLines[index].credit = 0;
        if (field === 'credit' && value > 0) newLines[index].debit = 0;
        setEntryForm({ ...entryForm, lines: newLines });
    };
    const addEntryLine = () => setEntryForm({ ...entryForm, lines: [...entryForm.lines, { accountCode: '', debit: 0, credit: 0 }] });
    const removeEntryLine = (index) => { if (entryForm.lines.length > 2) setEntryForm({ ...entryForm, lines: entryForm.lines.filter((_, i) => i !== index) }); };

    const getPageTitle = () => {
        switch(activeView) {
            case 'overview': return 'Accounting Overview';
            case 'accounts': return 'Chart of Accounts';
            case 'balance-sheet': return 'Balance Sheet';
            case 'income-statement': return 'Profit & Loss Statement';
            case 'ledger': return 'General Ledger';
            case 'journal': return 'Journal Entries';
            default: return 'Accounting';
        }
    };

    return (
        <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500 relative">

            {/* Loading Overlay */}
            {loading && (
                <div className="absolute inset-0 bg-white/60 backdrop-blur-[1px] z-50 flex items-center justify-center rounded-2xl">
                    <Loader2 className="animate-spin text-indigo-600" size={32} />
                </div>
            )}

            {/* --- 1. HEADER & CONTROLS --- */}
            <div className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100 flex flex-col xl:flex-row justify-between items-center gap-4 print:hidden">
                <div className="flex items-center gap-4 w-full xl:w-auto">
                    <div className="p-2.5 bg-indigo-50 text-indigo-600 rounded-xl">
                        {activeView === 'overview' ? <LayoutDashboard size={24} /> :
                         activeView === 'accounts' ? <List size={24} /> :
                         activeView === 'balance-sheet' ? <FileText size={24} /> :
                         activeView === 'income-statement' ? <TrendingUp size={24} /> :
                         <BookOpen size={24} />}
                    </div>
                    <div>
                        <h2 className="font-bold text-slate-800 text-lg">{getPageTitle()}</h2>
                        <p className="text-xs text-slate-500">Financial Reporting & Analysis</p>
                    </div>
                </div>

                <div className="flex flex-wrap items-center gap-3 w-full xl:w-auto justify-end">

                    {/* Date Filter (Only shows for relevant views) */}
                    {activeView !== 'accounts' && (
                        <div className="flex items-center bg-white border border-slate-200 rounded-xl p-1 shadow-sm gap-2">
                            <div className="flex items-center">
                                <input
                                    type="date"
                                    className="bg-transparent text-xs font-bold text-slate-600 outline-none px-2 py-1"
                                    value={dateRange.startDate}
                                    onChange={(e) => setDateRange({...dateRange, startDate: e.target.value})}
                                />
                                <span className="text-slate-300 text-[10px] font-bold px-1">TO</span>
                                <input
                                    type="date"
                                    className="bg-transparent text-xs font-bold text-slate-600 outline-none px-2 py-1"
                                    value={dateRange.endDate}
                                    onChange={(e) => setDateRange({...dateRange, endDate: e.target.value})}
                                />
                            </div>

                            {/* ✅ Clear Filter Button */}
                            {(dateRange.startDate || dateRange.endDate) && (
                                <button
                                    onClick={handleClearFilters}
                                    className="p-1 bg-rose-50 text-rose-500 hover:bg-rose-100 hover:text-rose-600 rounded-lg transition"
                                    title="Clear Dates & Refresh"
                                >
                                    <X size={14} />
                                </button>
                            )}
                        </div>
                    )}

                    {/* Actions */}
                    <div className="flex gap-2">
                        {activeView === 'accounts' && (
                            <button onClick={() => setShowAddModal(true)} className="bg-emerald-600 hover:bg-emerald-700 text-white p-2 rounded-xl shadow-sm transition flex items-center gap-2 px-4 text-xs font-bold">
                                <Plus size={16} /> New Account
                            </button>
                        )}
                        <button onClick={() => setShowEntryModal(true)} className="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-xl text-xs font-bold flex items-center gap-2 shadow-sm shadow-indigo-200 transition">
                            <Plus size={16} /> Post Entry
                        </button>
                    </div>
                </div>
            </div>

            {/* --- VIEW 1: OVERVIEW DASHBOARD --- */}
            {activeView === 'overview' && (
                <div className="space-y-6 print:hidden">
                    {/* Summary Cards */}
                    <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
                        <SummaryCard label="Total Assets" value={getTotal('ASSET')} icon={DollarSign} color="bg-blue-50 text-blue-600" border="border-blue-100" />
                        <SummaryCard label="Total Liabilities" value={getTotal('LIABILITY')} icon={AlertCircle} color="bg-orange-50 text-orange-600" border="border-orange-100" />
                        <SummaryCard label="Total Equity" value={getTotal('EQUITY')} icon={Users} color="bg-purple-50 text-purple-600" border="border-purple-100" />
                        <SummaryCard label="Net Income" value={getNetIncome()} icon={TrendingUp} color="bg-emerald-50 text-emerald-600" border="border-emerald-100" isNet />
                    </div>

                    {/* Charts Row */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                            <h3 className="font-bold text-slate-800 mb-6 flex items-center gap-2"><TrendingUp size={18} className="text-indigo-600"/> Profit & Loss</h3>
                            <div className="h-64 w-full">
                                <ResponsiveContainer width="100%" height="100%">
                                    <BarChart data={plData}>
                                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#F1F5F9" />
                                        <XAxis dataKey="name" fontSize={12} tickLine={false} axisLine={false} />
                                        <YAxis fontSize={12} tickLine={false} axisLine={false} tickFormatter={(val) => `${val/1000}k`} />
                                        <Tooltip cursor={{fill: 'transparent'}} contentStyle={{borderRadius: '12px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)'}} formatter={(value) => formatMoney(value)} />
                                        <Bar dataKey="amount" radius={[6, 6, 0, 0]} barSize={40}>
                                            <Cell fill="#10B981" />
                                            <Cell fill="#EF4444" />
                                            <Cell fill="#6366f1" />
                                        </Bar>
                                    </BarChart>
                                </ResponsiveContainer>
                            </div>
                        </div>
                        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                            <h3 className="font-bold text-slate-800 mb-6 flex items-center gap-2"><PieIcon size={18} className="text-blue-600"/> Asset Composition</h3>
                            <div className="h-64 w-full flex items-center justify-center">
                                {assetData.length > 0 ? (
                                    <ResponsiveContainer width="100%" height="100%">
                                        <PieChart>
                                            <Pie data={assetData} cx="50%" cy="50%" innerRadius={60} outerRadius={80} paddingAngle={5} dataKey="value">
                                                {assetData.map((entry, index) => (<Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />))}
                                            </Pie>
                                            <Tooltip contentStyle={{borderRadius: '12px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)'}} formatter={(value) => formatMoney(value)} />
                                            <Legend verticalAlign="bottom" height={36} iconType="circle" />
                                        </PieChart>
                                    </ResponsiveContainer>
                                ) : (
                                    <p className="text-slate-400 text-sm italic">No assets recorded.</p>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* --- VIEW 2: CHART OF ACCOUNTS --- */}
            {activeView === 'accounts' && (
                <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden print:hidden">
                    <div className="overflow-x-auto">
                        <table className="w-full text-sm text-left">
                            <thead className="bg-slate-50 text-slate-500 font-bold border-b border-slate-100 uppercase text-xs tracking-wider">
                                <tr>
                                    <th className="p-4 pl-6">Code</th>
                                    <th className="p-4">Name</th>
                                    <th className="p-4">Type</th>
                                    <th className="p-4 text-right">Balance</th>
                                    <th className="p-4 text-center">Status</th>
                                    <th className="p-4 text-center">Action</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100">
                                {accounts.map((acc) => (
                                    <tr key={acc.code} className="hover:bg-slate-50 transition duration-150">
                                        <td className="p-4 pl-6 font-mono text-slate-500 text-xs font-bold">{acc.code}</td>
                                        <td className="p-4 font-bold text-slate-700">{acc.name}</td>
                                        <td className="p-4">
                                            <span className={`text-[10px] font-bold px-2.5 py-1 rounded-full border ${
                                                acc.type === 'ASSET' ? 'bg-blue-50 text-blue-700 border-blue-100' :
                                                acc.type === 'LIABILITY' ? 'bg-amber-50 text-amber-700 border-amber-100' :
                                                acc.type === 'EQUITY' ? 'bg-purple-50 text-purple-700 border-purple-100' :
                                                acc.type === 'INCOME' ? 'bg-emerald-50 text-emerald-700 border-emerald-100' :
                                                'bg-rose-50 border-rose-100 text-rose-700'
                                            }`}>
                                                {acc.type}
                                            </span>
                                        </td>
                                        <td className="p-4 text-right font-mono font-medium text-slate-800">{formatMoney(acc.balance)}</td>
                                        <td className="p-4 text-center">
                                            <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full flex items-center justify-center gap-1 w-fit mx-auto ${acc.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                                                <div className={`w-1.5 h-1.5 rounded-full ${acc.active ? 'bg-green-500' : 'bg-gray-400'}`}></div>
                                                {acc.active ? 'Active' : 'Disabled'}
                                            </span>
                                        </td>
                                        <td className="p-4 text-center">
                                            <button onClick={() => toggleAccount(acc.code)} className="text-slate-400 hover:text-indigo-600 transition">
                                                {acc.active ? <ToggleRight size={24} className="text-emerald-500"/> : <ToggleLeft size={24}/>}
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {/* --- VIEW 3: BALANCE SHEET --- */}
            {activeView === 'balance-sheet' && (
                <div className="space-y-6 print:hidden">
                    <div className={`p-4 rounded-xl flex items-center justify-between shadow-sm border ${isBalanced ? 'bg-emerald-50 border-emerald-200' : 'bg-rose-50 border-rose-200'}`}>
                        <div className="flex items-center gap-3">
                            <div className={`p-2 rounded-full ${isBalanced ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'}`}>
                                {isBalanced ? <CheckCircle size={24} /> : <AlertTriangle size={24} />}
                            </div>
                            <div>
                                <h3 className={`font-bold ${isBalanced ? 'text-emerald-900' : 'text-rose-900'}`}>{isBalanced ? 'Balanced' : 'Unbalanced'}</h3>
                                {!isBalanced && <p className="text-sm text-rose-700">Diff: <strong>KES {formatMoney(balanceDifference)}</strong></p>}
                            </div>
                        </div>
                        <button onClick={() => setPrintingReport('balance-sheet')} className="bg-white border border-slate-200 text-slate-700 px-4 py-2 rounded-lg font-bold text-sm flex items-center gap-2 hover:bg-slate-50 transition shadow-sm"><Printer size={16}/> Print Report</button>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <StatementSection title="Assets" accounts={accounts.filter(a => a.type === 'ASSET' && a.active)} total={totalAssets} color="blue" icon={DollarSign} />
                        <div className="space-y-6">
                            <StatementSection title="Liabilities" accounts={accounts.filter(a => a.type === 'LIABILITY' && a.active)} total={totalLiabilities} color="orange" icon={AlertCircle} />
                            <div className="bg-white rounded-2xl shadow-sm border-t-4 border-purple-500 p-6 border-x border-b border-slate-200">
                                <div className="flex justify-between items-center mb-4 border-b border-slate-100 pb-4">
                                    <h3 className="font-bold text-slate-800 flex items-center gap-2"><Users className="text-purple-500" size={20}/> Equity</h3>
                                    <span className="text-purple-700 font-bold bg-purple-50 px-3 py-1 rounded-full text-sm">{formatMoney(totalEquity)}</span>
                                </div>
                                <div className="space-y-2">
                                    {accounts.filter(a => a.type === 'EQUITY' && a.active).map(a => (<StatementRow key={a.code} account={a} />))}
                                    <div className="flex justify-between text-sm bg-purple-50 p-3 rounded-lg border border-purple-100 mt-2">
                                        <span className="text-purple-900 font-bold">Net Income (Current)</span>
                                        <span className="font-mono font-bold text-purple-700">{formatMoney(getNetIncome())}</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* --- VIEW 4: INCOME STATEMENT --- */}
            {activeView === 'income-statement' && (
                <div className="max-w-4xl mx-auto bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden print:hidden">
                    <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50">
                        <div>
                            <h3 className="text-xl font-bold text-slate-800">Income Statement</h3>
                            <p className="text-slate-500 text-sm">Profit & Loss Summary</p>
                        </div>
                        <button onClick={() => setPrintingReport('income-statement')} className="bg-white border border-slate-200 text-slate-700 px-4 py-2 rounded-lg font-bold text-sm flex items-center gap-2 hover:bg-slate-50 transition shadow-sm"><Printer size={16}/> Print</button>
                    </div>
                    <div className="p-8 space-y-8">
                        <div>
                            <h4 className="text-xs font-bold text-emerald-600 uppercase tracking-wider mb-4 border-b border-emerald-100 pb-2">Revenue</h4>
                            {accounts.filter(a => a.type === 'INCOME' && a.active).map(a => (<StatementRow key={a.code} account={a} />))}
                            <div className="flex justify-between pt-4 font-bold text-emerald-900 text-lg px-2"><span>Total Revenue</span><span>{formatMoney(getTotal('INCOME'))}</span></div>
                        </div>
                        <div>
                            <h4 className="text-xs font-bold text-rose-600 uppercase tracking-wider mb-4 border-b border-rose-100 pb-2">Expenses</h4>
                            {accounts.filter(a => a.type === 'EXPENSE' && a.active).map(a => (<StatementRow key={a.code} account={a} />))}
                            <div className="flex justify-between pt-4 font-bold text-rose-900 text-lg px-2"><span>Total Expenses</span><span>{formatMoney(getTotal('EXPENSE'))}</span></div>
                        </div>
                        <div className="bg-slate-900 text-white p-6 rounded-xl flex justify-between items-center shadow-lg">
                            <span className="text-lg font-bold">NET INCOME</span>
                            <span className={`text-2xl font-mono font-bold ${getNetIncome() >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>{formatMoney(getNetIncome())}</span>
                        </div>
                    </div>
                </div>
            )}

            {/* --- VIEW 5: LEDGER & JOURNAL --- */}
            {(activeView === 'ledger' || activeView === 'journal') && (
                <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden print:hidden">
                    <div className="p-5 border-b border-slate-100 bg-slate-50/50 flex justify-between items-center">
                        <h3 className="font-bold text-slate-800">{activeView === 'ledger' ? 'General Ledger Activity' : 'Journal Entries'}</h3>
                        <button onClick={() => setPrintingReport(activeView)} className="text-slate-500 hover:text-indigo-600 p-2 bg-white border border-slate-200 rounded-lg shadow-sm"><Printer size={18}/></button>
                    </div>
                    {activeView === 'ledger' && !dateRange.startDate ? (
                        <div className="p-20 text-center text-slate-400">
                            <Calendar size={48} className="mx-auto mb-4 text-slate-300"/>
                            <p>Select a Date Range above to view Ledger Activity.</p>
                        </div>
                    ) : (
                        <div className="p-0">
                            {activeView === 'ledger' ? (
                                <table className="w-full text-sm text-left">
                                    <thead className="bg-slate-50 text-slate-500 font-bold border-b border-slate-100 uppercase text-xs">
                                        <tr><th className="p-4">Account</th><th className="p-4 text-right">Opening</th><th className="p-4 text-right text-emerald-600">Debit</th><th className="p-4 text-right text-rose-600">Credit</th><th className="p-4 text-right">Change</th><th className="p-4 text-right font-black">Closing</th></tr>
                                    </thead>
                                    <tbody className="divide-y divide-slate-100">
                                        {activityReport.map((row, i) => (
                                            <tr key={i} className="hover:bg-slate-50"><td className="p-4 font-bold text-slate-700">{row.accountName}</td><td className="p-4 text-right font-mono text-slate-500">{formatMoney(row.openingBalance)}</td><td className="p-4 text-right font-mono text-emerald-600">{formatMoney(row.periodDebits)}</td><td className="p-4 text-right font-mono text-rose-600">{formatMoney(row.periodCredits)}</td><td className="p-4 text-right font-mono">{formatMoney(row.netChange)}</td><td className="p-4 text-right font-mono font-bold bg-slate-50">{formatMoney(row.closingBalance)}</td></tr>
                                        ))}
                                    </tbody>
                                </table>
                            ) : (
                                <div className="p-4 space-y-4">
                                    {journal.map(entry => (
                                        <div key={entry.id} className="bg-white border border-slate-200 rounded-xl p-4 hover:shadow-md transition">
                                            <div className="flex justify-between items-start mb-3 border-b border-slate-50 pb-2">
                                                <div><p className="font-bold text-slate-800">{entry.description}</p><p className="text-xs text-slate-400 font-mono">Ref: {entry.referenceNo}</p></div>
                                                <span className="text-xs bg-slate-100 px-2 py-1 rounded font-bold text-slate-600">{new Date(entry.transactionDate).toLocaleDateString()}</span>
                                            </div>
                                            <div className="space-y-1">
                                                {entry.lines.map(line => (
                                                    <div key={line.id} className="flex justify-between text-sm py-1"><span className="text-slate-600">{line.account.name}</span><div className="flex gap-6 font-mono text-xs"><span className={`w-20 text-right ${line.debit > 0 ? 'text-emerald-600 font-bold' : 'text-slate-300'}`}>{line.debit > 0 ? formatMoney(line.debit) : '-'}</span><span className={`w-20 text-right ${line.credit > 0 ? 'text-slate-600 font-bold' : 'text-slate-300'}`}>{line.credit > 0 ? formatMoney(line.credit) : '-'}</span></div></div>
                                                ))}
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    )}
                </div>
            )}

            {/* --- MODALS --- */}
            {/* ADD ACCOUNT */}
            {showAddModal && (
                <div className="fixed inset-0 bg-slate-900/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
                    <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6 animate-in zoom-in-95 duration-200">
                        <div className="flex justify-between items-center mb-6">
                            <h3 className="font-bold text-lg text-slate-800">Add Account</h3>
                            <button onClick={() => setShowAddModal(false)}><X className="text-slate-400 hover:text-rose-500" /></button>
                        </div>
                        <form onSubmit={handleCreateAccount} className="space-y-4">
                            <input type="text" placeholder="Code (e.g. 1001)" required className="w-full p-3 border rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 outline-none" value={newAccount.code} onChange={e => setNewAccount({...newAccount, code: e.target.value})} />
                            <input type="text" placeholder="Account Name" required className="w-full p-3 border rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 outline-none" value={newAccount.name} onChange={e => setNewAccount({...newAccount, name: e.target.value})} />
                            <select className="w-full p-3 border rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 outline-none" value={newAccount.type} onChange={e => setNewAccount({...newAccount, type: e.target.value})}>
                                <option value="ASSET">Asset</option><option value="LIABILITY">Liability</option><option value="EQUITY">Equity</option><option value="INCOME">Income</option><option value="EXPENSE">Expense</option>
                            </select>
                            <button type="submit" className="w-full bg-emerald-600 text-white py-3 rounded-xl font-bold hover:bg-emerald-700 transition">Save Account</button>
                        </form>
                    </div>
                </div>
            )}

            {/* POST ENTRY */}
            {showEntryModal && (
                <div className="fixed inset-0 bg-slate-900/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
                    <div className="bg-white rounded-2xl shadow-xl w-full max-w-4xl p-6 animate-in zoom-in-95 duration-200">
                        <div className="flex justify-between items-center mb-6 border-b border-slate-100 pb-4">
                            <h3 className="font-bold text-lg text-slate-800">Post Journal Entry</h3>
                            <button onClick={() => setShowEntryModal(false)}><X className="text-slate-400 hover:text-rose-500" /></button>
                        </div>
                        <form onSubmit={submitJournalEntry} className="space-y-6">
                            <div className="grid grid-cols-3 gap-4">
                                <input type="date" required className="p-3 border rounded-xl bg-slate-50 outline-none" value={entryForm.date} onChange={e => setEntryForm({...entryForm, date: e.target.value})} />
                                <input type="text" placeholder="Reference" required className="p-3 border rounded-xl bg-slate-50 outline-none" value={entryForm.reference} onChange={e => setEntryForm({...entryForm, reference: e.target.value})} />
                                <input type="text" placeholder="Description" required className="p-3 border rounded-xl bg-slate-50 outline-none" value={entryForm.description} onChange={e => setEntryForm({...entryForm, description: e.target.value})} />
                            </div>
                            <div className="bg-slate-50 rounded-xl p-4 border border-slate-200">
                                {entryForm.lines.map((line, i) => (
                                    <div key={i} className="flex gap-2 mb-2 items-center">
                                        <select className="flex-1 p-2 border rounded-lg text-sm" value={line.accountCode} onChange={e => handleEntryChange(i, 'accountCode', e.target.value)}>
                                            <option value="">Select Account</option>
                                            {accounts.filter(a => a.active).map(a => <option key={a.code} value={a.code}>{a.code} - {a.name}</option>)}
                                        </select>
                                        <input type="number" placeholder="Debit" className="w-32 p-2 border rounded-lg text-right text-sm" value={line.debit} onChange={e => handleEntryChange(i, 'debit', e.target.value)} />
                                        <input type="number" placeholder="Credit" className="w-32 p-2 border rounded-lg text-right text-sm" value={line.credit} onChange={e => handleEntryChange(i, 'credit', e.target.value)} />
                                        <button type="button" onClick={() => removeEntryLine(i)} className="text-slate-400 hover:text-rose-500"><Trash2 size={16}/></button>
                                    </div>
                                ))}
                                <button type="button" onClick={addEntryLine} className="text-xs font-bold text-indigo-600 hover:underline mt-2 flex items-center gap-1"><Plus size={14}/> Add Line</button>
                            </div>
                            <div className="flex justify-end gap-4 pt-2">
                                <button type="button" onClick={() => setShowEntryModal(false)} className="text-slate-500 hover:text-slate-700 font-bold">Cancel</button>
                                <button type="submit" className="bg-indigo-600 text-white px-6 py-2 rounded-xl font-bold hover:bg-indigo-700 transition shadow-lg shadow-indigo-200">Post Entry</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* --- HIDDEN PRINT AREA --- */}
            {printingReport && (
                <div ref={printRef} id="printable-report-content" className="hidden print:block fixed inset-0 bg-white z-[9999] p-10">
                    <div className="text-center mb-8 border-b-2 border-slate-800 pb-4">
                        <h1 className="text-2xl font-black uppercase text-slate-900">{orgName}</h1>
                        <p className="text-sm text-slate-600">{orgAddress}</p>
                        <h2 className="text-xl font-bold mt-4 uppercase underline">
                            {printingReport === 'balance-sheet' ? 'Balance Sheet' :
                             printingReport === 'income-statement' ? 'Income Statement' : 'Financial Report'}
                        </h2>
                        <p className="text-xs text-slate-500 mt-1">Generated: {new Date().toLocaleString()}</p>
                    </div>
                    {printingReport === 'balance-sheet' && (
                        <div className="grid grid-cols-2 gap-10 text-sm">
                            <div><h3 className="font-bold border-b mb-2">Assets</h3>{accounts.filter(a => a.type === 'ASSET' && a.active).map(a => <div key={a.code} className="flex justify-between py-1"><span>{a.name}</span><span className="font-mono">{formatMoney(a.balance)}</span></div>)}<div className="font-bold border-t pt-2 flex justify-between mt-2"><span>Total</span><span>{formatMoney(totalAssets)}</span></div></div>
                            <div><h3 className="font-bold border-b mb-2">Liabilities & Equity</h3>{accounts.filter(a => ['LIABILITY', 'EQUITY'].includes(a.type) && a.active).map(a => <div key={a.code} className="flex justify-between py-1"><span>{a.name}</span><span className="font-mono">{formatMoney(a.balance)}</span></div>)}<div className="flex justify-between py-1"><span>Net Income</span><span className="font-mono">{formatMoney(getNetIncome())}</span></div><div className="font-bold border-t pt-2 flex justify-between mt-2"><span>Total</span><span>{formatMoney(totalLiabilities + totalEquity)}</span></div></div>
                        </div>
                    )}
                    {printingReport === 'income-statement' && (
                        <div className="text-sm max-w-2xl mx-auto">
                            <h3 className="font-bold border-b mb-2">Revenue</h3>{accounts.filter(a => a.type === 'INCOME' && a.active).map(a => <div key={a.code} className="flex justify-between py-1"><span>{a.name}</span><span className="font-mono">{formatMoney(a.balance)}</span></div>)}<div className="font-bold text-right pt-2 mb-6">{formatMoney(getTotal('INCOME'))}</div>
                            <h3 className="font-bold border-b mb-2">Expenses</h3>{accounts.filter(a => a.type === 'EXPENSE' && a.active).map(a => <div key={a.code} className="flex justify-between py-1"><span>{a.name}</span><span className="font-mono">{formatMoney(a.balance)}</span></div>)}<div className="font-bold text-right pt-2 mb-6">{formatMoney(getTotal('EXPENSE'))}</div>
                            <div className="text-xl font-bold border-t-4 border-double pt-4 flex justify-between"><span>Net Income</span><span>{formatMoney(getNetIncome())}</span></div>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}

// --- SUB-COMPONENTS ---
const SummaryCard = ({ label, value, icon: Icon, color, border, isNet }) => (
    <div className={`p-6 rounded-2xl border ${border} ${color.replace('text', 'bg').split(' ')[0]} bg-opacity-20 relative overflow-hidden group hover:shadow-md transition`}>
        <div className={`absolute right-0 top-0 p-4 opacity-10 transition-transform group-hover:scale-110`}><Icon size={64} /></div>
        <p className={`text-xs font-bold uppercase tracking-wider ${color.split(' ')[1]} opacity-80`}>{label}</p>
        <h3 className={`text-2xl font-bold mt-1 ${isNet ? (value >= 0 ? 'text-emerald-700' : 'text-rose-700') : 'text-slate-800'}`}>KES {Number(value).toLocaleString(undefined, {minimumFractionDigits: 2})}</h3>
    </div>
);

const StatementSection = ({ title, accounts, total, color, icon: Icon }) => (
    <div className={`bg-white rounded-2xl shadow-sm border-t-4 border-${color}-500 p-6 border-x border-b border-slate-200`}>
        <div className="flex justify-between items-center mb-4 border-b border-slate-100 pb-4">
            <h3 className="font-bold text-slate-800 flex items-center gap-2"><Icon className={`text-${color}-500`} size={20}/> {title}</h3>
            <span className={`text-${color}-700 font-bold bg-${color}-50 px-3 py-1 rounded-full text-sm`}>{Number(total).toLocaleString(undefined, {minimumFractionDigits: 2})}</span>
        </div>
        <div className="space-y-2">
            {accounts.map(a => <StatementRow key={a.code} account={a} />)}
        </div>
    </div>
);

const StatementRow = ({ account }) => (
    <div className="flex justify-between text-sm group hover:bg-slate-50 p-2 rounded transition">
        <span className="text-slate-600 font-medium">{account.name} <span className="text-xs text-slate-300 ml-1 font-mono">#{account.code}</span></span>
        <span className="font-mono font-bold text-slate-700">{Number(account.balance).toLocaleString(undefined, {minimumFractionDigits: 2})}</span>
    </div>
);