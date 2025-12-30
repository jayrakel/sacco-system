import React, { useState, useEffect } from 'react';
import OfficerDashboard from './roles/OfficerDashboard';
import SecretaryDashboard from './roles/SecretaryDashboard';
import ChairpersonDashboard from './roles/ChairpersonDashboard';
import TreasurerDashboard from './roles/TreasurerDashboard';
import DashboardHeader from '../../components/DashboardHeader';

export default function LoanGovernancePortal() {
    const [user, setUser] = useState(null);

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
    }, []);

    if (!user) return <div>Loading...</div>;

    // --- THE TRAFFIC COP ---
    const renderDashboard = () => {
        switch (user.role) {
            case 'LOAN_OFFICER':
                return <OfficerDashboard />;
            case 'SECRETARY':
                return <SecretaryDashboard />;
            case 'CHAIRMAN':
                return <ChairpersonDashboard />;
            case 'TREASURER':
                return <TreasurerDashboard />;
            case 'ADMIN':
                // Admins see a tabbed view or specific view (For now, let's show Officer view)
                return <OfficerDashboard />;
            default:
                return <div className="p-10 text-center">You do not have permission to view this portal.</div>;
        }
    };

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800">
            <DashboardHeader user={user} title={`${user.role.replace('_', ' ')} PORTAL`} />
            <main className="max-w-7xl mx-auto px-4 mt-8">
                {renderDashboard()}
            </main>
        </div>
    );
}