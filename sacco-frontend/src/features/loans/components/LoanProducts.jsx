import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { Package, Plus, DollarSign, Clock, Users, Save } from 'lucide-react';

export default function LoanProducts() {
    const [products, setProducts] = useState([]);
    const [members, setMembers] = useState([]);
    const [showApplyModal, setShowApplyModal] = useState(false);
    const [showCreateModal, setShowCreateModal] = useState(false);

    // Application Form State
    const [application, setApplication] = useState({ memberId: '', productId: '', amount: '', duration: '' });
    // Product Creation State
    const [newProduct, setNewProduct] = useState({ name: '', interestRate: 12, interestType: 'FLAT_RATE', maxTenureMonths: 24, maxLimit: 100000 });

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        try {
            const [prodRes, memRes] = await Promise.all([
                api.get('/api/loans/products'),
                api.get('/api/members/active')
            ]);
            if (prodRes.data.success) setProducts(prodRes.data.data);
            if (memRes.data.success) setMembers(memRes.data.data);
        } catch (e) { console.error(e); }
    };

    const handleCreateProduct = async (e) => {
        e.preventDefault();
        try {
            await api.post('/api/loans/products', newProduct);
            alert("Product Created!");
            setShowCreateModal(false);
            fetchData();
        } catch (e) { alert("Failed to create product"); }
    };

    const handleApply = async (e) => {
        e.preventDefault();
        const params = new URLSearchParams(application);
        try {
            await api.post('/api/loans/apply', null, { params });
            alert("Application Submitted Successfully!");
            setShowApplyModal(false);
        } catch (e) { alert(e.response?.data?.message || "Application Failed"); }
    };

    return (
        <div className="space-y-6 animate-in fade-in">
            {/* Header */}
            <div className="flex justify-between items-center bg-white p-4 rounded-xl shadow-sm border border-slate-200">
                <h2 className="font-bold text-slate-800 flex items-center gap-2"><Package className="text-blue-600"/> Loan Products</h2>
                <div className="flex gap-2">
                    <button onClick={() => setShowCreateModal(true)} className="bg-slate-900 text-white px-3 py-2 rounded-lg text-xs font-bold flex items-center gap-2 hover:bg-slate-800">
                        <Plus size={14}/> New Product
                    </button>
                    <button onClick={() => setShowApplyModal(true)} className="bg-emerald-600 text-white px-3 py-2 rounded-lg text-xs font-bold flex items-center gap-2 hover:bg-emerald-700">
                        <FileText size={14}/> Apply for Loan
                    </button>
                </div>
            </div>

            {/* Product Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {products.map(p => (
                    <div key={p.id} className="bg-white p-5 rounded-xl shadow-sm border border-slate-200 hover:shadow-md transition">
                        <h3 className="font-bold text-lg text-slate-800 mb-1">{p.name}</h3>
                        <p className="text-xs text-slate-500 mb-4 h-10">{p.description || "Standard loan product for members."}</p>
                        <div className="space-y-2 text-sm">
                            <div className="flex justify-between"><span className="text-slate-500">Interest:</span> <span className="font-bold">{p.interestRate}% ({p.interestType})</span></div>
                            <div className="flex justify-between"><span className="text-slate-500">Max Limit:</span> <span className="font-bold">KES {Number(p.maxLimit).toLocaleString()}</span></div>
                            <div className="flex justify-between"><span className="text-slate-500">Max Tenure:</span> <span className="font-bold">{p.maxTenureMonths} Months</span></div>
                        </div>
                    </div>
                ))}
            </div>

            {/* APPLICATION MODAL */}
            {showApplyModal && (
                <div className="fixed inset-0 bg-slate-900/50 flex items-center justify-center p-4 z-50 backdrop-blur-sm">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
                        <h3 className="font-bold text-lg mb-4">Loan Application</h3>
                        <form onSubmit={handleApply} className="space-y-4">
                            <div>
                                <label className="block text-xs font-bold text-slate-500 mb-1">Select Member</label>
                                <select required className="w-full p-2 border rounded" onChange={e => setApplication({...application, memberId: e.target.value})}>
                                    <option value="">-- Select Member --</option>
                                    {members.map(m => <option key={m.id} value={m.id}>{m.firstName} {m.lastName}</option>)}
                                </select>
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-slate-500 mb-1">Loan Product</label>
                                <select required className="w-full p-2 border rounded" onChange={e => setApplication({...application, productId: e.target.value})}>
                                    <option value="">-- Select Product --</option>
                                    {products.map(p => <option key={p.id} value={p.id}>{p.name} ({p.interestRate}%)</option>)}
                                </select>
                            </div>
                            <div className="grid grid-cols-2 gap-4">
                                <div><label className="block text-xs font-bold text-slate-500 mb-1">Amount</label><input type="number" required className="w-full p-2 border rounded" onChange={e => setApplication({...application, amount: e.target.value})} /></div>
                                <div><label className="block text-xs font-bold text-slate-500 mb-1">Duration (Months)</label><input type="number" required className="w-full p-2 border rounded" onChange={e => setApplication({...application, duration: e.target.value})} /></div>
                            </div>
                            <div className="flex justify-end gap-2 mt-4">
                                <button type="button" onClick={() => setShowApplyModal(false)} className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded">Cancel</button>
                                <button type="submit" className="px-4 py-2 bg-emerald-600 text-white rounded font-bold hover:bg-emerald-700">Submit Application</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* CREATE PRODUCT MODAL */}
            {showCreateModal && (
                <div className="fixed inset-0 bg-slate-900/50 flex items-center justify-center p-4 z-50 backdrop-blur-sm">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
                        <h3 className="font-bold text-lg mb-4">Create New Product</h3>
                        <form onSubmit={handleCreateProduct} className="space-y-4">
                            <div><label className="block text-xs font-bold text-slate-500">Name</label><input type="text" required className="w-full p-2 border rounded" onChange={e => setNewProduct({...newProduct, name: e.target.value})} /></div>
                            <div className="grid grid-cols-2 gap-4">
                                <div><label className="block text-xs font-bold text-slate-500">Interest Rate (%)</label><input type="number" required className="w-full p-2 border rounded" value={newProduct.interestRate} onChange={e => setNewProduct({...newProduct, interestRate: e.target.value})} /></div>
                                <div><label className="block text-xs font-bold text-slate-500">Type</label>
                                    <select className="w-full p-2 border rounded" value={newProduct.interestType} onChange={e => setNewProduct({...newProduct, interestType: e.target.value})}>
                                        <option value="FLAT_RATE">Flat Rate</option>
                                        <option value="REDUCING_BALANCE">Reducing Balance</option>
                                    </select>
                                </div>
                            </div>
                            <div className="grid grid-cols-2 gap-4">
                                <div><label className="block text-xs font-bold text-slate-500">Max Limit</label><input type="number" required className="w-full p-2 border rounded" value={newProduct.maxLimit} onChange={e => setNewProduct({...newProduct, maxLimit: e.target.value})} /></div>
                                <div><label className="block text-xs font-bold text-slate-500">Max Tenure (Months)</label><input type="number" required className="w-full p-2 border rounded" value={newProduct.maxTenureMonths} onChange={e => setNewProduct({...newProduct, maxTenureMonths: e.target.value})} /></div>
                            </div>
                            <div className="flex justify-end gap-2 mt-4">
                                <button type="button" onClick={() => setShowCreateModal(false)} className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded">Cancel</button>
                                <button type="submit" className="px-4 py-2 bg-slate-900 text-white rounded font-bold hover:bg-slate-800">Save Product</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}