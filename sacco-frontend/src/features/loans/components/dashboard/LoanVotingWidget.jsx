import React from 'react';
import { Gavel, ThumbsUp, ThumbsDown } from 'lucide-react';

export default function LoanVotingWidget({ activeVotes, onVote }) {
    if (!activeVotes || activeVotes.length === 0) return null;

    return (
        <div className="bg-gradient-to-r from-purple-50 to-indigo-50 border border-purple-100 rounded-3xl p-6 shadow-sm">
            <div className="flex items-center gap-3 mb-4">
                <div className="p-2 bg-purple-100 text-purple-600 rounded-xl"><Gavel size={24}/></div>
                <div>
                    <h2 className="text-lg font-bold text-slate-800">Active Committee Votes</h2>
                    <p className="text-slate-500 text-sm">Cast your vote on pending loan applications.</p>
                </div>
            </div>

            <div className="grid gap-4 md:grid-cols-2">
                {activeVotes.map(loan => (
                    <div key={loan.id} className="bg-white p-5 rounded-2xl shadow-sm border border-purple-100 flex flex-col justify-between hover:shadow-md transition">
                        <div className="mb-4">
                            <div className="flex justify-between items-start">
                                <h3 className="font-bold text-slate-800">{loan.memberName}</h3>
                                <span className="text-[10px] font-mono bg-slate-100 text-slate-500 px-2 py-1 rounded">{loan.loanNumber}</span>
                            </div>
                            <p className="text-2xl font-black text-slate-700 mt-2">KES {Number(loan.principalAmount).toLocaleString()}</p>
                            <div className="flex gap-4 mt-2 text-xs text-slate-500">
                                <span>Duration: {loan.duration} {loan.durationUnit}</span>
                            </div>
                            <div className="flex justify-between text-sm pt-2 border-t border-slate-50 mt-2">
                                <span className="text-slate-500">Member Savings</span>
                                <span className="font-medium text-emerald-600">KES {Number(loan.memberSavings || 0).toLocaleString()}</span>
                            </div>
                        </div>

                        <div className="flex gap-3 mt-auto pt-4 border-t border-slate-50">
                            <button onClick={() => onVote(loan.id, false)}
                                className="flex-1 py-2.5 border border-red-100 bg-red-50 text-red-600 rounded-xl font-bold text-sm hover:bg-red-100 flex items-center justify-center gap-2 transition">
                                <ThumbsDown size={16}/> Decline
                            </button>
                            <button onClick={() => onVote(loan.id, true)}
                                className="flex-1 py-2.5 bg-emerald-600 text-white rounded-xl font-bold text-sm hover:bg-emerald-700 flex items-center justify-center gap-2 shadow-sm transition">
                                <ThumbsUp size={16}/> Approve
                            </button>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}