import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../api';
import {
    Settings, Save, AlertCircle, ArrowLeft, Upload, Image as ImageIcon,
    Banknote, Package, Link, FileText, PiggyBank, Calculator, RefreshCw
} from 'lucide-react';

// Import Sub-Components
import LoanProducts from '../../features/loans/components/LoanProducts';
import SavingsProducts from '../../features/savings/components/SavingsProducts';
import AccountingConfig from '../../features/finance/components/AccountingConfig';

export default function SystemSettings() {
    const [activeTab, setActiveTab] = useState('general');
    const navigate = useNavigate();

    // --- STATE ---
    const [settings, setSettings] = useState([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState('');

    const BASE_URL = import.meta.env.VITE_API_URL + "/uploads/settings/";

    useEffect(() => {
        fetchSettings();
    }, []);

    const fetchSettings = async () => {
        try {
            const response = await api.get('/api/settings');
            if (response.data.success) {
                const data = response.data.data;
                setSettings(data);

                // Ensure defaults exist for Loan Logic
                if (!data.find(s => s.key === 'LOAN_LIMIT_MULTIPLIER'))
                    setSettings(prev => [...prev, { key: 'LOAN_LIMIT_MULTIPLIER', value: '3.0' }]);
                if (!data.find(s => s.key === 'LOAN_GRACE_PERIOD_WEEKS'))
                    setSettings(prev => [...prev, { key: 'LOAN_GRACE_PERIOD_WEEKS', value: '1' }]);
            }
        } catch (error) {
            console.error("Failed to load settings");
        } finally {
            setLoading(false);
        }
    };

    const handleValueChange = (key, newValue) => {
        setSettings(prev => prev.map(s => s.key === key ? { ...s, value: newValue } : s));
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
            const textSettings = settings.filter(s => !s.key.includes('LOGO') && !s.key.includes('FAVICON'));
            await Promise.all(textSettings.map(s =>
                api.post('/api/settings', { key: s.key, value: s.value }) // Use POST to ensure create/update
            ));
            setMessage('Configuration saved successfully!');
        } catch (error) {
            setMessage('Error saving settings.');
        }
        setSaving(false);
    };

    if (loading) return <div className="p-10 text-center text-slate-400">Loading Configuration...</div>;

    const brandingSettings = settings.filter(s => s.key.includes('SACCO') || s.key.includes('BRAND'));
    const bankSettings = settings.filter(s => s.key.includes('BANK') || s.key.includes('PAYBILL'));

    return (
        <div className="min-h-screen bg-slate-50 p-4 sm:p-8 font-sans flex justify-center">
            <div className="max-w-6xl w-full bg-white rounded-xl shadow-lg border border-slate-200 overflow-hidden">

                {/* Header */}
                <div className="bg-slate-900 text-white p-6 flex justify-between items-center">
                    <div className="flex items-center gap-3">
                        <Settings className="text-emerald-400" size={28} />
                        <div>
                            <h2 className="text-xl font-bold">System Configuration</h2>
                            <p className="text-slate-400 text-sm">Manage global variables and logic.</p>
                        </div>
                    </div>
                    <button onClick={() => navigate('/admin-dashboard')} className="flex items-center gap-2 text-slate-400 hover:text-white transition">
                        <ArrowLeft size={20} /> Back
                    </button>
                </div>

                {/* Tabs */}
                <div className="bg-slate-100 p-2 flex gap-2 overflow-x-auto border-b border-slate-200">
                    {['general', 'loanLogic', 'products', 'accounting'].map(tab => (
                        <button
                            key={tab}
                            onClick={() => setActiveTab(tab)}
                            className={`px-4 py-2 rounded-lg text-sm font-bold flex items-center gap-2 transition whitespace-nowrap capitalize ${activeTab === tab ? 'bg-white shadow text-slate-800' : 'text-slate-500 hover:text-slate-700'}`}
                        >
                            {tab === 'general' && <Settings size={16}/>}
                            {tab === 'loanLogic' && <Calculator size={16}/>}
                            {tab === 'products' && <Package size={16}/>}
                            {tab === 'accounting' && <Link size={16}/>}
                            {tab === 'loanLogic' ? 'Loan Logic' : tab}
                        </button>
                    ))}
                </div>

                <div className="p-8">
                    {message && (
                        <div className={`mb-6 p-4 rounded-lg flex items-center gap-3 border ${message.includes('Error') || message.includes('Failed') ? 'bg-red-50 text-red-700 border-red-200' : 'bg-green-50 text-green-700 border-green-200'}`}>
                            <AlertCircle size={20} />
                            <span>{message}</span>
                        </div>
                    )}

                    {/* VIEW 1: GENERAL */}
                    {activeTab === 'general' && (
                        <div className="space-y-8 animate-in fade-in">
                            <div>
                                <h3 className="text-lg font-bold text-slate-800 mb-4 border-b pb-2">Branding & Identity</h3>
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                    {brandingSettings.map((setting) => (
                                        <div key={setting.key} className="bg-slate-50 p-4 rounded-lg border border-slate-200">
                                            <label className="block text-xs font-bold text-slate-500 mb-2 uppercase tracking-wide">{setting.key.replace(/_/g, ' ')}</label>
                                            {setting.key.includes('LOGO') || setting.key.includes('FAVICON') ? (
                                                <div className="flex items-center gap-4">
                                                    <div className="w-16 h-16 bg-white border rounded-lg flex items-center justify-center overflow-hidden">
                                                        {setting.value ? <img src={`${BASE_URL}${setting.value}`} alt="Logo" className="w-full h-full object-contain" /> : <ImageIcon className="text-slate-300" />}
                                                    </div>
                                                    <label className="cursor-pointer bg-white border border-slate-300 hover:bg-slate-50 text-slate-700 px-4 py-2 rounded text-sm flex items-center gap-2 transition">
                                                        <Upload size={14} /> Upload Image
                                                        <input type="file" accept="image/*" className="hidden" onChange={(e) => handleFileUpload(setting.key, e.target.files[0])} />
                                                    </label>
                                                </div>
                                            ) : (
                                                <input type="text" value={setting.value} onChange={(e) => handleValueChange(setting.key, e.target.value)} className="w-full p-2 border rounded font-bold text-slate-800 focus:ring-2 focus:ring-blue-500 outline-none" />
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </div>

                            {/* Banking Section */}
                            <div>
                                <h3 className="text-lg font-bold text-slate-800 mb-4 border-b pb-2 flex items-center gap-2"><Banknote size={20} className="text-indigo-600"/> Banking & Paybill</h3>
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                    {bankSettings.map((setting) => (
                                        <div key={setting.key} className="bg-indigo-50 p-4 rounded-lg border border-indigo-100">
                                            <label className="block text-xs font-bold text-indigo-800 mb-1 uppercase tracking-wide">{setting.key.replace(/_/g, ' ')}</label>
                                            <input type="text" value={setting.value} onChange={(e) => handleValueChange(setting.key, e.target.value)} className="w-full p-2 border border-indigo-200 rounded font-bold text-slate-800 focus:ring-2 focus:ring-indigo-500 outline-none bg-white" />
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </div>
                    )}

                    {/* VIEW 2: LOAN LOGIC (NEW) */}
                    {activeTab === 'loanLogic' && (
                        <div className="space-y-6 animate-in fade-in max-w-2xl">
                             <div className="bg-amber-50 p-4 rounded-xl border border-amber-200 mb-6">
                                <h4 className="font-bold text-amber-800 flex items-center gap-2 mb-1"><AlertCircle size={18}/> Critical Configuration</h4>
                                <p className="text-sm text-amber-700">These settings directly affect loan qualification limits. Changes apply immediately to new applications.</p>
                            </div>

                            <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
                                <label className="block text-sm font-bold text-slate-700 mb-2">Loan Limit Multiplier (x Savings)</label>
                                <div className="flex items-center gap-4">
                                    <input
                                        type="number"
                                        step="0.1"
                                        value={settings.find(s => s.key === 'LOAN_LIMIT_MULTIPLIER')?.value || '3.0'}
                                        onChange={(e) => handleValueChange('LOAN_LIMIT_MULTIPLIER', e.target.value)}
                                        className="w-32 p-3 border border-slate-300 rounded-lg font-mono font-bold text-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                                    />
                                    <p className="text-sm text-slate-500">
                                        Example: If a member has <strong>KES 10,000</strong> savings, they can borrow up to <strong>KES {(10000 * (settings.find(s => s.key === 'LOAN_LIMIT_MULTIPLIER')?.value || 3)).toLocaleString()}</strong>.
                                    </p>
                                </div>
                            </div>

                            <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
                                <label className="block text-sm font-bold text-slate-700 mb-2">Default Grace Period (Weeks)</label>
                                <div className="flex items-center gap-4">
                                    <input
                                        type="number"
                                        value={settings.find(s => s.key === 'LOAN_GRACE_PERIOD_WEEKS')?.value || '1'}
                                        onChange={(e) => handleValueChange('LOAN_GRACE_PERIOD_WEEKS', e.target.value)}
                                        className="w-32 p-3 border border-slate-300 rounded-lg font-mono font-bold text-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                                    />
                                    <p className="text-sm text-slate-500">Period after disbursement before the first repayment installment is due.</p>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* VIEW 3 & 4: PRODUCTS & ACCOUNTING */}
                    {activeTab === 'products' && <div className="animate-in fade-in space-y-12"><LoanProducts /><div className="border-t pt-8"><SavingsProducts /></div></div>}
                    {activeTab === 'accounting' && <AccountingConfig />}

                    {/* GLOBAL SAVE BUTTON */}
                    {(activeTab === 'general' || activeTab === 'loanLogic') && (
                        <div className="mt-8 pt-6 border-t border-slate-200 flex justify-end">
                            <button onClick={saveSettings} disabled={saving} className="bg-slate-900 hover:bg-emerald-600 text-white px-8 py-3 rounded-xl font-bold flex items-center gap-2 transition disabled:opacity-50 shadow-lg">
                                {saving ? <><RefreshCw className="animate-spin" size={20}/> Saving...</> : <><Save size={20} /> Save Configuration</>}
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}