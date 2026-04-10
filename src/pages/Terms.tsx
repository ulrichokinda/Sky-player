import React from 'react';
import { Header } from '../components/Header';
import { Footer } from '../components/Footer';
import { Shield, Scale, Globe, Lock, AlertTriangle, Copyright } from 'lucide-react';
import { motion } from 'motion/react';

export const Terms = () => {
  return (
    <div className="min-h-screen bg-black text-white flex flex-col">
      <Header />
      
      <main className="flex-1 max-w-4xl mx-auto px-6 py-16">
        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="space-y-12"
        >
          <div className="text-center space-y-4">
            <h1 className="text-4xl md:text-6xl font-black italic tracking-tighter">
              TERMES ET <span className="text-primary">CONDITIONS</span>
            </h1>
            <p className="text-zinc-500 font-medium">Dernière mise à jour : 08 Avril 2026</p>
          </div>

          <div className="bg-primary/5 border border-primary/20 p-6 rounded-3xl flex gap-4 items-start">
            <AlertTriangle className="text-primary shrink-0" size={24} />
            <div className="text-sm leading-relaxed">
              <p className="font-bold text-primary mb-2 uppercase tracking-widest text-[10px]">Avertissement Important (DMCA / Copyright)</p>
              <p className="text-zinc-300">
                Sky Player Pro est exclusivement un <strong>lecteur multimédia</strong>. Nous ne fournissons, n'hébergeons, ne vendons et ne diffusons <strong>aucun contenu</strong> (chaînes TV, films, séries). Les utilisateurs sont seuls responsables du contenu qu'ils ajoutent via leurs propres listes de lecture M3U, JSON ou codes Xtream.
              </p>
            </div>
          </div>

          <section className="space-y-8">
            <div className="grid md:grid-cols-2 gap-8">
              <div className="space-y-4">
                <div className="flex items-center gap-3 text-primary">
                  <Globe size={20} />
                  <h2 className="font-black uppercase tracking-widest text-xs">1. Portée Internationale</h2>
                </div>
                <p className="text-zinc-400 text-sm leading-relaxed">
                  Ces conditions régissent l'utilisation de Sky Player Pro dans le respect du <strong>RGPD (Europe)</strong>, du <strong>CCPA/DMCA (USA)</strong> et des législations sur le commerce électronique en <strong>Afrique Francophone</strong>.
                </p>
              </div>

              <div className="space-y-4">
                <div className="flex items-center gap-3 text-primary">
                  <Lock size={20} />
                  <h2 className="font-black uppercase tracking-widest text-xs">2. Protection des Données (RGPD)</h2>
                </div>
                <p className="text-zinc-400 text-sm leading-relaxed">
                  Conformément au Règlement Général sur la Protection des Données (UE), nous collectons uniquement les données nécessaires au fonctionnement de l'app (Adresse MAC, Email). Vous disposez d'un droit d'accès, de rectification et de suppression.
                </p>
              </div>
            </div>

            <div className="space-y-6 pt-8 border-t border-zinc-900">
              <div className="flex items-center gap-3 text-primary">
                <Shield size={20} />
                <h2 className="font-black uppercase tracking-widest text-xs">3. Utilisation de l'Application</h2>
              </div>
              <div className="space-y-4 text-zinc-400 text-sm leading-relaxed">
                <p>
                  L'utilisateur s'engage à ne pas utiliser Sky Player Pro pour diffuser du contenu illégal ou protégé par des droits d'auteur sans l'autorisation des ayants droit. Sky Player Pro décline toute responsabilité en cas d'utilisation frauduleuse.
                </p>
                <p>
                  <strong>Activations :</strong> Les crédits achetés par les revendeurs sont destinés à l'activation technique de l'interface. Ils ne constituent en aucun cas un abonnement à un contenu média.
                </p>
              </div>
            </div>

            <div className="space-y-6 pt-8 border-t border-zinc-900">
              <div className="flex items-center gap-3 text-primary">
                <Scale size={20} />
                <h2 className="font-black uppercase tracking-widest text-xs">4. Remboursements et Crédits</h2>
              </div>
              <p className="text-zinc-400 text-sm leading-relaxed">
                En raison de la nature numérique des services (activations instantanées), aucun remboursement n'est possible une fois les crédits consommés ou l'appareil activé, conformément aux exceptions du droit de rétractation pour les contenus numériques.
              </p>
            </div>

            <div className="space-y-6 pt-8 border-t border-zinc-900">
              <div className="flex items-center gap-3 text-primary">
                <AlertTriangle size={20} />
                <h2 className="font-black uppercase tracking-widest text-xs">5. Limitation de Responsabilité</h2>
              </div>
              <p className="text-zinc-400 text-sm leading-relaxed">
                Sky Player Pro ne peut être tenu responsable des pannes de serveurs tiers, de la qualité des flux fournis par vos fournisseurs IPTV ou de la perte de données liée à une mauvaise configuration de l'utilisateur.
              </p>
            </div>

            <div className="space-y-6 pt-8 border-t border-zinc-900">
              <div className="flex items-center gap-3 text-primary">
                <Copyright size={20} />
                <h2 className="font-black uppercase tracking-widest text-xs">6. Propriété Intellectuelle</h2>
              </div>
              <p className="text-zinc-400 text-sm leading-relaxed">
                Le nom "Sky Player Pro", le logo et l'interface utilisateur sont la propriété exclusive de Sky Player. Toute reproduction ou modification sans autorisation est interdite.
              </p>
            </div>
          </section>

          <div className="pt-12 text-center">
            <p className="text-zinc-600 text-[10px] font-black uppercase tracking-[0.2em]">
              En utilisant cette application, vous acceptez l'intégralité de ces termes.
            </p>
          </div>
        </motion.div>
      </main>

      <Footer />
    </div>
  );
};
