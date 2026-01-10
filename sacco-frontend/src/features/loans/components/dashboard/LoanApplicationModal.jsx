import React, { useState, useEffect } from 'react';
import api from '../../../../api';
import { X, ChevronRight, AlertCircle, Users, FileText } from 'lucide-react';

export default function LoanApplicationModal({ isOpen, onClose, onDraftCreated, resumeLoan }) {
    const [step, setStep] = useState(1);
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const [formData, setFormData] = useState({
        productId: '',
        amount: '',
        durationWeeks: ''
    });

    // Load Data & State Management
    useEffect(() => {
        if (isOpen) {
            fetchProducts();

            // Logic: Decide which step to show
            if (resumeLoan) {
                // If resuming a draft
                setFormData({
                    productId: resumeLoan.product?.id || '', // Adapt based on your DTO structure
                    amount: resumeLoan.principalAmount,
                    durationWeeks: resumeLoan.repaymentPeriod // or durationWeeks
                });

                if (resumeLoan.feePaid) {
                    setStep(2); // Go to Guarantors
                } else {
                    setStep(1); // Review Details (or could allow editing if logic permits)
                }
            } else {
                // Fresh Start
                setStep(1);
                setFormData({ productId: '', amount: '', durationWeeks: '' });
            }
            setError('');
        }
    }, [isOpen, resumeLoan]);

    const fetchProducts = async () => {
        try {
            const res = await api.get('/api/loans/products');
            if (res.data.success) {
                setProducts(res.data.data.filter(p => p.active));
            }
        } catch (e) {
            setError("Failed to load loan products.");
        }
    };

    // --- STEP 1: CREATE DRAFT ---
    const handleCreateDraft = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');

        const selectedProduct = products.find(p => p.id === formData.productId);
        if (!selectedProduct) { setError("Invalid Product"); setLoading(false); return; }

        if (Number(formData.amount) < selectedProduct.minAmount || Number(formData.amount) > selectedProduct.maxAmount) {
            setError(`Amount must be between ${selectedProduct.minAmount} and ${selectedProduct.maxAmount}`);
            setLoading(false);
            return;
        }

        try {
            const payload = {
                productId: formData.productId,
                amount: Number(formData.amount),
                durationWeeks: Number(formData.durationWeeks)
            };

            // ✅ CALL BACKEND TO PERSIST DRAFT
            const res = await api.post('/api/loans/initiate', payload);

            if (res.data.success) {
                // Notify Parent (MemberLoans) that draft exists
                // This triggers the Dashboard State Change & Opens Fee Modal
                onDraftCreated(res.data.data);
            } else {
                setError(res.data.message);
            }
        } catch (e) {
            setError(e.response?.data?.message || "Failed to start application");
        } finally {
            setLoading(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-in fade-in">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl overflow-hidden border border-slate-200 flex flex-col max-h-[90vh]">

                {/* Header */}
                <div className="bg-slate-50 p-4 border-b flex justify-between items-center">
                    <div>
                        <h2 className="font-bold text-slate-800 text-lg">
                            {step === 1 ? 'Start Application' : 'Add Guarantors'}
                        </h2>
                        <p className="text-xs text-slate-500">Step {step} of 3</p>
                    </div>
                    <button onClick={onClose} className="p-2 hover:bg-slate-200 rounded-full transition"><X size={20}/></button>
                </div>

                <div className="p-6 overflow-y-auto">
                    {error && (
                        <div className="mb-4 p-3 bg-red-50 text-red-600 text-sm rounded-lg flex items-center gap-2">
                            <AlertCircle size={16}/> {error}
                        </div>
                    )}

                    {/* --- STEP 1: FORM --- */}
                    {step === 1 && (
                        <form onSubmit={handleCreateDraft} className="space-y-5">
                            <div>
                                <label className="block text-sm font-bold text-slate-700 mb-1">Select Product</label>
                                <select
                                    className="w-full p-3 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-emerald-500 outline-none"
                                    value={formData.productId}
                                    onChange={(e) => setFormData({...formData, productId: e.target.value})}
                                    required
                                    disabled={!!resumeLoan} // Lock if resuming (optional)
                                >
                                    <option value="">-- Choose a Loan Product --</option>
                                    {products.map(p => (
                                        <option key={p.id} value={p.id}>
                                            {p.productName} (Interest: {p.interestRate}%)
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-bold text-slate-700 mb-1">Amount (KES)</label>
                                    <input
                                        type="number"
                                        className="w-full p-3 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-emerald-500 outline-none font-mono"
                                        value={formData.amount}
                                        onChange={(e) => setFormData({...formData, amount: e.target.value})}
                                        required
                                        readOnly={!!resumeLoan} // Lock if resuming
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-bold text-slate-700 mb-1">Duration (Weeks/Months)</label>
                                    <input
                                        type="number"
                                        className="w-full p-3 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-emerald-500 outline-none font-mono"
                                        value={formData.durationWeeks}
                                        onChange={(e) => setFormData({...formData, durationWeeks: e.target.value})}
                                        required
                                        readOnly={!!resumeLoan}
                                    />
                                </div>
                            </div>

                            <div className="pt-4">
                                <button
                                    type="submit"
                                    disabled={loading}
                                    className="w-full bg-slate-900 hover:bg-slate-800 text-white font-bold py-3 rounded-xl flex items-center justify-center gap-2 disabled:opacity-50"
                                >
                                    {loading ? "Creating Draft..." : <>Create Draft & Proceed <ChevronRight size={18}/></>}
                                </button>
                            </div>
                        </form>
                    )}

                    {/* --- STEP 2: GUARANTORS (Placeholder) --- */}
                    {step === 2 && (
                        <div className="text-center py-10">
                            <Users size={48} className="mx-auto text-emerald-200 mb-4"/>
                            <h3 className="text-xl font-bold text-slate-800">Guarantor Selection</h3>
                            <p className="text-slate-500">This module is under construction.</p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}