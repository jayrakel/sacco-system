import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { CreditCard, Check, X, Ban, AlertTriangle, Play, RefreshCw, FileText, Plus } from 'lucide-react';

export default function LoanManager() {
    const [loans, setLoans] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState('ALL');

    useEffect(() => { fetchLoans(); }, []);

    const fetchLoans = async () => {
        setLoading(true);
        try {
            const res = await api.get('/api/loans');
            if (res.data.success) setLoans(res.data.data);
        } catch (error) { console.error(error); }
        finally { setLoading(false); }
    };

    const handleAction = async (id, action, promptText = null) => {
        let params = {};
        if (promptText) {
            const value = prompt(promptText);
            if (!value) return;
            // Map prompt value to expected param name
            if (action === 'write-off') params = { reason: value };
            if (action === 'restructure') params = { newDuration: value };
        }

        if (!window.confirm(`Are you sure you want to ${action.toUpperCase()} this loan?`)) return;

        try {
            await api.post(`/api/loans/${id}/${action}`, null, { params });
            alert("Action Successful!");
            fetchLoans();
        } catch (error) {
            alert(error.response?.data?.message || "Action Failed");
        }
    };

    const getStatusColor = (status) => {
        switch (status) {
            case 'APPROVED': return 'bg-blue-100 text-blue-700 border-blue-200';
            case 'DISBURSED': return 'bg-emerald-100 text-emerald-700 border-emerald-200';
            case 'PENDING': return 'bg-amber-100 text-amber-700 border-amber-200';
            case 'REJECTED': return 'bg-red-100 text-red-700 border-red-200';
            case 'WRITTEN_OFF': return 'bg-gray-100 text-gray-600 border-gray-200 decoration-through';
            default: return 'bg-slate-100 text-slate-600';
        }
    };

    const filteredLoans = filter === 'ALL' ? loans : loans.filter(l => l.status === filter);

    return (
        <div className="space-y-6 animate-in fade-in">
            {/* Header */}
            <div className="flex justify-between items-center bg-white p-4 rounded-xl shadow-sm border border-slate-200">
                <h2 className="font-bold text-slate-800 flex items-center gap-2">
                    <CreditCard className="text-purple-600" size={20}/> Loan Portfolio
                </h2>
                <div className="flex bg-slate-100 p-1 rounded-lg">
                    {['ALL', 'PENDING', 'APPROVED', 'DISBURSED', 'DEFAULTED'].map(f => (
                        <button key={f} onClick={() => setFilter(f)}
                            className={`px-3 py-1 text-xs font-bold rounded transition ${filter === f ? 'bg-white shadow text-slate-800' : 'text-slate-500'}`}>
                            {f}
                        </button>
                    ))}
                </div>
            </div>

            {/* Loans Table */}
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
                                <td className="p-4">
                                    <span className={`px-2 py-1 rounded text-[10px] font-bold border ${getStatusColor(loan.status)}`}>
                                        {loan.status}
                                    </span>
                                </td>
                                <td className="p-4 flex justify-center gap-2">
                                    {loan.status === 'PENDING' && (
                                        <>
                                            <button onClick={() => handleAction(loan.id, 'approve')} className="p-1.5 bg-blue-50 text-blue-600 rounded hover:bg-blue-100" title="Approve"><Check size={16}/></button>
                                            <button onClick={() => handleAction(loan.id, 'reject')} className="p-1.5 bg-red-50 text-red-600 rounded hover:bg-red-100" title="Reject"><X size={16}/></button>
                                        </>
                                    )}
                                    {loan.status === 'APPROVED' && (
                                        <button onClick={() => handleAction(loan.id, 'disburse')} className="flex items-center gap-1 px-3 py-1 bg-emerald-600 text-white rounded text-xs font-bold hover:bg-emerald-700">
                                            <Play size={12}/> Disburse
                                        </button>
                                    )}
                                    {(loan.status === 'DISBURSED' || loan.status === 'DEFAULTED') && (
                                        <>
                                            <button onClick={() => handleAction(loan.id, 'restructure', "Enter new duration in months:")} className="p-1.5 bg-amber-50 text-amber-600 rounded hover:bg-amber-100" title="Restructure"><RefreshCw size={16}/></button>
                                            <button onClick={() => handleAction(loan.id, 'write-off', "Reason for write-off:")} className="p-1.5 bg-gray-100 text-gray-600 rounded hover:bg-red-100 hover:text-red-600" title="Write Off"><Ban size={16}/></button>
                                        </>
                                    )}
                                </td>
                            </tr>
                        ))}
                        {filteredLoans.length === 0 && <tr><td colSpan="5" className="p-8 text-center text-slate-400">No loans found.</td></tr>}
                    </tbody>
                </table>
            </div>
        </div>
    );
}