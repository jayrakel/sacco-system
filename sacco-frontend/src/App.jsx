import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import AdminDashboard from './pages/AdminDashboard';
import ChangePassword from './pages/ChangePassword';
import SystemSetup from './pages/SystemSetup';
import VerifyEmail from './pages/VerifyEmail'; // <--- 1. IMPORT THIS

// ... import dashboards ...
import { LoansDashboard, FinanceDashboard, ChairpersonDashboard, SecretaryDashboard } from './pages/RoleDashboards';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/change-password" element={<ChangePassword />} />
        <Route path="/system-setup" element={<SystemSetup />} />

        {/* 👇 2. ADD THIS MISSING ROUTE 👇 */}
        <Route path="/verify-email" element={<VerifyEmail />} />

        {/* Dashboards */}
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/admin-dashboard" element={<AdminDashboard />} />
        <Route path="/loans-dashboard" element={<LoansDashboard />} />
        <Route path="/finance-dashboard" element={<FinanceDashboard />} />
        <Route path="/chairperson-dashboard" element={<ChairpersonDashboard />} />
        <Route path="/secretary-dashboard" element={<SecretaryDashboard />} />

        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </Router>
  );
}

export default App;