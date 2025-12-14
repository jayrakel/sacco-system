import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { BookOpen, FileText, ArrowRightLeft, Loader2, PieChart, TrendingUp } from 'lucide-react';

export default function AccountingReports() {
    const [accounts, setAccounts] = useState([]);
    const [journal, setJournal] = useState([]);
    const [view, setView] = useState('balance-sheet'); // Default to Balance Sheet
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        setLoading(true);
        try {
            const [accRes, jourRes] = await Promise.all([
                api.get('/api/accounting/accounts'),
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

    // --- HELPER FUNCTIONS FOR REPORTS ---
    const getTotal = (type) => accounts
        .filter(a => a.type === type)
        .reduce((sum, a) => sum + parseFloat(a.balance), 0);

    const formatMoney = (amount) => Number(amount).toLocaleString(undefined, {minimumFractionDigits: 2});

    if (loading) return <div className="p-10 text-center text-slate-400"><Loader2 className="animate-spin inline mr-2"/> Loading Books...</div>;

    return (
        <div className="space-y-6 animate-in fade-in">

            {/* Header / Toggle Navigation */}
            <div className="flex flex-col md:flex-row justify-between items-center bg-white p-4 rounded-xl shadow-sm border border-slate-200 gap-4">
                <h2 className="font-bold text-slate-800 flex items-center gap-2">
                    <BookOpen className="text-indigo-600" size={20} />
                    General Ledger
                </h2>
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
                            className={`px-4 py-1.5 text-sm font-bold rounded-md transition whitespace-nowrap ${view === tab.id ? 'bg-white shadow text-slate-800' : 'text-slate-500 hover:text-slate-700'}`}
                        >
                            {tab.label}
                        </button>
                    ))}
                </div>
            </div>

            {/* --- VIEW: BALANCE SHEET --- */}
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
                                {accounts.filter(a => a.type === 'ASSET').map(a => (
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
                        {/* Liabilities */}
                        <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                            <div className="bg-amber-50 p-4 border-b border-amber-100 flex justify-between items-center">
                                <h3 className="font-bold text-amber-800">Liabilities</h3>
                                <span className="bg-white px-2 py-1 rounded text-amber-700 text-xs font-bold">Total: KES {formatMoney(getTotal('LIABILITY'))}</span>
                            </div>
                            <table className="w-full text-sm">
                                <tbody className="divide-y divide-slate-50">
                                    {accounts.filter(a => a.type === 'LIABILITY').map(a => (
                                        <tr key={a.code} className="hover:bg-slate-50">
                                            <td className="p-4 text-slate-600">{a.name} <span className="text-xs text-slate-300">({a.code})</span></td>
                                            <td className="p-4 text-right font-mono font-bold text-slate-800">{formatMoney(a.balance)}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>

                        {/* Equity */}
                        <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                            <div className="bg-blue-50 p-4 border-b border-blue-100 flex justify-between items-center">
                                <h3 className="font-bold text-blue-800">Equity</h3>
                                <span className="bg-white px-2 py-1 rounded text-blue-700 text-xs font-bold">Total: KES {formatMoney(getTotal('EQUITY'))}</span>
                            </div>
                            <table className="w-full text-sm">
                                <tbody className="divide-y divide-slate-50">
                                    {accounts.filter(a => a.type === 'EQUITY').map(a => (
                                        <tr key={a.code} className="hover:bg-slate-50">
                                            <td className="p-4 text-slate-600">{a.name} <span className="text-xs text-slate-300">({a.code})</span></td>
                                            <td className="p-4 text-right font-mono font-bold text-slate-800">{formatMoney(a.balance)}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>

                        {/* Equation Check */}
                        <div className={`p-4 rounded-xl border flex justify-between items-center ${
                            Math.abs(getTotal('ASSET') - (getTotal('LIABILITY') + getTotal('EQUITY'))) < 1
                            ? 'bg-green-50 border-green-200 text-green-800'
                            : 'bg-red-50 border-red-200 text-red-800'
                        }`}>
                            <span className="font-bold text-sm uppercase">Balance Check</span>
                            <span className="font-mono font-bold">
                                {Math.abs(getTotal('ASSET') - (getTotal('LIABILITY') + getTotal('EQUITY'))) < 1
                                ? "BALANCED ✅"
                                : "UNBALANCED ⚠️"}
                            </span>
                        </div>
                    </div>
                </div>
            )}

            {/* --- VIEW: INCOME STATEMENT --- */}
            {view === 'income-statement' && (
                <div className="max-w-3xl mx-auto bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                    <div className="p-6 border-b border-slate-100 text-center">
                        <h3 className="text-xl font-bold text-slate-800">Income Statement</h3>
                        <p className="text-slate-400 text-sm">Profit & Loss Overview</p>
                    </div>

                    <div className="p-6 space-y-6">
                        {/* Income Section */}
                        <div>
                            <h4 className="text-sm font-bold text-slate-500 uppercase mb-2 border-b pb-1">Operating Income</h4>
                            {accounts.filter(a => a.type === 'INCOME').map(a => (
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

                        {/* Expense Section */}
                        <div>
                            <h4 className="text-sm font-bold text-slate-500 uppercase mb-2 border-b pb-1">Operating Expenses</h4>
                            {accounts.filter(a => a.type === 'EXPENSE').length === 0 ? (
                                <p className="text-xs text-slate-400 italic py-2">No expenses recorded.</p>
                            ) : (
                                accounts.filter(a => a.type === 'EXPENSE').map(a => (
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

                        {/* Net Income */}
                        <div className="flex justify-between items-center py-4 border-t-2 border-slate-900 mt-4">
                            <span className="text-lg font-extrabold text-slate-900">NET INCOME</span>
                            <span className={`text-xl font-mono font-extrabold ${getTotal('INCOME') - getTotal('EXPENSE') >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
                                {formatMoney(getTotal('INCOME') - getTotal('EXPENSE'))}
                            </span>
                        </div>
                    </div>
                </div>
            )}

            {/* --- VIEW: CHART OF ACCOUNTS (Raw Data) --- */}
            {view === 'accounts' && (
                <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                    <table className="w-full text-sm text-left">
                        <thead className="bg-slate-50 text-slate-500 font-bold border-b border-slate-100">
                            <tr>
                                <th className="p-4">Code</th>
                                <th className="p-4">Account Name</th>
                                <th className="p-4">Type</th>
                                <th className="p-4 text-right">Balance (KES)</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {accounts.map(acc => (
                                <tr key={acc.code} className="hover:bg-slate-50">
                                    <td className="p-4 font-mono text-slate-500">{acc.code}</td>
                                    <td className="p-4 font-bold text-slate-700">{acc.name}</td>
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
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {/* --- VIEW: JOURNAL ENTRIES --- */}
            {view === 'journal' && (
                <div className="space-y-4">
                    {journal.map(entry => (
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