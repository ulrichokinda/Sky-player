import React, { useState } from 'react';
import { useNavigate, Link, useSearchParams } from 'react-router-dom';
import { auth, signInWithEmailAndPassword, signInWithPopup, googleProvider, sendPasswordResetEmail } from '../firebase';
import { api } from '../services/api';
import { Card, Button, Input, Badge } from '../components/ui';
import { LogIn, Mail, Lock, Chrome, ArrowRight, ShieldCheck, ArrowLeft } from 'lucide-react';
import { motion } from 'motion/react';

export const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [resetLoading, setResetLoading] = useState(false);
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const userCredential = await signInWithEmailAndPassword(auth, email, password);
      const user = userCredential.user;

      if (!user.emailVerified) {
        await auth.signOut();
        alert("Votre adresse e-mail n'a pas encore été vérifiée. Veuillez vérifier votre boîte de réception (et vos spams) pour confirmer votre compte.");
        return;
      }
      
      const redirect = searchParams.get('redirect');
      const plan = searchParams.get('plan');
      
      if (redirect) {
        navigate(`${redirect}${plan ? `?plan=${plan}` : ''}`);
      } else {
        navigate('/dashboard');
      }
    } catch (error: any) {
      alert("Erreur de connexion: " + error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = async (e?: React.FormEvent) => {
    if (e && typeof e.preventDefault === 'function') {
      e.preventDefault();
    }
    setLoading(true);
    try {
      const result = await signInWithPopup(auth, googleProvider);
      const user = result.user;
      
      // Ensure user profile exists in Firestore
      await api.registerUser({
        uid: user.uid,
        email: user.email || '',
        username: user.displayName || user.email?.split('@')[0] || 'Utilisateur',
        role: 'client'
      });

      const redirect = searchParams.get('redirect');
      const plan = searchParams.get('plan');
      
      if (redirect) {
        window.location.href = `${window.location.origin}/#${redirect}${plan ? `?plan=${plan}` : ''}`;
      } else {
        window.location.href = `${window.location.origin}/#/dashboard`;
      }
    } catch (error: any) {
      if (error.code === 'auth/popup-blocked') {
        alert("La fenêtre de connexion Google a été bloquée par votre navigateur. Veuillez autoriser les popups pour ce site ou ouvrir l'application dans un nouvel onglet.");
      } else if (error.code === 'auth/unauthorized-domain') {
        const domain = window.location.hostname;
        const redirectUri = `https://${domain}/__/auth/handler`;
        alert(`ERREUR DE DOMAINE NON AUTORISÉ :
        
1. Allez dans Firebase Console > Authentification > Paramètres > Domaines Autorisés.
2. Ajoutez le domaine : "${domain}"
3. Ajoutez aussi votre domaine personnalisé : "skyplayerapp.xyz"

ÉGALEMENT (Très Important pour Google OAuth) :
1. Allez dans Google Cloud Console > Identifiants.
2. Modifiez votre Client OAuth 2.0 Web.
3. Dans "URIs de redirection autorisées", ajoutez :
   - ${redirectUri}
   - https://skyplayerapp.xyz/__/auth/handler`);
      } else if (error.code === 'auth/invalid-credential' || error.message.includes('auth/invalid-credential')) {
        alert(`ERREUR D'IDENTIFICATION (PROBLÈME DE CLÉ) : 
        
1. Allez dans votre Console Firebase > Authentification > Google.
2. Cliquez sur 'Enregistrer' sans rien changer (cela rafraîchit la liaison avec Google Cloud).
3. Vérifiez que votre Projet ID est bien correct dans le fichier de config.`);
      } else {
        alert("Erreur de connexion: " + error.message);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleForgotPassword = async () => {
    if (!email) {
      alert("Veuillez entrer votre email pour réinitialiser votre mot de passe.");
      return;
    }
    setResetLoading(true);
    try {
      await sendPasswordResetEmail(auth, email);
      alert("Un email de réinitialisation a été envoyé à " + email);
    } catch (error: any) {
      alert("Erreur: " + error.message);
    } finally {
      setResetLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-black text-white font-sans flex items-center justify-center p-6 relative overflow-hidden">
      {/* Background elements */}
      <div className="absolute top-0 left-0 w-full h-full bg-[radial-gradient(circle_at_20%_20%,_rgba(244,197,10,0.08)_0%,_transparent_50%)] pointer-events-none" />
      <div className="absolute bottom-0 right-0 w-full h-full bg-[radial-gradient(circle_at_80%_80%,_rgba(244,197,10,0.08)_0%,_transparent_50%)] pointer-events-none" />
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[800px] h-[800px] bg-primary/5 rounded-full blur-[120px] pointer-events-none" />

      <Link 
        to="/" 
        className="absolute top-8 left-8 p-3 bg-zinc-900/40 border border-zinc-800/50 backdrop-blur-xl rounded-full text-zinc-400 hover:text-white hover:bg-zinc-800 transition-all z-50 group"
        title="Retour à l'accueil"
      >
        <ArrowLeft size={18} className="group-hover:-translate-x-1 transition-transform" />
      </Link>

      <motion.div 
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.5, ease: [0, 0.71, 0.2, 1.01] }}
        className="w-full max-w-[440px]"
      >
        <Card className="p-8 md:p-10 space-y-8 border shadow-none border-zinc-800/80 bg-zinc-900/40 backdrop-blur-3xl relative overflow-hidden">
          {/* Decorative line */}
          <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-transparent via-primary/50 to-transparent" />
          
          <header className="text-center space-y-6">
            <div className="mx-auto w-20 h-20 bg-zinc-800/50 rounded-3xl flex items-center justify-center text-primary border border-zinc-700/50 shadow-inner group transition-all duration-500 hover:scale-105">
              <div className="relative">
                <LogIn size={36} className="relative z-10" />
                <div className="absolute inset-0 bg-primary/20 blur-xl rounded-full scale-150 opacity-0 group-hover:opacity-100 transition-opacity" />
              </div>
            </div>
            
            <div className="space-y-2">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary/10 border border-primary/20 text-[10px] font-black uppercase tracking-[0.2em] text-primary">
                <ShieldCheck size={12} />
                SKY PLAYER PRO
              </div>
              <h1 className="text-4xl font-black tracking-tighter uppercase italic"><span>Authentification</span></h1>
              <p className="text-zinc-500 text-sm font-medium max-w-[280px] mx-auto"><span>Accédez à votre infrastructure de distribution média sécurisée.</span></p>
            </div>
          </header>

          <form onSubmit={handleLogin} className="space-y-6">
            <div className="space-y-5">
              <div className="space-y-1.5 focus-within:z-20 relative">
                <Input
                  label="IDENTIFIANT EMAIL"
                  type="email"
                  value={email}
                  onChange={(e: any) => setEmail(e.target.value)}
                  placeholder="votre@email.com"
                  icon={Mail}
                  required
                  className="bg-zinc-950/50 border-zinc-800/50 focus:border-primary/50 h-14"
                />
              </div>
              <div className="space-y-1.5 focus-within:z-20 relative">
                <Input
                  label="MOT DE PASSE"
                  type="password"
                  value={password}
                  onChange={(e: any) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  icon={Lock}
                  required
                  className="bg-zinc-950/50 border-zinc-800/50 focus:border-primary/50 h-14"
                />
                <div className="flex justify-end pt-1">
                  <button 
                    type="button"
                    onClick={handleForgotPassword}
                    disabled={resetLoading}
                    className="text-[10px] font-black text-zinc-500 uppercase tracking-widest hover:text-primary transition-colors pr-2"
                  >
                    {resetLoading ? 'Envoi en cours...' : 'Mot de passe oublié ?'}
                  </button>
                </div>
              </div>
            </div>

            <Button 
              type="submit" 
              fullWidth 
              size="lg" 
              loading={loading}
              className="h-14 text-sm font-black uppercase tracking-widest group bg-primary hover:bg-primary/90 text-black border-none"
            >
              <span>Se Connecter</span>
              <ArrowRight size={18} className="ml-2 group-hover:translate-x-1 transition-transform" />
            </Button>
          </form>

          <div className="relative">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-zinc-800/50"></div>
            </div>
            <div className="relative flex justify-center text-[9px] uppercase tracking-[0.4em] font-black text-zinc-600">
              <span className="bg-zinc-900/60 px-4 backdrop-blur-sm"><span>Connexion Alternative</span></span>
            </div>
          </div>

          <button 
            type="button"
            onClick={() => handleGoogleLogin({} as React.FormEvent)}
            disabled={loading}
            className="w-full h-14 rounded-2xl border border-zinc-800 bg-zinc-950/50 hover:bg-white hover:text-black hover:border-white transition-all flex items-center justify-center gap-3 font-bold text-sm disabled:opacity-50 group"
          >
            <Chrome size={20} className="group-hover:scale-110 transition-transform" />
            <span>Continuer avec Google</span>
          </button>

          <footer className="text-center pt-4">
            <p className="text-zinc-500 text-xs font-medium">
              <span>Nouveau sur la plateforme ?</span>{' '}
              <Link to="/register" className="text-primary font-black hover:underline underline-offset-4 decoration-2">
                <span>Créer un compte pro</span>
              </Link>
            </p>
          </footer>
        </Card>
        
        <div className="mt-8 flex items-center justify-center gap-2 text-zinc-600">
          <p className="text-[10px] font-black uppercase tracking-[0.2em]">SKY PLAYER INFRASTRUCTURE &bull; SECURE NODES</p>
        </div>
      </motion.div>
    </div>
  );
};
