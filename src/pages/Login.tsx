import React, { useState } from 'react';
import { useNavigate, Link, useSearchParams } from 'react-router-dom';
import { auth, signInWithEmailAndPassword, sendPasswordResetEmail } from '../firebase';
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
    <div className="min-h-screen bg-black text-white font-sans flex flex-col items-center justify-center p-4 sm:p-6 relative overflow-x-hidden py-12 sm:py-24 perspective-[2000px]">
      {/* 3D Background elements */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,_rgba(244,197,10,0.15)_0%,_transparent_40%)] pointer-events-none" />
      <div className="absolute bottom-0 right-0 w-full h-full bg-[radial-gradient(ellipse_at_80%_80%,_rgba(244,197,10,0.1)_0%,_transparent_50%)] pointer-events-none" />
      <motion.div 
        animate={{ rotate: 360 }}
        transition={{ duration: 150, repeat: Infinity, ease: "linear" }}
        className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[800px] h-[800px] bg-primary/10 rounded-full blur-[120px] pointer-events-none" 
      />

      <Link 
        to="/" 
        className="absolute top-6 left-6 sm:top-8 sm:left-8 p-3 bg-zinc-900/40 border border-zinc-800/50 backdrop-blur-xl rounded-full text-zinc-400 hover:text-white hover:bg-zinc-800 hover:scale-110 transition-all z-50 group hover:shadow-[0_0_20px_rgba(244,197,10,0.2)]"
        title="Retour à l'accueil"
      >
        <ArrowLeft size={18} className="group-hover:-translate-x-1 transition-transform" />
      </Link>

      <motion.div 
        initial={{ opacity: 0, rotateX: 20, y: 40, scale: 0.9 }}
        animate={{ opacity: 1, rotateX: 0, y: 0, scale: 1 }}
        transition={{ duration: 0.7, ease: [0.16, 1, 0.3, 1] }}
        className="w-full max-w-[480px] relative z-10"
        style={{ transformStyle: 'preserve-3d' }}
      >
        <Card 
          className="p-6 sm:p-10 space-y-8 border-none shadow-[0_20px_50px_rgba(0,0,0,0.5)] bg-zinc-900/40 backdrop-blur-3xl relative overflow-hidden ring-1 ring-zinc-800/50 hover:ring-primary/30 transition-all duration-700"
          style={{ transform: "translateZ(20px)" }}
        >
          {/* Decorative glowing gradient inside card */}
          <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-transparent via-primary to-transparent opacity-50" />
          <div className="absolute inset-0 bg-gradient-to-b from-primary/5 to-transparent pointer-events-none" />
          
          <header className="text-center space-y-6 relative z-10">
            <motion.div 
              whileHover={{ scale: 1.1, rotateY: 180 }}
              transition={{ duration: 0.6 }}
              className="mx-auto w-20 h-20 bg-zinc-950/80 rounded-2xl flex items-center justify-center text-primary border border-zinc-800/80 shadow-[0_0_30px_rgba(244,197,10,0.15)] group transition-all duration-500 transform-gpu"
            >
              <div className="relative">
                <LogIn size={36} className="relative z-10" />
                <div className="absolute inset-0 bg-primary/30 blur-xl rounded-full scale-150 opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
              </div>
            </motion.div>
            
            <div className="space-y-3">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-zinc-950/50 border border-primary/20 text-[10px] font-black uppercase tracking-[0.2em] text-primary shadow-[0_0_10px_rgba(244,197,10,0.1)]">
                <ShieldCheck size={12} className="text-primary" />
                SKY PLAYER PRO
              </div>
              <h1 className="text-3xl sm:text-4xl font-black tracking-tighter uppercase italic text-transparent bg-clip-text bg-gradient-to-r from-white via-zinc-200 to-zinc-400 drop-shadow-sm">
                Authentification
              </h1>
              <p className="text-zinc-500 text-xs sm:text-sm font-medium w-full mx-auto leading-relaxed">
                Accédez à votre infrastructure de distribution média sécurisée.
              </p>
            </div>
          </header>

          <form onSubmit={handleLogin} className="space-y-6 relative z-10">
            <div className="space-y-5">
              <div className="space-y-1.5 focus-within:z-20 relative group">
                <div className="absolute -inset-0.5 bg-gradient-to-r from-primary/0 via-primary/20 to-primary/0 rounded-xl opacity-0 group-focus-within:opacity-100 transition duration-500 blur-sm"></div>
                <Input
                  label="IDENTIFIANT EMAIL"
                  type="email"
                  value={email}
                  onChange={(e: any) => setEmail(e.target.value)}
                  placeholder="votre@email.com"
                  icon={Mail}
                  required
                  className="bg-zinc-950/80 border-zinc-800/80 focus:border-primary/50 h-14 sm:h-16 relative z-10 shadow-inner"
                />
              </div>
              <div className="space-y-1.5 focus-within:z-20 relative group">
                <div className="absolute -inset-0.5 bg-gradient-to-r from-primary/0 via-primary/20 to-primary/0 rounded-xl opacity-0 group-focus-within:opacity-100 transition duration-500 blur-sm"></div>
                <Input
                  label="MOT DE PASSE"
                  type="password"
                  value={password}
                  onChange={(e: any) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  icon={Lock}
                  required
                  className="bg-zinc-950/80 border-zinc-800/80 focus:border-primary/50 h-14 sm:h-16 relative z-10 shadow-inner"
                />
                <div className="flex justify-end pt-2">
                  <button 
                    type="button"
                    onClick={handleForgotPassword}
                    disabled={resetLoading}
                    className="text-[10px] sm:text-xs font-black text-zinc-500 uppercase tracking-widest hover:text-primary transition-colors pr-2 flex items-center gap-1"
                  >
                    {resetLoading ? 'Envoi en cours...' : 'Mot de passe oublié ?'}
                  </button>
                </div>
              </div>
            </div>

            <motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
              <Button 
                type="submit" 
                fullWidth 
                size="lg" 
                loading={loading}
                className="h-14 sm:h-16 text-xs sm:text-sm font-black uppercase tracking-widest group bg-gradient-to-r from-primary via-[#ffb900] to-primary hover:from-[#ffb900] hover:to-primary text-black border-none shadow-[0_0_20px_rgba(244,197,10,0.3)] transition-all overflow-hidden relative"
              >
                <span className="relative z-10 flex items-center justify-center">
                  Se Connecter
                  <ArrowRight size={18} className="shrink-0 ml-2 group-hover:translate-x-2 transition-transform" />
                </span>
              </Button>
            </motion.div>
          </form>

          <footer className="text-center pt-6 pb-2 relative z-10 border-t border-zinc-800/50 mt-6">
            <p className="text-zinc-400 text-xs sm:text-sm font-medium mt-6">
              <span>Nouveau sur la plateforme ?</span>{' '}
              <Link to="/register" className="text-primary font-black hover:text-white transition-colors underline-offset-4 decoration-2 hover:underline">
                <span>Créer un compte pro</span>
              </Link>
            </p>
          </footer>
        </Card>
        
        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5, duration: 0.5 }}
          className="mt-8 sm:mt-10 flex items-center justify-center gap-2 text-zinc-600 drop-shadow-md"
        >
          <p className="text-[9px] sm:text-[10px] font-black uppercase tracking-[0.2em] text-center w-full break-words px-4">
            SKY PLAYER INFRASTRUCTURE &bull; SECURE NODES
          </p>
        </motion.div>
      </motion.div>
    </div>
  );
};
