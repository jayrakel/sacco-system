import React, { useState, useEffect } from 'react';
import api from '../../api';
import { 
  BarChart, Bar, AreaChart, Area, PieChart, Pie, Cell, 
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer 
} from 'recharts';
import { 
  LayoutDashboard, FileText, TrendingUp, DollarSign, 
  AlertCircle, Users, Download, RefreshCw, Search, Printer, 
  PieChart as PieIcon, ArrowUpRight, ArrowDownRight 
} from 'lucide-react';
import BrandedSpinner from '../../components/BrandedSpinner';

const ReportsDashboard = () => {
  // Main Navigation (The "Original" Structure)
  const [activeTab, setActiveTab] = useState('executive'); // executive | risk | statements
  
  // Executive Sub-Navigation (The "Reference" Structure)
  const [activeExecView, setActiveExecView] = useState('dashboard'); // dashboard | balance-sheet | income-statement

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
  const [statement, setStatement] = useState([]);
  const [dateRange, setDateRange] = useState('30'); 
  const [statementConfig, setStatementConfig] = useState({
    startDate: new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().split('T')[0],
    endDate: new Date().toISOString().split('T')[0]
  });

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
    try {
      const res = await api.get(`/api/reports/member-statement/${selectedMember}?startDate=${statementConfig.startDate}&endDate=${statementConfig.endDate}`);
      if (res.data.success) setStatement(res.data.data);
    } catch (e) { alert("Failed to fetch statement"); }
    finally { setLoading(false); }
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

  // --- RENDERERS ---

  const renderKPIs = () => (
    <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
      <div className="bg-gradient-to-br from-blue-600 to-indigo-700 text-white p-6 rounded-xl shadow-lg relative overflow-hidden group hover:scale-[1.02] transition-transform">
        <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity">
          <DollarSign size={100} />
        </div>
        <p className="text-blue-100 text-sm font-medium mb-1">Total Assets</p>
        <h3 className="text-3xl font-bold">
          {formatCurrency(parseFloat(reportData?.totalLoansOutstanding || 0) + parseFloat(reportData?.totalSavings || 0))}
        </h3>
        <div className="mt-4 flex items-center text-xs text-blue-200">
          <TrendingUp size={14} className="mr-1"/> +2.5% from last month
        </div>
      </div>

      <div className="bg-gradient-to-br from-emerald-500 to-teal-600 text-white p-6 rounded-xl shadow-lg relative overflow-hidden group hover:scale-[1.02] transition-transform">
        <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity">
          <Users size={100} />
        </div>
        <p className="text-emerald-100 text-sm font-medium mb-1">Member Deposits</p>
        <h3 className="text-3xl font-bold">{formatCurrency(reportData?.totalSavings)}</h3>
        <div className="mt-4 flex items-center text-xs text-emerald-200">
          <ArrowUpRight size={14} className="mr-1"/> Growing steadily
        </div>
      </div>

      <div className="bg-gradient-to-br from-violet-500 to-purple-600 text-white p-6 rounded-xl shadow-lg relative overflow-hidden group hover:scale-[1.02] transition-transform">
        <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity">
          <PieIcon size={100} />
        </div>
        <p className="text-purple-100 text-sm font-medium mb-1">Loan Portfolio</p>
        <h3 className="text-3xl font-bold">{formatCurrency(reportData?.totalLoansOutstanding)}</h3>
        <div className="mt-4 flex items-center text-xs text-purple-200">
          <FileText size={14} className="mr-1"/> {reportData?.totalLoansIssued} Active Loans
        </div>
      </div>

      <div className="bg-gradient-to-br from-orange-500 to-red-500 text-white p-6 rounded-xl shadow-lg relative overflow-hidden group hover:scale-[1.02] transition-transform">
        <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity">
          <AlertCircle size={100} />
        </div>
        <p className="text-orange-100 text-sm font-medium mb-1">Net Income</p>
        <h3 className="text-3xl font-bold">{formatCurrency(reportData?.netIncome)}</h3>
        <div className="mt-4 flex items-center text-xs text-orange-200">
          Revenue - Expenses
        </div>
      </div>
    </div>
  );

  const renderFinancialCharts = () => (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 animate-in fade-in">
      <div className="lg:col-span-2 bg-white p-6 rounded-xl shadow-sm border border-slate-100">
        <div className="flex justify-between items-center mb-6">
          <h3 className="text-lg font-bold text-slate-800">Financial Performance Trend</h3>
          <select 
            value={dateRange} 
            onChange={(e) => setDateRange(e.target.value)}
            className="text-sm border-slate-200 bg-slate-50 rounded-lg p-2 focus:ring-indigo-500"
          >
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
                  <linearGradient id="colorIncome" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10B981" stopOpacity={0.1}/>
                    <stop offset="95%" stopColor="#10B981" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="colorExpense" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#EF4444" stopOpacity={0.1}/>
                    <stop offset="95%" stopColor="#EF4444" stopOpacity={0}/>
                  </linearGradient>
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
           {/* Mock Pie Data based on GL - ideally fetch this breakdown */}
           <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie
                data={[
                  { name: 'Interest', value: parseFloat(reportData?.totalInterestCollected || 0) },
                  { name: 'Fees & Fines', value: Math.max(0, parseFloat(reportData?.totalIncome || 0) - parseFloat(reportData?.totalInterestCollected || 0)) }
                ]}
                cx="50%"
                cy="50%"
                innerRadius={60}
                outerRadius={80}
                paddingAngle={5}
                dataKey="value"
              >
                <Cell fill="#4F46E5" />
                <Cell fill="#10B981" />
              </Pie>
              <Tooltip formatter={(value) => formatCurrency(value)} />
              <Legend verticalAlign="bottom" height={36}/>
            </PieChart>
          </ResponsiveContainer>
          <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
            <div className="text-center">
              <p className="text-xs text-slate-400 font-bold uppercase">Total</p>
              <p className="text-xl font-bold text-slate-800">{formatCurrency(reportData?.totalIncome)}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  const renderBalanceSheet = () => (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 animate-in fade-in">
      <div className="bg-white p-6 rounded-xl shadow-sm border-t-4 border-blue-600">
        <h3 className="text-lg font-bold mb-6 text-slate-800 flex items-center">
          <DollarSign className="w-5 h-5 mr-2 text-blue-600"/> Assets (What We Own)
        </h3>
        <div className="space-y-4">
          <div className="flex justify-between p-3 bg-slate-50 rounded-lg">
            <span className="text-slate-600">Loan Portfolio</span>
            <span className="font-bold text-slate-900">{formatCurrency(reportData?.totalLoansOutstanding)}</span>
          </div>
          <div className="flex justify-between p-3 bg-slate-50 rounded-lg">
            <span className="text-slate-600">Cash Reserves (Savings Proxy)</span>
            <span className="font-bold text-slate-900">{formatCurrency(reportData?.totalSavings)}</span>
          </div>
          <div className="flex justify-between pt-4 mt-2 border-t border-slate-100">
            <span className="text-lg font-bold text-blue-800">Total Assets</span>
            <span className="text-lg font-bold text-blue-800">
              {formatCurrency(parseFloat(reportData?.totalLoansOutstanding || 0) + parseFloat(reportData?.totalSavings || 0))}
            </span>
          </div>
        </div>
      </div>

      <div className="space-y-6">
        <div className="bg-white p-6 rounded-xl shadow-sm border-t-4 border-red-500">
          <h3 className="text-lg font-bold mb-6 text-slate-800 flex items-center">
            <Users className="w-5 h-5 mr-2 text-red-500"/> Liabilities (What We Owe)
          </h3>
          <div className="space-y-4">
            <div className="flex justify-between p-3 bg-slate-50 rounded-lg">
              <span className="text-slate-600">Member Deposits</span>
              <span className="font-bold text-slate-900">{formatCurrency(reportData?.totalSavings)}</span>
            </div>
            <div className="flex justify-between pt-4 mt-2 border-t border-slate-100">
              <span className="text-lg font-bold text-red-700">Total Liabilities</span>
              <span className="text-lg font-bold text-red-700">{formatCurrency(reportData?.totalSavings)}</span>
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-xl shadow-sm border-t-4 border-purple-600">
          <h3 className="text-lg font-bold mb-6 text-slate-800 flex items-center">
            <TrendingUp className="w-5 h-5 mr-2 text-purple-600"/> Equity
          </h3>
          <div className="space-y-4">
            <div className="flex justify-between p-3 bg-slate-50 rounded-lg">
              <span className="text-slate-600">Share Capital</span>
              <span className="font-bold text-slate-900">{formatCurrency(reportData?.totalShareCapital)}</span>
            </div>
            <div className="flex justify-between p-3 bg-slate-50 rounded-lg">
              <span className="text-slate-600">Retained Earnings (Net Income)</span>
              <span className="font-bold text-slate-900">{formatCurrency(reportData?.netIncome)}</span>
            </div>
            <div className="flex justify-between pt-4 mt-2 border-t border-slate-100">
              <span className="text-lg font-bold text-purple-800">Total Equity</span>
              <span className="text-lg font-bold text-purple-800">
                {formatCurrency(parseFloat(reportData?.totalShareCapital || 0) + parseFloat(reportData?.netIncome || 0))}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  const renderIncomeStatement = () => (
    <div className="bg-white p-8 rounded-xl shadow-sm border border-slate-200 max-w-4xl mx-auto animate-in fade-in">
      <div className="text-center mb-10">
        <h2 className="text-2xl font-bold text-slate-900">Income Statement</h2>
        <p className="text-slate-500">For the period ending {new Date().toLocaleDateString()}</p>
      </div>

      <div className="space-y-8">
        {/* Revenue Section */}
        <div>
          <h3 className="text-sm font-bold text-emerald-700 uppercase mb-4 border-b border-emerald-100 pb-2">Revenue</h3>
          <div className="space-y-3 pl-4">
            <div className="flex justify-between">
              <span className="text-slate-700">Interest Income on Loans</span>
              <span className="font-medium font-mono">{formatCurrency(reportData?.totalInterestCollected)}</span>
            </div>
            <div className="flex justify-between text-slate-500">
              <span>Other Operating Income</span>
              <span className="font-mono">{formatCurrency(parseFloat(reportData?.totalIncome || 0) - parseFloat(reportData?.totalInterestCollected || 0))}</span>
            </div>
            <div className="flex justify-between pt-2 border-t border-slate-100 font-bold text-slate-900">
              <span>Total Revenue</span>
              <span>{formatCurrency(reportData?.totalIncome)}</span>
            </div>
          </div>
        </div>

        {/* Expenses Section */}
        <div>
          <h3 className="text-sm font-bold text-red-700 uppercase mb-4 border-b border-red-100 pb-2">Operating Expenses</h3>
          <div className="space-y-3 pl-4">
            <div className="flex justify-between">
              <span className="text-slate-700">General & Admin Expenses</span>
              <span className="font-medium font-mono">{formatCurrency(reportData?.totalExpenses)}</span>
            </div>
            <div className="flex justify-between pt-2 border-t border-slate-100 font-bold text-slate-900">
              <span>Total Expenses</span>
              <span>({formatCurrency(reportData?.totalExpenses)})</span>
            </div>
          </div>
        </div>

        {/* Net Income Section */}
        <div className="bg-blue-50 p-6 rounded-xl border border-blue-100 flex justify-between items-center">
          <div>
            <h3 className="text-lg font-bold text-blue-900">Net Income</h3>
            <p className="text-sm text-blue-600">Net Profit / (Loss)</p>
          </div>
          <span className={`text-2xl font-bold font-mono ${parseFloat(reportData?.netIncome) >= 0 ? 'text-blue-700' : 'text-red-600'}`}>
            {formatCurrency(reportData?.netIncome)}
          </span>
        </div>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-gray-50 pb-20 font-sans">
      
      {/* Top Navigation Bar */}
      <div className="bg-white border-b border-slate-200 sticky top-0 z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center">
              <div className="p-2 bg-indigo-50 rounded-lg mr-3">
                <LayoutDashboard className="h-6 w-6 text-indigo-600" />
              </div>
              <div>
                <h1 className="text-xl font-bold text-slate-900">Financial Reports</h1>
                <p className="text-xs text-slate-500">Real-time Accounting & Analytics</p>
              </div>
            </div>
            
            <div className="flex space-x-1 bg-slate-100 p-1 rounded-lg">
              {[
                { id: 'executive', label: 'Executive' },
                { id: 'risk', label: 'Risk Analysis' },
                { id: 'statements', label: 'Statements' }
              ].map(tab => (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`px-4 py-2 text-sm font-medium rounded-md transition-all ${
                    activeTab === tab.id ? 'bg-white shadow text-indigo-700' : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mt-8">
        
        {/* EXECUTIVE TAB CONTENT */}
        {activeTab === 'executive' && (
          <>
            {/* View Switcher (Reference Style) */}
            <div className="flex flex-wrap gap-3 mb-8">
              {[
                { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
                { id: 'balance-sheet', label: 'Balance Sheet', icon: FileText },
                { id: 'income-statement', label: 'Income Statement', icon: TrendingUp },
              ].map(view => (
                <button
                  key={view.id}
                  onClick={() => setActiveExecView(view.id)}
                  className={`flex items-center px-5 py-2.5 rounded-full font-medium text-sm transition-all ${
                    activeExecView === view.id 
                      ? 'bg-slate-900 text-white shadow-lg transform scale-105' 
                      : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
                  }`}
                >
                  <view.icon size={16} className="mr-2" />
                  {view.label}
                </button>
              ))}
              
              <button 
                onClick={handleForceGenerate}
                disabled={refreshing}
                className="ml-auto flex items-center px-4 py-2 bg-indigo-50 text-indigo-700 rounded-full text-sm font-medium hover:bg-indigo-100 transition-colors"
              >
                <RefreshCw size={16} className={`mr-2 ${refreshing ? 'animate-spin' : ''}`} />
                Refresh GL
              </button>
            </div>

            {loading ? <div className="py-20 flex justify-center"><BrandedSpinner /></div> : (
              <>
                {activeExecView === 'dashboard' && (
                  <>
                    {renderKPIs()}
                    {renderFinancialCharts()}
                  </>
                )}
                {activeExecView === 'balance-sheet' && renderBalanceSheet()}
                {activeExecView === 'income-statement' && renderIncomeStatement()}
              </>
            )}
          </>
        )}

        {/* RISK TAB CONTENT (Legacy Operational) */}
        {activeTab === 'risk' && (
          <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden animate-in fade-in">
            <div className="p-6 border-b border-slate-100 bg-slate-50 flex justify-between items-center">
              <div>
                <h3 className="font-bold text-slate-800 flex items-center gap-2"><AlertCircle size={20} className="text-red-500"/> Portfolio Risk (PAR)</h3>
                <p className="text-sm text-slate-500 mt-1">Loans overdue by 1+ days</p>
              </div>
              <button className="px-4 py-2 bg-white border border-slate-300 rounded-lg text-sm font-medium hover:bg-slate-50 text-slate-700">Export Risk Report</button>
            </div>
            <table className="w-full text-sm text-left">
              <thead className="bg-slate-50 text-slate-500 font-bold border-b uppercase text-xs">
                <tr>
                  <th className="p-4">Loan No</th>
                  <th className="p-4">Member</th>
                  <th className="p-4">Days Overdue</th>
                  <th className="p-4">Category</th>
                  <th className="p-4 text-right">Outstanding</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {agingReport.map((item, idx) => (
                  <tr key={idx} className="hover:bg-slate-50">
                    <td className="p-4 font-mono text-slate-500">{item.loanNumber}</td>
                    <td className="p-4 font-bold text-slate-700">{item.memberName}</td>
                    <td className="p-4 text-red-600 font-bold">{item.daysOverdue} Days</td>
                    <td className="p-4"><span className="bg-red-100 text-red-700 px-2 py-1 rounded text-[10px] font-bold uppercase">{item.category}</span></td>
                    <td className="p-4 text-right font-mono font-bold">{formatCurrency(item.amountOutstanding)}</td>
                  </tr>
                ))}
                {agingReport.length === 0 && !loading && <tr><td colSpan="5" className="p-12 text-center text-slate-400 italic">No overdue loans found. Excellent!</td></tr>}
              </tbody>
            </table>
          </div>
        )}

        {/* STATEMENTS TAB CONTENT (Legacy Operational) */}
        {activeTab === 'statements' && (
          <div className="space-y-6 animate-in fade-in">
            <div className="bg-white p-5 rounded-xl shadow-sm border border-slate-200 print:hidden">
              <div className="flex flex-col md:flex-row gap-4 items-end">
                <div className="flex-1">
                  <label className="block text-xs font-bold text-slate-500 mb-1">Select Member</label>
                  <div className="relative">
                    <Search className="absolute left-3 top-2.5 text-slate-400" size={16} />
                    <select className="w-full pl-10 p-2.5 border border-slate-300 rounded-lg text-sm focus:ring-indigo-500 focus:border-indigo-500" 
                      value={selectedMember} onChange={e => setSelectedMember(e.target.value)}>
                      <option value="">-- Search Member --</option>
                      {members.map(m => <option key={m.id} value={m.id}>{m.firstName} {m.lastName} - {m.memberNumber}</option>)}
                    </select>
                  </div>
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-500 mb-1">Start Date</label>
                  <input type="date" className="p-2.5 border border-slate-300 rounded-lg text-sm" 
                    value={statementConfig.startDate} onChange={e => setStatementConfig({...statementConfig, startDate: e.target.value})} />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-500 mb-1">End Date</label>
                  <input type="date" className="p-2.5 border border-slate-300 rounded-lg text-sm" 
                    value={statementConfig.endDate} onChange={e => setStatementConfig({...statementConfig, endDate: e.target.value})} />
                </div>
                <button onClick={handleGenerateStatement} disabled={!selectedMember || loading} 
                  className="px-6 py-2.5 bg-slate-900 text-white rounded-lg font-bold text-sm hover:bg-slate-800 disabled:opacity-50 transition-colors">
                  Generate
                </button>
              </div>
            </div>

            {statement.length > 0 && (
              <div className="bg-white shadow-lg border border-slate-200 print:shadow-none print:border-0 max-w-4xl mx-auto p-8 rounded-xl">
                <div className="flex justify-between items-start mb-8 pb-8 border-b border-slate-200">
                  <div>
                    <h1 className="text-2xl font-bold text-slate-900 uppercase">Statement of Account</h1>
                    <p className="text-sm text-slate-500 mt-1">Generated: {new Date().toLocaleDateString()}</p>
                  </div>
                  <div className="text-right">
                    <button onClick={() => window.print()} className="print:hidden flex items-center text-indigo-600 font-bold text-sm hover:underline">
                      <Printer size={16} className="mr-1"/> Print / PDF
                    </button>
                  </div>
                </div>
                
                <table className="w-full text-sm text-left">
                  <thead className="bg-slate-100 text-slate-600 font-bold border-b border-slate-200 uppercase text-xs">
                    <tr>
                      <th className="p-3">Date</th>
                      <th className="p-3">Ref</th>
                      <th className="p-3">Description</th>
                      <th className="p-3 text-right">Debit</th>
                      <th className="p-3 text-right">Credit</th>
                      <th className="p-3 text-right">Balance</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {statement.map((tx, i) => (
                      <tr key={i} className="hover:bg-slate-50">
                        <td className="p-3 text-slate-600 font-mono text-xs">{new Date(tx.date).toLocaleDateString()}</td>
                        <td className="p-3 font-mono text-xs text-slate-500">{tx.reference}</td>
                        <td className="p-3 text-slate-800">{tx.description}</td>
                        <td className="p-3 text-right font-mono text-red-600">{tx.amount < 0 ? formatCurrency(Math.abs(tx.amount)) : '-'}</td>
                        <td className="p-3 text-right font-mono text-emerald-600">{tx.amount > 0 ? formatCurrency(tx.amount) : '-'}</td>
                        <td className="p-3 text-right font-mono font-bold text-slate-900">{formatCurrency(tx.runningBalance)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

      </div>
    </div>
  );
};

export default ReportsDashboard;