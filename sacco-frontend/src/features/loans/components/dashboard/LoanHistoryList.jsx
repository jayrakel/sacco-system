import React from 'react';
import { Edit, Eye, Clock, AlertCircle, CheckCircle, XCircle, FileText, Ban, Trash2 } from 'lucide-react';
import api from '../../../../api';
import BrandedSpinner from '../../../../components/BrandedSpinner';

export default function LoanHistoryList({ loans, onSelect, onRefresh, }) {
    const [deletingLoanId, setDeletingLoanId] = React.useState(null);
    if (!loans || loans.length === 0) return null;

    const getStatusBadge = (statusString) => {
        if (!statusString) return <span className="bg-slate-100 text-slate-500 px-2 py-1 rounded text-[10px]">UNKNOWN</span>;

        const status = statusString.toUpperCase();

        const styles = {
            // --- 1. APPLICATION PHASE ---
            DRAFT: "bg-slate-100 text-slate-600 border border-slate-200",
            PENDING_GUARANTORS: "bg-amber-50 text-amber-700 border border-amber-200",
            AWAITING_GUARANTORS: "bg-orange-50 text-orange-700 border border-orange-200", // ✅ Ensure added to Loan.java
            SUBMITTED: "bg-blue-50 text-blue-700 border border-blue-200",
            UNDER_REVIEW: "bg-indigo-50 text-indigo-700 border border-indigo-200", // ✅ Added

            // --- 2. DECISION PHASE ---
            APPROVED: "bg-emerald-50 text-emerald-700 border border-emerald-200",
            REJECTED: "bg-red-50 text-red-700 border border-red-200",
            CANCELLED: "bg-gray-50 text-gray-500 border border-gray-200 line-through", // ✅ Added

            // --- 3. ACTIVE PHASE ---
            DISBURSED: "bg-purple-50 text-purple-700 border border-purple-200",
            ACTIVE: "bg-green-50 text-green-700 border border-green-200 font-bold",

            // --- 4. PROBLEM PHASE ---
            IN_ARREARS: "bg-rose-50 text-rose-700 border border-rose-200 font-bold",
            DEFAULTED: "bg-red-100 text-red-900 border border-red-300 font-black", // ✅ Added

            // --- 5. END STATE ---
            CLOSED: "bg-slate-200 text-slate-600 border border-slate-300",
            WRITTEN_OFF: "bg-black text-white border border-slate-800" // ✅ Added
        };

        // Friendly Names Mapping
        const labels = {
            DRAFT: "Draft",
            PENDING_GUARANTORS: "Adding Guarantors",
            AWAITING_GUARANTORS: "Waiting for Signatures",
            SUBMITTED: "Submitted",
            UNDER_REVIEW: "Under Review",
            APPROVED: "Approved",
            REJECTED: "Rejected",
            CANCELLED: "Cancelled",
            DISBURSED: "Disbursed",
            ACTIVE: "Active",
            IN_ARREARS: "In Arrears",
            DEFAULTED: "Defaulted",
            CLOSED: "Closed",
            WRITTEN_OFF: "Written Off"
        };

        // Icon Mapping
        const icons = {
            DRAFT: <FileText size={10}/>,
            AWAITING_GUARANTORS: <Clock size={10}/>,
            UNDER_REVIEW: <Clock size={10}/>,
            APPROVED: <CheckCircle size={10}/>,
            REJECTED: <XCircle size={10}/>,
            CANCELLED: <Ban size={10}/>,
            IN_ARREARS: <AlertCircle size={10}/>,
            DEFAULTED: <AlertCircle size={10}/>
        };

        return (
            <span className={`px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wide flex w-fit items-center gap-1.5 ${styles[status] || 'bg-slate-100 text-slate-600'}`}>
                {icons[status]}
                {labels[status] || status.replace(/_/g, ' ')}
            </span>
        );
    };

    const handleDelete = async (e, loan) => {
            e.stopPropagation(); // Prevent row click
            if (!window.confirm(`Are you sure you want to delete the application for ${loan.productName}? This action cannot be undone.`)) return;

            setDeletingLoanId(loan.id);
            try {
                await api.delete(`/api/loans/${loan.id}`);
                if (onRefresh) onRefresh(); // Refresh the list
            } catch (err) {
                alert(err.response?.data?.message || "Failed to delete loan.");
            } finally {
                setDeletingLoanId(null);
            }
        };

    return (
            <div className="bg-white rounded-3xl shadow-sm border border-slate-200 overflow-hidden animate-in fade-in slide-in-from-bottom-4">
                <div className="p-6 bg-slate-50 border-b border-slate-100 flex justify-between items-center">
                    <h3 className="font-black text-slate-700 uppercase text-xs tracking-widest">Your Loan Applications</h3>
                    <span className="bg-slate-200 text-slate-600 px-2 py-0.5 rounded-full text-[10px] font-bold">{loans.length}</span>
                </div>
                <div className="overflow-x-auto">
                    <table className="w-full text-sm text-left">
                        <thead className="bg-slate-50/50 text-slate-400 font-bold uppercase text-[10px] tracking-widest border-b border-slate-100">
                            <tr>
                                <th className="p-4">Date</th>
                                <th className="p-4">Loan Ref</th>
                                <th className="p-4">Product</th>
                                <th className="p-4 text-right">Amount</th>
                                <th className="p-4">Status</th>
                                <th className="p-4 text-center">Action</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {loans.map(loan => {
                                // ✅ Check if deletable (Matches backend logic)
                                const isDeletable = ['DRAFT', 'PENDING_GUARANTORS'].includes(loan.loanStatus || loan.status);

                                return (
                                    <tr key={loan.id} className="hover:bg-slate-50 transition group cursor-pointer" onClick={() => onSelect(loan)}>
                                        <td className="p-4 text-slate-500 font-mono text-xs">
                                            {new Date(loan.applicationDate).toLocaleDateString()}
                                        </td>
                                        <td className="p-4 font-mono text-slate-600 font-bold text-xs">{loan.loanNumber}</td>
                                        <td className="p-4 text-slate-700 font-medium">
                                            {loan.productName || loan.product?.productName}
                                        </td>
                                        <td className="p-4 text-right font-black text-slate-900 font-mono">
                                            KES {Number(loan.principalAmount || 0).toLocaleString()}
                                        </td>
                                        <td className="p-4">
                                            {getStatusBadge(loan.loanStatus || loan.status)}
                                        </td>
                                        <td className="p-4 text-center">
                                            <div className="flex items-center justify-center gap-2">
                                                {/* View/Edit Button */}
                                                <button
                                                    onClick={(e) => { e.stopPropagation(); onSelect(loan); }}
                                                    className="text-slate-400 hover:text-blue-600 p-2 hover:bg-blue-50 rounded-lg transition"
                                                    title="View Details"
                                                >
                                                    {(loan.loanStatus === 'DRAFT' || loan.loanStatus === 'PENDING_GUARANTORS')
                                                        ? <Edit size={16} />
                                                        : <Eye size={16} />
                                                    }
                                                </button>

                                                {/* ✅ Delete Button (Only for unfinished loans) */}
                                                {isDeletable && (
                                                    <button
                                                        onClick={(e) => handleDelete(e, loan)}
                                                        className="text-slate-400 hover:text-red-600 p-2 hover:bg-red-50 rounded-lg transition"
                                                        title="Delete Application"
                                                        disabled={deletingLoanId === loan.id}
                                                    >
                                                        {deletingLoanId === loan.id ? <BrandedSpinner size="small"/> : <Trash2 size={16} />}
                                                    </button>
                                                )}
                                            </div>
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            </div>
        );
}