import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { CreditCard, PlusCircle, CheckCircle, Clock, XCircle } from 'lucide-react';

export default function MemberLoans({ user }) {
    const [loans, setLoans] = useState([]);
    const [products, setProducts] = useState([]);
    const [showApply, setShowApply] = useState(false);
    const [loading, setLoading] = useState(true);

    // Application Form State
    const [selectedProduct, setSelectedProduct] = useState('');
    const [amount, setAmount] = useState('');
    const [duration, setDuration] = useState('');

    useEffect(() => {
        if(user?.memberId) fetchLoans();
        fetchProducts();
    }, [user]);

    const fetchLoans = async () => {
        try {
            const res = await api.get(`/api/loans/member/${user.memberId}`);
            if(res.data.success) setLoans(res.data.data);
            setLoading(false);
        } catch (e) { console.error(e); setLoading(false); }
    };

    const fetchProducts = async () => {
        try {
            const res = await api.get('/api/loans/products');
            if(res.data.success) setProducts(res.data.data);
        } catch (e) { console.error(e); }
    };

    const handleApply = async (e) => {
        e.preventDefault();
        try {
            // Using URLSearchParams as Controller expects @RequestParam
            const params = new URLSearchParams();
            params.append('memberId', user.memberId);
            params.append('productId', selectedProduct);
            params.append('amount', amount);
            params.append('duration', duration);

            await api.post('/api/loans/apply', params);
            alert("Application Submitted Successfully!");
            setShowApply(false);
            fetchLoans();
        } catch (e) {
            alert(e.response?.data?.message || "Application Failed");
        }
    };

    const getStatusBadge = (status) => {
        switch(status) {
            case 'APPROVED': return <span className="bg-emerald-100 text-emerald-700 px-2 py-1 rounded text-xs font-bold flex items-center gap-1"><CheckCircle size={12}/> Approved</span>;
            case 'PENDING': return <span className="bg-amber-100 text-amber-700 px-2 py-1 rounded text-xs font-bold flex items-center gap-1"><Clock size={12}/> Pending</span>;
            case 'REJECTED': return <span className="bg-red-100 text-red-700 px-2 py-1 rounded text-xs font-bold flex items-center gap-1"><XCircle size={12}/> Rejected</span>;
            case 'DISBURSED': return <span className="bg-blue-100 text-blue-700 px-2 py-1 rounded text-xs font-bold flex items-center gap-1"><CreditCard size={12}/> Active</span>;
            default: return <span className="bg-slate-100 text-slate-600 px-2 py-1 rounded text-xs font-bold">{status}</span>;
        }
    };

    if(loading) return <div className="p-8 text-center text-slate-400">Loading Loans...</div>;

    return (
        <div className="space-y-6 animate-in fade-in">
            <div className="flex justify-between items-center">
                <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2"><CreditCard className="text-indigo-600"/> My Loans</h2>
                <button onClick={() => setShowApply(true)} className="bg-indigo-900 text-white px-4 py-2 rounded-xl text-sm font-bold flex items-center gap-2 hover:bg-indigo-800 transition">
                    <PlusCircle size={16}/> Apply for Loan
                </button>
            </div>

            <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                <table className="w-full text-sm text-left">
                    <thead className="bg-slate-50 text-slate-500 font-bold border-b">
                        <tr>
                            <th className="p-4">Loan No</th>
                            <th className="p-4 text-right">Principal</th>
                            <th className="p-4 text-right">Balance</th>
                            <th className="p-4">Status</th>
                            <th className="p-4 text-right">Next Payment</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y">
                        {loans.map(loan => (
                            <tr key={loan.id} className="hover:bg-slate-50">
                                <td className="p-4 font-mono text-slate-500">{loan.loanNumber}</td>
                                <td className="p-4 text-right font-bold">KES {Number(loan.principalAmount).toLocaleString()}</td>
                                <td className="p-4 text-right font-bold text-slate-800">KES {Number(loan.loanBalance).toLocaleString()}</td>
                                <td className="p-4">{getStatusBadge(loan.status)}</td>
                                <td className="p-4 text-right text-slate-500">{loan.expectedRepaymentDate || '-'}</td>
                            </tr>
                        ))}
                        {loans.length === 0 && <tr><td colSpan="5" className="p-8 text-center text-slate-400 italic">No loan history found.</td></tr>}
                    </tbody>
                </table>
            </div>

            {/* Loan Application Modal */}
            {showApply && (
                <div className="fixed inset-0 bg-slate-900/50 flex items-center justify-center p-4 z-50 backdrop-blur-sm">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
                        <h3 className="text-lg font-bold text-slate-800 mb-4">Loan Application</h3>
                        <form onSubmit={handleApply} className="space-y-4">
                            <div>
                                <label className="block text-xs font-bold text-slate-500 mb-1">Select Product</label>
                                <select
                                    className="w-full p-2 border rounded-lg"
                                    value={selectedProduct}
                                    onChange={e => setSelectedProduct(e.target.value)}
                                    required
                                >
                                    <option value="">-- Choose Loan Type --</option>
                                    {products.map(p => <option key={p.id} value={p.id}>{p.name} (Max: {p.maxLimit})</option>)}
                                </select>
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-slate-500 mb-1">Amount (KES)</label>
                                <input type="number" className="w-full p-2 border rounded-lg" required value={amount} onChange={e => setAmount(e.target.value)} />
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-slate-500 mb-1">Duration (Months)</label>
                                <input type="number" className="w-full p-2 border rounded-lg" required value={duration} onChange={e => setDuration(e.target.value)} />
                            </div>
                            <div className="flex gap-2 pt-2">
                                <button type="button" onClick={() => setShowApply(false)} className="flex-1 py-2 text-slate-500 font-bold hover:bg-slate-50 rounded-lg">Cancel</button>
                                <button type="submit" className="flex-1 py-2 bg-emerald-600 text-white font-bold rounded-lg hover:bg-emerald-700">Submit Application</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}