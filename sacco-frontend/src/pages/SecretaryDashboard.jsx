import React, { useState, useEffect } from 'react';
import { FileText } from 'lucide-react';
import DashboardHeader from '../components/DashboardHeader';

export default function SecretaryDashboard() {
    const [user, setUser] = useState(null);

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
    }, []);

    return (
        <div className="min-h-screen bg-slate-50 font-sans text-slate-800 pb-12">
            <DashboardHeader user={user} title="Secretariat Portal" />
            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8 space-y-8 animate-in fade-in">
                <div>
                    <h1 className="text-2xl font-bold text-slate-800">Records & Minutes</h1>
                    <p className="text-slate-500 text-sm">Manage meetings and member correspondence.</p>
                </div>
                <div className="bg-white p-16 rounded-xl shadow-sm border border-slate-200 text-center flex flex-col items-center justify-center min-h-[400px]">
                    <div className="p-6 bg-slate-50 rounded-full mb-4">
                        <FileText size={48} className="text-slate-300"/>
                    </div>
                    <h3 className="text-lg font-bold text-slate-700">Document Management System</h3>
                    <p className="text-slate-400 max-w-md mt-2">This module is under construction. It will allow you to upload minutes, manage AGM agendas, and send bulk communications.</p>
                </div>
            </main>
        </div>
    );
}