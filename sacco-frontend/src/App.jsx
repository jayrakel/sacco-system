import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { SystemBranding } from './context/SettingsContext'; // Import Branding Helper

// ... Import all your pages ...
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import AdminDashboard from './pages/AdminDashboard';
import ChangePassword from './pages/ChangePassword';
import SystemSetup from './pages/SystemSetup';
import VerifyEmail from './pages/VerifyEmail';
import AddMember from './pages/members/AddMember';
import SystemSettings from './pages/admin/SystemSettings';
import { LoansDashboard, FinanceDashboard, ChairpersonDashboard, SecretaryDashboard } from './pages/RoleDashboards';

function App() {
  return (
    <>
      {/* ✅ Activates Favicon & Title Updates */}
      <SystemBranding />

      <Router>
        <Routes>
          <Route path="/" element={<Login />} />
          <Route path="/change-password" element={<ChangePassword />} />
          <Route path="/system-setup" element={<SystemSetup />} />
          <Route path="/verify-email" element={<VerifyEmail />} />

          {/* Dashboards */}
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/admin-dashboard" element={<AdminDashboard />} />

          {/* Admin Tools */}
          <Route path="/add-member" element={<AddMember />} />
          <Route path="/admin/settings" element={<SystemSettings />} />

          {/* Role Dashboards */}
          <Route path="/loans-dashboard" element={<LoansDashboard />} />
          <Route path="/finance-dashboard" element={<FinanceDashboard />} />
          <Route path="/chairperson-dashboard" element={<ChairpersonDashboard />} />
          <Route path="/secretary-dashboard" element={<SecretaryDashboard />} />

          <Route path="*" element={<Navigate to="/" />} />
        </Routes>
      </Router>
    </>
  );
}

export default App;