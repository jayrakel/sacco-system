import React, { useState, useEffect, useRef } from 'react';
import api from '../../../api';
import { useSettings } from '../../../context/SettingsContext';
import { X, CheckCircle, ArrowRight, AlertCircle, ShieldCheck } from 'lucide-react';
import BrandedSpinner from '../../../components/BrandedSpinner';

export default function LoanFeePaymentModal({ isOpen, onClose, onSuccess, loan }) {
    const { settings } = useSettings();
    const [step, setStep] = useState(1);
    const [phone, setPhone] = useState('');
    const [feeAmount, setFeeAmount] = useState(500); // Default, updated by fetch
    const [statusMessage, setStatusMessage] = useState('');
    const [paymentStatus, setPaymentStatus] = useState('idle');

    // Polling State
    const pollingInterval = useRef(null);
    const pollingAttempts = useRef(0);
    const MAX_ATTEMPTS = 36; // 3 minutes timeout

    // ✅ FIX: Simple URL builder for Logo
    const getLogoUrl = () => {
        if (!settings?.SACCO_LOGO) return null;
        if (settings.SACCO_LOGO.startsWith('http')) return settings.SACCO_LOGO;

        const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8082';
        const path = settings.SACCO_LOGO.startsWith('/') ? settings.SACCO_LOGO : `/${settings.SACCO_LOGO}`;
        return `${baseUrl}${path}`;
    };

    const logoUrl = getLogoUrl();

    useEffect(() => {
        if (isOpen) {
            setStep(1);
            setPaymentStatus('idle');
            setPhone('');
            fetchFee();
        }
        return () => stopPolling();
    }, [isOpen]);

    const stopPolling = () => {
        if (pollingInterval.current) {
            clearInterval(pollingInterval.current);
            pollingInterval.current = null;
        }
    };

    // ✅ FIX: Use correct endpoint for fee
    const fetchFee = async () => {
        try {
            // Corrected Path: /api/settings/{key}
            const res = await api.get('/api/settings/LOAN_APPLICATION_FEE');
            const val = res.data.data || res.data;
            if (val) setFeeAmount(Number(val));
        } catch (e) {
            console.warn("Using default fee 500 (Setting not found)");
        }
    };

    const handlePay = async (e) => {
        e.preventDefault();

        if (!loan || !loan.id) {
            setPaymentStatus('failed');
            setStatusMessage("Application not initialized.");
            return;
        }

        setStep(2);
        setPaymentStatus('processing');
        setStatusMessage('Sending M-Pesa request...');
        pollingAttempts.current = 0;

        try {
            const payload = { phoneNumber: phone, draftId: loan.id };
            const res = await api.post('/api/payments/mpesa/pay-loan-fee', payload);

            if (res.data.success) {
                if (res.data.data.status === 'COMPLETED') {
                     confirmLoanFee(res.data.data.checkoutRequestId);
                     return;
                }
                setStatusMessage('Check your phone to enter PIN...');
                startPolling(res.data.data.checkoutRequestId);
            } else {
                setPaymentStatus('failed');
                setStatusMessage(res.data.message || 'Failed to initiate payment');
            }
        } catch (e) {
            console.error("Payment Init Error:", e);
            setPaymentStatus('failed');
            setStatusMessage('Network connection failed.');
        }
    };

    const startPolling = (reqId) => {
        stopPolling();

        pollingInterval.current = setInterval(async () => {
            pollingAttempts.current += 1;

            if (pollingAttempts.current > MAX_ATTEMPTS) {
                stopPolling();
                setPaymentStatus('failed');
                setStatusMessage('Payment timed out. If you paid, please contact support.');
                return;
            }

            try {
                const res = await api.get(`/api/payments/mpesa/check-status/${reqId}`);
                const status = res.data.data.status;

                if (status === 'COMPLETED') {
                    stopPolling();
                    confirmLoanFee(reqId);
                }
                else if (status === 'FAILED' || status === 'CANCELLED') {
                    stopPolling();
                    setPaymentStatus('failed');
                    setStatusMessage(res.data.data.message);
                }
            } catch (e) {
                // Ignore transient errors
            }
        }, 5000);
    };

    const confirmLoanFee = async (referenceCode) => {
        try {
            await api.post(`/api/loans/drafts/${loan.id}/pay-fee`, { referenceCode });
            setPaymentStatus('verified');
            setTimeout(() => {
                onSuccess();
            }, 2000);
        } catch (e) {
            console.error("System Update Failed:", e);
            setPaymentStatus('failed');
            setStatusMessage("Payment successful, but system update failed. Contact support.");
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-in fade-in">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden flex flex-col border border-slate-200">
                <div className="bg-slate-50 border-b border-slate-100 p-4 flex justify-between items-center">
                    <h3 className="font-bold flex items-center gap-3 text-slate-800">
                        {logoUrl ? (
                            <img
                                src={logoUrl}
                                alt="Logo"
                                className="h-8 w-auto max-w-[100px] object-contain"
                            />
                        ) : (
                            <ShieldCheck size={24} className="text-emerald-600"/>
                        )}
                        <span className="text-sm uppercase tracking-wider">Application Fee</span>
                    </h3>
                    <button onClick={onClose} className="p-1 hover:bg-slate-200 rounded-full transition"><X size={20} className="text-slate-400"/></button>
                </div>

                <div className="p-6">
                    {step === 1 && (
                        <form onSubmit={handlePay} className="space-y-6">
                            <div className="bg-emerald-50 p-4 rounded-xl border border-emerald-100 text-center">
                                <p className="text-emerald-600 text-xs font-bold uppercase tracking-widest mb-1">
                                    Fee for {loan?.draftReference || 'Application'}
                                </p>
                                <h2 className="text-3xl font-black text-slate-800 tracking-tight">KES {Number(feeAmount).toLocaleString()}</h2>
                                <p className="text-xs text-slate-500 mt-1">Non-refundable processing fee</p>
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-slate-500 mb-1 ml-1">M-Pesa Number</label>
                                <div className="relative">
                                    <div className="absolute left-3 top-1/2 -translate-y-1/2 w-8">
                                        <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/1/15/M-PESA_LOGO-01.svg/320px-M-PESA_LOGO-01.svg.png" alt="M-Pesa" className="w-8 h-auto object-contain"/>
                                    </div>
                                    <input
                                        type="tel"
                                        placeholder="07XX XXX XXX"
                                        required
                                        className="w-full pl-14 pr-4 py-3 bg-white border border-slate-200 rounded-xl font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-emerald-500 transition-all"
                                        value={phone}
                                        onChange={e => setPhone(e.target.value)}
                                    />
                                </div>
                            </div>
                            <button type="submit" className="w-full bg-slate-900 hover:bg-emerald-600 text-white font-bold py-3.5 rounded-xl flex items-center justify-center gap-2 transition-all shadow-lg active:scale-95">
                                Pay Now <ArrowRight size={18}/>
                            </button>
                        </form>
                    )}

                    {step === 2 && (
                        <div className="text-center py-8 space-y-6 animate-in zoom-in-95 duration-300">
                            {paymentStatus === 'processing' && (
                                <div className="flex flex-col items-center">
                                    <BrandedSpinner size="large" showTagline={false} borderColor="border-emerald-500" />
                                    <div className="mt-6">
                                        <h3 className="text-lg font-bold text-slate-800">Check Your Phone</h3>
                                        <p className="text-slate-500 text-sm mt-1">{statusMessage}</p>
                                        <p className="text-xs text-slate-400 mt-4">Time remaining: {Math.ceil((MAX_ATTEMPTS - pollingAttempts.current) * 5 / 60)} min</p>
                                    </div>
                                </div>
                            )}
                            {paymentStatus === 'verified' && (
                                <div className="flex flex-col items-center animate-in zoom-in duration-300">
                                    <div className="w-20 h-20 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mb-6 shadow-inner">
                                        <CheckCircle size={40}/>
                                    </div>
                                    <h3 className="text-xl font-bold text-slate-800">Payment Verified!</h3>
                                    <p className="text-slate-500 text-sm mt-1">Continuing application...</p>
                                </div>
                            )}
                            {paymentStatus === 'failed' && (
                                <div className="flex flex-col items-center">
                                    <div className="w-20 h-20 bg-red-50 text-red-600 rounded-full flex items-center justify-center mb-6">
                                        <AlertCircle size={40}/>
                                    </div>
                                    <h3 className="text-xl font-bold text-slate-800">Payment Failed</h3>
                                    <p className="text-red-600 text-sm mt-2 font-medium px-4 py-2 bg-red-50 rounded-lg">{statusMessage}</p>
                                    <button onClick={() => setStep(1)} className="mt-6 bg-slate-100 hover:bg-slate-200 text-slate-700 px-6 py-2 rounded-lg text-sm font-bold transition">Try Again</button>
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}