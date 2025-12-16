import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { User, Phone, Mail, MapPin, Calendar, FileText, Shield } from 'lucide-react';

export default function MemberProfile({ user }) {
    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // ✅ Use the secure 'me' endpoint
        api.get('/api/members/me')
           .then(res => {
               if(res.data.success) setProfile(res.data.data);
               setLoading(false);
           })
           .catch(err => {
               console.error("Profile load failed", err);
               setLoading(false);
           });
    }, []);

    if (loading) return <div className="p-10 text-center text-slate-400">Loading your profile...</div>;
    if (!profile) return <div className="p-10 text-center text-red-400">Failed to load profile. Please contact support.</div>;

    const InfoRow = ({ icon: Icon, label, value }) => (
        <div className="flex items-center gap-4 p-4 border-b border-slate-50 last:border-0 hover:bg-slate-50 transition rounded-lg">
            <div className="p-2 bg-slate-100 text-slate-500 rounded-lg"><Icon size={18}/></div>
            <div className="flex-1">
                <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wide mb-0.5">{label}</p>
                <p className="text-sm font-bold text-slate-800">{value || 'Not Provided'}</p>
            </div>
        </div>
    );

    return (
        <div className="max-w-3xl mx-auto space-y-6 animate-in fade-in">

            {/* Header Card */}
            <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 flex flex-col md:flex-row items-center gap-6">
                <div className="w-24 h-24 rounded-full bg-indigo-100 text-indigo-600 flex items-center justify-center text-3xl font-bold border-4 border-white shadow-lg">
                    {profile.firstName?.charAt(0)}{profile.lastName?.charAt(0)}
                </div>
                <div className="text-center md:text-left flex-1">
                    <h2 className="text-2xl font-bold text-slate-800">{profile.firstName} {profile.lastName}</h2>
                    <div className="flex items-center justify-center md:justify-start gap-3 mt-2">
                        <span className="bg-slate-100 text-slate-600 px-3 py-1 rounded-full text-xs font-bold border border-slate-200">
                            Member #{profile.memberNumber}
                        </span>
                        <span className={`px-3 py-1 rounded-full text-xs font-bold uppercase flex items-center gap-1 ${profile.status === 'ACTIVE' ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'}`}>
                            <Shield size={12}/> {profile.status}
                        </span>
                    </div>
                </div>
            </div>

            {/* Details Card */}
            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
                <div className="p-4 bg-slate-50 border-b border-slate-200 font-bold text-slate-700 text-sm">
                    Personal Information
                </div>
                <div className="p-2">
                    <InfoRow icon={Mail} label="Email Address" value={profile.email} />
                    <InfoRow icon={Phone} label="Phone Number" value={profile.phoneNumber} />
                    <InfoRow icon={FileText} label="ID / Passport Number" value={profile.idNumber} />
                    <InfoRow icon={MapPin} label="Residential Address" value={profile.address} />
                    <InfoRow icon={Calendar} label="Date of Birth" value={profile.dateOfBirth} />
                </div>
            </div>
        </div>
    );
}