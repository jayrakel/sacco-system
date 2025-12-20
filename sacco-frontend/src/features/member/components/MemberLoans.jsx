import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { CreditCard, PlusCircle, CheckCircle, Clock, XCircle, FileText, Banknote, ArrowRight, Trash2, Edit, Users, AlertCircle } from 'lucide-react';
import LoanApplicationModal from './LoanApplicationModal';
import LoanFeePaymentModal from './LoanFeePaymentModal';

export default function MemberLoans({ user }) {
    const [loans, setLoans] = useState([]);
    const [loading, setLoading] = useState(true);

    // Modal States
    const [isApplyModalOpen, setIsApplyModalOpen] = useState(false);
    const [isPayFeeModalOpen, setIsPayFeeModalOpen] = useState(false);
    const [isEligibilityModalOpen, setIsEligibilityModalOpen] = useState(false);

    // Used for both Paying Fee AND Resuming Draft
    const [selectedLoan, setSelectedLoan] = useState(null);
    const [eligibilityData, setEligibilityData] = useState(null);
    const [isEligible, setIsEligible] = useState(false);
    const [checkingEligibility, setCheckingEligibility] = useState(true);

    useEffect(() => {
        fetchLoans();
        checkEligibility(); // Check on page load
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

    // Check eligibility on page load
    const checkEligibility = async () => {
        setCheckingEligibility(true);
        try {
            const res = await api.get('/api/loans/eligibility/check');
            if (res.data.success) {
                setIsEligible(res.data.eligible);
                setEligibilityData(res.data);
            }
        } catch (e) {
            console.error("Failed to check eligibility", e);
            setIsEligible(false);
        } finally {
            setCheckingEligibility(false);
        }
    };

    // Handle button click - check eligibility and open form
    const handleApplyNewLoan = async () => {
        if (isEligible) {
            // Member is eligible - open application form directly
            console.log("Opening application form for eligible member");
            setSelectedLoan(null); // Ensure it's a new application
            setIsApplyModalOpen(true);
        } else {
            // Show why they're not eligible
            setIsEligibilityModalOpen(true);
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

    const handleCloseFeeModal = () => {
        setIsPayFeeModalOpen(false);
        setSelectedLoan(null);
    };

    const handleFeePaymentSuccess = (draftLoan) => {
        console.log("Fee payment success called with:", draftLoan);
        console.log("Current selectedLoan:", selectedLoan);

        // After successful fee payment for new application, open the application form
        if (!selectedLoan) {
            // This is a new application - set the draft loan returned from payment
            console.log("Setting draft loan and opening application modal");
            setSelectedLoan(draftLoan);
            setIsPayFeeModalOpen(false);
            setIsApplyModalOpen(true);
        } else {
            // This is for an existing loan - just refresh
            console.log("Existing loan - refreshing list");
            fetchLoans();
        }
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

                    {/* Eligibility Status Indicator */}
                    {!checkingEligibility && (
                        <div className="mt-3">
                            {isEligible ? (
                                <div className="inline-flex items-center gap-2 text-xs bg-green-50 text-green-700 px-3 py-1.5 rounded-full border border-green-200">
                                    <CheckCircle size={14}/>
                                    <span className="font-bold">Eligible for loan application</span>
                                </div>
                            ) : (
                                <div className="space-y-2">
                                    <div className="inline-flex items-center gap-2 text-xs bg-red-50 text-red-700 px-3 py-1.5 rounded-full border border-red-200">
                                        <XCircle size={14}/>
                                        <span className="font-bold">Not Eligible - Requirements:</span>
                                    </div>
                                    {eligibilityData && eligibilityData.reasons && (
                                        <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 max-w-xl">
                                            <ul className="space-y-1">
                                                {eligibilityData.reasons.map((reason, idx) => (
                                                    <li key={idx} className="flex items-start gap-2 text-xs text-amber-900">
                                                        <span className="text-amber-600 mt-0.5">•</span>
                                                        <span>{reason}</span>
                                                    </li>
                                                ))}
                                            </ul>
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    )}
                </div>
                <button
                    onClick={handleApplyNewLoan}
                    disabled={checkingEligibility || !isEligible}
                    className={`px-5 py-2.5 rounded-xl text-sm font-bold flex items-center gap-2 transition shadow-lg ${
                        checkingEligibility
                            ? 'bg-slate-300 text-slate-500 cursor-wait'
                            : isEligible
                                ? 'bg-indigo-900 text-white hover:bg-indigo-800 shadow-indigo-900/20'
                                : 'bg-slate-300 text-slate-500 cursor-not-allowed hover:bg-slate-400'
                    }`}
                >
                    {checkingEligibility ? (
                        <>
                            <Clock size={18} className="animate-spin"/> Checking...
                        </>
                    ) : (
                        <>
                            <PlusCircle size={18}/> {isEligible ? 'Apply New Loan' : 'View Requirements'}
                        </>
                    )}
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
                onClose={handleCloseFeeModal}
                onSuccess={handleFeePaymentSuccess}
                loan={selectedLoan}
                isNewApplication={!selectedLoan} // true if no loan selected (new application)
            />

            {/* Eligibility Rejection Modal */}
            {isEligibilityModalOpen && eligibilityData && (
                <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-in fade-in">
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
                        {/* Header */}
                        <div className="bg-red-50 border-b border-red-100 p-6 flex items-center gap-3">
                            <div className="p-2 bg-red-100 text-red-600 rounded-full">
                                <XCircle size={24}/>
                            </div>
                            <div>
                                <h3 className="font-bold text-red-900">Loan Application Restricted</h3>
                                <p className="text-sm text-red-600">You do not meet the eligibility requirements</p>
                            </div>
                        </div>

                        {/* Body */}
                        <div className="p-6 space-y-6">
                            {/* Reasons */}
                            <div>
                                <h4 className="text-sm font-bold text-slate-700 mb-3">Requirements Not Met:</h4>
                                <ul className="space-y-2">
                                    {eligibilityData.reasons.map((reason, idx) => (
                                        <li key={idx} className="flex items-start gap-2 text-sm text-slate-600">
                                            <AlertCircle size={16} className="text-red-500 flex-shrink-0 mt-0.5"/>
                                            <span>{reason}</span>
                                        </li>
                                    ))}
                                </ul>
                            </div>

                            {/* Current vs Required */}
                            <div className="bg-slate-50 p-4 rounded-lg space-y-3 border border-slate-200">
                                <h4 className="text-xs font-bold text-slate-500 uppercase">Your Status vs Requirements</h4>

                                <div className="grid grid-cols-2 gap-4 text-sm">
                                    <div>
                                        <p className="text-xs text-slate-400">Current Savings</p>
                                        <p className="font-bold text-slate-700">KES {Number(eligibilityData.currentSavings).toLocaleString()}</p>
                                    </div>
                                    <div>
                                        <p className="text-xs text-slate-400">Required</p>
                                        <p className="font-bold text-emerald-600">KES {Number(eligibilityData.requiredSavings).toLocaleString()}</p>
                                    </div>
                                </div>

                                {eligibilityData.requiredShareCapital > 0 && (
                                    <div className="grid grid-cols-2 gap-4 text-sm pt-2 border-t border-slate-200">
                                        <div>
                                            <p className="text-xs text-slate-400">Share Capital</p>
                                            <p className="font-bold text-slate-700">KES {Number(eligibilityData.currentShareCapital).toLocaleString()}</p>
                                        </div>
                                        <div>
                                            <p className="text-xs text-slate-400">Required</p>
                                            <p className="font-bold text-emerald-600">KES {Number(eligibilityData.requiredShareCapital).toLocaleString()}</p>
                                        </div>
                                    </div>
                                )}

                                <div className="pt-2 border-t border-slate-200 text-xs text-slate-500">
                                    <p>✓ Minimum membership: <span className="font-bold">{eligibilityData.requiredMonths} months</span></p>
                                </div>
                            </div>

                            {/* Next Steps */}
                            <div className="bg-blue-50 p-4 rounded-lg border border-blue-100">
                                <p className="text-sm text-blue-900 font-medium mb-2">💡 What you can do:</p>
                                <ul className="text-sm text-blue-700 space-y-1">
                                    <li>• Increase your savings through regular deposits</li>
                                    <li>• Purchase more share capital</li>
                                    <li>• Continue membership to meet duration requirement</li>
                                </ul>
                            </div>
                        </div>

                        {/* Footer */}
                        <div className="bg-slate-50 p-4 flex justify-end border-t border-slate-200">
                            <button
                                onClick={() => setIsEligibilityModalOpen(false)}
                                className="bg-slate-700 hover:bg-slate-800 text-white px-6 py-2 rounded-lg font-bold transition"
                            >
                                Close
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}