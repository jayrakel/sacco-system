import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { Wallet, CheckCircle } from 'lucide-react';

export default function TreasurerDashboard() {
    const [loans, setLoans] = useState([]);

    useEffect(() => {
        api.get('/api/loans/admin/pending').then(res => {
            if(res.data.success) {
                // Treasurer sees loans ready for payout (Passed voting or direct approval)
                setLoans(res.data.data.filter(l => l.status === 'VOTING_OPEN' || l.status === 'APPROVED'));
            }
        });
    }, []);

    const handleDisburse = async (loanId) => {
        if(!window.confirm("Confirm Fund Transfer?")) return;
        await api.post(`/api/loans/admin/${loanId}/disburse`);
        alert("Funds Disbursed.");
        window.location.reload();
    };

    return (
        <div className="bg-white rounded-2xl shadow-sm p-6">
            <h2 className="text-xl font-bold mb-4 flex gap-2"><Wallet/> Disbursement Center</h2>

            <div className="space-y-4">
                {loans.map(loan => (
                    <div key={loan.id} className="p-4 border border-emerald-100 bg-emerald-50 rounded-xl flex justify-between items-center">
                        <div>
                            <h3 className="font-bold text-emerald-900">{loan.memberName}</h3>
                            <p className="font-mono text-emerald-700">KES {loan.principalAmount.toLocaleString()}</p>
                            <p className="text-xs text-emerald-600">Ref: {loan.loanNumber}</p>
                        </div>
                        <button
                            onClick={() => handleDisburse(loan.id)}
                            className="px-6 py-3 bg-emerald-600 text-white font-bold rounded-xl shadow hover:bg-emerald-700 flex items-center gap-2"
                        >
                            <CheckCircle size={18}/> Disburse
                        </button>
                    </div>
                ))}
            </div>
            {loans.length === 0 && <p className="text-slate-400">No authorized disbursements pending.</p>}
        </div>
    );
}