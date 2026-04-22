import React from 'react';
import { Link } from 'react-router-dom';

export const Footer = () => (
  <footer className="border-t border-zinc-800 mt-12 py-8 text-center text-zinc-500 text-sm">
    <div className="flex flex-wrap justify-center gap-6 mb-4">
      <Link to="/about" className="hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded-lg px-2">À propos</Link>
      <Link to="/contact" className="hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded-lg px-2">Contact</Link>
      <Link to="/privacy" rel="privacy-policy" className="hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded-lg px-2">Politique de Confidentialité</Link>
      <Link to="/terms" rel="terms-of-service" className="hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded-lg px-2">Conditions Générales</Link>
      <Link to="/faq" className="hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded-lg px-2">FAQ</Link>
      <Link to="/assets" className="hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded-lg px-2">Kit Média</Link>
    </div>
    <div className="mb-4">
      <a href="mailto:contact@skyplayerapp.xyz" className="text-primary hover:text-white transition-colors">
        contact@skyplayerapp.xyz
      </a>
    </div>
    <p>© 2026 Sky Player. Tous droits réservés.</p>
  </footer>
);
