import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { loginUser } from '../features/auth/services/authService';
import { ShieldCheck, Lock, Mail, ChevronRight, AlertTriangle } from 'lucide-react';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const response = await loginUser({ email, password });

      // Java returns: { success: true, token: "...", user: {...} }
      const { user, token } = response;

      // 1. Save Token (CRITICAL)
      localStorage.setItem('sacco_token', token);

      // 2. Save User info
      localStorage.setItem('sacco_user', JSON.stringify(user));

      // 3. Navigate based on role
      if (user.role === 'ADMIN') {
          navigate('/admin-dashboard');
      } else {
          navigate('/dashboard');
      }

    } catch (err) {
      console.error("Login Error:", err);
      // Java backend sends error messages in the 'message' field
      setError(err.response?.data?.message || "Connection failed. Is the backend running?");
    }
    setLoading(false);
  };

  return (
    <div className="min-h-screen flex bg-slate-50 font-sans">
      {/* Left Side Branding */}
      <div className="hidden lg:flex w-1/2 bg-slate-900 flex-col justify-center items-center p-12 text-white relative">
        <div className="relative z-10 text-center">
          <div className="bg-emerald-500/20 p-6 rounded-full inline-block mb-8">
            <ShieldCheck size={64} className="text-emerald-400" />
          </div>
          <h1 className="text-5xl font-bold mb-6">Sacco System</h1>
          <p className="text-slate-400 text-xl max-w-md mx-auto">
            Secure, Transparent, and Automated Management.
          </p>
        </div>
      </div>

      {/* Right Side Form */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-8">
        <div className="w-full max-w-md">
          <div className="bg-white p-10 rounded-2xl shadow-xl border border-slate-100">
            <h2 className="text-3xl font-bold text-slate-800 mb-2">Welcome Back</h2>
            <p className="text-slate-500 mb-8">Enter your credentials to access the portal.</p>

            {error && (
              <div className="mb-6 p-4 bg-red-50 border-l-4 border-red-500 text-red-700 text-sm flex gap-2">
                <AlertTriangle size={18} /> {error}
              </div>
            )}

            <form onSubmit={handleLogin} className="space-y-6">
              <div>
                <label className="text-sm font-bold text-slate-700">Email</label>
                <div className="relative mt-1">
                  <Mail className="absolute left-3 top-3 text-slate-400" size={20} />
                  <input
                    type="email"
                    required
                    className="w-full border p-3 pl-10 rounded-xl"
                    placeholder="admin@sacco.com"
                    value={email}
                    onChange={e => setEmail(e.target.value)}
                  />
                </div>
              </div>
              <div>
                <label className="text-sm font-bold text-slate-700">Password</label>
                <div className="relative mt-1">
                  <Lock className="absolute left-3 top-3 text-slate-400" size={20} />
                  <input
                    type="password"
                    required
                    className="w-full border p-3 pl-10 rounded-xl"
                    placeholder="••••••••"
                    value={password}
                    onChange={e => setPassword(e.target.value)}
                  />
                </div>
              </div>
              <button disabled={loading} className="w-full bg-slate-900 hover:bg-emerald-600 text-white font-bold py-3.5 rounded-xl transition flex justify-center gap-2">
                {loading ? "Verifying..." : <>Sign In <ChevronRight /></>}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}