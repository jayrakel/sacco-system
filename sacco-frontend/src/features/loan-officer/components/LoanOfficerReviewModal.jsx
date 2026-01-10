import React, { useState } from 'react';
import api from '../../../api';
import { X, User, Briefcase, CheckCircle, XCircle, DollarSign, Calendar, Users } from 'lucide-react';
import BrandedSpinner from '../../../components/BrandedSpinner';

export default function LoanOfficerReviewModal({ loan, onClose, onAction }) {
    const [loading, setLoading] = useState(false);
    const [approvedAmount, setApprovedAmount] = useState(loan.principalAmount);
    const [notes, setNotes] = useState('');
    const [rejectionReason, setRejectionReason] = useState('');
    const [showApproveConfirm, setShowApproveConfirm] = useState(false);
    const [showRejectConfirm, setShowRejectConfirm] = useState(false);

    const formatCurrency = (amount) => {
        return new Intl.NumberFormat('en-KE', {
            style: 'currency',
            currency: 'KES',
            minimumFractionDigits: 0
        }).format(amount);
    };

    const handleStartReview = async () => {
        if (loan.loanStatus === 'SUBMITTED') {
            try {
                setLoading(true);
                await api.post(`/api/loan-officer/loans/${loan.id}/start-review`);
                alert('Loan moved to Under Review');
                onClose();
                window.location.reload(); // Refresh to show updated status
            } catch (error) {
                alert(error.response?.data?.message || 'Failed to start review');
            } finally {
                setLoading(false);
            }
        }
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
                            </div>

                            {/* RIGHT COLUMN: ACTIONS */}
                            <div className="space-y-6">
                                {/* Quick Actions */}
                                <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-200 space-y-4">
                                    <h3 className="font-bold text-slate-700 border-b pb-2">Officer Actions</h3>

                                    {loan.loanStatus === 'SUBMITTED' && (
                                        <button
                                            onClick={handleStartReview}
                                            className="w-full py-2 px-4 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-lg transition"
                                        >
                                            Start Review
                                        </button>
                                    )}

                                    {/* Approve Section */}
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
                            <h3 className="text-lg font-bold text-slate-800 mb-4">Confirm Approval</h3>
                            <p className="text-slate-600 mb-4">
                                Are you sure you want to approve this loan for <strong>{formatCurrency(approvedAmount)}</strong>?
                            </p>
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
                                    Confirm
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

