import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Home, ArrowLeft, XCircle, AlertCircle, RefreshCw } from 'lucide-react';

export default function BadRequest() {
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
                        <XCircle size={64} className="text-orange-600" />
                    </div>
                </div>

                {/* Error Message */}
                <div className="space-y-4 mb-8">
                    <h1 className="text-6xl font-bold text-orange-600 mb-2">400</h1>
                    <h2 className="text-3xl font-bold text-gray-800">
                        Bad Request
                    </h2>
                    <p className="text-lg text-gray-600 max-w-md mx-auto">
                        The request you sent didn't quite make sense to our server.
                    </p>
                </div>

                {/* What Went Wrong */}
                <div className="mb-8 p-6 bg-orange-50 rounded-lg border border-orange-100 text-left max-w-md mx-auto">
                    <h3 className="font-bold text-orange-900 mb-3 flex items-center gap-2">
                        <AlertCircle size={20} />
                        What Usually Causes This?
                    </h3>
                    <ul className="space-y-2 text-sm text-orange-900">
                        <li className="flex items-start gap-2">
                            <span className="text-orange-600 mt-0.5">•</span>
                            <span>Some required information might be missing from your request</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <span className="text-orange-600 mt-0.5">•</span>
                            <span>The data format doesn't match what we expected</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <span className="text-orange-600 mt-0.5">•</span>
                            <span>Some validation rules weren't met</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <span className="text-orange-600 mt-0.5">•</span>
                            <span>You might have clicked something twice by accident</span>
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
                        Home
                    </button>
                </div>

                {/* Pro Tips */}
                <div className="p-6 bg-blue-50 rounded-lg border border-blue-100">
                    <h3 className="font-bold text-blue-900 mb-2">How to Fix This:</h3>
                    <ul className="text-sm text-blue-800 space-y-1 text-left">
                        <li>• Double-check all required fields are filled</li>
                        <li>• Make sure dates and numbers are in the correct format</li>
                        <li>• Try refreshing the page and starting over</li>
                        <li>• If the problem persists, contact support</li>
                    </ul>
                </div>
            </div>
        </div>
    );
}

