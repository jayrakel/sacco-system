import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { CreditCard, PlusCircle, CheckCircle, Clock, XCircle, FileText, Banknote, ArrowRight, Trash2, Edit, Users } from 'lucide-react';
import LoanApplicationModal from './LoanApplicationModal';
import LoanFeePaymentModal from './LoanFeePaymentModal';

export default function MemberLoans({ user }) {
    const [loans, setLoans] = useState([]);
    const [loading, setLoading] = useState(true);

    // Modal States
    const [isApplyModalOpen, setIsApplyModalOpen] = useState(false);
    const [isPayFeeModalOpen, setIsPayFeeModalOpen] = useState(false);

    // Used for both Paying Fee AND Resuming Draft
    const [selectedLoan, setSelectedLoan] = useState(null);

    useEffect(() => {
        fetchLoans();
    }, []);

    const fetchLoans = async () => {
        try {
            const res = await api.get('/api/loans/my-loans');
            if (res.data.success) setLoans(res.data.data);
        } catch (e) {
            console.error("Failed to load loans", e);
        } finally {
            setLoading(false);
        }
    };

    // --- ACTIONS ---

    const handleDelete = async (id) => {
        if(!window.confirm("Are you sure you want to delete this application?")) return;
        try {
            await api.delete(`/api/loans/${id}`);
            fetchLoans(); // Refresh list
        } catch (e) {
            alert(e.response?.data?.message || "Delete failed");
        }
    };

    const handleContinue = (loan) => {
        setSelectedLoan(loan);
        setIsApplyModalOpen(true); // Re-open Wizard in Resume Mode
    };

    const handlePayFee = (loan) => {
        setSelectedLoan(loan);
        setIsPayFeeModalOpen(true);
    };

    const handleCloseApplyModal = () => {
        setIsApplyModalOpen(false);
        setSelectedLoan(null); // Reset selection so next "New Loan" click is fresh
    };

    // ✅ HELPER: Friendly Status Text
        const getStatusBadge = (status) => {
            const styles = {
                DRAFT: "bg-slate-100 text-slate-600",
                GUARANTORS_PENDING: "bg-amber-50 text-amber-600",
                GUARANTORS_APPROVED: "bg-blue-50 text-blue-600",
                SUBMITTED: "bg-indigo-50 text-indigo-600",
                LOAN_OFFICER_REVIEW: "bg-purple-50 text-purple-600", // "Under Review"
                SECRETARY_TABLED: "bg-orange-50 text-orange-600", // "Awaiting Committee"
                VOTING_OPEN: "bg-pink-50 text-pink-600",
                ADMIN_APPROVED: "bg-emerald-50 text-emerald-600",
                DISBURSED: "bg-green-100 text-green-700",
                REJECTED: "bg-red-50 text-red-600",
            };

            const labels = {
                LOAN_OFFICER_REVIEW: "Under Review",
                SECRETARY_TABLED: "Tabled for Meeting",
                VOTING_OPEN: "Voting in Progress",
                ADMIN_APPROVED: "Approved - Pending Payout",
                DISBURSED: "Active",
            };

            const style = styles[status] || "bg-slate-100 text-slate-600";
            const label = labels[status] || status.replace(/_/g, ' ');

            return <span className={`px-2 py-1 rounded text-[10px] font-bold uppercase ${style}`}>{label}</span>;
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
                    onClick={() => setIsApplyModalOpen(true)}
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
                                <th className="p-4 text-right">Next Due</th>
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
                                    <td className="p-4 text-right text-slate-500 text-xs">{loan.expectedRepaymentDate || '-'}</td>
                                    <td className="p-4 text-center">
                                        <div className="flex items-center justify-center gap-2">

                                            {/* 1. Continue Draft */}
                                            {loan.status === 'DRAFT' && (
                                                <button onClick={() => handleContinue(loan)} className="text-indigo-600 hover:bg-indigo-50 p-1.5 rounded-lg border border-transparent hover:border-indigo-100 transition" title="Continue Application">
                                                    <Edit size={16}/>
                                                </button>
                                            )}

                                            {/* 2. Manage Guarantors */}
                                            {loan.status === 'GUARANTORS_PENDING' && (
                                                <button onClick={() => handleContinue(loan)} className="text-amber-600 hover:bg-amber-50 p-1.5 rounded-lg border border-transparent hover:border-amber-100 transition" title="Manage Guarantors">
                                                    <Users size={16}/>
                                                </button>
                                            )}

                                            {/* 3. Pay Fee */}
                                            {(loan.status === 'GUARANTORS_APPROVED' || loan.status === 'APPLICATION_FEE_PENDING') && (
                                                <button
                                                    onClick={() => handlePayFee(loan)}
                                                    className="bg-purple-600 text-white px-3 py-1.5 rounded-lg text-xs font-bold hover:bg-purple-700 transition shadow-md shadow-purple-600/20 flex items-center gap-1"
                                                >
                                                    Pay <ArrowRight size={12}/>
                                                </button>
                                            )}

                                            {/* 4. Delete Application (If draft/pending) */}
                                            {['DRAFT', 'GUARANTORS_PENDING', 'GUARANTORS_APPROVED', 'APPLICATION_FEE_PENDING'].includes(loan.status) && (
                                                <button onClick={() => handleDelete(loan.id)} className="text-red-500 hover:bg-red-50 p-1.5 rounded-lg border border-transparent hover:border-red-100 transition" title="Delete Application">
                                                    <Trash2 size={16}/>
                                                </button>
                                            )}

                                            {/* 5. View Details (Active) */}
                                            {['SUBMITTED', 'ACTIVE', 'DISBURSED', 'COMPLETED', 'REJECTED', 'LOAN_OFFICER_REVIEW', 'SECRETARY_TABLED', 'VOTING_OPEN', 'ADMIN_APPROVED'].includes(loan.status) && (
                                                <button className="text-slate-400 hover:text-slate-600 p-1.5 hover:bg-slate-100 rounded-lg transition">
                                                    <FileText size={16}/>
                                                </button>
                                            )}
                                        </div>
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

            {/* Application Wizard (Now handles New + Resume) */}
            <LoanApplicationModal
                isOpen={isApplyModalOpen}
                onClose={handleCloseApplyModal}
                onSuccess={fetchLoans}
                user={user}
                resumeLoan={selectedLoan} // ✅ Pass the loan to resume
            />

            {/* Fee Payment Modal */}
            <LoanFeePaymentModal
                isOpen={isPayFeeModalOpen}
                onClose={() => setIsPayFeeModalOpen(false)}
                onSuccess={fetchLoans}
                loan={selectedLoan}
            />
        </div>
    );
}