import React, { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import api from '../../../api';
import BrandedSpinner from '../../../components/BrandedSpinner';
import DashboardHeader from '../../../components/DashboardHeader';
import LoanOfficerReviewModal from './LoanOfficerReviewModal';
import {
    FileText,
    CheckCircle,
    XCircle,
    Clock,
    TrendingUp,
    AlertCircle,
    Eye,
    LayoutDashboard,
    History
} from 'lucide-react';

export default function LoanOfficerDashboard() {
    const [user, setUser] = useState(null);
    const [searchParams] = useSearchParams();
    const activeTab = searchParams.get('tab') || 'pending';
    const [loading, setLoading] = useState(true);
    const [stats, setStats] = useState(null);
    const [allLoans, setAllLoans] = useState([]);
    const [selectedLoan, setSelectedLoan] = useState(null);
    const [showReviewModal, setShowReviewModal] = useState(false);

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
        loadDashboard();
    }, []);

    const TabButton = ({ id, label, icon: Icon }) => (
        <Link
            to={`?tab=${id}`}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl font-bold text-xs sm:text-sm transition-all duration-200 border whitespace-nowrap ${
                activeTab === id
                ? 'bg-indigo-900 text-white shadow-md border-indigo-900'
                : 'bg-white text-slate-600 hover:bg-slate-50 border-slate-200'
            }`}
        >
            <Icon size={16} className={activeTab === id ? "text-indigo-200" : "text-slate-400"}/>
            {label}
        </Link>
    );

    const loadDashboard = async () => {
        setLoading(true);
        try {
            const [statsRes, loansRes] = await Promise.all([
                api.get('/api/loan-officer/statistics'),
                api.get('/api/loan-officer/pending-loans')
            ]);

            setStats(statsRes.data.data);
            setAllLoans(loansRes.data.data);
        } catch (error) {
            console.error('Failed to load dashboard:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleReviewClick = async (loanId) => {
        try {
            const res = await api.get(`/api/loan-officer/loans/${loanId}`);
            setSelectedLoan(res.data.data);
            setShowReviewModal(true);
        } catch (error) {
            console.error('Failed to load loan:', error);
            alert('Failed to load loan details');
        }
    };

    const handleReviewAction = async (loanId, action, data) => {
        try {
            if (action === 'approve') {
                await api.post(`/api/loan-officer/loans/${loanId}/approve`, {
                    approvedAmount: data.approvedAmount || selectedLoan.principalAmount,
                    notes: data.notes || ''
                });
                alert('Loan approved successfully!');
            } else if (action === 'reject') {
                await api.post(`/api/loan-officer/loans/${loanId}/reject`, {
                    reason: data.reason
                });
                alert('Loan rejected.');
            }
            setShowReviewModal(false);
            setSelectedLoan(null);
            loadDashboard(); // Reload data
        } catch (error) {
            alert(error.response?.data?.message || 'Action failed');
        }
    };

    // Filter loans by status based on active tab
    const getFilteredLoans = () => {
        switch(activeTab) {
            case 'pending':
                return allLoans.filter(l => l.status === 'SUBMITTED' || l.status === 'UNDER_REVIEW');
            case 'approved':
                return allLoans.filter(l => l.status === 'APPROVED');
            case 'rejected':
                return allLoans.filter(l => l.status === 'REJECTED');
            case 'all':
                return allLoans;
            default:
                return allLoans.filter(l => l.status === 'SUBMITTED' || l.status === 'UNDER_REVIEW');
        }
    };

    const formatCurrency = (amount) => {
        return new Intl.NumberFormat('en-KE', {
            style: 'currency',
            currency: 'KES',
            minimumFractionDigits: 0
        }).format(amount);
    };

    const getStatusBadge = (status) => {
        const badges = {
            'SUBMITTED': 'bg-blue-100 text-blue-800',
            'UNDER_REVIEW': 'bg-yellow-100 text-yellow-800',
            'APPROVED': 'bg-green-100 text-green-800',
            'REJECTED': 'bg-red-100 text-red-800'
        };
        return badges[status] || 'bg-gray-100 text-gray-800';
    };

    const filteredLoans = getFilteredLoans();

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <BrandedSpinner />
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800 pb-12">
            <DashboardHeader user={user} title="Loan Officer Portal" />

            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8">
                {/* Tabs */}
                <div className="mb-8 overflow-x-auto pb-2 scrollbar-hide">
                    <div className="flex gap-1 w-max">
                        <TabButton id="pending" label="Pending Review" icon={Clock} />
                        <TabButton id="approved" label="Approved" icon={CheckCircle} />
                        <TabButton id="rejected" label="Rejected" icon={XCircle} />
                        <TabButton id="all" label="All Loans" icon={History} />
                    </div>
                </div>

                {/* Statistics Cards */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                    <StatCard
                        icon={<Clock className="text-yellow-600" size={24} />}
                        label="Pending Review"
                        value={stats?.pendingReview || 0}
                        bgColor="bg-yellow-50"
                        textColor="text-yellow-900"
                    />
                    <StatCard
                        icon={<CheckCircle className="text-green-600" size={24} />}
                        label="Approved"
                        value={stats?.approved || 0}
                        bgColor="bg-green-50"
                        textColor="text-green-900"
                    />
                    <StatCard
                        icon={<XCircle className="text-red-600" size={24} />}
                        label="Rejected"
                        value={stats?.rejected || 0}
                        bgColor="bg-red-50"
                        textColor="text-red-900"
                    />
                    <StatCard
                        icon={<TrendingUp className="text-blue-600" size={24} />}
                        label="Active Loans"
                        value={stats?.activeLoans || 0}
                        bgColor="bg-blue-50"
                        textColor="text-blue-900"
                    />
                </div>

                {/* Financial Summary */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
                    <div className="bg-white rounded-lg shadow p-6">
                        <h3 className="text-sm font-medium text-gray-500 mb-2">Total Disbursed</h3>
                        <p className="text-3xl font-bold text-gray-900">
                            {formatCurrency(stats?.totalDisbursed || 0)}
                        </p>
                    </div>
                    <div className="bg-white rounded-lg shadow p-6">
                        <h3 className="text-sm font-medium text-gray-500 mb-2">Total Outstanding</h3>
                        <p className="text-3xl font-bold text-gray-900">
                            {formatCurrency(stats?.totalOutstanding || 0)}
                        </p>
                    </div>
                </div>

                {/* Loans Table */}
                <div className="bg-white rounded-lg shadow">
                    <div className="px-6 py-4 border-b border-gray-200">
                        <h2 className="text-xl font-semibold text-gray-900 flex items-center gap-2">
                            <FileText size={20} />
                            {activeTab === 'pending' && 'Pending Loan Applications'}
                            {activeTab === 'approved' && 'Approved Loans'}
                            {activeTab === 'rejected' && 'Rejected Loans'}
                            {activeTab === 'all' && 'All Loan Applications'}
                        </h2>
                    </div>

                    {filteredLoans.length === 0 ? (
                        <div className="p-8 text-center">
                            <AlertCircle className="mx-auto h-12 w-12 text-gray-400 mb-4" />
                            <p className="text-gray-500">No loans in this category</p>
                        </div>
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="min-w-full divide-y divide-gray-200">
                                <thead className="bg-gray-50">
                                    <tr>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                            Loan #
                                        </th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                            Applicant
                                        </th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                            Product
                                        </th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                            Amount
                                        </th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                            Guarantors
                                        </th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                            Status
                                        </th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                            Date
                                        </th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                            Actions
                                        </th>
                                    </tr>
                                </thead>
                                <tbody className="bg-white divide-y divide-gray-200">
                                    {filteredLoans.map((loan) => (
                                        <tr key={loan.id} className="hover:bg-gray-50">
                                            <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                                                {loan.loanNumber}
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap">
                                                <div className="text-sm font-medium text-gray-900">{loan.memberName}</div>
                                                <div className="text-sm text-gray-500">{loan.memberNumber}</div>
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                                                {loan.productName}
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-gray-900">
                                                {formatCurrency(loan.principalAmount)}
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                                <span className={`${loan.guarantorsApproved === loan.guarantorsCount ? 'text-green-600 font-medium' : 'text-yellow-600'}`}>
                                                    {loan.guarantorsApproved}/{loan.guarantorsCount}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap">
                                                <span className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${getStatusBadge(loan.status)}`}>
                                                    {loan.status.replace('_', ' ')}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                                {new Date(loan.applicationDate).toLocaleDateString()}
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                                                <button
                                                    onClick={() => handleReviewClick(loan.id)}
                                                    className="text-emerald-600 hover:text-emerald-900 flex items-center gap-1"
                                                >
                                                    <Eye size={16} />
                                                    Review
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            </main>

            {/* Review Modal */}
            {showReviewModal && selectedLoan && (
                <LoanOfficerReviewModal
                    loan={selectedLoan}
                    onClose={() => {
                        setShowReviewModal(false);
                        setSelectedLoan(null);
                    }}
                    onAction={handleReviewAction}
                />
            )}
        </div>
    );
}

function StatCard({ icon, label, value, bgColor, textColor }) {
    return (
        <div className={`${bgColor} rounded-lg shadow p-6`}>
            <div className="flex items-center justify-between">
                <div>
                    <p className="text-sm font-medium text-gray-600 mb-1">{label}</p>
                    <p className={`text-3xl font-bold ${textColor}`}>{value}</p>
                </div>
                <div className={`${bgColor} p-3 rounded-full`}>
                    {icon}
                </div>
            </div>
        </div>
    );
}

