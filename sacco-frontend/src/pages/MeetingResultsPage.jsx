import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api';
import DashboardHeader from '../components/DashboardHeader';
import BrandedSpinner from '../components/BrandedSpinner';
import {
    Calendar, Clock, MapPin, ArrowLeft, Vote,
    ThumbsUp, ThumbsDown, MinusCircle, Users,
    CheckCircle, XCircle, AlertTriangle
} from 'lucide-react';

export default function MeetingResultsPage() {
    const { meetingId } = useParams();
    const navigate = useNavigate();
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [meetingDetails, setMeetingDetails] = useState(null);
    const [votingResults, setVotingResults] = useState(null);

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
        loadMeetingData();

        // Auto-refresh every 30 seconds
        const interval = setInterval(loadMeetingData, 30000);
        return () => clearInterval(interval);
    }, [meetingId]);

    const loadMeetingData = async () => {
        setLoading(true);
        try {
            // Always get meeting details
            const detailsRes = await api.get(`/api/meetings/${meetingId}`);
            setMeetingDetails(detailsRes.data.data);

            // Only get voting results if meeting has started voting
            const meetingStatus = detailsRes.data.data.status;
            if (meetingStatus === 'IN_PROGRESS' || meetingStatus === 'VOTING_CLOSED' || meetingStatus === 'COMPLETED') {
                try {
                    const resultsRes = await api.get(`/api/voting/meetings/${meetingId}/results`);
                    setVotingResults(resultsRes.data.data);
                } catch (err) {
                    console.log('Voting results not available yet');
                    setVotingResults(null);
                }
            } else {
                // SCHEDULED or CANCELLED - no voting results
                setVotingResults(null);
            }
        } catch (error) {
            console.error('Failed to load meeting data:', error);
            alert('Failed to load meeting data. Meeting may not exist.');
        } finally {
            setLoading(false);
        }
    };

    const formatCurrency = (amount) => {
        return new Intl.NumberFormat('en-KE', {
            style: 'currency',
            currency: 'KES',
            minimumFractionDigits: 0
        }).format(amount);
    };

    const getOutcomeColor = (outcome) => {
        switch (outcome) {
            case 'APPROVED': return 'bg-green-100 text-green-800';
            case 'REJECTED': return 'bg-red-100 text-red-800';
            case 'TIED': return 'bg-amber-100 text-amber-800';
            default: return 'bg-slate-100 text-slate-800';
        }
    };

    const getOutcomeIcon = (outcome) => {
        switch (outcome) {
            case 'APPROVED': return <CheckCircle size={20} />;
            case 'REJECTED': return <XCircle size={20} />;
            case 'TIED': return <AlertTriangle size={20} />;
            default: return <Vote size={20} />;
        }
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <BrandedSpinner />
            </div>
        );
    }

    if (!meetingDetails || !votingResults) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <div className="text-center">
                    <p className="text-slate-500">Meeting not found</p>
                    <button onClick={() => navigate(-1)} className="mt-4 text-indigo-600 hover:underline">
                        Go Back
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-slate-50">
            <DashboardHeader user={user} title="Meeting Results" />

            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8 pb-12">
                {/* Back Button */}
                <button
                    onClick={() => navigate(-1)}
                    className="flex items-center gap-2 text-slate-600 hover:text-slate-800 mb-4"
                >
                    <ArrowLeft size={20} />
                    Back
                </button>

                {/* Meeting Header */}
                <div className="bg-white rounded-lg shadow-md p-6 mb-8">
                    <div className="flex justify-between items-start mb-4">
                        <div>
                            <h1 className="text-3xl font-bold text-slate-800 mb-2">{meetingDetails.title}</h1>
                            <p className="text-sm text-slate-500">Meeting #: {meetingDetails.meetingNumber}</p>
                        </div>
                        <span className={`px-3 py-1 rounded-full text-sm font-semibold ${
                            meetingDetails.status === 'COMPLETED'
                                ? 'bg-green-100 text-green-800'
                                : meetingDetails.status === 'IN_PROGRESS'
                                ? 'bg-blue-100 text-blue-800'
                                : 'bg-slate-100 text-slate-800'
                        }`}>
                            {meetingDetails.status}
                        </span>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <div className="flex items-center gap-2 text-slate-600">
                            <Calendar size={16} />
                            <span>{new Date(meetingDetails.meetingDate).toLocaleDateString('en-US', {
                                weekday: 'short',
                                year: 'numeric',
                                month: 'short',
                                day: 'numeric'
                            })}</span>
                        </div>
                        <div className="flex items-center gap-2 text-slate-600">
                            <Clock size={16} />
                            <span>{meetingDetails.meetingTime}</span>
                        </div>
                        <div className="flex items-center gap-2 text-slate-600">
                            <MapPin size={16} />
                            <span>{meetingDetails.venue}</span>
                        </div>
                    </div>
                </div>

                {/* Show different content based on meeting status */}
                {meetingDetails.status === 'SCHEDULED' ? (
                    // SCHEDULED: Show agenda without voting results
                    <div className="bg-white rounded-lg shadow-md overflow-hidden">
                        <div className="px-6 py-4 border-b border-gray-200">
                            <h2 className="text-xl font-semibold text-gray-900">Meeting Agenda</h2>
                            <p className="text-sm text-slate-500 mt-1">Voting has not started yet</p>
                        </div>

                        <div className="p-6">
                            {meetingDetails.agendaItems && meetingDetails.agendaItems.length > 0 ? (
                                <div className="divide-y divide-gray-200">
                                    {meetingDetails.agendaItems.map((item, index) => (
                                        <div key={item.id} className="py-4">
                                            <div className="flex items-start gap-3">
                                                <span className="text-sm font-semibold text-slate-500">#{index + 1}</span>
                                                <div className="flex-1">
                                                    <h3 className="text-lg font-bold text-slate-800">{item.loanNumber}</h3>
                                                    <p className="text-slate-600">{item.memberName}</p>
                                                    <div className="flex gap-4 mt-2 text-sm text-slate-500">
                                                        <span>Product: {item.productName}</span>
                                                        <span>Amount: KES {Number(item.amount).toLocaleString()}</span>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <div className="text-center py-8 text-slate-500">
                                    <AlertTriangle className="mx-auto h-12 w-12 text-slate-300 mb-4" />
                                    <p>No loans on agenda yet</p>
                                </div>
                            )}
                        </div>
                    </div>
                ) : votingResults ? (
                    // IN_PROGRESS, VOTING_CLOSED, COMPLETED: Show voting results
                    <>
                        {/* Voting Statistics */}
                        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
                            <StatCard
                                icon={<Vote className="text-blue-600" size={24} />}
                                label="Agenda Items"
                                value={votingResults.totalAgendaItems}
                                bgColor="bg-blue-50"
                            />
                            <StatCard
                                icon={<Users className="text-purple-600" size={24} />}
                                label="Total Votes Cast"
                                value={votingResults.totalVotesCast}
                                bgColor="bg-purple-50"
                            />
                            <StatCard
                                icon={<CheckCircle className="text-green-600" size={24} />}
                                label="Approved"
                                value={votingResults.results.filter(r => r.outcome === 'APPROVED').length}
                                bgColor="bg-green-50"
                            />
                            <StatCard
                                icon={<XCircle className="text-red-600" size={24} />}
                                label="Rejected"
                                value={votingResults.results.filter(r => r.outcome === 'REJECTED').length}
                                bgColor="bg-red-50"
                            />
                        </div>

                        {/* Voting Results */}
                        <div className="bg-white rounded-lg shadow-md overflow-hidden">
                            <div className="px-6 py-4 border-b border-gray-200">
                                <h2 className="text-xl font-semibold text-gray-900">
                                    {meetingDetails.status === 'IN_PROGRESS' ? 'Live Voting Results' : 'Voting Results'}
                                </h2>
                            </div>

                            <div className="divide-y divide-gray-200">
                                {votingResults.results.map((result, index) => (
                            <div key={result.agendaId} className="p-6 hover:bg-slate-50 transition">
                                <div className="flex justify-between items-start mb-4">
                                    <div className="flex-1">
                                        <div className="flex items-center gap-3 mb-2">
                                            <span className="text-sm font-semibold text-slate-500">#{index + 1}</span>
                                            <h3 className="text-lg font-bold text-slate-800">{result.loanNumber}</h3>
                                            <span className={`px-2 py-1 rounded-full text-xs font-semibold flex items-center gap-1 ${getOutcomeColor(result.outcome)}`}>
                                                {getOutcomeIcon(result.outcome)}
                                                {result.outcome}
                                            </span>
                                        </div>
                                        <p className="text-slate-600 mb-2">{result.memberName}</p>
                                        <p className="text-lg font-semibold text-slate-800">{formatCurrency(result.amount)}</p>
                                    </div>
                                </div>

                                {/* Vote Breakdown */}
                                <div className="grid grid-cols-2 md:grid-cols-5 gap-3 mt-4">
                                    <div className="bg-slate-50 rounded-lg p-3">
                                        <p className="text-xs text-slate-500 mb-1">Total Votes</p>
                                        <p className="text-xl font-bold text-slate-800">{result.totalVotes}</p>
                                    </div>
                                    <div className="bg-green-50 rounded-lg p-3">
                                        <div className="flex items-center gap-1 mb-1">
                                            <ThumbsUp size={12} className="text-green-600" />
                                            <p className="text-xs text-green-600">Approve</p>
                                        </div>
                                        <p className="text-xl font-bold text-green-700">{result.approveVotes}</p>
                                    </div>
                                    <div className="bg-red-50 rounded-lg p-3">
                                        <div className="flex items-center gap-1 mb-1">
                                            <ThumbsDown size={12} className="text-red-600" />
                                            <p className="text-xs text-red-600">Reject</p>
                                        </div>
                                        <p className="text-xl font-bold text-red-700">{result.rejectVotes}</p>
                                    </div>
                                    <div className="bg-slate-50 rounded-lg p-3">
                                        <div className="flex items-center gap-1 mb-1">
                                            <MinusCircle size={12} className="text-slate-600" />
                                            <p className="text-xs text-slate-600">Abstain</p>
                                        </div>
                                        <p className="text-xl font-bold text-slate-700">{result.abstainVotes}</p>
                                    </div>
                                    <div className="bg-amber-50 rounded-lg p-3">
                                        <div className="flex items-center gap-1 mb-1">
                                            <Clock size={12} className="text-amber-600" />
                                            <p className="text-xs text-amber-600">Defer</p>
                                        </div>
                                        <p className="text-xl font-bold text-amber-700">{result.deferVotes}</p>
                                    </div>
                                </div>

                                {/* Vote Percentage Bar */}
                                {result.totalVotes > 0 && (
                                    <div className="mt-4">
                                        <div className="flex h-4 rounded-full overflow-hidden">
                                            <div
                                                className="bg-green-500"
                                                style={{ width: `${(result.approveVotes / result.totalVotes) * 100}%` }}
                                            />
                                            <div
                                                className="bg-red-500"
                                                style={{ width: `${(result.rejectVotes / result.totalVotes) * 100}%` }}
                                            />
                                            <div
                                                className="bg-slate-300"
                                                style={{ width: `${(result.abstainVotes / result.totalVotes) * 100}%` }}
                                            />
                                            <div
                                                className="bg-amber-400"
                                                style={{ width: `${(result.deferVotes / result.totalVotes) * 100}%` }}
                                            />
                                        </div>
                                    </div>
                                )}
                            </div>
                        ))}
                    </div>
                </div>
                    </>
                ) : (
                    // No voting results available
                    <div className="bg-white rounded-lg shadow-md p-8 text-center">
                        <AlertTriangle className="mx-auto h-16 w-16 text-slate-300 mb-4" />
                        <h3 className="text-xl font-semibold text-slate-700 mb-2">No Voting Results Yet</h3>
                        <p className="text-slate-500">
                            This meeting has not started voting yet.
                        </p>
                    </div>
                )}
            </main>
        </div>
    );
}

function StatCard({ icon, label, value, bgColor }) {
    return (
        <div className={`${bgColor} rounded-lg shadow p-4`}>
            <div className="flex items-center justify-between">
                <div>
                    <p className="text-xs font-medium text-gray-600 mb-1">{label}</p>
                    <p className="text-2xl font-bold text-gray-900">{value}</p>
                </div>
                <div className="opacity-75">{icon}</div>
            </div>
        </div>
    );
}

