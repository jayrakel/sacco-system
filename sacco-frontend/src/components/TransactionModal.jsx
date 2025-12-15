import React, { useState, useEffect } from 'react';
import api from '../api';
import { X, Save, ArrowDownLeft, ArrowUpRight, Repeat, Loader2, Smartphone } from 'lucide-react';

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
        paymentMethod: 'CASH', // CASH, MPESA
        phoneNumber: '', // ✅ New field for M-Pesa
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

    // Fetch account for a specific member & Auto-fill Phone
    const fetchAccount = async (memId, isTarget = false) => {
        try {
            const res = await api.get(`/api/savings/member/${memId}`);
            if (res.data.success && res.data.data.length > 0) {
                const accNum = res.data.data[0].accountNumber;

                // Auto-fill phone number if member found in our local list
                let phone = '';
                if (!isTarget) {
                    const member = members.find(m => m.id === memId);
                    if (member) phone = member.phoneNumber;
                }

                setFormData(prev => isTarget
                    ? { ...prev, targetMemberId: memId, targetAccountNumber: accNum }
                    : { ...prev, memberId: memId, accountNumber: accNum, phoneNumber: phone }
                );
            }
        } catch (error) { console.error("Could not fetch account", error); }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSubmitting(true);

        try {
            // ✅ CASE 1: M-PESA STK PUSH
            if (formData.paymentMethod === 'MPESA' && formData.type !== 'TRANSFER') {
                const params = new URLSearchParams();
                params.append('memberId', formData.memberId);
                params.append('amount', formData.amount);
                params.append('phoneNumber', formData.phoneNumber);

                // This triggers the PaymentService.initiateMpesaPayment backend logic
                await api.post('/api/payments/mpesa/stk', null, { params });
                alert("STK Push Sent! Check the member's phone to complete the transaction.");
            }
            // ✅ CASE 2: STANDARD TRANSACTION (Cash / Internal Transfer)
            else {
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
                alert("Transaction Recorded Successfully!");
            }

            onSuccess();
            onClose();
            // Reset Form
            setFormData({
                type: 'DEPOSIT', memberId: '', accountNumber: '',
                targetMemberId: '', targetAccountNumber: '',
                amount: '', description: '',
                paymentMethod: 'CASH', phoneNumber: '', referenceCode: ''
            });

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
                                onClick={() => setFormData({ ...formData, type, paymentMethod: 'CASH' })} // Reset payment method on switch
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
                            placeholder="e.g. Monthly Contribution..."
                        />
                    </div>

                    {/* ✅ Payment Method Toggle (Only for Deposit/Withdrawal) */}
                    {formData.type !== 'TRANSFER' && (
                        <div className="grid grid-cols-2 gap-3 mt-2">
                            <button
                                type="button"
                                onClick={() => setFormData({...formData, paymentMethod: 'CASH'})}
                                className={`p-3 rounded-xl border flex items-center justify-center gap-2 text-sm font-bold transition ${formData.paymentMethod === 'CASH' ? 'border-emerald-500 bg-emerald-50 text-emerald-700' : 'border-slate-200 text-slate-500 hover:bg-slate-50'}`}
                            >
                                💵 Cash
                            </button>
                            <button
                                type="button"
                                onClick={() => setFormData({...formData, paymentMethod: 'MPESA'})}
                                className={`p-3 rounded-xl border flex items-center justify-center gap-2 text-sm font-bold transition ${formData.paymentMethod === 'MPESA' ? 'border-green-500 bg-green-50 text-green-700' : 'border-slate-200 text-slate-500 hover:bg-slate-50'}`}
                            >
                                <Smartphone size={16}/> M-Pesa
                            </button>
                        </div>
                    )}

                    {/* ✅ M-Pesa Phone Number Input */}
                    {formData.paymentMethod === 'MPESA' && formData.type !== 'TRANSFER' && (
                        <div className="animate-in fade-in slide-in-from-top-2 duration-200">
                            <label className="block text-xs font-bold text-slate-500 uppercase mb-1">M-Pesa Number</label>
                            <input
                                type="text"
                                required
                                value={formData.phoneNumber}
                                onChange={(e) => setFormData({ ...formData, phoneNumber: e.target.value })}
                                className="w-full p-3 border border-green-200 rounded-xl bg-green-50/30 text-sm focus:outline-none focus:ring-2 focus:ring-green-500 transition"
                                placeholder="07XX..."
                            />
                            <p className="text-[10px] text-green-600 mt-1">An STK push will be sent to this number.</p>
                        </div>
                    )}

                    <button
                        type="submit"
                        disabled={submitting || !formData.accountNumber || (formData.type === 'TRANSFER' && !formData.targetAccountNumber)}
                        className={`w-full py-3 rounded-xl font-bold text-white flex items-center justify-center gap-2 transition shadow-lg ${
                            formData.paymentMethod === 'MPESA' ? 'bg-slate-900 hover:bg-slate-800 shadow-slate-200' :
                            formData.type === 'DEPOSIT' ? 'bg-emerald-600 hover:bg-emerald-700 shadow-emerald-200' :
                            formData.type === 'WITHDRAWAL' ? 'bg-red-600 hover:bg-red-700 shadow-red-200' :
                            'bg-blue-600 hover:bg-blue-700 shadow-blue-200'
                        } disabled:opacity-50 disabled:cursor-not-allowed`}
                    >
                        {submitting ? <Loader2 className="animate-spin" /> : (formData.paymentMethod === 'MPESA' ? <Smartphone size={18}/> : <Save size={18} />)}
                        {submitting
                            ? (formData.paymentMethod === 'MPESA' ? 'Sending Request...' : 'Processing...')
                            : (formData.paymentMethod === 'MPESA' ? 'Send STK Push' : `Confirm ${formData.type}`)
                        }
                    </button>

                </form>
            </div>
        </div>
    );
}