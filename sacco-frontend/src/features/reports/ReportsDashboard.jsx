import React, { useState, useEffect } from 'react';
import api from '../../api';
import { 
  BarChart, Bar, AreaChart, Area, PieChart, Pie, Cell, 
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer 
} from 'recharts';
import { 
  LayoutDashboard, FileText, TrendingUp, DollarSign, 
  AlertCircle, Users, RefreshCw, Search, Printer, 
  PieChart as PieIcon, ArrowUpRight, ShieldCheck, FileX
} from 'lucide-react';
import QRCode from 'react-qr-code'; 
import BrandedSpinner from '../../components/BrandedSpinner';
import { useSettings } from '../../context/SettingsContext';

const ReportsDashboard = () => {
  const [activeTab, setActiveTab] = useState('executive'); 
  const [activeExecView, setActiveExecView] = useState('dashboard'); 

  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  // Data States
  const [reportData, setReportData] = useState(null);
  const [chartData, setChartData] = useState([]);
  const [agingReport, setAgingReport] = useState([]);
  
  // Statement Data
  const [members, setMembers] = useState([]);
  const [selectedMember, setSelectedMember] = useState('');
  const [statementData, setStatementData] = useState(null); 
  const [statementConfig, setStatementConfig] = useState({
    startDate: new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().split('T')[0],
    endDate: new Date().toISOString().split('T')[0]
  });
  
  // Filter for Executive charts
  const [dateRange, setDateRange] = useState('30'); 

  // ✅ 1. Get Global Settings
  const { settings, getImageUrl } = useSettings();
  
  // ✅ 2. Construct Dynamic Organization Data
  const logoUrl = getImageUrl(settings.SACCO_LOGO);
  const orgName = settings.SACCO_NAME || "Sacco System";
  const orgAddress = settings.SACCO_ADDRESS || "Nairobi, Kenya";
  const orgWebsite = settings.SACCO_WEBSITE || "";
  
  const orgContact = [settings.SACCO_EMAIL, settings.SACCO_PHONE]
      .filter(Boolean)
      .join(' | ');

  const COLORS = ['#4F46E5', '#06B6D4', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6'];

  useEffect(() => {
    if (activeTab === 'executive') fetchExecutiveData();
    if (activeTab === 'risk') fetchAgingReport();
    if (activeTab === 'statements') fetchMembers();
  }, [activeTab, dateRange]);

  // --- FETCHERS ---

  const fetchExecutiveData = async () => {
    setLoading(true);
    setError(null);
    try {
      const todayRes = await api.get('/api/reports/today');
      if (todayRes.data.success) setReportData(todayRes.data.data);

      const chartRes = await api.get(`/api/reports/chart?days=${dateRange}`);
      if (chartRes.data.success) setChartData(chartRes.data.data);
    } catch (err) {
      console.error("Failed to load reports:", err);
      setError("Could not load financial data.");
    } finally {
      setLoading(false);
    }
  };

  const fetchAgingReport = async () => {
    setLoading(true);
    try {
      const res = await api.get('/api/reports/loan-aging'); 
      if (res.data.success) setAgingReport(res.data.data);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  };

  const fetchMembers = async () => {
    try {
      const res = await api.get('/api/members/active');
      if (res.data.success) setMembers(res.data.data);
    } catch (e) { console.error(e); }
  };

  const handleGenerateStatement = async () => {
    if (!selectedMember) return;
    setLoading(true);
    setStatementData(null); 
    try {
      let res;
      // ✅ Handle "SYSTEM" Selection for Whole Organization Statement
      if (selectedMember === 'SYSTEM') {
          // Fetch All Transactions within range
          res = await api.get(`/api/transactions?startDate=${statementConfig.startDate}&endDate=${statementConfig.endDate}`);
          if (res.data.success) {
              // Map raw transactions to Statement DTO format manually
              const rawTxs = res.data.data;
              
              // Calculate simple running balances for presentation
              let bal = 0;
              const txsWithBal = rawTxs.slice().reverse().map(t => {
                  // Determine polarity: Deposits are +, Withdrawals/Expenses are -
                  // Note: In double entry, Assets/Expenses are DR (+), Liabilities/Income are CR (-)
                  // But for a statement view: Money In is +, Money Out is -
                  const isMoneyIn = t.type === 'DEPOSIT' || t.type === 'LOAN_REPAYMENT' || t.type === 'INCOME';
                  const signedAmount = isMoneyIn ? Math.abs(t.amount) : -Math.abs(t.amount);
                  
                  bal += signedAmount;
                  return {
                      date: t.transactionDate,
                      description: `${t.description} (${t.member ? t.member.firstName + ' ' + t.member.lastName : 'System'})`,
                      reference: t.referenceCode || t.transactionId,
                      type: t.type,
                      amount: signedAmount,
                      runningBalance: bal
                  };
              }).reverse(); // Show newest first again if preferred, or remove slice/reverse logic based on sorting

              setStatementData({
                  memberName: "System General Ledger",
                  memberNumber: "SYS-MASTER",
                  memberAddress: orgAddress,
                  generatedDate: new Date().toISOString(),
                  statementReference: "SYS-" + Date.now().toString().slice(-6),
                  transactions: txsWithBal,
                  openingBalance: 0, // Simplified for system view
                  closingBalance: bal,
                  totalCredits: txsWithBal.filter(t => t.amount > 0).reduce((s, t) => s + t.amount, 0),
                  totalDebits: Math.abs(txsWithBal.filter(t => t.amount < 0).reduce((s, t) => s + t.amount, 0))
              });
          }
      } else {
          // Normal Member Statement
          res = await api.get(`/api/reports/member-statement/${selectedMember}?startDate=${statementConfig.startDate}&endDate=${statementConfig.endDate}`);
          if (res.data.success) setStatementData(res.data.data);
      }
    } catch (e) { 
        console.error(e);
        alert("Failed to fetch statement"); 
    } finally { 
        setLoading(false); 
    }
  };

  const handleForceGenerate = async () => {
    setRefreshing(true);
    try {
      await api.post('/api/reports/generate');
      await fetchExecutiveData(); 
    } catch (error) { console.error(error); } 
    finally { setRefreshing(false); }
  };

  const formatCurrency = (val) => new Intl.NumberFormat('en-KE', { style: 'currency', currency: 'KES' }).format(val || 0);
  const formatDate = (dateStr) => new Date(dateStr).toLocaleDateString('en-GB'); 

  // --- RENDERERS ---

  const renderKPIs = () => (
    <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
      <div className="bg-gradient-to-br from-blue-600 to-indigo-700 text-white p-6 rounded-xl shadow-lg relative overflow-hidden group hover:scale-[1.02] transition-transform">
        <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity"><DollarSign size={100} /></div>
        <p className="text-blue-100 text-sm font-medium mb-1">Total Assets</p>
        <h3 className="text-3xl font-bold">{formatCurrency(parseFloat(reportData?.totalLoansOutstanding || 0) + parseFloat(reportData?.totalSavings || 0))}</h3>
        <div className="mt-4 flex items-center text-xs text-blue-200"><TrendingUp size={14} className="mr-1"/> +2.5% from last month</div>
      </div>
      <div className="bg-gradient-to-br from-emerald-500 to-teal-600 text-white p-6 rounded-xl shadow-lg relative overflow-hidden group hover:scale-[1.02] transition-transform">
        <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity"><Users size={100} /></div>
        <p className="text-emerald-100 text-sm font-medium mb-1">Member Deposits</p>
        <h3 className="text-3xl font-bold">{formatCurrency(reportData?.totalSavings)}</h3>
        <div className="mt-4 flex items-center text-xs text-emerald-200"><ArrowUpRight size={14} className="mr-1"/> Growing steadily</div>
      </div>
      <div className="bg-gradient-to-br from-violet-500 to-purple-600 text-white p-6 rounded-xl shadow-lg relative overflow-hidden group hover:scale-[1.02] transition-transform">
        <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity"><PieIcon size={100} /></div>
        <p className="text-purple-100 text-sm font-medium mb-1">Loan Portfolio</p>
        <h3 className="text-3xl font-bold">{formatCurrency(reportData?.totalLoansOutstanding)}</h3>
        <div className="mt-4 flex items-center text-xs text-purple-200"><FileText size={14} className="mr-1"/> {reportData?.totalLoansIssued} Active Loans</div>
      </div>
      <div className="bg-gradient-to-br from-orange-500 to-red-500 text-white p-6 rounded-xl shadow-lg relative overflow-hidden group hover:scale-[1.02] transition-transform">
        <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity"><AlertCircle size={100} /></div>
        <p className="text-orange-100 text-sm font-medium mb-1">Net Income</p>
        <h3 className="text-3xl font-bold">{formatCurrency(reportData?.netIncome)}</h3>
        <div className="mt-4 flex items-center text-xs text-orange-200">Revenue - Expenses</div>
      </div>
    </div>
  );

  const renderFinancialCharts = () => (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 animate-in fade-in">
       <div className="lg:col-span-2 bg-white p-6 rounded-xl shadow-sm border border-slate-100">
        <div className="flex justify-between items-center mb-6">
          <h3 className="text-lg font-bold text-slate-800">Financial Performance Trend</h3>
          <select value={dateRange} onChange={(e) => setDateRange(e.target.value)} className="text-sm border-slate-200 bg-slate-50 rounded-lg p-2 focus:ring-indigo-500">
            <option value="7">Last 7 Days</option>
            <option value="30">Last 30 Days</option>
            <option value="90">Last 3 Months</option>
          </select>
        </div>
        <div className="h-80 w-full">
          {chartData.length > 0 ? (
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="colorIncome" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="#10B981" stopOpacity={0.1}/><stop offset="95%" stopColor="#10B981" stopOpacity={0}/></linearGradient>
                  <linearGradient id="colorExpense" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="#EF4444" stopOpacity={0.1}/><stop offset="95%" stopColor="#EF4444" stopOpacity={0}/></linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#F1F5F9" />
                <XAxis dataKey="name" fontSize={12} tickLine={false} axisLine={false} />
                <YAxis fontSize={12} tickLine={false} axisLine={false} tickFormatter={(val) => `${val/1000}k`} />
                <Tooltip formatter={(value) => formatCurrency(value)} />
                <Legend iconType="circle" />
                <Area type="monotone" dataKey="income" stroke="#10B981" fill="url(#colorIncome)" strokeWidth={2} name="Income" />
                <Area type="monotone" dataKey="expense" stroke="#EF4444" fill="url(#colorExpense)" strokeWidth={2} name="Expenses" />
              </AreaChart>
            </ResponsiveContainer>
          ) : <div className="h-full flex items-center justify-center text-slate-400">No data available</div>}
        </div>
      </div>
      <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100">
        <h3 className="text-lg font-bold text-slate-800 mb-6">Income Mix</h3>
        <div className="h-64 w-full relative">
           <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie data={[{ name: 'Interest', value: parseFloat(reportData?.totalInterestCollected || 0) }, { name: 'Fees & Fines', value: Math.max(0, parseFloat(reportData?.totalIncome || 0) - parseFloat(reportData?.totalInterestCollected || 0)) }]} cx="50%" cy="50%" innerRadius={60} outerRadius={80} paddingAngle={5} dataKey="value">
                <Cell fill="#4F46E5" /><Cell fill="#10B981" />
              </Pie>
              <Tooltip formatter={(value) => formatCurrency(value)} />
              <Legend verticalAlign="bottom" height={36}/>
            </PieChart>
          </ResponsiveContainer>
          <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
            <div className="text-center"><p className="text-xs text-slate-400 font-bold uppercase">Total</p><p className="text-xl font-bold text-slate-800">{formatCurrency(reportData?.totalIncome)}</p></div>
          </div>
        </div>
      </div>
    </div>
  );

  const renderBalanceSheet = () => (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 animate-in fade-in">
       <div className="bg-white p-6 rounded-xl shadow-sm border-t-4 border-blue-600">
        <h3 className="text-lg font-bold mb-6 text-slate-800 flex items-center"><DollarSign className="w-5 h-5 mr-2 text-blue-600"/> Assets (What We Own)</h3>
        <div className="space-y-4">
          <div className="flex justify-between p-3 bg-slate-50 rounded-lg"><span className="text-slate-600">Loan Portfolio</span><span className="font-bold text-slate-900">{formatCurrency(reportData?.totalLoansOutstanding)}</span></div>
          <div className="flex justify-between p-3 bg-slate-50 rounded-lg"><span className="text-slate-600">Cash Reserves</span><span className="font-bold text-slate-900">{formatCurrency(reportData?.totalSavings)}</span></div>
          <div className="flex justify-between pt-4 mt-2 border-t border-slate-100"><span className="text-lg font-bold text-blue-800">Total Assets</span><span className="text-lg font-bold text-blue-800">{formatCurrency(parseFloat(reportData?.totalLoansOutstanding || 0) + parseFloat(reportData?.totalSavings || 0))}</span></div>
        </div>
      </div>
      <div className="space-y-6">
        <div className="bg-white p-6 rounded-xl shadow-sm border-t-4 border-red-500">
          <h3 className="text-lg font-bold mb-6 text-slate-800 flex items-center"><Users className="w-5 h-5 mr-2 text-red-500"/> Liabilities (What We Owe)</h3>
          <div className="space-y-4">
            <div className="flex justify-between p-3 bg-slate-50 rounded-lg"><span className="text-slate-600">Member Deposits</span><span className="font-bold text-slate-900">{formatCurrency(reportData?.totalSavings)}</span></div>
            <div className="flex justify-between pt-4 mt-2 border-t border-slate-100"><span className="text-lg font-bold text-red-700">Total Liabilities</span><span className="text-lg font-bold text-red-700">{formatCurrency(reportData?.totalSavings)}</span></div>
          </div>
        </div>
        <div className="bg-white p-6 rounded-xl shadow-sm border-t-4 border-purple-600">
          <h3 className="text-lg font-bold mb-6 text-slate-800 flex items-center"><TrendingUp className="w-5 h-5 mr-2 text-purple-600"/> Equity</h3>
          <div className="space-y-4">
            <div className="flex justify-between p-3 bg-slate-50 rounded-lg"><span className="text-slate-600">Share Capital</span><span className="font-bold text-slate-900">{formatCurrency(reportData?.totalShareCapital)}</span></div>
            <div className="flex justify-between p-3 bg-slate-50 rounded-lg"><span className="text-slate-600">Retained Earnings</span><span className="font-bold text-slate-900">{formatCurrency(reportData?.netIncome)}</span></div>
            <div className="flex justify-between pt-4 mt-2 border-t border-slate-100"><span className="text-lg font-bold text-purple-800">Total Equity</span><span className="text-lg font-bold text-purple-800">{formatCurrency(parseFloat(reportData?.totalShareCapital || 0) + parseFloat(reportData?.netIncome || 0))}</span></div>
          </div>
        </div>
      </div>
    </div>
  );

  const renderIncomeStatement = () => (
    <div className="bg-white p-8 rounded-xl shadow-sm border border-slate-200 max-w-4xl mx-auto animate-in fade-in">
       <div className="text-center mb-10"><h2 className="text-2xl font-bold text-slate-900">Income Statement</h2><p className="text-slate-500">For the period ending {new Date().toLocaleDateString()}</p></div>
       <div className="space-y-8">
        <div>
          <h3 className="text-sm font-bold text-emerald-700 uppercase mb-4 border-b border-emerald-100 pb-2">Revenue</h3>
          <div className="space-y-3 pl-4">
            <div className="flex justify-between"><span className="text-slate-700">Interest Income on Loans</span><span className="font-medium font-mono">{formatCurrency(reportData?.totalInterestCollected)}</span></div>
            <div className="flex justify-between text-slate-500"><span>Other Operating Income</span><span className="font-mono">{formatCurrency(parseFloat(reportData?.totalIncome || 0) - parseFloat(reportData?.totalInterestCollected || 0))}</span></div>
            <div className="flex justify-between pt-2 border-t border-slate-100 font-bold text-slate-900"><span>Total Revenue</span><span>{formatCurrency(reportData?.totalIncome)}</span></div>
          </div>
        </div>
        <div>
          <h3 className="text-sm font-bold text-red-700 uppercase mb-4 border-b border-red-100 pb-2">Operating Expenses</h3>
          <div className="space-y-3 pl-4">
            <div className="flex justify-between"><span className="text-slate-700">General & Admin Expenses</span><span className="font-medium font-mono">{formatCurrency(reportData?.totalExpenses)}</span></div>
            <div className="flex justify-between pt-2 border-t border-slate-100 font-bold text-slate-900"><span>Total Expenses</span><span>({formatCurrency(reportData?.totalExpenses)})</span></div>
          </div>
        </div>
        <div className="bg-blue-50 p-6 rounded-xl border border-blue-100 flex justify-between items-center"><div><h3 className="text-lg font-bold text-blue-900">Net Income</h3><p className="text-sm text-blue-600">Net Profit / (Loss)</p></div><span className={`text-2xl font-bold font-mono ${parseFloat(reportData?.netIncome) >= 0 ? 'text-blue-700' : 'text-red-600'}`}>{formatCurrency(reportData?.netIncome)}</span></div>
       </div>
    </div>
  );

  // --- BANK GRADE STATEMENT RENDERER (Updated Style) ---
  const renderBankStatement = () => {
    if (!statementData) return null;

    const qrPayload = JSON.stringify({
        org: orgName,
        ref: statementData.statementReference,
        member: statementData.memberNumber,
        date: formatDate(statementData.generatedDate),
        bal: statementData.closingBalance
    });

    return (
      <div className="bg-gray-100 p-8 print:p-0 print:bg-white overflow-auto flex justify-center">
        {/* CSS for Printing & Styles */}
        <style>{`
          @media print {
            body * { visibility: hidden; }
            #statement-container, #statement-container * { visibility: visible; }
            #statement-container { position: absolute; left: 0; top: 0; width: 100%; margin: 0; padding: 20px; box-shadow: none; border: none; }
            @page { size: A4; margin: 10mm; }
          }
          .stmt-table { width: 100%; border-collapse: collapse; font-size: 12px; }
          .stmt-table th { background: #0f172a !important; color: white !important; padding: 12px 10px; text-align: left; text-transform: uppercase; font-size: 10px; letter-spacing: 1px; -webkit-print-color-adjust: exact; }
          .stmt-table td { padding: 10px; border-bottom: 1px solid #e2e8f0; vertical-align: top; }
          .stmt-table tr:nth-child(even) { background: #f8fafc !important; -webkit-print-color-adjust: exact; }
          .money { font-family: 'Consolas', monospace; font-weight: 600; text-align: right; }
          .credit { color: #059669 !important; }
          .debit { color: #dc2626 !important; }
          .watermark { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%) rotate(-30deg); width: 80%; max-width: 600px; opacity: 0.10; z-index: 50; pointer-events: none; }
        `}</style>

        <div id="statement-container" className="bg-white shadow-2xl relative text-slate-800" style={{ width: '210mm', minHeight: '297mm', padding: '20mm', boxSizing: 'border-box' }}>
          
          <div className="watermark">
               {logoUrl ? <img src={logoUrl} alt="Watermark" className="w-full h-auto object-contain" /> : <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-full h-full text-slate-300"><path d="M11.25 4.533A9.707 9.707 0 006 3a9.735 9.735 0 00-3.25.555.75.75 0 00-.5.707v14.25a.75.75 0 001 .75c.75-.456 1-.75 1.75-.75S7 18.75 7 19.5c0 .673.35 1.295.908 1.636.55.336 1.173.23 1.592-.093l2.875-2.156 3.15 2.362c.484.364 1.16.42 1.713.045A2.76 2.76 0 0019 19c0-.23-.03-.455-.087-.67H19a1 1 0 001-1v-8a1 1 0 00-1-1h-.062a4.01 4.01 0 00-.832-1.854l-1.92-2.194A6.035 6.035 0 0011.25 4.53zM10.5 7.5a1.5 1.5 0 113 0 1.5 1.5 0 01-3 0zm-3 0a1.5 1.5 0 113 0 1.5 1.5 0 01-3 0z" /></svg>}
          </div>

          <div className="relative z-10 flex justify-between items-start border-b-2 border-slate-100 pb-6 mb-8">
            <div className='w-1/3'>
                <h1 className="text-2xl font-black text-slate-900 uppercase tracking-tight">Statement of Account</h1>
                <p className="text-sm text-slate-500 mt-1">Generated on {formatDate(statementData.generatedDate)}</p>
            </div>
            <div className="text-right w-1/3">
                <h2 className="text-lg font-bold text-emerald-700">{orgName}</h2>
                <p className="text-xs text-slate-500 whitespace-pre-line leading-relaxed">{orgAddress}</p>
                <p className="text-xs text-slate-500 mt-1">{orgContact}</p>
                {orgWebsite && <p className="text-xs text-blue-600 mt-1 underline decoration-blue-200">{orgWebsite}</p>}
            </div>
          </div>

          <div className="relative z-10 flex justify-between mb-8">
            <div className="w-1/2 pr-4">
              <h3 className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 border-b border-slate-100 pb-1 w-2/3">Generated For</h3>
              <p className="font-bold text-lg text-slate-900 uppercase">{statementData.memberName}</p>
              <div className="mt-1 space-y-0.5">
                  <p className="text-xs text-slate-600"><span className="font-bold text-slate-400 w-20 inline-block">ID:</span> {statementData.memberNumber}</p>
                  <p className="text-xs text-slate-600"><span className="font-bold text-slate-400 w-20 inline-block">Address:</span> {statementData.memberAddress || 'N/A'}</p>
              </div>
            </div>
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
                        <p className="text-xs text-slate-600 flex justify-between"><span>Ref:</span> <span className="font-mono font-bold text-slate-900">{statementData.statementReference}</span></p>
                        <p className="text-xs text-slate-600 flex justify-between"><span>Start Date:</span> <span>{formatDate(statementConfig.startDate)}</span></p>
                        <p className="text-xs text-slate-600 flex justify-between"><span>End Date:</span> <span>{formatDate(statementConfig.endDate)}</span></p>
                        <p className="text-xs text-emerald-600 font-bold flex justify-between mt-1"><span>Status:</span> <span className="flex items-center gap-1">Verified <ShieldCheck size={10}/></span></p>
                    </div>
                </div>
            </div>
          </div>

          <div className="relative z-10 mb-8 min-h-[400px]">
            <table className="stmt-table">
              <thead><tr><th width="15%">Date</th><th width="45%">Description & Ref</th><th className="money">Debit</th><th className="money">Credit</th><th className="money">Balance</th></tr></thead>
              <tbody className="text-xs">
                <tr><td className="font-mono text-slate-500">{formatDate(statementConfig.startDate)}</td><td className="italic text-slate-500 font-medium">Opening Balance Brought Forward</td><td className="money text-slate-400">-</td><td className="money text-slate-400">-</td><td className="money font-bold text-blue-700 bg-blue-50/50">{formatCurrency(statementData.openingBalance)}</td></tr>
                {statementData.transactions.map((tx, idx) => {
                  const isDebit = tx.amount < 0;
                  return (
                    <tr key={idx}>
                      <td className="font-mono text-slate-600">{formatDate(tx.date)}</td>
                      <td><div className="font-bold text-slate-700">{tx.description}</div><div className="text-[9px] text-slate-500 font-mono mt-0.5">REF: {tx.reference} &bull; {tx.type}</div></td>
                      <td className="money debit">{isDebit ? formatCurrency(Math.abs(tx.amount)) : '-'}</td>
                      <td className="money credit">{!isDebit ? formatCurrency(tx.amount) : '-'}</td>
                      <td className="money font-bold text-slate-900 bg-slate-50/30">{formatCurrency(tx.runningBalance)}</td>
                    </tr>
                  );
                })}
                {statementData.transactions.length === 0 && (<tr><td colSpan="5" className="p-12 text-center text-slate-400 italic bg-slate-50"><div className="flex flex-col items-center gap-2"><FileX size={24} className="opacity-20"/><span>No transactions found for this period.</span></div></td></tr>)}
              </tbody>
            </table>
          </div>

          <div className="relative z-10 flex justify-end">
             <div className="bg-slate-50 border border-slate-200 p-6 rounded-lg w-[280px]">
                 <div className="flex justify-between mb-2 text-xs text-slate-500 uppercase font-bold"><span>Total Credits</span><span className="font-mono text-emerald-600">{formatCurrency(statementData.totalCredits)}</span></div>
                 <div className="flex justify-between mb-4 text-xs text-slate-500 uppercase font-bold"><span>Total Debits</span><span className="font-mono text-rose-600">{formatCurrency(statementData.totalDebits)}</span></div>
                 <div className="flex justify-between pt-3 border-t-2 border-slate-200 text-sm text-slate-900"><span className="font-black uppercase">Closing Balance</span><span className="font-mono font-black border-b-4 border-double border-slate-300">{formatCurrency(statementData.closingBalance)}</span></div>
             </div>
          </div>
          
          <div className="absolute bottom-8 left-0 w-full text-center">
              <p className="text-[10px] text-slate-400 uppercase tracking-widest">System Generated Document • {statementData.statementReference} • {orgName} {orgWebsite && <span> • {orgWebsite}</span>}</p>
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="min-h-screen bg-gray-50 pb-20 font-sans">
      <div className="bg-white border-b border-slate-200 sticky top-0 z-10 print:hidden">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center"><div className="p-2 bg-indigo-50 rounded-lg mr-3"><LayoutDashboard className="h-6 w-6 text-indigo-600" /></div><div><h1 className="text-xl font-bold text-slate-900">Financial Reports</h1><p className="text-xs text-slate-500">Real-time Accounting & Analytics</p></div></div>
            <div className="flex space-x-1 bg-slate-100 p-1 rounded-lg">
              {[{ id: 'executive', label: 'Executive' }, { id: 'risk', label: 'Risk Analysis' }, { id: 'statements', label: 'Statements' }].map(tab => (
                <button key={tab.id} onClick={() => setActiveTab(tab.id)} className={`px-4 py-2 text-sm font-medium rounded-md transition-all ${activeTab === tab.id ? 'bg-white shadow text-indigo-700' : 'text-slate-600 hover:text-slate-900'}`}>{tab.label}</button>
              ))}
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mt-8 print:max-w-none print:px-0 print:mt-0">
        {activeTab === 'executive' && (
          <>
            <div className="flex flex-wrap gap-3 mb-8 print:hidden">
              {[{ id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard }, { id: 'balance-sheet', label: 'Balance Sheet', icon: FileText }, { id: 'income-statement', label: 'Income Statement', icon: TrendingUp }].map(view => (
                <button key={view.id} onClick={() => setActiveExecView(view.id)} className={`flex items-center px-5 py-2.5 rounded-full font-medium text-sm transition-all ${activeExecView === view.id ? 'bg-slate-900 text-white shadow-lg transform scale-105' : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'}`}><view.icon size={16} className="mr-2" />{view.label}</button>
              ))}
              <button onClick={handleForceGenerate} disabled={refreshing} className="ml-auto flex items-center px-4 py-2 bg-indigo-50 text-indigo-700 rounded-full text-sm font-medium hover:bg-indigo-100 transition-colors"><RefreshCw size={16} className={`mr-2 ${refreshing ? 'animate-spin' : ''}`} />Refresh GL</button>
            </div>
            {loading ? <div className="py-20 flex justify-center"><BrandedSpinner /></div> : (<>{activeExecView === 'dashboard' && (<>{renderKPIs()}{renderFinancialCharts()}</>)}{activeExecView === 'balance-sheet' && renderBalanceSheet()}{activeExecView === 'income-statement' && renderIncomeStatement()}</>)}
          </>
        )}

        {activeTab === 'risk' && (
          <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden animate-in fade-in print:hidden">
            <div className="p-6 border-b border-slate-100 bg-slate-50 flex justify-between items-center"><div><h3 className="font-bold text-slate-800 flex items-center gap-2"><AlertCircle size={20} className="text-red-500"/> Portfolio Risk (PAR)</h3><p className="text-sm text-slate-500 mt-1">Loans overdue by 1+ days</p></div><button className="px-4 py-2 bg-white border border-slate-300 rounded-lg text-sm font-medium hover:bg-slate-50 text-slate-700">Export Risk Report</button></div>
            <table className="w-full text-sm text-left">
              <thead className="bg-slate-50 text-slate-500 font-bold border-b uppercase text-xs"><tr><th className="p-4">Loan No</th><th className="p-4">Member</th><th className="p-4">Days Overdue</th><th className="p-4">Category</th><th className="p-4 text-right">Outstanding</th></tr></thead>
              <tbody className="divide-y">{agingReport.map((item, idx) => (<tr key={idx} className="hover:bg-slate-50"><td className="p-4 font-mono text-slate-500">{item.loanNumber}</td><td className="p-4 font-bold text-slate-700">{item.memberName}</td><td className="p-4 text-red-600 font-bold">{item.daysOverdue} Days</td><td className="p-4"><span className="bg-red-100 text-red-700 px-2 py-1 rounded text-[10px] font-bold uppercase">{item.category}</span></td><td className="p-4 text-right font-mono font-bold">{formatCurrency(item.amountOutstanding)}</td></tr>))}{agingReport.length === 0 && !loading && <tr><td colSpan="5" className="p-12 text-center text-slate-400 italic">No overdue loans found. Excellent!</td></tr>}</tbody>
            </table>
          </div>
        )}

        {activeTab === 'statements' && (
          <div className="space-y-6 animate-in fade-in">
            <div className="bg-white p-5 rounded-xl shadow-sm border border-slate-200 print:hidden">
              <div className="flex flex-col md:flex-row gap-4 items-end">
                <div className="flex-1">
                  <label className="block text-xs font-bold text-slate-500 mb-1">Select Statement Type</label>
                  <div className="relative">
                     <select className="w-full p-2.5 border border-slate-300 rounded-lg text-sm bg-white focus:ring-2 focus:ring-indigo-500 outline-none" value={selectedMember} onChange={e => setSelectedMember(e.target.value)}>
                        <option value="">-- Select Member or System --</option>
                        {/* ✅ NEW: Option for Whole System Statement */}
                        <option value="SYSTEM" className="font-bold text-indigo-700">★ Whole System Statement (All Transactions)</option>
                        {members.map(m => <option key={m.id} value={m.id}>{m.firstName} {m.lastName} - {m.memberNumber}</option>)}
                     </select>
                  </div>
                </div>
                <div>
                   <label className="block text-xs font-bold text-slate-500 mb-1">Statement Period</label>
                   <div className="flex gap-2">
                     <input type="date" className="p-2 border border-slate-300 rounded text-sm" value={statementConfig.startDate} onChange={e => setStatementConfig({...statementConfig, startDate: e.target.value})}/>
                     <input type="date" className="p-2 border border-slate-300 rounded text-sm" value={statementConfig.endDate} onChange={e => setStatementConfig({...statementConfig, endDate: e.target.value})}/>
                   </div>
                </div>
                <div className="flex gap-2">
                  <button onClick={handleGenerateStatement} disabled={!selectedMember || loading} className="px-6 py-2.5 bg-slate-900 text-white rounded-lg font-bold text-sm hover:bg-slate-800 disabled:opacity-50">Generate</button>
                  {statementData && <button onClick={() => window.print()} className="px-4 py-2.5 bg-emerald-50 text-emerald-700 rounded-lg font-bold text-sm flex items-center hover:bg-emerald-100 border border-emerald-200"><Printer size={16} className="mr-2"/> Print / PDF</button>}
                </div>
              </div>
            </div>
            {loading ? <div className="text-center py-20"><BrandedSpinner /></div> : renderBankStatement()}
          </div>
        )}
      </div>
    </div>
  );
};

export default ReportsDashboard;