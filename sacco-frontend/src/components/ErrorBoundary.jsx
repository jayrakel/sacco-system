import React from 'react';
import { AlertTriangle, RefreshCw, Home } from 'lucide-react';

class ErrorBoundary extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            hasError: false,
            error: null,
            errorInfo: null
        };
    }

    static getDerivedStateFromError(error) {
        return { hasError: true };
    }

    componentDidCatch(error, errorInfo) {
        console.error('Error caught by boundary:', error, errorInfo);
        this.setState({
            error: error,
            errorInfo: errorInfo
        });

        // Log to error reporting service (e.g., Sentry)
        // logErrorToService(error, errorInfo);
    }

    handleRefresh = () => {
        window.location.reload();
    };

    handleGoHome = () => {
        window.location.href = '/dashboard';
    };

    render() {
        if (this.state.hasError) {
            return (
                <div className="min-h-screen bg-gradient-to-br from-red-50 via-white to-orange-50 flex items-center justify-center p-4">
                    <div className="max-w-2xl w-full text-center">
                        {/* Error Icon */}
                        <div className="mb-8">
                            <div className="w-32 h-32 mx-auto bg-red-100 rounded-full flex items-center justify-center">
                                <AlertTriangle size={64} className="text-red-600 animate-pulse" />
                            </div>
                        </div>

                        {/* Error Message */}
                        <div className="space-y-4 mb-8">
                            <h1 className="text-4xl font-bold text-slate-800">
                                Something Went Wrong
                            </h1>
                            <p className="text-lg text-slate-600 max-w-md mx-auto">
                                The application encountered an unexpected error.
                                We're sorry for the inconvenience.
                            </p>
                        </div>

                        {/* Error Details (Development Mode) */}
                        {process.env.NODE_ENV === 'development' && this.state.error && (
                            <div className="mb-8 p-6 bg-slate-100 rounded-xl border border-slate-200 text-left max-w-2xl mx-auto">
                                <h3 className="font-bold text-slate-900 mb-3">Error Details:</h3>
                                <div className="bg-white p-4 rounded border border-slate-300 overflow-auto">
                                    <p className="text-sm text-red-600 font-mono mb-2">
                                        {this.state.error.toString()}
                                    </p>
                                    {this.state.errorInfo && (
                                        <pre className="text-xs text-slate-600 overflow-auto max-h-40">
                                            {this.state.errorInfo.componentStack}
                                        </pre>
                                    )}
                                </div>
                            </div>
                        )}

                        {/* Action Buttons */}
                        <div className="flex flex-col sm:flex-row gap-4 justify-center items-center">
                            <button
                                onClick={this.handleRefresh}
                                className="flex items-center gap-2 px-6 py-3 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-all font-medium shadow-lg hover:shadow-xl"
                            >
                                <RefreshCw size={18} />
                                Reload Page
                            </button>

                            <button
                                onClick={this.handleGoHome}
                                className="flex items-center gap-2 px-6 py-3 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition-all font-medium shadow-lg hover:shadow-xl"
                            >
                                <Home size={18} />
                                Go to Dashboard
                            </button>
                        </div>

                        {/* Support Info */}
                        <div className="mt-12 p-6 bg-blue-50 rounded-xl border border-blue-100">
                            <h3 className="font-bold text-blue-900 mb-2">Need Help?</h3>
                            <p className="text-sm text-blue-700">
                                If this problem persists, please contact support with the error
                                details shown above. Our team will help you resolve the issue.
                            </p>
                        </div>
                    </div>
                </div>
            );
        }

        return this.props.children;
    }
}

export default ErrorBoundary;

