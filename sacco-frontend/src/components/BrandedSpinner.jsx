import { ShieldCheck } from 'lucide-react';

export default function BrandedSpinner({ iconUrl, size = "large", color = "brand" }) {
  const sizeClasses = {
    small: "w-6 h-6",
    medium: "w-12 h-12",
    large: "w-20 h-20",
    xl: "w-32 h-32"
  };

  const outerSize = sizeClasses[size] || sizeClasses.large;
  const innerSize = size === "small" ? "w-3 h-3" : size === "medium" ? "w-6 h-6" : size === "large" ? "w-10 h-10" : "w-16 h-16";

  // ✅ Use Brand Colors
  const borderColor = color === "white" ? "border-white" : "border-brand-primary";
  const iconColor = color === "white" ? "text-white" : "text-brand-primary";

  return (
    <div className="relative flex items-center justify-center">
      {/* Dynamic Border Color */}
      <div className={`absolute ${outerSize} border-4 border-slate-200/50 ${borderColor} border-t-transparent rounded-full animate-spin`}></div>

      <div className={`${innerSize} animate-pulse flex items-center justify-center`}>
         {iconUrl ? (
           <img src={iconUrl} alt="Loading..." className="w-full h-full object-contain drop-shadow-sm" />
         ) : (
           <ShieldCheck className={`w-full h-full ${iconColor}`} />
         )}
      </div>
    </div>
  );
}