import React from 'react';
import {
    AlertCircle, CheckCircle, Lock, Unlock,
    TrendingUp, Calendar, CreditCard, ChevronRight,
    ShieldCheck
} from 'lucide-react';

export default function LoanEligibilityWidget({ eligibilityData, settings, onApply }) {
    // 1. Extract Settings & Data
    const MIN_SAVINGS = settings?.MIN_SAVINGS_FOR_LOAN || 5000;
    const MIN_MONTHS = settings?.MIN_MONTHS_MEMBERSHIP || 3;
    const MAX_LOANS = settings?.MAX_ACTIVE_LOANS || 1;

    // 2. Determine Status of Each Requirement
    const hasSavings = Number(eligibilityData?.currentSavings || 0) >= Number(eligibilityData?.requiredSavings || MIN_SAVINGS);
    const hasDuration = Number(eligibilityData?.membershipMonths || 0) >= Number(eligibilityData?.requiredMonths || MIN_MONTHS);
    const hasLoanSlot = Number(eligibilityData?.currentActiveLoans || 0) < Number(eligibilityData?.maxActiveLoans || MAX_LOANS);

    const isEligible = eligibilityData?.eligible;

    // 3. Calculate Progress (0 to 100)
    const requirements = [hasSavings, hasDuration, hasLoanSlot];
    const progress = Math.round((requirements.filter(Boolean).length / requirements.length) * 100);

    return (
        <div className={`relative overflow-hidden rounded-3xl border-2 transition-all duration-700 ease-in-out shadow-lg group ${
            isEligible
                ? "bg-white border-emerald-500/50 shadow-emerald-500/10"
                : "bg-white border-slate-100 shadow-slate-200"
        }`}>

            {/* --- BACKGROUND DECORATION (Surprise Element) --- */}
            {isEligible && (
                <div className="absolute top-0 right-0 w-96 h-96 bg-emerald-500/5 rounded-full blur-3xl -mr-32 -mt-32 pointer-events-none transition-opacity duration-1000"></div>
            )}

            <div className="p-8 relative z-10">

                {/* --- HEADER: LOCK STATUS & PROGRESS --- */}
                <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 mb-8">
                    <div className="flex items-center gap-4">
                        <div className={`p-3 rounded-2xl transition-colors duration-500 ${
                            isEligible ? "bg-emerald-100 text-emerald-600" : "bg-slate-100 text-slate-500"
                        }`}>
                            {isEligible ? <Unlock size={28} className="animate-bounce" /> : <Lock size={28} />}
                        </div>
                        <div>
                            <h2 className="text-xl font-black text-slate-800">
                                {isEligible ? "Loan Access Unlocked" : "Loan Access Locked"}
                            </h2>
                            <p className="text-slate-500 text-sm font-medium">
                                {isEligible
                                    ? "You have met all requirements. You can now apply."
                                    : "Complete the requirements below to unlock loans."}
                            </p>
                        </div>
                    </div>

                    {/* Progress Bar */}
                    <div className="w-full md:w-48">
                        <div className="flex justify-between text-xs font-bold uppercase tracking-wider mb-2">
                            <span className={isEligible ? "text-emerald-600" : "text-slate-400"}>Progress</span>
                            <span className={isEligible ? "text-emerald-600" : "text-slate-800"}>{progress}%</span>
                        </div>
                        <div className="h-3 w-full bg-slate-100 rounded-full overflow-hidden">
                            <div
                                className={`h-full rounded-full transition-all duration-1000 ease-out ${
                                    isEligible ? "bg-emerald-500" : "bg-slate-300"
                                }`}
                                style={{ width: `${progress}%` }}
                            />
                        </div>
                    </div>
                </div>

                {/* --- GRID: THE 3 REQUIREMENTS --- */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">

                    {/* CARD 1: SAVINGS */}
                    <div className={`p-5 rounded-2xl border transition-all duration-300 ${
                        hasSavings ? "bg-emerald-50/50 border-emerald-100" : "bg-slate-50 border-slate-100 opacity-80"
                    }`}>
                        <div className="flex justify-between items-start mb-3">
                            <div className="flex items-center gap-2">
                                <div className={`p-1.5 rounded-lg ${hasSavings ? "bg-emerald-200/50 text-emerald-700" : "bg-slate-200 text-slate-500"}`}>
                                    <TrendingUp size={16} />
                                </div>
                                <span className="text-xs font-black uppercase tracking-widest text-slate-500">Savings</span>
                            </div>
                            {hasSavings
                                ? <CheckCircle size={18} className="text-emerald-500" />
                                : <AlertCircle size={18} className="text-amber-400" />
                            }
                        </div>
                        <p className="text-2xl font-black text-slate-800">KES {Number(eligibilityData?.currentSavings || 0).toLocaleString()}</p>
                        <p className="text-xs text-slate-500 mt-1 font-medium">Target: KES {Number(eligibilityData?.requiredSavings || MIN_SAVINGS).toLocaleString()}</p>
                    </div>

                    {/* CARD 2: MEMBERSHIP TIME */}
                    <div className={`p-5 rounded-2xl border transition-all duration-300 ${
                        hasDuration ? "bg-emerald-50/50 border-emerald-100" : "bg-slate-50 border-slate-100 opacity-80"
                    }`}>
                        <div className="flex justify-between items-start mb-3">
                            <div className="flex items-center gap-2">
                                <div className={`p-1.5 rounded-lg ${hasDuration ? "bg-emerald-200/50 text-emerald-700" : "bg-slate-200 text-slate-500"}`}>
                                    <Calendar size={16} />
                                </div>
                                <span className="text-xs font-black uppercase tracking-widest text-slate-500">History</span>
                            </div>
                            {hasDuration
                                ? <CheckCircle size={18} className="text-emerald-500" />
                                : <AlertCircle size={18} className="text-amber-400" />
                            }
                        </div>
                        <p className="text-2xl font-black text-slate-800">{eligibilityData?.membershipMonths || 0} <span className="text-sm font-bold text-slate-400">Months</span></p>
                        <p className="text-xs text-slate-500 mt-1 font-medium">Required: {eligibilityData?.requiredMonths || MIN_MONTHS} Months</p>
                    </div>

                    {/* CARD 3: ACTIVE LOANS */}
                    <div className={`p-5 rounded-2xl border transition-all duration-300 ${
                        hasLoanSlot ? "bg-emerald-50/50 border-emerald-100" : "bg-red-50 border-red-100"
                    }`}>
                        <div className="flex justify-between items-start mb-3">
                            <div className="flex items-center gap-2">
                                <div className={`p-1.5 rounded-lg ${hasLoanSlot ? "bg-emerald-200/50 text-emerald-700" : "bg-slate-200 text-slate-500"}`}>
                                    <CreditCard size={16} />
                                </div>
                                <span className="text-xs font-black uppercase tracking-widest text-slate-500">Limits</span>
                            </div>
                            {hasLoanSlot
                                ? <CheckCircle size={18} className="text-emerald-500" />
                                : <AlertCircle size={18} className="text-red-400" />
                            }
                        </div>
                        <p className="text-2xl font-black text-slate-800">{eligibilityData?.currentActiveLoans || 0} <span className="text-slate-300 text-lg">/</span> {eligibilityData?.maxActiveLoans || MAX_LOANS}</p>
                        <p className="text-xs text-slate-500 mt-1 font-medium">Active Loans</p>
                    </div>
                </div>

                {/* --- FOOTER: THE ACTION --- */}
                <div className="mt-8 pt-8 border-t border-slate-100 flex flex-col md:flex-row justify-between items-center gap-4">

                    {/* Status Text */}
                    <div className="flex items-center gap-2">
                        {isEligible ? (
                            <span className="flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-100 text-emerald-700 text-xs font-bold uppercase tracking-widest">
                                <ShieldCheck size={14} /> Eligibility Verified
                            </span>
                        ) : (
                            <span className="px-3 py-1 rounded-full bg-slate-100 text-slate-500 text-xs font-bold uppercase tracking-widest">
                                Verification Pending
                            </span>
                        )}
                    </div>

                    {/* THE MAGIC BUTTON */}
                    <button
                        onClick={isEligible ? onApply : undefined}
                        disabled={!isEligible}
                        className={`
                            relative overflow-hidden px-8 py-4 rounded-xl font-black text-sm uppercase tracking-widest transition-all duration-300 transform
                            ${isEligible
                                ? "bg-indigo-600 text-white hover:bg-indigo-500 hover:scale-105 shadow-xl shadow-indigo-200 cursor-pointer"
                                : "bg-slate-100 text-slate-400 cursor-not-allowed border border-slate-200"
                            }
                        `}
                    >
                        <div className="relative z-10 flex items-center gap-2">
                            {isEligible ? "Start Application" : "Requirements Not Met"}
                            {isEligible && <ChevronRight size={18} />}
                        </div>

                        {/* Button Shimmer Effect for "Surprise" */}
                        {isEligible && (
                            <div className="absolute top-0 left-0 w-full h-full bg-gradient-to-r from-transparent via-white/20 to-transparent -translate-x-full animate-[shimmer_2s_infinite]"></div>
                        )}
                    </button>
                </div>

            </div>
        </div>
    );
}