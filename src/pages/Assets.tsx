import React, { useRef } from 'react';
import { Header } from '../components/Header';
import { Footer } from '../components/Footer';
import { Card, Button } from '../components/ui';
import { Download, Image as ImageIcon, FileCode } from 'lucide-react';
import { motion } from 'motion/react';

export const Assets = () => {
  const svgRef = useRef<SVGSVGElement>(null);

  const downloadPNG = (size: number) => {
    const svg = svgRef.current;
    if (!svg) return;

    const canvas = document.createElement('canvas');
    canvas.width = size;
    canvas.height = size;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const svgData = new XMLSerializer().serializeToString(svg);
    const svgBlob = new Blob([svgData], { type: 'image/svg+xml;charset=utf-8' });
    const url = URL.createObjectURL(svgBlob);

    const img = new Image();
    img.onload = () => {
      ctx.clearRect(0, 0, size, size);
      ctx.drawImage(img, 0, 0, size, size);
      URL.revokeObjectURL(url);
      
      const pngUrl = canvas.toDataURL('image/png');
      const downloadLink = document.createElement('a');
      downloadLink.href = pngUrl;
      downloadLink.download = `sky-player-logo-${size}x${size}.png`;
      document.body.appendChild(downloadLink);
      downloadLink.click();
      document.body.removeChild(downloadLink);
    };
    img.src = url;
  };

  const downloadSVG = () => {
    const svg = svgRef.current;
    if (!svg) return;
    const svgData = new XMLSerializer().serializeToString(svg);
    const svgBlob = new Blob([svgData], { type: 'image/svg+xml;charset=utf-8' });
    const url = URL.createObjectURL(svgBlob);
    const downloadLink = document.createElement('a');
    downloadLink.href = url;
    downloadLink.download = 'sky-player-logo.svg';
    document.body.appendChild(downloadLink);
    downloadLink.click();
    document.body.removeChild(downloadLink);
    URL.revokeObjectURL(url);
  };

  return (
    <div className="min-h-screen bg-black text-white selection:bg-primary/30">
      <Header />
      
      <main className="max-w-5xl mx-auto px-6 py-12 md:py-20 space-y-12">
        <div className="text-center space-y-4">
          <motion.h1 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="text-5xl font-black tracking-tighter"
          >
            Kit <span className="text-primary">Média</span>
          </motion.h1>
          <p className="text-zinc-500">Téléchargez les ressources graphiques officielles pour les Stores et le marketing.</p>
        </div>

        <div className="grid md:grid-cols-2 gap-12 items-center">
          <div className="flex justify-center">
            <div className="p-12 bg-zinc-900 rounded-[3rem] border border-zinc-800 shadow-2xl">
              <svg 
                ref={svgRef}
                width="200" 
                height="200" 
                viewBox="0 0 100 100" 
                fill="none" 
                xmlns="http://www.w3.org/2000/svg"
              >
                <rect width="100" height="100" rx="24" fill="#0EA5E9" />
                <path d="M35 30L70 50L35 70V30Z" fill="white" stroke="white" strokeWidth="4" strokeLinejoin="round" />
                <path d="M25 75C40 75 50 65 75 65" stroke="white" strokeWidth="6" strokeLinecap="round" opacity="0.3" />
              </svg>
            </div>
          </div>

          <div className="space-y-6">
            <Card className="p-6 space-y-4">
              <div className="flex items-center gap-3 text-primary">
                <ImageIcon size={20} />
                <h3 className="font-bold">Formats PNG (Haute Résolution)</h3>
              </div>
              <p className="text-sm text-zinc-500">Idéal pour le Play Store, Samsung Store et LG Content Store.</p>
              <div className="grid grid-cols-2 gap-3">
                <Button variant="secondary" onClick={() => downloadPNG(512)} className="gap-2">
                  <Download size={14} /> 512x512
                </Button>
                <Button variant="secondary" onClick={() => downloadPNG(1024)} className="gap-2">
                  <Download size={14} /> 1024x1024
                </Button>
              </div>
            </Card>

            <Card className="p-6 space-y-4">
              <div className="flex items-center gap-3 text-primary">
                <FileCode size={20} />
                <h3 className="font-bold">Format Vectoriel (SVG)</h3>
              </div>
              <p className="text-sm text-zinc-500">Format original pour une qualité infinie sans perte.</p>
              <Button variant="secondary" onClick={downloadSVG} className="w-full gap-2">
                <Download size={14} /> Télécharger le SVG
              </Button>
            </Card>
          </div>
        </div>

        <section className="bg-zinc-900/50 border border-zinc-800 rounded-[2.5rem] p-8 text-center space-y-4">
          <h3 className="font-bold text-lg">Besoin d'aide pour la soumission ?</h3>
          <p className="text-sm text-zinc-500 max-w-2xl mx-auto">
            Chaque Store a des exigences spécifiques. Le format 512x512 est généralement requis pour l'icône de l'application, tandis que le 1024x1024 est utilisé pour les bannières promotionnelles.
          </p>
        </section>
      </main>

      <Footer />
    </div>
  );
};
