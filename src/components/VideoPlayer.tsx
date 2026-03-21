import React, { useEffect, useRef, useState } from 'react';
import Hls from 'hls.js';
import { Maximize, Minimize, Play, Pause, Volume2, VolumeX, Settings, SkipBack, SkipForward } from 'lucide-react';

interface PlayerProps {
  url: string;
  title?: string;
}

export const VideoPlayer: React.FC<PlayerProps> = ({ url, title }) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isMuted, setIsMuted] = useState(false);
  const [progress, setProgress] = useState(0);
  const [duration, setDuration] = useState(0);

  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!videoRef.current || !url) return;

    const video = videoRef.current;
    let hls: Hls | null = null;
    setError(null);

    if (Hls.isSupported()) {
      hls = new Hls({
        enableWorker: true,
        lowLatencyMode: true,
        manifestLoadingMaxRetry: 2,
      });
      
      hls.loadSource(url);
      hls.attachMedia(video);
      
      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        video.play().catch((err) => {
          console.warn("Autoplay blocked or failed:", err);
        });
        setIsPlaying(true);
      });

      hls.on(Hls.Events.ERROR, (event, data) => {
        if (data.fatal) {
          setError("Flux vidéo non disponible");
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
        video.play().catch(() => {});
        setIsPlaying(true);
      });
      video.addEventListener('error', () => {
        setError("Erreur de chargement du flux");
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
    if (videoRef.current) {
      if (document.fullscreenElement) {
        document.exitFullscreen();
      } else {
        videoRef.current.parentElement?.requestFullscreen();
      }
    }
  };

  return (
    <div className="relative group bg-black rounded-xl overflow-hidden aspect-video shadow-2xl">
      <video
        ref={videoRef}
        className="w-full h-full cursor-pointer"
        onTimeUpdate={handleTimeUpdate}
        onLoadedMetadata={handleLoadedMetadata}
        onClick={togglePlay}
        playsInline
      />
      
      {error && (
        <div className="absolute inset-0 flex flex-col items-center justify-center bg-zinc-900/90 text-white p-6 text-center z-10">
          <div className="w-16 h-16 bg-red-500/20 rounded-full flex items-center justify-center mb-4">
            <VolumeX size={32} className="text-red-500" />
          </div>
          <h3 className="text-xl font-bold mb-2">{error}</h3>
          <p className="text-sm text-zinc-400 max-w-xs">
            Le lien de diffusion est peut-être expiré ou temporairement inaccessible.
          </p>
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
              <button onClick={togglePlay} className="hover:text-primary transition-colors p-1 tv-focus rounded-lg">
                {isPlaying ? <Pause size={20} className="lg:w-6 lg:h-6" /> : <Play size={20} className="lg:w-6 lg:h-6" />}
              </button>
              <button onClick={toggleMute} className="hover:text-primary transition-colors p-1 tv-focus rounded-lg">
                {isMuted ? <VolumeX size={20} className="lg:w-6 lg:h-6" /> : <Volume2 size={20} className="lg:w-6 lg:h-6" />}
              </button>
              <span className="text-[10px] lg:text-sm font-mono opacity-80 truncate max-w-[100px] lg:max-w-none">
                {title || "Direct"}
              </span>
            </div>
            
            <div className="flex items-center gap-3 lg:gap-4">
              <button className="hover:text-primary transition-colors p-1 tv-focus rounded-lg">
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
