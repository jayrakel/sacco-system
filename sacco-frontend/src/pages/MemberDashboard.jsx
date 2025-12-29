import React, { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { CreditCard, PiggyBank, HandCoins, Activity, FileText, User as UserIcon } from 'lucide-react';
import DashboardHeader from '../components/DashboardHeader';
import MemberOverview from '../features/member/components/MemberOverview';
import MemberSavings from '../features/member/components/MemberSavings';
import MemberLoans from '../features/member/components/MemberLoans';
import MemberActivities from '../features/member/components/MemberActivities';
import MemberStatements from '../features/member/components/MemberStatements';
import MemberProfile from '../features/member/components/MemberProfile';
import api from '../api';

export default function MemberDashboard() {
    const [user, setUser] = useState(null);
    const [searchParams] = useSearchParams();
    const [pendingVotesCount, setPendingVotesCount] = useState(0); // ✅ Track Pending Votes

    const activeTab = searchParams.get('tab') || 'overview';

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
        fetchUserProfile();
        fetchPendingVotes(); // ✅ Check for notifications on load
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

    // ✅ NEW: Fetch active votes count to show the Red Dot
    // This endpoint excludes the user's own loans automatically via backend logic
    const fetchPendingVotes = async () => {
        try {
            const res = await api.get('/api/loans/voting/active');
            if (res.data.success) {
                setPendingVotesCount(res.data.data.length);
            }
        } catch (e) {
            console.error("Failed to check pending votes", e);
        }
    };

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800 pb-12">
            <DashboardHeader user={user} title="Member Portal" />

            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8 space-y-8 animate-in fade-in duration-500">

                {/* Dashboard Tabs Grid */}
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3 pb-2 border-b border-slate-200">
                    <Link to="?tab=overview" className={`w-full p-3 rounded-xl flex items-center gap-3 transition text-sm font-bold border group text-left ${activeTab === 'overview' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-white hover:bg-emerald-50 hover:text-emerald-700 border-slate-100'}`}>
                        <div className={`p-1.5 rounded-lg shadow-sm ${activeTab === 'overview' ? 'bg-emerald-100 text-emerald-600' : 'bg-slate-100 text-emerald-600'}`}><CreditCard size={16}/></div>
                        <span className="block truncate">Overview</span>
                    </Link>

                    <Link to="?tab=savings" className={`w-full p-3 rounded-xl flex items-center gap-3 transition text-sm font-bold border group text-left ${activeTab === 'savings' ? 'bg-blue-50 text-blue-700 border-blue-200' : 'bg-white hover:bg-blue-50 hover:text-blue-700 border-slate-100'}`}>
                        <div className={`p-1.5 rounded-lg shadow-sm ${activeTab === 'savings' ? 'bg-blue-100 text-blue-600' : 'bg-slate-100 text-blue-600'}`}><PiggyBank size={16}/></div>
                        <span className="block truncate">Savings</span>
                    </Link>

                    {/* ✅ LOANS TAB WITH NOTIFICATION DOT */}
                    <Link to="?tab=loans" className={`w-full p-3 rounded-xl flex items-center gap-3 transition text-sm font-bold border group text-left relative ${activeTab === 'loans' ? 'bg-indigo-50 text-indigo-700 border-indigo-200' : 'bg-white hover:bg-indigo-50 hover:text-indigo-700 border-slate-100'}`}>
                        <div className={`p-1.5 rounded-lg shadow-sm ${activeTab === 'loans' ? 'bg-indigo-100 text-indigo-600' : 'bg-slate-100 text-indigo-600'}`}><HandCoins size={16}/></div>
                        <span className="block truncate">Loans</span>

                        {/* 🔴 THE RED DOT INDICATOR */}
                        {pendingVotesCount > 0 && (
                            <span className="absolute top-2 right-2 flex h-3 w-3">
                                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
                                <span className="relative inline-flex rounded-full h-3 w-3 bg-red-500 border-2 border-white"></span>
                            </span>
                        )}
                    </Link>

                    <Link to="?tab=statements" className={`w-full p-3 rounded-xl flex items-center gap-3 transition text-sm font-bold border group text-left ${activeTab === 'statements' ? 'bg-purple-50 text-purple-700 border-purple-200' : 'bg-white hover:bg-purple-50 hover:text-purple-700 border-slate-100'}`}>
                        <div className={`p-1.5 rounded-lg shadow-sm ${activeTab === 'statements' ? 'bg-purple-100 text-purple-600' : 'bg-slate-100 text-purple-600'}`}><FileText size={16}/></div>
                        <span className="block truncate">Statements</span>
                    </Link>

                    <Link to="?tab=activities" className={`w-full p-3 rounded-xl flex items-center gap-3 transition text-sm font-bold border group text-left ${activeTab === 'activities' ? 'bg-amber-50 text-amber-700 border-amber-200' : 'bg-white hover:bg-amber-50 hover:text-amber-700 border-slate-100'}`}>
                        <div className={`p-1.5 rounded-lg shadow-sm ${activeTab === 'activities' ? 'bg-amber-100 text-amber-600' : 'bg-slate-100 text-amber-600'}`}><Activity size={16}/></div>
                        <span className="block truncate">Activities</span>
                    </Link>

                    <Link to="?tab=profile" className={`w-full p-3 rounded-xl flex items-center gap-3 transition text-sm font-bold border group text-left ${activeTab === 'profile' ? 'bg-slate-800 text-white border-slate-800' : 'bg-white hover:bg-slate-50 border-slate-100'}`}>
                        <div className={`p-1.5 rounded-lg shadow-sm ${activeTab === 'profile' ? 'bg-slate-700 text-white' : 'bg-slate-100 text-slate-600'}`}><UserIcon size={16}/></div>
                        <span className="block truncate">Profile</span>
                    </Link>
                </div>

                {/* Tab Content Rendering */}
                <div className="min-h-[400px] mt-6">
                    {activeTab === 'overview' && <MemberOverview user={user} />}
                    {activeTab === 'savings' && <MemberSavings />}

                    {/* ✅ LATEST: Pass fetchPendingVotes to update dot after voting */}
                    {activeTab === 'loans' && (
                        <div className="animate-in slide-in-from-bottom-2 duration-500">
                            <MemberLoans
                                user={user}
                                onUpdate={fetchUserProfile}
                                onVoteCast={fetchPendingVotes} // Triggers dot update
                            />
                        </div>
                    )}

                    {activeTab === 'statements' && <MemberStatements user={user} />}
                    {activeTab === 'activities' && <MemberActivities />}
                    {activeTab === 'profile' && <MemberProfile onUpdate={fetchUserProfile} />}
                </div>
            </main>
        </div>
    );
}