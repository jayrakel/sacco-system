import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { BookOpen, Loader2, Plus, X, Calendar, Filter, RefreshCw, Trash2, Save } from 'lucide-react';

export default function AccountingReports() {
    const [accounts, setAccounts] = useState([]);
    const [journal, setJournal] = useState([]);
    const [view, setView] = useState('balance-sheet'); // Default View
    const [loading, setLoading] = useState(true);

    // Date Filters (Default: No Dates = Live Data)
    const [dateRange, setDateRange] = useState({
        startDate: '',
        endDate: ''
    });

    // Modals State
    const [showAddModal, setShowAddModal] = useState(false);
    const [showEntryModal, setShowEntryModal] = useState(false); // ✅ NEW: Manual Entry Modal

    // Forms
    const [newAccount, setNewAccount] = useState({ code: '', name: '', type: 'ASSET' });

    // ✅ NEW: Manual Entry Form State
    const [entryForm, setEntryForm] = useState({
        description: '',
        reference: '',
        date: new Date().toISOString().split('T')[0],
        lines: [
            { accountCode: '', debit: 0, credit: 0 },
            { accountCode: '', debit: 0, credit: 0 }
        ]
    });

    // Fetch data whenever dates change
    useEffect(() => {
        fetchData();
    }, [dateRange.endDate, dateRange.startDate]);

    const fetchData = async () => {
        setLoading(true);
        try {
            let accEndpoint = '/api/accounting/accounts'; // Default: Live Data

            // If user selects a date, switch to Historical Report API
            if (dateRange.endDate) {
                let query = `?endDate=${dateRange.endDate}`;
                if (dateRange.startDate) {
                    query += `&startDate=${dateRange.startDate}`;
                }
                accEndpoint = `/api/accounting/report${query}`;
            }

            const [accRes, jourRes] = await Promise.all([
                api.get(accEndpoint),
                api.get('/api/accounting/journal')
            ]);

            if(accRes.data.success) setAccounts(accRes.data.data);
            if(jourRes.data.success) setJournal(jourRes.data.data);
        } catch (error) {
            console.error("Error fetching accounting data", error);
        } finally {
            setLoading(false);
        }
    };

    const clearFilters = () => {
        setDateRange({ startDate: '', endDate: '' });
    };

    const toggleAccount = async (code) => {
        try {
            await api.put(`/api/accounting/accounts/${code}/toggle`);
            fetchData();
        } catch (e) {
            console.error("Failed to toggle account", e);
        }
    };

    const handleCreateAccount = async (e) => {
        e.preventDefault();
        try {
            await api.post('/api/accounting/accounts', newAccount);
            alert("Account Created Successfully!");
            setShowAddModal(false);
            setNewAccount({ code: '', name: '', type: 'ASSET' });
            fetchData();
        } catch (error) {
            alert(error.response?.data?.message || "Failed to create account. Code might exist.");
        }
    };

    // --- ✅ MANUAL JOURNAL ENTRY LOGIC ---

    const handleEntryChange = (index, field, value) => {
        const newLines = [...entryForm.lines];
        newLines[index][field] = value;

        // Auto-zero the opposite field if typing (Cannot debit and credit same line)
        if (field === 'debit' && value > 0) newLines[index].credit = 0;
        if (field === 'credit' && value > 0) newLines[index].debit = 0;

        setEntryForm({ ...entryForm, lines: newLines });
    };

    const addEntryLine = () => {
        setEntryForm({ ...entryForm, lines: [...entryForm.lines, { accountCode: '', debit: 0, credit: 0 }] });
    };

    const removeEntryLine = (index) => {
        if (entryForm.lines.length > 2) {
            const newLines = entryForm.lines.filter((_, i) => i !== index);
            setEntryForm({ ...entryForm, lines: newLines });
        }
    };

    const submitJournalEntry = async (e) => {
        e.preventDefault();

        // Validate Balance
        const totalDebit = entryForm.lines.reduce((sum, line) => sum + Number(line.debit), 0);
        const totalCredit = entryForm.lines.reduce((sum, line) => sum + Number(line.credit), 0);

        if (Math.abs(totalDebit - totalCredit) > 0.01) {
            alert(`Entry is Unbalanced!\nTotal Debit: ${totalDebit.toFixed(2)}\nTotal Credit: ${totalCredit.toFixed(2)}\nDifference: ${Math.abs(totalDebit - totalCredit).toFixed(2)}`);
            return;
        }

        if (totalDebit === 0) {
            alert("Entry cannot be zero.");
            return;
        }

        try {
            await api.post('/api/accounting/journal', entryForm);
            alert("Journal Entry Posted Successfully!");
            setShowEntryModal(false);
            // Reset Form
            setEntryForm({
                description: '',
                reference: '',
                date: new Date().toISOString().split('T')[0],
                lines: [{ accountCode: '', debit: 0, credit: 0 }, { accountCode: '', debit: 0, credit: 0 }]
            });
            fetchData();
        } catch (error) {
            alert(error.response?.data?.message || "Failed to post entry.");
        }
    };

    // --- FINANCIAL CALCULATIONS ---
    const getTotal = (type) => accounts
        .filter(a => a.type === type && a.active)
        .reduce((sum, a) => sum + parseFloat(a.balance), 0);

    const getNetIncome = () => getTotal('INCOME') - getTotal('EXPENSE');

    const formatMoney = (amount) => Number(amount).toLocaleString(undefined, {minimumFractionDigits: 2});

    // Helper for entry modal totals
    const entryTotalDebit = entryForm.lines.reduce((s, l) => s + Number(l.debit), 0);
    const entryTotalCredit = entryForm.lines.reduce((s, l) => s + Number(l.credit), 0);

    if (loading) return <div className="p-10 text-center text-slate-400"><Loader2 className="animate-spin inline mr-2"/> Loading Books...</div>;

    return (
        <div className="space-y-6 animate-in fade-in">

            {/* HEADER & CONTROLS */}
            <div className="flex flex-col xl:flex-row justify-between items-center bg-white p-4 rounded-xl shadow-sm border border-slate-200 gap-4">
                <h2 className="font-bold text-slate-800 flex items-center gap-2">
                    <BookOpen className="text-indigo-600" size={20} />
                    General Ledger
                </h2>

                <div className="flex flex-col md:flex-row gap-4 items-center w-full md:w-auto">

                    {/* Date Range Pickers */}
                    <div className="flex items-center gap-2 bg-slate-50 p-1.5 rounded-lg border border-slate-200">
                        <Filter size={16} className="text-slate-400 ml-2"/>

                        <span className="text-xs font-bold text-slate-500">From:</span>
                        <input
                            type="date"
                            className="bg-white border border-slate-300 text-slate-700 text-xs rounded px-2 py-1 outline-none focus:border-indigo-500"
                            value={dateRange.startDate}
                            onChange={(e) => setDateRange({...dateRange, startDate: e.target.value})}
                        />

                        <span className="text-xs font-bold text-slate-500">To:</span>
                        <input
                            type="date"
                            className="bg-white border border-slate-300 text-slate-700 text-xs rounded px-2 py-1 outline-none focus:border-indigo-500"
                            value={dateRange.endDate}
                            onChange={(e) => setDateRange({...dateRange, endDate: e.target.value})}
                        />

                        {(dateRange.startDate || dateRange.endDate) && (
                            <button onClick={clearFilters} className="ml-1 text-slate-400 hover:text-red-500" title="Clear Filters">
                                <X size={16} />
                            </button>
                        )}
                    </div>

                    {/* View Switcher */}
                    <div className="flex bg-slate-100 p-1 rounded-lg overflow-x-auto max-w-full">
                        {[
                            { id: 'balance-sheet', label: 'Balance Sheet' },
                            { id: 'income-statement', label: 'Income Statement' },
                            { id: 'accounts', label: 'Chart of Accounts' },
                            { id: 'journal', label: 'Journal Entries' }
                        ].map(tab => (
                            <button
                                key={tab.id}
                                onClick={() => setView(tab.id)}
                                className={`px-4 py-1.5 text-xs sm:text-sm font-bold rounded-md transition whitespace-nowrap ${view === tab.id ? 'bg-white shadow text-slate-800' : 'text-slate-500 hover:text-slate-700'}`}
                            >
                                {tab.label}
                            </button>
                        ))}
                    </div>

                    {/* ✅ NEW: Global Post Entry Button */}
                    <button
                        onClick={() => setShowEntryModal(true)}
                        className="bg-slate-900 text-white px-4 py-2 rounded-lg text-xs font-bold flex items-center gap-2 hover:bg-slate-800 transition whitespace-nowrap shadow-lg shadow-slate-900/20"
                    >
                        <Plus size={16} /> Post Entry
                    </button>
                </div>
            </div>

            {/* MESSAGE: FILTER STATUS */}
            {(dateRange.startDate || dateRange.endDate) ? (
                <div className="bg-indigo-50 text-indigo-700 px-4 py-2 rounded-lg text-sm border border-indigo-100 flex items-center gap-2">
                    <Filter size={16}/>
                    Viewing <strong>Historical Report</strong>: {dateRange.startDate || 'Start'} to {dateRange.endDate || 'Now'}
                </div>
            ) : (
                <div className="bg-emerald-50 text-emerald-700 px-4 py-2 rounded-lg text-sm border border-emerald-100 flex items-center gap-2">
                    <RefreshCw size={16}/>
                    Viewing <strong>Live Balances</strong> (Real-time System State)
                </div>
            )}

            {/* ✅ NEW: POST JOURNAL ENTRY MODAL */}
            {showEntryModal && (
                <div className="fixed inset-0 bg-slate-900/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-4xl overflow-hidden animate-in zoom-in-95 duration-200">

                        {/* Modal Header */}
                        <div className="p-5 border-b border-slate-100 flex justify-between items-center bg-slate-50">
                            <h3 className="font-bold text-slate-800 flex items-center gap-2">
                                <BookOpen size={20} className="text-slate-500"/> Post Manual Journal Entry
                            </h3>
                            <button onClick={() => setShowEntryModal(false)}><X size={20} className="text-slate-400 hover:text-red-500"/></button>
                        </div>

                        <form onSubmit={submitJournalEntry} className="p-6 space-y-6">

                            {/* Entry Details */}
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                                <div>
                                    <label className="block text-xs font-bold text-slate-500 uppercase mb-1">Date</label>
                                    <input type="date" required className="w-full p-2 border rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none" value={entryForm.date} onChange={e => setEntryForm({...entryForm, date: e.target.value})} />
                                </div>
                                <div>
                                    <label className="block text-xs font-bold text-slate-500 uppercase mb-1">Reference No</label>
                                    <input type="text" required placeholder="e.g. INV-001" className="w-full p-2 border rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none" value={entryForm.reference} onChange={e => setEntryForm({...entryForm, reference: e.target.value})} />
                                </div>
                                <div>
                                    <label className="block text-xs font-bold text-slate-500 uppercase mb-1">Description</label>
                                    <input type="text" required placeholder="e.g. Purchase of Office Desk" className="w-full p-2 border rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none" value={entryForm.description} onChange={e => setEntryForm({...entryForm, description: e.target.value})} />
                                </div>
                            </div>

                            {/* Lines Table */}
                            <div className="border rounded-xl overflow-hidden shadow-sm">
                                <table className="w-full text-sm text-left">
                                    <thead className="bg-slate-50 text-slate-500 font-bold border-b">
                                        <tr>
                                            <th className="p-3 pl-4">Account</th>
                                            <th className="p-3 w-32 text-right">Debit</th>
                                            <th className="p-3 w-32 text-right">Credit</th>
                                            <th className="p-3 w-10"></th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y">
                                        {entryForm.lines.map((line, i) => (
                                            <tr key={i} className="hover:bg-slate-50">
                                                <td className="p-2 pl-4">
                                                    <select required className="w-full p-2 border rounded bg-white focus:ring-2 focus:ring-indigo-500 outline-none" value={line.accountCode} onChange={e => handleEntryChange(i, 'accountCode', e.target.value)}>
                                                        <option value="">-- Select Account --</option>
                                                        {accounts.filter(a => a.active).map(a => <option key={a.code} value={a.code}>{a.code} - {a.name}</option>)}
                                                    </select>
                                                </td>
                                                <td className="p-2"><input type="number" min="0" step="0.01" className="w-full p-2 border rounded text-right focus:ring-2 focus:ring-indigo-500 outline-none" value={line.debit} onChange={e => handleEntryChange(i, 'debit', e.target.value)} /></td>
                                                <td className="p-2"><input type="number" min="0" step="0.01" className="w-full p-2 border rounded text-right focus:ring-2 focus:ring-indigo-500 outline-none" value={line.credit} onChange={e => handleEntryChange(i, 'credit', e.target.value)} /></td>
                                                <td className="p-2 text-center"><button type="button" onClick={() => removeEntryLine(i)} className="text-slate-400 hover:text-red-500 transition"><Trash2 size={16}/></button></td>
                                            </tr>
                                        ))}
                                    </tbody>
                                    <tfoot className="bg-slate-50 font-bold border-t">
                                        <tr>
                                            <td className="p-3 text-right">Totals:</td>
                                            <td className={`p-3 text-right ${Math.abs(entryTotalDebit - entryTotalCredit) > 0.01 ? 'text-red-600' : 'text-emerald-600'}`}>{entryTotalDebit.toFixed(2)}</td>
                                            <td className={`p-3 text-right ${Math.abs(entryTotalDebit - entryTotalCredit) > 0.01 ? 'text-red-600' : 'text-emerald-600'}`}>{entryTotalCredit.toFixed(2)}</td>
                                            <td></td>
                                        </tr>
                                    </tfoot>
                                </table>
                            </div>

                            <button type="button" onClick={addEntryLine} className="text-xs font-bold text-blue-600 hover:underline flex items-center gap-1">
                                <Plus size={14}/> Add Line
                            </button>

                            <div className="flex justify-end gap-3 pt-4 border-t border-slate-100">
                                <button type="button" onClick={() => setShowEntryModal(false)} className="px-4 py-2 rounded-lg text-slate-600 hover:bg-slate-100 font-bold text-sm transition">Cancel</button>
                                <button
                                    type="submit"
                                    className={`px-6 py-2 rounded-lg font-bold text-sm text-white transition shadow-lg ${
                                        Math.abs(entryTotalDebit - entryTotalCredit) > 0.01 || entryTotalDebit === 0
                                        ? 'bg-slate-300 cursor-not-allowed'
                                        : 'bg-emerald-600 hover:bg-emerald-700 shadow-emerald-200'
                                    }`}
                                    disabled={Math.abs(entryTotalDebit - entryTotalCredit) > 0.01 || entryTotalDebit === 0}
                                >
                                    Post Entry
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* ADD ACCOUNT MODAL (Separate from Entry Modal) */}
            {showAddModal && (
                <div className="fixed inset-0 bg-slate-900/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in-95 duration-200">
                        <div className="p-4 border-b border-slate-100 flex justify-between items-center bg-slate-50">
                            <h3 className="font-bold text-slate-800">Add New Ledger Account</h3>
                            <button onClick={() => setShowAddModal(false)}><X size={20} className="text-slate-400 hover:text-red-500"/></button>
                        </div>
                        <form onSubmit={handleCreateAccount} className="p-6 space-y-4">
                            <div>
                                <label className="block text-xs font-bold text-slate-500 uppercase mb-1">Account Code</label>
                                <input
                                    type="text" required placeholder="e.g. 1100"
                                    className="w-full p-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
                                    value={newAccount.code} onChange={e => setNewAccount({...newAccount, code: e.target.value})}
                                />
                                <p className="text-[10px] text-slate-400 mt-1">1xxx: Asset, 2xxx: Liability, 3xxx: Equity, 4xxx: Income, 5xxx: Expense</p>
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-slate-500 uppercase mb-1">Account Name</label>
                                <input
                                    type="text" required placeholder="e.g. Inventory / Steel Doors"
                                    className="w-full p-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
                                    value={newAccount.name} onChange={e => setNewAccount({...newAccount, name: e.target.value})}
                                />
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-slate-500 uppercase mb-1">Account Type</label>
                                <select
                                    className="w-full p-2 border border-slate-300 rounded-lg bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
                                    value={newAccount.type} onChange={e => setNewAccount({...newAccount, type: e.target.value})}
                                >
                                    <option value="ASSET">Asset (Resources)</option>
                                    <option value="LIABILITY">Liability (Debts)</option>
                                    <option value="EQUITY">Equity (Capital)</option>
                                    <option value="INCOME">Income (Revenue)</option>
                                    <option value="EXPENSE">Expense (Costs)</option>
                                </select>
                            </div>
                            <button type="submit" className="w-full bg-emerald-600 text-white py-2 rounded-lg font-bold hover:bg-emerald-700 flex justify-center gap-2 transition">
                                <Save size={18} /> Create Account
                            </button>
                        </form>
                    </div>
                </div>
            )}

            {/* --- VIEW 1: BALANCE SHEET --- */}
            {view === 'balance-sheet' && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {/* Assets Side */}
                    <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden h-fit">
                        <div className="bg-emerald-50 p-4 border-b border-emerald-100 flex justify-between items-center">
                            <h3 className="font-bold text-emerald-800">Assets</h3>
                            <span className="bg-white px-2 py-1 rounded text-emerald-700 text-xs font-bold">Total: KES {formatMoney(getTotal('ASSET'))}</span>
                        </div>
                        <table className="w-full text-sm">
                            <tbody className="divide-y divide-slate-50">
                                {accounts.filter(a => a.type === 'ASSET' && a.active).map(a => (
                                    <tr key={a.code} className="hover:bg-slate-50">
                                        <td className="p-4 text-slate-600">{a.name} <span className="text-xs text-slate-300">({a.code})</span></td>
                                        <td className="p-4 text-right font-mono font-bold text-slate-800">{formatMoney(a.balance)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    {/* Liabilities & Equity Side */}
                    <div className="space-y-6">
                        <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                            <div className="bg-amber-50 p-4 border-b border-amber-100 flex justify-between items-center">
                                <h3 className="font-bold text-amber-800">Liabilities</h3>
                                <span className="bg-white px-2 py-1 rounded text-amber-700 text-xs font-bold">Total: KES {formatMoney(getTotal('LIABILITY'))}</span>
                            </div>
                            <table className="w-full text-sm">
                                <tbody className="divide-y divide-slate-50">
                                    {accounts.filter(a => a.type === 'LIABILITY' && a.active).map(a => (
                                        <tr key={a.code} className="hover:bg-slate-50">
                                            <td className="p-4 text-slate-600">{a.name} <span className="text-xs text-slate-300">({a.code})</span></td>
                                            <td className="p-4 text-right font-mono font-bold text-slate-800">{formatMoney(a.balance)}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>

                        <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                            <div className="bg-blue-50 p-4 border-b border-blue-100 flex justify-between items-center">
                                <h3 className="font-bold text-blue-800">Equity</h3>
                                <span className="bg-white px-2 py-1 rounded text-blue-700 text-xs font-bold">
                                    Total: KES {formatMoney(getTotal('EQUITY') + getNetIncome())}
                                </span>
                            </div>
                            <table className="w-full text-sm">
                                <tbody className="divide-y divide-slate-50">
                                    {accounts.filter(a => a.type === 'EQUITY' && a.active).map(a => (
                                        <tr key={a.code} className="hover:bg-slate-50">
                                            <td className="p-4 text-slate-600">{a.name} <span className="text-xs text-slate-300">({a.code})</span></td>
                                            <td className="p-4 text-right font-mono font-bold text-slate-800">{formatMoney(a.balance)}</td>
                                        </tr>
                                    ))}
                                    <tr className="bg-blue-50/50">
                                        <td className="p-4 text-slate-700 italic font-medium">Net Income (Current Period)</td>
                                        <td className={`p-4 text-right font-mono font-bold ${getNetIncome() >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
                                            {formatMoney(getNetIncome())}
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>

                        <div className={`p-4 rounded-xl border flex justify-between items-center ${
                            Math.abs(getTotal('ASSET') - (getTotal('LIABILITY') + getTotal('EQUITY') + getNetIncome())) < 1
                            ? 'bg-green-50 border-green-200 text-green-800'
                            : 'bg-red-50 border-red-200 text-red-800'
                        }`}>
                            <span className="font-bold text-sm uppercase">Accounting Equation Check</span>
                            <span className="font-mono font-bold">
                                {Math.abs(getTotal('ASSET') - (getTotal('LIABILITY') + getTotal('EQUITY') + getNetIncome())) < 1
                                ? "BALANCED ✅"
                                : "UNBALANCED ⚠️"}
                            </span>
                        </div>
                    </div>
                </div>
            )}

            {/* --- VIEW 2: INCOME STATEMENT --- */}
            {view === 'income-statement' && (
                <div className="max-w-3xl mx-auto bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                    <div className="p-6 border-b border-slate-100 text-center">
                        <h3 className="text-xl font-bold text-slate-800">Income Statement</h3>
                        <p className="text-slate-400 text-sm">Profit & Loss Overview</p>

                        {!dateRange.startDate && !dateRange.endDate && (
                            <p className="mt-2 text-xs text-blue-600 bg-blue-50 inline-block px-3 py-1 rounded-full">
                                ℹ️ Showing all-time data. Set "From/To" dates to filter.
                            </p>
                        )}
                    </div>

                    <div className="p-6 space-y-6">
                        <div>
                            <h4 className="text-sm font-bold text-slate-500 uppercase mb-2 border-b pb-1">Operating Income</h4>
                            {accounts.filter(a => a.type === 'INCOME' && a.active).map(a => (
                                <div key={a.code} className="flex justify-between py-2 text-sm">
                                    <span className="text-slate-700">{a.name}</span>
                                    <span className="font-mono font-medium">{formatMoney(a.balance)}</span>
                                </div>
                            ))}
                            <div className="flex justify-between py-2 text-sm font-bold bg-slate-50 px-2 rounded mt-2 border-t border-slate-200">
                                <span>Total Income</span>
                                <span className="text-emerald-600">{formatMoney(getTotal('INCOME'))}</span>
                            </div>
                        </div>

                        <div>
                            <h4 className="text-sm font-bold text-slate-500 uppercase mb-2 border-b pb-1">Operating Expenses</h4>
                            {accounts.filter(a => a.type === 'EXPENSE' && a.active).length === 0 ? (
                                <p className="text-xs text-slate-400 italic py-2">No expenses recorded.</p>
                            ) : (
                                accounts.filter(a => a.type === 'EXPENSE' && a.active).map(a => (
                                    <div key={a.code} className="flex justify-between py-2 text-sm">
                                        <span className="text-slate-700">{a.name}</span>
                                        <span className="font-mono font-medium">{formatMoney(a.balance)}</span>
                                    </div>
                                ))
                            )}
                            <div className="flex justify-between py-2 text-sm font-bold bg-slate-50 px-2 rounded mt-2 border-t border-slate-200">
                                <span>Total Expenses</span>
                                <span className="text-red-600">{formatMoney(getTotal('EXPENSE'))}</span>
                            </div>
                        </div>

                        <div className="flex justify-between items-center py-4 border-t-2 border-slate-900 mt-4">
                            <span className="text-lg font-extrabold text-slate-900">NET INCOME</span>
                            <span className={`text-xl font-mono font-extrabold ${getTotal('INCOME') - getTotal('EXPENSE') >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
                                {formatMoney(getTotal('INCOME') - getTotal('EXPENSE'))}
                            </span>
                        </div>
                    </div>
                </div>
            )}

            {/* --- VIEW 3: CHART OF ACCOUNTS (Editable) --- */}
            {view === 'accounts' && (
                <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                    {/* ✅ HEADER INSIDE CARD WITH ADD BUTTON */}
                    <div className="p-5 border-b border-slate-100 flex justify-between items-center bg-slate-50/50">
                        <div>
                            <h3 className="font-bold text-slate-800">Chart of Accounts</h3>
                            <p className="text-xs text-slate-500">Manage your system's financial buckets.</p>
                        </div>
                        <button
                            onClick={() => setShowAddModal(true)}
                            className="bg-emerald-600 text-white px-4 py-2 rounded-lg text-xs font-bold flex items-center gap-2 hover:bg-emerald-700 transition shadow-lg shadow-emerald-600/20"
                        >
                            <Plus size={16} /> Add Account
                        </button>
                    </div>

                    <table className="w-full text-sm text-left">
                        <thead className="bg-slate-50 text-slate-500 font-bold border-b border-slate-100">
                            <tr>
                                <th className="p-4">Code</th>
                                <th className="p-4">Account Name</th>
                                <th className="p-4">Type</th>
                                <th className="p-4 text-right">Balance (KES)</th>
                                <th className="p-4 text-center">Action</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {accounts.map(acc => (
                                <tr key={acc.code} className={`hover:bg-slate-50 ${!acc.active ? 'opacity-50 bg-slate-100' : ''}`}>
                                    <td className="p-4 font-mono text-slate-500">{acc.code}</td>
                                    <td className="p-4 font-bold text-slate-700">
                                        {acc.name}
                                        {!acc.active && <span className="ml-2 text-[10px] bg-red-100 text-red-600 px-1 rounded border border-red-200">DISABLED</span>}
                                    </td>
                                    <td className="p-4">
                                        <span className={`px-2 py-1 rounded text-xs font-bold ${
                                            acc.type === 'ASSET' ? 'bg-emerald-100 text-emerald-700' :
                                            acc.type === 'LIABILITY' ? 'bg-amber-100 text-amber-700' :
                                            acc.type === 'INCOME' ? 'bg-blue-100 text-blue-700' :
                                            'bg-slate-100 text-slate-700'
                                        }`}>{acc.type}</span>
                                    </td>
                                    <td className="p-4 text-right font-mono font-bold">
                                        {formatMoney(acc.balance)}
                                    </td>
                                    <td className="p-4 text-center">
                                        <button
                                            onClick={() => toggleAccount(acc.code)}
                                            className={`text-xs font-bold px-3 py-1 rounded border transition ${
                                                acc.active
                                                ? 'text-red-600 border-red-200 hover:bg-red-50'
                                                : 'text-emerald-600 border-emerald-200 hover:bg-emerald-50'
                                            }`}
                                        >
                                            {acc.active ? 'Disable' : 'Enable'}
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {/* --- VIEW 4: JOURNAL ENTRIES --- */}
            {view === 'journal' && (
                <div className="space-y-4">
                    {journal.length === 0 ? <p className="text-center text-slate-400 py-10 italic">No journal entries found.</p> : journal.map(entry => (
                        <div key={entry.id} className="bg-white rounded-xl shadow-sm border border-slate-200 p-4 hover:shadow-md transition">
                            <div className="flex justify-between items-start mb-3 border-b border-slate-50 pb-2">
                                <div>
                                    <p className="font-bold text-slate-800">{entry.description}</p>
                                    <p className="text-xs text-slate-400 font-mono">Ref: {entry.referenceNo}</p>
                                </div>
                                <span className="text-xs text-slate-500 bg-slate-100 px-2 py-1 rounded font-medium">
                                    {new Date(entry.transactionDate).toLocaleString()}
                                </span>
                            </div>
                            <div className="space-y-1">
                                {entry.lines.map(line => (
                                    <div key={line.id} className="flex justify-between text-sm">
                                        <span className={`${line.debit > 0 ? 'text-slate-700 font-medium' : 'pl-8 text-slate-500'}`}>
                                            {line.account.name} <span className="text-xs text-slate-300">({line.account.code})</span>
                                        </span>
                                        <div className="flex gap-4 font-mono text-xs">
                                            <span className="w-24 text-right text-emerald-600 font-bold">
                                                {line.debit > 0 ? formatMoney(line.debit) : '-'}
                                            </span>
                                            <span className="w-24 text-right text-slate-600 font-bold">
                                                {line.credit > 0 ? formatMoney(line.credit) : '-'}
                                            </span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}