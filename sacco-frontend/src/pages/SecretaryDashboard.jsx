import React, { useState, useEffect } from 'react';
import api from '../api'; // ✅ FIXED PATH
import { FileText, Calendar, Clock, Users, CheckCircle, Gavel, XCircle } from 'lucide-react';
import DashboardHeader from '../components/DashboardHeader'; // ✅ FIXED PATH
import BrandedSpinner from '../components/BrandedSpinner'; // ✅ FIXED PATH
import ShareCapitalCard from '../components/ShareCapitalCard'; // ✅ FIXED PATH

export default function SecretaryDashboard() {
    const [user, setUser] = useState(null);
    const [pendingLoans, setPendingLoans] = useState([]); // APPROVED (To Schedule)
    const [scheduledLoans, setScheduledLoans] = useState([]); // SECRETARY_TABLED (Scheduled)
    const [activeVotes, setActiveVotes] = useState([]);   // VOTING_OPEN (Monitor & Finalize)
    const [loading, setLoading] = useState(true);

    // Modal State
    const [selectedLoan, setSelectedLoan] = useState(null);
    const [meetingDateTime, setMeetingDateTime] = useState('');

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
        fetchDashboardData();
    }, []);

    const fetchDashboardData = async () => {
        setLoading(true);
        try {
            const res = await api.get('/api/loans/admin/pending');
            if (res.data.success) {
                const allData = res.data.data;
                setPendingLoans(allData.filter(l => l.status === 'APPROVED'));
                setScheduledLoans(allData.filter(l => l.status === 'SECRETARY_TABLED'));
                // ✅ NEW: Fetch active votes to monitor progress
                setActiveVotes(allData.filter(l => l.status === 'VOTING_OPEN'));
            }
        } catch (e) {
            console.error("Failed to load secretary data", e);
        } finally {
            setLoading(false);
        }
    };

    const handleTableLoan = async () => {
        if (!selectedLoan || !meetingDateTime) return alert("Please select date and time.");

        try {
            await api.post(`/api/loans/secretary/${selectedLoan.id}/table`, { meetingDate: meetingDateTime });
            alert(`Loan ${selectedLoan.loanNumber} scheduled for ${meetingDateTime.replace('T', ' ')}.`);
            setSelectedLoan(null);
            fetchDashboardData();
        } catch (error) {
            alert(error.response?.data?.message || "Failed to table loan. Please try again.");
        }
    };

    // ✅ NEW: Secretary starts voting manually if needed (or Chair does it, depending on strictness)
    const handleStartVoting = async (loanId) => {
        if(!window.confirm("Start voting for this loan?")) return;
        try {
            await api.post(`/api/loans/secretary/${loanId}/start-voting`); // Using shared service logic
            alert("Voting Started.");
            fetchDashboardData();
        } catch (e) { alert("Failed to start voting."); }
    };

    // ✅ NEW: Secretary Finalizes based on counts
    const handleFinalizeVote = async (loanId, approved) => {
        const decision = approved ? "APPROVE" : "REJECT";
        const minutes = prompt(`Enter meeting minutes regarding this ${decision}:`);
        if (!minutes) return;

        try {
            await api.post(`/api/loans/secretary/${loanId}/finalize-vote`, { approved, minutes });
            alert(`Vote Finalized: ${decision}D`);
            fetchDashboardData();
        } catch (e) { alert("Failed to finalize vote."); }
    };

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800 pb-12">
            <DashboardHeader user={user} title="Secretariat Portal" />

            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8 space-y-8 animate-in fade-in">

                {/* 1. HEADER & STATS */}
                <div className="flex flex-col md:flex-row justify-between items-end gap-4">
                    <div>
                        <h1 className="text-2xl font-bold text-slate-800">Meeting Management</h1>
                        <p className="text-slate-500 text-sm">Prepare committee agendas and monitor voting progress.</p>
                    </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <StatCard icon={<FileText size={24}/>} color="amber" label="To Schedule" value={pendingLoans.length} />
                    <StatCard icon={<Gavel size={24}/>} color="purple" label="Voting Active" value={activeVotes.length} />
                    <ShareCapitalCard />
                </div>

                {/* 2. ACTIVE VOTING MONITOR (NEW SECTION) */}
                {activeVotes.length > 0 && (
                    <div className="bg-white rounded-2xl shadow-sm border border-purple-200 overflow-hidden">
                        <div className="p-6 border-b border-purple-100 bg-purple-50 flex justify-between items-center">
                            <h2 className="font-bold text-purple-900 flex items-center gap-2">
                                <Gavel size={20}/> Voting In Progress
                            </h2>
                            <span className="bg-purple-200 text-purple-800 text-xs font-bold px-2 py-1 rounded">Action Required</span>
                        </div>
                        <div className="p-4 grid gap-4">
                            {activeVotes.map(loan => (
                                <div key={loan.id} className="flex flex-col md:flex-row justify-between items-center p-4 bg-white border rounded-xl shadow-sm gap-4">
                                    <div>
                                        <h3 className="font-bold text-lg">{loan.memberName}</h3>
                                        <p className="text-sm text-slate-500 font-mono">{loan.loanNumber} • KES {Number(loan.principalAmount).toLocaleString()}</p>
                                    </div>

                                    {/* ✅ VOTING PROGRESS DISPLAY */}
                                    <div className="flex items-center gap-6 bg-slate-50 px-4 py-2 rounded-lg">
                                        <div className="text-center">
                                            <span className="block text-xs font-bold text-slate-400 uppercase">Yes</span>
                                            <span className="block text-xl font-bold text-emerald-600">{loan.votesYes || 0}</span>
                                        </div>
                                        <div className="w-px h-8 bg-slate-200"></div>
                                        <div className="text-center">
                                            <span className="block text-xs font-bold text-slate-400 uppercase">No</span>
                                            <span className="block text-xl font-bold text-red-600">{loan.votesNo || 0}</span>
                                        </div>
                                    </div>

                                    <div className="flex gap-2">
                                        <button onClick={() => handleFinalizeVote(loan.id, false)} className="flex items-center gap-1 px-4 py-2 border border-red-200 text-red-600 rounded-lg text-sm font-bold hover:bg-red-50">
                                            <XCircle size={16}/> Reject
                                        </button>
                                        <button onClick={() => handleFinalizeVote(loan.id, true)} className="flex items-center gap-1 px-4 py-2 bg-emerald-600 text-white rounded-lg text-sm font-bold hover:bg-emerald-700 shadow-sm">
                                            <CheckCircle size={16}/> Pass Vote
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {/* 3. PENDING SCHEDULING */}
                <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
                    <div className="p-6 border-b border-slate-100 bg-slate-50/50 flex justify-between items-center">
                        <h2 className="font-bold text-slate-800 flex items-center gap-2">
                            <Clock size={20} className="text-amber-600"/> Pending Scheduling
                        </h2>
                        <span className="text-xs font-bold bg-amber-100 text-amber-700 px-3 py-1 rounded-full">
                            {pendingLoans.length} Loans
                        </span>
                    </div>

                    <table className="w-full text-sm text-left">
                        <thead className="bg-slate-50 text-slate-500 font-bold border-b border-slate-100 uppercase text-xs">
                            <tr>
                                <th className="p-4">Reference</th>
                                <th className="p-4">Applicant</th>
                                <th className="p-4 text-right">Amount</th>
                                <th className="p-4">Action</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {loading ? (
                                <tr><td colSpan="4" className="p-12 text-center"><BrandedSpinner size="medium"/></td></tr>
                            ) : pendingLoans.map(loan => (
                                <tr key={loan.id} className="hover:bg-amber-50/30 transition">
                                    <td className="p-4 font-mono text-slate-500 text-xs">{loan.loanNumber}</td>
                                    <td className="p-4 font-bold text-slate-700">{loan.memberName}</td>
                                    <td className="p-4 text-right font-mono font-bold">KES {Number(loan.principalAmount).toLocaleString()}</td>
                                    <td className="p-4 text-center">
                                        <button
                                            onClick={() => setSelectedLoan(loan)}
                                            className="inline-flex items-center gap-2 px-4 py-2 bg-slate-900 text-white rounded-lg text-xs font-bold hover:bg-slate-800 transition shadow-sm"
                                        >
                                            <Calendar size={14}/> Schedule
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>

                {/* 4. SCHEDULED LIST */}
                {scheduledLoans.length > 0 && (
                    <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden opacity-90 hover:opacity-100 transition">
                        <div className="p-6 border-b border-slate-100 bg-slate-50/50">
                            <h2 className="font-bold text-slate-800 flex items-center gap-2">
                                <Users size={20} className="text-blue-600"/> Upcoming Agenda Items
                            </h2>
                        </div>
                        <div className="p-4 grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                            {scheduledLoans.map(loan => (
                                <div key={loan.id} className="p-4 border rounded-xl flex items-center justify-between bg-slate-50">
                                    <div>
                                        <p className="font-bold text-sm text-slate-700">{loan.memberName}</p>
                                        <p className="text-xs text-blue-600 font-bold mt-1">
                                            {new Date(loan.meetingDate).toLocaleString('en-US', { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' })}
                                        </p>
                                    </div>
                                    <button onClick={() => handleStartVoting(loan.id)} className="px-3 py-1 bg-blue-600 text-white text-xs font-bold rounded hover:bg-blue-700">Start Voting</button>
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </main>

            {/* MODAL */}
            {selectedLoan && (
                <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm overflow-hidden animate-in zoom-in-95 duration-200">
                        <div className="bg-slate-50 p-6 border-b border-slate-100">
                            <h3 className="text-lg font-bold text-slate-800">Schedule Meeting</h3>
                            <p className="text-sm text-slate-500">Select precise date & time for Loan {selectedLoan.loanNumber}</p>
                        </div>
                        <div className="p-6">
                            <label className="block text-xs font-bold text-slate-500 uppercase mb-2">Meeting Date & Time</label>
                            <input
                                type="datetime-local"
                                value={meetingDateTime}
                                onChange={(e) => setMeetingDateTime(e.target.value)}
                                className="w-full p-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-amber-500 outline-none text-slate-700"
                            />
                            <div className="mt-6 flex gap-3">
                                <button onClick={() => setSelectedLoan(null)} className="flex-1 py-3 bg-white border border-slate-200 text-slate-600 font-bold rounded-xl hover:bg-slate-50">Cancel</button>
                                <button onClick={handleTableLoan} className="flex-1 py-3 bg-slate-900 text-white font-bold rounded-xl hover:bg-slate-800 transition flex items-center justify-center gap-2"><CheckCircle size={18}/> Confirm</button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

function StatCard({ icon, color, label, value }) {
    const colors = { amber: "bg-amber-100 text-amber-600", purple: "bg-purple-100 text-purple-600", blue: "bg-blue-100 text-blue-600" };
    return (
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 flex items-center gap-4 hover:shadow-md transition">
            <div className={`p-4 rounded-xl ${colors[color]}`}>{icon}</div>
            <div><p className="text-xs font-bold text-slate-400 uppercase tracking-wider">{label}</p><h3 className="text-3xl font-black text-slate-800 mt-1">{value}</h3></div>
        </div>
    );
}