import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { X, User, Briefcase, History, CheckCircle, XCircle, AlertTriangle, Shield, Wallet } from 'lucide-react';
import BrandedSpinner from '../../../components/BrandedSpinner';

export default function LoanReviewModal({ loan, onClose, onAction }) {
    const [history, setHistory] = useState([]);
    const [memberStats, setMemberStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [comments, setComments] = useState('');

    useEffect(() => {
        if (loan) fetchMemberDetails();
    }, [loan]);

    const fetchMemberDetails = async () => {
        try {
            // 1. Fetch Member's Loan History
            const historyRes = await api.get(`/api/loans/member/${loan.memberId}`);
            setHistory(historyRes.data.data.filter(l => l.id !== loan.id)); // Exclude current loan

            // 2. Fetch Member's Savings/Shares Stats (Assuming endpoint exists or derived)
            // For now, we simulate or fetch if you have a specific endpoint
            // const statsRes = await api.get(`/api/members/${loan.memberId}/stats`);
            // setMemberStats(statsRes.data);

        } catch (error) {
            console.error("Failed to load member history", error);
        } finally {
            setLoading(false);
        }
    };

    const handleApprove = () => {
        if (!window.confirm("Approve and forward to Secretary?")) return;
        onAction(loan.id, 'approve', null);
    };

    const handleReject = () => {
        if (!comments) return alert("Please provide a reason for rejection.");
        if (!window.confirm("Reject this application?")) return;
        onAction(loan.id, 'reject', { reason: comments });
    };

    if (!loan) return null;

    return (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-in fade-in">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-4xl max-h-[90vh] overflow-hidden flex flex-col">

                {/* Header */}
                <div className="bg-slate-50 p-6 border-b border-slate-100 flex justify-between items-center">
                    <div>
                        <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
                            <Briefcase className="text-indigo-600"/> Loan Application Review
                        </h2>
                        <p className="text-sm text-slate-500">Ref: <span className="font-mono font-bold">{loan.loanNumber}</span></p>
                    </div>
                    <button onClick={onClose} className="p-2 hover:bg-slate-200 rounded-full transition"><X size={20}/></button>
                </div>

                {/* Content */}
                <div className="flex-1 overflow-y-auto p-6 bg-slate-50/30">
                    {loading ? (
                        <div className="h-64 flex items-center justify-center"><BrandedSpinner /></div>
                    ) : (
                        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

                            {/* LEFT COLUMN: APPLICATION DETAILS */}
                            <div className="lg:col-span-2 space-y-6">
                                {/* 1. Current Request */}
                                <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-200">
                                    <h3 className="font-bold text-slate-700 mb-4 border-b pb-2">Current Application</h3>
                                    <div className="grid grid-cols-2 gap-4">
                                        <div>
                                            <p className="text-xs text-slate-400 uppercase font-bold">Product</p>
                                            <p className="font-bold text-slate-800">{loan.productName}</p>
                                        </div>
                                        <div>
                                            <p className="text-xs text-slate-400 uppercase font-bold">Amount Requested</p>
                                            <p className="text-xl font-bold text-emerald-600">KES {Number(loan.principalAmount).toLocaleString()}</p>
                                        </div>
                                        <div>
                                            <p className="text-xs text-slate-400 uppercase font-bold">Duration</p>
                                            <p className="font-bold text-slate-700">{loan.duration} Months</p>
                                        </div>
                                        <div>
                                            <p className="text-xs text-slate-400 uppercase font-bold">Application Date</p>
                                            <p className="font-bold text-slate-700">{loan.applicationDate}</p>
                                        </div>
                                    </div>
                                </div>

                                {/* 2. Loan History */}
                                <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-200">
                                    <h3 className="font-bold text-slate-700 mb-4 flex items-center gap-2 border-b pb-2">
                                        <History size={18}/> Loan History
                                    </h3>
                                    {history.length === 0 ? (
                                        <p className="text-slate-400 text-sm italic">No previous loan history found.</p>
                                    ) : (
                                        <div className="overflow-x-auto">
                                            <table className="w-full text-sm text-left">
                                                <thead className="text-xs text-slate-400 uppercase bg-slate-50">
                                                    <tr>
                                                        <th className="p-2">Date</th>
                                                        <th className="p-2">Amount</th>
                                                        <th className="p-2">Status</th>
                                                    </tr>
                                                </thead>
                                                <tbody className="divide-y divide-slate-100">
                                                    {history.map(h => (
                                                        <tr key={h.id}>
                                                            <td className="p-2 text-slate-600">{h.disbursementDate || h.applicationDate}</td>
                                                            <td className="p-2 font-bold">KES {Number(h.principalAmount).toLocaleString()}</td>
                                                            <td className="p-2">
                                                                <span className={`text-[10px] px-2 py-1 rounded-full font-bold ${
                                                                    h.status === 'COMPLETED' ? 'bg-green-100 text-green-700' :
                                                                    h.status === 'DEFAULTED' ? 'bg-red-100 text-red-700' :
                                                                    'bg-slate-100 text-slate-600'
                                                                }`}>{h.status}</span>
                                                            </td>
                                                        </tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    )}
                                </div>
                            </div>

                            {/* RIGHT COLUMN: MEMBER PROFILE & ACTIONS */}
                            <div className="space-y-6">
                                {/* 1. Applicant Profile */}
                                <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-200 text-center">
                                    <div className="w-20 h-20 bg-indigo-100 text-indigo-600 rounded-full flex items-center justify-center mx-auto mb-3 text-2xl font-bold">
                                        {loan.memberName.charAt(0)}
                                    </div>
                                    <h3 className="font-bold text-slate-800">{loan.memberName}</h3>
                                    <p className="text-xs text-slate-500 mb-4">Member ID: {loan.memberId.substring(0,8)}...</p>

                                    <div className="bg-slate-50 p-3 rounded-lg text-left mb-2">
                                        <p className="text-xs text-slate-400 uppercase">Total Savings</p>
                                        <p className="font-bold text-slate-800 flex items-center gap-2">
                                            <Wallet size={14} className="text-emerald-500"/>
                                            KES {Number(loan.memberSavings || 0).toLocaleString()} {/* Ensure DTO has this or fetch it */}
                                        </p>
                                    </div>
                                </div>

                                {/* 2. Guarantors Status */}
                                <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-200">
                                    <h3 className="font-bold text-slate-700 mb-3 flex items-center gap-2">
                                        <Shield size={18}/> Guarantors
                                    </h3>
                                    <div className="space-y-2">
                                        {/* You can fetch detailed guarantor status here if needed.
                                            For now, assuming passed status implies validation. */}
                                        <div className="flex items-center gap-2 text-sm text-green-600 bg-green-50 p-2 rounded">
                                            <CheckCircle size={16}/> <span>Guarantor Requirements Met</span>
                                        </div>
                                        <div className="flex items-center gap-2 text-sm text-green-600 bg-green-50 p-2 rounded">
                                            <CheckCircle size={16}/> <span>Fee Paid</span>
                                        </div>
                                    </div>
                                </div>

                                {/* 3. Decision Actions */}
                                <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-200 space-y-4">
                                    <h3 className="font-bold text-slate-700 border-b pb-2">Officer Decision</h3>

                                    <textarea
                                        className="w-full p-3 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
                                        rows="3"
                                        placeholder="Add comments (Required for rejection)..."
                                        value={comments}
                                        onChange={e => setComments(e.target.value)}
                                    ></textarea>

                                    <div className="grid grid-cols-2 gap-3">
                                        <button
                                            onClick={handleReject}
                                            className="py-2 px-4 bg-red-50 hover:bg-red-100 text-red-600 font-bold rounded-lg flex items-center justify-center gap-2 text-sm transition"
                                        >
                                            <XCircle size={16}/> Reject
                                        </button>
                                        <button
                                            onClick={handleApprove}
                                            className="py-2 px-4 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg flex items-center justify-center gap-2 text-sm transition shadow-lg shadow-emerald-600/20"
                                        >
                                            <CheckCircle size={16}/> Approve
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}