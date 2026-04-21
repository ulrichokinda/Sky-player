import React from 'react';
import { Footer } from '../components/Footer';
import { Header } from '../components/Header';
import { Card, Badge } from '../components/ui';
import { Shield, Zap, Users, Globe, Tv, LayoutDashboard } from 'lucide-react';
import { motion } from 'motion/react';

export const About = () => (
  <div className="min-h-screen bg-black text-white selection:bg-primary/30">
    <Header />
    
    <main className="max-w-5xl mx-auto px-6 py-12 md:py-20 space-y-24">
      {/* Hero Section */}
      <section className="text-center space-y-8">
        <motion.div
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          className="inline-block px-4 py-1.5 rounded-full bg-primary/10 border border-primary/20 text-primary text-[10px] font-black uppercase tracking-[0.3em] mb-4"
        >
          Technologie Média de Pointe
        </motion.div>
        <motion.h1 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-5xl md:text-8xl font-black tracking-tighter leading-none"
        >
          L'Infrastructure <span className="text-primary italic">Pro</span> <br/>
          de Demain.
        </motion.h1>
        <motion.p 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="text-xl md:text-2xl text-zinc-400 max-w-3xl mx-auto leading-relaxed font-light"
        >
          Sky Pro Solutions est une plateforme SaaS spécialisée dans le développement d'outils de distribution et de gestion de flux multimédias pour le marché africain.
        </motion.p>
      </section>

      {/* Corporate Mission */}
      <section className="grid md:grid-cols-2 gap-16 items-start">
        <div className="space-y-8">
          <Badge variant="default" className="border-zinc-800 text-zinc-500 uppercase tracking-widest text-[10px]">Notre Mission</Badge>
          <h2 className="text-4xl font-black tracking-tight">Digitaliser la distribution média en Afrique.</h2>
          <div className="space-y-6 text-zinc-400 text-lg leading-relaxed">
            <p>
              Fondée sur l'expertise logicielle, notre entreprise développe des solutions technologiques robustes permettant aux distributeurs de contenus de gérer leur activité avec une efficacité sans précédent.
            </p>
            <p>
              Nous construisons l'infrastructure invisible — serveurs de gestion, panels revendeurs et interfaces de lecture optimisées — qui permet aux professionnels du média de se concentrer sur leur croissance business tout en garantissant une expérience premium à leurs clients finaux.
            </p>
          </div>
        </div>
        <div className="bg-zinc-900/40 border border-zinc-800 p-12 rounded-[3rem] space-y-8 relative overflow-hidden group">
          <div className="absolute top-0 right-0 w-64 h-64 bg-primary/5 blur-[100px] group-hover:bg-primary/10 transition-all duration-700" />
          <h3 className="text-2xl font-black italic">Chiffres Clés</h3>
          <div className="grid grid-cols-2 gap-8">
            <div className="space-y-1">
              <p className="text-3xl font-black text-primary tracking-tighter">99.9%</p>
              <p className="text-xs uppercase font-bold text-zinc-600 tracking-widest">Uptime Cloud</p>
            </div>
            <div className="space-y-1">
              <p className="text-3xl font-black text-primary tracking-tighter">10k+</p>
              <p className="text-xs uppercase font-bold text-zinc-600 tracking-widest">Licences Actives</p>
            </div>
            <div className="space-y-1">
              <p className="text-3xl font-black text-primary tracking-tighter">15ms</p>
              <p className="text-xs uppercase font-bold text-zinc-600 tracking-widest">Temps de Réponse</p>
            </div>
            <div className="space-y-1">
              <p className="text-3xl font-black text-primary tracking-tighter">24/7</p>
              <p className="text-xs uppercase font-bold text-zinc-600 tracking-widest">Surveillance</p>
            </div>
          </div>
        </div>
      </section>

      {/* Solutions Section */}
      <section className="space-y-16">
        <div className="text-center space-y-4">
          <h2 className="text-4xl font-black tracking-tight">Nos Solutions Business</h2>
          <p className="text-zinc-500">Un écosystème complet pour les entreprises du secteur média.</p>
        </div>
        <div className="grid md:grid-cols-3 gap-8">
          <Card className="p-8 space-y-6 hover:border-primary/50 transition-colors">
            <div className="w-14 h-14 bg-primary/10 rounded-2xl flex items-center justify-center text-primary">
              <LayoutDashboard size={28} />
            </div>
            <div className="space-y-2">
              <h3 className="text-xl font-bold">Cloud Management</h3>
              <p className="text-zinc-500 text-sm leading-relaxed">
                Tableau de bord centralisé pour la gestion en temps réel des licences, des comptes revendeurs et des métriques de consommation.
              </p>
            </div>
          </Card>
          <Card className="p-8 space-y-6 hover:border-primary/50 transition-colors">
            <div className="w-14 h-14 bg-primary/10 rounded-2xl flex items-center justify-center text-primary">
              <Shield size={28} />
            </div>
            <div className="space-y-2">
              <h3 className="text-xl font-bold">Fintech Integration</h3>
              <p className="text-zinc-500 text-sm leading-relaxed">
                Passerelles de paiement automatisées supportant les principaux opérateurs de Mobile Money pour une autonomie totale des partenaires.
              </p>
            </div>
          </Card>
          <Card className="p-8 space-y-6 hover:border-primary/50 transition-colors">
            <div className="w-14 h-14 bg-primary/10 rounded-2xl flex items-center justify-center text-primary">
              <Zap size={28} />
            </div>
            <div className="space-y-2">
              <h3 className="text-xl font-bold">High-Density Player</h3>
              <p className="text-zinc-500 text-sm leading-relaxed">
                Moteur de lecture natif optimisé pour les réseaux à faible bande passante, garantissant une fluidité maximale.
              </p>
            </div>
          </Card>
        </div>
      </section>

      {/* Ethical Commitment section used for auditors */}
      <section className="bg-zinc-900/30 border border-zinc-800 rounded-[3rem] p-12 md:p-16">
        <div className="max-w-3xl mx-auto space-y-10 text-center">
          <div className="space-y-4">
            <h2 className="text-3xl font-black text-white">Engagement & Conformité</h2>
            <p className="text-zinc-500 leading-relaxed text-lg">
              En tant qu'éditeur de logiciels, Sky Pro Solutions s'engage sur la transparence totale de ses outils technologiques.
            </p>
          </div>
          
          <div className="grid sm:grid-cols-2 gap-8 text-left">
            <div className="space-y-4 p-6 bg-black/40 rounded-2xl border border-zinc-800">
              <h4 className="font-black text-xs uppercase tracking-widest text-primary">Éditeur de Logiciel</h4>
              <p className="text-sm text-zinc-400">Nous fournissons exclusivement des outils technologiques de gestion et de lecture. Nous ne gérons ni n'hébergeons aucun contenu tiers.</p>
            </div>
            <div className="space-y-4 p-6 bg-black/40 rounded-2xl border border-zinc-800">
              <h4 className="font-black text-xs uppercase tracking-widest text-primary">Sécurité des Flux</h4>
              <p className="text-sm text-zinc-400">Nos solutions privilégient le cryptage et la confidentialité, garantissant que les données de nos partenaires restent leur propriété exclusive.</p>
            </div>
          </div>

          <div className="pt-8 border-t border-zinc-800">
            <p className="text-zinc-500 text-xs italic">
              Sky Pro Solutions adhère aux standards internationaux du SaaS et collabore avec des partenaires financiers de premier plan pour sécuriser l'écosystème numérique africain.
            </p>
          </div>
        </div>
      </section>
    </main>

    <Footer />
  </div>
);
