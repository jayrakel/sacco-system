import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { CreditCard, Check, X, Ban, RefreshCw, Play, Receipt, AlertCircle } from 'lucide-react';

export default function LoanManager() {
    const [loans, setLoans] = useState([]);
    const [filter, setFilter] = useState('ALL');

    // ✅ NEW: Charges Modal State
    const [showChargesModal, setShowChargesModal] = useState(false);
    const [selectedLoanCharges, setSelectedLoanCharges] = useState([]);
    const [loadingCharges, setLoadingCharges] = useState(false);

    useEffect(() => { fetchLoans(); }, []);

    const fetchLoans = async () => {
        try {
            const res = await api.get('/api/loans');
            if (res.data.success) setLoans(res.data.data);
        } catch (error) { console.error(error); }
    };

    const handleAction = async (id, action, promptText = null) => {
        let params = {};
        if (promptText) {
            const value = prompt(promptText);
            if (!value) return;
            if (action === 'write-off') params = { reason: value };
            if (action === 'restructure') params = { newDuration: value };
        }

        if (!window.confirm(`Confirm ${action.toUpperCase()}?`)) return;

        try {
            await api.post(`/api/loans/${id}/${action}`, null, { params });
            alert("Action Successful!");
            fetchLoans();
        } catch (error) { alert(error.response?.data?.message || "Action Failed"); }
    };

    // ✅ View Charges
    const handleViewCharges = async (loanId) => {
        setShowChargesModal(true);
        setLoadingCharges(true);
        try {
            const res = await api.get(`/api/charges/loan/${loanId}`);
            if (res.data.success) setSelectedLoanCharges(res.data.data);
        } catch (e) { console.error(e); }
        finally { setLoadingCharges(false); }
    };

    // ✅ Waive Charge
    const handleWaive = async (chargeId) => {
        const reason = prompt("Enter waiver reason:");
        if (!reason) return;
        try {
            await api.post(`/api/charges/${chargeId}/waive`, null, { params: { reason } });
            alert("Charge Waived!");
            setShowChargesModal(false); // Close to refresh context
        } catch (e) { alert("Failed to waive charge"); }
    };

    const filteredLoans = filter === 'ALL' ? loans : loans.filter(l => l.status === filter);

    return (
        <div className="space-y-6 animate-in fade-in">
            {/* Header */}
            <div className="flex justify-between items-center bg-white p-4 rounded-xl shadow-sm border border-slate-200">
                <h2 className="font-bold text-slate-800 flex items-center gap-2"><CreditCard className="text-purple-600" size={20}/> Loan Portfolio</h2>
                <div className="flex bg-slate-100 p-1 rounded-lg">
                    {['ALL', 'PENDING', 'APPROVED', 'DISBURSED', 'DEFAULTED'].map(f => (
                        <button key={f} onClick={() => setFilter(f)} className={`px-3 py-1 text-xs font-bold rounded transition ${filter === f ? 'bg-white shadow text-slate-800' : 'text-slate-500'}`}>{f}</button>
                    ))}
                </div>
            </div>

            {/* Table */}
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                <table className="w-full text-sm text-left">
                    <thead className="bg-slate-50 text-slate-500 font-bold border-b">
                        <tr>
                            <th className="p-4">Loan No</th>
                            <th className="p-4">Member</th>
                            <th className="p-4 text-right">Amount</th>
                            <th className="p-4">Status</th>
                            <th className="p-4 text-center">Actions</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y">
                        {filteredLoans.map(loan => (
                            <tr key={loan.id} className="hover:bg-slate-50">
                                <td className="p-4 font-mono text-slate-500">{loan.loanNumber}</td>
                                <td className="p-4 font-bold text-slate-700">{loan.memberName}</td>
                                <td className="p-4 text-right font-mono">KES {Number(loan.principalAmount).toLocaleString()}</td>
                                <td className="p-4"><span className="bg-slate-100 px-2 py-1 rounded text-xs font-bold">{loan.status}</span></td>
                                <td className="p-4 flex justify-center gap-2">
                                    {loan.status === 'PENDING' && (
                                        <>
                                            <button onClick={() => handleAction(loan.id, 'approve')} className="p-1.5 bg-blue-50 text-blue-600 rounded hover:bg-blue-100" title="Approve"><Check size={16}/></button>
                                            <button onClick={() => handleAction(loan.id, 'reject')} className="p-1.5 bg-red-50 text-red-600 rounded hover:bg-red-100" title="Reject"><X size={16}/></button>
                                        </>
                                    )}
                                    {loan.status === 'APPROVED' && (
                                        <button onClick={() => handleAction(loan.id, 'disburse')} className="flex items-center gap-1 px-3 py-1 bg-emerald-600 text-white rounded text-xs font-bold hover:bg-emerald-700"><Play size={12}/> Disburse</button>
                                    )}
                                    <button onClick={() => handleViewCharges(loan.id)} className="p-1.5 bg-purple-50 text-purple-600 rounded hover:bg-purple-100" title="View Charges"><Receipt size={16}/></button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {/* ✅ CHARGES MODAL */}
            {showChargesModal && (
                <div className="fixed inset-0 bg-slate-900/50 flex items-center justify-center p-4 z-50 backdrop-blur-sm">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-lg p-6 animate-in zoom-in-95">
                        <div className="flex justify-between items-center mb-4 border-b pb-2">
                            <h3 className="font-bold text-lg">Applied Charges</h3>
                            <button onClick={() => setShowChargesModal(false)}><X size={20} className="text-slate-400"/></button>
                        </div>
                        <div className="space-y-3">
                            {selectedLoanCharges.length === 0 ? <p className="text-slate-400 text-center italic">No charges found.</p> : selectedLoanCharges.map(charge => (
                                <div key={charge.id} className="flex justify-between items-center p-3 bg-slate-50 rounded-lg border border-slate-100">
                                    <div>
                                        <p className="font-bold text-sm text-slate-700">{charge.type.replace(/_/g, ' ')}</p>
                                        <p className="text-xs text-slate-500">{charge.description}</p>
                                        {charge.isWaived && <span className="text-[10px] bg-red-100 text-red-600 px-1 rounded">WAIVED: {charge.waiverReason}</span>}
                                    </div>
                                    <div className="text-right">
                                        <p className="font-mono font-bold text-slate-800">KES {charge.amount}</p>
                                        {!charge.isWaived && charge.status !== 'PAID' && (
                                            <button onClick={() => handleWaive(charge.id)} className="text-[10px] text-blue-600 underline font-bold">Waive</button>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}