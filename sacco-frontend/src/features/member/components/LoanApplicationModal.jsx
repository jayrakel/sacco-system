import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { useSettings } from '../../../context/SettingsContext';
import { X, CheckCircle, Calculator, UserPlus, Search, ArrowRight, ArrowLeft, Users, AlertCircle, Loader, Lock, Info } from 'lucide-react';

export default function LoanApplicationModal({ isOpen, onClose, onSuccess, user, resumeLoan }) {
    // ✅ FIXED: Actually consume the context to get the settings object
    const { settings } = useSettings();

    const [step, setStep] = useState(1);
    const [loading, setLoading] = useState(false);
    const [loanId, setLoanId] = useState(null);

    // Eligibility State
    const [eligibility, setEligibility] = useState({ eligible: true, reasons: [] });
    const [checkingEligibility, setCheckingEligibility] = useState(false);

    // Step 1 Data
    const [products, setProducts] = useState([]);
    // ✅ FIXED: Default durationUnit is now 'WEEKS'
    const [formData, setFormData] = useState({ productId: '', amount: '', duration: '', durationUnit: 'WEEKS' });
    const [calculatedRepayment, setCalculatedRepayment] = useState(null);

    // Limits
    const [rawMemberLimit, setRawMemberLimit] = useState(0);
    const [effectiveLimit, setEffectiveLimit] = useState(0);

    // Step 2 Data
    const [members, setMembers] = useState([]);
    const [selectedGuarantors, setSelectedGuarantors] = useState([]);
    const [guaranteeAmount, setGuaranteeAmount] = useState('');
    const [activeGuarantorId, setActiveGuarantorId] = useState('');

    useEffect(() => {
        if (isOpen) {
            console.log("LoanApplicationModal opened with resumeLoan:", resumeLoan);
            fetchProducts();
            fetchMembers();
            fetchLimit();

            if (resumeLoan) {
                // ✅ RESUME MODE - Bypass eligibility check for existing drafts
                console.log("Resume mode - loan ID:", resumeLoan.id);
                setLoanId(resumeLoan.id);
                setEligibility({ eligible: true, reasons: [] }); // Assume eligible if resuming

                if (resumeLoan.principalAmount && Number(resumeLoan.principalAmount) > 0) {
                    console.log("Loan has product details - jumping to step 2");
                    setStep(2);
                    fetchExistingGuarantors(resumeLoan.id);
                } else {
                    console.log("Loan needs product details - staying on step 1");
                    setStep(1);
                    setFormData({ productId: '', amount: '', duration: '', durationUnit: 'WEEKS' });
                }
            } else {
                // ✅ NEW MODE - Check Eligibility
                console.log("New application mode");
                checkEligibility(); // <--- Trigger the check
                setStep(1);
                setLoanId(null);
                setSelectedGuarantors([]);
                setFormData({ productId: '', amount: '', duration: '', durationUnit: 'WEEKS' });
            }
        }
    }, [isOpen, resumeLoan]);

    // Recalculate Effective Limit
    useEffect(() => {
        const product = products.find(p => p.id === formData.productId);
        if (product && rawMemberLimit > 0) {
            const limit = Math.min(rawMemberLimit, product.maxLimit);
            setEffectiveLimit(limit);
        } else {
            setEffectiveLimit(rawMemberLimit);
        }
    }, [formData.productId, rawMemberLimit, products]);

    const checkEligibility = async () => {
        setCheckingEligibility(true);
        try {
            // Ensure no extra parameters are being sent that could cause a 400 validation error
            const res = await api.get('/api/loans/eligibility/check');
            if (res.data.success) {
                setEligibility(res.data.data);
            }
        } catch (e) {
            console.error("Eligibility check failed:", e.response?.data || e.message);
            // Fallback so the UI doesn't hang
            setEligibility({ eligible: false, reasons: ["Unable to verify eligibility at this time."] });
        } finally {
            setCheckingEligibility(false);
        }
    };

    const fetchLimit = async () => {
        try {
            const res = await api.get('/api/loans/limits/check');
            if (res.data.success) {
                setRawMemberLimit(res.data.limit);
            }
        } catch (e) { console.error(e); }
    };

    const fetchProducts = async () => {
        try {
            const res = await api.get('/api/loans/products');
            if (res.data.success) setProducts(res.data.data);
        } catch (e) { console.error(e); }
    };

    const fetchMembers = async () => {
        try {
            const res = await api.get('/api/loans/guarantors/eligible');
            if (res.data.success) {
                setMembers(res.data.data);
            }
        } catch (e) {
            console.error("Failed to fetch eligible guarantors", e);
            setMembers([]);
        }
    };

    const fetchExistingGuarantors = async (id) => {
        try {
            const res = await api.get(`/api/loans/${id}/guarantors`);
            if (res.data.success) {
                const formatted = res.data.data.map(g => ({
                    id: g.memberId,
                    firstName: g.memberName.split(' ')[0],
                    lastName: g.memberName.split(' ').slice(1).join(' ') || '',
                    memberNumber: '...',
                    amount: g.guaranteeAmount
                }));
                setSelectedGuarantors(formatted);
            }
        } catch (e) { console.error(e); }
    };

    // --- STEP 1: CREATE OR UPDATE DRAFT ---
    const handleCreateDraft = async (e) => {
        e.preventDefault();

        if (parseFloat(formData.amount) > effectiveLimit) {
            alert(`Amount cannot exceed your limit of KES ${Number(effectiveLimit).toLocaleString()}`);
            return;
        }

        setLoading(true);
        try {
            const payload = {
                productId: formData.productId,
                amount: formData.amount,
                duration: formData.duration,
                durationUnit: 'WEEKS'
            };

            if (loanId) {
                payload.loanId = loanId;
            }

            const res = await api.post('/api/loans/apply', payload);

            if (res.data.success) {
                setLoanId(res.data.data.id);
                setStep(2);
            }
        } catch (e) {
            alert(e.response?.data?.message || "Failed to process request");
        } finally {
            setLoading(false);
        }
    };

    // --- STEP 2: ADD GUARANTOR ---
    const handleAddGuarantor = async () => {
        if (!activeGuarantorId || !guaranteeAmount) return;

        if (selectedGuarantors.some(g => g.id === activeGuarantorId)) {
            alert("Member already added as guarantor.");
            return;
        }

        setLoading(true);
        try {
            await api.post(`/api/loans/${loanId}/guarantors`, {
                memberId: activeGuarantorId,
                guaranteeAmount: guaranteeAmount
            });

            const member = members.find(m => m.id === activeGuarantorId);
            setSelectedGuarantors([...selectedGuarantors, { ...member, amount: guaranteeAmount }]);

            setActiveGuarantorId('');
            setGuaranteeAmount('');
        } catch (e) {
            alert(e.response?.data?.message || "Failed to add guarantor");
        } finally {
            setLoading(false);
        }
    };

    // --- STEP 3: SEND REQUESTS ---
    const handleFinalSubmit = async () => {
        if (selectedGuarantors.length === 0) {
            alert("Please add at least one guarantor.");
            return;
        }
        setLoading(true);
        try {
            await api.post(`/api/loans/${loanId}/send-requests`);
            alert("Application Submitted! Guarantors have been notified.");
            onSuccess();
            onClose();
        } catch (e) {
            alert(e.response?.data?.message || "Submission failed");
        } finally {
            setLoading(false);
        }
    };

    // Calculator Preview
    useEffect(() => {
        const product = products.find(p => p.id === formData.productId);
        if (product && formData.amount && formData.duration) {
            const P = parseFloat(formData.amount);
            const rate = product.interestRate || 0;
            const divisor = 52;
            const N = parseInt(formData.duration);

            const totalInterest = P * (rate / 100) * (N / divisor);
            const weekly = (P + totalInterest) / N;

            setCalculatedRepayment(weekly.toFixed(2));
        } else {
            setCalculatedRepayment(null);
        }
    }, [formData, products]);

    if (!isOpen) return null;

    // --- RENDER LOGIC: Check Eligibility First ---
    const isBlocked = !checkingEligibility && !eligibility.eligible && !resumeLoan;

    return (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-in fade-in duration-200">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl overflow-hidden flex flex-col max-h-[90vh]">

                {/* Header */}
                <div className="bg-indigo-900 p-5 flex justify-between items-center text-white shrink-0">
                    <div>
                        <h3 className="font-bold text-lg flex items-center gap-2"><Calculator size={20}/> {resumeLoan ? 'Continue Application' : 'New Loan Application'}</h3>
                        <p className="text-indigo-200 text-xs mt-0.5">{resumeLoan ? 'Manage your guarantors' : 'Complete the steps below to apply'}</p>
                    </div>
                    <button onClick={onClose} className="hover:bg-white/10 p-1 rounded-full transition"><X size={20}/></button>
                </div>

                {/* --- CONTENT AREA --- */}

                {/* 1. LOADING STATE */}
                {checkingEligibility && (
                    <div className="p-12 flex flex-col items-center justify-center h-64 text-slate-500">
                        <Loader size={32} className="animate-spin text-indigo-600 mb-4"/>
                        <p>Checking eligibility...</p>
                    </div>
                )}

                {/* 2. BLOCKED STATE (Not Eligible) */}
                {isBlocked && (
                    <div className="p-8 flex flex-col items-center text-center h-full justify-center">
                        <div className="w-16 h-16 bg-red-100 text-red-600 rounded-full flex items-center justify-center mb-4">
                            <AlertCircle size={32}/>
                        </div>
                        <h3 className="text-xl font-bold text-slate-800 mb-2">Not Eligible</h3>

                        {/* ✅ IMPROVEMENT: Display dynamic settings from the Admin context */}
                        <p className="text-slate-500 mb-6">
                            Minimum savings required: <b>KES {Number(settings?.MIN_SAVINGS_FOR_LOAN || 0).toLocaleString()}</b>
                        </p>

                        <div className="w-full bg-red-50 border border-red-100 rounded-xl p-4 text-left mb-6">
                            <h4 className="text-xs font-bold text-red-800 uppercase mb-2">Reasons:</h4>
                            <ul className="space-y-2">
                                 {eligibility.reasons.map((r, i) => (
                                    <li key={i} className="flex gap-2 text-sm text-red-700">
                                        <span className="shrink-0">•</span> {r}
                                    </li>
                                 ))}
                            </ul>
                        </div>

                        <button onClick={onClose} className="bg-slate-100 text-slate-700 px-6 py-2 rounded-lg font-bold hover:bg-slate-200 transition">
                            Close
                        </button>
                    </div>
                )}

                {/* 3. ALLOWED STATE (Eligible or Resuming) */}
                {!checkingEligibility && !isBlocked && (
                    <>
                        {/* Steps */}
                        <div className="flex justify-between px-10 py-4 bg-slate-50 border-b border-slate-100 shrink-0">
                            <StepIndicator num={1} label="Details" current={step} />
                            <div className="w-12 h-0.5 bg-slate-200 self-center mx-2"/>
                            <StepIndicator num={2} label="Guarantors" current={step} />
                            <div className="w-12 h-0.5 bg-slate-200 self-center mx-2"/>
                            <StepIndicator num={3} label="Finish" current={step} />
                        </div>

                        {/* Body */}
                        <div className="p-6 overflow-y-auto flex-1">

                            {/* STEP 1: CONFIGURATION */}
                            {step === 1 && (
                                <form id="step1-form" onSubmit={handleCreateDraft} className="space-y-5">

                                    {/* LIMIT BANNER */}
                                    <div className={`p-4 rounded-xl border flex gap-3 items-center ${effectiveLimit > 0 ? 'bg-indigo-50 border-indigo-100' : 'bg-red-50 border-red-100'}`}>
                                        <div className={`p-2 rounded-full ${effectiveLimit > 0 ? 'bg-indigo-100 text-indigo-600' : 'bg-red-100 text-red-600'}`}>
                                            {effectiveLimit > 0 ? <Lock size={20}/> : <AlertCircle size={20}/>}
                                        </div>
                                        <div>
                                            <p className="text-xs font-bold uppercase text-slate-500">Your Qualifying Limit</p>
                                            <p className={`text-xl font-bold font-mono ${effectiveLimit > 0 ? 'text-indigo-800' : 'text-red-700'}`}>
                                                KES {Number(effectiveLimit).toLocaleString()}
                                            </p>
                                            <div className="text-[10px] text-slate-400 mt-1 flex items-center gap-1">
                                                <Info size={10}/>
                                                {formData.productId
                                                    ? "Based on your savings & product maximum."
                                                    : "Select a product to see final limit."}
                                            </div>
                                        </div>
                                    </div>

                                    <div>
                                        <label className="block text-xs font-bold text-slate-500 uppercase mb-1.5">Select Product</label>
                                        <select
                                            required
                                            className="w-full p-3 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 outline-none transition"
                                            value={formData.productId}
                                            onChange={e => setFormData({...formData, productId: e.target.value})}
                                        >
                                            <option value="">-- Choose Loan Type --</option>
                                            {products.map(p => (
                                                <option key={p.id} value={p.id}>{p.name} (Max: {Number(p.maxLimit).toLocaleString()})</option>
                                            ))}
                                        </select>
                                    </div>

                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                                        <div>
                                            <label className="block text-xs font-bold text-slate-500 uppercase mb-1.5">Amount (KES)</label>
                                            <input
                                                type="number" required min="100"
                                                max={effectiveLimit}
                                                className={`w-full p-3 border rounded-xl outline-none focus:ring-2 ${parseFloat(formData.amount) > effectiveLimit ? 'border-red-300 focus:ring-red-200 text-red-600' : 'border-slate-200 focus:ring-indigo-500'}`}
                                                value={formData.amount}
                                                onChange={e => setFormData({...formData, amount: e.target.value})}
                                            />
                                            {parseFloat(formData.amount) > effectiveLimit && (
                                                <p className="text-xs text-red-500 mt-1 font-bold">Exceeds limit</p>
                                            )}
                                        </div>

                                        <div>
                                            <label className="block text-xs font-bold text-slate-500 uppercase mb-1.5">Duration (Weeks)</label>
                                            <input
                                                type="number" required min="1"
                                                placeholder="e.g. 52"
                                                className="w-full p-3 border border-slate-200 rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none"
                                                value={formData.duration}
                                                onChange={e => setFormData({...formData, duration: e.target.value})}
                                            />
                                        </div>
                                    </div>

                                    {calculatedRepayment && (
                                        <div className="bg-emerald-50 p-4 rounded-xl border border-emerald-100 flex gap-3 items-center animate-in slide-in-from-top-2">
                                            <div className="p-2 bg-emerald-100 text-emerald-600 rounded-full"><CheckCircle size={18}/></div>
                                            <div>
                                                <p className="text-xs text-emerald-800 font-bold uppercase">Estimated Repayment</p>
                                                <p className="text-lg font-bold text-emerald-700 font-mono">
                                                    KES {Number(calculatedRepayment).toLocaleString()} <span className="text-sm font-normal text-emerald-600">/ week</span>
                                                </p>
                                            </div>
                                        </div>
                                    )}
                                </form>
                            )}

                            {/* STEP 2: GUARANTORS */}
                            {step === 2 && (
                                <div className="space-y-5">
                                    <div className="bg-blue-50 p-4 rounded-xl border border-blue-100 flex gap-3">
                                        <AlertCircle className="text-blue-600 shrink-0" size={20}/>
                                        <p className="text-sm text-blue-800">
                                            Search for members to guarantee your loan. They must accept the request for your application to proceed.
                                        </p>
                                    </div>

                                    <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 space-y-3">
                                        <label className="text-xs font-bold text-slate-500 uppercase">Add Guarantor</label>
                                        <div className="flex flex-col sm:flex-row gap-2">
                                            <div className="flex-1 relative">
                                                <select
                                                    className="w-full p-3 border border-slate-300 rounded-lg outline-none focus:ring-2 focus:ring-indigo-500"
                                                    value={activeGuarantorId}
                                                    onChange={e => setActiveGuarantorId(e.target.value)}
                                                >
                                                    <option value="">-- Select Member --</option>
                                                    {members.map(m => (
                                                        <option key={m.id} value={m.id}>{m.firstName} {m.lastName} - {m.memberNumber}</option>
                                                    ))}
                                                </select>
                                            </div>
                                            <div className="w-full sm:w-32">
                                                <input
                                                    type="number" placeholder="Amount"
                                                    className="w-full p-3 border border-slate-300 rounded-lg outline-none focus:ring-2 focus:ring-indigo-500"
                                                    value={guaranteeAmount}
                                                    onChange={e => setGuaranteeAmount(e.target.value)}
                                                />
                                            </div>
                                            <button
                                                onClick={handleAddGuarantor}
                                                disabled={!activeGuarantorId || !guaranteeAmount || loading}
                                                className="bg-indigo-600 text-white px-4 py-2 rounded-lg font-bold hover:bg-indigo-700 disabled:opacity-50 flex items-center justify-center gap-2"
                                            >
                                                {loading ? <Loader size={16} className="animate-spin"/> : <UserPlus size={18}/>} Add
                                            </button>
                                        </div>
                                    </div>

                                    <div className="border border-slate-200 rounded-xl overflow-hidden">
                                        <div className="bg-slate-100 px-4 py-2 border-b border-slate-200 text-xs font-bold text-slate-500 uppercase flex justify-between">
                                            <span>Selected Guarantors</span>
                                            <span>{selectedGuarantors.length} Added</span>
                                        </div>
                                        <div className="divide-y divide-slate-100 max-h-40 overflow-y-auto">
                                            {selectedGuarantors.map((g, i) => (
                                                <div key={i} className="p-3 flex justify-between items-center bg-white hover:bg-slate-50">
                                                    <div>
                                                        <p className="font-bold text-slate-700 text-sm">{g.firstName} {g.lastName}</p>
                                                        <p className="text-xs text-slate-400">{g.memberNumber}</p>
                                                    </div>
                                                    <span className="font-mono font-bold text-slate-800 text-sm bg-slate-100 px-2 py-1 rounded">
                                                        KES {Number(g.amount).toLocaleString()}
                                                    </span>
                                                </div>
                                            ))}
                                            {selectedGuarantors.length === 0 && (
                                                <div className="p-6 text-center text-slate-400 text-sm italic">
                                                    No guarantors selected yet.
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            )}

                            {/* STEP 3: CONFIRMATION */}
                            {step === 3 && (
                                <div className="flex flex-col items-center justify-center h-full py-6 space-y-6">
                                    <div className="w-20 h-20 bg-indigo-50 text-indigo-600 rounded-full flex items-center justify-center animate-bounce">
                                        <Users size={32}/>
                                    </div>
                                    <div className="text-center space-y-2">
                                        <h3 className="text-2xl font-bold text-slate-800">Ready to Submit?</h3>
                                        <p className="text-slate-500 max-w-xs mx-auto">
                                            We will send notifications to your <b>{selectedGuarantors.length} guarantors</b>. Once accepted, you can pay the application fee.
                                        </p>
                                    </div>
                                    <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 w-full max-w-sm">
                                        <div className="flex justify-between text-sm mb-2">
                                            <span className="text-slate-500">Total Guaranteed</span>
                                            <span className="font-bold text-slate-800">KES {selectedGuarantors.reduce((sum, g) => sum + parseFloat(g.amount), 0).toLocaleString()}</span>
                                        </div>
                                    </div>
                                </div>
                            )}
                        </div>

                        {/* Footer */}
                        <div className="p-5 border-t border-slate-200 bg-slate-50 flex justify-between shrink-0">
                            {step > 1 ? (
                                <button
                                    onClick={() => setStep(step - 1)}
                                    className="px-5 py-2.5 text-slate-600 font-bold hover:bg-slate-200 rounded-xl transition flex items-center gap-2"
                                >
                                    <ArrowLeft size={18}/> Back
                                </button>
                            ) : <div></div>}

                            <div className="flex gap-2">
                                {step === 1 && (
                                    <button form="step1-form" type="submit" disabled={loading || parseFloat(formData.amount) > effectiveLimit} className="bg-indigo-900 text-white px-6 py-2.5 rounded-xl font-bold hover:bg-indigo-800 transition shadow-lg shadow-indigo-200 flex items-center gap-2 disabled:opacity-50 disabled:shadow-none">
                                        {loading ? 'Creating Draft...' : 'Next Step'} <ArrowRight size={18}/>
                                    </button>
                                )}
                                {step === 2 && (
                                    <button
                                        onClick={() => setStep(3)}
                                        disabled={selectedGuarantors.length === 0}
                                        className="bg-indigo-900 text-white px-6 py-2.5 rounded-xl font-bold hover:bg-indigo-800 transition shadow-lg shadow-indigo-200 flex items-center gap-2 disabled:opacity-50 disabled:shadow-none"
                                    >
                                        Review <ArrowRight size={18}/>
                                    </button>
                                )}
                                {step === 3 && (
                                    <button
                                        onClick={handleFinalSubmit}
                                        disabled={loading}
                                        className="bg-emerald-600 text-white px-8 py-2.5 rounded-xl font-bold hover:bg-emerald-700 transition shadow-lg shadow-emerald-200 flex items-center gap-2"
                                    >
                                        {loading ? 'Sending...' : 'Send Requests'} <CheckCircle size={18}/>
                                    </button>
                                )}
                            </div>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}

const StepIndicator = ({ num, label, current }) => (
    <div className={`flex flex-col items-center gap-1 ${current >= num ? 'text-indigo-700' : 'text-slate-300'}`}>
        <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold border-2 transition-all ${current >= num ? 'bg-indigo-50 border-indigo-600 scale-110' : 'border-slate-200'}`}>
            {num}
        </div>
        <span className="text-[10px] font-bold uppercase tracking-wide">{label}</span>
    </div>
);