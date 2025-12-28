import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { toast } from 'react-hot-toast';

export default function LoanEntryWorkflow() {
    const [step, setStep] = useState('PAYMENT'); // PAYMENT, FORM, SUBMITTED
    const [products, setProducts] = useState([]);
    const [selectedProduct, setSelectedProduct] = useState("");
    const [loading, setLoading] = useState(false);
    
    // Form States for Step 2
    const [amount, setAmount] = useState("");
    const [duration, setDuration] = useState("");
    const [loanId, setLoanId] = useState(null); // Received from 'initiate-with-fee'

    // Load products with safety checks
    useEffect(() => {
        api.get('/api/loans/products')
            .then(res => {
                // Ensure we set an empty array if data is missing to avoid .map() crash
                const data = res.data?.data || [];
                setProducts(Array.isArray(data) ? data : []);
            })
            .catch(err => {
                console.error("Failed to load products", err);
                setProducts([]);
            });
    }, []);

    // Step 1: Handle Fee Payment
    const handlePayment = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            const res = await api.post(`/api/loans/initiate-with-fee`, {
                productId: selectedProduct,
                reference: "MPESA_REF_" + Math.random().toString(36).substring(7).toUpperCase()
            });
            
            if (res.data?.success) {
                setLoanId(res.data.data.id); // Save the draft loan ID
                setStep('FORM');
                toast.success("Fee verified! Please complete application details.");
            }
        } catch (err) {
            toast.error("Payment failed: " + (err.response?.data?.message || "Server Error"));
        } finally {
            setLoading(false);
        }
    };

    // Step 2: Handle Application Submission
    const handleFormSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            const res = await api.post(`/api/loans/apply`, null, {
                params: {
                    loanId: loanId,
                    amount: amount,
                    duration: duration
                }
            });

            if (res.data?.success) {
                setStep('SUBMITTED');
                toast.success("Application submitted for review!");
            }
        } catch (err) {
            toast.error("Application failed: " + (err.response?.data?.message || "Check limits"));
        } finally {
            setLoading(false);
        }
    };

    if (step === 'PAYMENT') {
        return (
            <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm animate-in fade-in slide-in-from-bottom-4">
                <h3 className="text-lg font-bold text-slate-800">New Loan Application</h3>
                <p className="text-slate-500 text-sm mb-4">Select a product and pay the application fee to unlock the form.</p>
                
                <form onSubmit={handlePayment} className="space-y-4">
                    <select 
                        className="w-full p-2 border rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
                        value={selectedProduct}
                        onChange={(e) => setSelectedProduct(e.target.value)}
                        required
                    >
                        <option value="">Choose Loan Product...</option>
                        {products?.map(p => (
                            <option key={p.id} value={p.id}>{p.name} - Fee: KES {p.processingFee}</option>
                        ))}
                    </select>
                    <button 
                        type="submit"
                        disabled={loading || !selectedProduct}
                        className="w-full bg-indigo-600 text-white py-2 rounded-lg font-semibold hover:bg-indigo-700 transition-colors disabled:opacity-50"
                    >
                        {loading ? "Processing..." : "Pay Fee to Continue"}
                    </button>
                </form>
            </div>
        );
    }

    if (step === 'FORM') {
        return (
            <div className="bg-white p-6 rounded-2xl border border-indigo-100 shadow-md animate-in zoom-in-95 duration-300">
                <h3 className="text-lg font-bold text-indigo-900">Loan Application Details</h3>
                <p className="text-slate-500 text-sm mb-4">Draft ID: {loanId?.substring(0,8)}...</p>
                <form onSubmit={handleFormSubmit} className="space-y-4">
                    <div>
                        <label className="text-xs font-bold text-slate-500 uppercase ml-1">Loan Amount</label>
                        <input 
                            type="number" 
                            placeholder="Amount (KES)" 
                            className="w-full p-2 border rounded focus:ring-2 focus:ring-indigo-500"
                            value={amount}
                            onChange={(e) => setAmount(e.target.value)}
                            required 
                        />
                    </div>
                    <div>
                        <label className="text-xs font-bold text-slate-500 uppercase ml-1">Duration</label>
                        <input 
                            type="number" 
                            placeholder="Duration (Months)" 
                            className="w-full p-2 border rounded focus:ring-2 focus:ring-indigo-500"
                            value={duration}
                            onChange={(e) => setDuration(e.target.value)}
                            required 
                        />
                    </div>
                    <button 
                        disabled={loading}
                        className="w-full bg-indigo-600 text-white py-2 rounded font-bold hover:bg-indigo-700 transition-all shadow-lg shadow-indigo-200"
                    >
                        {loading ? "Checking Limits..." : "Submit Application"}
                    </button>
                </form>
            </div>
        );
    }

    if (step === 'SUBMITTED') {
        return (
            <div className="bg-emerald-50 p-8 rounded-2xl border border-emerald-100 text-center animate-in fade-in">
                <div className="w-16 h-16 bg-emerald-500 text-white rounded-full flex items-center justify-center mx-auto mb-4 text-2xl shadow-lg">✓</div>
                <h3 className="text-xl font-bold text-emerald-900">Application Received</h3>
                <p className="text-emerald-700 mt-2 text-sm">Your application has been forwarded to the Loan Officer for review. You will be notified of the outcome.</p>
                <button 
                    onClick={() => window.location.reload()} 
                    className="mt-6 text-emerald-600 font-bold hover:underline"
                >
                    Apply for another loan
                </button>
            </div>
        );
    }
}