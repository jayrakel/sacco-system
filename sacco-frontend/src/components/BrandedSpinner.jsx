import React from 'react';
import { ShieldCheck } from 'lucide-react';
import { useSettings } from '../context/SettingsContext';

export default function BrandedSpinner({
    iconUrl,
    size = "large",
    color = "white",
    showTagline = true,
    borderColor // ✅ New Prop: Allows you to pass any Tailwind border color class
}) {
  const { settings, getImageUrl } = useSettings();

  const sizeClasses = {
    small: "w-6 h-6",
    medium: "w-12 h-12",
    large: "w-20 h-20",
    xl: "w-32 h-32"
  };

  const outerSize = sizeClasses[size] || sizeClasses.large;

  // Adjust inner icon size based on container
  const innerSize = size === "small" ? "w-3 h-3"
                  : size === "medium" ? "w-6 h-6"
                  : size === "large" ? "w-10 h-10"
                  : "w-16 h-16";

  // ✅ DYNAMIC COLOR LOGIC:
  // 1. If 'borderColor' prop is present, use it (e.g., "border-emerald-500").
  // 2. Else if color mode is "white", use white.
  // 3. Default to "border-indigo-900" (Brand Color).
  const activeRingColor = borderColor || (color === "white" ? "border-white" : "border-indigo-900");

  const trackColor = color === "white" ? "border-white/20" : "border-slate-200";
  const iconColor = color === "white" ? "text-white" : "text-indigo-900";

  // Icon Resolution
  const systemFavicon = getImageUrl(settings?.SACCO_FAVICON);
  const finalIcon = iconUrl || systemFavicon;

  return (
    <div className="flex flex-col items-center justify-center gap-4">
      {/* Spinner Container */}
      <div className="relative flex items-center justify-center">

        {/* 1. STATIC TRACK (Background Ring) */}
        <div className={`absolute ${outerSize} border-4 ${trackColor} rounded-full`}></div>

        {/* 2. SPINNING ARC (Dynamic Color) */}
        <div className={`absolute ${outerSize} border-4 ${activeRingColor} border-t-transparent rounded-full animate-spin`}></div>

        {/* 3. CENTER ICON (Pulsing) */}
        <div className={`${innerSize} animate-pulse flex items-center justify-center z-10`}>
           {finalIcon ? (
             <img
               src={finalIcon}
               alt="Loading..."
               className="w-full h-full object-contain drop-shadow-sm"
               onError={(e) => { e.target.style.display = 'none'; }}
             />
           ) : (
             <ShieldCheck className={`w-full h-full ${iconColor}`} />
           )}
        </div>
      </div>

      {/* Tagline Display */}
{/*       {showTagline && settings?.SACCO_TAGLINE && ( */}
{/*         <p className={`text-xs font-bold uppercase tracking-widest animate-pulse ${color === 'white' ? 'text-white/80' : 'text-slate-500'}`}> */}
{/*             {settings.SACCO_TAGLINE} */}
{/*         </p> */}
{/*       )} */}
    </div>
  );
}