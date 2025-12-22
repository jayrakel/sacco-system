import React from 'react';
import { useNavigate } from 'react-router-dom';
import { LogIn, Clock, Shield, AlertCircle } from 'lucide-react';

export default function SessionExpired() {
    const navigate = useNavigate();

    const handleLogin = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        navigate('/login', { state: { from: window.location.pathname } });
    };

    return (
        <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
            <div className="max-w-2xl w-full text-center">
                {/* Clock Icon */}
                <div className="mb-8">
                    <div className="w-32 h-32 mx-auto bg-purple-100 rounded-full flex items-center justify-center">
                        <Clock size={64} className="text-purple-600" />
                    </div>
                </div>

                {/* Error Message */}
                <div className="space-y-4 mb-8">
                    <h2 className="text-3xl font-bold text-gray-800">
                        Session Expired
                    </h2>
                    <p className="text-lg text-gray-600 max-w-md mx-auto">
                        Your login session has expired for security reasons.
                        Please log in again to continue.
                    </p>
                </div>

                {/* Info Box */}
                <div className="mb-8 p-6 bg-purple-50 rounded-lg border border-purple-100 text-left max-w-md mx-auto">
                    <h3 className="font-bold text-purple-900 mb-3 flex items-center gap-2">
                        <AlertCircle size={20} />
                        Why Did This Happen?
                    </h3>
                    <ul className="space-y-2 text-sm text-purple-800">
                        <li className="flex items-start gap-2">
                            <span className="text-purple-600 mt-0.5">•</span>
                            <span>You've been inactive for an extended period</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <span className="text-purple-600 mt-0.5">•</span>
                            <span>Your session timed out for security purposes</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <span className="text-purple-600 mt-0.5">•</span>
                            <span>You may have logged in from another device</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <span className="text-purple-600 mt-0.5">•</span>
                            <span>Your authentication token has expired</span>
                        </li>
                    </ul>
                </div>

                {/* Action Button */}
                <div className="mb-8">
                    <button
                        onClick={handleLogin}
                        className="flex items-center gap-2 px-8 py-4 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition-colors font-medium mx-auto"
                    >
                        <LogIn size={20} />
                        Log In Again
                    </button>
                </div>

                {/* Security Note */}
                <div className="p-6 bg-blue-50 rounded-lg border border-blue-100 max-w-md mx-auto">
                    <div className="flex items-start gap-3">
                        <Shield size={24} className="text-blue-600 flex-shrink-0 mt-0.5" />
                        <div className="text-left">
                            <h3 className="font-bold text-blue-900 mb-1">Security First</h3>
                            <p className="text-sm text-blue-700">
                                We automatically log you out after a period of inactivity
                                to protect your account and sensitive financial information.
                            </p>
                        </div>
                    </div>
                </div>

                {/* Data Safety */}
                <div className="mt-6 p-4 bg-green-50 rounded-lg border border-green-200 max-w-md mx-auto">
                    <p className="text-sm text-green-800 flex items-center justify-center gap-2">
                        <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                            <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd"/>
                        </svg>
                        <span className="font-medium">Your data is safe and secured</span>
                    </p>
                </div>

                {/* Footer */}
                <div className="mt-8 text-sm text-gray-500">
                    <p>You'll be redirected to your previous page after logging in.</p>
                </div>
            </div>
        </div>
    );
}

