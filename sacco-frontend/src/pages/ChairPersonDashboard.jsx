import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../api';
import { Users, TrendingUp, CheckCircle, Gavel, Calendar, Clock, AlertCircle, Vote, PlayCircle, XCircle, Eye, MapPin } from 'lucide-react';
import DashboardHeader from '../components/DashboardHeader';
import BrandedSpinner from '../components/BrandedSpinner';
import ShareCapitalCard from '../components/ShareCapitalCard';

export default function ChairpersonDashboard() {
    const [user, setUser] = useState(null);

    // ✅ UPDATED: Use new meeting-based states
    const [scheduledMeetings, setScheduledMeetings] = useState([]);
    const [activeMeetings, setActiveMeetings] = useState([]);
    const [completedMeetings, setCompletedMeetings] = useState([]);

    const [loading, setLoading] = useState(true);
    const [currentTime, setCurrentTime] = useState(new Date());

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
        fetchAgenda();

        // Update clock every minute
        const timer = setInterval(() => setCurrentTime(new Date()), 60000);

        // Auto-refresh meetings every 30 seconds
        const refreshInterval = setInterval(fetchAgenda, 30000);

        return () => {
            clearInterval(timer);
            clearInterval(refreshInterval);
        };
    }, []);

    const fetchAgenda = async () => {
        setLoading(true);
        try {
            // ✅ Use /all endpoint to get meetings of all statuses
            const res = await api.get('/api/meetings/all');
            const meetings = res.data.data || [];

            // Separate by status
            setScheduledMeetings(meetings.filter(m => m.status === 'SCHEDULED'));
            setActiveMeetings(meetings.filter(m => m.status === 'IN_PROGRESS'));
            setCompletedMeetings(meetings.filter(m => m.status === 'COMPLETED'));
        } catch (e) {
            console.error("Failed to load dashboard", e);
        } finally {
            setLoading(false);
        }
    };

    // ✅ UPDATED: Open voting for a meeting
    const handleOpenVoting = async (meetingId) => {
        if (!window.confirm('Open voting for this meeting? This can only be done after the meeting time has passed.')) {
            return;
        }

        try {
            await api.post(`/api/voting/meetings/${meetingId}/open`);
            alert("Voting opened successfully! Committee members can now vote.");
            fetchAgenda();
        } catch (error) {
            alert(error.response?.data?.message || "Failed to open voting");
        }
    };

    // ✅ Close voting (no more votes can be cast)
    const handleCloseVoting = async (meetingId) => {
        if (!window.confirm('Close voting? No more votes will be accepted. Secretary will finalize results and generate minutes.')) {
            return;
        }

        try {
            await api.post(`/api/voting/meetings/${meetingId}/close`);
            alert("Voting closed successfully! No more votes can be cast. Awaiting secretary to finalize results.");
            fetchAgenda();
        } catch (error) {
            alert(error.response?.data?.message || "Failed to close voting");
        }
    };

    // Helper: Check if meeting time has passed
    const canOpenVoting = (meeting) => {
        const meetingDateTime = new Date(`${meeting.meetingDate}T${meeting.meetingTime}`);
        return new Date() >= meetingDateTime;
    };

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800 pb-12">
            <DashboardHeader user={user} title="Executive Dashboard" />

            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8 space-y-8 animate-in fade-in">

                {/* 1. Executive Overview (Updated Counts) */}
                <div>
                    <h1 className="text-2xl font-bold text-slate-800">Executive Overview</h1>
                    <p className="text-slate-500 text-sm">High-level insights and governance actions.</p>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                    {/* Scheduled Meetings Card */}
                    <div className="bg-white p-8 rounded-2xl shadow-sm border border-blue-100 flex flex-col items-center text-center">
                        <div className="p-4 bg-blue-50 text-blue-600 rounded-full mb-4"><Calendar size={32}/></div>
                        <h3 className="font-bold text-slate-700 text-lg">Scheduled Meetings</h3>
                        <div className="text-3xl font-bold text-slate-900 mt-2">{scheduledMeetings.length}</div>
                    </div>

                    {/* Active Voting Sessions Card */}
                    <div className="bg-white p-8 rounded-2xl shadow-sm border border-green-100 flex flex-col items-center text-center">
                        <div className="p-4 bg-green-50 text-green-600 rounded-full mb-4"><Vote size={32}/></div>
                        <h3 className="font-bold text-slate-700 text-lg">Active Voting</h3>
                        <div className="text-3xl font-bold text-slate-900 mt-2">{activeMeetings.length}</div>
                    </div>

                    {/* Completed Meetings Card */}
                    <div className="bg-white p-8 rounded-2xl shadow-sm border border-indigo-100 flex flex-col items-center text-center">
                        <div className="p-4 bg-indigo-50 text-indigo-600 rounded-full mb-4"><CheckCircle size={32}/></div>
                        <h3 className="font-bold text-slate-700 text-lg">Completed</h3>
                        <div className="text-3xl font-bold text-indigo-600 mt-2">
                            {completedMeetings.length}
                        </div>
                    </div>

                    <ShareCapitalCard />
                </div>

                {/* 2. ✅ SCHEDULED MEETINGS - Ready to Open Voting */}
                {scheduledMeetings.length > 0 && (
                    <div className="bg-white rounded-2xl shadow-sm border border-blue-200 overflow-hidden">
                        <div className="p-6 border-b border-blue-100 bg-blue-50/50 flex justify-between items-center">
                            <h2 className="text-lg font-bold text-blue-900 flex items-center gap-2">
                                <Calendar size={20}/> Scheduled Meetings - Ready to Open Voting
                            </h2>
                            <span className="text-xs bg-blue-100 text-blue-700 px-3 py-1 rounded-full font-bold">
                                {scheduledMeetings.length} Scheduled
                            </span>
                        </div>
                        <div className="divide-y divide-blue-50">
                            {scheduledMeetings.map(meeting => {
                                const canOpen = canOpenVoting(meeting);
                                const meetingDateTime = new Date(`${meeting.meetingDate}T${meeting.meetingTime}`);
                                return (
                                    <div key={meeting.id} className="p-6 hover:bg-slate-50 transition">
                                        <div className="flex justify-between items-start mb-4">
                                            <div className="flex-1">
                                                <div className="flex items-center gap-3 mb-2">
                                                    <h3 className="font-bold text-slate-800 text-lg">{meeting.title}</h3>
                                                    <span className="px-2 py-1 text-xs font-semibold rounded-full bg-blue-100 text-blue-800">
                                                        SCHEDULED
                                                    </span>
                                                </div>
                                                <div className="flex flex-wrap gap-4 text-sm text-slate-600 mb-2">
                                                    <div className="flex items-center gap-1">
                                                        <Calendar size={14}/>
                                                        {new Date(meeting.meetingDate).toLocaleDateString('en-US', {
                                                            weekday: 'short',
                                                            year: 'numeric',
                                                            month: 'short',
                                                            day: 'numeric'
                                                        })}
                                                    </div>
                                                    <div className="flex items-center gap-1">
                                                        <Clock size={14}/>
                                                        {meeting.meetingTime}
                                                    </div>
                                                    <div className="flex items-center gap-1">
                                                        <MapPin size={14}/>
                                                        {meeting.venue}
                                                    </div>
                                                    <div className="flex items-center gap-1">
                                                        <Users size={14}/>
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
                                                <Eye size={16}/>
                                                View Details
                                            </Link>
                                            {canOpen ? (
                                                <button
                                                    onClick={() => handleOpenVoting(meeting.id)}
                                                    className="flex items-center gap-1 px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg font-semibold transition"
                                                >
                                                    <PlayCircle size={16}/>
                                                    Open Voting
                                                </button>
                                            ) : (
                                                <div className="flex items-center gap-2 px-4 py-2 bg-slate-100 text-slate-500 rounded-lg">
                                                    <Clock size={16}/>
                                                    <span className="text-sm">
                                                        Available at {meetingDateTime.toLocaleTimeString()}
                                                    </span>
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                )}

                {/* 3. ✅ ACTIVE VOTING SESSIONS */}
                {activeMeetings.length > 0 && (
                    <div className="bg-green-50 rounded-2xl border border-green-100 p-6">
                        <div className="flex justify-between items-center mb-4">
                            <h2 className="text-lg font-bold text-green-900 flex items-center gap-2">
                                <Vote size={20}/> Active Voting Sessions
                            </h2>
                        </div>
                        <div className="grid gap-4">
                            {activeMeetings.map(meeting => (
                                <div key={meeting.id} className="bg-white p-4 rounded-xl border border-green-100 shadow-sm">
                                    <div className="flex justify-between items-center">
                                        <div className="flex-1">
                                            <p className="font-bold text-slate-800">{meeting.title}</p>
                                            <p className="text-xs text-slate-500">Meeting #: {meeting.meetingNumber}</p>
                                            <p className="text-xs text-slate-600 mt-1">{meeting.loanCount} loans • {meeting.venue}</p>
                                        </div>
                                        <div className="flex items-center gap-3">
                                            <span className="text-xs font-bold bg-green-100 text-green-700 px-3 py-1 rounded-full animate-pulse">
                                                Voting Open
                                            </span>
                                            <Link
                                                to={`/meetings/${meeting.id}/results`}
                                                className="flex items-center gap-1 px-3 py-2 text-sm font-semibold text-indigo-600 hover:text-indigo-700 hover:bg-indigo-50 rounded-lg transition border border-indigo-200"
                                            >
                                                <Eye size={16}/>
                                                View Votes
                                            </Link>
                                            <button
                                                onClick={() => handleCloseVoting(meeting.id)}
                                                className="flex items-center gap-1 px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg font-semibold transition text-sm"
                                            >
                                                <XCircle size={16}/>
                                                Close Voting
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {/* 4. COMPLETED MEETINGS */}
                {completedMeetings.length > 0 && (
                    <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
                        <div className="p-6 border-b border-slate-100 bg-slate-50/50 flex justify-between items-center">
                            <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                                <CheckCircle size={20} className="text-indigo-600"/> Completed Meetings
                            </h2>
                            <span className="text-xs bg-indigo-100 text-indigo-700 px-3 py-1 rounded-full font-bold">
                                {completedMeetings.length} Completed
                            </span>
                        </div>
                        <div className="divide-y divide-slate-100">
                            {loading ? (
                                <div className="p-12 text-center"><BrandedSpinner size="medium"/></div>
                            ) : completedMeetings.map(meeting => (
                                <div key={meeting.id} className="p-6 hover:bg-slate-50 transition">
                                    <div className="flex justify-between items-center">
                                        <div>
                                            <h3 className="font-bold text-slate-800">{meeting.title}</h3>
                                            <p className="text-xs text-slate-500 font-mono">{meeting.meetingNumber}</p>
                                            <p className="text-xs text-slate-600 mt-1">
                                                {meeting.loanCount} loans • {new Date(meeting.meetingDate).toLocaleDateString()}
                                            </p>
                                        </div>
                                        <Link
                                            to={`/meetings/${meeting.id}/results`}
                                            className="flex items-center gap-1 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-semibold transition text-sm"
                                        >
                                            <Eye size={16}/>
                                            View Results
                                        </Link>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {/* Empty State */}
                {!loading && scheduledMeetings.length === 0 && activeMeetings.length === 0 && completedMeetings.length === 0 && (
                    <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-12 text-center">
                        <Calendar className="mx-auto h-16 w-16 text-slate-300 mb-4" />
                        <h3 className="text-xl font-semibold text-slate-700 mb-2">No Meetings</h3>
                        <p className="text-slate-500">
                            There are no committee meetings scheduled at this time.
                            <br />
                            The secretary will schedule meetings when loans are ready for review.
                        </p>
                    </div>
                )}
            </main>
        </div>
    );
}