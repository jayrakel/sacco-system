import React, { useState } from 'react';
import { X, CreditCard, Lock, CheckCircle, Loader2 } from 'lucide-react';

export default function LoanFeePaymentModal({ isOpen, onClose, onSuccess, loan }) {
    const [processing, setProcessing] = useState(false);

    // Hardcoded fee for now (In real app, fetch from loan product settings)
    const FEE_AMOUNT = 500;

    const handlePay = async () => {
        setProcessing(true);

        // SIMULATION: Call backend to process fee payment
        // In production: await loanService.payFee(loan.id)
        setTimeout(() => {
            setProcessing(false);
            onSuccess(loan); // Proceed to next step
        }, 1500);
    };

    if (!isOpen || !loan) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-indigo-900/80 backdrop-blur-sm animate-in fade-in">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden text-center relative">

                {/* Decorative Background */}
                <div className="absolute top-0 w-full h-32 bg-gradient-to-b from-indigo-50 to-white z-0"></div>

                <div className="relative z-10 p-8">
                    <div className="w-16 h-16 bg-white rounded-full shadow-lg mx-auto flex items-center justify-center mb-6 text-indigo-600 border-4 border-indigo-50">
                        <Lock size={32} />
                    </div>

                    <h2 className="text-2xl font-black text-slate-800 mb-2">Application Fee Required</h2>
                    <p className="text-slate-500 mb-8">
                        To process your loan application
                        <span className="font-mono font-bold text-slate-700 bg-slate-100 px-2 py-0.5 rounded mx-1">
                            {loan.loanNumber}
                        </span>,
                        a standard processing fee is required.
                    </p>

                    <div className="bg-slate-50 rounded-2xl p-6 border border-slate-100 mb-8">
                        <p className="text-xs font-bold uppercase tracking-widest text-slate-400 mb-1">Total Due</p>
                        <p className="text-4xl font-black text-slate-800">KES {FEE_AMOUNT}</p>
                    </div>

                    <button
                        onClick={handlePay}
                        disabled={processing}
                        className="w-full bg-emerald-500 hover:bg-emerald-600 text-white py-4 rounded-xl font-bold text-lg shadow-xl shadow-emerald-200 flex items-center justify-center gap-2 transition-transform transform active:scale-95"
                    >
                        {processing ? <Loader2 className="animate-spin" /> : <CreditCard size={20} />}
                        {processing ? "Processing..." : "Pay Now & Continue"}
                    </button>

                    <button onClick={onClose} className="mt-4 text-sm text-slate-400 font-medium hover:text-slate-600">
                        Cancel & Pay Later
                    </button>
                </div>
            </div>
        </div>
    );
}