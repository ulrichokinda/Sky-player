import React, { useState } from 'react';
import { X, Grid, Layout, ChevronLeft, Tv } from 'lucide-react';
import { motion } from 'motion/react';
import { Channel } from '../lib/playlistParser';

interface MultiScreenPlayerProps {
  channels: Channel[];
  onBack: () => void;
}

export const MultiScreenPlayer: React.FC<MultiScreenPlayerProps> = ({ channels, onBack }) => {
  const [gridSize, setGridSize] = useState<2 | 4>(4);
  const [selectedChannels, setSelectedChannels] = useState<(Channel | null)[]>(new Array(gridSize).fill(null));

  const handleChannelSelect = (channel: Channel, index: number) => {
    const newSelected = [...selectedChannels];
    newSelected[index] = channel;
    setSelectedChannels(newSelected);
  };

  return (
    <div className="fixed inset-0 bg-black z-[100] flex flex-col">
      {/* Header */}
      <div className="p-4 bg-zinc-900 border-b border-white/10 flex items-center justify-between">
        <div className="flex items-center gap-4">
          <button onClick={onBack} className="p-2 hover:bg-white/10 rounded-xl transition-colors">
            <ChevronLeft size={24} />
          </button>
          <h2 className="text-xl font-black italic tracking-tighter">Multi-Écran Pro</h2>
        </div>
        <div className="flex items-center gap-2">
          <button 
            onClick={() => {
              setGridSize(2);
              setSelectedChannels(new Array(2).fill(null));
            }}
            className={`p-2 rounded-xl transition-all ${gridSize === 2 ? 'bg-primary text-black' : 'bg-white/10 text-white'}`}
          >
            <Layout size={20} />
          </button>
          <button 
            onClick={() => {
              setGridSize(4);
              setSelectedChannels(new Array(4).fill(null));
            }}
            className={`p-2 rounded-xl transition-all ${gridSize === 4 ? 'bg-primary text-black' : 'bg-white/10 text-white'}`}
          >
            <Grid size={20} />
          </button>
        </div>
      </div>

      {/* Grid */}
      <div className={`flex-1 grid ${gridSize === 2 ? 'grid-cols-2' : 'grid-cols-2 grid-rows-2'} gap-1 p-1 bg-zinc-950`}>
        {selectedChannels.map((channel, i) => (
          <div key={i} className="relative bg-zinc-900 border border-white/5 flex items-center justify-center overflow-hidden group">
            {channel ? (
              <>
                <video 
                  src={channel.url} 
                  autoPlay 
                  muted 
                  className="w-full h-full object-contain"
                  onMouseOver={(e) => (e.currentTarget.muted = false)}
                  onMouseOut={(e) => (e.currentTarget.muted = true)}
                />
              <div className="absolute top-2 left-2 bg-black/60 backdrop-blur-md px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-widest opacity-0 group-hover:opacity-100 transition-opacity text-white">
                  {channel.name}
                </div>
                <button 
                  onClick={() => handleChannelSelect(null as any, i)}
                  className="absolute top-2 right-2 p-2 bg-red-500/20 hover:bg-red-500 text-red-500 hover:text-white rounded-full opacity-0 group-hover:opacity-100 transition-all border border-red-500/20"
                >
                  <X size={14} />
                </button>
              </>
            ) : (
              <div className="flex flex-col items-center gap-4 p-4 text-center">
                <div className="w-16 h-16 rounded-3xl bg-white/5 flex items-center justify-center text-zinc-700">
                  <Tv size={32} />
                </div>
                <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Choisir une chaîne</p>
                <select 
                  className="bg-zinc-800 border border-white/10 rounded-xl px-4 py-2 text-xs focus:outline-none focus:border-primary text-white w-full max-w-[150px]"
                  onChange={(e) => handleChannelSelect(channels[parseInt(e.target.value)], i)}
                >
                  <option value="">Sélectionner...</option>
                  {channels.map((c, idx) => (
                    <option key={idx} value={idx}>{c.name}</option>
                  ))}
                </select>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};
