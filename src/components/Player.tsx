import React, { useEffect, useRef, useState } from 'react';
import Hls from 'hls.js';
import { Play, Pause, Volume2, VolumeX, Maximize, ChevronLeft, List, Settings } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

interface PlayerProps {
  url: string;
  onBack: () => void;
  channelName?: string;
}

export const Player: React.FC<PlayerProps> = ({ url, onBack, channelName = "Chaîne en direct" }) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isMuted, setIsMuted] = useState(false);
  const [showControls, setShowControls] = useState(true);
  const [isLoading, setIsLoading] = useState(true);
  const controlsTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    let hls: Hls | null = null;

    if (Hls.isSupported()) {
      hls = new Hls();
      hls.loadSource(url);
      hls.attachMedia(video);
      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        video.play().catch(() => setIsPlaying(false));
        setIsPlaying(true);
        setIsLoading(false);
      });
      hls.on(Hls.Events.ERROR, (_, data) => {
        if (data.fatal) {
          switch (data.type) {
            case Hls.ErrorTypes.NETWORK_ERROR:
              hls?.startLoad();
              break;
            case Hls.ErrorTypes.MEDIA_ERROR:
              hls?.recoverMediaError();
              break;
            default:
              hls?.destroy();
              break;
          }
        }
      });
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
      video.src = url;
      video.addEventListener('loadedmetadata', () => {
        video.play().catch(() => setIsPlaying(false));
        setIsPlaying(true);
        setIsLoading(false);
      });
    }

    return () => {
      if (hls) {
        hls.destroy();
      }
    };
  }, [url]);

  const togglePlay = () => {
    if (videoRef.current) {
      if (isPlaying) {
        videoRef.current.pause();
      } else {
        videoRef.current.play();
      }
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
    if (controlsTimeoutRef.current) {
      clearTimeout(controlsTimeoutRef.current);
    }
    controlsTimeoutRef.current = setTimeout(() => {
      setShowControls(false);
    }, 3000);
  };

  return (
    <div 
      className="fixed inset-0 bg-black flex items-center justify-center overflow-hidden group cursor-none"
      onMouseMove={handleMouseMove}
      style={{ cursor: showControls ? 'default' : 'none' }}
    >
      <video
        ref={videoRef}
        className="w-full h-full object-contain"
        playsInline
        onClick={togglePlay}
      />

      {isLoading && (
        <div className="absolute inset-0 flex items-center justify-center bg-black/50">
          <div className="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin" />
        </div>
      )}

      <AnimatePresence>
        {showControls && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-black/60 flex flex-col justify-between p-6"
          >
            {/* Top Bar */}
            <div className="flex items-center justify-between">
              <button 
                onClick={onBack}
                className="flex items-center gap-2 text-white/80 hover:text-white transition-colors bg-white/10 px-4 py-2 rounded-full backdrop-blur-md"
              >
                <ChevronLeft size={20} />
                <span className="text-sm font-bold">Retour</span>
              </button>
              
              <div className="flex flex-col items-end">
                <h2 className="text-xl font-black text-white tracking-tight">{channelName}</h2>
                <span className="text-[10px] text-primary font-bold uppercase tracking-widest">En Direct</span>
              </div>
            </div>

            {/* Bottom Bar */}
            <div className="flex items-center justify-between gap-6">
              <div className="flex items-center gap-4">
                <button 
                  onClick={togglePlay}
                  className="w-12 h-12 flex items-center justify-center bg-primary text-black rounded-full hover:scale-110 transition-transform"
                >
                  {isPlaying ? <Pause size={24} fill="currentColor" /> : <Play size={24} fill="currentColor" className="ml-1" />}
                </button>

                <button 
                  onClick={toggleMute}
                  className="text-white/80 hover:text-white transition-colors"
                >
                  {isMuted ? <VolumeX size={24} /> : <Volume2 size={24} />}
                </button>
              </div>

              <div className="flex items-center gap-4">
                <button className="text-white/80 hover:text-white transition-colors">
                  <List size={24} />
                </button>
                <button className="text-white/80 hover:text-white transition-colors">
                  <Settings size={24} />
                </button>
                <button 
                  onClick={() => videoRef.current?.requestFullscreen()}
                  className="text-white/80 hover:text-white transition-colors"
                >
                  <Maximize size={24} />
                </button>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};
