import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Logo } from '../components/Logo';
import { Button, Badge } from '../components/ui';
import { Footer } from '../components/Footer';
import { CheckCircle2, Menu, X, LayoutDashboard, UserPlus, Download, LogIn, Zap, Shield, Cpu, Monitor, Activity, Sliders } from 'lucide-react';
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
              <Button fullWidth size="lg" icon={Download} className="mt-4">Télécharger l'APK</Button>
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

        {/* Avantages */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 md:gap-8 py-8 md:py-12">
          <div className="space-y-2 p-6 bg-zinc-900/20 rounded-3xl border border-zinc-800/50 hover:border-primary/20 transition-colors">
            <div className="w-10 h-10 bg-primary/10 rounded-xl flex items-center justify-center text-primary mb-4 mx-auto">
              <Shield size={24} />
            </div>
            <h3 className="text-primary font-bold text-lg">Sécurité & Stabilité</h3>
            <p className="text-zinc-500 text-sm">Une infrastructure robuste garantissant un streaming sans coupure, même en heure de pointe.</p>
          </div>
          <div className="space-y-2 p-6 bg-zinc-900/20 rounded-3xl border border-zinc-800/50 hover:border-primary/20 transition-colors">
            <div className="w-10 h-10 bg-primary/10 rounded-xl flex items-center justify-center text-primary mb-4 mx-auto">
              <Zap size={24} />
            </div>
            <h3 className="text-primary font-bold text-lg">Zapping Ultra-Rapide</h3>
            <p className="text-zinc-500 text-sm">Technologie Turbo-Start pour un changement de chaîne instantané en moins de 1.5s.</p>
          </div>
          <div className="space-y-2 p-6 bg-zinc-900/20 rounded-3xl border border-zinc-800/50 hover:border-primary/20 transition-colors">
            <div className="w-10 h-10 bg-primary/10 rounded-xl flex items-center justify-center text-primary mb-4 mx-auto">
              <Cpu size={24} />
            </div>
            <h3 className="text-primary font-bold text-lg">Moteur IPTV Pro</h3>
            <p className="text-zinc-500 text-sm">Optimisé pour les connexions africaines avec une gestion intelligente du tampon.</p>
          </div>
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
                <span className="text-4xl font-black text-primary">2000F</span>
                <span className="text-zinc-500 text-sm">/ an</span>
              </div>
              <ul className="space-y-3 text-sm text-zinc-400">
                <li className="flex items-center gap-2"><CheckCircle2 size={16} className="text-primary" /> Mises à jour incluses</li>
                <li className="flex items-center gap-2"><CheckCircle2 size={16} className="text-primary" /> Support technique 24/7</li>
                <li className="flex items-center gap-2"><CheckCircle2 size={16} className="text-primary" /> Sans publicité</li>
              </ul>
              <Link to="/payment" onClick={(e) => handlePlanClick(e, '1an')}>
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
              <Link to="/payment" onClick={(e) => handlePlanClick(e, 'vie')}>
                <Button fullWidth size="lg" variant="outline" className="mt-4 border-primary/50 text-primary hover:bg-primary hover:text-black">Prendre l'offre à vie</Button>
              </Link>
            </div>
          </div>
        </div>

        {/* Call to Action */}
        <div className="flex flex-col sm:flex-row justify-center gap-4 pb-12">
          <Button size="lg" className="bg-primary hover:bg-primary-dark w-full sm:w-auto">Télécharger Sky Player</Button>
          <Link to="/dashboard" className="w-full sm:w-auto">
            <Button variant="outline" size="lg" className="w-full">Espace Revendeur</Button>
          </Link>
        </div>
      </main>
      <Footer />
    </div>
  );
};
