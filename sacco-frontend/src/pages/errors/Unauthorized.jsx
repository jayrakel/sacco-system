import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Home, ArrowLeft, Lock, Shield, LogOut } from 'lucide-react';

export default function Unauthorized() {
    const navigate = useNavigate();

    // Check if user is logged in
    const isLoggedIn = !!localStorage.getItem('sacco_token');

    const handleLogout = () => {
        localStorage.removeItem('sacco_token');
        localStorage.removeItem('sacco_user');
        navigate('/');
    };

    return (
        <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
            <div className="max-w-2xl w-full text-center">
                {/* Lock Icon */}
                <div className="mb-8">
                    <div className="w-32 h-32 mx-auto bg-red-100 rounded-full flex items-center justify-center">
                        <Lock size={64} className="text-red-600" />
                    </div>
                </div>

                {/* Error Message */}
                <div className="space-y-4 mb-8">
                    <h1 className="text-6xl font-bold text-red-600 mb-2">403</h1>
                    <h2 className="text-3xl font-bold text-gray-800">
                        Access Denied
                    </h2>
                    <p className="text-lg text-gray-600 max-w-md mx-auto">
                        You don't have permission to access this page.
                        This area is restricted to authorized users only.
                    </p>
                </div>

                {/* Reasons */}
                <div className="mb-8 p-6 bg-amber-50 rounded-lg border border-amber-100 text-left max-w-md mx-auto">
                    <h3 className="font-bold text-amber-900 mb-3 flex items-center gap-2">
                        <Shield size={20} />
                        Possible Reasons:
                    </h3>
                    <ul className="space-y-2 text-sm text-amber-800">
                        <li className="flex items-start gap-2">
                            <span className="text-amber-600 mt-0.5">•</span>
                            <span>Your role doesn't have access to this feature</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <span className="text-amber-600 mt-0.5">•</span>
                            <span>You need to be logged in with appropriate permissions</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <span className="text-amber-600 mt-0.5">•</span>
                            <span>Your session may have expired</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <span className="text-amber-600 mt-0.5">•</span>
                            <span>This feature is restricted to administrators only</span>
                        </li>
                    </ul>
                </div>

                {/* Action Buttons */}
                <div className="flex flex-col sm:flex-row gap-4 justify-center items-center mb-8">
                    <button
                        onClick={() => navigate(-1)}
                        className="flex items-center gap-2 px-6 py-3 bg-white text-gray-700 rounded-lg border border-gray-300 hover:bg-gray-50 transition-colors font-medium"
                    >
                        <ArrowLeft size={18} />
                        Go Back
                    </button>

                    {isLoggedIn ? (
                        <button
                            onClick={() => navigate('/dashboard')}
                            className="flex items-center gap-2 px-6 py-3 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition-colors font-medium"
                        >
                            <Home size={18} />
                            Dashboard
                        </button>
                    ) : (
                        <button
                            onClick={() => navigate('/')}
                            className="flex items-center gap-2 px-6 py-3 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition-colors font-medium"
                        >
                            <Home size={18} />
                            Login
                        </button>
                    )}

                    {isLoggedIn && (
                        <button
                            onClick={handleLogout}
                            className="flex items-center gap-2 px-6 py-3 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors font-medium"
                        >
                            <LogOut size={18} />
                            Logout
                        </button>
                    )}
                </div>

                {/* Contact Admin */}
                <div className="p-6 bg-blue-50 rounded-lg border border-blue-100">
                    <h3 className="font-bold text-blue-900 mb-2">Need Access?</h3>
                    <p className="text-sm text-blue-700">
                        If you believe you should have access to this page,
                        please contact your system administrator or SACCO management.
                    </p>
                </div>
            </div>
        </div>
    );
}

