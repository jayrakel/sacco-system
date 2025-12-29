import React, { useState, useEffect } from 'react';
import api from '../../../api';
import { Calendar } from 'lucide-react';

export default function SecretaryDashboard() {
    const [loans, setLoans] = useState([]);
    const [meetingDate, setMeetingDate] = useState('');
    const [selectedLoan, setSelectedLoan] = useState(null);

    useEffect(() => {
        api.get('/api/loans/admin/pending').then(res => {
            if(res.data.success) {
                // Secretary only cares about APPROVED loans
                setLoans(res.data.data.filter(l => l.status === 'APPROVED'));
            }
        });
    }, []);

    const handleTable = async () => {
        await api.post(`/api/loans/secretary/${selectedLoan}/table`, { meetingDate });
        alert("Tabled for meeting!");
        window.location.reload();
    };

    return (
        <div className="bg-white rounded-2xl shadow-sm p-6">
            <h2 className="text-xl font-bold mb-4 flex gap-2"><Calendar/> Meeting Schedule Manager</h2>

            <div className="grid gap-4">
                {loans.map(loan => (
                    <div key={loan.id} className="border p-4 rounded-xl flex justify-between items-center bg-slate-50">
                        <div>
                            <h3 className="font-bold">{loan.memberName}</h3>
                            <span className="text-xs bg-amber-100 text-amber-700 px-2 py-1 rounded">Technically Approved</span>
                        </div>

                        {/* Inline Date Picker for Speed */}
                        <div className="flex items-center gap-2">
                            <input
                                type="date"
                                onChange={(e) => { setMeetingDate(e.target.value); setSelectedLoan(loan.id); }}
                                className="border p-2 rounded"
                            />
                            <button
                                onClick={handleTable}
                                disabled={!meetingDate || selectedLoan !== loan.id}
                                className="px-4 py-2 bg-slate-800 text-white rounded-lg"
                            >
                                Schedule
                            </button>
                        </div>
                    </div>
                ))}
            </div>
            {loans.length === 0 && <p className="text-slate-400">No approved loans waiting for scheduling.</p>}
        </div>
    );
}