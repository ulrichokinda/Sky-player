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
import { getTVPlatform, isTV } from '../lib/TVUtils';

interface SimpleUserViewProps {
  channels: any[];
  onNotify: (msg: string, type: 'success' | 'error' | 'info') => void;
}

export const SimpleUserView: React.FC<SimpleUserViewProps> = ({ onNotify }) => {
  const { branding } = useBranding();
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [lowDataMode, setLowDataMode] = useState(false);
  const [macAddress, setMacAddress] = useState('00:00:00:00:00:00');
  const [platformName, setPlatformName] = useState(getTVPlatform());
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
            // Priority 1: Use identifier but clean it
            let cleanId = id.identifier.toUpperCase().replace(/[^A-F0-9]/g, '');
            
            // If it's a short ID (like Android ID usually 16 hex chars), format it as MAC-style for user familiarity
            if (cleanId.length >= 12) {
              cleanId = cleanId.substring(0, 12).match(/.{1,2}/g)?.join(':') || cleanId;
            } else if (cleanId.length > 0) {
              // Pad if too short
              cleanId = cleanId.padEnd(12, '0').match(/.{1,2}/g)?.join(':') || cleanId;
            }
            
            // On TV, we want maximal stability. 
            if (!devId || devId.startsWith('00:E4:55')) {
              devId = cleanId;
              localStorage.setItem('sky_player_device_id', devId);
            }
          }
        } catch (e) {
          console.error('Device ID error:', e);
        }
      } else {
        // Platform specific web detection for Samsung / LG
        const tvPlat = getTVPlatform();
        setPlatformName(tvPlat);

        if (tvPlat === 'Tizen') {
          try {
            // @ts-ignore
            const tizenId = window.tizen?.systeminfo?.getPropertyValue('ID');
            if (tizenId) {
              devId = tizenId.substring(0, 12).toUpperCase().match(/.{1,2}/g)?.join(':') || tizenId;
              localStorage.setItem('sky_player_device_id', devId);
            }
          } catch (e) {}
        } else if (tvPlat === 'webOS') {
          try {
            // @ts-ignore
            const webosId = window.webOS?.deviceInfo?.modelName; // fallback if ID not available
            // Usually we rely on localStorage for webOS as hardware ID access is restricted for non-priviledged apps
          } catch (e) {}
        }
      }

      if (!devId) {
        // Fallback to random if no hardware ID available
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
  const [loadingState, setLoadingState] = useState(''); // Text for the loading step
  const [progressBytes, setProgressBytes] = useState(0);

  useEffect(() => {
    if (macAddress === '00:00:00:00:00:00') return;

    const checkActivation = async () => {
      try {
        setLoadingState('Connexion au serveur SkyPlayer (MAC: ' + macAddress + ')...');
        const data = await api.checkMacStatus(macAddress);
        if (data.active) {
          let targetUrl = data.activation?.playlist_url;
          
          if (!targetUrl && data.activation?.xtream_host && data.activation?.xtream_username && data.activation?.xtream_password) {
            const host = data.activation.xtream_host.endsWith('/') ? data.activation.xtream_host.slice(0, -1) : data.activation.xtream_host;
            targetUrl = `${host}/get.php?username=${data.activation.xtream_username}&password=${data.activation.xtream_password}&type=m3u&output=m3u8`;
          }

          if (targetUrl && targetUrl !== playlistUrl) {
            setPlaylistUrl(targetUrl);
            setLoadingState('Connexion à votre serveur Xtream IPTV...');
            setIsChecking(true);
            setProgressBytes(0);
            
            // Extends safety timeout for loading to 90 seconds explicitly for heavy servers
            const timeout = setTimeout(() => {
              if (channels.length === 0) {
                setError("Le chargement dépasse le délai (90s). Certains serveurs IPTV sont très lents ou saturent. Réessayez ou vérifiez votre lien.");
                setIsPlaylistError(true);
                setIsChecking(false);
                setLoadingState('');
              }
            }, 90000);
            
            try {
              setLoadingState('Téléchargement des données (cela peut être volumineux)...');
              const parsedChannels = await fetchAndParsePlaylist(targetUrl, (msg) => {
                setLoadingState(msg);
                // Extract size if present in message for simple visual progress
                const sizeMatch = msg.match(/(\d+)\s*KB/);
                if (sizeMatch) setProgressBytes(parseInt(sizeMatch[1]) * 1024);
              });
              clearTimeout(timeout);
              
              if (parsedChannels && parsedChannels.length > 0) {
                setChannels(parsedChannels);
                setCurrentChannelIndex(0);
                setError(null);
                setIsPlaylistError(false);
              } else {
                setError("La liste de lecture est vide, invalide, ou l'accès a été refusé par le serveur cible.");
                setIsPlaylistError(true);
              }
            } catch (err: any) {
              clearTimeout(timeout);
              console.error("SimpleUserView parsing error", err);
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
        setLoadingState('');
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
      <div className="w-full max-w-5xl mx-auto flex flex-col gap-6 p-4 md:p-8 mt-10">
        
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
            
            <div className="flex flex-col items-center gap-1">
              <span className="text-zinc-500 text-[10px] font-black uppercase tracking-[0.2em] mb-1">ID Appareil ({platformName})</span>
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
      </div>

        {/* Content Section */}
        <div className="flex flex-col gap-6 max-w-2xl mx-auto w-full shrink-0">
          {isChecking || loadingState ? (
            <div className="flex flex-col items-center gap-6 py-12">
              <Loader2 className="w-12 h-12 text-primary animate-spin" />
              <div className="flex flex-col items-center gap-2">
                <p className="text-lg text-white font-medium text-center">{loadingState || "Vérification de l'activation..."}</p>
                {progressBytes > 0 && (
                  <p className="text-xs text-primary font-bold">
                    {(progressBytes / 1024).toFixed(0)} KB téléchargés
                  </p>
                )}
                <div className="w-48 h-1 bg-zinc-800 rounded-full overflow-hidden mt-4">
                  <div className="h-full bg-primary animate-pulse w-full"></div>
                </div>
              </div>
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
                  <p className="text-xs text-primary font-bold uppercase tracking-widest mb-2">Comment activer sur {platformName} ?</p>
                  <ol className="text-[10px] text-zinc-400 space-y-2 list-decimal list-inside leading-tight">
                    <li>Copiez l'ID exact affiché ci-dessus : <strong className="text-white">{macAddress}</strong></li>
                    <li>Ajoutez cet ID dans votre <a href="/dashboard" className="text-primary hover:underline font-bold">Panel Revendeur</a></li>
                    <li>Assurez-vous d'avoir configuré soit un lien M3U, soit des codes Xtream</li>
                    <li>Si l'ID change, utilisez le bouton ✎ en haut à droite pour le forcer</li>
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
                <Button onClick={() => window.location.reload()} variant="outline" size="sm" icon={RotateCcw} fullWidth>
                  Rafraîchir
                </Button>
              </div>
            </div>
          ) : (
            <>
              <motion.div 
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1 }}
                className="bg-zinc-900/50 border border-zinc-800 p-4 rounded-2xl flex items-center gap-4 mt-4"
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
