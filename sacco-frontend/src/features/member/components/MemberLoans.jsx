import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { CreditCard, PlusCircle, CheckCircle, Clock, XCircle, FileText } from 'lucide-react';
import LoanApplicationModal from './LoanApplicationModal'; // ✅ Import Modal

export default function MemberLoans({ user }) {
    const [loans, setLoans] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isModalOpen, setIsModalOpen] = useState(false);

    useEffect(() => {
        fetchLoans();
    }, []);

    const fetchLoans = async () => {
        try {
            // ✅ Use the secured endpoint (No ID required in URL)
            const res = await api.get('/api/loans/my-loans');
            if (res.data.success) setLoans(res.data.data);
        } catch (e) {
            console.error("Failed to load loans", e);
        } finally {
            setLoading(false);
        }
    };

    const getStatusBadge = (status) => {
        switch(status) {
            case 'APPROVED': return <span className="bg-emerald-100 text-emerald-700 px-2 py-1 rounded-full text-[10px] font-bold uppercase flex items-center gap-1 w-fit"><CheckCircle size={12}/> Approved</span>;
            case 'PENDING': return <span className="bg-amber-100 text-amber-700 px-2 py-1 rounded-full text-[10px] font-bold uppercase flex items-center gap-1 w-fit"><Clock size={12}/> Pending</span>;
            case 'REJECTED': return <span className="bg-red-100 text-red-700 px-2 py-1 rounded-full text-[10px] font-bold uppercase flex items-center gap-1 w-fit"><XCircle size={12}/> Rejected</span>;
            case 'DISBURSED': return <span className="bg-blue-100 text-blue-700 px-2 py-1 rounded-full text-[10px] font-bold uppercase flex items-center gap-1 w-fit"><CreditCard size={12}/> Active</span>;
            default: return <span className="bg-slate-100 text-slate-600 px-2 py-1 rounded-full text-[10px] font-bold uppercase">{status}</span>;
        }
    };

    if(loading) return <div className="p-10 text-center text-slate-400">Loading your loan history...</div>;

    return (
        <div className="space-y-6 animate-in fade-in">
            {/* Header */}
            <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 flex justify-between items-center">
                <div>
                    <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2"><CreditCard className="text-indigo-600"/> My Loans</h2>
                    <p className="text-slate-500 text-sm mt-1">Track repayments and application status.</p>
                </div>
                <button
                    onClick={() => setIsModalOpen(true)}
                    className="bg-indigo-900 text-white px-5 py-2.5 rounded-xl text-sm font-bold flex items-center gap-2 hover:bg-indigo-800 transition shadow-lg shadow-indigo-900/20"
                >
                    <PlusCircle size={18}/> Apply New Loan
                </button>
            </div>

            {/* Loans Table */}
            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full text-sm text-left">
                        <thead className="bg-slate-50 text-slate-500 font-bold border-b border-slate-100 uppercase text-xs tracking-wider">
                            <tr>
                                <th className="p-4">Loan No</th>
                                <th className="p-4 text-right">Principal</th>
                                <th className="p-4 text-right">Balance</th>
                                <th className="p-4">Status</th>
                                <th className="p-4">Next Due</th>
                                <th className="p-4 text-center">Action</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {loans.map(loan => (
                                <tr key={loan.id} className="hover:bg-slate-50 transition">
                                    <td className="p-4 font-mono text-slate-500 font-bold">{loan.loanNumber}</td>
                                    <td className="p-4 text-right font-medium text-slate-700">KES {Number(loan.principalAmount).toLocaleString()}</td>
                                    <td className="p-4 text-right font-bold text-slate-900">KES {Number(loan.loanBalance).toLocaleString()}</td>
                                    <td className="p-4">{getStatusBadge(loan.status)}</td>
                                    <td className="p-4 text-slate-500 text-xs">{loan.expectedRepaymentDate || '-'}</td>
                                    <td className="p-4 text-center">
                                        <button className="text-blue-600 hover:text-blue-800 font-bold text-xs underline decoration-dotted flex items-center justify-center gap-1 mx-auto">
                                            <FileText size={12}/> Details
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            {loans.length === 0 && (
                                <tr>
                                    <td colSpan="6" className="p-12 text-center text-slate-400 italic flex flex-col items-center gap-2">
                                        <div className="w-12 h-12 bg-slate-100 rounded-full flex items-center justify-center mb-2"><CreditCard size={24} className="opacity-20"/></div>
                                        You have no loan history yet.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Application Modal */}
            <LoanApplicationModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                onSuccess={fetchLoans}
                memberId={user?.memberId} // Pass ID if available, else modal relies on backend context
            />
        </div>
    );
}