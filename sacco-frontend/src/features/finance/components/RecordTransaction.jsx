import React from 'react';
import { PlusCircle } from 'lucide-react';

export default function RecordTransaction({ onSuccess }) {
    // Placeholder for transaction recording logic
    const handleRecord = () => {
        const amount = prompt("Enter amount to record (simulated):");
        if (amount) {
            alert(`Recorded transaction of KES ${amount}`);
            if (onSuccess) onSuccess();
        }
    };

    return (
        <div className="bg-gradient-to-br from-slate-900 to-slate-800 rounded-2xl shadow-lg p-6 text-white">
            <h3 className="font-bold text-lg mb-2">Quick Action</h3>
            <p className="text-slate-400 text-sm mb-6">Manually record an offline transaction or expense.</p>

            <button
                onClick={handleRecord}
                className="w-full bg-white text-slate-900 py-3 rounded-xl font-bold hover:bg-slate-50 transition flex items-center justify-center gap-2"
            >
                <PlusCircle size={18} /> Record Transaction
            </button>
        </div>
    );
}