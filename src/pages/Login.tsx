import React, { useState } from 'react';
import { useNavigate, Link, useSearchParams } from 'react-router-dom';
import { auth, signInWithEmailAndPassword, sendPasswordResetEmail } from '../firebase';
import { LogIn, Mail, Lock, ArrowRight, ArrowLeft, Eye, EyeOff, CheckCircle } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { MosaicWaves } from '../components/MosaicWaves';

export const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [resetLoading, setResetLoading] = useState(false);
  const [resetSent, setResetSent] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const userCredential = await signInWithEmailAndPassword(auth, email, password);
      const user = userCredential.user;

      if (!user.emailVerified) {
        await auth.signOut();
        setError("Email non vérifié. Vérifiez votre boîte de réception.");
        return;
      }

      const redirect = searchParams.get('redirect');
      const plan = searchParams.get('plan');
      if (redirect) {
        navigate(`${redirect}${plan ? `?plan=${plan}` : ''}`);
      } else {
        navigate('/dashboard');
      }
    } catch (err: any) {
      const msg = err.code === 'auth/user-not-found'
        ? 'Aucun compte trouvé avec cet email.'
        : err.code === 'auth/wrong-password'
        ? 'Mot de passe incorrect.'
        : err.code === 'auth/too-many-requests'
        ? 'Trop de tentatives. Réessayez plus tard.'
        : 'Erreur de connexion. Vérifiez vos identifiants.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleForgotPassword = async () => {
    if (!email) { setError('Entrez votre email pour réinitialiser.'); return; }
    setResetLoading(true);
    try {
      await sendPasswordResetEmail(auth, email);
      setResetSent(true);
    } catch (err: any) {
      setError(err.code === 'auth/user-not-found' ? 'Aucun compte trouvé.' : 'Erreur d\'envoi.');
    } finally {
      setResetLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#030712] text-white flex items-center justify-center p-4 relative overflow-hidden">
      {/* Background */}
      <div className="fixed inset-0 z-0">
        <MosaicWaves
          width="100%" height="100%"
          pitch={6} fill={0.4} shape="dot" speed={0.6}
          color="#1e40af" hotColor="#60a5fa" backgroundColor="#030712"
          opacity={0.7} vignette={0.4} gamma={2.0}
          cursorInteraction={true} cursorGlow={0.3}
        />
      </div>

      {/* Ambient glow */}
      <div className="fixed top-1/4 left-1/4 w-96 h-96 bg-blue-600/10 rounded-full blur-[120px] pointer-events-none z-0" />
      <div className="fixed bottom-1/4 right-1/4 w-80 h-80 bg-indigo-500/8 rounded-full blur-[100px] pointer-events-none z-0" />

      {/* Back button */}
      <Link
        to="/"
        className="fixed top-6 left-6 z-50 p-2.5 rounded-full border border-white/10 bg-white/5 backdrop-blur-md text-white/50 hover:text-white hover:bg-white/10 hover:border-white/20 transition-all duration-300"
      >
        <ArrowLeft size={18} />
      </Link>

      {/* Card */}
      <motion.div
        initial={{ opacity: 0, y: 30, scale: 0.96 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
        className="relative z-10 w-full max-w-md"
      >
        <div className="relative rounded-3xl border border-white/[0.06] bg-white/[0.03] backdrop-blur-xl overflow-hidden">
          {/* Top gradient line */}
          <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-blue-500/50 to-transparent" />

          <div className="p-8 sm:p-10 space-y-8">
            {/* Header */}
            <div className="text-center space-y-4">
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ delay: 0.2, type: 'spring', stiffness: 200 }}
                className="mx-auto w-14 h-14 rounded-2xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center"
              >
                <LogIn size={24} className="text-blue-400" />
              </motion.div>

              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.3 }}
              >
                <p className="text-[10px] font-bold uppercase tracking-[0.25em] text-blue-400/70 mb-2">
                  Sky Player Pro
                </p>
                <h1 className="text-2xl font-bold tracking-tight text-white/90">
                  Connexion
                </h1>
                <p className="text-sm text-white/30 mt-1">
                  Accédez à votre espace de streaming
                </p>
              </motion.div>
            </div>

            {/* Error */}
            <AnimatePresence>
              {error && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  className="px-4 py-3 rounded-xl bg-red-500/10 border border-red-500/20 text-red-400 text-xs font-medium"
                >
                  {error}
                </motion.div>
              )}
            </AnimatePresence>

            {/* Reset sent */}
            <AnimatePresence>
              {resetSent && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  className="px-4 py-3 rounded-xl bg-green-500/10 border border-green-500/20 text-green-400 text-xs font-medium flex items-center gap-2"
                >
                  <CheckCircle size={14} />
                  Email de réinitialisation envoyé à {email}
                </motion.div>
              )}
            </AnimatePresence>

            {/* Form */}
            <form onSubmit={handleLogin} className="space-y-4">
              {/* Email */}
              <motion.div
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.35 }}
                className="space-y-1.5"
              >
                <label className="text-[10px] font-bold uppercase tracking-widest text-white/30 pl-1">
                  Email
                </label>
                <div className="relative group">
                  <div className="absolute inset-0 rounded-xl bg-blue-500/5 opacity-0 group-focus-within:opacity-100 transition-opacity duration-500 blur-sm" />
                  <div className="relative flex items-center bg-white/[0.04] border border-white/[0.08] rounded-xl overflow-hidden focus-within:border-blue-500/30 transition-colors duration-300">
                    <div className="pl-4 text-white/20 group-focus-within:text-blue-400 transition-colors">
                      <Mail size={18} />
                    </div>
                    <input
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="votre@email.com"
                      required
                      className="w-full bg-transparent px-4 py-4 text-sm text-white/90 placeholder:text-white/20 outline-none"
                    />
                  </div>
                </div>
              </motion.div>

              {/* Password */}
              <motion.div
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.4 }}
                className="space-y-1.5"
              >
                <label className="text-[10px] font-bold uppercase tracking-widest text-white/30 pl-1">
                  Mot de passe
                </label>
                <div className="relative group">
                  <div className="absolute inset-0 rounded-xl bg-blue-500/5 opacity-0 group-focus-within:opacity-100 transition-opacity duration-500 blur-sm" />
                  <div className="relative flex items-center bg-white/[0.04] border border-white/[0.08] rounded-xl overflow-hidden focus-within:border-blue-500/30 transition-colors duration-300">
                    <div className="pl-4 text-white/20 group-focus-within:text-blue-400 transition-colors">
                      <Lock size={18} />
                    </div>
                    <input
                      type={showPassword ? 'text' : 'password'}
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="••••••••"
                      required
                      className="w-full bg-transparent px-4 py-4 text-sm text-white/90 placeholder:text-white/20 outline-none"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="pr-4 text-white/20 hover:text-white/50 transition-colors"
                    >
                      {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                </div>
              </motion.div>

              {/* Forgot password */}
              <div className="flex justify-end">
                <button
                  type="button"
                  onClick={handleForgotPassword}
                  disabled={resetLoading}
                  className="text-[11px] font-medium text-white/25 hover:text-blue-400 transition-colors pr-1"
                >
                  {resetLoading ? 'Envoi...' : 'Mot de passe oublié ?'}
                </button>
              </div>

              {/* Submit */}
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.45 }}
                whileHover={{ scale: 1.01 }}
                whileTap={{ scale: 0.99 }}
              >
                <button
                  type="submit"
                  disabled={loading}
                  className="w-full py-4 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-sm tracking-wide transition-all duration-300 flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed shadow-[0_0_30px_rgba(37,99,235,0.2)] hover:shadow-[0_0_40px_rgba(37,99,235,0.35)]"
                >
                  {loading ? (
                    <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  ) : (
                    <>
                      Se connecter
                      <ArrowRight size={16} className="group-hover:translate-x-1 transition-transform" />
                    </>
                  )}
                </button>
              </motion.div>
            </form>

            {/* Footer */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.5 }}
              className="text-center pt-4 border-t border-white/[0.05]"
            >
              <p className="text-white/30 text-xs">
                Pas encore de compte ?{' '}
                <Link to="/register" className="text-blue-400 hover:text-blue-300 font-semibold transition-colors">
                  Créer un compte
                </Link>
              </p>
            </motion.div>
          </div>
        </div>

        {/* Bottom text */}
        <p className="text-center text-[9px] uppercase tracking-[0.3em] text-white/10 mt-6 font-bold">
          Sky Player Infrastructure
        </p>
      </motion.div>
    </div>
  );
};
