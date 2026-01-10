import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { X, User, Briefcase, CheckCircle, XCircle, DollarSign, Calendar, Users, History, TrendingUp } from 'lucide-react';
import BrandedSpinner from '../../../components/BrandedSpinner';

export default function LoanOfficerReviewModal({ loan, onClose, onAction }) {
    const [loading, setLoading] = useState(false);
    const [memberHistory, setMemberHistory] = useState(null);
    const [loadingHistory, setLoadingHistory] = useState(true);
    const [approvedAmount, setApprovedAmount] = useState(loan.principalAmount);
    const [notes, setNotes] = useState('');
    const [rejectionReason, setRejectionReason] = useState('');
    const [showApproveConfirm, setShowApproveConfirm] = useState(false);
    const [showRejectConfirm, setShowRejectConfirm] = useState(false);

    useEffect(() => {
        loadMemberHistory();
    }, []);

    const loadMemberHistory = async () => {
        setLoadingHistory(true);
        try {
            // Fetch member's loan history
            const historyRes = await api.get(`/api/loans/member/${loan.member.id}`);
            setMemberHistory(historyRes.data.data || []);
        } catch (error) {
            console.error('Failed to load member history:', error);
        } finally {
            setLoadingHistory(false);
        }
    };

    const formatCurrency = (amount) => {
        return new Intl.NumberFormat('en-KE', {
            style: 'currency',
            currency: 'KES',
            minimumFractionDigits: 0
        }).format(amount);
    };


    const handleApprove = () => {
        if (!approvedAmount || approvedAmount <= 0) {
            return alert('Please enter a valid approved amount');
        }
        if (approvedAmount > loan.principalAmount) {
            return alert('Approved amount cannot exceed requested amount');
        }
        setShowApproveConfirm(true);
    };

    const confirmApprove = async () => {
        setLoading(true);
        try {
            await onAction(loan.id, 'approve', { approvedAmount, notes });
            setShowApproveConfirm(false);
        } catch (error) {
            alert('Approval failed');
        } finally {
            setLoading(false);
        }
    };

    const handleReject = () => {
        if (!rejectionReason.trim()) {
            return alert('Please provide a rejection reason');
        }
        setShowRejectConfirm(true);
    };

    const confirmReject = async () => {
        setLoading(true);
        try {
            await onAction(loan.id, 'reject', { reason: rejectionReason });
            setShowRejectConfirm(false);
        } catch (error) {
            alert('Rejection failed');
        } finally {
            setLoading(false);
        }
    };

    if (!loan) return null;

    const guarantorsApproved = loan.guarantors?.filter(g => g.status === 'ACCEPTED').length || 0;
    const allGuarantorsApproved = guarantorsApproved === (loan.guarantors?.length || 0);

    return (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-in fade-in">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-5xl max-h-[90vh] overflow-hidden flex flex-col">

                {/* Header */}
                <div className="bg-slate-50 p-6 border-b border-slate-100 flex justify-between items-center">
                    <div>
                        <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
                            <Briefcase className="text-indigo-600"/> Loan Officer Review
                        </h2>
                        <p className="text-sm text-slate-500">Loan #: <span className="font-mono font-bold">{loan.loanNumber}</span></p>
                    </div>
                    <button onClick={onClose} className="p-2 hover:bg-slate-200 rounded-full transition">
                        <X size={20}/>
                    </button>
                </div>

                {/* Content */}
                <div className="flex-1 overflow-y-auto p-6 bg-slate-50/30">
                    {loading ? (
                        <div className="h-64 flex items-center justify-center"><BrandedSpinner /></div>
                    ) : (
                        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

                            {/* LEFT COLUMN: APPLICATION DETAILS */}
                            <div className="lg:col-span-2 space-y-6">
                                {/* Applicant Information */}
                                <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-200">
                                    <h3 className="font-bold text-slate-700 mb-4 border-b pb-2 flex items-center gap-2">
                                        <User size={18}/> Applicant Information
                                    </h3>
                                    <div className="grid grid-cols-2 gap-4">
                                        <div>
                                            <p className="text-xs text-slate-400 uppercase font-bold">Name</p>
                                            <p className="font-bold text-slate-800">{loan.member.firstName} {loan.member.lastName}</p>
                                        </div>
                                        <div>
                                            <p className="text-xs text-slate-400 uppercase font-bold">Member Number</p>
                                            <p className="font-bold text-slate-800">{loan.member.memberNumber}</p>
                                        </div>
                                        <div>
                                            <p className="text-xs text-slate-400 uppercase font-bold">Email</p>
                                            <p className="text-slate-700">{loan.member.email}</p>
                                        </div>
                                        <div>
                                            <p className="text-xs text-slate-400 uppercase font-bold">Phone</p>
                                            <p className="text-slate-700">{loan.member.phoneNumber}</p>
                                        </div>
                                        <div>
                                            <p className="text-xs text-slate-400 uppercase font-bold">Status</p>
                                            <p className="text-slate-700">{loan.member.memberStatus}</p>
                                        </div>
                                        <div>
                                            <p className="text-xs text-slate-400 uppercase font-bold">Member Since</p>
                                            <p className="text-slate-700">{new Date(loan.member.createdAt).toLocaleDateString()}</p>
                                        </div>
                                    </div>
                                </div>

                                {/* Loan Details */}
                                <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-200">
                                    <h3 className="font-bold text-slate-700 mb-4 border-b pb-2 flex items-center gap-2">
                                        <DollarSign size={18}/> Loan Details
                                    </h3>
                                    <div className="grid grid-cols-2 gap-4">
                                        <div>
                                            <p className="text-xs text-slate-400 uppercase font-bold">Product</p>
                                            <p className="font-bold text-slate-800">{loan.product.productName}</p>
                                        </div>
                                        <div>
                                            <p className="text-xs text-slate-400 uppercase font-bold">Amount Requested</p>
                                            <p className="text-xl font-bold text-emerald-600">{formatCurrency(loan.principalAmount)}</p>
                                        </div>
                                        <div>
                                            <p className="text-xs text-slate-400 uppercase font-bold">Interest Rate</p>
                                            <p className="font-bold text-slate-700">{loan.interestRate}% per annum</p>
                                        </div>
                                        <div>
                                            <p className="text-xs text-slate-400 uppercase font-bold">Duration</p>
                                            <p className="font-bold text-slate-700">{loan.durationWeeks} weeks</p>
                                        </div>
                                        <div>
                                            <p className="text-xs text-slate-400 uppercase font-bold">Application Date</p>
                                            <p className="font-bold text-slate-700">{new Date(loan.applicationDate).toLocaleDateString()}</p>
                                        </div>
                                        <div>
                                            <p className="text-xs text-slate-400 uppercase font-bold">Current Status</p>
                                            <span className="px-2 py-1 text-xs rounded-full bg-blue-100 text-blue-800 font-bold">
                                                {loan.loanStatus.replace('_', ' ')}
                                            </span>
                                        </div>
                                    </div>
                                </div>

                                {/* Guarantors */}
                                <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-200">
                                    <h3 className="font-bold text-slate-700 mb-4 flex items-center gap-2 border-b pb-2">
                                        <Users size={18}/> Guarantors ({guarantorsApproved}/{loan.guarantors?.length || 0} Approved)
                                    </h3>
                                    {!loan.guarantors || loan.guarantors.length === 0 ? (
                                        <p className="text-slate-400 text-sm italic">No guarantors added</p>
                                    ) : (
                                        <div className="space-y-3">
                                            {loan.guarantors.map((guarantor) => (
                                                <div key={guarantor.id} className="flex items-center justify-between p-4 bg-slate-50 rounded-lg">
                                                    <div>
                                                        <p className="font-bold text-slate-800">
                                                            {guarantor.member.firstName} {guarantor.member.lastName}
                                                        </p>
                                                        <p className="text-sm text-slate-500">{guarantor.member.memberNumber}</p>
                                                    </div>
                                                    <div className="text-right">
                                                        <p className="font-bold text-slate-800">{formatCurrency(guarantor.guaranteedAmount)}</p>
                                                        <span className={`text-xs px-2 py-1 rounded-full font-bold ${
                                                            guarantor.status === 'ACCEPTED' ? 'bg-green-100 text-green-700' :
                                                            guarantor.status === 'REJECTED' ? 'bg-red-100 text-red-700' :
                                                            'bg-yellow-100 text-yellow-700'
                                                        }`}>
                                                            {guarantor.status}
                                                        </span>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                    {!allGuarantorsApproved && (
                                        <div className="mt-4 bg-yellow-50 border border-yellow-200 rounded-lg p-3">
                                            <p className="text-sm text-yellow-800">⚠️ Not all guarantors have approved yet</p>
                                        </div>
                                    )}
                                </div>

                                {/* Member Financial History */}
                                <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-200">
                                    <h3 className="font-bold text-slate-700 mb-4 flex items-center gap-2 border-b pb-2">
                                        <History size={18}/> Member Financial History
                                    </h3>
                                    {loadingHistory ? (
                                        <div className="flex justify-center py-4"><BrandedSpinner /></div>
                                    ) : !memberHistory || memberHistory.length === 0 ? (
                                        <p className="text-slate-400 text-sm italic">No previous loan history</p>
                                    ) : (
                                        <div className="space-y-3 max-h-60 overflow-y-auto">
                                            {memberHistory.filter(l => l.id !== loan.id).map((pastLoan) => (
                                                <div key={pastLoan.id} className="p-3 bg-slate-50 rounded-lg text-sm">
                                                    <div className="flex justify-between items-start">
                                                        <div>
                                                            <p className="font-bold text-slate-800">{pastLoan.loanNumber}</p>
                                                            <p className="text-xs text-slate-500">{pastLoan.product?.productName || 'N/A'}</p>
                                                        </div>
                                                        <span className={`text-xs px-2 py-1 rounded-full font-bold ${
                                                            pastLoan.loanStatus === 'CLOSED' ? 'bg-green-100 text-green-700' :
                                                            pastLoan.loanStatus === 'ACTIVE' ? 'bg-blue-100 text-blue-700' :
                                                            pastLoan.loanStatus === 'DEFAULTED' ? 'bg-red-100 text-red-700' :
                                                            'bg-gray-100 text-gray-700'
                                                        }`}>
                                                            {pastLoan.loanStatus}
                                                        </span>
                                                    </div>
                                                    <div className="mt-2 grid grid-cols-2 gap-2 text-xs">
                                                        <div>
                                                            <span className="text-slate-400">Principal:</span>
                                                            <span className="font-bold ml-1">{formatCurrency(pastLoan.principalAmount)}</span>
                                                        </div>
                                                        {pastLoan.loanStatus === 'ACTIVE' && (
                                                            <div>
                                                                <span className="text-slate-400">Outstanding:</span>
                                                                <span className="font-bold ml-1 text-orange-600">
                                                                    {formatCurrency(pastLoan.totalOutstandingAmount || 0)}
                                                                </span>
                                                            </div>
                                                        )}
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            </div>

                            {/* RIGHT COLUMN: ACTIONS */}
                            <div className="space-y-6">
                                {/* Quick Actions */}
                                <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-200 space-y-4">
                                    <h3 className="font-bold text-slate-700 border-b pb-2">Loan Officer Decision</h3>

                                    {/* Approve/Reject Section - Available for SUBMITTED or UNDER_REVIEW loans */}
                                    {(loan.loanStatus === 'SUBMITTED' || loan.loanStatus === 'UNDER_REVIEW') && (
                                        <>
                                            <div>
                                                <label className="block text-xs font-bold text-slate-600 mb-2">Approved Amount (KES)</label>
                                                <input
                                                    type="number"
                                                    value={approvedAmount}
                                                    onChange={(e) => setApprovedAmount(Number(e.target.value))}
                                                    max={loan.principalAmount}
                                                    className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-green-500 outline-none"
                                                />
                                                <p className="text-xs text-slate-400 mt-1">Max: {formatCurrency(loan.principalAmount)}</p>
                                            </div>

                                            <div>
                                                <label className="block text-xs font-bold text-slate-600 mb-2">Notes (Optional)</label>
                                                <textarea
                                                    value={notes}
                                                    onChange={(e) => setNotes(e.target.value)}
                                                    rows="3"
                                                    className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-green-500 outline-none text-sm"
                                                    placeholder="Add approval notes..."
                                                />
                                            </div>

                                            <button
                                                onClick={handleApprove}
                                                disabled={!allGuarantorsApproved}
                                                className="w-full py-3 px-4 bg-emerald-600 hover:bg-emerald-700 disabled:bg-slate-300 disabled:cursor-not-allowed text-white font-bold rounded-lg flex items-center justify-center gap-2 transition shadow-lg"
                                            >
                                                <CheckCircle size={18}/> Approve Loan
                                            </button>

                                            {/* Reject Section */}
                                            <div className="border-t pt-4">
                                                <label className="block text-xs font-bold text-slate-600 mb-2">Rejection Reason *</label>
                                                <textarea
                                                    value={rejectionReason}
                                                    onChange={(e) => setRejectionReason(e.target.value)}
                                                    rows="3"
                                                    className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-red-500 outline-none text-sm"
                                                    placeholder="Required for rejection..."
                                                />
                                            </div>

                                            <button
                                                onClick={handleReject}
                                                className="w-full py-3 px-4 bg-red-600 hover:bg-red-700 text-white font-bold rounded-lg flex items-center justify-center gap-2 transition"
                                            >
                                                <XCircle size={18}/> Reject Loan
                                            </button>
                                        </>
                                    )}
                                </div>
                            </div>
                        </div>
                    )}
                </div>

                {/* Approve Confirmation Modal */}
                {showApproveConfirm && (
                    <div className="absolute inset-0 bg-black/50 flex items-center justify-center p-4">
                        <div className="bg-white rounded-lg p-6 max-w-md w-full">
                            <h3 className="text-lg font-bold text-slate-800 mb-4 flex items-center gap-2">
                                <CheckCircle className="text-green-600" size={24}/>
                                Approve & Forward to Committee
                            </h3>
                            <p className="text-slate-600 mb-2">
                                Are you sure you want to approve this loan for <strong>{formatCurrency(approvedAmount)}</strong>?
                            </p>
                            <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 mb-4">
                                <p className="text-sm text-blue-800">
                                    ℹ️ This loan will be forwarded to the committee/secretary for final approval before disbursement.
                                </p>
                            </div>
                            <div className="flex gap-3">
                                <button
                                    onClick={() => setShowApproveConfirm(false)}
                                    className="flex-1 px-4 py-2 border border-slate-300 rounded-lg hover:bg-slate-50"
                                >
                                    Cancel
                                </button>
                                <button
                                    onClick={confirmApprove}
                                    className="flex-1 px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg"
                                >
                                    Approve & Forward
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                {/* Reject Confirmation Modal */}
                {showRejectConfirm && (
                    <div className="absolute inset-0 bg-black/50 flex items-center justify-center p-4">
                        <div className="bg-white rounded-lg p-6 max-w-md w-full">
                            <h3 className="text-lg font-bold text-slate-800 mb-4">Confirm Rejection</h3>
                            <p className="text-slate-600 mb-4">
                                Are you sure you want to reject this loan application? The applicant will be notified.
                            </p>
                            <div className="flex gap-3">
                                <button
                                    onClick={() => setShowRejectConfirm(false)}
                                    className="flex-1 px-4 py-2 border border-slate-300 rounded-lg hover:bg-slate-50"
                                >
                                    Cancel
                                </button>
                                <button
                                    onClick={confirmReject}
                                    className="flex-1 px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg"
                                >
                                    Confirm Rejection
                                </button>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}

