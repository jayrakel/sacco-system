import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import { Shield, AlertCircle, CheckCircle2, User, Mail, Phone, Lock, ArrowRight, Sparkles, Users, Building2, Clock, Crown, Users2, FileText, ClipboardList, Wallet, BarChart3 } from 'lucide-react';

const REQUIRED_ROLES = [
  { 
    role: 'CHAIRPERSON', 
    label: 'Chairperson',
    description: 'Overall leadership and decision-making',
    icon: Crown,
    color: 'from-purple-500 to-purple-700'
  },
  { 
    role: 'ASSISTANT_CHAIRPERSON', 
    label: 'Assistant Chairperson',
    description: 'Supports the chairperson',
    icon: Users2,
    color: 'from-indigo-500 to-indigo-700'
  },
  { 
    role: 'SECRETARY', 
    label: 'Secretary',
    description: 'Records and documentation',
    icon: FileText,
    color: 'from-blue-500 to-blue-700'
  },
  { 
    role: 'ASSISTANT_SECRETARY', 
    label: 'Assistant Secretary',
    description: 'Supports the secretary',
    icon: ClipboardList,
    color: 'from-cyan-500 to-cyan-700'
  },
  { 
    role: 'TREASURER', 
    label: 'Treasurer',
    description: 'Financial management and oversight',
    icon: Wallet,
    color: 'from-emerald-500 to-emerald-700'
  },
  { 
    role: 'LOAN_OFFICER', 
    label: 'Loan Officer',
    description: 'Loan processing and management',
    icon: BarChart3,
    color: 'from-orange-500 to-orange-700'
  }
];

