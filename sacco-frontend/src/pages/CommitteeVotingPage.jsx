import React, { useState, useEffect } from 'react';
import api from '../api';
import DashboardHeader from '../components/DashboardHeader';
import BrandedSpinner from '../components/BrandedSpinner';
import { Vote, ThumbsUp, ThumbsDown, MinusCircle, Clock, CheckCircle, AlertCircle } from 'lucide-react';

export default function CommitteeVotingPage({ embedded = false, onVoteCast }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [loans, setLoans] = useState([]);
    const [selectedLoan, setSelectedLoan] = useState(null);
    const [showVoteModal, setShowVoteModal] = useState(false);

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
        loadLoansForVoting();

        // Auto-refresh every 30 seconds
        const interval = setInterval(loadLoansForVoting, 30000);
        return () => clearInterval(interval);
    }, []);

    const loadLoansForVoting = async () => {
        setLoading(true);
        try {
            const res = await api.get('/api/voting/loans/available');
            setLoans(res.data.data || []);
        } catch (error) {
            console.error('Failed to load loans:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleVoteClick = (loan) => {
        setSelectedLoan(loan);
        setShowVoteModal(true);
    };

    const handleVoteSuccess = () => {
        setShowVoteModal(false);
        setSelectedLoan(null);
        loadLoansForVoting();
        if (onVoteCast) onVoteCast(); // Notify parent component
    };

    const formatCurrency = (amount) => {
        return new Intl.NumberFormat('en-KE', {
            style: 'currency',
            currency: 'KES',
            minimumFractionDigits: 0
        }).format(amount);
    };

    if (loading) {
        if (embedded) {
            return (
                <div className="flex items-center justify-center py-12">
                    <BrandedSpinner />
                </div>
            );
        }
        return (
            <div className="flex items-center justify-center min-h-screen">
                <BrandedSpinner />
            </div>
        );
    }

    const votedLoans = loans.filter(l => l.hasVoted);
    const pendingLoans = loans.filter(l => !l.hasVoted);

    // ✅ EMBEDDED MODE (inside member dashboard)
    if (embedded) {
        return (
            <div className="space-y-8">
                {/* Statistics */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <StatCard
                        icon={<Vote className="text-blue-600" size={24} />}
                        label="Total Loans"
                        value={loans.length}
                        bgColor="bg-blue-50"
                    />
                    <StatCard
                        icon={<AlertCircle className="text-amber-600" size={24} />}
                        label="Pending Your Vote"
                        value={pendingLoans.length}
                        bgColor="bg-amber-50"
                    />
                    <StatCard
                        icon={<CheckCircle className="text-green-600" size={24} />}
                        label="Already Voted"
                        value={votedLoans.length}
                        bgColor="bg-green-50"
                    />
                </div>

                {loans.length === 0 ? (
                    <div className="bg-white rounded-lg shadow p-12 text-center">
                        <Vote className="mx-auto h-16 w-16 text-slate-300 mb-4" />
                        <h3 className="text-xl font-semibold text-slate-700 mb-2">No Active Voting Sessions</h3>
                        <p className="text-slate-500">
                            There are no loans available for voting at this time.
                            <br />
                            The chairperson will open voting when a committee meeting is in session.
                        </p>
                    </div>
                ) : (
                    <>
                        {/* Pending Votes */}
                        {pendingLoans.length > 0 && (
                            <div>
                                <h2 className="text-xl font-bold text-slate-800 mb-4 flex items-center gap-2">
                                    <AlertCircle className="text-amber-600" size={20} />
                                    Pending Your Vote ({pendingLoans.length})
                                </h2>
                                <div className="space-y-4">
                                    {pendingLoans.map((loan) => (
                                        <LoanCard
                                            key={loan.agendaItemId}
                                            loan={loan}
                                            onVote={handleVoteClick}
                                            formatCurrency={formatCurrency}
                                        />
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Already Voted */}
                        {votedLoans.length > 0 && (
                            <div>
                                <h2 className="text-xl font-bold text-slate-800 mb-4 flex items-center gap-2">
                                    <CheckCircle className="text-green-600" size={20} />
                                    Already Voted ({votedLoans.length})
                                </h2>
                                <div className="space-y-4">
                                    {votedLoans.map((loan) => (
                                        <LoanCard
                                            key={loan.agendaItemId}
                                            loan={loan}
                                            formatCurrency={formatCurrency}
                                            voted={true}
                                        />
                                    ))}
                                </div>
                            </div>
                        )}
                    </>
                )}

                {/* Vote Modal */}
                {showVoteModal && selectedLoan && (
                    <VoteModal
                        loan={selectedLoan}
                        onClose={() => {
                            setShowVoteModal(false);
                            setSelectedLoan(null);
                        }}
                        onSuccess={handleVoteSuccess}
                        formatCurrency={formatCurrency}
                    />
                )}
            </div>
        );
    }

    // ✅ STANDALONE MODE (separate page)
    return (
        <div className="min-h-screen bg-slate-50">
            <DashboardHeader user={user} title="Committee Voting" />

            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8 pb-12">
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-slate-800">Committee Loan Voting</h1>
                    <p className="text-slate-600 mt-1">Review and vote on loan applications</p>
                </div>

                {/* Statistics */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                    <StatCard
                        icon={<Vote className="text-blue-600" size={24} />}
                        label="Total Loans"
                        value={loans.length}
                        bgColor="bg-blue-50"
                    />
                    <StatCard
                        icon={<AlertCircle className="text-amber-600" size={24} />}
                        label="Pending Your Vote"
                        value={pendingLoans.length}
                        bgColor="bg-amber-50"
                    />
                    <StatCard
                        icon={<CheckCircle className="text-green-600" size={24} />}
                        label="Already Voted"
                        value={votedLoans.length}
                        bgColor="bg-green-50"
                    />
                </div>

                {loans.length === 0 ? (
                    <div className="bg-white rounded-lg shadow p-12 text-center">
                        <Vote className="mx-auto h-16 w-16 text-slate-300 mb-4" />
                        <h3 className="text-xl font-semibold text-slate-700 mb-2">No Active Voting Sessions</h3>
                        <p className="text-slate-500">
                            There are no loans available for voting at this time.
                            <br />
                            The chairperson will open voting when a committee meeting is in session.
                        </p>
                    </div>
                ) : (
                    <>
                        {/* Pending Votes */}
                        {pendingLoans.length > 0 && (
                            <div className="mb-8">
                                <h2 className="text-xl font-bold text-slate-800 mb-4 flex items-center gap-2">
                                    <AlertCircle className="text-amber-600" size={20} />
                                    Pending Your Vote ({pendingLoans.length})
                                </h2>
                                <div className="space-y-4">
                                    {pendingLoans.map((loan) => (
                                        <LoanCard
                                            key={loan.agendaItemId}
                                            loan={loan}
                                            onVote={handleVoteClick}
                                            formatCurrency={formatCurrency}
                                        />
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Already Voted */}
                        {votedLoans.length > 0 && (
                            <div>
                                <h2 className="text-xl font-bold text-slate-800 mb-4 flex items-center gap-2">
                                    <CheckCircle className="text-green-600" size={20} />
                                    Already Voted ({votedLoans.length})
                                </h2>
                                <div className="space-y-4">
                                    {votedLoans.map((loan) => (
                                        <LoanCard
                                            key={loan.agendaItemId}
                                            loan={loan}
                                            formatCurrency={formatCurrency}
                                            voted={true}
                                        />
                                    ))}
                                </div>
                            </div>
                        )}
                    </>
                )}
            </main>

            {/* Vote Modal */}
            {showVoteModal && selectedLoan && (
                <VoteModal
                    loan={selectedLoan}
                    onClose={() => {
                        setShowVoteModal(false);
                        setSelectedLoan(null);
                    }}
                    onSuccess={() => {
                        setShowVoteModal(false);
                        setSelectedLoan(null);
                        loadLoansForVoting();
                    }}
                    formatCurrency={formatCurrency}
                />
            )}
        </div>
    );
}

function StatCard({ icon, label, value, bgColor }) {
    return (
        <div className={`${bgColor} rounded-lg shadow p-6`}>
            <div className="flex items-center justify-between">
                <div>
                    <p className="text-sm font-medium text-gray-600 mb-1">{label}</p>
                    <p className="text-3xl font-bold text-gray-900">{value}</p>
                </div>
                <div className="opacity-75">{icon}</div>
            </div>
        </div>
    );
}

function LoanCard({ loan, onVote, formatCurrency, voted = false }) {
    return (
        <div className={`bg-white rounded-lg shadow-md p-6 ${voted ? 'opacity-75' : 'hover:shadow-lg'} transition`}>
            <div className="flex justify-between items-start">
                <div className="flex-1">
                    <div className="flex items-center gap-3 mb-3">
                        <h3 className="text-lg font-bold text-slate-800">{loan.loanNumber}</h3>
                        {voted && (
                            <span className="px-2 py-1 text-xs font-semibold rounded-full bg-green-100 text-green-800">
                                ✓ VOTED
                            </span>
                        )}
                        <span className="text-xs text-slate-400">Meeting: {loan.meetingNumber}</span>
                    </div>

                    <div className="grid grid-cols-2 gap-4 mb-4">
                        <div>
                            <p className="text-xs text-slate-500 uppercase font-medium mb-1">Applicant</p>
                            <p className="font-semibold text-slate-800">{loan.memberName}</p>
                            <p className="text-sm text-slate-500">{loan.memberNumber}</p>
                        </div>
                        <div>
                            <p className="text-xs text-slate-500 uppercase font-medium mb-1">Loan Product</p>
                            <p className="font-semibold text-slate-800">{loan.productName}</p>
                        </div>
                        <div>
                            <p className="text-xs text-slate-500 uppercase font-medium mb-1">Principal Amount</p>
                            <p className="text-lg font-bold text-slate-800">{formatCurrency(loan.principalAmount)}</p>
                        </div>
                        <div>
                            <p className="text-xs text-slate-500 uppercase font-medium mb-1">Approved Amount</p>
                            <p className="text-lg font-bold text-green-600">{formatCurrency(loan.approvedAmount)}</p>
                        </div>
                        <div>
                            <p className="text-xs text-slate-500 uppercase font-medium mb-1">Duration</p>
                            <p className="font-semibold text-slate-800">{loan.durationWeeks} weeks</p>
                        </div>
                    </div>

                    {!voted && (
                        <button
                            onClick={() => onVote(loan)}
                            className="w-full sm:w-auto px-6 py-3 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-semibold transition flex items-center justify-center gap-2"
                        >
                            <Vote size={18} />
                            Cast Your Vote
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
}

function VoteModal({ loan, onClose, onSuccess, formatCurrency }) {
    const [decision, setDecision] = useState('');
    const [comments, setComments] = useState('');
    const [submitting, setSubmitting] = useState(false);

    const handleSubmit = async () => {
        if (!decision) {
            alert('Please select your vote decision');
            return;
        }

        if (!window.confirm(`Confirm your vote to ${decision} this loan?`)) {
            return;
        }

        setSubmitting(true);
        try {
            await api.post('/api/voting/cast', {
                agendaItemId: loan.agendaItemId,
                decision: decision,
                comments: comments
            });

            alert('Vote cast successfully!');
            onSuccess();
        } catch (error) {
            alert(error.response?.data?.message || 'Failed to cast vote');
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-hidden flex flex-col">
                <div className="bg-slate-50 p-6 border-b border-slate-100">
                    <h2 className="text-xl font-bold text-slate-800">Cast Your Vote</h2>
                    <p className="text-sm text-slate-500 mt-1">Review the loan details and make your decision</p>
                </div>

                <div className="flex-1 overflow-y-auto p-6">
                    {/* Loan Summary */}
                    <div className="bg-blue-50 rounded-lg p-4 mb-6">
                        <h3 className="font-bold text-blue-900 mb-2">{loan.loanNumber} - {loan.memberName}</h3>
                        <div className="grid grid-cols-2 gap-3 text-sm">
                            <div>
                                <span className="text-blue-700">Product:</span>
                                <span className="font-semibold ml-2">{loan.productName}</span>
                            </div>
                            <div>
                                <span className="text-blue-700">Amount:</span>
                                <span className="font-semibold ml-2">{formatCurrency(loan.approvedAmount)}</span>
                            </div>
                            <div>
                                <span className="text-blue-700">Member:</span>
                                <span className="font-semibold ml-2">{loan.memberNumber}</span>
                            </div>
                            <div>
                                <span className="text-blue-700">Duration:</span>
                                <span className="font-semibold ml-2">{loan.durationWeeks} weeks</span>
                            </div>
                        </div>
                    </div>

                    {/* Vote Decision */}
                    <div className="mb-6">
                        <label className="block text-sm font-bold text-slate-700 mb-3">Your Decision *</label>
                        <div className="grid grid-cols-2 gap-3">
                            <button
                                onClick={() => setDecision('APPROVE')}
                                className={`p-4 rounded-lg border-2 transition ${
                                    decision === 'APPROVE'
                                        ? 'border-green-600 bg-green-50 text-green-700'
                                        : 'border-slate-200 hover:border-green-300'
                                }`}
                            >
                                <ThumbsUp className="mx-auto mb-2" size={24} />
                                <span className="font-semibold">Approve</span>
                            </button>
                            <button
                                onClick={() => setDecision('REJECT')}
                                className={`p-4 rounded-lg border-2 transition ${
                                    decision === 'REJECT'
                                        ? 'border-red-600 bg-red-50 text-red-700'
                                        : 'border-slate-200 hover:border-red-300'
                                }`}
                            >
                                <ThumbsDown className="mx-auto mb-2" size={24} />
                                <span className="font-semibold">Reject</span>
                            </button>
                            <button
                                onClick={() => setDecision('ABSTAIN')}
                                className={`p-4 rounded-lg border-2 transition ${
                                    decision === 'ABSTAIN'
                                        ? 'border-slate-600 bg-slate-50 text-slate-700'
                                        : 'border-slate-200 hover:border-slate-300'
                                }`}
                            >
                                <MinusCircle className="mx-auto mb-2" size={24} />
                                <span className="font-semibold">Abstain</span>
                            </button>
                            <button
                                onClick={() => setDecision('DEFER')}
                                className={`p-4 rounded-lg border-2 transition ${
                                    decision === 'DEFER'
                                        ? 'border-amber-600 bg-amber-50 text-amber-700'
                                        : 'border-slate-200 hover:border-amber-300'
                                }`}
                            >
                                <Clock className="mx-auto mb-2" size={24} />
                                <span className="font-semibold">Defer</span>
                            </button>
                        </div>
                    </div>

                    {/* Comments */}
                    <div>
                        <label className="block text-sm font-bold text-slate-700 mb-2">Comments (Optional)</label>
                        <textarea
                            value={comments}
                            onChange={(e) => setComments(e.target.value)}
                            rows="4"
                            className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
                            placeholder="Add any comments or reasons for your decision..."
                        />
                    </div>
                </div>

                <div className="bg-slate-50 p-6 border-t border-slate-100 flex gap-3">
                    <button
                        type="button"
                        onClick={onClose}
                        disabled={submitting}
                        className="flex-1 px-4 py-2 border border-slate-300 rounded-lg hover:bg-slate-100 font-semibold transition disabled:opacity-50"
                    >
                        Cancel
                    </button>
                    <button
                        onClick={handleSubmit}
                        disabled={submitting || !decision}
                        className="flex-1 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-300 text-white rounded-lg font-semibold transition"
                    >
                        {submitting ? 'Submitting...' : 'Submit Vote'}
                    </button>
                </div>
            </div>
        </div>
    );
}

