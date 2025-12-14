import { useEffect, useState, useRef } from 'react'; // 1. Import useRef
import { useSearchParams, useNavigate } from 'react-router-dom';
import api from '../api';
import { CheckCircle, XCircle, Loader } from 'lucide-react';

export default function VerifyEmail() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState('verifying');
  const [message, setMessage] = useState('Verifying your email...');

  // 2. Add a ref to track if we already called the API
  const hasCalledAPI = useRef(false);

  useEffect(() => {
    const token = searchParams.get('token');

    if (!token) {
      setStatus('error');
      setMessage('Invalid verification link.');
      return;
    }

    // 3. Check if we already called the API
    if (hasCalledAPI.current) return;

    // 4. Mark as called immediately
    hasCalledAPI.current = true;

    verifyToken(token);
  }, []);

  const verifyToken = async (token) => {
    try {
      const response = await api.post('/api/verify/email', { token });
      if (response.data.success) {
        setStatus('success');
        setMessage('Email verified successfully!');
        setTimeout(() => navigate('/'), 3000);
      }
    } catch (err) {
      // Ideally, check if the error is "Token not found" but the user is already verified?
      // For now, this logic is fine because the double-call prevention fixes the root cause.
      setStatus('error');
      setMessage(err.response?.data?.message || "Verification failed. Token may be expired.");
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 p-4 font-sans">
      <div className="max-w-md w-full bg-white p-8 rounded-2xl shadow-xl text-center border border-slate-100">

        {/* Loading State */}
        {status === 'verifying' && (
          <div className="flex flex-col items-center">
            <Loader className="animate-spin text-blue-600 w-16 h-16 mb-4" />
            <h2 className="text-2xl font-bold text-slate-800">Verifying...</h2>
            <p className="text-slate-500 mt-2">Please wait while we confirm your identity.</p>
          </div>
        )}

        {/* Success State */}
        {status === 'success' && (
          <div className="flex flex-col items-center">
            <CheckCircle className="text-green-500 w-16 h-16 mb-4" />
            <h2 className="text-2xl font-bold text-slate-800">Verified!</h2>
            <p className="text-slate-500 mt-2">{message}</p>
            <p className="text-sm text-slate-400 mt-4">Redirecting to login...</p>
          </div>
        )}

        {/* Error State */}
        {status === 'error' && (
          <div className="flex flex-col items-center">
            <XCircle className="text-red-500 w-16 h-16 mb-4" />
            <h2 className="text-2xl font-bold text-slate-800">Verification Failed</h2>
            <p className="text-red-600 mt-2">{message}</p>
            <button
                onClick={() => navigate('/')}
                className="mt-6 bg-slate-900 text-white px-6 py-2 rounded-lg hover:bg-slate-800 transition"
            >
                Back to Login
            </button>
          </div>
        )}

      </div>
    </div>
  );
}