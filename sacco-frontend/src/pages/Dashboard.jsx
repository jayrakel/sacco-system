import React from 'react';
import { Wallet, TrendingUp } from 'lucide-react';

const Sample3DCard = () => {
  return (
    <div className="p-10 bg-slate-50 min-h-[300px] flex items-center justify-center gap-8">

      {/* CARD START */}
      <div className="group relative w-72 transition-all duration-300 ease-out hover:-translate-y-2">

        {/* 1. The '3D' Shadow Layer (Creates the physical depth) */}
        <div className="absolute inset-0 rounded-3xl bg-indigo-900/20 translate-x-4 translate-y-4 blur-sm transition-all duration-300 group-hover:translate-x-6 group-hover:translate-y-6 group-hover:blur-md"></div>

        {/* 2. The Main Surface (Glass/Ceramic feel) */}
        <div className="relative h-full bg-white rounded-3xl p-6 border border-white/50 shadow-inner overflow-hidden">

          {/* Decorative Gradient Glow (Subtle lighting) */}
          <div className="absolute -top-12 -right-12 w-32 h-32 bg-gradient-to-br from-indigo-400 to-purple-500 rounded-full blur-3xl opacity-20"></div>

          {/* Icon Container (Floating 3D Element) */}
          <div className="flex justify-between items-start mb-6">
            <div className="relative">
                {/* Icon Shadow */}
                <div className="absolute inset-0 bg-indigo-500 blur-lg opacity-40 rounded-xl translate-y-2"></div>
                {/* Icon Body */}
                <div className="relative h-14 w-14 flex items-center justify-center bg-gradient-to-br from-indigo-500 to-purple-600 rounded-2xl text-white shadow-lg border-t border-white/30 transform transition-transform group-hover:rotate-6">
                    <Wallet size={28} strokeWidth={2.5} />
                </div>
            </div>

            {/* Badge */}
            <span className="flex items-center gap-1 bg-emerald-50 text-emerald-700 px-3 py-1.5 rounded-xl text-xs font-bold border border-emerald-100 shadow-sm">
                <TrendingUp size={14} /> +12%
            </span>
          </div>

          {/* Content Typography */}
          <div className="relative z-10">
            <h4 className="text-slate-400 font-bold text-xs uppercase tracking-wider mb-1">Total Savings</h4>
            <div className="flex items-baseline gap-1">
                <span className="text-3xl font-black text-slate-800 tracking-tight">KES 4.2M</span>
            </div>
            <p className="text-xs text-slate-400 mt-2 font-medium">Updated 2 mins ago</p>
          </div>

          {/* Bottom Reflection (Glossy effect) */}
          <div className="absolute bottom-0 left-0 right-0 h-1/3 bg-gradient-to-t from-slate-50/50 to-transparent pointer-events-none"></div>
        </div>
      </div>
      {/* CARD END */}

    </div>
  );
};

export default Sample3DCard;