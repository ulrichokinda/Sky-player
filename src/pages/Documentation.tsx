import React from 'react';
import { motion } from 'motion/react';
import { Link } from 'react-router-dom';
import { 
  ArrowLeft, 
  Book, 
  Cpu, 
  Zap, 
  ShieldCheck, 
  Globe, 
  Tv, 
  Smartphone, 
  Database, 
  Code,
  CheckCircle2,
  AlertCircle,
  Clock,
  Layout,
  Terminal,
  Server
} from 'lucide-react';
import { Button, Badge, Card } from '../components/ui';

export function Documentation() {
  const sections = [
    {
      id: 'introduction',
      title: 'Introduction',
      icon: Book,
      content: `Sky Player est une plateforme de streaming multimédia de nouvelle génération conçue spécifiquement pour le marché africain. Elle combine un moteur de lecture haute performance avec une infrastructure de gestion de distribution (SaaS) pour les revendeurs et les fournisseurs de contenu.`
    },
    {
      id: 'capabilities',
      title: 'Capacités Techniques',
      icon: Cpu,
      subsections: [
        {
          title: 'Moteur de Lecture',
          items: [
            'Support natif HLS (m3u8), DASH, RTMP et RTSP.',
            'Décodage matériel 4K HEVC (H.265) et AV1.',
            'Adaptive Jitter Buffer pour les réseaux à faible bande passante (3G/4G).',
            'Commutation de flux fluide (Gapless playback).'
          ]
        },
        {
          title: 'Protocoles Supportés',
          items: [
            'Xtream Codes API v2 (Live, VOD, Series, Catchup).',
            'M3U / M3U8 Playlists.',
            'EPG XMLTV et JSON.',
            'DRM Widevine L1 & L3.'
          ]
        }
      ]
    },
    {
      id: 'infrastructure',
      title: 'Infrastructure Edge',
      icon: Server,
      content: `Notre architecture est distribuée sur des clusters Edge pour garantir la latence la plus faible possible en Afrique de l'Ouest et Centrale.`,
      features: [
        'Caching dynamique des métadonnées TMDB.',
        'Proxy HTTP local pour contourner les restrictions CORS.',
        'Obfuscation des flux pour la protection de la vie privée.',
        'Disponibilité garantie de 99.9% via Google Cloud Platform.'
      ]
    },
    {
      id: 'setup',
      title: 'Guide de Configuration',
      icon: Zap,
      steps: [
        {
          title: 'Installation',
          desc: 'Téléchargez l\'APK officiel depuis notre site et installez-le sur votre appareil Android ou TV Box.'
        },
        {
          title: 'Activation',
          desc: 'Lancez l\'application et notez votre adresse MAC. Utilisez notre panel revendeur pour activer votre accès.'
        },
        {
          title: 'Import de Playlist',
          desc: 'Connectez-vous via Xtream Codes (Host, User, Pass) ou importez un lien M3U direct.'
        }
      ]
    },
    {
      id: 'reseller',
      title: 'Panel Revendeur',
      icon: ShieldCheck,
      content: `Le panel Sky Pro permet aux professionnels de gérer leur flotte d'appareils et leurs clients en temps réel.`,
      features: [
        'Gestion centralisée des adresses MAC.',
        'Système de crédits prépayés.',
        'Automatisation des activations via API.',
        'Stats d\'utilisation et monitoring des serveurs.'
      ]
    }
  ];

  return (
    <div className="min-h-screen bg-black text-white font-sans selection:bg-primary selection:text-black">
      {/* Header */}
      <header className="fixed top-0 left-0 w-full z-50 bg-black/80 backdrop-blur-xl border-b border-zinc-900">
        <div className="max-w-7xl mx-auto px-6 h-20 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2 group">
            <div className="w-10 h-10 bg-primary rounded-xl flex items-center justify-center text-black font-black italic shadow-[0_0_20px_rgba(244,197,10,0.3)] group-hover:scale-105 transition-transform">SP</div>
            <span className="text-xl font-black tracking-tighter uppercase italic">Sky <span className="text-primary">Docs</span></span>
          </Link>
          <div className="flex items-center gap-4">
            <Link to="/faq">
              <Button variant="ghost" size="sm" className="text-zinc-400">FAQ</Button>
            </Link>
            <Link to="/login">
              <Button variant="primary" size="sm">Se Connecter</Button>
            </Link>
          </div>
        </div>
      </header>

      <div className="max-w-7xl mx-auto px-6 pt-32 pb-20">
        <div className="grid grid-cols-1 lg:grid-cols-[280px_1fr] gap-12">
          {/* Sidebar Navigation */}
          <aside className="hidden lg:block space-y-8 sticky top-32 h-fit">
            <nav className="space-y-1">
              <p className="text-[10px] font-black text-zinc-500 uppercase tracking-[0.2em] mb-4 px-3">Navigation</p>
              {sections.map((section) => (
                <a 
                  key={section.id} 
                  href={`#${section.id}`}
                  className="flex items-center gap-3 px-3 py-2 rounded-lg text-zinc-400 hover:text-primary hover:bg-primary/5 transition-all group"
                >
                  <section.icon size={18} className="group-hover:scale-110 transition-transform" />
                  <span className="text-sm font-bold uppercase tracking-tight">{section.title}</span>
                </a>
              ))}
            </nav>

            <Card className="p-5 border-zinc-800 bg-zinc-900/50 space-y-4">
              <p className="text-xs text-zinc-400 font-medium">Besoin d'aide technique ?</p>
              <Button fullWidth size="sm" variant="outline" className="text-[10px] font-black uppercase tracking-widest border-zinc-700">Contact Support</Button>
            </Card>
          </aside>

          {/* Main Content */}
          <main className="space-y-24">
            {/* Hero Section */}
            <header className="space-y-6">
              <Badge variant="primary" className="px-4 py-1.5 bg-primary/20 text-primary border-primary/30 uppercase font-black tracking-widest text-[10px]">Documentation Officielle</Badge>
              <h1 className="text-5xl md:text-7xl font-black tracking-tighter leading-none uppercase italic">
                Maîtrisez <br/>
                <span className="text-primary">L'Écosystème</span> Sky.
              </h1>
              <p className="text-zinc-400 text-lg md:text-xl max-w-2xl leading-relaxed">
                Retrouvez toutes les spécifications techniques, les guides d'intégration et les meilleures pratiques pour exploiter la puissance de Sky Player.
              </p>
            </header>

            {/* Sections */}
            {sections.map((section) => (
              <section key={section.id} id={section.id} className="scroll-mt-32 space-y-8">
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 bg-zinc-900 rounded-2xl flex items-center justify-center text-primary border border-zinc-800">
                    <section.icon size={24} />
                  </div>
                  <h2 className="text-3xl font-black uppercase tracking-tighter italic">{section.title} <span className="text-zinc-800">/</span></h2>
                </div>

                <div className="prose prose-invert max-w-none">
                  {section.content && (
                    <p className="text-zinc-400 text-lg leading-relaxed mb-8">
                      {section.content}
                    </p>
                  )}

                  {section.subsections && (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mt-8">
                      {section.subsections.map((sub, i) => (
                        <Card key={i} className="p-6 border-zinc-800 bg-zinc-900/30 backdrop-blur-sm space-y-4">
                          <h4 className="text-primary font-black uppercase tracking-widest text-xs">{sub.title}</h4>
                          <ul className="space-y-3">
                            {sub.items.map((item, j) => (
                              <li key={j} className="flex gap-3 text-zinc-400 text-sm">
                                <CheckCircle2 size={16} className="text-primary shrink-0 mt-0.5" />
                                <span>{item}</span>
                              </li>
                            ))}
                          </ul>
                        </Card>
                      ))}
                    </div>
                  )}

                  {section.features && (
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mt-8">
                      {section.features.map((feat, i) => (
                        <div key={i} className="p-5 bg-zinc-950 border border-zinc-900 rounded-2xl flex items-center gap-4 group hover:border-primary/30 transition-colors">
                          <div className="w-2 h-2 rounded-full bg-primary" />
                          <p className="text-zinc-300 text-sm font-medium">{feat}</p>
                        </div>
                      ))}
                    </div>
                  )}

                  {section.steps && (
                    <div className="space-y-4 mt-8">
                      {section.steps.map((step, i) => (
                        <div key={i} className="flex gap-6 p-6 bg-zinc-900/50 rounded-3xl border border-zinc-800 items-start group hover:bg-zinc-900 transition-colors">
                          <span className="text-4xl font-black text-zinc-800 group-hover:text-primary transition-colors">0{i+1}</span>
                          <div className="space-y-1">
                            <h4 className="text-lg font-bold uppercase tracking-tight">{step.title}</h4>
                            <p className="text-zinc-500 text-sm">{step.desc}</p>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </section>
            ))}

            {/* Support Footer */}
            <Card className="p-12 border-primary/20 bg-primary/5 text-center space-y-6 rounded-[3rem]">
              <div className="mx-auto w-16 h-16 bg-primary rounded-3xl flex items-center justify-center text-black">
                <AlertCircle size={32} />
              </div>
              <div className="space-y-2">
                <h3 className="text-2xl font-black uppercase tracking-tight">Une question spécifique ?</h3>
                <p className="text-zinc-400 max-w-md mx-auto">Nos ingénieurs sont disponibles pour vous accompagner dans vos déploiements d'infrastructure média.</p>
              </div>
              <div className="flex flex-col sm:flex-row justify-center gap-4">
                <Button variant="primary" size="lg" className="px-10">Contacter le Support</Button>
                <Link to="/about">
                  <Button variant="outline" size="lg" className="px-10 border-zinc-800">En savoir plus</Button>
                </Link>
              </div>
            </Card>
          </main>
        </div>
      </div>

      {/* Footer Minimal */}
      <footer className="border-t border-zinc-900 py-12 bg-zinc-950 px-6">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row justify-between items-center gap-8">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 bg-zinc-800 rounded-lg flex items-center justify-center text-xs font-black italic">SP</div>
            <p className="text-[10px] font-bold text-zinc-600 uppercase tracking-[0.2em]">Sky Player Documentation &copy; 2026 &bull; Alpha Release</p>
          </div>
          <div className="flex gap-8">
            <Link to="/privacy" className="text-[10px] font-bold text-zinc-600 uppercase tracking-widest hover:text-primary">Confidentialité</Link>
            <Link to="/terms" className="text-[10px] font-bold text-zinc-600 uppercase tracking-widest hover:text-primary">Conditions</Link>
            <Link to="/contact" className="text-[10px] font-bold text-zinc-600 uppercase tracking-widest hover:text-primary">Contact</Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
