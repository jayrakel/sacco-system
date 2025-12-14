import React, { useState, useEffect } from 'react';
import api from '../api';
import { X, Save, ArrowDownLeft, ArrowUpRight, Repeat, Loader2 } from 'lucide-react';

export default function TransactionModal({ isOpen, onClose, onSuccess }) {
    const [members, setMembers] = useState([]);
    const [loading, setLoading] = useState(false);
    const [submitting, setSubmitting] = useState(false);

    // Form State
    const [formData, setFormData] = useState({
        type: 'DEPOSIT', // DEPOSIT, WITHDRAWAL, TRANSFER
        memberId: '',
        accountNumber: '',
        targetMemberId: '', // For Transfer
        targetAccountNumber: '', // For Transfer
        amount: '',
        description: '',
        paymentMethod: 'CASH',
        referenceCode: ''
    });

    useEffect(() => {
        if (isOpen) {
            setLoading(true);
            api.get('/api/members/active')
                .then(res => { if(res.data.success) setMembers(res.data.data); })
                .catch(console.error)
                .finally(() => setLoading(false));
        }
    }, [isOpen]);

    // Fetch account for a specific member
    const fetchAccount = async (memId, isTarget = false) => {
        try {
            const res = await api.get(`/api/savings/member/${memId}`);
            if (res.data.success && res.data.data.length > 0) {
                const accNum = res.data.data[0].accountNumber;
                setFormData(prev => isTarget
                    ? { ...prev, targetMemberId: memId, targetAccountNumber: accNum }
                    : { ...prev, memberId: memId, accountNumber: accNum }
                );
            }
        } catch (error) { console.error("Could not fetch account", error); }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSubmitting(true);

        try {
            const params = new URLSearchParams();
            params.append('amount', formData.amount);
            params.append('description', formData.description);

            let endpoint = '';

            if (formData.type === 'TRANSFER') {
                endpoint = '/api/transactions/transfer';
                params.append('fromAccount', formData.accountNumber);
                params.append('toAccount', formData.targetAccountNumber);
            } else {
                endpoint = formData.type === 'DEPOSIT' ? '/api/savings/deposit' : '/api/savings/withdraw';
                params.append('accountNumber', formData.accountNumber);
            }

            await api.post(endpoint, null, { params });

            alert("Transaction Successful!");
            onSuccess();
            onClose();
            setFormData({ type: 'DEPOSIT', memberId: '', accountNumber: '', targetMemberId: '', targetAccountNumber: '', amount: '', description: '', paymentMethod: 'CASH', referenceCode: '' });

        } catch (error) {
            alert(error.response?.data?.message || "Transaction Failed");
        } finally {
            setSubmitting(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
            <div className="bg-white rounded-2xl shadow-xl w-full max-w-lg overflow-hidden animate-in fade-in zoom-in-95 duration-200">

                {/* Header */}
                <div className="px-6 py-4 border-b border-slate-100 flex justify-between items-center bg-slate-50">
                    <h3 className="font-bold text-slate-800 flex items-center gap-2">
                        {formData.type === 'DEPOSIT' && <ArrowDownLeft className="text-emerald-600" />}
                        {formData.type === 'WITHDRAWAL' && <ArrowUpRight className="text-red-600" />}
                        {formData.type === 'TRANSFER' && <Repeat className="text-blue-600" />}
                        Record Transaction
                    </h3>
                    <button onClick={onClose} className="text-slate-400 hover:text-slate-600 transition"><X size={20} /></button>
                </div>

                <form onSubmit={handleSubmit} className="p-6 space-y-4">

                    {/* Transaction Type Toggle */}
                    <div className="flex bg-slate-100 p-1 rounded-lg">
                        {['DEPOSIT', 'WITHDRAWAL', 'TRANSFER'].map(type => (
                            <button
                                key={type}
                                type="button"
                                onClick={() => setFormData({ ...formData, type })}
                                className={`flex-1 py-2 text-xs font-bold rounded-md transition uppercase ${formData.type === type ? 'bg-white shadow-sm text-slate-800' : 'text-slate-500 hover:text-slate-700'}`}
                            >
                                {type}
                            </button>
                        ))}
                    </div>

                    {/* Source Member */}
                    <div>
                        <label className="block text-xs font-bold text-slate-500 uppercase mb-1">
                            {formData.type === 'TRANSFER' ? 'From Member' : 'Select Member'}
                        </label>
                        <select
                            required
                            value={formData.memberId}
                            onChange={(e) => fetchAccount(e.target.value, false)}
                            className="w-full p-3 border border-slate-200 rounded-xl bg-slate-50 text-sm focus:outline-none focus:ring-2 focus:ring-slate-900 transition"
                        >
                            <option value="">-- Select Member --</option>
                            {members.map(m => (
                                <option key={m.id} value={m.id}>{m.firstName} {m.lastName} - {m.memberNumber}</option>
                            ))}
                        </select>
                        {formData.accountNumber && <p className="text-[10px] text-blue-600 mt-1 font-mono">Acc: {formData.accountNumber}</p>}
                    </div>

                    {/* Target Member (Only for Transfer) */}
                    {formData.type === 'TRANSFER' && (
                        <div>
                            <label className="block text-xs font-bold text-slate-500 uppercase mb-1">To Member</label>
                            <select
                                required
                                value={formData.targetMemberId}
                                onChange={(e) => fetchAccount(e.target.value, true)}
                                className="w-full p-3 border border-slate-200 rounded-xl bg-slate-50 text-sm focus:outline-none focus:ring-2 focus:ring-slate-900 transition"
                            >
                                <option value="">-- Select Receiver --</option>
                                {members.filter(m => m.id !== formData.memberId).map(m => (
                                    <option key={m.id} value={m.id}>{m.firstName} {m.lastName} - {m.memberNumber}</option>
                                ))}
                            </select>
                            {formData.targetAccountNumber && <p className="text-[10px] text-blue-600 mt-1 font-mono">Acc: {formData.targetAccountNumber}</p>}
                        </div>
                    )}

                    {/* Amount */}
                    <div>
                        <label className="block text-xs font-bold text-slate-500 uppercase mb-1">Amount (KES)</label>
                        <input
                            type="number" required min="1"
                            value={formData.amount}
                            onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
                            className="w-full p-3 border border-slate-200 rounded-xl font-bold text-lg text-slate-800 focus:outline-none focus:ring-2 focus:ring-slate-900 transition"
                            placeholder="0.00"
                        />
                    </div>

                    {/* Description */}
                    <div>
                        <label className="block text-xs font-bold text-slate-500 uppercase mb-1">Description / Ref</label>
                        <input
                            type="text"
                            value={formData.description}
                            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                            className="w-full p-3 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-slate-900 transition"
                            placeholder="e.g. M-Pesa Ref..."
                        />
                    </div>

                    <button
                        type="submit"
                        disabled={submitting || !formData.accountNumber || (formData.type === 'TRANSFER' && !formData.targetAccountNumber)}
                        className={`w-full py-3 rounded-xl font-bold text-white flex items-center justify-center gap-2 transition shadow-lg ${
                            formData.type === 'DEPOSIT' ? 'bg-emerald-600 hover:bg-emerald-700 shadow-emerald-200' :
                            formData.type === 'WITHDRAWAL' ? 'bg-red-600 hover:bg-red-700 shadow-red-200' :
                            'bg-blue-600 hover:bg-blue-700 shadow-blue-200'
                        } disabled:opacity-50 disabled:cursor-not-allowed`}
                    >
                        {submitting ? <Loader2 className="animate-spin" /> : <Save size={18} />}
                        {submitting ? 'Processing...' : `Confirm ${formData.type}`}
                    </button>

                </form>
            </div>
        </div>
    );
}