import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import { LogOut, Wallet, User } from 'lucide-react';

export default function Dashboard() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const storedUser = localStorage.getItem('sacco_user');
    if (storedUser) {
      setUser(JSON.parse(storedUser));
    }
    setLoading(false);
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('sacco_token');
    localStorage.removeItem('sacco_user');
    navigate('/');
  };

  if (loading) return <div className="p-10">Loading...</div>;

  return (
    <div className="min-h-screen bg-slate-50 font-sans">
      <nav className="bg-white shadow-sm p-4 flex justify-between items-center">
        <h1 className="text-xl font-bold text-slate-800">Sacco Portal</h1>
        <button
          onClick={handleLogout}
          className="flex items-center gap-2 text-sm text-red-600 hover:bg-red-50 px-3 py-2 rounded-lg transition"
        >
          <LogOut size={16} /> Logout
        </button>
      </nav>

      <div className="max-w-4xl mx-auto p-8">
        <div className="bg-slate-900 text-white rounded-2xl p-8 shadow-xl mb-8">
          <h2 className="text-2xl font-bold mb-2">Welcome back, {user?.full_name || 'Member'}!</h2>
          <p className="text-slate-400">Member No: {user?.membership_number || 'N/A'}</p>
        </div>

        <div className="grid md:grid-cols-2 gap-6">
          <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-2 bg-emerald-100 rounded-lg text-emerald-600">
                <Wallet size={24} />
              </div>
              <h3 className="font-bold text-slate-700">My Savings</h3>
            </div>
            <p className="text-3xl font-bold text-slate-900">KES 0.00</p>
            <p className="text-sm text-slate-500 mt-1">Total accumulated savings</p>
          </div>

          <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-2 bg-blue-100 rounded-lg text-blue-600">
                <User size={24} />
              </div>
              <h3 className="font-bold text-slate-700">Loan Status</h3>
            </div>
            <p className="text-slate-500">No active loans.</p>
            <button className="mt-4 text-blue-600 text-sm font-semibold hover:underline">Apply for Loan →</button>
          </div>
        </div>
      </div>
    </div>
  );
}