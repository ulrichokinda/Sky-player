import React, { useEffect, useRef, useState } from 'react';
import Hls from 'hls.js';
import { Play, Pause, Volume2, VolumeX, Maximize, ChevronLeft, List, Settings, SkipBack, SkipForward, Search, X, Star } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { Channel } from '../lib/playlistParser';

interface PlayerProps {
  url: string;
  onBack: () => void;
  channelName?: string;
  onNext?: () => void;
  onPrev?: () => void;
  channels?: Channel[];
  onChannelSelect?: (index: number) => void;
}

export const Player: React.FC<PlayerProps> = ({ 
  url, 
  onBack, 
  channelName = "Chaîne en direct", 
  onNext, 
  onPrev,
  channels = [],
  onChannelSelect
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
  const controlsTimeoutRef = useRef<NodeJS.Timeout | null>(null);

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
    if (!video) return;

    let hls: Hls | null = null;
    setIsLoading(true);

    if (url.includes('.m3u8') || url.includes('playlist.m3u8')) {
      if (Hls.isSupported()) {
        hls = new Hls({
          enableWorker: true,
          lowLatencyMode: true,
        });
        hls.loadSource(url);
        hls.attachMedia(video);
        hls.on(Hls.Events.MANIFEST_PARSED, () => {
          video.play().catch(() => setIsPlaying(false));
          setIsPlaying(true);
          setIsLoading(false);
        });
      } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
        video.src = url;
        video.addEventListener('loadedmetadata', () => {
          video.play().catch(() => setIsPlaying(false));
          setIsPlaying(true);
          setIsLoading(false);
        });
      }
    } else {
      // Direct MP4/TS/MKV link
      video.src = url;
      video.play().catch(() => setIsPlaying(false));
      setIsPlaying(true);
      setIsLoading(false);
    }

    return () => {
      if (hls) hls.destroy();
    };
  }, [url]);

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

      <AnimatePresence>
        {showControls && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 bg-gradient-to-t from-black/90 via-transparent to-black/80 flex flex-col justify-between p-8 z-40"
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
                    <span className="text-[10px] text-zinc-400 font-bold uppercase tracking-widest">En Direct • 4K Ultra HD</span>
                  </div>
                </div>
              </div>
              
              <div className="flex items-center gap-3">
                <button 
                  onClick={() => toggleFavorite(url)}
                  className={`p-3 rounded-2xl backdrop-blur-xl transition-all ${
                    favorites.includes(url) ? 'bg-yellow-500 text-black' : 'bg-white/10 text-white hover:bg-white/20'
                  }`}
                >
                  <Star size={20} fill={favorites.includes(url) ? "currentColor" : "none"} />
                </button>
                <button 
                  onClick={() => setShowChannelList(true)}
                  className="p-3 bg-white/10 hover:bg-white/20 rounded-2xl backdrop-blur-xl transition-all flex items-center gap-2"
                >
                  <List size={20} />
                  <span className="text-xs font-black uppercase tracking-widest hidden md:block">Chaînes</span>
                </button>
              </div>
            </div>

            {/* Bottom Bar */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-6">
                <div className="flex items-center gap-2">
                  <button onClick={onPrev} className="p-2 text-white/50 hover:text-white transition-colors"><SkipBack size={28} /></button>
                  <button 
                    onClick={togglePlay}
                    className="w-16 h-16 flex items-center justify-center bg-primary text-black rounded-3xl hover:scale-110 transition-transform shadow-2xl shadow-primary/20"
                  >
                    {isPlaying ? <Pause size={32} fill="currentColor" /> : <Play size={32} fill="currentColor" className="ml-1" />}
                  </button>
                  <button onClick={onNext} className="p-2 text-white/50 hover:text-white transition-colors"><SkipForward size={28} /></button>
                </div>

                <div className="h-10 w-[1px] bg-white/10 mx-2" />

                <button onClick={toggleMute} className="text-white/70 hover:text-white transition-colors">
                  {isMuted ? <VolumeX size={24} /> : <Volume2 size={24} />}
                </button>
              </div>

              <div className="flex items-center gap-4">
                <div className="flex bg-white/5 p-1 rounded-xl backdrop-blur-md">
                  {(['contain', 'cover', 'fill'] as const).map((ratio) => (
                    <button
                      key={ratio}
                      onClick={() => setAspectRatio(ratio)}
                      className={`px-3 py-1.5 rounded-lg text-[10px] font-black uppercase tracking-widest transition-all ${
                        aspectRatio === ratio ? 'bg-primary text-black' : 'text-zinc-500 hover:text-white'
                      }`}
                    >
                      {ratio}
                    </button>
                  ))}
                </div>
                <button 
                  onClick={() => videoRef.current?.requestFullscreen()}
                  className="p-3 bg-white/10 hover:bg-white/20 rounded-xl transition-all"
                >
                  <Maximize size={20} />
                </button>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

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

              <div className="p-4">
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
                {filteredChannels.map((channel, index) => (
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
                      <p className="font-bold text-sm line-clamp-1">{channel.name}</p>
                      {channel.group && <p className={`text-[10px] uppercase font-black tracking-widest ${channel.url === url ? 'text-black/60' : 'text-zinc-600'}`}>{channel.group}</p>}
                    </div>
                  </button>
                ))}
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
};
