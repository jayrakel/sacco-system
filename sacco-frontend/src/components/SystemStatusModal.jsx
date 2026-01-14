import React, { useState, useEffect } from 'react';
import { X, Server, Database, Activity, RefreshCw, CheckCircle, AlertTriangle, HardDrive, Cpu } from 'lucide-react';
import api from '../api';

export default function SystemStatusModal({ isOpen, onClose }) {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true); // Start loading true by default when open
    const [error, setError] = useState(null);

    const fetchDiagnostics = async () => {
        setLoading(true);
        setError(null);
        try {
            // Artificial delay for "realistic" scanning feel
            await new Promise(r => setTimeout(r, 800));
            const res = await api.get('/api/reports/system-diagnostics');
            if (res.data.success) {
                setData(res.data.data);
            } else {
                setError("Failed to retrieve diagnostics.");
            }
        } catch (err) {
            console.error(err);
            setError("System unreachable.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (isOpen) {
            fetchDiagnostics();
        } else {
            // Reset state when closed
            setData(null);
            setLoading(true);
        }
    }, [isOpen]);

    if (!isOpen) return null;

    // Helper for Status Dots
    const StatusIndicator = ({ status }) => (
        <span className={`flex items-center gap-2 text-xs font-bold px-2 py-1 rounded-full ${
            status === 'Connected' || status === 'Active'
            ? 'bg-emerald-100 text-emerald-700'
            : 'bg-rose-100 text-rose-700'
        }`}>
            <span className={`w-2 h-2 rounded-full ${
                status === 'Connected' || status === 'Active' ? 'bg-emerald-500' : 'bg-rose-500'
            }`}></span>
            {status}
        </span>
    );

    return (
        <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-200">
            <div className="bg-white w-full max-w-lg rounded-2xl shadow-2xl overflow-hidden border border-slate-100">

                {/* Header */}
                <div className="bg-slate-50 px-6 py-4 border-b border-slate-100 flex justify-between items-center">
                    <div>
                        <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                            <Activity className="text-indigo-600" size={20} />
                            System Health Monitor
                        </h2>
                        <p className="text-xs text-slate-500">Real-time server diagnostics</p>
                    </div>
                    <button onClick={onClose} className="p-2 hover:bg-slate-200 rounded-full transition">
                        <X size={20} className="text-slate-500" />
                    </button>
                </div>

                {/* Content */}
                <div className="p-6">
                    {loading ? (
                        <div className="flex flex-col items-center justify-center py-12 gap-4">
                            <div className="w-12 h-12 border-4 border-indigo-100 border-t-indigo-600 rounded-full animate-spin"></div>
                            <p className="text-sm font-medium text-slate-500 animate-pulse">Running system diagnostics...</p>
                        </div>
                    ) : error ? (
                        <div className="flex flex-col items-center justify-center py-8 text-rose-500 gap-2">
                            <AlertTriangle size={32} />
                            <p className="font-bold">{error}</p>
                            <button onClick={fetchDiagnostics} className="mt-4 px-4 py-2 bg-slate-100 text-slate-700 rounded-lg text-sm font-bold hover:bg-slate-200">Retry</button>
                        </div>
                    ) : data ? ( // ✅ CHECK IF DATA EXISTS BEFORE RENDERING
                        <div className="space-y-6">

                            {/* 1. Database Health */}
                            <div className="bg-slate-50 p-4 rounded-xl border border-slate-100">
                                <div className="flex justify-between items-center mb-3">
                                    <div className="flex items-center gap-3">
                                        <div className="p-2 bg-white rounded-lg shadow-sm text-blue-600"><Database size={18} /></div>
                                        <div>
                                            <h4 className="text-sm font-bold text-slate-800">Primary Database</h4>
                                            <p className="text-xs text-slate-500">PostgreSQL Connection</p>
                                        </div>
                                    </div>
                                    <StatusIndicator status={data.dbStatus} />
                                </div>
                                <div className="flex items-center gap-4 text-xs text-slate-600 pl-14">
                                    <div className="flex items-center gap-1">
                                        <span className="text-slate-400">Latency:</span>
                                        <span className="font-mono font-bold text-slate-800">{data.dbLatency}</span>
                                    </div>
                                    <div className="flex items-center gap-1">
                                        <span className="text-slate-400">Pool:</span>
                                        <span className="font-mono font-bold text-slate-800">HikariCP</span>
                                    </div>
                                </div>
                            </div>

                            {/* 2. Resources (Memory & Disk) */}
                            <div className="grid grid-cols-2 gap-4">
                                {/* Memory */}
                                <div className="p-4 rounded-xl border border-slate-100 shadow-sm">
                                    <div className="flex items-center gap-2 mb-3 text-slate-500">
                                        <Cpu size={16} />
                                        <span className="text-xs font-bold uppercase">Memory (JVM)</span>
                                    </div>
                                    <h3 className="text-xl font-bold text-slate-800 mb-1">{data.memoryUsed}</h3>
                                    <div className="w-full bg-slate-100 h-2 rounded-full overflow-hidden mb-2">
                                        <div
                                            className={`h-full rounded-full transition-all duration-1000 ${data.memoryUsagePercent > 80 ? 'bg-rose-500' : 'bg-indigo-500'}`}
                                            style={{ width: `${data.memoryUsagePercent}%` }}
                                        ></div>
                                    </div>
                                    <p className="text-xs text-slate-400 flex justify-between">
                                        <span>{data.memoryUsagePercent}% Used</span>
                                        <span>Total: {data.memoryTotal}</span>
                                    </p>
                                </div>

                                {/* Disk */}
                                <div className="p-4 rounded-xl border border-slate-100 shadow-sm">
                                    <div className="flex items-center gap-2 mb-3 text-slate-500">
                                        <HardDrive size={16} />
                                        <span className="text-xs font-bold uppercase">Disk Space</span>
                                    </div>
                                    <h3 className="text-xl font-bold text-slate-800 mb-1">{data.diskUsed}</h3>
                                    <div className="w-full bg-slate-100 h-2 rounded-full overflow-hidden mb-2">
                                        <div
                                            className="h-full bg-emerald-500 rounded-full transition-all duration-1000"
                                            style={{ width: `${data.diskUsagePercent}%` }}
                                        ></div>
                                    </div>
                                    <p className="text-xs text-slate-400 flex justify-between">
                                        <span>{data.diskUsagePercent}% Used</span>
                                        <span>Total: {data.diskTotal}</span>
                                    </p>
                                </div>
                            </div>

                            {/* 3. Services Status */}
                            <div className="space-y-2">
                                <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-2">External Services</h4>
                                <div className="flex justify-between items-center p-3 rounded-lg border border-slate-100 hover:bg-slate-50 transition">
                                    <span className="text-sm font-medium text-slate-700 flex items-center gap-2">
                                        <span className="w-1.5 h-1.5 rounded-full bg-slate-300"></span> Email Service (SMTP)
                                    </span>
                                    <span className="text-xs font-bold text-emerald-600 bg-emerald-50 px-2 py-1 rounded">{data.emailService}</span>
                                </div>
                                <div className="flex justify-between items-center p-3 rounded-lg border border-slate-100 hover:bg-slate-50 transition">
                                    <span className="text-sm font-medium text-slate-700 flex items-center gap-2">
                                        <span className="w-1.5 h-1.5 rounded-full bg-slate-300"></span> SMS Gateway
                                    </span>
                                    <span className="text-xs font-bold text-amber-600 bg-amber-50 px-2 py-1 rounded">{data.smsService}</span>
                                </div>
                            </div>

                        </div>
                    ) : (
                        <div className="text-center py-12 text-slate-400">
                            <p>No data available</p>
                        </div>
                    )}
                </div>

                {/* Footer */}
                <div className="bg-slate-50 px-6 py-4 border-t border-slate-100 flex justify-between items-center">
                    <p className="text-xs text-slate-400 font-mono">
                        Last Check: {data ? new Date(data.timestamp || Date.now()).toLocaleTimeString() : '--:--'}
                    </p>
                    <button
                        onClick={fetchDiagnostics}
                        disabled={loading}
                        className="flex items-center gap-2 bg-white border border-slate-200 text-slate-700 px-4 py-2 rounded-lg text-sm font-bold hover:bg-slate-100 transition shadow-sm disabled:opacity-50"
                    >
                        <RefreshCw size={16} className={loading ? "animate-spin" : ""} />
                        Re-run Diagnostics
                    </button>
                </div>
            </div>
        </div>
    );
}