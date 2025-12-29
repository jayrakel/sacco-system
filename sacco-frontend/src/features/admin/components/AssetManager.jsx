import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { Briefcase, Plus, Save } from 'lucide-react';

export default function AssetManager() {
    const [assets, setAssets] = useState([]);
    const [showModal, setShowModal] = useState(false);
    const [form, setForm] = useState({ name: '', serialNumber: '', purchaseCost: '', purchaseDate: '', status: 'ACTIVE' });

    useEffect(() => {
        api.get('/api/assets').then(res => setAssets(res.data.data)).catch(console.error);
    }, []);

    const handleSave = async (e) => {
        e.preventDefault();
        try {
            // FIX: Send 'form' instead of 'formData'
            // Ensure numeric values are actually numbers if the backend expects it, 
            // though your backend does parsing from strings, so strings are fine.
            await api.post('/api/assets/register', {
                ...form,
                // Add defaults for missing fields if you don't add inputs for them
                category: form.category || 'GENERAL', 
                usefulLifeYears: form.usefulLifeYears || 5 
            });
            
            alert("Asset Saved!");
            setShowModal(false);
            const res = await api.get('/api/assets');
            setAssets(res.data.data);
        } catch (err) { 
            console.error(err); // Log the error to see details
            alert("Failed to save asset: " + (err.response?.data?.message || err.message)); 
        }
    };

    return (
        <div className="space-y-6 animate-in fade-in">
            <div className="flex justify-between items-center bg-white p-4 rounded-xl shadow-sm border border-slate-200">
                <h2 className="font-bold text-slate-800 flex items-center gap-2"><Briefcase className="text-blue-600"/> Asset Registry</h2>
                <button onClick={() => setShowModal(true)} className="bg-slate-900 text-white px-3 py-2 rounded-lg text-xs font-bold flex items-center gap-2 hover:bg-slate-800"><Plus size={14}/> Add Asset</button>
            </div>

            <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                <table className="w-full text-sm text-left">
                    <thead className="bg-slate-50 text-slate-500 font-bold border-b">
                        <tr><th className="p-4">Name</th><th className="p-4">Serial</th><th className="p-4 text-right">Cost</th><th className="p-4">Status</th></tr>
                    </thead>
                    <tbody className="divide-y">
                        {assets.map(a => (
                            <tr key={a.id} className="hover:bg-slate-50">
                                <td className="p-4 font-bold text-slate-700">{a.name}</td>
                                <td className="p-4 font-mono text-xs">{a.serialNumber}</td>
                                <td className="p-4 text-right">KES {Number(a.purchaseCost).toLocaleString()}</td>
                                <td className="p-4"><span className="bg-green-100 text-green-700 px-2 py-1 rounded text-xs font-bold">{a.status}</span></td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {showModal && (
                <div className="fixed inset-0 bg-slate-900/50 flex items-center justify-center p-4 z-50 backdrop-blur-sm">
                    <div className="bg-white rounded-xl p-6 w-full max-w-md shadow-xl">
                        <h3 className="font-bold mb-4">Record New Asset</h3>
                        <form onSubmit={handleSave} className="space-y-3">
                            <input className="w-full p-2 border rounded" placeholder="Asset Name" required onChange={e => setForm({...form, name: e.target.value})} />
                            <input className="w-full p-2 border rounded" placeholder="Serial Number" onChange={e => setForm({...form, serialNumber: e.target.value})} />
                            <input className="w-full p-2 border rounded" type="number" placeholder="Cost" required onChange={e => setForm({...form, purchaseCost: e.target.value})} />
                            <input className="w-full p-2 border rounded" type="date" required onChange={e => setForm({...form, purchaseDate: e.target.value})} />
                            <div className="flex justify-end gap-2 mt-4">
                                <button type="button" onClick={() => setShowModal(false)} className="px-4 py-2 text-slate-500">Cancel</button>
                                <button type="submit" className="px-4 py-2 bg-emerald-600 text-white rounded font-bold">Save</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}