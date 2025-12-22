import React, { useState, useEffect } from 'react';
import { Plus, Edit2, Trash2, Package, X, DollarSign, Target, CheckCircle, XCircle } from 'lucide-react';
import api from '../../../api';

/**
 * Admin Component for Managing Deposit/Contribution Products
 */
export default function DepositProductsManager() {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showModal, setShowModal] = useState(false);
    const [editMode, setEditMode] = useState(false);
    const [currentProduct, setCurrentProduct] = useState({
        id: null,
        name: '',
        description: '',
        targetAmount: '',
        status: 'ACTIVE'
    });

    useEffect(() => {
        fetchProducts();
    }, []);

    const fetchProducts = async () => {
        try {
            const res = await api.get('/api/admin/deposit-products');
            if (res.data.success) {
                setProducts(res.data.products);
            }
        } catch (error) {
            console.error('Failed to fetch products:', error);
        } finally {
            setLoading(false);
        }
    };

    const openCreateModal = () => {
        setCurrentProduct({
            id: null,
            name: '',
            description: '',
            targetAmount: '',
            status: 'ACTIVE'
        });
        setEditMode(false);
        setShowModal(true);
    };

    const openEditModal = (product) => {
        setCurrentProduct({
            id: product.id,
            name: product.name,
            description: product.description,
            targetAmount: product.targetAmount || '',
            status: product.status
        });
        setEditMode(true);
        setShowModal(true);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        try {
            const payload = {
                name: currentProduct.name,
                description: currentProduct.description,
                targetAmount: currentProduct.targetAmount ? parseFloat(currentProduct.targetAmount) : null,
                status: currentProduct.status
            };

            if (editMode) {
                await api.put(`/api/admin/deposit-products/${currentProduct.id}`, payload);
                alert('Product updated successfully!');
            } else {
                await api.post('/api/admin/deposit-products', payload);
                alert('Product created successfully!');
            }

            setShowModal(false);
            fetchProducts();
        } catch (error) {
            alert(error.response?.data?.message || 'Failed to save product');
        }
    };

    const handleClose = async (id) => {
        if (!confirm('Are you sure you want to close this product? It will no longer accept contributions.')) return;

        try {
            await api.post(`/api/admin/deposit-products/${id}/close`);
            alert('Product closed successfully!');
            fetchProducts();
        } catch (error) {
            alert(error.response?.data?.message || 'Failed to close product');
        }
    };

    const handleDelete = async (id) => {
        if (!confirm('Are you sure you want to delete this product? This action cannot be undone.')) return;

        try {
            await api.delete(`/api/admin/deposit-products/${id}`);
            alert('Product deleted successfully!');
            fetchProducts();
        } catch (error) {
            alert(error.response?.data?.message || 'Failed to delete product');
        }
    };

    if (loading) {
        return <div className="p-8 text-center text-slate-400">Loading products...</div>;
    }

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex justify-between items-center">
                <div className="flex items-center gap-3">
                    <div className="p-2 bg-purple-100 rounded-lg">
                        <Package className="text-purple-600" size={24} />
                    </div>
                    <div>
                        <h2 className="text-xl font-bold text-slate-800">Contribution Products</h2>
                        <p className="text-sm text-slate-500">Manage custom contribution products like meat, harambee, projects</p>
                    </div>
                </div>
                <button
                    onClick={openCreateModal}
                    className="flex items-center gap-2 px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 font-medium"
                >
                    <Plus size={18} /> Create Product
                </button>
            </div>

            {/* Products Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {products.map(product => {
                    const progress = product.targetAmount
                        ? (product.currentAmount / product.targetAmount) * 100
                        : 0;

                    return (
                        <div key={product.id} className="bg-white rounded-xl shadow-sm border border-slate-200 p-5 hover:shadow-md transition">
                            {/* Status Badge */}
                            <div className="flex justify-between items-start mb-3">
                                <span className={`px-2 py-1 rounded-lg text-xs font-bold ${
                                    product.status === 'ACTIVE' ? 'bg-green-100 text-green-700' :
                                    product.status === 'COMPLETED' ? 'bg-blue-100 text-blue-700' :
                                    'bg-slate-100 text-slate-700'
                                }`}>
                                    {product.status}
                                </span>
                                <div className="flex gap-1">
                                    <button
                                        onClick={() => openEditModal(product)}
                                        className="p-1.5 hover:bg-slate-100 rounded text-slate-600"
                                        title="Edit"
                                    >
                                        <Edit2 size={14} />
                                    </button>
                                    {product.currentAmount === 0 && (
                                        <button
                                            onClick={() => handleDelete(product.id)}
                                            className="p-1.5 hover:bg-red-50 rounded text-red-600"
                                            title="Delete"
                                        >
                                            <Trash2 size={14} />
                                        </button>
                                    )}
                                </div>
                            </div>

                            {/* Product Info */}
                            <h3 className="text-lg font-bold text-slate-800 mb-1">{product.name}</h3>
                            <p className="text-sm text-slate-500 mb-4 line-clamp-2">{product.description}</p>

                            {/* Progress */}
                            {product.targetAmount && (
                                <>
                                    <div className="mb-2">
                                        <div className="flex justify-between text-xs mb-1">
                                            <span className="text-slate-500">Collected</span>
                                            <span className="font-bold text-slate-700">{progress.toFixed(0)}%</span>
                                        </div>
                                        <div className="h-2 bg-slate-100 rounded-full overflow-hidden">
                                            <div
                                                className="h-full bg-purple-600 transition-all"
                                                style={{ width: `${Math.min(progress, 100)}%` }}
                                            />
                                        </div>
                                    </div>
                                    <div className="flex justify-between text-xs mb-3">
                                        <span className="text-slate-600">
                                            KES {Number(product.currentAmount).toLocaleString()}
                                        </span>
                                        <span className="text-slate-500">
                                            / {Number(product.targetAmount).toLocaleString()}
                                        </span>
                                    </div>
                                </>
                            )}

                            {!product.targetAmount && (
                                <div className="mb-3">
                                    <p className="text-xs text-slate-500 mb-1">Total Collected</p>
                                    <p className="text-xl font-bold text-slate-800">
                                        KES {Number(product.currentAmount).toLocaleString()}
                                    </p>
                                </div>
                            )}

                            {/* Actions */}
                            {product.status === 'ACTIVE' && (
                                <button
                                    onClick={() => handleClose(product.id)}
                                    className="w-full py-2 bg-slate-100 text-slate-700 rounded-lg hover:bg-slate-200 text-sm font-medium"
                                >
                                    Close Product
                                </button>
                            )}

                            <div className="mt-2 pt-2 border-t border-slate-100 text-xs text-slate-400">
                                Created by {product.createdByName}
                            </div>
                        </div>
                    );
                })}
            </div>

            {products.length === 0 && (
                <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-12 text-center">
                    <Package className="mx-auto text-slate-300 mb-4" size={48} />
                    <p className="text-slate-500 font-medium mb-2">No contribution products yet</p>
                    <p className="text-sm text-slate-400 mb-4">Create custom products like meat contribution, harambee, group projects</p>
                    <button
                        onClick={openCreateModal}
                        className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 font-medium"
                    >
                        Create First Product
                    </button>
                </div>
            )}

            {/* Create/Edit Modal */}
            {showModal && (
                <div className="fixed inset-0 bg-slate-900/50 flex items-center justify-center p-4 z-50 backdrop-blur-sm">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
                        <div className="flex justify-between items-center mb-4">
                            <h3 className="text-lg font-bold text-slate-800">
                                {editMode ? 'Edit Product' : 'Create New Product'}
                            </h3>
                            <button
                                onClick={() => setShowModal(false)}
                                className="p-1 hover:bg-slate-100 rounded"
                            >
                                <X size={20} />
                            </button>
                        </div>

                        <form onSubmit={handleSubmit} className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">
                                    Product Name *
                                </label>
                                <input
                                    type="text"
                                    required
                                    value={currentProduct.name}
                                    onChange={(e) => setCurrentProduct({ ...currentProduct, name: e.target.value })}
                                    placeholder="e.g., Meat Contribution"
                                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-purple-500"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">
                                    Description
                                </label>
                                <textarea
                                    value={currentProduct.description}
                                    onChange={(e) => setCurrentProduct({ ...currentProduct, description: e.target.value })}
                                    placeholder="Brief description of the contribution product"
                                    rows={3}
                                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-purple-500"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">
                                    Target Amount (Optional)
                                </label>
                                <div className="relative">
                                    <span className="absolute left-3 top-2 text-slate-500">KES</span>
                                    <input
                                        type="number"
                                        value={currentProduct.targetAmount}
                                        onChange={(e) => setCurrentProduct({ ...currentProduct, targetAmount: e.target.value })}
                                        placeholder="100000"
                                        step="0.01"
                                        className="w-full pl-14 pr-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-purple-500"
                                    />
                                </div>
                                <p className="text-xs text-slate-500 mt-1">Leave empty for no target</p>
                            </div>

                            {editMode && (
                                <div>
                                    <label className="block text-sm font-medium text-slate-700 mb-1">
                                        Status
                                    </label>
                                    <select
                                        value={currentProduct.status}
                                        onChange={(e) => setCurrentProduct({ ...currentProduct, status: e.target.value })}
                                        className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-purple-500"
                                    >
                                        <option value="ACTIVE">Active</option>
                                        <option value="CLOSED">Closed</option>
                                        <option value="COMPLETED">Completed</option>
                                    </select>
                                </div>
                            )}

                            <div className="flex gap-3 pt-4">
                                <button
                                    type="button"
                                    onClick={() => setShowModal(false)}
                                    className="flex-1 py-2 px-4 border border-slate-300 text-slate-700 rounded-lg hover:bg-slate-50 font-medium"
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    className="flex-1 py-2 px-4 bg-purple-600 text-white rounded-lg hover:bg-purple-700 font-medium"
                                >
                                    {editMode ? 'Update' : 'Create'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
