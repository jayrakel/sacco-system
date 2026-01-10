import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../../../api';
import BrandedSpinner from '../../../components/BrandedSpinner';
import DashboardHeader from '../../../components/DashboardHeader';
import {
    ArrowLeft,
    User,
    DollarSign,
    Calendar,
    TrendingUp,
    CheckCircle,
    XCircle,
    AlertCircle,
    Users,
    FileText
} from 'lucide-react';

export default function LoanReviewPage() {
    const { loanId } = useParams();
    const navigate = useNavigate();
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [loan, setLoan] = useState(null);
    const [showApproveModal, setShowApproveModal] = useState(false);
    const [showRejectModal, setShowRejectModal] = useState(false);
    const [processing, setProcessing] = useState(false);

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
        loadLoanDetails();
    }, [loanId]);

    const loadLoanDetails = async () => {
        setLoading(true);
        try {
            console.log('Loading loan:', loanId);
            const res = await api.get(`/api/loan-officer/loans/${loanId}`);
            console.log('Loan response:', res.data);

            if (res.data.success && res.data.data) {
                setLoan(res.data.data);
            } else {
                console.error('Invalid response structure:', res.data);
                alert('Invalid response from server');
            }
        } catch (error) {
            console.error('Failed to load loan:', error);
            console.error('Error response:', error.response?.data);
            alert(`Failed to load loan details: ${error.response?.data?.message || error.message}`);
        } finally {
            setLoading(false);
        }
    };

    const handleStartReview = async () => {
        try {
            await api.post(`/api/loan-officer/loans/${loanId}/start-review`);
            alert('Loan moved to Under Review');
            loadLoanDetails();
        } catch (error) {
            alert(error.response?.data?.message || 'Failed to start review');
        }
    };

    const formatCurrency = (amount) => {
        return new Intl.NumberFormat('en-KE', {
            style: 'currency',
            currency: 'KES',
            minimumFractionDigits: 0
        }).format(amount);
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <BrandedSpinner />
            </div>
        );
    }

    if (!loan) {
        return <div className="p-8">Loan not found</div>;
    }

    const guarantorsApproved = loan.guarantors.filter(g => g.status === 'ACCEPTED').length;
    const allGuarantorsApproved = guarantorsApproved === loan.guarantors.length;

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800 pb-12">
            <DashboardHeader user={user} title="Loan Review" />

            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8">
                {/* Header */}
                <div className="mb-6 flex items-center justify-between">
                    <button
                        onClick={() => navigate('/loan-officer/dashboard')}
                        className="flex items-center gap-2 text-gray-600 hover:text-gray-900"
                    >
                        <ArrowLeft size={20} />
                        Back to Dashboard
                    </button>
                    <StatusBadge status={loan.loanStatus} />
                </div>

                <h1 className="text-3xl font-bold text-gray-900 mb-2">Loan Application Review</h1>
                <p className="text-gray-600 mb-8">Loan Number: {loan.loanNumber}</p>

                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    {/* Main Content */}
                    <div className="lg:col-span-2 space-y-6">
                        {/* Applicant Information */}
                        <div className="bg-white rounded-lg shadow p-6">
                            <h2 className="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
                                <User size={20} />
                                Applicant Information
                            </h2>
                            <div className="grid grid-cols-2 gap-4">
                                <InfoRow label="Name" value={`${loan.member.firstName} ${loan.member.lastName}`} />
                                <InfoRow label="Member Number" value={loan.member.memberNumber} />
                                <InfoRow label="Email" value={loan.member.email} />
                                <InfoRow label="Phone" value={loan.member.phoneNumber} />
                                <InfoRow label="Member Status" value={loan.member.memberStatus} />
                                <InfoRow label="Join Date" value={new Date(loan.member.createdAt).toLocaleDateString()} />
                            </div>
                        </div>

                        {/* Loan Details */}
                        <div className="bg-white rounded-lg shadow p-6">
                            <h2 className="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
                                <DollarSign size={20} />
                                Loan Details
                            </h2>
                            <div className="grid grid-cols-2 gap-4">
                                <InfoRow label="Product" value={loan.product.productName} />
                                <InfoRow label="Requested Amount" value={formatCurrency(loan.principalAmount)} />
                                <InfoRow label="Interest Rate" value={`${loan.interestRate}% per annum`} />
                                <InfoRow label="Duration" value={`${loan.durationWeeks} weeks`} />
                                <InfoRow label="Application Date" value={new Date(loan.applicationDate).toLocaleDateString()} />
                                <InfoRow
                                    label="Weekly Repayment"
                                    value={loan.weeklyRepaymentAmount ? formatCurrency(loan.weeklyRepaymentAmount) : 'N/A'}
                                />
                            </div>
                        </div>

                        {/* Guarantors */}
                        <div className="bg-white rounded-lg shadow p-6">
                            <h2 className="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
                                <Users size={20} />
                                Guarantors ({guarantorsApproved}/{loan.guarantors.length} Approved)
                            </h2>
                            {loan.guarantors.length === 0 ? (
                                <p className="text-gray-500">No guarantors added</p>
                            ) : (
                                <div className="space-y-3">
                                    {loan.guarantors.map((guarantor) => (
                                        <div key={guarantor.id} className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                                            <div>
                                                <p className="font-medium text-gray-900">
                                                    {guarantor.member.firstName} {guarantor.member.lastName}
                                                </p>
                                                <p className="text-sm text-gray-500">{guarantor.member.memberNumber}</p>
                                            </div>
                                            <div className="text-right">
                                                <p className="font-semibold text-gray-900">
                                                    {formatCurrency(guarantor.guaranteedAmount)}
                                                </p>
                                                <span className={`text-xs px-2 py-1 rounded-full ${
                                                    guarantor.status === 'ACCEPTED' ? 'bg-green-100 text-green-800' :
                                                    guarantor.status === 'REJECTED' ? 'bg-red-100 text-red-800' :
                                                    'bg-yellow-100 text-yellow-800'
                                                }`}>
                                                    {guarantor.status}
                                                </span>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Actions Sidebar */}
                    <div className="space-y-6">
                        {/* Quick Stats */}
                        <div className="bg-white rounded-lg shadow p-6">
                            <h3 className="text-lg font-semibold text-gray-900 mb-4">Quick Stats</h3>
                            <div className="space-y-3">
                                <QuickStat
                                    icon={<DollarSign size={16} />}
                                    label="Requested"
                                    value={formatCurrency(loan.principalAmount)}
                                />
                                <QuickStat
                                    icon={<Users size={16} />}
                                    label="Guarantors"
                                    value={`${guarantorsApproved}/${loan.guarantors.length}`}
                                    highlighted={allGuarantorsApproved}
                                />
                                <QuickStat
                                    icon={<Calendar size={16} />}
                                    label="Duration"
                                    value={`${loan.durationWeeks} weeks`}
                                />
                            </div>
                        </div>

                        {/* Actions */}
                        <div className="bg-white rounded-lg shadow p-6">
                            <h3 className="text-lg font-semibold text-gray-900 mb-4">Actions</h3>
                            <div className="space-y-3">
                                {loan.loanStatus === 'SUBMITTED' && (
                                    <button
                                        onClick={handleStartReview}
                                        className="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-4 rounded-lg flex items-center justify-center gap-2"
                                    >
                                        <FileText size={18} />
                                        Start Review
                                    </button>
                                )}

                                {(loan.loanStatus === 'SUBMITTED' || loan.loanStatus === 'UNDER_REVIEW') && (
                                    <>
                                        <button
                                            onClick={() => setShowApproveModal(true)}
                                            disabled={!allGuarantorsApproved}
                                            className="w-full bg-green-600 hover:bg-green-700 disabled:bg-gray-300 disabled:cursor-not-allowed text-white font-medium py-2 px-4 rounded-lg flex items-center justify-center gap-2"
                                        >
                                            <CheckCircle size={18} />
                                            Approve Loan
                                        </button>

                                        <button
                                            onClick={() => setShowRejectModal(true)}
                                            className="w-full bg-red-600 hover:bg-red-700 text-white font-medium py-2 px-4 rounded-lg flex items-center justify-center gap-2"
                                        >
                                            <XCircle size={18} />
                                            Reject Loan
                                        </button>
                                    </>
                                )}

                                {!allGuarantorsApproved && (
                                    <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3 mt-4">
                                        <p className="text-sm text-yellow-800 flex items-start gap-2">
                                            <AlertCircle size={16} className="mt-0.5 flex-shrink-0" />
                                            All guarantors must approve before you can approve this loan
                                        </p>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            </main>

            {/* Modals */}
            {showApproveModal && (
                <ApproveModal
                    loan={loan}
                    onClose={() => setShowApproveModal(false)}
                    onSuccess={() => {
                        setShowApproveModal(false);
                        navigate('/loan-officer/dashboard');
                    }}
                />
            )}

            {showRejectModal && (
                <RejectModal
                    loan={loan}
                    onClose={() => setShowRejectModal(false)}
                    onSuccess={() => {
                        setShowRejectModal(false);
                        navigate('/loan-officer/dashboard');
                    }}
                />
            )}
        </div>
    );
}

function StatusBadge({ status }) {
    const badges = {
        'SUBMITTED': { bg: 'bg-blue-100', text: 'text-blue-800', label: 'Submitted' },
        'UNDER_REVIEW': { bg: 'bg-yellow-100', text: 'text-yellow-800', label: 'Under Review' },
        'APPROVED': { bg: 'bg-green-100', text: 'text-green-800', label: 'Approved' },
        'REJECTED': { bg: 'bg-red-100', text: 'text-red-800', label: 'Rejected' }
    };
    const badge = badges[status] || badges['SUBMITTED'];

    return (
        <span className={`px-4 py-2 rounded-full text-sm font-semibold ${badge.bg} ${badge.text}`}>
            {badge.label}
        </span>
    );
}

function InfoRow({ label, value }) {
    return (
        <div>
            <p className="text-sm text-gray-500">{label}</p>
            <p className="font-medium text-gray-900">{value}</p>
        </div>
    );
}

function QuickStat({ icon, label, value, highlighted }) {
    return (
        <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-gray-600">
                {icon}
                <span className="text-sm">{label}</span>
            </div>
            <span className={`font-semibold ${highlighted ? 'text-green-600' : 'text-gray-900'}`}>
                {value}
            </span>
        </div>
    );
}

function ApproveModal({ loan, onClose, onSuccess }) {
    const [approvedAmount, setApprovedAmount] = useState(loan.principalAmount);
    const [notes, setNotes] = useState('');
    const [processing, setProcessing] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setProcessing(true);

        try {
            await api.post(`/api/loan-officer/loans/${loan.id}/approve`, {
                approvedAmount: Number(approvedAmount),
                notes
            });
            alert('Loan approved successfully! Applicant has been notified.');
            onSuccess();
        } catch (error) {
            alert(error.response?.data?.message || 'Failed to approve loan');
        } finally {
            setProcessing(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
            <div className="bg-white rounded-lg max-w-md w-full p-6">
                <h2 className="text-2xl font-bold text-gray-900 mb-4 flex items-center gap-2">
                    <CheckCircle className="text-green-600" size={24} />
                    Approve Loan
                </h2>

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            Approved Amount (KES)
                        </label>
                        <input
                            type="number"
                            value={approvedAmount}
                            onChange={(e) => setApprovedAmount(e.target.value)}
                            max={loan.principalAmount}
                            required
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
                        />
                        <p className="text-sm text-gray-500 mt-1">
                            Maximum: {new Intl.NumberFormat('en-KE', { style: 'currency', currency: 'KES' }).format(loan.principalAmount)}
                        </p>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            Notes (Optional)
                        </label>
                        <textarea
                            value={notes}
                            onChange={(e) => setNotes(e.target.value)}
                            rows="3"
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
                            placeholder="Add any notes about this approval..."
                        />
                    </div>

                    <div className="flex gap-3 pt-4">
                        <button
                            type="button"
                            onClick={onClose}
                            className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            disabled={processing}
                            className="flex-1 px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg disabled:bg-gray-300"
                        >
                            {processing ? 'Processing...' : 'Approve Loan'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

function RejectModal({ loan, onClose, onSuccess }) {
    const [reason, setReason] = useState('');
    const [processing, setProcessing] = useState(false);

    const predefinedReasons = [
        'Insufficient credit history',
        'Guarantor capacity exceeded',
        'Incomplete documentation',
        'Member has outstanding arrears',
        'Does not meet eligibility criteria'
    ];

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!reason.trim()) {
            alert('Please provide a rejection reason');
            return;
        }

        setProcessing(true);

        try {
            await api.post(`/api/loan-officer/loans/${loan.id}/reject`, { reason });
            alert('Loan rejected. Applicant has been notified.');
            onSuccess();
        } catch (error) {
            alert(error.response?.data?.message || 'Failed to reject loan');
        } finally {
            setProcessing(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
            <div className="bg-white rounded-lg max-w-md w-full p-6">
                <h2 className="text-2xl font-bold text-gray-900 mb-4 flex items-center gap-2">
                    <XCircle className="text-red-600" size={24} />
                    Reject Loan
                </h2>

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            Quick Select
                        </label>
                        <div className="space-y-2">
                            {predefinedReasons.map((preReason) => (
                                <button
                                    key={preReason}
                                    type="button"
                                    onClick={() => setReason(preReason)}
                                    className="w-full text-left px-3 py-2 text-sm border border-gray-200 rounded-lg hover:bg-gray-50"
                                >
                                    {preReason}
                                </button>
                            ))}
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            Rejection Reason *
                        </label>
                        <textarea
                            value={reason}
                            onChange={(e) => setReason(e.target.value)}
                            rows="4"
                            required
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
                            placeholder="Provide a detailed reason for rejection..."
                        />
                    </div>

                    <div className="bg-red-50 border border-red-200 rounded-lg p-3">
                        <p className="text-sm text-red-800">
                            ⚠️ This action will notify the applicant and cannot be undone.
                        </p>
                    </div>

                    <div className="flex gap-3 pt-4">
                        <button
                            type="button"
                            onClick={onClose}
                            className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            disabled={processing}
                            className="flex-1 px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg disabled:bg-gray-300"
                        >
                            {processing ? 'Processing...' : 'Reject Loan'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

