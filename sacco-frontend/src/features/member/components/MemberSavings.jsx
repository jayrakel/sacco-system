import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { ArrowUpRight, ArrowDownLeft, Wallet, DollarSign } from 'lucide-react';
import MultiDepositForm from './MultiDepositForm';

export default function MemberSavings({ user }) {
    const [accounts, setAccounts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [action, setAction] = useState(null); // 'DEPOSIT' | 'WITHDRAW' | null
    const [activeTab, setActiveTab] = useState('accounts'); // 'accounts' | 'multi-deposit'

    // Form State
    const [selectedAccount, setSelectedAccount] = useState('');
    const [amount, setAmount] = useState('');
    const [description, setDescription] = useState('');

    useEffect(() => {
        fetchAccounts();
    }, []);

    const fetchAccounts = async () => {
        try {
            const res = await api.get('/api/savings/my-balance');
            if (res.data.success) {
                setAccounts(res.data.accounts);
            }
            setLoading(false);
        } catch (e) {
            console.error(e);
            setLoading(false);
        }
    };

    const handleTransaction = async (e) => {
        e.preventDefault();
        if (!selectedAccount || !amount) return;

        try {
            const endpoint = action === 'DEPOSIT' ? '/api/savings/deposit' : '/api/savings/withdraw';
            // Using FormData for simple request param handling as per controller
            const params = new URLSearchParams();
            params.append('accountNumber', selectedAccount);
            params.append('amount', amount);
            params.append('description', description || `${action} by Member`);

            await api.post(endpoint, params);
            alert(`${action} Successful!`);
            setAction(null);
            setAmount('');
            setDescription('');
            fetchAccounts(); // Refresh
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

            {/* Tab Content */}
            {activeTab === 'accounts' && (
                <>
                    <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
                        <Wallet className="text-emerald-600"/> My Savings Accounts
                    </h2>

            {/* Accounts List */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {accounts.map(acc => (
                    <div key={acc.id} className="bg-white p-5 rounded-xl shadow-sm border border-slate-200 relative overflow-hidden">
                        <div className="absolute top-0 right-0 p-2 bg-slate-50 rounded-bl-xl border-b border-l text-[10px] font-bold text-slate-500 uppercase">
                            {acc.status}
                        </div>
                        <p className="text-xs font-bold text-slate-400 uppercase mb-1">{acc.productName}</p>
                        <h3 className="text-2xl font-mono font-bold text-slate-800">KES {Number(acc.balance).toLocaleString()}</h3>
                        <p className="text-xs text-slate-500 mt-2 font-mono">Acc: {acc.accountNumber}</p>

                        <div className="flex gap-2 mt-4 pt-4 border-t border-slate-100">
                            <button onClick={() => { setAction('DEPOSIT'); setSelectedAccount(acc.accountNumber); }} className="flex-1 bg-emerald-50 text-emerald-700 text-xs font-bold py-2 rounded-lg hover:bg-emerald-100 flex items-center justify-center gap-1">
                                <ArrowDownLeft size={14}/> Deposit
                            </button>
                            <button onClick={() => { setAction('WITHDRAW'); setSelectedAccount(acc.accountNumber); }} className="flex-1 bg-amber-50 text-amber-700 text-xs font-bold py-2 rounded-lg hover:bg-amber-100 flex items-center justify-center gap-1">
                                <ArrowUpRight size={14}/> Withdraw
                            </button>
                        </div>
                    </div>
                ))}
            </div>

            {/* Transaction Modal */}
            {action && (
                <div className="fixed inset-0 bg-slate-900/50 flex items-center justify-center p-4 z-50 backdrop-blur-sm">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-sm p-6">
                        <h3 className="text-lg font-bold text-slate-800 mb-4">{action === 'DEPOSIT' ? 'Deposit Funds' : 'Withdraw Funds'}</h3>

                        <form onSubmit={handleTransaction} className="space-y-4">
                            <div>
                                <label className="block text-xs font-bold text-slate-500 mb-1">Account</label>
                                <select
                                    className="w-full p-2 border rounded-lg bg-slate-50"
                                    value={selectedAccount}
                                    onChange={e => setSelectedAccount(e.target.value)}
                                    disabled // Locked to the button clicked
                                >
                                    <option value={selectedAccount}>{selectedAccount}</option>
                                </select>
                            </div>

                            <div>
                                <label className="block text-xs font-bold text-slate-500 mb-1">Amount (KES)</label>
                                <input
                                    type="number"
                                    required
                                    min="1"
                                    className="w-full p-2 border rounded-lg"
                                    value={amount}
                                    onChange={e => setAmount(e.target.value)}
                                />
                            </div>

                            <div>
                                <label className="block text-xs font-bold text-slate-500 mb-1">Notes (Optional)</label>
                                <input
                                    type="text"
                                    className="w-full p-2 border rounded-lg"
                                    value={description}
                                    onChange={e => setDescription(e.target.value)}
                                />
                            </div>

                            <div className="flex gap-2 pt-2">
                                <button type="button" onClick={() => setAction(null)} className="flex-1 py-2 text-slate-500 font-bold hover:bg-slate-50 rounded-lg">Cancel</button>
                                <button type="submit" className="flex-1 py-2 bg-indigo-600 text-white font-bold rounded-lg hover:bg-indigo-700">Confirm</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
            </>
            )}

            {/* Multi-Deposit Tab */}
            {activeTab === 'multi-deposit' && (
                <MultiDepositForm user={user} />
            )}
        </div>
    );
}