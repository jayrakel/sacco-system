import React, { useState, useEffect } from 'react';
import api from '../api';
import { Landmark, Send, CheckCircle } from 'lucide-react';
import DashboardHeader from '../components/DashboardHeader';

export default function TreasurerDashboard() {
    const [pendingDisbursements, setPendingDisbursements] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchPending();
    }, []);

    const fetchPending = async () => {
        try {
            const res = await api.get('/api/loans?status=TREASURER_DISBURSEMENT');
            setPendingDisbursements(res.data.data);
        } finally {
            setLoading(false);
        }
    };

    const handleDisburse = async (loanId) => {
        const ref = prompt("Enter Bank Transaction / M-Pesa Reference Code:");
        if (!ref) return;

        try {
            await api.post(`/api/loans/${loanId}/disburse?reference=${ref}`);
            alert("Disbursement successful. Loan is now active.");
            fetchPending();
        } catch (err) {
            alert("Failed to disburse: " + err.response?.data?.message);
        }
    };

    return (
        <div className="min-h-screen bg-slate-50">
            <DashboardHeader title="Treasury Portal" />
            <main className="max-w-7xl mx-auto px-4 mt-8 space-y-6">
                <h1 className="text-2xl font-bold">Pending Disbursements</h1>
                
                <div className="bg-white rounded-2xl border overflow-hidden">
                    <div className="divide-y">
                        {pendingDisbursements.map(loan => (
                            <div key={loan.id} className="p-6 flex justify-between items-center">
                                <div>
                                    <h3 className="font-bold text-lg">{loan.memberName}</h3>
                                    <p className="text-indigo-600 font-mono font-bold">KES {loan.principalAmount.toLocaleString()}</p>
                                    <p className="text-xs text-slate-400 uppercase font-bold mt-1">{loan.loanNumber}</p>
                                </div>
                                <button 
                                    onClick={() => handleDisburse(loan.id)}
                                    className="bg-emerald-600 text-white px-6 py-3 rounded-xl font-bold flex items-center gap-2 hover:bg-emerald-700 transition"
                                >
                                    <Landmark size={20}/> Confirm Disbursement
                                </button>
                            </div>
                        ))}
                        {pendingDisbursements.length === 0 && (
                            <div className="p-20 text-center text-slate-400">No loans currently awaiting disbursement.</div>
                        )}
                    </div>
                </div>
            </main>
        </div>
    );
}