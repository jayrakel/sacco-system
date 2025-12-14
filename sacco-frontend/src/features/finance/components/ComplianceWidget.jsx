import React from 'react';
import { ShieldCheck, AlertTriangle, CheckCircle } from 'lucide-react';

export default function ComplianceWidget({ onComplete }) {
    // This is a static placeholder for now, mimicking the functionality from the other repo
    const status = "compliant"; // or "risk"

    return (
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6">
            <div className="flex items-center gap-3 mb-4">
                <div className={`p-2 rounded-lg ${status === 'compliant' ? 'bg-emerald-100 text-emerald-600' : 'bg-amber-100 text-amber-600'}`}>
                    {status === 'compliant' ? <ShieldCheck size={20} /> : <AlertTriangle size={20} />}
                </div>
                <h3 className="font-bold text-slate-800">System Compliance</h3>
            </div>

            <div className="space-y-3">
                <div className="flex items-center justify-between text-sm">
                    <span className="text-slate-500">Daily Reconciliation</span>
                    <span className="text-emerald-600 font-bold flex items-center gap-1"><CheckCircle size={14} /> Done</span>
                </div>
                <div className="flex items-center justify-between text-sm">
                    <span className="text-slate-500">Backup Status</span>
                    <span className="text-emerald-600 font-bold flex items-center gap-1"><CheckCircle size={14} /> Secured</span>
                </div>
                <div className="w-full bg-slate-100 h-2 rounded-full overflow-hidden mt-2">
                    <div className="bg-emerald-500 h-full w-full"></div>
                </div>
                <p className="text-xs text-slate-400 text-center pt-2">Last check: Just now</p>
            </div>
        </div>
    );
}