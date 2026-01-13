import React, { useState, useEffect } from 'react';
import api from '../api';
import { TrendingUp, PieChart, Coins, Users } from 'lucide-react';

export default function ShareCapitalCard({ memberId = null, showOwnershipPercentage = false, forCurrentUser = false }) {
    const [shareData, setShareData] = useState(null);
    const [totalShareCapital, setTotalShareCapital] = useState(null);
    const [currentMemberId, setCurrentMemberId] = useState(memberId);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchShareData = async () => {
            try {
                setLoading(true);

                // If forCurrentUser is true, fetch current member's ID first
                if (forCurrentUser && !memberId) {
                    try {
                        const memberRes = await api.get('/members/me');
                        if (memberRes.data.success && memberRes.data.data?.id) {
                            setCurrentMemberId(memberRes.data.data.id);
                            // Fetch share data with the retrieved member ID
                            const res = await api.get(`/shares/member/${memberRes.data.data.id}`);
                            if (res.data.success) {
                                setShareData(res.data.data);
                            }
                        }
                    } catch (err) {
                        console.error('Failed to fetch current member:', err);
                    }
                } else if (memberId || currentMemberId) {
                    // If memberId is provided, fetch member-specific data
                    const res = await api.get(`/shares/member/${memberId || currentMemberId}`);
                    if (res.data.success) {
                        setShareData(res.data.data);
                    }
                }

                // Always fetch total share capital for admin/executive views
                if (!memberId && !forCurrentUser || showOwnershipPercentage) {
                    const totalRes = await api.get('/api/shares/total');
                    if (totalRes.data.success) {
                        setTotalShareCapital(totalRes.data.data);
                    }
                }

                setLoading(false);
            } catch (error) {
                console.error('Failed to fetch share capital data:', error);
                setLoading(false);
            }
        };

        fetchShareData();
    }, [memberId, showOwnershipPercentage, forCurrentUser]);

    if (loading) {
        return (
            <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-200 animate-pulse">
                <div className="h-24 bg-slate-100 rounded"></div>
            </div>
        );
    }

    // Member View - Show their share capital
    if ((memberId || currentMemberId || forCurrentUser) && shareData) {
        return (
            <div className="bg-gradient-to-br from-amber-50 to-orange-50 p-6 rounded-xl shadow-sm border border-amber-200">
                <div className="flex justify-between items-start mb-4">
                    <div className="flex items-center gap-3">
                        <div className="p-3 bg-amber-500 text-white rounded-xl shadow-lg">
                            <Coins size={24} />
                        </div>
                        <div>
                            <h3 className="font-bold text-slate-800 text-sm">Share Capital</h3>
                            <p className="text-xs text-slate-500">Your Ownership Stake</p>
                        </div>
                    </div>
                </div>

                <div className="grid grid-cols-2 gap-4 mb-4">
                    <div>
                        <p className="text-xs text-slate-600 font-semibold uppercase mb-1">Total Value</p>
                        <p className="text-2xl font-bold text-amber-700">
                            KES {Number(shareData.paidAmount || 0).toLocaleString()}
                        </p>
                    </div>
                    <div>
                        <p className="text-xs text-slate-600 font-semibold uppercase mb-1">Shares Owned</p>
                        <p className="text-2xl font-bold text-slate-800">
                            {Number(shareData.paidShares || 0).toLocaleString()}
                        </p>
                    </div>
                </div>

                {showOwnershipPercentage && (
                    <div className="pt-3 border-t border-amber-200">
                        <div className="flex justify-between items-center">
                            <span className="text-xs text-slate-600 font-semibold flex items-center gap-1">
                                <PieChart size={14} /> Ownership
                            </span>
                            <span className="text-sm font-bold text-amber-600">
                                {Number(shareData.ownershipPercentage || 0).toFixed(2)}%
                            </span>
                        </div>
                    </div>
                )}

                <div className="mt-3 pt-3 border-t border-amber-200">
                    <div className="flex justify-between items-center text-xs">
                        <span className="text-slate-500">Share Value</span>
                        <span className="font-bold text-slate-700">
                            KES {Number(shareData.shareValue || 0).toLocaleString()}
                        </span>
                    </div>
                </div>
            </div>
        );
    }

    // Admin/Executive View - Show total SACCO share capital
    if (!memberId && totalShareCapital) {
        return (
            <div className="bg-gradient-to-br from-amber-50 to-orange-50 p-6 rounded-xl shadow-sm border border-amber-200">
                <div className="flex justify-between items-start mb-4">
                    <div className="flex items-center gap-3">
                        <div className="p-3 bg-amber-500 text-white rounded-xl shadow-lg">
                            <TrendingUp size={24} />
                        </div>
                        <div>
                            <h3 className="font-bold text-slate-800 text-sm">Total Share Capital</h3>
                            <p className="text-xs text-slate-500">SACCO Equity Base</p>
                        </div>
                    </div>
                </div>

                <div className="grid grid-cols-2 gap-4 mb-4">
                    <div>
                        <p className="text-xs text-slate-600 font-semibold uppercase mb-1">Total Value</p>
                        <p className="text-2xl font-bold text-amber-700">
                            KES {Number(totalShareCapital.totalShareCapital || 0).toLocaleString()}
                        </p>
                    </div>
                    <div>
                        <p className="text-xs text-slate-600 font-semibold uppercase mb-1">Total Shares</p>
                        <p className="text-2xl font-bold text-slate-800">
                            {Number(totalShareCapital.totalShares || 0).toLocaleString()}
                        </p>
                    </div>
                </div>

                <div className="mt-3 pt-3 border-t border-amber-200">
                    <div className="flex justify-between items-center text-xs">
                        <span className="text-slate-500">Current Share Value</span>
                        <span className="font-bold text-slate-700">
                            KES {Number(totalShareCapital.shareValue || 0).toLocaleString()}
                        </span>
                    </div>
                </div>
            </div>
        );
    }

    return null;
}
