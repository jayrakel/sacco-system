import React, { useState, useEffect } from 'react';
import api from '../../api';
import { 
  BarChart, Bar, AreaChart, Area, PieChart, Pie, Cell, 
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer 
} from 'recharts';
import { 
  LayoutDashboard, FileText, TrendingUp, DollarSign, 
  AlertCircle, Users, RefreshCw, Search, Printer, 
  PieChart as PieIcon, ArrowUpRight, ShieldCheck, FileX,
  CheckCircle, AlertTriangle
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

  // --- DATA STATES ---
  const [reportData, setReportData] = useState(null); 
  const [accounts, setAccounts] = useState([]);       
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
  const [printingExecReport, setPrintingExecReport] = useState(null); 

  // Settings
  const { settings, getImageUrl } = useSettings();
  const logoUrl = getImageUrl(settings.SACCO_LOGO);
  const orgName = settings.SACCO_NAME || "Sacco System";
  const orgAddress = settings.SACCO_ADDRESS || "Nairobi, Kenya";
  const orgWebsite = settings.SACCO_WEBSITE || "";
  const orgContact = [settings.SACCO_EMAIL, settings.SACCO_PHONE].filter(Boolean).join(' | ');

  const COLORS = ['#4F46E5', '#06B6D4', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6'];

  useEffect(() => {
    if (activeTab === 'executive') fetchExecutiveData();
  }, [activeTab, dateRange]);

  useEffect(() => {
    if (activeTab === 'risk') fetchAgingReport();
  }, [activeTab]);

  useEffect(() => {
    if (activeTab === 'statements') fetchMembers();
  }, [activeTab]);

  // --- FETCHERS ---

  const fetchExecutiveData = async () => {
    setLoading(true);
    setError(null);
    try {
      const todayRes = await api.get('/api/reports/today');
      if (todayRes.data.success) setReportData(todayRes.data.data);

      const accRes = await api.get('/api/accounting/accounts');
      if (accRes.data.success) setAccounts(accRes.data.data);

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
      if (selectedMember === 'SYSTEM') {
          res = await api.get(`/api/transactions?startDate=${statementConfig.startDate}&endDate=${statementConfig.endDate}`);
          if (res.data.success) {
              const rawTxs = res.data.data;
              let bal = 0;
              const txsWithBal = rawTxs.slice().reverse().map(t => {
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
              }).reverse(); 

              setStatementData({
                  memberName: "System General Ledger",
                  memberNumber: "SYS-MASTER",
                  memberAddress: orgAddress,
                  generatedDate: new Date().toISOString(),
                  statementReference: "SYS-" + Date.now().toString().slice(-6),
                  transactions: txsWithBal,
                  openingBalance: 0,
                  closingBalance: bal,
                  totalCredits: txsWithBal.filter(t => t.amount > 0).reduce((s, t) => s + t.amount, 0),
                  totalDebits: Math.abs(txsWithBal.filter(t => t.amount < 0).reduce((s, t) => s + t.amount, 0))
              });
          }
      } else {
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

  const handlePrintExecReport = (type) => {
      setPrintingExecReport(type);
      setTimeout(() => {
          window.print();
          setPrintingExecReport(null);
      }, 500);
  };

  const formatCurrency = (val) => new Intl.NumberFormat('en-KE', { style: 'currency', currency: 'KES' }).format(val || 0);
  const formatDate = (dateStr) => new Date(dateStr).toLocaleDateString('en-GB'); 

  // --- ACCOUNTING CALCULATIONS ---
  const getTotal = (type) => accounts
      .filter(a => a.type === type && a.active)
      .reduce((sum, a) => sum + parseFloat(a.balance || 0), 0);

  const getNetIncome = () => getTotal('INCOME') - getTotal('EXPENSE');

  const totalAssets = getTotal('ASSET');
  const totalLiabilities = getTotal('LIABILITY');
  const totalEquity = getTotal('EQUITY') + getNetIncome(); 
  const balanceDifference = totalAssets - (totalLiabilities + totalEquity);
  const isBalanced = Math.abs(balanceDifference) < 1.0;

  // --- RENDERERS ---

  const renderKPIs = () => (
    <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
      <div className="bg-gradient-to-br from-blue-600 to-indigo-700 text-white p-6 rounded-xl shadow-lg relative overflow-hidden group hover:scale-[1.02] transition-transform">
        <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity"><DollarSign size={100} /></div>
        <p className="text-blue-100 text-sm font-medium mb-1">Total Assets</p>
        <h3 className="text-3xl font-bold">{formatCurrency(totalAssets)}</h3>
        <div className="mt-4 flex items-center text-xs text-blue-200"><TrendingUp size={14} className="mr-1"/> Book Value</div>
      </div>
      <div className="bg-gradient-to-br from-emerald-500 to-teal-600 text-white p-6 rounded-xl shadow-lg relative overflow-hidden group hover:scale-[1.02] transition-transform">
        <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity"><Users size={100} /></div>
        <p className="text-emerald-100 text-sm font-medium mb-1">Total Liabilities</p>
        <h3 className="text-3xl font-bold">{formatCurrency(totalLiabilities)}</h3>
        <div className="mt-4 flex items-center text-xs text-emerald-200"><ArrowUpRight size={14} className="mr-1"/> Includes Deposits</div>
      </div>
      <div className="bg-gradient-to-br from-violet-500 to-purple-600 text-white p-6 rounded-xl shadow-lg relative overflow-hidden group hover:scale-[1.02] transition-transform">
        <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity"><PieIcon size={100} /></div>
        <p className="text-purple-100 text-sm font-medium mb-1">Equity</p>
        <h3 className="text-3xl font-bold">{formatCurrency(totalEquity)}</h3>
        <div className="mt-4 flex items-center text-xs text-purple-200"><FileText size={14} className="mr-1"/> Capital + Retained Earnings</div>
      </div>
      <div className="bg-gradient-to-br from-orange-500 to-red-500 text-white p-6 rounded-xl shadow-lg relative overflow-hidden group hover:scale-[1.02] transition-transform">
        <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity"><AlertCircle size={100} /></div>
        <p className="text-orange-100 text-sm font-medium mb-1">Net Income</p>
        <h3 className="text-3xl font-bold">{formatCurrency(getNetIncome())}</h3>
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
              <Pie data={[{ name: 'Revenue', value: getTotal('INCOME') }, { name: 'Expenses', value: getTotal('EXPENSE') }]} cx="50%" cy="50%" innerRadius={60} outerRadius={80} paddingAngle={5} dataKey="value">
                <Cell fill="#10B981" /><Cell fill="#EF4444" />
              </Pie>
              <Tooltip formatter={(value) => formatCurrency(value)} />
              <Legend verticalAlign="bottom" height={36}/>
            </PieChart>
          </ResponsiveContainer>
          <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
            <div className="text-center"><p className="text-xs text-slate-400 font-bold uppercase">Net Profit</p><p className="text-xl font-bold text-slate-800">{formatCurrency(getNetIncome())}</p></div>
          </div>
        </div>
      </div>
    </div>
  );

  const renderBalanceSheet = () => (
    <div className="space-y-6 animate-in fade-in">
      <div className={`p-4 rounded-xl flex items-center justify-between shadow-sm border ${isBalanced ? 'bg-emerald-50 border-emerald-200' : 'bg-red-50 border-red-200'}`}>
          <div className="flex items-center gap-3">
              <div className={`p-2 rounded-full ${isBalanced ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'}`}>
                  {isBalanced ? <CheckCircle size={24} /> : <AlertTriangle size={24} />}
              </div>
              <div>
                  <h3 className={`font-bold ${isBalanced ? 'text-emerald-900' : 'text-red-900'}`}>
                      {isBalanced ? 'Balance Sheet is Balanced' : 'Balance Sheet is Unbalanced'}
                  </h3>
                  {!isBalanced && <p className="text-sm text-red-700">Difference: <strong>{formatCurrency(balanceDifference)}</strong> (Assets ≠ Liab + Equity)</p>}
              </div>
          </div>
          <button onClick={() => handlePrintExecReport('balance-sheet')} className="bg-white border border-slate-300 text-slate-700 px-4 py-2 rounded-lg font-bold text-sm flex items-center gap-2 hover:bg-slate-50 transition shadow-sm">
              <Printer size={16}/> Download PDF
          </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-white p-6 rounded-xl shadow-sm border-t-4 border-blue-600">
          <h3 className="text-lg font-bold mb-6 text-slate-800 flex items-center"><DollarSign className="w-5 h-5 mr-2 text-blue-600"/> Assets (What We Own)</h3>
          <div className="space-y-2">
            {accounts.filter(a => a.type === 'ASSET' && a.active).map(a => (
                <div key={a.code} className="flex justify-between p-2 bg-slate-50 rounded-lg text-sm">
                    <span className="text-slate-600">{a.name} <span className="text-xs text-slate-300">({a.code})</span></span>
                    <span className="font-bold text-slate-900">{formatCurrency(a.balance)}</span>
                </div>
            ))}
            <div className="flex justify-between pt-4 mt-2 border-t border-slate-100"><span className="text-lg font-bold text-blue-800">Total Assets</span><span className="text-lg font-bold text-blue-800">{formatCurrency(totalAssets)}</span></div>
          </div>
        </div>
        <div className="space-y-6">
          <div className="bg-white p-6 rounded-xl shadow-sm border-t-4 border-red-500">
            <h3 className="text-lg font-bold mb-6 text-slate-800 flex items-center"><Users className="w-5 h-5 mr-2 text-red-500"/> Liabilities (What We Owe)</h3>
            <div className="space-y-2">
              {accounts.filter(a => a.type === 'LIABILITY' && a.active).map(a => (
                  <div key={a.code} className="flex justify-between p-2 bg-slate-50 rounded-lg text-sm">
                      <span className="text-slate-600">{a.name} <span className="text-xs text-slate-300">({a.code})</span></span>
                      <span className="font-bold text-slate-900">{formatCurrency(a.balance)}</span>
                  </div>
              ))}
              <div className="flex justify-between pt-4 mt-2 border-t border-slate-100"><span className="text-lg font-bold text-red-700">Total Liabilities</span><span className="text-lg font-bold text-red-700">{formatCurrency(totalLiabilities)}</span></div>
            </div>
          </div>
          <div className="bg-white p-6 rounded-xl shadow-sm border-t-4 border-purple-600">
            <h3 className="text-lg font-bold mb-6 text-slate-800 flex items-center"><TrendingUp className="w-5 h-5 mr-2 text-purple-600"/> Equity</h3>
            <div className="space-y-2">
              {accounts.filter(a => a.type === 'EQUITY' && a.active).map(a => (
                  <div key={a.code} className="flex justify-between p-2 bg-slate-50 rounded-lg text-sm">
                      <span className="text-slate-600">{a.name} <span className="text-xs text-slate-300">({a.code})</span></span>
                      <span className="font-bold text-slate-900">{formatCurrency(a.balance)}</span>
                  </div>
              ))}
              <div className="flex justify-between p-2 bg-purple-50 rounded-lg text-sm border border-purple-100">
                  <span className="text-purple-900">Net Income (Retained Earnings)</span>
                  <span className="font-bold text-purple-900">{formatCurrency(getNetIncome())}</span>
              </div>
              <div className="flex justify-between pt-4 mt-2 border-t border-slate-100"><span className="text-lg font-bold text-purple-800">Total Equity</span><span className="text-lg font-bold text-purple-800">{formatCurrency(totalEquity)}</span></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  const renderIncomeStatement = () => (
    <div className="bg-white p-8 rounded-xl shadow-sm border border-slate-200 max-w-4xl mx-auto animate-in fade-in">
       <div className="text-center mb-10 flex justify-between items-center">
           <div className="text-left">
               <h2 className="text-2xl font-bold text-slate-900">Income Statement</h2>
               <p className="text-slate-500">For the period ending {new Date().toLocaleDateString()}</p>
           </div>
           <button onClick={() => handlePrintExecReport('income-statement')} className="bg-white border border-slate-300 text-slate-700 px-4 py-2 rounded-lg font-bold text-sm flex items-center gap-2 hover:bg-slate-50 transition shadow-sm">
                <Printer size={16}/> Download PDF
           </button>
       </div>
       <div className="space-y-8">
        <div>
          <h3 className="text-sm font-bold text-emerald-700 uppercase mb-4 border-b border-emerald-100 pb-2">Revenue</h3>
          <div className="space-y-3 pl-4">
            {accounts.filter(a => a.type === 'INCOME' && a.active).map(a => (
                <div key={a.code} className="flex justify-between">
                    <span className="text-slate-700">{a.name}</span>
                    <span className="font-medium font-mono">{formatCurrency(a.balance)}</span>
                </div>
            ))}
            <div className="flex justify-between pt-2 border-t border-slate-100 font-bold text-slate-900"><span>Total Revenue</span><span>{formatCurrency(getTotal('INCOME'))}</span></div>
          </div>
        </div>
        <div>
          <h3 className="text-sm font-bold text-red-700 uppercase mb-4 border-b border-red-100 pb-2">Operating Expenses</h3>
          <div className="space-y-3 pl-4">
            {accounts.filter(a => a.type === 'EXPENSE' && a.active).map(a => (
                <div key={a.code} className="flex justify-between">
                    <span className="text-slate-700">{a.name}</span>
                    <span className="font-medium font-mono">{formatCurrency(a.balance)}</span>
                </div>
            ))}
            <div className="flex justify-between pt-2 border-t border-slate-100 font-bold text-slate-900"><span>Total Expenses</span><span>{formatCurrency(getTotal('EXPENSE'))}</span></div>
          </div>
        </div>
        <div className="bg-blue-50 p-6 rounded-xl border border-blue-100 flex justify-between items-center"><div><h3 className="text-lg font-bold text-blue-900">Net Income</h3><p className="text-sm text-blue-600">Net Profit / (Loss)</p></div><span className={`text-2xl font-bold font-mono ${getNetIncome() >= 0 ? 'text-blue-700' : 'text-red-600'}`}>{formatCurrency(getNetIncome())}</span></div>
       </div>
    </div>
  );

  // --- EXECUTIVE PRINTABLE REPORT RENDERER ---
  const renderPrintableExecReport = () => (
      <div id="printable-area" className="fixed inset-0 bg-white z-[9999] overflow-auto hidden print:block">
          <style>{`
              @media print {
                  @page { size: A4; margin: 15mm 15mm 25mm 15mm; } 
                  html, body { overflow: visible !important; height: auto !important; margin: 0 !important; padding: 0 !important; }
                  body * { visibility: hidden; } 
                  #printable-area, #printable-area * { visibility: visible; }
                  #printable-area {
                      position: absolute; left: 0; top: 0; width: 100%; height: auto;
                      margin: 0; padding: 0; background: transparent; /* Transparent so footer shows */
                      display: block !important; overflow: visible !important;
                  }
                  .print-footer {
                      position: fixed; bottom: 0; left: 0; width: 100%; 
                      text-align: center; font-size: 10px; color: #475569; font-weight: bold;
                      text-transform: uppercase; letter-spacing: 0.1em;
                      padding-bottom: 5mm; background-color: white; z-index: 10000;
                  }
                  .print-hidden { display: none !important; }
              }
              .watermark { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%) rotate(-30deg); width: 80%; max-width: 500px; opacity: 0.08; z-index: -1; pointer-events: none; }
          `}</style>

          <div className="watermark">
              {logoUrl ? <img src={logoUrl} alt="Watermark" className="w-full h-auto object-contain" /> : null}
          </div>

          <div className="flex justify-between items-start border-b-2 border-slate-800 pb-4 mb-8">
              <div>
                  {logoUrl && <img src={logoUrl} alt="Logo" className="h-16 w-auto mb-2 object-contain" />}
                  <h1 className="text-xl font-black text-slate-900 uppercase tracking-tight whitespace-nowrap">{printingExecReport === 'balance-sheet' ? 'Statement of Financial Position' : 'Statement of Comprehensive Income'}</h1>
                  <p className="text-sm text-slate-600 mt-1">As of {new Date().toLocaleDateString('en-GB')}</p>
              </div>
              <div className="text-right flex-1 pl-4">
                <h2 className="text-lg font-bold text-emerald-700 whitespace-nowrap">{orgName}</h2>
                <p className="text-xs text-slate-500 whitespace-pre-line leading-relaxed">{orgAddress}</p>
                <p className="text-xs text-slate-500 mt-1 whitespace-nowrap">{orgContact}</p>
            </div>
          </div>

          <div className="min-h-[500px]">
              {printingExecReport === 'balance-sheet' ? (
                  <div className="grid grid-cols-2 gap-8">
                      <div>
                          <h3 className="font-bold text-slate-800 border-b border-slate-300 pb-2 mb-4">ASSETS</h3>
                          {accounts.filter(a => a.type === 'ASSET' && a.active).map(a => (
                              <div key={a.code} className="flex justify-between py-1 text-sm"><span className="text-slate-700">{a.name}</span><span className="font-mono">{formatCurrency(a.balance)}</span></div>
                          ))}
                          <div className="flex justify-between border-t-2 border-slate-800 pt-2 mt-4 font-bold"><span className="uppercase">Total Assets</span><span>{formatCurrency(totalAssets)}</span></div>
                      </div>
                      <div className="space-y-8 text-sm">
                          <div>
                              <h3 className="font-bold text-slate-800 border-b border-slate-300 pb-2 mb-4">LIABILITIES</h3>
                              {accounts.filter(a => a.type === 'LIABILITY' && a.active).map(a => (
                                  <div key={a.code} className="flex justify-between py-1 text-sm"><span className="text-slate-700">{a.name}</span><span className="font-mono">{formatCurrency(a.balance)}</span></div>
                              ))}
                              <div className="flex justify-between border-t border-slate-300 pt-2 mt-2 font-bold"><span>Total Liabilities</span><span>{formatCurrency(totalLiabilities)}</span></div>
                          </div>
                          <div>
                              <h3 className="font-bold text-slate-800 border-b border-slate-300 pb-2 mb-4">EQUITY</h3>
                              {accounts.filter(a => a.type === 'EQUITY' && a.active).map(a => (
                                  <div key={a.code} className="flex justify-between py-1 text-sm"><span className="text-slate-700">{a.name}</span><span className="font-mono">{formatCurrency(a.balance)}</span></div>
                              ))}
                              <div className="flex justify-between py-1 text-sm"><span className="text-slate-700">Net Income</span><span className="font-mono">{formatCurrency(getNetIncome())}</span></div>
                              <div className="flex justify-between border-t border-slate-300 pt-2 mt-2 font-bold"><span>Total Equity</span><span>{formatCurrency(totalEquity)}</span></div>
                          </div>
                          <div className="flex justify-between border-t-2 border-slate-800 pt-2 font-bold"><span className="uppercase">Total Liab. & Equity</span><span>{formatCurrency(totalLiabilities + totalEquity)}</span></div>
                      </div>
                  </div>
              ) : (
                  <div className="max-w-3xl mx-auto">
                      <h3 className="font-bold text-slate-800 border-b border-slate-300 pb-2 mb-4">REVENUE</h3>
                      {accounts.filter(a => a.type === 'INCOME' && a.active).map(a => (
                          <div key={a.code} className="flex justify-between py-1 text-sm"><span className="text-slate-700">{a.name}</span><span className="font-mono">{formatCurrency(a.balance)}</span></div>
                      ))}
                      <div className="flex justify-between border-t border-slate-300 pt-2 mt-2 mb-8 font-bold"><span>Total Revenue</span><span>{formatCurrency(getTotal('INCOME'))}</span></div>

                      <h3 className="font-bold text-slate-800 border-b border-slate-300 pb-2 mb-4">EXPENSES</h3>
                      {accounts.filter(a => a.type === 'EXPENSE' && a.active).map(a => (
                          <div key={a.code} className="flex justify-between py-1 text-sm"><span className="text-slate-700">{a.name}</span><span className="font-mono">{formatCurrency(a.balance)}</span></div>
                      ))}
                      <div className="flex justify-between border-t border-slate-300 pt-2 mt-2 font-bold"><span>Total Expenses</span><span>{formatCurrency(getTotal('EXPENSE'))}</span></div>

                      <div className="flex justify-between border-t-4 double border-slate-800 pt-4 mt-8 text-xl font-bold"><span>NET INCOME</span><span>{formatCurrency(getNetIncome())}</span></div>
                  </div>
              )}
          </div>

          <div className="mt-20 grid grid-cols-2 gap-20 pb-10">
              <div className="border-t border-slate-400 pt-2">
                  <p className="font-bold text-slate-800 text-sm">Prepared By:</p>
                  <p className="text-xs text-slate-500 mt-8">Signature & Date</p>
              </div>
              <div className="border-t border-slate-400 pt-2">
                  <p className="font-bold text-slate-800 text-sm">Approved By:</p>
                  <p className="text-xs text-slate-500 mt-8">Signature & Date</p>
              </div>
          </div>
          
          {/* Repeating Footer - Positioned Fixed */}
          <div className="print-footer">
              SYSTEM GENERATED DOCUMENT • {new Date().toISOString()} • BETTERLINK VENTURES LIMITED
          </div>
      </div>
  );

  // --- BANK GRADE STATEMENT RENDERER ---
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
      <div className="bg-gray-100 p-8 print:p-0 print:bg-white overflow-auto flex justify-center print:block">
        <style>{`
          @media print {
            /* 1. Global Reset & Page Margin for Footer */
            /* Margin bottom 25mm reserves space for the fixed footer */
            @page { size: A4; margin: 15mm 15mm 25mm 15mm; } 
            
            html, body { overflow: visible !important; height: auto !important; margin: 0 !important; padding: 0 !important; }
            body * { visibility: hidden; } /* Hide entire UI */

            /* 2. Target Specific Container */
            #printable-area, #printable-area * { visibility: visible; }
            
            #printable-area {
                position: absolute;
                left: 0;
                top: 0;
                width: 100%; /* Fit width */
                height: auto;
                margin: 0;
                padding: 0; 
                background: transparent; /* Critical: Transparent so footer shows */
                display: block !important;
                overflow: visible !important;
            }
            
            /* 3. Sticky Footer Logic */
            .print-footer {
                position: fixed;
                bottom: 0;
                left: 0;
                width: 100%;
                text-align: center;
                font-size: 10px;
                color: #475569;
                font-weight: bold;
                text-transform: uppercase;
                letter-spacing: 0.1em;
                /* Padding to pull it off the very edge */
                padding-bottom: 5mm; 
                background-color: white;
                z-index: 10000;
            }
          }
          
          /* Common Styles */
          .stmt-table { width: 100%; border-collapse: collapse; font-size: 11px; table-layout: fixed; }
          .stmt-table th { background: #0f172a !important; color: white !important; padding: 8px; text-align: left; text-transform: uppercase; font-size: 9px; letter-spacing: 0.5px; -webkit-print-color-adjust: exact; }
          .stmt-table td { padding: 8px; border-bottom: 1px solid #e2e8f0; vertical-align: top; word-wrap: break-word; }
          .stmt-table tr:nth-child(even) { background: #f8fafc !important; -webkit-print-color-adjust: exact; }
          .money { font-family: 'Consolas', monospace; font-weight: 600; text-align: right; }
          .credit { color: #059669 !important; }
          .debit { color: #dc2626 !important; }
          .watermark { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%) rotate(-30deg); width: 70%; opacity: 0.06; z-index: 0; pointer-events: none; }
        `}</style>

        {/* Added ID #printable-area for strict targeting */}
        <div id="printable-area" className="bg-white shadow-2xl relative text-slate-800 print:shadow-none print:w-full" style={{ maxWidth: '210mm', minHeight: '297mm', padding: '10mm', margin: '0 auto' }}>
          
          <div className="watermark">
               {logoUrl ? <img src={logoUrl} alt="Watermark" className="w-full h-auto object-contain" /> : <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-full h-full text-slate-300"><path d="M11.25 4.533A9.707 9.707 0 006 3a9.735 9.735 0 00-3.25.555.75.75 0 00-.5.707v14.25a.75.75 0 001 .75c.75-.456 1-.75 1.75-.75S7 18.75 7 19.5c0 .673.35 1.295.908 1.636.55.336 1.173.23 1.592-.093l2.875-2.156 3.15 2.362c.484.364 1.16.42 1.713.045A2.76 2.76 0 0019 19c0-.23-.03-.455-.087-.67H19a1 1 0 001-1v-8a1 1 0 00-1-1h-.062a4.01 4.01 0 00-.832-1.854l-1.92-2.194A6.035 6.035 0 0011.25 4.53zM10.5 7.5a1.5 1.5 0 113 0 1.5 1.5 0 01-3 0zm-3 0a1.5 1.5 0 113 0 1.5 1.5 0 01-3 0z" /></svg>}
          </div>

          <div className="relative z-10 flex justify-between items-start border-b-2 border-slate-100 pb-6 mb-6">
            <div className='w-1/2'>
                <h1 className="text-2xl font-black text-slate-900 uppercase tracking-tight">Statement of Account</h1>
                <p className="text-sm text-slate-500 mt-1">Generated on {formatDate(statementData.generatedDate)}</p>
            </div>
            <div className="text-right w-1/2">
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
                    <QRCode value={qrPayload} size={70} level="M" />
                </div>
                <span className="block text-[8px] font-bold text-slate-400 uppercase mt-1 tracking-widest">Official Seal</span>
            </div>
            <div className="w-1/2 pl-4 text-right">
                <div className="ml-auto w-3/4">
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
              <thead><tr><th width="12%">Date</th><th width="48%">Description & Ref</th><th width="13%" className="money">Debit</th><th width="13%" className="money">Credit</th><th width="14%" className="money">Balance</th></tr></thead>
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

          <div className="relative z-10 flex justify-end pb-10">
              <div className="bg-slate-50 border border-slate-200 p-4 rounded-lg w-[260px]">
                  <div className="flex justify-between mb-2 text-xs text-slate-500 uppercase font-bold"><span>Total Credits</span><span className="font-mono text-emerald-600">{formatCurrency(statementData.totalCredits)}</span></div>
                  <div className="flex justify-between mb-4 text-xs text-slate-500 uppercase font-bold"><span>Total Debits</span><span className="font-mono text-rose-600">{formatCurrency(statementData.totalDebits)}</span></div>
                  <div className="flex justify-between pt-3 border-t-2 border-slate-200 text-sm text-slate-900"><span className="font-black uppercase">Closing Balance</span><span className="font-mono font-black border-b-4 border-double border-slate-300">{formatCurrency(statementData.closingBalance)}</span></div>
              </div>
          </div>
          
          {/* Repeating Sticky Footer */}
          <div className="print-footer">
              SYSTEM GENERATED DOCUMENT • {new Date().toISOString()} • BETTERLINK VENTURES LIMITED
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
            {loading ? <div className="py-20 flex justify-center"><BrandedSpinner /></div> : (
              <>
                {activeExecView === 'dashboard' && (<>{renderKPIs()}{renderFinancialCharts()}</>)}
                {activeExecView === 'balance-sheet' && renderBalanceSheet()}
                {activeExecView === 'income-statement' && renderIncomeStatement()}
                {/* Print View Rendered Conditionally */}
                {printingExecReport && renderPrintableExecReport()}
              </>
            )}
          </>
        )}

        {/* ... Other Tabs remain the same (Risk, Statements) ... */}
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