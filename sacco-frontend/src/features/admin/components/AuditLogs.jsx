import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { ShieldAlert, Search, RefreshCw, User, Monitor, CheckCircle, XCircle, Clock, Filter, ChevronLeft, ChevronRight } from 'lucide-react';

export default function AuditLogs() {
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const [statusFilter, setStatusFilter] = useState('all');
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalItems, setTotalItems] = useState(0);
    const pageSize = 50;

    useEffect(() => {
        fetchLogs();
    }, [currentPage]);

    const fetchLogs = async () => {
        setLoading(true);
        try {
            const res = await api.get('/api/audit', {
                params: { page: currentPage, size: pageSize }
            });
            if (res.data.success) {
                setLogs(res.data.data);
                setTotalPages(res.data.totalPages);
                setTotalItems(res.data.totalItems);
            }
        } catch (error) {
            console.error("Failed to load audit logs", error);
        } finally {
            setLoading(false);
        }
    };

    const filteredLogs = logs.filter(log => {
        const matchesSearch = searchTerm === '' ||
            (log.userName || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
            (log.userEmail || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
            (log.action || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
            (log.description || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
            (log.entityType || '').toLowerCase().includes(searchTerm.toLowerCase());
        
        const matchesStatus = statusFilter === 'all' || log.status === statusFilter;
        
        return matchesSearch && matchesStatus;
    });

    const getStatusIcon = (status) => {
        switch(status) {
            case 'SUCCESS':
                return <CheckCircle size={16} className="text-emerald-500" />;
            case 'FAILURE':
                return <XCircle size={16} className="text-rose-500" />;
            case 'PENDING':
                return <Clock size={16} className="text-amber-500" />;
            default:
                return null;
        }
    };

    const getActionColor = (action) => {
        if (action.includes('DELETE') || action.includes('REJECT')) return 'bg-red-100 text-red-700';
        if (action.includes('APPROVE') || action.includes('CREATE')) return 'bg-green-100 text-green-700';
        if (action.includes('LOGIN') || action.includes('LOGOUT')) return 'bg-blue-100 text-blue-700';
        if (action.includes('UPDATE')) return 'bg-amber-100 text-amber-700';
        return 'bg-slate-100 text-slate-700';
    };

    return (
        <div className="space-y-6 animate-in fade-in">

            {/* Header */}
            <div className="flex flex-col md:flex-row justify-between items-center bg-white p-4 rounded-xl shadow-sm border border-slate-200 gap-4">
                <div>
                    <h2 className="font-bold text-slate-800 flex items-center gap-2">
                        <ShieldAlert className="text-indigo-600"/> Audit & Security Log
                    </h2>
                    <p className="text-xs text-slate-500 mt-1">
                        {totalItems.toLocaleString()} total events tracked
                    </p>
                </div>

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
                    
                    <select
                        value={statusFilter}
                        onChange={(e) => setStatusFilter(e.target.value)}
                        className="px-3 py-2 border rounded-lg text-sm outline-none focus:border-indigo-500 bg-white"
                    >
                        <option value="all">All Status</option>
                        <option value="SUCCESS">Success</option>
                        <option value="FAILURE">Failed</option>
                        <option value="PENDING">Pending</option>
                    </select>
                    
                    <button onClick={fetchLogs} className="bg-slate-100 hover:bg-slate-200 text-slate-600 p-2 rounded-lg transition">
                        <RefreshCw size={18} />
                    </button>
                </div>
            </div>

            {/* Stats */}
            <div className="grid grid-cols-3 gap-4">
                <div className="bg-white p-4 rounded-xl shadow-sm border border-slate-200 text-center">
                    <div className="text-2xl font-bold text-emerald-600">
                        {logs.filter(l => l.status === 'SUCCESS').length}
                    </div>
                    <div className="text-xs text-slate-500 uppercase font-bold">Successful</div>
                </div>
                <div className="bg-white p-4 rounded-xl shadow-sm border border-slate-200 text-center">
                    <div className="text-2xl font-bold text-rose-600">
                        {logs.filter(l => l.status === 'FAILURE').length}
                    </div>
                    <div className="text-xs text-slate-500 uppercase font-bold">Failed</div>
                </div>
                <div className="bg-white p-4 rounded-xl shadow-sm border border-slate-200 text-center">
                    <div className="text-2xl font-bold text-amber-600">
                        {logs.filter(l => l.status === 'PENDING').length}
                    </div>
                    <div className="text-xs text-slate-500 uppercase font-bold">Pending</div>
                </div>
            </div>

            {/* Logs Table */}
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                <table className="w-full text-sm text-left">
                    <thead className="bg-slate-50 text-slate-500 font-bold border-b text-xs uppercase">
                        <tr>
                            <th className="p-4">Status</th>
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
                            <tr><td colSpan="7" className="p-8 text-center text-slate-400">Loading audit trail...</td></tr>
                        ) : filteredLogs.length === 0 ? (
                            <tr><td colSpan="7" className="p-8 text-center text-slate-400">No logs found.</td></tr>
                        ) : (
                            filteredLogs.map(log => (
                                <tr key={log.id} className="hover:bg-slate-50 transition">
                                    <td className="p-4">
                                        {getStatusIcon(log.status)}
                                    </td>
                                    <td className="p-4 text-slate-500 text-xs whitespace-nowrap">
                                        {new Date(log.createdAt).toLocaleString('en-US', {
                                            month: 'short',
                                            day: 'numeric',
                                            hour: '2-digit',
                                            minute: '2-digit',
                                            second: '2-digit'
                                        })}
                                    </td>
                                    <td className="p-4">
                                        <div className="flex items-center gap-2">
                                            <User size={14} className="text-slate-400"/>
                                            <div>
                                                <div className="font-bold text-slate-700">{log.userName || 'System'}</div>
                                                <div className="text-xs text-slate-500 font-normal">{log.userEmail || 'system@sacco.com'}</div>
                                            </div>
                                        </div>
                                    </td>
                                    <td className="p-4">
                                        <span className={`px-2 py-1 rounded text-[10px] font-bold uppercase ${getActionColor(log.action)}`}>
                                            {log.action.replace(/_/g, ' ')}
                                        </span>
                                    </td>
                                    <td className="p-4 text-slate-600 font-mono text-xs">
                                        {log.entityType || 'N/A'} 
                                        {log.entityId && <span className="text-slate-400"> #{log.entityId.substring(0,8)}</span>}
                                    </td>
                                    <td className="p-4 text-slate-600 max-w-md">
                                        <div className="truncate" title={log.description}>
                                            {log.description || 'No details'}
                                        </div>
                                        {log.errorMessage && (
                                            <div className="text-xs text-rose-600 mt-1 truncate" title={log.errorMessage}>
                                                Error: {log.errorMessage}
                                            </div>
                                        )}
                                    </td>
                                    <td className="p-4 text-slate-400 text-xs font-mono">
                                        <div className="flex items-center gap-1">
                                            <Monitor size={12}/> {log.ipAddress || 'N/A'}
                                        </div>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
                <div className="flex justify-center items-center gap-2">
                    <button
                        onClick={() => setCurrentPage(prev => Math.max(0, prev - 1))}
                        disabled={currentPage === 0}
                        className="p-2 bg-white border border-slate-300 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-slate-50 transition"
                    >
                        <ChevronLeft size={18} />
                    </button>
                    
                    <div className="flex items-center gap-1">
                        {[...Array(Math.min(totalPages, 5))].map((_, i) => {
                            let pageNum;
                            if (totalPages <= 5) {
                                pageNum = i;
                            } else if (currentPage < 3) {
                                pageNum = i;
                            } else if (currentPage > totalPages - 3) {
                                pageNum = totalPages - 5 + i;
                            } else {
                                pageNum = currentPage - 2 + i;
                            }
                            
                            return (
                                <button
                                    key={pageNum}
                                    onClick={() => setCurrentPage(pageNum)}
                                    className={`w-10 h-10 rounded-lg transition ${
                                        currentPage === pageNum
                                            ? 'bg-indigo-600 text-white font-bold'
                                            : 'bg-white border border-slate-300 text-slate-600 hover:bg-slate-50'
                                    }`}
                                >
                                    {pageNum + 1}
                                </button>
                            );
                        })}
                    </div>
                    
                    <button
                        onClick={() => setCurrentPage(prev => Math.min(totalPages - 1, prev + 1))}
                        disabled={currentPage === totalPages - 1}
                        className="p-2 bg-white border border-slate-300 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-slate-50 transition"
                    >
                        <ChevronRight size={18} />
                    </button>
                </div>
            )}
        </div>
    );
}