import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { PiggyBank, Plus, Search, UserCheck } from 'lucide-react';

export default function SavingsManager() {
    const [accounts, setAccounts] = useState([]);
    const [members, setMembers] = useState([]);
    const [products, setProducts] = useState([]);
    const [showModal, setShowModal] = useState(false);
    const [formData, setFormData] = useState({ memberId: '', productId: '', initialDeposit: 0 });

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        try {
            const [accRes, memRes, prodRes] = await Promise.all([
                api.get('/api/savings'),
                api.get('/api/members/active'),
                api.get('/api/savings/products')
            ]);
            if (accRes.data.success) setAccounts(accRes.data.data);
            if (memRes.data.success) setMembers(memRes.data.data);
            if (prodRes.data.success) setProducts(prodRes.data.data);
        } catch (e) { console.error(e); }
    };

    const handleOpenAccount = async (e) => {
        e.preventDefault();
        const params = new URLSearchParams(formData);
        try {
            await api.post('/api/savings/open', null, { params });
            alert("Account Opened Successfully!");
            setShowModal(false);
            fetchData();
        } catch (e) { alert(e.response?.data?.message || "Failed to open account"); }
    };

    return (
        <div className="space-y-6 animate-in fade-in">
            {/* Header */}
            <div className="flex justify-between items-center bg-white p-4 rounded-xl shadow-sm border border-slate-200">
                <h2 className="font-bold text-slate-800 flex items-center gap-2"><PiggyBank className="text-emerald-600"/> Member Accounts</h2>
                <button onClick={() => setShowModal(true)} className="bg-emerald-600 text-white px-3 py-2 rounded-lg text-xs font-bold flex items-center gap-2 hover:bg-emerald-700">
                    <Plus size={14}/> Open New Account
                </button>
            </div>

            {/* Accounts Table */}
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                <table className="w-full text-sm text-left">
                    <thead className="bg-slate-50 text-slate-500 font-bold border-b">
                        <tr>
                            <th className="p-4">Account No</th>
                            <th className="p-4">Member</th>
                            <th className="p-4">Product</th>
                            <th className="p-4 text-right">Balance</th>
                            <th className="p-4">Maturity</th>
                            <th className="p-4">Status</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y">
                        {accounts.map(acc => (
                            <tr key={acc.id} className="hover:bg-slate-50">
                                <td className="p-4 font-mono text-slate-500">{acc.accountNumber}</td>
                                <td className="p-4 font-bold text-slate-700">{acc.memberName}</td>
                                <td className="p-4"><span className="bg-blue-50 text-blue-700 px-2 py-1 rounded text-xs font-bold">{acc.productName}</span></td>
                                <td className="p-4 text-right font-mono font-bold">KES {Number(acc.balance).toLocaleString()}</td>
                                <td className="p-4 text-xs text-slate-500">{acc.maturityDate ? new Date(acc.maturityDate).toLocaleDateString() : '-'}</td>
                                <td className="p-4"><span className="bg-green-100 text-green-700 px-2 py-1 rounded text-[10px] font-bold">{acc.status}</span></td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {/* Open Account Modal */}
            {showModal && (
                <div className="fixed inset-0 bg-slate-900/50 flex items-center justify-center p-4 z-50 backdrop-blur-sm">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
                        <h3 className="font-bold text-lg mb-4 text-slate-800">Open Savings Account</h3>
                        <form onSubmit={handleOpenAccount} className="space-y-4">
                            <div>
                                <label className="block text-xs font-bold text-slate-500 mb-1">Select Member</label>
                                <select required className="w-full p-2 border rounded" onChange={e => setFormData({...formData, memberId: e.target.value})}>
                                    <option value="">-- Select Member --</option>
                                    {members.map(m => <option key={m.id} value={m.id}>{m.firstName} {m.lastName} ({m.memberNumber})</option>)}
                                </select>
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-slate-500 mb-1">Select Product</label>
                                <select required className="w-full p-2 border rounded" onChange={e => setFormData({...formData, productId: e.target.value})}>
                                    <option value="">-- Select Product --</option>
                                    {products.map(p => <option key={p.id} value={p.id}>{p.name} ({p.interestRate}%)</option>)}
                                </select>
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-slate-500 mb-1">Initial Deposit (Optional)</label>
                                <input type="number" className="w-full p-2 border rounded" onChange={e => setFormData({...formData, initialDeposit: e.target.value})} />
                            </div>
                            <div className="flex justify-end gap-2 mt-4">
                                <button type="button" onClick={() => setShowModal(false)} className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded">Cancel</button>
                                <button type="submit" className="px-4 py-2 bg-emerald-600 text-white rounded font-bold hover:bg-emerald-700">Open Account</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}