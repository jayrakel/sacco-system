// Simplified logic for your Treasurer Dashboard
import React, { useState, useEffect } from 'react';
import api from '../../../api'; // Your axios instance

export default function LoanDisbursementQueue() {
    const [pending, setPending] = useState([]);

    const fetchPending = async () => {
        const res = await api.get('/api/loans/disbursements/pending-preparation');
        setPending(res.data);
    };

    const handleComplete = async (disbId, ref) => {
        await api.post(`/api/loans/disbursements/complete/${disbId}`, { reference: ref });
        alert("Loan Disbursed! Repayment schedule is now active.");
        fetchPending();
    };

    return (
        <div className="p-4">
            <h2 className="text-xl font-bold">Pending Disbursements</h2>
            {pending.map(loan => (
                <div key={loan.id} className="border p-4 mb-2 flex justify-between">
                    <span>{loan.loanNumber} - KES {loan.amount}</span>
                    <button 
                        onClick={() => handleComplete(loan.id, "TXN-REF-123")}
                        className="bg-green-600 text-white px-4 py-2 rounded"
                    >
                        Mark as Disbursed
                    </button>
                </div>
            ))}
        </div>
    );
}