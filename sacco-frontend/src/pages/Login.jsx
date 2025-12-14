import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { loginUser } from '../features/auth/services/authService';
import api from '../api';
import { ShieldCheck, Lock, Mail, ChevronRight, AlertTriangle, RefreshCw } from 'lucide-react';
import { useSettings } from '../context/SettingsContext';
import BrandedSpinner from '../components/BrandedSpinner'; // ✅ Import New Component

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [localLoading, setLocalLoading] = useState(false);
  const [error, setError] = useState('');
  const [showResend, setShowResend] = useState(false);
  const [resendStatus, setResendStatus] = useState('');
  const navigate = useNavigate();

  const { settings, getImageUrl, loading: brandingLoading } = useSettings();
  const logoUrl = getImageUrl(settings.SACCO_LOGO);
  const iconUrl = getImageUrl(settings.SACCO_FAVICON);

  const handleLogin = async (e) => {
      e.preventDefault();
      setLocalLoading(true);
      setError('');
      setShowResend(false);

      try {
        const userData = await loginUser({ email, password });

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
        if (msg.includes("verify your email")) setShowResend(true);
      }
      setLocalLoading(false);
  };

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
      {/* Left Side Branding */}
      <div className="hidden lg:flex w-1/2 bg-slate-900 flex-col justify-center items-center p-12 text-white relative">
        <div className="relative z-10 text-center">
          <div className="mb-8 inline-block p-6 bg-white/5 rounded-full backdrop-blur-sm border border-white/10 shadow-2xl">
            {iconUrl ? (
                <img src={iconUrl} alt="Icon" className="w-24 h-24 object-contain" />
            ) : (
                <ShieldCheck size={80} className="text-emerald-400" />
            )}
          </div>
          <h1 className="text-5xl font-bold mb-4 tracking-tight">{settings.SACCO_NAME}</h1>
          <p className="text-slate-400 text-xl max-w-md mx-auto leading-relaxed">
            {settings.SACCO_TAGLINE || 'Secure, Transparent, and Automated Management.'}
          </p>
        </div>
        {/* Background Blobs */}
        <div className="absolute top-0 left-0 w-full h-full overflow-hidden z-0">
            <div className="absolute top-10 left-10 w-32 h-32 bg-emerald-500 rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-blob"></div>
            <div className="absolute top-10 right-10 w-32 h-32 bg-blue-500 rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-blob animation-delay-2000"></div>
            <div className="absolute bottom-10 left-20 w-32 h-32 bg-purple-500 rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-blob animation-delay-4000"></div>
        </div>
      </div>

      {/* Right Side Form */}
      <div className="w-full lg:w-1/2 flex flex-col justify-center p-8 relative">
        <div className="w-full max-w-md mx-auto flex-1 flex flex-col justify-center">
          <div className="bg-white p-10 rounded-2xl shadow-xl border border-slate-100">

            <div className="mb-6 flex justify-center">
                {logoUrl ? (
                    <img src={logoUrl} alt={settings.SACCO_NAME} className="h-16 object-contain" />
                ) : (
                    <div className="flex items-center gap-2 text-slate-800">
                        <ShieldCheck className="text-emerald-600" size={32} />
                        <span className="text-xl font-bold">{settings.SACCO_NAME}</span>
                    </div>
                )}
            </div>

            <h2 className="text-2xl font-bold text-slate-800 mb-2 text-center">Welcome Back</h2>
            <p className="text-slate-500 mb-8 text-center">Enter your credentials to access the portal.</p>

            {error && (
              <div className="mb-6 p-4 bg-red-50 border-l-4 border-red-500 text-red-700 text-sm rounded-r">
                <div className="flex gap-2 items-center">
                    <AlertTriangle size={18} /> {error}
                </div>
                {showResend && (
                    <div className="mt-3 pt-3 border-t border-red-100">
                        <button onClick={handleResend} className="text-slate-900 underline font-bold hover:text-emerald-600 flex items-center gap-2">
                            <RefreshCw size={14} /> Resend Verification Link
                        </button>
                    </div>
                )}
              </div>
            )}

            {resendStatus && (
                <div className="mb-6 p-4 bg-blue-50 text-blue-700 text-sm rounded border border-blue-100">{resendStatus}</div>
            )}

            <form onSubmit={handleLogin} className="space-y-6">
              <div>
                <label className="block text-sm font-bold text-slate-700 mb-1">Email</label>
                <div className="relative">
                  <Mail className="absolute left-3 top-3 text-slate-400" size={20} />
                  <input type="email" required className="w-full border border-slate-300 p-3 pl-10 rounded-xl focus:ring-2 focus:ring-slate-900 outline-none transition" value={email} onChange={e => setEmail(e.target.value)} />
                </div>
              </div>
              <div>
                <label className="block text-sm font-bold text-slate-700 mb-1">Password</label>
                <div className="relative">
                  <Lock className="absolute left-3 top-3 text-slate-400" size={20} />
                  <input type="password" required className="w-full border border-slate-300 p-3 pl-10 rounded-xl focus:ring-2 focus:ring-slate-900 outline-none transition" value={password} onChange={e => setPassword(e.target.value)} />
                </div>
              </div>
              <button
                disabled={localLoading}
                className="w-full bg-slate-900 hover:bg-emerald-600 text-white font-bold py-3.5 rounded-xl transition flex justify-center gap-2 items-center disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {localLoading ? (
                    // ✅ 2. BUTTON SPINNER (Small)
                    <div className="flex items-center gap-2">
                        <BrandedSpinner iconUrl={iconUrl} size="small" color="white" />
                        <span>Verifying...</span>
                    </div>
                ) : (
                    <>Sign In <ChevronRight size={20} /></>
                )}
              </button>
            </form>
          </div>
        </div>

        {/* Footer */}
        <div className="w-full text-center py-6 text-slate-400 text-sm mt-auto border-t border-slate-200">
            <p>© {new Date().getFullYear()} {settings.SACCO_NAME}. All rights reserved.</p>
            <div className="flex justify-center gap-4 mt-2">
                <a href="#" className="hover:text-emerald-600 transition">Privacy Policy</a>
                <span>•</span>
                <a href="#" className="hover:text-emerald-600 transition">Terms of Service</a>
                <span>•</span>
                <a href="#" className="hover:text-emerald-600 transition">Support</a>
            </div>
        </div>

      </div>
    </div>
  );
}