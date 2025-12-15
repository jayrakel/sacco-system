import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { Settings, Save, Calendar, Link, AlertTriangle } from 'lucide-react';

export default function AccountingConfig() {
    const [mappings, setMappings] = useState([]);
    const [accounts, setAccounts] = useState([]);
    const [periods, setPeriods] = useState([]);
    const [loading, setLoading] = useState(true);

    const [newPeriod, setNewPeriod] = useState({ name: '', startDate: '', endDate: '', active: true, closed: false });

    useEffect(() => { fetchData(); }, []);

    const fetchData = async () => {
        setLoading(true);
        try {
            const [mapRes, accRes, perRes] = await Promise.all([
                api.get('/api/accounting/config/mappings'),
                api.get('/api/accounting/accounts'),
                api.get('/api/accounting/config/periods')
            ]);
            if (mapRes.data.success) setMappings(mapRes.data.data);
            if (accRes.data.success) setAccounts(accRes.data.data);
            if (perRes.data.success) setPeriods(perRes.data.data);
        } catch (e) { console.error(e); }
        finally { setLoading(false); }
    };

    const handleUpdateMapping = async (mapping) => {
        try {
            await api.put('/api/accounting/config/mappings', mapping);
            alert("Mapping Updated!");
        } catch (e) { alert("Update failed"); }
    };

    const handleCreatePeriod = async (e) => {
        e.preventDefault();
        try {
            await api.post('/api/accounting/config/periods', newPeriod);
            alert("Fiscal Period Created!");
            fetchData();
        } catch (e) { alert("Failed to create period"); }
    };

    const handleTogglePeriod = async (id) => {
        try {
            await api.put(`/api/accounting/config/periods/${id}/toggle`);
            fetchData();
        } catch (e) { alert("Action failed"); }
    };

    if(loading) return <div className="p-10 text-center text-slate-400">Loading Configuration...</div>;

    return (
        <div className="space-y-8 animate-in fade-in">

            {/* GL MAPPINGS SECTION */}
            <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-200">
                <div className="flex items-center gap-2 mb-4 pb-2 border-b border-slate-100">
                    <Link className="text-indigo-600" size={20}/>
                    <h3 className="font-bold text-slate-800">GL Account Mappings</h3>
                </div>
                <p className="text-xs text-slate-500 mb-4">Connect business events to specific Ledger Accounts.</p>

                <div className="grid grid-cols-1 gap-4">
                    {mappings.map((map, idx) => (
                        <div key={idx} className="grid grid-cols-1 md:grid-cols-12 gap-4 items-center bg-slate-50 p-3 rounded-lg border border-slate-200">
                            <div className="md:col-span-3 font-bold text-xs text-slate-700 uppercase">{map.eventName.replace(/_/g, ' ')}</div>

                            <div className="md:col-span-4">
                                <label className="block text-[10px] font-bold text-slate-400 mb-1">Debit Account</label>
                                <select
                                    className="w-full p-2 text-sm border rounded bg-white"
                                    value={map.debitAccountCode}
                                    onChange={(e) => {
                                        const newMap = {...map, debitAccountCode: e.target.value};
                                        const updated = [...mappings]; updated[idx] = newMap; setMappings(updated);
                                    }}
                                >
                                    {accounts.map(a => <option key={a.code} value={a.code}>{a.code} - {a.name}</option>)}
                                </select>
                            </div>

                            <div className="md:col-span-4">
                                <label className="block text-[10px] font-bold text-slate-400 mb-1">Credit Account</label>
                                <select
                                    className="w-full p-2 text-sm border rounded bg-white"
                                    value={map.creditAccountCode}
                                    onChange={(e) => {
                                        const newMap = {...map, creditAccountCode: e.target.value};
                                        const updated = [...mappings]; updated[idx] = newMap; setMappings(updated);
                                    }}
                                >
                                    {accounts.map(a => <option key={a.code} value={a.code}>{a.code} - {a.name}</option>)}
                                </select>
                            </div>

                            <div className="md:col-span-1 text-right">
                                <button onClick={() => handleUpdateMapping(map)} className="p-2 bg-slate-200 text-slate-600 rounded hover:bg-emerald-500 hover:text-white transition"><Save size={16}/></button>
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            {/* FISCAL PERIODS SECTION */}
            <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-200">
                <div className="flex items-center gap-2 mb-4 pb-2 border-b border-slate-100">
                    <Calendar className="text-emerald-600" size={20}/>
                    <h3 className="font-bold text-slate-800">Fiscal Periods</h3>
                </div>

                {/* Create Form */}
                <form onSubmit={handleCreatePeriod} className="flex flex-col md:flex-row gap-3 items-end mb-6 bg-slate-50 p-4 rounded-lg">
                    <div className="flex-1 w-full">
                        <label className="text-xs font-bold text-slate-500">Period Name</label>
                        <input type="text" required placeholder="FY 2024" className="w-full p-2 border rounded" value={newPeriod.name} onChange={e => setNewPeriod({...newPeriod, name: e.target.value})} />
                    </div>
                    <div className="flex-1 w-full">
                        <label className="text-xs font-bold text-slate-500">Start Date</label>
                        <input type="date" required className="w-full p-2 border rounded" value={newPeriod.startDate} onChange={e => setNewPeriod({...newPeriod, startDate: e.target.value})} />
                    </div>
                    <div className="flex-1 w-full">
                        <label className="text-xs font-bold text-slate-500">End Date</label>
                        <input type="date" required className="w-full p-2 border rounded" value={newPeriod.endDate} onChange={e => setNewPeriod({...newPeriod, endDate: e.target.value})} />
                    </div>
                    <button type="submit" className="bg-emerald-600 text-white px-4 py-2 rounded font-bold text-sm hover:bg-emerald-700">Add Period</button>
                </form>

                {/* List */}
                <table className="w-full text-sm text-left">
                    <thead className="bg-slate-50 text-slate-500 font-bold border-b">
                        <tr>
                            <th className="p-3">Name</th>
                            <th className="p-3">Start</th>
                            <th className="p-3">End</th>
                            <th className="p-3">Status</th>
                            <th className="p-3 text-center">Action</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y">
                        {periods.map(p => (
                            <tr key={p.id} className="hover:bg-slate-50">
                                <td className="p-3 font-bold">{p.name}</td>
                                <td className="p-3">{p.startDate}</td>
                                <td className="p-3">{p.endDate}</td>
                                <td className="p-3">
                                    <span className={`px-2 py-1 rounded text-[10px] font-bold uppercase ${p.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                                        {p.active ? 'Active' : 'Closed'}
                                    </span>
                                </td>
                                <td className="p-3 text-center">
                                    <button onClick={() => handleTogglePeriod(p.id)} className="text-blue-600 hover:underline text-xs font-bold">
                                        {p.active ? 'Close' : 'Re-open'}
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}