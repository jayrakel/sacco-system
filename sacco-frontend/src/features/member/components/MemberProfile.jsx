import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { User, Phone, Mail, MapPin, Briefcase, Lock, Users, Camera, Save, Plus, Trash2 } from 'lucide-react';
import ChangePassword from '../../../pages/ChangePassword';

export default function MemberProfile() {
    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState('personal'); 
    const [isEditing, setIsEditing] = useState(false);

    // Form Data State
    const [formData, setFormData] = useState({});
    const [selectedFile, setSelectedFile] = useState(null);
    const [previewUrl, setPreviewUrl] = useState(null);

    const BASE_URL = "http://localhost:8080/uploads/";

    useEffect(() => {
        fetchProfile();
    }, []);

    const fetchProfile = async () => {
        try {
            const res = await api.get('/api/members/me');
            if (res.data.success) {
                const data = res.data.data;
                setProfile(data);
                // Initialize form data with correct structure
                setFormData({
                    ...data,
                    // If backend sends null, init empty objects/arrays to avoid crash
                    beneficiaries: data.beneficiaries || [],
                    employmentDetails: data.employmentDetails || {} 
                });
            }
        } catch (e) {
            console.error(e);
        } finally {
            setLoading(false);
        }
    };

    const handleFileChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            setSelectedFile(file);
            setPreviewUrl(URL.createObjectURL(file));
        }
    };

    // --- NESTED UPDATE HANDLERS ---

    const handleEmploymentChange = (field, value) => {
        setFormData({
            ...formData,
            employmentDetails: { ...formData.employmentDetails, [field]: value }
        });
    };

    const handleBeneficiaryChange = (index, field, value) => {
        const updated = [...formData.beneficiaries];
        updated[index][field] = value;
        setFormData({ ...formData, beneficiaries: updated });
    };

    const addBeneficiary = () => {
        setFormData({
            ...formData,
            beneficiaries: [...formData.beneficiaries, { fullName: '', relationship: '', allocation: 0 }]
        });
    };

    const removeBeneficiary = (index) => {
        const updated = formData.beneficiaries.filter((_, i) => i !== index);
        setFormData({ ...formData, beneficiaries: updated });
    };

    const handleSave = async () => {
        const data = new FormData();
        
        // Ensure numbers are numbers for the backend
        const payload = {
            ...formData,
            employmentDetails: {
                ...formData.employmentDetails,
                grossMonthlyIncome: Number(formData.employmentDetails.grossMonthlyIncome),
                netMonthlyIncome: Number(formData.employmentDetails.netMonthlyIncome)
            }
        };

        data.append("member", JSON.stringify(payload));
        if (selectedFile) data.append("file", selectedFile);

        try {
            const res = await api.put('/api/members/me', data, {
                headers: { "Content-Type": "multipart/form-data" }
            });
            if (res.data.success) {
                alert("Profile Updated Successfully!");
                setProfile(res.data.data); // Update view with returned data
                setIsEditing(false);
            }
        } catch (e) {
            alert(e.response?.data?.message || "Update Failed");
        }
    };

    if (loading) return <div className="p-10 text-center text-slate-400">Loading profile...</div>;

    return (
        <div className="max-w-4xl mx-auto space-y-6 animate-in fade-in">
            {/* Header / Avatar Card (Same as before) */}
            <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 flex flex-col md:flex-row items-center gap-6">
                <div className="relative group">
                    <div className="w-28 h-28 rounded-full bg-slate-100 overflow-hidden border-4 border-white shadow-lg">
                        {previewUrl || profile.profileImageUrl ? (
                            <img src={previewUrl || `${BASE_URL}${profile.profileImageUrl}`} alt="Profile" className="w-full h-full object-cover" />
                        ) : (
                            <div className="w-full h-full flex items-center justify-center text-3xl font-bold text-slate-400">
                                {profile.firstName?.charAt(0)}{profile.lastName?.charAt(0)}
                            </div>
                        )}
                    </div>
                    {isEditing && (
                        <label className="absolute bottom-0 right-0 bg-indigo-600 text-white p-2 rounded-full cursor-pointer hover:bg-indigo-700 shadow-md transition">
                            <Camera size={16}/>
                            <input type="file" className="hidden" accept="image/*" onChange={handleFileChange}/>
                        </label>
                    )}
                </div>

                <div className="text-center md:text-left flex-1">
                    <h2 className="text-2xl font-bold text-slate-800">{profile.firstName} {profile.lastName}</h2>
                    <p className="text-slate-500 text-sm mb-3">{profile.email}</p>
                    <div className="flex items-center gap-3 justify-center md:justify-start">
                        <span className="bg-slate-100 px-3 py-1 rounded-full text-xs font-bold border">#{profile.memberNumber}</span>
                    </div>
                </div>

                {!isEditing ? (
                    <button onClick={() => setIsEditing(true)} className="bg-slate-900 text-white px-6 py-2 rounded-xl text-sm font-bold shadow-lg hover:bg-slate-800 transition">
                        Edit Profile
                    </button>
                ) : (
                    <div className="flex gap-2">
                        <button onClick={() => {setIsEditing(false); setFormData(profile)}} className="border border-slate-300 px-4 py-2 rounded-xl text-sm font-bold hover:bg-slate-50">Cancel</button>
                        <button onClick={handleSave} className="bg-emerald-600 text-white px-6 py-2 rounded-xl text-sm font-bold hover:bg-emerald-700 flex items-center gap-2"><Save size={16}/> Save Changes</button>
                    </div>
                )}
            </div>

            {/* TABS */}
            <div className="flex gap-2 border-b border-slate-200 pb-1 overflow-x-auto">
                {['personal', 'employment', 'beneficiaries', 'security'].map(tab => (
                    <button
                        key={tab}
                        onClick={() => setActiveTab(tab)}
                        className={`px-4 py-2 text-sm font-bold rounded-lg transition capitalize flex items-center gap-2 ${activeTab === tab ? 'bg-indigo-50 text-indigo-700' : 'text-slate-500 hover:text-slate-700'}`}
                    >
                        {tab === 'employment' && <Briefcase size={16}/>}
                        {tab === 'beneficiaries' && <Users size={16}/>}
                        {tab === 'personal' && <User size={16}/>}
                        {tab === 'security' && <Lock size={16}/>}
                        {tab}
                    </button>
                ))}
            </div>

            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6">
                
                {/* 1. PERSONAL */}
                {activeTab === 'personal' && (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <Field label="First Name" value={profile.firstName} disabled />
                        <Field label="Last Name" value={profile.lastName} disabled />
                        <Field label="ID Number" value={profile.nationalId} disabled />
                        <Field label="KRA PIN" value={formData.kraPin} onChange={v => setFormData({...formData, kraPin: v})} isEditing={isEditing} />
                        <EditableField label="Email" value={formData.email} onChange={v => setFormData({...formData, email: v})} isEditing={isEditing} icon={Mail} />
                        <EditableField label="Phone" value={formData.phoneNumber} onChange={v => setFormData({...formData, phoneNumber: v})} isEditing={isEditing} icon={Phone} />
                        <div className="md:col-span-2">
                            <EditableField label="Address" value={formData.address} onChange={v => setFormData({...formData, address: v})} isEditing={isEditing} icon={MapPin} />
                        </div>
                    </div>
                )}

                {/* 2. EMPLOYMENT (NEW) */}
                {activeTab === 'employment' && (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <EditableField label="Employer Name" value={formData.employmentDetails.employerName} onChange={v => handleEmploymentChange('employerName', v)} isEditing={isEditing} icon={Briefcase} />
                        <EditableField label="Staff Number" value={formData.employmentDetails.staffNumber} onChange={v => handleEmploymentChange('staffNumber', v)} isEditing={isEditing} />
                        <EditableField label="Gross Income" value={formData.employmentDetails.grossMonthlyIncome} onChange={v => handleEmploymentChange('grossMonthlyIncome', v)} isEditing={isEditing} />
                        <EditableField label="Net Income" value={formData.employmentDetails.netMonthlyIncome} onChange={v => handleEmploymentChange('netMonthlyIncome', v)} isEditing={isEditing} />
                        <div className="md:col-span-2 border-t border-slate-100 pt-4">
                            <h4 className="text-xs font-bold text-slate-400 uppercase mb-4">Bank Details</h4>
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                <EditableField label="Bank Name" value={formData.employmentDetails.bankName} onChange={v => handleEmploymentChange('bankName', v)} isEditing={isEditing} />
                                <EditableField label="Account Number" value={formData.employmentDetails.bankAccountNumber} onChange={v => handleEmploymentChange('bankAccountNumber', v)} isEditing={isEditing} />
                            </div>
                        </div>
                    </div>
                )}

                {/* 3. BENEFICIARIES (NEW LIST) */}
                {activeTab === 'beneficiaries' && (
                    <div className="space-y-4">
                        {isEditing && (
                            <div className="flex justify-end">
                                <button type="button" onClick={addBeneficiary} className="text-xs bg-indigo-50 text-indigo-700 px-3 py-1.5 rounded-lg font-bold flex items-center gap-1">
                                    <Plus size={14}/> Add New
                                </button>
                            </div>
                        )}

                        {formData.beneficiaries.map((b, idx) => (
                            <div key={idx} className="bg-slate-50 p-4 rounded-xl border border-slate-200 relative">
                                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                                    <EditableField label="Name" value={b.fullName} onChange={v => handleBeneficiaryChange(idx, 'fullName', v)} isEditing={isEditing} />
                                    <EditableField label="Relation" value={b.relationship} onChange={v => handleBeneficiaryChange(idx, 'relationship', v)} isEditing={isEditing} />
                                    <EditableField label="Phone" value={b.phoneNumber} onChange={v => handleBeneficiaryChange(idx, 'phoneNumber', v)} isEditing={isEditing} />
                                    <EditableField label="Allocation %" value={b.allocation} onChange={v => handleBeneficiaryChange(idx, 'allocation', v)} isEditing={isEditing} />
                                </div>
                                {isEditing && (
                                    <button onClick={() => removeBeneficiary(idx)} className="absolute -top-2 -right-2 bg-white border border-rose-200 text-rose-500 p-1 rounded-full hover:bg-rose-50"><Trash2 size={12}/></button>
                                )}
                            </div>
                        ))}
                    </div>
                )}

                {/* 4. SECURITY */}
                {activeTab === 'security' && <ChangePassword />}
            </div>
        </div>
    );
}

// Helpers
const Field = ({ label, value, disabled }) => (
    <div className={`p-3 rounded-xl border ${disabled ? 'bg-slate-100 border-slate-200' : 'bg-white border-slate-300'}`}>
        <label className="text-[10px] font-bold text-slate-400 uppercase mb-1">{label}</label>
        <div className="font-bold text-slate-700 text-sm">{value || 'N/A'}</div>
    </div>
);

const EditableField = ({ label, value, onChange, isEditing, icon: Icon }) => (
    <div>
        <label className="text-[10px] font-bold text-slate-500 uppercase mb-1 flex items-center gap-1">
            {Icon && <Icon size={10}/>} {label}
        </label>
        {isEditing ? (
            <input
                type="text"
                className="w-full p-3 border border-indigo-200 rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none bg-indigo-50/30 font-bold text-slate-800 text-sm"
                value={value || ''}
                onChange={(e) => onChange(e.target.value)}
            />
        ) : (
            <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl font-bold text-slate-700 text-sm min-h-[46px] flex items-center">
                {value || 'N/A'}
            </div>
        )}
    </div>
);