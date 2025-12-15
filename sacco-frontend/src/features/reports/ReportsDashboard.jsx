import React, { useState, useEffect } from 'react';
import api from '../../api';
import { FileText, AlertTriangle, Search, User, Download, FileBarChart } from 'lucide-react';

export default function ReportsDashboard() {
    const [view, setView] = useState('aging'); // aging | statement
    const [loading, setLoading] = useState(false);

    // Data States
    const [agingReport, setAgingReport] = useState([]);
    const [members, setMembers] = useState([]);
    const [selectedMember, setSelectedMember] = useState('');
    const [statement, setStatement] = useState([]);

    useEffect(() => {
        if (view === 'aging') fetchAgingReport();
        if (view === 'statement') fetchMembers();
    }, [view]);

    const fetchAgingReport = async () => {
        setLoading(true);
        try {
            const res = await api.get('/api/reports/loan-aging');
            if (res.data.success) setAgingReport(res.data.data);
        } catch (e) { console.error(e); }
        finally { setLoading(false); }
    };

    const fetchMembers = async () => {
        try {
            const res = await api.get('/api/members/active');
            if (res.data.success) setMembers(res.data.data);
        } catch (e) { console.error(e); }
    };

    const handleGenerateStatement = async () => {
        if (!selectedMember) return;
        setLoading(true);
        try {
            const res = await api.get(`/api/reports/member-statement/${selectedMember}`);
            if (res.data.success) setStatement(res.data.data);
        } catch (e) { alert("Failed to fetch statement"); }
        finally { setLoading(false); }
    };

    return (
        <div className="space-y-6 animate-in fade-in">

            {/* Header / Tabs */}
            <div className="bg-white p-4 rounded-xl shadow-sm border border-slate-200 flex flex-col md:flex-row justify-between items-center gap-4">
                <h2 className="font-bold text-slate-800 flex items-center gap-2">
                    <FileBarChart className="text-indigo-600"/> Operational Reports
                </h2>
                <div className="flex bg-slate-100 p-1 rounded-lg">
                    <button onClick={() => setView('aging')} className={`px-4 py-2 text-xs font-bold rounded transition ${view === 'aging' ? 'bg-white shadow text-slate-800' : 'text-slate-500'}`}>Loan Aging / Risk</button>
                    <button onClick={() => setView('statement')} className={`px-4 py-2 text-xs font-bold rounded transition ${view === 'statement' ? 'bg-white shadow text-slate-800' : 'text-slate-500'}`}>Member Statements</button>
                </div>
            </div>

            {/* LOAN AGING VIEW */}
            {view === 'aging' && (
                <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                    <div className="p-5 border-b border-slate-100 bg-red-50 flex justify-between items-center">
                        <div>
                            <h3 className="font-bold text-red-800 flex items-center gap-2"><AlertTriangle size={18}/> Portfolio At Risk</h3>
                            <p className="text-xs text-red-600">Overdue loans categorized by delinquency duration.</p>
                        </div>
                    </div>
                    <table className="w-full text-sm text-left">
                        <thead className="bg-slate-50 text-slate-500 font-bold border-b">
                            <tr>
                                <th className="p-4">Loan No</th>
                                <th className="p-4">Member</th>
                                <th className="p-4">Days Overdue</th>
                                <th className="p-4">Category</th>
                                <th className="p-4 text-right">Outstanding (KES)</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y">
                            {agingReport.map((item, idx) => (
                                <tr key={idx} className="hover:bg-slate-50">
                                    <td className="p-4 font-mono text-slate-500">{item.loanNumber}</td>
                                    <td className="p-4 font-bold text-slate-700">{item.memberName}</td>
                                    <td className="p-4 text-red-600 font-bold">{item.daysOverdue} Days</td>
                                    <td className="p-4"><span className="bg-red-100 text-red-700 px-2 py-1 rounded text-[10px] font-bold uppercase">{item.category}</span></td>
                                    <td className="p-4 text-right font-mono font-bold">{Number(item.amountOutstanding).toLocaleString()}</td>
                                </tr>
                            ))}
                            {agingReport.length === 0 && !loading && <tr><td colSpan="5" className="p-8 text-center text-slate-400 italic">No overdue loans found. Great job!</td></tr>}
                        </tbody>
                    </table>
                </div>
            )}

            {/* MEMBER STATEMENT VIEW */}
            {view === 'statement' && (
                <div className="space-y-4">
                    <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-200 flex flex-col md:flex-row gap-4 items-end">
                        <div className="w-full md:w-1/2">
                            <label className="block text-xs font-bold text-slate-500 mb-1">Select Member</label>
                            <select className="w-full p-2 border rounded-lg" value={selectedMember} onChange={e => setSelectedMember(e.target.value)}>
                                <option value="">-- Choose Member --</option>
                                {members.map(m => <option key={m.id} value={m.id}>{m.firstName} {m.lastName} - {m.memberNumber}</option>)}
                            </select>
                        </div>
                        <button onClick={handleGenerateStatement} disabled={!selectedMember || loading} className="px-6 py-2.5 bg-slate-900 text-white rounded-lg font-bold text-sm hover:bg-slate-800 disabled:opacity-50">
                            {loading ? 'Generating...' : 'Generate Statement'}
                        </button>
                    </div>

                    {statement.length > 0 && (
                        <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                            <div className="p-4 border-b bg-slate-50 flex justify-between items-center">
                                <h3 className="font-bold text-slate-700">Statement of Account</h3>
                                <button onClick={() => window.print()} className="text-blue-600 text-xs font-bold flex items-center gap-1 hover:underline"><Download size={14}/> Print / PDF</button>
                            </div>
                            <table className="w-full text-sm text-left">
                                <thead className="bg-slate-50 text-slate-500 font-bold border-b">
                                    <tr>
                                        <th className="p-3">Date</th>
                                        <th className="p-3">Ref</th>
                                        <th className="p-3">Description</th>
                                        <th className="p-3 text-right">Amount</th>
                                        <th className="p-3 text-right">Balance</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y">
                                    {statement.map((tx, i) => (
                                        <tr key={i} className="hover:bg-slate-50">
                                            <td className="p-3 text-slate-500">{new Date(tx.date).toLocaleDateString()}</td>
                                            <td className="p-3 font-mono text-xs">{tx.reference}</td>
                                            <td className="p-3 text-slate-700">{tx.description}</td>
                                            <td className={`p-3 text-right font-bold ${tx.amount < 0 ? 'text-red-600' : 'text-emerald-600'}`}>
                                                {Number(tx.amount).toLocaleString()}
                                            </td>
                                            <td className="p-3 text-right font-mono font-bold text-slate-800">
                                                {Number(tx.runningBalance).toLocaleString()}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}