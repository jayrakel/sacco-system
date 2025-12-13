import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import ChangePassword from './pages/ChangePassword'; // <--- 1. Import it

// Placeholder for Admin
const AdminDashboard = () => (
    <div className="p-10">
        <h1 className="text-2xl font-bold text-blue-600 mb-4">Admin Dashboard</h1>
        <p>Welcome Admin. You have full system access.</p>
    </div>
);

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Login />} />

        {/* 2. Add the Route here 👇 */}
        <Route path="/change-password" element={<ChangePassword />} />

        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/admin-dashboard" element={<AdminDashboard />} />

        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </Router>
  );
}

export default App;