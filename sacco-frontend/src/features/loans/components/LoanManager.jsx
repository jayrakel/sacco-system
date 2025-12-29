import React, { useState, useEffect } from 'react';
import { CheckCircle, XCircle, Clock, FileText, ChevronRight, Eye, Gavel, Calendar } from 'lucide-react';
import api from '../../../api';

export default function LoanManager({ currentUser }) {
    const [loans, setLoans] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedLoan, setSelectedLoan] = useState(null); // For details modal

    useEffect(() => {
        fetchLoans();
    }, []);

    const fetchLoans = async () => {
        try {
            const res = await api.get('/api/loans');
            if (res.data.success) setLoans(res.data.data);
        } catch (e) {
            console.error("Failed to load loans");
        } finally {
            setLoading(false);
        }
    };

    // --- ACTIONS MAPPED TO NEW BACKEND LOGIC ---

    const handleOfficerReview = async (id) => {
        if(!confirm("Start Review Process?")) return;
        try { await api.post(`/api/loans/${id}/review`); fetchLoans(); } catch(e) { alert(e.message); }
    };

    const handleOfficerApprove = async (id) => {
        if(!confirm("Approve and Table for Secretary?")) return;
        try { await api.post(`/api/loans/${id}/approve`); fetchLoans(); } catch(e) { alert(e.message); }
    };

    const handleTableLoan = async (id) => {
        const date = prompt("Enter Meeting Date (YYYY-MM-DD):", new Date().toISOString().split('T')[0]);
        if(!date) return;
        try { 
            await api.post(`/api/loans/${id}/table`, null, { params: { meetingDate: date } }); 
            alert("Loan Tabled!");
            fetchLoans(); 
        } catch(e) { alert("Failed to table loan"); }
    };

    const handleOpenVoting = async (id) => {
        if(!confirm("Open Voting for this loan?")) return;
        try { await api.post(`/api/loans/${id}/vote/open`); fetchLoans(); } catch(e) { alert(e.response?.data?.message || "Voting Error"); }
    };

    const handleFinalizeVote = async (id) => {
        const approved = confirm("Did the vote pass?");
        const comments = prompt("Enter meeting minutes/comments:");
        try {
            await api.post(`/api/loans/${id}/vote/close`, null, { params: { manualApproved: approved, comments } });
            alert("Vote Finalized!");
            fetchLoans();
        } catch(e) { alert("Error finalizing"); }
    };

    const handleSecretaryFinalApprove = async (id) => {
        if(!confirm("Give Final Approval for Disbursement?")) return;
        try { await api.post(`/api/loans/${id}/final-approve`); fetchLoans(); } catch(e) { alert("Error approving"); }
    };

    const handleDisburse = async (id) => {
        const ref = prompt("Enter Cheque/Ref Number:");
        if(!ref) return;
        try { await api.post(`/api/loans/${id}/disburse`, null, { params: { checkNumber: ref } }); fetchLoans(); } catch(e) { alert("Error disbursing"); }
    };

    // --- HELPER: RENDER ACTION BUTTONS BASED ON STATUS & ROLE ---
    const renderActions = (loan) => {
        const role = currentUser?.role || 'ADMIN'; // Default to admin for testing if null
        const status = loan.status;

        // 1. OFFICER ACTIONS
        if (status === 'SUBMITTED' && (role === 'LOAN_OFFICER' || role === 'ADMIN')) {
            return <button onClick={() => handleOfficerReview(loan.id)} className="btn-action bg-blue-100 text-blue-700">Start Review</button>;
        }
        if (status === 'LOAN_OFFICER_REVIEW' && (role === 'LOAN_OFFICER' || role === 'ADMIN')) {
            return <button onClick={() => handleOfficerApprove(loan.id)} className="btn-action bg-green-100 text-green-700">Approve & Table</button>;
        }

        // 2. SECRETARY ACTIONS (Tabling)
        if (status === 'SECRETARY_TABLED' && (role === 'SECRETARY' || role === 'ADMIN')) {
            return <button onClick={() => handleTableLoan(loan.id)} className="btn-action bg-amber-100 text-amber-700"><Calendar size={14}/> Table Loan</button>;
        }

        // 3. CHAIRPERSON ACTIONS (Voting)
        if (status === 'ON_AGENDA' && (role === 'CHAIRPERSON' || role === 'ADMIN')) {
            return <button onClick={() => handleOpenVoting(loan.id)} className="btn-action bg-purple-100 text-purple-700"><Gavel size={14}/> Open Voting</button>;
        }
        if (status === 'VOTING_OPEN' && (role === 'CHAIRPERSON' || role === 'ADMIN')) {
            return <button onClick={() => handleFinalizeVote(loan.id)} className="btn-action bg-rose-100 text-rose-700">Close & Finalize</button>;
        }

        // 4. SECRETARY FINAL APPROVAL
        if (status === 'SECRETARY_DECISION' && (role === 'SECRETARY' || role === 'ADMIN')) {
            return <button onClick={() => handleSecretaryFinalApprove(loan.id)} className="btn-action bg-emerald-100 text-emerald-700"><CheckCircle size={14}/> Final Approval</button>;
        }

        // 5. TREASURER ACTIONS (Disburse)
        if (status === 'TREASURER_DISBURSEMENT' && (role === 'TREASURER' || role === 'ADMIN')) {
            return <button onClick={() => handleDisburse(loan.id)} className="btn-action bg-slate-800 text-white">Disburse Funds</button>;
        }

        return <span className="text-xs text-slate-400 italic">No actions</span>;
    };

    if (loading) return <div className="p-10 text-center text-slate-400">Loading Loans...</div>;

    return (
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
            <div className="p-5 border-b border-slate-100 flex justify-between items-center">
                <h2 className="text-lg font-bold text-slate-800">Loan Applications</h2>
                <div className="text-xs font-bold bg-indigo-50 text-indigo-700 px-3 py-1 rounded-full">
                    Acting as: {currentUser?.role || 'Viewer'}
                </div>
            </div>
            
            <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                    <thead className="bg-slate-50 text-slate-500 uppercase font-bold text-xs">
                        <tr>
                            <th className="p-4">Loan #</th>
                            <th className="p-4">Member</th>
                            <th className="p-4 text-right">Amount</th>
                            <th className="p-4">Status</th>
                            <th className="p-4">Next Step</th>
                            <th className="p-4 text-center">Actions</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                        {loans.map(loan => (
                            <tr key={loan.id} className="hover:bg-slate-50 transition">
                                <td className="p-4 font-mono text-xs">{loan.loanNumber}</td>
                                <td className="p-4 font-bold text-slate-700">{loan.memberName}</td>
                                <td className="p-4 text-right font-bold">KES {loan.principalAmount.toLocaleString()}</td>
                                <td className="p-4">
                                    <StatusBadge status={loan.status} />
                                </td>
                                <td className="p-4 text-xs text-slate-500 italic">
                                    {getNextStepText(loan.status)}
                                </td>
                                <td className="p-4 text-center">
                                    {renderActions(loan)}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
            <style jsx>{`
                .btn-action {
                    @apply px-3 py-1.5 rounded-lg text-xs font-bold transition flex items-center justify-center gap-1 mx-auto hover:brightness-95;
                }
            `}</style>
        </div>
    );
}

const StatusBadge = ({ status }) => {
    const colors = {
        SUBMITTED: 'bg-blue-50 text-blue-700',
        LOAN_OFFICER_REVIEW: 'bg-indigo-50 text-indigo-700',
        SECRETARY_TABLED: 'bg-amber-50 text-amber-700',
        ON_AGENDA: 'bg-purple-50 text-purple-700',
        VOTING_OPEN: 'bg-rose-50 text-rose-700 animate-pulse',
        SECRETARY_DECISION: 'bg-teal-50 text-teal-700',
        TREASURER_DISBURSEMENT: 'bg-cyan-50 text-cyan-700',
        DISBURSED: 'bg-emerald-50 text-emerald-700',
        REJECTED: 'bg-red-50 text-red-700'
    };
    return <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase ${colors[status] || 'bg-gray-100'}`}>{status.replace(/_/g, ' ')}</span>;
};

const getNextStepText = (status) => {
    switch(status) {
        case 'SUBMITTED': return 'Waiting for Officer Review';
        case 'LOAN_OFFICER_REVIEW': return 'Officer decision pending';
        case 'SECRETARY_TABLED': return 'Secretary to set meeting date';
        case 'ON_AGENDA': return 'Chairperson to open voting';
        case 'VOTING_OPEN': return 'Members voting in progress';
        case 'SECRETARY_DECISION': return 'Secretary final ratification';
        case 'TREASURER_DISBURSEMENT': return 'Treasurer to release funds';
        default: return '-';
    }
};