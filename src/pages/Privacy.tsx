import React from 'react';
import { Footer } from '../components/Footer';
import { Header } from '../components/Header';
import { Card } from '../components/ui';
import { motion } from 'motion/react';

export const Privacy = () => (
  <div className="min-h-screen bg-black text-white selection:bg-primary/30">
    <Header />
    
    <main className="max-w-4xl mx-auto px-6 py-12 md:py-20 space-y-16">
      <section className="space-y-6">
        <motion.h1 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-5xl font-black tracking-tighter"
        >
          Politique de <span className="text-primary">Confidentialité</span>
        </motion.h1>
        <p className="text-zinc-400 leading-relaxed">
          Dernière mise à jour : 6 Avril 2026. Chez Sky Player, la protection de votre vie privée est notre priorité absolue. Cette politique détaille comment nous traitons vos informations conformément aux législations sur la protection des données personnelles en vigueur en Afrique francophone (notamment les recommandations de la CDP, APDP, et CIL).
        </p>
      </section>

      <div className="space-y-12">
        <section className="space-y-4">
          <h2 className="text-2xl font-bold text-white">1. Collecte des Informations</h2>
          <div className="text-zinc-400 space-y-4 leading-relaxed">
            <p>Nous collectons uniquement les informations strictement nécessaires au bon fonctionnement de nos services :</p>
            <ul className="list-disc pl-6 space-y-2">
              <li><span className="text-white font-medium">Informations de compte :</span> Votre adresse email et votre numéro de téléphone lors de votre inscription en tant que revendeur.</li>
              <li><span className="text-white font-medium">Données techniques :</span> L'adresse MAC de votre appareil pour l'activation de l'application.</li>
              <li><span className="text-white font-medium">Transactions :</span> Les détails relatifs aux paiements effectués via Mobile Money (Orange Money, MTN Mobile Money, Moov Money, etc.) pour le suivi de vos crédits.</li>
            </ul>
            <p className="italic text-primary/80">Note : Sky Player ne collecte jamais le contenu de vos listes de lecture IPTV.</p>
          </div>
        </section>

        <section className="space-y-4">
          <h2 className="text-2xl font-bold text-white">2. Utilisation des Données</h2>
          <div className="text-zinc-400 space-y-4 leading-relaxed">
            <p>Vos données sont utilisées exclusivement pour :</p>
            <ul className="list-disc pl-6 space-y-2">
              <li>Gérer votre compte revendeur et vos activations.</li>
              <li>Assurer le support technique et répondre à vos demandes.</li>
              <li>Sécuriser les transactions financières.</li>
              <li>Améliorer les performances de l'application sur les réseaux locaux.</li>
            </ul>
          </div>
        </section>

        <section className="space-y-4">
          <h2 className="text-2xl font-bold text-white">3. Partage et Transfert</h2>
          <p className="text-zinc-400 leading-relaxed">
            Sky Player ne vend, ne loue et n'échange jamais vos données personnelles avec des tiers à des fins commerciales. Vos informations ne sont partagées qu'avec nos prestataires de services de paiement (agrégateurs Mobile Money) pour finaliser vos transactions, et ce, sous des protocoles de sécurité stricts.
          </p>
        </section>

        <section className="space-y-4">
          <h2 className="text-2xl font-bold text-white">4. Sécurité des Données</h2>
          <p className="text-zinc-400 leading-relaxed">
            Nous mettons en œuvre des mesures de sécurité techniques et organisationnelles robustes (chiffrement SSL/TLS, pare-feu, contrôles d'accès) pour protéger vos données contre tout accès non autorisé, modification ou destruction. Nos serveurs sont hébergés dans des centres de données hautement sécurisés.
          </p>
        </section>

        <section className="space-y-4">
          <h2 className="text-2xl font-bold text-white">5. Vos Droits</h2>
          <div className="text-zinc-400 space-y-4 leading-relaxed">
            <p>Conformément aux lois sur la protection des données, vous disposez des droits suivants :</p>
            <ul className="list-disc pl-6 space-y-2">
              <li><span className="text-white font-medium">Droit d'accès :</span> Obtenir une copie de vos données.</li>
              <li><span className="text-white font-medium">Droit de rectification :</span> Corriger des informations inexactes.</li>
              <li><span className="text-white font-medium">Droit à l'effacement :</span> Demander la suppression de votre compte.</li>
              <li><span className="text-white font-medium">Droit d'opposition :</span> Refuser l'utilisation de vos données pour certaines finalités.</li>
            </ul>
            <p>Pour exercer ces droits, contactez notre Délégué à la Protection des Données (DPO) via la page Contact.</p>
          </div>
        </section>

        <section className="space-y-4">
          <h2 className="text-2xl font-bold text-white">6. Cookies</h2>
          <p className="text-zinc-400 leading-relaxed">
            Notre site utilise des cookies essentiels pour maintenir votre session active et mémoriser vos préférences de navigation. Vous pouvez désactiver les cookies dans les réglages de votre navigateur, bien que cela puisse affecter certaines fonctionnalités du tableau de bord.
          </p>
        </section>

        <section className="space-y-4">
          <h2 className="text-2xl font-bold text-white">7. Conservation des Données</h2>
          <p className="text-zinc-400 leading-relaxed">
            Nous conservons vos données personnelles uniquement le temps nécessaire aux finalités pour lesquelles elles ont été collectées :
          </p>
          <ul className="list-disc pl-6 space-y-2 text-zinc-400">
            <li>Les données de compte revendeur sont conservées tant que le compte est actif.</li>
            <li>Les données de transaction sont conservées pendant 10 ans conformément aux obligations légales et comptables.</li>
            <li>Les adresses MAC activées sont conservées pour la durée de l'abonnement choisi (1 an ou à vie).</li>
          </ul>
        </section>

        <section className="space-y-4">
          <h2 className="text-2xl font-bold text-white">8. Consentement</h2>
          <p className="text-zinc-400 leading-relaxed">
            En utilisant nos services, vous consentez explicitement à la collecte et au traitement de vos données tels que décrits dans cette politique. Vous pouvez retirer votre consentement à tout moment, ce qui entraînera la clôture de votre compte et l'arrêt des services associés.
          </p>
        </section>

        <section className="space-y-4">
          <h2 className="text-2xl font-bold text-white">9. Hébergement et Transfert International</h2>
          <p className="text-zinc-400 leading-relaxed">
            Bien que nos services s'adressent principalement au marché africain, nos serveurs peuvent être situés dans des zones géographiques offrant des standards de protection élevés (Union Européenne). Tout transfert de données hors de votre pays de résidence est effectué dans le respect des mécanismes de protection juridique appropriés.
          </p>
        </section>

        <section className="space-y-4">
          <h2 className="text-2xl font-bold text-white">10. Modifications de la Politique</h2>
          <p className="text-zinc-400 leading-relaxed">
            Sky Player se réserve le droit de modifier cette politique à tout moment. Les utilisateurs seront informés de tout changement significatif par email ou via une notification sur le tableau de bord.
          </p>
        </section>

        <section className="space-y-4">
          <h2 className="text-2xl font-bold text-white">11. Politique de Remboursement</h2>
          <div className="text-zinc-400 space-y-4 leading-relaxed">
            <p>Compte tenu de la nature numérique de nos services (activation logicielle immédiate), les conditions de remboursement suivantes s'appliquent :</p>
            <ul className="list-disc pl-6 space-y-2">
              <li><span className="text-white font-medium">Absence de droit de rétractation :</span> Une fois l'adresse MAC activée sur nos serveurs, le service est considéré comme consommé et aucun remboursement ne pourra être effectué.</li>
              <li><span className="text-white font-medium">Erreur d'activation :</span> Si vous avez commis une erreur lors de la saisie de votre adresse MAC, contactez le support dans les 2 heures suivant le paiement. Un transfert gracieux pourra être envisagé à la discrétion de l'équipe technique.</li>
              <li><span className="text-white font-medium">Problèmes de contenu :</span> Sky Player étant uniquement un lecteur, aucun remboursement ne sera accordé pour des problèmes liés à la qualité, à la disponibilité ou au contenu des chaînes fournies par votre opérateur IPTV tiers.</li>
              <li><span className="text-white font-medium">Dysfonctionnement technique :</span> En cas de défaut technique majeur de l'application empêchant son utilisation et non résolu par notre support sous 72 heures, un remboursement intégral pourra être traité via le mode de paiement initial (Mobile Money).</li>
            </ul>
          </div>
        </section>

        <Card className="bg-primary/5 border-primary/20 p-8">
          <h2 className="text-xl font-bold text-primary mb-4">Contact</h2>
          <p className="text-zinc-400 text-sm leading-relaxed">
            Pour toute question concernant cette politique de confidentialité ou pour toute réclamation, vous pouvez nous écrire à : <span className="text-white font-bold">inestaulrichokinda@gmail.com</span> ou via notre formulaire de contact en ligne.
          </p>
        </Card>
      </div>
    </main>

    <Footer />
  </div>
);
