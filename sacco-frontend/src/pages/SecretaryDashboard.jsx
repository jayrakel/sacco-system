import React, { useState, useEffect } from 'react';
import api from '../api';
import { useSettings } from '../context/SettingsContext'; // ✅ Import Settings Context
import { FileText, Calendar, CheckSquare, Users, Clock, Gavel, AlertCircle, CheckCircle, XCircle } from 'lucide-react';
import DashboardHeader from '../components/DashboardHeader';
import BrandedSpinner from '../components/BrandedSpinner';

export default function SecretaryDashboard() {
    const { settings } = useSettings(); // ✅ Access System Settings
    const [user, setUser] = useState(null);
    const [loans, setLoans] = useState([]);
    const [loading, setLoading] = useState(true);

    // Tabling Modal State
    const [selectedLoan, setSelectedLoan] = useState(null);
    const [meetingDate, setMeetingDate] = useState(new Date().toISOString().split('T')[0]);

    // ✅ Finalize Modal State
    const [finalizeLoan, setFinalizeLoan] = useState(null);
    const [manualDecision, setManualDecision] = useState(null); // true = Approve, false = Reject
    const [secretaryComments, setSecretaryComments] = useState('');

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
        fetchTabledLoans();
    }, []);

    const fetchTabledLoans = async () => {
        setLoading(true);
        try {
            const res = await api.get('/api/loans');
            if (res.data.success) {
                const readyLoans = res.data.data.filter(l =>
                    l.status === 'SECRETARY_TABLED' || l.status === 'VOTING_OPEN'
                );
                setLoans(readyLoans);
            }
        } catch (e) {
            console.error(e);
        } finally {
            setLoading(false);
        }
    };

    // 1. TABLE LOAN (Start Voting)
    const handleTableLoan = async () => {
        if (!selectedLoan) return;
        try {
            await api.post(`/api/loans/${selectedLoan.id}/table`, null, {
                params: { meetingDate }
            });
            // Auto-start voting immediately after tabling for convenience?
            // Or let them click "Start Voting" separately. For now, strictly tabling.
            alert(`Loan ${selectedLoan.loanNumber} added to agenda.`);

            // Optional: Automatically open voting floor
            await api.post(`/api/loans/${selectedLoan.id}/start-voting`);

            setSelectedLoan(null);
            fetchTabledLoans();
        } catch (error) {
            alert(error.response?.data?.message || "Failed to table loan");
        }
    };

    // 2. FINALIZE VOTE (End Voting)
    const handleFinalizeVote = async () => {
        if (!finalizeLoan) return;

        const isManual = settings?.LOAN_VOTING_METHOD === 'MANUAL';

        // Validation for Manual Mode
        if (isManual && manualDecision === null) {
            alert("Please select a decision (Approve or Reject).");
            return;
        }

        try {
            await api.post(`/api/loans/${finalizeLoan.id}/finalize`, null, {
                params: {
                    approved: isManual ? manualDecision : true, // Ignored by backend if AUTOMATIC
                    comments: secretaryComments || (isManual ? '' : 'Automatic Tally')
                }
            });

            alert(isManual ? "Decision Recorded Successfully" : "Votes Tallied & Outcome Decided");
            setFinalizeLoan(null);
            setManualDecision(null);
            setSecretaryComments('');
            fetchTabledLoans();
        } catch (error) {
            alert(error.response?.data?.message || "Failed to finalize vote");
        }
    };

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800 pb-12">
            <DashboardHeader user={user} title="Secretariat Portal" />

            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8 space-y-8 animate-in fade-in">

                {/* HEADER & MODE BADGE */}
                <div className="flex justify-between items-end">
                    <div>
                        <h1 className="text-2xl font-bold text-slate-800">Meeting & Minutes Management</h1>
                        <p className="text-slate-500 text-sm">Review officer-approved loans and manage committee voting.</p>
                    </div>

                    {/* ✅ VOTING MODE INDICATOR */}
                    <div className={`px-4 py-2 rounded-xl border flex items-center gap-3 shadow-sm ${
                        settings?.LOAN_VOTING_METHOD === 'MANUAL'
                        ? 'bg-purple-50 border-purple-100 text-purple-700'
                        : 'bg-blue-50 border-blue-100 text-blue-700'
                    }`}>
                        <Gavel size={20} />
                        <div className="text-right">
                            <p className="text-[10px] uppercase font-bold opacity-60">Voting System</p>
                            <p className="font-bold text-sm leading-none">
                                {settings?.LOAN_VOTING_METHOD === 'MANUAL' ? 'Manual Decision' : 'Automatic (50%+1)'}
                            </p>
                        </div>
                    </div>
                </div>

                {/* STATS */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 flex items-center gap-4">
                        <div className="p-3 bg-orange-100 text-orange-600 rounded-xl"><FileText size={24}/></div>
                        <div>
                            <p className="text-xs font-bold text-slate-400 uppercase">Pending Tabling</p>
                            <h3 className="text-2xl font-bold text-slate-800">
                                {loans.filter(l => l.status === 'SECRETARY_TABLED').length}
                            </h3>
                        </div>
                    </div>
                    <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 flex items-center gap-4">
                        <div className="p-3 bg-pink-100 text-pink-600 rounded-xl"><Users size={24}/></div>
                        <div>
                            <p className="text-xs font-bold text-slate-400 uppercase">Voting Active</p>
                            <h3 className="text-2xl font-bold text-slate-800">
                                {loans.filter(l => l.status === 'VOTING_OPEN').length}
                            </h3>
                        </div>
                    </div>
                </div>

                {/* AGENDA LIST */}
                <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
                    <div className="p-6 border-b border-slate-100 bg-slate-50/50 flex justify-between items-center">
                        <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                            <CheckSquare size={20} className="text-slate-400"/> Agenda Items
                        </h2>
                    </div>

                    <table className="w-full text-sm text-left">
                        <thead className="bg-slate-50 text-slate-500 font-bold border-b border-slate-100 uppercase text-xs">
                            <tr>
                                <th className="p-4">Ref</th>
                                <th className="p-4">Applicant</th>
                                <th className="p-4">Amount</th>
                                <th className="p-4">Status</th>
                                <th className="p-4 text-center">Votes (Y/N)</th>
                                <th className="p-4 text-center">Action</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {loading ? (
                                <tr><td colSpan="6" className="p-12 flex justify-center"><BrandedSpinner size="medium"/></td></tr>
                            ) : loans.length === 0 ? (
                                <tr><td colSpan="6" className="p-8 text-center text-slate-400 italic">No agenda items found.</td></tr>
                            ) : loans.map(loan => (
                                <tr key={loan.id} className="hover:bg-slate-50 transition">
                                    <td className="p-4 font-mono text-slate-500 text-xs">{loan.loanNumber}</td>
                                    <td className="p-4 font-bold text-slate-700">{loan.memberName}</td>
                                    <td className="p-4 font-mono text-slate-600">KES {Number(loan.principalAmount).toLocaleString()}</td>

                                    <td className="p-4">
                                        {loan.status === 'SECRETARY_TABLED' ? (
                                            <span className="bg-orange-50 text-orange-600 px-2 py-1 rounded text-[10px] font-bold uppercase border border-orange-100">Awaiting Tabling</span>
                                        ) : (
                                            <span className="bg-pink-50 text-pink-600 px-2 py-1 rounded text-[10px] font-bold uppercase border border-pink-100 animate-pulse">Voting Open</span>
                                        )}
                                    </td>

                                    <td className="p-4 text-center font-mono font-bold text-xs">
                                        <span className="text-emerald-600">{loan.votesYes || 0}</span>
                                        <span className="text-slate-300 mx-1">/</span>
                                        <span className="text-red-500">{loan.votesNo || 0}</span>
                                    </td>

                                    <td className="p-4 flex justify-center">
                                        {loan.status === 'SECRETARY_TABLED' && (
                                            <button
                                                onClick={() => setSelectedLoan(loan)}
                                                className="flex items-center gap-2 px-4 py-2 bg-slate-900 text-white rounded-lg text-xs font-bold hover:bg-slate-800 transition"
                                            >
                                                <Calendar size={14}/> Table Item
                                            </button>
                                        )}
                                        {/* ✅ NEW: Finalize Button */}
                                        {loan.status === 'VOTING_OPEN' && (
                                            <button
                                                onClick={() => setFinalizeLoan(loan)}
                                                className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg text-xs font-bold hover:bg-indigo-700 transition shadow-lg shadow-indigo-600/20"
                                            >
                                                <Gavel size={14}/> Finalize Vote
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </main>

            {/* MODAL 1: TABLING */}
            {selectedLoan && (
                <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-in fade-in">
                    <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6 space-y-6">
                        <h3 className="text-lg font-bold text-slate-800 border-b pb-2">Table Loan for Meeting</h3>
                        <p className="text-sm text-slate-600">
                            Table <span className="font-bold">{selectedLoan.loanNumber}</span> for committee voting.
                        </p>
                        <div>
                            <label className="block text-xs font-bold text-slate-500 uppercase mb-1">Meeting Date</label>
                            <input type="date" className="w-full p-3 border rounded-xl font-bold" value={meetingDate} onChange={(e) => setMeetingDate(e.target.value)} />
                        </div>
                        <div className="flex gap-3">
                            <button onClick={() => setSelectedLoan(null)} className="flex-1 py-3 bg-slate-100 rounded-xl font-bold">Cancel</button>
                            <button onClick={handleTableLoan} className="flex-1 py-3 bg-indigo-600 text-white rounded-xl font-bold">Confirm</button>
                        </div>
                    </div>
                </div>
            )}

            {/* ✅ MODAL 2: FINALIZE VOTE (ADAPTIVE) */}
            {finalizeLoan && (
                <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-in fade-in">
                    <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6 space-y-6">
                        <div className="flex justify-between items-center border-b pb-2">
                            <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                                <Gavel className="text-indigo-600"/> Finalize Voting
                            </h3>
                            <span className="text-[10px] uppercase font-bold px-2 py-1 bg-slate-100 rounded text-slate-500">
                                Mode: {settings?.LOAN_VOTING_METHOD}
                            </span>
                        </div>

                        {/* --- MANUAL MODE UI --- */}
                        {settings?.LOAN_VOTING_METHOD === 'MANUAL' ? (
                            <div className="space-y-4">
                                <div className="p-4 bg-purple-50 rounded-xl border border-purple-100 text-sm text-purple-800">
                                    <p className="font-bold mb-1">⚠️ Secretary's Discretion</p>
                                    <p>The system is in <b>Manual Mode</b>. Regardless of the vote count ({finalizeLoan.votesYes} vs {finalizeLoan.votesNo}), you must make the final decision.</p>
                                </div>

                                <div className="grid grid-cols-2 gap-4">
                                    <button
                                        onClick={() => setManualDecision(true)}
                                        className={`p-4 rounded-xl border-2 flex flex-col items-center gap-2 transition ${manualDecision === true ? 'border-emerald-500 bg-emerald-50 text-emerald-700' : 'border-slate-100 hover:border-emerald-200'}`}
                                    >
                                        <CheckCircle size={24} />
                                        <span className="font-bold text-sm">Approve</span>
                                    </button>
                                    <button
                                        onClick={() => setManualDecision(false)}
                                        className={`p-4 rounded-xl border-2 flex flex-col items-center gap-2 transition ${manualDecision === false ? 'border-red-500 bg-red-50 text-red-700' : 'border-slate-100 hover:border-red-200'}`}
                                    >
                                        <XCircle size={24} />
                                        <span className="font-bold text-sm">Reject</span>
                                    </button>
                                </div>

                                <textarea
                                    className="w-full p-3 border border-slate-200 rounded-xl text-sm outline-none focus:ring-2 focus:ring-purple-500"
                                    rows="3"
                                    placeholder="Enter decision comments or minutes..."
                                    value={secretaryComments}
                                    onChange={e => setSecretaryComments(e.target.value)}
                                ></textarea>
                            </div>
                        ) : (
                            /* --- AUTOMATIC MODE UI --- */
                            <div className="space-y-4">
                                <div className="p-4 bg-blue-50 rounded-xl border border-blue-100 text-sm text-blue-800">
                                    <p className="font-bold mb-1">🤖 Automatic Tally</p>
                                    <p>The system will calculate the result based on the <b>50% + 1</b> rule.</p>
                                </div>

                                <div className="flex justify-center gap-8 py-4">
                                    <div className="text-center">
                                        <p className="text-3xl font-bold text-emerald-600">{finalizeLoan.votesYes}</p>
                                        <p className="text-xs font-bold uppercase text-slate-400">Votes Yes</p>
                                    </div>
                                    <div className="text-center">
                                        <p className="text-3xl font-bold text-red-500">{finalizeLoan.votesNo}</p>
                                        <p className="text-xs font-bold uppercase text-slate-400">Votes No</p>
                                    </div>
                                </div>
                                <p className="text-center text-xs text-slate-400 italic">Click Confirm to apply the mathematical result.</p>
                            </div>
                        )}

                        <div className="flex gap-3 pt-2">
                            <button
                                onClick={() => { setFinalizeLoan(null); setManualDecision(null); }}
                                className="flex-1 py-3 bg-slate-100 text-slate-600 font-bold rounded-xl hover:bg-slate-200 transition"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={handleFinalizeVote}
                                className="flex-1 py-3 bg-indigo-600 text-white font-bold rounded-xl hover:bg-indigo-700 transition shadow-lg shadow-indigo-600/20"
                            >
                                Confirm Decision
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}