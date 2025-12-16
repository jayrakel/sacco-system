import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../api';
import {
    Settings, Save, AlertCircle, ArrowLeft, Upload, Image as ImageIcon,
    Banknote, Package, Link, FileText, PiggyBank, Calendar
} from 'lucide-react';

// Import Sub-Components (Ensure these files exist in your project structure)
import LoanProducts from '../../features/loans/components/LoanProducts';
import SavingsProducts from '../../features/savings/components/SavingsProducts';
import AccountingConfig from '../../features/finance/components/AccountingConfig';

export default function SystemSettings() {
    const [activeTab, setActiveTab] = useState('general');
    const navigate = useNavigate();

    // --- GENERAL SETTINGS STATE ---
    const [settings, setSettings] = useState([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState('');

    // Base URL for image display
    const BASE_URL = import.meta.env.VITE_API_URL + "/uploads/settings/";

    useEffect(() => {
        fetchSettings();
    }, []);

    const fetchSettings = async () => {
        try {
            const response = await api.get('/api/settings');
            if (response.data.success) {
                setSettings(response.data.data);
            }
        } catch (error) {
            console.error("Failed to load settings");
        } finally {
            setLoading(false);
        }
    };

    const handleValueChange = (key, newValue) => {
        setSettings(settings.map(s => s.key === key ? { ...s, value: newValue } : s));
    };

    const handleFileUpload = async (key, file) => {
        if (!file) return;

        const formData = new FormData();
        formData.append("file", file);

        try {
            const response = await api.post(`/api/settings/upload/${key}`, formData, {
                headers: { "Content-Type": "multipart/form-data" }
            });

            if (response.data.success) {
                setSettings(settings.map(s => s.key === key ? response.data.data : s));
                setMessage(`Image for ${key.replace(/_/g, ' ')} updated!`);
            }
        } catch (error) {
            setMessage("Failed to upload image.");
        }
    };

    const saveSettings = async () => {
        setSaving(true);
        setMessage('');
        try {
            // Filter out image settings, only save text values here
            const textSettings = settings.filter(s => !s.key.includes('LOGO') && !s.key.includes('FAVICON'));

            await Promise.all(textSettings.map(s =>
                api.put(`/api/settings/${s.key}`, { value: s.value })
            ));
            setMessage('Configuration saved successfully!');
        } catch (error) {
            setMessage('Error saving settings.');
        }
        setSaving(false);
    };

    if (loading) return <div className="p-10 text-center text-slate-400">Loading Configuration...</div>;

    // Categorize Settings for General Tab
    const brandingSettings = settings.filter(s => s.key.includes('SACCO') || s.key.includes('BRAND'));
    const bankSettings = settings.filter(s => s.key.includes('BANK') || s.key.includes('PAYBILL'));

    // --- RENDER ---
    return (
        <div className="min-h-screen bg-slate-50 p-4 sm:p-8 font-sans flex justify-center">
            <div className="max-w-6xl w-full bg-white rounded-xl shadow-lg border border-slate-200 overflow-hidden">

                {/* Header */}
                <div className="bg-slate-900 text-white p-6 flex justify-between items-center">
                    <div className="flex items-center gap-3">
                        <Settings className="text-emerald-400" size={28} />
                        <div>
                            <h2 className="text-xl font-bold">System Configuration</h2>
                            <p className="text-slate-400 text-sm">Manage branding, products, and accounting rules.</p>
                        </div>
                    </div>
                    <button
                        onClick={() => navigate('/admin-dashboard')}
                        className="flex items-center gap-2 text-slate-400 hover:text-white transition"
                    >
                        <ArrowLeft size={20} /> Back
                    </button>
                </div>

                {/* Navigation Tabs */}
                <div className="bg-slate-100 p-2 flex gap-2 overflow-x-auto border-b border-slate-200">
                    <button
                        onClick={() => setActiveTab('general')}
                        className={`px-4 py-2 rounded-lg text-sm font-bold flex items-center gap-2 transition whitespace-nowrap ${activeTab === 'general' ? 'bg-white shadow text-slate-800' : 'text-slate-500 hover:text-slate-700'}`}
                    >
                        <Settings size={16}/> General & Branding
                    </button>
                    <button
                        onClick={() => setActiveTab('products')}
                        className={`px-4 py-2 rounded-lg text-sm font-bold flex items-center gap-2 transition whitespace-nowrap ${activeTab === 'products' ? 'bg-white shadow text-slate-800' : 'text-slate-500 hover:text-slate-700'}`}
                    >
                        <Package size={16}/> Loan & Savings Products
                    </button>
                    <button
                        onClick={() => setActiveTab('accounting')}
                        className={`px-4 py-2 rounded-lg text-sm font-bold flex items-center gap-2 transition whitespace-nowrap ${activeTab === 'accounting' ? 'bg-white shadow text-slate-800' : 'text-slate-500 hover:text-slate-700'}`}
                    >
                        <Link size={16}/> Accounting Rules
                    </button>
                </div>

                <div className="p-8">

                    {/* VIEW 1: GENERAL SETTINGS */}
                    {activeTab === 'general' && (
                        <div className="space-y-8 animate-in fade-in">
                            {message && (
                                <div className={`p-4 rounded-lg flex items-center gap-3 border ${message.includes('Error') || message.includes('Failed') ? 'bg-red-50 text-red-700 border-red-200' : 'bg-green-50 text-green-700 border-green-200'}`}>
                                    <AlertCircle size={20} />
                                    <span>{message}</span>
                                </div>
                            )}

                            {/* Branding Section */}
                            <div>
                                <h3 className="text-lg font-bold text-slate-800 mb-4 border-b pb-2">Branding & Identity</h3>
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                    {brandingSettings.map((setting) => (
                                        <div key={setting.key} className="bg-slate-50 p-4 rounded-lg border border-slate-200">
                                            <label className="block text-xs font-bold text-slate-500 mb-2 uppercase tracking-wide">
                                                {setting.key.replace(/_/g, ' ')}
                                            </label>

                                            {setting.key.includes('LOGO') || setting.key.includes('FAVICON') ? (
                                                <div className="flex items-center gap-4">
                                                    <div className="w-16 h-16 bg-white border rounded-lg flex items-center justify-center overflow-hidden">
                                                        {setting.value ? (
                                                            <img src={`${BASE_URL}${setting.value}`} alt="Logo" className="w-full h-full object-contain" />
                                                        ) : (
                                                            <ImageIcon className="text-slate-300" />
                                                        )}
                                                    </div>
                                                    <div className="flex-1">
                                                        <label className="cursor-pointer bg-white border border-slate-300 hover:bg-slate-50 text-slate-700 px-4 py-2 rounded text-sm flex items-center gap-2 w-fit transition">
                                                            <Upload size={14} /> Upload Image
                                                            <input type="file" accept="image/*" className="hidden" onChange={(e) => handleFileUpload(setting.key, e.target.files[0])} />
                                                        </label>
                                                        <p className="text-xs text-slate-400 mt-1">Recommended: PNG or JPG</p>
                                                    </div>
                                                </div>
                                            ) : (
                                                setting.key.includes('COLOR') ? (
                                                    <div className="flex items-center gap-3">
                                                        <input type="color" value={setting.value || '#000000'} onChange={(e) => handleValueChange(setting.key, e.target.value)} className="w-12 h-12 rounded cursor-pointer border-0 p-0" />
                                                        <input type="text" value={setting.value} onChange={(e) => handleValueChange(setting.key, e.target.value)} className="w-full p-2 border rounded font-mono text-sm" />
                                                    </div>
                                                ) : (
                                                    <input type="text" value={setting.value} onChange={(e) => handleValueChange(setting.key, e.target.value)} className="w-full p-2 border rounded font-bold text-slate-800 focus:ring-2 focus:ring-blue-500 outline-none" />
                                                )
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </div>

                            {/* Banking Section */}
                            <div>
                                <h3 className="text-lg font-bold text-slate-800 mb-4 border-b pb-2 flex items-center gap-2">
                                    <Banknote size={20} className="text-indigo-600"/> Deposit Account Details
                                </h3>
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                    {bankSettings.map((setting) => (
                                        <div key={setting.key} className="bg-indigo-50 p-4 rounded-lg border border-indigo-100">
                                            <label className="block text-xs font-bold text-indigo-800 mb-1 uppercase tracking-wide">
                                                {setting.key.replace(/_/g, ' ')}
                                            </label>
                                            <input
                                                type="text"
                                                value={setting.value}
                                                onChange={(e) => handleValueChange(setting.key, e.target.value)}
                                                className="w-full p-2 border border-indigo-200 rounded font-bold text-slate-800 focus:ring-2 focus:ring-indigo-500 outline-none bg-white"
                                                placeholder={setting.description}
                                            />
                                            <p className="text-xs text-indigo-400 mt-2">{setting.description}</p>
                                        </div>
                                    ))}
                                    {bankSettings.length === 0 && <p className="text-slate-400 italic text-sm">No bank details configured. Restart backend to initialize.</p>}
                                </div>
                            </div>

                            {/* Save Button */}
                            <div className="flex justify-end pt-4 border-t border-slate-200">
                                <button
                                    onClick={saveSettings}
                                    disabled={saving}
                                    className="bg-emerald-600 hover:bg-emerald-700 text-white px-6 py-3 rounded-lg font-bold flex items-center gap-2 transition disabled:opacity-50"
                                >
                                    {saving ? "Saving..." : <><Save size={20} /> Save Changes</>}
                                </button>
                            </div>
                        </div>
                    )}

                    {/* VIEW 2: PRODUCTS */}
                    {activeTab === 'products' && (
                        <div className="space-y-12 animate-in fade-in">
                            <div>
                                <h3 className="text-lg font-bold text-slate-800 mb-4 flex items-center gap-2">
                                    <FileText size={20} className="text-purple-600"/> Loan Products
                                </h3>
                                <LoanProducts />
                            </div>

                            <div className="border-t pt-8">
                                <h3 className="text-lg font-bold text-slate-800 mb-4 flex items-center gap-2">
                                    <PiggyBank size={20} className="text-emerald-600"/> Savings Products
                                </h3>
                                <SavingsProducts />
                            </div>
                        </div>
                    )}

                    {/* VIEW 3: ACCOUNTING */}
                    {activeTab === 'accounting' && (
                        <AccountingConfig />
                    )}

                </div>
            </div>
        </div>
    );
}