import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { Wallet, DollarSign, TrendingUp, History } from 'lucide-react';
import MultiDepositForm from './MultiDepositForm';

export default function MemberSavings({ user }) {
    const [accounts, setAccounts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [action, setAction] = useState(null); // 'DEPOSIT' | 'WITHDRAW'
    const [activeTab, setActiveTab] = useState('accounts');

    // Form State
    const [selectedAccount, setSelectedAccount] = useState('');
    const [amount, setAmount] = useState('');
    const [description, setDescription] = useState('');

    useEffect(() => {
        fetchAccounts();
    }, []);

    const fetchAccounts = async () => {
        try {
            // ✅ Correct Path: /api/savings/my-balance
            const res = await api.get('/api/savings/my-balance');
            if (res.data.success) {
                setAccounts(res.data.accounts || []);
            }
        } catch (e) {
            console.error("Failed to load savings:", e);
        } finally {
            setLoading(false);
        }
    };

    const handleTransaction = async (e) => {
        e.preventDefault();
        if (!selectedAccount || !amount) return;

        try {
            const endpoint = action === 'DEPOSIT' ? '/api/savings/deposit' : '/api/savings/withdraw';

            // ✅ Backend expects @RequestParam, so we use FormData/URLSearchParams
            const params = new URLSearchParams();
            params.append('accountNumber', selectedAccount);
            params.append('amount', amount);
            params.append('description', description || `${action} by Member`);

            await api.post(endpoint, params);

            alert(`${action} Successful!`);

            // Reset and Refresh
            setAction(null);
            setAmount('');
            setDescription('');
            fetchAccounts();
        } catch (e) {
            alert(e.response?.data?.message || "Transaction Failed");
        }
    };

    if (loading) return <div className="p-8 text-center text-slate-400">Loading Accounts...</div>;

    return (
        <div className="space-y-6 animate-in fade-in">
            {/* Tabs */}
            <div className="flex gap-3 border-b border-slate-200 pb-2">
                <button
                    onClick={() => setActiveTab('accounts')}
                    className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition ${
                        activeTab === 'accounts'
                            ? 'bg-emerald-100 text-emerald-700'
                            : 'text-slate-500 hover:bg-slate-50'
                    }`}
                >
                    <Wallet size={18} /> My Accounts
                </button>
                <button
                    onClick={() => setActiveTab('multi-deposit')}
                    className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition ${
                        activeTab === 'multi-deposit'
                            ? 'bg-indigo-100 text-indigo-700'
                            : 'text-slate-500 hover:bg-slate-50'
                    }`}
                >
                    <DollarSign size={18} /> Make Deposit
                </button>
            </div>

            {/* Content */}
            {activeTab === 'accounts' ? (
                <>
                    <div className="flex justify-between items-center">
                        <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
                            <Wallet className="text-emerald-600"/> Portfolio Overview
                        </h2>
                    </div>

                    {/* Accounts Grid */}
                    {accounts.length === 0 ? (
                        <div className="p-10 text-center bg-slate-50 rounded-xl border border-dashed border-slate-300">
                            <p className="text-slate-500">No savings accounts found.</p>
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            {accounts.map(acc => (
                                <div key={acc.id} className="bg-white p-6 rounded-xl shadow-sm border border-slate-200 relative overflow-hidden group hover:shadow-md transition">
                                    {/* Status Badge */}
                                    <div className={`absolute top-0 right-0 px-3 py-1 rounded-bl-xl text-[10px] font-bold uppercase tracking-wider
                                        ${acc.accountStatus === 'ACTIVE' ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-50 text-slate-500'}`}>
                                        {acc.accountStatus} {/* ✅ FIX: Mapped to correct DTO field */}
                                    </div>

                                    <div className="flex flex-col gap-2">
                                        <span className="text-xs font-bold text-slate-400 uppercase tracking-wide">{acc.productName}</span>

                                        <div className="flex items-baseline gap-1">
                                            <span className="text-xs text-slate-400 font-bold">KES</span>
                                            {/* ✅ FIX: Mapped to balanceAmount */}
                                            <h3 className="text-3xl font-mono font-bold text-slate-800">
                                                {Number(acc.balanceAmount || 0).toLocaleString()}
                                            </h3>
                                        </div>

                                        <p className="text-xs text-slate-500 font-mono tracking-wider">**** {acc.accountNumber.slice(-4)}</p>
                                    </div>

                                    {/* Quick Actions */}
                                    <div className="mt-6 flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                        <button
                                            onClick={() => { setSelectedAccount(acc.accountNumber); setAction('DEPOSIT'); }}
                                            className="flex-1 bg-emerald-600 text-white text-xs font-bold py-2 rounded-lg hover:bg-emerald-700"
                                        >
                                            Deposit
                                        </button>
                                        {/* Withdraw is usually restricted, but adding conditionally */}
                                        <button
                                            onClick={() => { setSelectedAccount(acc.accountNumber); setAction('WITHDRAW'); }}
                                            className="flex-1 bg-white border border-slate-200 text-slate-600 text-xs font-bold py-2 rounded-lg hover:bg-slate-50"
                                        >
                                            Withdraw
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}

                    {/* Transaction Modal */}
                    {action && (
                        <div className="fixed inset-0 bg-slate-900/50 flex items-center justify-center p-4 z-50 backdrop-blur-sm animate-in fade-in">
                            <div className="bg-white rounded-xl shadow-xl w-full max-w-sm p-6 relative">
                                <h3 className="text-lg font-bold text-slate-800 mb-4">
                                    {action === 'DEPOSIT' ? 'Deposit Funds' : 'Withdraw Funds'}
                                </h3>

                                <form onSubmit={handleTransaction} className="space-y-4">
                                    <div>
                                        <label className="block text-xs font-bold text-slate-500 mb-1">Account</label>
                                        <input
                                            type="text"
                                            className="w-full p-3 border border-slate-200 rounded-xl bg-slate-50 text-slate-600 font-mono text-sm"
                                            value={selectedAccount}
                                            readOnly
                                        />
                                    </div>

                                    <div>
                                        <label className="block text-xs font-bold text-slate-500 mb-1">Amount (KES)</label>
                                        <input
                                            type="number"
                                            required
                                            min="1"
                                            className="w-full p-3 border border-slate-200 rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none font-bold text-lg"
                                            value={amount}
                                            onChange={e => setAmount(e.target.value)}
                                            placeholder="0.00"
                                        />
                                    </div>

                                    <div>
                                        <label className="block text-xs font-bold text-slate-500 mb-1">Notes (Optional)</label>
                                        <input
                                            type="text"
                                            className="w-full p-3 border border-slate-200 rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none text-sm"
                                            value={description}
                                            onChange={e => setDescription(e.target.value)}
                                            placeholder="e.g., Monthly Savings"
                                        />
                                    </div>

                                    <div className="flex gap-3 pt-2">
                                        <button type="button" onClick={() => setAction(null)} className="flex-1 py-3 text-slate-500 font-bold hover:bg-slate-50 rounded-xl transition">Cancel</button>
                                        <button type="submit" className="flex-1 py-3 bg-indigo-600 text-white font-bold rounded-xl hover:bg-indigo-700 transition">
                                            Confirm
                                        </button>
                                    </div>
                                </form>
                            </div>
                        </div>
                    )}
                </>
            ) : (
                /* Multi-Deposit Tab */
                <MultiDepositForm user={user} />
            )}
        </div>
    );
}