import React, { useState, useEffect } from 'react';
import api from '../../../api';

export default function LoanEntryWorkflow() {
    const [step, setStep] = useState('PAYMENT'); // Valid steps: PAYMENT, FORM, SUBMITTED
    const [products, setProducts] = useState([]);
    const [selectedProduct, setSelectedProduct] = useState(null);
    const [loading, setLoading] = useState(false);

    // Load products so the user can choose which loan they are paying for
    useEffect(() => {
        api.get('/api/loans/products').then(res => setProducts(res.data.data));
    }, []);

    const handlePayment = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            // This calls the new backend 'initiate-with-fee' endpoint we discussed
            const res = await api.post(`/api/loans/initiate-with-fee`, {
                productId: selectedProduct,
                reference: "MPESA_REF_FROM_PROMPT" // Replace with actual payment integration
            });
            setStep('FORM'); // Fee successful! Move to the next step
        } catch (err) {
            alert("Payment failed: " + err.response?.data?.message);
        } finally {
            setLoading(false);
        }
    };

    if (step === 'PAYMENT') {
        return (
            <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
                <h3 className="text-lg font-bold text-slate-800">New Loan Application</h3>
                <p className="text-slate-500 text-sm mb-4">Select a product and pay the application fee to unlock the form.</p>
                
                <form onSubmit={handlePayment} className="space-y-4">
                    <select 
                        className="w-full p-2 border rounded-lg"
                        onChange={(e) => setSelectedProduct(e.target.value)}
                        required
                    >
                        <option value="">Choose Loan Product...</option>
                        {products.map(p => (
                            <option key={p.id} value={p.id}>{p.name} - Fee: KES {p.processingFee}</option>
                        ))}
                    </select>
                    <button 
                        type="submit"
                        disabled={loading}
                        className="w-full bg-indigo-600 text-white py-2 rounded-lg font-semibold hover:bg-indigo-700 transition-colors"
                    >
                        {loading ? "Processing..." : "Pay Fee to Continue"}
                    </button>
                </form>
            </div>
        );
    }

    if (step === 'FORM') {
    return (
        <div className="bg-white p-6 rounded-2xl border border-indigo-100">
            <h3 className="text-lg font-bold">Loan Application Details</h3>
            <form onSubmit={handleFormSubmit} className="space-y-4 mt-4">
                <input 
                    type="number" 
                    placeholder="Amount (KES)" 
                    className="w-full p-2 border rounded"
                    onChange={(e) => setAmount(e.target.value)}
                    required 
                />
                <input 
                    type="number" 
                    placeholder="Duration (Months)" 
                    className="w-full p-2 border rounded"
                    onChange={(e) => setDuration(e.target.value)}
                    required 
                />
                <button className="w-full bg-indigo-600 text-white py-2 rounded font-bold">
                    Check Limits & Save
                </button>
            </form>
        </div>
    );
}
}