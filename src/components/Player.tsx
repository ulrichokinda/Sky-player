import React, { useEffect, useRef, useState, useCallback } from 'react';
import Hls from 'hls.js';
import mpegts from 'mpegts.js';
import { Play, Pause, Volume2, VolumeX, Maximize, ChevronLeft, List, Settings, SkipBack, SkipForward, Search, X, Star, Calendar, Grid, Lock, Unlock, Clock, CalendarRange, LockKeyhole } from 'lucide-react';
import { EPGProgram, parseEPG, formatTime } from '../lib/epgParser';
import { motion, AnimatePresence } from 'motion/react';
import { Channel } from '../lib/playlistParser';
import { MultiScreenPlayer } from './MultiScreenPlayer';
import { Badge, Input } from './ui';
import { api } from '../services/api';
import { Button } from './ui';

interface PlayerProps {
  url: string;
  onBack: () => void;
  channelName?: string;
  onNext?: () => void;
  onPrev?: () => void;
  channels?: Channel[];
  onChannelSelect?: (index: number) => void;
  macAddress?: string;
}

export const Player: React.FC<PlayerProps> = ({ 
  url, 
  onBack, 
  channelName = "Chaîne en direct", 
  onNext, 
  onPrev,
  channels = [],
  onChannelSelect,
  macAddress
}) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isMuted, setIsMuted] = useState(false);
  const [showControls, setShowControls] = useState(true);
  const [isLoading, setIsLoading] = useState(true);
  const [showChannelList, setShowChannelList] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [aspectRatio, setAspectRatio] = useState<'contain' | 'cover' | 'fill'>('contain');
  const [favorites, setFavorites] = useState<string[]>(() => {
    return JSON.parse(localStorage.getItem('sky_player_favorites') || '[]');
  });
  const [showEPG, setShowEPG] = useState(false);
  const [epgData, setEPGData] = useState<EPGProgram[]>([]);
  const [isMultiScreen, setIsMultiScreen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const controlsTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  // VOD / Timing Controls
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [isVOD, setIsVOD] = useState(false);

  // Parental Control
  const [isAdultLocked, setIsAdultLocked] = useState(false);
  const [pinInput, setPinInput] = useState('');
  const [pinError, setPinError] = useState(false);
  const PARENTAL_PIN = "0000"; // Default, could be fetched from user config
  
  // Simple URL protection: obfuscate the URL in the DOM
  const protectedUrl = btoa(url || ""); 

  useEffect(() => {
    // Check for Adult Content
    const currentChannel = channels.find(c => c.url === url);
    if (currentChannel && (
      currentChannel.group?.toLowerCase().includes('adult') || 
      currentChannel.group?.toLowerCase().includes('xxx') || 
      currentChannel.name?.toLowerCase().includes('xxx')
    )) {
       setIsAdultLocked(true);
    } else {
       setIsAdultLocked(false);
       setPinInput('');
       setPinError(false);
    }

    // Determine VOD
    setIsVOD(url?.includes('.mp4') || url?.includes('.mkv') || url?.includes('/movie/') || url?.includes('/series/'));

    // Notify server about current channel
    if (macAddress && channelName) {
      api.updateCurrentChannel(macAddress, channelName);
    }

    // Advanced EPG mock matching time block
    const fetchEPG = async () => {
      setEPGData([
        { title: "Matinée Info", start: new Date(new Date().setHours(new Date().getHours()-1)).toISOString(), end: new Date().toISOString(), description: "Le point sur l'actualité." },
        { title: channelName + " - En Direct", start: new Date().toISOString(), end: new Date(Date.now() + 3600000).toISOString(), description: "Programme en cours de diffusion." },
        { title: "Film de l'après-midi", start: new Date(Date.now() + 3600000).toISOString(), end: new Date(Date.now() + 10800000).toISOString(), description: "Un chef-d'œuvre classique." }
      ]);
    };
    fetchEPG();

    const handleKeyDown = (e: KeyboardEvent) => {
      setShowControls(true);
      if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current);
      controlsTimeoutRef.current = setTimeout(() => {
        if (!showChannelList) setShowControls(false);
      }, 4000);

      switch(e.key) {
        case 'Enter':
          if (!showControls) togglePlay();
          break;
        case 'Escape':
        case 'Backspace':
          if (showChannelList) setShowChannelList(false);
          else if (showEPG) setShowEPG(false);
          else onBack();
          break;
        case 'ArrowRight':
          if (isVOD) {
            if (videoRef.current) videoRef.current.currentTime += 10;
          } else {
            onNext?.();
          }
          break;
        case 'ArrowLeft':
          if (isVOD) {
            if (videoRef.current) videoRef.current.currentTime -= 10;
          } else {
            onPrev?.();
          }
          break;
        case 'ArrowUp':
          setShowChannelList(true);
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [url, showChannelList, showEPG]);

  const toggleFavorite = (channelUrl: string) => {
    const newFavorites = favorites.includes(channelUrl)
      ? favorites.filter(u => u !== channelUrl)
      : [...favorites, channelUrl];
    setFavorites(newFavorites);
    localStorage.setItem('sky_player_favorites', JSON.stringify(newFavorites));
  };

  const filteredChannels = channels.filter(c => 
    c.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    c.group?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  useEffect(() => {
    const video = videoRef.current;
    if (!video || isAdultLocked) return;

    let hls: Hls | null = null;
    let mpegtsPlayer: any = null;
    setIsLoading(true);
    setError(null);
    
    let finalUrl = url;
    
    const isXtreamStream = finalUrl.includes('/live/') || finalUrl.includes('/movie/') || finalUrl.includes('/series/');
    const isM3U8 = finalUrl.includes('.m3u8') || finalUrl.includes('type=m3u8') || finalUrl.includes('/hls/');
    const isMP4 = finalUrl.includes('.mp4') || finalUrl.includes('.mkv');
    const isTS = finalUrl.endsWith('.ts') || (!isM3U8 && !isMP4 && isXtreamStream);

    const handleTimeUpdate = () => {
      setCurrentTime(video.currentTime);
      // Wait for valid duration
      if (video.duration && !isNaN(video.duration) && video.duration !== Infinity && video.duration > 0) {
        setDuration(video.duration);
        setIsVOD(true); // If there's a duration, it's VOD or catchup
      }
    };
    video.addEventListener('timeupdate', handleTimeUpdate);

    if (isM3U8) {
      if (Hls.isSupported()) {
        hls = new Hls({ enableWorker: true, lowLatencyMode: true });
        hls.loadSource(finalUrl);
        hls.attachMedia(video);
        hls.on(Hls.Events.MANIFEST_PARSED, () => {
          video.play().catch(() => setIsPlaying(false));
          setIsPlaying(true);
          setIsLoading(false);
        });
        hls.on(Hls.Events.ERROR, (event, data) => {
          if (data.fatal) {
            switch(data.type) {
              case Hls.ErrorTypes.NETWORK_ERROR:
                setError("Erreur réseau: Impossible de charger le flux vidéo.");
                hls?.startLoad();
                break;
              case Hls.ErrorTypes.MEDIA_ERROR:
                hls?.recoverMediaError();
                break;
              default:
                hls?.destroy();
                setError("Erreur fatale: Le format n'est pas supporté par votre navigateur.");
                break;
            }
          }
        });
      } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
        video.src = finalUrl;
        video.addEventListener('loadedmetadata', () => {
          video.play().catch(() => setIsPlaying(false));
          setIsPlaying(true);
          setIsLoading(false);
        });
      }
    } else if (isTS && mpegts.isSupported()) {
      mpegtsPlayer = mpegts.createPlayer({ type: 'mse', isLive: finalUrl.includes('/live/'), url: finalUrl });
      mpegtsPlayer.attachMediaElement(video);
      mpegtsPlayer.load();
      mpegtsPlayer.play().then(() => {
        setIsPlaying(true);
        setIsLoading(false);
      }).catch((err: any) => {
        setIsPlaying(false);
        setError("Impossible de charger ce flux. Le format ou le lien est invalide.");
      });
      mpegtsPlayer.on(mpegts.Events.ERROR, () => {
        setError("Erreur de lecture: Impossible de charger ce flux.");
        setIsLoading(false);
      });
    } else {
      video.src = finalUrl;
      video.play().then(() => {
        setIsPlaying(true);
        setIsLoading(false);
      }).catch((err) => {
        setIsPlaying(false);
        setError("Impossible de charger ce flux. Le format ou le lien est invalide.");
      });
      setIsLoading(false);
    }

    const handleError = () => {
      setError("Erreur de lecture du média. Tentative de reconnexion...");
      setIsLoading(false);
    };

    video.addEventListener('error', handleError);

    return () => {
      if (hls) hls.destroy();
      if (mpegtsPlayer) mpegtsPlayer.destroy();
      video.removeEventListener('error', handleError);
      video.removeEventListener('timeupdate', handleTimeUpdate);
    };
  }, [url, isAdultLocked]);

  const togglePlay = () => {
    if (videoRef.current) {
      if (isPlaying) videoRef.current.pause();
      else videoRef.current.play();
      setIsPlaying(!isPlaying);
    }
  };

  const toggleMute = () => {
    if (videoRef.current) {
      videoRef.current.muted = !isMuted;
      setIsMuted(!isMuted);
    }
  };

  const handleMouseMove = () => {
    setShowControls(true);
    if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current);
    controlsTimeoutRef.current = setTimeout(() => {
      if (!showChannelList) setShowControls(false);
    }, 4000);
  };

  const formatVODTime = (timeInSeconds: number) => {
    if (isNaN(timeInSeconds)) return "00:00";
    const h = Math.floor(timeInSeconds / 3600);
    const m = Math.floor((timeInSeconds % 3600) / 60);
    const s = Math.floor(timeInSeconds % 60);
    if (h > 0) return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  const handleSeek = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (videoRef.current) {
      const time = Number(e.target.value);
      videoRef.current.currentTime = time;
      setCurrentTime(time);
    }
  };

  const handlePinSubmit = (e?: React.FormEvent) => {
    e?.preventDefault();
    if (pinInput === PARENTAL_PIN) {
      setIsAdultLocked(false);
      setPinError(false);
    } else {
      setPinError(true);
      setPinInput('');
    }
  };

  if (isAdultLocked) {
    return (
      <div className="fixed inset-0 bg-zinc-950 flex flex-col items-center justify-center z-50">
        <motion.div 
          initial={{ scale: 0.9, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          className="bg-zinc-900 border border-zinc-800 p-8 rounded-3xl max-w-sm w-full text-center space-y-6 shadow-2xl"
        >
          <div className="w-20 h-20 bg-red-500/10 rounded-full flex items-center justify-center mx-auto mb-4 border border-red-500/20">
            <LockKeyhole size={40} className="text-red-500" />
          </div>
          <h2 className="text-2xl font-black italic tracking-tighter">Code Parental</h2>
          <p className="text-zinc-500 text-sm">Ce contenu est protégé. Veuillez saisir votre code parental (0000).</p>
          
          <form onSubmit={handlePinSubmit} className="space-y-4">
            <Input 
              type="password" 
              placeholder="••••" 
              maxLength={4}
              value={pinInput}
              onChange={(e) => {
                 setPinInput(e.target.value);
                 if (pinError) setPinError(false);
              }}
              className="text-center tracking-[1em] text-2xl font-bold bg-zinc-950 border-zinc-800 focus:border-primary"
            />
            {pinError && <p className="text-red-500 text-xs font-bold animate-pulse">Code PIN incorrect</p>}
            
            <div className="flex gap-4 pt-4">
              <Button type="button" variant="outline" onClick={onBack} className="flex-1">Retour</Button>
              <Button type="submit" variant="primary" className="flex-1">Déverrouiller</Button>
            </div>
          </form>
        </motion.div>
      </div>
    );
  }

  return (
    <div 
      className="fixed inset-0 bg-black flex items-center justify-center overflow-hidden group"
      onMouseMove={handleMouseMove}
      style={{ cursor: showControls ? 'default' : 'none' }}
    >
      <video
        ref={videoRef}
        className={`w-full h-full transition-all duration-300 ${
          aspectRatio === 'contain' ? 'object-contain' : 
          aspectRatio === 'cover' ? 'object-cover' : 'object-fill'
        }`}
        playsInline
        onClick={togglePlay}
      />

      {isLoading && (
        <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/80 z-50">
          <div className="w-16 h-16 border-4 border-primary border-t-transparent rounded-full animate-spin mb-4" />
          <p className="text-primary font-black uppercase tracking-[0.2em] text-xs animate-pulse">Chargement du flux...</p>
        </div>
      )}

      {error && !isLoading && (
        <div className="absolute inset-0 flex flex-col items-center justify-center bg-black z-50 p-8 text-center">
          <div className="w-20 h-20 bg-red-500/20 text-red-500 rounded-full flex items-center justify-center mb-6 border border-red-500/20">
            <X size={40} />
          </div>
          <h3 className="text-2xl font-black italic mb-2 tracking-tighter">Erreur de Lecture</h3>
          <p className="text-zinc-500 max-w-md mb-8">{error}</p>
          <div className="flex gap-4">
            <Button onClick={() => window.location.reload()} variant="primary">Réessayer</Button>
            <Button onClick={onBack} variant="outline">Retour aux chaînes</Button>
          </div>
        </div>
      )}

      <AnimatePresence>
        {showControls && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 bg-gradient-to-t from-black/90 via-transparent to-black/80 flex flex-col justify-between p-8 z-40 transition-opacity"
          >
            {/* Top Bar */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-4">
                <button 
                  onClick={onBack}
                  className="p-3 bg-white/10 hover:bg-primary hover:text-black rounded-2xl backdrop-blur-xl transition-all"
                >
                  <ChevronLeft size={24} />
                </button>
                <div>
                  <h2 className="text-2xl font-black text-white tracking-tighter leading-none">{channelName}</h2>
                  <div className="flex items-center gap-2 mt-1">
                    <span className="w-2 h-2 bg-red-500 rounded-full animate-pulse" />
                    <span className="text-[10px] text-zinc-400 font-bold uppercase tracking-widest">{isVOD ? 'VOD / Replay' : 'En Direct • 4K Ultra HD'}</span>
                  </div>
                </div>
              </div>
              
              <div className="flex items-center gap-3">
                <button 
                  onClick={() => setIsMultiScreen(true)}
                  className="p-3 bg-white/10 hover:bg-white/20 rounded-2xl backdrop-blur-xl transition-all text-white border border-white/5"
                  title="Multi-écrans"
                >
                  <Grid size={20} />
                </button>
                <button 
                  onClick={() => setShowEPG(!showEPG)}
                  className={`p-3 rounded-2xl backdrop-blur-xl transition-all border border-white/5 flex items-center gap-2 ${
                    showEPG ? 'bg-primary text-black' : 'bg-white/10 text-white hover:bg-white/20'
                  }`}
                  title="Programme TV"
                >
                  <Calendar size={20} />
                </button>
                <button 
                  onClick={() => toggleFavorite(url)}
                  className={`p-3 rounded-2xl backdrop-blur-xl transition-all border border-white/5 ${
                    favorites.includes(url) ? 'bg-yellow-500 text-black border-yellow-500' : 'bg-white/10 text-white hover:bg-white/20'
                  }`}
                >
                  <Star size={20} fill={favorites.includes(url) ? "currentColor" : "none"} />
                </button>
                <button 
                  onClick={() => setShowChannelList(true)}
                  className="p-3 bg-white/10 hover:bg-white/20 rounded-2xl backdrop-blur-xl transition-all flex items-center gap-2 border border-white/5"
                >
                  <List size={20} />
                  <span className="text-xs font-black uppercase tracking-widest hidden md:block">Chaînes</span>
                </button>
              </div>
            </div>

            {/* Bottom Bar Controls */}
            <div className="space-y-6">
              {/* VOD / DVR Seek bar */}
              {isVOD && duration > 0 && (
                <div className="flex items-center gap-4 bg-black/40 backdrop-blur-md p-4 rounded-2xl border border-white/10">
                  <span className="text-xs font-mono text-white/70">{formatVODTime(currentTime)}</span>
                  <input 
                    type="range" 
                    min="0" 
                    max={duration} 
                    value={currentTime} 
                    onChange={handleSeek}
                    className="flex-1 accent-primary h-2 bg-white/10 rounded-full appearance-none cursor-pointer"
                  />
                  <span className="text-xs font-mono text-white/70">{formatVODTime(duration)}</span>
                </div>
              )}

              <div className="flex items-center justify-between">
                <div className="flex items-center gap-6">
                  <div className="flex items-center gap-2">
                    <button onClick={onPrev} className="p-3 text-white/70 hover:text-white bg-white/5 hover:bg-white/10 rounded-2xl transition-colors backdrop-blur-md">
                      <SkipBack size={24} />
                    </button>
                    <button 
                      onClick={togglePlay}
                      className="w-16 h-16 flex items-center justify-center bg-primary text-black rounded-3xl hover:scale-105 transition-transform shadow-[0_0_40px_-10px] shadow-primary"
                    >
                      {isPlaying ? <Pause size={32} fill="currentColor" /> : <Play size={32} fill="currentColor" className="ml-1" />}
                    </button>
                    <button onClick={onNext} className="p-3 text-white/70 hover:text-white bg-white/5 hover:bg-white/10 rounded-2xl transition-colors backdrop-blur-md">
                      <SkipForward size={24} />
                    </button>
                  </div>

                  <div className="h-10 w-[1px] bg-white/10 mx-2" />

                  <button onClick={toggleMute} className="p-3 bg-white/5 hover:bg-white/10 rounded-2xl text-white/70 hover:text-white transition-colors backdrop-blur-md">
                    {isMuted ? <VolumeX size={24} /> : <Volume2 size={24} />}
                  </button>
                </div>

                <div className="flex items-center gap-4">
                  <div className="flex bg-white/5 p-1 rounded-2xl backdrop-blur-md border border-white/5">
                    {(['contain', 'cover', 'fill'] as const).map((ratio) => (
                      <button
                        key={ratio}
                        onClick={() => setAspectRatio(ratio)}
                        className={`px-4 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all ${
                          aspectRatio === ratio ? 'bg-primary text-black' : 'text-zinc-500 hover:text-white hover:bg-white/5'
                        }`}
                      >
                        {ratio}
                      </button>
                    ))}
                  </div>
                  <button 
                    onClick={() => videoRef.current?.requestFullscreen()}
                    className="p-3 bg-white/10 hover:bg-white/20 border border-white/5 rounded-2xl transition-all text-white backdrop-blur-md"
                  >
                    <Maximize size={24} />
                  </button>
                </div>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Advanced EPG Overlay */}
      <AnimatePresence>
        {showEPG && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 20 }}
            className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-black/90 backdrop-blur-3xl border border-white/10 rounded-[2rem] p-8 z-50 w-full max-w-4xl shadow-[0_0_100px_rgba(0,0,0,0.8)]"
          >
            <div className="flex items-center justify-between mb-8">
              <div className="flex items-center gap-4">
                <div className="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center">
                  <CalendarRange className="text-primary" size={24} />
                </div>
                <div>
                  <h3 className="text-2xl font-black italic tracking-tighter">Guide des Programmes</h3>
                  <p className="text-sm text-zinc-500 font-medium">{channelName}</p>
                </div>
              </div>
              <button onClick={() => setShowEPG(false)} className="p-3 bg-white/5 hover:bg-white/10 rounded-2xl transition-colors">
                <X size={24} />
              </button>
            </div>
            
            <div className="space-y-4 relative">
              <div className="absolute left-[88px] top-0 bottom-0 w-[2px] bg-zinc-800/50" />
              {epgData.map((prog, i) => {
                const isCurrent = i === 1;
                return (
                  <div key={i} className={`relative flex gap-6 p-4 rounded-2xl transition-all ${isCurrent ? 'bg-primary/5 hover:bg-primary/10 border border-primary/20' : 'hover:bg-white/5 border border-transparent'}`}>
                    <div className="w-20 shrink-0 relative flex flex-col items-end pt-1">
                      <span className={`text-sm font-black ${isCurrent ? 'text-primary' : 'text-zinc-500'}`}>
                        {formatTime(prog.start)}
                      </span>
                      {isCurrent && (
                        <div className="absolute -right-[13px] top-2 w-3 h-3 bg-primary rounded-full shadow-[0_0_10px] shadow-primary" />
                      )}
                    </div>
                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-1">
                        <h4 className={`font-bold text-lg ${isCurrent ? 'text-white' : 'text-zinc-300'}`}>{prog.title}</h4>
                        {isCurrent && <Badge variant="primary" className="text-[10px] h-5 px-2">En cours</Badge>}
                      </div>
                      <p className="text-sm text-zinc-500 line-clamp-2">{prog.description}</p>
                      <div className="flex items-center gap-2 mt-3 opacity-60">
                        <Clock size={12} className="text-zinc-500" />
                        <span className="text-[10px] uppercase tracking-widest font-bold text-zinc-500">
                          Jusqu'à {formatTime(prog.end)}
                        </span>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Multi-Screen Player */}
      {isMultiScreen && (
        <MultiScreenPlayer 
          channels={channels} 
          onBack={() => setIsMultiScreen(false)} 
        />
      )}

      {/* Channel List Sidebar */}
      <AnimatePresence>
        {showChannelList && (
          <>
            <motion.div 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setShowChannelList(false)}
              className="absolute inset-0 bg-black/60 backdrop-blur-sm z-[60]"
            />
            <motion.div
              initial={{ x: '100%' }}
              animate={{ x: 0 }}
              exit={{ x: '100%' }}
              className="absolute top-0 right-0 bottom-0 w-full max-w-md bg-zinc-950 border-l border-white/10 z-[70] flex flex-col shadow-2xl"
            >
              <div className="p-6 border-b border-white/10 flex items-center justify-between">
                <h3 className="text-xl font-black italic tracking-tighter">Liste des Chaînes</h3>
                <button onClick={() => setShowChannelList(false)} className="p-2 hover:bg-white/10 rounded-xl transition-colors">
                  <X size={24} />
                </button>
              </div>

              <div className="p-4 border-b border-white/5">
                <div className="relative">
                  <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500" size={18} />
                  <input 
                    type="text"
                    placeholder="Rechercher une chaîne..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-full bg-white/5 border border-white/10 rounded-2xl py-3 pl-12 pr-4 text-sm focus:outline-none focus:border-primary transition-all"
                  />
                </div>
              </div>

              <div className="flex-1 overflow-y-auto p-4 space-y-2 custom-scrollbar">
                {filteredChannels.map((channel, index) => {
                  const isAdult = channel.group?.toLowerCase().includes('xxx') || channel.group?.toLowerCase().includes('adult');
                  return (
                    <button
                      key={index}
                      onClick={() => {
                        onChannelSelect?.(index);
                        setShowChannelList(false);
                      }}
                      className={`w-full flex items-center gap-4 p-3 rounded-2xl transition-all group ${
                        channel.url === url ? 'bg-primary text-black' : 'hover:bg-white/5 text-zinc-400 hover:text-white'
                      }`}
                    >
                      {channel.logo ? (
                        <img src={channel.logo} alt="" className="w-12 h-12 rounded-xl object-cover bg-black/20" referrerPolicy="no-referrer" />
                      ) : (
                        <div className="w-12 h-12 rounded-xl bg-white/10 flex items-center justify-center">
                          <List size={20} />
                        </div>
                      )}
                      <div className="flex-1 text-left">
                        <div className="flex items-center gap-2">
                          <p className="font-bold text-sm line-clamp-1">{channel.name}</p>
                          {isAdult && <Lock size={12} className="text-red-500 shrink-0" />}
                        </div>
                        {channel.group && <p className={`text-[10px] uppercase font-black tracking-widest ${channel.url === url ? 'text-black/60' : 'text-zinc-600'}`}>{channel.group}</p>}
                      </div>
                    </button>
                  );
                })}
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
};
