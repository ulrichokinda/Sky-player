import React, { useState, useEffect } from 'react';
import { Tv, Copy, ArrowRight, ShieldAlert, Gift, Wifi, WifiOff, RotateCcw, Loader2, Star, Activity, Check, X, Edit2 } from 'lucide-react';
import { Card, Badge, Button, cn } from './ui';
import { Logo } from './Logo';
import { motion } from 'motion/react';
import { Player } from './Player';
import { fetchAndParsePlaylist, Channel } from '../lib/playlistParser';
import { runSpeedTest } from '../lib/speedTest';
import { api } from '../services/api';
import { useBranding } from './BrandingProvider';

interface SimpleUserViewProps {
  channels: any[];
  onNotify: (msg: string, type: 'success' | 'error' | 'info') => void;
}

export const SimpleUserView: React.FC<SimpleUserViewProps> = ({ onNotify }) => {
  const { branding } = useBranding();
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [lowDataMode, setLowDataMode] = useState(false);
  const [macAddress, setMacAddress] = useState('00:00:00:00:00:00');
  const [playlistUrl, setPlaylistUrl] = useState<string | null>(null);
  const [channels, setChannels] = useState<Channel[]>([]);
  const [currentChannelIndex, setCurrentChannelIndex] = useState(0);
  const [isChecking, setIsChecking] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [speed, setSpeed] = useState<number | null>(null);
  const [isTestingSpeed, setIsTestingSpeed] = useState(false);

  useEffect(() => {
    // Force landscape mode if possible
    try {
      const orientation = window.screen.orientation as any;
      if (orientation && orientation.lock) {
        orientation.lock('landscape').catch(() => {
          console.log('Orientation lock not supported on this device/browser');
        });
      }
    } catch (e) {
      console.error('Orientation lock error:', e);
    }
  }, []);

  const [isEditingMac, setIsEditingMac] = useState(false);
  const [newMacInput, setNewMacInput] = useState('');

  useEffect(() => {
    let deviceId = localStorage.getItem('sky_player_device_id');
    if (!deviceId) {
      const chars = '0123456789ABCDEF';
      // Use a common virtual prefix like 00:1A:79 (MAG/IPTV standard) or a custom one
      const prefix = '00:E4:55'; // SkyPlayer Custom Prefix
      const suffix = Array.from({ length: 3 }, () => 
        chars[Math.floor(Math.random() * 16)] + chars[Math.floor(Math.random() * 16)]
      ).join(':');
      deviceId = `${prefix}:${suffix}`;
      localStorage.setItem('sky_player_device_id', deviceId);
    }
    setMacAddress(deviceId);
    setNewMacInput(deviceId);

    const checkActivation = async () => {
      try {
        const data = await api.checkMacStatus(deviceId);
        if (data.active && data.activation?.playlist_url) {
          if (data.activation.playlist_url !== playlistUrl) {
            setPlaylistUrl(data.activation.playlist_url);
            const parsedChannels = await fetchAndParsePlaylist(data.activation.playlist_url);
            setChannels(parsedChannels);
            setCurrentChannelIndex(0);
          }
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
    const interval = setInterval(checkActivation, 10000);

    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    return () => {
      clearInterval(interval);
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, [playlistUrl]);

  const handleSaveMac = () => {
    const normalized = newMacInput.toUpperCase().trim();
    if (normalized.length < 12) {
      onNotify('MAC invalide', 'error');
      return;
    }
    localStorage.setItem('sky_player_device_id', normalized);
    setMacAddress(normalized);
    setIsEditingMac(false);
    onNotify('MAC mise à jour !', 'success');
    window.location.reload();
  };

  const startSpeedTest = async () => {
    setIsTestingSpeed(true);
    await runSpeedTest((s) => setSpeed(s));
    setIsTestingSpeed(false);
  };

  if (channels.length > 0) {
    const currentChannel = channels[currentChannelIndex];
    return (
      <Player 
        url={currentChannel.url} 
        onBack={() => setChannels([])} 
        channelName={currentChannel.name}
        channels={channels}
        onChannelSelect={(index) => setCurrentChannelIndex(index)}
        onNext={() => setCurrentChannelIndex((prev) => (prev + 1) % channels.length)}
        onPrev={() => setCurrentChannelIndex((prev) => (prev - 1 + channels.length) % channels.length)}
        macAddress={macAddress}
      />
    );
  }

  return (
    <div className="min-h-screen bg-black text-white flex flex-col overflow-hidden tv-container w-screen h-screen">
      {/* Top Marquee */}
      <div className="w-full bg-primary/20 border-b border-primary/30 py-1.5 overflow-hidden whitespace-nowrap sticky top-0 z-50">
        <div className="inline-block animate-marquee">
          <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary px-8">
            🎁 ESSAI GRATUIT DE 14 JOURS DISPONIBLE APRÈS INSTALLATION — PROFITEZ DE TOUTES LES FONCTIONNALITÉS PREMIUM SANS FRAIS — SKY PLAYER PRO : LA RÉFÉRENCE DU STREAMING 🎁
          </span>
          <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary px-8">
            🎁 ESSAI GRATUIT DE 14 JOURS DISPONIBLE APRÈS INSTALLATION — PROFITEZ DE TOUTES LES FONCTIONNALITÉS PREMIUM SANS FRAIS — SKY PLAYER PRO : LA RÉFÉRENCE DU STREAMING 🎁
          </span>
        </div>
      </div>

      <div className="w-full max-w-7xl mx-auto flex flex-col gap-4 md:gap-6 flex-1 p-4 md:p-8">
        
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
          {branding?.logoUrl ? (
            <img 
              src={branding.logoUrl} 
              alt={branding.appName || 'Logo'} 
              className="h-20 max-w-full object-contain mb-4" 
              referrerPolicy="no-referrer" 
            />
          ) : (
            <Logo size={80} />
          )}
          
          <div className="flex flex-col items-center gap-2 bg-zinc-900/50 px-6 py-4 rounded-2xl border border-zinc-800 w-full max-w-sm relative group">
            <button 
              onClick={() => window.location.reload()} 
              className="absolute -top-2 -right-2 p-2 bg-zinc-800 hover:bg-primary text-zinc-500 hover:text-black rounded-full border border-zinc-700 transition-all opacity-0 group-hover:opacity-100"
              title="Rafraîchir"
            >
              <RotateCcw size={12} />
            </button>
            <span className="text-zinc-500 text-[10px] font-black uppercase tracking-widest">Adresse MAC Virtuelle</span>
            <div className="flex items-center gap-3">
              {isEditingMac ? (
                <div className="flex items-center gap-2">
                  <input 
                    type="text"
                    value={newMacInput}
                    onChange={(e) => setNewMacInput(e.target.value)}
                    className="bg-black border border-primary/50 text-primary font-mono px-3 py-1 rounded text-center focus:outline-none focus:border-primary"
                    autoFocus
                  />
                  <button 
                    onClick={handleSaveMac}
                    className="p-1.5 bg-primary text-black rounded-full hover:bg-primary-dark transition-colors"
                  >
                    <Check size={16} />
                  </button>
                  <button 
                    onClick={() => { setIsEditingMac(false); setNewMacInput(macAddress); }}
                    className="p-1.5 bg-zinc-800 text-zinc-400 rounded-full hover:bg-zinc-700 transition-colors"
                  >
                    <X size={16} />
                  </button>
                </div>
              ) : (
                <>
                  <span className="text-lg md:text-xl font-mono font-bold text-white tracking-wider">{macAddress}</span>
                  <div className="flex items-center gap-1">
                    <button 
                      onClick={() => { navigator.clipboard.writeText(macAddress); onNotify('MAC copiée !', 'success'); }} 
                      className="text-zinc-500 hover:text-primary p-1.5 hover:bg-zinc-800 rounded-full transition-colors"
                      title="Copier"
                    >
                      <Copy size={16} />
                    </button>
                    <button 
                      onClick={() => setIsEditingMac(true)} 
                      className="text-zinc-500 hover:text-primary p-1.5 hover:bg-zinc-800 rounded-full transition-colors"
                      title="Modifier"
                    >
                      <Edit2 size={16} />
                    </button>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>

        <div className="flex flex-col gap-4 max-w-2xl mx-auto w-full">
          {isChecking ? (
            <div className="flex flex-col items-center gap-4 py-8">
              <Loader2 className="w-8 h-8 text-primary animate-spin" />
              <p className="text-sm text-zinc-500 font-medium">Vérification de l'activation en cours...</p>
            </div>
          ) : error ? (
            <div className="flex flex-col items-center gap-6 py-8 text-center">
              <div className="w-16 h-16 bg-red-500/10 rounded-full flex items-center justify-center text-red-500">
                <ShieldAlert size={32} />
              </div>
              <div className="space-y-2">
                <h3 className="text-xl font-bold text-white">Appareil non activé</h3>
                <p className="text-sm text-zinc-400 max-w-md">
                  Votre adresse MAC <span className="text-primary font-mono">{macAddress}</span> n'est pas encore enregistrée dans notre système.
                </p>
              </div>
              <div className="bg-zinc-900/50 border border-zinc-800 p-4 rounded-2xl text-left w-full">
                <p className="text-xs text-zinc-500 font-bold uppercase tracking-widest mb-2">Comment activer ?</p>
                <ol className="text-xs text-zinc-400 space-y-2 list-decimal list-inside">
                  <li>Notez votre adresse MAC ci-dessus</li>
                  <li>Connectez-vous à votre <a href="/dashboard" className="text-primary hover:underline">Panel Revendeur</a></li>
                  <li>Allez dans la section "Activations"</li>
                  <li>Entrez votre MAC et validez</li>
                </ol>
              </div>
              
              <div className="flex flex-col gap-2 w-full">
                <Button 
                  onClick={async () => {
                    try {
                      setIsChecking(true);
                      await api.activateTrial(macAddress);
                      onNotify('Essai gratuit activé !', 'success');
                      window.location.reload();
                    } catch (err) {
                      onNotify('Erreur lors de l\'activation de l\'essai', 'error');
                    } finally {
                      setIsChecking(false);
                    }
                  }} 
                  variant="primary" 
                  fullWidth
                  icon={Gift}
                >
                  Activer l'essai gratuit (14 jours)
                </Button>
                <Button onClick={() => window.location.reload()} variant="outline" size="sm" icon={RotateCcw} fullWidth>
                  Réessayer la vérification
                </Button>
              </div>
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
                    Sky Player ne fournit aucun contenu. Vous devez ajouter votre propre liste de lecture M3U, JSON ou vos codes Xtream pour regarder vos chaînes.
                  </p>
                </div>
              </motion.div>

              <div className="grid grid-cols-2 gap-4 mt-2">
                <button 
                  onClick={startSpeedTest}
                  className="bg-zinc-900/50 border border-zinc-800 p-4 rounded-2xl flex flex-col items-center gap-2 hover:bg-primary/10 hover:border-primary/30 transition-all group"
                >
                  <div className={`p-3 rounded-xl ${isTestingSpeed ? 'bg-primary text-black animate-spin' : 'bg-zinc-800 text-zinc-400 group-hover:text-primary'}`}>
                    <Activity size={20} />
                  </div>
                  <span className="text-[10px] font-black uppercase tracking-widest">Test de Débit</span>
                  {speed !== null && <span className="text-primary font-bold">{speed.toFixed(1)} Mbps</span>}
                </button>

                <button 
                  onClick={() => {
                    const favs = JSON.parse(localStorage.getItem('sky_player_favorites') || '[]');
                    onNotify(`Vous avez ${favs.length} favoris`, 'info');
                  }}
                  className="bg-zinc-900/50 border border-zinc-800 p-4 rounded-2xl flex flex-col items-center gap-2 hover:bg-yellow-500/10 hover:border-yellow-500/30 transition-all group"
                >
                  <div className="p-3 bg-zinc-800 text-zinc-400 group-hover:text-yellow-500 rounded-xl">
                    <Star size={20} />
                  </div>
                  <span className="text-[10px] font-black uppercase tracking-widest">Mes Favoris</span>
                </button>
              </div>
            </>
          )}
        </div>

        <div className="mt-auto pt-4 border-t border-zinc-900 flex flex-col items-center gap-4">
          <a 
            href="/dashboard"
            className="flex items-center gap-2 bg-primary hover:bg-primary-dark px-8 py-3.5 rounded-full text-[11px] font-black uppercase tracking-widest transition-all tv-focus"
          >
            Devenir Revendeur <ArrowRight size={14} />
          </a>
          
          {/* Bottom Warning */}
          <div className="w-full text-center pb-2">
            <p className="text-[9px] text-zinc-600 font-medium uppercase tracking-widest max-w-2xl mx-auto leading-relaxed">
              ⚠️ Sky Player est un lecteur multimédia neutre. Nous ne fournissons aucun contenu, lien ou flux. Vous êtes responsable du contenu que vous ajoutez.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
