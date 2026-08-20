import React from 'react';
import { Footer } from '../components/Footer';
import { Header } from '../components/Header';
import { Card } from '../components/ui';
import { motion } from 'motion/react';
import { ChevronDown } from 'lucide-react';

const faqs = [
  { q: "Comment utiliser le Guide des Programmes (EPG) ?", a: "Dans le lecteur vidéo, cliquez sur l'icône 'Calendrier'. Vous verrez alors la liste des programmes en cours et à venir pour la chaîne sélectionnée, avec leurs descriptions et horaires." },
  { q: "Comment activer le mode Multi-Écran ?", a: "Cliquez sur l'icône 'Grille' (Grid) dans le lecteur. Vous pourrez alors choisir de diviser votre écran en 2 ou 4 parties et sélectionner une chaîne différente pour chaque fenêtre." },
  { q: "Comment fonctionne le Replay (Catch-up) ?", a: "Si votre fournisseur IPTV supporte le Replay, vous pouvez accéder aux archives directement depuis le guide EPG. Cliquez sur un programme passé pour lancer la lecture en différé." },
  { q: "Mes flux sont-ils protégés ?", a: "Oui, Sky Player intègre une technologie d'obfuscation qui masque l'URL de votre serveur dans le code de l'application, empêchant ainsi toute tentative de vol de vos accès par des tiers." },
  { q: "Supportez-vous le format m3u8 (HLS) ?", a: "Absolument. Sky Player est optimisé pour le protocole HLS (m3u8), qui est le standard le plus stable pour le streaming sur Smart TV et appareils mobiles, garantissant moins de buffering." },
  { q: "Où puis-je télécharger Sky Player ?", a: "Notre application est disponible en téléchargement sur le magasin Samsung Tizen TV, LG Content Store et le Google Play Store pour Android TV. Vous pouvez également télécharger l'APK directement depuis notre page d'accueil." },
  { q: "Le lecteur Sky Player contient-il des chaînes ?", a: "Non, Sky Player est exclusivement un lecteur multimédia. Vous devez posséder votre propre liste de lecture (M3U ou codes Xtream) pour utiliser l'application. Nous ne fournissons aucun contenu audiovisuel." },
  { q: "Quels sont les tarifs d'activation ?", a: "Sky Player propose deux formules : une activation annuelle à 2 000 F CFA ou une activation à vie (Lifetime) à 4 675 F CFA par appareil. Il n'y a aucun frais caché par la suite." },
  { q: "Comment verrouiller mon adresse MAC ?", a: "Pour protéger votre vie privée, vous pouvez verrouiller votre adresse MAC dans les paramètres de l'application. Cela empêche toute réinitialisation non autorisée de votre liste de lecture par un tiers." },
  { q: "Que faire si ma liste de lecture ne se charge pas ?", a: "Vérifiez d'abord votre connexion internet. Si le problème persiste, assurez-vous que votre lien M3U est toujours actif auprès de votre fournisseur. Sky Player affichera un message d'erreur spécifique si le lien est invalide." },
  { q: "Puis-je transférer mon activation sur un autre téléviseur ?", a: "L'activation est liée à l'adresse MAC unique de l'appareil. En cas de changement de téléviseur, une nouvelle activation sera nécessaire." },
  { q: "Quelle est votre politique de remboursement ?", a: "En raison de la nature numérique de l'activation, aucun remboursement n'est possible une fois l'adresse MAC activée. Toutefois, en cas d'erreur de saisie signalée sous 2h ou de problème technique majeur non résolu sous 72h, un remboursement peut être envisagé." }
];

export const FAQ = () => (
  <div className="min-h-screen bg-black text-white selection:bg-primary/30">
    <Header />
    
    <main className="max-w-4xl mx-auto px-6 py-12 md:py-20 space-y-12">
      <div className="text-center space-y-4">
        <motion.h1 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-5xl font-black tracking-tighter"
        >
          Questions <span className="text-primary">Fréquentes</span>
        </motion.h1>
        <p className="text-zinc-500">Tout ce que vous devez savoir sur Sky Player et son fonctionnement.</p>
      </div>

      <div className="space-y-4">
        {faqs.map((faq, i) => (
          <motion.div 
            key={i}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.05 }}
          >
            <Card className="group hover:border-primary/30 transition-all cursor-default">
              <h3 className="text-lg font-bold text-white mb-3 flex items-center justify-between">
                {faq.q}
                <ChevronDown size={18} className="text-zinc-600 group-hover:text-primary transition-colors" />
              </h3>
              <p className="text-zinc-400 leading-relaxed text-sm">{faq.a}</p>
            </Card>
          </motion.div>
        ))}
      </div>
    </main>

    <Footer />
  </div>
);
