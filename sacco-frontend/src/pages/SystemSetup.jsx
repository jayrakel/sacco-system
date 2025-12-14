import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import { Shield, UserPlus, Save, AlertCircle } from 'lucide-react';

const REQUIRED_ROLES = [
  { role: 'CHAIRPERSON', label: 'Chairperson' },
  { role: 'ASSISTANT_CHAIRPERSON', label: 'Assistant Chairperson' },
  { role: 'SECRETARY', label: 'Secretary' },
  { role: 'ASSISTANT_SECRETARY', label: 'Assistant Secretary' },
  { role: 'TREASURER', label: 'Treasurer' },
  { role: 'LOAN_OFFICER', label: 'Loan Officer' }
];

export default function SystemSetup() {
  const [admins, setAdmins] = useState(
    REQUIRED_ROLES.map(r => ({
      role: r.role,
      label: r.label,
      firstName: '',
      lastName: '',
      email: '',
      phoneNumber: ''
    }))
  );

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleChange = (index, field, value) => {
    const updatedAdmins = [...admins];
    updatedAdmins[index][field] = value;
    setAdmins(updatedAdmins);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    // Basic Validation
    for (const admin of admins) {
        if (!admin.firstName || !admin.lastName || !admin.email || !admin.phoneNumber) {
            setError(`Please fill in all fields for ${admin.label}`);
            setLoading(false);
            return;
        }
    }

    try {
      // Send the list to the backend
      const response = await api.post('/api/setup/critical-admins', {
          admins: admins.map(({ label, ...rest }) => rest) // Remove label before sending
      });

      if (response.data.success) {
        alert("System Setup Complete! Temporary passwords have been generated (check backend console).");
        navigate('/admin-dashboard');
      }
    } catch (err) {
      console.error("Setup Error:", err);
      setError(err.response?.data?.message || "Failed to save admins.");
    }
    setLoading(false);
  };

  return (
    <div className="min-h-screen bg-slate-50 py-10 px-4 font-sans">
      <div className="max-w-5xl mx-auto">

        <div className="bg-slate-900 text-white p-8 rounded-t-2xl shadow-xl flex items-center gap-4">
          <div className="p-3 bg-emerald-500/20 rounded-full">
            <Shield size={32} className="text-emerald-400" />
          </div>
          <div>
            <h1 className="text-3xl font-bold">System Initialization</h1>
            <p className="text-slate-400">Please appoint the critical officers to activate the system.</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="bg-white p-8 rounded-b-2xl shadow-xl border border-slate-200">

          {error && (
            <div className="mb-6 p-4 bg-red-50 text-red-700 rounded-xl flex items-center gap-3 border border-red-100">
              <AlertCircle /> {error}
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            {admins.map((admin, index) => (
              <div key={admin.role} className="p-6 bg-slate-50 rounded-xl border border-slate-200 hover:border-blue-300 transition-colors">
                <div className="flex items-center gap-2 mb-4 border-b border-slate-200 pb-2">
                    <UserPlus size={18} className="text-blue-600" />
                    <h3 className="font-bold text-slate-700 uppercase text-sm tracking-wide">{admin.label}</h3>
                </div>

                <div className="space-y-3">
                    <div className="grid grid-cols-2 gap-3">
                        <input
                            type="text" placeholder="First Name" required
                            className="w-full p-2 border rounded text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                            value={admin.firstName}
                            onChange={e => handleChange(index, 'firstName', e.target.value)}
                        />
                        <input
                            type="text" placeholder="Last Name" required
                            className="w-full p-2 border rounded text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                            value={admin.lastName}
                            onChange={e => handleChange(index, 'lastName', e.target.value)}
                        />
                    </div>
                    <input
                        type="email" placeholder="Email Address" required
                        className="w-full p-2 border rounded text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                        value={admin.email}
                        onChange={e => handleChange(index, 'email', e.target.value)}
                    />
                    <input
                        type="tel" placeholder="Phone Number" required
                        className="w-full p-2 border rounded text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                        value={admin.phoneNumber}
                        onChange={e => handleChange(index, 'phoneNumber', e.target.value)}
                    />
                </div>
              </div>
            ))}
          </div>

          <div className="mt-8 pt-6 border-t border-slate-100 flex justify-end">
            <button
                type="submit"
                disabled={loading}
                className="bg-emerald-600 hover:bg-emerald-700 text-white px-8 py-3 rounded-xl font-bold text-lg flex items-center gap-2 shadow-lg hover:shadow-emerald-500/30 transition disabled:opacity-50"
            >
                {loading ? "Initializing..." : <><Save size={20} /> Save & Activate System</>}
            </button>
          </div>

        </form>
      </div>
    </div>
  );
}