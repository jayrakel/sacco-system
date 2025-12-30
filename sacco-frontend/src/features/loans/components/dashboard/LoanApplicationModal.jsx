import React, { useState, useEffect } from 'react';
import { X, ChevronRight, Calculator, AlertCircle, Check, Loader2 } from 'lucide-react';
import { loanService } from '../../../../api';

export default function LoanApplicationModal({ isOpen, onClose, onSuccess, resumeLoan }) {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState(null);

    // Form State
    const [formData, setFormData] = useState({
        productId: '',
        amount: '',
        durationWeeks: '',
        purpose: ''
    });

    const [estimates, setEstimates] = useState(null);

    // 1. Fetch Products on Load
    useEffect(() => {
        if (isOpen) {
            fetchProducts();
            // Pre-fill if resuming a draft
            if (resumeLoan) {
                setFormData({
                    productId: resumeLoan.product?.id || '',
                    amount: resumeLoan.principalAmount || '',
                    durationWeeks: resumeLoan.durationWeeks || '',
                    purpose: ''
                });
            } else {
                // Reset form
                setFormData({ productId: '', amount: '', durationWeeks: '', purpose: '' });
                setEstimates(null);
            }
        }
    }, [isOpen, resumeLoan]);

    const fetchProducts = async () => {
        setLoading(true);
        try {
            const res = await loanService.getProducts();
            if (res.success) setProducts(res.data);
        } catch (err) {
            setError("Failed to load loan products.");
        } finally {
            setLoading(false);
        }
    };

    // 2. Calculate Estimates Logic
    useEffect(() => {
        if (formData.productId && formData.amount && formData.durationWeeks) {
            const product = products.find(p => p.id === formData.productId);
            if (product) {
                const principal = parseFloat(formData.amount);
                const rate = product.interestRate / 100; // Assuming monthly rate?
                const interest = principal * rate; // Simple flat rate logic (adjust as needed)
                const totalRepayment = principal + interest;
                const weekly = totalRepayment / parseFloat(formData.durationWeeks);

                setEstimates({
                    interest: interest.toFixed(2),
                    total: totalRepayment.toFixed(2),
                    weekly: weekly.toFixed(2),
                    rate: product.interestRate
                });
            }
        }
    }, [formData, products]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSubmitting(true);
        setError(null);

        try {
            // Call the API to create Draft
            const res = await loanService.applyForLoan({
                productId: formData.productId,
                amount: parseFloat(formData.amount),
                durationWeeks: parseInt(formData.durationWeeks),
                // purpose: formData.purpose
            });

            if (res.success) {
                onSuccess(res.data); // Pass the created Draft back
                onClose();
            } else {
                setError(res.message);
            }
        } catch (err) {
            setError(err.response?.data?.message || "Application failed. Please try again.");
        } finally {
            setSubmitting(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm animate-in fade-in">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden flex flex-col max-h-[90vh]">

                {/* Header */}
                <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50">
                    <div>
                        <h2 className="text-xl font-black text-slate-800">New Loan Application</h2>
                        <p className="text-sm text-slate-500">Create a draft to get started.</p>
                    </div>
                    <button onClick={onClose} className="p-2 hover:bg-slate-100 rounded-full text-slate-400 transition">
                        <X size={20} />
                    </button>
                </div>

                {/* Body */}
                <div className="p-6 overflow-y-auto space-y-6">
                    {error && (
                        <div className="bg-red-50 text-red-600 p-4 rounded-xl text-sm font-medium flex gap-3 items-start">
                            <AlertCircle size={18} className="shrink-0 mt-0.5" />
                            {error}
                        </div>
                    )}

                    {loading ? (
                        <div className="text-center py-10 text-slate-400"><Loader2 className="animate-spin mx-auto mb-2"/> Loading Products...</div>
                    ) : (
                        <form id="loanForm" onSubmit={handleSubmit} className="space-y-5">

                            {/* Product Select */}
                            <div className="space-y-2">
                                <label className="text-xs font-bold uppercase tracking-wider text-slate-500">Select Product</label>
                                <select
                                    className="w-full p-4 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:border-indigo-500 focus:ring-4 focus:ring-indigo-500/10 transition-all font-medium text-slate-700 outline-none"
                                    value={formData.productId}
                                    onChange={e => setFormData({ ...formData, productId: e.target.value })}
                                    required
                                >
                                    <option value="">-- Choose a Loan Product --</option>
                                    {products.map(p => (
                                        <option key={p.id} value={p.id}>{p.name} (Max: {p.maxAmount?.toLocaleString()})</option>
                                    ))}
                                </select>
                            </div>

                            {/* Amount & Duration */}
                            <div className="grid grid-cols-2 gap-4">
                                <div className="space-y-2">
                                    <label className="text-xs font-bold uppercase tracking-wider text-slate-500">Amount (KES)</label>
                                    <input
                                        type="number"
                                        className="w-full p-4 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:border-indigo-500 outline-none font-bold text-slate-800"
                                        placeholder="0.00"
                                        value={formData.amount}
                                        onChange={e => setFormData({ ...formData, amount: e.target.value })}
                                        required
                                        min="1000"
                                    />
                                </div>
                                <div className="space-y-2">
                                    <label className="text-xs font-bold uppercase tracking-wider text-slate-500">Duration (Weeks)</label>
                                    <input
                                        type="number"
                                        className="w-full p-4 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:border-indigo-500 outline-none font-bold text-slate-800"
                                        placeholder="e.g. 12"
                                        value={formData.durationWeeks}
                                        onChange={e => setFormData({ ...formData, durationWeeks: e.target.value })}
                                        required
                                        min="1"
                                    />
                                </div>
                            </div>

                            {/* Live Calculations Card */}
                            {estimates && (
                                <div className="bg-indigo-50 p-5 rounded-2xl border border-indigo-100 space-y-3">
                                    <div className="flex items-center gap-2 text-indigo-800 font-bold text-sm mb-2">
                                        <Calculator size={16} /> Repayment Estimates
                                    </div>
                                    <div className="flex justify-between text-sm">
                                        <span className="text-indigo-600/70">Interest Rate</span>
                                        <span className="font-bold text-indigo-900">{estimates.rate}%</span>
                                    </div>
                                    <div className="flex justify-between text-sm">
                                        <span className="text-indigo-600/70">Estimated Interest</span>
                                        <span className="font-bold text-indigo-900">KES {estimates.interest}</span>
                                    </div>
                                    <div className="border-t border-indigo-200/50 my-2"></div>
                                    <div className="flex justify-between items-center">
                                        <span className="text-xs font-black uppercase text-indigo-500 tracking-wider">Total Repayment</span>
                                        <span className="font-black text-xl text-indigo-700">KES {estimates.total}</span>
                                    </div>
                                </div>
                            )}

                        </form>
                    )}
                </div>

                {/* Footer */}
                <div className="p-6 border-t border-slate-100 bg-slate-50 flex justify-end gap-3">
                    <button
                        onClick={onClose}
                        type="button"
                        className="px-6 py-3 rounded-xl text-sm font-bold text-slate-500 hover:bg-slate-200 transition"
                    >
                        Cancel
                    </button>
                    <button
                        form="loanForm"
                        disabled={submitting || loading}
                        className="bg-indigo-600 hover:bg-indigo-700 text-white px-8 py-3 rounded-xl text-sm font-bold shadow-lg shadow-indigo-200 flex items-center gap-2 transition disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {submitting ? <Loader2 className="animate-spin" size={18}/> : <Check size={18}/>}
                        {resumeLoan ? 'Update Draft' : 'Create Draft Application'}
                    </button>
                </div>
            </div>
        </div>
    );
}