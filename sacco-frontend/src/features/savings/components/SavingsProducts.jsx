import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { Package, Plus, Save, Trash2, Percent, Clock, Lock } from 'lucide-react';

export default function SavingsProducts() {
    const [products, setProducts] = useState([]);
    const [showModal, setShowModal] = useState(false);
    const [newProduct, setNewProduct] = useState({
        name: '', description: '', type: 'SAVINGS',
        interestRate: 5.0, minBalance: 0, minDurationMonths: 0, allowWithdrawal: true
    });

    useEffect(() => { fetchProducts(); }, []);

    const fetchProducts = async () => {
        try {
            const res = await api.get('/api/savings/products');
            if (res.data.success) setProducts(res.data.data);
        } catch (e) { console.error(e); }
    };

    const handleCreate = async (e) => {
        e.preventDefault();
        try {
            await api.post('/api/savings/products', newProduct);
            alert("Product Created!");
            setShowModal(false);
            fetchProducts();
        } catch (e) { alert("Error creating product"); }
    };

    return (
        <div className="space-y-6 animate-in fade-in">
            {/* Header */}
            <div className="flex justify-between items-center bg-white p-4 rounded-xl shadow-sm border border-slate-200">
                <h2 className="font-bold text-slate-800 flex items-center gap-2"><Package className="text-emerald-600"/> Savings Products</h2>
                <button onClick={() => setShowModal(true)} className="bg-slate-900 text-white px-3 py-2 rounded-lg text-xs font-bold flex items-center gap-2 hover:bg-slate-800">
                    <Plus size={14}/> New Product
                </button>
            </div>

            {/* List */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {products.map(p => (
                    <div key={p.id} className="bg-white p-5 rounded-xl shadow-sm border border-slate-200 hover:shadow-md transition">
                        <div className="flex justify-between items-start mb-2">
                            <h3 className="font-bold text-lg text-slate-800">{p.name}</h3>
                            <span className="text-[10px] bg-slate-100 px-2 py-1 rounded font-bold uppercase text-slate-500">{p.type}</span>
                        </div>
                        <p className="text-xs text-slate-500 mb-4 h-8">{p.description}</p>

                        <div className="space-y-2 text-xs font-medium text-slate-600 bg-slate-50 p-3 rounded-lg">
                            <div className="flex justify-between items-center"><span className="flex gap-1 items-center"><Percent size={12}/> Interest:</span> <span className="text-emerald-600 font-bold">{p.interestRate}%</span></div>
                            <div className="flex justify-between items-center"><span className="flex gap-1 items-center"><Lock size={12}/> Min Bal:</span> <span>KES {p.minBalance}</span></div>
                            {p.minDurationMonths > 0 && <div className="flex justify-between items-center"><span className="flex gap-1 items-center"><Clock size={12}/> Lock Period:</span> <span>{p.minDurationMonths} Months</span></div>}
                            {!p.allowWithdrawal && <div className="text-red-500 text-[10px] text-center mt-2 font-bold bg-red-50 py-1 rounded">Withdrawals Locked</div>}
                        </div>
                    </div>
                ))}
            </div>

            {/* Create Modal */}
            {showModal && (
                <div className="fixed inset-0 bg-slate-900/50 flex items-center justify-center p-4 z-50 backdrop-blur-sm">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
                        <h3 className="font-bold text-lg mb-4 text-slate-800">Create Savings Product</h3>
                        <form onSubmit={handleCreate} className="space-y-3">
                            <div><label className="text-xs font-bold text-slate-500">Name</label><input type="text" required className="w-full p-2 border rounded" onChange={e => setNewProduct({...newProduct, name: e.target.value})} /></div>
                            <div><label className="text-xs font-bold text-slate-500">Description</label><input type="text" className="w-full p-2 border rounded" onChange={e => setNewProduct({...newProduct, description: e.target.value})} /></div>
                            <div className="grid grid-cols-2 gap-3">
                                <div><label className="text-xs font-bold text-slate-500">Type</label>
                                    <select className="w-full p-2 border rounded" onChange={e => setNewProduct({...newProduct, type: e.target.value})}>
                                        <option value="SAVINGS">Ordinary Savings</option>
                                        <option value="FIXED_DEPOSIT">Fixed Deposit</option>
                                        <option value="RECURRING_DEPOSIT">Recurring</option>
                                    </select>
                                </div>
                                <div><label className="text-xs font-bold text-slate-500">Interest (%)</label><input type="number" required className="w-full p-2 border rounded" value={newProduct.interestRate} onChange={e => setNewProduct({...newProduct, interestRate: e.target.value})} /></div>
                            </div>
                            <div className="grid grid-cols-2 gap-3">
                                <div><label className="text-xs font-bold text-slate-500">Min Balance</label><input type="number" className="w-full p-2 border rounded" value={newProduct.minBalance} onChange={e => setNewProduct({...newProduct, minBalance: e.target.value})} /></div>
                                <div><label className="text-xs font-bold text-slate-500">Lock Months</label><input type="number" className="w-full p-2 border rounded" value={newProduct.minDurationMonths} onChange={e => setNewProduct({...newProduct, minDurationMonths: e.target.value})} /></div>
                            </div>
                            <div className="flex items-center gap-2 pt-2"><input type="checkbox" checked={!newProduct.allowWithdrawal} onChange={e => setNewProduct({...newProduct, allowWithdrawal: !e.target.checked})} /><label className="text-xs font-bold text-slate-600">Lock Withdrawals (Until Maturity)</label></div>

                            <div className="flex justify-end gap-2 mt-4 pt-4 border-t">
                                <button type="button" onClick={() => setShowModal(false)} className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded text-sm">Cancel</button>
                                <button type="submit" className="px-4 py-2 bg-emerald-600 text-white rounded font-bold hover:bg-emerald-700 text-sm flex items-center gap-2"><Save size={14}/> Save</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}