import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../api';
import {
    Settings, Save, AlertCircle, ArrowLeft, Upload, Image as ImageIcon,
    Banknote, Package, Link, FileText, PiggyBank, Calendar, Sliders, Wrench, RefreshCw, Plus, X
} from 'lucide-react';

// Import Sub-Components
import LoanProducts from '../../features/loans/components/LoanProducts';
import SavingsProducts from '../../features/savings/components/SavingsProducts';
import AccountingConfig from '../../features/finance/components/AccountingConfig';
import DepositProductsManager from '../../features/finance/components/DepositProductsManager';

export default function SystemSettings() {
    const [activeTab, setActiveTab] = useState('general');
    const navigate = useNavigate();

    // --- GENERAL SETTINGS STATE ---
    const [settings, setSettings] = useState([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState('');

    // --- ADD SETTING STATE ---
    const [showAddModal, setShowAddModal] = useState(false);
    const [newSetting, setNewSetting] = useState({ key: '', value: '', description: '' });

    // --- MAINTENANCE STATE ---
    const [recalculating, setRecalculating] = useState(false);
    const [recalculateMessage, setRecalculateMessage] = useState('');

    // Base URL for image display
    const BASE_URL = "http://localhost:8082/uploads/settings/";

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

    const handleAddSetting = async (e) => {
        e.preventDefault();
        try {
            await api.post('/api/settings', newSetting);
            alert("Setting Added!");
            setShowAddModal(false);
            setNewSetting({ key: '', value: '', description: '' });
            fetchSettings();
        } catch (err) {
            alert("Failed to add setting");
        }
    };

    const recalculateShares = async () => {
        if (!window.confirm('This will recalculate all share capital records based on the current SHARE_VALUE setting. Continue?')) {
            return;
        }

        setRecalculating(true);
        setRecalculateMessage('');
        try {
            const response = await api.post('/api/shares/admin/recalculate');
            if (response.data.success) {
                setRecalculateMessage(`✅ ${response.data.message}. ${response.data.data.recordsUpdated} records updated with share value KES ${response.data.data.shareValue}`);
            } else {
                setRecalculateMessage(`❌ ${response.data.message}`);
            }
        } catch (error) {
            setRecalculateMessage('❌ Failed to recalculate shares: ' + (error.response?.data?.message || error.message));
        }
        setRecalculating(false);
    };

    if (loading) return <div className="p-10 text-center text-slate-400">Loading Configuration...</div>;

    // Categorize Settings
    const brandingSettings = settings.filter(s => s.key.includes('SACCO') || s.key.includes('BRAND'));
    const bankSettings = settings.filter(s => s.key.includes('BANK') || s.key.includes('PAYBILL'));

    const operationalSettings = settings.filter(s =>
        s.key.includes('FEE') || s.key.includes('RATE') || s.key.includes('CONTRIBUTION') ||
        s.key.includes('INTEREST') || s.key.includes('GRACE') || s.key.includes('MULTIPLIER') ||
        s.key.includes('VOTING') || s.key.includes('MIN_') || s.key.includes('MAX_') ||
        s.key === 'SHARE_VALUE'
    );

    // ✅ Catch-all for new custom settings that don't fit the above filters
    const miscSettings = settings.filter(s =>
        !brandingSettings.includes(s) &&
        !bankSettings.includes(s) &&
        !operationalSettings.includes(s)
    );

    return (
        <div className="min-h-screen bg-slate-50 p-4 sm:p-8 font-sans flex justify-center">
            <div className="max-w-6xl w-full bg-white rounded-xl shadow-lg border border-slate-200 overflow-hidden">

                {/* Header */}
                <div className="bg-slate-900 text-white p-6 flex justify-between items-center">
                    <div className="flex items-center gap-3">
                        <Settings className="text-emerald-400" size={28} />
                        <div>
                            <h2 className="text-xl font-bold">System Configuration</h2>
                            <p className="text-slate-400 text-sm">Manage branding, products, and system rules.</p>
                        </div>
                    </div>
                    <div className="flex gap-3">
                        <button
                            onClick={() => setShowAddModal(true)}
                            className="flex items-center gap-2 bg-emerald-600 hover:bg-emerald-500 text-white px-3 py-2 rounded text-sm font-bold transition"
                        >
                            <Plus size={16} /> Add Setting
                        </button>
                        <button
                            onClick={() => navigate('/admin-dashboard')}
                            className="flex items-center gap-2 text-slate-400 hover:text-white transition"
                        >
                            <ArrowLeft size={20} /> Back
                        </button>
                    </div>
                </div>

                {/* Navigation Tabs */}
                <div className="bg-slate-100 p-2 flex gap-2 overflow-x-auto border-b border-slate-200">
                    <button onClick={() => setActiveTab('general')} className={`px-4 py-2 rounded-lg text-sm font-bold flex items-center gap-2 transition whitespace-nowrap ${activeTab === 'general' ? 'bg-white shadow text-slate-800' : 'text-slate-500 hover:text-slate-700'}`}>
                        <Settings size={16}/> General & Branding
                    </button>
                    <button onClick={() => setActiveTab('parameters')} className={`px-4 py-2 rounded-lg text-sm font-bold flex items-center gap-2 transition whitespace-nowrap ${activeTab === 'parameters' ? 'bg-white shadow text-slate-800' : 'text-slate-500 hover:text-slate-700'}`}>
                        <Sliders size={16}/> System Parameters
                    </button>
                    <button onClick={() => setActiveTab('products')} className={`px-4 py-2 rounded-lg text-sm font-bold flex items-center gap-2 transition whitespace-nowrap ${activeTab === 'products' ? 'bg-white shadow text-slate-800' : 'text-slate-500 hover:text-slate-700'}`}>
                        <Package size={16}/> Loan & Savings Products
                    </button>
                    <button onClick={() => setActiveTab('deposit-products')} className={`px-4 py-2 rounded-lg text-sm font-bold flex items-center gap-2 transition whitespace-nowrap ${activeTab === 'deposit-products' ? 'bg-white shadow text-slate-800' : 'text-slate-500 hover:text-slate-700'}`}>
                        <PiggyBank size={16}/> Deposit Products
                    </button>
                    <button onClick={() => setActiveTab('accounting')} className={`px-4 py-2 rounded-lg text-sm font-bold flex items-center gap-2 transition whitespace-nowrap ${activeTab === 'accounting' ? 'bg-white shadow text-slate-800' : 'text-slate-500 hover:text-slate-700'}`}>
                        <Link size={16}/> Accounting Rules
                    </button>
                    <button onClick={() => setActiveTab('maintenance')} className={`px-4 py-2 rounded-lg text-sm font-bold flex items-center gap-2 transition whitespace-nowrap ${activeTab === 'maintenance' ? 'bg-white shadow text-slate-800' : 'text-slate-500 hover:text-slate-700'}`}>
                        <Wrench size={16}/> Maintenance
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
                                                    </div>
                                                </div>
                                            ) : (
                                                setting.key.includes('COLOR') ? (
                                                    <div className="flex items-center gap-3">
                                                        <input type="color" value={setting.value || '#000000'} onChange={(e) => handleValueChange(setting.key, e.target.value)} className="w-12 h-12 rounded cursor-pointer border-0 p-0" />
                                                        <input type="text" value={setting.value} onChange={(e) => handleValueChange(setting.key, e.target.value)} className="w-full p-2 border rounded font-mono text-sm" />
                                                    </div>
                                                ) : setting.key.includes('ADDRESS') ? (
                                                    <textarea
                                                        value={setting.value}
                                                        onChange={(e) => handleValueChange(setting.key, e.target.value)}
                                                        rows="3"
                                                        className="w-full p-2 border rounded font-medium text-slate-800 focus:ring-2 focus:ring-blue-500 outline-none resize-y"
                                                    />
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
                                            />
                                        </div>
                                    ))}
                                </div>
                            </div>

                            <div className="flex justify-end pt-4 border-t border-slate-200">
                                <button onClick={saveSettings} disabled={saving} className="bg-emerald-600 hover:bg-emerald-700 text-white px-6 py-3 rounded-lg font-bold flex items-center gap-2 transition disabled:opacity-50">
                                    {saving ? "Saving..." : <><Save size={20} /> Save Changes</>}
                                </button>
                            </div>
                        </div>
                    )}

                    {/* VIEW 2: SYSTEM PARAMETERS */}
                    {activeTab === 'parameters' && (
                        <div className="space-y-8 animate-in fade-in">
                            {message && (
                                <div className={`p-4 rounded-lg flex items-center gap-3 border ${message.includes('Error') || message.includes('Failed') ? 'bg-red-50 text-red-700 border-red-200' : 'bg-green-50 text-green-700 border-green-200'}`}>
                                    <AlertCircle size={20} />
                                    <span>{message}</span>
                                </div>
                            )}

                            <div>
                                <h3 className="text-lg font-bold text-slate-800 mb-4 border-b pb-2 flex items-center gap-2">
                                    <Sliders size={20} className="text-purple-600"/> Operational Parameters
                                </h3>
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                    {operationalSettings.map((setting) => (
                                        <div key={setting.key} className="bg-purple-50 p-4 rounded-lg border border-purple-100">
                                            <label className="block text-xs font-bold text-purple-800 mb-2 uppercase tracking-wide">
                                                {setting.key.replace(/_/g, ' ')}
                                            </label>
                                            <input
                                                type="text"
                                                value={setting.value}
                                                onChange={(e) => handleValueChange(setting.key, e.target.value)}
                                                className="w-full p-2 border border-purple-200 rounded font-bold text-slate-800 focus:ring-2 focus:ring-purple-500 outline-none bg-white"
                                            />
                                        </div>
                                    ))}
                                </div>
                            </div>

                            {/* Miscellaneous Settings (Newly Added) */}
                            {miscSettings.length > 0 && (
                                <div className="mt-8">
                                    <h3 className="text-lg font-bold text-slate-800 mb-4 border-b pb-2 flex items-center gap-2">
                                        <Settings size={20} className="text-gray-600"/> Other Settings
                                    </h3>
                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                        {miscSettings.map((setting) => (
                                            <div key={setting.key} className="bg-gray-50 p-4 rounded-lg border border-gray-200">
                                                <label className="block text-xs font-bold text-gray-700 mb-2 uppercase tracking-wide">
                                                    {setting.key.replace(/_/g, ' ')}
                                                </label>
                                                <div className="text-xs text-gray-500 mb-1">{setting.description}</div>
                                                <input
                                                    type="text"
                                                    value={setting.value}
                                                    onChange={(e) => handleValueChange(setting.key, e.target.value)}
                                                    className="w-full p-2 border border-gray-300 rounded font-medium text-slate-800 focus:ring-2 focus:ring-gray-500 outline-none bg-white"
                                                />
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}

                            <div className="flex justify-end pt-4 border-t border-slate-200 mt-6">
                                <button onClick={saveSettings} disabled={saving} className="bg-emerald-600 hover:bg-emerald-700 text-white px-6 py-3 rounded-lg font-bold flex items-center gap-2 transition disabled:opacity-50">
                                    {saving ? "Saving..." : <><Save size={20} /> Save Changes</>}
                                </button>
                            </div>
                        </div>
                    )}

                    {/* OTHER TABS */}
                    {activeTab === 'products' && (
                        <div className="space-y-12 animate-in fade-in">
                            <div><h3 className="text-lg font-bold text-slate-800 mb-4 flex items-center gap-2"><FileText size={20} className="text-purple-600"/> Loan Products</h3><LoanProducts /></div>
                            <div className="border-t pt-8"><h3 className="text-lg font-bold text-slate-800 mb-4 flex items-center gap-2"><PiggyBank size={20} className="text-emerald-600"/> Savings Products</h3><SavingsProducts /></div>
                        </div>
                    )}

                    {activeTab === 'deposit-products' && <DepositProductsManager />}

                    {activeTab === 'accounting' && <AccountingConfig />}

                    {activeTab === 'maintenance' && (
                        <div className="space-y-6 animate-in fade-in">
                            <div className="bg-slate-50 border border-slate-200 rounded-lg p-6">
                                <h3 className="text-lg font-bold text-slate-800 mb-2 flex items-center gap-2"><Wrench size={20} className="text-blue-600"/> System Maintenance Tools</h3>
                                <div className="bg-white border border-slate-200 rounded-lg p-6 mt-4">
                                    <div className="flex items-start gap-4">
                                        <div className="p-3 bg-amber-100 rounded-lg"><RefreshCw size={24} className="text-amber-600"/></div>
                                        <div className="flex-1">
                                            <h4 className="font-bold text-slate-800 mb-2">Recalculate Share Capital</h4>
                                            <p className="text-sm text-slate-600 mb-4">Recalculates all member share capital records based on the current <strong>SHARE_VALUE</strong>.</p>
                                            {recalculateMessage && <div className={`mb-4 p-3 rounded-lg text-sm ${recalculateMessage.includes('✅') ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>{recalculateMessage}</div>}
                                            <button onClick={recalculateShares} disabled={recalculating} className="flex items-center gap-2 px-4 py-2 bg-amber-600 text-white rounded-lg hover:bg-amber-700 disabled:opacity-50">{recalculating ? "Recalculating..." : "Recalculate All Shares"}</button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}

                </div>
            </div>

            {/* ADD SETTING MODAL */}
            {showAddModal && (
                <div className="fixed inset-0 bg-slate-900/50 flex items-center justify-center p-4 z-50 backdrop-blur-sm">
                    <div className="bg-white rounded-xl p-6 w-full max-w-md shadow-xl">
                        <div className="flex justify-between items-center mb-4">
                            <h3 className="font-bold text-lg">Add New Setting</h3>
                            <button onClick={() => setShowAddModal(false)}><X size={20} className="text-slate-400 hover:text-slate-600"/></button>
                        </div>
                        <form onSubmit={handleAddSetting} className="space-y-4">
                            <div>
                                <label className="block text-xs font-bold text-slate-500 mb-1">Key (Unique Name)</label>
                                <input className="w-full p-2 border rounded uppercase" placeholder="e.g. MPESA_API_KEY" required value={newSetting.key} onChange={e => setNewSetting({...newSetting, key: e.target.value.replace(/\s+/g, '_')})} />
                                <p className="text-[10px] text-slate-400 mt-1">Use SACCO_ prefix for branding, FEE_ for fees.</p>
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-slate-500 mb-1">Value</label>
                                <input className="w-full p-2 border rounded" placeholder="Setting Value" required value={newSetting.value} onChange={e => setNewSetting({...newSetting, value: e.target.value})} />
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-slate-500 mb-1">Description</label>
                                <textarea className="w-full p-2 border rounded" placeholder="What is this setting for?" value={newSetting.description} onChange={e => setNewSetting({...newSetting, description: e.target.value})} />
                            </div>
                            <div className="flex justify-end gap-2 pt-2">
                                <button type="button" onClick={() => setShowAddModal(false)} className="px-4 py-2 text-slate-500 hover:bg-slate-50 rounded">Cancel</button>
                                <button type="submit" className="px-4 py-2 bg-emerald-600 text-white rounded font-bold hover:bg-emerald-700">Save Setting</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}