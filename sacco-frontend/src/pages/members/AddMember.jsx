import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../api';
import { User, CreditCard, Briefcase, Users, Save, X, Plus, Trash2 } from 'lucide-react';
import BrandedSpinner from '../../components/BrandedSpinner';

export default function AddMember() {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [activeTab, setActiveTab] = useState('personal'); // personal, employment, beneficiaries

    // 1. Personal Info State
    const [personal, setPersonal] = useState({
        firstName: '', lastName: '', email: '', phoneNumber: '',
        idNumber: '', kraPin: '', address: '', dateOfBirth: ''
    });

    // 2. Employment State (The "Brain" Data)
    const [employment, setEmployment] = useState({
        terms: 'PERMANENT', // Enum default
        employerName: '',
        staffNumber: '',
        stationOrDepartment: '',
        grossMonthlyIncome: '',
        netMonthlyIncome: '',
        bankName: '',
        bankBranch: '',
        bankAccountNumber: ''
    });

    // 3. Beneficiaries State (List)
    const [beneficiaries, setBeneficiaries] = useState([
        { fullName: '', relationship: '', idNumber: '', phoneNumber: '', allocation: 100 }
    ]);

    // 4. Registration Payment State
    const [payment, setPayment] = useState({
        method: 'CASH', reference: 'REG-' + Date.now(), bankCode: '1200'
    });

    const [file, setFile] = useState(null);

    // --- HANDLERS ---

    const handleBeneficiaryChange = (index, field, value) => {
        const updated = [...beneficiaries];
        updated[index][field] = value;
        setBeneficiaries(updated);
    };

    const addBeneficiary = () => {
        setBeneficiaries([...beneficiaries, { fullName: '', relationship: '', idNumber: '', phoneNumber: '', allocation: 0 }]);
    };

    const removeBeneficiary = (index) => {
        if (beneficiaries.length > 1) {
            setBeneficiaries(beneficiaries.filter((_, i) => i !== index));
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);

        // Validate Allocation Total
        const totalAllocation = beneficiaries.reduce((sum, b) => sum + Number(b.allocation), 0);
        if (totalAllocation !== 100) {
            alert(`Beneficiary allocation must total 100%. Current: ${totalAllocation}%`);
            setLoading(false);
            setActiveTab('beneficiaries');
            return;
        }

        const formData = new FormData();
        
        // Construct the complex DTO
        const memberDTO = {
            ...personal,
            employmentDetails: {
                ...employment,
                // Ensure numbers are sent as numbers/strings that backend BigDecimal can parse
                grossMonthlyIncome: parseFloat(employment.grossMonthlyIncome) || 0,
                netMonthlyIncome: parseFloat(employment.netMonthlyIncome) || 0
            },
            beneficiaries: beneficiaries
        };

        formData.append("member", JSON.stringify(memberDTO));
        if (file) formData.append("file", file);
        formData.append("paymentMethod", payment.method);
        formData.append("referenceCode", payment.reference);
        if (payment.method === 'BANK_TRANSFER') formData.append("bankAccountCode", payment.bankCode);

        try {
            const res = await api.post('/api/members', formData, {
                headers: { "Content-Type": "multipart/form-data" }
            });
            if (res.data.success) {
                alert("Member Registered Successfully!");
                navigate('/dashboard/members');
            }
        } catch (error) {
            console.error(error);
            alert(error.response?.data?.message || "Registration Failed");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="max-w-5xl mx-auto space-y-6 animate-in fade-in pb-20">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-2xl font-bold text-slate-800">New Member Registration</h1>
                    <p className="text-slate-500 text-sm">Capture KYC, Employment, and Next of Kin details.</p>
                </div>
                <button onClick={() => navigate(-1)} className="p-2 hover:bg-slate-100 rounded-full transition">
                    <X size={24} className="text-slate-500"/>
                </button>
            </div>

            {/* PROGRESS TABS */}
            <div className="flex gap-2 border-b border-slate-200">
                <TabButton id="personal" icon={User} label="Personal Info" active={activeTab} set={setActiveTab} />
                <TabButton id="employment" icon={Briefcase} label="Employment & Financials" active={activeTab} set={setActiveTab} />
                <TabButton id="beneficiaries" icon={Users} label="Beneficiaries" active={activeTab} set={setActiveTab} />
            </div>

            <form onSubmit={handleSubmit} className="bg-white p-8 rounded-2xl shadow-sm border border-slate-200">
                
                {/* 1. PERSONAL DETAILS */}
                {activeTab === 'personal' && (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <Input label="First Name" value={personal.firstName} onChange={v => setPersonal({...personal, firstName: v})} required />
                        <Input label="Last Name" value={personal.lastName} onChange={v => setPersonal({...personal, lastName: v})} required />
                        <Input label="Email Address" type="email" value={personal.email} onChange={v => setPersonal({...personal, email: v})} required />
                        <Input label="Phone Number" value={personal.phoneNumber} onChange={v => setPersonal({...personal, phoneNumber: v})} required />
                        <Input label="ID / Passport Number" value={personal.idNumber} onChange={v => setPersonal({...personal, idNumber: v})} required />
                        <Input label="KRA PIN" value={personal.kraPin} onChange={v => setPersonal({...personal, kraPin: v})} />
                        <Input label="Date of Birth" type="date" value={personal.dateOfBirth} onChange={v => setPersonal({...personal, dateOfBirth: v})} required />
                        
                        <div className="md:col-span-2">
                            <label className="block text-xs font-bold text-slate-500 uppercase mb-1">Physical Address</label>
                            <textarea 
                                className="w-full p-3 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:ring-2 focus:ring-indigo-500 font-medium"
                                rows="2"
                                value={personal.address}
                                onChange={e => setPersonal({...personal, address: e.target.value})}
                            ></textarea>
                        </div>
                        
                        <div className="md:col-span-2 border-t border-slate-100 pt-6 mt-2">
                            <label className="block text-xs font-bold text-slate-500 uppercase mb-2">Profile Photo (Optional)</label>
                            <input 
                                type="file" 
                                onChange={e => setFile(e.target.files[0])}
                                className="block w-full text-sm text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-bold file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100"
                            />
                        </div>

                        {/* Payment for Registration */}
                        <div className="md:col-span-2 bg-indigo-50 p-6 rounded-xl border border-indigo-100 mt-4">
                            <h3 className="font-bold text-indigo-900 mb-4 flex items-center gap-2"><CreditCard size={18}/> Registration Fee Payment</h3>
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                                <div>
                                    <label className="block text-xs font-bold text-indigo-400 uppercase mb-1">Method</label>
                                    <select 
                                        className="w-full p-2.5 rounded-lg border-indigo-200 focus:ring-indigo-500"
                                        value={payment.method}
                                        onChange={e => setPayment({...payment, method: e.target.value})}
                                    >
                                        <option value="CASH">Cash</option>
                                        <option value="MPESA">M-Pesa</option>
                                        <option value="BANK_TRANSFER">Bank Transfer</option>
                                    </select>
                                </div>
                                <Input label="Reference Code" value={payment.reference} onChange={v => setPayment({...payment, reference: v})} required />
                                {payment.method === 'BANK_TRANSFER' && (
                                     <Input label="Bank GL Code" value={payment.bankCode} onChange={v => setPayment({...payment, bankCode: v})} />
                                )}
                            </div>
                        </div>
                    </div>
                )}

                {/* 2. EMPLOYMENT DETAILS */}
                {activeTab === 'employment' && (
                    <div className="space-y-6">
                        <div className="bg-blue-50 p-4 rounded-xl border border-blue-100 text-sm text-blue-800 mb-4">
                            💡 <strong>Why we need this:</strong> Accurate income details determine loan limits (1/3rd rule) and creditworthiness.
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <div>
                                <label className="block text-xs font-bold text-slate-500 uppercase mb-1">Employment Terms</label>
                                <select 
                                    className="w-full p-3 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:ring-2 focus:ring-indigo-500 font-bold text-slate-700"
                                    value={employment.terms}
                                    onChange={e => setEmployment({...employment, terms: e.target.value})}
                                >
                                    <option value="PERMANENT">Permanent</option>
                                    <option value="CONTRACT">Contract</option>
                                    <option value="CASUAL">Casual</option>
                                    <option value="SELF_EMPLOYED">Self Employed</option>
                                    <option value="RETIRED">Retired</option>
                                </select>
                            </div>
                            <Input label="Employer Name" value={employment.employerName} onChange={v => setEmployment({...employment, employerName: v})} />
                            <Input label="Staff / Payroll No" value={employment.staffNumber} onChange={v => setEmployment({...employment, staffNumber: v})} />
                            <Input label="Station / Department" value={employment.stationOrDepartment} onChange={v => setEmployment({...employment, stationOrDepartment: v})} />
                            
                            <div className="md:col-span-2 border-t border-slate-100 my-2"></div>
                            
                            <Input label="Gross Monthly Income (KES)" type="number" value={employment.grossMonthlyIncome} onChange={v => setEmployment({...employment, grossMonthlyIncome: v})} required />
                            <Input label="Net Monthly Income (KES)" type="number" value={employment.netMonthlyIncome} onChange={v => setEmployment({...employment, netMonthlyIncome: v})} required />
                            
                            <div className="md:col-span-2 border-t border-slate-100 my-2"></div>

                            <Input label="Bank Name" placeholder="e.g. Equity Bank" value={employment.bankName} onChange={v => setEmployment({...employment, bankName: v})} />
                            <Input label="Bank Branch" value={employment.bankBranch} onChange={v => setEmployment({...employment, bankBranch: v})} />
                            <div className="md:col-span-2">
                                <Input label="Bank Account Number" value={employment.bankAccountNumber} onChange={v => setEmployment({...employment, bankAccountNumber: v})} />
                            </div>
                        </div>
                    </div>
                )}

                {/* 3. BENEFICIARIES */}
                {activeTab === 'beneficiaries' && (
                    <div className="space-y-4">
                        <div className="flex justify-between items-center mb-4">
                            <h3 className="font-bold text-slate-700">Designated Next of Kin</h3>
                            <button type="button" onClick={addBeneficiary} className="text-xs bg-indigo-50 text-indigo-700 px-3 py-1.5 rounded-lg font-bold flex items-center gap-1 hover:bg-indigo-100">
                                <Plus size={14}/> Add Beneficiary
                            </button>
                        </div>

                        {beneficiaries.map((b, index) => (
                            <div key={index} className="bg-slate-50 p-4 rounded-xl border border-slate-200 relative group">
                                <div className="grid grid-cols-1 md:grid-cols-5 gap-3">
                                    <Input label="Full Name" value={b.fullName} onChange={v => handleBeneficiaryChange(index, 'fullName', v)} required />
                                    <Input label="Relationship" placeholder="e.g. Spouse" value={b.relationship} onChange={v => handleBeneficiaryChange(index, 'relationship', v)} required />
                                    <Input label="ID Number" value={b.idNumber} onChange={v => handleBeneficiaryChange(index, 'idNumber', v)} />
                                    <Input label="Phone" value={b.phoneNumber} onChange={v => handleBeneficiaryChange(index, 'phoneNumber', v)} required />
                                    <div>
                                        <Input label="Allocation %" type="number" value={b.allocation} onChange={v => handleBeneficiaryChange(index, 'allocation', v)} required />
                                    </div>
                                </div>
                                
                                {beneficiaries.length > 1 && (
                                    <button 
                                        type="button" 
                                        onClick={() => removeBeneficiary(index)}
                                        className="absolute -top-2 -right-2 bg-white border border-rose-200 text-rose-500 p-1.5 rounded-full shadow-sm hover:bg-rose-50 transition"
                                    >
                                        <Trash2 size={14}/>
                                    </button>
                                )}
                            </div>
                        ))}

                        <div className="flex justify-end pt-4">
                             <div className={`text-sm font-bold px-4 py-2 rounded-lg ${beneficiaries.reduce((sum, b) => sum + Number(b.allocation), 0) === 100 ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'}`}>
                                Total Allocation: {beneficiaries.reduce((sum, b) => sum + Number(b.allocation), 0)}%
                             </div>
                        </div>
                    </div>
                )}

                {/* SUBMIT FOOTER */}
                <div className="mt-8 pt-6 border-t border-slate-200 flex justify-between">
                    <button type="button" onClick={() => navigate(-1)} className="px-6 py-2.5 text-slate-500 font-bold hover:bg-slate-50 rounded-xl transition">
                        Cancel
                    </button>
                    <button 
                        type="submit" 
                        disabled={loading}
                        className="bg-indigo-900 text-white px-8 py-2.5 rounded-xl font-bold hover:bg-indigo-800 transition flex items-center gap-2 shadow-lg shadow-indigo-900/20 disabled:opacity-50"
                    >
                        {loading ? <BrandedSpinner size="sm" color="white"/> : <><Save size={18}/> Complete Registration</>}
                    </button>
                </div>
            </form>
        </div>
    );
}

// Sub-components for cleaner code
const TabButton = ({ id, icon: Icon, label, active, set }) => (
    <button
        type="button"
        onClick={() => set(id)}
        className={`px-5 py-3 text-sm font-bold flex items-center gap-2 border-b-2 transition ${
            active === id 
            ? 'border-indigo-600 text-indigo-700 bg-indigo-50/50' 
            : 'border-transparent text-slate-500 hover:text-slate-700 hover:bg-slate-50'
        }`}
    >
        <Icon size={16}/> {label}
    </button>
);

const Input = ({ label, type = "text", value, onChange, required, placeholder }) => (
    <div>
        <label className="block text-xs font-bold text-slate-500 uppercase mb-1">{label} {required && <span className="text-rose-500">*</span>}</label>
        <input 
            type={type}
            required={required}
            placeholder={placeholder}
            className="w-full p-3 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:ring-2 focus:ring-indigo-500 font-bold text-slate-700 transition"
            value={value}
            onChange={(e) => onChange(e.target.value)}
        />
    </div>
);