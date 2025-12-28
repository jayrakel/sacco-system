import { useNavigate } from 'react-router-dom';
import { LogOut, PieChart, FileText, Briefcase, Users } from 'lucide-react';
import api from '../api';

// Shared Logout Helper
const LogoutButton = () => {
    const navigate = useNavigate();
    const handleLogout = async () => {
        try {
            await api.post('/api/auth/logout');
        } catch (error) {
            console.error('Logout error:', error);
        } finally {
            localStorage.removeItem('sacco_token');
            localStorage.removeItem('sacco_user');
            navigate('/');
        }
    };
    return (
        <button onClick={handleLogout} className="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-lg flex items-center gap-2 transition shadow-sm">
            <LogOut size={16}/> Logout
        </button>
    );
};

// ...existing code...

// --- TREASURER ---
export const FinanceDashboard = () => (
    <div className="p-10 bg-emerald-50 min-h-screen font-sans">
        <div className="flex justify-between items-center mb-8">
            <div className="flex items-center gap-3">
                <div className="p-3 bg-emerald-100 rounded-full text-emerald-600">
                    <PieChart size={24} />
                </div>
                <div>
                    <h1 className="text-2xl font-bold text-slate-800">Treasurer Portal</h1>
                    <p className="text-slate-500 text-sm">Financial reports & transaction management</p>
                </div>
            </div>
            <LogoutButton />
        </div>
        <div className="bg-white p-8 rounded-xl shadow-sm border border-slate-200 text-center">
            <p className="text-slate-400">Ledgers and withdrawal requests will appear here.</p>
        </div>
    </div>
);

// --- CHAIRPERSON (High Level Overview) ---
export const ChairpersonDashboard = () => (
    <div className="p-10 bg-indigo-50 min-h-screen font-sans">
        <div className="flex justify-between items-center mb-8">
            <div className="flex items-center gap-3">
                <div className="p-3 bg-indigo-100 rounded-full text-indigo-600">
                    <Users size={24} />
                </div>
                <div>
                    <h1 className="text-2xl font-bold text-slate-800">Executive Oversight</h1>
                    <p className="text-slate-500 text-sm">Chairperson's Dashboard</p>
                </div>
            </div>
            <LogoutButton />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="bg-white p-6 rounded-xl shadow-sm border border-indigo-100">
                <h3 className="font-bold text-slate-700 mb-2">System Health</h3>
                <div className="text-3xl font-bold text-indigo-600">98%</div>
                <p className="text-xs text-slate-400">Operational Uptime</p>
            </div>
            <div className="bg-white p-6 rounded-xl shadow-sm border border-indigo-100">
                <h3 className="font-bold text-slate-700 mb-2">Total Assets</h3>
                <div className="text-3xl font-bold text-indigo-600">KES 4.2M</div>
                <p className="text-xs text-slate-400">Across all accounts</p>
            </div>
            <div className="bg-white p-6 rounded-xl shadow-sm border border-indigo-100">
                <h3 className="font-bold text-slate-700 mb-2">Pending Approvals</h3>
                <div className="text-3xl font-bold text-indigo-600">3</div>
                <p className="text-xs text-slate-400">Requires executive sign-off</p>
            </div>
        </div>
    </div>
);

// --- SECRETARY (Records & Correspondence) ---
export const SecretaryDashboard = () => (
    <div className="p-10 bg-amber-50 min-h-screen font-sans">
        <div className="flex justify-between items-center mb-8">
            <div className="flex items-center gap-3">
                <div className="p-3 bg-amber-100 rounded-full text-amber-600">
                    <FileText size={24} />
                </div>
                <div>
                    <h1 className="text-2xl font-bold text-slate-800">Secretariat Portal</h1>
                    <p className="text-slate-500 text-sm">Minutes, Records & Communication</p>
                </div>
            </div>
            <LogoutButton />
        </div>
        <div className="bg-white p-8 rounded-xl shadow-sm border border-slate-200 text-center">
            <p className="text-slate-400">Meeting minutes and member correspondence will appear here.</p>
        </div>
    </div>
);