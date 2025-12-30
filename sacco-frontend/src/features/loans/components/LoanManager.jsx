import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

// Simple Badge Component for Status
const StatusBadge = ({ status }) => {
  const styles = {
    ACTIVE: "bg-green-100 text-green-800",
    PENDING: "bg-yellow-100 text-yellow-800",
    REJECTED: "bg-red-100 text-red-800",
    COMPLETED: "bg-blue-100 text-blue-800",
    DRAFT: "bg-gray-100 text-gray-800"
  };
  return (
    <span className={`px-2 py-1 rounded-full text-xs font-semibold ${styles[status] || "bg-gray-100"}`}>
      {status}
    </span>
  );
};

const LoanManager = ({ canApply, eligibilityMessage, activeLoans, totalBalance, onRefresh }) => {
  const navigate = useNavigate();

  const handleApplyClick = () => {
    if (canApply) {
      // Navigate to the Application Form (We will build this view next)
      navigate('/loans/apply');
    }
  };

  return (
    <div className="space-y-6">

      {/* 1. Summary & Action Card */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-100 flex flex-col md:flex-row justify-between items-center gap-4">
        <div>
          <h2 className="text-sm font-medium text-gray-500">Total Outstanding Balance</h2>
          <p className="text-3xl font-bold text-gray-900">
            KES {totalBalance?.toLocaleString() || '0.00'}
          </p>
        </div>

        {/* THE SMART BUTTON */}
        <div className="flex flex-col items-end">
          <button
            onClick={handleApplyClick}
            disabled={!canApply}
            className={`px-6 py-2.5 rounded-lg font-medium transition-colors ${
              canApply
                ? "bg-blue-600 hover:bg-blue-700 text-white shadow-md"
                : "bg-gray-200 text-gray-400 cursor-not-allowed"
            }`}
          >
            + Apply for New Loan
          </button>

          {/* Eligibility Feedback */}
          {!canApply && (
            <span className="text-xs text-red-500 mt-2 font-medium bg-red-50 px-2 py-1 rounded">
              🚫 {eligibilityMessage}
            </span>
          )}
          {canApply && (
             <span className="text-xs text-green-600 mt-2 font-medium">
             ✅ You are eligible to apply
           </span>
          )}
        </div>
      </div>

      {/* 2. Active Loans List */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-100 overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-800">Your Active Loans</h3>
        </div>

        {activeLoans.length === 0 ? (
          <div className="p-8 text-center text-gray-500">
            You have no active loans.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 text-gray-600">
                <tr>
                  <th className="px-6 py-3">Loan #</th>
                  <th className="px-6 py-3">Product</th>
                  <th className="px-6 py-3">Date Applied</th>
                  <th className="px-6 py-3 text-right">Principal</th>
                  <th className="px-6 py-3 text-right">Balance</th>
                  <th className="px-6 py-3 text-center">Status</th>
                  <th className="px-6 py-3"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {activeLoans.map((loan) => (
                  <tr key={loan.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-6 py-4 font-medium">{loan.loanNumber}</td>
                    <td className="px-6 py-4">{loan.productName}</td>
                    <td className="px-6 py-4">{loan.dateApplied}</td>
                    <td className="px-6 py-4 text-right">
                      {loan.principalAmount?.toLocaleString()}
                    </td>
                    <td className="px-6 py-4 text-right font-medium text-gray-900">
                      {loan.balance?.toLocaleString()}
                    </td>
                    <td className="px-6 py-4 text-center">
                      <StatusBadge status={loan.status} />
                    </td>
                    <td className="px-6 py-4 text-right">
                      <button
                        className="text-blue-600 hover:text-blue-800 text-xs font-medium"
                        onClick={() => navigate(`/loans/${loan.id}`)}
                      >
                        View Details →
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default LoanManager;