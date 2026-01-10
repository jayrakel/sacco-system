import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../api';
import DashboardHeader from '../components/DashboardHeader';
import BrandedSpinner from '../components/BrandedSpinner';
import { Calendar, Clock, MapPin, Users, Vote, CheckCircle, XCircle, AlertCircle, Eye, PlayCircle } from 'lucide-react';

export default function ChairpersonMeetingDashboard() {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [scheduledMeetings, setScheduledMeetings] = useState([]);
    const [activeMeetings, setActiveMeetings] = useState([]);

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
        loadMeetings();

        // Auto-refresh every 30 seconds
        const interval = setInterval(loadMeetings, 30000);
        return () => clearInterval(interval);
    }, []);

    const loadMeetings = async () => {
        setLoading(true);
        try {
            const res = await api.get('/api/meetings/scheduled');
            const meetings = res.data.data || [];

            // Separate scheduled vs in-progress
            const scheduled = meetings.filter(m => m.status === 'SCHEDULED');
            const active = meetings.filter(m => m.status === 'IN_PROGRESS');

            setScheduledMeetings(scheduled);
            setActiveMeetings(active);
        } catch (error) {
            console.error('Failed to load meetings:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleOpenVoting = async (meetingId) => {
        if (!window.confirm('Open voting for this meeting? This can only be done after the meeting time has passed.')) {
            return;
        }

        try {
            await api.post(`/api/voting/meetings/${meetingId}/open`);
            alert('Voting opened successfully! Committee members can now vote.');
            loadMeetings();
        } catch (error) {
            alert(error.response?.data?.message || 'Failed to open voting');
        }
    };

    const handleCloseVoting = async (meetingId) => {
        if (!window.confirm('Close voting and finalize results? This action cannot be undone.')) {
            return;
        }

        try {
            await api.post(`/api/voting/meetings/${meetingId}/close`);
            alert('Voting closed successfully! Results have been finalized.');
            loadMeetings();
        } catch (error) {
            alert(error.response?.data?.message || 'Failed to close voting');
        }
    };

    const canOpenVoting = (meeting) => {
        const meetingDateTime = new Date(`${meeting.meetingDate}T${meeting.meetingTime}`);
        const now = new Date();
        return now >= meetingDateTime;
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <BrandedSpinner />
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-slate-50">
            <DashboardHeader user={user} title="Chairperson - Meeting Control" />

            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8 pb-12">
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-slate-800">Committee Meeting Control</h1>
                    <p className="text-slate-600 mt-1">Open voting for scheduled meetings and manage active sessions</p>
                </div>

                {/* Statistics */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                    <StatCard
                        icon={<Calendar className="text-blue-600" size={24} />}
                        label="Scheduled Meetings"
                        value={scheduledMeetings.length}
                        bgColor="bg-blue-50"
                    />
                    <StatCard
                        icon={<Vote className="text-green-600" size={24} />}
                        label="Active Voting"
                        value={activeMeetings.length}
                        bgColor="bg-green-50"
                    />
                    <StatCard
                        icon={<Users className="text-purple-600" size={24} />}
                        label="Total Agenda Items"
                        value={[...scheduledMeetings, ...activeMeetings].reduce((sum, m) => sum + (m.loanCount || 0), 0)}
                        bgColor="bg-purple-50"
                    />
                </div>

                {/* Scheduled Meetings - Ready to Open */}
                <div className="mb-8">
                    <h2 className="text-xl font-bold text-slate-800 mb-4 flex items-center gap-2">
                        <Calendar size={20} />
                        Scheduled Meetings - Ready to Open Voting
                    </h2>
                    {scheduledMeetings.length === 0 ? (
                        <div className="bg-white rounded-lg shadow p-8 text-center">
                            <CheckCircle className="mx-auto h-12 w-12 text-green-400 mb-4" />
                            <p className="text-slate-500">No scheduled meetings at this time</p>
                        </div>
                    ) : (
                        <div className="space-y-4">
                            {scheduledMeetings.map((meeting) => (
                                <MeetingCard
                                    key={meeting.id}
                                    meeting={meeting}
                                    canOpenVoting={canOpenVoting(meeting)}
                                    onOpenVoting={handleOpenVoting}
                                    type="scheduled"
                                />
                            ))}
                        </div>
                    )}
                </div>

                {/* Active Voting Sessions */}
                <div>
                    <h2 className="text-xl font-bold text-slate-800 mb-4 flex items-center gap-2">
                        <Vote size={20} />
                        Active Voting Sessions
                    </h2>
                    {activeMeetings.length === 0 ? (
                        <div className="bg-white rounded-lg shadow p-8 text-center">
                            <AlertCircle className="mx-auto h-12 w-12 text-slate-400 mb-4" />
                            <p className="text-slate-500">No active voting sessions</p>
                        </div>
                    ) : (
                        <div className="space-y-4">
                            {activeMeetings.map((meeting) => (
                                <MeetingCard
                                    key={meeting.id}
                                    meeting={meeting}
                                    onCloseVoting={handleCloseVoting}
                                    type="active"
                                />
                            ))}
                        </div>
                    )}
                </div>
            </main>
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

function MeetingCard({ meeting, canOpenVoting, onOpenVoting, onCloseVoting, type }) {
    const meetingDateTime = new Date(`${meeting.meetingDate}T${meeting.meetingTime}`);
    const isPastTime = new Date() >= meetingDateTime;

    return (
        <div className="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition">
            <div className="flex justify-between items-start mb-4">
                <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                        <h3 className="text-lg font-bold text-slate-800">{meeting.title}</h3>
                        <span className={`px-2 py-1 text-xs font-semibold rounded-full ${
                            type === 'active'
                                ? 'bg-green-100 text-green-800'
                                : 'bg-blue-100 text-blue-800'
                        }`}>
                            {type === 'active' ? 'VOTING OPEN' : 'SCHEDULED'}
                        </span>
                    </div>
                    <div className="flex flex-wrap gap-4 text-sm text-slate-600 mb-3">
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
                            {meeting.loanCount} loan(s) on agenda
                        </div>
                    </div>
                    <p className="text-xs text-slate-400">Meeting #: {meeting.meetingNumber}</p>
                </div>
            </div>

            <div className="flex gap-2">
                <Link
                    to={`/meetings/${meeting.id}/results`}
                    className="flex items-center gap-1 px-3 py-2 text-sm font-semibold text-indigo-600 hover:text-indigo-700 hover:bg-indigo-50 rounded-lg transition border border-indigo-200"
                >
                    <Eye size={16} />
                    View Details
                </Link>

                {type === 'scheduled' && (
                    <>
                        {canOpenVoting ? (
                            <button
                                onClick={() => onOpenVoting(meeting.id)}
                                className="flex items-center gap-1 px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg font-semibold transition"
                            >
                                <PlayCircle size={16} />
                                Open Voting
                            </button>
                        ) : (
                            <div className="flex items-center gap-2 px-4 py-2 bg-slate-100 text-slate-500 rounded-lg">
                                <Clock size={16} />
                                <span className="text-sm">
                                    Available {isPastTime ? 'now' : 'at ' + meetingDateTime.toLocaleTimeString()}
                                </span>
                            </div>
                        )}
                    </>
                )}

                {type === 'active' && (
                    <button
                        onClick={() => onCloseVoting(meeting.id)}
                        className="flex items-center gap-1 px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg font-semibold transition"
                    >
                        <XCircle size={16} />
                        Close Voting
                    </button>
                )}
            </div>
        </div>
    );
}

