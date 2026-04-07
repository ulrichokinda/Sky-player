import React, { useState, useEffect } from 'react';
import { Tv, Copy, ArrowRight, ShieldAlert, Gift, Wifi, WifiOff, RotateCcw, Loader2 } from 'lucide-react';
import { Card, Badge, cn } from './ui';
import { Logo } from './Logo';
import { motion } from 'motion/react';
import { Player } from './Player';

interface SimpleUserViewProps {
  channels: any[];
  onNotify: (msg: string, type: 'success' | 'error' | 'info') => void;
}

export const SimpleUserView: React.FC<SimpleUserViewProps> = ({ onNotify }) => {
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [lowDataMode, setLowDataMode] = useState(false);
  const [macAddress, setMacAddress] = useState('00:00:00:00:00:00');
  const [playlistUrl, setPlaylistUrl] = useState<string | null>(null);
  const [isChecking, setIsChecking] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Generate or retrieve a persistent device ID formatted as MAC
    let deviceId = localStorage.getItem('sky_player_device_id');
    if (!deviceId) {
      const chars = '0123456789ABCDEF';
      deviceId = Array.from({ length: 6 }, () => 
        chars[Math.floor(Math.random() * 16)] + chars[Math.floor(Math.random() * 16)]
      ).join(':');
      localStorage.setItem('sky_player_device_id', deviceId);
    }
    setMacAddress(deviceId);

    const checkActivation = async () => {
      try {
        const response = await fetch(`/api/check-mac/${deviceId}`);
        const data = await response.json();
        if (data.active && data.playlist_url) {
          setPlaylistUrl(data.playlist_url);
          setError(null);
        } else if (data.error) {
          setError(data.error);
        }
      } catch (err) {
        console.error('Erreur lors de la vérification de l\'activation:', err);
      } finally {
        setIsChecking(false);
      }
    };

    checkActivation();
    const interval = setInterval(checkActivation, 10000); // Poll every 10 seconds

    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    return () => {
      clearInterval(interval);
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  if (playlistUrl) {
    return <Player url={playlistUrl} onBack={() => setPlaylistUrl(null)} />;
  }

  return (
    <div className="min-h-screen bg-black text-white p-4 md:p-8 flex flex-col overflow-hidden tv-container">
      <div className="w-full max-w-7xl mx-auto flex flex-col gap-6 md:gap-8 flex-1">
        
        {/* Header/Logo Section */}
        <div className="flex flex-col items-center justify-center gap-4 md:gap-6 py-4 relative">
          <div className="absolute top-0 right-0 flex items-center gap-2">
            <button 
              onClick={() => setLowDataMode(!lowDataMode)}
              className={cn(
                "px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-widest border transition-all",
                lowDataMode ? "bg-primary/20 border-primary text-primary" : "bg-zinc-900 border-zinc-800 text-zinc-500"
              )}
            >
              Mode Éco : {lowDataMode ? 'ON' : 'OFF'}
            </button>
            <Badge variant={isOnline ? 'success' : 'error'} className="gap-2">
              {isOnline ? <Wifi size={10} /> : <WifiOff size={10} />}
              {isOnline ? 'Connecté' : 'Hors-ligne'}
            </Badge>
          </div>
          <Logo size={80} />
          
          <div className="flex flex-col items-center gap-2 bg-zinc-900/50 px-6 py-4 rounded-2xl border border-zinc-800 w-full max-w-sm relative group">
            <button 
              onClick={() => window.location.reload()} 
              className="absolute -top-2 -right-2 p-2 bg-zinc-800 hover:bg-primary text-zinc-500 hover:text-black rounded-full border border-zinc-700 transition-all opacity-0 group-hover:opacity-100"
              title="Rafraîchir"
            >
              <RotateCcw size={12} />
            </button>
            <span className="text-zinc-500 text-[10px] font-black uppercase tracking-widest">Adresse MAC</span>
            <div className="flex items-center gap-3">
              <span className="text-lg md:text-xl font-mono font-bold text-white tracking-wider">{macAddress}</span>
              <button 
                onClick={() => { navigator.clipboard.writeText(macAddress); onNotify('MAC copiée !', 'success'); }} 
                className="text-primary hover:text-white p-1.5 hover:bg-zinc-800 rounded-full transition-colors"
              >
                <Copy size={16} />
              </button>
            </div>
          </div>
        </div>

        {/* Info Section */}
        <div className="flex flex-col gap-4 max-w-2xl mx-auto w-full">
          {isChecking ? (
            <div className="flex flex-col items-center gap-4 py-8">
              <Loader2 className="w-8 h-8 text-primary animate-spin" />
              <p className="text-sm text-zinc-500 font-medium">Vérification de l'activation en cours...</p>
            </div>
          ) : (
            <>
              <motion.div 
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="bg-primary/10 border border-primary/20 p-4 rounded-2xl flex items-center gap-4"
              >
                <div className="w-10 h-10 bg-primary/20 rounded-full flex items-center justify-center text-primary shrink-0">
                  <Gift size={20} />
                </div>
                <div>
                  <h4 className="font-bold text-sm text-primary">Essai Gratuit de 14 Jours</h4>
                  <p className="text-xs text-zinc-400">Profitez de toutes les fonctionnalités premium gratuitement pendant 14 jours après l'installation.</p>
                </div>
              </motion.div>

              <motion.div 
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1 }}
                className="bg-zinc-900/50 border border-zinc-800 p-4 rounded-2xl flex items-center gap-4"
              >
                <div className="w-10 h-10 bg-zinc-800 rounded-full flex items-center justify-center text-zinc-400 shrink-0">
                  <ShieldAlert size={20} />
                </div>
                <div>
                  <h4 className="font-bold text-sm text-white">Lecteur Multimédia Uniquement</h4>
                  <p className="text-xs text-zinc-500 leading-relaxed">
                    Sky Player ne fournit aucun contenu. Vous devez ajouter votre propre liste de lecture M3U ou vos codes Xtream pour regarder vos chaînes.
                  </p>
                </div>
              </motion.div>
            </>
          )}
        </div>

        {/* Footer Section */}
        <div className="mt-auto pt-4 border-t border-zinc-900 flex justify-center">
          <a 
            href="https://ais-dev-lfwiazz5uklpv2b4uunzg7-511075437969.europe-west2.run.app/dashboard"
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-2 bg-primary hover:bg-primary-dark px-8 py-3.5 rounded-full text-[11px] font-black uppercase tracking-widest transition-all tv-focus"
          >
            Devenir Revendeur <ArrowRight size={14} />
          </a>
        </div>
      </div>
    </div>
  );
};
