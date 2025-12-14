import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { loginUser } from '../features/auth/services/authService';
import api from '../api'; // Need direct api access for the resend call
import { ShieldCheck, Lock, Mail, ChevronRight, AlertTriangle, RefreshCw } from 'lucide-react';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [showResend, setShowResend] = useState(false); // Toggle for resend button
  const [resendStatus, setResendStatus] = useState('');

  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setShowResend(false);

    try {
      const userData = await loginUser({ email, password });

      // ... (Your existing Token/Role logic remains EXACTLY same) ...
      if (!userData.token) throw new Error("No token");
      localStorage.setItem('sacco_token', userData.token);
      localStorage.setItem('sacco_user', JSON.stringify(userData));

      if (userData.systemSetupRequired) { navigate('/system-setup'); return; }
      if (userData.mustChangePassword) { navigate('/change-password'); return; }

      switch (userData.role) {
        case 'ADMIN': navigate('/admin-dashboard'); break;
        case 'LOAN_OFFICER': navigate('/loans-dashboard'); break;
        case 'TREASURER': navigate('/finance-dashboard'); break;
        case 'CHAIRPERSON':
        case 'ASSISTANT_CHAIRPERSON': navigate('/chairperson-dashboard'); break;
        case 'SECRETARY':
        case 'ASSISTANT_SECRETARY': navigate('/secretary-dashboard'); break;
        default: navigate('/dashboard'); break;
      }

    } catch (err) {
      console.error("Login Error:", err);
      const msg = err.response?.data?.message || "Connection failed.";
      setError(msg);

      // ✅ Check if error is about verification
      if (msg.includes("verify your email")) {
          setShowResend(true);
      }
    }
    setLoading(false);
  };

  // ✅ New Handler for Resend
  const handleResend = async () => {
      if (!email) return;
      setResendStatus('Sending...');
      try {
          await api.post('/api/auth/resend-verification', { email });
          setResendStatus('Sent! Check your inbox.');
          setShowResend(false);
      } catch (err) {
          setResendStatus('Failed to send.');
      }
  };

  return (
    <div className="min-h-screen flex bg-slate-50 font-sans">
      {/* ... Left Side Branding (Keep same) ... */}
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

      <div className="w-full lg:w-1/2 flex items-center justify-center p-8">
        <div className="w-full max-w-md">
          <div className="bg-white p-10 rounded-2xl shadow-xl border border-slate-100">
            <h2 className="text-3xl font-bold text-slate-800 mb-2">Welcome Back</h2>
            <p className="text-slate-500 mb-8">Enter your credentials to access the portal.</p>

            {error && (
              <div className="mb-6 p-4 bg-red-50 border-l-4 border-red-500 text-red-700 text-sm rounded-r">
                <div className="flex gap-2 items-center">
                    <AlertTriangle size={18} /> {error}
                </div>

                {/* ✅ Show Resend Button if blocked */}
                {showResend && (
                    <div className="mt-3 pt-3 border-t border-red-100">
                        <button
                            onClick={handleResend}
                            className="text-slate-900 underline font-bold hover:text-emerald-600 flex items-center gap-2"
                        >
                            <RefreshCw size={14} /> Resend Verification Link
                        </button>
                    </div>
                )}
              </div>
            )}

            {resendStatus && (
                <div className="mb-6 p-4 bg-blue-50 text-blue-700 text-sm rounded border border-blue-100">
                    {resendStatus}
                </div>
            )}

            <form onSubmit={handleLogin} className="space-y-6">
               {/* ... (Keep Email and Password Inputs exactly the same) ... */}
               <div>
                <label className="text-sm font-bold text-slate-700">Email</label>
                <div className="relative mt-1">
                  <Mail className="absolute left-3 top-3 text-slate-400" size={20} />
                  <input
                    type="email" required
                    className="w-full border border-slate-300 p-3 pl-10 rounded-xl focus:ring-2 focus:ring-slate-900 outline-none"
                    value={email} onChange={e => setEmail(e.target.value)}
                  />
                </div>
              </div>
              <div>
                <label className="text-sm font-bold text-slate-700">Password</label>
                <div className="relative mt-1">
                  <Lock className="absolute left-3 top-3 text-slate-400" size={20} />
                  <input
                    type="password" required
                    className="w-full border border-slate-300 p-3 pl-10 rounded-xl focus:ring-2 focus:ring-slate-900 outline-none"
                    value={password} onChange={e => setPassword(e.target.value)}
                  />
                </div>
              </div>

              <button disabled={loading} className="w-full bg-slate-900 hover:bg-emerald-600 text-white font-bold py-3.5 rounded-xl transition flex justify-center gap-2 items-center">
                {loading ? "Verifying..." : <>Sign In <ChevronRight size={20} /></>}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}