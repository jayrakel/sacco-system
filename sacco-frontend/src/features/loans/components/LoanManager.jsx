import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { CreditCard, Check, X, Eye, ArrowRight, Clock, FileText } from 'lucide-react';
import LoanReviewModal from './LoanReviewModal'; // ✅ Import the new Modal

export default function LoanManager({onUpdate}) {
    const [loans, setLoans] = useState([]);
    const [filter, setFilter] = useState('SUBMITTED');
    const [loading, setLoading] = useState(false);

    // ✅ Review Modal State
    const [reviewLoan, setReviewLoan] = useState(null);

    useEffect(() => { fetchLoans(); }, []);

    const fetchLoans = async () => {
        setLoading(true);
        try {
            const res = await api.get('/api/loans');
            if (res.data.success) setLoans(res.data.data);
        } catch (error) { console.error(error); }
        finally { setLoading(false); }
    };

    // ✅ Handle "Start Review"
    const handleStartReview = async (loan) => {
            if (!window.confirm(`Start review for ${loan.loanNumber}?`)) return;
            try {
                await api.post(`/api/loans/${loan.id}/review`);
                setReviewLoan(loan);

                fetchLoans(); // Refresh local table
                if (onUpdate) onUpdate(); // ✅ Refresh parent stats immediately
            } catch (error) {
                alert(error.response?.data?.message || "Failed");
            }
        };

    // ✅ Handle "Resume Review" (If already in review status)
    const handleResumeReview = (loan) => {
        setReviewLoan(loan);
    };

    // ✅ Handle Decision from Modal
    const handleDecision = async (id, action, payload) => {
            try {
                const endpoint = action === 'approve' ? 'approve' : 'reject';
                const config = payload ? { params: payload } : {};

                await api.post(`/api/loans/${id}/${endpoint}`, null, config);

                alert(`Loan ${action}d successfully!`);
                setReviewLoan(null);

                fetchLoans(); // Refresh local table
                if (onUpdate) onUpdate(); // ✅ Refresh parent stats immediately
            } catch (error) {
                alert(error.response?.data?.message || "Action Failed");
            }
        };

    // Filter Logic
    const getFilteredLoans = () => {
        switch (filter) {
            case 'SUBMITTED': return loans.filter(l => l.status === 'SUBMITTED');
            case 'REVIEW': return loans.filter(l => l.status === 'LOAN_OFFICER_REVIEW');
            case 'TABLED': return loans.filter(l => l.status === 'SECRETARY_TABLED' || l.status === 'VOTING_OPEN');
            case 'APPROVED': return loans.filter(l => l.status === 'ADMIN_APPROVED' || l.status === 'TREASURER_DISBURSEMENT');
            case 'ACTIVE': return loans.filter(l => l.status === 'DISBURSED' || l.status === 'ACTIVE');
            case 'REJECTED': return loans.filter(l => l.status === 'REJECTED');
            default: return loans;
        }
    };

    const displayLoans = getFilteredLoans();

    return (
        <div className="space-y-6 animate-in fade-in">
            {/* Header & Filters */}
            <div className="bg-white p-4 rounded-xl shadow-sm border border-slate-200 flex flex-col sm:flex-row justify-between items-center gap-4">
                <h2 className="font-bold text-slate-800 flex items-center gap-2">
                    <CreditCard className="text-indigo-600" size={20}/> Loan Portfolio
                </h2>

                <div className="flex bg-slate-100 p-1 rounded-lg overflow-x-auto max-w-full">
                    {[
                        { id: 'SUBMITTED', label: 'New Inbox' },
                        { id: 'REVIEW', label: 'Under Review' },
                        { id: 'TABLED', label: 'Tabled' },
                        { id: 'APPROVED', label: 'Approved' },
                        { id: 'ACTIVE', label: 'Active Portfolio' }
                    ].map(tab => (
                        <button
                            key={tab.id}
                            onClick={() => setFilter(tab.id)}
                            className={`px-4 py-2 text-xs font-bold rounded-md transition whitespace-nowrap ${filter === tab.id ? 'bg-white shadow text-indigo-700' : 'text-slate-500 hover:text-slate-700'}`}
                        >
                            {tab.label}
                        </button>
                    ))}
                </div>
            </div>

            {/* Table */}
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                <table className="w-full text-sm text-left">
                    <thead className="bg-slate-50 text-slate-500 font-bold border-b border-slate-100 uppercase text-xs">
                        <tr>
                            <th className="p-4">Ref</th>
                            <th className="p-4">Applicant</th>
                            <th className="p-4">Product</th>
                            <th className="p-4 text-right">Amount</th>
                            <th className="p-4">Date</th>
                            <th className="p-4 text-center">Actions</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                        {loading ? (
                            <tr><td colSpan="6" className="p-8 text-center text-slate-400">Loading loans...</td></tr>
                        ) : displayLoans.length === 0 ? (
                            <tr><td colSpan="6" className="p-8 text-center text-slate-400 italic">No loans found in this category.</td></tr>
                        ) : displayLoans.map(loan => (
                            <tr key={loan.id} className="hover:bg-slate-50 transition">
                                <td className="p-4 font-mono text-slate-500 text-xs">{loan.loanNumber}</td>
                                <td className="p-4">
                                    <p className="font-bold text-slate-700">{loan.memberName}</p>
                                </td>
                                <td className="p-4 text-slate-600">
                                    <span className="bg-slate-100 text-slate-600 px-2 py-1 rounded text-xs font-bold">{loan.productName}</span>
                                </td>
                                <td className="p-4 text-right font-mono font-bold text-slate-700">
                                    KES {Number(loan.principalAmount).toLocaleString()}
                                </td>
                                <td className="p-4 text-slate-500 text-xs">
                                    {loan.submissionDate || loan.applicationDate}
                                </td>
                                <td className="p-4 flex justify-center gap-2">

                                    {/* STATUS: SUBMITTED (Start Review) */}
                                    {loan.status === 'SUBMITTED' && (
                                        <button
                                            onClick={() => handleStartReview(loan)}
                                            className="flex items-center gap-1 px-3 py-1.5 bg-indigo-50 text-indigo-600 rounded-lg text-xs font-bold hover:bg-indigo-100 border border-indigo-200"
                                        >
                                            <Eye size={14}/> Start Review
                                        </button>
                                    )}

                                    {/* STATUS: UNDER REVIEW (Resume Review) */}
                                    {loan.status === 'LOAN_OFFICER_REVIEW' && (
                                        <button
                                            onClick={() => handleResumeReview(loan)}
                                            className="flex items-center gap-1 px-3 py-1.5 bg-amber-50 text-amber-600 rounded-lg text-xs font-bold hover:bg-amber-100 border border-amber-200"
                                        >
                                            <Clock size={14}/> Resume Review
                                        </button>
                                    )}

                                    {/* STATUS: OTHERS (View Only) */}
                                    {!['SUBMITTED', 'LOAN_OFFICER_REVIEW'].includes(loan.status) && (
                                        <span className="text-xs text-slate-400 italic flex items-center gap-1">
                                            <FileText size={12}/> {loan.status.replace(/_/g, ' ')}
                                        </span>
                                    )}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {/* ✅ RENDER REVIEW MODAL */}
            {reviewLoan && (
                <LoanReviewModal
                    loan={reviewLoan}
                    onClose={() => setReviewLoan(null)}
                    onAction={handleDecision}
                />
            )}
        </div>
    );
}