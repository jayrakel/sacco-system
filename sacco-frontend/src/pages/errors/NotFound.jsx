import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Home, ArrowLeft, Search, HelpCircle } from 'lucide-react';

export default function NotFound() {
    const navigate = useNavigate();

    // Check if user is logged in
    const isLoggedIn = !!localStorage.getItem('sacco_token');
    const homePath = isLoggedIn ? '/dashboard' : '/';

    return (
        <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
            <div className="max-w-2xl w-full text-center">
                {/* 404 Number */}
                <div className="mb-8">
                    <h1 className="text-9xl font-bold text-gray-300 select-none">404</h1>
                    <div className="relative -mt-16">
                        <div className="absolute inset-0 flex items-center justify-center">
                            <Search size={48} className="text-gray-400" />
                        </div>
                    </div>
                </div>

                {/* Error Message */}
                <div className="space-y-4 mb-8">
                    <h2 className="text-3xl font-bold text-gray-800">
                        Page Not Found
                    </h2>
                    <p className="text-lg text-gray-600 max-w-md mx-auto">
                        The page you're looking for doesn't exist or has been moved.
                    </p>
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

                    <button
                        onClick={() => navigate(homePath)}
                        className="flex items-center gap-2 px-6 py-3 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition-colors font-medium"
                    >
                        <Home size={18} />
                        Back to Home
                    </button>
                </div>

                {/* Help Section */}
                <div className="mt-12 p-6 bg-blue-50 rounded-lg border border-blue-100">
                    <div className="flex items-start gap-3">
                        <HelpCircle size={24} className="text-blue-600 flex-shrink-0 mt-0.5" />
                        <div className="text-left">
                            <h3 className="font-bold text-blue-900 mb-1">Need Help?</h3>
                            <p className="text-sm text-blue-700">
                                If you believe this is an error, please contact the system administrator
                                or try accessing the page from the main navigation menu.
                            </p>
                        </div>
                    </div>
                </div>

                {/* Quick Links */}
                {isLoggedIn && (
                    <div className="mt-8">
                        <p className="text-sm text-gray-500 mb-3">Quick Links:</p>
                        <div className="flex flex-wrap gap-3 justify-center">
                            <button
                                onClick={() => navigate('/dashboard')}
                                className="text-sm px-4 py-2 text-gray-600 hover:text-emerald-600 hover:bg-emerald-50 rounded-lg transition"
                            >
                                Dashboard
                            </button>
                            <button
                                onClick={() => navigate('/members')}
                                className="text-sm px-4 py-2 text-gray-600 hover:text-emerald-600 hover:bg-emerald-50 rounded-lg transition"
                            >
                                Members
                            </button>
                            <button
                                onClick={() => navigate('/loans')}
                                className="text-sm px-4 py-2 text-gray-600 hover:text-emerald-600 hover:bg-emerald-50 rounded-lg transition"
                            >
                                Loans
                            </button>
                            <button
                                onClick={() => navigate('/savings')}
                                className="text-sm px-4 py-2 text-gray-600 hover:text-emerald-600 hover:bg-emerald-50 rounded-lg transition"
                            >
                                Savings
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}

