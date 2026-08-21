import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { auth, createUserWithEmailAndPassword, sendEmailVerification, signOut } from '../firebase';
import { api } from '../services/api';
import { Mail, Lock, User, Phone, Globe, ArrowRight, ArrowLeft, Eye, EyeOff, UserPlus, CheckCircle, XCircle } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { MosaicWaves } from '../components/MosaicWaves';

const COUNTRIES = [
  'Bénin', 'Burkina Faso', 'Cameroun', 'Centrafrique', 'Congo (Brazzaville)',
  "Côte d'Ivoire", 'Gabon', 'Guinée', 'Mali', 'Niger', 'RDC (Kinshasa)',
  'Sénégal', 'TCHAD', 'Togo', 'Autre'
];

const PWD_RULES = [
  { test: (p: string) => p.length >= 8, label: '8 caractères minimum' },
  { test: (p: string) => /[A-Z]/.test(p), label: 'Une majuscule' },
  { test: (p: string) => /[a-z]/.test(p), label: 'Une minuscule' },
  { test: (p: string) => /\d/.test(p), label: 'Un chiffre' },
  { test: (p: string) => /[@$!%*?&]/.test(p), label: 'Un caractère spécial' },
];

function FieldInput({ label, type = 'text', icon: Icon, value, onChange, placeholder, required, rightElement, error }: {
  label: string; type?: string; icon: any; value: string; onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  placeholder?: string; required?: boolean; rightElement?: React.ReactNode; error?: string;
}) {
  return (
    <div className="space-y-1.5">
      <label className="text-[10px] font-bold uppercase tracking-widest text-white/30 pl-1">{label}</label>
      <div className="relative group">
        <div className="absolute inset-0 rounded-xl bg-blue-500/5 opacity-0 group-focus-within:opacity-100 transition-opacity duration-500 blur-sm" />
        <div className="relative flex items-center bg-white/[0.04] border border-white/[0.08] rounded-xl overflow-hidden focus-within:border-blue-500/30 transition-colors duration-300">
          <div className="pl-4 text-white/20 group-focus-within:text-blue-400 transition-colors shrink-0">
            <Icon size={18} />
          </div>
          <input
            type={type} value={value} onChange={onChange} placeholder={placeholder} required={required}
            className="w-full bg-transparent px-4 py-3.5 text-sm text-white/90 placeholder:text-white/20 outline-none min-w-0"
          />
          {rightElement}
        </div>
      </div>
      {error && <p className="text-[11px] text-red-400 pl-1">{error}</p>}
    </div>
  );
}

