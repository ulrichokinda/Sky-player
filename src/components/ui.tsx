import React from 'react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { RotateCcw, ChevronRight, CheckCircle2, AlertCircle, Info, X } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export const Card = ({ children, className, variant = 'default', ...props }: { children: React.ReactNode; className?: string; variant?: 'default' | 'glass' | 'outline' } & React.HTMLAttributes<HTMLDivElement>) => (
  <div 
    className={cn(
      "rounded-3xl p-6 transition-all duration-300",
      variant === 'default' && "bg-zinc-900/40 border border-zinc-800/50 backdrop-blur-md",
      variant === 'glass' && "glass-panel",
      variant === 'outline' && "border border-zinc-800",
      className
    )}
    {...props}
  >
    {children}
  </div>
);

export const Badge = ({ children, className, variant = 'default', ...props }: { children: React.ReactNode; className?: string; variant?: 'default' | 'primary' | 'success' | 'warning' | 'error' | 'info' } & React.HTMLAttributes<HTMLDivElement>) => (
  <div 
    className={cn(
      "inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-widest border",
      variant === 'default' && "bg-zinc-800 border-zinc-700 text-zinc-400",
      variant === 'primary' && "bg-primary/10 border-primary/20 text-primary",
      variant === 'success' && "bg-emerald-500/10 border-emerald-500/20 text-emerald-500",
      variant === 'warning' && "bg-amber-500/10 border-amber-500/20 text-amber-500",
      variant === 'error' && "bg-red-500/10 border-red-500/20 text-red-500",
      variant === 'info' && "bg-blue-500/10 border-blue-500/20 text-blue-500",
      className
    )}
    {...props}
  >
    {children}
  </div>
);

export const Button = ({ 
  children, 
  onClick, 
  className, 
  variant = 'primary', 
  size = 'md', 
  disabled, 
  loading, 
  icon: Icon,
  type = 'button',
  fullWidth,
  ...props 
}: { 
  children: React.ReactNode; 
  onClick?: () => void; 
  className?: string; 
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger' | 'white';
  size?: 'sm' | 'md' | 'lg';
  disabled?: boolean;
  loading?: boolean;
  icon?: any;
  type?: 'button' | 'submit' | 'reset';
  fullWidth?: boolean;
} & React.ButtonHTMLAttributes<HTMLButtonElement>) => (
  <button
    type={type}
    onClick={onClick}
    disabled={disabled || loading}
    className={cn(
      "relative inline-flex items-center justify-center gap-2 font-black uppercase tracking-widest transition-all active:scale-95 disabled:opacity-50 disabled:pointer-events-none focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-primary focus-visible:scale-105",
      fullWidth && "w-full",
      size === 'sm' && "px-4 py-2 text-[10px] rounded-xl",
      size === 'md' && "px-6 py-3.5 text-xs rounded-2xl",
      size === 'lg' && "px-8 py-5 text-sm rounded-[1.5rem]",
      variant === 'primary' && "bg-primary text-black hover:bg-primary/90 shadow-lg shadow-primary/20",
      variant === 'secondary' && "bg-zinc-800 text-white hover:bg-zinc-700",
      variant === 'outline' && "bg-transparent border border-zinc-800 text-zinc-400 hover:border-zinc-600 hover:text-white",
      variant === 'ghost' && "bg-transparent text-zinc-500 hover:bg-white/5 hover:text-white",
      variant === 'danger' && "bg-red-500/10 border border-red-500/20 text-red-500 hover:bg-red-500 hover:text-white",
      variant === 'white' && "bg-white text-black hover:bg-zinc-200",
      className
    )}
    {...props}
  >
    {loading ? <RotateCcw className="animate-spin" size={size === 'sm' ? 14 : 18} /> : Icon && <Icon size={size === 'sm' ? 14 : 18} />}
    {children}
  </button>
);

