import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { SystemBranding, useSettings } from './context/SettingsContext';
import BrandedSpinner from './components/BrandedSpinner';

// Pages
import Login from './pages/Login';
import MemberDashboard from './pages/MemberDashboard';
import AdminDashboard from './pages/AdminDashboard';
import ChangePassword from './pages/ChangePassword';
import SystemSetup from './pages/SystemSetup';
import VerifyEmail from './pages/VerifyEmail';
import AddMember from './pages/members/AddMember';
import SystemSettings from './pages/admin/SystemSettings';
import ResetPassword from './pages/ResetPassword';

// Role Dashboards
import LoansDashboard from './pages/LoansDashboard';
import FinanceDashboard from './pages/FinanceDashboard';
import ChairpersonDashboard from './pages/ChairpersonDashboard';
import SecretaryDashboard from './pages/SecretaryDashboard';


// Error Pages
import NotFound from './pages/errors/NotFound';
import Unauthorized from './pages/errors/Unauthorized';
import ServerError from './pages/errors/ServerError';
import NetworkError from './pages/errors/NetworkError';
import SessionExpired from './pages/errors/SessionExpired';
import BadRequest from './pages/errors/BadRequest';

function App() {
  const { loading, settings, getImageUrl } = useSettings();
  const iconUrl = getImageUrl(settings.SACCO_FAVICON);

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

      <Router future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <Routes>
          <Route path="/" element={<Login />} />
          <Route path="/change-password" element={<ChangePassword />} />
          <Route path="/system-setup" element={<SystemSetup />} />
          <Route path="/verify-email" element={<VerifyEmail />} />
          <Route path="/reset-password" element={<ResetPassword />} />

          {/* Dashboards */}
          <Route path="/dashboard" element={<MemberDashboard />} />
          <Route path="/admin-dashboard" element={<AdminDashboard />} />

          {/* Role Dashboards */}
          <Route path="/loans-dashboard" element={<LoansDashboard />} />
          <Route path="/finance-dashboard" element={<FinanceDashboard />} />
          <Route path="/chairperson-dashboard" element={<ChairpersonDashboard />} />
          <Route path="/secretary-dashboard" element={<SecretaryDashboard />} />


          {/* Admin Tools */}
          <Route path="/add-member" element={<AddMember />} />
          <Route path="/admin/settings" element={<SystemSettings />} />

          {/* Error Pages */}
          <Route path="/bad-request" element={<BadRequest />} />
          <Route path="/unauthorized" element={<Unauthorized />} />
          <Route path="/server-error" element={<ServerError />} />
          <Route path="/network-error" element={<NetworkError />} />
          <Route path="/session-expired" element={<SessionExpired />} />
          <Route path="/404" element={<NotFound />} />

          <Route path="*" element={<NotFound />} />
        </Routes>
      </Router>
    </>
  );
}

export default App;