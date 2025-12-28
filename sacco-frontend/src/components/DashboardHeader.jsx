import React, { useState, useEffect, useRef } from 'react';
import api from '../api';
import { Wallet, LogOut, Bell, Archive, XCircle, MailOpen, Users, Check, X, User } from 'lucide-react';
import { useNavigate, Link } from 'react-router-dom';
import BrandedSpinner from './BrandedSpinner';
import { useSettings } from '../context/SettingsContext';
import { logoutUser } from '../features/auth/services/authService';

export default function DashboardHeader({ user, title = "SaccoPortal" }) {
    // --- STATE MANAGEMENT ---
    const [notifications, setNotifications] = useState({ unread: [], history: [], archive: [] });
    const [requests, setRequests] = useState([]);
    const [logo, setLogo] = useState(null);
    const [isLoggingOut, setIsLoggingOut] = useState(false);

    // --- HOOKS ---
    const { settings, getImageUrl } = useSettings();
    const navigate = useNavigate();
    const notifRef = useRef(null);
    const reqRef = useRef(null);
    const profileRef = useRef(null); // ✅ NEW REF

    // --- DROPDOWN VISIBILITY ---
    const [showNotifDropdown, setShowNotifDropdown] = useState(false);
    const [showReqDropdown, setShowReqDropdown] = useState(false);
    const [showProfileDropdown, setShowProfileDropdown] = useState(false); // ✅ NEW STATE
    const [showArchive, setShowArchive] = useState(false);

    const onLogout = () => {
        setIsLoggingOut(true);
        setTimeout(() => {
            logoutUser();
            navigate('/');
        }, 1500);
    };

    // --- CLICK OUTSIDE HANDLER ---
    useEffect(() => {
        function handleClickOutside(event) {
            if (notifRef.current && !notifRef.current.contains(event.target)) setShowNotifDropdown(false);
            if (reqRef.current && !reqRef.current.contains(event.target)) setShowReqDropdown(false);
            if (profileRef.current && !profileRef.current.contains(event.target)) setShowProfileDropdown(false); // ✅ CLOSE PROFILE
        }
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    // --- FETCH DATA ---
    useEffect(() => {
        const fetchData = async () => {
            try {
                // 1. Notifications
                const notifRes = await api.get('/api/notifications').catch(() => ({ data: { data: [] } }));
                const rawNotifs = Array.isArray(notifRes.data.data) ? notifRes.data.data : [];
                const unread = rawNotifs.filter(n => !n.read);
                const history = rawNotifs.filter(n => n.read);
                setNotifications({ unread, history, archive: [] });

                // 2. Guarantor Requests
                const reqRes = await api.get('/api/loans/guarantors/requests').catch(() => ({ data: { data: [] } }));
                setRequests(reqRes.data.data || []);

                // 3. Logo (System Settings)
                const settingRes = await api.get('/api/settings').catch(() => ({ data: { data: [] } }));
                const logoSetting = settingRes.data.data ? settingRes.data.data.find(s => s.key === 'SACCO_LOGO') : null;
                if (logoSetting && logoSetting.value) setLogo(getImageUrl(logoSetting.value));

            } catch (err) { console.error("Fetch error", err); }
        };
        fetchData();
        const interval = setInterval(fetchData, 30000);
        return () => clearInterval(interval);
    }, [user, getImageUrl]);

    // --- ACTIONS ---
    const handleMarkAsRead = async (id) => {
        const noteToMove = notifications.unread.find(n => n.id === id);
        if (!noteToMove) return;
        setNotifications(prev => ({
            ...prev,
            unread: prev.unread.filter(n => n.id !== id),
            history: [{ ...noteToMove, read: true }, ...prev.history]
        }));
        await api.patch(`/api/notifications/${id}/read`);
    };

    const respondToRequest = async (requestId, accepted) => {
        const action = accepted ? "accept" : "decline";
        if(!window.confirm(`Confirm you want to ${action} this request?`)) return;
        try {
            await api.post(`/api/loans/guarantors/${requestId}/respond?accepted=${accepted}`);
            setRequests(prev => prev.filter(r => r.requestId !== requestId));
            alert(`Request ${action}ed successfully.`);
        } catch (err) { alert("Action failed"); }
    };

    if (isLoggingOut) {
        const iconUrl = getImageUrl(settings?.SACCO_FAVICON);
        return (
            <div className="fixed inset-0 z-50 flex flex-col items-center justify-center bg-slate-50 gap-6 animate-in fade-in duration-700">
                <BrandedSpinner iconUrl={iconUrl} size="xl" borderColor="border-white-100" />
                <div className="text-center space-y-2">
                    <h2 className="text-2xl font-bold text-slate-800 tracking-tight">{settings?.SACCO_NAME}</h2>
                    <p className="text-slate-400 text-sm font-medium animate-pulse">Securely Logging Out...</p>
                </div>
            </div>
        );
    }

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
                            {notifications.history.length === 0 ? <p className="text-center text-slate-400 text-sm">No archives.</p> : notifications.history.map(n => (
                                <div key={n.id} className="pb-4 border-b border-slate-100">
                                    <p className="text-sm font-bold text-slate-700">{n.title}</p>
                                    <p className="text-sm text-slate-600 whitespace-pre-line">{n.message}</p>
                                    <p className="text-xs text-slate-400 mt-1">{new Date(n.createdAt).toLocaleString()}</p>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            )}

            {/* --- NAVBAR --- */}
            <nav className="bg-white border-b border-slate-200 px-4 sm:px-6 py-4 flex justify-between items-center sticky top-0 z-40 shadow-sm">

                {/* LOGO AREA */}
                <div className="flex items-center gap-3">
                    {logo ? (
                        <img src={logo} alt="Logo" className="h-10 w-auto object-contain" />
                    ) : (
                        <div className="bg-indigo-900 text-white p-2 rounded-lg shadow-lg"><Wallet size={20} /></div>
                    )}
                    <span className="font-bold text-xl hidden sm:block text-slate-800">{title}</span>
                </div>

                {/* ICONS AREA */}
                <div className="flex items-center gap-4">

                    {/* 1. GUARANTOR REQUESTS */}
                    <div className="relative" ref={reqRef}>
                        <button onClick={() => setShowReqDropdown(!showReqDropdown)} className="p-2 relative hover:bg-slate-100 rounded-full transition text-slate-600">
                            <Users size={22} />
                            {requests.length > 0 && <span className="absolute top-0 right-0 w-3 h-3 bg-amber-500 rounded-full border-2 border-white animate-pulse"></span>}
                        </button>
                        {showReqDropdown && (
                             <div className="absolute right-0 mt-3 w-80 bg-white rounded-xl shadow-xl border border-slate-100 overflow-hidden z-50 animate-in fade-in zoom-in-95 duration-200">
                                {/* ... Guarantor Request List (Same as before) ... */}
                                <div className="bg-amber-50 p-3 border-b border-amber-100 flex justify-between items-center">
                                    <span className="text-xs font-bold text-amber-800 uppercase tracking-wider">Guarantor Requests</span>
                                    <button onClick={() => setShowReqDropdown(false)}><XCircle size={16} className="text-amber-400 hover:text-amber-600"/></button>
                                </div>
                                <div className="max-h-64 overflow-y-auto custom-scrollbar">
                                    {requests.length === 0 ? <div className="p-6 text-center text-slate-400 text-xs italic">No pending requests.</div> : requests.map(r => (
                                        <div key={r.requestId} className="p-4 border-b border-slate-50 hover:bg-slate-50 transition-colors">
                                            <p className="text-sm font-bold text-slate-800">{r.applicantName}</p>
                                            <div className="flex gap-2 mt-2">
                                                <button onClick={() => respondToRequest(r.requestId, true)} className="flex-1 bg-emerald-600 text-white text-xs py-1 rounded">Accept</button>
                                                <button onClick={() => respondToRequest(r.requestId, false)} className="flex-1 bg-white border border-red-200 text-red-600 text-xs py-1 rounded">Decline</button>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>

                    {/* 2. NOTIFICATIONS */}
                    <div className="relative" ref={notifRef}>
                        <button onClick={() => setShowNotifDropdown(!showNotifDropdown)} className="p-2 relative hover:bg-slate-100 rounded-full transition text-slate-600">
                            <Bell size={22} />
                            {notifications.unread.length > 0 && <span className="absolute top-0 right-0 w-3 h-3 bg-red-500 rounded-full border-2 border-white"></span>}
                        </button>
                        {showNotifDropdown && (
                             <div className="absolute right-0 mt-3 w-80 bg-white rounded-xl shadow-xl border border-slate-100 overflow-hidden z-50 animate-in fade-in zoom-in-95 duration-200">
                                <div className="bg-slate-50 p-3 border-b border-slate-100 flex justify-between items-center">
                                    <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Notifications</span>
                                    <button onClick={() => { setShowArchive(true); setShowNotifDropdown(false); }} className="text-xs text-indigo-600 font-bold hover:underline flex items-center gap-1">
                                        <Archive size={14}/> History
                                    </button>
                                </div>
                                <div className="max-h-64 overflow-y-auto custom-scrollbar">
                                    {[...notifications.unread, ...notifications.history].length === 0 ?
                                        <div className="p-6 text-center text-slate-400 text-xs italic">No new notifications.</div> :
                                        [...notifications.unread].slice(0,5).map(n => (
                                            <div key={n.id} onClick={() => !n.read && handleMarkAsRead(n.id)} className={`p-4 border-b border-slate-50 hover:bg-slate-50 cursor-pointer transition-colors ${!n.read ? 'bg-indigo-50/40' : ''}`}>
                                                <div className="flex gap-3">
                                                    <div className={`mt-0.5 ${!n.read ? 'text-indigo-600' : 'text-slate-400'}`}>
                                                        {!n.read ? <MailOpen size={16}/> : <Check size={16}/>}
                                                    </div>
                                                    <div>
                                                        <p className={`text-sm ${!n.read ? 'font-bold text-slate-800' : 'text-slate-600'}`}>{n.title}</p>
                                                        <p className="text-xs text-slate-500 mt-0.5 line-clamp-2">{n.message}</p>
                                                    </div>
                                                </div>
                                            </div>
                                        ))
                                    }
                                </div>
                            </div>
                        )}
                    </div>

                    <div className="h-8 w-px bg-slate-200 hidden sm:block"></div>

                    {/* 3. ✅ NEW USER PROFILE DROPDOWN */}
                    <div className="relative" ref={profileRef}>
                        <button 
                            onClick={() => setShowProfileDropdown(!showProfileDropdown)}
                            className="flex items-center gap-2 hover:bg-slate-50 p-1.5 rounded-full transition border border-transparent hover:border-slate-200"
                        >
                            <div className="w-9 h-9 rounded-full bg-indigo-100 flex items-center justify-center text-indigo-700 font-bold overflow-hidden border border-indigo-200">
                                {user?.profileImageUrl ? (
                                    <img src={getImageUrl(user.profileImageUrl)} alt="Profile" className="w-full h-full object-cover" />
                                ) : (
                                    <span>{user?.firstName?.charAt(0)}{user?.lastName?.charAt(0)}</span>
                                )}
                            </div>
                        </button>

                        {showProfileDropdown && (
                            <div className="absolute right-0 mt-3 w-64 bg-white rounded-xl shadow-xl border border-slate-100 overflow-hidden z-50 animate-in fade-in zoom-in-95 duration-200">
                                {/* Header */}
                                <div className="p-4 border-b border-slate-50 bg-slate-50/50">
                                    <p className="text-sm font-bold text-slate-800">{user?.firstName} {user?.lastName}</p>
                                    <p className="text-xs text-slate-500 font-medium uppercase mt-0.5">{user?.role || 'MEMBER'}</p>
                                    <p className="text-xs text-slate-400 truncate">{user?.email}</p>
                                </div>

                                {/* Menu Items */}
                                <div className="p-2">
                                    <Link 
                                        to="/dashboard?tab=profile" 
                                        onClick={() => setShowProfileDropdown(false)}
                                        className="flex items-center gap-3 w-full p-2.5 rounded-lg text-sm font-bold text-slate-600 hover:text-indigo-700 hover:bg-indigo-50 transition"
                                    >
                                        <User size={18}/> My Profile
                                    </Link>
                                    
                                    <button 
                                        onClick={onLogout}
                                        className="flex items-center gap-3 w-full p-2.5 rounded-lg text-sm font-bold text-rose-600 hover:bg-rose-50 transition mt-1"
                                    >
                                        <LogOut size={18}/> Sign Out
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </nav>
        </>
    );
}