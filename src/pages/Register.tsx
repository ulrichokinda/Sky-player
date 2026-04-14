import React, { useState } from 'react';
import { useNavigate, Link, useSearchParams } from 'react-router-dom';
import { Logo } from '../components/Logo';
import { Button, Input, Card, Badge } from '../components/ui';
import { auth, createUserWithEmailAndPassword, signInWithPopup, googleProvider } from '../firebase';
import { api } from '../services/api';
import { Footer } from '../components/Footer';
import { Mail, Lock, User, Phone, Globe, Chrome, ArrowRight, ShieldCheck, ArrowLeft, Eye, EyeOff } from 'lucide-react';
import { motion } from 'motion/react';

const COUNTRIES = [
  'Bénin', 'Burkina Faso', 'Burundi', 'Cameroun', 'Centrafrique', 'Comores', 
  'Congo-Brazzaville', 'RDC', 'Côte d\'Ivoire', 'Djibouti', 'Gabon', 'Guinée', 
  'Guinée Équatoriale', 'Madagascar', 'Mali', 'Maroc', 'Maurice', 'Mauritanie', 
  'Niger', 'Rwanda', 'Sénégal', 'Seychelles', 'Tchad', 'Togo', 'Tunisie'
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
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (password !== confirmPassword) {
      alert('Les mots de passe ne correspondent pas.');
      return;
    }

    setLoading(true);
    try {
      // 1. Create user in Firebase Auth
      const result = await createUserWithEmailAndPassword(auth, email, password);
      const user = result.user;

      // 2. Create user profile in Firestore
      await api.registerUser({
        uid: user.uid,
        email,
        username,
        firstName,
        lastName,
        phone,
        country,
        role: 'client'
      });

      alert('Inscription réussie !');
      
      const redirect = searchParams.get('redirect');
      const plan = searchParams.get('plan');
      
      if (redirect) {
        navigate(`${redirect}${plan ? `?plan=${plan}` : ''}`);
      } else {
        navigate('/dashboard');
      }
    } catch (error: any) {
      alert('Erreur : ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = async () => {
    setLoading(true);
    try {
      const result = await signInWithPopup(auth, googleProvider);
      const user = result.user;
      
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
      alert("Erreur Google Login: " + error.message);
    } finally {
      setLoading(false);
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
        className="w-full max-w-lg"
      >
        <Card className="p-8 md:p-12 space-y-8 border-zinc-800/50 bg-zinc-900/30 backdrop-blur-2xl shadow-2xl shadow-primary/5">
          <header className="text-center space-y-4">
            <Logo size={60} className="mx-auto" />
            <div className="space-y-2">
              <Badge variant="primary">Rejoindre le Réseau</Badge>
              <h1 className="text-4xl font-black tracking-tighter">Créer un compte</h1>
              <p className="text-zinc-500 text-sm font-medium">Devenez revendeur officiel et commencez à gagner.</p>
            </div>
          </header>

          <form onSubmit={handleRegister} className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <Input label="Prénom" value={firstName} onChange={(e: any) => setFirstName(e.target.value)} icon={User} required />
              <Input label="Nom" value={lastName} onChange={(e: any) => setLastName(e.target.value)} icon={User} required />
              <Input label="Nom d'utilisateur" value={username} onChange={(e: any) => setUsername(e.target.value)} icon={User} required />
              <Input label="Email" type="email" value={email} onChange={(e: any) => setEmail(e.target.value)} icon={Mail} required />
              <Input 
                label="Mot de passe" 
                type={showPassword ? "text" : "password"} 
                value={password} 
                onChange={(e: any) => setPassword(e.target.value)} 
                icon={Lock} 
                required 
                rightElement={
                  <button 
                    type="button" 
                    onClick={() => setShowPassword(!showPassword)}
                    className="p-2 text-zinc-500 hover:text-white transition-colors"
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                }
              />
              <Input 
                label="Confirmer le mot de passe" 
                type={showConfirmPassword ? "text" : "password"} 
                value={confirmPassword} 
                onChange={(e: any) => setConfirmPassword(e.target.value)} 
                icon={Lock} 
                required 
                error={confirmPassword && password !== confirmPassword ? "Les mots de passe ne correspondent pas" : ""}
                rightElement={
                  <button 
                    type="button" 
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    className="p-2 text-zinc-500 hover:text-white transition-colors"
                  >
                    {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                }
              />
              <Input label="Téléphone" value={phone} onChange={(e: any) => setPhone(e.target.value)} icon={Phone} required />
              
              <div className="space-y-2 md:col-span-2">
                <label className="text-[10px] font-black text-zinc-500 uppercase tracking-widest ml-2 flex items-center gap-2">
                  <Globe size={10} />
                  Pays de Résidence
                </label>
                <select 
                  value={country} 
                  onChange={(e) => setCountry(e.target.value)}
                  className="w-full bg-zinc-950 border border-zinc-800 rounded-xl p-4 text-sm focus:border-primary outline-none transition-all hover:border-zinc-700"
                >
                  {COUNTRIES.map(c => <option key={c} value={c}>{c}</option>)}
                </select>
              </div>
            </div>

            <Button type="submit" fullWidth size="lg" loading={loading} className="py-6 text-base group">
              S'inscrire Maintenant
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
            S'inscrire avec Google
          </Button>

          <footer className="text-center space-y-4">
            <p className="text-zinc-500 text-xs font-medium">
              Déjà membre ?{' '}
              <Link to="/login" className="text-primary font-black hover:underline">
                Se connecter
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
