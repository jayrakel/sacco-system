import React, { useState, useEffect } from 'react';
import { CreditCard, Wallet, PiggyBank, HandCoins, ThumbsUp, ThumbsDown, Megaphone } from 'lucide-react';
import DashboardHeader from '../components/DashboardHeader';
import MemberOverview from '../features/member/components/MemberOverview';
import MemberSavings from '../features/member/components/MemberSavings';
import MemberLoans from '../features/member/components/MemberLoans';
import api from '../api';

export default function MemberDashboard() {
    const [user, setUser] = useState(null);
    const [activeTab, setActiveTab] = useState('overview');
    const [votingAgenda, setVotingAgenda] = useState([]); // ✅ Store Active Votes

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));

        fetchVotingAgenda(); // ✅ Check for active votes on load
    }, []);

    const fetchVotingAgenda = async () => {
        try {
            const res = await api.get('/api/loans/agenda/active');
            if (res.data.success) {
                setVotingAgenda(res.data.data);
            }
        } catch (error) {
            console.error("Failed to fetch voting agenda", error);
        }
    };

    const handleVote = async (loanId, voteYes) => {
        try {
            await api.post(`/api/loans/${loanId}/vote`, null, {
                params: { voteYes }
            });
            alert(`Vote cast successfully!`);
            // Refresh to remove the item or update state if needed
            fetchVotingAgenda();
        } catch (error) {
            alert(error.response?.data?.message || "Voting failed");
        }
    };

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800 pb-12">
            <DashboardHeader user={user} title="Member Portal" />

            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8 space-y-8 animate-in fade-in">

                {/* ✅ NEW: DEMOCRATIC VOTING SECTION */}
                {votingAgenda.length > 0 && (
                    <div className="bg-gradient-to-r from-indigo-600 to-violet-600 rounded-2xl shadow-xl p-6 text-white mb-8 border border-white/10">
                        <div className="flex items-center gap-3 mb-4 border-b border-white/20 pb-4">
                            <div className="p-2 bg-white/20 rounded-full animate-pulse">
                                <Megaphone className="text-white" size={24} />
                            </div>
                            <div>
                                <h2 className="text-xl font-bold">General Assembly in Session</h2>
                                <p className="text-indigo-100 text-sm">Please cast your vote on the following agenda items.</p>
                            </div>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                            {votingAgenda.map(loan => (
                                <div key={loan.id} className="bg-white/10 backdrop-blur-sm p-5 rounded-xl border border-white/20 flex flex-col justify-between hover:bg-white/15 transition">
                                    <div className="mb-4">
                                        <div className="flex justify-between items-start mb-2">
                                            <span className="bg-white/20 px-2 py-1 rounded text-xs font-mono">{loan.loanNumber}</span>
                                            <span className="font-bold text-emerald-300">KES {Number(loan.principalAmount).toLocaleString()}</span>
                                        </div>
                                        <h3 className="font-bold text-lg">{loan.memberName}</h3>
                                        <p className="text-sm text-indigo-200">{loan.productName}</p>
                                    </div>

                                    <div className="flex gap-3">
                                        <button
                                            onClick={() => handleVote(loan.id, true)}
                                            className="flex-1 bg-emerald-500 hover:bg-emerald-600 text-white py-2 rounded-lg font-bold flex items-center justify-center gap-2 transition shadow-lg shadow-emerald-900/20"
                                        >
                                            <ThumbsUp size={16}/> Yes
                                        </button>
                                        <button
                                            onClick={() => handleVote(loan.id, false)}
                                            className="flex-1 bg-rose-500 hover:bg-rose-600 text-white py-2 rounded-lg font-bold flex items-center justify-center gap-2 transition shadow-lg shadow-rose-900/20"
                                        >
                                            <ThumbsDown size={16}/> No
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {/* Standard Dashboard Tabs */}
                <div className="flex gap-4 overflow-x-auto pb-2 border-b border-slate-200">
                    <TabButton active={activeTab === 'overview'} onClick={() => setActiveTab('overview')} icon={<CreditCard size={18}/>} label="Overview" />
                    <TabButton active={activeTab === 'savings'} onClick={() => setActiveTab('savings')} icon={<PiggyBank size={18}/>} label="My Savings" />
                    <TabButton active={activeTab === 'loans'} onClick={() => setActiveTab('loans')} icon={<HandCoins size={18}/>} label="My Loans" />
                </div>

                <div className="min-h-[400px] mt-6">
                    {activeTab === 'overview' && <MemberOverview user={user} />}
                    {activeTab === 'savings' && <MemberSavings />}
                    {activeTab === 'loans' && <MemberLoans />}
                </div>
            </main>
        </div>
    );
}

// Helper for Tabs
function TabButton({ active, onClick, icon, label }) {
    return (
        <button
            onClick={onClick}
            className={`flex items-center gap-2 px-6 py-3 rounded-xl font-bold transition-all whitespace-nowrap
                ${active ? "bg-slate-900 text-white shadow-lg shadow-slate-900/20" : "bg-white text-slate-500 hover:bg-slate-50 border border-slate-200"}`}
        >
            {icon} {label}
        </button>
    );
}