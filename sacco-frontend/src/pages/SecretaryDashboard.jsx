import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import api from '../api';
import BrandedSpinner from '../components/BrandedSpinner';
import DashboardHeader from '../components/DashboardHeader';
import {
    Calendar,
    Clock,
    MapPin,
    FileText,
    Users,
    Plus,
    Eye,
    CheckCircle,
    AlertCircle,
    History,
    RefreshCw,
    Vote,
    XCircle
} from 'lucide-react';

export default function SecretaryDashboard() {
    const [user, setUser] = useState(null);
    const [searchParams] = useSearchParams();
    const activeTab = searchParams.get('tab') || 'awaiting';
    const [loading, setLoading] = useState(true);
    const [loansAwaitingMeeting, setLoansAwaitingMeeting] = useState([]);
    const [scheduledMeetings, setScheduledMeetings] = useState([]);
    const [activeMeetings, setActiveMeetings] = useState([]);
    const [votingClosedMeetings, setVotingClosedMeetings] = useState([]);
    const [completedMeetings, setCompletedMeetings] = useState([]);
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [showEditModal, setShowEditModal] = useState(false);
    const [meetingToEdit, setMeetingToEdit] = useState(null);
    const [lastRefresh, setLastRefresh] = useState(null);

    const loadDashboard = useCallback(async () => {
        // Don't refresh if modal is open
        if (showCreateModal || showEditModal) return;

        setLoading(true);
        try {
            const [loansRes, meetingsRes] = await Promise.all([
                api.get('/api/meetings/loans/awaiting'),
                api.get('/api/meetings/all') // ✅ Get ALL meetings, not just scheduled
            ]);

            setLoansAwaitingMeeting(loansRes.data.data || []);

            const allMeetings = meetingsRes.data.data || [];
            setScheduledMeetings(allMeetings.filter(m => m.status === 'SCHEDULED'));
            setActiveMeetings(allMeetings.filter(m => m.status === 'IN_PROGRESS'));
            setVotingClosedMeetings(allMeetings.filter(m => m.status === 'VOTING_CLOSED'));
            setCompletedMeetings(allMeetings.filter(m => m.status === 'COMPLETED'));

            setLastRefresh(new Date());
        } catch (error) {
            console.error('Failed to load secretary dashboard:', error);
        } finally {
            setLoading(false);
        }
    }, [showCreateModal, showEditModal]); // Only recreate when modals change

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
        loadDashboard();
    }, []); // Initial load only

    useEffect(() => {
        // Auto-refresh every 30 seconds, but pause when modal is open
        const interval = setInterval(() => {
            if (!showCreateModal && !showEditModal) {
                loadDashboard();
            }
        }, 30000);
        return () => clearInterval(interval);
    }, [showCreateModal, showEditModal, loadDashboard]); // Re-setup interval when modal state or function changes

    const handleFinalizeVoting = async (meetingId) => {
        if (!window.confirm('Finalize voting results and generate meeting minutes? This will forward approved loans for disbursement.')) {
            return;
        }

        try {
            await api.post(`/api/voting/meetings/${meetingId}/finalize`);
            alert('Voting results finalized successfully! Minutes generated and loans forwarded for disbursement.');
            loadDashboard();
        } catch (error) {
            alert(error.response?.data?.message || 'Failed to finalize voting');
        }
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
        }).format(amount);
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <BrandedSpinner />
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800 pb-12">
            <DashboardHeader user={user} title="Secretary Portal" />

            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8">
                {/* Header with Refresh */}
                <div className="mb-6 flex items-center justify-between">
                    <div>
                        <h1 className="text-2xl font-bold text-slate-800">Committee Meeting Management</h1>
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

                {/* Tabs */}
                <div className="mb-8 overflow-x-auto pb-2 scrollbar-hide">
                    <div className="flex gap-1 w-max">
                        <TabButton id="awaiting" label="Loans Awaiting Meeting" icon={AlertCircle} />
                        <TabButton id="meetings" label="Scheduled Meetings" icon={Calendar} />
                        <TabButton id="active" label="Active Voting" icon={Vote} />
                        <TabButton id="minutes" label="Meeting Minutes" icon={FileText} />
                        <TabButton id="history" label="Meeting History" icon={History} />
                    </div>
                </div>

                {/* Statistics Cards */}
                <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
                    <StatCard
                        icon={<AlertCircle className="text-amber-600" size={24} />}
                        label="Loans Awaiting Meeting"
                        value={loansAwaitingMeeting.length}
                        bgColor="bg-amber-50"
                        textColor="text-amber-900"
                    />
                    <StatCard
                        icon={<Calendar className="text-blue-600" size={24} />}
                        label="Scheduled Meetings"
                        value={scheduledMeetings.length}
                        bgColor="bg-blue-50"
                        textColor="text-blue-900"
                    />
                    <StatCard
                        icon={<Vote className="text-green-600" size={24} />}
                        label="Active Voting"
                        value={activeMeetings.length}
                        bgColor="bg-green-50"
                        textColor="text-green-900"
                    />
                    <StatCard
                        icon={<FileText className="text-purple-600" size={24} />}
                        label="Completed Minutes"
                        value={completedMeetings.filter(m => m.minutes).length}
                        bgColor="bg-purple-50"
                        textColor="text-purple-900"
                    />
                </div>

                {/* Content based on active tab */}
                {activeTab === 'awaiting' && (
                    <LoansAwaitingSection
                        loans={loansAwaitingMeeting}
                        onCreateMeeting={() => setShowCreateModal(true)}
                        formatCurrency={formatCurrency}
                    />
                )}

                {activeTab === 'meetings' && (
                    <ScheduledMeetingsSection
                        meetings={scheduledMeetings}
                        onCreateMeeting={() => setShowCreateModal(true)}
                    />
                )}

                {activeTab === 'active' && (
                    <ActiveVotingSection
                        meetings={[...activeMeetings, ...votingClosedMeetings]}
                        onFinalize={handleFinalizeVoting}
                    />
                )}

                {activeTab === 'minutes' && (
                    <MinutesListSection meetings={completedMeetings} />
                )}

                {activeTab === 'history' && (
                    <div className="bg-white rounded-lg shadow p-8 text-center">
                        <History className="mx-auto h-12 w-12 text-slate-400 mb-4" />
                        <p className="text-slate-500">Meeting history coming soon...</p>
                    </div>
                )}
            </main>

            {/* Create Meeting Modal */}
            {showCreateModal && (
                <CreateMeetingModal
                    onClose={() => setShowCreateModal(false)}
                    loansAvailable={loansAwaitingMeeting}
                    onSuccess={() => {
                        setShowCreateModal(false);
                        loadDashboard();
                    }}
                />
            )}
        </div>
    );
}

