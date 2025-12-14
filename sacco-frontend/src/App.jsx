import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { SystemBranding, useSettings } from './context/SettingsContext'; // ✅ Import useSettings
import BrandedSpinner from './components/BrandedSpinner'; // ✅ Import Spinner

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
  // ✅ 1. Get Loading State & Branding
  const { loading, settings, getImageUrl } = useSettings();
  const iconUrl = getImageUrl(settings.SACCO_FAVICON);

  // ✅ 2. GLOBAL SPLASH SCREEN (Shows on F5/Reload for ANY page)
  if (loading) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-slate-50 gap-6 animate-in fade-in duration-700">
        <BrandedSpinner iconUrl={iconUrl} size="xl" />
        <div className="text-center space-y-2">
            <h2 className="text-2xl font-bold text-slate-800 tracking-tight">{settings.SACCO_NAME}</h2>
            <p className="text-slate-400 text-sm font-medium animate-pulse">Initializing Secure System...</p>
        </div>
      </div>
    );
  }

  return (
    <>
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