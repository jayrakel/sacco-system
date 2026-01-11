import React from 'react';
import { Calendar, Wallet, Clock, CreditCard, AlertTriangle } from 'lucide-react';

export default function ActiveLoanCard({ loan }) {
    if (!loan) return null;

    const getDaysRemaining = (nextDate) => {
        if (!nextDate) return 0;
        const diff = new Date(nextDate) - new Date();
        return Math.ceil(diff / (1000 * 60 * 60 * 24));
    };

    const daysRemaining = getDaysRemaining(loan.nextPaymentDate);

    // Use domain directory fields
    const outstandingBalance = loan.totalOutstandingAmount || 0;
    const weeklyPayment = loan.weeklyRepaymentAmount || 0;
    const arrears = loan.totalArrears || 0;
    const prepaid = loan.totalPrepaid || 0;

    return (
        <div className="bg-gradient-to-br from-indigo-900 to-slate-900 rounded-3xl p-8 text-white shadow-xl relative overflow-hidden">
            <div className="absolute top-0 right-0 w-64 h-64 bg-white/5 rounded-full -mr-16 -mt-16 blur-3xl pointer-events-none"></div>

            <div className="relative z-10 flex flex-col md:flex-row justify-between gap-8">
                {/* Balance Info */}
                <div className="space-y-6 flex-1">
                    <div>
                        <div className="flex items-center gap-2 mb-1 opacity-80">
                            <span className="text-xs font-bold uppercase tracking-widest text-indigo-300">Total Outstanding Balance</span>
                            {arrears > 0 && <span className="bg-red-500/20 text-red-300 px-2 py-0.5 rounded text-[10px] font-bold border border-red-500/30">IN ARREARS</span>}
                        </div>
                        <h1 className="text-4xl md:text-5xl font-black tracking-tight">
                            KES {Number(outstandingBalance).toLocaleString()}
                        </h1>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div className="bg-white/10 p-4 rounded-2xl backdrop-blur-sm border border-white/5">
                            <div className="flex items-center gap-2 mb-2 text-indigo-200">
                                <Calendar size={16}/> <span className="text-xs font-bold">Weekly Due</span>
                            </div>
                            <p className="text-xl font-bold">KES {Number(weeklyPayment).toLocaleString()}</p>
                        </div>
                        <div className={`p-4 rounded-2xl backdrop-blur-sm border ${arrears > 0 ? 'bg-red-500/10 border-red-500/30' : 'bg-emerald-500/10 border-emerald-500/30'}`}>
                            <div className={`flex items-center gap-2 mb-2 ${arrears > 0 ? 'text-red-300' : 'text-emerald-300'}`}>
                                {arrears > 0 ? <AlertTriangle size={16}/> : <Wallet size={16}/>}
                                <span className="text-xs font-bold">{arrears > 0 ? "Arrears" : "Prepaid"}</span>
                            </div>
                            <p className="text-xl font-bold">KES {Number(arrears > 0 ? arrears : prepaid).toLocaleString()}</p>
                        </div>
                    </div>
                </div>

                {/* Timeline & Action */}
                <div className="flex flex-col justify-between min-w-[280px] bg-white/5 p-6 rounded-2xl border border-white/5">
                    <div>
                        <h3 className="text-lg font-bold mb-4 flex items-center gap-2">
                            <Clock size={18} className="text-indigo-400"/> Repayment Countdown
                        </h3>
                        {loan.nextPaymentDate ? (
                            daysRemaining > 0 ? (
                                <div className="text-center py-6">
                                    <span className="text-5xl font-black text-indigo-300">{daysRemaining}</span>
                                    <p className="text-sm font-bold uppercase tracking-widest opacity-60 mt-1">Days Remaining</p>
                                    <p className="text-xs text-indigo-200 mt-2">Due on {loan.nextPaymentDate}</p>
                                </div>
                            ) : (
                                <div className="text-center py-6 bg-red-500/10 rounded-xl border border-red-500/30">
                                    <span className="text-xl font-black text-red-300">PAYMENT DUE!</span>
                                </div>
                            )
                        ) : (
                            <div className="text-center py-6 opacity-60"><p className="text-sm">Schedule Generated</p></div>
                        )}
                    </div>
                    <button onClick={() => alert(`Pay KES ${weeklyPayment} to Paybill: 123456\nAccount: ${loan.loanNumber}`)}
                        className="w-full bg-emerald-500 hover:bg-emerald-400 text-white font-bold py-3 rounded-xl transition-all shadow-lg flex items-center justify-center gap-2">
                        <CreditCard size={18}/> Make Repayment
                    </button>
                </div>
            </div>
        </div>
    );
}