import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api';
import DashboardHeader from '../components/DashboardHeader';
import BrandedSpinner from '../components/BrandedSpinner';
import {
    Calendar, Clock, MapPin, ArrowLeft, Save, Edit,
    FileText, Users, CheckCircle, Eye, Download, Printer, ChevronDown
} from 'lucide-react';

export default function MeetingMinutesPage() {
    const { meetingId } = useParams();
    const navigate = useNavigate();
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [meetingDetails, setMeetingDetails] = useState(null);
    const [minutes, setMinutes] = useState('');
    const [isEditing, setIsEditing] = useState(false);
    const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
    const [showExportMenu, setShowExportMenu] = useState(false);
    const editorRef = useRef(null);

    useEffect(() => {
        const storedUser = localStorage.getItem('sacco_user');
        if (storedUser) setUser(JSON.parse(storedUser));
        loadMeetingData();
    }, [meetingId]);

    // Warn user about unsaved changes
    useEffect(() => {
        const handleBeforeUnload = (e) => {
            if (hasUnsavedChanges) {
                e.preventDefault();
                e.returnValue = '';
            }
        };

        window.addEventListener('beforeunload', handleBeforeUnload);
        return () => window.removeEventListener('beforeunload', handleBeforeUnload);
    }, [hasUnsavedChanges]);

    const loadMeetingData = async () => {
        setLoading(true);
        try {
            const res = await api.get(`/api/meetings/${meetingId}`);
            const meeting = res.data.data;
            setMeetingDetails(meeting);
            setMinutes(meeting.minutes || '');

            // Auto-enable editing if no minutes exist
            if (!meeting.minutes) {
                setIsEditing(true);
            }
        } catch (error) {
            console.error('Failed to load meeting data:', error);
            alert('Failed to load meeting data');
        } finally {
            setLoading(false);
        }
    };

    const handleSaveMinutes = async () => {
        if (!hasUnsavedChanges) {
            alert('No changes to save');
            return;
        }

        setSaving(true);
        try {
            await api.patch(`/api/meetings/${meetingId}/minutes`, {
                minutes: minutes
            });

            alert('Minutes saved successfully!');
            setHasUnsavedChanges(false);
            setIsEditing(false);
            loadMeetingData(); // Reload to get updated data
        } catch (error) {
            alert(error.response?.data?.message || 'Failed to save minutes');
        } finally {
            setSaving(false);
        }
    };

    const handleMinutesChange = (e) => {
        setMinutes(e.target.value);
        setHasUnsavedChanges(true);
    };

    // Rich text formatting helpers
    const insertFormatting = (prefix, suffix = '') => {
        const textarea = editorRef.current;
        if (!textarea) return;

        const start = textarea.selectionStart;
        const end = textarea.selectionEnd;
        const selectedText = minutes.substring(start, end);
        const before = minutes.substring(0, start);
        const after = minutes.substring(end);

        const newText = before + prefix + selectedText + suffix + after;
        setMinutes(newText);
        setHasUnsavedChanges(true);

        // Set cursor position after inserted text
        setTimeout(() => {
            textarea.focus();
            const newCursorPos = start + prefix.length + selectedText.length + suffix.length;
            textarea.setSelectionRange(newCursorPos, newCursorPos);
        }, 0);
    };

    const makeBold = () => insertFormatting('**', '**');
    const makeItalic = () => insertFormatting('_', '_');
    const makeHeading = () => {
        const textarea = editorRef.current;
        const start = textarea.selectionStart;
        const lineStart = minutes.lastIndexOf('\n', start - 1) + 1;
        insertFormatting('\n### ', '');
    };
    const insertBullet = () => insertFormatting('\n- ');
    const insertNumber = () => insertFormatting('\n1. ');
    const insertSeparator = () => insertFormatting('\n' + '='.repeat(60) + '\n');


    const handlePrint = () => {
        window.print();
    };

    const handleExportWord = () => {
        // Create HTML content with proper Word formatting
        const htmlContent = `
            <html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:w='urn:schemas-microsoft-com:office:word' xmlns='http://www.w3.org/TR/REC-html40'>
            <head>
                <meta charset='utf-8'>
                <title>Meeting Minutes</title>
                <style>
                    body {
                        font-family: 'Calibri', 'Arial', sans-serif;
                        font-size: 11pt;
                        line-height: 1.5;
                        margin: 1in;
                    }
                    h1 { font-size: 16pt; font-weight: bold; margin-bottom: 12pt; }
                    h2 { font-size: 14pt; font-weight: bold; margin-top: 12pt; margin-bottom: 6pt; }
                    h3 { font-size: 12pt; font-weight: bold; margin-top: 6pt; margin-bottom: 6pt; }
                    p { margin: 6pt 0; white-space: pre-wrap; }
                    .header { text-align: center; margin-bottom: 24pt; }
                    .metadata { margin-bottom: 12pt; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>${meetingDetails.title}</h1>
                    <p><strong>Meeting Number:</strong> ${meetingDetails.meetingNumber}</p>
                </div>
                <div class="metadata">
                    <p><strong>Date:</strong> ${new Date(meetingDetails.meetingDate).toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}</p>
                    <p><strong>Time:</strong> ${meetingDetails.meetingTime}</p>
                    <p><strong>Venue:</strong> ${meetingDetails.venue}</p>
                </div>
                <hr>
                <div class="content">
                    ${minutes.split('\n').map(line => `<p>${line || '&nbsp;'}</p>`).join('')}
                </div>
            </body>
            </html>
        `;

        const blob = new Blob([htmlContent], { type: 'application/msword' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Minutes_${meetingDetails.meetingNumber}.doc`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        setShowExportMenu(false);
    };

    const handleExportPDF = async () => {
        // For PDF, we'll use the print dialog with instructions
        alert('PDF Export: Please use your browser\'s Print function and select "Save as PDF" as the destination.\n\nSteps:\n1. Click OK\n2. Press Ctrl+P (or Cmd+P on Mac)\n3. Select "Save as PDF"\n4. Click Save');
        handlePrint();
        setShowExportMenu(false);
    };

    const handleExportHTML = () => {
        const htmlContent = `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${meetingDetails.title} - Minutes</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            max-width: 800px;
            margin: 40px auto;
            padding: 20px;
            line-height: 1.6;
            background: #f5f5f5;
        }
        .container {
            background: white;
            padding: 40px;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        h1 {
            color: #2c3e50;
            border-bottom: 3px solid #3498db;
            padding-bottom: 10px;
        }
        .metadata {
            background: #ecf0f1;
            padding: 15px;
            border-radius: 5px;
            margin: 20px 0;
        }
        .metadata p {
            margin: 5px 0;
        }
        .content {
            margin-top: 30px;
        }
        pre {
            white-space: pre-wrap;
            font-family: inherit;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>${meetingDetails.title}</h1>
        <p><strong>Meeting Number:</strong> ${meetingDetails.meetingNumber}</p>

        <div class="metadata">
            <p><strong>Date:</strong> ${new Date(meetingDetails.meetingDate).toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}</p>
            <p><strong>Time:</strong> ${meetingDetails.meetingTime}</p>
            <p><strong>Venue:</strong> ${meetingDetails.venue}</p>
            <p><strong>Status:</strong> ${meetingDetails.status}</p>
        </div>

        <div class="content">
            <h2>Meeting Minutes</h2>
            <pre>${minutes}</pre>
        </div>

        <footer style="margin-top: 40px; padding-top: 20px; border-top: 1px solid #ddd; text-align: center; color: #7f8c8d; font-size: 0.9em;">
            <p>Generated on ${new Date().toLocaleString()}</p>
        </footer>
    </div>
</body>
</html>`;

        const blob = new Blob([htmlContent], { type: 'text/html' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Minutes_${meetingDetails.meetingNumber}.html`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        setShowExportMenu(false);
    };

    const handleExportTXT = () => {
        const txtContent = `
${meetingDetails.title.toUpperCase()}
${'='.repeat(meetingDetails.title.length)}

Meeting Number: ${meetingDetails.meetingNumber}
Date: ${new Date(meetingDetails.meetingDate).toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
Time: ${meetingDetails.meetingTime}
Venue: ${meetingDetails.venue}
Status: ${meetingDetails.status}

${'='.repeat(60)}

${minutes}

${'='.repeat(60)}
Generated: ${new Date().toLocaleString()}
`;

        const blob = new Blob([txtContent], { type: 'text/plain' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Minutes_${meetingDetails.meetingNumber}.txt`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        setShowExportMenu(false);
    };

    const handleExportRTF = () => {
        // Create RTF content
        const rtfContent = `{\\rtf1\\ansi\\deff0
{\\fonttbl{\\f0 Times New Roman;}}
{\\colortbl;\\red0\\green0\\blue0;\\red0\\green0\\blue255;}
\\paperw12240\\paperh15840\\margl1440\\margr1440\\margt1440\\margb1440
\\f0\\fs24

{\\pard\\qc\\b\\fs32 ${meetingDetails.title}\\par}
{\\pard\\qc Meeting Number: ${meetingDetails.meetingNumber}\\par}
\\par
{\\pard\\b Date: \\b0 ${new Date(meetingDetails.meetingDate).toLocaleDateString()}\\par}
{\\pard\\b Time: \\b0 ${meetingDetails.meetingTime}\\par}
{\\pard\\b Venue: \\b0 ${meetingDetails.venue}\\par}
\\par
\\line
\\par
${minutes.split('\n').map(line => `{\\pard ${line.replace(/[\\{}]/g, '')}\\par}`).join('\n')}
\\par
\\line
\\par
{\\pard\\qc\\i Generated: ${new Date().toLocaleString()}\\par}
}`;

        const blob = new Blob([rtfContent], { type: 'application/rtf' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Minutes_${meetingDetails.meetingNumber}.rtf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        setShowExportMenu(false);
    };

    const handleExportMarkdown = () => {
        const mdContent = `# ${meetingDetails.title}

**Meeting Number:** ${meetingDetails.meetingNumber}

## Meeting Details

- **Date:** ${new Date(meetingDetails.meetingDate).toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
- **Time:** ${meetingDetails.meetingTime}
- **Venue:** ${meetingDetails.venue}
- **Status:** ${meetingDetails.status}

---

## Meeting Minutes

${minutes}

---

*Generated: ${new Date().toLocaleString()}*
`;

        const blob = new Blob([mdContent], { type: 'text/markdown' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Minutes_${meetingDetails.meetingNumber}.md`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        setShowExportMenu(false);
    };

    const getStatusBadgeColor = (status) => {
        switch (status) {
            case 'COMPLETED':
                return 'bg-green-100 text-green-800';
            case 'VOTING_CLOSED':
                return 'bg-orange-100 text-orange-800';
            case 'IN_PROGRESS':
                return 'bg-blue-100 text-blue-800';
            case 'SCHEDULED':
                return 'bg-slate-100 text-slate-800';
            default:
                return 'bg-slate-100 text-slate-800';
        }
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <BrandedSpinner />
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-slate-50">
            <DashboardHeader user={user} title="Meeting Minutes" />

            <main className="max-w-7xl mx-auto px-4 sm:px-6 mt-8 pb-12">
                {/* Back Button */}
                <button
                    onClick={() => navigate(-1)}
                    className="flex items-center gap-2 text-slate-600 hover:text-slate-800 mb-4 print:hidden"
                >
                    <ArrowLeft size={20} />
                    Back
                </button>

                {/* Meeting Header */}
                <div className="bg-white rounded-lg shadow-md p-6 mb-6 print:shadow-none">
                    <div className="flex justify-between items-start mb-4">
                        <div className="flex-1">
                            <h1 className="text-3xl font-bold text-slate-800 mb-2">{meetingDetails.title}</h1>
                            <p className="text-sm text-slate-500">Meeting #: {meetingDetails.meetingNumber}</p>
                        </div>
                        <div className="flex gap-2">
                            <span className={`px-3 py-1 rounded-full text-sm font-semibold ${getStatusBadgeColor(meetingDetails.status)}`}>
                                {meetingDetails.status.replace('_', ' ')}
                            </span>
                        </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-slate-600">
                        <div className="flex items-center gap-2">
                            <Calendar size={16} />
                            <span>{new Date(meetingDetails.meetingDate).toLocaleDateString('en-US', {
                                weekday: 'long',
                                year: 'numeric',
                                month: 'long',
                                day: 'numeric'
                            })}</span>
                        </div>
                        <div className="flex items-center gap-2">
                            <Clock size={16} />
                            <span>{meetingDetails.meetingTime}</span>
                        </div>
                        <div className="flex items-center gap-2">
                            <MapPin size={16} />
                            <span>{meetingDetails.venue}</span>
                        </div>
                    </div>
                </div>

                {/* Action Buttons */}
                <div className="flex gap-3 mb-6 print:hidden">
                    {!isEditing ? (
                        <>
                            <button
                                onClick={() => setIsEditing(true)}
                                className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-semibold transition"
                            >
                                <Edit size={18} />
                                Edit Minutes
                            </button>
                            <button
                                onClick={handlePrint}
                                className="flex items-center gap-2 px-4 py-2 bg-slate-600 hover:bg-slate-700 text-white rounded-lg font-semibold transition"
                            >
                                <Printer size={18} />
                                Print
                            </button>

                            {/* Export Dropdown */}
                            <div className="relative">
                                <button
                                    onClick={() => setShowExportMenu(!showExportMenu)}
                                    className="flex items-center gap-2 px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg font-semibold transition"
                                >
                                    <Download size={18} />
                                    Export
                                    <ChevronDown size={16} className={`transition-transform ${showExportMenu ? 'rotate-180' : ''}`} />
                                </button>

                                {showExportMenu && (
                                    <div className="absolute top-full mt-2 right-0 bg-white border border-slate-200 rounded-lg shadow-lg py-2 min-w-[200px] z-10">
                                        <button
                                            onClick={handleExportWord}
                                            className="w-full text-left px-4 py-2 hover:bg-slate-50 flex items-center gap-2 text-slate-700"
                                        >
                                            <FileText size={16} className="text-blue-600" />
                                            <div>
                                                <div className="font-semibold">Word Document</div>
                                                <div className="text-xs text-slate-500">.doc format</div>
                                            </div>
                                        </button>
                                        <button
                                            onClick={handleExportPDF}
                                            className="w-full text-left px-4 py-2 hover:bg-slate-50 flex items-center gap-2 text-slate-700"
                                        >
                                            <FileText size={16} className="text-red-600" />
                                            <div>
                                                <div className="font-semibold">PDF Document</div>
                                                <div className="text-xs text-slate-500">Print to PDF</div>
                                            </div>
                                        </button>
                                        <button
                                            onClick={handleExportHTML}
                                            className="w-full text-left px-4 py-2 hover:bg-slate-50 flex items-center gap-2 text-slate-700"
                                        >
                                            <FileText size={16} className="text-orange-600" />
                                            <div>
                                                <div className="font-semibold">HTML Webpage</div>
                                                <div className="text-xs text-slate-500">.html format</div>
                                            </div>
                                        </button>
                                        <button
                                            onClick={handleExportTXT}
                                            className="w-full text-left px-4 py-2 hover:bg-slate-50 flex items-center gap-2 text-slate-700"
                                        >
                                            <FileText size={16} className="text-slate-600" />
                                            <div>
                                                <div className="font-semibold">Plain Text</div>
                                                <div className="text-xs text-slate-500">.txt format</div>
                                            </div>
                                        </button>
                                        <button
                                            onClick={handleExportRTF}
                                            className="w-full text-left px-4 py-2 hover:bg-slate-50 flex items-center gap-2 text-slate-700"
                                        >
                                            <FileText size={16} className="text-purple-600" />
                                            <div>
                                                <div className="font-semibold">Rich Text Format</div>
                                                <div className="text-xs text-slate-500">.rtf format</div>
                                            </div>
                                        </button>
                                        <button
                                            onClick={handleExportMarkdown}
                                            className="w-full text-left px-4 py-2 hover:bg-slate-50 flex items-center gap-2 text-slate-700"
                                        >
                                            <FileText size={16} className="text-green-600" />
                                            <div>
                                                <div className="font-semibold">Markdown</div>
                                                <div className="text-xs text-slate-500">.md format</div>
                                            </div>
                                        </button>
                                    </div>
                                )}
                            </div>
                        </>
                    ) : (
                        <>
                            <button
                                onClick={handleSaveMinutes}
                                disabled={saving || !hasUnsavedChanges}
                                className="flex items-center gap-2 px-4 py-2 bg-green-600 hover:bg-green-700 disabled:bg-slate-400 text-white rounded-lg font-semibold transition"
                            >
                                <Save size={18} />
                                {saving ? 'Saving...' : 'Save Minutes'}
                            </button>
                            <button
                                onClick={() => {
                                    if (hasUnsavedChanges) {
                                        if (window.confirm('You have unsaved changes. Discard them?')) {
                                            setMinutes(meetingDetails.minutes || '');
                                            setIsEditing(false);
                                            setHasUnsavedChanges(false);
                                        }
                                    } else {
                                        setIsEditing(false);
                                    }
                                }}
                                className="flex items-center gap-2 px-4 py-2 border border-slate-300 hover:bg-slate-50 rounded-lg font-semibold transition"
                            >
                                Cancel
                            </button>
                            {hasUnsavedChanges && (
                                <span className="flex items-center gap-2 text-amber-600 font-semibold">
                                    <span className="h-2 w-2 bg-amber-600 rounded-full animate-pulse"></span>
                                    Unsaved changes
                                </span>
                            )}
                        </>
                    )}
                </div>

                {/* Minutes Content */}
                <div className="bg-white rounded-lg shadow-md overflow-hidden print:shadow-none">
                    <div className="px-6 py-4 border-b border-gray-200 print:border-b-2 print:border-black">
                        <h2 className="text-xl font-semibold text-gray-900 flex items-center gap-2">
                            <FileText size={20} />
                            Meeting Minutes
                        </h2>
                    </div>

                    <div className="p-6">
                        {isEditing ? (
                            <div>
                                <div className="mb-4 p-3 bg-blue-50 border border-blue-200 rounded-lg print:hidden">
                                    <p className="text-sm text-blue-800">
                                        <strong>Editing Mode:</strong> You can edit the auto-generated minutes or write your own.
                                        Use the formatting toolbar below to style your text.
                                    </p>
                                </div>

                                {/* Rich Text Formatting Toolbar */}
                                <div className="mb-3 p-3 bg-slate-50 border border-slate-200 rounded-lg flex flex-wrap gap-2 print:hidden">
                                    <button
                                        type="button"
                                        onClick={makeBold}
                                        className="px-3 py-1.5 bg-white border border-slate-300 rounded hover:bg-slate-100 font-bold text-sm"
                                        title="Bold (surround with **)"
                                    >
                                        <strong>B</strong>
                                    </button>
                                    <button
                                        type="button"
                                        onClick={makeItalic}
                                        className="px-3 py-1.5 bg-white border border-slate-300 rounded hover:bg-slate-100 italic text-sm"
                                        title="Italic (surround with _)"
                                    >
                                        <em>I</em>
                                    </button>
                                    <button
                                        type="button"
                                        onClick={makeHeading}
                                        className="px-3 py-1.5 bg-white border border-slate-300 rounded hover:bg-slate-100 font-semibold text-sm"
                                        title="Heading (### )"
                                    >
                                        H
                                    </button>
                                    <div className="w-px bg-slate-300"></div>
                                    <button
                                        type="button"
                                        onClick={insertBullet}
                                        className="px-3 py-1.5 bg-white border border-slate-300 rounded hover:bg-slate-100 text-sm"
                                        title="Bullet point"
                                    >
                                        • List
                                    </button>
                                    <button
                                        type="button"
                                        onClick={insertNumber}
                                        className="px-3 py-1.5 bg-white border border-slate-300 rounded hover:bg-slate-100 text-sm"
                                        title="Numbered list"
                                    >
                                        1. List
                                    </button>
                                    <div className="w-px bg-slate-300"></div>
                                    <button
                                        type="button"
                                        onClick={insertSeparator}
                                        className="px-3 py-1.5 bg-white border border-slate-300 rounded hover:bg-slate-100 text-sm"
                                        title="Insert separator line"
                                    >
                                        ═══
                                    </button>
                                    <div className="ml-auto text-xs text-slate-500 flex items-center">
                                        💡 Tip: Select text then click formatting button
                                    </div>
                                </div>

                                <textarea
                                    ref={editorRef}
                                    value={minutes}
                                    onChange={handleMinutesChange}
                                    className="w-full min-h-[600px] p-4 border border-slate-300 rounded-lg font-mono text-sm focus:ring-2 focus:ring-indigo-500 outline-none resize-y"
                                    placeholder="Enter meeting minutes here...

Example format:

COMMITTEE MEETING MINUTES
=========================

Meeting: [Meeting Title]
Date: [Date]
Time: [Time]
Venue: [Venue]

ATTENDEES
=========
- [Name], [Role]
- [Name], [Role]

AGENDA
======

1. Opening Remarks
   [Content]

2. Loan Applications Review
   [Details]

3. Voting Results
   [Results]

4. Any Other Business
   [Content]

5. Closing Remarks
   [Content]

Meeting adjourned at [Time]
"
                                />
                            </div>
                        ) : (
                            <div className="prose max-w-none">
                                {minutes ? (
                                    <pre className="whitespace-pre-wrap font-sans text-slate-800 leading-relaxed print:text-black">
                                        {minutes}
                                    </pre>
                                ) : (
                                    <div className="text-center py-12">
                                        <FileText className="mx-auto h-16 w-16 text-slate-300 mb-4" />
                                        <p className="text-slate-500 text-lg font-semibold">No minutes available</p>
                                        <p className="text-slate-400 text-sm mt-2">
                                            Click "Edit Minutes" to start writing
                                        </p>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </div>

                {/* Meeting Info Card (for reference while editing) */}
                {isEditing && meetingDetails.agendaItems && meetingDetails.agendaItems.length > 0 && (
                    <div className="mt-6 bg-blue-50 rounded-lg shadow p-6 print:hidden">
                        <h3 className="text-lg font-bold text-slate-800 mb-4">Quick Reference - Agenda Items</h3>
                        <div className="space-y-2">
                            {meetingDetails.agendaItems.map((item, index) => (
                                <div key={item.id} className="flex items-start gap-3 text-sm">
                                    <span className="font-semibold text-slate-600">{index + 1}.</span>
                                    <div className="flex-1">
                                        <p className="font-semibold text-slate-800">{item.loanNumber} - {item.memberName}</p>
                                        <p className="text-slate-600">{item.productName} - KES {Number(item.amount).toLocaleString()}</p>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </main>

            {/* Print Styles */}
            <style>{`
                @media print {
                    body { background: white; }
                    .print\\:hidden { display: none !important; }
                    .print\\:shadow-none { box-shadow: none !important; }
                    .print\\:border-b-2 { border-bottom-width: 2px !important; }
                    .print\\:border-black { border-color: black !important; }
                    .print\\:text-black { color: black !important; }
                    pre { page-break-inside: avoid; }
                }
            `}</style>
        </div>
    );
}

