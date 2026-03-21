import React from 'react';
import logoImage from './designarena_image_m7.PNG';

interface LogoProps {
  className?: string;
  size?: number;
  showText?: boolean;
}

export const Logo: React.FC<LogoProps> = ({ className = "", size = 40, showText = true }) => {
  return (
    <div className={`flex items-center gap-3 ${className}`}>
      <img 
        src={logoImage} 
        alt="Logo" 
        style={{ width: size, height: size }} 
        className="object-contain"
        referrerPolicy="no-referrer"
      />
      {showText && (
        <div className="flex flex-col leading-none">
          <span className="text-2xl lg:text-3xl font-black text-white tracking-tighter">
            WO <span className="text-primary">PLAYER</span>
          </span>
          <span className="text-[10px] lg:text-xs font-black text-zinc-500 uppercase tracking-[0.3em] mt-1">
            Premium Streaming
          </span>
        </div>
      )}
    </div>
  );
};
