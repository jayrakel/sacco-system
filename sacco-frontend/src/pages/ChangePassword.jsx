import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import { Lock, CheckCircle, AlertTriangle, Eye, EyeOff } from 'lucide-react';

export default function ChangePassword() {
  const [passwords, setPasswords] = useState({
    currentPassword: '',
    newPassword: '',
    confirmationPassword: ''
  });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const navigate = useNavigate();

  const handleChange = (e) => {
    setPasswords({ ...passwords, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);

    if (passwords.newPassword !== passwords.confirmationPassword) {
      setError("New passwords do not match.");
      setLoading(false);
      return;
    }

    if (passwords.newPassword.length < 6) {
        setError("Password must be at least 6 characters long.");
        setLoading(false);
        return;
    }

    try {
      const response = await api.post('/api/auth/change-password', {
          currentPassword: passwords.currentPassword,
          newPassword: passwords.newPassword,
          confirmationPassword: passwords.confirmationPassword
      });

      if (response.data.success) {
        setSuccess("Password updated successfully! Redirecting...");

        // 1. Get current user from storage to check role
        const user = JSON.parse(localStorage.getItem('sacco_user'));

        if (user) {
            // 2. Update local flag so they aren't asked again
            user.mustChangePassword = false;
            localStorage.setItem('sacco_user', JSON.stringify(user));

            // 3. SMART REDIRECT (Matching Login.jsx logic)
            setTimeout(() => {
                switch (user.role) {
                    case 'ADMIN':
                        navigate('/admin-dashboard');
                        break;
                    case 'LOAN_OFFICER':
                        navigate('/loans-dashboard');
                        break;
                    case 'TREASURER':
                        navigate('/finance-dashboard');
                        break;
                    case 'CHAIRPERSON':
                    case 'ASSISTANT_CHAIRPERSON':
                        navigate('/chairperson-dashboard');
                        break;
                    case 'SECRETARY':
                    case 'ASSISTANT_SECRETARY':
                        navigate('/secretary-dashboard');
                        break;
                    case 'MEMBER':
                    default:
                        navigate('/dashboard');
                        break;
                }
            }, 2000);
        } else {
            // Fallback
            setTimeout(() => navigate('/'), 2000);
        }
      }
    } catch (err) {
      console.error("Change Password Error:", err);
      setError(err.response?.data?.message || "Failed to change password.");
    }
    setLoading(false);
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 p-4 font-sans">
      <div className="max-w-md w-full bg-white rounded-2xl shadow-xl p-8 border border-slate-100">

        <div className="text-center mb-8">
          <div className="bg-amber-100 p-4 rounded-full inline-block mb-4">
            <Lock className="text-amber-600 w-8 h-8" />
          </div>
          <h2 className="text-2xl font-bold text-slate-800">Secure Your Account</h2>
          <p className="text-slate-500 mt-2 text-sm">
            For security, you must update your password before continuing.
          </p>
        </div>

        {error && (
          <div className="mb-6 p-4 bg-red-50 border-l-4 border-red-500 text-red-700 text-sm flex gap-2 rounded-r">
            <AlertTriangle size={18} className="shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {success && (
          <div className="mb-6 p-4 bg-green-50 border-l-4 border-green-500 text-green-700 text-sm flex gap-2 rounded-r">
            <CheckCircle size={18} className="shrink-0" />
            <span>{success}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">Current Password</label>
            <input
              type={showPassword ? "text" : "password"}
              name="currentPassword"
              value={passwords.currentPassword}
              onChange={handleChange}
              className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent transition outline-none"
              placeholder="••••••••"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">New Password</label>
            <input
              type={showPassword ? "text" : "password"}
              name="newPassword"
              value={passwords.newPassword}
              onChange={handleChange}
              className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent transition outline-none"
              placeholder="New secure password"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">Confirm New Password</label>
            <div className="relative">
                <input
                type={showPassword ? "text" : "password"}
                name="confirmationPassword"
                value={passwords.confirmationPassword}
                onChange={handleChange}
                className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent transition outline-none"
                placeholder="Repeat new password"
                required
                />
                <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-3.5 text-slate-400 hover:text-slate-600"
                >
                    {showPassword ? <EyeOff size={20}/> : <Eye size={20}/>}
                </button>
            </div>
          </div>

          <button
            type="submit"
            disabled={loading || success}
            className="w-full bg-slate-900 text-white py-3.5 rounded-xl font-bold hover:bg-emerald-600 transition duration-200 disabled:opacity-50 disabled:cursor-not-allowed flex justify-center items-center gap-2 mt-4"
          >
            {loading ? "Updating..." : "Update Password"}
          </button>
        </form>
      </div>
    </div>
  );
}