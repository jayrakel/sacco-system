import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { ShieldAlert, Search, RefreshCw, User, Monitor } from 'lucide-react';

export default function AuditLogs() {
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');

    useEffect(() => {
        fetchLogs();
    }, []);

    const fetchLogs = async () => {
        setLoading(true);
        try {
            const res = await api.get('/api/audit');
            if (res.data.success) setLogs(res.data.data);
        } catch (error) {
            console.error("Failed to load audit logs", error);
        } finally {
            setLoading(false);
        }
    };

    const filteredLogs = logs.filter(log =>
        log.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
        log.action.toLowerCase().includes(searchTerm.toLowerCase()) ||
        log.details.toLowerCase().includes(searchTerm.toLowerCase())
    );

    return (
        <div className="space-y-6 animate-in fade-in">

            {/* Header */}
            <div className="flex flex-col md:flex-row justify-between items-center bg-white p-4 rounded-xl shadow-sm border border-slate-200 gap-4">
                <h2 className="font-bold text-slate-800 flex items-center gap-2">
                    <ShieldAlert className="text-indigo-600"/> Audit & Security Log
                </h2>

                <div className="flex gap-2 w-full md:w-auto">
                    <div className="relative flex-1 md:w-64">
                        <Search className="absolute left-3 top-2.5 text-slate-400" size={16} />
                        <input
                            type="text"
                            placeholder="Search logs..."
                            className="w-full pl-9 pr-4 py-2 border rounded-lg text-sm outline-none focus:border-indigo-500"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>
                    <button onClick={fetchLogs} className="bg-slate-100 hover:bg-slate-200 text-slate-600 p-2 rounded-lg transition">
                        <RefreshCw size={18} />
                    </button>
                </div>
            </div>

            {/* Logs Table */}
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                <table className="w-full text-sm text-left">
                    <thead className="bg-slate-50 text-slate-500 font-bold border-b">
                        <tr>
                            <th className="p-4">Timestamp</th>
                            <th className="p-4">User</th>
                            <th className="p-4">Action</th>
                            <th className="p-4">Entity</th>
                            <th className="p-4">Details</th>
                            <th className="p-4">IP Address</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y">
                        {loading ? (
                            <tr><td colSpan="6" className="p-8 text-center text-slate-400">Loading trail...</td></tr>
                        ) : filteredLogs.length === 0 ? (
                            <tr><td colSpan="6" className="p-8 text-center text-slate-400">No logs found.</td></tr>
                        ) : (
                            filteredLogs.map(log => (
                                <tr key={log.id} className="hover:bg-slate-50">
                                    <td className="p-4 text-slate-500 text-xs whitespace-nowrap">
                                        {new Date(log.timestamp).toLocaleString()}
                                    </td>
                                    <td className="p-4 font-bold text-slate-700 flex items-center gap-2">
                                        <User size={14} className="text-slate-400"/> {log.username}
                                    </td>
                                    <td className="p-4">
                                        <span className={`px-2 py-1 rounded text-[10px] font-bold uppercase ${
                                            log.action.includes('DELETE') || log.action.includes('REJECT') ? 'bg-red-100 text-red-700' :
                                            log.action.includes('APPROVE') || log.action.includes('CREATE') ? 'bg-green-100 text-green-700' :
                                            'bg-blue-50 text-blue-700'
                                        }`}>
                                            {log.action.replace(/_/g, ' ')}
                                        </span>
                                    </td>
                                    <td className="p-4 text-slate-600 font-mono text-xs">
                                        {log.entityName} <span className="text-slate-400">#{log.entityId ? log.entityId.substring(0,6) : ''}</span>
                                    </td>
                                    <td className="p-4 text-slate-600 max-w-xs truncate" title={log.details}>
                                        {log.details}
                                    </td>
                                    <td className="p-4 text-slate-400 text-xs font-mono flex items-center gap-1">
                                        <Monitor size={12}/> {log.ipAddress}
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}