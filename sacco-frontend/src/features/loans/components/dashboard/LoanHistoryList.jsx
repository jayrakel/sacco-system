import React from 'react';
import { Edit } from 'lucide-react';

export default function LoanHistoryList({ loans, onSelect, onPayFee }) {
    if (!loans || loans.length === 0) return null;

    const getStatusBadge = (status) => {
        const styles = {
            DRAFT: "bg-slate-100 text-slate-600",
            APPLICATION_FEE_PENDING: "bg-red-50 text-red-600",
            GUARANTORS_PENDING: "bg-amber-50 text-amber-600",
            ACTIVE: "bg-green-100 text-green-700",
            IN_ARREARS: "bg-red-100 text-red-700",
            VOTING_OPEN: "bg-purple-100 text-purple-700",
            DISBURSED: "bg-blue-100 text-blue-700"
        };
        return <span className={`px-2 py-1 rounded text-[10px] font-bold uppercase ${styles[status] || 'bg-slate-100'}`}>{status?.replace(/_/g, ' ')}</span>;
    };

    return (
        <div className="bg-white rounded-3xl shadow-sm border border-slate-200 overflow-hidden">
            <div className="p-6 bg-slate-50 border-b border-slate-100">
                <h3 className="font-black text-slate-700 uppercase text-xs tracking-widest">Your Loan Applications</h3>
            </div>
            <div className="overflow-x-auto">
                <table className="w-full text-sm text-left">
                    <thead className="bg-slate-50/50 text-slate-400 font-bold uppercase text-[10px] tracking-widest border-b border-slate-100">
                        <tr>
                            <th className="p-4">Loan No</th>
                            <th className="p-4 text-right">Principal</th>
                            <th className="p-4">Status</th>
                            <th className="p-4 text-center">Action</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                        {loans.map(loan => (
                            <tr key={loan.id} className="hover:bg-indigo-50/30 transition">
                                <td className="p-4 font-mono text-slate-500 font-bold">{loan.loanNumber || 'NEW DRAFT'}</td>
                                <td className="p-4 text-right font-black text-slate-900 font-mono">KES {Number(loan.principalAmount || 0).toLocaleString()}</td>
                                <td className="p-4">{getStatusBadge(loan.status)}</td>
                                <td className="p-4 text-center">
                                    {loan.status === 'DRAFT' && <button onClick={() => onSelect(loan)} className="text-indigo-600 p-2 hover:bg-indigo-50 rounded-lg"><Edit size={16}/></button>}
                                    {loan.status === 'APPLICATION_FEE_PENDING' && <button onClick={() => onPayFee(loan)} className="bg-purple-600 text-white px-3 py-1 text-[10px] rounded hover:bg-purple-700">PAY FEE</button>}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}