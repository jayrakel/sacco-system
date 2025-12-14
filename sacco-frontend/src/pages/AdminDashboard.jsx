import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useSettings } from '../context/SettingsContext'; // ✅ Import Settings
import api from '../api';
import { Users, Settings, Wallet, CreditCard, TrendingUp, LogOut, UserPlus } from 'lucide-react';

export default function AdminDashboard() {
  const [stats, setStats] = useState({
    totalMembers: 0,
    totalSavings: 0,
    totalLoansIssued: 0,
    netIncome: 0
  });
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(true);

  const navigate = useNavigate();
  const { settings, getImageUrl } = useSettings(); // ✅ Typo fixed here
  const logoUrl = getImageUrl(settings.SACCO_LOGO);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [reportRes, membersRes] = await Promise.all([
        api.get('/api/reports/today'),
        api.get('/api/members')
      ]);

      if (reportRes.data.success) setStats(reportRes.data.data);
      if (membersRes.data.success) setMembers(membersRes.data.data);

    } catch (error) {
      console.error("Dashboard Load Error:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('sacco_token');
    localStorage.removeItem('sacco_user');
    navigate('/');
  };

  return (
    <div className="min-h-screen bg-slate-100 font-sans">
      <nav className="bg-slate-900 text-white p-4 shadow-lg flex justify-between items-center sticky top-0 z-10">
        <div className="flex items-center gap-3">
            {/* ✅ Dynamic Dashboard Logo */}
            {logoUrl ? (
                <img src={logoUrl} alt="Logo" className="w-8 h-8 object-contain bg-white rounded p-0.5" />
            ) : (
                <TrendingUp className="text-emerald-400" />
            )}
            <h1 className="text-xl font-bold">
                {settings.SACCO_NAME} <span className="text-slate-400 text-sm font-normal">| Admin Portal</span>
            </h1>
        </div>

        <button
          onClick={handleLogout}
          className="flex items-center gap-2 text-sm bg-slate-800 hover:bg-slate-700 px-4 py-2 rounded-lg transition"
        >
          <LogOut size={16} /> Logout
        </button>
      </nav>

      <div className="max-w-7xl mx-auto p-8">

        {/* Statistics Cards */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
          <StatCard
            icon={<Users className="text-blue-600" />}
            label="Total Members"
            value={stats.totalMembers}
            color="bg-blue-50 border-blue-200"
          />
          <StatCard
            icon={<Wallet className="text-emerald-600" />}
            label="Total Savings"
            value={`KES ${stats.totalSavings?.toLocaleString() || 0}`}
            color="bg-emerald-50 border-emerald-200"
          />
          <StatCard
            icon={<CreditCard className="text-purple-600" />}
            label="Loans Issued"
            value={`KES ${stats.totalLoansIssued?.toLocaleString() || 0}`}
            color="bg-purple-50 border-purple-200"
          />
          <StatCard
            icon={<TrendingUp className="text-amber-600" />}
            label="Net Income"
            value={`KES ${stats.netIncome?.toLocaleString() || 0}`}
            color="bg-amber-50 border-amber-200"
          />
        </div>

        {/* Recent Members Table */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <div className="p-6 border-b border-slate-100 flex justify-between items-center">
            <h2 className="text-lg font-bold text-slate-800">Recent Members</h2>

            <div className="flex gap-4">
                {/* Configuration Button */}
                <Link to="/admin/settings" className="flex items-center gap-2 bg-slate-100 text-slate-700 px-4 py-2 rounded-lg text-sm hover:bg-slate-200 transition border border-slate-300">
                    <Settings size={16} /> Configuration
                </Link>

                {/* New Member Button */}
                <Link to="/add-member" className="flex items-center gap-2 bg-emerald-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-emerald-700 transition">
                    <UserPlus size={16} /> New Member
                </Link>

                <button onClick={fetchData} className="text-sm text-blue-600 hover:underline">Refresh</button>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-slate-500 uppercase">
                <tr>
                  <th className="p-4">Member No</th>
                  <th className="p-4">Name</th>
                  <th className="p-4">Email</th>
                  <th className="p-4">Status</th>
                  <th className="p-4 text-right">Savings</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {members.length === 0 ? (
                  <tr><td colSpan="5" className="p-8 text-center text-slate-400">No members found.</td></tr>
                ) : (
                  members.map((member) => (
                    <tr key={member.id} className="hover:bg-slate-50 transition">
                      <td className="p-4 font-mono text-slate-600">{member.memberNumber}</td>
                      <td className="p-4 font-medium text-slate-900">{member.firstName} {member.lastName}</td>
                      <td className="p-4 text-slate-500">{member.email}</td>
                      <td className="p-4">
                        <span className={`px-2 py-1 rounded-full text-xs font-semibold ${
                          member.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                        }`}>
                          {member.status}
                        </span>
                      </td>
                      <td className="p-4 text-right font-bold text-slate-700">
                        {member.totalSavings?.toLocaleString()}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}

function StatCard({ icon, label, value, color }) {
  return (
    <div className={`p-6 rounded-xl border ${color} shadow-sm`}>
      <div className="flex items-center gap-4">
        <div className="p-3 bg-white rounded-full shadow-sm">
          {icon}
        </div>
        <div>
          <p className="text-slate-500 text-sm font-medium">{label}</p>
          <h3 className="text-2xl font-bold text-slate-800">{value}</h3>
        </div>
      </div>
    </div>
  );
}