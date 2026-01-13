import React, { useState, useEffect } from 'react';
import api from '../../../../api';
import { UserCheck, CheckCircle, XCircle, Clock, AlertCircle } from 'lucide-react';
import BrandedSpinner from '../../../../components/BrandedSpinner';

export default function GuarantorRequestsWidget() {
    const [requests, setRequests] = useState([]);
    const [loading, setLoading] = useState(true);
    const [processingId, setProcessingId] = useState(null);

    useEffect(() => {
        fetchRequests();
    }, []);

    const fetchRequests = async () => {
        try {
            // ✅ FIX: Corrected URL to match LoanController ('/guarantors/requests')
            const res = await api.get('/api/loans/guarantors/requests');
            if (res.data.success) setRequests(res.data.data);
        } catch (e) {
            console.error("Failed to load requests", e);
        } finally {
            setLoading(false);
        }
    };

    const handleResponse = async (requestId, isApproved) => {
        if (!window.confirm(isApproved ? "Accept liability for this loan?" : "Reject this request?")) return;

        setProcessingId(requestId);
        try {
            await api.post(`/api/loans/guarantors/${requestId}/respond`, { approved: isApproved });
            // Remove from list locally for instant feedback
            setRequests(prev => prev.filter(r => r.requestId !== requestId));
        } catch (e) {
            alert(e.response?.data?.message || "Action failed");
        } finally {
            setProcessingId(null);
        }
    };

    if (loading) return <div className="h-40 flex items-center justify-center"><BrandedSpinner size="small"/></div>;
    if (!requests || requests.length === 0) return null; // Hide if empty

    return (
        <div className="bg-orange-50 border border-orange-100 rounded-3xl p-6 mb-6 animate-in fade-in slide-in-from-top-4">
            <div className="flex items-center gap-3 mb-4">
                <div className="bg-orange-100 p-2 rounded-full text-orange-600">
                    <UserCheck size={20}/>
                </div>
                <div>
                    <h3 className="font-bold text-orange-900">Guarantorship Requests</h3>
                    <p className="text-xs text-orange-700">Members requested your guarantee</p>
                </div>
            </div>

            <div className="space-y-3">
                {requests.map(req => (
                    <div key={req.requestId} className="bg-white p-4 rounded-xl shadow-sm border border-orange-100 flex justify-between items-center">
                        <div>
                            <p className="font-bold text-slate-800">{req.borrowerName}</p>
                            <p className="text-xs text-slate-500">
                                Requesting <span className="font-bold text-slate-700">KES {Number(req.amount).toLocaleString()}</span>
                            </p>
                            <p className="text-[10px] text-slate-400 mt-1">{req.loanType} • {new Date(req.dateRequested).toLocaleDateString()}</p>
                        </div>

                        <div className="flex gap-2">
                            {processingId === req.requestId ? (
                                <BrandedSpinner size="small"/>
                            ) : (
                                <>
                                    <button
                                        onClick={() => handleResponse(req.requestId, false)}
                                        className="p-2 bg-red-50 text-red-600 rounded-lg hover:bg-red-100 transition"
                                        title="Reject"
                                    >
                                        <XCircle size={18}/>
                                    </button>
                                    <button
                                        onClick={() => handleResponse(req.requestId, true)}
                                        className="px-4 py-2 bg-slate-900 text-white text-xs font-bold rounded-lg hover:bg-emerald-600 transition flex items-center gap-2"
                                    >
                                        Accept <CheckCircle size={14}/>
                                    </button>
                                </>
                            )}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}