import React, { useState, useEffect } from 'react';
import api from '../api';
import { FileText, Calendar, CheckSquare, Users, Clock } from 'lucide-react';
import DashboardHeader from '../components/DashboardHeader';
import BrandedSpinner from '../components/BrandedSpinner';

export default function SecretaryDashboard() {
    const [user, setUser] = useState(null);
    const [loans, setLoans] = useState([]);
    const [loading, setLoading] = useState(true);

    // "Tabling" Modal State
    const [selectedLoan, setSelectedLoan] = useState(null);
    const [meetingDate, setMeetingDate] = useState(new Date().toISOString().split('T')[0]);

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
                // Filter for loans ready for the Secretary
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

    const handleTableLoan = async () => {
        if (!selectedLoan) return;

        try {
            await api.post(`/api/loans/${selectedLoan.id}/table`, null, {
                params: { meetingDate }
            });
            alert(`Loan ${selectedLoan.loanNumber} added to meeting agenda.`);
            setSelectedLoan(null); // Close modal
            fetchTabledLoans(); // Refresh list
        } catch (error) {
            alert(error.response?.data?.message || "Failed to table loan");
        }
    };

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800 pb-12">
            <DashboardHeader user={user} title="Secretariat Portal" />

            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8 space-y-8 animate-in fade-in">

                {/* 1. Header Stats */}
                <div className="flex justify-between items-end">
                    <div>
                        <h1 className="text-2xl font-bold text-slate-800">Meeting & Minutes Management</h1>
                        <p className="text-slate-500 text-sm">Review officer-approved loans and table them for committee voting.</p>
                    </div>
                </div>

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
                            <p className="text-xs font-bold text-slate-400 uppercase">Voting In Progress</p>
                            <h3 className="text-2xl font-bold text-slate-800">
                                {loans.filter(l => l.status === 'VOTING_OPEN').length}
                            </h3>
                        </div>
                    </div>
                </div>

                {/* 2. Task List */}
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
                                <th className="p-4">Officer Approval</th>
                                <th className="p-4">Current Status</th>
                                <th className="p-4 text-center">Action</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {loading ? (
                                <tr><td colSpan="6" className="p-12 flex justify-center"><BrandedSpinner size="medium"/></td></tr>
                            ) : loans.length === 0 ? (
                                <tr><td colSpan="6" className="p-8 text-center text-slate-400 italic">No loans pending tabling.</td></tr>
                            ) : loans.map(loan => (
                                <tr key={loan.id} className="hover:bg-slate-50 transition">
                                    <td className="p-4 font-mono text-slate-500 text-xs">{loan.loanNumber}</td>
                                    <td className="p-4 font-bold text-slate-700">{loan.memberName}</td>
                                    <td className="p-4 font-mono text-slate-600">KES {Number(loan.principalAmount).toLocaleString()}</td>
                                    <td className="p-4 text-xs text-slate-500">{loan.approvalDate || "Recently"}</td>

                                    <td className="p-4">
                                        {loan.status === 'SECRETARY_TABLED' ? (
                                            <span className="bg-orange-50 text-orange-600 px-2 py-1 rounded text-[10px] font-bold uppercase border border-orange-100">Awaiting Tabling</span>
                                        ) : (
                                            <span className="bg-pink-50 text-pink-600 px-2 py-1 rounded text-[10px] font-bold uppercase border border-pink-100">Voting Open</span>
                                        )}
                                    </td>

                                    <td className="p-4 flex justify-center">
                                        {loan.status === 'SECRETARY_TABLED' && (
                                            <button
                                                onClick={() => setSelectedLoan(loan)}
                                                className="flex items-center gap-2 px-4 py-2 bg-slate-900 text-white rounded-lg text-xs font-bold hover:bg-slate-800 transition shadow-lg shadow-slate-900/20"
                                            >
                                                <Calendar size={14}/> Table to Minute
                                            </button>
                                        )}
                                        {loan.status === 'VOTING_OPEN' && (
                                            <span className="text-xs text-slate-400 italic flex items-center gap-1">
                                                <Clock size={14}/> Voting in progress...
                                            </span>
                                        )}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </main>

            {/* TABLING MODAL */}
            {selectedLoan && (
                <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-in fade-in">
                    <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6 space-y-6">
                        <h3 className="text-lg font-bold text-slate-800 border-b pb-2">Table Loan for Meeting</h3>

                        <div className="space-y-4">
                            <p className="text-sm text-slate-600">
                                You are about to table loan <span className="font-bold">{selectedLoan.loanNumber}</span> for
                                <span className="font-bold"> {selectedLoan.memberName}</span>.
                                This will open the floor for committee voting.
                            </p>

                            <div>
                                <label className="block text-xs font-bold text-slate-500 uppercase mb-1">Select Meeting Date</label>
                                <input
                                    type="date"
                                    className="w-full p-3 border border-slate-200 rounded-xl outline-none focus:ring-2 focus:ring-indigo-500 font-bold text-slate-700"
                                    value={meetingDate}
                                    onChange={(e) => setMeetingDate(e.target.value)}
                                />
                            </div>
                        </div>

                        <div className="flex gap-3 pt-2">
                            <button
                                onClick={() => setSelectedLoan(null)}
                                className="flex-1 py-3 bg-slate-100 text-slate-600 font-bold rounded-xl hover:bg-slate-200 transition"
                            >
                                Cancel
                                </button>
                            <button
                                onClick={handleTableLoan}
                                className="flex-1 py-3 bg-indigo-600 text-white font-bold rounded-xl hover:bg-indigo-700 transition shadow-lg shadow-indigo-600/20"
                            >
                                {/* ✅ CORRECTED TEXT */}
                                Confirm & Add to Agenda
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}