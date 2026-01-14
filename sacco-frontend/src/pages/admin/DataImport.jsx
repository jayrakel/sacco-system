import React, { useState } from 'react';
import api from '../../api';
import { Upload, FileSpreadsheet, Calendar, CheckCircle, AlertCircle, Loader2, Info, Download } from 'lucide-react';

export default function DataImport() {
    const [selectedFile, setSelectedFile] = useState(null);
    const [selectedDate, setSelectedDate] = useState('');
    const [uploading, setUploading] = useState(false);
    const [result, setResult] = useState(null);
    const [dragActive, setDragActive] = useState(false);

    const handleDrag = (e) => {
        e.preventDefault();
        e.stopPropagation();
        if (e.type === "dragenter" || e.type === "dragover") {
            setDragActive(true);
        } else if (e.type === "dragleave") {
            setDragActive(false);
        }
    };

    const handleDrop = (e) => {
        e.preventDefault();
        e.stopPropagation();
        setDragActive(false);

        if (e.dataTransfer.files && e.dataTransfer.files[0]) {
            handleFileSelect(e.dataTransfer.files[0]);
        }
    };

    const handleFileSelect = (file) => {
        const validExtensions = ['.csv', '.xls', '.xlsx'];
        const fileExtension = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();

        if (!validExtensions.includes(fileExtension)) {
            setResult({
                success: false,
                message: 'Invalid file format. Please upload CSV, XLS, or XLSX files only.'
            });
            return;
        }

        setSelectedFile(file);
        setResult(null);
    };

    const handleFileChange = (e) => {
        if (e.target.files && e.target.files[0]) {
            handleFileSelect(e.target.files[0]);
        }
    };

    const handleUpload = async () => {
        if (!selectedFile) {
            setResult({
                success: false,
                message: 'Please select a file first'
            });
            return;
        }

        setUploading(true);
        setResult(null);

        try {
            const formData = new FormData();
            formData.append('file', selectedFile);
            if (selectedDate) {
                formData.append('date', selectedDate);
            }

            const response = await api.post('/api/v1/legacy-import/upload', formData, {
                headers: {
                    'Content-Type': 'multipart/form-data',
                }
            });

            setResult(response.data);
            if (response.data.success) {
                // Clear file after successful upload
                setTimeout(() => {
                    setSelectedFile(null);
                    setSelectedDate('');
                }, 3000);
            }
        } catch (error) {
            setResult({
                success: false,
                message: error.response?.data?.message || 'Upload failed: ' + error.message
            });
        } finally {
            setUploading(false);
        }
    };

    const formatFileSize = (bytes) => {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 p-6">
            <div className="max-w-4xl mx-auto space-y-6">
                {/* Header */}
                <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6">
                    <div className="flex items-center gap-4">
                        <div className="p-3 bg-indigo-50 rounded-xl">
                            <FileSpreadsheet className="text-indigo-600" size={28} />
                        </div>
                        <div>
                            <h1 className="text-2xl font-bold text-slate-800">Legacy Data Import</h1>
                            <p className="text-slate-500 text-sm">Upload Excel or CSV files to import member transactions</p>
                        </div>
                    </div>
                </div>

                {/* Instructions */}
                <div className="bg-blue-50 border border-blue-200 rounded-xl p-4">
                    <div className="flex gap-3">
                        <Info className="text-blue-600 flex-shrink-0" size={20} />
                        <div className="text-sm text-blue-800">
                            <p className="font-semibold mb-2">File Format Requirements:</p>
                            <ul className="list-disc list-inside space-y-1 text-blue-700">
                                <li>Supported formats: <strong>CSV, XLS, XLSX</strong></li>
                                <li>First row should contain headers</li>
                                <li>Required columns: Member Name, Loan Amount, Weekly Savings, Loan Paid, Other Payments</li>
                                <li>Optional: Transaction date (or specify below)</li>
                            </ul>
                        </div>
                    </div>
                </div>

                {/* Upload Area */}
                <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 space-y-6">
                    {/* Drag & Drop Zone */}
                    <div
                        className={`relative border-2 border-dashed rounded-xl p-8 transition-all ${
                            dragActive
                                ? 'border-indigo-500 bg-indigo-50'
                                : 'border-slate-300 hover:border-indigo-400 bg-slate-50'
                        }`}
                        onDragEnter={handleDrag}
                        onDragLeave={handleDrag}
                        onDragOver={handleDrag}
                        onDrop={handleDrop}
                    >
                        <input
                            type="file"
                            id="file-upload"
                            className="hidden"
                            accept=".csv,.xls,.xlsx"
                            onChange={handleFileChange}
                        />

                        <label
                            htmlFor="file-upload"
                            className="flex flex-col items-center justify-center cursor-pointer"
                        >
                            <Upload className={`mb-4 ${dragActive ? 'text-indigo-600' : 'text-slate-400'}`} size={48} />
                            <p className="text-lg font-semibold text-slate-700 mb-2">
                                {selectedFile ? selectedFile.name : 'Drop your file here or click to browse'}
                            </p>
                            <p className="text-sm text-slate-500">
                                {selectedFile
                                    ? `Size: ${formatFileSize(selectedFile.size)}`
                                    : 'CSV, XLS, or XLSX (Max 10MB)'
                                }
                            </p>
                        </label>
                    </div>

                    {/* Date Selection */}
                    <div className="space-y-2">
                        <label className="flex items-center gap-2 text-sm font-semibold text-slate-700">
                            <Calendar size={16} />
                            Transaction Date (Optional)
                        </label>
                        <input
                            type="date"
                            value={selectedDate}
                            onChange={(e) => setSelectedDate(e.target.value)}
                            className="w-full px-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                            placeholder="Leave empty to extract from filename"
                        />
                        <p className="text-xs text-slate-500">
                            If not specified, the system will attempt to extract the date from the filename
                        </p>
                    </div>

                    {/* Upload Button */}
                    <button
                        onClick={handleUpload}
                        disabled={!selectedFile || uploading}
                        className={`w-full py-3 px-6 rounded-xl font-semibold text-white flex items-center justify-center gap-2 transition-all ${
                            !selectedFile || uploading
                                ? 'bg-slate-300 cursor-not-allowed'
                                : 'bg-indigo-600 hover:bg-indigo-700 shadow-lg shadow-indigo-200'
                        }`}
                    >
                        {uploading ? (
                            <>
                                <Loader2 className="animate-spin" size={20} />
                                Processing...
                            </>
                        ) : (
                            <>
                                <Upload size={20} />
                                Upload & Import Data
                            </>
                        )}
                    </button>
                </div>

                {/* Result Message */}
                {result && (
                    <div className={`rounded-xl p-4 flex items-start gap-3 animate-in fade-in slide-in-from-top-2 duration-300 ${
                        result.success
                            ? 'bg-emerald-50 border border-emerald-200'
                            : 'bg-red-50 border border-red-200'
                    }`}>
                        {result.success ? (
                            <CheckCircle className="text-emerald-600 flex-shrink-0" size={24} />
                        ) : (
                            <AlertCircle className="text-red-600 flex-shrink-0" size={24} />
                        )}
                        <div className="flex-1">
                            <p className={`font-semibold ${result.success ? 'text-emerald-800' : 'text-red-800'}`}>
                                {result.success ? 'Success!' : 'Error'}
                            </p>
                            <p className={`text-sm ${result.success ? 'text-emerald-700' : 'text-red-700'}`}>
                                {result.message}
                            </p>
                        </div>
                    </div>
                )}

                {/* Sample Template Download */}
                <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6">
                    <h3 className="font-semibold text-slate-800 mb-3 flex items-center gap-2">
                        <Download size={18} />
                        Need a template?
                    </h3>
                    <p className="text-sm text-slate-600 mb-4">
                        Download our sample Excel template to ensure your data is formatted correctly
                    </p>
                    <a
                        href="/sample-template.xlsx"
                        download
                        className="inline-flex items-center gap-2 px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-sm font-medium transition-colors"
                    >
                        <Download size={16} />
                        Download Sample Template
                    </a>
                </div>
            </div>
        </div>
    );
}