export const Register = () => {
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [phone, setPhone] = useState('');
  const [country, setCountry] = useState(COUNTRIES[0]);
  const [customCountry, setCustomCountry] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const navigate = useNavigate();

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (password !== confirmPassword) { setError('Les mots de passe ne correspondent pas.'); return; }
    if (!PWD_RULES.every(r => r.test(password))) { setError('Le mot de passe ne respecte pas les critères.'); return; }
    if (country === 'Autre' && !customCountry) { setError('Précisez votre pays.'); return; }

    setLoading(true);
    try {
      const result = await createUserWithEmailAndPassword(auth, email, password);
      await sendEmailVerification(result.user);
      await api.registerUser({
        uid: result.user.uid, email, username, firstName, lastName, phone,
        country: country === 'Autre' ? customCountry : country, role: 'client'
      });
      await signOut(auth);
      setSuccess('Compte créé ! Vérifiez votre email pour activer votre compte.');
      setTimeout(() => navigate('/login'), 3000);
    } catch (err: any) {
      const msg = err.code === 'auth/email-already-in-use' ? 'Cet email est déjà utilisé.'
        : err.code === 'auth/weak-password' ? 'Mot de passe trop faible.'
        : 'Erreur lors de l\'inscription.';
      setError(msg);
    } finally { setLoading(false); }
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

      <div className="fixed top-1/4 left-1/4 w-96 h-96 bg-blue-600/10 rounded-full blur-[120px] pointer-events-none z-0" />
      <div className="fixed bottom-1/4 right-1/4 w-80 h-80 bg-indigo-500/8 rounded-full blur-[100px] pointer-events-none z-0" />

      <Link to="/" className="fixed top-6 left-6 z-50 p-2.5 rounded-full border border-white/10 bg-white/5 backdrop-blur-md text-white/50 hover:text-white hover:bg-white/10 hover:border-white/20 transition-all duration-300">
        <ArrowLeft size={18} />
      </Link>

      <motion.div
        initial={{ opacity: 0, y: 30, scale: 0.96 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
        className="relative z-10 w-full max-w-lg"
      >
        <div className="relative rounded-3xl border border-white/[0.06] bg-white/[0.03] backdrop-blur-xl overflow-hidden">
          <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-blue-500/50 to-transparent" />

          <div className="p-8 sm:p-10 space-y-7">
            {/* Header */}
            <div className="text-center space-y-4">
              <motion.div
                initial={{ scale: 0 }} animate={{ scale: 1 }}
                transition={{ delay: 0.2, type: 'spring', stiffness: 200 }}
                className="mx-auto w-14 h-14 rounded-2xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center"
              >
                <UserPlus size={24} className="text-blue-400" />
              </motion.div>
              <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.3 }}>
                <p className="text-[10px] font-bold uppercase tracking-[0.25em] text-blue-400/70 mb-2">Sky Player Pro</p>
                <h1 className="text-2xl font-bold tracking-tight text-white/90">Créer un compte</h1>
                <p className="text-sm text-white/30 mt-1">Rejoignez le réseau de streaming</p>
              </motion.div>
            </div>

            {/* Alerts */}
            <AnimatePresence>
              {error && (
                <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} exit={{ opacity: 0, height: 0 }}
                  className="px-4 py-3 rounded-xl bg-red-500/10 border border-red-500/20 text-red-400 text-xs font-medium">{error}</motion.div>
              )}
              {success && (
                <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} exit={{ opacity: 0, height: 0 }}
                  className="px-4 py-3 rounded-xl bg-green-500/10 border border-green-500/20 text-green-400 text-xs font-medium flex items-center gap-2">
                  <CheckCircle size={14} /> {success}
                </motion.div>
              )}
            </AnimatePresence>

            {/* Form */}
            <form onSubmit={handleRegister} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <motion.div initial={{ opacity: 0, x: -15 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: 0.35 }}>
                  <FieldInput label="Prénom" icon={User} value={firstName} onChange={e => setFirstName(e.target.value)} placeholder="Jean" required />
                </motion.div>
                <motion.div initial={{ opacity: 0, x: 15 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: 0.35 }}>
                  <FieldInput label="Nom" icon={User} value={lastName} onChange={e => setLastName(e.target.value)} placeholder="Dupont" required />
                </motion.div>
              </div>

              <motion.div initial={{ opacity: 0, x: -15 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: 0.4 }}>
                <FieldInput label="Nom d'utilisateur" icon={User} value={username} onChange={e => setUsername(e.target.value)} placeholder="jeandupont" required />
              </motion.div>

              <motion.div initial={{ opacity: 0, x: -15 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: 0.45 }}>
                <FieldInput label="Email" type="email" icon={Mail} value={email} onChange={e => setEmail(e.target.value)} placeholder="votre@email.com" required />
              </motion.div>

              <div className="grid grid-cols-2 gap-3">
                <motion.div initial={{ opacity: 0, x: -15 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: 0.5 }}>
                  <FieldInput label="Mot de passe" type={showPassword ? 'text' : 'password'} icon={Lock}
                    value={password} onChange={e => setPassword(e.target.value)} placeholder="••••••••" required
                    rightElement={<button type="button" onClick={() => setShowPassword(!showPassword)} className="p-2.5 text-white/20 hover:text-white/50 transition-colors shrink-0">{showPassword ? <EyeOff size={15} /> : <Eye size={15} />}</button>} />
                </motion.div>
                <motion.div initial={{ opacity: 0, x: 15 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: 0.5 }}>
                  <FieldInput label="Confirmer" type={showConfirm ? 'text' : 'password'} icon={Lock}
                    value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} placeholder="••••••••" required
                    error={confirmPassword && password !== confirmPassword ? 'Ne correspond pas' : ''}
                    rightElement={<button type="button" onClick={() => setShowConfirm(!showConfirm)} className="p-2.5 text-white/20 hover:text-white/50 transition-colors shrink-0">{showConfirm ? <EyeOff size={15} /> : <Eye size={15} />}</button>} />
                </motion.div>
              </div>

              {/* Password rules */}
              {password && (
                <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex flex-wrap gap-x-3 gap-y-1 px-1">
                  {PWD_RULES.map((rule, i) => (
                    <span key={i} className={`text-[10px] font-medium flex items-center gap-1 ${rule.test(password) ? 'text-green-400' : 'text-white/20'}`}>
                      {rule.test(password) ? <CheckCircle size={10} /> : <XCircle size={10} />}
                      {rule.label}
                    </span>
                  ))}
                </motion.div>
              )}

              <motion.div initial={{ opacity: 0, x: -15 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: 0.55 }}>
                <FieldInput label="Téléphone" icon={Phone} value={phone} onChange={e => setPhone(e.target.value)} placeholder="+241 00 00 00 00" required />
              </motion.div>

              {/* Country */}
              <motion.div initial={{ opacity: 0, x: -15 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: 0.6 }} className="space-y-1.5">
                <label className="text-[10px] font-bold uppercase tracking-widest text-white/30 pl-1 flex items-center gap-1.5">
                  <Globe size={10} /> Pays
                </label>
                <div className="relative group">
                  <div className="absolute inset-0 rounded-xl bg-blue-500/5 opacity-0 group-focus-within:opacity-100 transition-opacity duration-500 blur-sm" />
                  <select
                    value={country} onChange={e => setCountry(e.target.value)}
                    className="relative z-10 w-full bg-white/[0.04] border border-white/[0.08] rounded-xl px-4 py-3.5 text-sm text-white/80 focus:border-blue-500/30 outline-none transition-colors appearance-none"
                    style={{ backgroundImage: `url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 20 20'%3e%3cpath stroke='%23666' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M6 8l4 4 4-4'/%3e%3c/svg%3e")`, backgroundPosition: 'right 0.5rem center', backgroundRepeat: 'no-repeat', backgroundSize: '1.5em 1.5em', paddingRight: '2.5rem' }}
                  >
                    {COUNTRIES.map(c => <option key={c} value={c}>{c}</option>)}
                  </select>
                </div>
                {country === 'Autre' && (
                  <FieldInput label="" icon={Globe} value={customCountry} onChange={e => setCustomCountry(e.target.value)} placeholder="Nom du pays" required />
                )}
              </motion.div>

              {/* Submit */}
              <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.65 }}
                whileHover={{ scale: 1.01 }} whileTap={{ scale: 0.99 }}>
                <button type="submit" disabled={loading}
                  className="w-full py-4 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-sm tracking-wide transition-all duration-300 flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed shadow-[0_0_30px_rgba(37,99,235,0.2)] hover:shadow-[0_0_40px_rgba(37,99,235,0.35)]">
                  {loading ? (
                    <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  ) : (
                    <>S'inscrire <ArrowRight size={16} /></>
                  )}
                </button>
              </motion.div>
            </form>

            {/* Footer */}
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.7 }}
              className="text-center pt-4 border-t border-white/[0.05]">
              <p className="text-white/30 text-xs">
                Déjà un compte ?{' '}
                <Link to="/login" className="text-blue-400 hover:text-blue-300 font-semibold transition-colors">Se connecter</Link>
              </p>
            </motion.div>
          </div>
        </div>
        <p className="text-center text-[9px] uppercase tracking-[0.3em] text-white/10 mt-6 font-bold">Sky Player Infrastructure</p>
      </motion.div>
    </div>
  );
};
