import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';

// Placeholder for Dashboards (We will create these next)
const Dashboard = () => <div className="p-10 text-2xl font-bold text-green-600">User Dashboard - Login Successful!</div>;
const AdminDashboard = () => <div className="p-10 text-2xl font-bold text-blue-600">Admin Dashboard - Login Successful!</div>;

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/admin-dashboard" element={<AdminDashboard />} />

        {/* Redirect unknown routes to login */}
        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </Router>
  );
}

export default App;