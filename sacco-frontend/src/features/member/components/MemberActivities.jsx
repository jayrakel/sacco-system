import React, { useState, useEffect } from 'react';
import { Activity, CheckCircle, XCircle, Clock, Search, Filter } from 'lucide-react';
import api from '../../../api';
import BrandedSpinner from '../../../components/BrandedSpinner';

export default function MemberActivities() {
    const [activities, setActivities] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState('all');
    const [searchTerm, setSearchTerm] = useState('');
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalItems, setTotalItems] = useState(0);

    useEffect(() => {
        fetchActivities();
    }, [currentPage]);

    const fetchActivities = async () => {
        try {
            setLoading(true);
            const response = await api.get('/api/audit/my-logs', {
                params: { page: currentPage, size: 20 }
            });
            
            if (response.data.success) {
                setActivities(response.data.data);
                setTotalPages(response.data.totalPages);
                setTotalItems(response.data.totalItems);
            }
        } catch (error) {
            console.error('Failed to fetch activities:', error);
        } finally {
            setLoading(false);
        }
    };

    const getStatusIcon = (status) => {
        switch(status) {
            case 'SUCCESS':
                return <CheckCircle size={18} className="text-emerald-500" />;
            case 'FAILURE':
                return <XCircle size={18} className="text-rose-500" />;
            case 'PENDING':
                return <Clock size={18} className="text-amber-500" />;
            default:
                return <Activity size={18} className="text-slate-400" />;
        }
    };

    const getActionColor = (action) => {
        const colors = {
            LOGIN: 'text-blue-600 bg-blue-50',
            LOGOUT: 'text-slate-600 bg-slate-50',
            CREATE: 'text-emerald-600 bg-emerald-50',
            UPDATE: 'text-amber-600 bg-amber-50',
            DELETE: 'text-rose-600 bg-rose-50',
            APPROVE: 'text-green-600 bg-green-50',
            REJECT: 'text-red-600 bg-red-50',
            VIEW: 'text-indigo-600 bg-indigo-50',
            DEPOSIT: 'text-teal-600 bg-teal-50',
            WITHDRAW: 'text-orange-600 bg-orange-50',
        };
        return colors[action] || 'text-slate-600 bg-slate-50';
    };

    const formatDate = (dateString) => {
        const date = new Date(dateString);
        const now = new Date();
        const diffInSeconds = Math.floor((now - date) / 1000);
        
        if (diffInSeconds < 60) return 'Just now';
        if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)}m ago`;
        if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)}h ago`;
        if (diffInSeconds < 604800) return `${Math.floor(diffInSeconds / 86400)}d ago`;
        
        return date.toLocaleDateString('en-US', { 
            month: 'short', 
            day: 'numeric', 
            year: date.getFullYear() !== now.getFullYear() ? 'numeric' : undefined 
        }) + ' at ' + date.toLocaleTimeString('en-US', { 
            hour: '2-digit', 
            minute: '2-digit' 
        });
    };

    const filteredActivities = activities.filter(activity => {
        const matchesSearch = searchTerm === '' || 
            activity.action.toLowerCase().includes(searchTerm.toLowerCase()) ||
            activity.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
            activity.entityType?.toLowerCase().includes(searchTerm.toLowerCase());
        
        const matchesFilter = filter === 'all' || activity.status === filter;
        
        return matchesSearch && matchesFilter;
    });

    if (loading && activities.length === 0) {
        return (
            <div className="flex justify-center items-center h-64">
                <BrandedSpinner />
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
                <div className="flex items-center gap-3 mb-6">
                    <div className="p-3 bg-indigo-100 rounded-xl">
                        <Activity className="text-indigo-600" size={24} />
                    </div>
                    <div>
                        <h2 className="text-2xl font-bold text-slate-800">Recent Activities</h2>
                        <p className="text-slate-500 text-sm">Track all your account activities and transactions</p>
                    </div>
                </div>

                {/* Filters and Search */}
                <div className="flex flex-col sm:flex-row gap-4">
                    <div className="flex-1 relative">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
                        <input
                            type="text"
                            placeholder="Search activities..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full pl-10 pr-4 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                        />
                    </div>
                    <div className="flex gap-2">
                        <button
                            onClick={() => setFilter('all')}
                            className={`px-4 py-2.5 rounded-lg font-medium transition-all ${
                                filter === 'all' 
                                    ? 'bg-indigo-600 text-white shadow-md' 
                                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                            }`}
                        >
                            All
                        </button>
                        <button
                            onClick={() => setFilter('SUCCESS')}
                            className={`px-4 py-2.5 rounded-lg font-medium transition-all ${
                                filter === 'SUCCESS' 
                                    ? 'bg-emerald-600 text-white shadow-md' 
                                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                            }`}
                        >
                            Success
                        </button>
                        <button
                            onClick={() => setFilter('FAILURE')}
                            className={`px-4 py-2.5 rounded-lg font-medium transition-all ${
                                filter === 'FAILURE' 
                                    ? 'bg-rose-600 text-white shadow-md' 
                                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                            }`}
                        >
                            Failed
                        </button>
                    </div>
                </div>

                {/* Stats */}
                <div className="mt-6 grid grid-cols-3 gap-4">
                    <div className="text-center p-3 bg-slate-50 rounded-lg">
                        <div className="text-2xl font-bold text-slate-800">{totalItems}</div>
                        <div className="text-sm text-slate-500">Total Activities</div>
                    </div>
                    <div className="text-center p-3 bg-emerald-50 rounded-lg">
                        <div className="text-2xl font-bold text-emerald-600">
                            {activities.filter(a => a.status === 'SUCCESS').length}
                        </div>
                        <div className="text-sm text-emerald-600">Successful</div>
                    </div>
                    <div className="text-center p-3 bg-rose-50 rounded-lg">
                        <div className="text-2xl font-bold text-rose-600">
                            {activities.filter(a => a.status === 'FAILURE').length}
                        </div>
                        <div className="text-sm text-rose-600">Failed</div>
                    </div>
                </div>
            </div>

            {/* Activities List */}
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                {filteredActivities.length === 0 ? (
                    <div className="text-center py-12">
                        <Activity className="mx-auto text-slate-300 mb-3" size={48} />
                        <p className="text-slate-500 font-medium">No activities found</p>
                        <p className="text-slate-400 text-sm">Your account activities will appear here</p>
                    </div>
                ) : (
                    <div className="divide-y divide-slate-100">
                        {filteredActivities.map((activity, index) => (
                            <div 
                                key={activity.id || index} 
                                className="p-5 hover:bg-slate-50 transition-colors"
                            >
                                <div className="flex items-start gap-4">
                                    <div className="flex-shrink-0 pt-1">
                                        {getStatusIcon(activity.status)}
                                    </div>
                                    
                                    <div className="flex-1 min-w-0">
                                        <div className="flex items-start justify-between gap-3 mb-2">
                                            <div className="flex items-center gap-2 flex-wrap">
                                                <span className={`px-3 py-1 rounded-full text-xs font-bold ${getActionColor(activity.action)}`}>
                                                    {activity.action}
                                                </span>
                                                {activity.entityType && (
                                                    <span className="text-xs text-slate-500 bg-slate-100 px-2 py-1 rounded">
                                                        {activity.entityType}
                                                    </span>
                                                )}
                                            </div>
                                            <span className="text-sm text-slate-400 whitespace-nowrap">
                                                {formatDate(activity.createdAt)}
                                            </span>
                                        </div>
                                        
                                        <p className="text-slate-700 text-sm mb-1">
                                            {activity.description || 'No description available'}
                                        </p>
                                        
                                        {activity.errorMessage && (
                                            <div className="mt-2 p-2 bg-rose-50 border border-rose-200 rounded text-xs text-rose-600">
                                                <strong>Error:</strong> {activity.errorMessage}
                                            </div>
                                        )}
                                        
                                        {activity.ipAddress && (
                                            <div className="mt-2 flex items-center gap-3 text-xs text-slate-400">
                                                <span>IP: {activity.ipAddress}</span>
                                                {activity.entityId && (
                                                    <span>ID: {activity.entityId}</span>
                                                )}
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
                <div className="flex justify-center items-center gap-2">
                    <button
                        onClick={() => setCurrentPage(prev => Math.max(0, prev - 1))}
                        disabled={currentPage === 0}
                        className="px-4 py-2 bg-white border border-slate-300 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-slate-50 transition"
                    >
                        Previous
                    </button>
                    
                    <div className="flex items-center gap-1">
                        {[...Array(totalPages)].map((_, i) => (
                            <button
                                key={i}
                                onClick={() => setCurrentPage(i)}
                                className={`w-10 h-10 rounded-lg transition ${
                                    currentPage === i
                                        ? 'bg-indigo-600 text-white font-bold'
                                        : 'bg-white border border-slate-300 text-slate-600 hover:bg-slate-50'
                                }`}
                            >
                                {i + 1}
                            </button>
                        ))}
                    </div>
                    
                    <button
                        onClick={() => setCurrentPage(prev => Math.min(totalPages - 1, prev + 1))}
                        disabled={currentPage === totalPages - 1}
                        className="px-4 py-2 bg-white border border-slate-300 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-slate-50 transition"
                    >
                        Next
                    </button>
                </div>
            )}
        </div>
    );
}
