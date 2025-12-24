import React, { useState, useEffect, useRef } from 'react';
import api from '../../../api';
import { 
    BookOpen, Loader2, Plus, X, Calendar, Filter, RefreshCw, Trash2, Save,
    LayoutDashboard, TrendingUp, DollarSign, Users, AlertCircle, FileText, PieChart as PieIcon,
    Printer, CheckCircle, AlertTriangle, Download
} from 'lucide-react';
import { 
    BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer 
} from 'recharts';
import { useSettings } from '../../../context/SettingsContext';

export default function AccountingReports() {
    const [accounts, setAccounts] = useState([]);
    const [journal, setJournal] = useState([]);
    const [activityReport, setActivityReport] = useState([]); // Stores dynamic ledger data
    const [view, setView] = useState('overview'); 
    const [loading, setLoading] = useState(true);

    // Global Settings for Branding
    const { settings, getImageUrl } = useSettings();
    const logoUrl = getImageUrl(settings.SACCO_LOGO);
    const orgName = settings.SACCO_NAME || "Sacco System";
    const orgAddress = settings.SACCO_ADDRESS || "Nairobi, Kenya";
    const orgContact = [settings.SACCO_EMAIL, settings.SACCO_PHONE].filter(Boolean).join(' | ');

    // Date Filters
    const [dateRange, setDateRange] = useState({ startDate: '', endDate: '' });

    // Modals & Printing
    const [showAddModal, setShowAddModal] = useState(false);
    const [showEntryModal, setShowEntryModal] = useState(false);
    const [printingReport, setPrintingReport] = useState(null); // 'balance-sheet' | 'income-statement' | 'ledger'
    const [isDownloading, setIsDownloading] = useState(false);

    // Forms
    const [newAccount, setNewAccount] = useState({ code: '', name: '', type: 'ASSET' });
    const [entryForm, setEntryForm] = useState({
        description: '',
        reference: '',
        date: new Date().toISOString().split('T')[0],
        lines: [{ accountCode: '', debit: 0, credit: 0 }, { accountCode: '', debit: 0, credit: 0 }]
    });

    const COLORS = ['#10B981', '#3B82F6', '#F59E0B', '#EF4444', '#8B5CF6'];

    useEffect(() => { fetchData(); }, [dateRange.endDate, dateRange.startDate]);

    const fetchData = async () => {
        setLoading(true);
        try {
            let accEndpoint = '/api/accounting/accounts'; 
            let activityEndpoint = null;

            if (dateRange.endDate) {
                let query = `?endDate=${dateRange.endDate}`;
                if (dateRange.startDate) query += `&startDate=${dateRange.startDate}`;
                accEndpoint = `/api/accounting/report${query}`;
                
                // Fetch Activity Report specifically if dates are present
                if (dateRange.startDate) {
                    activityEndpoint = `/api/accounting/report/activity${query}`;
                }
            }

            const promises = [
                api.get(accEndpoint),
                api.get('/api/accounting/journal')
            ];

            if (activityEndpoint) {
                promises.push(api.get(activityEndpoint));
            }

            const results = await Promise.all(promises);
            const accRes = results[0];
            const jourRes = results[1];
            const actRes = results.length > 2 ? results[2] : null;

            if(accRes.data.success) setAccounts(accRes.data.data);
            if(jourRes.data.success) setJournal(jourRes.data.data);
            
            // Handle activity report data
            if (actRes && actRes.data) {
                setActivityReport(actRes.data.activity || []);
            } else {
                setActivityReport([]); 
            }

        } catch (error) { console.error("Error fetching data", error); } 
        finally { setLoading(false); }
    };

    // --- LOGIC HELPERS ---
    const getTotal = (type) => accounts
        .filter(a => a.type === type && a.active)
        .reduce((sum, a) => sum + parseFloat(a.balance || 0), 0);

    const getNetIncome = () => getTotal('INCOME') - getTotal('EXPENSE');
    const formatMoney = (amount) => Number(amount).toLocaleString(undefined, {minimumFractionDigits: 2});

    // Balance Sheet Logic
    const totalAssets = getTotal('ASSET');
    const totalLiabilities = getTotal('LIABILITY');
    const totalEquity = getTotal('EQUITY') + getNetIncome(); // Retained Earnings included
    const balanceDifference = totalAssets - (totalLiabilities + totalEquity);
    const isBalanced = Math.abs(balanceDifference) < 1.0; // Allow small floating point margin

    // --- MANUAL ENTRY LOGIC ---
    const handleEntryChange = (index, field, value) => {
        const newLines = [...entryForm.lines];
        newLines[index][field] = value;
        if (field === 'debit' && value > 0) newLines[index].credit = 0;
        if (field === 'credit' && value > 0) newLines[index].debit = 0;
        setEntryForm({ ...entryForm, lines: newLines });
    };
    const addEntryLine = () => setEntryForm({ ...entryForm, lines: [...entryForm.lines, { accountCode: '', debit: 0, credit: 0 }] });
    const removeEntryLine = (index) => {
        if (entryForm.lines.length > 2) setEntryForm({ ...entryForm, lines: entryForm.lines.filter((_, i) => i !== index) });
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
    const handleCreateAccount = async (e) => {
        e.preventDefault();
        try {
            await api.post('/api/accounting/accounts', newAccount);
            setShowAddModal(false);
            setNewAccount({ code: '', name: '', type: 'ASSET' });
            fetchData();
        } catch (err) { alert("Failed to create account."); }
    };
    const toggleAccount = async (code) => { await api.put(`/api/accounting/accounts/${code}/toggle`); fetchData(); };

    // --- PRINTING & DOWNLOADING LOGIC ---
    const handlePrint = (reportType) => {
        setPrintingReport(reportType);
        setTimeout(() => {
            window.print();
        }, 500);
    };

    const handleDownloadPdf = async () => {
        setIsDownloading(true);
        const element = document.getElementById('printable-report-content');
        if (!element) {
            alert("Report content not found!");
            setIsDownloading(false);
            return;
        }

        // Dynamically load html2pdf from CDN if not present
        if (!window.html2pdf) {
            try {
                await new Promise((resolve, reject) => {
                    const script = document.createElement('script');
                    script.src = 'https://cdnjs.cloudflare.com/ajax/libs/html2pdf.js/0.10.1/html2pdf.bundle.min.js';
                    script.onload = resolve;
                    script.onerror = reject;
                    document.body.appendChild(script);
                });
            } catch (e) {
                alert("Could not load PDF generator. Please use the Print -> Save as PDF option.");
                window.print();
                setIsDownloading(false);
                return;
            }
        }

        const opt = {
            margin:       [10, 10, 10, 10], // mm
            filename:     `${printingReport}_${new Date().toISOString().split('T')[0]}.pdf`,
            image:        { type: 'jpeg', quality: 0.98 },
            html2canvas:  { scale: 2, useCORS: true },
            jsPDF:        { unit: 'mm', format: 'a4', orientation: 'portrait' },
            pagebreak:    { mode: ['avoid-all', 'css', 'legacy'] }
        };

        window.html2pdf().set(opt).from(element).save().then(() => {
            setIsDownloading(false);
        });
    };

    // --- CHART DATA PREP ---
    const plData = [
        { name: 'Revenue', amount: getTotal('INCOME') },
        { name: 'Expenses', amount: getTotal('EXPENSE') },
        { name: 'Net Profit', amount: getNetIncome() },
    ];
    const assetData = accounts.filter(a => a.type === 'ASSET' && a.active && parseFloat(a.balance) > 0).map(a => ({ name: a.name, value: parseFloat(a.balance) })).sort((a, b) => b.value - a.value).slice(0, 5);
    const incomeData = accounts.filter(a => a.type === 'INCOME' && a.active && parseFloat(a.balance) > 0).map(a => ({ name: a.name, amount: parseFloat(a.balance) })).sort((a, b) => b.amount - a.amount).slice(0, 5);
    const entryTotalDebit = entryForm.lines.reduce((s, l) => s + Number(l.debit), 0);
    const entryTotalCredit = entryForm.lines.reduce((s, l) => s + Number(l.credit), 0);

    if (loading) return <div className="p-20 text-center text-slate-400"><Loader2 className="animate-spin inline mr-2"/> Compiling Ledger...</div>;

    return (
        <div className="space-y-6 animate-in fade-in">

            {/* --- HEADER & CONTROLS (Hidden on Print) --- */}
            <div className="flex flex-col xl:flex-row justify-between items-center bg-white p-4 rounded-xl shadow-sm border border-slate-200 gap-4 print:hidden">
                <h2 className="font-bold text-slate-800 flex items-center gap-2 text-lg">
                    <BookOpen className="text-indigo-600" size={24} />
                    Finance & Accounting
                </h2>

                <div className="flex flex-col md:flex-row gap-3 items-center w-full md:w-auto">
                    <div className="flex items-center gap-2 bg-slate-50 p-1 rounded-lg border border-slate-200">
                        <input type="date" className="bg-transparent text-xs font-medium text-slate-600 p-1.5 outline-none" value={dateRange.startDate} onChange={(e) => setDateRange({...dateRange, startDate: e.target.value})} />
                        <span className="text-slate-300">-</span>
                        <input type="date" className="bg-transparent text-xs font-medium text-slate-600 p-1.5 outline-none" value={dateRange.endDate} onChange={(e) => setDateRange({...dateRange, endDate: e.target.value})} />
                        {(dateRange.startDate || dateRange.endDate) && (
                            <button onClick={() => setDateRange({startDate:'', endDate:''})} className="p-1 hover:text-red-500"><X size={14}/></button>
                        )}
                    </div>

                    <div className="flex bg-slate-100 p-1 rounded-lg overflow-x-auto">
                        {[
                            { id: 'overview', label: 'Overview', icon: LayoutDashboard },
                            { id: 'balance-sheet', label: 'Balance Sheet', icon: FileText },
                            { id: 'income-statement', label: 'P&L', icon: TrendingUp },
                            { id: 'ledger', label: 'Ledger', icon: BookOpen },
                            { id: 'journal', label: 'Journal', icon: Calendar }
                        ].map(tab => (
                            <button
                                key={tab.id}
                                onClick={() => setView(tab.id)}
                                className={`px-3 py-1.5 text-xs sm:text-sm font-bold rounded-md transition flex items-center gap-2 whitespace-nowrap ${view === tab.id ? 'bg-white shadow text-slate-900' : 'text-slate-500 hover:text-slate-700'}`}
                            >
                                <tab.icon size={14} className={view === tab.id ? "text-indigo-600" : ""} />
                                {tab.label}
                            </button>
                        ))}
                    </div>

                    <button onClick={() => setShowEntryModal(true)} className="bg-slate-900 text-white px-4 py-2 rounded-lg text-xs font-bold flex items-center gap-2 hover:bg-slate-800 transition shadow-lg shadow-slate-900/20">
                        <Plus size={16} /> Post Entry
                    </button>
                </div>
            </div>

            {/* --- VIEW 1: OVERVIEW --- */}
            {view === 'overview' && (
                <div className="space-y-6 print:hidden">
                    {/* KPI Cards */}
                    <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                        <div className="bg-gradient-to-br from-blue-600 to-indigo-700 text-white p-6 rounded-xl shadow-lg relative overflow-hidden">
                            <DollarSign className="absolute right-4 top-4 opacity-20 w-12 h-12" />
                            <p className="text-blue-100 text-xs font-bold uppercase">Total Assets</p>
                            <h3 className="text-2xl font-bold mt-1">KES {formatMoney(getTotal('ASSET'))}</h3>
                        </div>
                        <div className="bg-gradient-to-br from-emerald-500 to-teal-600 text-white p-6 rounded-xl shadow-lg relative overflow-hidden">
                            <TrendingUp className="absolute right-4 top-4 opacity-20 w-12 h-12" />
                            <p className="text-emerald-100 text-xs font-bold uppercase">Total Income</p>
                            <h3 className="text-2xl font-bold mt-1">KES {formatMoney(getTotal('INCOME'))}</h3>
                        </div>
                        <div className="bg-gradient-to-br from-orange-500 to-red-500 text-white p-6 rounded-xl shadow-lg relative overflow-hidden">
                            <AlertCircle className="absolute right-4 top-4 opacity-20 w-12 h-12" />
                            <p className="text-orange-100 text-xs font-bold uppercase">Total Expenses</p>
                            <h3 className="text-2xl font-bold mt-1">KES {formatMoney(getTotal('EXPENSE'))}</h3>
                        </div>
                        <div className="bg-gradient-to-br from-violet-500 to-purple-600 text-white p-6 rounded-xl shadow-lg relative overflow-hidden">
                            <Users className="absolute right-4 top-4 opacity-20 w-12 h-12" />
                            <p className="text-purple-100 text-xs font-bold uppercase">Net Income</p>
                            <h3 className="text-2xl font-bold mt-1">KES {formatMoney(getNetIncome())}</h3>
                        </div>
                    </div>
                    {/* Charts */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                        <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-200">
                            <h3 className="font-bold text-slate-800 mb-6 flex items-center gap-2"><TrendingUp size={18} className="text-emerald-600"/> P&L Summary</h3>
                            <div className="h-64 w-full"><ResponsiveContainer width="100%" height="100%"><BarChart data={plData}><CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" /><XAxis dataKey="name" fontSize={12} /><YAxis fontSize={12} tickFormatter={(val) => `${val/1000}k`} /><Tooltip formatter={(value) => formatMoney(value)} cursor={{fill: 'transparent'}} /><Bar dataKey="amount" radius={[4, 4, 0, 0]} barSize={50}><Cell fill="#10B981" /><Cell fill="#EF4444" /><Cell fill="#3B82F6" /></Bar></BarChart></ResponsiveContainer></div>
                        </div>
                        <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-200">
                            <h3 className="font-bold text-slate-800 mb-6 flex items-center gap-2"><PieIcon size={18} className="text-blue-600"/> Asset Allocation</h3>
                            <div className="h-64 w-full flex items-center justify-center">{assetData.length > 0 ? (<ResponsiveContainer width="100%" height="100%"><PieChart><Pie data={assetData} cx="50%" cy="50%" innerRadius={60} outerRadius={80} paddingAngle={5} dataKey="value">{assetData.map((entry, index) => (<Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />))}</Pie><Tooltip formatter={(value) => formatMoney(value)} /><Legend verticalAlign="bottom" height={36}/></PieChart></ResponsiveContainer>) : (<p className="text-slate-400 text-sm">No assets recorded yet.</p>)}</div>
                        </div>
                        <div className="lg:col-span-2 bg-white p-6 rounded-xl shadow-sm border border-slate-200">
                            <h3 className="font-bold text-slate-800 mb-6 flex items-center gap-2"><DollarSign size={18} className="text-purple-600"/> Top Revenue Sources</h3>
                            <div className="h-64 w-full">{incomeData.length > 0 ? (<ResponsiveContainer width="100%" height="100%"><BarChart data={incomeData} layout="vertical" margin={{ left: 40 }}><CartesianGrid strokeDasharray="3 3" horizontal={true} vertical={false} stroke="#E2E8F0" /><XAxis type="number" fontSize={12} tickFormatter={(val) => `${val/1000}k`} /><YAxis dataKey="name" type="category" width={150} fontSize={11} fontWeight={500} /><Tooltip formatter={(value) => formatMoney(value)} cursor={{fill: '#F8FAFC'}} /><Bar dataKey="amount" fill="#8B5CF6" radius={[0, 4, 4, 0]} barSize={20} /></BarChart></ResponsiveContainer>) : (<p className="text-slate-400 text-sm text-center pt-20">No revenue data available.</p>)}</div>
                        </div>
                    </div>
                </div>
            )}

            {/* --- VIEW 2: BALANCE SHEET --- */}
            {view === 'balance-sheet' && (
                <div className="space-y-6 print:hidden">
                    {/* Balance Checker Banner */}
                    <div className={`p-4 rounded-xl flex items-center justify-between shadow-sm border ${isBalanced ? 'bg-emerald-50 border-emerald-200' : 'bg-red-50 border-red-200'}`}>
                        <div className="flex items-center gap-3">
                            <div className={`p-2 rounded-full ${isBalanced ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'}`}>
                                {isBalanced ? <CheckCircle size={24} /> : <AlertTriangle size={24} />}
                            </div>
                            <div>
                                <h3 className={`font-bold ${isBalanced ? 'text-emerald-900' : 'text-red-900'}`}>
                                    {isBalanced ? 'Balance Sheet is Balanced' : 'Balance Sheet is Unbalanced'}
                                </h3>
                                {!isBalanced && <p className="text-sm text-red-700">Difference: <strong>KES {formatMoney(balanceDifference)}</strong> (Assets ≠ Liab + Equity)</p>}
                            </div>
                        </div>
                        <button onClick={() => handlePrint('balance-sheet')} className="bg-white border border-slate-300 text-slate-700 px-4 py-2 rounded-lg font-bold text-sm flex items-center gap-2 hover:bg-slate-50 transition shadow-sm">
                            <Printer size={16}/> Print / Download
                        </button>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        {/* Assets */}
                        <div className="bg-white rounded-xl shadow-sm border-t-4 border-blue-500 p-6">
                            <div className="flex justify-between items-center mb-4 border-b border-slate-100 pb-4">
                                <h3 className="font-bold text-slate-800 flex items-center gap-2"><DollarSign className="text-blue-500" size={20}/> Assets</h3>
                                <span className="text-blue-700 font-bold bg-blue-50 px-3 py-1 rounded-full text-sm">{formatMoney(totalAssets)}</span>
                            </div>
                            <div className="space-y-3">
                                {accounts.filter(a => a.type === 'ASSET' && a.active).map(a => (
                                    <div key={a.code} className="flex justify-between text-sm group hover:bg-slate-50 p-2 rounded transition">
                                        <span className="text-slate-600">{a.name} <span className="text-xs text-slate-300 ml-1">{a.code}</span></span>
                                        <span className="font-mono font-medium text-slate-900">{formatMoney(a.balance)}</span>
                                    </div>
                                ))}
                            </div>
                        </div>

                        {/* Liabilities & Equity */}
                        <div className="space-y-6">
                            <div className="bg-white rounded-xl shadow-sm border-t-4 border-red-500 p-6">
                                <div className="flex justify-between items-center mb-4 border-b border-slate-100 pb-4">
                                    <h3 className="font-bold text-slate-800 flex items-center gap-2"><AlertCircle className="text-red-500" size={20}/> Liabilities</h3>
                                    <span className="text-red-700 font-bold bg-red-50 px-3 py-1 rounded-full text-sm">{formatMoney(totalLiabilities)}</span>
                                </div>
                                <div className="space-y-3">
                                    {accounts.filter(a => a.type === 'LIABILITY' && a.active).map(a => (
                                        <div key={a.code} className="flex justify-between text-sm hover:bg-slate-50 p-2 rounded transition">
                                            <span className="text-slate-600">{a.name} <span className="text-xs text-slate-300 ml-1">{a.code}</span></span>
                                            <span className="font-mono font-medium text-slate-900">{formatMoney(a.balance)}</span>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            <div className="bg-white rounded-xl shadow-sm border-t-4 border-purple-500 p-6">
                                <div className="flex justify-between items-center mb-4 border-b border-slate-100 pb-4">
                                    <h3 className="font-bold text-slate-800 flex items-center gap-2"><Users className="text-purple-500" size={20}/> Equity</h3>
                                    <span className="text-purple-700 font-bold bg-purple-50 px-3 py-1 rounded-full text-sm">{formatMoney(totalEquity)}</span>
                                </div>
                                <div className="space-y-3">
                                    {accounts.filter(a => a.type === 'EQUITY' && a.active).map(a => (
                                        <div key={a.code} className="flex justify-between text-sm hover:bg-slate-50 p-2 rounded transition">
                                            <span className="text-slate-600">{a.name} <span className="text-xs text-slate-300 ml-1">{a.code}</span></span>
                                            <span className="font-mono font-medium text-slate-900">{formatMoney(a.balance)}</span>
                                        </div>
                                    ))}
                                    <div className="flex justify-between text-sm bg-purple-50/50 p-2 rounded border border-purple-100">
                                        <span className="text-purple-900 font-medium">Net Income (Current)</span>
                                        <span className="font-mono font-bold text-purple-700">{formatMoney(getNetIncome())}</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* --- VIEW 3: INCOME STATEMENT --- */}
            {view === 'income-statement' && (
                <div className="max-w-4xl mx-auto bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden print:hidden">
                    <div className="p-8 border-b border-slate-100 text-center bg-slate-50/50 flex justify-between items-center">
                        <div className="text-left">
                            <h3 className="text-2xl font-bold text-slate-800">Income Statement</h3>
                            <p className="text-slate-500 text-sm mt-1">Statement of Profit & Loss</p>
                        </div>
                        <button onClick={() => handlePrint('income-statement')} className="bg-white border border-slate-300 text-slate-700 px-4 py-2 rounded-lg font-bold text-sm flex items-center gap-2 hover:bg-slate-50 transition shadow-sm">
                            <Printer size={16}/> Print / Download
                        </button>
                    </div>
                    <div className="p-8 space-y-8">
                        <div>
                            <h4 className="text-xs font-bold text-emerald-600 uppercase tracking-wider mb-4 border-b border-emerald-100 pb-2">Revenue</h4>
                            {accounts.filter(a => a.type === 'INCOME' && a.active).map(a => (
                                <div key={a.code} className="flex justify-between py-2 border-b border-slate-50 last:border-0">
                                    <span className="text-slate-600">{a.name}</span>
                                    <span className="font-mono font-medium text-slate-800">{formatMoney(a.balance)}</span>
                                </div>
                            ))}
                            <div className="flex justify-between pt-4 font-bold text-emerald-800 text-lg">
                                <span>Total Revenue</span>
                                <span>{formatMoney(getTotal('INCOME'))}</span>
                            </div>
                        </div>

                        <div>
                            <h4 className="text-xs font-bold text-red-600 uppercase tracking-wider mb-4 border-b border-red-100 pb-2">Expenses</h4>
                            {accounts.filter(a => a.type === 'EXPENSE' && a.active).map(a => (
                                <div key={a.code} className="flex justify-between py-2 border-b border-slate-50 last:border-0">
                                    <span className="text-slate-600">{a.name}</span>
                                    <span className="font-mono font-medium text-slate-800">{formatMoney(a.balance)}</span>
                                </div>
                            ))}
                            <div className="flex justify-between pt-4 font-bold text-red-800 text-lg">
                                <span>Total Expenses</span>
                                <span>{formatMoney(getTotal('EXPENSE'))}</span>
                            </div>
                        </div>

                        <div className="bg-slate-900 text-white p-6 rounded-xl flex justify-between items-center shadow-lg">
                            <span className="text-lg font-bold">NET INCOME</span>
                            <span className={`text-2xl font-mono font-bold ${getNetIncome() >= 0 ? 'text-emerald-400' : 'text-red-400'}`}>
                                {formatMoney(getNetIncome())}
                            </span>
                        </div>
                    </div>
                </div>
            )}

            {/* --- VIEW 4: LEDGER ACTIVITY (NEW) --- */}
            {view === 'ledger' && (
                <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden print:hidden">
                    <div className="p-4 border-b border-slate-100 bg-slate-50 flex justify-between items-center">
                        <div className="flex items-center gap-4">
                            <h3 className="font-bold text-slate-700">General Ledger Activity</h3>
                            <span className="text-xs text-slate-500 bg-white border px-2 py-1 rounded">
                                {dateRange.startDate ? `${dateRange.startDate} to ${dateRange.endDate || 'Now'}` : 'All Time'}
                            </span>
                        </div>
                        <div className="flex gap-2">
                             <button onClick={() => setShowAddModal(true)} className="bg-white border border-slate-300 text-slate-700 px-3 py-1.5 rounded-lg text-xs font-bold hover:bg-slate-50 flex items-center gap-1"><Plus size={14}/> Add Account</button>
                             <button onClick={() => handlePrint('ledger')} className="text-slate-600 hover:text-indigo-600 p-2"><Printer size={18}/></button>
                        </div>
                    </div>

                    {!dateRange.startDate ? (
                        <div className="p-12 text-center text-slate-400 flex flex-col items-center">
                            <Calendar size={48} className="mb-4 text-slate-200"/>
                            <p>Please select a Start Date to view the Period Activity and Opening Balances.</p>
                        </div>
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm text-left">
                                <thead className="bg-slate-50 text-slate-500 font-bold border-b border-slate-100">
                                    <tr>
                                        <th className="p-4">Account</th>
                                        <th className="p-4 text-right">Opening Bal</th>
                                        <th className="p-4 text-right text-emerald-600">Debit</th>
                                        <th className="p-4 text-right text-red-600">Credit</th>
                                        <th className="p-4 text-right">Net Change</th>
                                        <th className="p-4 text-right font-black">Closing Bal</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-100">
                                    {activityReport.map((row, i) => (
                                        <tr key={i} className="hover:bg-slate-50">
                                            <td className="p-4">
                                                <div className="font-bold text-slate-700">{row.accountName}</div>
                                                <div className="text-xs text-slate-400 font-mono">{row.accountCode}</div>
                                            </td>
                                            <td className="p-4 text-right font-mono text-slate-500">{formatMoney(row.openingBalance)}</td>
                                            <td className="p-4 text-right font-mono text-emerald-600">{formatMoney(row.periodDebits)}</td>
                                            <td className="p-4 text-right font-mono text-red-600">{formatMoney(row.periodCredits)}</td>
                                            <td className="p-4 text-right font-mono font-medium">{formatMoney(row.netChange)}</td>
                                            <td className="p-4 text-right font-mono font-bold text-slate-900 bg-slate-50/50">{formatMoney(row.closingBalance)}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            )}

            {/* --- VIEW 5: JOURNAL --- */}
            {view === 'journal' && (
                <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden print:hidden">
                    <div className="p-4 space-y-4">
                        {journal.map(entry => (
                            <div key={entry.id} className="bg-white rounded-xl shadow-sm border border-slate-200 p-4 hover:shadow-md transition">
                                <div className="flex justify-between items-start mb-3 border-b border-slate-50 pb-2"><div><p className="font-bold text-slate-800">{entry.description}</p><p className="text-xs text-slate-400 font-mono">Ref: {entry.referenceNo}</p></div><span className="text-xs bg-slate-100 px-2 py-1 rounded font-medium">{new Date(entry.transactionDate).toLocaleDateString()}</span></div>
                                <div className="space-y-1">{entry.lines.map(line => (<div key={line.id} className="flex justify-between text-sm"><span className="text-slate-600">{line.account.name} <span className="text-xs text-slate-300">({line.account.code})</span></span><div className="flex gap-4 font-mono text-xs"><span className="w-20 text-right text-emerald-600">{line.debit > 0 ? formatMoney(line.debit) : '-'}</span><span className="w-20 text-right text-slate-600">{line.credit > 0 ? formatMoney(line.credit) : '-'}</span></div></div>))}</div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* --- PRINTABLE REPORT TEMPLATE (Hidden unless printing) --- */}
            {printingReport && (
                <div className="fixed inset-0 bg-white z-[9999] overflow-auto hidden print:block" id="printable-report-content">
                    <style>{`
                        @media print {
                            @page { size: A4; margin: 10mm; }
                            body { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
                            /* Hide everything except the printable root content */
                            body > *:not(#root) { display: none; }
                            #root > *:not(.print\\:block) { display: none; }
                            
                            /* Layout for Repeated Header/Footer */
                            tfoot { display: table-footer-group; }
                            thead { display: table-header-group; }
                            .report-footer-space { height: 40px; }
                            .report-header-space { height: 100px; }
                            
                            /* Fixed Header/Footer Positioning */
                            .report-header-fixed { position: fixed; top: 0; left: 0; right: 0; height: 100px; background: white; z-index: 10; border-bottom: 2px solid #0f172a; }
                            .report-footer-fixed { position: fixed; bottom: 0; left: 0; right: 0; height: 30px; border-top: 1px solid #cbd5e1; background: white; z-index: 10; padding-top: 5px; }
                        }
                    `}</style>
                    
                    {/* Floating Controls for Screen View (Hidden in Print) */}
                    <div className="fixed top-4 right-4 flex gap-2 print:hidden z-50">
                        <button 
                            onClick={handleDownloadPdf} 
                            disabled={isDownloading}
                            className={`bg-indigo-600 text-white px-4 py-2 rounded-lg font-bold shadow-lg flex items-center gap-2 hover:bg-indigo-700 ${isDownloading ? 'opacity-50 cursor-wait' : ''}`}
                        >
                            {isDownloading ? <Loader2 className="animate-spin" size={16}/> : <Download size={16}/>} 
                            {isDownloading ? 'Generating...' : 'Download PDF'}
                        </button>
                        <button onClick={() => window.print()} className="bg-slate-700 text-white px-4 py-2 rounded-lg font-bold shadow-lg flex items-center gap-2 hover:bg-slate-800">
                            <Printer size={16}/> Print
                        </button>
                        <button onClick={() => setPrintingReport(null)} className="bg-white text-slate-700 border px-4 py-2 rounded-lg font-bold shadow-lg hover:bg-slate-50">
                            Close
                        </button>
                    </div>

                    {/* MAIN PRINTABLE TABLE (Wraps content to allow header/footer repetition) */}
                    <table className="w-full text-sm">
                        {/* THEAD: Reserved space for fixed header */}
                        <thead>
                            <tr>
                                <td><div className="report-header-space"></div></td>
                            </tr>
                        </thead>

                        {/* TBODY: Actual Content */}
                        <tbody>
                            <tr>
                                <td>
                                    <div className="py-4">
                                        {/* Dynamic Content Switching */}
                                        {printingReport === 'balance-sheet' && (
                                            <div className="grid grid-cols-2 gap-8">
                                                <div>
                                                    <h3 className="font-bold text-slate-800 border-b border-black pb-1 mb-2">ASSETS</h3>
                                                    {accounts.filter(a => a.type === 'ASSET' && a.active).map(a => (
                                                        <div key={a.code} className="flex justify-between py-1 border-b border-slate-100"><span className="text-slate-700">{a.name}</span><span className="font-mono">{formatMoney(a.balance)}</span></div>
                                                    ))}
                                                    <div className="flex justify-between border-t-2 border-black pt-2 mt-4 font-bold"><span className="uppercase">Total Assets</span><span>{formatMoney(totalAssets)}</span></div>
                                                </div>
                                                <div className="space-y-8">
                                                    <div>
                                                        <h3 className="font-bold text-slate-800 border-b border-black pb-1 mb-2">LIABILITIES</h3>
                                                        {accounts.filter(a => a.type === 'LIABILITY' && a.active).map(a => (
                                                            <div key={a.code} className="flex justify-between py-1 border-b border-slate-100"><span className="text-slate-700">{a.name}</span><span className="font-mono">{formatMoney(a.balance)}</span></div>
                                                        ))}
                                                        <div className="flex justify-between border-t border-black pt-2 mt-2 font-bold text-sm"><span>Total Liabilities</span><span>{formatMoney(totalLiabilities)}</span></div>
                                                    </div>
                                                    <div>
                                                        <h3 className="font-bold text-slate-800 border-b border-black pb-1 mb-2">EQUITY</h3>
                                                        {accounts.filter(a => a.type === 'EQUITY' && a.active).map(a => (
                                                            <div key={a.code} className="flex justify-between py-1 border-b border-slate-100"><span className="text-slate-700">{a.name}</span><span className="font-mono">{formatMoney(a.balance)}</span></div>
                                                        ))}
                                                        <div className="flex justify-between py-1 border-b border-slate-100"><span className="text-slate-700">Net Income</span><span className="font-mono">{formatMoney(getNetIncome())}</span></div>
                                                        <div className="flex justify-between border-t border-black pt-2 mt-2 font-bold text-sm"><span>Total Equity</span><span>{formatMoney(totalEquity)}</span></div>
                                                    </div>
                                                    <div className="flex justify-between border-t-2 border-black pt-2 font-bold"><span className="uppercase">Total Liab. & Equity</span><span>{formatMoney(totalLiabilities + totalEquity)}</span></div>
                                                </div>
                                            </div>
                                        )}

                                        {printingReport === 'income-statement' && (
                                            <div className="max-w-2xl mx-auto">
                                                 <h3 className="font-bold text-slate-800 border-b border-black pb-1 mb-2">REVENUE</h3>
                                                 {accounts.filter(a => a.type === 'INCOME' && a.active).map(a => (
                                                     <div key={a.code} className="flex justify-between py-1 border-b border-slate-100"><span className="text-slate-700">{a.name}</span><span className="font-mono">{formatMoney(a.balance)}</span></div>
                                                 ))}
                                                 <div className="flex justify-between border-t border-black pt-2 mt-2 mb-8 font-bold"><span>Total Revenue</span><span>{formatMoney(getTotal('INCOME'))}</span></div>

                                                 <h3 className="font-bold text-slate-800 border-b border-black pb-1 mb-2">EXPENSES</h3>
                                                 {accounts.filter(a => a.type === 'EXPENSE' && a.active).map(a => (
                                                     <div key={a.code} className="flex justify-between py-1 border-b border-slate-100"><span className="text-slate-700">{a.name}</span><span className="font-mono">{formatMoney(a.balance)}</span></div>
                                                 ))}
                                                 <div className="flex justify-between border-t border-black pt-2 mt-2 font-bold"><span>Total Expenses</span><span>{formatMoney(getTotal('EXPENSE'))}</span></div>

                                                 <div className="flex justify-between border-t-4 double border-black pt-4 mt-8 text-xl font-bold"><span>NET INCOME</span><span>{formatMoney(getNetIncome())}</span></div>
                                            </div>
                                        )}

                                        {printingReport === 'ledger' && (
                                            <table className="w-full text-xs">
                                                <thead className="border-b border-black font-bold text-left">
                                                    <tr>
                                                        <th className="py-2">Account</th>
                                                        <th className="text-right">Opening</th>
                                                        <th className="text-right">Debit</th>
                                                        <th className="text-right">Credit</th>
                                                        <th className="text-right">Closing</th>
                                                    </tr>
                                                </thead>
                                                <tbody className="divide-y divide-slate-200">
                                                    {activityReport.map((row, i) => (
                                                        <tr key={i}>
                                                            <td className="py-2">
                                                                <span className="font-bold">{row.accountName}</span> <span className="text-slate-500">({row.accountCode})</span>
                                                            </td>
                                                            <td className="text-right">{formatMoney(row.openingBalance)}</td>
                                                            <td className="text-right">{formatMoney(row.periodDebits)}</td>
                                                            <td className="text-right">{formatMoney(row.periodCredits)}</td>
                                                            <td className="text-right font-bold">{formatMoney(row.closingBalance)}</td>
                                                        </tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                        )}
                                        
                                        {/* Signatories Area */}
                                        <div className="mt-20 grid grid-cols-2 gap-20 page-break-inside-avoid">
                                            <div className="border-t border-black pt-2">
                                                <p className="font-bold text-slate-800 text-sm">Prepared By:</p>
                                                <p className="text-xs text-slate-500 mt-8">Signature & Date</p>
                                            </div>
                                            <div className="border-t border-black pt-2">
                                                <p className="font-bold text-slate-800 text-sm">Approved By:</p>
                                                <p className="text-xs text-slate-500 mt-8">Signature & Date</p>
                                            </div>
                                        </div>
                                    </div>
                                </td>
                            </tr>
                        </tbody>

                        {/* TFOOT: Reserved space for fixed footer */}
                        <tfoot>
                            <tr>
                                <td><div className="report-footer-space"></div></td>
                            </tr>
                        </tfoot>
                    </table>

                    {/* FIXED HEADER CONTENT */}
                    <div className="report-header-fixed px-10 py-6 flex justify-between items-start">
                        <div>
                            {logoUrl && <img src={logoUrl} alt="Logo" className="h-12 object-contain mb-1" />}
                            <h1 className="text-xl font-black text-slate-900 uppercase">
                                {printingReport.replace('-', ' ')} REPORT
                            </h1>
                            <p className="text-xs text-slate-600">
                                Period: {dateRange.startDate ? `${dateRange.startDate} to ${dateRange.endDate}` : `As of ${new Date().toLocaleDateString()}`}
                            </p>
                        </div>
                        <div className="text-right">
                            <h2 className="font-bold text-slate-800">{orgName}</h2>
                            <p className="text-[10px] text-slate-600 whitespace-pre-line leading-tight">{orgAddress}</p>
                            <p className="text-[10px] text-slate-600 mt-1">{orgContact}</p>
                        </div>
                    </div>

                    {/* FIXED FOOTER CONTENT */}
                    <div className="report-footer-fixed px-10 flex justify-between items-center text-[10px] text-slate-400 uppercase tracking-widest">
                        <span>System Generated • {new Date().toLocaleString()}</span>
                        <span>{orgName}</span>
                    </div>

                </div>
            )}

            {showEntryModal && (
                <div className="fixed inset-0 bg-slate-900/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm print:hidden">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-4xl overflow-hidden animate-in zoom-in-95 duration-200">
                        <div className="p-5 border-b border-slate-100 flex justify-between items-center bg-slate-50">
                            <h3 className="font-bold text-slate-800 flex items-center gap-2"><BookOpen size={20} className="text-slate-500"/> Post Manual Journal Entry</h3>
                            <button onClick={() => setShowEntryModal(false)}><X size={20} className="text-slate-400 hover:text-red-500"/></button>
                        </div>
                        <form onSubmit={submitJournalEntry} className="p-6 space-y-6">
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                                <div><label className="block text-xs font-bold text-slate-500 uppercase mb-1">Date</label><input type="date" required className="w-full p-2 border rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none" value={entryForm.date} onChange={e => setEntryForm({...entryForm, date: e.target.value})} /></div>
                                <div><label className="block text-xs font-bold text-slate-500 uppercase mb-1">Reference No</label><input type="text" required placeholder="e.g. INV-001" className="w-full p-2 border rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none" value={entryForm.reference} onChange={e => setEntryForm({...entryForm, reference: e.target.value})} /></div>
                                <div><label className="block text-xs font-bold text-slate-500 uppercase mb-1">Description</label><input type="text" required placeholder="e.g. Purchase of Office Desk" className="w-full p-2 border rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none" value={entryForm.description} onChange={e => setEntryForm({...entryForm, description: e.target.value})} /></div>
                            </div>
                            <div className="border rounded-xl overflow-hidden shadow-sm">
                                <table className="w-full text-sm text-left">
                                    <thead className="bg-slate-50 text-slate-500 font-bold border-b"><tr><th className="p-3 pl-4">Account</th><th className="p-3 w-32 text-right">Debit</th><th className="p-3 w-32 text-right">Credit</th><th className="p-3 w-10"></th></tr></thead>
                                    <tbody className="divide-y">{entryForm.lines.map((line, i) => (<tr key={i} className="hover:bg-slate-50"><td className="p-2 pl-4"><select required className="w-full p-2 border rounded bg-white focus:ring-2 focus:ring-indigo-500 outline-none" value={line.accountCode} onChange={e => handleEntryChange(i, 'accountCode', e.target.value)}><option value="">-- Select Account --</option>{accounts.filter(a => a.active).map(a => <option key={a.code} value={a.code}>{a.code} - {a.name}</option>)}</select></td><td className="p-2"><input type="number" min="0" step="0.01" className="w-full p-2 border rounded text-right focus:ring-2 focus:ring-indigo-500 outline-none" value={line.debit} onChange={e => handleEntryChange(i, 'debit', e.target.value)} /></td><td className="p-2"><input type="number" min="0" step="0.01" className="w-full p-2 border rounded text-right focus:ring-2 focus:ring-indigo-500 outline-none" value={line.credit} onChange={e => handleEntryChange(i, 'credit', e.target.value)} /></td><td className="p-2 text-center"><button type="button" onClick={() => removeEntryLine(i)} className="text-slate-400 hover:text-red-500 transition"><Trash2 size={16}/></button></td></tr>))}</tbody>
                                    <tfoot className="bg-slate-50 font-bold border-t"><tr><td className="p-3 text-right">Totals:</td><td className="p-3 text-right text-slate-800">{entryTotalDebit.toFixed(2)}</td><td className="p-3 text-right text-slate-800">{entryTotalCredit.toFixed(2)}</td><td></td></tr></tfoot>
                                </table>
                            </div>
                            <button type="button" onClick={addEntryLine} className="text-xs font-bold text-blue-600 hover:underline flex items-center gap-1"><Plus size={14}/> Add Line</button>
                            <div className="flex justify-end gap-3 pt-4 border-t border-slate-100">
                                <button type="button" onClick={() => setShowEntryModal(false)} className="px-4 py-2 rounded-lg text-slate-600 hover:bg-slate-100 font-bold text-sm transition">Cancel</button>
                                <button type="submit" className={`px-6 py-2 rounded-lg font-bold text-sm text-white transition shadow-lg ${Math.abs(entryTotalDebit - entryTotalCredit) > 0.01 || entryTotalDebit === 0 ? 'bg-slate-300 cursor-not-allowed' : 'bg-emerald-600 hover:bg-emerald-700 shadow-emerald-200'}`} disabled={Math.abs(entryTotalDebit - entryTotalCredit) > 0.01 || entryTotalDebit === 0}>Post Entry</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {showAddModal && (
                <div className="fixed inset-0 bg-slate-900/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm print:hidden">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in-95 duration-200">
                        <div className="p-4 border-b border-slate-100 flex justify-between items-center bg-slate-50">
                            <h3 className="font-bold text-slate-800">Add New Ledger Account</h3>
                            <button onClick={() => setShowAddModal(false)}><X size={20} className="text-slate-400 hover:text-red-500"/></button>
                        </div>
                        <form onSubmit={handleCreateAccount} className="p-6 space-y-4">
                            <div><label className="block text-xs font-bold text-slate-500 uppercase mb-1">Account Code</label><input type="text" required placeholder="e.g. 1100" className="w-full p-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none" value={newAccount.code} onChange={e => setNewAccount({...newAccount, code: e.target.value})} /></div>
                            <div><label className="block text-xs font-bold text-slate-500 uppercase mb-1">Account Name</label><input type="text" required placeholder="e.g. Inventory / Steel Doors" className="w-full p-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none" value={newAccount.name} onChange={e => setNewAccount({...newAccount, name: e.target.value})} /></div>
                            <div><label className="block text-xs font-bold text-slate-500 uppercase mb-1">Account Type</label><select className="w-full p-2 border border-slate-300 rounded-lg bg-white focus:ring-2 focus:ring-indigo-500 outline-none" value={newAccount.type} onChange={e => setNewAccount({...newAccount, type: e.target.value})}><option value="ASSET">Asset (Resources)</option><option value="LIABILITY">Liability (Debts)</option><option value="EQUITY">Equity (Capital)</option><option value="INCOME">Income (Revenue)</option><option value="EXPENSE">Expense (Costs)</option></select></div>
                            <button type="submit" className="w-full bg-emerald-600 text-white py-2 rounded-lg font-bold hover:bg-emerald-700 flex justify-center gap-2 transition"><Save size={18} /> Create Account</button>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}