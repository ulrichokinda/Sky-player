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

  const handleGoogleLogin = async () => {
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
        navigate(`${redirect}${plan ? `?plan=${plan}` : ''}`);
      } else {
        navigate('/dashboard');
      }
    } catch (error: any) {
      if (error.code === 'auth/popup-blocked') {
        alert("La fenêtre de connexion Google a été bloquée par votre navigateur. Veuillez autoriser les popups pour ce site ou ouvrir l'application dans un nouvel onglet.");
      } else {
        alert("Erreur Google Login: " + error.message);
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
    <div className="min-h-screen bg-black text-white font-sans flex items-center justify-center p-6 bg-[radial-gradient(circle_at_center,_var(--tw-gradient-stops))] from-primary/10 via-transparent to-transparent relative">
      <Link 
        to="/" 
        className="absolute top-8 left-8 p-3 bg-zinc-900/50 border border-zinc-800 rounded-full text-zinc-400 hover:text-white hover:bg-zinc-800 transition-all z-50 group"
        title="Retour à l'accueil"
      >
        <ArrowLeft size={20} className="group-hover:-translate-x-1 transition-transform" />
      </Link>
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-md"
      >
        <Card className="p-8 md:p-12 space-y-8 border-zinc-800/50 bg-zinc-900/30 backdrop-blur-2xl shadow-2xl shadow-primary/5">
          <header className="text-center space-y-4">
            <div className="mx-auto w-16 h-16 bg-primary/10 rounded-2xl flex items-center justify-center text-primary border border-primary/20">
              <LogIn size={32} />
            </div>
            <div className="space-y-2">
              <Badge variant="primary">Espace Revendeur</Badge>
              <h1 className="text-4xl font-black tracking-tighter">Connexion</h1>
              <p className="text-zinc-500 text-sm font-medium">Accédez à votre console d'administration.</p>
            </div>
          </header>

          <form onSubmit={handleLogin} className="space-y-6">
            <div className="space-y-4">
              <Input
                label="Email Professionnel"
                type="email"
                value={email}
                onChange={(e: any) => setEmail(e.target.value)}
                placeholder="votre@email.com"
                icon={Mail}
                required
              />
              <div className="space-y-1">
                <Input
                  label="Mot de passe"
                  type="password"
                  value={password}
                  onChange={(e: any) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  icon={Lock}
                  required
                />
                <button 
                  type="button"
                  onClick={handleForgotPassword}
                  disabled={resetLoading}
                  className="text-[10px] font-black text-primary uppercase tracking-widest hover:text-primary/80 transition-colors ml-2"
                >
                  {resetLoading ? 'Envoi...' : 'Mot de passe oublié ?'}
                </button>
              </div>
            </div>

            <Button 
              type="submit" 
              fullWidth 
              size="lg" 
              loading={loading}
              className="py-6 text-base group"
            >
              Se Connecter
              <ArrowRight size={18} className="ml-2 group-hover:translate-x-1 transition-transform" />
            </Button>
          </form>

          <div className="relative">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-zinc-800"></div>
            </div>
            <div className="relative flex justify-center text-[10px] uppercase tracking-[0.3em] font-black text-zinc-600">
              <span className="bg-zinc-950 px-4">OU</span>
            </div>
          </div>

          <Button 
            variant="outline" 
            fullWidth 
            onClick={handleGoogleLogin}
            className="border-zinc-800 hover:bg-white hover:text-black py-4"
          >
            <Chrome size={18} className="mr-2" />
            Continuer avec Google
          </Button>

          <footer className="text-center space-y-4">
            <p className="text-zinc-500 text-xs font-medium">
              Pas encore de compte ?{' '}
              <Link to="/register" className="text-primary font-black hover:underline">
                Créer un compte
              </Link>
            </p>
            <div className="flex items-center justify-center gap-2 text-zinc-700">
              <ShieldCheck size={12} />
              <span className="text-[10px] font-bold uppercase tracking-widest">Sécurisé par Sky Player</span>
            </div>
          </footer>
        </Card>
      </motion.div>
    </div>
  );
};
