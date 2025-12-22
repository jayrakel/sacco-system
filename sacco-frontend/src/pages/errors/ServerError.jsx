import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Home, ArrowLeft, RefreshCw, AlertTriangle, Mail } from 'lucide-react';

export default function ServerError() {
    const navigate = useNavigate();

    const handleRefresh = () => {
        window.location.reload();
    };

    return (
        <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
            <div className="max-w-2xl w-full text-center">
                {/* Error Icon */}
                <div className="mb-8">
                    <div className="w-32 h-32 mx-auto bg-orange-100 rounded-full flex items-center justify-center">
                        <AlertTriangle size={64} className="text-orange-600" />
                    </div>
                </div>

                {/* Error Message */}
                <div className="space-y-4 mb-8">
                    <h1 className="text-6xl font-bold text-orange-600 mb-2">500</h1>
                    <h2 className="text-3xl font-bold text-gray-800">
                        Internal Server Error
                    </h2>
                    <p className="text-lg text-gray-600 max-w-md mx-auto">
                        Something went wrong on our end.
                        Our team has been notified and we're working on it.
                    </p>
                </div>

                {/* What Happened */}
                <div className="mb-8 p-6 bg-red-50 rounded-lg border border-red-100 text-left max-w-md mx-auto">
                    <h3 className="font-bold text-red-900 mb-3">What Happened?</h3>
                    <ul className="space-y-2 text-sm text-red-800">
                        <li className="flex items-start gap-2">
                            <span className="text-red-600 mt-0.5">•</span>
                            <span>The server encountered an unexpected condition</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <span className="text-red-600 mt-0.5">•</span>
                            <span>This is a temporary issue and should be resolved soon</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <span className="text-red-600 mt-0.5">•</span>
                            <span>Your data is safe and no information was lost</span>
                        </li>
                    </ul>
                </div>

                {/* Action Buttons */}
                <div className="flex flex-col sm:flex-row gap-4 justify-center items-center mb-8">
                    <button
                        onClick={handleRefresh}
                        className="flex items-center gap-2 px-6 py-3 bg-orange-600 text-white rounded-lg hover:bg-orange-700 transition-colors font-medium"
                    >
                        <RefreshCw size={18} />
                        Try Again
                    </button>

                    <button
                        onClick={() => navigate(-1)}
                        className="flex items-center gap-2 px-6 py-3 bg-white text-gray-700 rounded-lg border border-gray-300 hover:bg-gray-50 transition-colors font-medium"
                    >
                        <ArrowLeft size={18} />
                        Go Back
                    </button>

                    <button
                        onClick={() => navigate('/dashboard')}
                        className="flex items-center gap-2 px-6 py-3 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition-colors font-medium"
                    >
                        <Home size={18} />
                        Dashboard
                    </button>
                </div>

                {/* Support Info */}
                <div className="p-6 bg-blue-50 rounded-lg border border-blue-100">
                    <div className="flex items-start gap-3">
                        <Mail size={24} className="text-blue-600 flex-shrink-0 mt-0.5" />
                        <div className="text-left">
                            <h3 className="font-bold text-blue-900 mb-1">Still Having Issues?</h3>
                            <p className="text-sm text-blue-700 mb-3">
                                If the problem persists, please contact our support team
                                with the following information:
                            </p>
                            <div className="bg-white p-3 rounded border border-blue-200">
                                <p className="text-xs text-gray-600 font-mono">
                                    Error Code: 500<br />
                                    Time: {new Date().toLocaleString()}<br />
                                    Page: {window.location.pathname}
                                </p>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Status */}
                <div className="mt-6 text-sm text-gray-500">
                    <p>Our technical team has been automatically notified of this issue.</p>
                </div>
            </div>
        </div>
    );
}

