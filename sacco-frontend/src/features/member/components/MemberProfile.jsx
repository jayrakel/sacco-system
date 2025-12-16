import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { User, Phone, Mail, MapPin, Calendar, FileText } from 'lucide-react';

export default function MemberProfile({ user }) {
    const [profile, setProfile] = useState(null);

    useEffect(() => {
        if(user?.memberId) {
            api.get(`/api/members/${user.memberId}`).then(res => {
                if(res.data.success) setProfile(res.data.data);
            });
        }
    }, [user]);

    if(!profile) return <div className="p-8 text-center text-slate-400">Loading Profile...</div>;

    const InfoRow = ({ icon: Icon, label, value }) => (
        <div className="flex items-center gap-4 p-3 border-b border-slate-100 last:border-0">
            <div className="text-slate-400"><Icon size={18}/></div>
            <div className="flex-1">
                <p className="text-xs font-bold text-slate-400 uppercase">{label}</p>
                <p className="text-sm font-medium text-slate-800">{value || 'N/A'}</p>
            </div>
        </div>
    );

    return (
        <div className="max-w-2xl mx-auto space-y-6 animate-in fade-in">
            <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                <div className="flex items-center gap-4 mb-6">
                    <div className="w-16 h-16 rounded-full bg-indigo-100 text-indigo-600 flex items-center justify-center text-xl font-bold border-2 border-indigo-50">
                        {profile.firstName?.charAt(0)}{profile.lastName?.charAt(0)}
                    </div>
                    <div>
                        <h2 className="text-xl font-bold text-slate-800">{profile.firstName} {profile.lastName}</h2>
                        <p className="text-slate-500 text-sm">Member #{profile.memberNumber}</p>
                        <span className="inline-block mt-1 px-2 py-0.5 bg-green-100 text-green-700 rounded text-[10px] font-bold uppercase">{profile.status}</span>
                    </div>
                </div>

                <div className="bg-slate-50 rounded-xl p-2">
                    <InfoRow icon={Mail} label="Email Address" value={profile.email} />
                    <InfoRow icon={Phone} label="Phone Number" value={profile.phoneNumber} />
                    <InfoRow icon={FileText} label="ID / Passport" value={profile.idNumber} />
                    <InfoRow icon={MapPin} label="Address" value={profile.address} />
                    <InfoRow icon={Calendar} label="Date of Birth" value={profile.dateOfBirth} />
                </div>
            </div>
        </div>
    );
}