import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import api from '../api';
import BrandedSpinner from '../components/BrandedSpinner';
import DashboardHeader from '../components/DashboardHeader';
import {
    DollarSign,
    TrendingUp,
    Clock,
    CheckCircle,
    AlertCircle,
    Eye,
    Send,
    History,
    RefreshCw,
    XCircle,
    Calendar,
    User,
    FileText
} from 'lucide-react';

export default function TreasurerDashboard() {
    const [user, setUser] = useState(null);
    const [searchParams] = useSearchParams();
    const activeTab = searchParams.get('tab') || 'pending';
    const [loading, setLoading] = useState(true);
    const [pendingDisbursements, setPendingDisbursements] = useState([]);
    const [disbursedLoans, setDisbursedLoans] = useState([]);
    const [statistics, setStatistics] = useState({
        pendingCount: 0,
        pendingAmount: 0,
        disbursedCount: 0,
        disbursedAmount: 0,
        todayDisbursed: 0,
        todayAmount: 0
    });
    const [selectedLoan, setSelectedLoan] = useState(null);
    const [showDisbursementModal, setShowDisbursementModal] = useState(false);
    const [lastRefresh, setLastRefresh] = useState(null);

    const loadDashboard = useCallback(async () => {
        if (showDisbursementModal) return;

        setLoading(true);
        try {
            const [pendingRes, disbursedRes, statsRes] = await Promise.all([
                api.get('/api/finance/loans/pending-disbursement'),
                api.get('/api/finance/loans/disbursed'),
                api.get('/api/finance/statistics')
            ]);

            setPendingDisbursements(pendingRes.data.data || []);
            setDisbursedLoans(disbursedRes.data.data || []);
            setStatistics(statsRes.data.data || statistics);
            setLastRefresh(new Date());
        } catch (error) {
            console.error('Failed to load treasurer dashboard:', error);
        } finally {
            setLoading(false);
        }
    }, [showDisbursementModal]);

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
        loadDashboard();
    }, []);

    useEffect(() => {
        const interval = setInterval(() => {
            if (!showDisbursementModal) {
                loadDashboard();
            }
        }, 30000);
        return () => clearInterval(interval);
    }, [showDisbursementModal, loadDashboard]);

    const handleDisburseLoan = (loan) => {
        setSelectedLoan(loan);
        setShowDisbursementModal(true);
    };

    const TabButton = ({ id, label, icon: Icon }) => (
        <Link
            to={`?tab=${id}`}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl font-bold text-xs sm:text-sm transition-all duration-200 border whitespace-nowrap ${
                activeTab === id
                    ? 'bg-indigo-900 text-white shadow-md border-indigo-900'
                    : 'bg-white text-slate-600 hover:bg-slate-50 border-slate-200'
            }`}
        >
            <Icon size={16} className={activeTab === id ? "text-indigo-200" : "text-slate-400"} />
            {label}
        </Link>
    );

    const formatCurrency = (amount) => {
        return new Intl.NumberFormat('en-KE', {
            style: 'currency',
            currency: 'KES',
            minimumFractionDigits: 0
        }).format(amount || 0);
    };

    if (loading && !lastRefresh) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <BrandedSpinner />
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800 pb-12">
            <DashboardHeader user={user} title="Treasurer Portal" />

            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8">
                <div className="mb-6 flex items-center justify-between">
                    <div>
                        <h1 className="text-2xl font-bold text-slate-800">Finance & Disbursement Management</h1>
                        {lastRefresh && (
                            <p className="text-sm text-slate-500 mt-1">
                                Last updated: {lastRefresh.toLocaleTimeString()}
                            </p>
                        )}
                    </div>
                    <button
                        onClick={() => loadDashboard()}
                        disabled={loading}
                        className="flex items-center gap-2 px-4 py-2 bg-white border border-slate-200 rounded-lg hover:bg-slate-50 transition disabled:opacity-50"
                    >
                        <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
                        <span className="text-sm font-medium">Refresh</span>
                    </button>
                </div>

                <div className="mb-8 overflow-x-auto pb-2 scrollbar-hide">
                    <div className="flex gap-1 w-max">
                        <TabButton id="pending" label="Pending Disbursement" icon={Clock} />
                        <TabButton id="disbursed" label="Disbursed Loans" icon={CheckCircle} />
                        <TabButton id="history" label="Transaction History" icon={History} />
                    </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
                    <StatCard
                        icon={<Clock className="text-amber-600" size={24} />}
                        label="Pending Disbursement"
                        value={statistics.pendingCount}
                        subtitle={formatCurrency(statistics.pendingAmount)}
                        bgColor="bg-amber-50"
                        textColor="text-amber-900"
                    />
                    <StatCard
                        icon={<CheckCircle className="text-green-600" size={24} />}
                        label="Total Disbursed"
                        value={statistics.disbursedCount}
                        subtitle={formatCurrency(statistics.disbursedAmount)}
                        bgColor="bg-green-50"
                        textColor="text-green-900"
                    />
                    <StatCard
                        icon={<TrendingUp className="text-blue-600" size={24} />}
                        label="Today's Disbursements"
                        value={statistics.todayDisbursed}
                        subtitle={formatCurrency(statistics.todayAmount)}
                        bgColor="bg-blue-50"
                        textColor="text-blue-900"
                    />
                    <StatCard
                        icon={<DollarSign className="text-purple-600" size={24} />}
                        label="Avg Loan Amount"
                        value={statistics.disbursedCount > 0 ? formatCurrency(statistics.disbursedAmount / statistics.disbursedCount) : 'KES 0'}
                        subtitle="Per loan"
                        bgColor="bg-purple-50"
                        textColor="text-purple-900"
                    />
                </div>

                {activeTab === 'pending' && (
                    <PendingDisbursementSection
                        loans={pendingDisbursements}
                        onDisburse={handleDisburseLoan}
                        formatCurrency={formatCurrency}
                    />
                )}

                {activeTab === 'disbursed' && (
                    <DisbursedLoansSection
                        loans={disbursedLoans}
                        formatCurrency={formatCurrency}
                    />
                )}

                {activeTab === 'history' && (
                    <div className="bg-white rounded-lg shadow p-8 text-center">
                        <History className="mx-auto h-12 w-12 text-slate-400 mb-4" />
                        <p className="text-slate-500">Transaction history coming soon...</p>
                    </div>
                )}
            </main>

            {showDisbursementModal && (
                <DisbursementModal
                    loan={selectedLoan}
                    onClose={() => {
                        setShowDisbursementModal(false);
                        setSelectedLoan(null);
                    }}
                    onSuccess={() => {
                        setShowDisbursementModal(false);
                        setSelectedLoan(null);
                        loadDashboard();
                    }}
                    formatCurrency={formatCurrency}
                />
            )}
        </div>
    );
}

function StatCard({ icon, label, value, subtitle, bgColor, textColor }) {
    return (
        <div className={`${bgColor} rounded-lg shadow p-6`}>
            <div className="flex items-center justify-between mb-2">
                <div className="opacity-75">{icon}</div>
            </div>
            <p className="text-sm font-medium text-gray-600 mb-1">{label}</p>
            <p className={`text-2xl font-bold ${textColor}`}>{value}</p>
            {subtitle && <p className="text-xs text-gray-500 mt-1">{subtitle}</p>}
        </div>
    );
}

function PendingDisbursementSection({ loans, onDisburse, formatCurrency }) {
    return (
        <div className="bg-white rounded-lg shadow">
            <div className="px-6 py-4 border-b border-gray-200">
                <h2 className="text-xl font-semibold text-gray-900 flex items-center gap-2">
                    <Clock size={20} />
                    Loans Awaiting Disbursement
                </h2>
                <p className="text-sm text-slate-500 mt-1">
                    Loans approved by committee, ready for fund disbursement
                </p>
            </div>

            {loans.length === 0 ? (
                <div className="p-8 text-center">
                    <CheckCircle className="mx-auto h-12 w-12 text-green-400 mb-4" />
                    <p className="text-slate-500">No loans pending disbursement</p>
                    <p className="text-sm text-slate-400 mt-1">All approved loans have been disbursed</p>
                </div>
            ) : (
                <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-200">
                        <thead className="bg-gray-50">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Loan #</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Member</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Product</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Approved Amount</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Duration</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Approval Date</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="bg-white divide-y divide-gray-200">
                            {loans.map((loan) => (
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
                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-green-600">
                                        {formatCurrency(loan.approvedAmount)}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                                        {loan.durationWeeks} weeks
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        {new Date(loan.approvalDate).toLocaleDateString()}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                                        <button
                                            onClick={() => onDisburse(loan)}
                                            className="flex items-center gap-1 px-3 py-1.5 bg-green-600 hover:bg-green-700 text-white rounded-lg font-semibold transition"
                                        >
                                            <Send size={14} />
                                            Disburse
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
}

function DisbursedLoansSection({ loans, formatCurrency }) {
    return (
        <div className="bg-white rounded-lg shadow">
            <div className="px-6 py-4 border-b border-gray-200">
                <h2 className="text-xl font-semibold text-gray-900 flex items-center gap-2">
                    <CheckCircle size={20} />
                    Disbursed Loans
                </h2>
                <p className="text-sm text-slate-500 mt-1">
                    Successfully disbursed loans history
                </p>
            </div>

            {loans.length === 0 ? (
                <div className="p-8 text-center">
                    <XCircle className="mx-auto h-12 w-12 text-slate-400 mb-4" />
                    <p className="text-slate-500">No loans disbursed yet</p>
                    <p className="text-sm text-slate-400 mt-1">Disbursed loans will appear here</p>
                </div>
            ) : (
                <div className="p-6 space-y-4">
                    {loans.map((loan) => (
                        <div key={loan.id} className="border border-slate-200 rounded-lg p-4 hover:shadow-md transition">
                            <div className="flex justify-between items-start">
                                <div className="flex-1">
                                    <div className="flex items-center gap-3 mb-2">
                                        <h3 className="text-lg font-bold text-slate-800">{loan.loanNumber}</h3>
                                        <span className="px-2 py-1 text-xs font-semibold rounded-full bg-green-100 text-green-800">
                                            DISBURSED
                                        </span>
                                    </div>
                                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                                        <div>
                                            <p className="text-slate-500">Member</p>
                                            <p className="font-semibold text-slate-800">{loan.memberName}</p>
                                            <p className="text-xs text-slate-500">{loan.memberNumber}</p>
                                        </div>
                                        <div>
                                            <p className="text-slate-500">Amount Disbursed</p>
                                            <p className="font-semibold text-green-600">{formatCurrency(loan.disbursedAmount)}</p>
                                        </div>
                                        <div>
                                            <p className="text-slate-500">Disbursement Date</p>
                                            <p className="font-semibold text-slate-800">
                                                {new Date(loan.disbursementDate).toLocaleDateString()}
                                            </p>
                                        </div>
                                        <div>
                                            <p className="text-slate-500">Status</p>
                                            <p className="font-semibold text-blue-600">{loan.loanStatus}</p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

function DisbursementModal({ loan, onClose, onSuccess, formatCurrency }) {
    const [disbursing, setDisbursing] = useState(false);
    const [disbursementMethod, setDisbursementMethod] = useState('MPESA');
    const [phoneNumber, setPhoneNumber] = useState(loan.memberPhone || '');
    const [reference, setReference] = useState('');

    const handleDisburse = async () => {
        if (!phoneNumber || !reference) {
            alert('Please fill in all required fields');
            return;
        }

        if (!window.confirm(`Confirm disbursement of ${formatCurrency(loan.approvedAmount)} to ${loan.memberName}?`)) {
            return;
        }

        setDisbursing(true);
        try {
            await api.post(`/api/finance/loans/${loan.id}/disburse`, {
                disbursementMethod,
                phoneNumber,
                reference
            });

            alert('Loan disbursed successfully!');
            onSuccess();
        } catch (error) {
            alert(error.response?.data?.message || 'Failed to disburse loan');
        } finally {
            setDisbursing(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 overflow-y-auto">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl my-8 flex flex-col max-h-[90vh]">
                {/* Fixed Header */}
                <div className="bg-slate-50 p-6 border-b border-slate-100 flex-shrink-0">
                    <h2 className="text-xl font-bold text-slate-800">Disburse Loan</h2>
                    <p className="text-sm text-slate-500 mt-1">Process loan disbursement to member</p>
                </div>

                {/* Scrollable Content */}
                <div className="p-6 space-y-4 overflow-y-auto flex-1">
                    <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                        <h3 className="font-bold text-blue-900 mb-3">Loan Details</h3>
                        <div className="grid grid-cols-2 gap-3 text-sm">
                            <div>
                                <p className="text-blue-700">Loan Number</p>
                                <p className="font-semibold text-blue-900">{loan.loanNumber}</p>
                            </div>
                            <div>
                                <p className="text-blue-700">Member</p>
                                <p className="font-semibold text-blue-900">{loan.memberName}</p>
                                <p className="text-xs text-blue-700">{loan.memberNumber}</p>
                            </div>
                            <div>
                                <p className="text-blue-700">Product</p>
                                <p className="font-semibold text-blue-900">{loan.productName}</p>
                            </div>
                            <div>
                                <p className="text-blue-700">Approved Amount</p>
                                <p className="font-semibold text-green-600">{formatCurrency(loan.approvedAmount)}</p>
                            </div>
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-bold text-slate-700 mb-2">Disbursement Method *</label>
                        <select
                            value={disbursementMethod}
                            onChange={(e) => setDisbursementMethod(e.target.value)}
                            className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
                        >
                            <option value="MPESA">M-Pesa</option>
                            <option value="BANK">Bank Transfer</option>
                            <option value="CASH">Cash</option>
                            <option value="CHECK">Cheque</option>
                        </select>
                    </div>

                    <div>
                        <label className="block text-sm font-bold text-slate-700 mb-2">
                            {disbursementMethod === 'MPESA' ? 'M-Pesa Phone Number' : 'Contact Number'} *
                        </label>
                        <input
                            type="text"
                            value={phoneNumber}
                            onChange={(e) => setPhoneNumber(e.target.value)}
                            className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
                            placeholder="e.g., 0712345678"
                            required
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-bold text-slate-700 mb-2">
                            Reference/Transaction ID *
                        </label>
                        <input
                            type="text"
                            value={reference}
                            onChange={(e) => setReference(e.target.value)}
                            className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
                            placeholder="e.g., TXN123456789"
                            required
                        />
                    </div>

                    <div className="bg-amber-50 border border-amber-200 rounded-lg p-3">
                        <p className="text-sm text-amber-800">
                            <strong>Important:</strong> Please verify all details before disbursement. This action cannot be undone.
                        </p>
                    </div>
                </div>

                {/* Fixed Footer */}
                <div className="bg-slate-50 p-4 sm:p-6 border-t border-slate-100 flex gap-3 flex-shrink-0">
                    <button
                        type="button"
                        onClick={onClose}
                        className="flex-1 px-4 py-3 border border-slate-300 rounded-lg hover:bg-slate-100 font-semibold transition text-sm sm:text-base"
                        disabled={disbursing}
                    >
                        Cancel
                    </button>
                    <button
                        onClick={handleDisburse}
                        disabled={disbursing}
                        className="flex-1 px-4 py-3 bg-green-600 hover:bg-green-700 disabled:bg-slate-300 text-white rounded-lg font-semibold transition text-sm sm:text-base"
                    >
                        {disbursing ? 'Disbursing...' : `Disburse ${formatCurrency(loan.approvedAmount)}`}
                    </button>
                </div>
            </div>
        </div>
    );
}

