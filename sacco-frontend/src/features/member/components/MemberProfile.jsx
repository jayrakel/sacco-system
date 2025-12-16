import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { User, Phone, Mail, MapPin, Calendar, FileText, Shield, Camera, Save, Lock, Users } from 'lucide-react';
import ChangePassword from '../../../pages/ChangePassword'; // Reusing your existing component

export default function MemberProfile() {
    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState('personal'); // personal, kin, security
    const [isEditing, setIsEditing] = useState(false);

    // Form State
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
                setProfile(res.data.data);
                setFormData(res.data.data);
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

    const handleSave = async () => {
        const data = new FormData();
        data.append("member", JSON.stringify(formData));
        if (selectedFile) data.append("file", selectedFile);

        try {
            const res = await api.put('/api/members/me', data, {
                headers: { "Content-Type": "multipart/form-data" }
            });
            if (res.data.success) {
                alert("Profile Updated Successfully!");
                setProfile(res.data.data);
                setIsEditing(false);
            }
        } catch (e) {
            alert(e.response?.data?.message || "Update Failed");
        }
    };

    if (loading) return <div className="p-10 text-center text-slate-400">Loading profile...</div>;

    return (
        <div className="max-w-4xl mx-auto space-y-6 animate-in fade-in">

            {/* Header / Avatar Card */}
            <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 flex flex-col md:flex-row items-center gap-6">
                <div className="relative group">
                    <div className="w-28 h-28 rounded-full bg-slate-100 overflow-hidden border-4 border-white shadow-lg">
                        {previewUrl || profile.profileImageUrl ? (
                            <img
                                src={previewUrl || `${BASE_URL}${profile.profileImageUrl}`}
                                alt="Profile"
                                className="w-full h-full object-cover"
                            />
                        ) : (
                            <div className="w-full h-full flex items-center justify-center text-3xl font-bold text-slate-400">
                                {profile.firstName?.charAt(0)}{profile.lastName?.charAt(0)}
                            </div>
                        )}
                    </div>
                    {/* Camera Icon Overlay */}
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
                    <div className="flex items-center justify-center md:justify-start gap-3">
                        <span className="bg-slate-100 text-slate-600 px-3 py-1 rounded-full text-xs font-bold border border-slate-200">
                            #{profile.memberNumber}
                        </span>
                        <span className={`px-3 py-1 rounded-full text-xs font-bold uppercase flex items-center gap-1 ${profile.status === 'ACTIVE' ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'}`}>
                            <Shield size={12}/> {profile.status}
                        </span>
                    </div>
                </div>

                {!isEditing ? (
                    <button onClick={() => setIsEditing(true)} className="bg-slate-900 text-white px-6 py-2 rounded-xl text-sm font-bold shadow-lg shadow-slate-900/20 hover:bg-slate-800 transition">
                        Edit Profile
                    </button>
                ) : (
                    <div className="flex gap-2">
                        <button onClick={() => {setIsEditing(false); setPreviewUrl(null); setFormData(profile)}} className="bg-white border border-slate-300 text-slate-600 px-4 py-2 rounded-xl text-sm font-bold hover:bg-slate-50 transition">
                            Cancel
                        </button>
                        <button onClick={handleSave} className="bg-emerald-600 text-white px-6 py-2 rounded-xl text-sm font-bold shadow-lg shadow-emerald-600/20 hover:bg-emerald-700 transition flex items-center gap-2">
                            <Save size={16}/> Save Changes
                        </button>
                    </div>
                )}
            </div>

            {/* Navigation Tabs */}
            <div className="flex gap-2 border-b border-slate-200 pb-1 overflow-x-auto">
                {['personal', 'kin', 'security'].map(tab => (
                    <button
                        key={tab}
                        onClick={() => setActiveTab(tab)}
                        className={`px-4 py-2 text-sm font-bold rounded-lg transition capitalize flex items-center gap-2 ${activeTab === tab ? 'bg-indigo-50 text-indigo-700' : 'text-slate-500 hover:text-slate-700'}`}
                    >
                        {tab === 'personal' && <User size={16}/>}
                        {tab === 'kin' && <Users size={16}/>}
                        {tab === 'security' && <Lock size={16}/>}
                        {tab === 'kin' ? 'Next of Kin' : tab}
                    </button>
                ))}
            </div>

            {/* TAB CONTENT */}
            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 animate-in slide-in-from-bottom-2 duration-300">

                {/* 1. PERSONAL INFO */}
                {activeTab === 'personal' && (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <Field label="First Name" value={profile.firstName} disabled />
                        <Field label="Last Name" value={profile.lastName} disabled />
                        <Field label="ID / Passport" value={profile.idNumber} icon={FileText} disabled />
                        <Field label="KRA PIN" value={profile.kraPin} icon={FileText} disabled />

                        <EditableField
                            label="Email Address"
                            value={formData.email}
                            onChange={v => setFormData({...formData, email: v})}
                            isEditing={isEditing}
                            icon={Mail}
                        />
                        <EditableField
                            label="Phone Number"
                            value={formData.phoneNumber}
                            onChange={v => setFormData({...formData, phoneNumber: v})}
                            isEditing={isEditing}
                            icon={Phone}
                        />
                        <div className="md:col-span-2">
                            <EditableField
                                label="Residential Address"
                                value={formData.address}
                                onChange={v => setFormData({...formData, address: v})}
                                isEditing={isEditing}
                                icon={MapPin}
                            />
                        </div>
                    </div>
                )}

                {/* 2. NEXT OF KIN */}
                {activeTab === 'kin' && (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <EditableField
                            label="Next of Kin Name"
                            value={formData.nextOfKinName}
                            onChange={v => setFormData({...formData, nextOfKinName: v})}
                            isEditing={isEditing}
                            icon={User}
                        />
                         <EditableField
                            label="Relationship"
                            value={formData.nextOfKinRelation}
                            onChange={v => setFormData({...formData, nextOfKinRelation: v})}
                            isEditing={isEditing}
                            icon={Users}
                        />
                        <div className="md:col-span-2">
                            <EditableField
                                label="Next of Kin Phone"
                                value={formData.nextOfKinPhone}
                                onChange={v => setFormData({...formData, nextOfKinPhone: v})}
                                isEditing={isEditing}
                                icon={Phone}
                            />
                        </div>
                    </div>
                )}

                {/* 3. SECURITY */}
                {activeTab === 'security' && (
                    <div>
                        <h3 className="font-bold text-slate-800 mb-4">Change Password</h3>
                        <div className="max-w-md">
                            <ChangePassword />
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}

// Helper Components for Cleaner Code
const Field = ({ label, value, icon: Icon, disabled }) => (
    <div className={`p-3 rounded-xl border ${disabled ? 'bg-slate-50 border-slate-200' : 'bg-white border-slate-300'}`}>
        <label className="text-[10px] font-bold text-slate-400 uppercase mb-1 flex items-center gap-1">
            {Icon && <Icon size={10}/>} {label}
        </label>
        <div className="font-bold text-slate-700 text-sm">{value || 'N/A'}</div>
    </div>
);

const EditableField = ({ label, value, onChange, isEditing, icon: Icon }) => (
    <div className="relative">
        <label className="text-[10px] font-bold text-slate-500 uppercase mb-1 flex items-center gap-1">
            {Icon && <Icon size={10}/>} {label}
        </label>
        {isEditing ? (
            <input
                type="text"
                className="w-full p-3 border border-indigo-200 rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none bg-indigo-50/30 font-bold text-slate-800 text-sm transition"
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