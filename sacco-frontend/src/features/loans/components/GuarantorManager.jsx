import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { Search, Plus, UserCheck, AlertTriangle, CheckCircle, Info, X, Shield, Lock, Send, Clock, BellRing } from 'lucide-react';
import BrandedSpinner from '../../../components/BrandedSpinner';

export default function GuarantorManager({ loan, onSuccess, applicantLimits }) {
    // --- 1. DETERMINE MODE ---
    const status = loan.loanStatus || loan.status || 'DRAFT';
    const isEditable = ['DRAFT', 'PENDING_GUARANTORS'].includes(status);
    const isWaiting = status === 'AWAITING_GUARANTORS';

    // State
    const [searchTerm, setSearchTerm] = useState('');
    const [searchResults, setSearchResults] = useState([]);
    const [isSearching, setIsSearching] = useState(false);
    const [selectedMember, setSelectedMember] = useState(null);
    const [guaranteeAmount, setGuaranteeAmount] = useState('');
    const [guarantors, setGuarantors] = useState([]);
    const [loading, setLoading] = useState(false);
    const [initialLoading, setInitialLoading] = useState(true);
    const [message, setMessage] = useState(null);

    // --- INIT ---
    useEffect(() => {
        fetchGuarantors();
    }, [loan.id]);

    const fetchGuarantors = async () => {
        try {
            const res = await api.get(`/api/loans/${loan.id}/guarantors`);
            if (res.data.success) setGuarantors(res.data.data);
        } catch (e) {
            console.error("Failed to load guarantors", e);
        } finally {
            setInitialLoading(false);
        }
    };

    // --- CALCULATIONS ---
    const principal = Number(loan.principalAmount);
    const mySavings = applicantLimits?.totalDeposits ? Number(applicantLimits.totalDeposits) : 0;
    const selfGuaranteedAmount = Math.min(principal, mySavings);
    const othersGuaranteedAmount = guarantors.reduce((sum, g) => sum + Number(g.guaranteedAmount || 0), 0);
    const totalCovered = selfGuaranteedAmount + othersGuaranteedAmount;
    const remainingGap = Math.max(0, principal - totalCovered);
    const isFullySecured = remainingGap <= 0;

    // --- SEARCH ---
    useEffect(() => {
        if (!isEditable) return;
        const timer = setTimeout(() => {
            if (searchTerm.length >= 3) searchMembers();
            else setSearchResults([]);
        }, 400);
        return () => clearTimeout(timer);
    }, [searchTerm, isEditable]);

    const searchMembers = async () => {
        setIsSearching(true);
        try {
            const res = await api.get(`/api/members?search=${searchTerm}`);
            const data = res.data.data?.content || res.data.data || [];
            const filtered = data.filter(m => m.id !== loan.memberId && !guarantors.some(g => g.memberId === m.id));
            setSearchResults(filtered);
        } catch (e) { console.error(e); } finally { setIsSearching(false); }
    };

    // --- ACTIONS ---
    const handleSelectMember = (member) => {
        setSelectedMember(member);
        setSearchTerm('');
        setSearchResults([]);
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
            const newGuarantor = {
                memberId: selectedMember.id,
                firstName: selectedMember.firstName,
                lastName: selectedMember.lastName,
                guaranteedAmount: Number(guaranteeAmount),
                status: 'PENDING',
                // We add a temp ID if needed, but fetchGuarantors corrects it
                id: 'temp-' + Date.now()
            };
            setGuarantors(prev => [...prev, newGuarantor]);
            setSelectedMember(null);
            setGuaranteeAmount('');
            setMessage({ type: 'success', text: `Request sent to ${selectedMember.firstName}!` });
            // Ideally refresh from backend to get real ID for Remind button
            fetchGuarantors();
        } catch (e) {
            setMessage({ type: 'error', text: e.response?.data?.message || 'Failed to add guarantor.' });
        } finally { setLoading(false); }
    };

    const handleRemind = async (guarantorId) => {
        if (!guarantorId || guarantorId.toString().startsWith('temp')) return;
        try {
            const res = await api.post(`/api/loans/guarantors/${guarantorId}/remind`);
            if (res.data.success) {
                setMessage({ type: 'success', text: 'Reminder email sent!' });
            }
        } catch (e) {
            setMessage({ type: 'error', text: 'Failed to send reminder.' });
        }
    };

    const handleSubmitApplication = async () => {
        if (!isFullySecured) return;
        if (!window.confirm("Confirm submission?")) return;
        setLoading(true);
        try {
            await api.post(`/api/loans/${loan.id}/submit`);
            onSuccess();
        } catch (e) {
            setMessage({ type: 'error', text: 'Submission failed.' });
            setLoading(false);
        }
    };

    if (initialLoading) return <div className="p-8 flex justify-center"><BrandedSpinner /></div>;

    return (
        <div className="space-y-6 animate-in fade-in">

            {/* Status Banner */}
            {!isEditable && (
                <div className={`p-4 rounded-xl border flex items-start gap-3 ${isWaiting ? 'bg-orange-50 border-orange-200 text-orange-800' : 'bg-blue-50 border-blue-200 text-blue-800'}`}>
                    {isWaiting ? <Clock className="shrink-0 mt-0.5" size={20}/> : <Info className="shrink-0 mt-0.5" size={20}/>}
                    <div>
                        <p className="font-bold text-sm">{isWaiting ? 'Waiting for Signatures' : 'Application Status'}</p>
                        <p className="text-xs opacity-90 mt-1">
                            {isWaiting
                                ? "This loan is pending approval from the guarantors listed below."
                                : "This application has been submitted and is under review."}
                        </p>
                    </div>
                </div>
            )}

            {/* Coverage Visualizer */}
            <div className="bg-slate-50 border border-slate-200 rounded-xl p-5 space-y-3">
                <div className="flex justify-between items-end">
                    <div>
                        <p className="text-xs font-bold text-slate-500 uppercase">Loan Security</p>
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
                <div className="h-4 bg-slate-200 rounded-full overflow-hidden flex w-full">
                    <div className="bg-blue-500 h-full flex items-center justify-center text-[9px] text-white font-bold" style={{ width: `${(selfGuaranteedAmount / principal) * 100}%` }}>ME</div>
                    <div className="bg-emerald-500 h-full flex items-center justify-center text-[9px] text-white font-bold" style={{ width: `${(othersGuaranteedAmount / principal) * 100}%` }}>OTHERS</div>
                </div>
            </div>

            {/* Messages */}
            {message && <div className={`p-3 rounded-lg text-sm flex items-center gap-2 ${message.type === 'error' ? 'bg-red-50 text-red-600' : 'bg-green-50 text-green-700'}`}>{message.type === 'error' ? <AlertTriangle size={16}/> : <CheckCircle size={16}/>}{message.text}</div>}

            {/* Search (Editable Mode) */}
            {isEditable && (
                !selectedMember ? (
                    <div className="relative z-20">
                        <label className="text-sm font-bold text-slate-700 mb-1 block">Add Guarantor</label>
                        <div className="relative">
                            <Search size={18} className="absolute left-3 top-3.5 text-slate-400"/>
                            <input type="text" className="w-full pl-10 p-3 bg-white border border-slate-300 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none" placeholder="Search member..." value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)}/>
                            {isSearching && <div className="absolute right-3 top-3.5"><BrandedSpinner size="small"/></div>}
                        </div>
                        {searchResults.length > 0 && (
                            <div className="absolute top-full left-0 right-0 mt-1 bg-white border border-slate-200 rounded-xl shadow-xl max-h-56 overflow-y-auto z-50">
                                {searchResults.map(m => (
                                    <button key={m.id} onClick={() => handleSelectMember(m)} className="w-full text-left p-3 hover:bg-slate-50 border-b border-slate-50 flex justify-between items-center group">
                                        <div><p className="font-bold text-sm">{m.firstName} {m.lastName}</p><p className="text-xs text-slate-500">{m.memberNumber}</p></div>
                                        <Plus size={16} className="text-slate-400 group-hover:text-blue-600"/>
                                    </button>
                                ))}
                            </div>
                        )}
                    </div>
                ) : (
                    <form onSubmit={handleSendRequest} className="bg-blue-50 p-4 rounded-xl border border-blue-100 space-y-3 animate-in zoom-in-95">
                        <div className="flex justify-between items-center pb-2 border-b border-blue-200">
                            <span className="font-bold text-blue-900 flex items-center gap-2"><UserCheck size={18}/> {selectedMember.firstName} {selectedMember.lastName}</span>
                            <button type="button" onClick={() => setSelectedMember(null)} className="text-blue-400 hover:text-red-500"><X size={18}/></button>
                        </div>
                        <input type="number" className="w-full p-2 border rounded-lg font-bold" value={guaranteeAmount} onChange={e => setGuaranteeAmount(e.target.value)} max={principal} autoFocus placeholder="0.00"/>
                        <button type="submit" disabled={loading || !guaranteeAmount} className="w-full bg-blue-600 text-white font-bold py-2.5 rounded-lg flex justify-center gap-2">{loading ? <BrandedSpinner size="small" color="border-white"/> : 'Send Request'}</button>
                    </form>
                )
            )}

            {/* List of Guarantors */}
            {guarantors.length > 0 ? (
                <div className="space-y-2 pt-2">
                    <p className="text-xs font-bold uppercase text-slate-400">Guarantor Status ({guarantors.length})</p>
                    {guarantors.map((g, idx) => (
                        <div key={idx} className="flex justify-between items-center p-3 bg-white border border-slate-100 rounded-xl shadow-sm">
                            <div className="flex items-center gap-3">
                                <div className="h-9 w-9 bg-slate-100 rounded-full flex items-center justify-center font-bold text-sm text-slate-500">
                                    {g.firstName ? g.firstName.charAt(0) : '?'}
                                </div>
                                <div>
                                    <p className="text-sm font-bold text-slate-700">{g.firstName} {g.lastName}</p>
                                    <p className="text-xs text-slate-500">KES {Number(g.guaranteedAmount).toLocaleString()}</p>
                                </div>
                            </div>

                            <div className="flex items-center gap-2">
                                {/* ✅ REMIND BUTTON */}
                                {isEditable && g.status === 'PENDING' && (
                                    <button
                                        onClick={() => handleRemind(g.id)}
                                        className="p-1.5 text-blue-600 bg-blue-50 hover:bg-blue-100 rounded-md transition"
                                        title="Resend Notification"
                                    >
                                        <BellRing size={14}/>
                                    </button>
                                )}

                                <div className={`flex items-center gap-1 text-xs font-bold px-2 py-1 rounded-md border
                                    ${g.status === 'ACCEPTED' ? 'bg-emerald-50 text-emerald-700 border-emerald-100' :
                                      g.status === 'REJECTED' ? 'bg-red-50 text-red-700 border-red-100' :
                                      'bg-orange-50 text-orange-700 border-orange-100'}`}>
                                    {g.status || 'Waiting'}
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            ) : !isEditable && (
                <div className="text-center py-8 text-slate-400 text-sm bg-slate-50 rounded-xl border border-dashed border-slate-200">
                    No guarantors added (Self-Guaranteed)
                </div>
            )}

            {/* Final Submit */}
            {isEditable && (
                <div className="pt-2 border-t border-slate-100">
                    <button
                        onClick={handleSubmitApplication}
                        disabled={!isFullySecured || loading}
                        className={`w-full font-bold py-4 rounded-xl flex justify-center items-center gap-2 transition-all shadow-lg ${isFullySecured ? 'bg-slate-900 text-white hover:bg-emerald-600' : 'bg-slate-100 text-slate-400 cursor-not-allowed'}`}
                    >
                        {isFullySecured ? <>Submit for Approval <CheckCircle size={20}/></> : <><Lock size={18}/> Add Guarantors to Submit</>}
                    </button>
                </div>
            )}
        </div>
    );
}