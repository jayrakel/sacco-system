import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { X, CheckCircle, Calculator, UserPlus, Search, ArrowRight, ArrowLeft, Users, AlertCircle } from 'lucide-react';

export default function LoanApplicationModal({ isOpen, onClose, onSuccess, user }) {
    const [step, setStep] = useState(1);
    const [loading, setLoading] = useState(false);
    const [loanId, setLoanId] = useState(null); // Created in Step 1

    // Step 1 Data
    const [products, setProducts] = useState([]);
    const [formData, setFormData] = useState({ productId: '', amount: '', duration: '', durationUnit: 'MONTHS' });
    const [calculatedRepayment, setCalculatedRepayment] = useState(null);

    // Step 2 Data
    const [members, setMembers] = useState([]); // All potential guarantors
    const [searchTerm, setSearchTerm] = useState('');
    const [selectedGuarantors, setSelectedGuarantors] = useState([]); // Local list before saving
    const [guaranteeAmount, setGuaranteeAmount] = useState('');
    const [activeGuarantorId, setActiveGuarantorId] = useState(null); // Currently selected member ID

    useEffect(() => {
        if (isOpen) {
            setStep(1);
            setLoanId(null);
            setSelectedGuarantors([]);
            fetchProducts();
            fetchMembers();
        }
    }, [isOpen]);

    const fetchProducts = async () => {
        try {
            const res = await api.get('/api/loans/products');
            if (res.data.success) setProducts(res.data.data);
        } catch (e) { console.error("Error loading products", e); }
    };

    const fetchMembers = async () => {
        try {
            const res = await api.get('/api/members/active'); // Ensure this endpoint exists or use /api/members
            if (res.data.success) {
                // Filter out self
                const others = res.data.data.filter(m => m.id !== user?.memberId);
                setMembers(others);
            }
        } catch (e) { console.error("Error loading members", e); }
    };

    // --- STEP 1 LOGIC ---
    const handleCreateDraft = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            const params = new URLSearchParams();
            if(user?.memberId) params.append('memberId', user.memberId);
            params.append('productId', formData.productId);
            params.append('amount', formData.amount);
            params.append('duration', formData.duration);
            params.append('durationUnit', formData.durationUnit);

            const res = await api.post('/api/loans/apply', params);
            if (res.data.success) {
                setLoanId(res.data.data.id);
                setStep(2); // Move to Guarantors
            }
        } catch (e) {
            alert(e.response?.data?.message || "Failed to create draft");
        } finally {
            setLoading(false);
        }
    };

    // --- STEP 2 LOGIC ---
    const handleAddGuarantor = async () => {
        if (!activeGuarantorId || !guaranteeAmount) return;
        setLoading(true);
        try {
            // Send to backend immediately
            await api.post(`/api/loans/${loanId}/guarantors`, {
                memberId: activeGuarantorId,
                guaranteeAmount: guaranteeAmount
            });

            // Update local UI
            const member = members.find(m => m.id === activeGuarantorId);
            setSelectedGuarantors([...selectedGuarantors, { ...member, amount: guaranteeAmount }]);

            // Reset fields
            setActiveGuarantorId(null);
            setGuaranteeAmount('');
            setSearchTerm('');
        } catch (e) {
            alert(e.response?.data?.message || "Failed to add guarantor");
        } finally {
            setLoading(false);
        }
    };

    // --- STEP 3 LOGIC ---
    const handleFinalSubmit = async () => {
        if (selectedGuarantors.length === 0) {
            alert("Please add at least one guarantor.");
            return;
        }
        setLoading(true);
        try {
            // This triggers the notifications to guarantors
            await api.post(`/api/loans/${loanId}/send-requests`);
            alert("Loan Application Sent! Waiting for guarantors.");
            onSuccess();
            onClose();
        } catch (e) {
            alert(e.response?.data?.message || "Submission failed");
        } finally {
            setLoading(false);
        }
    };

    // Calculator Effect
    useEffect(() => {
        const product = products.find(p => p.id === formData.productId);
        if (product && formData.amount && formData.duration) {
            const P = parseFloat(formData.amount);
            const R = (product.interestRate / 100) / (formData.durationUnit === 'WEEKS' ? 52 : 12);
            const N = parseInt(formData.duration);
            let monthly = 0;
            // Simple flat rate approx for preview
            const totalInt = P * (product.interestRate / 100) * (N / (formData.durationUnit === 'WEEKS' ? 52 : 12));
            monthly = (P + totalInt) / N;
            setCalculatedRepayment(monthly.toFixed(2));
        }
    }, [formData, products]);

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4 z-50">
            <div className="bg-white rounded-2xl shadow-xl w-full max-w-2xl overflow-hidden flex flex-col max-h-[90vh]">

                {/* Header */}
                <div className="bg-indigo-900 p-4 flex justify-between items-center text-white shrink-0">
                    <h3 className="font-bold flex items-center gap-2"><Calculator size={18}/> New Loan Application</h3>
                    <button onClick={onClose}><X size={18}/></button>
                </div>

                {/* Progress Bar */}
                <div className="flex justify-between px-8 py-4 bg-slate-50 border-b shrink-0">
                    <StepIndicator num={1} label="Details" current={step} />
                    <StepIndicator num={2} label="Guarantors" current={step} />
                    <StepIndicator num={3} label="Submit" current={step} />
                </div>

                {/* Content Area */}
                <div className="p-6 overflow-y-auto flex-1">

                    {/* STEP 1: DETAILS */}
                    {step === 1 && (
                        <form id="step1-form" onSubmit={handleCreateDraft} className="space-y-4">
                            <div>
                                <label className="block text-xs font-bold text-slate-500 mb-1">Loan Product</label>
                                <select required className="input-field" value={formData.productId} onChange={e => setFormData({...formData, productId: e.target.value})}>
                                    <option value="">-- Select Product --</option>
                                    {products.map(p => <option key={p.id} value={p.id}>{p.name} (Max: {Number(p.maxLimit).toLocaleString()})</option>)}
                                </select>
                            </div>
                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-xs font-bold text-slate-500 mb-1">Amount (KES)</label>
                                    <input type="number" required className="input-field" value={formData.amount} onChange={e => setFormData({...formData, amount: e.target.value})} />
                                </div>
                                <div className="flex gap-2">
                                    <div className="flex-1">
                                        <label className="block text-xs font-bold text-slate-500 mb-1">Duration</label>
                                        <input type="number" required className="input-field" value={formData.duration} onChange={e => setFormData({...formData, duration: e.target.value})} />
                                    </div>
                                    <div className="w-1/3">
                                        <label className="block text-xs font-bold text-slate-500 mb-1">Unit</label>
                                        <select className="input-field" value={formData.durationUnit} onChange={e => setFormData({...formData, durationUnit: e.target.value})}>
                                            <option value="MONTHS">Months</option>
                                            <option value="WEEKS">Weeks</option>
                                        </select>
                                    </div>
                                </div>
                            </div>
                            {calculatedRepayment && (
                                <div className="bg-emerald-50 p-3 rounded-lg border border-emerald-100 flex gap-2 items-center">
                                    <CheckCircle size={16} className="text-emerald-600"/>
                                    <span className="text-sm font-bold text-emerald-800">Est. Installment: KES {Number(calculatedRepayment).toLocaleString()} / {formData.durationUnit.toLowerCase().slice(0, -1)}</span>
                                </div>
                            )}
                        </form>
                    )}

                    {/* STEP 2: GUARANTORS */}
                    {step === 2 && (
                        <div className="space-y-4">
                            <div className="bg-blue-50 p-3 rounded-lg text-xs text-blue-700 flex gap-2">
                                <AlertCircle size={16}/> Select members to guarantee your loan. They will receive a notification to accept or decline.
                            </div>

                            {/* Search & Add */}
                            <div className="flex gap-2 items-end">
                                <div className="flex-1 relative">
                                    <label className="block text-xs font-bold text-slate-500 mb-1">Search Member</label>
                                    <div className="flex items-center border rounded-lg bg-white overflow-hidden focus-within:ring-2 ring-indigo-500">
                                        <Search size={16} className="text-slate-400 ml-2"/>
                                        <select
                                            className="w-full p-2 outline-none bg-transparent text-sm"
                                            value={activeGuarantorId || ''}
                                            onChange={e => setActiveGuarantorId(e.target.value)}
                                        >
                                            <option value="">-- Find Member --</option>
                                            {members.map(m => (
                                                <option key={m.id} value={m.id}>{m.firstName} {m.lastName} ({m.memberNumber})</option>
                                            ))}
                                        </select>
                                    </div>
                                </div>
                                <div className="w-32">
                                    <label className="block text-xs font-bold text-slate-500 mb-1">Amount</label>
                                    <input
                                        type="number" className="input-field" placeholder="0.00"
                                        value={guaranteeAmount} onChange={e => setGuaranteeAmount(e.target.value)}
                                    />
                                </div>
                                <button
                                    onClick={handleAddGuarantor}
                                    disabled={!activeGuarantorId || !guaranteeAmount || loading}
                                    className="bg-indigo-600 text-white p-2.5 rounded-lg hover:bg-indigo-700 disabled:opacity-50"
                                >
                                    <UserPlus size={20}/>
                                </button>
                            </div>

                            {/* List */}
                            <div className="border rounded-xl overflow-hidden">
                                <table className="w-full text-sm text-left">
                                    <thead className="bg-slate-50 font-bold text-slate-500">
                                        <tr><th className="p-3">Member</th><th className="p-3 text-right">Guaranteed</th></tr>
                                    </thead>
                                    <tbody className="divide-y">
                                        {selectedGuarantors.map((g, i) => (
                                            <tr key={i}>
                                                <td className="p-3">{g.firstName} {g.lastName}</td>
                                                <td className="p-3 text-right font-mono">KES {Number(g.amount).toLocaleString()}</td>
                                            </tr>
                                        ))}
                                        {selectedGuarantors.length === 0 && <tr><td colSpan="2" className="p-4 text-center text-slate-400 italic">No guarantors added yet.</td></tr>}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    )}

                    {/* STEP 3: REVIEW */}
                    {step === 3 && (
                        <div className="space-y-6 text-center py-4">
                            <div className="w-16 h-16 bg-indigo-100 text-indigo-600 rounded-full flex items-center justify-center mx-auto mb-4">
                                <Users size={32}/>
                            </div>
                            <h3 className="text-xl font-bold text-slate-800">Ready to Send?</h3>
                            <p className="text-slate-600 max-w-sm mx-auto">
                                You are about to request guarantorship from <span className="font-bold">{selectedGuarantors.length} members</span> for a loan of <span className="font-bold">KES {Number(formData.amount).toLocaleString()}</span>.
                            </p>
                            <p className="text-xs text-slate-400">Once accepted by guarantors, you will be prompted to pay the application fee.</p>
                        </div>
                    )}
                </div>

                {/* Footer Actions */}
                <div className="p-4 border-t bg-slate-50 flex justify-between shrink-0">
                    {step > 1 && (
                        <button onClick={() => setStep(step - 1)} className="px-4 py-2 text-slate-600 font-bold hover:text-slate-900 flex items-center gap-1">
                            <ArrowLeft size={16}/> Back
                        </button>
                    )}
                    <div className="ml-auto">
                        {step === 1 && (
                            <button form="step1-form" type="submit" disabled={loading} className="btn-primary">
                                {loading ? 'Creating...' : 'Next: Guarantors'} <ArrowRight size={16}/>
                            </button>
                        )}
                        {step === 2 && (
                            <button onClick={() => setStep(3)} disabled={selectedGuarantors.length === 0} className="btn-primary">
                                Next: Review <ArrowRight size={16}/>
                            </button>
                        )}
                        {step === 3 && (
                            <button onClick={handleFinalSubmit} disabled={loading} className="btn-primary bg-emerald-600 hover:bg-emerald-700">
                                {loading ? 'Sending...' : 'Send Requests'} <CheckCircle size={16}/>
                            </button>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}

const StepIndicator = ({ num, label, current }) => (
    <div className={`flex items-center gap-2 ${current >= num ? 'text-indigo-700' : 'text-slate-300'}`}>
        <div className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold border ${current >= num ? 'bg-indigo-100 border-indigo-200' : 'border-slate-200'}`}>
            {num}
        </div>
        <span className="text-xs font-bold uppercase hidden sm:block">{label}</span>
    </div>
);