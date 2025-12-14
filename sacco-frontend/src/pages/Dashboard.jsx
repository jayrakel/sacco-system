import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import { LogOut, Wallet, User, TrendingUp, CreditCard, RefreshCw } from 'lucide-react';
import { useSettings } from '../context/SettingsContext';

export default function Dashboard() {
  const [user, setUser] = useState(null);
  const [savings, setSavings] = useState({ balance: 0, accountNumber: 'Loading...' });
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const navigate = useNavigate();
  const { settings, getImageUrl } = useSettings();
  const logoUrl = getImageUrl(settings.SACCO_LOGO);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    setRefreshing(true);
    try {
      // 1. Get User Info from Local Storage
      const storedUser = localStorage.getItem('sacco_user');
      if (storedUser) {
        setUser(JSON.parse(storedUser));
      }

      // 2. Fetch Real Savings Balance from Backend
      const response = await api.get('/api/savings/my-balance');
      if (response.data.success) {
        setSavings(response.data);
      }

    } catch (error) {
      console.error("Dashboard Error:", error);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('sacco_token');
    localStorage.removeItem('sacco_user');
    navigate('/');
  };

  if (loading) return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50">
        <div className="animate-pulse flex flex-col items-center">
            <div className="h-12 w-12 bg-slate-200 rounded-full mb-4"></div>
            <div className="h-4 w-32 bg-slate-200 rounded"></div>
        </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-slate-50 font-sans">

      {/* Navbar */}
      <nav className="bg-white shadow-sm p-4 sticky top-0 z-10">
        <div className="max-w-6xl mx-auto flex justify-between items-center">
            <div className="flex items-center gap-3">
                {logoUrl ? (
                    <img src={logoUrl} alt="Logo" className="h-8 w-8 object-contain" />
                ) : (
                    <TrendingUp className="text-emerald-600" />
                )}
                <h1 className="text-xl font-bold text-slate-800 hidden md:block">{settings.SACCO_NAME}</h1>
            </div>

            <div className="flex items-center gap-4">
                <span className="text-sm font-medium text-slate-600 hidden sm:block">
                    {user?.firstName} {user?.lastName}
                </span>
                <div className="h-8 w-8 bg-slate-100 rounded-full flex items-center justify-center text-slate-600 font-bold border border-slate-200">
                    {user?.firstName?.charAt(0)}{user?.lastName?.charAt(0)}
                </div>
                <button
                  onClick={handleLogout}
                  className="flex items-center gap-2 text-sm text-red-600 hover:bg-red-50 px-3 py-2 rounded-lg transition"
                >
                  <LogOut size={16} /> <span className="hidden sm:inline">Logout</span>
                </button>
            </div>
        </div>
      </nav>

      <div className="max-w-6xl mx-auto p-4 md:p-8">

        {/* Welcome Header */}
                <div className="bg-slate-900 text-white rounded-2xl p-8 shadow-xl mb-8 relative overflow-hidden">
                  <div className="relative z-10">
                      {/* ✅ USE CORRECT FIELDS */}
                      <h2 className="text-3xl font-bold mb-2">Welcome, {user?.firstName} {user?.lastName}!</h2>
                      <p className="text-slate-400">
                          Member No: <span className="font-mono text-emerald-400">{user?.memberNumber || 'PENDING'}</span>
                      </p>
                  </div>

          {/* Background decoration */}
          <div className="absolute right-0 top-0 h-full w-1/3 bg-gradient-to-l from-emerald-900/50 to-transparent"></div>
          <div className="absolute -bottom-10 -right-10 h-64 w-64 bg-emerald-500/20 rounded-full blur-3xl"></div>
        </div>

        {/* Stats Grid */}
        <div className="grid md:grid-cols-2 gap-6 mb-8">

          {/* Savings Card */}
          <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100 relative group overflow-hidden">
            <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition">
                <Wallet size={100} className="text-emerald-600" />
            </div>

            <div className="flex justify-between items-start mb-4">
                <div className="flex items-center gap-3">
                  <div className="p-2 bg-emerald-100 rounded-lg text-emerald-600">
                    <Wallet size={24} />
                  </div>
                  <div>
                      <h3 className="font-bold text-slate-700">My Savings</h3>
                      <p className="text-xs text-slate-400 font-mono">{savings.accountNumber}</p>
                  </div>
                </div>
                <button onClick={fetchDashboardData} className={`text-slate-400 hover:text-emerald-600 transition ${refreshing ? 'animate-spin' : ''}`}>
                    <RefreshCw size={18} />
                </button>
            </div>

            <p className="text-4xl font-bold text-slate-900 mb-1">
                KES {Number(savings.balance).toLocaleString()}
            </p>
            <p className="text-sm text-slate-500 mb-6">Total available balance</p>

            <div className="flex gap-3">
                <button className="flex-1 bg-emerald-600 hover:bg-emerald-700 text-white py-2 rounded-lg text-sm font-bold transition flex items-center justify-center gap-2">
                    <CreditCard size={16} /> Deposit
                </button>
                <button className="flex-1 bg-slate-100 hover:bg-slate-200 text-slate-700 py-2 rounded-lg text-sm font-bold transition">
                    Statement
                </button>
            </div>
          </div>

          {/* Loan Status Card */}
          <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100 relative group overflow-hidden">
             <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition">
                <User size={100} className="text-blue-600" />
            </div>

            <div className="flex items-center gap-3 mb-4">
              <div className="p-2 bg-blue-100 rounded-lg text-blue-600">
                <User size={24} />
              </div>
              <h3 className="font-bold text-slate-700">Loan Limit</h3>
            </div>

            {/* Logic: Loan Limit is typically 3x Savings */}
            <p className="text-4xl font-bold text-slate-900 mb-1">
                KES {(Number(savings.balance) * 3).toLocaleString()}
            </p>
            <p className="text-sm text-slate-500 mb-6">Estimated qualification (3x Savings)</p>

            <button className="w-full border border-blue-600 text-blue-600 hover:bg-blue-50 py-2 rounded-lg text-sm font-bold transition">
                Apply for Loan →
            </button>
          </div>
        </div>

        {/* Recent Activity Placeholder */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
            <div className="p-6 border-b border-slate-100">
                <h3 className="font-bold text-slate-800">Recent Transactions</h3>
            </div>
            <div className="p-8 text-center text-slate-400">
                <p>No recent transactions found.</p>
            </div>
        </div>

      </div>
    </div>
  );
}