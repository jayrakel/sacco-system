import React, { useState, useEffect } from 'react';
import { loanService } from '../api'; // Adjust path as needed
import LoanManager from '../features/loans/components/LoanManager';
import BrandedSpinner from '../components/BrandedSpinner';

const LoansDashboard = () => {
  const [dashboardData, setDashboardData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

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

  useEffect(() => {
    fetchDashboard();
  }, []);

  if (loading) return <BrandedSpinner />;
  if (error) return <div className="text-red-500 text-center p-8">{error}</div>;

  return (
    <div className="p-6 space-y-6">
      <h1 className="text-2xl font-bold text-gray-800">My Loans</h1>

      {/* Pass the backend data directly to the manager component */}
      {dashboardData && (
        <LoanManager
          canApply={dashboardData.canApply}
          eligibilityMessage={dashboardData.eligibilityMessage}
          activeLoans={dashboardData.activeLoans}
          totalBalance={dashboardData.totalOutstandingBalance}
          onRefresh={fetchDashboard} // Allow child to reload data after applying
        />
      )}
    </div>
  );
};

export default LoansDashboard;