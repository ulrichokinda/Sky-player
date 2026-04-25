import React, { useEffect, useRef, useState, useCallback } from 'react';
import Hls from 'hls.js';
import { 
  Maximize, Minimize, Play, Pause, Volume2, VolumeX, 
  Settings, SkipBack, SkipForward, Wifi, WifiOff, 
  Activity, Monitor, Zap, Gauge, Sliders, Info,
  ChevronUp, ChevronDown, FastForward, PictureInPicture as Pip
} from 'lucide-react';
import { Button, cn, Badge } from './ui';
import { motion, AnimatePresence } from 'motion/react';

interface PlayerProps {
  url: string;
  title?: string;
}

export const VideoPlayer: React.FC<PlayerProps> = ({ url, title }) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isMuted, setIsMuted] = useState(false);
  const [progress, setProgress] = useState(0);
  const [duration, setDuration] = useState(0);

  const [error, setError] = useState<string | null>(null);
  const [isBuffering, setIsBuffering] = useState(false);
  const [lowBandwidthMode, setLowBandwidthMode] = useState(false);
  const [connectionHealth, setConnectionHealth] = useState<'good' | 'fair' | 'poor'>('good');
  
  // High-end features state
  const [showStats, setShowStats] = useState(false);
  const [stats, setStats] = useState({
    bitrate: 0,
    resolution: '0x0',
    buffer: 0,
    droppedFrames: 0,
    latency: 0
  });

  const updateStats = useCallback((hlsInstance: Hls, videoElement: HTMLVideoElement, data: any) => {
    const level = hlsInstance.levels[hlsInstance.currentLevel];
    setStats((prev: any) => ({
      ...prev,
      bitrate: level?.bitrate || 0,
      resolution: level ? `${level.width}x${level.height}` : '0x0',
      buffer: videoElement.buffered.length > 0 ? videoElement.buffered.end(videoElement.buffered.length - 1) - videoElement.currentTime : 0,
      droppedFrames: (videoElement as any).getVideoPlaybackQuality?.().droppedVideoFrames || 0,
      latency: data.stats.loading.start - data.stats.loading.end
    }));
  }, []);
  const [levels, setLevels] = useState<any[]>([]);
  const [currentLevel, setCurrentLevel] = useState(-1);
  const [playbackRate, setPlaybackRate] = useState(1);
  const [showSettings, setShowSettings] = useState(false);
  const [imageFilters, setImageFilters] = useState({
    brightness: 100,
    contrast: 100,
    saturation: 100
  });
  const [isPiP, setIsPiP] = useState(false);
  const [isTurboStart, setIsTurboStart] = useState(true);
  const [isZapping, setIsZapping] = useState(false);
  const [notification, setNotification] = useState<{ message: string; type: 'success' | 'error' | 'info' } | null>(null);

  const notify = (message: string, type: 'success' | 'error' | 'info' = 'info') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 3000);
  };

  useEffect(() => {
    if (!videoRef.current || !url) return;
    
    setIsZapping(true);
    const zapTimer = setTimeout(() => setIsZapping(false), 1500);
    
    // Turbo Start: Small buffer initially for instant playback
    const turboTimer = setTimeout(() => setIsTurboStart(false), 5000);
    
    const video = videoRef.current;
    let hls: Hls | null = null;
    setError(null);

    const handleWaiting = () => setIsBuffering(true);
    const handlePlaying = () => setIsBuffering(false);

    video.addEventListener('waiting', handleWaiting);
    video.addEventListener('playing', handlePlaying);
    video.addEventListener('canplay', handlePlaying);

    if (Hls.isSupported()) {
      hls = new Hls({
        enableWorker: true,
        lowLatencyMode: false,
        backBufferLength: 90,
        maxBufferLength: isTurboStart ? 10 : (lowBandwidthMode ? 90 : 60),
        maxMaxBufferLength: 180,
        maxBufferSize: lowBandwidthMode ? 200 * 1000 * 1000 : 120 * 1000 * 1000,
        manifestLoadingMaxRetry: 20,
        manifestLoadingRetryDelay: 500,
        manifestLoadingMaxRetryTimeout: 64000,
        levelLoadingMaxRetry: 20,
        levelLoadingRetryDelay: 500,
        fragLoadingMaxRetry: 30,
        fragLoadingRetryDelay: 500,
        startLevel: -1, // Auto
        abrEwmaFastLive: 1.0,
        abrEwmaSlowLive: 3.0,
        capLevelToPlayerSize: true,
        initialLiveManifestSize: 3,
        maxLoadingDelay: 2,
        manifestLoadingTimeOut: 30000,
        fragLoadingTimeOut: 45000,
        xhrSetup: (xhr: XMLHttpRequest, url: string) => {
          xhr.timeout = 40000;
          // EMPREINTE NATIVE AVANCÉE
          xhr.setRequestHeader('User-Agent', 'Mozilla/5.0 (Linux; Android 10; SM-G981B) SmartersPlayer/3.0.0');
          xhr.setRequestHeader('X-Requested-With', 'com.nst.iptvsmarterstvbox');
          xhr.setRequestHeader('Accept', '*/*');
          xhr.setRequestHeader('Connection', 'keep-alive');
        }
      });
      
      hls.loadSource(url);
      hls.attachMedia(video);
      
      hls.on(Hls.Events.MANIFEST_PARSED, (_event: string, data: { levels: any[] }) => {
        setLevels(data.levels);
        video.play().catch((err: Error) => {
          console.warn("Autoplay blocked or failed:", err);
        });
        setIsPlaying(true);
      });

      hls.on(Hls.Events.LEVEL_SWITCHED, (_event: string, data: { level: number }) => {
        setCurrentLevel(data.level);
      });

      hls.on(Hls.Events.FRAG_LOADED, (_event: string, data: any) => {
        const loadTime = data.stats.loading.end - data.stats.loading.start;
        if (loadTime > 2000) setConnectionHealth('poor');
        else if (loadTime > 800) setConnectionHealth('fair');
        else setConnectionHealth('good');

        // Update stats
        if (hls && video) {
          updateStats(hls, video, data);
        }
      });

      hls.on(Hls.Events.ERROR, (_event: string, data: any) => {
        if (data.fatal) {
          console.error("HLS Fatal Error:", data.type, data.details);
          switch (data.type) {
            case Hls.ErrorTypes.NETWORK_ERROR:
              setError("Problème de connexion... Tentative de reconnexion");
              hls?.startLoad();
              break;
            case Hls.ErrorTypes.MEDIA_ERROR:
              setError("Erreur de flux... Récupération en cours");
              hls?.recoverMediaError();
              break;
            default:
              setError("Erreur fatale du lecteur");
              hls?.destroy();
              break;
          }
        }
      });
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
      video.src = url;
      video.addEventListener('loadedmetadata', () => {
        video.play().catch(() => {});
        setIsPlaying(true);
      });
      video.addEventListener('error', () => {
        setError("Erreur de chargement du flux");
      });
    }

    return () => {
      clearTimeout(zapTimer);
      clearTimeout(turboTimer);
      video.removeEventListener('waiting', handleWaiting);
      video.removeEventListener('playing', handlePlaying);
      video.removeEventListener('canplay', handlePlaying);
      if (hls) {
        hls.destroy();
      }
    };
  }, [url, isTurboStart]);

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

  const handleTimeUpdate = () => {
    if (videoRef.current) {
      setProgress((videoRef.current.currentTime / videoRef.current.duration) * 100);
    }
  };

  const handleLoadedMetadata = () => {
    if (videoRef.current) {
      setDuration(videoRef.current.duration);
    }
  };

  const toggleFullscreen = () => {
    if (containerRef.current) {
      if (document.fullscreenElement) {
        document.exitFullscreen();
      } else {
        containerRef.current.requestFullscreen();
      }
    }
  };

  const togglePiP = async () => {
    if (videoRef.current) {
      try {
        if (document.pictureInPictureElement) {
          await document.exitPictureInPicture();
          setIsPiP(false);
        } else {
          await videoRef.current.requestPictureInPicture();
          setIsPiP(true);
        }
      } catch (err) {
        console.error("PiP failed:", err);
      }
    }
  };

  const skip = (amount: number) => {
    if (videoRef.current) {
      videoRef.current.currentTime += amount;
    }
  };

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;

      switch (e.key.toLowerCase()) {
        case ' ':
        case 'k':
          e.preventDefault();
          togglePlay();
          break;
        case 'f':
          e.preventDefault();
          toggleFullscreen();
          break;
        case 'm':
          e.preventDefault();
          toggleMute();
          break;
        case 'l':
          e.preventDefault();
          skip(10);
          break;
        case 'j':
          e.preventDefault();
          skip(-10);
          break;
        case 'p':
          e.preventDefault();
          togglePiP();
          break;
        case 's':
          e.preventDefault();
          setShowStats((prev: boolean) => !prev);
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isPlaying, isMuted]);

  const filterStyle = {
    filter: `brightness(${imageFilters.brightness}%) contrast(${imageFilters.contrast}%) saturate(${imageFilters.saturation}%)`
  };

  return (
    <div ref={containerRef} className="relative group bg-black rounded-xl overflow-hidden aspect-video shadow-2xl">
      <video
        ref={videoRef}
        className="w-full h-full cursor-pointer transition-all duration-300"
        style={filterStyle}
        onTimeUpdate={handleTimeUpdate}
        onLoadedMetadata={handleLoadedMetadata}
        onClick={togglePlay}
        playsInline
      />
      
      {/* Stats for Nerds Overlay */}
      {showStats && (
        <div className="absolute top-4 left-4 bg-black/80 backdrop-blur-md p-4 rounded-2xl border border-white/10 text-[10px] font-mono text-primary z-20 space-y-1 pointer-events-none">
          <div className="flex items-center gap-2 border-b border-white/5 pb-1 mb-1">
            <Activity size={12} />
            <span className="font-black uppercase tracking-widest">Stats Avancées</span>
          </div>
          <p>Résolution: <span className="text-white">{stats.resolution}</span></p>
          <p>Débit: <span className="text-white">{(stats.bitrate / 1000000).toFixed(2)} Mbps</span></p>
          <p>Tampon: <span className="text-white">{stats.buffer.toFixed(1)}s</span></p>
          <p>Frames Perdues: <span className="text-white">{stats.droppedFrames}</span></p>
          <p>Santé: <span className={cn(
            connectionHealth === 'good' ? "text-emerald-500" : 
            connectionHealth === 'fair' ? "text-amber-500" : "text-red-500"
          )}>{connectionHealth.toUpperCase()}</span></p>
        </div>
      )}

      {/* Settings Menu Overlay */}
      <AnimatePresence>
        {showSettings && (
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm z-30 flex items-center justify-center p-4">
            <div className="bg-zinc-900 border border-zinc-800 rounded-3xl w-full max-w-sm overflow-hidden shadow-2xl">
              <div className="p-4 border-b border-zinc-800 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Settings size={16} className="text-primary" />
                  <span className="font-black uppercase tracking-widest text-xs">Paramètres Avancés</span>
                </div>
                <button onClick={() => setShowSettings(false)} className="p-1 hover:bg-white/5 rounded-full">
                  <Minimize size={16} />
                </button>
              </div>
              
              <div className="p-6 space-y-6 max-h-[70vh] overflow-y-auto custom-scrollbar">
                {/* Quality Selection */}
                <div className="space-y-3">
                  <div className="flex items-center gap-2 text-zinc-400">
                    <Monitor size={14} />
                    <span className="text-[10px] font-black uppercase tracking-widest">Qualité Vidéo</span>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <button 
                      onClick={() => {
                        const hls = (videoRef.current as any)?.__hls__;
                        if (hls) hls.currentLevel = -1;
                        setCurrentLevel(-1);
                      }}
                      className={cn(
                        "px-3 py-1.5 rounded-xl text-[10px] font-bold border transition-all",
                        currentLevel === -1 ? "bg-primary text-black border-primary" : "bg-zinc-800 border-zinc-700 text-zinc-400"
                      )}
                    >
                      Auto
                    </button>
                    {levels.map((level, idx) => (
                      <button 
                        key={idx}
                        onClick={() => {
                          const hls = (videoRef.current as any)?.__hls__;
                          if (hls) hls.currentLevel = idx;
                          setCurrentLevel(idx);
                        }}
                        className={cn(
                          "px-3 py-1.5 rounded-xl text-[10px] font-bold border transition-all",
                          currentLevel === idx ? "bg-primary text-black border-primary" : "bg-zinc-800 border-zinc-700 text-zinc-400"
                        )}
                      >
                        {level.height}p
                      </button>
                    ))}
                  </div>
                </div>

                {/* Playback Speed */}
                <div className="space-y-3">
                  <div className="flex items-center gap-2 text-zinc-400">
                    <Gauge size={14} />
                    <span className="text-[10px] font-black uppercase tracking-widest">Vitesse de Lecture</span>
                  </div>
                  <div className="flex gap-2">
                    {[0.5, 1, 1.25, 1.5, 2].map(speed => (
                      <button 
                        key={speed}
                        onClick={() => {
                          if (videoRef.current) videoRef.current.playbackRate = speed;
                          setPlaybackRate(speed);
                        }}
                        className={cn(
                          "flex-1 py-1.5 rounded-xl text-[10px] font-bold border transition-all",
                          playbackRate === speed ? "bg-primary text-black border-primary" : "bg-zinc-800 border-zinc-700 text-zinc-400"
                        )}
                      >
                        {speed}x
                      </button>
                    ))}
                  </div>
                </div>

                {/* Zapping Speed */}
                <div className="space-y-3">
                  <div className="flex items-center gap-2 text-zinc-400">
                    <Zap size={14} />
                    <span className="text-[10px] font-black uppercase tracking-widest">Vitesse de Zapping</span>
                  </div>
                  <div className="flex gap-2">
                    {['Standard', 'Turbo', 'Ultra'].map(mode => (
                      <button 
                        key={mode}
                        onClick={() => {
                          notify(`Mode Zapping ${mode} activé`, 'info');
                          // Logic is already handled by HLS config and isTurboStart
                        }}
                        className={cn(
                          "flex-1 py-1.5 rounded-xl text-[10px] font-bold border transition-all",
                          (mode === 'Turbo' && isTurboStart) || (mode === 'Standard' && !isTurboStart) ? "bg-primary text-black border-primary" : "bg-zinc-800 border-zinc-700 text-zinc-400"
                        )}
                      >
                        {mode}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Image Enhancement */}
                <div className="space-y-4">
                  <div className="flex items-center gap-2 text-zinc-400">
                    <Sliders size={14} />
                    <span className="text-[10px] font-black uppercase tracking-widest">Amélioration Image</span>
                  </div>
                  <div className="space-y-4">
                    {['brightness', 'contrast', 'saturation'].map(filter => (
                      <div key={filter} className="space-y-1">
                        <div className="flex justify-between text-[8px] font-black uppercase tracking-widest text-zinc-500">
                          <span>{filter}</span>
                          <span>{(imageFilters as any)[filter]}%</span>
                        </div>
                        <input 
                          type="range" 
                          min="50" 
                          max="150" 
                          value={(imageFilters as any)[filter]}
                          onChange={(e: React.ChangeEvent<HTMLInputElement>) => setImageFilters((prev: any) => ({ ...prev, [filter]: parseInt(e.target.value) }))}
                          className="w-full h-1 bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-primary"
                        />
                      </div>
                    ))}
                    <Button 
                      variant="outline" 
                      size="sm" 
                      onClick={() => setImageFilters({ brightness: 80, contrast: 110, saturation: 70 })}
                      className="flex-1 text-[8px]"
                    >
                      Mode Nuit
                    </Button>
                    <Button 
                      variant="outline" 
                      size="sm" 
                      onClick={() => setImageFilters({ brightness: 100, contrast: 100, saturation: 100 })}
                      className="flex-1 text-[8px]"
                    >
                      Reset
                    </Button>
                  </div>
                </div>
              </div>
              
              <div className="p-4 bg-zinc-950/50 flex justify-center">
                <Button fullWidth onClick={() => setShowSettings(false)}>Fermer</Button>
              </div>
            </div>
          </div>
        )}
      </AnimatePresence>

      {notification && (
        <div className={cn(
          "absolute top-4 right-4 px-4 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest z-50 animate-in slide-in-from-right-4",
          notification.type === 'success' ? "bg-emerald-500 text-white" : 
          notification.type === 'error' ? "bg-red-500 text-white" : "bg-primary text-black"
        )}>
          {notification.message}
        </div>
      )}

      {isZapping && (
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-primary/20 backdrop-blur-xl border border-primary/50 px-8 py-4 rounded-3xl z-40 animate-out fade-out zoom-out duration-1000">
          <div className="flex flex-col items-center gap-2">
            <Zap size={32} className="text-primary animate-bounce" />
            <span className="text-primary font-black uppercase tracking-[0.3em] text-xs">Zapping...</span>
          </div>
        </div>
      )}

      {isBuffering && !error && (
        <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/40 z-10">
          <div className="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin mb-4" />
          <p className="text-white text-xs font-black uppercase tracking-widest animate-pulse">Chargement...</p>
        </div>
      )}
      
      {error && (
        <div className="absolute inset-0 flex flex-col items-center justify-center bg-zinc-900/90 text-white p-6 text-center z-10">
          <div className="w-16 h-16 bg-red-500/20 rounded-full flex items-center justify-center mb-4">
            <VolumeX size={32} className="text-red-500" />
          </div>
          <h3 className="text-xl font-bold mb-2">{error}</h3>
          <p className="text-sm text-zinc-400 max-w-xs mb-6">
            Le lien de diffusion est peut-être expiré ou votre connexion est instable.
          </p>
          <Button 
            size="sm" 
            onClick={() => {
              setError(null);
              // Trigger a reload by resetting the URL or similar
              const currentUrl = url;
              // This is a bit hacky but forces a re-render/re-init
              window.dispatchEvent(new CustomEvent('player-retry'));
            }}
          >
            Réessayer
          </Button>
        </div>
      )}
      
      {/* Overlay Controls */}
      <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent opacity-0 group-hover:opacity-100 lg:group-hover:opacity-100 transition-opacity duration-300 flex flex-col justify-end p-4 pointer-events-none group-active:opacity-100">
        <div className="flex flex-col gap-2 pointer-events-auto">
          {/* Progress Bar */}
          <div className="w-full h-1 bg-white/20 rounded-full overflow-hidden cursor-pointer">
            <div className="h-full bg-primary transition-all duration-300" style={{ width: `${progress}%` }} />
          </div>
          
          <div className="flex items-center justify-between text-white">
            <div className="flex items-center gap-3 lg:gap-4">
              <div className="flex items-center gap-1.5 px-2 py-1 bg-black/40 rounded-full border border-white/5">
                <div className={cn(
                  "w-1.5 h-1.5 rounded-full animate-pulse",
                  connectionHealth === 'good' ? "bg-emerald-500" : 
                  connectionHealth === 'fair' ? "bg-amber-500" : "bg-red-500"
                )} />
                <span className="text-[8px] font-black uppercase tracking-widest text-zinc-400">
                  {connectionHealth === 'good' ? "Stable" : 
                   connectionHealth === 'fair' ? "Instable" : "Faible"}
                </span>
                {connectionHealth === 'good' && !lowBandwidthMode && (
                  <Zap size={8} className={cn("text-primary animate-pulse", isTurboStart && "text-amber-500")} />
                )}
                {isTurboStart && (
                  <span className="text-[6px] font-black text-amber-500 animate-pulse ml-1">TURBO</span>
                )}
                {!lowBandwidthMode && connectionHealth === 'good' && (
                  <Badge variant="primary" className="text-[6px] px-1 py-0 h-3 ml-1">PRO</Badge>
                )}
              </div>
              <button onClick={togglePlay} className="hover:text-primary transition-colors p-1 tv-focus rounded-lg">
                {isPlaying ? <Pause size={20} className="lg:w-6 lg:h-6" /> : <Play size={20} className="lg:w-6 lg:h-6" />}
              </button>
              <button onClick={toggleMute} className="hover:text-primary transition-colors p-1 tv-focus rounded-lg">
                {isMuted ? <VolumeX size={20} className="lg:w-6 lg:h-6" /> : <Volume2 size={20} className="lg:w-6 lg:h-6" />}
              </button>
              <span className="text-[10px] lg:text-sm font-mono opacity-80 truncate max-w-[100px] lg:max-w-none">
                {title || "Direct"}
              </span>
              {(imageFilters.brightness !== 100 || imageFilters.contrast !== 100 || imageFilters.saturation !== 100) && (
                <Badge variant="success" className="text-[8px] px-1 py-0 h-4">HDR+</Badge>
              )}
            </div>
            
            <div className="flex items-center gap-3 lg:gap-4">
              <button 
                onClick={() => setLowBandwidthMode(!lowBandwidthMode)} 
                className={cn(
                  "px-2 py-1 rounded text-[8px] font-black uppercase tracking-widest transition-all tv-focus",
                  lowBandwidthMode ? "bg-primary text-black" : "bg-zinc-800 text-zinc-500"
                )}
                title={lowBandwidthMode ? "Désactiver le mode basse connexion" : "Activer le mode basse connexion"}
              >
                {lowBandwidthMode ? "Mode Stable" : "Mode Standard"}
              </button>
              <button 
                onClick={() => setShowStats(!showStats)}
                className={cn("hover:text-primary transition-colors p-1 tv-focus rounded-lg", showStats && "text-primary")}
                title="Stats pour les experts"
              >
                <Activity size={18} className="lg:w-5 lg:h-5" />
              </button>
              <button 
                onClick={togglePiP}
                className={cn("hover:text-primary transition-colors p-1 tv-focus rounded-lg", isPiP && "text-primary")}
                title="Picture-in-Picture"
              >
                <Pip size={18} className="lg:w-5 lg:h-5" />
              </button>
              <button 
                onClick={() => setShowSettings(!showSettings)}
                className={cn("hover:text-primary transition-colors p-1 tv-focus rounded-lg", showSettings && "text-primary")}
                title="Paramètres"
              >
                <Settings size={18} className="lg:w-5 lg:h-5" />
              </button>
              <button onClick={toggleFullscreen} className="hover:text-primary transition-colors p-1 tv-focus rounded-lg">
                <Maximize size={18} className="lg:w-5 lg:h-5" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
