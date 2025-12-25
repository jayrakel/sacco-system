import React, { useState } from 'react';
import api from '../../../api';
import { UserPlus, CheckCircle } from 'lucide-react';

export default function GuarantorManager({ loanId, principalAmount }) {
    const [guarantors, setGuarantors] = useState([]);
    const [inviteData, setInviteData] = useState({ memberNumber: '', amount: '' });

    const handleInvite = async (e) => {
        e.preventDefault();
        try {
            // First: Find member by number
            const memberRes = await api.get(`/api/members/search?number=${inviteData.memberNumber}`);
            const member = memberRes.data.data;

            // Second: Invite them
            const res = await api.post(`/api/loans/${loanId}/guarantors`, {
                memberId: member.id,
                amount: inviteData.amount
            });
            
            setGuarantors([...guarantors, res.data.data]);
            setInviteData({ memberNumber: '', amount: '' });
        } catch (err) {
            alert("Invitation failed: " + (err.response?.data?.message || "Member not found"));
        }
    };

    const totalGuaranteed = guarantors
        .filter(g => g.status === 'ACCEPTED')
        .reduce((sum, g) => sum + Number(g.guaranteeAmount), 0);

    return (
        <div className="space-y-6">
            <div className="bg-slate-900 text-white p-6 rounded-2xl shadow-lg">
                <p className="text-slate-400 text-xs font-bold uppercase tracking-wider">Loan Coverage</p>
                <div className="flex justify-between items-end mt-2">
                    <h2 className="text-3xl font-bold">KES {totalGuaranteed.toLocaleString()}</h2>
                    <p className="text-slate-400 text-sm">Target: KES {principalAmount.toLocaleString()}</p>
                </div>
                <div className="w-full bg-white/10 h-2 mt-4 rounded-full overflow-hidden">
                    <div 
                        className="bg-emerald-500 h-full transition-all duration-500" 
                        style={{ width: `${Math.min((totalGuaranteed / principalAmount) * 100, 100)}%` }}
                    />
                </div>
            </div>

            <form onSubmit={handleInvite} className="flex gap-2">
                <input 
                    type="text" 
                    placeholder="Member No (e.g. MEM001)" 
                    className="flex-1 p-3 border rounded-xl"
                    value={inviteData.memberNumber}
                    onChange={(e) => setInviteData({...inviteData, memberNumber: e.target.value})}
                />
                <input 
                    type="number" 
                    placeholder="Amount" 
                    className="w-32 p-3 border rounded-xl"
                    value={inviteData.amount}
                    onChange={(e) => setInviteData({...inviteData, amount: e.target.value})}
                />
                <button className="bg-indigo-600 text-white p-3 rounded-xl"><UserPlus size={20}/></button>
            </form>

            <div className="divide-y border rounded-2xl overflow-hidden">
                {guarantors.map(g => (
                    <div key={g.id} className="p-4 bg-white flex justify-between items-center">
                        <div>
                            <p className="font-bold">{g.memberName}</p>
                            <p className="text-xs text-slate-500">KES {g.guaranteeAmount.toLocaleString()}</p>
                        </div>
                        <span className={`text-xs font-bold px-2 py-1 rounded-full ${
                            g.status === 'ACCEPTED' ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'
                        }`}>
                            {g.status}
                        </span>
                    </div>
                ))}
            </div>
        </div>
    );
}