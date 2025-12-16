import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { X, CheckCircle, AlertCircle, Calculator } from 'lucide-react';

export default function LoanApplicationModal({ isOpen, onClose, onSuccess, memberId }) {
    const [products, setProducts] = useState([]);
    const [formData, setFormData] = useState({
        productId: '',
        amount: '',
        duration: ''
    });
    const [loading, setLoading] = useState(false);
    const [calculatedRepayment, setCalculatedRepayment] = useState(null);

    useEffect(() => {
        if (isOpen) {
            fetchProducts();
        }
    }, [isOpen]);

    const fetchProducts = async () => {
        try {
            const res = await api.get('/api/loans/products');
            if (res.data.success) setProducts(res.data.data);
        } catch (e) {
            console.error("Failed to load loan products");
        }
    };

    // Simple estimation (Client-side only for preview)
    const calculatePreview = () => {
        const product = products.find(p => p.id === formData.productId);
        if (!product || !formData.amount || !formData.duration) return;

        const P = parseFloat(formData.amount);
        const R = (product.interestRate / 100) / 12; // Monthly rate
        const N = parseInt(formData.duration);

        let monthly = 0;
        if (product.interestType === 'FLAT_RATE') {
            const totalInterest = P * (product.interestRate / 100) * (N / 12);
            monthly = (P + totalInterest) / N;
        } else {
            // Reducing Balance
            monthly = (P * R * Math.pow(1 + R, N)) / (Math.pow(1 + R, N) - 1);
        }
        setCalculatedRepayment(monthly.toFixed(2));
    };

    useEffect(() => {
        calculatePreview();
    }, [formData]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            const params = new URLSearchParams();
            // If memberId is passed, use it; otherwise rely on backend context
            if(memberId) params.append('memberId', memberId);
            params.append('productId', formData.productId);
            params.append('amount', formData.amount);
            params.append('duration', formData.duration);

            await api.post('/api/loans/apply', params);
            alert("Application Submitted Successfully!");
            onSuccess();
            onClose();
        } catch (e) {
            alert(e.response?.data?.message || "Application Failed");
        } finally {
            setLoading(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-in fade-in duration-200">
            <div className="bg-white rounded-2xl shadow-xl w-full max-w-md overflow-hidden">

                {/* Header */}
                <div className="bg-indigo-900 p-4 flex justify-between items-center text-white">
                    <h3 className="font-bold flex items-center gap-2"><Calculator size={18}/> Apply for Loan</h3>
                    <button onClick={onClose} className="hover:bg-white/20 p-1 rounded-full transition"><X size={18}/></button>
                </div>

                <form onSubmit={handleSubmit} className="p-6 space-y-4">

                    {/* Product Select */}
                    <div>
                        <label className="block text-xs font-bold text-slate-500 mb-1 uppercase">Loan Product</label>
                        <select
                            required
                            className="w-full p-3 border border-slate-200 rounded-xl bg-slate-50 focus:ring-2 focus:ring-indigo-500 outline-none transition"
                            value={formData.productId}
                            onChange={e => setFormData({...formData, productId: e.target.value})}
                        >
                            <option value="">-- Select a Loan Type --</option>
                            {products.map(p => (
                                <option key={p.id} value={p.id}>{p.name} (Max: {Number(p.maxLimit).toLocaleString()})</option>
                            ))}
                        </select>
                    </div>

                    {/* Amount & Duration */}
                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-xs font-bold text-slate-500 mb-1 uppercase">Amount (KES)</label>
                            <input
                                type="number" required min="100"
                                className="w-full p-3 border border-slate-200 rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none transition"
                                value={formData.amount}
                                onChange={e => setFormData({...formData, amount: e.target.value})}
                            />
                        </div>
                        <div>
                            <label className="block text-xs font-bold text-slate-500 mb-1 uppercase">Duration (Months)</label>
                            <input
                                type="number" required min="1" max="60"
                                className="w-full p-3 border border-slate-200 rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none transition"
                                value={formData.duration}
                                onChange={e => setFormData({...formData, duration: e.target.value})}
                            />
                        </div>
                    </div>

                    {/* Repayment Preview */}
                    {calculatedRepayment && (
                        <div className="bg-emerald-50 p-4 rounded-xl border border-emerald-100 flex items-start gap-3">
                            <CheckCircle className="text-emerald-600 mt-1" size={18}/>
                            <div>
                                <p className="text-xs font-bold text-emerald-800 uppercase">Estimated Monthly Installment</p>
                                <p className="text-xl font-bold text-emerald-700 font-mono">KES {Number(calculatedRepayment).toLocaleString()}</p>
                            </div>
                        </div>
                    )}

                    {/* Actions */}
                    <div className="flex gap-3 pt-4 border-t border-slate-100">
                        <button type="button" onClick={onClose} className="flex-1 py-3 text-slate-500 font-bold hover:bg-slate-50 rounded-xl transition">Cancel</button>
                        <button type="submit" disabled={loading} className="flex-1 py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-xl shadow-md shadow-indigo-200 transition disabled:opacity-50">
                            {loading ? 'Submitting...' : 'Submit Application'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}