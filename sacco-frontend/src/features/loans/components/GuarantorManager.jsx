import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { Search, Plus, UserCheck, AlertTriangle, CheckCircle, Info, X, Shield, Lock, Send } from 'lucide-react';
import BrandedSpinner from '../../../components/BrandedSpinner';

export default function GuarantorManager({ loan, onSuccess, applicantLimits }) {
    // Search State
    const [searchTerm, setSearchTerm] = useState('');
    const [searchResults, setSearchResults] = useState([]);
    const [isSearching, setIsSearching] = useState(false);

    // Selection State
    const [selectedMember, setSelectedMember] = useState(null);
    const [guaranteeAmount, setGuaranteeAmount] = useState('');

    // List & Status State
    const [guarantors, setGuarantors] = useState([]);
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState(null);

    // --- 1. SMART COVERAGE LOGIC ---

    // A. How much is the loan?
    const principal = Number(loan.principalAmount);

    // B. How much does the APPLICANT cover themselves? (Self-Guarantee)
    // ✅ FIX: Use the actual totalDeposits passed from the backend.
    // We no longer guess by dividing maxEligibleAmount by 3.
    const mySavings = applicantLimits?.totalDeposits
        ? Number(applicantLimits.totalDeposits)
        : 0;

    // The applicant guarantees their own loan up to their savings amount
    // (e.g., If Loan is 50k and Savings are 80k, Self-Guarantee is 50k)
    // (e.g., If Loan is 100k and Savings are 80k, Self-Guarantee is 80k)
    const selfGuaranteedAmount = Math.min(principal, mySavings);

    // C. How much have OTHERS pledged?
    const othersGuaranteedAmount = guarantors.reduce((sum, g) => sum + Number(g.guaranteedAmount || 0), 0);

    // D. What is the GAP?
    const totalCovered = selfGuaranteedAmount + othersGuaranteedAmount;
    const remainingGap = Math.max(0, principal - totalCovered);
    const isFullySecured = remainingGap <= 0;


    // --- 2. SEARCH LOGIC ---
    useEffect(() => {
        const timer = setTimeout(() => {
            if (searchTerm.length >= 3) searchMembers();
            else setSearchResults([]);
        }, 400); // Debounce
        return () => clearTimeout(timer);
    }, [searchTerm]);

    const searchMembers = async () => {
        setIsSearching(true);
        try {
            const res = await api.get(`/api/members?search=${searchTerm}`);
            const data = res.data.data?.content || res.data.data || [];
            // Filter: Remove self and already added guarantors
            const filtered = data.filter(m => m.id !== loan.memberId && !guarantors.some(g => g.memberId === m.id));
            setSearchResults(filtered);
        } catch (e) {
            console.error(e);
        } finally {
            setIsSearching(false);
        }
    };

    // --- 3. ACTIONS ---

    const handleSelectMember = (member) => {
        setSelectedMember(member);
        setSearchTerm('');
        setSearchResults([]);
        // Auto-fill: Suggest the remaining gap (or 0 if fully covered but they want extra security)
        setGuaranteeAmount(remainingGap > 0 ? remainingGap : '');
        setMessage(null);
    };

    const handleSendRequest = async (e) => {
        e.preventDefault();
        if (!selectedMember || !guaranteeAmount) return;

        setLoading(true);
        setMessage(null);

        try {
            await api.post(`/api/loans/${loan.id}/guarantors`, {
                memberId: selectedMember.id,
                amount: Number(guaranteeAmount)
            });

            // Update local state to reflect the new pledge
            setGuarantors(prev => [...prev, {
                ...selectedMember,
                memberId: selectedMember.id,
                guaranteedAmount: Number(guaranteeAmount),
                status: 'PENDING'
            }]);

            setSelectedMember(null);
            setGuaranteeAmount('');
            setMessage({ type: 'success', text: `Request sent to ${selectedMember.firstName}!` });

        } catch (e) {
            setMessage({ type: 'error', text: e.response?.data?.message || 'Failed to add guarantor.' });
        } finally {
            setLoading(false);
        }
    };

    const handleSubmitApplication = async () => {
        if (!isFullySecured) return;
        if (!window.confirm("Confirm submission? Requests will be finalized and sent for approval.")) return;

        setLoading(true);
        try {
            await api.post(`/api/loans/${loan.id}/submit`);
            onSuccess(); // Close modal, refresh dashboard
        } catch (e) {
            setMessage({ type: 'error', text: 'Submission failed. Please try again.' });
            setLoading(false);
        }
    };

    return (
        <div className="space-y-6 animate-in fade-in">

            {/* --- SMART COVERAGE VISUALIZER --- */}
            <div className="bg-slate-50 border border-slate-200 rounded-xl p-5 space-y-3">
                <div className="flex justify-between items-end">
                    <div>
                        <p className="text-xs font-bold text-slate-500 uppercase tracking-wider">Loan Security Status</p>
                        <p className="text-2xl font-black text-slate-800">KES {principal.toLocaleString()}</p>
                    </div>
                    <div className="text-right">
                        <span className={`text-xs font-bold px-2 py-1 rounded ${isFullySecured ? 'bg-emerald-100 text-emerald-700' : 'bg-orange-100 text-orange-700'}`}>
                            {isFullySecured ? 'Fully Secured' : 'Unsecured Gap'}
                        </span>
                        <p className={`text-lg font-bold ${isFullySecured ? 'text-emerald-600' : 'text-orange-600'}`}>
                            {remainingGap <= 0 ? '✓ Covered' : `- KES ${remainingGap.toLocaleString()}`}
                        </p>
                    </div>
                </div>

                {/* Progress Bar */}
                <div className="h-4 bg-slate-200 rounded-full overflow-hidden flex w-full">
                    {/* 1. Self Guarantee (Blue) */}
                    <div
                        className="bg-blue-500 h-full transition-all duration-500 flex items-center justify-center text-[9px] text-white font-bold"
                        style={{ width: `${(selfGuaranteedAmount / principal) * 100}%` }}
                        title={`Self Covered: ${selfGuaranteedAmount.toLocaleString()}`}
                    >
                        {selfGuaranteedAmount > 0 && "ME"}
                    </div>
                    {/* 2. Others Guarantee (Green) */}
                    <div
                        className="bg-emerald-500 h-full transition-all duration-500 flex items-center justify-center text-[9px] text-white font-bold"
                        style={{ width: `${(othersGuaranteedAmount / principal) * 100}%` }}
                        title={`Guarantors: ${othersGuaranteedAmount.toLocaleString()}`}
                    >
                        {othersGuaranteedAmount > 0 && "OTHERS"}
                    </div>
                </div>

                {/* Legend */}
                <div className="flex gap-4 text-xs text-slate-500 font-medium">
                    <div className="flex items-center gap-1"><div className="w-2 h-2 rounded-full bg-blue-500"></div> My Savings ({selfGuaranteedAmount.toLocaleString()})</div>
                    <div className="flex items-center gap-1"><div className="w-2 h-2 rounded-full bg-emerald-500"></div> Guarantors ({othersGuaranteedAmount.toLocaleString()})</div>
                </div>
            </div>

            {/* Error/Success Messages */}
            {message && (
                <div className={`p-3 rounded-lg text-sm font-medium flex items-start gap-2 ${message.type === 'error' ? 'bg-red-50 text-red-600' : 'bg-green-50 text-green-700'}`}>
                    {message.type === 'error' ? <AlertTriangle size={18} className="shrink-0 mt-0.5"/> : <CheckCircle size={18} className="shrink-0 mt-0.5"/>}
                    {message.text}
                </div>
            )}

            {/* --- GUARANTOR SEARCH --- */}
            {!selectedMember ? (
                <div className="relative z-20">
                    <label className="text-sm font-bold text-slate-700 mb-1 block">Add Guarantor</label>
                    <div className="relative">
                        <Search size={18} className="absolute left-3 top-3.5 text-slate-400"/>
                        <input
                            type="text"
                            className="w-full pl-10 p-3 bg-white border border-slate-300 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none transition shadow-sm"
                            placeholder="Search by Name, Member No, or Phone..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                        {isSearching && <div className="absolute right-3 top-3.5"><BrandedSpinner size="small"/></div>}
                    </div>

                    {/* ✅ SCROLLABLE DROPDOWN (Fixed Height + Scroll) */}
                    {searchResults.length > 0 && (
                        <div className="absolute top-full left-0 right-0 mt-1 bg-white border border-slate-200 rounded-xl shadow-xl max-h-56 overflow-y-auto animate-in fade-in slide-in-from-top-2">
                            {searchResults.map(m => (
                                <button
                                    key={m.id}
                                    onClick={() => handleSelectMember(m)}
                                    className="w-full text-left p-3 hover:bg-slate-50 border-b border-slate-50 last:border-0 flex justify-between items-center group transition"
                                >
                                    <div>
                                        <p className="font-bold text-sm text-slate-800 group-hover:text-blue-700">{m.firstName} {m.lastName}</p>
                                        <p className="text-xs text-slate-500">{m.memberNumber}</p>
                                    </div>
                                    <Plus size={16} className="text-slate-300 group-hover:text-blue-600"/>
                                </button>
                            ))}
                        </div>
                    )}
                </div>
            ) : (
                /* --- AMOUNT INPUT FORM --- */
                <form onSubmit={handleSendRequest} className="bg-blue-50 p-4 rounded-xl border border-blue-100 space-y-4 animate-in zoom-in-95 duration-200">
                    <div className="flex justify-between items-center pb-2 border-b border-blue-200">
                        <span className="font-bold text-blue-900 flex items-center gap-2">
                            <UserCheck size={18}/> {selectedMember.firstName} {selectedMember.lastName}
                        </span>
                        <button type="button" onClick={() => setSelectedMember(null)} className="text-blue-400 hover:text-red-500 transition"><X size={20}/></button>
                    </div>

                    <div>
                        <label className="text-xs font-bold text-blue-700 uppercase mb-1 block">Request Guarantee Amount (KES)</label>
                        <input
                            type="number"
                            className="w-full p-3 rounded-lg border border-blue-200 focus:ring-2 focus:ring-blue-500 outline-none font-bold text-lg text-slate-800"
                            value={guaranteeAmount}
                            onChange={(e) => setGuaranteeAmount(e.target.value)}
                            // Validations: Can't exceed remaining gap (unless gap is 0, then user decides)
                            // Can't exceed loan total
                            max={principal}
                            placeholder="0.00"
                            autoFocus
                        />
                        <p className="text-xs text-blue-600 mt-1">
                            Recommended: KES {remainingGap > 0 ? remainingGap.toLocaleString() : '0'}
                        </p>
                    </div>

                    <button
                        type="submit"
                        disabled={loading || !guaranteeAmount || guaranteeAmount <= 0}
                        className="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-3 rounded-xl flex justify-center items-center gap-2 disabled:opacity-50 transition shadow-md"
                    >
                        {loading ? <BrandedSpinner size="small" color="border-white"/> : <>Send Request <Send size={16}/></>}
                    </button>
                </form>
            )}

            {/* --- LIST OF ADDED GUARANTORS --- */}
            {guarantors.length > 0 && (
                <div className="space-y-2 pt-2">
                    <div className="flex justify-between items-end">
                        <p className="text-xs font-bold uppercase text-slate-400 tracking-wider">Pending Approvals</p>
                        <span className="text-xs bg-slate-100 text-slate-600 px-2 py-0.5 rounded-full font-bold">{guarantors.length}</span>
                    </div>
                    {guarantors.map((g, idx) => (
                        <div key={idx} className="flex justify-between items-center p-3 bg-white border border-slate-100 rounded-xl shadow-sm">
                            <div className="flex items-center gap-3">
                                <div className="h-9 w-9 bg-slate-100 rounded-full flex items-center justify-center font-bold text-sm text-slate-500">
                                    {g.firstName.charAt(0)}
                                </div>
                                <div>
                                    <p className="text-sm font-bold text-slate-700">{g.firstName} {g.lastName}</p>
                                    <p className="text-xs text-slate-500">KES {Number(g.guaranteedAmount).toLocaleString()}</p>
                                </div>
                            </div>
                            <div className="flex items-center gap-1 text-xs font-bold text-orange-600 bg-orange-50 px-2 py-1 rounded-md border border-orange-100">
                                <span className="w-1.5 h-1.5 bg-orange-500 rounded-full animate-pulse"></span> Waiting
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* --- FINAL ACTION BUTTON --- */}
            <div className="pt-2 border-t border-slate-100">
                <button
                    onClick={handleSubmitApplication}
                    disabled={!isFullySecured || loading}
                    className={`w-full font-bold py-4 rounded-xl flex items-center justify-center gap-2 transition-all shadow-lg active:scale-95
                        ${isFullySecured
                            ? 'bg-slate-900 hover:bg-emerald-600 text-white cursor-pointer'
                            : 'bg-slate-100 text-slate-400 cursor-not-allowed border border-slate-200'
                        }
                    `}
                >
                    {isFullySecured ? (
                         <>Submit for Approval <CheckCircle size={20}/></>
                    ) : (
                        <><Lock size={18}/> Add Guarantors to Submit</>
                    )}
                </button>
                {!isFullySecured && (
                    <p className="text-center text-xs text-slate-400 mt-2">
                        You need to cover the remaining KES {remainingGap.toLocaleString()} to submit.
                    </p>
                )}
            </div>
        </div>
    );
}