import React, { useState, useEffect } from 'react';
import { Tv, Copy, ArrowRight, ShieldAlert, Gift, Wifi, WifiOff, RotateCcw, Loader2, Star, Activity, Check, X, Edit2 } from 'lucide-react';
import { Card, Badge, Button, cn } from './ui';
import { Logo } from './Logo';
import { motion } from 'motion/react';
import { Player } from './Player';
import { fetchAndParsePlaylist, Channel } from '../lib/playlistParser';
import { runSpeedTest } from '../lib/speedTest';
import { api } from '../services/api';
import { BrandingProvider, useBranding } from './BrandingProvider';
import { Device } from '@capacitor/device';
import { Capacitor } from '@capacitor/core';

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
  const [loadTimeout, setLoadTimeout] = useState<NodeJS.Timeout | null>(null);

  useEffect(() => {
    const initDevice = async () => {
      let devId = localStorage.getItem('sky_player_device_id');
      
      if (Capacitor.isNativePlatform()) {
        try {
          const id = await Device.getId();
          if (id.identifier) {
            // Keep it consistent. If it's already a MAC format in storage, keep it.
            // If it's the first time on native, use the identifier.
            if (!devId || devId.startsWith('00:E4:55')) {
              devId = id.identifier.toUpperCase().replace(/[^A-F0-9]/g, '');
              // Format as MAC if it's 12 chars
              if (devId.length >= 12) {
                devId = devId.substring(0, 12).match(/.{1,2}/g)?.join(':') || devId;
              }
              localStorage.setItem('sky_player_device_id', devId);
            }
          }
        } catch (e) {
          console.error('Device ID error:', e);
        }
      }

      if (!devId) {
        const chars = '0123456789ABCDEF';
        const prefix = '00:E4:55'; 
        const suffix = Array.from({ length: 3 }, () => 
          chars[Math.floor(Math.random() * 16)] + chars[Math.floor(Math.random() * 16)]
        ).join(':');
        devId = `${prefix}:${suffix}`;
        localStorage.setItem('sky_player_device_id', devId);
      }
      setMacAddress(devId);
    };
    initDevice();

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

  const [isPlaylistError, setIsPlaylistError] = useState(false);

  useEffect(() => {
    if (macAddress === '00:00:00:00:00:00') return;

    const checkActivation = async () => {
      try {
        const data = await api.checkMacStatus(macAddress);
        if (data.active) {
          let targetUrl = data.activation?.playlist_url;
          
          if (!targetUrl && data.activation?.xtream_host && data.activation?.xtream_username && data.activation?.xtream_password) {
            const host = data.activation.xtream_host.endsWith('/') ? data.activation.xtream_host.slice(0, -1) : data.activation.xtream_host;
            targetUrl = `${host}/get.php?username=${data.activation.xtream_username}&password=${data.activation.xtream_password}&type=m3u_plus&output=m3u8`;
          }

          if (targetUrl && targetUrl !== playlistUrl) {
            setPlaylistUrl(targetUrl);
            
            // Add safety timeout for loading
            const timeout = setTimeout(() => {
              if (channels.length === 0) {
                setError("Le chargement de la liste de lecture prend trop de temps (30s). L'hôte Xtream est trop lent ou bloque l'accès.");
                setIsPlaylistError(true);
                setIsChecking(false);
              }
            }, 30000);
            
            try {
              const parsedChannels = await fetchAndParsePlaylist(targetUrl);
              clearTimeout(timeout);
              if (parsedChannels.length > 0) {
                setChannels(parsedChannels);
                setCurrentChannelIndex(0);
                setError(null);
                setIsPlaylistError(false);
              } else {
                setError("La liste de lecture est vide, invalide, ou l'accès a été refusé par le serveur cible.");
                setIsPlaylistError(true);
              }
            } catch (err) {
              clearTimeout(timeout);
              setError("Erreur réseau lors du téléchargement de la liste. Le serveur cible bloque peut-être la connexion.");
              setIsPlaylistError(true);
            }
          }
        } else if (data.error) {
          setError(data.error);
          setIsPlaylistError(false);
        }
      } catch (err) {
        console.error('Erreur lors de la vérification de l\'activation:', err);
        setError("Erreur fatale lors de la connexion.");
        setIsPlaylistError(false);
      } finally {
        setIsChecking(false);
      }
    };


    const updateDeviceInfo = async () => {
      try {
        const info = await Device.getInfo();
        let country = 'N/A';
        try {
          const res = await fetch('https://ipapi.co/json/');
          const data = await res.json();
          country = data.country_code || 'N/A';
        } catch (e) {}

        api.sendHeartbeat({
          mac: macAddress,
          system: `${info.platform} ${info.osVersion}`,
          version: `4.0.0-ULTRA`,
          country: country
        });
      } catch (e) {}
    };

    checkActivation();
    updateDeviceInfo();
    
    const interval = setInterval(() => {
      checkActivation();
      updateDeviceInfo();
    }, 30000);

    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    return () => {
      clearInterval(interval);
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, [macAddress, playlistUrl]);

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
    <div className="min-h-screen bg-black text-white flex flex-col overflow-y-auto custom-scrollbar w-full pb-8">
      {/* Top Marquee */}
      <div className="w-full bg-primary/20 border-b border-primary/30 py-1.5 overflow-hidden whitespace-nowrap shrink-0">
        <div className="inline-block animate-marquee shrink-0">
          <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary px-8">
            🎁 ESSAI GRATUIT DE 14 JOURS DISPONIBLE APRÈS INSTALLATION — PROFITEZ DE TOUTES LES FONCTIONNALITÉS PREMIUM SANS FRAIS — SKY PLAYER PRO : LA RÉFÉRENCE DU STREAMING 🎁
          </span>
          <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary px-8">
            🎁 ESSAI GRATUIT DE 14 JOURS DISPONIBLE APRÈS INSTALLATION — PROFITEZ DE TOUTES LES FONCTIONNALITÉS PREMIUM SANS FRAIS — SKY PLAYER PRO : LA RÉFÉRENCE DU STREAMING 🎁
          </span>
        </div>
      </div>

      <div className="w-full max-w-5xl mx-auto flex flex-col gap-6 p-4 md:p-8">
        
        {/* Header Section */}
        <div className="flex flex-col md:flex-row items-center justify-between gap-6 shrink-0 bg-zinc-900/30 p-6 rounded-3xl border border-white/5">
          {branding?.logoUrl ? (
            <img 
              src={branding.logoUrl} 
              alt={branding.appName || 'Logo'} 
              className="h-16 max-w-[200px] object-contain shrink-0" 
              referrerPolicy="no-referrer" 
            />
          ) : (
            <div className="shrink-0 scale-75 md:scale-100 origin-left">
              <Logo size={60} />
            </div>
          )}

          <div className="flex items-center gap-4 shrink-0">
            <button 
              onClick={() => setLowDataMode(!lowDataMode)}
              className={cn(
                "px-4 py-2 rounded-full text-[10px] font-black uppercase tracking-widest border transition-all",
                lowDataMode ? "bg-primary/20 border-primary text-primary" : "bg-zinc-900 border-zinc-800 text-zinc-500 hover:text-white"
              )}
            >
              Mode Éco : {lowDataMode ? 'ON' : 'OFF'}
            </button>
            <Badge variant={isOnline ? 'success' : 'error'} className="gap-2 py-2">
              {isOnline ? <Wifi size={12} /> : <WifiOff size={12} />}
              {isOnline ? 'Connecté' : 'Hors-ligne'}
            </Badge>
          </div>
        </div>

        {/* Mac Address Section */}
        <div className="flex justify-center shrink-0">
          <div className="flex flex-col items-center gap-3 bg-zinc-900/50 px-8 py-6 rounded-2xl border border-zinc-800 w-full max-w-md relative group">
            <div className="absolute -top-3 -right-3 flex gap-2">
              <button 
                onClick={() => setIsEditingMac(!isEditingMac)} 
                className="p-2 bg-zinc-800 hover:bg-zinc-700 text-zinc-400 hover:text-white rounded-full border border-zinc-700 transition-all shadow-lg"
                title="Modifier l'ID"
              >
                <Edit2 size={14} />
              </button>
              <button 
                onClick={() => window.location.reload()} 
                className="p-2 bg-zinc-800 hover:bg-primary text-zinc-400 hover:text-black rounded-full border border-zinc-700 transition-all shadow-lg"
                title="Rafraîchir"
              >
                <RotateCcw size={14} />
              </button>
            </div>
            
            <span className="text-zinc-500 text-xs font-black uppercase tracking-[0.2em]">Votre Adresse MAC</span>
            
            {isEditingMac ? (
              <div className="flex items-center gap-2 w-full">
                <input 
                  type="text"
                  value={newMacInput}
                  onChange={(e) => setNewMacInput(e.target.value.toUpperCase())}
                  placeholder="FF:FF:FF:FF:FF:FF"
                  className="bg-black border border-zinc-700 rounded-lg px-3 py-2 text-sm font-mono w-full focus:border-primary outline-none"
                />
                <button 
                  onClick={() => {
                    if (newMacInput.length >= 1) {
                      setMacAddress(newMacInput);
                      localStorage.setItem('sky_player_device_id', newMacInput);
                      setIsEditingMac(false);
                      window.location.reload();
                    }
                  }}
                  className="p-2 bg-primary text-black rounded-lg hover:bg-primary/80"
                >
                  <Check size={16} />
                </button>
              </div>
            ) : (
              <div className="flex items-center gap-3 w-full justify-center">
                <span className="text-xl md:text-2xl font-mono font-bold text-white tracking-widest">{macAddress}</span>
                <button 
                  onClick={() => { navigator.clipboard.writeText(macAddress); onNotify('MAC copiée !', 'success'); }} 
                  className="text-zinc-500 hover:text-primary p-2 hover:bg-zinc-800 rounded-lg transition-colors bg-zinc-900"
                  title="Copier"
                >
                  <Copy size={16} />
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Content Section */}
        <div className="flex flex-col gap-6 max-w-2xl mx-auto w-full shrink-0">
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
              <div className="space-y-4">
                <h3 className="text-xl font-bold text-white">
                  {isPlaylistError ? "Erreur Serveur Distant" : "Problème d'Activation"}
                </h3>
                {isPlaylistError && <p className="text-emerald-400 text-xs font-bold uppercase tracking-widest bg-emerald-400/10 py-1 px-3 rounded-full mb-2">✅ Appareil bien activé sur votre panel</p>}
                <p className="text-sm text-zinc-400 max-w-md mx-auto">
                  Détails : <span className="text-red-400 font-bold">{error}</span>
                </p>
                <p className="text-xs text-zinc-500 max-w-md mx-auto bg-zinc-900 p-2 rounded">
                  Votre adresse MAC en cours de vérification est : <strong className="text-white">{macAddress}</strong>
                </p>
              </div>
              
              {!isPlaylistError ? (
                <div className="bg-zinc-900/50 border border-zinc-800 p-4 rounded-2xl text-left w-full mt-4">
                  <p className="text-xs text-zinc-500 font-bold uppercase tracking-widest mb-2">Comment activer ?</p>
                  <ol className="text-xs text-zinc-400 space-y-2 list-decimal list-inside">
                    <li>Notez votre adresse MAC ci-dessus</li>
                    <li>Connectez-vous à votre <a href="/dashboard" className="text-primary hover:underline">Panel Revendeur</a></li>
                    <li>Allez dans la section "Activations"</li>
                    <li>Entrez votre MAC et validez</li>
                  </ol>
                </div>
              ) : (
                <div className="bg-amber-500/10 border border-amber-500/20 p-4 rounded-2xl text-left w-full mt-4">
                  <p className="text-xs text-amber-500 font-bold uppercase tracking-widest mb-2">Le serveur de chaînes bloque l'accès</p>
                  <p className="text-xs text-amber-500/80 mb-2">L'adresse de votre playlist/Xtream est soit erronée, soit votre fournisseur IPTV bloque cette application.</p>
                  <ol className="text-xs text-amber-500/70 space-y-1 list-decimal list-inside">
                    <li>Vérifiez l'URL, l'utilisateur et le mot de passe dans votre panel.</li>
                    <li>Contactez votre fournisseur IPTV pour autoriser le lecteur.</li>
                  </ol>
                </div>
              )}
              
              <div className="flex flex-col gap-2 w-full mt-4">
                {!isPlaylistError && (
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
                )}
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
