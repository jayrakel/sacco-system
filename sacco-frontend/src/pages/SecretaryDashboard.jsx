import React, { useState, useEffect } from 'react';
import api from '../api';
import { useSettings } from '../context/SettingsContext';
import { FileText, Calendar, CheckSquare, Users, Clock, Gavel, CheckCircle, XCircle } from 'lucide-react';
import DashboardHeader from '../components/DashboardHeader';
import BrandedSpinner from '../components/BrandedSpinner';
import ShareCapitalCard from '../components/ShareCapitalCard';

export default function SecretaryDashboard() {
    const { settings } = useSettings();
    const [user, setUser] = useState(null);
    const [loans, setLoans] = useState([]);
    const [loading, setLoading] = useState(true);

    const [selectedLoan, setSelectedLoan] = useState(null);
    const [meetingDate, setMeetingDate] = useState(new Date().toISOString().split('T')[0]);

    const [finalizeLoan, setFinalizeLoan] = useState(null);
    const [manualDecision, setManualDecision] = useState(null);
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
                    ['SECRETARY_TABLED', 'VOTING_OPEN'].includes(l.status)
                );
                setLoans(readyLoans);
            }
        } catch (e) {
            console.error(e);
        } finally {
            setLoading(false);
        }
    };

    const handleTableLoan = async () => {
        if (!selectedLoan) return;
        try {
            await api.post(`/api/loans/${selectedLoan.id}/table?meetingDate=${meetingDate}`);
            await api.post(`/api/loans/${selectedLoan.id}/start-voting`);
            alert("Loan added to agenda and voting opened.");
            setSelectedLoan(null);
            fetchTabledLoans();
        } catch (error) {
            alert(error.response?.data?.message || "Failed to table loan");
        }
    };

    const handleFinalizeVote = async () => {
        if (!finalizeLoan) return;
        const isManual = settings?.LOAN_VOTING_METHOD === 'MANUAL';

        if (isManual && manualDecision === null) {
            alert("Please select a decision (Approve or Reject).");
            return;
        }

        try {
            await api.post(`/api/loans/${finalizeLoan.id}/finalize`, null, {
                params: {
                    approved: isManual ? manualDecision : null,
                    comments: secretaryComments || 'Meeting minutes recorded.'
                }
            });
            alert("Decision finalized and recorded.");
            setFinalizeLoan(null);
            setManualDecision(null);
            setSecretaryComments('');
            fetchTabledLoans();
        } catch (error) {
            alert(error.response?.data?.message || "Failed to finalize");
        }
    };

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800 pb-12">
            <DashboardHeader user={user} title="Secretariat Portal" />
            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8 space-y-8 animate-in fade-in">
                <div className="flex justify-between items-end">
                    <div>
                        <h1 className="text-2xl font-bold text-slate-800">Meeting Management</h1>
                        <p className="text-slate-500 text-sm">Review officer-approved loans and manage committee voting.</p>
                    </div>
                    <div className={`px-4 py-2 rounded-xl border flex items-center gap-3 shadow-sm ${settings?.LOAN_VOTING_METHOD === 'MANUAL' ? 'bg-purple-50 border-purple-100 text-purple-700' : 'bg-blue-50 border-blue-100 text-blue-700'}`}>
                        <Gavel size={20} />
                        <div className="text-right">
                            <p className="text-[10px] uppercase font-bold opacity-60">Voting Mode</p>
                            <p className="font-bold text-sm leading-none">{settings?.LOAN_VOTING_METHOD === 'MANUAL' ? 'Manual Discretion' : 'Automatic Tally'}</p>
                        </div>
                    </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <StatCard icon={<FileText size={24}/>} color="orange" label="Awaiting Agenda" value={loans.filter(l => l.status === 'SECRETARY_TABLED').length} />
                    <StatCard icon={<Users size={24}/>} color="pink" label="Voting Active" value={loans.filter(l => l.status === 'VOTING_OPEN').length} />
                    <ShareCapitalCard />
                </div>

                <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
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
                            ) : loans.map(loan => (
                                <tr key={loan.id} className="hover:bg-slate-50">
                                    <td className="p-4 font-mono text-xs">{loan.loanNumber}</td>
                                    <td className="p-4 font-bold">{loan.memberName}</td>
                                    <td className="p-4">KES {Number(loan.principalAmount).toLocaleString()}</td>
                                    <td className="p-4">
                                        <span className={`px-2 py-1 rounded text-[10px] font-bold uppercase ${loan.status === 'SECRETARY_TABLED' ? 'bg-orange-50 text-orange-600' : 'bg-pink-50 text-pink-600 animate-pulse'}`}>
                                            {loan.status.replace('_', ' ')}
                                        </span>
                                    </td>
                                    <td className="p-4 text-center font-bold">
                                        <span className="text-emerald-600">{loan.votesYes || 0}</span> / <span className="text-red-500">{loan.votesNo || 0}</span>
                                    </td>
                                    <td className="p-4 flex justify-center">
                                        {loan.status === 'SECRETARY_TABLED' ? (
                                            <button onClick={() => setSelectedLoan(loan)} className="flex items-center gap-2 px-4 py-2 bg-slate-900 text-white rounded-lg text-xs font-bold"><Calendar size={14}/> Table</button>
                                        ) : (
                                            <button onClick={() => setFinalizeLoan(loan)} className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg text-xs font-bold"><Gavel size={14}/> Finalize</button>
                                        )}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </main>

            {/* Finalize Modal */}
            {finalizeLoan && (
                <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
                    <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6 space-y-6">
                        <h3 className="text-lg font-bold border-b pb-2">Finalize Voting Result</h3>
                        {settings?.LOAN_VOTING_METHOD === 'MANUAL' ? (
                            <div className="space-y-4">
                                <div className="grid grid-cols-2 gap-4">
                                    <button onClick={() => setManualDecision(true)} className={`p-4 rounded-xl border-2 flex flex-col items-center gap-2 ${manualDecision === true ? 'border-emerald-500 bg-emerald-50 text-emerald-700' : 'border-slate-100'}`}><CheckCircle size={24} /><span>Approve</span></button>
                                    <button onClick={() => setManualDecision(false)} className={`p-4 rounded-xl border-2 flex flex-col items-center gap-2 ${manualDecision === false ? 'border-red-500 bg-red-50 text-red-700' : 'border-slate-100'}`}><XCircle size={24} /><span>Reject</span></button>
                                </div>
                                <textarea className="w-full p-3 border rounded-xl text-sm" rows="3" placeholder="Decision comments..." value={secretaryComments} onChange={e => setSecretaryComments(e.target.value)} />
                            </div>
                        ) : (
                            <div className="text-center py-4">
                                <p className="text-sm text-slate-500">System will use automatic 50%+1 tally.</p>
                                <div className="flex justify-center gap-8 mt-4"><p className="text-2xl font-bold text-emerald-600">{finalizeLoan.votesYes} Y</p><p className="text-2xl font-bold text-red-500">{finalizeLoan.votesNo} N</p></div>
                            </div>
                        )}
                        <div className="flex gap-3"><button onClick={() => setFinalizeLoan(null)} className="flex-1 py-3 bg-slate-100 font-bold rounded-xl">Cancel</button><button onClick={handleFinalizeVote} className="flex-1 py-3 bg-indigo-600 text-white font-bold rounded-xl">Confirm</button></div>
                    </div>
                </div>
            )}
        </div>
    );
}

function StatCard({ icon, color, label, value }) {
    const colors = { orange: "bg-orange-100 text-orange-600", pink: "bg-pink-100 text-pink-600" };
    return (
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-200 flex items-center gap-4">
            <div className={`p-3 rounded-xl ${colors[color]}`}>{icon}</div>
            <div><p className="text-xs font-bold text-slate-400 uppercase">{label}</p><h3 className="text-2xl font-bold text-slate-800">{value}</h3></div>
        </div>
    );
}