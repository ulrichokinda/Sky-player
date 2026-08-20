import React from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { X, ShieldCheck, TrendingUp, Zap, ChevronRight } from 'lucide-react';
import { Button, Badge } from './ui';
import { Link } from 'react-router-dom';

interface ResellerPricingModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const ResellerPricingModal = ({ isOpen, onClose }: ResellerPricingModalProps) => {
  const resellerPlans = [
    { id: '10cr', name: '10 Crédits', price: 12750, credits: 10, desc: 'Idéal pour débuter' },
    { id: '20cr', name: '20 Crédits', price: 23750, credits: 20, desc: 'Le choix populaire' },
    { id: '50cr', name: '50 Crédits', price: 45750, credits: 50, desc: 'Meilleure valeur' }
  ];

  if (!isOpen) return null;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        <motion.div 
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="absolute inset-0 bg-black/80 backdrop-blur-sm"
          onClick={onClose}
        />
        <motion.div 
          initial={{ opacity: 0, scale: 0.95, y: 20 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: 20 }}
          className="relative bg-zinc-950 border border-zinc-800 rounded-3xl w-full max-w-4xl max-h-[90vh] overflow-y-auto custom-scrollbar shadow-2xl z-10"
        >
          <div className="sticky top-0 bg-zinc-950/80 backdrop-blur-md z-20 flex items-center justify-between p-6 border-b border-zinc-800">
            <div>
              <Badge variant="primary" className="mb-2">Espace Pro</Badge>
              <h2 className="text-2xl font-black italic">Tarifs Revendeur</h2>
            </div>
            <button 
              onClick={onClose}
              className="p-2 bg-zinc-900 hover:bg-zinc-800 rounded-full transition-colors text-zinc-400 hover:text-white"
            >
              <X size={20} />
            </button>
          </div>

          <div className="p-6 md:p-8 space-y-10">
            <div className="text-center space-y-4 max-w-2xl mx-auto">
              <p className="text-zinc-400">
                L'inscription est gratuite, mais vous trouverez ci-dessous les tarifs de recharge de crédits pour avoir une visibilité totale sur votre activité future. Bénéficiez des tarifs régressifs conçus pour maximiser vos marges.
              </p>
            </div>

            <div className="grid md:grid-cols-3 gap-6">
              {resellerPlans.map((plan) => (
                <div key={plan.id} className="p-6 bg-zinc-900/40 border border-zinc-800 rounded-2xl flex flex-col items-center text-center space-y-4 relative overflow-hidden group hover:border-primary/50 transition-all">
                  <div className="w-12 h-12 bg-primary/10 rounded-xl flex items-center justify-center text-primary group-hover:scale-110 transition-transform">
                    <TrendingUp size={24} />
                  </div>
                  <div>
                    <h3 className="text-xl font-bold">{plan.name}</h3>
                    <p className="text-xs text-zinc-500 uppercase tracking-wider mt-1">{plan.desc}</p>
                  </div>
                  <div className="pt-2">
                    <span className="text-3xl font-black">{plan.price.toLocaleString()}</span>
                    <span className="text-sm text-zinc-500 ml-1">FCFA</span>
                  </div>
                  <div className="w-full h-px bg-zinc-800/50" />
                  <p className="text-sm text-primary font-bold">
                    Soit {(plan.price / plan.credits).toFixed()} FCFA le crédit
                  </p>
                </div>
              ))}
            </div>

            <div className="p-6 md:p-8 bg-primary/10 border border-primary/20 rounded-3xl text-center space-y-6">
              <ShieldCheck className="text-primary mx-auto" size={40} />
              <div className="space-y-2">
                <h3 className="text-2xl font-black">Prêt à développer vos revenus ?</h3>
                <p className="text-zinc-400 max-w-xl mx-auto">
                  Créez votre compte gratuitement dès aujourd'hui pour accéder à votre panel reseller. L'activation des appareils se fait de manière instantanée, vous permettant de revendre librement avec intelligence.
                </p>
              </div>
              <div className="flex justify-center items-center pt-4">
                <Link to="/register" className="w-full max-w-sm block" onClick={onClose}>
                  <Button size="lg" className="w-full px-2 sm:px-4 text-xs sm:text-sm font-black h-14 sm:h-16 uppercase tracking-widest flex items-center justify-center">
                    <span>M'inscrire</span>
                    <ChevronRight className="shrink-0 ml-1" size={18} />
                  </Button>
                </Link>
              </div>
            </div>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
};
