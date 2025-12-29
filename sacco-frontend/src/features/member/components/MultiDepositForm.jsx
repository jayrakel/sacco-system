import React, { useState, useEffect } from 'react';
import { Plus, Trash2, DollarSign, TrendingUp, Landmark } from 'lucide-react';
import api from '../../../api';

/**
 * Multi-Destination Deposit Component
 * Allows members to deposit money with routing to multiple destinations
 */
const MultiDepositForm = () => {
  const [totalAmount, setTotalAmount] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('MPESA');
  const [paymentReference, setPaymentReference] = useState('');
  
  // ✅ NEW: Store selected bank code
  const [bankAccountCode, setBankAccountCode] = useState('');
  
  const [allocations, setAllocations] = useState([
    { destinationType: 'SAVINGS_ACCOUNT', amount: '', targetId: '', notes: '' }
  ]);

  // Available destinations & Banks
  const [savingsAccounts, setSavingsAccounts] = useState([]);
  const [activeLoans, setActiveLoans] = useState([]);
  const [pendingFines, setPendingFines] = useState([]);
  const [contributionProducts, setContributionProducts] = useState([]);
  const [activeBanks, setActiveBanks] = useState([]); // ✅ NEW: Store active banks

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Load available destinations
  useEffect(() => {
    loadDestinations();
  }, []);

  const loadDestinations = async () => {
    try {
      const [savings, loans, fines, products, banks] = await Promise.all([
        api.get('/api/savings/my-accounts'),
        api.get('/api/loans/my-loans?status=ACTIVE'),
        api.get('/api/fines/my-fines?status=PENDING'),
        api.get('/api/deposits/products/available'),
        api.get('/api/accounting/accounts/active-banks') // ✅ Fetch banks
      ]);

      setSavingsAccounts(savings.data.accounts || []);
      setActiveLoans(loans.data.loans || []);
      setPendingFines(fines.data.fines || []);
      setContributionProducts(products.data.products || []);
      setActiveBanks(banks.data.data || []); // ✅ Set banks
    } catch (err) {
      console.error('Failed to load destinations:', err);
    }
  };

  const addAllocation = () => {
    setAllocations([
      ...allocations,
      { destinationType: 'SAVINGS_ACCOUNT', amount: '', targetId: '', notes: '' }
    ]);
  };

  const removeAllocation = (index) => {
    if (allocations.length > 1) {
      setAllocations(allocations.filter((_, i) => i !== index));
    }
  };

  const updateAllocation = (index, field, value) => {
    const updated = [...allocations];
    updated[index][field] = value;
    if (field === 'destinationType') {
      updated[index].targetId = '';
    }
    setAllocations(updated);
  };

  const getAllocatedTotal = () => {
    return allocations.reduce((sum, alloc) => {
      const amount = parseFloat(alloc.amount) || 0;
      return sum + amount;
    }, 0);
  };

  const validateForm = () => {
    const total = parseFloat(totalAmount);
    const allocated = getAllocatedTotal();

    if (!total || total <= 0) {
      setError('Please enter a valid total amount');
      return false;
    }

    if (total !== allocated) {
      setError(`Total amount (${total}) does not match allocated amount (${allocated})`);
      return false;
    }
    
    // ✅ Validate Bank Selection
    if (paymentMethod === 'BANK' && !bankAccountCode) {
        setError('Please select the Bank Account you deposited to.');
        return false;
    }

    for (let alloc of allocations) {
      if (!alloc.amount || parseFloat(alloc.amount) <= 0) {
        setError('All allocations must have a positive amount');
        return false;
      }
      if (!alloc.targetId && alloc.destinationType !== 'SHARE_CAPITAL') {
        setError('Please select a destination for each allocation');
        return false;
      }
    }

    return true;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!validateForm()) return;

    setLoading(true);

    try {
      const request = {
        totalAmount: parseFloat(totalAmount),
        paymentMethod,
        paymentReference,
        bankAccountCode: paymentMethod === 'BANK' ? bankAccountCode : null, // ✅ Send Bank Code
        allocations: allocations.map(alloc => {
          const allocation = {
            destinationType: alloc.destinationType,
            amount: parseFloat(alloc.amount),
            notes: alloc.notes
          };
          switch (alloc.destinationType) {
            case 'SAVINGS_ACCOUNT': allocation.savingsAccountId = alloc.targetId; break;
            case 'LOAN_REPAYMENT': allocation.loanId = alloc.targetId; break;
            case 'FINE_PAYMENT': allocation.fineId = alloc.targetId; break;
            case 'CONTRIBUTION_PRODUCT': allocation.depositProductId = alloc.targetId; break;
          }
          return allocation;
        })
      };

      const response = await api.post('/api/deposits/create', request);

      setSuccess(`Deposit processed successfully! Reference: ${response.data.deposit.transactionReference}`);
      setTotalAmount('');
      setPaymentReference('');
      setBankAccountCode('');
      setAllocations([{ destinationType: 'SAVINGS_ACCOUNT', amount: '', targetId: '', notes: '' }]);
      loadDestinations();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to process deposit');
    } finally {
      setLoading(false);
    }
  };

  // ... [Keep getDestinationOptions exactly as before] ...
  const getDestinationOptions = (type) => {
    switch (type) {
      case 'SAVINGS_ACCOUNT':
        return savingsAccounts.map(acc => ({
          value: acc.id,
          label: `${acc.accountNumber} (Balance: KES ${Number(acc.balance).toLocaleString()})`
        }));
      case 'LOAN_REPAYMENT':
        return activeLoans.map(loan => ({
          value: loan.id,
          label: `${loan.loanNumber} (Balance: KES ${Number(loan.outstandingBalance).toLocaleString()})`
        }));
      case 'FINE_PAYMENT':
        return pendingFines.map(fine => ({
          value: fine.id,
          label: `${fine.description} (KES ${Number(fine.amount).toLocaleString()})`
        }));
      case 'CONTRIBUTION_PRODUCT':
        return contributionProducts.map(product => ({
          value: product.id,
          label: `${product.name} (${Number(product.currentAmount).toLocaleString()} / ${Number(product.targetAmount).toLocaleString()})`
        }));
      default:
        return [];
    }
  };

  const allocatedTotal = getAllocatedTotal();
  const remaining = (parseFloat(totalAmount) || 0) - allocatedTotal;

  return (
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
      <div className="flex items-center gap-3 mb-6">
        <div className="p-2 bg-indigo-100 rounded-lg">
          <DollarSign className="text-indigo-600" size={24} />
        </div>
        <div>
          <h2 className="text-xl font-bold text-slate-800">Make Deposit</h2>
          <p className="text-sm text-slate-500">Allocate funds to multiple destinations</p>
        </div>
      </div>

      {error && <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">{error}</div>}
      {success && <div className="mb-4 p-4 bg-green-50 border border-green-200 rounded-lg text-green-700 text-sm">{success}</div>}

      <form onSubmit={handleSubmit}>
        {/* Total Amount */}
        <div className="mb-6">
          <label className="block text-sm font-medium text-slate-700 mb-2">Total Amount (KES)</label>
          <input
            type="number"
            value={totalAmount}
            onChange={(e) => setTotalAmount(e.target.value)}
            className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
            placeholder="10000"
            step="0.01"
            required
          />
        </div>

        {/* Payment Details */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-2">Payment Method</label>
            <select
              value={paymentMethod}
              onChange={(e) => setPaymentMethod(e.target.value)}
              className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
            >
              <option value="MPESA">M-Pesa</option>
              <option value="BANK">Bank Transfer</option>
              <option value="CASH">Cash</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-2">Payment Reference</label>
            <input
              type="text"
              value={paymentReference}
              onChange={(e) => setPaymentReference(e.target.value)}
              className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
              placeholder="Ref Number / Receipt No"
            />
          </div>

          {/* ✅ NEW: Bank Selection Dropdown (Only shows if BANK is selected) */}
          {paymentMethod === 'BANK' && (
             <div className="md:col-span-2 bg-indigo-50 p-4 rounded-lg border border-indigo-100 animate-in fade-in">
                <label className="block text-sm font-bold text-indigo-800 mb-2 flex items-center gap-2">
                    <Landmark size={16}/> Select Sacco Bank Account
                </label>
                <select
                  value={bankAccountCode}
                  onChange={(e) => setBankAccountCode(e.target.value)}
                  className="w-full px-4 py-2 border border-indigo-200 rounded-lg focus:ring-2 focus:ring-indigo-500 bg-white"
                  required
                >
                  <option value="">-- Choose Bank Account --</option>
                  {activeBanks.length === 0 ? (
                      <option disabled>No active bank accounts found</option>
                  ) : (
                      activeBanks.map(bank => (
                        <option key={bank.code} value={bank.code}>
                           {bank.name} ({bank.code})
                        </option>
                      ))
                  )}
                </select>
                <p className="text-xs text-indigo-500 mt-1">Select the specific bank account where you made the deposit.</p>
             </div>
          )}
        </div>

        {/* Allocations Section (Unchanged) */}
        <div className="mb-6">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-sm font-bold text-slate-700">ALLOCATIONS</h3>
            <button
              type="button"
              onClick={addAllocation}
              className="flex items-center gap-2 px-3 py-1 bg-indigo-100 text-indigo-600 rounded-lg hover:bg-indigo-200 text-sm font-medium"
            >
              <Plus size={16} /> Add Allocation
            </button>
          </div>

          <div className="space-y-4">
            {allocations.map((allocation, index) => (
              <div key={index} className="p-4 border border-slate-200 rounded-lg bg-slate-50">
                <div className="grid grid-cols-12 gap-3">
                  <div className="col-span-3">
                    <label className="block text-xs font-medium text-slate-600 mb-1">Type</label>
                    <select
                      value={allocation.destinationType}
                      onChange={(e) => updateAllocation(index, 'destinationType', e.target.value)}
                      className="w-full px-2 py-1 border border-slate-300 rounded text-sm"
                    >
                      <option value="SAVINGS_ACCOUNT">Savings</option>
                      <option value="LOAN_REPAYMENT">Loan</option>
                      <option value="FINE_PAYMENT">Fine</option>
                      <option value="CONTRIBUTION_PRODUCT">Contribution</option>
                      <option value="SHARE_CAPITAL">Share Capital</option>
                    </select>
                  </div>

                  {allocation.destinationType !== 'SHARE_CAPITAL' && (
                    <div className="col-span-4">
                      <label className="block text-xs font-medium text-slate-600 mb-1">Destination</label>
                      <select
                        value={allocation.targetId}
                        onChange={(e) => updateAllocation(index, 'targetId', e.target.value)}
                        className="w-full px-2 py-1 border border-slate-300 rounded text-sm"
                        required
                      >
                        <option value="">Select...</option>
                        {getDestinationOptions(allocation.destinationType).map(opt => (
                          <option key={opt.value} value={opt.value}>{opt.label}</option>
                        ))}
                      </select>
                    </div>
                  )}

                  <div className={allocation.destinationType === 'SHARE_CAPITAL' ? 'col-span-4' : 'col-span-2'}>
                    <label className="block text-xs font-medium text-slate-600 mb-1">Amount</label>
                    <input
                      type="number"
                      value={allocation.amount}
                      onChange={(e) => updateAllocation(index, 'amount', e.target.value)}
                      className="w-full px-2 py-1 border border-slate-300 rounded text-sm"
                      placeholder="0"
                      step="0.01"
                      required
                    />
                  </div>

                  <div className="col-span-2">
                    <label className="block text-xs font-medium text-slate-600 mb-1">Notes</label>
                    <input
                      type="text"
                      value={allocation.notes}
                      onChange={(e) => updateAllocation(index, 'notes', e.target.value)}
                      className="w-full px-2 py-1 border border-slate-300 rounded text-sm"
                      placeholder="Optional"
                    />
                  </div>

                  <div className="col-span-1 flex items-end">
                    <button
                      type="button"
                      onClick={() => removeAllocation(index)}
                      disabled={allocations.length === 1}
                      className="w-full p-1 text-red-600 hover:bg-red-50 rounded disabled:opacity-30"
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Summary (Unchanged) */}
        <div className="mb-6 p-4 bg-slate-100 rounded-lg">
          <div className="flex justify-between items-center mb-2">
            <span className="text-sm font-medium text-slate-700">Total Amount:</span>
            <span className="text-sm font-bold text-slate-800">
              KES {Number(totalAmount || 0).toLocaleString()}
            </span>
          </div>
          <div className="flex justify-between items-center mb-2">
            <span className="text-sm font-medium text-slate-700">Allocated:</span>
            <span className="text-sm font-bold text-indigo-600">
              KES {allocatedTotal.toLocaleString()}
            </span>
          </div>
          <div className="flex justify-between items-center border-t border-slate-300 pt-2">
            <span className="text-sm font-bold text-slate-700">Remaining:</span>
            <span className={`text-sm font-bold ${remaining === 0 ? 'text-green-600' : 'text-red-600'}`}>
              KES {remaining.toLocaleString()}
            </span>
          </div>
        </div>

        <button
          type="submit"
          disabled={loading || remaining !== 0}
          className="w-full py-3 bg-indigo-600 text-white rounded-lg font-medium hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {loading ? 'Processing...' : 'Process Deposit'}
        </button>
      </form>
    </div>
  );
};

export default MultiDepositForm;