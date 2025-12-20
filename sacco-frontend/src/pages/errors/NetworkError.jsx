import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Home, RefreshCw, WifiOff, Signal, AlertCircle } from 'lucide-react';

export default function NetworkError() {
    const navigate = useNavigate();

    const handleRefresh = () => {
        window.location.reload();
    };

    const checkConnection = async () => {
        try {
            const response = await fetch('/api/health', { method: 'HEAD' });
            if (response.ok) {
                window.location.reload();
            } else {
                alert('Server is still unreachable. Please try again later.');
            }
        } catch (error) {
            alert('No internet connection. Please check your network settings.');
        }
    };

    return (
        <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
            <div className="max-w-2xl w-full text-center">
                {/* WiFi Off Icon */}
                <div className="mb-8">
                    <div className="w-32 h-32 mx-auto bg-gray-100 rounded-full flex items-center justify-center">
                        <WifiOff size={64} className="text-gray-600" />
                    </div>
                </div>

                {/* Error Message */}
                <div className="space-y-4 mb-8">
                    <h2 className="text-3xl font-bold text-gray-800">
                        No Internet Connection
                    </h2>
                    <p className="text-lg text-gray-600 max-w-md mx-auto">
                        We're having trouble connecting to the server.
                        Please check your internet connection and try again.
                    </p>
                </div>

                {/* Troubleshooting Steps */}
                <div className="mb-8 p-6 bg-blue-50 rounded-lg border border-blue-100 text-left max-w-md mx-auto">
                    <h3 className="font-bold text-blue-900 mb-3 flex items-center gap-2">
                        <AlertCircle size={20} />
                        Troubleshooting Steps:
                    </h3>
                    <ol className="space-y-2 text-sm text-blue-800 list-decimal list-inside">
                        <li>Check your WiFi or mobile data connection</li>
                        <li>Make sure you're not in airplane mode</li>
                        <li>Try turning your WiFi off and on again</li>
                        <li>Restart your router if using WiFi</li>
                        <li>Check if other websites are working</li>
                        <li>Contact your network administrator if the issue persists</li>
                    </ol>
                </div>

                {/* Action Buttons */}
                <div className="flex flex-col sm:flex-row gap-4 justify-center items-center mb-8">
                    <button
                        onClick={checkConnection}
                        className="flex items-center gap-2 px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-medium"
                    >
                        <Signal size={18} />
                        Check Connection
                    </button>

                    <button
                        onClick={handleRefresh}
                        className="flex items-center gap-2 px-6 py-3 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition-colors font-medium"
                    >
                        <RefreshCw size={18} />
                        Retry
                    </button>

                    <button
                        onClick={() => navigate('/dashboard')}
                        className="flex items-center gap-2 px-6 py-3 bg-white text-gray-700 rounded-lg border border-gray-300 hover:bg-gray-50 transition-colors font-medium"
                    >
                        <Home size={18} />
                        Home
                    </button>
                </div>

                {/* Connection Status */}
                <div className="p-4 bg-gray-100 rounded-lg border border-gray-200 max-w-md mx-auto">
                    <div className="flex items-center justify-between text-sm">
                        <span className="text-gray-600">Connection Status:</span>
                        <div className="flex items-center gap-2">
                            <div className="w-2 h-2 bg-red-500 rounded-full"></div>
                            <span className="font-medium text-red-600">Offline</span>
                        </div>
                    </div>
                </div>

                {/* Help Text */}
                <div className="mt-8 text-sm text-gray-500">
                    <p>
                        The page will automatically reload once your connection is restored.
                    </p>
                </div>
            </div>
        </div>
    );
}

