import React, { useState, useEffect } from 'react';
import api from '../../../../api';
import { X, ChevronRight, AlertTriangle, Info, CheckCircle, Calculator, Users } from 'lucide-react';
import BrandedSpinner from '../../../../components/BrandedSpinner';
import GuarantorManager from '../GuarantorManager';

export default function LoanApplicationModal({ isOpen, onClose, onSuccess, draft, existingLoan }) {
    // 1 = Details Form, 2 = Guarantor Manager
    const [step, setStep] = useState(1);

    // Holds the Loan Object (Either created now in Step 1 OR passed via Resume)
    const [activeLoan, setActiveLoan] = useState(null);

    // Form States
    const [loading, setLoading] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState(null);
    const [products, setProducts] = useState([]);

    // Limits (Savings/Eligibility) - Needed for Step 1 Validation AND Step 2 Self-Guarantee Logic
    const [limits, setLimits] = useState({ maxEligibleAmount: 0, totalDeposits: 0, currency: 'KES' });

    const [formData, setFormData] = useState({ productId: '', amount: '', durationWeeks: '' });
    const [selectedProduct, setSelectedProduct] = useState(null);
    const [validation, setValidation] = useState({ isValid: false, message: '' });

    // --- INIT LOGIC ---
    useEffect(() => {
        if (isOpen) {
            // Always fetch limits so Step 2 has them for "Self-Guarantee" calculations
            fetchData();

            if (existingLoan) {
                // RESUME MODE: Skip Step 1, go straight to Step 2
                console.log("Resuming existing loan application:", existingLoan);
                setActiveLoan(existingLoan);
                setStep(2);
            } else {
                // NEW APPLICATION: Start at Step 1
                setStep(1);
                setActiveLoan(null);
                setFormData({ productId: '', amount: '', durationWeeks: '' });
                setSelectedProduct(null);
                setValidation({ isValid: false, message: '' });
            }
            setError(null);
        }
    }, [isOpen, existingLoan]);

    const fetchData = async () => {
        setLoading(true);
        try {
            const [prodRes, limitRes] = await Promise.all([
                api.get('/api/loans/products'),
                api.get('/api/loans/limits')
            ]);

            if (prodRes.data.success) setProducts(prodRes.data.data);
            if (limitRes.data.success) setLimits(limitRes.data.data);
        } catch (e) {
            console.error("Fetch Error:", e);
            setError("Failed to load loan options. Please check connection.");
        } finally {
            setLoading(false);
        }
    };

    // --- FORM HANDLERS ---
    const handleProductChange = (e) => {
        const prodId = e.target.value;
        const product = products.find(p => p.id === prodId);
        setFormData(prev => ({ ...prev, productId: prodId }));
        setSelectedProduct(product || null);
        validateForm(prodId, formData.amount, formData.durationWeeks, product);
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        validateForm(formData.productId, name === 'amount' ? value : formData.amount, name === 'durationWeeks' ? value : formData.durationWeeks, selectedProduct);
    };

    const validateForm = (prodId, amountStr, durationStr, product) => {
        const amount = Number(amountStr);
        const duration = Number(durationStr);
        if (!prodId) return setValidation({ isValid: false });
        if (amount > limits.maxEligibleAmount) return setValidation({ isValid: false, message: `Exceeds limit` });
        if (product) {
            if (amount < product.minAmount) return setValidation({ isValid: false, message: `Min: ${product.minAmount}` });
            if (amount > product.maxAmount) return setValidation({ isValid: false, message: `Max: ${product.maxAmount}` });
            if (duration > product.maxDurationWeeks) return setValidation({ isValid: false, message: `Max duration: ${product.maxDurationWeeks} weeks` });
        }
        setValidation({ isValid: (amount > 0 && duration > 0), message: '' });
    };

    // --- ✅ STEP 1 SUBMIT: TRANSITION LOGIC ---
    const handleStep1Submit = async (e) => {
        e.preventDefault();

        // Safety check: Ensure we have a draft ID to submit to
        if (!draft || !draft.id) {
            setError("Session error: No active draft found. Please refresh.");
            return;
        }

        if (!validation.isValid) return;

        setSubmitting(true);
        setError(null);

        try {
            console.log("Submitting Step 1 Details...");
            const res = await api.post(`/api/loans/drafts/${draft.id}/submit-details`, {
                productId: formData.productId,
                amount: Number(formData.amount),
                durationWeeks: Number(formData.durationWeeks)
            });

            if (res.data.success) {
                console.log("Step 1 Success. Transitioning to Step 2 with Loan:", res.data.data);

                // 1. Store the created loan (needed for Step 2)
                setActiveLoan(res.data.data);

                // 2. Move UI to Step 2
                setStep(2);
            } else {
                setError(res.data.message);
            }
        } catch (e) {
            console.error(e);
            setError(e.response?.data?.message || "Failed to create loan application.");
        } finally {
            setSubmitting(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-in fade-in">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden flex flex-col border border-slate-200">

                {/* Header changes based on Step */}
                <div className="bg-slate-50 border-b border-slate-100 p-4 flex justify-between items-center">
                    <h3 className="font-bold text-slate-800 flex items-center gap-2">
                        {step === 1 ? <Calculator size={20} className="text-emerald-600"/> : <Users size={20} className="text-blue-600"/>}
                        {step === 1 ? 'Loan Details' : 'Manage Guarantors'}
                    </h3>
                    <button onClick={onClose} className="p-1 hover:bg-slate-200 rounded-full transition"><X size={20} className="text-slate-400"/></button>
                </div>

                <div className="p-6">
                    {/* --- STEP 1: DETAILS FORM --- */}
                    {step === 1 && (
                        loading && !products.length ? <div className="py-10 flex justify-center"><BrandedSpinner/></div> :
                        <form onSubmit={handleStep1Submit} className="space-y-6">
                             {error && <div className="text-red-600 bg-red-50 p-3 rounded-lg text-sm border border-red-100 flex items-center gap-2"><AlertTriangle size={16}/> {error}</div>}

                             <div className="bg-emerald-50 p-4 rounded-xl border border-emerald-100">
                                <p className="text-xs font-bold text-emerald-600 uppercase">Your Limit</p>
                                <p className="text-2xl font-black text-slate-800">{limits.currency} {limits.maxEligibleAmount.toLocaleString()}</p>
                            </div>

                             <div>
                                <label className="block text-sm font-bold text-slate-700 mb-1">Product</label>
                                <select name="productId" className="w-full p-3 bg-slate-50 border rounded-xl" value={formData.productId} onChange={handleProductChange} required>
                                    <option value="">-- Select --</option>
                                    {products.map(p => <option key={p.id} value={p.id}>{p.productName} ({p.interestRate}%)</option>)}
                                </select>
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-bold text-slate-700 mb-1">Amount</label>
                                    <input type="number" name="amount" className="w-full p-3 border rounded-xl font-bold" value={formData.amount} onChange={handleChange} required />
                                </div>
                                <div>
                                    <label className="block text-sm font-bold text-slate-700 mb-1">Weeks</label>
                                    <input type="number" name="durationWeeks" className="w-full p-3 border rounded-xl font-bold" value={formData.durationWeeks} onChange={handleChange} required />
                                </div>
                            </div>

                             {validation.message && <p className="text-red-500 text-sm">{validation.message}</p>}

                             <button type="submit" disabled={!validation.isValid || submitting} className="w-full bg-slate-900 text-white font-bold py-3.5 rounded-xl flex justify-center items-center gap-2 hover:bg-emerald-600 transition disabled:bg-slate-300 shadow-lg">
                                {submitting ? <BrandedSpinner size="small" color="border-white"/> : <>Next <ChevronRight size={18}/></>}
                            </button>
                        </form>
                    )}

                    {/* --- STEP 2: GUARANTORS --- */}
                    {/* This renders only when step is 2 AND activeLoan is set */}
                    {step === 2 && activeLoan && (
                        <GuarantorManager
                            loan={activeLoan}
                            onSuccess={onSuccess}
                            applicantLimits={limits} // ✅ Passes Savings Data for Self-Guarantee Logic
                        />
                    )}
                </div>
            </div>
        </div>
    );
}