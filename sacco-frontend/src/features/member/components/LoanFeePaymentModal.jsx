import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { X, CreditCard, Smartphone, CheckCircle, Loader, ArrowRight, AlertCircle } from 'lucide-react';
// ✅ FIX: Use 3 levels up ('../../../') to reach 'src/components'
import BrandedSpinner from '../../../components/BrandedSpinner';

export default function LoanFeePaymentModal({ isOpen, onClose, onSuccess, loan, isNewApplication = false }) {
    const [step, setStep] = useState(1);
    const [loading, setLoading] = useState(false);
    const [fee, setFee] = useState(0);
    const [phoneNumber, setPhoneNumber] = useState('');
    const [paymentStatus, setPaymentStatus] = useState('idle'); // idle, processing, verified, failed

    useEffect(() => {
        if (isOpen) {
            setStep(1);
            setPaymentStatus('idle');
            if (isNewApplication) {
                // For new applications, fetch system processing fee
                fetchSystemProcessingFee();
            } else if (loan) {
                // For existing loans, fetch specific loan fee
                fetchFee();
            }
        }
    }, [isOpen, loan, isNewApplication]);

    const fetchSystemProcessingFee = async () => {
        try {
            // Get default processing fee from system settings or products
            const res = await api.get('/api/loans/products');
            if (res.data.success && res.data.data.length > 0) {
                // Use the first product's processing fee as default
                // Or you can add a system setting for this
                setFee(res.data.data[0].processingFee || 500);
            }
        } catch (e) {
            console.error("Failed to fetch processing fee", e);
            setFee(500); // Default fallback
        }
    };

    const fetchFee = async () => {
        try {
            if (isNewApplication) {
                // For new applications, fetch from system setting
                const res = await api.get('/api/loans/application-fee');
                if (res.data.success) {
                    setFee(res.data.amount);
                } else {
                    setFee(500); // Fallback
                }
                return;
            }

            // For existing loans with product
            if (!loan) return;

            // 1. Check if fee is passed directly
            if (loan.processingFee) {
                setFee(loan.processingFee);
                return;
            }

            // 2. Fallback: Fetch from backend
            const res = await api.get(`/api/loans/${loan.id}`);
            if (res.data.success) {
                const loanData = res.data.data;
                if (loanData.processingFee) {
                    setFee(loanData.processingFee);
                } else if (loanData.productName) {
                    const productRes = await api.get('/api/loans/products');
                    const product = productRes.data.data.find(p => p.name === loanData.productName);
                    setFee(product ? product.processingFee : 0);
                }
            }
        } catch (e) {
            console.error("Failed to fetch fee", e);
            setFee(500); // Fallback
        }
    };

    const initiatePayment = async (e) => {
        e.preventDefault();
        setStep(2); // Move to Payment Verification UI
        setPaymentStatus('processing');

        // SIMULATION: In production, this would trigger the STK Push
        setTimeout(() => {
            confirmPayment();
        }, 5000); // 5s delay for user to read instructions
    };

    const confirmPayment = async () => {
        try {
            const refCode = "MPESA" + Math.floor(100000 + Math.random() * 900000);
            console.log("Processing payment with reference:", refCode);
            console.log("Is new application:", isNewApplication);

            if (isNewApplication) {
                // For new applications, use the new endpoint that creates draft
                const params = new URLSearchParams();
                params.append('referenceCode', refCode);

                console.log("Calling /api/loans/pay-application-fee...");
                const res = await api.post('/api/loans/pay-application-fee', params);
                console.log("Payment response:", res.data);

                setPaymentStatus('verified');
                setTimeout(() => {
                    console.log("Calling onSuccess with draft loan:", res.data.data);
                    onSuccess(res.data.data); // Pass the draft loan data
                    onClose();
                }, 2500);
            } else {
                // For existing loans, use the original logic
                const params = new URLSearchParams();
                params.append('referenceCode', refCode);

                await api.post(`/api/loans/${loan.id}/pay-fee`, params);

                setPaymentStatus('verified');
                setTimeout(() => {
                    onSuccess();
                    onClose();
                }, 2500);
            }
        } catch (e) {
            console.error("Payment failed:", e);
            setPaymentStatus('failed');
            setStep(1);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-in fade-in duration-200">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden flex flex-col">

                {/* Header */}
                <div className="bg-slate-50 border-b border-slate-100 p-4 flex justify-between items-center text-slate-800">
                    <h3 className="font-bold flex items-center gap-2">
                        <CreditCard size={20} className="text-indigo-600"/>
                        {isNewApplication ? 'Loan Application Fee' : 'Pay Application Fee'}
                    </h3>
                    <button onClick={onClose} className="p-1 hover:bg-slate-200 rounded-full transition"><X size={20} className="text-slate-400"/></button>
                </div>

                <div className="p-8">
                    {/* STEP 1: REVIEW & METHOD */}
                    {step === 1 && (
                        <form onSubmit={initiatePayment} className="space-y-8 animate-in slide-in-from-right-4 duration-300">
                            <div className="text-center">
                                <p className="text-slate-500 text-xs font-bold uppercase tracking-wider mb-2">
                                    {isNewApplication ? 'Application Fee' : 'Total Amount Due'}
                                </p>
                                <h2 className="text-4xl font-extrabold text-slate-900 tracking-tight">KES {Number(fee).toLocaleString()}</h2>
                                {!isNewApplication && loan && (
                                    <div className="mt-2 inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-slate-100 text-slate-500 text-xs font-medium">
                                        <span>Loan Ref:</span>
                                        <span className="font-mono text-slate-700 font-bold">{loan.loanNumber}</span>
                                    </div>
                                )}
                                {isNewApplication && (
                                    <p className="mt-3 text-sm text-slate-600">
                                        Pay this fee to access the loan application form
                                    </p>
                                )}
                            </div>

                            <div className="space-y-4">
                                <div className="border-2 border-emerald-500 bg-emerald-50/50 p-4 rounded-xl flex items-center justify-between cursor-pointer ring-4 ring-emerald-500/10 transition">
                                    <div className="flex items-center gap-4">
                                        <div className="bg-emerald-500 text-white p-2.5 rounded-lg shadow-sm"><Smartphone size={24}/></div>
                                        <div>
                                            <p className="font-bold text-slate-800 text-sm">M-Pesa Express</p>
                                            <p className="text-xs text-slate-500 font-medium">Instant STK Push to your phone</p>
                                        </div>
                                    </div>
                                    <CheckCircle size={20} className="text-emerald-600"/>
                                </div>

                                <div>
                                    <label className="block text-xs font-bold text-slate-500 mb-1.5 ml-1">Confirm M-Pesa Number</label>
                                    <input
                                        type="tel"
                                        required
                                        placeholder="07..."
                                        className="w-full p-3.5 border border-slate-300 rounded-xl outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-500/10 font-mono text-lg font-bold text-slate-800 transition"
                                        value={phoneNumber}
                                        onChange={e => setPhoneNumber(e.target.value)}
                                    />
                                </div>
                            </div>

                            <button type="submit" disabled={loading} className="w-full bg-slate-900 hover:bg-emerald-600 text-white font-bold py-4 rounded-xl flex items-center justify-center gap-2 transition-all transform active:scale-[0.98] shadow-lg shadow-slate-900/20">
                                Pay KES {Number(fee).toLocaleString()} <ArrowRight size={20}/>
                            </button>
                        </form>
                    )}

                    {/* STEP 2: PROCESSING */}
                    {step === 2 && (
                        <div className="text-center py-4 space-y-8 animate-in zoom-in-95 duration-300">

                            {/* STATUS: PROCESSING */}
                            {paymentStatus === 'processing' && (
                                <div className="flex flex-col items-center">
                                    {/* ✅ Uses the new BrandedSpinner you liked */}
                                    <BrandedSpinner size="large" showTagline={false} borderColor="border-emerald-500" />

                                    <div className="mt-8 space-y-2">
                                        <h3 className="text-xl font-bold text-slate-800">Check your phone</h3>
                                        <p className="text-slate-500 text-sm leading-relaxed max-w-[240px] mx-auto">
                                            We've sent an M-Pesa request to <span className="font-mono font-bold text-slate-800">{phoneNumber || "your number"}</span>.
                                        </p>
                                    </div>

                                    <div className="mt-6 flex items-center gap-2 text-emerald-600 bg-emerald-50 px-4 py-2 rounded-full border border-emerald-100">
                                        <Loader size={16} className="animate-spin"/>
                                        <span className="text-xs font-bold uppercase tracking-wider">Waiting for PIN...</span>
                                    </div>
                                </div>
                            )}

                            {/* STATUS: SUCCESS */}
                            {paymentStatus === 'verified' && (
                                <div className="flex flex-col items-center animate-in zoom-in duration-300">
                                    <div className="w-24 h-24 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mb-6 border-4 border-white shadow-xl shadow-emerald-100">
                                        <CheckCircle size={48}/>
                                    </div>
                                    <h3 className="text-2xl font-bold text-emerald-700">Payment Confirmed!</h3>
                                    <p className="text-slate-500 text-sm mt-2">Your application has been submitted.</p>
                                </div>
                            )}

                            {/* STATUS: FAILED */}
                            {paymentStatus === 'failed' && (
                                <div className="flex flex-col items-center animate-in zoom-in duration-300">
                                    <div className="w-24 h-24 bg-red-50 text-red-600 rounded-full flex items-center justify-center mb-6 border-4 border-white shadow-xl shadow-red-50">
                                        <AlertCircle size={48}/>
                                    </div>
                                    <h3 className="text-2xl font-bold text-red-700">Payment Failed</h3>
                                    <p className="text-slate-500 text-sm mt-2 max-w-[250px]">We couldn't verify the transaction. Please check your balance or try again.</p>
                                    <button onClick={() => setStep(1)} className="mt-8 text-indigo-600 font-bold text-sm hover:underline flex items-center gap-1">
                                        <ArrowRight size={16} className="rotate-180"/> Try Again
                                    </button>
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}