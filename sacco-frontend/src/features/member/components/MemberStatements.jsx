import React, { useState, useRef } from 'react';
import api from '../../../api';
import QRCode from 'react-qr-code'; 
import { Printer, Calendar, Search, ShieldCheck, FileX } from 'lucide-react';
import { useSettings } from '../../../context/SettingsContext';

export default function MemberStatements({ user }) {
    const [statement, setStatement] = useState(null);
    const [transactions, setTransactions] = useState([]); 
    const [loading, setLoading] = useState(false);
    
    const { settings, getImageUrl } = useSettings();
    
    const logoUrl = getImageUrl(settings.SACCO_LOGO);
    const orgName = settings.SACCO_NAME || "Sacco System";
    const orgAddress = settings.SACCO_ADDRESS || "Nairobi, Kenya";
    const orgWebsite = settings.SACCO_WEBSITE || "";
    
    const orgContact = [settings.SACCO_EMAIL, settings.SACCO_PHONE]
        .filter(Boolean)
        .join(' | ');

    const [config, setConfig] = useState({
        startDate: new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().split('T')[0], 
        endDate: new Date().toISOString().split('T')[0] 
    });

    const [docMeta, setDocMeta] = useState({ ref: '', generatedAt: '' });
    const statementRef = useRef();

    const handleGenerate = async () => {
        setLoading(true);
        try {
            const res = await api.get(`/api/reports/my-statement?startDate=${config.startDate}&endDate=${config.endDate}`);
            
            if (res.data.success) {
                const data = res.data.data;
                const rawTransactions = data.transactions || [];

                const cleanTransactions = rawTransactions.filter(tx => {
                    const desc = tx.description.toLowerCase();
                    return !desc.includes('registration fee') && !desc.includes('joining fee');
                });

                setStatement(data); 
                setTransactions(cleanTransactions);
                
                const randomRef = Math.floor(1000 + Math.random() * 9000);
                setDocMeta({
                    ref: `STMT-${new Date().getFullYear()}${new Date().getMonth()+1}-${randomRef}`,
                    generatedAt: new Date().toLocaleString()
                });
            }
        } catch (error) {
            console.error(error);
            alert("Failed to generate statement. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    const handlePrint = () => {
        if (!statementRef.current) return;
        window.print();
    };

    const calculateTotals = () => {
        const credit = transactions.reduce((sum, tx) => sum + (tx.amount > 0 ? tx.amount : 0), 0);
        const debit = transactions.reduce((sum, tx) => sum + (tx.amount < 0 ? Math.abs(tx.amount) : 0), 0);
        const closing = transactions.length > 0 ? transactions[transactions.length - 1].runningBalance : (statement?.openingBalance || 0);
        return { credit, debit, closing, opening: statement?.openingBalance || 0 };
    };

    const totals = calculateTotals();

    const qrPayload = statement ? JSON.stringify({
        org: orgName,
        ref: docMeta.ref,
        member: user?.memberNumber,
        date: docMeta.generatedAt,
        bal: totals.closing
    }) : "";

    return (
        <div className="space-y-6 animate-in fade-in pb-10">
            {/* Control Panel */}
            <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 print:hidden flex flex-col md:flex-row gap-4 items-end">
                <div className="flex-1 w-full">
                    <label className="block text-xs font-bold text-slate-500 mb-1 flex items-center gap-1">
                        <Calendar size={14}/> Start Date
                    </label>
                    <input 
                        type="date" 
                        className="w-full p-2.5 border border-slate-300 rounded-xl bg-slate-50 font-medium"
                        value={config.startDate}
                        onChange={e => setConfig({...config, startDate: e.target.value})}
                    />
                </div>
                <div className="flex-1 w-full">
                    <label className="block text-xs font-bold text-slate-500 mb-1 flex items-center gap-1">
                        <Calendar size={14}/> End Date
                    </label>
                    <input 
                        type="date" 
                        className="w-full p-2.5 border border-slate-300 rounded-xl bg-slate-50 font-medium"
                        value={config.endDate}
                        onChange={e => setConfig({...config, endDate: e.target.value})}
                    />
                </div>
                <button 
                    onClick={handleGenerate} 
                    disabled={loading}
                    className="px-6 py-2.5 bg-indigo-900 text-white rounded-xl font-bold hover:bg-indigo-800 transition shadow-lg shadow-indigo-900/20 disabled:opacity-50 flex items-center gap-2"
                >
                    {loading ? 'Generating...' : <><Search size={18}/> Generate Statement</>}
                </button>
                {statement && (
                    <button 
                        onClick={handlePrint}
                        className="px-6 py-2.5 bg-emerald-600 text-white rounded-xl font-bold hover:bg-emerald-700 transition shadow-lg shadow-emerald-900/20 flex items-center gap-2"
                    >
                        <Printer size={18}/> Download PDF
                    </button>
                )}
            </div>

            {/* PREVIEW AREA */}
            {statement && (
                <div className="flex justify-center bg-slate-100 p-8 rounded-xl border border-slate-200 overflow-auto print:p-0 print:bg-white print:border-none print:overflow-visible">
                    
                    {/* A4 CONTAINER */}
                    <div 
                        ref={statementRef} 
                        id="statement-container"
                        className="bg-white shadow-2xl relative text-slate-800 print:shadow-none"
                        style={{
                            width: '210mm',
                            minHeight: '297mm',
                            padding: '20mm',
                            boxSizing: 'border-box'
                        }}
                    >
                        
                        <style>{`
                            @media print {
                                body { 
                                    visibility: hidden; 
                                    margin: 0; 
                                    padding: 0; 
                                    overflow: hidden; 
                                }
                                #statement-container {
                                    visibility: visible;
                                    position: fixed;
                                    left: 0;
                                    top: 0;
                                    margin: 0;
                                    padding: 20mm !important;
                                    width: 210mm;
                                    height: 100%;
                                    z-index: 9999;
                                    background: white;
                                }
                                #statement-container * {
                                    visibility: visible;
                                }
                                .print:hidden { display: none !important; }
                                * { -webkit-print-color-adjust: exact !important; print-color-adjust: exact !important; }
                            }
                            .stmt-table { width: 100%; border-collapse: collapse; font-size: 12px; }
                            .stmt-table th { background: #0f172a !important; color: white !important; padding: 12px 10px; text-align: left; text-transform: uppercase; font-size: 10px; letter-spacing: 1px; }
                            .stmt-table td { padding: 10px; border-bottom: 1px solid #e2e8f0; vertical-align: top; }
                            .stmt-table tr:nth-child(even) { background: #f8fafc !important; }
                            .money { font-family: 'Consolas', monospace; font-weight: 600; text-align: right; }
                            .credit { color: #059669 !important; }
                            .debit { color: #dc2626 !important; }
                            
                            .watermark {
                                position: absolute; 
                                top: 50%; 
                                left: 50%; 
                                transform: translate(-50%, -50%) rotate(-30deg);
                                width: 80%; 
                                max-width: 600px;
                                opacity: 0.15; 
                                z-index: 50; 
                                pointer-events: none; 
                                mix-blend-mode: multiply;
                            }
                        `}</style>

                        {/* WATERMARK */}
                        <div className="watermark">
                             {logoUrl ? (
                                <img src={logoUrl} alt="Watermark" className="w-full h-auto object-contain" />
                             ) : (
                                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-full h-full text-slate-300">
                                    <path d="M12 2L2 22h20L12 2zm0 3.5L18.5 20H5.5L12 5.5z"/>
                                </svg>
                             )}
                        </div>

                        {/* HEADER - No Wrap */}
                        <div className="flex justify-between items-start mb-8 border-b-2 border-slate-100 pb-6 relative z-10">
                            <div className="flex-1">
                                <h1 className="text-2xl font-black text-slate-900 uppercase tracking-tight whitespace-nowrap">Statement of Account</h1>
                                <p className="text-sm text-slate-500 mt-1">Generated on {docMeta.generatedAt}</p>
                            </div>

                            <div className="text-right flex-1">
                                <h2 className="text-lg font-bold text-emerald-700 whitespace-nowrap">{orgName}</h2>
                                <p className="text-xs text-slate-500 whitespace-pre-line leading-relaxed">{orgAddress}</p>
                                <p className="text-xs text-slate-500 mt-1 whitespace-nowrap">{orgContact}</p>
                                {orgWebsite && (
                                    <p className="text-xs text-blue-600 mt-1 underline decoration-blue-200">
                                        {orgWebsite}
                                    </p>
                                )}
                            </div>
                        </div>

                        {/* DETAILS GRID */}
                        <div className="flex justify-between mb-8 relative z-10">
                            <div className="w-1/2 pr-4">
                                <h3 className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 border-b border-slate-100 pb-1 w-2/3">Generated For</h3>
                                <p className="font-bold text-lg text-slate-900 uppercase whitespace-nowrap">{user?.firstName} {user?.lastName}</p>
                                <div className="mt-1 space-y-0.5">
                                    <p className="text-xs text-slate-600 flex gap-2"><span className="font-bold text-slate-400 inline-block w-20">Member No:</span> {user?.memberNumber || 'N/A'}</p>
                                    <p className="text-xs text-slate-600 flex gap-2"><span className="font-bold text-slate-400 inline-block w-20">Email:</span> {user?.email}</p>
                                    <p className="text-xs text-slate-600 flex gap-2"><span className="font-bold text-slate-400 inline-block w-20">Phone:</span> {user?.phoneNumber || 'N/A'}</p>
                                </div>
                            </div>
                            
                            {/* QR CODE */}
                            <div className="absolute top-0 left-1/2 -translate-x-1/2 text-center">
                                <div className="bg-white p-1 border border-slate-200 inline-block">
                                    <QRCode value={qrPayload} size={80} level="M" />
                                </div>
                                <span className="block text-[9px] font-bold text-slate-400 uppercase mt-1 tracking-widest">Official Seal</span>
                            </div>

                            <div className="w-1/2 pl-4 text-right">
                                <div className="ml-auto w-2/3">
                                    <h3 className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 border-b border-slate-100 pb-1">Document Details</h3>
                                    <div className="space-y-0.5">
                                        <p className="text-xs text-slate-600 flex justify-between"><span>Ref:</span> <span className="font-mono font-bold text-slate-900">{docMeta.ref}</span></p>
                                        <p className="text-xs text-slate-600 flex justify-between"><span>Start Date:</span> <span>{new Date(config.startDate).toLocaleDateString()}</span></p>
                                        <p className="text-xs text-slate-600 flex justify-between"><span>End Date:</span> <span>{new Date(config.endDate).toLocaleDateString()}</span></p>
                                        <p className="text-xs text-emerald-600 font-bold flex justify-between mt-1"><span>Status:</span> <span className="flex items-center gap-1">Verified <ShieldCheck size={10}/></span></p>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* TABLE */}
                        <div className="relative z-10 mb-8 min-h-[400px]">
                            <table className="stmt-table">
                                <thead>
                                    <tr>
                                        <th width="15%">Date</th>
                                        <th width="45%">Description & Ref</th>
                                        <th className="money">Debit</th>
                                        <th className="money">Credit</th>
                                        <th className="money">Balance</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {/* Opening Balance Row (Added Explicitly) */}
                                    <tr>
                                        <td className="font-mono text-slate-500">{new Date(config.startDate).toLocaleDateString()}</td>
                                        <td className="italic text-slate-500 font-medium">Opening Balance Brought Forward</td>
                                        <td className="money text-slate-400">-</td>
                                        <td className="money text-slate-400">-</td>
                                        <td className="money font-bold text-blue-700 bg-blue-50/50">{Number(totals.opening).toLocaleString()}</td>
                                    </tr>

                                    {transactions.map((tx, i) => (
                                        <tr key={i}>
                                            <td className="font-mono text-slate-600">{new Date(tx.date).toLocaleDateString()}</td>
                                            <td>
                                                <div className="font-bold text-slate-700 text-xs">{tx.description}</div>
                                                <div className="text-[9px] text-slate-400 font-mono uppercase mt-0.5">REF: {tx.reference}</div>
                                            </td>
                                            <td className="money debit">
                                                {tx.amount < 0 ? Number(Math.abs(tx.amount)).toLocaleString() : '-'}
                                            </td>
                                            <td className="money credit">
                                                {tx.amount > 0 ? Number(tx.amount).toLocaleString() : '-'}
                                            </td>
                                            <td className="money font-bold text-slate-900">
                                                {Number(tx.runningBalance).toLocaleString()}
                                            </td>
                                        </tr>
                                    ))}
                                    {transactions.length === 0 && (
                                        <tr><td colSpan="5" className="p-12 text-center text-slate-400 italic bg-slate-50">
                                            <div className="flex flex-col items-center gap-2">
                                                <FileX size={24} className="opacity-20"/>
                                                <span>No transactions found for this period.</span>
                                            </div>
                                        </td></tr>
                                    )}
                                </tbody>
                            </table>
                        </div>

                        {/* SUMMARY FOOTER (Updated with Opening Balance) */}
                        <div className="flex justify-end relative z-10">
                            <div className="bg-slate-50 border border-slate-200 p-6 rounded-lg w-[320px]">
                                <div className="flex justify-between mb-2 text-xs text-slate-500 uppercase font-bold">
                                    <span>Opening Balance</span>
                                    <span className="font-mono text-slate-700">{Number(totals.opening).toLocaleString()}</span>
                                </div>
                                <div className="flex justify-between mb-2 text-xs text-slate-500 uppercase font-bold">
                                    <span>Total Credits</span>
                                    <span className="font-mono text-emerald-600">{Number(totals.credit).toLocaleString()}</span>
                                </div>
                                <div className="flex justify-between mb-4 text-xs text-slate-500 uppercase font-bold">
                                    <span>Total Debits</span>
                                    <span className="font-mono text-rose-600">{Number(totals.debit).toLocaleString()}</span>
                                </div>
                                <div className="flex justify-between pt-3 border-t-2 border-slate-200 text-sm text-slate-900">
                                    <span className="font-black uppercase">Closing Balance</span>
                                    <span className="font-mono font-black border-b-4 border-double border-slate-300">{Number(totals.closing).toLocaleString()}</span>
                                </div>
                            </div>
                        </div>

                        {/* PRINT FOOTER */}
                        <div className="absolute bottom-8 left-0 w-full text-center">
                            <p className="text-[10px] text-slate-400 uppercase tracking-widest">
                                System Generated Document • {docMeta.ref} • {orgName}
                                {orgWebsite && <span> • {orgWebsite}</span>}
                            </p>
                        </div>

                    </div>
                </div>
            )}
        </div>
    );
}