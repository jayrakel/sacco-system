import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { Package, Plus, FileText } from 'lucide-react';

export default function LoanProducts() {
    const [products, setProducts] = useState([]);
    const [members, setMembers] = useState([]); // Kept for future admin override logic
    const [showApplyModal, setShowApplyModal] = useState(false);
    const [showCreateModal, setShowCreateModal] = useState(false);

    // Application Form
    const [application, setApplication] = useState({ memberId: '', productId: '', amount: '', duration: '' });

    // Product Creation Form
    const [newProduct, setNewProduct] = useState({
        name: '',
        description: '',
        interestRate: 12,
        interestType: 'FLAT_RATE',
        maxDurationWeeks: 52,
        maxAmount: 100000,
        applicationFee: 0,
        penaltyRate: 0,
        receivableAccountCode: '1201',
        incomeAccountCode: '4001'
    });

    useEffect(() => { fetchData(); }, []);

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

        // ✅ FIX: Explicitly set isActive to true in payload
        const payload = {
            ...newProduct,
            isActive: true
        };

        try {
            await api.post('/api/loans/products', payload);
            alert("Product Created!");
            setShowCreateModal(false);
            fetchData();
        } catch (e) {
            console.error(e);
            alert(e.response?.data?.message || "Failed to create product");
        }
    };

    const handleApply = async (e) => {
        e.preventDefault();
        try {
             alert("Please use the Member Portal to apply for loans.");
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
                        <div className="flex justify-between items-start">
                            <div>
                                <h3 className="font-bold text-lg text-slate-800 mb-1">{p.name}</h3>
                                <span className={`text-[10px] font-bold px-2 py-1 rounded-full ${p.applicationFee > 0 ? 'bg-indigo-100 text-indigo-700' : 'bg-emerald-100 text-emerald-700'}`}>
                                    {p.applicationFee > 0 ? `Fee: KES ${p.applicationFee}` : 'No Application Fee'}
                                </span>
                            </div>
                            <div className="text-right">
                                <p className="text-2xl font-black text-slate-800">{p.interestRate}%</p>
                                <p className="text-[10px] text-slate-400 uppercase">Interest</p>
                            </div>
                        </div>

                        <p className="text-xs text-slate-500 my-4 h-10 line-clamp-2">{p.description || "Standard loan product."}</p>

                        <div className="space-y-2 text-xs bg-slate-50 p-3 rounded-lg border border-slate-100">
                            <div className="flex justify-between"><span className="text-slate-500">Interest Type:</span> <span className="font-bold text-emerald-600">{p.interestType}</span></div>
                            <div className="flex justify-between"><span className="text-slate-500">Max Limit:</span> <span className="font-bold">KES {Number(p.maxAmount).toLocaleString()}</span></div>
                            <div className="flex justify-between"><span className="text-slate-500">Max Tenure:</span> <span className="font-bold">{Math.round(p.maxDurationWeeks / 4)} Months ({p.maxDurationWeeks} Wks)</span></div>
                            <div className="flex justify-between"><span className="text-slate-500">Penalty Rate:</span> <span className="font-bold text-red-500">{p.penaltyRate || 0}%</span></div>
                        </div>
                    </div>
                ))}
            </div>

            {/* CREATE PRODUCT MODAL */}
            {showCreateModal && (
                <div className="fixed inset-0 bg-slate-900/50 flex items-center justify-center p-4 z-50 backdrop-blur-sm">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
                        <h3 className="font-bold text-lg mb-4">Create New Product</h3>
                        <form onSubmit={handleCreateProduct} className="space-y-4">
                            <div><label className="block text-xs font-bold text-slate-500">Name</label><input type="text" required className="w-full p-2 border rounded" onChange={e => setNewProduct({...newProduct, name: e.target.value})} /></div>
                            <div><label className="block text-xs font-bold text-slate-500">Description</label><textarea className="w-full p-2 border rounded" onChange={e => setNewProduct({...newProduct, description: e.target.value})} /></div>

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
                                <div><label className="block text-xs font-bold text-slate-500">Max Limit</label><input type="number" required className="w-full p-2 border rounded" value={newProduct.maxAmount} onChange={e => setNewProduct({...newProduct, maxAmount: e.target.value})} /></div>
                                <div><label className="block text-xs font-bold text-slate-500">Max Weeks</label><input type="number" required className="w-full p-2 border rounded" value={newProduct.maxDurationWeeks} onChange={e => setNewProduct({...newProduct, maxDurationWeeks: e.target.value})} /></div>
                            </div>

                            <div className="grid grid-cols-2 gap-4 bg-slate-50 p-3 rounded-lg border border-slate-100">
                                <div><label className="block text-xs font-bold text-slate-500 mb-1">App Fee</label><input type="number" className="w-full p-2 border rounded" value={newProduct.applicationFee} onChange={e => setNewProduct({...newProduct, applicationFee: e.target.value})} /></div>
                                <div><label className="block text-xs font-bold text-slate-500 mb-1">Penalty (%)</label><input type="number" className="w-full p-2 border rounded" value={newProduct.penaltyRate} onChange={e => setNewProduct({...newProduct, penaltyRate: e.target.value})} /></div>
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