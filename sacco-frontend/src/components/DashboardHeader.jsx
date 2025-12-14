import React, { useState, useEffect, useRef } from 'react';
import api from '../api';
import { Wallet, LogOut, Bell, Archive, XCircle, MailOpen, Users, Check, X } from 'lucide-react'; // ✅ Replaced Handshake with Users
import { useNavigate } from 'react-router-dom';

export default function DashboardHeader({ user, title = "SaccoPortal" }) {
    const [notifications, setNotifications] = useState({ unread: [], history: [], archive: [] });
    const [requests, setRequests] = useState([]);
    const [logo, setLogo] = useState(null);
    const [showNotifDropdown, setShowNotifDropdown] = useState(false);
    const [showReqDropdown, setShowReqDropdown] = useState(false);
    const [showArchive, setShowArchive] = useState(false);

    const notifRef = useRef(null);
    const reqRef = useRef(null);
    const navigate = useNavigate();

    const onLogout = () => {
        localStorage.removeItem('sacco_token');
        localStorage.removeItem('sacco_user');
        navigate('/');
    };

    useEffect(() => {
        function handleClickOutside(event) {
            if (notifRef.current && !notifRef.current.contains(event.target)) setShowNotifDropdown(false);
            if (reqRef.current && !reqRef.current.contains(event.target)) setShowReqDropdown(false);
        }
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    useEffect(() => {
        const fetchData = async () => {
            try {
                // Notifications
                const notifRes = await api.get('/api/loan/notifications').catch(() => ({ data: [] }));
                const rawNotifs = notifRes.data;
                if (Array.isArray(rawNotifs)) setNotifications({ unread: rawNotifs, history: [], archive: [] });
                else if (rawNotifs && typeof rawNotifs === 'object') setNotifications({ unread: rawNotifs.unread || [], history: rawNotifs.history || [], archive: rawNotifs.archive || [] });

                // Guarantor Requests
                const reqRes = await api.get('/api/loan/guarantors/requests').catch(() => ({ data: [] }));
                setRequests(reqRes.data);

                // Logo
                const settingRes = await api.get('/api/settings').catch(() => ({ data: [] }));
                const logoSetting = settingRes.data.data ? settingRes.data.data.find(s => s.key === 'SACCO_LOGO') : null;

                if (logoSetting && logoSetting.value) {
                    setLogo(`http://localhost:8080/uploads/settings/${logoSetting.value}`);
                }

            } catch (err) { console.error("Fetch error", err); }
        };
        fetchData();
    }, [user]);

    const handleMarkAsRead = async (id) => {
        const noteToMove = notifications.unread.find(n => n.id === id);
        if (!noteToMove) return;
        setNotifications(prev => ({ ...prev, unread: prev.unread.filter(n => n.id !== id), history: [{...noteToMove, is_read: true}, ...prev.history] }));
        await api.put(`/api/loan/notifications/${id}/read`);
    };

    const respondToRequest = async (requestId, decision) => {
        if(!window.confirm(`Confirm you want to ${decision} this request?`)) return;
        try {
            await api.post('/api/loan/guarantors/respond', { requestId, decision });
            setRequests(prev => prev.filter(r => r.id !== requestId));
            alert(`Request ${decision.toLowerCase()} successfully.`);
        } catch (err) { alert("Action failed"); }
    };

    return (
        <>
            {/* --- ARCHIVE MODAL --- */}
            {showArchive && (
                <div className="fixed inset-0 bg-slate-900/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-[80vh] flex flex-col">
                        <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50 rounded-t-2xl">
                            <h3 className="font-bold text-lg flex items-center gap-2 text-slate-700"><Archive size={20} className="text-slate-400"/> Archive</h3>
                            <button onClick={() => setShowArchive(false)}><XCircle size={24} className="text-slate-400 hover:text-red-500"/></button>
                        </div>
                        <div className="p-6 overflow-y-auto custom-scrollbar space-y-4">
                            {notifications.archive.length === 0 ? <p className="text-center text-slate-400 text-sm">No archives.</p> : notifications.archive.map(n => (
                                <div key={n.id} className="pb-4 border-b border-slate-100"><p className="text-sm text-slate-600 whitespace-pre-line">{n.message}</p><p className="text-xs text-slate-400 mt-1">{new Date(n.created_at).toLocaleDateString()}</p></div>
                            ))}
                        </div>
                    </div>
                </div>
            )}

            {/* --- NAVBAR --- */}
            <nav className="bg-white border-b border-slate-200 px-4 sm:px-6 py-4 flex justify-between items-center sticky top-0 z-40">
                <div className="flex items-center gap-3">
                    {logo ? (
                        <img src={logo} alt="Logo" className="h-10 w-auto object-contain" />
                    ) : (
                        <div className="bg-emerald-600 text-white p-2 rounded-lg shadow-lg"><Wallet size={20} /></div>
                    )}
                    <span className="font-bold text-xl hidden sm:block text-slate-800">{title}</span>
                </div>

                <div className="flex items-center gap-4">

                    {/* GUARANTOR REQUESTS ICON */}
                    <div className="relative" ref={reqRef}>
                        <button onClick={() => setShowReqDropdown(!showReqDropdown)} className="p-2 relative hover:bg-slate-100 rounded-full transition text-slate-600">
                            <Users size={20} /> {/* ✅ Changed Icon */}
                            {requests.length > 0 && <span className="absolute top-1 right-1 w-2.5 h-2.5 bg-amber-500 rounded-full border-2 border-white animate-pulse"></span>}
                        </button>
                        {showReqDropdown && (
                            <div className="absolute right-0 mt-3 w-80 bg-white rounded-xl shadow-xl border border-slate-100 overflow-hidden z-50">
                                <div className="bg-amber-50 p-3 border-b border-amber-100"><span className="text-xs font-bold text-amber-700 uppercase">Guarantor Requests</span></div>
                                <div className="max-h-64 overflow-y-auto custom-scrollbar">
                                    {requests.length === 0 ? <div className="p-6 text-center text-slate-400 text-xs italic">No pending requests.</div> : requests.map(r => (
                                        <div key={r.id} className="p-4 border-b border-slate-50 hover:bg-slate-50">
                                            <p className="text-sm font-bold text-slate-700">{r.applicant_name}</p>
                                            <p className="text-xs text-slate-500 mb-3">Needs guarantee for loan of KES {parseInt(r.amount_requested).toLocaleString()}</p>
                                            <div className="flex gap-2">
                                                <button onClick={() => respondToRequest(r.id, 'ACCEPTED')} className="flex-1 bg-emerald-600 text-white py-1 rounded text-xs font-bold flex items-center justify-center gap-1"><Check size={12}/> Accept</button>
                                                <button onClick={() => respondToRequest(r.id, 'DECLINED')} className="flex-1 bg-red-100 text-red-600 py-1 rounded text-xs font-bold flex items-center justify-center gap-1 hover:bg-red-200"><X size={12}/> Decline</button>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>

                    {/* NOTIFICATIONS BELL */}
                    <div className="relative" ref={notifRef}>
                        <button onClick={() => setShowNotifDropdown(!showNotifDropdown)} className="p-2 relative hover:bg-slate-100 rounded-full transition text-slate-600">
                            <Bell size={20} />
                            {notifications.unread.length > 0 && <span className="absolute top-1 right-1 w-2.5 h-2.5 bg-red-500 rounded-full border-2 border-white"></span>}
                        </button>
                        {showNotifDropdown && (
                            <div className="absolute right-0 mt-3 w-80 bg-white rounded-xl shadow-xl border border-slate-100 overflow-hidden z-50">
                                <div className="bg-slate-50 p-3 border-b border-slate-100 flex justify-between items-center">
                                    <span className="text-xs font-bold text-slate-500 uppercase">Recent History</span>
                                    <button onClick={() => { setShowArchive(true); setShowNotifDropdown(false); }} className="text-xs text-blue-600 font-bold hover:underline flex items-center gap-1"><Archive size={12}/> Archive</button>
                                </div>
                                <div className="max-h-64 overflow-y-auto custom-scrollbar">
                                    {[...notifications.unread, ...notifications.history].length === 0 ? <div className="p-6 text-center text-slate-400 text-xs italic">No notifications.</div> :
                                        [...notifications.unread, ...notifications.history].slice(0,5).map(n => (
                                            <div key={n.id} onClick={() => !n.is_read && handleMarkAsRead(n.id)} className={`p-4 border-b border-slate-50 hover:bg-slate-50 cursor-pointer ${!n.is_read ? 'bg-blue-50/50' : ''}`}>
                                                <p className={`text-xs text-slate-600 line-clamp-2 mb-1 ${!n.is_read ? 'font-bold' : ''}`}>{n.message}</p>
                                                <p className="text-[10px] text-slate-400 text-right">{new Date(n.created_at).toLocaleDateString()}</p>
                                            </div>
                                        ))
                                    }
                                </div>
                            </div>
                        )}
                    </div>

                    <div className="h-8 w-px bg-slate-200 hidden sm:block"></div>
                    <div className="text-right hidden sm:block"><p className="text-xs text-slate-400 font-medium uppercase tracking-wider">{user?.role}</p><p className="text-sm font-bold text-slate-700">{user?.firstName} {user?.lastName}</p></div>
                    <button onClick={onLogout} className="bg-slate-100 hover:bg-slate-200 text-slate-600 px-4 py-2 rounded-lg text-sm font-medium flex items-center gap-2 transition"><LogOut size={16}/></button>
                </div>
            </nav>
        </>
    );
}