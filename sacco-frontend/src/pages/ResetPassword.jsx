import React, { useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import api from '../api';
import { Lock, CheckCircle, AlertTriangle, ArrowRight } from 'lucide-react';
import BrandedSpinner from '../components/BrandedSpinner';

export default function ResetPassword() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const token = searchParams.get('token');

    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [status, setStatus] = useState('idle'); // idle, loading, success, error
    const [msg, setMsg] = useState('');

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (password !== confirmPassword) {
            setMsg("Passwords do not match!");
            setStatus('error');
            return;
        }
        if (password.length < 6) {
            setMsg("Password must be at least 6 characters.");
            setStatus('error');
            return;
        }

        setStatus('loading');
        try {
            await api.post('/api/auth/reset-password', { token, newPassword: password });
            setStatus('success');
            setTimeout(() => navigate('/'), 3000);
        } catch (err) {
            setStatus('error');
            setMsg(err.response?.data?.message || "Invalid or expired token.");
        }
    };

    if (!token) return <div className="min-h-screen flex items-center justify-center text-slate-500">Invalid Link</div>;

    return (
        <div className="min-h-screen flex items-center justify-center bg-slate-50 p-4">
            <div className="bg-white w-full max-w-md p-8 rounded-2xl shadow-xl border border-slate-100">
                <div className="text-center mb-8">
                    <div className="bg-emerald-50 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4 text-emerald-600">
                        <Lock size={32} />
                    </div>
                    <h2 className="text-2xl font-bold text-slate-800">Set New Password</h2>
                    <p className="text-slate-500 mt-2">Secure your account with a strong password.</p>
                </div>

                {status === 'success' ? (
                    <div className="text-center animate-in fade-in">
                        <div className="bg-green-100 text-green-700 p-4 rounded-xl mb-4 flex items-center justify-center gap-2">
                            <CheckCircle size={20} /> Password Changed!
                        </div>
                        <p className="text-slate-500 mb-6">Redirecting to login...</p>
                        <button onClick={() => navigate('/')} className="w-full bg-slate-900 text-white font-bold py-3 rounded-xl">Go to Login</button>
                    </div>
                ) : (
                    <form onSubmit={handleSubmit} className="space-y-5">
                        {status === 'error' && (
                            <div className="bg-red-50 text-red-700 p-3 rounded-lg text-sm flex items-center gap-2">
                                <AlertTriangle size={16} /> {msg}
                            </div>
                        )}

                        <div>
                            <label className="block text-xs font-bold text-slate-500 uppercase mb-1">New Password</label>
                            <input 
                                type="password" required className="w-full border border-slate-300 p-3 rounded-xl focus:ring-2 focus:ring-emerald-500 outline-none"
                                value={password} onChange={e => setPassword(e.target.value)}
                            />
                        </div>
                        <div>
                            <label className="block text-xs font-bold text-slate-500 uppercase mb-1">Confirm Password</label>
                            <input 
                                type="password" required className="w-full border border-slate-300 p-3 rounded-xl focus:ring-2 focus:ring-emerald-500 outline-none"
                                value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)}
                            />
                        </div>

                        <button 
                            disabled={status === 'loading'}
                            className="w-full bg-slate-900 hover:bg-emerald-600 text-white font-bold py-3.5 rounded-xl transition flex justify-center items-center gap-2 disabled:opacity-50"
                        >
                            {status === 'loading' ? <BrandedSpinner size="small" color="white" showTagline={false}/> : <>Update Password <ArrowRight size={18} /></>}
                        </button>
                    </form>
                )}
            </div>
        </div>
    );
}