import React, { useState, useEffect } from 'react';
import api from '../../../../api';
import { X, ChevronRight, AlertTriangle, Info, CheckCircle, Calculator } from 'lucide-react';
import BrandedSpinner from '../../../../components/BrandedSpinner';

export default function LoanApplicationModal({ isOpen, onClose, onSuccess, draft }) {
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState(null);

    // Data from Backend
    const [products, setProducts] = useState([]);
    const [limits, setLimits] = useState({ maxEligibleAmount: 0, currency: 'KES' });

    // Form State
    const [formData, setFormData] = useState({
        productId: '',
        amount: '',
        durationWeeks: ''
    });

    // Computed State for Validation
    const [selectedProduct, setSelectedProduct] = useState(null);
    const [validation, setValidation] = useState({ isValid: false, message: '' });

    useEffect(() => {
        if (isOpen) {
            fetchData();
            // Reset form when opening
            setFormData({ productId: '', amount: '', durationWeeks: '' });
            setSelectedProduct(null);
            setError(null);
            setValidation({ isValid: false, message: '' });
        }
    }, [isOpen]);

    // ✅ FETCH DATA: Products & Limits
    const fetchData = async () => {
        setLoading(true);
        setError(null);
        try {
            // Run both requests in parallel for speed
            const [productsRes, limitsRes] = await Promise.all([
                api.get('/api/loans/products'),
                api.get('/api/loans/limits')
            ]);

            if (productsRes.data.success) {
                setProducts(productsRes.data.data);
            }
            if (limitsRes.data.success) {
                setLimits(limitsRes.data.data);
            }
        } catch (e) {
            console.error("Failed to load loan data", e);
            setError("Could not load loan options. Please check your connection or contact support.");
        } finally {
            setLoading(false);
        }
    };

    // Handle Product Change
    const handleProductChange = (e) => {
        const prodId = e.target.value;
        const product = products.find(p => p.id === prodId);

        setFormData(prev => ({ ...prev, productId: prodId }));
        setSelectedProduct(product || null);

        // Re-validate with new product rules
        validateForm(prodId, formData.amount, formData.durationWeeks, product);
    };

    // Handle Input Changes & Validate
    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));

        const newAmount = name === 'amount' ? value : formData.amount;
        const newDuration = name === 'durationWeeks' ? value : formData.durationWeeks;

        validateForm(formData.productId, newAmount, newDuration, selectedProduct);
    };

    // ✅ SMART GUARDRAILS (Frontend Side)
    const validateForm = (prodId, amountStr, durationStr, product) => {
        const amount = Number(amountStr);
        const duration = Number(durationStr);

        // 0. Basic Checks
        if (!prodId) {
            setValidation({ isValid: false, message: '' }); // No error, just invalid
            return;
        }

        // 1. Check Global Limit (Savings * Multiplier)
        if (amount > limits.maxEligibleAmount) {
            setValidation({
                isValid: false,
                message: `Amount exceeds your eligibility limit of ${limits.currency} ${limits.maxEligibleAmount.toLocaleString()}`
            });
            return;
        }

        if (product) {
            // 2. Check Product Min/Max
            if (amount < product.minAmount) {
                setValidation({ isValid: false, message: `Minimum amount for ${product.productName} is ${limits.currency} ${product.minAmount.toLocaleString()}` });
                return;
            }
            if (amount > product.maxAmount) {
                setValidation({ isValid: false, message: `Maximum amount for ${product.productName} is ${limits.currency} ${product.maxAmount.toLocaleString()}` });
                return;
            }

            // 3. Check Duration
            if (duration > product.maxDurationWeeks) {
                setValidation({ isValid: false, message: `Maximum duration for this product is ${product.maxDurationWeeks} weeks` });
                return;
            }
        }

        // 4. Valid Input Check
        if (amount > 0 && duration > 0) {
            setValidation({ isValid: true, message: '' });
        } else {
            setValidation({ isValid: false, message: '' });
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!validation.isValid) return;

        setSubmitting(true);
        setError(null);

        try {
            // ✅ SUBMIT: Hits the new backend endpoint
            const res = await api.post(`/api/loans/drafts/${draft.id}/submit-details`, {
                productId: formData.productId,
                amount: Number(formData.amount),
                durationWeeks: Number(formData.durationWeeks)
            });

            if (res.data.success) {
                onSuccess(); // Close modal & Refresh Dashboard
            } else {
                setError(res.data.message);
            }
        } catch (e) {
            console.error(e);
            // Show specific backend error (e.g., "Liquidity Limit Reached")
            setError(e.response?.data?.message || "Application failed. Please check your inputs.");
        } finally {
            setSubmitting(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-in fade-in">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden flex flex-col border border-slate-200">

                {/* Header */}
                <div className="bg-slate-50 border-b border-slate-100 p-4 flex justify-between items-center">
                    <h3 className="font-bold text-slate-800 flex items-center gap-2">
                        <Calculator size={20} className="text-emerald-600"/>
                        Loan Application Details
                    </h3>
                    <button onClick={onClose} className="p-1 hover:bg-slate-200 rounded-full transition"><X size={20} className="text-slate-400"/></button>
                </div>

                <div className="p-6">
                    {loading ? (
                        <div className="py-12 flex flex-col items-center justify-center text-slate-500 gap-3">
                            <BrandedSpinner size="large" />
                            <p className="text-sm font-medium">Checking eligibility & products...</p>
                        </div>
                    ) : (
                        <form onSubmit={handleSubmit} className="space-y-6">

                            {/* Error Banner */}
                            {error && (
                                <div className="bg-red-50 text-red-600 p-4 rounded-xl text-sm flex items-start gap-3 border border-red-100">
                                    <AlertTriangle size={18} className="mt-0.5 shrink-0"/>
                                    <span className="font-medium">{error}</span>
                                </div>
                            )}

                            {/* Eligibility Banner (The Guardrail Visualized) */}
                            <div className="bg-emerald-50 border border-emerald-100 p-4 rounded-xl flex items-center justify-between">
                                <div>
                                    <p className="text-xs text-emerald-600 font-bold uppercase tracking-wider mb-1">Your Maximum Limit</p>
                                    <p className="text-2xl font-black text-slate-800 tracking-tight">
                                        <span className="text-sm text-slate-500 font-normal mr-1">{limits.currency}</span>
                                        {limits.maxEligibleAmount?.toLocaleString()}
                                    </p>
                                </div>
                                <div className="h-10 w-10 bg-white rounded-full flex items-center justify-center shadow-sm text-emerald-600">
                                    <CheckCircle size={20}/>
                                </div>
                            </div>

                            {/* 1. Product Selection */}
                            <div>
                                <label className="block text-sm font-bold text-slate-700 mb-2">Select Loan Product</label>
                                <select
                                    name="productId"
                                    className="w-full p-3 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-emerald-500 outline-none transition font-medium text-slate-700"
                                    value={formData.productId}
                                    onChange={handleProductChange}
                                    required
                                >
                                    <option value="">-- Choose a Product --</option>
                                    {products.map(p => (
                                        <option key={p.id} value={p.id}>
                                            {p.productName} (Rate: {p.interestRate}%)
                                        </option>
                                    ))}
                                </select>
                                {selectedProduct && (
                                    <div className="mt-2 text-xs text-slate-500 flex items-center gap-4 bg-slate-50 p-2 rounded-lg border border-slate-100">
                                        <span className="flex items-center gap-1"><Info size={12}/> Max: {selectedProduct.maxAmount.toLocaleString()}</span>
                                        <span>Max Period: {selectedProduct.maxDurationWeeks} weeks</span>
                                    </div>
                                )}
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                {/* 2. Amount Input */}
                                <div>
                                    <label className="block text-sm font-bold text-slate-700 mb-2">Amount ({limits.currency})</label>
                                    <input
                                        type="number"
                                        name="amount"
                                        className={`w-full p-3 bg-white border rounded-xl focus:ring-2 outline-none transition font-bold text-lg
                                            ${(validation.isValid || !formData.amount) ? 'border-slate-200 focus:ring-emerald-500' : 'border-red-300 focus:ring-red-200 text-red-600'}
                                        `}
                                        placeholder="0.00"
                                        value={formData.amount}
                                        onChange={handleChange}
                                        required
                                    />
                                </div>

                                {/* 3. Duration Input */}
                                <div>
                                    <label className="block text-sm font-bold text-slate-700 mb-2">Period (Weeks)</label>
                                    <input
                                        type="number"
                                        name="durationWeeks"
                                        className="w-full p-3 bg-white border border-slate-200 rounded-xl focus:ring-2 focus:ring-emerald-500 outline-none transition font-bold text-lg"
                                        placeholder="e.g. 24"
                                        value={formData.durationWeeks}
                                        onChange={handleChange}
                                        required
                                    />
                                </div>
                            </div>

                            {/* Validation Message (Real-time Feedback) */}
                            {!validation.isValid && validation.message && (
                                <p className="text-sm text-red-500 font-bold bg-red-50 p-3 rounded-lg flex items-center gap-2 animate-pulse">
                                    <AlertTriangle size={16}/> {validation.message}
                                </p>
                            )}

                            {/* Submit Button */}
                            <button
                                type="submit"
                                disabled={!validation.isValid || submitting}
                                className="w-full bg-slate-900 hover:bg-emerald-600 disabled:bg-slate-300 disabled:cursor-not-allowed text-white font-bold py-4 rounded-xl flex items-center justify-center gap-2 transition-all shadow-lg active:scale-95"
                            >
                                {submitting ? <BrandedSpinner size="small" color="border-white"/> : (
                                    <>
                                        Submit Application <ChevronRight size={18}/>
                                    </>
                                )}
                            </button>
                        </form>
                    )}
                </div>
            </div>
        </div>
    );
}