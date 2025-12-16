import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    LayoutDashboard, PiggyBank, CreditCard, User, Bell, LogOut, Menu, X
} from 'lucide-react';

// Components
import DashboardHeader from '../components/DashboardHeader';
import MemberOverview from '../features/member/components/MemberOverview';
import MemberSavings from '../features/member/components/MemberSavings';
import MemberLoans from '../features/member/components/MemberLoans';
import MemberProfile from '../features/member/components/MemberProfile';

export default function MemberDashboard() {
    const [user, setUser] = useState(null);
    const [activeTab, setActiveTab] = useState('overview');
    const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (!storedUser) {
            navigate('/login');
            return;
        }
        setUser(JSON.parse(storedUser));
    }, [navigate]);

    const handleLogout = () => {
        localStorage.removeItem('sacco_token');
        localStorage.removeItem('sacco_user');
        navigate('/login');
    };

    const renderContent = () => {
        if (!user) return <div className="p-8 text-center">Loading Profile...</div>;

        // Pass user/memberId to children so they can fetch their own data
        switch (activeTab) {
            case 'overview': return <MemberOverview user={user} />;
            case 'savings': return <MemberSavings user={user} />;
            case 'loans': return <MemberLoans user={user} />;
            case 'profile': return <MemberProfile user={user} />;
            default: return <MemberOverview user={user} />;
        }
    };

    const NavItem = ({ id, label, icon: Icon }) => (
        <button
            onClick={() => { setActiveTab(id); setIsMobileMenuOpen(false); }}
            className={`w-full flex items-center gap-3 px-4 py-3 text-sm font-bold rounded-xl transition-all ${
                activeTab === id
                ? 'bg-indigo-900 text-white shadow-md'
                : 'text-slate-500 hover:bg-slate-100 hover:text-slate-900'
            }`}
        >
            <Icon size={18} />
            {label}
        </button>
    );

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800">
            {/* Header */}
            <DashboardHeader user={user} title="Member Portal" />

            <div className="max-w-7xl mx-auto px-4 sm:px-6 py-8">
                <div className="flex flex-col md:flex-row gap-6">

                    {/* Sidebar Navigation */}
                    <aside className={`md:w-64 bg-white rounded-2xl shadow-sm border border-slate-200 p-4 flex flex-col gap-2 h-fit ${isMobileMenuOpen ? 'block' : 'hidden md:flex'}`}>
                        <div className="md:hidden flex justify-end mb-2">
                            <button onClick={() => setIsMobileMenuOpen(false)}><X /></button>
                        </div>

                        <NavItem id="overview" label="Overview" icon={LayoutDashboard} />
                        <NavItem id="savings" label="My Savings" icon={PiggyBank} />
                        <NavItem id="loans" label="My Loans" icon={CreditCard} />
                        <NavItem id="profile" label="My Profile" icon={User} />

                        <div className="border-t border-slate-100 my-2 pt-2">
                            <button onClick={handleLogout} className="w-full flex items-center gap-3 px-4 py-3 text-sm font-bold text-red-600 hover:bg-red-50 rounded-xl transition-all">
                                <LogOut size={18} /> Logout
                            </button>
                        </div>
                    </aside>

                    {/* Mobile Toggle */}
                    <div className="md:hidden mb-4">
                        <button onClick={() => setIsMobileMenuOpen(true)} className="flex items-center gap-2 bg-white p-3 rounded-lg shadow-sm border text-sm font-bold">
                            <Menu size={20} /> Menu
                        </button>
                    </div>

                    {/* Main Content Area */}
                    <main className="flex-1 min-h-[500px]">
                        {renderContent()}
                    </main>
                </div>
            </div>
        </div>
    );
}