export const Input = ({ label, error, rightElement, ...props }: any) => (
  <div className="space-y-2 w-full">
    {label && <label className="text-[10px] font-black text-zinc-500 uppercase tracking-widest ml-2">{label}</label>}
    <div className="relative flex items-center">
      <input
        {...props}
        className={cn(
          "w-full bg-zinc-950 border border-zinc-800 rounded-2xl px-5 py-4 text-sm text-white placeholder:text-zinc-700 focus:outline-none focus:border-primary/50 focus:ring-4 focus:ring-primary/20 transition-all focus-visible:ring-primary focus-visible:border-primary",
          error && "border-red-500/50 focus:border-red-500",
          rightElement && "pr-12",
          props.className
        )}
      />
      {rightElement && (
        <div className="absolute right-2">
          {rightElement}
        </div>
      )}
    </div>
    {error && <p className="text-[10px] text-red-500 font-bold ml-2">{error}</p>}
  </div>
);

export const Select = ({ label, error, children, ...props }: any) => (
  <div className="space-y-2 w-full">
    {label && <label className="text-[10px] font-black text-zinc-500 uppercase tracking-widest ml-2">{label}</label>}
    <div className="relative">
      <select
        {...props}
        className={cn(
          "w-full bg-zinc-950 border border-zinc-800 rounded-2xl px-5 py-4 text-sm text-white focus:outline-none focus:border-primary/50 focus:ring-4 focus:ring-primary/20 transition-all appearance-none cursor-pointer focus-visible:ring-primary focus-visible:border-primary",
          error && "border-red-500/50 focus:border-red-500",
          props.className
        )}
      >
        {children}
      </select>
      <div className="absolute right-5 top-1/2 -translate-y-1/2 pointer-events-none text-zinc-500">
        <ChevronRight size={18} className="rotate-90" />
      </div>
    </div>
    {error && <p className="text-[10px] text-red-500 font-bold ml-2">{error}</p>}
  </div>
);

export const Textarea = ({ label, error, ...props }: any) => (
  <div className="space-y-2 w-full">
    {label && <label className="text-[10px] font-black text-zinc-500 uppercase tracking-widest ml-2">{label}</label>}
    <textarea
      {...props}
      className={cn(
        "w-full bg-zinc-950 border border-zinc-800 rounded-2xl px-5 py-4 text-sm text-white placeholder:text-zinc-700 focus:outline-none focus:border-primary/50 focus:ring-1 focus:ring-primary/20 transition-all min-h-[100px] resize-none",
        error && "border-red-500/50 focus:border-red-500",
        props.className
      )}
    />
    {error && <p className="text-[10px] text-red-500 font-bold ml-2">{error}</p>}
  </div>
);

export const Toast = ({ message, type = 'info', onClose }: { message: string; type?: 'success' | 'error' | 'info'; onClose: () => void }) => {
  React.useEffect(() => {
    const timer = setTimeout(onClose, 5000);
    return () => clearTimeout(timer);
  }, [onClose]);

  return (
    <motion.div
      initial={{ opacity: 0, y: 50, scale: 0.9 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      exit={{ opacity: 0, y: 20, scale: 0.9 }}
      className={cn(
        "fixed bottom-8 right-8 z-[200] flex items-center gap-3 px-6 py-4 rounded-2xl shadow-2xl border backdrop-blur-xl",
        type === 'success' && "bg-emerald-500/10 border-emerald-500/20 text-emerald-500",
        type === 'error' && "bg-red-500/10 border-red-500/20 text-red-500",
        type === 'info' && "bg-blue-500/10 border-blue-500/20 text-blue-500"
      )}
    >
      {type === 'success' && <CheckCircle2 size={20} />}
      {type === 'error' && <AlertCircle size={20} />}
      {type === 'info' && <Info size={20} />}
      <p className="text-sm font-bold">{message}</p>
      <button onClick={onClose} className="ml-4 p-1 hover:bg-white/10 rounded-lg transition-colors">
        <X size={16} />
      </button>
    </motion.div>
  );
};
