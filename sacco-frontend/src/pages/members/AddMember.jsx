import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../api';
import { UserPlus, Save, X, AlertCircle, User, Users, Camera, Info, CreditCard, CheckCircle, Building } from 'lucide-react';

export default function AddMember() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [regFee, setRegFee] = useState('0');
  
  // ✅ New State for Bank Accounts & Success Modal
  const [bankAccounts, setBankAccounts] = useState([]);
  const [showSuccessModal, setShowSuccessModal] = useState(false);
  const [createdMemberData, setCreatedMemberData] = useState(null);

  const [selectedFile, setSelectedFile] = useState(null);
  const [preview, setPreview] = useState(null);

  const [formData, setFormData] = useState({
    firstName: '', lastName: '', email: '', phoneNumber: '',
    idNumber: '', kraPin: '', dateOfBirth: '', address: '',
    nextOfKinName: '', nextOfKinPhone: '', nextOfKinRelation: '',
    paymentMethod: 'CASH',
    referenceCode: '',
    bankAccountCode: '' // ✅ Added state for selected bank
  });

  useEffect(() => {
    const initData = async () => {
        try {
            // 1. Fetch Settings
            const res = await api.get('/api/settings');
            const fee = res.data.data.find(s => s.key === 'REGISTRATION_FEE')?.value || '0';
            setRegFee(fee);

            // 2. ✅ Fetch Accounts for dropdown
            const accountsRes = await api.get('/api/accounting/accounts');
            if(accountsRes.data.success) {
                // Filter only Active BANKS (Codes 1010 to 1099 based on your accounts.json)
                const banks = accountsRes.data.data.filter(acc => 
                    acc.type === 'ASSET' && 
                    acc.active && 
                    parseInt(acc.code) >= 1010 && 
                    parseInt(acc.code) <= 1099
                );
                setBankAccounts(banks);
            }
        } catch(e) { console.error(e); }
    };
    initData();
  }, []);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
        setSelectedFile(file);
        setPreview(URL.createObjectURL(file));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    if (formData.paymentMethod !== 'CASH' && !formData.referenceCode) {
        setError("Reference Code is required for MPESA/Bank payments.");
        setLoading(false);
        return;
    }

    // ✅ Validate Bank Selection
    if (formData.paymentMethod === 'BANK_TRANSFER' && !formData.bankAccountCode) {
        setError("Please select the Bank Account that received the funds.");
        setLoading(false);
        return;
    }

    try {
      const submitData = new FormData();

      const memberJson = JSON.stringify({
          firstName: formData.firstName,
          lastName: formData.lastName,
          email: formData.email,
          phoneNumber: formData.phoneNumber,
          idNumber: formData.idNumber,
          kraPin: formData.kraPin,
          dateOfBirth: formData.dateOfBirth,
          address: formData.address,
          nextOfKinName: formData.nextOfKinName,
          nextOfKinPhone: formData.nextOfKinPhone,
          nextOfKinRelation: formData.nextOfKinRelation
      });

      const jsonBlob = new Blob([memberJson], { type: "application/json" });
      submitData.append("member", jsonBlob);

      if (selectedFile) submitData.append("file", selectedFile);

      // ✅ Send bankAccountCode in params
      const response = await api.post('/api/members', submitData, {
          params: {
              paymentMethod: formData.paymentMethod,
              referenceCode: formData.paymentMethod === 'CASH' ? 'CASH' : formData.referenceCode,
              bankAccountCode: formData.bankAccountCode // Sending the selected bank
          }
      });

      if (response.data.success) {
        // ✅ Show Modal instead of alert
        setCreatedMemberData(response.data.data);
        setShowSuccessModal(true);
      }
    } catch (err) {
      console.error("Registration Error:", err);
      const msg = err.response?.data?.message || "Failed to register member.";
      setError(msg);
    }
    setLoading(false);
  };

  return (
    <div className="min-h-screen bg-slate-50 p-8 font-sans flex justify-center items-start relative">
      <div className="max-w-6xl w-full bg-white rounded-xl shadow-lg border border-slate-200 overflow-hidden">
        
        {/* Header */}
        <div className="bg-slate-900 text-white p-6 flex justify-between items-center">
          <div className="flex items-center gap-3">
            <UserPlus className="text-emerald-400" size={28} />
            <div>
              <h2 className="text-xl font-bold">Register New Member</h2>
              <p className="text-slate-400 text-sm">Enter applicant details.</p>
            </div>
          </div>
          <button onClick={() => navigate(-1)} className="text-slate-400 hover:text-white transition">
            <X size={24} />
          </button>
        </div>

        {error && (
          <div className="m-6 p-4 bg-red-50 text-red-700 rounded-lg flex items-center gap-3 border-l-4 border-red-500">
            <AlertCircle size={20} />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="p-8 space-y-8">
          {/* ... (Photo, Personal Info, Contact, Next of Kin sections remain UNCHANGED) ... */}
          {/* I am omitting them here for brevity, keep your existing code for these sections */}
          
          {/* Profile Photo */}
          <div className="flex flex-col items-center justify-center">
            <div className="w-32 h-32 rounded-full bg-slate-100 border-2 border-dashed border-slate-300 flex items-center justify-center overflow-hidden relative group hover:border-emerald-500 transition">
                {preview ? <img src={preview} alt="Preview" className="w-full h-full object-cover" /> : <Camera className="text-slate-400" size={32} />}
                <input type="file" accept="image/*" onChange={handleFileChange} className="absolute inset-0 opacity-0 cursor-pointer"/>
            </div>
            <p className="text-sm text-slate-500 mt-2">Click to upload photo</p>
          </div>

           {/* Personal Info */}
           <div>
            <div className="flex items-center gap-2 mb-4 border-b pb-2">
                <User className="text-blue-600" size={20}/>
                <h3 className="text-lg font-bold text-slate-800">Personal Information</h3>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <input type="text" name="firstName" placeholder="First Name" required className="input-field" onChange={handleChange}/>
                <input type="text" name="lastName" placeholder="Last Name" required className="input-field" onChange={handleChange}/>
                <input type="text" name="idNumber" placeholder="ID / Passport Number" required className="input-field" onChange={handleChange}/>
                <input type="text" name="kraPin" placeholder="KRA PIN" required className="input-field" onChange={handleChange}/>
                <div>
                    <label className="text-xs text-slate-500 font-bold ml-1">Date of Birth</label>
                    <input type="date" name="dateOfBirth" required className="input-field mt-1" onChange={handleChange}/>
                </div>
                <div>
                    <label className="text-xs text-slate-500 font-bold ml-1">Physical Address</label>
                    <input type="text" name="address" placeholder="City, Street, House No." required className="input-field mt-1" onChange={handleChange}/>
                </div>
            </div>
          </div>

          {/* Contact Details */}
          <div>
            <div className="flex items-center gap-2 mb-4 border-b pb-2">
                <h3 className="text-lg font-bold text-slate-800">Contact Details</h3>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <input type="email" name="email" placeholder="Email Address" required className="input-field" onChange={handleChange}/>
                <input type="tel" name="phoneNumber" placeholder="Phone Number" required className="input-field" onChange={handleChange}/>
            </div>
          </div>

          {/* Next of Kin */}
          <div>
            <div className="flex items-center gap-2 mb-4 border-b pb-2">
                <Users className="text-blue-600" size={20}/>
                <h3 className="text-lg font-bold text-slate-800">Next of Kin</h3>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <input type="text" name="nextOfKinName" placeholder="Full Name" required className="input-field" onChange={handleChange}/>
                <input type="tel" name="nextOfKinPhone" placeholder="Phone Number" required className="input-field" onChange={handleChange}/>
                <input type="text" name="nextOfKinRelation" placeholder="Relationship" required className="input-field" onChange={handleChange}/>
            </div>
          </div>

          {/* ✅ Payment Details Section (UPDATED) */}
          <div className="bg-slate-50 p-6 rounded-lg border border-slate-200">
            <div className="flex items-center gap-2 mb-4 pb-2 border-b border-slate-200">
                <CreditCard className="text-emerald-600" size={20}/>
                <h3 className="text-lg font-bold text-slate-800">Registration Fee Payment</h3>
            </div>

            <div className="flex items-center justify-between mb-6 bg-blue-50 p-3 rounded border border-blue-100">
                <div className="flex items-center gap-2">
                    <Info className="text-blue-600" size={18} />
                    <span className="text-sm text-blue-800">Required Fee Amount:</span>
                </div>
                <span className="text-lg font-bold text-blue-900">KES {Number(regFee).toLocaleString()}</span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                    <label className="block text-sm font-bold text-slate-700 mb-2">Payment Method</label>
                    <select
                        name="paymentMethod"
                        className="input-field"
                        value={formData.paymentMethod}
                        onChange={handleChange}
                    >
                        <option value="CASH">Cash</option>
                        <option value="MPESA">M-Pesa</option>
                        <option value="BANK_TRANSFER">Bank Transfer</option>
                    </select>
                </div>

                {formData.paymentMethod !== 'CASH' && (
                    <div>
                        <label className="block text-sm font-bold text-slate-700 mb-2">Transaction Reference</label>
                        <input
                            type="text"
                            name="referenceCode"
                            placeholder="e.g. QH8282..."
                            required
                            className="input-field uppercase"
                            onChange={handleChange}
                        />
                    </div>
                )}

                {/* ✅ Dynamic Bank Dropdown */}
                {formData.paymentMethod === 'BANK_TRANSFER' && (
                    <div className="md:col-span-2 bg-white p-4 rounded border border-slate-200 shadow-sm">
                        <div className="flex items-center gap-2 mb-2 text-slate-700 font-bold">
                            <Building size={18} />
                            <span>Select Receiving Bank Account</span>
                        </div>
                        <select
                            name="bankAccountCode"
                            className="input-field border-emerald-500 ring-1 ring-emerald-100"
                            onChange={handleChange}
                            required
                        >
                            <option value="">-- Choose Account --</option>
                            {bankAccounts.map(acc => (
                                <option key={acc.code} value={acc.code}>
                                    {acc.name} ({acc.code}) - Bal: {acc.balance.toLocaleString()}
                                </option>
                            ))}
                        </select>
                        <p className="text-xs text-slate-500 mt-1">
                            Select the internal Sacco account where the transfer was received.
                        </p>
                    </div>
                )}
            </div>
          </div>

          <div className="pt-4 border-t flex justify-end">
            <button
              type="submit" disabled={loading}
              className="bg-emerald-600 hover:bg-emerald-700 text-white px-8 py-3 rounded-lg font-bold flex items-center gap-2 transition disabled:opacity-50 shadow-lg hover:shadow-emerald-500/30"
            >
              {loading ? "Registering..." : <><Save size={20} /> Register Member</>}
            </button>
          </div>
        </form>
      </div>

      {/* ✅ CUSTOM SUCCESS MODAL */}
      {showSuccessModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-6 relative animate-in zoom-in-95 duration-200">
                <div className="absolute -top-12 left-1/2 -translate-x-1/2 bg-emerald-500 p-4 rounded-full shadow-lg border-4 border-white">
                    <CheckCircle className="text-white w-12 h-12" />
                </div>
                
                <div className="mt-8 text-center">
                    <h3 className="text-2xl font-bold text-slate-800">Registration Successful!</h3>
                    <p className="text-slate-500 mt-2">The member has been added to the system.</p>
                    
                    <div className="bg-slate-50 rounded-lg p-4 mt-6 border border-slate-200">
                        <p className="text-xs font-bold text-slate-400 uppercase tracking-wider">Member Number</p>
                        <p className="text-3xl font-mono font-bold text-emerald-600 mt-1">
                            {createdMemberData?.memberNumber}
                        </p>
                    </div>

                    <div className="mt-6 flex gap-3">
                        <button 
                            onClick={() => navigate('/admin-dashboard?tab=members')}
                            className="flex-1 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold py-3 rounded-lg transition"
                        >
                            View All Members
                        </button>
                        <button 
                            onClick={() => navigate('/admin-dashboard?tab=register')}
                            className="flex-1 bg-emerald-600 hover:bg-emerald-700 text-white font-bold py-3 rounded-lg transition shadow-lg hover:shadow-emerald-500/30"
                        >
                            Add Another
                        </button>
                    </div>
                </div>
            </div>
        </div>
      )}

      <style>{`.input-field { width: 100%; padding: 12px; border: 1px solid #cbd5e1; border-radius: 8px; outline: none; background: #f8fafc; transition: all 0.2s; } .input-field:focus { background: white; border-color: #0f172a; box-shadow: 0 0 0 2px rgba(15, 23, 42, 0.1); }`}</style>
    </div>
  );
}