export default function SystemSetup() {
  const [selectedRole, setSelectedRole] = useState(0);
  const [admins, setAdmins] = useState(
    REQUIRED_ROLES.map(r => ({
      role: r.role,
      label: r.label,
      description: r.description,
      icon: r.icon,
      color: r.color,
      firstName: '',
      lastName: '',
      email: '',
      phoneNumber: '',
      completed: false
    }))
  );

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [createdAccounts, setCreatedAccounts] = useState([]);
  const navigate = useNavigate();

  const currentAdmin = admins[selectedRole];
  const completedCount = admins.filter(a => a.completed).length;

  const handleChange = (field, value) => {
    const updatedAdmins = [...admins];
    updatedAdmins[selectedRole][field] = value;
    setAdmins(updatedAdmins);
    setError('');
  };

  const validateCurrentRole = () => {
    const admin = admins[selectedRole];
    if (!admin.firstName.trim()) {
      setError('First name is required');
      return false;
    }
    if (!admin.lastName.trim()) {
      setError('Last name is required');
      return false;
    }
    if (!admin.email.trim()) {
      setError('Email is required');
      return false;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(admin.email)) {
      setError('Please enter a valid email address');
      return false;
    }
    if (!admin.phoneNumber.trim()) {
      setError('Phone number is required');
      return false;
    }
    if (!/^[+]?[\d\s\-()]+$/.test(admin.phoneNumber)) {
      setError('Please enter a valid phone number');
      return false;
    }
    
    const duplicateEmail = admins.find((a, idx) => 
      idx !== selectedRole && a.email.toLowerCase() === admin.email.toLowerCase() && a.email.trim() !== ''
    );
    if (duplicateEmail) {
      setError(`This email is already assigned to ${duplicateEmail.label}`);
      return false;
    }
    
    return true;
  };

  const handleSaveAndNext = () => {
    if (!validateCurrentRole()) {
      return;
    }
    
    const updatedAdmins = [...admins];
    updatedAdmins[selectedRole].completed = true;
    setAdmins(updatedAdmins);
    setError('');
    
    if (selectedRole < admins.length - 1) {
      setSelectedRole(selectedRole + 1);
    }
  };

  const handleRoleSelect = (index) => {
    setError('');
    setSelectedRole(index);
  };

  const handleSubmit = async () => {
    if (!validateCurrentRole()) {
      return;
    }

    for (let i = 0; i < admins.length; i++) {
      const admin = admins[i];
      if (!admin.firstName || !admin.lastName || !admin.email || !admin.phoneNumber) {
        setError(`Please complete all fields for ${admin.label}`);
        setSelectedRole(i);
        return;
      }
    }

    setLoading(true);
    setError('');

    try {
      const response = await api.post('/api/setup/critical-admins', {
          admins: admins.map(({ label, description, icon, color, completed, ...rest }) => rest)
      });

      if (response.data.success) {
        setCreatedAccounts(response.data.results);
        setSuccess(true);
        
        setTimeout(() => {
          navigate('/');
        }, 5000);
      }
    } catch (err) {
      console.error("Setup Error:", err);
      setError(err.response?.data?.message || "Failed to create admin accounts. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 flex items-center justify-center p-4">
        <div className="max-w-6xl w-full">
          {/* Header Section */}
          <div className="text-center mb-10">
            <div className="inline-flex items-center justify-center w-24 h-24 bg-gradient-to-br from-emerald-400 to-emerald-600 rounded-full mb-6 shadow-2xl shadow-emerald-500/50 animate-pulse">
              <CheckCircle2 size={48} className="text-white" />
            </div>
            <h1 className="text-6xl font-black text-transparent bg-clip-text bg-gradient-to-r from-emerald-400 via-blue-400 to-purple-400 mb-4">
              Setup Complete!
            </h1>
            <p className="text-xl text-slate-300 mb-2">
              {admins.length} Administrative Accounts Created Successfully
            </p>
            <div className="inline-flex items-center gap-2 px-4 py-2 bg-emerald-500/10 border border-emerald-500/30 rounded-full">
              <Shield size={16} className="text-emerald-400" />
              <span className="text-sm text-emerald-300 font-semibold">System Ready for Operations</span>
            </div>
          </div>

          {/* Accounts Grid */}
          <div className="bg-white/5 backdrop-blur-sm border border-white/10 rounded-2xl p-8 mb-8">
            <div className="flex items-center justify-between mb-6 pb-4 border-b border-white/10">
              <h2 className="text-xl font-bold text-white flex items-center gap-2">
                <Users size={24} className="text-blue-400" />
                Created Accounts
              </h2>
              <span className="text-sm text-slate-400">{createdAccounts.filter(acc => acc.success).length} Officers</span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {createdAccounts.filter(acc => acc.success).map((account, idx) => {
                const roleData = REQUIRED_ROLES.find(r => r.role === account.role);
                const IconComponent = roleData?.icon;
                return (
                  <div key={idx} className={`bg-gradient-to-br ${roleData?.color} p-[1px] rounded-xl hover:scale-105 transition-all`}>
                    <div className="bg-slate-900/90 backdrop-blur-sm rounded-xl p-5 h-full">
                      <div className="flex items-start gap-4 mb-4">
                        <div className={`p-3 rounded-lg bg-gradient-to-br ${roleData?.color}`}>
                          {IconComponent && <IconComponent size={24} className="text-white" />}
                        </div>
                        <div className="flex-1">
                          <h3 className="font-bold text-white text-base mb-1">{roleData?.label}</h3>
                          <p className="text-xs text-slate-400 mb-2">{roleData?.description}</p>
                          <div className="flex items-center gap-2 text-xs text-blue-300">
                            <Mail size={12} />
                            <span className="font-mono">{account.officialEmail}</span>
                          </div>
                        </div>
                        <div className="flex-shrink-0">
                          <div className="w-8 h-8 rounded-full bg-emerald-500/20 flex items-center justify-center border border-emerald-500/30">
                            <CheckCircle2 size={16} className="text-emerald-400" />
                          </div>
                        </div>
                      </div>
                      
                      <div className="bg-slate-800/80 border border-slate-700 rounded-lg p-4">
                        <div className="flex items-center justify-between mb-2">
                          <div className="flex items-center gap-2">
                            <Lock size={14} className="text-slate-400" />
                            <span className="text-xs text-slate-300 font-semibold">Temporary Password</span>
                          </div>
                          <button
                            onClick={() => {
                              navigator.clipboard.writeText(account.tempPassword);
                            }}
                            className="text-xs px-3 py-1 bg-blue-500/20 hover:bg-blue-500/30 text-blue-300 rounded-md border border-blue-500/30 transition-colors font-medium"
                          >
                            Copy
                          </button>
                        </div>
                        <code className="text-sm font-mono text-emerald-400 font-bold block select-all">
                          {account.tempPassword}
                        </code>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Important Notice */}
          <div className="bg-gradient-to-r from-yellow-500/10 via-orange-500/10 to-yellow-500/10 border border-yellow-500/30 rounded-xl p-6 mb-6">
            <div className="flex items-start gap-4">
              <div className="flex-shrink-0 w-12 h-12 rounded-full bg-yellow-500/20 flex items-center justify-center">
                <AlertCircle size={24} className="text-yellow-400" />
              </div>
              <div className="flex-1">
                <h3 className="text-lg font-bold text-yellow-200 mb-2">Important Security Notice</h3>
                <ul className="space-y-2 text-sm text-yellow-100/80">
                  <li className="flex items-start gap-2">
                    <span className="text-yellow-400 mt-0.5">•</span>
                    <span>Login credentials have been sent to each officer's personal email address</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="text-yellow-400 mt-0.5">•</span>
                    <span>Officers must verify their email and change password on first login</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="text-yellow-400 mt-0.5">•</span>
                    <span>Official SACCO emails (@sacco.local) will be used as login usernames</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="text-yellow-400 mt-0.5">•</span>
                    <span>Temporary passwords shown above are for reference only - save them securely</span>
                  </li>
                </ul>
              </div>
            </div>
          </div>

          {/* Redirect Notice */}
          <div className="text-center">
            <div className="inline-flex items-center gap-3 px-6 py-3 bg-blue-500/20 border border-blue-500/30 rounded-full backdrop-blur-sm">
              <Clock size={18} className="text-blue-300 animate-spin" />
              <span className="text-sm text-blue-200 font-medium">Redirecting to login page in 5 seconds...</span>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900 flex">
      {/* Left Sidebar - Role Selection */}
      <div className="w-80 bg-slate-800/50 backdrop-blur-sm border-r border-white/10 p-6 overflow-y-auto">
        <div className="mb-8">
          <div className="flex items-center gap-3 mb-2">
            <div className="p-2 bg-blue-500/20 rounded-lg">
              <Building2 className="text-blue-400" size={24} />
            </div>
            <div>
              <h1 className="text-xl font-bold text-white">System Setup</h1>
              <p className="text-xs text-blue-300">Configure Leadership</p>
            </div>
          </div>
        </div>

        {/* Progress Summary */}
        <div className="mb-6 p-4 bg-gradient-to-br from-blue-500/10 to-purple-500/10 rounded-xl border border-white/10">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs text-blue-200 font-medium">Progress</span>
            <span className="text-xs text-white font-bold">{completedCount}/{admins.length}</span>
          </div>
          <div className="w-full bg-slate-700 rounded-full h-2 overflow-hidden">
            <div 
              className="bg-gradient-to-r from-emerald-500 to-blue-500 h-2 transition-all duration-500"
              style={{ width: `${(completedCount / admins.length) * 100}%` }}
            />
          </div>
        </div>

        {/* Role List */}
        <div className="space-y-2">
          <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3 flex items-center gap-2">
            <Users size={14} />
            Officers ({admins.length})
          </h3>
          {admins.map((admin, index) => (
            <button
              key={admin.role}
              onClick={() => handleRoleSelect(index)}
              disabled={loading}
              className={`w-full text-left p-4 rounded-xl transition-all duration-200 ${
                index === selectedRole
                  ? 'bg-gradient-to-r ' + admin.color + ' shadow-lg scale-105'
                  : admin.completed
                    ? 'bg-emerald-500/20 hover:bg-emerald-500/30 border border-emerald-500/30'
                    : 'bg-white/5 hover:bg-white/10 border border-white/10'
              } ${loading ? 'cursor-not-allowed opacity-50' : 'cursor-pointer'}`}
            >
              <div className="flex items-center gap-3">
                <admin.icon className="flex-shrink-0" size={24} />
                <div className="flex-1 min-w-0">
                  <p className={`text-sm font-bold truncate ${
                    index === selectedRole ? 'text-white' : admin.completed ? 'text-emerald-200' : 'text-slate-300'
                  }`}>
                    {admin.label}
                  </p>
                  <p className={`text-xs truncate ${
                    index === selectedRole ? 'text-white/80' : admin.completed ? 'text-emerald-300/70' : 'text-slate-500'
                  }`}>
                    {admin.description}
                  </p>
                </div>
                {admin.completed && (
                  <CheckCircle2 className="text-emerald-300 flex-shrink-0" size={18} />
                )}
              </div>
            </button>
          ))}
        </div>

        {/* Info Box */}
        <div className="mt-6 p-4 bg-yellow-500/10 border border-yellow-500/20 rounded-xl">
          <div className="flex gap-2 mb-2">
            <Shield className="text-yellow-400 flex-shrink-0" size={16} />
            <span className="text-xs font-semibold text-yellow-200">Security Note</span>
          </div>
          <p className="text-xs text-yellow-100/80 leading-relaxed">
            Each officer will receive secure credentials via email. Password change required on first login.
          </p>
        </div>
      </div>

      {/* Right Content - Form */}
      <div className="flex-1 overflow-y-auto p-8">
        <div className="max-w-3xl mx-auto">
          {error && (
            <div className="mb-6 p-4 bg-red-500/20 border border-red-500/30 text-red-200 rounded-xl flex items-center gap-3 backdrop-blur-sm animate-shake">
              <AlertCircle className="flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Form Card */}
          <div className="bg-white/10 backdrop-blur-xl rounded-2xl border border-white/20 p-8 shadow-2xl">
            {/* Header */}
            <div className="flex items-start gap-4 mb-8 pb-6 border-b border-white/10">
              <div className={`p-4 rounded-2xl bg-gradient-to-br ${currentAdmin.color} shadow-lg`}>
                <currentAdmin.icon size={48} className="text-white" />
              </div>
              <div className="flex-1">
                <h2 className="text-3xl font-bold text-white mb-2">{currentAdmin.label}</h2>
                <p className="text-blue-200">{currentAdmin.description}</p>
                <p className="text-xs text-slate-400 mt-2">
                  Official Email: <code className="text-blue-300 font-mono">{currentAdmin.role.toLowerCase().replace(/_/g, '')}@sacco.local</code>
                </p>
              </div>
              {currentAdmin.completed && (
                <div className="flex items-center gap-2 bg-emerald-500/20 px-3 py-1 rounded-full border border-emerald-500/30">
                  <CheckCircle2 className="text-emerald-300" size={16} />
                  <span className="text-xs font-semibold text-emerald-200">Completed</span>
                </div>
              )}
            </div>

            {/* Form Fields */}
            <div className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label className="block text-sm font-semibold text-blue-200 mb-2 flex items-center gap-2">
                    <User size={16} />
                    First Name
                  </label>
                  <input
                    type="text"
                    placeholder="Enter first name"
                    value={currentAdmin.firstName}
                    onChange={(e) => handleChange('firstName', e.target.value)}
                    className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-xl text-white placeholder-slate-400 focus:ring-2 focus:ring-blue-400 focus:border-transparent outline-none transition backdrop-blur-sm"
                  />
                </div>
                <div>
                  <label className="block text-sm font-semibold text-blue-200 mb-2 flex items-center gap-2">
                    <User size={16} />
                    Last Name
                  </label>
                  <input
                    type="text"
                    placeholder="Enter last name"
                    value={currentAdmin.lastName}
                    onChange={(e) => handleChange('lastName', e.target.value)}
                    className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-xl text-white placeholder-slate-400 focus:ring-2 focus:ring-blue-400 focus:border-transparent outline-none transition backdrop-blur-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-semibold text-blue-200 mb-2 flex items-center gap-2">
                  <Mail size={16} />
                  Personal Email Address
                </label>
                <input
                  type="email"
                  placeholder="officer@example.com"
                  value={currentAdmin.email}
                  onChange={(e) => handleChange('email', e.target.value)}
                  className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-xl text-white placeholder-slate-400 focus:ring-2 focus:ring-blue-400 focus:border-transparent outline-none transition backdrop-blur-sm"
                />
                <p className="text-xs text-slate-400 mt-2 flex items-center gap-1">
                  <Lock size={12} />
                  Credentials and verification link will be sent here
                </p>
              </div>

              <div>
                <label className="block text-sm font-semibold text-blue-200 mb-2 flex items-center gap-2">
                  <Phone size={16} />
                  Phone Number
                </label>
                <input
                  type="tel"
                  placeholder="+254 700 000 000"
                  value={currentAdmin.phoneNumber}
                  onChange={(e) => handleChange('phoneNumber', e.target.value)}
                  className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-xl text-white placeholder-slate-400 focus:ring-2 focus:ring-blue-400 focus:border-transparent outline-none transition backdrop-blur-sm"
                />
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex justify-between items-center mt-8 pt-6 border-t border-white/10">
              <button
                type="button"
                onClick={() => selectedRole > 0 && handleRoleSelect(selectedRole - 1)}
                disabled={selectedRole === 0 || loading}
                className="px-6 py-3 bg-white/10 hover:bg-white/20 text-white rounded-xl font-semibold transition disabled:opacity-30 disabled:cursor-not-allowed border border-white/10"
              >
                ← Previous
              </button>

              <div className="flex gap-3">
                {selectedRole < admins.length - 1 ? (
                  <button
                    type="button"
                    onClick={handleSaveAndNext}
                    disabled={loading}
                    className="px-8 py-3 bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white rounded-xl font-bold shadow-lg transition disabled:opacity-50 flex items-center gap-2"
                  >
                    Save & Next
                    <ArrowRight size={18} />
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={handleSubmit}
                    disabled={loading || completedCount < admins.length - 1}
                    className="px-8 py-3 bg-gradient-to-r from-emerald-500 to-emerald-600 hover:from-emerald-600 hover:to-emerald-700 text-white rounded-xl font-bold shadow-lg transition disabled:opacity-50 flex items-center gap-2"
                  >
                    {loading ? (
                      <>
                        <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                        Creating...
                      </>
                    ) : (
                      <>
                        <CheckCircle2 size={20} />
                        Complete Setup
                      </>
                    )}
                  </button>
                )}
              </div>
            </div>
          </div>

          {/* Help Text */}
          <div className="mt-6 text-center">
            <p className="text-sm text-slate-400">
              {completedCount === admins.length ? (
                <span className="text-emerald-300 font-semibold">✓ All roles configured! Ready to finalize setup.</span>
              ) : (
                <span>{admins.length - completedCount} role{admins.length - completedCount !== 1 ? 's' : ''} remaining</span>
              )}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
