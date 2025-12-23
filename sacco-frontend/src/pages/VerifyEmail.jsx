import { useEffect, useState, useRef } from 'react'; // 1. Import useRef
import { useSearchParams, useNavigate } from 'react-router-dom';
import api from '../api';
import { CheckCircle, XCircle, Loader, Mail } from 'lucide-react';

export default function VerifyEmail() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState('verifying');
  const [message, setMessage] = useState('Verifying your email...');
  const [email, setEmail] = useState('');
  const [resending, setResending] = useState(false);
  const [resendMessage, setResendMessage] = useState('');

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
      const response = await api.post('/api/auth/verify/email', { token });
      if (response.data.success) {
        setStatus('success');
        setMessage('Email verified successfully!');
        setTimeout(() => navigate('/'), 3000);
      }
    } catch (err) {
      setStatus('error');
      const errorMsg = err.response?.data?.message || "Verification failed. Token may be expired.";
      setMessage(errorMsg);
      
      // Check if error is about expired token
      if (errorMsg.toLowerCase().includes('expired')) {
        setResendMessage('Your verification link has expired. Enter your email to receive a new one.');
      }
    }
  };

  const handleResendEmail = async () => {
    if (!email || !email.includes('@')) {
      setResendMessage('Please enter a valid email address');
      return;
    }

    setResending(true);
    setResendMessage('');
    
    try {
      const response = await api.post('/api/resend-verification', { email });
      if (response.data.success) {
        setResendMessage('✓ Verification email sent! Please check your inbox.');
        setEmail('');
      }
    } catch (err) {
      setResendMessage(err.response?.data?.message || 'Failed to resend email. Please try again.');
    } finally {
      setResending(false);
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
            
            {/* Resend Email Section */}
            {resendMessage && message.toLowerCase().includes('expired') && (
              <div className="mt-6 w-full max-w-sm bg-blue-50 border border-blue-200 rounded-lg p-4">
                <p className="text-sm text-blue-800 mb-3">{resendMessage}</p>
                <div className="flex gap-2">
                  <div className="relative flex-1">
                    <Mail className="absolute left-3 top-2.5 text-slate-400" size={16} />
                    <input
                      type="email"
                      placeholder="Enter your email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      className="w-full pl-9 pr-4 py-2 border rounded-lg text-sm outline-none focus:border-blue-500"
                      disabled={resending}
                    />
                  </div>
                  <button
                    onClick={handleResendEmail}
                    disabled={resending}
                    className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap"
                  >
                    {resending ? 'Sending...' : 'Resend'}
                  </button>
                </div>
              </div>
            )}
            
            {resendMessage && !resendMessage.includes('expired') && (
              <p className={`mt-4 text-sm ${resendMessage.includes('✓') ? 'text-green-600' : 'text-red-600'}`}>
                {resendMessage}
              </p>
            )}
            
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