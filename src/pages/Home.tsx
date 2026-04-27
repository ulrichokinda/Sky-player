import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Logo } from '../components/Logo';
import { Button, Badge } from '../components/ui';
import { Footer } from '../components/Footer';
import { CheckCircle2, Menu, X, LayoutDashboard, UserPlus, Download, LogIn, Zap, Shield, Cpu, Monitor, Activity, Sliders, Calendar, Grid, RotateCw, Lock, ChevronRight, Store, CreditCard, ShieldCheck, Wallet, Smartphone, Users } from 'lucide-react';
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
      {/* Hidden link for Google OAuth bot verification */}
      <a href="https://skyplayerapp.xyz/#/privacy" rel="privacy-policy" style={{ opacity: 0, position: 'absolute', zIndex: -1 }}>Politique de Confidentialité SkyPlayer</a>
      
      <nav className="flex items-center justify-between py-6 px-4 md:px-8 sticky top-0 bg-black/80 backdrop-blur-md z-50 border-b border-white/5">
        <Logo size={40} />
        
        {/* Desktop Nav */}
        <div className="hidden md:flex items-center gap-4">
          <Link to="/about">
            <Button variant="ghost" size="sm" className="text-zinc-400">À Propos</Button>
          </Link>
          <Link to="/faq">
            <Button variant="ghost" size="sm" className="text-zinc-400">FAQ</Button>
          </Link>
          <div className="w-px h-6 bg-zinc-800 mx-2" />
          {user ? (
            <Link to="/dashboard">
              <Button variant="ghost" icon={LayoutDashboard} size="sm">Tableau de Bord</Button>
            </Link>
          ) : (
            <div className="flex items-center gap-2">
              <Link to="/login">
                <Button variant="ghost" icon={LogIn} size="sm">Connexion</Button>
              </Link>
              <Link to="/register">
                <Button variant="primary" size="sm" className="px-6">Espace Pro</Button>
              </Link>
            </div>
          )}
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
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="fixed inset-0 z-40 bg-black pt-24 px-6 md:hidden overflow-y-auto"
          >
            <div className="flex flex-col gap-6">
              <div className="grid grid-cols-2 gap-3">
                <Link to="/about" onClick={() => setIsMenuOpen(false)} className="p-4 bg-zinc-900 rounded-2xl border border-zinc-800 text-center">
                  <p className="text-zinc-400 text-sm">À Propos</p>
                </Link>
                <Link to="/faq" onClick={() => setIsMenuOpen(false)} className="p-4 bg-zinc-900 rounded-2xl border border-zinc-800 text-center">
                  <p className="text-zinc-400 text-sm">FAQ</p>
                </Link>
              </div>

              {user ? (
                <Link to="/dashboard" onClick={() => setIsMenuOpen(false)}>
                  <div className="flex items-center gap-4 p-5 bg-primary/10 rounded-2xl border border-primary/20">
                    <LayoutDashboard className="text-primary" />
                    <div className="text-left">
                      <p className="font-bold text-primary">Accès Dashboard</p>
                      <p className="text-xs text-primary/60">Gérez vos clients et crédits</p>
                    </div>
                  </div>
                </Link>
              ) : (
                <div className="space-y-4">
                  <Link to="/login" onClick={() => setIsMenuOpen(false)}>
                    <div className="flex items-center gap-4 p-5 bg-zinc-900 rounded-2xl border border-zinc-800">
                      <LogIn className="text-zinc-400" />
                      <div className="text-left">
                        <p className="font-bold">Connexion</p>
                        <p className="text-xs text-zinc-500">Accédez à votre compte</p>
                      </div>
                    </div>
                  </Link>
                  <Link to="/register" onClick={() => setIsMenuOpen(false)}>
                    <div className="flex items-center gap-4 p-5 bg-primary rounded-2xl border border-primary/30">
                      <UserPlus className="text-black" />
                      <div className="text-left">
                        <p className="font-bold text-black">Devenir Revendeur</p>
                        <p className="text-xs text-black/60">Lancez votre business</p>
                      </div>
                    </div>
                  </Link>
                </div>
              )}
              
              <div className="pt-6 border-t border-zinc-800">
                <Button fullWidth size="lg" icon={Download} onClick={() => window.location.href = 'https://firebasestorage.googleapis.com/v0/b/skyplayer-60634.firebasestorage.app/o/SkyPlayer.apk?alt=media'}>Télécharger APK</Button>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <main className="max-w-6xl mx-auto mt-12 md:mt-24 text-center space-y-16 px-4 md:px-6">
        
        {/* Titre d'accroche */}
        <div className="space-y-6">
          <Badge variant="primary" className="mx-auto mb-4 px-5 py-2 text-[10px] uppercase font-black tracking-[0.3em] bg-primary/20 text-primary border-primary/30 animate-pulse">Technologie de Streaming de Pointe</Badge>
          <h1 className="text-5xl sm:text-6xl md:text-8xl font-black tracking-tighter leading-[0.9] uppercase italic">
            Lecteur de <br className="hidden sm:block"/>
            <span className="text-primary">Streaming</span> Pro.
          </h1>
          <p className="text-zinc-400 text-lg md:text-2xl max-w-3xl mx-auto leading-relaxed">
            Le moteur de lecture le plus avancé pour l'Afrique. 
            Optimisé pour les réseaux instables, compatible nativement avec tous vos flux IPTV, Xtream et M3U.
          </p>
          
          <div className="flex flex-col sm:flex-row justify-center items-center gap-4 pt-8">
            <Button size="lg" className="bg-primary hover:bg-primary-dark w-full sm:w-auto px-10 text-lg font-black h-16" onClick={() => window.location.href = 'https://firebasestorage.googleapis.com/v0/b/skyplayer-60634.firebasestorage.app/o/SkyPlayer.apk?alt=media'} icon={Download}>Télécharger APK</Button>
            <Link to="/register" className="w-full sm:w-auto">
              <Button variant="outline" size="lg" className="w-full px-10 text-lg border-zinc-800 hover:border-primary/50 h-16">Panel Revendeur</Button>
            </Link>
          </div>
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
              <Activity size={24} />
            </div>
            <h3 className="text-primary font-bold text-lg">Activations Instantanées</h3>
            <p className="text-zinc-500 text-sm">Système d'activation automatisé par API. Pas de temps d'attente pour vos clients finaux.</p>
          </div>
          <div className="space-y-2 p-6 bg-zinc-900/20 rounded-3xl border border-zinc-800/50 hover:border-primary/20 transition-all hover:-translate-y-1 group">
            <div className="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center text-primary mb-4 mx-auto group-hover:bg-primary group-hover:text-black transition-all">
              <Store size={24} />
            </div>
            <h3 className="text-primary font-bold text-lg">Panel Revendeur Pro</h3>
            <p className="text-zinc-500 text-sm">Gérez des milliers de clients depuis une interface unique, sécurisée et performante.</p>
          </div>
          <div className="space-y-2 p-6 bg-zinc-900/20 rounded-3xl border border-zinc-800/50 hover:border-primary/20 transition-all hover:-translate-y-1 group">
            <div className="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center text-primary mb-4 mx-auto group-hover:bg-primary group-hover:text-black transition-all">
              <CreditCard size={24} />
            </div>
            <h3 className="text-primary font-bold text-lg">Paiements Mobiles</h3>
            <p className="text-zinc-500 text-sm">Rechargement de crédits via Mobile Money (Airtel, MTN, Moov, Orange) en temps réel.</p>
          </div>
          <div className="space-y-2 p-6 bg-zinc-900/20 rounded-3xl border border-zinc-800/50 hover:border-primary/20 transition-all hover:-translate-y-1 group">
            <div className="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center text-primary mb-4 mx-auto group-hover:bg-primary group-hover:text-black transition-all">
              <Lock size={24} />
            </div>
            <h3 className="text-primary font-bold text-lg">Sécurité Bancaire</h3>
            <p className="text-zinc-500 text-sm">Toutes vos transactions et données clients sont protégées par un cryptage SSL de niveau bancaire.</p>
          </div>
        </div>

        <div className="mt-8">
          <Link to="/faq" className="inline-flex items-center gap-2 text-primary hover:text-white transition-colors font-bold uppercase tracking-widest text-xs group">
            <span>En savoir plus dans la FAQ</span>
            <ChevronRight size={16} className="group-hover:translate-x-1 transition-transform" />
          </Link>
        </div>

        {/* Technical Specs Section */}
        <div className="py-20 space-y-12">
          <div className="text-left space-y-4">
            <h2 className="text-3xl md:text-5xl font-black italic uppercase tracking-tighter">Spécifications <span className="text-primary">Techniques</span></h2>
            <p className="text-zinc-500 max-w-2xl">Une ingénierie de précision pour une performance absolue sur tous les réseaux.</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="p-8 bg-zinc-900 border border-zinc-800 rounded-[2rem] text-left space-y-6">
              <div className="w-12 h-12 bg-white/5 rounded-xl flex items-center justify-center">
                <Cpu className="text-primary" size={24} />
              </div>
              <div className="space-y-2">
                <h4 className="text-lg font-bold uppercase tracking-tight">Moteur de Lecture Natif</h4>
                <p className="text-zinc-500 text-sm leading-relaxed">
                  Support complet de <strong>HLS (m3u8)</strong>, <strong>DASH</strong>, <strong>RTMP</strong> et <strong>RTSP</strong>. Décodage matériel <strong>4K HEVC (H.265)</strong> intégré pour une consommation CPU minimale.
                </p>
              </div>
            </div>

            <div className="p-8 bg-zinc-900 border border-zinc-800 rounded-[2rem] text-left space-y-6">
              <div className="w-12 h-12 bg-white/5 rounded-xl flex items-center justify-center">
                <Activity className="text-primary" size={24} />
              </div>
              <div className="space-y-2">
                <h4 className="text-lg font-bold uppercase tracking-tight">Adaptive Jitter Buffer</h4>
                <p className="text-zinc-500 text-sm leading-relaxed">
                  Algorithme propriétaire de mise en cache dynamique (<strong>Zero-Latency</strong>). S'adapte en temps réel aux micro-coupures des connexions 3G/4G et satellites.
                </p>
              </div>
            </div>

            <div className="p-8 bg-zinc-900 border border-zinc-800 rounded-[2rem] text-left space-y-6">
              <div className="w-12 h-12 bg-white/5 rounded-xl flex items-center justify-center">
                <Zap className="text-primary" size={24} />
              </div>
              <div className="space-y-2">
                <h4 className="text-lg font-bold uppercase tracking-tight">Connecteur Xtream API v2</h4>
                <p className="text-zinc-500 text-sm leading-relaxed">
                  Synchronisation ultra-rapide des catégories, EPG et métadonnées (TMDB). Capable de charger des bibliothèques de +50,000 items en moins de 3 secondes.
                </p>
              </div>
            </div>
            
            <div className="p-8 bg-zinc-900 border border-zinc-800 rounded-[2rem] text-left space-y-6">
              <div className="w-12 h-12 bg-white/5 rounded-xl flex items-center justify-center">
                <Lock className="text-primary" size={24} />
              </div>
              <div className="space-y-2">
                <h4 className="text-lg font-bold uppercase tracking-tight">Proxy & Sécurité</h4>
                <p className="text-zinc-500 text-sm leading-relaxed">
                  Tunneling HTTP local pour contourner les restrictions <strong>CORS</strong> et obfuscation des streams pour protéger votre vie privée et vos accès.
                </p>
              </div>
            </div>

            <div className="p-8 bg-zinc-900 border border-zinc-800 rounded-[2rem] text-left space-y-6">
              <div className="w-12 h-12 bg-white/5 rounded-xl flex items-center justify-center">
                <ShieldCheck className="text-primary" size={24} />
              </div>
              <div className="space-y-2">
                <h4 className="text-lg font-bold uppercase tracking-tight">Certifié Widevine L1</h4>
                <p className="text-zinc-500 text-sm leading-relaxed">
                  Support DRM natif pour la lecture de contenus protégés en Haute Définition sur les appareils compatibles (Android TV certifiées).
                </p>
              </div>
            </div>

            <div className="p-8 bg-zinc-900 border border-zinc-800 rounded-[2rem] text-left space-y-6">
              <div className="w-12 h-12 bg-white/5 rounded-xl flex items-center justify-center">
                <Grid className="text-primary" size={24} />
              </div>
              <div className="space-y-2">
                <h4 className="text-lg font-bold uppercase tracking-tight">Multi-EPG Sync</h4>
                <p className="text-zinc-500 text-sm leading-relaxed">
                  Agrégation automatique des guides de programmes depuis plusieurs sources (XMLTV, JSON API) avec rafraîchissement asynchrone en arrière-plan.
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Why Sky Pro Section */}
        <div className="py-20 bg-zinc-900/10 border-y border-zinc-800/50 -mx-4 md:-mx-8 px-4 md:px-8">
          <div className="max-w-5xl mx-auto space-y-16">
            <div className="text-center space-y-4">
              <h2 className="text-3xl md:text-5xl font-black italic">Le Choix des Professionnels</h2>
              <p className="text-zinc-500 max-w-xl mx-auto">Sky Player Pro n'est pas qu'un simple lecteur. C'est une infrastructure complète pour bâtir votre business de distribution média.</p>
            </div>

            <div className="grid md:grid-cols-2 gap-12 text-left">
              <div className="flex gap-6">
                <div className="w-14 h-14 bg-primary/10 rounded-2xl flex items-center justify-center text-primary shrink-0">
                  <Shield size={28} />
                </div>
                <div className="space-y-2">
                  <h4 className="text-xl font-bold">Infrastructure Cloud</h4>
                  <p className="text-zinc-500 text-sm leading-relaxed">Centralisez vos clients et serveurs sur notre cloud sécurisé. Gérez tout à distance sans aucune manipulation technique complexe.</p>
                </div>
              </div>
              <div className="flex gap-6">
                <div className="w-14 h-14 bg-primary/10 rounded-2xl flex items-center justify-center text-primary shrink-0">
                  <Zap size={28} />
                </div>
                <div className="space-y-2">
                  <h4 className="text-xl font-bold">Performance Native</h4>
                  <p className="text-zinc-500 text-sm leading-relaxed">Cœur de lecture optimisé pour les processeurs d'entrée de gamme des TV connectées et smartphones Android en Afrique.</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Pourquoi nous choisir ? */}
        <div className="py-16 md:py-24 space-y-16">
          <div className="space-y-4">
            <Badge variant="primary">L'Excellence Technologique & Pratique</Badge>
            <h2 className="text-3xl md:text-6xl font-black tracking-tighter">Pourquoi choisir <span className="text-primary">Sky Player</span> ?</h2>
            <p className="text-zinc-500 max-w-2xl mx-auto">Nous offrons bien plus qu'un simple lecteur. Découvrez les avantages exclusifs qui font de Sky Player le choix numéro 1.</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 text-left">
            <div className="p-8 bg-zinc-900/40 border border-zinc-800 rounded-[2.5rem] space-y-4 hover:bg-zinc-800/40 transition-colors">
              <Monitor className="text-primary" size={32} />
              <h4 className="text-xl font-bold">Qualité HDR+</h4>
              <p className="text-zinc-500 text-sm leading-relaxed">Profitez de vos contenus avec une clarté absolue. Ajustez lumière et contraste pour une qualité visuelle irréprochable.</p>
            </div>
            <div className="p-8 bg-zinc-900/40 border border-zinc-800 rounded-[2.5rem] space-y-4 hover:bg-zinc-800/40 transition-colors">
              <ShieldCheck className="text-primary" size={32} />
              <h4 className="text-xl font-bold">FIabilité Anti-Coupure</h4>
              <p className="text-zinc-500 text-sm leading-relaxed">Notre système de cache intelligent garantit un flux vidéo continu, même avec des connexions internet capricieuses ou instables.</p>
            </div>
            <div className="p-8 bg-zinc-900/40 border border-zinc-800 rounded-[2.5rem] space-y-4 hover:bg-zinc-800/40 transition-colors">
              <Smartphone className="text-primary" size={32} />
              <h4 className="text-xl font-bold">Multi-Plateforme</h4>
              <p className="text-zinc-500 text-sm leading-relaxed">Une expérience fluide et uniformisée que vous soyez sur Android TV, Box Internet, Tablette ou Smartphone.</p>
            </div>
            <div className="p-8 bg-zinc-900/40 border border-zinc-800 rounded-[2.5rem] space-y-4 hover:bg-zinc-800/40 transition-colors">
              <Download className="text-primary" size={32} />
              <h4 className="text-xl font-bold">Mode Éco-Data</h4>
              <p className="text-zinc-500 text-sm leading-relaxed">Stoppez le gaspillage ! Notre compression limite la consommation de données mobiles sans sacrifier la qualité visuelle.</p>
            </div>
            <div className="p-8 bg-zinc-900/40 border border-zinc-800 rounded-[2.5rem] space-y-4 hover:bg-zinc-800/40 transition-colors">
              <Sliders className="text-primary" size={32} />
              <h4 className="text-xl font-bold">Contrôle Total</h4>
              <p className="text-zinc-500 text-sm leading-relaxed">Picture-in-Picture, vitesse de lecture ajustable, changement de piste audio et sous-titres à la volée.</p>
            </div>
            <div className="p-8 bg-zinc-900/40 border border-zinc-800 rounded-[2.5rem] space-y-4 hover:bg-zinc-800/40 transition-colors">
              <Activity className="text-primary" size={32} />
              <h4 className="text-xl font-bold">Stats Temps Réel</h4>
              <p className="text-zinc-500 text-sm leading-relaxed">Analysez votre débit, votre latence et la résolution diffusée grâce à nos outils de diagnostic de pointe.</p>
            </div>
            <div className="p-8 bg-zinc-900/40 border border-zinc-800 rounded-[2.5rem] space-y-4 hover:bg-zinc-800/40 transition-colors">
              <Wallet className="text-primary" size={32} />
              <h4 className="text-xl font-bold">Paiements Locaux</h4>
              <p className="text-zinc-500 text-sm leading-relaxed">Activation instantanée via vos moyens de Mobile Money locaux (Wave, MTN, Orange, Moov, Airtel). Fini la carte bancaire !</p>
            </div>
            <div className="p-8 bg-zinc-900/40 border border-zinc-800 rounded-[2.5rem] space-y-4 hover:bg-zinc-800/40 transition-colors">
              <Users className="text-primary" size={32} />
              <h4 className="text-xl font-bold">Panel Revendeur</h4>
              <p className="text-zinc-500 text-sm leading-relaxed">Vendez des activations. Une plateforme intégrée pour générer des revenus en devenant l'administrateur de votre propre réseau client.</p>
            </div>
          </div>

          {/* Mobile Payments Section */}
          <div className="mt-12 p-12 bg-primary/5 border border-primary/10 rounded-[3rem] text-center space-y-6 relative overflow-hidden">
            <div className="absolute top-0 right-0 w-32 h-32 bg-primary/10 blur-[60px] rounded-full -translate-y-1/2 translate-x-1/2" />
            
            <div className="flex justify-center flex-wrap gap-4 mb-2">
              <span className="px-5 py-2 bg-zinc-950 rounded-xl text-[10px] font-black tracking-widest text-zinc-500 border border-zinc-800 uppercase">Orange Money</span>
              <span className="px-5 py-2 bg-zinc-950 rounded-xl text-[10px] font-black tracking-widest text-zinc-500 border border-zinc-800 uppercase">MTN MoMo</span>
              <span className="px-5 py-2 bg-zinc-950 rounded-xl text-[10px] font-black tracking-widest text-zinc-500 border border-zinc-800 uppercase">Airtel Money</span>
              <span className="px-5 py-2 bg-zinc-950 rounded-xl text-[10px] font-black tracking-widest text-zinc-500 border border-zinc-800 uppercase">Wave</span>
            </div>
            
            <div className="space-y-2">
              <h3 className="text-3xl font-black italic">Une Solution Locale et Native</h3>
              <p className="text-zinc-400 max-w-3xl mx-auto text-base">
                Notre plateforme est conçue pour s'intégrer nativement avec les infrastructures de paiement africaines. 
                Rechargez vos crédits revendeurs en quelques secondes via vos opérateurs locaux préférés.
              </p>
            </div>
          </div>
        </div>

        {/* Section Achat Simple */}
        <div className="py-12 md:py-16 space-y-10">
          <div className="space-y-4 text-center">
            <h2 className="text-3xl md:text-5xl font-black italic">Licences & Écosystème</h2>
            <p className="text-zinc-500 max-w-2xl mx-auto">Choisissez le modèle qui convient à votre déploiement. De l'utilisateur individuel au large réseau de distribution.</p>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 max-w-4xl mx-auto">
            {/* Forfait 1 An */}
            <div className="bg-zinc-900/40 border border-zinc-800 p-6 md:p-8 rounded-[2rem] hover:border-primary/30 transition-all group text-left space-y-6">
              <div className="space-y-2">
                <Badge variant="primary">Utilisation Standard</Badge>
                <h3 className="text-2xl font-bold">Licence Annuelle</h3>
                <p className="text-zinc-500 text-sm">Parfait pour tester la puissance de Sky Pro sur un appareil unique.</p>
              </div>
              <div className="flex items-baseline gap-2">
                <span className="text-4xl font-black text-primary">2285F</span>
                <span className="text-zinc-500 text-sm">/ an par device</span>
              </div>
              <ul className="space-y-3 text-sm text-zinc-400">
                <li className="flex items-center gap-2"><CheckCircle2 size={16} className="text-primary" /> Mises à jour OTA incluses</li>
                <li className="flex items-center gap-2"><CheckCircle2 size={16} className="text-primary" /> Support technique Standard</li>
                <li className="flex items-center gap-2"><CheckCircle2 size={16} className="text-primary" /> Sans publicité</li>
              </ul>
              <Link to={`/payment?plan=1an`} onClick={(e) => handlePlanClick(e, '1an')}>
                <Button fullWidth size="lg" className="mt-4">Activer une licence</Button>
              </Link>
            </div>

            {/* Forfait À Vie */}
            <div className="bg-zinc-900/40 border border-zinc-800 p-6 md:p-8 rounded-[2rem] hover:border-primary/30 transition-all group text-left space-y-6">
              <div className="space-y-2">
                <Badge variant="success">Économie Maximale</Badge>
                <h3 className="text-2xl font-bold">Licence Perpétuelle</h3>
                <p className="text-zinc-500 text-sm">Paiement unique. Idéal pour les installations résidentielles longue durée.</p>
              </div>
              <div className="flex items-baseline gap-2">
                <span className="text-4xl font-black text-primary">4675F</span>
                <span className="text-zinc-500 text-sm">/ une fois</span>
              </div>
              <ul className="space-y-3 text-sm text-zinc-400">
                <li className="flex items-center gap-2"><CheckCircle2 size={16} className="text-primary" /> Accès illimité à vie</li>
                <li className="flex items-center gap-2"><CheckCircle2 size={16} className="text-primary" /> Priorité sur les fonctions Cloud</li>
                <li className="flex items-center gap-2"><CheckCircle2 size={16} className="text-primary" /> Support technique VIP</li>
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