// Statistics Card Component
function StatCard({ icon, label, value, bgColor, textColor }) {
    return (
        <div className={`${bgColor} rounded-lg shadow p-6`}>
            <div className="flex items-center justify-between">
                <div>
                    <p className="text-sm font-medium text-gray-600 mb-1">{label}</p>
                    <p className={`text-3xl font-bold ${textColor}`}>{value}</p>
                </div>
                <div className="opacity-75">{icon}</div>
            </div>
        </div>
    );
}

// Loans Awaiting Meeting Section
function LoansAwaitingSection({ loans, onCreateMeeting, formatCurrency }) {
    return (
        <div className="bg-white rounded-lg shadow">
            <div className="px-6 py-4 border-b border-gray-200 flex justify-between items-center">
                <h2 className="text-xl font-semibold text-gray-900 flex items-center gap-2">
                    <FileText size={20} />
                    Loans Awaiting Committee Meeting
                </h2>
                <button
                    onClick={onCreateMeeting}
                    className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-semibold transition"
                >
                    <Plus size={18} />
                    Schedule Meeting
                </button>
            </div>

            {loans.length === 0 ? (
                <div className="p-8 text-center">
                    <CheckCircle className="mx-auto h-12 w-12 text-green-400 mb-4" />
                    <p className="text-slate-500">No loans awaiting committee meeting</p>
                    <p className="text-sm text-slate-400 mt-1">All approved loans have been scheduled</p>
                </div>
            ) : (
                <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-200">
                        <thead className="bg-gray-50">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Loan #</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Member</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Product</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Amount</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Approved</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Approval Date</th>
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
                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-gray-900">
                                        {formatCurrency(loan.principalAmount)}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-green-600">
                                        {formatCurrency(loan.approvedAmount)}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        {new Date(loan.approvalDate).toLocaleDateString()}
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

// Scheduled Meetings Section
function ScheduledMeetingsSection({ meetings, onCreateMeeting }) {
    return (
        <div className="bg-white rounded-lg shadow">
            <div className="px-6 py-4 border-b border-gray-200 flex justify-between items-center">
                <h2 className="text-xl font-semibold text-gray-900 flex items-center gap-2">
                    <Calendar size={20} />
                    Scheduled Meetings
                </h2>
                <button
                    onClick={onCreateMeeting}
                    className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-semibold transition"
                >
                    <Plus size={18} />
                    New Meeting
                </button>
            </div>

            {meetings.length === 0 ? (
                <div className="p-8 text-center">
                    <Calendar className="mx-auto h-12 w-12 text-slate-400 mb-4" />
                    <p className="text-slate-500">No scheduled meetings</p>
                    <button
                        onClick={onCreateMeeting}
                        className="mt-4 text-indigo-600 hover:text-indigo-700 font-semibold"
                    >
                        Schedule your first meeting
                    </button>
                </div>
            ) : (
                <div className="p-6 space-y-4">
                    {meetings.map((meeting) => (
                        <MeetingCard key={meeting.id} meeting={meeting} />
                    ))}
                </div>
            )}
        </div>
    );
}

// Meeting Card Component
function MeetingCard({ meeting }) {
    return (
        <div className="border border-slate-200 rounded-lg p-4 hover:shadow-md transition">
            <div className="flex justify-between items-start">
                <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                        <h3 className="text-lg font-bold text-slate-800">{meeting.title}</h3>
                        <span className="px-2 py-1 text-xs font-semibold rounded-full bg-blue-100 text-blue-800">
                            {meeting.meetingType.replace('_', ' ')}
                        </span>
                    </div>
                    <div className="flex flex-wrap gap-4 text-sm text-slate-600">
                        <div className="flex items-center gap-1">
                            <Calendar size={14} />
                            {new Date(meeting.meetingDate).toLocaleDateString('en-US', {
                                weekday: 'short',
                                year: 'numeric',
                                month: 'short',
                                day: 'numeric'
                            })}
                        </div>
                        <div className="flex items-center gap-1">
                            <Clock size={14} />
                            {meeting.meetingTime}
                        </div>
                        <div className="flex items-center gap-1">
                            <MapPin size={14} />
                            {meeting.venue}
                        </div>
                        <div className="flex items-center gap-1">
                            <FileText size={14} />
                            {meeting.loanCount} loan(s) on agenda
                        </div>
                    </div>
                    <p className="text-xs text-slate-400 mt-2">Meeting #: {meeting.meetingNumber}</p>
                </div>
                <Link
                    to={`/secretary/meetings/${meeting.id}`}
                    className="flex items-center gap-1 px-3 py-1.5 text-sm font-semibold text-indigo-600 hover:text-indigo-700 hover:bg-indigo-50 rounded-lg transition"
                >
                    <Eye size={16} />
                    View
                </Link>
            </div>
        </div>
    );
}

// Minutes List Section Component
function MinutesListSection({ meetings }) {
    const meetingsWithMinutes = meetings.filter(m => m.minutes);

    return (
        <div className="bg-white rounded-lg shadow">
            <div className="px-6 py-4 border-b border-gray-200">
                <h2 className="text-xl font-semibold text-gray-900 flex items-center gap-2">
                    <FileText size={20} />
                    Meeting Minutes Archive
                </h2>
                <p className="text-sm text-slate-500 mt-1">
                    View and manage all meeting minutes
                </p>
            </div>

            {meetingsWithMinutes.length === 0 ? (
                <div className="p-8 text-center">
                    <FileText className="mx-auto h-12 w-12 text-slate-400 mb-4" />
                    <p className="text-slate-500">No minutes available yet</p>
                    <p className="text-sm text-slate-400 mt-1">
                        Minutes will appear here after meetings are completed and finalized
                    </p>
                </div>
            ) : (
                <div className="p-6">
                    <div className="space-y-4">
                        {meetingsWithMinutes
                            .sort((a, b) => new Date(b.meetingDate) - new Date(a.meetingDate))
                            .map((meeting) => (
                                <div
                                    key={meeting.id}
                                    className="border border-slate-200 rounded-lg p-4 hover:shadow-md transition bg-gradient-to-r from-white to-slate-50"
                                >
                                    <div className="flex justify-between items-start">
                                        <div className="flex-1">
                                            <div className="flex items-start gap-3 mb-3">
                                                <div className="flex-shrink-0 mt-1">
                                                    <div className="h-10 w-10 bg-purple-100 rounded-lg flex items-center justify-center">
                                                        <FileText className="text-purple-600" size={20} />
                                                    </div>
                                                </div>
                                                <div className="flex-1">
                                                    <h3 className="text-lg font-bold text-slate-800 mb-1">
                                                        {meeting.title}
                                                    </h3>
                                                    <div className="flex flex-wrap gap-3 text-sm text-slate-600">
                                                        <div className="flex items-center gap-1">
                                                            <Calendar size={14} />
                                                            {new Date(meeting.meetingDate).toLocaleDateString('en-US', {
                                                                weekday: 'short',
                                                                year: 'numeric',
                                                                month: 'short',
                                                                day: 'numeric'
                                                            })}
                                                        </div>
                                                        <div className="flex items-center gap-1">
                                                            <Clock size={14} />
                                                            {meeting.meetingTime}
                                                        </div>
                                                        <div className="flex items-center gap-1">
                                                            <MapPin size={14} />
                                                            {meeting.venue}
                                                        </div>
                                                        <div className="flex items-center gap-1">
                                                            <Users size={14} />
                                                            {meeting.loanCount} loan(s) reviewed
                                                        </div>
                                                    </div>
                                                    <div className="flex items-center gap-2 mt-2">
                                                        <span className="text-xs text-slate-400">
                                                            Meeting #: {meeting.meetingNumber}
                                                        </span>
                                                        <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-green-100 text-green-800">
                                                            COMPLETED
                                                        </span>
                                                    </div>
                                                </div>
                                            </div>

                                            {/* Minutes Preview */}
                                            <div className="ml-13 mt-3 p-3 bg-white border border-slate-200 rounded-lg">
                                                <p className="text-xs font-semibold text-slate-600 mb-2">Minutes Preview:</p>
                                                <pre className="text-xs text-slate-700 whitespace-pre-wrap line-clamp-3 font-mono">
                                                    {meeting.minutes}
                                                </pre>
                                            </div>
                                        </div>
                                    </div>

                                    {/* Action Buttons */}
                                    <div className="flex gap-2 mt-4 ml-13">
                                        <Link
                                            to={`/meetings/${meeting.id}/minutes`}
                                            className="flex items-center gap-1 px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg font-semibold transition text-sm"
                                        >
                                            <Eye size={16} />
                                            View Full Minutes
                                        </Link>
                                        <Link
                                            to={`/meetings/${meeting.id}/results`}
                                            className="flex items-center gap-1 px-3 py-2 text-sm font-semibold text-indigo-600 hover:text-indigo-700 hover:bg-indigo-50 rounded-lg transition border border-indigo-200"
                                        >
                                            <Vote size={16} />
                                            Voting Results
                                        </Link>
                                        <button
                                            onClick={() => {
                                                const blob = new Blob([meeting.minutes], { type: 'text/plain' });
                                                const url = URL.createObjectURL(blob);
                                                const a = document.createElement('a');
                                                a.href = url;
                                                a.download = `Minutes_${meeting.meetingNumber}.txt`;
                                                document.body.appendChild(a);
                                                a.click();
                                                document.body.removeChild(a);
                                                URL.revokeObjectURL(url);
                                            }}
                                            className="flex items-center gap-1 px-3 py-2 text-sm font-semibold text-green-600 hover:text-green-700 hover:bg-green-50 rounded-lg transition border border-green-200"
                                        >
                                            <FileText size={16} />
                                            Download
                                        </button>
                                    </div>
                                </div>
                            ))}
                    </div>
                </div>
            )}
        </div>
    );
}

// Create Meeting Modal Component
function CreateMeetingModal({ onClose, loansAvailable, onSuccess }) {
    const [formData, setFormData] = useState({
        title: '',
        meetingType: 'LOAN_COMMITTEE',
        meetingDate: '',
        meetingTime: '14:00',
        venue: '',
        selectedLoans: []
    });
    const [creating, setCreating] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setCreating(true);

        try {
            await api.post('/api/meetings', {
                title: formData.title,
                meetingType: formData.meetingType,
                meetingDate: formData.meetingDate,
                meetingTime: formData.meetingTime,
                venue: formData.venue,
                loanIds: formData.selectedLoans
            });

            alert('Meeting created successfully!');
            onSuccess();
        } catch (error) {
            alert(error.response?.data?.message || 'Failed to create meeting');
        } finally {
            setCreating(false);
        }
    };

    const toggleLoanSelection = (loanId) => {
        setFormData(prev => ({
            ...prev,
            selectedLoans: prev.selectedLoans.includes(loanId)
                ? prev.selectedLoans.filter(id => id !== loanId)
                : [...prev.selectedLoans, loanId]
        }));
    };

    return (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-3xl max-h-[90vh] overflow-hidden flex flex-col">
                <div className="bg-slate-50 p-6 border-b border-slate-100">
                    <h2 className="text-xl font-bold text-slate-800">Schedule Committee Meeting</h2>
                    <p className="text-sm text-slate-500 mt-1">Create a new meeting and add loans to the agenda</p>
                </div>

                <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-4">
                    <div>
                        <label className="block text-sm font-bold text-slate-700 mb-2">Meeting Title *</label>
                        <input
                            type="text"
                            value={formData.title}
                            onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                            className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
                            placeholder="e.g., Monthly Loan Committee Meeting"
                            required
                        />
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-bold text-slate-700 mb-2">Meeting Date *</label>
                            <input
                                type="date"
                                value={formData.meetingDate}
                                onChange={(e) => setFormData({ ...formData, meetingDate: e.target.value })}
                                className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
                                min={new Date().toISOString().split('T')[0]}
                                required
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-bold text-slate-700 mb-2">Meeting Time *</label>
                            <input
                                type="time"
                                value={formData.meetingTime}
                                onChange={(e) => setFormData({ ...formData, meetingTime: e.target.value })}
                                className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
                                required
                            />
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-bold text-slate-700 mb-2">Venue *</label>
                        <input
                            type="text"
                            value={formData.venue}
                            onChange={(e) => setFormData({ ...formData, venue: e.target.value })}
                            className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
                            placeholder="e.g., Conference Room A"
                            required
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-bold text-slate-700 mb-2">
                            Select Loans for Agenda ({formData.selectedLoans.length} selected)
                        </label>
                        <div className="border border-slate-200 rounded-lg max-h-64 overflow-y-auto">
                            {loansAvailable.length === 0 ? (
                                <p className="p-4 text-center text-slate-500 text-sm">No loans available</p>
                            ) : (
                                loansAvailable.map((loan) => (
                                    <label
                                        key={loan.id}
                                        className="flex items-center p-3 hover:bg-slate-50 cursor-pointer border-b border-slate-100 last:border-0"
                                    >
                                        <input
                                            type="checkbox"
                                            checked={formData.selectedLoans.includes(loan.id)}
                                            onChange={() => toggleLoanSelection(loan.id)}
                                            className="mr-3 h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded"
                                        />
                                        <div className="flex-1">
                                            <p className="text-sm font-semibold text-slate-800">{loan.loanNumber} - {loan.memberName}</p>
                                            <p className="text-xs text-slate-500">{loan.productName} - KES {Number(loan.approvedAmount).toLocaleString()}</p>
                                        </div>
                                    </label>
                                ))
                            )}
                        </div>
                    </div>
                </form>

                <div className="bg-slate-50 p-6 border-t border-slate-100 flex gap-3">
                    <button
                        type="button"
                        onClick={onClose}
                        className="flex-1 px-4 py-2 border border-slate-300 rounded-lg hover:bg-slate-100 font-semibold transition"
                    >
                        Cancel
                    </button>
                    <button
                        onClick={handleSubmit}
                        disabled={creating}
                        className="flex-1 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-300 text-white rounded-lg font-semibold transition"
                    >
                        {creating ? 'Creating...' : 'Create Meeting'}
                    </button>
                </div>
            </div>
        </div>
    );
}

// Active Voting Section Component
function ActiveVotingSection({ meetings, onFinalize }) {
    return (
        <div className="bg-white rounded-lg shadow">
            <div className="px-6 py-4 border-b border-gray-200 flex justify-between items-center">
                <h2 className="text-xl font-semibold text-gray-900 flex items-center gap-2">
                    <Vote size={20} />
                    Active Voting & Pending Finalization
                </h2>
            </div>

            {meetings.length === 0 ? (
                <div className="p-8 text-center">
                    <CheckCircle className="mx-auto h-12 w-12 text-green-400 mb-4" />
                    <p className="text-slate-500">No active voting sessions or meetings awaiting finalization</p>
                    <p className="text-sm text-slate-400 mt-1">Chairperson will open voting when meetings begin</p>
                </div>
            ) : (
                <div className="p-6 space-y-4">
                    {meetings.map((meeting) => {
                        const isVotingOpen = meeting.status === 'IN_PROGRESS';
                        const isVotingClosed = meeting.status === 'VOTING_CLOSED';

                        return (
                            <div key={meeting.id} className={`border rounded-lg p-4 hover:shadow-md transition ${
                                isVotingOpen ? 'border-green-200 bg-green-50' : 'border-orange-200 bg-orange-50'
                            }`}>
                                <div className="flex justify-between items-start">
                                    <div className="flex-1">
                                        <div className="flex items-center gap-3 mb-2">
                                            <h3 className="text-lg font-bold text-slate-800">{meeting.title}</h3>
                                            {isVotingOpen && (
                                                <span className="px-2 py-1 text-xs font-semibold rounded-full bg-green-100 text-green-800 animate-pulse">
                                                    VOTING OPEN
                                                </span>
                                            )}
                                            {isVotingClosed && (
                                                <span className="px-2 py-1 text-xs font-semibold rounded-full bg-orange-100 text-orange-800">
                                                    AWAITING FINALIZATION
                                                </span>
                                            )}
                                        </div>
                                        <div className="flex flex-wrap gap-4 text-sm text-slate-600">
                                            <div className="flex items-center gap-1">
                                                <Calendar size={14} />
                                                {new Date(meeting.meetingDate).toLocaleDateString('en-US', {
                                                    weekday: 'short',
                                                    year: 'numeric',
                                                    month: 'short',
                                                    day: 'numeric'
                                                })}
                                            </div>
                                            <div className="flex items-center gap-1">
                                                <Clock size={14} />
                                                {meeting.meetingTime}
                                            </div>
                                            <div className="flex items-center gap-1">
                                                <MapPin size={14} />
                                                {meeting.venue}
                                            </div>
                                            <div className="flex items-center gap-1">
                                                <FileText size={14} />
                                                {meeting.loanCount} loan(s) on agenda
                                            </div>
                                        </div>
                                        <p className="text-xs text-slate-400 mt-2">Meeting #: {meeting.meetingNumber}</p>
                                    </div>
                                </div>

                                <div className="flex gap-2 mt-4">
                                    <Link
                                        to={`/meetings/${meeting.id}/results`}
                                        className="flex items-center gap-1 px-3 py-2 text-sm font-semibold text-indigo-600 hover:text-indigo-700 hover:bg-indigo-50 rounded-lg transition border border-indigo-200"
                                    >
                                        <Eye size={16} />
                                        {isVotingOpen ? 'View Live Results' : 'View Results'}
                                    </Link>

                                    {(isVotingClosed || meeting.status === 'COMPLETED') && (
                                        <Link
                                            to={`/meetings/${meeting.id}/minutes`}
                                            className="flex items-center gap-1 px-3 py-2 text-sm font-semibold text-green-600 hover:text-green-700 hover:bg-green-50 rounded-lg transition border border-green-200"
                                        >
                                            <FileText size={16} />
                                            View Minutes
                                        </Link>
                                    )}

                                    {isVotingClosed && (
                                        <button
                                            onClick={() => onFinalize(meeting.id)}
                                            className="flex items-center gap-1 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-semibold transition"
                                        >
                                            <CheckCircle size={16} />
                                            Finalize & Generate Minutes
                                        </button>
                                    )}
                                </div>

                                {isVotingOpen && (
                                    <div className="mt-3 p-3 bg-green-100 border border-green-200 rounded-lg">
                                        <p className="text-xs text-green-800">
                                            <strong>Voting in progress:</strong> Members are currently casting votes.
                                            Chairperson will close voting when ready.
                                        </p>
                                    </div>
                                )}

                                {isVotingClosed && (
                                    <div className="mt-3 p-3 bg-amber-50 border border-amber-200 rounded-lg">
                                        <p className="text-xs text-amber-800">
                                            <strong>Action Required:</strong> Chairperson has closed voting.
                                            Click "Finalize" to generate minutes and forward approved loans for disbursement.
                                        </p>
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}

