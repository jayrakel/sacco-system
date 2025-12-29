import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { ChevronRight, Briefcase } from 'lucide-react';

export default function OfficerDashboard() {
    const [loans, setLoans] = useState([]);

    useEffect(() => {
        // Fetch pending loans and FILTER for officer relevance
        api.get('/api/loans/admin/pending').then(res => {
            if(res.data.success) {
                // Officer only cares about SUBMITTED loans
                setLoans(res.data.data.filter(l => l.status === 'SUBMITTED'));
            }
        });
    }, []);

    const handleReview = async (loanId, decision) => {
        await api.post(`/api/loans/admin/${loanId}/review`, { decision });
        alert("Processed!");
        window.location.reload(); // Simple reload to refresh
    };

    return (
        <div className="bg-white rounded-2xl shadow-sm p-6">
            <h2 className="text-xl font-bold mb-4 flex gap-2"><Briefcase/> Technical Review Queue</h2>
            {loans.map(loan => (
                <div key={loan.id} className="border-b p-4 flex justify-between items-center">
                    <div>
                        <h3 className="font-bold">{loan.memberName}</h3>
                        <p className="text-sm text-slate-500">KES {loan.principalAmount.toLocaleString()}</p>
                    </div>
                    <div className="flex gap-2">
                        <button onClick={() => handleReview(loan.id, 'REJECT')} className="px-4 py-2 bg-red-50 text-red-600 rounded-lg">Reject</button>
                        <button onClick={() => handleReview(loan.id, 'APPROVE')} className="px-4 py-2 bg-blue-600 text-white rounded-lg">Approve</button>
                    </div>
                </div>
            ))}
            {loans.length === 0 && <p className="text-slate-400">No applications pending review.</p>}
        </div>
    );
}