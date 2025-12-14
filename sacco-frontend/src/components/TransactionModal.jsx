import React, { useState, useEffect } from 'react';
import api from '../api';
import { X, Save, ArrowDownLeft, ArrowUpRight, Search, Loader2 } from 'lucide-react';

export default function TransactionModal({ isOpen, onClose, onSuccess }) {
    const [members, setMembers] = useState([]);
    const [loading, setLoading] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');

    // Form State
    const [formData, setFormData] = useState({
        memberId: '',
        accountNumber: '',
        type: 'DEPOSIT', // DEPOSIT or WITHDRAWAL
        amount: '',
        description: '',
        paymentMethod: 'CASH',
        referenceCode: ''
    });

    // Fetch members when modal opens
    useEffect(() => {
        if (isOpen) {
            setLoading(true);
            api.get('/api/members/active')
                .then(res => {
                    if(res.data.success) setMembers(res.data.data);
                })
                .catch(console.error)
                .finally(() => setLoading(false));
        }
    }, [isOpen]);

    // Handle Member Selection to auto-fill Account Number
    const handleMemberSelect = async (e) => {
        const memberId = e.target.value;
        setFormData({ ...formData, memberId, accountNumber: '' }); // Reset account

        if (!memberId) return;

        try {
            // Fetch the member's savings account
            const res = await api.get(`/api/savings/member/${memberId}`);
            if (res.data.success && res.data.data.length > 0) {
                // Assuming first account is the main savings
                setFormData(prev => ({ ...prev, memberId, accountNumber: res.data.data[0].accountNumber }));
            }
        } catch (error) {
            console.error("Could not fetch account", error);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSubmitting(true);

        try {
            // Determine Endpoint based on Type
            const endpoint = formData.type === 'DEPOSIT' ? '/api/savings/deposit' : '/api/savings/withdraw';

            // Construct Payload (Matches your SavingsController)
            const payload = new FormData();
            payload.append('accountNumber', formData.accountNumber);
            payload.append('amount', formData.amount);
            payload.append('description', formData.description || `${formData.type} via Admin`);

            // Note: Your current SavingsController might strictly expect @RequestParam or Form Data.
            // We use the 'params' object for axios to send as query params if that's what your backend expects,
            // OR use a standard POST body. Let's stick to the URLSearchParams format for safety with your specific Controller.

            const params = new URLSearchParams();
            params.append('accountNumber', formData.accountNumber);
            params.append('amount', formData.amount);
            params.append('description', formData.description);

            await api.post(endpoint, null, { params }); // Sending as Query Params

            alert("Transaction Successful!");
            onSuccess(); // Refresh parent data
            onClose();   // Close modal
            setFormData({ memberId: '', accountNumber: '', type: 'DEPOSIT', amount: '', description: '', paymentMethod: 'CASH', referenceCode: '' });

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
                        {formData.type === 'DEPOSIT' ? <ArrowDownLeft className="text-emerald-600" /> : <ArrowUpRight className="text-red-600" />}
                        Record {formData.type === 'DEPOSIT' ? 'Deposit' : 'Withdrawal'}
                    </h3>
                    <button onClick={onClose} className="text-slate-400 hover:text-slate-600 transition"><X size={20} /></button>
                </div>

                <form onSubmit={handleSubmit} className="p-6 space-y-4">

                    {/* Transaction Type Toggle */}
                    <div className="flex bg-slate-100 p-1 rounded-lg">
                        <button
                            type="button"
                            onClick={() => setFormData({ ...formData, type: 'DEPOSIT' })}
                            className={`flex-1 py-2 text-sm font-bold rounded-md transition ${formData.type === 'DEPOSIT' ? 'bg-white text-emerald-700 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
                        >
                            Deposit
                        </button>
                        <button
                            type="button"
                            onClick={() => setFormData({ ...formData, type: 'WITHDRAWAL' })}
                            className={`flex-1 py-2 text-sm font-bold rounded-md transition ${formData.type === 'WITHDRAWAL' ? 'bg-white text-red-700 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
                        >
                            Withdrawal
                        </button>
                    </div>

                    {/* Member Selection */}
                    <div>
                        <label className="block text-xs font-bold text-slate-500 uppercase mb-1">Select Member</label>
                        <select
                            required
                            value={formData.memberId}
                            onChange={handleMemberSelect}
                            className="w-full p-3 border border-slate-200 rounded-xl bg-slate-50 focus:outline-none focus:ring-2 focus:ring-slate-900 transition"
                        >
                            <option value="">-- Select a Member --</option>
                            {members.map(m => (
                                <option key={m.id} value={m.id}>{m.firstName} {m.lastName} - {m.memberNumber}</option>
                            ))}
                        </select>
                    </div>

                    {/* Account Info (Read Only) */}
                    {formData.accountNumber && (
                        <div className="text-xs text-center text-slate-500 bg-blue-50 p-2 rounded-lg border border-blue-100">
                            Target Account: <span className="font-mono font-bold text-blue-700">{formData.accountNumber}</span>
                        </div>
                    )}

                    {/* Amount */}
                    <div>
                        <label className="block text-xs font-bold text-slate-500 uppercase mb-1">Amount (KES)</label>
                        <input
                            type="number"
                            required
                            min="1"
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
                            placeholder="e.g. Cash Deposit, M-Pesa Ref QK..."
                        />
                    </div>

                    <button
                        type="submit"
                        disabled={submitting || !formData.accountNumber}
                        className={`w-full py-3 rounded-xl font-bold text-white flex items-center justify-center gap-2 transition shadow-lg ${
                            formData.type === 'DEPOSIT'
                            ? 'bg-emerald-600 hover:bg-emerald-700 shadow-emerald-200'
                            : 'bg-red-600 hover:bg-red-700 shadow-red-200'
                        } ${submitting || !formData.accountNumber ? 'opacity-50 cursor-not-allowed' : ''}`}
                    >
                        {submitting ? <Loader2 className="animate-spin" /> : <Save size={18} />}
                        {submitting ? 'Processing...' : `Confirm ${formData.type === 'DEPOSIT' ? 'Deposit' : 'Withdrawal'}`}
                    </button>

                </form>
            </div>
        </div>
    );
}