import React, { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { CreditCard, PiggyBank, HandCoins, ThumbsUp, ThumbsDown, Megaphone, Activity, FileText } from 'lucide-react';
import DashboardHeader from '../components/DashboardHeader';
import MemberOverview from '../features/member/components/MemberOverview';
import MemberSavings from '../features/member/components/MemberSavings';
import MemberLoans from '../features/member/components/MemberLoans';
import MemberActivities from '../features/member/components/MemberActivities';
import MemberStatements from '../features/member/components/MemberStatements';
import LoanEntryWorkflow from '../features/loans/components/LoanEntryWorkflow'; // ✅ Imported your new workflow
import MemberProfile from '../features/member/components/MemberProfile'; 
import api from '../api';

export default function MemberDashboard() {
    const [user, setUser] = useState(null);
    const [searchParams] = useSearchParams();
    const [votingAgenda, setVotingAgenda] = useState([]);
    
    const activeTab = searchParams.get('tab') || 'overview';

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
        fetchUserProfile();
        fetchVotingAgenda(); 
    }, []);

    const fetchUserProfile = async () => {
        try {
            const res = await api.get('/api/members/me');
            if (res.data.success) {
                setUser(res.data.data);
                localStorage.setItem('sacco_user', JSON.stringify(res.data.data));
            }
        } catch (error) {
            console.error("Failed to refresh user profile", error);
        }
    };

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
            fetchVotingAgenda();
        } catch (error) {
            alert(error.response?.data?.message || "Voting failed");
        }
    };

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800 pb-12">
            <DashboardHeader user={user} title="Member Portal" />

            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8 space-y-8 animate-in fade-in">

                {/* DEMOCRATIC VOTING SECTION */}
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

                {/* Dashboard Tabs */}
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3 pb-2 border-b border-slate-200">
                    <Link to="?tab=overview" className={`w-full p-3 rounded-xl flex items-center gap-3 transition text-sm font-bold border hover:border-emerald-200 group text-left ${activeTab === 'overview' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-slate-50 hover:bg-emerald-50 hover:text-emerald-700 border-slate-100'}`}>
                        <div className={`p-1.5 rounded-lg shadow-sm group-hover:shadow ${activeTab === 'overview' ? 'bg-emerald-100 text-emerald-600' : 'bg-white text-emerald-600'}`}><CreditCard size={16}/></div>
                        <span className="block truncate">Overview</span>
                    </Link>
                    
                    <Link to="?tab=savings" className={`w-full p-3 rounded-xl flex items-center gap-3 transition text-sm font-bold border hover:border-blue-200 group text-left ${activeTab === 'savings' ? 'bg-blue-50 text-blue-700 border-blue-200' : 'bg-slate-50 hover:bg-blue-50 hover:text-blue-700 border-slate-100'}`}>
                        <div className={`p-1.5 rounded-lg shadow-sm group-hover:shadow ${activeTab === 'savings' ? 'bg-blue-100 text-blue-600' : 'bg-white text-blue-600'}`}><PiggyBank size={16}/></div>
                        <span className="block truncate">Savings</span>
                    </Link>
                    
                    <Link to="?tab=loans" className={`w-full p-3 rounded-xl flex items-center gap-3 transition text-sm font-bold border hover:border-yellow-200 group text-left ${activeTab === 'loans' ? 'bg-yellow-50 text-yellow-700 border-yellow-200' : 'bg-slate-50 hover:bg-yellow-50 hover:text-yellow-700 border-slate-100'}`}>
                        <div className={`p-1.5 rounded-lg shadow-sm group-hover:shadow ${activeTab === 'loans' ? 'bg-yellow-100 text-yellow-600' : 'bg-white text-yellow-600'}`}><HandCoins size={16}/></div>
                        <span className="block truncate">Loans</span>
                    </Link>
                    
                    <Link to="?tab=statements" className={`w-full p-3 rounded-xl flex items-center gap-3 transition text-sm font-bold border hover:border-purple-200 group text-left ${activeTab === 'statements' ? 'bg-purple-50 text-purple-700 border-purple-200' : 'bg-slate-50 hover:bg-purple-50 hover:text-purple-700 border-slate-100'}`}>
                        <div className={`p-1.5 rounded-lg shadow-sm group-hover:shadow ${activeTab === 'statements' ? 'bg-purple-100 text-purple-600' : 'bg-white text-purple-600'}`}><FileText size={16}/></div>
                        <span className="block truncate">Statements</span>
                    </Link>
                    
                    <Link to="?tab=activities" className={`w-full p-3 rounded-xl flex items-center gap-3 transition text-sm font-bold border hover:border-indigo-200 group text-left ${activeTab === 'activities' ? 'bg-indigo-50 text-indigo-700 border-indigo-200' : 'bg-slate-50 hover:bg-indigo-50 hover:text-indigo-700 border-slate-100'}`}>
                        <div className={`p-1.5 rounded-lg shadow-sm group-hover:shadow ${activeTab === 'activities' ? 'bg-indigo-100 text-indigo-600' : 'bg-white text-indigo-600'}`}><Activity size={16}/></div>
                        <span className="block truncate">Activities</span>
                    </Link>
                </div>

                <div className="min-h-[400px] mt-6">
                    {activeTab === 'overview' && <MemberOverview user={user} />}
                    {activeTab === 'savings' && <MemberSavings />}
                    
                    {/* ✅ LOANS TAB: Combined Entry Workflow and Loan History */}
                    {activeTab === 'loans' && (
                        <div className="space-y-8">
                            <LoanEntryWorkflow />
                            <div className="border-t border-slate-200 pt-8">
                                <h3 className="text-lg font-bold mb-4 text-slate-700">My Loan History</h3>
                                <MemberLoans />
                            </div>
                        </div>
                    )}
                    
                    {activeTab === 'statements' && <MemberStatements user={user} />}
                    {activeTab === 'activities' && <MemberActivities />}
                    {activeTab === 'profile' && <MemberProfile />} 
                </div>
            </main>
        </div>
    );
}