import React, { useState, useEffect } from 'react';
import loanService from '../features/loans/services/loanService'; // ✅ FIXED IMPORT (Default import from correct path)
import authService from '../features/auth/services/authService'; // ✅ Use authService for user data
import LoanManager from '../features/loans/components/LoanManager';
import LoanOfficerDashboard from '../features/loan-officer/components/LoanOfficerDashboard';
import BrandedSpinner from '../components/BrandedSpinner';

/**
 * Role-Aware Loans Dashboard
 * - LOAN_OFFICER / ADMIN: Show loan officer review dashboard
 * - MEMBER: Show personal loans view
 */
const LoansDashboard = () => {
  const [user, setUser] = useState(null);
  const [dashboardData, setDashboardData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    // ✅ FIX: Use consistent authService to get the logged-in user
    const currentUser = authService.getCurrentUser();
    setUser(currentUser);

    // Only fetch member dashboard data if NOT a loan officer/admin
    if (currentUser && currentUser.role !== 'LOAN_OFFICER' && currentUser.role !== 'ADMIN') {
        fetchDashboard();
    } else {
        setLoading(false); // Stop loading if we are showing the officer dashboard
    }
  }, []);

  const fetchDashboard = async () => {
    try {
      setLoading(true);
      const response = await loanService.getDashboard();
      if (response.success) {
        setDashboardData(response.data);
      } else {
        setError(response.message);
      }
    } catch (err) {
      console.error(err);
      setError("Failed to load loan dashboard.");
    } finally {
      setLoading(false);
    }
  };

  // 1. Show Loan Officer Dashboard
  if (user?.role === 'LOAN_OFFICER' || user?.role === 'ADMIN') {
    return <LoanOfficerDashboard />;
  }

  // 2. Show Loading
  if (loading) return (
    <div className="min-h-[60vh] flex items-center justify-center">
        <BrandedSpinner />
    </div>
  );

  // 3. Show Error
  if (error) return (
    <div className="p-8 text-center">
        <div className="bg-red-50 text-red-600 p-4 rounded-lg inline-block border border-red-100">
            {error}
        </div>
    </div>
  );

  // 4. Show Member View
  return (
    <div className="p-6 space-y-6 max-w-7xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-800">My Loans</h1>

      {dashboardData && (
        <LoanManager
          canApply={dashboardData.canApply}
          eligibilityMessage={dashboardData.eligibilityMessage}
          activeLoans={dashboardData.activeLoans || []}
          totalBalance={dashboardData.totalOutstandingBalance || 0}
          onRefresh={fetchDashboard}
        />
      )}
    </div>
  );
};

export default LoansDashboard;