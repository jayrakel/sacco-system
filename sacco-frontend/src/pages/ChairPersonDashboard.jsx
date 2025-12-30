import React, { useState, useEffect } from 'react';
import api from '../api';
import { Users, TrendingUp, CheckCircle, Gavel, Calendar, Clock, AlertCircle } from 'lucide-react';
import DashboardHeader from '../components/DashboardHeader';
import BrandedSpinner from '../components/BrandedSpinner';
import ShareCapitalCard from '../components/ShareCapitalCard';

export default function ChairpersonDashboard() {
    const [user, setUser] = useState(null);

    // ✅ NEW STATES for the missing stages
    const [scheduledMeetings, setScheduledMeetings] = useState([]); // Status: SECRETARY_TABLED
    const [activeVotes, setActiveVotes] = useState([]);             // Status: VOTING_OPEN
    const [approvalQueue, setApprovalQueue] = useState([]);         // Status: SECRETARY_DECISION

    const [loading, setLoading] = useState(true);
    const [currentTime, setCurrentTime] = useState(new Date());

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
        fetchAgenda();

        // Update clock every minute so the "Start Voting" button enables automatically when time reaches
        const timer = setInterval(() => setCurrentTime(new Date()), 60000);
        return () => clearInterval(timer);
    }, []);

    const fetchAgenda = async () => {
        setLoading(true);
        try {
            const res = await api.get('/api/loans/admin/pending');
            if (res.data.success) {
                const data = res.data.data;

                // ✅ 1. Scheduled: Ready for Chair to Open Floor
                setScheduledMeetings(data.filter(l => l.status === 'SECRETARY_TABLED'));

                // ✅ 2. Active: Currently being voted on
                setActiveVotes(data.filter(l => l.status === 'VOTING_OPEN'));

                // ✅ 3. Decision: Passed vote, waiting for final sign-off (Existing)
                setApprovalQueue(data.filter(l => l.status === 'SECRETARY_DECISION'));
            }
        } catch (e) {
            console.error("Failed to load dashboard", e);
        } finally {
            setLoading(false);
        }
    };

    // ✅ NEW HANDLER: Opens the voting floor
    const handleStartVoting = async (loan) => {
        if (!window.confirm(`Open the floor for voting on Loan ${loan.loanNumber}?`)) return;
        try {
            await api.post(`/api/loans/chairperson/${loan.id}/start-voting`);
            alert("Voting Session Started. Committee members can now vote.");
            fetchAgenda();
        } catch (error) {
            alert(error.response?.data?.message || "Failed to start voting");
        }
    };

    const handleFinalApproval = async (loan) => {
        if (!window.confirm(`Grant Final Executive Approval for Loan ${loan.loanNumber}? This authorizes disbursement.`)) return;
        try {
            await api.post(`/api/loans/chairperson/${loan.id}/final-approval`);
            alert("Final Approval Granted. Loan forwarded to Treasurer.");
            fetchAgenda();
        } catch (error) {
            alert(error.response?.data?.message || "Action failed");
        }
    };

    // Helper: Check if meeting time has passed
    const isMeetingTime = (dateStr) => {
        if (!dateStr) return true;
        return new Date() >= new Date(dateStr);
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
                    {/* Scheduled Card */}
                    <div className="bg-white p-8 rounded-2xl shadow-sm border border-blue-100 flex flex-col items-center text-center">
                        <div className="p-4 bg-blue-50 text-blue-600 rounded-full mb-4"><Calendar size={32}/></div>
                        <h3 className="font-bold text-slate-700 text-lg">Scheduled</h3>
                        <div className="text-3xl font-bold text-slate-900 mt-2">{scheduledMeetings.length}</div>
                    </div>

                    {/* Active Votes Card */}
                    <div className="bg-white p-8 rounded-2xl shadow-sm border border-purple-100 flex flex-col items-center text-center">
                        <div className="p-4 bg-purple-50 text-purple-600 rounded-full mb-4"><Users size={32}/></div>
                        <h3 className="font-bold text-slate-700 text-lg">Active Votes</h3>
                        <div className="text-3xl font-bold text-slate-900 mt-2">{activeVotes.length}</div>
                    </div>

                    {/* Pending Sign-off Card */}
                    <div className="bg-white p-8 rounded-2xl shadow-sm border border-indigo-100 flex flex-col items-center text-center">
                        <div className="p-4 bg-indigo-50 text-indigo-600 rounded-full mb-4"><Gavel size={32}/></div>
                        <h3 className="font-bold text-slate-700 text-lg">Pending Sign-offs</h3>
                        <div className="text-3xl font-bold text-indigo-600 mt-2">
                            {approvalQueue.length}
                        </div>
                    </div>

                    <ShareCapitalCard />
                </div>

                {/* 2. ✅ SCHEDULED MEETINGS (New Section) */}
                {scheduledMeetings.length > 0 && (
                    <div className="bg-white rounded-2xl shadow-sm border border-blue-200 overflow-hidden">
                        <div className="p-6 border-b border-blue-100 bg-blue-50/50 flex justify-between items-center">
                            <h2 className="text-lg font-bold text-blue-900 flex items-center gap-2">
                                <Clock size={20}/> Upcoming Meetings (Action Required)
                            </h2>
                            <span className="text-xs bg-blue-100 text-blue-700 px-3 py-1 rounded-full font-bold">
                                {scheduledMeetings.length} Scheduled
                            </span>
                        </div>
                        <div className="divide-y divide-blue-50">
                            {scheduledMeetings.map(loan => {
                                const canStart = isMeetingTime(loan.meetingDate);
                                return (
                                    <div key={loan.id} className="p-6 flex flex-col md:flex-row justify-between items-center hover:bg-slate-50 transition gap-4">
                                        <div>
                                            <h3 className="font-bold text-slate-800">{loan.memberName}</h3>
                                            <p className="text-sm text-slate-500 font-mono">{loan.loanNumber}</p>
                                            <div className="flex items-center gap-2 mt-1 text-sm font-bold text-blue-600">
                                                <Calendar size={14}/>
                                                <span>
                                                    {new Date(loan.meetingDate).toLocaleString('en-US', {
                                                        weekday: 'short', month: 'short', day: 'numeric',
                                                        hour: 'numeric', minute: '2-digit'
                                                    })}
                                                </span>
                                            </div>
                                        </div>

                                        <button
                                            onClick={() => handleStartVoting(loan)}
                                            disabled={!canStart}
                                            className={`flex items-center gap-2 px-6 py-3 rounded-lg text-sm font-bold transition shadow-md
                                                ${canStart
                                                    ? 'bg-blue-600 text-white hover:bg-blue-700 shadow-blue-900/20'
                                                    : 'bg-slate-100 text-slate-400 cursor-not-allowed'
                                                }`}
                                        >
                                            <Gavel size={16}/>
                                            {canStart ? "Open Voting Floor" : "Wait for Meeting Time"}
                                        </button>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                )}

                {/* 3. ✅ ACTIVE VOTES MONITOR (New Section) */}
                {activeVotes.length > 0 && (
                    <div className="bg-purple-50 rounded-2xl border border-purple-100 p-6">
                        <h2 className="text-lg font-bold text-purple-900 mb-4 flex items-center gap-2">
                            <Users size={20}/> Voting In Progress
                        </h2>
                        <div className="grid gap-4">
                            {activeVotes.map(loan => (
                                <div key={loan.id} className="bg-white p-4 rounded-xl border border-purple-100 flex justify-between items-center shadow-sm">
                                    <div>
                                        <p className="font-bold text-slate-800">{loan.memberName}</p>
                                        <p className="text-xs text-slate-500 font-mono">{loan.loanNumber}</p>
                                    </div>
                                    <div className="flex items-center gap-4">
                                        <div className="text-xs text-slate-500">
                                            <span className="font-bold text-green-600">{loan.votesYes || 0} YES</span>
                                            <span className="mx-2">/</span>
                                            <span className="font-bold text-red-600">{loan.votesNo || 0} NO</span>
                                        </div>
                                        <span className="text-xs font-bold bg-purple-100 text-purple-700 px-3 py-1 rounded-full animate-pulse">
                                            Voting Open
                                        </span>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {/* 4. FINAL APPROVAL TABLE (Existing) */}
                <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
                    <div className="p-6 border-b border-slate-100 bg-slate-50/50 flex justify-between items-center">
                        <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                            <CheckCircle size={20} className="text-indigo-600"/> Executive Sign-Off Required
                        </h2>
                        <span className="text-xs bg-indigo-100 text-indigo-700 px-3 py-1 rounded-full font-bold">
                            {approvalQueue.length} Pending
                        </span>
                    </div>

                    <table className="w-full text-sm text-left">
                        <thead className="bg-slate-50 text-slate-500 font-bold border-b border-slate-100 uppercase text-xs">
                            <tr>
                                <th className="p-4">Ref</th>
                                <th className="p-4">Applicant</th>
                                <th className="p-4 text-right">Amount</th>
                                <th className="p-4">Status</th>
                                <th className="p-4 text-center">Action</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {loading ? (
                                <tr><td colSpan="5" className="p-12 text-center"><BrandedSpinner size="medium"/></td></tr>
                            ) : approvalQueue.length === 0 ? (
                                <tr><td colSpan="5" className="p-8 text-center text-slate-400 italic">No final approvals pending.</td></tr>
                            ) : approvalQueue.map(loan => (
                                <tr key={loan.id} className="hover:bg-slate-50 transition">
                                    <td className="p-4 font-mono text-slate-500 text-xs">{loan.loanNumber}</td>
                                    <td className="p-4">
                                        <p className="font-bold text-slate-700">{loan.memberName}</p>
                                    </td>
                                    <td className="p-4 text-right font-mono font-bold text-slate-700">
                                        KES {Number(loan.principalAmount).toLocaleString()}
                                    </td>
                                    <td className="p-4 text-xs">
                                        <span className="bg-emerald-50 text-emerald-700 px-2 py-1 rounded font-bold border border-emerald-100">
                                            Passed Vote
                                        </span>
                                    </td>
                                    <td className="p-4 flex justify-center">
                                        <button
                                            onClick={() => handleFinalApproval(loan)}
                                            className="flex items-center gap-2 px-6 py-2 bg-indigo-900 text-white rounded-lg text-xs font-bold hover:bg-indigo-800 transition shadow-lg shadow-indigo-900/20"
                                        >
                                            <Gavel size={14}/> Grant Final Approval
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </main>
        </div>
    );
}