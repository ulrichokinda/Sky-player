import React, { useState } from 'react';
import { useNavigate, Link, useSearchParams } from 'react-router-dom';
import { Button, Input, Card, Badge } from '../components/ui';
import { auth, createUserWithEmailAndPassword, sendEmailVerification, signOut } from '../firebase';
import { api } from '../services/api';
import { Mail, Lock, User, Phone, Globe, Chrome, ArrowRight, ShieldCheck, ArrowLeft, Eye, EyeOff, UserPlus } from 'lucide-react';
import { motion } from 'motion/react';

const COUNTRIES = [
  'Bénin', 'Burkina Faso', 'Cameroun', 'Centrafrique', 'Congo (Brazzaville)', 
  'Côte d\'Ivoire', 'Gabon', 'Guinée', 'Mali', 'Niger', 'RDC (Kinshasa)', 
  'Sénégal', 'TCHAD', 'Togo', 'Autre'
];

export const Register = () => {
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [phone, setPhone] = useState('');
  const [country, setCountry] = useState(COUNTRIES[0]);
  const [customCountry, setCustomCountry] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (password !== confirmPassword) {
      alert('Les mots de passe ne correspondent pas.');
      return;
    }

    // New password requirements
    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
    if (!passwordRegex.test(password)) {
      alert('Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule, un chiffre et un caractère spécial.');
      return;
    }

    if (country === 'Autre' && !customCountry) {
      alert('Veuillez préciser votre pays.');
      return;
    }

    setLoading(true);
    try {
      // 1. Create user in Firebase Auth
      const result = await createUserWithEmailAndPassword(auth, email, password);
      const user = result.user;

      // 2. Send Email Verification
      await sendEmailVerification(user);

      // 3. Create user profile in Firestore
      await api.registerUser({
        uid: user.uid,
        email,
        username,
        firstName,
        lastName,
        phone,
        country: country === 'Autre' ? customCountry : country,
        role: 'client'
      });

      // 4. Force sign out until email is verified
      await signOut(auth);

      alert('Inscription réussie ! Un e-mail de vérification a été envoyé à ' + email + '. Veuillez confirmer votre e-mail avant de vous connecter.');
      navigate('/login');
    } catch (error: any) {
      alert('Erreur : ' + error.message);
    } finally {
      setLoading(false);
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
        className="w-full max-w-[600px] my-12 relative z-10"
        style={{ transformStyle: 'preserve-3d' }}
      >
        <Card 
          className="p-6 md:p-10 space-y-8 border-none shadow-[0_20px_50px_rgba(0,0,0,0.5)] bg-zinc-900/40 backdrop-blur-3xl relative overflow-hidden ring-1 ring-zinc-800/50 hover:ring-primary/30 transition-all duration-700"
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
                <UserPlus size={36} className="relative z-10" />
                <div className="absolute inset-0 bg-primary/30 blur-xl rounded-full scale-150 opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
              </div>
            </motion.div>
            
            <div className="space-y-3">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-zinc-950/50 border border-primary/20 text-[10px] font-black uppercase tracking-[0.2em] text-primary shadow-[0_0_10px_rgba(244,197,10,0.1)]">
                <ShieldCheck size={12} className="text-primary" />
                SKY PLAYER PRO
              </div>
              <h1 className="text-3xl md:text-4xl font-black tracking-tighter uppercase italic text-transparent bg-clip-text bg-gradient-to-r from-white via-zinc-200 to-zinc-400 drop-shadow-sm">
                Rejoindre le Réseau
              </h1>
              <p className="text-zinc-500 text-xs sm:text-sm font-medium w-full mx-auto leading-relaxed">
                Devenez revendeur officiel et commencez à gagner.
              </p>
            </div>
          </header>

          <form onSubmit={handleRegister} className="space-y-6 relative z-10">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-1.5 focus-within:z-20 relative group">
                <div className="absolute -inset-0.5 bg-gradient-to-r from-primary/0 via-primary/20 to-primary/0 rounded-xl opacity-0 group-focus-within:opacity-100 transition duration-500 blur-sm"></div>
                <Input label="PRÉNOM" value={firstName} onChange={(e: any) => setFirstName(e.target.value)} icon={User} required className="bg-zinc-950/80 border-zinc-800/80 focus:border-primary/50 h-14 relative z-10 shadow-inner" />
              </div>
              <div className="space-y-1.5 focus-within:z-20 relative group">
                <div className="absolute -inset-0.5 bg-gradient-to-r from-primary/0 via-primary/20 to-primary/0 rounded-xl opacity-0 group-focus-within:opacity-100 transition duration-500 blur-sm"></div>
                <Input label="NOM" value={lastName} onChange={(e: any) => setLastName(e.target.value)} icon={User} required className="bg-zinc-950/80 border-zinc-800/80 focus:border-primary/50 h-14 relative z-10 shadow-inner" />
              </div>
              <div className="space-y-1.5 focus-within:z-20 relative md:col-span-2 group">
                <div className="absolute -inset-0.5 bg-gradient-to-r from-primary/0 via-primary/20 to-primary/0 rounded-xl opacity-0 group-focus-within:opacity-100 transition duration-500 blur-sm"></div>
                <Input label="NOM D'UTILISATEUR" value={username} onChange={(e: any) => setUsername(e.target.value)} icon={User} required className="bg-zinc-950/80 border-zinc-800/80 focus:border-primary/50 h-14 relative z-10 shadow-inner" />
              </div>
              <div className="space-y-1.5 focus-within:z-20 relative md:col-span-2 group">
                <div className="absolute -inset-0.5 bg-gradient-to-r from-primary/0 via-primary/20 to-primary/0 rounded-xl opacity-0 group-focus-within:opacity-100 transition duration-500 blur-sm"></div>
                <Input label="IDENTIFIANT EMAIL" type="email" value={email} onChange={(e: any) => setEmail(e.target.value)} icon={Mail} required className="bg-zinc-950/80 border-zinc-800/80 focus:border-primary/50 h-14 relative z-10 shadow-inner" />
              </div>
              <div className="space-y-1.5 focus-within:z-20 relative group">
                <div className="absolute -inset-0.5 bg-gradient-to-r from-primary/0 via-primary/20 to-primary/0 rounded-xl opacity-0 group-focus-within:opacity-100 transition duration-500 blur-sm"></div>
                <Input 
                  label="MOT DE PASSE" 
                  type={showPassword ? "text" : "password"} 
                  value={password} 
                  onChange={(e: any) => setPassword(e.target.value)} 
                  icon={Lock} 
                  required 
                  className="bg-zinc-950/80 border-zinc-800/80 focus:border-primary/50 h-14 relative z-10 shadow-inner"
                  rightElement={
                    <button 
                      type="button" 
                      onClick={() => setShowPassword(!showPassword)}
                      className="p-2 pr-4 text-zinc-500 hover:text-white transition-colors"
                    >
                      {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                  }
                />
              </div>
              <div className="space-y-1.5 focus-within:z-20 relative group">
                <div className="absolute -inset-0.5 bg-gradient-to-r from-primary/0 via-primary/20 to-primary/0 rounded-xl opacity-0 group-focus-within:opacity-100 transition duration-500 blur-sm"></div>
                <Input 
                  label="CONFIRMER MOT DE PASSE" 
                  type={showConfirmPassword ? "text" : "password"} 
                  value={confirmPassword} 
                  onChange={(e: any) => setConfirmPassword(e.target.value)} 
                  icon={Lock} 
                  required 
                  className="bg-zinc-950/80 border-zinc-800/80 focus:border-primary/50 h-14 relative z-10 shadow-inner"
                  error={confirmPassword && password !== confirmPassword ? "Ne correspond pas" : ""}
                  rightElement={
                    <button 
                      type="button" 
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                      className="p-2 pr-4 text-zinc-500 hover:text-white transition-colors"
                    >
                      {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                  }
                />
              </div>
              <div className="space-y-1.5 focus-within:z-20 relative md:col-span-2 group">
                <div className="absolute -inset-0.5 bg-gradient-to-r from-primary/0 via-primary/20 to-primary/0 rounded-xl opacity-0 group-focus-within:opacity-100 transition duration-500 blur-sm"></div>
                <Input 
                  label="TÉLÉPHONE (AVEC INDICATIF)" 
                  placeholder="+241 00 00 00 00"
                  value={phone} 
                  onChange={(e: any) => setPhone(e.target.value)} 
                  icon={Phone} 
                  required 
                  className="bg-zinc-950/80 border-zinc-800/80 focus:border-primary/50 h-14 relative z-10 shadow-inner"
                />
              </div>
              
              <div className="space-y-1.5 md:col-span-2 focus-within:z-20 relative">
                <label className="text-[10px] font-black text-zinc-500 uppercase tracking-widest ml-2 flex items-center gap-2">
                  <Globe size={10} />
                  PAYS DE RÉSIDENCE
                </label>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="relative group">
                    <div className="absolute -inset-0.5 bg-gradient-to-r from-primary/0 via-primary/20 to-primary/0 rounded-xl opacity-0 group-focus-within:opacity-100 transition duration-500 blur-sm"></div>
                    <select 
                      value={country} 
                      onChange={(e) => setCountry(e.target.value)}
                      className="relative z-10 w-full bg-zinc-950/80 border border-zinc-800/80 rounded-xl p-4 text-sm focus:border-primary/50 outline-none transition-all hover:border-zinc-700 text-white h-14 appearance-none shadow-inner"
                      style={{ backgroundImage: 'url("data:image/svg+xml,%3csvg xmlns=\'http://www.w3.org/2000/svg\' fill=\'none\' viewBox=\'0 0 20 20\'%3e%3cpath stroke=\'%23a1a1aa\' stroke-linecap=\'round\' stroke-linejoin=\'round\' stroke-width=\'1.5\' d=\'M6 8l4 4 4-4\'/%3e%3c/svg%3e")', backgroundPosition: 'right 0.5rem center', backgroundRepeat: 'no-repeat', backgroundSize: '1.5em 1.5em', paddingRight: '2.5rem' }}
                    >
                      {COUNTRIES.map(c => <option key={c} value={c}>{c}</option>)}
                    </select>
                  </div>
                  {country === 'Autre' && (
                    <div className="relative group">
                      <div className="absolute -inset-0.5 bg-gradient-to-r from-primary/0 via-primary/20 to-primary/0 rounded-xl opacity-0 group-focus-within:opacity-100 transition duration-500 blur-sm"></div>
                      <Input 
                        placeholder="Précisez votre pays" 
                        value={customCountry} 
                        onChange={(e: any) => setCustomCountry(e.target.value)}
                        required
                        className="relative z-10 bg-zinc-950/80 border-zinc-800/80 focus:border-primary/50 h-14 shadow-inner"
                      />
                    </div>
                  )}
                </div>
              </div>
            </div>

            <motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }} className="pt-2">
              <Button 
                type="submit" 
                fullWidth 
                size="lg" 
                loading={loading}
                className="h-14 sm:h-16 text-xs sm:text-sm font-black uppercase tracking-widest group bg-gradient-to-r from-primary via-[#ffb900] to-primary hover:from-[#ffb900] hover:to-primary text-black border-none shadow-[0_0_20px_rgba(244,197,10,0.3)] transition-all overflow-hidden relative"
              >
                <span className="relative z-10 flex items-center justify-center">
                  S'inscrire Maintenant
                  <ArrowRight size={18} className="shrink-0 ml-2 group-hover:translate-x-2 transition-transform" />
                </span>
              </Button>
            </motion.div>
          </form>

          <footer className="text-center pt-6 pb-2 relative z-10 border-t border-zinc-800/50 mt-6">
            <p className="text-zinc-500 text-xs font-medium">
              <span>Déjà membre ?</span>{' '}
              <Link to="/login" className="text-primary font-black hover:text-white transition-colors underline-offset-4 decoration-2 hover:underline">
                <span>Se connecter</span>
              </Link>
            </p>
          </footer>
        </Card>
        
        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5, duration: 0.5 }}
          className="mt-8 sm:mt-10 flex items-center justify-center gap-2 text-zinc-600 drop-shadow-md pb-12"
        >
          <p className="text-[9px] sm:text-[10px] font-black uppercase tracking-[0.2em] text-center w-full break-words px-4">
            SKY PLAYER INFRASTRUCTURE &bull; SECURE NODES
          </p>
        </motion.div>
      </motion.div>
    </div>
  );
};
