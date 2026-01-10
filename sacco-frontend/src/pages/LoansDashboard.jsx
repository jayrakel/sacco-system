import React, { useState, useEffect } from 'react';
import { loanService } from '../api';
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
    const storedUser = localStorage.getItem('sacco_user');
    if (storedUser) {
      setUser(JSON.parse(storedUser));
    }
    fetchDashboard();
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
      setError("Failed to load loan dashboard.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  // Show loan officer dashboard for LOAN_OFFICER and ADMIN roles
  if (user?.role === 'LOAN_OFFICER' || user?.role === 'ADMIN') {
    return <LoanOfficerDashboard />;
  }

  // Member view (existing functionality)
  if (loading) return <BrandedSpinner />;
  if (error) return <div className="text-red-500 text-center p-8">{error}</div>;

  return (
    <div className="p-6 space-y-6">
      <h1 className="text-2xl font-bold text-gray-800">My Loans</h1>

      {dashboardData && (
        <LoanManager
          canApply={dashboardData.canApply}
          eligibilityMessage={dashboardData.eligibilityMessage}
          activeLoans={dashboardData.activeLoans}
          totalBalance={dashboardData.totalOutstandingBalance}
          onRefresh={fetchDashboard}
        />
      )}
    </div>
  );
};

export default LoansDashboard;