import React from 'react';
import { FileText, ArrowRight, AlertCircle, Clock } from 'lucide-react';

export default function DraftLoanWidget({ draftLoan, onResume }) {
    if (!draftLoan) return null;

    const isUnpaid = !draftLoan.feePaid;

    return (
        <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200 relative overflow-hidden">
            {/* Background Pattern */}
            <div className="absolute top-0 right-0 p-4 opacity-5">
                <FileText size={120} className="text-slate-900" />
            </div>

            <div className="relative z-10">
                <div className="flex items-center gap-2 mb-4">
                    <span className="bg-amber-100 text-amber-700 text-xs font-bold px-3 py-1 rounded-full uppercase tracking-wider flex items-center gap-1">
                        <Clock size={12} /> Application in Progress
                    </span>
                </div>

                <h3 className="text-2xl font-bold text-slate-800 mb-1">
                    {draftLoan.productName || 'Loan Application'}
                </h3>
                <p className="text-slate-500 font-mono text-sm mb-6">
                    Ref: {draftLoan.loanNumber}
                </p>

                <div className="flex gap-4 mb-6">
                    <div className="bg-slate-50 p-3 rounded-xl border border-slate-100 flex-1">
                        <p className="text-xs text-slate-400 font-bold uppercase">Amount</p>
                        <p className="text-lg font-bold text-slate-700">KES {Number(draftLoan.principalAmount).toLocaleString()}</p>
                    </div>
                    <div className="bg-slate-50 p-3 rounded-xl border border-slate-100 flex-1">
                        <p className="text-xs text-slate-400 font-bold uppercase">Status</p>
                        <p className={`text-lg font-bold ${isUnpaid ? 'text-red-600' : 'text-emerald-600'}`}>
                            {isUnpaid ? 'Fee Pending' : 'Drafting'}
                        </p>
                    </div>
                </div>

                <button
                    onClick={() => onResume(draftLoan)}
                    className={`w-full py-3 rounded-xl font-bold flex items-center justify-center gap-2 transition-all ${
                        isUnpaid
                        ? 'bg-amber-500 hover:bg-amber-600 text-white shadow-lg shadow-amber-500/20'
                        : 'bg-slate-900 hover:bg-slate-800 text-white shadow-lg shadow-slate-900/20'
                    }`}
                >
                    {isUnpaid ? (
                        <>Pay Application Fee <AlertCircle size={18}/></>
                    ) : (
                        <>Resume Application <ArrowRight size={18}/></>
                    )}
                </button>
            </div>
        </div>
    );
}