import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../api';
import { Settings, Save, AlertCircle, ArrowLeft, Upload, Image as ImageIcon } from 'lucide-react';

export default function SystemSettings() {
  const [settings, setSettings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');

  // Base URL for images
  const BASE_URL = "http://localhost:8080/uploads/settings/";

  const navigate = useNavigate();

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

  // ✅ New Handler for Image Uploads
  const handleFileUpload = async (key, file) => {
    if (!file) return;

    const formData = new FormData();
    formData.append("file", file);

    try {
        const response = await api.post(`/api/settings/upload/${key}`, formData, {
            headers: { "Content-Type": "multipart/form-data" }
        });

        if (response.data.success) {
            // Update local state with new filename
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
      // Filter out image keys, we handle those separately
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

  if (loading) return <div className="p-8">Loading configuration...</div>;

  // Separate settings
  const brandingSettings = settings.filter(s => s.key.includes('SACCO'));
  const financialSettings = settings.filter(s => !s.key.includes('SACCO'));

  return (
    <div className="min-h-screen bg-slate-50 p-8 font-sans flex justify-center">
      <div className="max-w-5xl w-full bg-white rounded-xl shadow-lg border border-slate-200 overflow-hidden">

        <div className="bg-slate-900 text-white p-6 flex justify-between items-center">
          <div className="flex items-center gap-3">
            <Settings className="text-emerald-400" size={28} />
            <div>
              <h2 className="text-xl font-bold">System Configuration</h2>
              <p className="text-slate-400 text-sm">Manage branding and financial parameters.</p>
            </div>
          </div>
          <button
            onClick={() => navigate('/admin-dashboard')}
            className="flex items-center gap-2 text-slate-400 hover:text-white transition"
          >
            <ArrowLeft size={20} /> Back
          </button>
        </div>

        {message && (
          <div className={`m-6 p-4 rounded-lg flex items-center gap-3 border-l-4 ${message.includes('Error') || message.includes('Failed') ? 'bg-red-50 text-red-700 border-red-500' : 'bg-green-50 text-green-700 border-green-500'}`}>
            <AlertCircle size={20} />
            <span>{message}</span>
          </div>
        )}

        <div className="p-8 space-y-8">

            {/* 1. BRANDING SECTION */}
            <div>
                <h3 className="text-lg font-bold text-slate-800 mb-4 border-b pb-2">Branding & Identity</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {brandingSettings.map((setting) => (
                        <div key={setting.key} className="bg-slate-50 p-4 rounded-lg border border-slate-200">
                            <label className="block text-xs font-bold text-slate-500 mb-2 uppercase tracking-wide">
                                {setting.key.replace(/_/g, ' ')}
                            </label>

                            {setting.key.includes('LOGO') || setting.key.includes('FAVICON') ? (
                                // File Input for Images
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
                                            <input
                                                type="file"
                                                accept="image/*"
                                                className="hidden"
                                                onChange={(e) => handleFileUpload(setting.key, e.target.files[0])}
                                            />
                                        </label>
                                        <p className="text-xs text-slate-400 mt-1">Recommended: PNG or JPG</p>
                                    </div>
                                </div>
                            ) : (
                                // Standard Text Input
                                <input
                                    type="text"
                                    value={setting.value}
                                    onChange={(e) => handleValueChange(setting.key, e.target.value)}
                                    className="w-full p-2 border rounded font-bold text-slate-800 focus:ring-2 focus:ring-blue-500 outline-none"
                                />
                            )}
                        </div>
                    ))}
                </div>
            </div>

            {/* 2. FINANCIAL SECTION */}
            <div>
                <h3 className="text-lg font-bold text-slate-800 mb-4 border-b pb-2">Financial Parameters</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {financialSettings.map((setting) => (
                        <div key={setting.key} className="bg-slate-50 p-4 rounded-lg border border-slate-200">
                            <label className="block text-xs font-bold text-slate-500 mb-1 uppercase tracking-wide">
                                {setting.key.replace(/_/g, ' ')}
                            </label>
                            <div className="flex gap-2 items-center">
                                <input
                                    type="number"
                                    value={setting.value}
                                    onChange={(e) => handleValueChange(setting.key, e.target.value)}
                                    className="w-full p-2 border rounded font-mono font-bold text-slate-800 focus:ring-2 focus:ring-blue-500 outline-none"
                                />
                                <span className="text-slate-400 text-sm font-semibold">
                                    {setting.key.includes('RATE') ? '%' : 'KES'}
                                </span>
                            </div>
                            <p className="text-xs text-slate-400 mt-2">{setting.description}</p>
                        </div>
                    ))}
                </div>
            </div>

        </div>

        <div className="p-6 bg-slate-50 border-t border-slate-200 flex justify-end">
          <button
            onClick={saveSettings}
            disabled={saving}
            className="bg-emerald-600 hover:bg-emerald-700 text-white px-6 py-3 rounded-lg font-bold flex items-center gap-2 transition disabled:opacity-50"
          >
            {saving ? "Saving..." : <><Save size={20} /> Update Configuration</>}
          </button>
        </div>

      </div>
    </div>
  );
}