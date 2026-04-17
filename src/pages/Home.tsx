import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Logo } from '../components/Logo';
import { Button, Badge } from '../components/ui';
import { Footer } from '../components/Footer';
import { CheckCircle2, Menu, X, LayoutDashboard, UserPlus, Download, LogIn, Zap, Shield, Cpu, Monitor, Activity, Sliders, Calendar, Grid, RotateCw, Lock, ChevronRight } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { auth, onAuthStateChanged } from '../firebase';

export const Home = () => {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [user, setUser] = useState<any>(null);
  const navigate = useNavigate();

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      setUser(user);
    });
    return () => unsubscribe();
  }, []);

  const handlePlanClick = (e: React.MouseEvent, plan: string) => {
    if (!user) {
      e.preventDefault();
      navigate(`/register?redirect=/payment&plan=${plan}`);
    }
  };

  return (
    <div className="min-h-screen bg-black text-white overflow-x-hidden">
      <nav className="flex items-center justify-between py-6 px-4 md:px-8 sticky top-0 bg-black/80 backdrop-blur-md z-50 border-b border-white/5">
        <Logo size={40} />
        
        {/* Desktop Nav */}
        <div className="hidden md:flex items-center gap-4">
          {user ? (
            <Link to="/dashboard">
              <Button variant="ghost" icon={LayoutDashboard}>Tableau de Bord</Button>
            </Link>
          ) : (
            <>
              <Link to="/login">
                <Button variant="ghost" icon={LogIn}>Connexion</Button>
              </Link>
              <Link to="/register">
                <Button variant="outline" className="border-primary/50 text-primary hover:bg-primary hover:text-black">S'inscrire</Button>
              </Link>
            </>
          )}
          <Link to="/payment">
            <Button>Devenir Revendeur</Button>
          </Link>
        </div>

        {/* Mobile Menu Button */}
        <button 
          className="md:hidden p-2 text-primary hover:bg-white/5 rounded-xl transition-colors focus-visible:ring-2 focus-visible:ring-primary"
          onClick={() => setIsMenuOpen(!isMenuOpen)}
        >
          {isMenuOpen ? <X size={28} /> : <Menu size={28} />}
        </button>
      </nav>

      {/* Mobile Menu Overlay */}
      <AnimatePresence>
        {isMenuOpen && (
          <motion.div 
            initial={{ opacity: 0, x: '100%' }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: '100%' }}
            className="fixed inset-0 z-40 bg-black pt-24 px-6 md:hidden"
          >
            <div className="flex flex-col gap-4">
              {user ? (
                <Link to="/dashboard" onClick={() => setIsMenuOpen(false)}>
                  <div className="flex items-center gap-4 p-5 bg-zinc-900 rounded-2xl border border-zinc-800">
                    <LayoutDashboard className="text-primary" />
                    <div className="text-left">
                      <p className="font-bold">Tableau de Bord</p>
                      <p className="text-xs text-zinc-500">Gérez vos clients et crédits</p>
                    </div>
                  </div>
                </Link>
              ) : (
                <>
                  <Link to="/login" onClick={() => setIsMenuOpen(false)}>
                    <div className="flex items-center gap-4 p-5 bg-zinc-900 rounded-2xl border border-zinc-800">
                      <LogIn className="text-primary" />
                      <div className="text-left">
                        <p className="font-bold">Connexion</p>
                        <p className="text-xs text-zinc-500">Accédez à votre compte</p>
                      </div>
                    </div>
                  </Link>
                  <Link to="/register" onClick={() => setIsMenuOpen(false)}>
                    <div className="flex items-center gap-4 p-5 bg-zinc-900 rounded-2xl border border-zinc-800">
                      <UserPlus className="text-primary" />
                      <div className="text-left">
                        <p className="font-bold">S'inscrire</p>
                        <p className="text-xs text-zinc-500">Créer un compte revendeur</p>
                      </div>
                    </div>
                  </Link>
                </>
              )}
              <Link to="/payment" onClick={() => setIsMenuOpen(false)}>
                <div className="flex items-center gap-4 p-5 bg-primary/10 rounded-2xl border border-primary/20">
                  <UserPlus className="text-primary" />
                  <div className="text-left">
                    <p className="font-bold text-primary">Devenir Revendeur</p>
                    <p className="text-xs text-zinc-400">Rejoignez notre réseau</p>
                  </div>
                </div>
              </Link>
              <Button fullWidth size="lg" icon={Download} className="mt-4" onClick={() => window.location.href = 'https://firebasestorage.googleapis.com/v0/b/skyplayer-60634.firebasestorage.app/o/SkyPlayer.apk?alt=media'}>Télécharger l'APK</Button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <main className="max-w-5xl mx-auto mt-12 md:mt-20 text-center space-y-12 px-4 md:px-6">
        
        {/* Titre d'accroche */}
        <div className="space-y-4">
          <h1 className="text-4xl sm:text-5xl md:text-7xl font-black tracking-tighter leading-tight">
            L'expérience IPTV <span className="text-primary">Premium</span><br className="hidden sm:block"/>
            pour l'Afrique.
          </h1>
          <p className="text-zinc-400 text-lg md:text-xl max-w-2xl mx-auto leading-relaxed">
            Sky Player est le lecteur IPTV le plus fluide et stable du marché. 
            Connectez vos propres serveurs et profitez d'une interface pensée pour vos habitudes.
          </p>
        </div>

        {/* Compatibility Badges */}
        <div className="flex flex-wrap justify-center items-center gap-3 md:gap-6 pt-4 pb-2">
          <div className="flex items-center gap-2 px-4 py-2 bg-zinc-900/60 border border-zinc-800 rounded-full text-zinc-300 text-sm font-medium">
            <Monitor size={18} className="text-primary" />
            <span>LG webOS</span>
          </div>
          <div className="flex items-center gap-2 px-4 py-2 bg-zinc-900/60 border border-zinc-800 rounded-full text-zinc-300 text-sm font-medium">
            <Monitor size={18} className="text-[#1428A0]" />
            <span>Samsung Tizen</span>
          </div>
          <div className="flex items-center gap-2 px-4 py-2 bg-zinc-900/60 border border-zinc-800 rounded-full text-zinc-300 text-sm font-medium">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" className="text-[#3DDC84]">
              <path d="M17.523 15.3414C17.523 15.3414 17.522 15.3414 17.521 15.3414L17.523 15.3414ZM16.324 15.3414C16.323 15.3414 16.323 15.3414 16.324 15.3414L16.324 15.3414ZM16.038 10.9634C15.895 10.9634 15.776 11.0774 15.776 11.2184V13.8824C15.776 14.0244 15.894 14.1384 16.038 14.1384H16.038C16.182 14.1384 16.3 14.0234 16.3 13.8824V11.2194C16.3 11.0774 16.182 10.9634 16.038 10.9634M6.478 15.3404C6.477 15.3404 6.476 15.3404 6.475 15.3404L6.478 15.3404ZM7.962 10.9634C7.818 10.9634 7.7 11.0774 7.7 11.2184V13.8824C7.7 14.0244 7.818 14.1384 7.962 14.1384H7.962C8.105 14.1384 8.224 14.0234 8.224 13.8824V11.2194C8.224 11.0774 8.105 10.9634 7.962 10.9634M12.001 22.0004C10.428 22.0004 9.07 21.0364 8.441 19.6454L6.753 19.6454C6.752 19.6454 6.751 19.6454 6.75 19.6454C6.188 19.6454 5.733 19.1914 5.733 18.6314V7.5514C5.733 6.9904 6.188 6.5364 6.75 6.5364H17.25C17.812 6.5364 18.267 6.9904 18.267 7.5514V18.6314C18.267 19.1914 17.812 19.6454 17.25 19.6454C17.249 19.6454 17.248 19.6454 17.247 19.6454L15.559 19.6454C14.93 21.0364 13.572 22.0004 12.001 22.0004M12.001 9.4074C10.519 9.4074 9.24 10.2794 8.653 11.5834H15.349C14.761 10.2784 13.483 9.4074 12.001 9.4074M10.153 10.7414C9.972 10.7414 9.825 10.5944 9.825 10.4134C9.825 10.2334 9.972 10.0864 10.153 10.0864C10.334 10.0864 10.481 10.2334 10.481 10.4134C10.481 10.5944 10.334 10.7414 10.153 10.7414M13.849 10.7414C13.668 10.7414 13.521 10.5944 13.521 10.4134C13.521 10.2334 13.668 10.0864 13.849 10.0864C14.03 10.0864 14.177 10.2334 14.177 10.4134C14.177 10.5944 14.03 10.7414 13.849 10.7414" />
            </svg>
            <span>Android TV</span>
          </div>
        </div>

        {/* Avantages */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 md:gap-8 py-8 md:py-12">
          <div className="space-y-2 p-6 bg-zinc-900/20 rounded-3xl border border-zinc-800/50 hover:border-primary/20 transition-all hover:-translate-y-1 group">
            <div className="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center text-primary mb-4 mx-auto group-hover:bg-primary group-hover:text-black transition-all">
              <Calendar size={24} />
            </div>
            <h3 className="text-primary font-bold text-lg">EPG Avancé</h3>
            <p className="text-zinc-500 text-sm">Guide des programmes complet avec description et horaires en temps réel.</p>
          </div>
          <div className="space-y-2 p-6 bg-zinc-900/20 rounded-3xl border border-zinc-800/50 hover:border-primary/20 transition-all hover:-translate-y-1 group">
            <div className="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center text-primary mb-4 mx-auto group-hover:bg-primary group-hover:text-black transition-all">
              <Grid size={24} />
            </div>
            <h3 className="text-primary font-bold text-lg">Multi-Écran</h3>
            <p className="text-zinc-500 text-sm">Regardez jusqu'à 4 flux simultanément. Ne ratez plus aucun match important.</p>
          </div>
          <div className="space-y-2 p-6 bg-zinc-900/20 rounded-3xl border border-zinc-800/50 hover:border-primary/20 transition-all hover:-translate-y-1 group">
            <div className="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center text-primary mb-4 mx-auto group-hover:bg-primary group-hover:text-black transition-all">
              <RotateCw size={24} />
            </div>
            <h3 className="text-primary font-bold text-lg">Replay (Catch-up)</h3>
            <p className="text-zinc-500 text-sm">Revenez en arrière sur vos émissions préférées. Le contrôle du temps est à vous.</p>
          </div>
          <div className="space-y-2 p-6 bg-zinc-900/20 rounded-3xl border border-zinc-800/50 hover:border-primary/20 transition-all hover:-translate-y-1 group">
            <div className="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center text-primary mb-4 mx-auto group-hover:bg-primary group-hover:text-black transition-all">
              <Lock size={24} />
            </div>
            <h3 className="text-primary font-bold text-lg">Flux Sécurisés</h3>
            <p className="text-zinc-500 text-sm">Protection anti-vol de serveur et cryptage des flux pour une confidentialité totale.</p>
          </div>
          <div className="space-y-2 p-6 bg-zinc-900/20 rounded-3xl border border-zinc-800/50 hover:border-primary/20 transition-all hover:-translate-y-1 group">
            <div className="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center text-primary mb-4 mx-auto group-hover:bg-primary group-hover:text-black transition-all">
              <Zap size={24} />
            </div>
            <h3 className="text-primary font-bold text-lg">HLS (m3u8) Natif</h3>
            <p className="text-zinc-500 text-sm">Support optimisé du protocole HLS pour une stabilité maximale sur mobile et Smart TV.</p>
          </div>
        </div>

        <div className="mt-8">
          <Link to="/faq" className="inline-flex items-center gap-2 text-primary hover:text-white transition-colors font-bold uppercase tracking-widest text-xs group">
            <span>En savoir plus dans la FAQ</span>
            <ChevronRight size={16} className="group-hover:translate-x-1 transition-transform" />
          </Link>
        </div>

        {/* Pourquoi nous choisir ? */}
        <div className="py-16 md:py-24 space-y-16">
          <div className="space-y-4">
            <Badge variant="primary">L'Excellence Technologique</Badge>
            <h2 className="text-3xl md:text-6xl font-black tracking-tighter">Pourquoi choisir <span className="text-primary">Sky Player</span> ?</h2>
            <p className="text-zinc-500 max-w-2xl mx-auto">Nous ne sommes pas juste un autre lecteur. Nous sommes la référence du streaming premium.</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 text-left">
            <div className="p-8 bg-zinc-900/40 border border-zinc-800 rounded-[2.5rem] space-y-4">
              <Monitor className="text-primary" size={32} />
              <h4 className="text-xl font-bold">Qualité HDR+</h4>
              <p className="text-zinc-500 text-sm leading-relaxed">Ajustez la luminosité, le contraste et la saturation directement dans le lecteur pour une image parfaite.</p>
            </div>
            <div className="p-8 bg-zinc-900/40 border border-zinc-800 rounded-[2.5rem] space-y-4">
              <Activity className="text-primary" size={32} />
              <h4 className="text-xl font-bold">Stats Temps Réel</h4>
              <p className="text-zinc-500 text-sm leading-relaxed">Surveillez votre débit, votre résolution et la santé de votre connexion avec nos outils de diagnostic intégrés.</p>
            </div>
            <div className="p-8 bg-zinc-900/40 border border-zinc-800 rounded-[2.5rem] space-y-4">
              <Sliders className="text-primary" size={32} />
              <h4 className="text-xl font-bold">Contrôle Total</h4>
              <p className="text-zinc-500 text-sm leading-relaxed">Vitesse de lecture ajustable, mode Picture-in-Picture et sélection manuelle de la qualité vidéo.</p>
            </div>
            <div className="p-8 bg-zinc-900/40 border border-zinc-800 rounded-[2.5rem] space-y-4">
              <Download className="text-primary" size={32} />
              <h4 className="text-xl font-bold">Mode Éco-Data</h4>
              <p className="text-zinc-500 text-sm leading-relaxed">Réduisez votre consommation de données mobiles tout en gardant une fluidité exceptionnelle.</p>
            </div>
          </div>

          {/* Mobile Payments Section */}
          <div className="mt-12 p-8 bg-primary/5 border border-primary/10 rounded-[2.5rem] text-center space-y-4">
            <div className="flex justify-center gap-4 mb-2">
              <div className="px-3 py-1 bg-zinc-900 rounded-lg text-[10px] font-bold text-zinc-400 border border-zinc-800">MTN MONEY</div>
              <div className="px-3 py-1 bg-zinc-900 rounded-lg text-[10px] font-bold text-zinc-400 border border-zinc-800">ORANGE MONEY</div>
              <div className="px-3 py-1 bg-zinc-900 rounded-lg text-[10px] font-bold text-zinc-400 border border-zinc-800">AIRTEL MONEY</div>
              <div className="px-3 py-1 bg-zinc-900 rounded-lg text-[10px] font-bold text-zinc-400 border border-zinc-800">MOOV MONEY</div>
            </div>
            <h3 className="text-2xl font-black italic">Paiements Mobiles Sans Frais</h3>
            <p className="text-zinc-400 max-w-2xl mx-auto text-sm">
              Sky Player accepte les moyens de paiements mobiles locaux sans aucun frais caché. 
              Activez votre compte instantanément via <span className="text-primary font-bold">MTN, Orange, Airtel ou Moov Money</span>.
            </p>
          </div>
        </div>

        {/* Section Achat Simple */}
        <div className="py-12 md:py-16 space-y-10">
          <div className="space-y-2">
            <h2 className="text-3xl md:text-5xl font-black italic">Activez votre application</h2>
            <p className="text-zinc-500 text-sm md:text-base">Choisissez le forfait qui vous convient pour profiter de Sky Player sans limites.</p>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 max-w-4xl mx-auto">
            {/* Forfait 1 An */}
            <div className="bg-zinc-900/40 border border-zinc-800 p-6 md:p-8 rounded-[2rem] hover:border-primary/30 transition-all group text-left space-y-6">
              <div className="space-y-2">
                <Badge variant="primary">Populaire</Badge>
                <h3 className="text-2xl font-bold">Abonnement 1 An</h3>
                <p className="text-zinc-500 text-sm">Idéal pour tester la puissance de Sky Player sur la durée.</p>
              </div>
              <div className="flex items-baseline gap-2">
                <span className="text-4xl font-black text-primary">2285F</span>
                <span className="text-zinc-500 text-sm">/ an</span>
              </div>
              <ul className="space-y-3 text-sm text-zinc-400">
                <li className="flex items-center gap-2"><CheckCircle2 size={16} className="text-primary" /> Mises à jour incluses</li>
                <li className="flex items-center gap-2"><CheckCircle2 size={16} className="text-primary" /> Support technique 24/7</li>
                <li className="flex items-center gap-2"><CheckCircle2 size={16} className="text-primary" /> Sans publicité</li>
              </ul>
              <Link to={`/payment?plan=1an`} onClick={(e) => handlePlanClick(e, '1an')}>
                <Button fullWidth size="lg" className="mt-4">Activer maintenant</Button>
              </Link>
            </div>

            {/* Forfait À Vie */}
            <div className="bg-zinc-900/40 border border-zinc-800 p-6 md:p-8 rounded-[2rem] hover:border-primary/30 transition-all group text-left space-y-6">
              <div className="space-y-2">
                <Badge variant="success">Meilleure Valeur</Badge>
                <h3 className="text-2xl font-bold">Durée à Vie</h3>
                <p className="text-zinc-500 text-sm">Payez une seule fois et profitez de l'application pour toujours.</p>
              </div>
              <div className="flex items-baseline gap-2">
                <span className="text-4xl font-black text-primary">4675F</span>
                <span className="text-zinc-500 text-sm">/ une fois</span>
              </div>
              <ul className="space-y-3 text-sm text-zinc-400">
                <li className="flex items-center gap-2"><CheckCircle2 size={16} className="text-primary" /> Accès illimité à vie</li>
                <li className="flex items-center gap-2"><CheckCircle2 size={16} className="text-primary" /> Priorité sur les nouveautés</li>
                <li className="flex items-center gap-2"><CheckCircle2 size={16} className="text-primary" /> Support VIP</li>
              </ul>
              <Link to={`/payment?plan=vie`} onClick={(e) => handlePlanClick(e, 'vie')}>
                <Button fullWidth size="lg" variant="outline" className="mt-4 border-primary/50 text-primary hover:bg-primary hover:text-black">Prendre l'offre à vie</Button>
              </Link>
            </div>
          </div>
        </div>

        {/* Call to Action */}
        <div className="flex flex-col sm:flex-row justify-center gap-4 pb-12">
          <Button size="lg" className="bg-primary hover:bg-primary-dark w-full sm:w-auto" onClick={() => window.location.href = 'https://firebasestorage.googleapis.com/v0/b/skyplayer-60634.firebasestorage.app/o/SkyPlayer.apk?alt=media'}>Télécharger Sky Player</Button>
          <Link to="/dashboard" className="w-full sm:w-auto">
            <Button variant="outline" size="lg" className="w-full">Espace Revendeur</Button>
          </Link>
        </div>
      </main>
      <Footer />
    </div>
  );
};
