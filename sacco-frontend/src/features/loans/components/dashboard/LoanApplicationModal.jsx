import React, { useState, useEffect } from 'react';
import api from '../../../../api'; // Adjust path if necessary
import { X, ChevronRight, AlertCircle, CheckCircle, Calculator, Users, FileText } from 'lucide-react';

export default function LoanApplicationModal({ isOpen, onClose }) {
    // --- STATE ---
    const [step, setStep] = useState(1); // 1: Details, 2: Guarantors, 3: Review
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    // Form Data (Matches Backend DTO)
    const [formData, setFormData] = useState({
        productId: '',
        amount: '',
        repaymentPeriod: ''
    });

    // Response from Backend after Step 1 (Draft Creation)
    const [activeLoanDraft, setActiveLoanDraft] = useState(null);

    // --- EFFECT: Load Products ---
    useEffect(() => {
        if (isOpen) {
            fetchProducts();
            setStep(1);
            setError('');
            setFormData({ productId: '', amount: '', repaymentPeriod: '' });
        }
    }, [isOpen]);

    const fetchProducts = async () => {
        try {
            const res = await api.get('/api/loans/products'); // Ensure this endpoint exists and returns list
            if (res.data.success) {
                setProducts(res.data.data.filter(p => p.active)); // Only active products
            }
        } catch (e) {
            setError("Failed to load loan products.");
        }
    };

    // --- ACTIONS ---

    // Step 1: Create Draft Loan (Initiate)
    const handleInitiate = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');

        // Validation
        const selectedProduct = products.find(p => p.id === formData.productId);
        if (!selectedProduct) { setError("Invalid Product"); setLoading(false); return; }

        if (Number(formData.amount) < selectedProduct.minLimit || Number(formData.amount) > selectedProduct.maxLimit) {
            setError(`Amount must be between ${selectedProduct.minLimit} and ${selectedProduct.maxLimit}`);
            setLoading(false);
            return;
        }

        try {
            // ✅ PAYLOAD MATCHES BACKEND LoanRequestDTO
            const payload = {
                productId: formData.productId,
                amount: Number(formData.amount),
                repaymentPeriod: Number(formData.repaymentPeriod)
            };

            const res = await api.post('/api/loans/apply', payload);

            if (res.data.success) {
                // Success! We have a draft loan ID now.
                setActiveLoanDraft(res.data.data); // Contains loanId, loanNumber, etc.
                setStep(2); // Move to Guarantors
            } else {
                setError(res.data.message);
            }
        } catch (e) {
            setError(e.response?.data?.message || "Failed to start application");
        } finally {
            setLoading(false);
        }
    };

    // Placeholder for future steps
    const handleSubmitFinal = async () => {
        // We will implement this in the next phase
        alert("Submitting Loan " + activeLoanDraft?.loanNumber);
        onClose();
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-in fade-in">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl overflow-hidden border border-slate-200 flex flex-col max-h-[90vh]">

                {/* Header */}
                <div className="bg-slate-50 p-4 border-b flex justify-between items-center">
                    <div>
                        <h2 className="font-bold text-slate-800 text-lg">New Loan Application</h2>
                        <p className="text-xs text-slate-500">Step {step} of 3</p>
                    </div>
                    <button onClick={onClose} className="p-2 hover:bg-slate-200 rounded-full transition"><X size={20}/></button>
                </div>

                {/* Progress Bar */}
                <div className="flex border-b border-slate-100">
                    <div className={`flex-1 p-2 text-center text-xs font-bold ${step >= 1 ? 'text-emerald-600 border-b-2 border-emerald-500' : 'text-slate-400'}`}>1. Details</div>
                    <div className={`flex-1 p-2 text-center text-xs font-bold ${step >= 2 ? 'text-emerald-600 border-b-2 border-emerald-500' : 'text-slate-400'}`}>2. Guarantors</div>
                    <div className={`flex-1 p-2 text-center text-xs font-bold ${step >= 3 ? 'text-emerald-600 border-b-2 border-emerald-500' : 'text-slate-400'}`}>3. Review</div>
                </div>

                {/* Body Content */}
                <div className="p-6 overflow-y-auto">
                    {error && (
                        <div className="mb-4 p-3 bg-red-50 text-red-600 text-sm rounded-lg flex items-center gap-2">
                            <AlertCircle size={16}/> {error}
                        </div>
                    )}

                    {/* --- STEP 1: LOAN DETAILS --- */}
                    {step === 1 && (
                        <form onSubmit={handleInitiate} className="space-y-5">
                            <div>
                                <label className="block text-sm font-bold text-slate-700 mb-1">Select Product</label>
                                <select
                                    className="w-full p-3 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-emerald-500 outline-none"
                                    value={formData.productId}
                                    onChange={(e) => setFormData({...formData, productId: e.target.value})}
                                    required
                                >
                                    <option value="">-- Choose a Loan Product --</option>
                                    {products.map(p => (
                                        <option key={p.id} value={p.id}>
                                            {p.productName} (Interest: {p.interestRate}% p.a)
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
                                        placeholder="e.g. 50000"
                                        value={formData.amount}
                                        onChange={(e) => setFormData({...formData, amount: e.target.value})}
                                        required
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-bold text-slate-700 mb-1">Repayment (Months)</label>
                                    <input
                                        type="number"
                                        className="w-full p-3 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-emerald-500 outline-none font-mono"
                                        placeholder="e.g. 12"
                                        value={formData.repaymentPeriod}
                                        onChange={(e) => setFormData({...formData, repaymentPeriod: e.target.value})}
                                        required
                                    />
                                </div>
                            </div>

                            {/* Dynamic Hint based on selection */}
                            {formData.productId && (
                                <div className="p-3 bg-blue-50 text-blue-700 text-xs rounded-lg">
                                    <strong>Product Limits:</strong>
                                    Min: KES {products.find(p=>p.id===formData.productId)?.minLimit?.toLocaleString()} |
                                    Max: KES {products.find(p=>p.id===formData.productId)?.maxLimit?.toLocaleString()}
                                </div>
                            )}

                            <div className="pt-4">
                                <button
                                    type="submit"
                                    disabled={loading}
                                    className="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-bold py-3 rounded-xl flex items-center justify-center gap-2 disabled:opacity-50"
                                >
                                    {loading ? "Processing..." : <>Next: Add Guarantors <ChevronRight size={18}/></>}
                                </button>
                            </div>
                        </form>
                    )}

                    {/* --- STEP 2: GUARANTORS (Placeholder for next interaction) --- */}
                    {step === 2 && (
                        <div className="text-center py-10">
                            <Users size={48} className="mx-auto text-emerald-200 mb-4"/>
                            <h3 className="text-xl font-bold text-slate-800">Draft Created: {activeLoanDraft?.loanNumber}</h3>
                            <p className="text-slate-500">We are ready to add guarantors in the next step.</p>

                            <button onClick={handleSubmitFinal} className="mt-6 bg-emerald-600 text-white px-6 py-2 rounded-lg font-bold">
                                Proceed (Simulated)
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}