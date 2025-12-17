import React, { useState, useEffect, useRef } from 'react';
import api from '../api'; // ✅ FIXED: Correct path (was ../../../api)
import { Wallet, LogOut, Bell, Archive, XCircle, MailOpen, Users, Check, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export default function DashboardHeader({ user, title = "SaccoPortal" }) {
    const navigate = useNavigate();
    const [notifications, setNotifications] = useState([]);
    const [guarantorRequests, setGuarantorRequests] = useState([]);
    const [showNotifications, setShowNotifications] = useState(false);
    const [loading, setLoading] = useState(false);
    const dropdownRef = useRef(null);

    useEffect(() => {
        fetchData();
        const interval = setInterval(fetchData, 30000); // Poll every 30s
        return () => clearInterval(interval);
    }, []);

    const fetchData = () => {
        fetchNotifications();
        fetchGuarantorRequests();
    };

    const fetchNotifications = async () => {
        try {
            const res = await api.get('/api/notifications');
            if (res.data.success) setNotifications(res.data.data);
        } catch (e) { console.error("Notification Fetch Error", e); }
    };

    const fetchGuarantorRequests = async () => {
        try {
            const res = await api.get('/api/loans/guarantors/requests');
            if (res.data.success) setGuarantorRequests(res.data.data);
        } catch (e) { console.error("Request Fetch Error", e); }
    };

    const handleRespond = async (id, accepted) => {
        setLoading(true);
        try {
            await api.post(`/api/loans/guarantors/${id}/respond?accepted=${accepted}`);
            fetchData(); // Refresh both lists
            alert(accepted ? "Request Accepted" : "Request Declined");
        } catch (e) {
            alert(e.response?.data?.message || "Action failed");
        } finally {
            setLoading(false);
        }
    };

    const markRead = async (id) => {
        try {
            await api.patch(`/api/notifications/${id}/read`);
            setNotifications(notifications.map(n => n.id === id ? { ...n, read: true } : n));
        } catch (e) {}
    };

    const unreadCount = notifications.filter(n => !n.read).length + guarantorRequests.length;

    return (
        <header className="bg-white shadow-sm border-b border-slate-200 py-4 px-8 flex justify-between items-center sticky top-0 z-40">

            {/* Title Section */}
            <div className="flex items-center gap-3">
                <div className="bg-indigo-900 p-2 rounded-lg text-white">
                    <Wallet size={24} />
                </div>
                <div>
                    <h1 className="text-xl font-bold text-slate-800">{title}</h1>
                    <p className="text-xs text-slate-500">Welcome back, {user?.firstName}</p>
                </div>
            </div>

            {/* Actions */}
            <div className="flex items-center gap-6">

                {/* Notifications Bell */}
                <div className="relative" ref={dropdownRef}>
                    <button
                        onClick={() => setShowNotifications(!showNotifications)}
                        className="relative p-2 text-slate-500 hover:bg-slate-100 rounded-full transition"
                    >
                        <Bell size={22} />
                        {unreadCount > 0 && (
                            <span className="absolute top-1 right-1 w-4 h-4 bg-red-500 text-white text-[10px] font-bold flex items-center justify-center rounded-full">
                                {unreadCount}
                            </span>
                        )}
                    </button>

                    {/* Dropdown */}
                    {showNotifications && (
                        <div className="absolute right-0 mt-3 w-80 bg-white rounded-xl shadow-xl border border-slate-100 overflow-hidden animate-in fade-in zoom-in-95 duration-200">
                            <div className="bg-indigo-900 p-3 text-white flex justify-between items-center">
                                <span className="font-bold text-sm">Notifications</span>
                                <button onClick={() => setShowNotifications(false)}><XCircle size={16}/></button>
                            </div>

                            <div className="max-h-96 overflow-y-auto">

                                {/* Guarantor Requests Section */}
                                {guarantorRequests.length > 0 && (
                                    <div className="bg-amber-50 border-b border-amber-100">
                                        <div className="p-2 text-[10px] font-bold text-amber-800 uppercase tracking-wide">Action Required</div>
                                        {guarantorRequests.map(req => (
                                            <div key={req.requestId} className="p-3 border-b border-amber-100 last:border-0">
                                                <div className="flex gap-2">
                                                    <Users size={16} className="text-amber-600 mt-1"/>
                                                    <div>
                                                        <p className="text-sm font-bold text-slate-800">Guarantorship Request</p>
                                                        <p className="text-xs text-slate-600 mt-1">
                                                            <span className="font-bold">{req.applicantName}</span> requests you to guarantee <span className="font-mono font-bold">KES {req.guaranteeAmount}</span> for their loan.
                                                        </p>
                                                        <div className="flex gap-2 mt-3">
                                                            <button
                                                                onClick={() => handleRespond(req.requestId, true)}
                                                                disabled={loading}
                                                                className="bg-emerald-600 text-white px-3 py-1 rounded text-xs font-bold flex items-center gap-1 hover:bg-emerald-700"
                                                            >
                                                                <Check size={12}/> Accept
                                                            </button>
                                                            <button
                                                                onClick={() => handleRespond(req.requestId, false)}
                                                                disabled={loading}
                                                                className="bg-red-500 text-white px-3 py-1 rounded text-xs font-bold flex items-center gap-1 hover:bg-red-600"
                                                            >
                                                                <X size={12}/> Decline
                                                            </button>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                )}

                                {/* General Notifications */}
                                {notifications.length === 0 && guarantorRequests.length === 0 ? (
                                    <div className="p-8 text-center text-slate-400 text-sm">No new notifications</div>
                                ) : (
                                    notifications.map(n => (
                                        <div
                                            key={n.id}
                                            onClick={() => markRead(n.id)}
                                            className={`p-3 border-b border-slate-50 hover:bg-slate-50 cursor-pointer transition ${!n.read ? 'bg-indigo-50/50' : ''}`}
                                        >
                                            <div className="flex gap-3">
                                                <div className={`mt-1 ${!n.read ? 'text-indigo-600' : 'text-slate-400'}`}>
                                                    {!n.read ? <MailOpen size={16}/> : <Archive size={16}/>}
                                                </div>
                                                <div>
                                                    <p className={`text-sm ${!n.read ? 'font-bold text-slate-800' : 'text-slate-600'}`}>{n.title}</p>
                                                    <p className="text-xs text-slate-500 mt-0.5 line-clamp-2">{n.message}</p>
                                                    <p className="text-[10px] text-slate-400 mt-2">{new Date(n.createdAt).toLocaleString()}</p>
                                                </div>
                                            </div>
                                        </div>
                                    ))
                                )}
                            </div>
                        </div>
                    )}
                </div>

                <button onClick={() => { localStorage.clear(); window.location.href = '/login'; }} className="text-slate-400 hover:text-red-500 transition" title="Logout">
                    <LogOut size={22} />
                </button>
            </div>
        </header>
    );
}