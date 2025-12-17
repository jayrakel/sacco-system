import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { X, CreditCard, Smartphone, CheckCircle, Loader, ArrowRight } from 'lucide-react';

export default function LoanFeePaymentModal({ isOpen, onClose, onSuccess, loan }) {
    const [step, setStep] = useState(1);
    const [loading, setLoading] = useState(false);
    const [fee, setFee] = useState(0);
    const [phoneNumber, setPhoneNumber] = useState('');
    const [paymentStatus, setPaymentStatus] = useState('idle'); // idle, processing, verified, failed

    useEffect(() => {
        if (isOpen && loan) {
            setStep(1);
            setPaymentStatus('idle');
            fetchFee();
        }
    }, [isOpen, loan]);

    const fetchFee = async () => {
        try {
            // Get specific loan details to find its fee
            const res = await api.get(`/api/loans/${loan.id}`);
            if (res.data.success) {
                // The backend DTO might not have the fee directly,
                // but we can fetch the product details if needed.
                // Assuming standard fee for now or available in response.
                // For robustness, let's fetch product configuration or rely on a known default/endpoint
                // Here we simulate a fetch or use a default if not in DTO
                const productRes = await api.get('/api/loans/products');
                const product = productRes.data.data.find(p => p.name === res.data.data.productName); // Adjust matching logic as needed

                setFee(product ? product.processingFee : 500);
            }
        } catch (e) {
            console.error("Failed to fetch fee", e);
            setFee(500); // Fallback default
        }
    };

    const initiatePayment = async (e) => {
        e.preventDefault();
        setStep(2); // Move to Payment Verification UI
        setPaymentStatus('processing');

        // SIMULATION: Simulate M-Pesa STK Push delay
        // In production, this would be a real socket/polling check
        setTimeout(() => {
            confirmPayment();
        }, 3000);
    };

    const confirmPayment = async () => {
        try {
            const refCode = "MPESA" + Math.floor(100000 + Math.random() * 900000);

            // Call Backend to Finalize
            const params = new URLSearchParams();
            params.append('referenceCode', refCode);

            await api.post(`/api/loans/${loan.id}/pay-fee`, params);

            setPaymentStatus('verified');
            setTimeout(() => {
                onSuccess();
                onClose();
            }, 2000);
        } catch (e) {
            setPaymentStatus('failed');
            alert("Payment Verification Failed: " + (e.response?.data?.message || "Unknown error"));
            setStep(1);
        }
    };

    if (!isOpen || !loan) return null;

    return (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4 z-50">
            <div className="bg-white rounded-2xl shadow-xl w-full max-w-md overflow-hidden flex flex-col animate-in zoom-in-95 duration-200">

                {/* Header */}
                <div className="bg-indigo-900 p-4 flex justify-between items-center text-white shrink-0">
                    <h3 className="font-bold flex items-center gap-2"><CreditCard size={18}/> Pay Application Fee</h3>
                    <button onClick={onClose}><X size={18}/></button>
                </div>

                <div className="p-6">
                    {/* STEP 1: REVIEW & METHOD */}
                    {step === 1 && (
                        <form onSubmit={initiatePayment} className="space-y-6">
                            <div className="text-center space-y-2">
                                <p className="text-slate-500 text-sm">Application Fee Required</p>
                                <h2 className="text-3xl font-bold text-slate-800">KES {Number(fee).toLocaleString()}</h2>
                                <p className="text-xs text-slate-400 bg-slate-50 py-1 px-2 rounded-full inline-block">
                                    Loan Ref: {loan.loanNumber}
                                </p>
                            </div>

                            <div className="space-y-3">
                                <label className="block text-xs font-bold text-slate-500 uppercase">Select Payment Method</label>

                                <div className="border border-indigo-500 bg-indigo-50 p-4 rounded-xl flex items-center justify-between cursor-pointer ring-1 ring-indigo-500">
                                    <div className="flex items-center gap-3">
                                        <div className="bg-green-500 text-white p-2 rounded-lg"><Smartphone size={20}/></div>
                                        <div>
                                            <p className="font-bold text-slate-800 text-sm">M-Pesa STK Push</p>
                                            <p className="text-xs text-slate-500">Fast & Automatic</p>
                                        </div>
                                    </div>
                                    <CheckCircle size={18} className="text-indigo-600"/>
                                </div>

                                <div>
                                    <label className="block text-xs font-bold text-slate-500 mb-1 ml-1">M-Pesa Phone Number</label>
                                    <input
                                        type="tel"
                                        required
                                        placeholder="07..."
                                        className="w-full p-3 border rounded-xl outline-none focus:ring-2 focus:ring-indigo-500 font-mono"
                                        value={phoneNumber}
                                        onChange={e => setPhoneNumber(e.target.value)}
                                    />
                                </div>
                            </div>

                            <button type="submit" disabled={loading} className="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-bold py-3 rounded-xl flex items-center justify-center gap-2 transition shadow-lg shadow-emerald-600/20">
                                Pay and Submit <ArrowRight size={18}/>
                            </button>
                        </form>
                    )}

                    {/* STEP 2: PROCESSING */}
                    {step === 2 && (
                        <div className="text-center py-8 space-y-6">
                            {paymentStatus === 'processing' && (
                                <>
                                    <div className="relative mx-auto w-20 h-20">
                                        <div className="absolute inset-0 border-4 border-slate-100 rounded-full"></div>
                                        <div className="absolute inset-0 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin"></div>
                                        <Smartphone className="absolute inset-0 m-auto text-slate-400" size={32}/>
                                    </div>
                                    <div>
                                        <h3 className="text-lg font-bold text-slate-800">Check your phone</h3>
                                        <p className="text-slate-500 text-sm mt-1">Enter your M-Pesa PIN to complete the transaction.</p>
                                        <div className="flex items-center justify-center gap-2 mt-4 text-indigo-600">
                                            <Loader size={14} className="animate-spin"/>
                                            <span className="text-xs font-bold">Waiting for verification...</span>
                                        </div>
                                    </div>
                                </>
                            )}

                            {paymentStatus === 'verified' && (
                                <div className="animate-in zoom-in duration-300">
                                    <div className="w-20 h-20 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto mb-4 border-4 border-white shadow-lg">
                                        <CheckCircle size={40}/>
                                    </div>
                                    <h3 className="text-xl font-bold text-emerald-700">Payment Successful!</h3>
                                    <p className="text-slate-500 text-sm mt-2">Your application has been submitted for review.</p>
                                </div>
                            )}

                            {paymentStatus === 'failed' && (
                                <div className="animate-in zoom-in duration-300">
                                    <div className="w-20 h-20 bg-red-100 text-red-600 rounded-full flex items-center justify-center mx-auto mb-4 border-4 border-white shadow-lg">
                                        <X size={40}/>
                                    </div>
                                    <h3 className="text-xl font-bold text-red-700">Payment Failed</h3>
                                    <p className="text-slate-500 text-sm mt-2">Please try again or contact support.</p>
                                    <button onClick={() => setStep(1)} className="mt-4 text-indigo-600 font-bold text-sm hover:underline">Try Again</button>
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}