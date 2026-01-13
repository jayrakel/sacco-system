import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { Package, Plus, FileText } from 'lucide-react';

export default function LoanProducts() {
    const [products, setProducts] = useState([]);
    const [members, setMembers] = useState([]); // Kept for future admin override logic
    const [showApplyModal, setShowApplyModal] = useState(false);
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [branding, setBranding] = useState({
        primary_color: '#3B82F6',
        secondary_color: '#1E40AF'
    });

    // Application Form
    const [application, setApplication] = useState({ memberId: '', productId: '', amount: '', duration: '' });

    // Product Creation Form
    const [newProduct, setNewProduct] = useState({
        productCode: '',
        name: '',
        description: '',
        interestRate: 12,
        interestType: 'FLAT',
        maxDurationWeeks: 52,
        maxAmount: 100000,
        applicationFee: 0,
        penaltyRate: 0,
        currencyCode: 'KES',
        receivableAccountCode: '',
        incomeAccountCode: ''
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
        } catch (e) {
            console.error("Failed to load loan products:", e);
        }
    };

    const handleCreateProduct = async (e) => {
        e.preventDefault();

        // ✅ Send only business information - backend auto-generates productCode and configures accounting
        const payload = {
            productName: newProduct.name,
            description: newProduct.description,
            interestRate: newProduct.interestRate,
            interestType: newProduct.interestType,
            maxDurationWeeks: newProduct.maxDurationWeeks,
            maxAmount: newProduct.maxAmount,
            applicationFee: newProduct.applicationFee,
            penaltyRate: newProduct.penaltyRate
        };

        try {
            await api.post('/api/loans/products', payload);
            alert("Product Created!");
            setShowCreateModal(false);

            // Reset form
            setNewProduct({
                name: '',
                description: '',
                interestRate: 12,
                interestType: 'FLAT',
                maxDurationWeeks: 52,
                maxAmount: 100000,
                applicationFee: 0,
                penaltyRate: 0
            });
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
                                <h3 className="font-bold text-lg text-slate-800 mb-1">{p.productName}</h3>
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
                <div className="fixed inset-0 bg-slate-900/60 flex items-center justify-center p-4 z-50 backdrop-blur-sm">
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] flex flex-col overflow-hidden">
                        {/* Modal Header - Fixed */}
                        <div className="bg-gradient-to-r from-blue-600 to-blue-700 px-6 py-5 flex items-center justify-between flex-shrink-0">
                            <div className="flex items-center gap-3">
                                <div className="bg-white/20 p-2 rounded-lg">
                                    <Package className="text-white" size={24} />
                                </div>
                                <div>
                                    <h3 className="font-bold text-xl text-white">Create Loan Product</h3>
                                    <p className="text-blue-100 text-xs mt-0.5">Configure a new loan product for members</p>
                                </div>
                            </div>
                            <button
                                type="button"
                                onClick={() => setShowCreateModal(false)}
                                className="text-white/80 hover:text-white hover:bg-white/10 p-2 rounded-lg transition"
                            >
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        </div>

                        {/* Modal Body - Scrollable */}
                        <div className="flex-1 overflow-y-auto px-6 py-6">
                            <form onSubmit={handleCreateProduct} id="loan-product-form" className="space-y-6">
                                {/* Basic Information */}
                                <div className="space-y-4">
                                    <div className="flex items-center gap-2 pb-2 border-b border-slate-200">
                                        <div className="w-1 h-4 bg-blue-600 rounded-full"></div>
                                        <h4 className="font-semibold text-slate-700 text-sm">Basic Information</h4>
                                    </div>

                                    <div>
                                        <label className="block text-sm font-semibold text-slate-700 mb-2">
                                            Product Name <span className="text-red-500">*</span>
                                        </label>
                                        <input
                                            type="text"
                                            required
                                            className="w-full px-4 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition"
                                            placeholder="e.g., Emergency Loan"
                                            value={newProduct.name}
                                            onChange={e => setNewProduct({...newProduct, name: e.target.value})}
                                        />
                                    </div>

                                    <div>
                                        <label className="block text-sm font-semibold text-slate-700 mb-2">Description</label>
                                        <textarea
                                            className="w-full px-4 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition resize-none"
                                            rows="3"
                                            placeholder="Brief description of the loan product..."
                                            value={newProduct.description}
                                            onChange={e => setNewProduct({...newProduct, description: e.target.value})}
                                        />
                                    </div>
                                </div>

                                {/* Loan Terms */}
                                <div className="bg-gradient-to-br from-emerald-50 to-teal-50 p-5 rounded-xl border border-emerald-200">
                                    <div className="flex items-center gap-2 mb-4">
                                        <div className="bg-emerald-600 p-1.5 rounded-lg">
                                            <svg className="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                            </svg>
                                        </div>
                                        <h4 className="font-semibold text-emerald-800 text-sm">Loan Terms & Limits</h4>
                                    </div>

                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                        <div>
                                            <label className="block text-sm font-semibold text-slate-700 mb-2">
                                                Interest Rate (%) <span className="text-red-500">*</span>
                                            </label>
                                            <input
                                                type="number"
                                                step="0.01"
                                                required
                                                className="w-full px-4 py-2.5 border border-emerald-300 rounded-lg bg-white focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition"
                                                value={newProduct.interestRate}
                                                onChange={e => setNewProduct({...newProduct, interestRate: e.target.value})}
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-sm font-semibold text-slate-700 mb-2">
                                                Interest Type <span className="text-red-500">*</span>
                                            </label>
                                            <select
                                                className="w-full px-4 py-2.5 border border-emerald-300 rounded-lg bg-white focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition"
                                                value={newProduct.interestType}
                                                onChange={e => setNewProduct({...newProduct, interestType: e.target.value})}
                                            >
                                                <option value="FLAT">Flat Rate</option>
                                                <option value="REDUCING_BALANCE">Reducing Balance</option>
                                            </select>
                                        </div>
                                        <div>
                                            <label className="block text-sm font-semibold text-slate-700 mb-2">
                                                Max Amount (KES) <span className="text-red-500">*</span>
                                            </label>
                                            <input
                                                type="number"
                                                required
                                                className="w-full px-4 py-2.5 border border-emerald-300 rounded-lg bg-white focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition"
                                                placeholder="100000"
                                                value={newProduct.maxAmount}
                                                onChange={e => setNewProduct({...newProduct, maxAmount: e.target.value})}
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-sm font-semibold text-slate-700 mb-2">
                                                Max Duration (Weeks) <span className="text-red-500">*</span>
                                            </label>
                                            <input
                                                type="number"
                                                required
                                                className="w-full px-4 py-2.5 border border-emerald-300 rounded-lg bg-white focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition"
                                                placeholder="52"
                                                value={newProduct.maxDurationWeeks}
                                                onChange={e => setNewProduct({...newProduct, maxDurationWeeks: e.target.value})}
                                            />
                                        </div>
                                    </div>
                                </div>

                                {/* Fees & Penalties */}
                                <div className="bg-gradient-to-br from-amber-50 to-orange-50 p-5 rounded-xl border border-amber-200">
                                    <div className="flex items-center gap-2 mb-4">
                                        <div className="bg-amber-600 p-1.5 rounded-lg">
                                            <svg className="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                                            </svg>
                                        </div>
                                        <h4 className="font-semibold text-amber-800 text-sm">Fees & Penalties</h4>
                                    </div>

                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                        <div>
                                            <label className="block text-sm font-semibold text-slate-700 mb-2">Application Fee (KES)</label>
                                            <input
                                                type="number"
                                                step="0.01"
                                                className="w-full px-4 py-2.5 border border-amber-300 rounded-lg bg-white focus:ring-2 focus:ring-amber-500 focus:border-transparent transition"
                                                placeholder="0"
                                                value={newProduct.applicationFee}
                                                onChange={e => setNewProduct({...newProduct, applicationFee: e.target.value})}
                                            />
                                            <p className="text-xs text-slate-500 mt-1">Optional processing fee</p>
                                        </div>
                                        <div>
                                            <label className="block text-sm font-semibold text-slate-700 mb-2">Penalty Rate (%)</label>
                                            <input
                                                type="number"
                                                step="0.01"
                                                className="w-full px-4 py-2.5 border border-amber-300 rounded-lg bg-white focus:ring-2 focus:ring-amber-500 focus:border-transparent transition"
                                                placeholder="0"
                                                value={newProduct.penaltyRate}
                                                onChange={e => setNewProduct({...newProduct, penaltyRate: e.target.value})}
                                            />
                                            <p className="text-xs text-slate-500 mt-1">Late payment penalty</p>
                                        </div>
                                    </div>
                                </div>

                                {/* Info Note */}
                                <div className="bg-gradient-to-r from-blue-50 to-indigo-50 p-4 rounded-xl border border-blue-200 shadow-sm">
                                    <div className="flex gap-3">
                                        <div className="flex-shrink-0">
                                            <svg className="w-5 h-5 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                                                <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
                                            </svg>
                                        </div>
                                        <div className="flex-1">
                                            <h5 className="font-semibold text-blue-900 text-sm mb-1">Auto-Configuration</h5>
                                            <p className="text-xs text-blue-800 leading-relaxed">
                                                The following will be automatically configured:
                                            </p>
                                            <ul className="text-xs text-blue-700 mt-2 space-y-1 ml-1">
                                                <li className="flex items-center gap-2">
                                                    <span className="w-1 h-1 bg-blue-600 rounded-full"></span>
                                                    <span><strong>Product Code:</strong> Generated from product name</span>
                                                </li>
                                                <li className="flex items-center gap-2">
                                                    <span className="w-1 h-1 bg-blue-600 rounded-full"></span>
                                                    <span><strong>Accounting:</strong> Set from GL Mappings</span>
                                                </li>
                                                <li className="flex items-center gap-2">
                                                    <span className="w-1 h-1 bg-blue-600 rounded-full"></span>
                                                    <span><strong>Currency:</strong> Defaults to KES</span>
                                                </li>
                                            </ul>
                                        </div>
                                    </div>
                                </div>
                            </form>
                        </div>

                        {/* Modal Footer - Fixed */}
                        <div className="border-t border-slate-200 bg-slate-50 px-6 py-4 flex justify-between items-center flex-shrink-0">
                            <p className="text-xs text-slate-500">
                                <span className="text-red-500">*</span> Required fields
                            </p>
                            <div className="flex gap-3">
                                <button
                                    type="button"
                                    onClick={() => setShowCreateModal(false)}
                                    className="px-5 py-2.5 text-slate-600 hover:bg-slate-200 rounded-lg font-medium transition"
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    form="loan-product-form"
                                    className="px-6 py-2.5 bg-gradient-to-r from-blue-600 to-blue-700 hover:from-blue-700 hover:to-blue-800 text-white rounded-lg font-semibold shadow-lg shadow-blue-500/30 transition-all duration-200 flex items-center gap-2"
                                >
                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                    </svg>
                                    Create Product
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}