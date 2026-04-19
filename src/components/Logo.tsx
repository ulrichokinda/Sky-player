import React from 'react';

interface LogoProps {
  className?: string;
  size?: number;
  showText?: boolean;
}

export const Logo: React.FC<LogoProps> = ({ className = "", size = 40, showText = true }) => {
  return (
    <div className={`flex items-center gap-3 ${className}`}>
      <svg 
        width={size} 
        height={size} 
        viewBox="0 0 100 100" 
        fill="none" 
        xmlns="http://www.w3.org/2000/svg"
        className="shrink-0"
      >
        {/* Background Circle with Gradient */}
        <defs>
          <linearGradient id="logoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#00AEEF" />
            <stop offset="100%" stopColor="#0EA5E9" />
          </linearGradient>
        </defs>
        
        <rect width="100" height="100" rx="24" fill="url(#logoGradient)" />
        
        {/* Stylized Play Button / S Shape */}
        <path 
          d="M35 30L70 50L35 70V30Z" 
          fill="white" 
          stroke="white" 
          strokeWidth="4" 
          strokeLinejoin="round"
        />
        
        {/* Sky Accent (Wing/Cloud line) */}
        <path 
          d="M25 75C40 75 50 65 75 65" 
          stroke="white" 
          strokeWidth="6" 
          strokeLinecap="round" 
          opacity="0.3"
        />
      </svg>
      
      {showText && (
        <div className="flex flex-col leading-none">
          <span className="text-2xl lg:text-3xl font-black text-white tracking-tighter">
            SKY <span className="text-primary">PLAYER</span>
          </span>
          <span className="text-[10px] lg:text-xs font-black text-zinc-500 uppercase tracking-[0.3em] mt-1">
            Premium Streaming
          </span>
        </div>
      )}
    </div>
  );
};
