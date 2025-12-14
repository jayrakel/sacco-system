import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../api';
import { UserPlus, Save, X, AlertCircle, User, Users, Camera, Info } from 'lucide-react';

export default function AddMember() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [regFee, setRegFee] = useState('0'); // Store fee

  const [selectedFile, setSelectedFile] = useState(null);
  const [preview, setPreview] = useState(null);

  const [formData, setFormData] = useState({
    firstName: '', lastName: '', email: '', phoneNumber: '',
    idNumber: '', kraPin: '', dateOfBirth: '', address: '',
    nextOfKinName: '', nextOfKinPhone: '', nextOfKinRelation: ''
  });

  // ✅ Fetch the Registration Fee on load
  useEffect(() => {
    const fetchFee = async () => {
        try {
            const res = await api.get('/api/settings');
            const fee = res.data.data.find(s => s.key === 'REGISTRATION_FEE')?.value || '0';
            setRegFee(fee);
        } catch(e) { console.error(e); }
    };
    fetchFee();
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

    try {
      const submitData = new FormData();
      const jsonBlob = new Blob([JSON.stringify(formData)], { type: "application/json" });
      submitData.append("member", jsonBlob);
      if (selectedFile) submitData.append("file", selectedFile);

      const response = await api.post('/api/members', submitData, {
          headers: { "Content-Type": "multipart/form-data" }
      });

      if (response.data.success) {
        alert(`Member Registered!\nMember No: ${response.data.data.memberNumber}\nFee Collected: KES ${Number(regFee).toLocaleString()}`);
        navigate(-1);
      }
    } catch (err) {
      console.error("Registration Error:", err);
      setError(err.response?.data?.message || "Failed to register member.");
    }
    setLoading(false);
  };

  return (
    <div className="min-h-screen bg-slate-50 p-8 font-sans flex justify-center items-start">
      <div className="max-w-4xl w-full bg-white rounded-xl shadow-lg border border-slate-200 overflow-hidden">

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

          <div className="flex flex-col items-center justify-center">
            <div className="w-32 h-32 rounded-full bg-slate-100 border-2 border-dashed border-slate-300 flex items-center justify-center overflow-hidden relative group">
                {preview ? (
                    <img src={preview} alt="Preview" className="w-full h-full object-cover" />
                ) : (
                    <Camera className="text-slate-400" size={32} />
                )}
                <input type="file" accept="image/*" onChange={handleFileChange} className="absolute inset-0 opacity-0 cursor-pointer"/>
            </div>
            <p className="text-sm text-slate-500 mt-2">Click to upload photo</p>
          </div>

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
                <input type="date" name="dateOfBirth" required className="input-field" onChange={handleChange}/>
                <input type="text" name="address" placeholder="Physical Address" required className="input-field" onChange={handleChange}/>
            </div>
          </div>

          <div>
            <div className="flex items-center gap-2 mb-4 border-b pb-2">
                <h3 className="text-lg font-bold text-slate-800">Contact Details</h3>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <input type="email" name="email" placeholder="Email Address" required className="input-field" onChange={handleChange}/>
                <input type="tel" name="phoneNumber" placeholder="Phone Number" required className="input-field" onChange={handleChange}/>
            </div>
          </div>

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

          {/* ✅ FEE NOTICE FOOTER */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
                <div className="p-2 bg-blue-100 rounded-full text-blue-600">
                    <Info size={20} />
                </div>
                <div>
                    <p className="text-sm font-bold text-slate-800">Registration Fee Required</p>
                    <p className="text-xs text-slate-500">This amount must be collected from the applicant.</p>
                </div>
            </div>
            <div className="text-xl font-bold text-blue-700">
                KES {Number(regFee).toLocaleString()}
            </div>
          </div>

          <div className="pt-4 border-t flex justify-end">
            <button
              type="submit" disabled={loading}
              className="bg-emerald-600 hover:bg-emerald-700 text-white px-8 py-3 rounded-lg font-bold flex items-center gap-2 transition disabled:opacity-50"
            >
              {loading ? "Saving..." : <><Save size={20} /> Register & Pay Fee</>}
            </button>
          </div>

        </form>
      </div>
      <style>{`.input-field { width: 100%; padding: 12px; border: 1px solid #e2e8f0; border-radius: 8px; outline: none; transition: all 0.2s; } .input-field:focus { border-color: #0f172a; box-shadow: 0 0 0 2px rgba(15, 23, 42, 0.1); }`}</style>
    </div>
  );
}