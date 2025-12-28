import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { X, CreditCard, Smartphone, CheckCircle, Loader, ArrowRight, AlertCircle } from 'lucide-react';
import BrandedSpinner from '../../../components/BrandedSpinner';

export default function LoanFeePaymentModal({ isOpen, onClose, onSuccess, loan, isNewApplication = false }) {
    const [step, setStep] = useState(1);
    const [loading, setLoading] = useState(false);
    const [products, setProducts] = useState([]);
    const [selectedProductId, setSelectedProductId] = useState('');
    const [fee, setFee] = useState(0);
    const [phoneNumber, setPhoneNumber] = useState('');
    const [paymentStatus, setPaymentStatus] = useState('idle'); // idle, processing, verified, failed

    useEffect(() => {
        if (isOpen) {
            setStep(1);
            setPaymentStatus('idle');
            fetchInitialData();
        }
    }, [isOpen, loan, isNewApplication]);

    const fetchInitialData = async () => {
        setLoading(true);
        try {
            // 1. Always fetch products so user can choose if it's a new application
            const res = await api.get('/api/loans/products');
            if (res.data.success) {
                setProducts(res.data.data);

                if (isNewApplication && res.data.data.length > 0) {
                    // Default to first product
                    setSelectedProductId(res.data.data[0].id);
                    setFee(res.data.data[0].processingFee || 500);
                }
            }

            // 2. If it's an existing loan, set the specific fee
            if (!isNewApplication && loan) {
                setFee(loan.processingFee || 500);
            }
        } catch (e) {
            console.error("Failed to fetch initial data", e);
        } finally {
            setLoading(false);
        }
    };

    // Update fee when product selection changes (for new apps)
    const handleProductChange = (e) => {
        const prodId = e.target.value;
        setSelectedProductId(prodId);
        const product = products.find(p => p.id === prodId);
        setFee(product ? product.processingFee : 500);
    };

    const initiatePayment = async (e) => {
        e.preventDefault();
        setStep(2);
        setPaymentStatus('processing');

        // SIMULATION: Triggers STK Push
        // In your real 5-day sprint, replace this timeout with your actual M-Pesa API call
        setTimeout(() => {
            confirmPayment();
        }, 4000);
    };

    const confirmPayment = async () => {
        try {
            const refCode = "MPESA" + Math.floor(100000 + Math.random() * 900000);

            if (isNewApplication) {
                // ✅ NEW WORKFLOW: Create the Draft Loan by paying the fee
                // Matches your LoanService.initiateWithFee(memberId, productId, refCode)
                const response = await api.post('/api/loans/initiate-with-fee', {
                    productId: selectedProductId,
                    referenceCode: refCode
                });

                if (response.data.success) {
                    setPaymentStatus('verified');
                    setTimeout(() => {
                        // Pass the created draft loan back to MemberLoans.jsx
                        onSuccess(response.data.data);
                        onClose();
                    }, 2000);
                }
            } else {
                // EXISTING WORKFLOW: Paying fee for a loan that already exists (e.g. Resume)
                await api.post(`/api/loans/${loan.id}/pay-fee`, { referenceCode: refCode });
                setPaymentStatus('verified');
                setTimeout(() => {
                    onSuccess();
                    onClose();
                }, 2000);
            }
        } catch (e) {
            console.error("Payment verification failed:", e);
            setPaymentStatus('failed');
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-in fade-in duration-200">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden flex flex-col">

                {/* Header */}
                <div className="bg-slate-50 border-b border-slate-100 p-4 flex justify-between items-center text-slate-800">
                    <h3 className="font-bold flex items-center gap-2 text-sm uppercase tracking-wider">
                        <CreditCard size={18} className="text-indigo-600"/>
                        {isNewApplication ? 'New Application' : 'Pending Fee'}
                    </h3>
                    <button onClick={onClose} className="p-1 hover:bg-slate-200 rounded-full transition"><X size={20} className="text-slate-400"/></button>
                </div>

                <div className="p-8">
                    {step === 1 && (
                        <form onSubmit={initiatePayment} className="space-y-6">
                            {/* Product Selection for New Applications */}
                            {isNewApplication && (
                                <div className="space-y-2">
                                    <label className="text-xs font-bold text-slate-500 uppercase ml-1">Select Loan Type</label>
                                    <select
                                        className="w-full p-3 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:ring-2 focus:ring-indigo-500 font-medium text-slate-700"
                                        value={selectedProductId}
                                        onChange={handleProductChange}
                                        required
                                    >
                                        {products.map(p => (
                                            <option key={p.id} value={p.id}>{p.name}</option>
                                        ))}
                                    </select>
                                </div>
                            )}

                            <div className="text-center py-4 bg-indigo-50/50 rounded-2xl border border-indigo-100">
                                <p className="text-slate-500 text-[10px] font-bold uppercase tracking-widest mb-1">Processing Fee</p>
                                <h2 className="text-3xl font-black text-indigo-900 tracking-tight">KES {Number(fee).toLocaleString()}</h2>
                                <p className="text-[10px] text-indigo-400 mt-1 font-medium">Non-refundable application charge</p>
                            </div>

                            <div className="space-y-4">
                                <div className="border border-emerald-200 bg-emerald-50/30 p-4 rounded-xl flex items-center justify-between">
                                    <div className="flex items-center gap-3">
                                        <div className="bg-emerald-500 text-white p-2 rounded-lg"><Smartphone size={20}/></div>
                                        <div>
                                            <p className="font-bold text-slate-800 text-sm">M-Pesa Express</p>
                                            <p className="text-[10px] text-slate-500">Instant STK Push</p>
                                        </div>
                                    </div>
                                    <CheckCircle size={18} className="text-emerald-500"/>
                                </div>

                                <div>
                                    <label className="block text-[10px] font-bold text-slate-400 uppercase mb-1 ml-1">M-Pesa Number</label>
                                    <input
                                        type="tel"
                                        required
                                        placeholder="0712345678"
                                        className="w-full p-3.5 border border-slate-200 rounded-xl outline-none focus:border-emerald-500 font-mono text-lg font-bold text-slate-800 transition"
                                        value={phoneNumber}
                                        onChange={e => setPhoneNumber(e.target.value)}
                                    />
                                </div>
                            </div>

                            <button type="submit" className="w-full bg-slate-900 hover:bg-indigo-600 text-white font-bold py-4 rounded-xl flex items-center justify-center gap-2 transition-all shadow-lg active:scale-95">
                                Pay & Unlock Form <ArrowRight size={18}/>
                            </button>
                        </form>
                    )}

                    {step === 2 && (
                        <div className="text-center py-10 space-y-8 animate-in zoom-in-95 duration-300">
                            {paymentStatus === 'processing' && (
                                <div className="flex flex-col items-center">
                                    <BrandedSpinner size="large" showTagline={false} borderColor="border-emerald-500" />
                                    <div className="mt-8">
                                        <h3 className="text-lg font-bold text-slate-800">Check Your Phone</h3>
                                        <p className="text-slate-500 text-sm mt-1">Enter your M-Pesa PIN to complete payment</p>
                                    </div>
                                    <div className="mt-6 flex items-center gap-2 text-emerald-600 bg-emerald-50 px-4 py-2 rounded-full border border-emerald-100 animate-pulse">
                                        <Loader size={14} className="animate-spin"/>
                                        <span className="text-[10px] font-bold uppercase tracking-widest">Waiting for PIN...</span>
                                    </div>
                                </div>
                            )}

                            {paymentStatus === 'verified' && (
                                <div className="flex flex-col items-center animate-in zoom-in duration-300">
                                    <div className="w-20 h-20 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mb-6 shadow-inner">
                                        <CheckCircle size={40}/>
                                    </div>
                                    <h3 className="text-xl font-bold text-slate-800">Fee Paid Successfully</h3>
                                    <p className="text-slate-500 text-sm mt-1">Unlocking your application form...</p>
                                </div>
                            )}

                            {paymentStatus === 'failed' && (
                                <div className="flex flex-col items-center">
                                    <div className="w-20 h-20 bg-red-50 text-red-600 rounded-full flex items-center justify-center mb-6">
                                        <AlertCircle size={40}/>
                                    </div>
                                    <h3 className="text-xl font-bold text-slate-800">Payment Failed</h3>
                                    <button onClick={() => setStep(1)} className="mt-6 bg-slate-100 hover:bg-slate-200 text-slate-700 px-6 py-2 rounded-lg text-sm font-bold transition">
                                        Try Again
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