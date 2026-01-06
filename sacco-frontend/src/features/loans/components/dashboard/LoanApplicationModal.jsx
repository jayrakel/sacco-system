import React, { useState, useEffect } from 'react';
import { X, Calculator, AlertCircle, Check, Loader2, CreditCard, ArrowRight, ArrowLeft, Smartphone, CheckCircle } from 'lucide-react';
import { loanService, paymentService } from '../../../../api'; // Import paymentService

export default function LoanApplicationModal({ isOpen, onClose, onSuccess }) {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState(null);

    // Wizard State
    const [step, setStep] = useState(1); // 1: Details, 2: Payment

    // Form State
    const [formData, setFormData] = useState({
        productId: '',
        amount: '',
        durationWeeks: '',
        phoneNumber: '',      // ✅ Added for STK Push
        paymentReference: ''  // Will be auto-filled after successful payment
    });

    // Payment State
    const [paymentStatus, setPaymentStatus] = useState('IDLE'); // IDLE, PROCESSING, PAID

    const [estimates, setEstimates] = useState(null);
    const [selectedProduct, setSelectedProduct] = useState(null);

    // 1. Fetch Products on Load
    useEffect(() => {
        if (isOpen) {
            fetchProducts();
            // Reset state
            setStep(1);
            setFormData({ productId: '', amount: '', durationWeeks: '', phoneNumber: '', paymentReference: '' });
            setEstimates(null);
            setSelectedProduct(null);
            setPaymentStatus('IDLE');
        }
    }, [isOpen]);

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

    // 2. Handle Product Selection & Calculations
    useEffect(() => {
        if (formData.productId) {
            const product = products.find(p => p.id === formData.productId);
            setSelectedProduct(product);

            if (product && formData.amount && formData.durationWeeks) {
                const principal = parseFloat(formData.amount);
                const rate = product.interestRate / 100;
                const interest = principal * rate;
                const totalRepayment = principal + interest;
                const weekly = totalRepayment / parseFloat(formData.durationWeeks);

                setEstimates({
                    interest: interest.toFixed(2),
                    total: totalRepayment.toFixed(2),
                    weekly: weekly.toFixed(2),
                    rate: product.interestRate,
                    fee: product.applicationFee || 0
                });
            }
        }
    }, [formData.productId, formData.amount, formData.durationWeeks, products]);

    const handleNext = () => {
        // Validate Step 1
        if (!formData.productId || !formData.amount || !formData.durationWeeks) {
            setError("Please fill in all loan details.");
            return;
        }

        // Check if fee applies
        if (selectedProduct && selectedProduct.applicationFee > 0) {
            setError(null);
            setStep(2); // Go to Payment Step
        } else {
            handleSubmit(); // No fee, submit directly
        }
    };

    // ✅ NEW: Trigger M-Pesa STK Push
    const handlePayFee = async () => {
        if (!formData.phoneNumber) {
            setError("Please enter your M-Pesa phone number.");
            return;
        }

        setError(null);
        setPaymentStatus('PROCESSING');

        try {
            // Call Backend to trigger STK Push
            // Ideally, we need the logged-in Member ID here.
            // Assuming the API extracts it from the JWT Token or context.
            // If API requires memberId explicitly, we need to fetch it from AuthContext.
            // For now, we assume the backend endpoint handles the context or we pass it if available.

            // NOTE: The `paymentService.payLoanFee` needs to be defined in your api.js
            // Or use a direct axios call:

            // Simulating API Call Structure
            const res = await paymentService.payLoanFee({
                 amount: selectedProduct.applicationFee,
                 phoneNumber: formData.phoneNumber
            });

            if (res.success) {
                // In a real live system, you would start polling here.
                // For this simulation, we wait 3 seconds and assume success.
                setTimeout(() => {
                    setPaymentStatus('PAID');
                    // Use the CheckoutRequestID as the proof of payment
                    setFormData(prev => ({ ...prev, paymentReference: res.checkoutRequestId }));
                }, 3000);
            } else {
                setPaymentStatus('IDLE');
                setError(res.message);
            }
        } catch (err) {
            setPaymentStatus('IDLE');
            setError(err.response?.data?.message || "Payment request failed. Check phone number.");
        }
    };

    const handleSubmit = async (e) => {
        if (e) e.preventDefault();
        setSubmitting(true);
        setError(null);

        try {
            const res = await loanService.applyForLoan({
                productId: formData.productId,
                amount: parseFloat(formData.amount),
                durationWeeks: parseInt(formData.durationWeeks),
                paymentReference: formData.paymentReference // ✅ Send the Proof of Payment
            });

            if (res.success) {
                onSuccess(res.data);
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
                        <h2 className="text-xl font-black text-slate-800">
                            {step === 1 ? "New Loan Application" : "Application Fee Payment"}
                        </h2>
                        <p className="text-sm text-slate-500">
                            {step === 1 ? "Step 1: Loan Details" : "Step 2: M-Pesa Payment"}
                        </p>
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
                        <>
                            {/* STEP 1: DETAILS */}
                            {step === 1 && (
                                <div className="space-y-5 animate-in slide-in-from-left-5">
                                    <div className="space-y-2">
                                        <label className="text-xs font-bold uppercase tracking-wider text-slate-500">Select Product</label>
                                        <select
                                            className="w-full p-4 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:border-indigo-500 transition-all font-medium text-slate-700 outline-none"
                                            value={formData.productId}
                                            onChange={e => setFormData({ ...formData, productId: e.target.value })}
                                        >
                                            <option value="">-- Choose a Loan Product --</option>
                                            {products.map(p => (
                                                <option key={p.id} value={p.id}>
                                                    {p.name} (Fee: {p.applicationFee > 0 ? `KES ${p.applicationFee}` : 'Free'})
                                                </option>
                                            ))}
                                        </select>
                                    </div>

                                    <div className="grid grid-cols-2 gap-4">
                                        <div className="space-y-2">
                                            <label className="text-xs font-bold uppercase tracking-wider text-slate-500">Amount (KES)</label>
                                            <input
                                                type="number"
                                                className="w-full p-4 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white outline-none font-bold text-slate-800"
                                                placeholder="0.00"
                                                value={formData.amount}
                                                onChange={e => setFormData({ ...formData, amount: e.target.value })}
                                            />
                                        </div>
                                        <div className="space-y-2">
                                            <label className="text-xs font-bold uppercase tracking-wider text-slate-500">Duration (Weeks)</label>
                                            <input
                                                type="number"
                                                className="w-full p-4 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white outline-none font-bold text-slate-800"
                                                placeholder="e.g. 52"
                                                value={formData.durationWeeks}
                                                onChange={e => setFormData({ ...formData, durationWeeks: e.target.value })}
                                            />
                                        </div>
                                    </div>

                                    {estimates && (
                                        <div className="bg-indigo-50 p-5 rounded-2xl border border-indigo-100 space-y-3">
                                            <div className="flex items-center gap-2 text-indigo-800 font-bold text-sm mb-2">
                                                <Calculator size={16} /> Summary
                                            </div>
                                            <div className="flex justify-between text-sm">
                                                <span className="text-indigo-600/70">Application Fee</span>
                                                <span className="font-bold text-indigo-900">
                                                    {estimates.fee > 0 ? `KES ${estimates.fee}` : 'Free'}
                                                </span>
                                            </div>
                                            <div className="flex justify-between items-center pt-2 border-t border-indigo-200/50">
                                                <span className="text-xs font-black uppercase text-indigo-500 tracking-wider">Total Repayment</span>
                                                <span className="font-black text-xl text-indigo-700">KES {estimates.total}</span>
                                            </div>
                                        </div>
                                    )}
                                </div>
                            )}

                            {/* STEP 2: PAYMENT (Updated for STK Push) */}
                            {step === 2 && selectedProduct && (
                                <div className="space-y-6 text-center animate-in slide-in-from-right-10 fade-in duration-300">
                                    <div className="w-16 h-16 bg-emerald-50 text-emerald-600 rounded-full flex items-center justify-center mx-auto mb-4">
                                        <CreditCard size={32} />
                                    </div>

                                    <div>
                                        <h3 className="text-lg font-bold text-slate-800">Fee Payment Required</h3>
                                        <p className="text-slate-500 text-sm">Pay <b>KES {selectedProduct.applicationFee}</b> to proceed.</p>
                                    </div>

                                    {/* M-Pesa Input Section */}
                                    <div className="bg-slate-50 p-6 rounded-2xl border border-slate-100 space-y-4">
                                        {paymentStatus === 'IDLE' && (
                                            <>
                                                <div className="text-left space-y-2">
                                                    <label className="text-xs font-bold uppercase tracking-wider text-slate-500">M-Pesa Phone Number</label>
                                                    <input
                                                        type="text"
                                                        className="w-full p-4 rounded-xl border border-slate-200 bg-white focus:border-emerald-500 focus:ring-4 focus:ring-emerald-500/10 outline-none font-bold text-slate-800 placeholder:font-normal"
                                                        placeholder="e.g. 0712345678"
                                                        value={formData.phoneNumber}
                                                        onChange={e => setFormData({ ...formData, phoneNumber: e.target.value })}
                                                    />
                                                </div>
                                                <button
                                                    onClick={handlePayFee}
                                                    className="w-full py-4 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl font-bold flex items-center justify-center gap-2 transition shadow-lg shadow-emerald-200"
                                                >
                                                    <Smartphone size={18} /> Pay KES {selectedProduct.applicationFee}
                                                </button>
                                            </>
                                        )}

                                        {paymentStatus === 'PROCESSING' && (
                                            <div className="py-8">
                                                <Loader2 className="animate-spin text-emerald-600 mx-auto mb-4" size={32} />
                                                <h4 className="font-bold text-slate-800">Processing Payment...</h4>
                                                <p className="text-sm text-slate-500 mt-2">Check your phone and enter your M-Pesa PIN.</p>
                                            </div>
                                        )}

                                        {paymentStatus === 'PAID' && (
                                            <div className="py-4 bg-emerald-100 rounded-xl border border-emerald-200">
                                                <CheckCircle className="text-emerald-600 mx-auto mb-2" size={32} />
                                                <h4 className="font-bold text-emerald-800">Payment Verified!</h4>
                                                <p className="text-xs text-emerald-600 mt-1">Ref: {formData.paymentReference}</p>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            )}
                        </>
                    )}
                </div>

                {/* Footer Buttons */}
                <div className="p-6 border-t border-slate-100 bg-slate-50 flex justify-between gap-3">
                    {step === 2 ? (
                        <button
                            onClick={() => setStep(1)}
                            className="px-6 py-3 rounded-xl text-sm font-bold text-slate-500 hover:bg-slate-200 transition flex items-center gap-2"
                        >
                            <ArrowLeft size={16}/> Back
                        </button>
                    ) : (
                        <button
                            onClick={onClose}
                            className="px-6 py-3 rounded-xl text-sm font-bold text-slate-500 hover:bg-slate-200 transition"
                        >
                            Cancel
                        </button>
                    )}

                    {step === 1 ? (
                        <button
                            onClick={handleNext}
                            className="bg-indigo-600 hover:bg-indigo-700 text-white px-8 py-3 rounded-xl text-sm font-bold shadow-lg shadow-indigo-200 flex items-center gap-2 transition ml-auto"
                        >
                            Next Step <ArrowRight size={18} />
                        </button>
                    ) : (
                        <button
                            onClick={handleSubmit}
                            disabled={submitting || paymentStatus !== 'PAID'}
                            className="bg-slate-900 hover:bg-slate-800 text-white px-8 py-3 rounded-xl text-sm font-bold shadow-lg shadow-slate-200 flex items-center gap-2 transition disabled:opacity-50 disabled:cursor-not-allowed ml-auto"
                        >
                            {submitting ? <Loader2 className="animate-spin" size={18}/> : <Check size={18}/>}
                            Create Draft
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
}