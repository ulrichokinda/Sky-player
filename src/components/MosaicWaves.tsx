import React, { useRef, useEffect, useCallback } from 'react';

export interface MosaicWavesProps {
  width?: string | number;
  height?: string | number;
  className?: string;
  children?: React.ReactNode;
  pitch?: number;
  fill?: number;
  shape?: 'square' | 'dot';
  feather?: number;
  speed?: number;
  warp?: number;
  warpScale?: number;
  detailScale?: number;
  waveScale?: number;
  falloff?: number;
  brightness?: number;
  ambient?: number;
  color?: string;
  hotColor?: string;
  backgroundColor?: string;
  gamma?: number;
  vignette?: number;
  opacity?: number;
  cursorInteraction?: boolean;
  cursorPull?: number;
  cursorGlow?: number;
  cursorReach?: number;
  adaptiveQuality?: boolean;
  targetFps?: number;
  dpr?: number;
  paused?: boolean;
}

function hexToRgb(hex: string): [number, number, number] {
  const h = hex.replace('#', '');
  return [
    parseInt(h.substring(0, 2), 16) / 255,
    parseInt(h.substring(2, 4), 16) / 255,
    parseInt(h.substring(4, 6), 16) / 255,
  ];
}

// ─── Canvas 2D fallback (no WebGL needed) ──────────────
function renderCanvas2D(
  canvas: HTMLCanvasElement,
  opts: {
    pitch: number; fill: number; shape: string; speed: number;
    color: string; hotColor: string; backgroundColor: string;
    cursorInteraction: boolean; cursorX: number; cursorY: number;
    time: number; paused: boolean;
  }
) {
  const ctx = canvas.getContext('2d');
  if (!ctx) return;
  const w = canvas.width;
  const h = canvas.height;
  const t = opts.time * opts.speed;

  ctx.fillStyle = opts.backgroundColor;
  ctx.fillRect(0, 0, w, h);

  const cols = Math.ceil(opts.pitch * (w / h));
  const rows = Math.ceil(opts.pitch);
  const tileW = w / cols;
  const tileH = h / rows;
  const tileSize = Math.min(tileW, tileH) * opts.fill * 0.55;

  const cRgb = hexToRgb(opts.color);
  const hRgb = hexToRgb(opts.hotColor);

  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) {
      const cx = (c + 0.5) * tileW;
      const cy = (r + 0.5) * tileH;

      // Multi-layer wave for organic motion
      const wave1 = Math.sin(c * 0.5 + t * 2.0) * Math.cos(r * 0.4 + t * 1.5);
      const wave2 = Math.sin(c * 0.3 - t * 1.2 + r * 0.7) * 0.5;
      const wave3 = Math.cos((c + r) * 0.2 + t * 0.8) * 0.3;
      const wave = (wave1 + wave2 + wave3 + 1.8) / 3.6; // normalized 0..1

      // Mouse influence
      let mouseFactor = 0;
      if (opts.cursorInteraction) {
        const dx = (cx / w) - opts.cursorX;
        const dy = (cy / h) - opts.cursorY;
        const dist = Math.sqrt(dx * dx + dy * dy);
        mouseFactor = Math.max(0, 1 - dist / 0.35) * 0.5;
      }

      const intensity = Math.min(1, Math.pow(wave + mouseFactor, 1.5));
      const r2 = Math.round(cRgb[0] + (hRgb[0] - cRgb[0]) * intensity);
      const g = Math.round(cRgb[1] + (hRgb[1] - cRgb[1]) * intensity);
      const b = Math.round(cRgb[2] + (hRgb[2] - cRgb[2]) * intensity);

      ctx.globalAlpha = intensity * 0.95 + 0.05;
      ctx.fillStyle = `rgb(${r2},${g},${b})`;

      if (opts.shape === 'dot') {
        ctx.beginPath();
        ctx.arc(cx, cy, tileSize, 0, Math.PI * 2);
        ctx.fill();
      } else {
        ctx.fillRect(cx - tileSize, cy - tileSize, tileSize * 2, tileSize * 2);
      }
    }
  }
  ctx.globalAlpha = 1;
}

export const MosaicWaves: React.FC<MosaicWavesProps> = ({
  width = '100%',
  height = '100%',
  className,
  children,
  pitch = 4,
  fill = 0.5,
  shape = 'square',
  feather = 0.15,
  speed = 1,
  warp = 0.375,
  warpScale = 3,
  detailScale = 7,
  waveScale = 4,
  falloff = 5,
  brightness = 5,
  ambient = 0,
  color = '#3366ff',
  hotColor = '#bfd4ff',
  backgroundColor = '#0a0a0a',
  gamma = 2.2,
  vignette = 0,
  opacity = 1,
  cursorInteraction = true,
  cursorPull = 0.35,
  cursorGlow = 0.25,
  cursorReach = 0.28,
  adaptiveQuality = true,
  targetFps = 60,
  dpr = 2,
  paused = false,
}) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const mouseRef = useRef<[number, number]>([0.5, 0.5]);
  const rafRef = useRef<number>(0);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const d = Math.min(dpr, window.devicePixelRatio || 1);
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * d;
    canvas.height = rect.height * d;

    let startTime = performance.now() / 1000;

    const render = () => {
      const time = performance.now() / 1000 - startTime;
      renderCanvas2D(canvas, {
        pitch, fill, shape: shape as string, speed,
        color, hotColor, backgroundColor,
        cursorInteraction,
        cursorX: mouseRef.current[0],
        cursorY: mouseRef.current[1],
        time, paused,
      });
      if (!paused) {
        rafRef.current = requestAnimationFrame(render);
      }
    };

    rafRef.current = requestAnimationFrame(render);

    const handleMouseMove = (e: MouseEvent) => {
      if (!cursorInteraction) return;
      const r = canvas.getBoundingClientRect();
      mouseRef.current = [
        (e.clientX - r.left) / r.width,
        1 - (e.clientY - r.top) / r.height,
      ];
    };

    const handleResize = () => {
      const d2 = Math.min(dpr, window.devicePixelRatio || 1);
      const r2 = canvas.getBoundingClientRect();
      canvas.width = r2.width * d2;
      canvas.height = r2.height * d2;
    };

    if (cursorInteraction) canvas.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('resize', handleResize);

    return () => {
      cancelAnimationFrame(rafRef.current);
      canvas.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('resize', handleResize);
    };
  }, [pitch, fill, shape, speed, color, hotColor, backgroundColor, cursorInteraction, paused, dpr]);

  return (
    <div
      className={className}
      style={{ position: 'relative', width, height, overflow: 'hidden' }}
    >
      <canvas
        ref={canvasRef}
        style={{ position: 'absolute', inset: 0, width: '100%', height: '100%' }}
      />
      {children && (
        <div style={{ position: 'relative', zIndex: 1 }}>{children}</div>
      )}
    </div>
  );
};

export default MosaicWaves;
