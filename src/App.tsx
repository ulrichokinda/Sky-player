import React, { useState, useEffect } from 'react';
import { Search, Tv, Film, PlayCircle, Star, History, Settings, LogOut, Menu, X, User, ChevronRight, LayoutGrid, ListVideo, Trophy, Users, CreditCard, ArrowLeft, Copy, PlusCircle, Globe, Zap as ZapIcon, Smartphone, Store, Download, HelpCircle, ShieldCheck, List } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { Logo } from './components/Logo';
import { VideoPlayer } from './components/VideoPlayer';
import { ResellerPanel } from './components/ResellerPanel';
import { cn, Card, Badge, Button, Input, Toast } from './components/ui';
import { auth, googleProvider } from './firebase';
import { signInWithPopup, onAuthStateChanged, signOut, createUserWithEmailAndPassword, signInWithEmailAndPassword } from 'firebase/auth';
import { api, UserProfile } from './services/api';
import { PAYMENT_METHODS } from './constants';
import { Focusable } from './components/TVFocusManager';

interface Channel {
  id: string;
  name: string;
  url: string;
  category: string;
  logo?: string;
}


const AFRICAN_FRANCOPHONE_COUNTRIES = [
  { name: 'Bénin', code: '+229' },
  { name: 'Burkina Faso', code: '+226' },
  { name: 'Burundi', code: '+257' },
  { name: 'Cameroun', code: '+237' },
  { name: 'Centrafrique', code: '+236' },
  { name: 'Comores', code: '+269' },
  { name: 'Congo-Brazzaville', code: '+242' },
  { name: 'RDC', code: '+243' },
  { name: 'Côte d\'Ivoire', code: '+225' },
  { name: 'Djibouti', code: '+253' },
  { name: 'Gabon', code: '+241' },
  { name: 'Guinée', code: '+224' },
  { name: 'Guinée Équatoriale', code: '+240' },
  { name: 'Madagascar', code: '+261' },
  { name: 'Mali', code: '+223' },
  { name: 'Maroc', code: '+212' },
  { name: 'Maurice', code: '+230' },
  { name: 'Mauritanie', code: '+222' },
  { name: 'Niger', code: '+227' },
  { name: 'Rwanda', code: '+250' },
  { name: 'Sénégal', code: '+221' },
  { name: 'Seychelles', code: '+248' },
  { name: 'Tchad', code: '+235' },
  { name: 'Togo', code: '+228' },
  { name: 'Tunisie', code: '+216' }
].sort((a, b) => a.name.localeCompare(b.name));

export default function App() {
  const [installDate] = useState(() => {
    const saved = localStorage.getItem('install_date');
    if (saved) return parseInt(saved);
    const now = Date.now();
    localStorage.setItem('install_date', now.toString());
    return now;
  });

  const [timeLeft, setTimeLeft] = useState('');

  useEffect(() => {
    const calculateTime = () => {
      const thirtyDaysMs = 30 * 24 * 60 * 60 * 1000;
      const expiry = installDate + thirtyDaysMs;
      const now = Date.now();
      const diff = expiry - now;

      if (diff <= 0) {
        setTimeLeft('Expiré');
        return;
      }

      const days = Math.floor(diff / (1000 * 60 * 60 * 24));
      const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((diff % (1000 * 60)) / 1000);

      setTimeLeft(`${days}j ${hours}h ${minutes}m ${seconds}s`);
    };

    calculateTime();
    const interval = setInterval(calculateTime, 1000);
    return () => clearInterval(interval);
  }, [installDate]);

  const [channels, setChannels] = useState<Channel[]>([]);
  const [activeTab, setActiveTab] = useState<string>(() => {
    const userData = JSON.parse(localStorage.getItem('user') || '{}');
    return (userData.role === 'reseller' || userData.role === 'admin') ? 'overview' : 'home';
  });
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [isGuest, setIsGuest] = useState(false);
  const [showResellerAuth, setShowResellerAuth] = useState(false);
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login');
  const [username, setUsername] = useState(() => {
    const userData = JSON.parse(localStorage.getItem('user') || '{}');
    return userData.username || '';
  });
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('+242 ');
  const [country, setCountry] = useState('Congo-Brazzaville');
  const [searchQuery, setSearchQuery] = useState('');
  const [notification, setNotification] = useState<{ type: 'success' | 'error' | 'info'; message: string } | null>(null);

  const notify = (msg: string, type: 'success' | 'error' | 'info' = 'info') => {
    setNotification({ message: msg, type });
  };
  const [selectedCategory, setSelectedCategory] = useState('Tous');
  const [isReseller, setIsReseller] = useState(() => {
    const userData = JSON.parse(localStorage.getItem('user') || '{}');
    return userData.role === 'reseller' || userData.role === 'admin';
  });
  const [showAddServerModal, setShowAddServerModal] = useState(false);
  const [newPlaylistName, setNewPlaylistName] = useState('');
  const [newPlaylistUrl, setNewPlaylistUrl] = useState('');
  const [customPlaylists, setCustomPlaylists] = useState<{name: string, url: string}[]>(() => {
    return JSON.parse(localStorage.getItem('custom_playlists') || '[]');
  });
  const [customChannels, setCustomChannels] = useState<Channel[]>([]);
  const [showCountryPicker, setShowCountryPicker] = useState(false);
  const [pickerType, setPickerType] = useState<'country' | 'phone'>('country');
  const [pickerSearch, setPickerSearch] = useState('');

  const navItems = [
    { id: 'home', label: 'Accueil', icon: Tv },
    { id: 'settings', label: 'Paramètres', icon: Settings },
  ];

  useEffect(() => {
    if (customPlaylists.length > 0) {
      const dummyChannels = customPlaylists.flatMap((p, idx) => [
        { id: `custom-${idx}-1`, name: `${p.name} - Canal 1`, category: p.name, url: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8' },
        { id: `custom-${idx}-2`, name: `${p.name} - Canal 2`, category: p.name, url: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8' }
      ]);
      setCustomChannels(dummyChannels);
    }
  }, [customPlaylists]);

  const [isAuthReady, setIsAuthReady] = useState(false);
  const [currentUser, setCurrentUser] = useState<any>(null);
  const [loading, setLoading] = useState(false);

  const [macAddress] = useState(() => {
    const saved = localStorage.getItem('ewo_mac');
    if (saved) return saved;
    const newMac = Array.from({ length: 6 }, () => Math.floor(Math.random() * 256).toString(16).padStart(2, '0').toUpperCase()).join(':');
    localStorage.setItem('ewo_mac', newMac);
    return newMac;
  });

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      if (user) {
        try {
          const userData = await api.getUser(user.uid);
          setCurrentUser({ ...user, ...userData });
          setIsLoggedIn(true);
          setIsReseller(userData.role === 'reseller' || userData.role === 'admin');
          setUsername(userData.username || user.displayName || '');
          if (userData.role === 'reseller' || userData.role === 'admin') {
            setActiveTab('overview');
          }
        } catch (error) {
          console.error("User not found in local DB, registering now:", error);
          const newUser: Partial<UserProfile> = {
            uid: user.uid,
            email: user.email!,
            username: user.displayName || 'Utilisateur',
            role: 'client',
            credits: 0
          };
          try {
            await api.registerUser(newUser);
            const userData = await api.getUser(user.uid);
            setCurrentUser({ ...user, ...userData });
            setIsLoggedIn(true);
            setIsReseller(userData.role === 'reseller' || userData.role === 'admin');
            setUsername(userData.username || user.displayName || '');
          } catch (regError) {
            console.error("Failed to auto-register user:", regError);
            setIsLoggedIn(true);
            setCurrentUser(user);
          }
        }
      } else {
        setIsLoggedIn(false);
        setCurrentUser(null);
        setIsReseller(false);
      }
      setIsAuthReady(true);
    });

    return () => unsubscribe();
  }, []);

  const handleLogout = async () => {
    await signOut(auth);
    setIsLoggedIn(false);
    setIsGuest(false);
    setIsReseller(false);
    setShowResellerAuth(false);
    setActiveTab('home');
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      if (authMode === 'login') {
        await signInWithEmailAndPassword(auth, email, password);
      } else {
        const userCredential = await createUserWithEmailAndPassword(auth, email, password);
        const user = userCredential.user;
        const userData: Partial<UserProfile> = {
          uid: user.uid,
          email: user.email!,
          username: username,
          phone: phone,
          country: country,
          role: 'reseller',
          credits: 0
        };
        try {
          await api.registerUser(userData);
          const fullUser = await api.getUser(user.uid);
          setCurrentUser({ ...user, ...fullUser });
          setIsReseller(true);
          notify('Inscription réussie !', 'success');
        } catch (err: any) {
          notify("Erreur lors de la création du profil: " + err.message, 'error');
        }
      }
    } catch (error: any) {
      notify("Erreur: " + error.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = async () => {
    try {
      const result = await signInWithPopup(auth, googleProvider);
      const user = result.user;
      try {
        const userData = await api.getUser(user.uid);
        setCurrentUser({ ...user, ...userData });
      } catch (err) {
        // User doesn't exist in local DB, create them
        const newUser: Partial<UserProfile> = {
          uid: user.uid,
          email: user.email!,
          username: user.displayName || 'Utilisateur',
          role: 'client',
          credits: 0
        };
        await api.registerUser(newUser);
        const fullUser = await api.getUser(user.uid);
        setCurrentUser({ ...user, ...fullUser });
      }
    } catch (error: any) {
      alert("Erreur Google Login: " + error.message);
    }
  };

  const handleAddPlaylist = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newPlaylistName || !newPlaylistUrl) return;
    const newList = [...customPlaylists, { name: newPlaylistName, url: newPlaylistUrl }];
    setCustomPlaylists(newList);
    localStorage.setItem('custom_playlists', JSON.stringify(newList));
    setNewPlaylistName('');
    setNewPlaylistUrl('');
    setShowAddServerModal(false);
    setIsGuest(true);
    notify('Playlist ajoutée avec succès !', 'success');
  };

  if (!isLoggedIn && !isGuest) {
    return (
      <div className="min-h-screen bg-black flex flex-col items-center justify-center p-4 sm:p-8 font-sans relative overflow-x-hidden overflow-y-auto">
        <div className="relative z-10 flex flex-col items-center justify-center gap-8 lg:gap-12 w-full max-w-4xl py-12">
          {/* Header Section */}
          <div className="flex flex-col items-center gap-6 text-center">
            <Logo size={100} showText={false} />
            <div className="space-y-2">
              <h1 className="text-5xl lg:text-7xl font-black text-white italic tracking-tighter leading-none">
                WO <span className="text-primary">PLAYER</span>
              </h1>
              <p className="text-zinc-500 uppercase tracking-[0.4em] text-[10px] sm:text-xs font-black">Premium Streaming Experience</p>
            </div>
          </div>

          {/* Main Content Card */}
          <div className="w-full max-w-md mx-auto">
            {!showResellerAuth ? (
              <Card className="p-8 text-center space-y-6">
                <div className="space-y-2">
                  <p className="text-zinc-500 text-[10px] font-black uppercase tracking-widest">Identifiant Matériel (MAC)</p>
                  <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
                    <div className="text-xl lg:text-2xl font-mono font-black text-white tracking-[0.2em] bg-black/60 py-4 px-8 rounded-2xl border border-white/5 inline-block shadow-inner">
                      {macAddress}
                    </div>
                    <Button 
                      variant="outline"
                      onClick={() => {
                        navigator.clipboard.writeText(macAddress);
                        notify('MAC copiée !', 'success');
                      }}
                      icon={Copy}
                      className="p-4"
                    />
                  </div>
                </div>
                
                <p className="text-zinc-400 text-xs sm:text-sm font-medium max-w-xs mx-auto leading-relaxed">
                  Veuillez transmettre cet identifiant à votre distributeur agréé pour l'activation de vos services.
                </p>

                {customPlaylists.length > 0 && (
                  <div className="pt-8 border-t border-white/5 space-y-4">
                    <p className="text-[10px] font-black text-zinc-600 uppercase tracking-widest">Vos Serveurs Connectés</p>
                    <div className="flex flex-wrap justify-center gap-3">
                      {customPlaylists.map((p, i) => (
                        <Button 
                          key={i}
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            setSelectedCategory(p.name);
                            setIsGuest(true);
                          }}
                          icon={Globe}
                        >
                          {p.name}
                        </Button>
                      ))}
                    </div>
                  </div>
                )}

                <div className="pt-6">
                  <Button 
                    variant="ghost" 
                    size="sm" 
                    onClick={() => setShowResellerAuth(true)}
                    className="text-zinc-500 hover:text-primary"
                  >
                    Espace Partenaire
                  </Button>
                </div>
              </Card>
            ) : (
              <Card className="p-8">
                <div className="flex justify-between items-center mb-8">
                  <h2 className="text-xl font-black text-white tracking-tight">Espace Partenaire</h2>
                  <Button 
                    variant="outline" 
                    size="sm" 
                    onClick={() => setShowResellerAuth(false)}
                    icon={ArrowLeft}
                  >
                    Retour
                  </Button>
                </div>
                
                <div className="flex bg-zinc-950 p-1.5 rounded-2xl mb-8">
                  <button 
                    onClick={() => setAuthMode('login')}
                    className={cn(
                      "flex-1 py-2.5 rounded-xl text-xs font-black uppercase tracking-widest transition-all",
                      authMode === 'login' ? "bg-primary text-white shadow-lg shadow-primary/20" : "text-zinc-500 hover:text-zinc-400"
                    )}
                  >
                    Connexion
                  </button>
                  <button 
                    onClick={() => setAuthMode('register')}
                    className={cn(
                      "flex-1 py-2.5 rounded-xl text-xs font-black uppercase tracking-widest transition-all",
                      authMode === 'register' ? "bg-primary text-white shadow-lg shadow-primary/20" : "text-zinc-500 hover:text-zinc-400"
                    )}
                  >
                    S'inscrire
                  </button>
                </div>

                <form onSubmit={handleLogin} className="space-y-4">
                  {authMode === 'login' ? (
                    <Input 
                      label="Email"
                      type="email"
                      value={email}
                      onChange={(e: any) => setEmail(e.target.value)}
                      placeholder="email@exemple.com"
                      required
                    />
                  ) : (
                    <>
                      <Input 
                        label="Nom d'utilisateur"
                        value={username}
                        onChange={(e: any) => setUsername(e.target.value)}
                        placeholder="Utilisateur"
                        required
                      />
                      <Input 
                        label="Email"
                        type="email"
                        value={email}
                        onChange={(e: any) => setEmail(e.target.value)}
                        placeholder="email@exemple.com"
                        required
                      />
                    </>
                  )}
                  
                  {authMode === 'register' && (
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <label className="text-[10px] font-black text-zinc-500 uppercase tracking-widest ml-1">Téléphone</label>
                        <div className="flex bg-zinc-950 border border-zinc-800 rounded-xl focus-within:border-primary transition-all overflow-hidden">
                          <button
                            type="button"
                            onClick={() => {
                              setPickerType('phone');
                              setShowCountryPicker(true);
                            }}
                            className="bg-zinc-900 px-3 py-3 text-xs text-primary font-black border-r border-zinc-800 hover:bg-zinc-800 transition-colors"
                          >
                            {phone.split(' ')[0] || '+242'}
                          </button>
                          <input 
                            type="tel"
                            value={phone.includes(' ') ? phone.split(' ').slice(1).join(' ') : ''}
                            onChange={(e) => {
                              const prefix = phone.split(' ')[0] || '+242';
                              setPhone(`${prefix} ${e.target.value}`);
                            }}
                            className="w-full bg-transparent px-4 py-3 text-sm text-white focus:outline-none"
                            placeholder="000 000"
                            required
                          />
                        </div>
                      </div>
                      <div className="space-y-2">
                        <label className="text-[10px] font-black text-zinc-500 uppercase tracking-widest ml-1">Pays</label>
                        <button
                          type="button"
                          onClick={() => {
                            setPickerType('country');
                            setShowCountryPicker(true);
                          }}
                          className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-sm text-white text-left focus:outline-none focus:border-primary transition-all flex items-center justify-between h-[46px]"
                        >
                          <span className={country ? 'text-white font-bold' : 'text-zinc-500'}>
                            {country || 'Sélectionner'}
                          </span>
                          <ChevronRight size={16} className="text-zinc-600" />
                        </button>
                      </div>
                    </div>
                  )}
                  
                  {authMode === 'register' && country && PAYMENT_METHODS[country as keyof typeof PAYMENT_METHODS] && (
                    <div className="mt-4 p-4 bg-zinc-900 rounded-xl border border-zinc-800">
                      <p className="text-[10px] font-black text-zinc-500 uppercase tracking-widest mb-2">Modes de paiement disponibles pour {country}:</p>
                      <div className="flex flex-wrap gap-2">
                        {PAYMENT_METHODS[country as keyof typeof PAYMENT_METHODS].map(method => (
                          <Badge key={method.id} variant="secondary">{method.name}</Badge>
                        ))}
                      </div>
                    </div>
                  )}

                  <Input 
                    label="Mot de passe"
                    type="password"
                    value={password}
                    onChange={(e: any) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    required
                  />

                  <Button 
                    type="submit"
                    className="w-full mt-4"
                    size="lg"
                    loading={loading}
                  >
                    {authMode === 'login' ? 'Se connecter' : 'Créer mon compte'}
                  </Button>

                  <div className="relative py-4">
                    <div className="absolute inset-0 flex items-center">
                      <div className="w-full border-t border-zinc-800"></div>
                    </div>
                    <div className="relative flex justify-center text-[10px] uppercase tracking-widest font-black">
                      <span className="bg-zinc-950 px-4 text-zinc-600">Ou continuer avec</span>
                    </div>
                  </div>

                  <Button 
                    type="button"
                    variant="outline"
                    className="w-full"
                    onClick={handleGoogleLogin}
                    icon={() => (
                      <svg className="w-4 h-4 mr-2" viewBox="0 0 24 24">
                        <path fill="currentColor" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                        <path fill="currentColor" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                        <path fill="currentColor" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                        <path fill="currentColor" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
                      </svg>
                    )}
                  >
                    Google
                  </Button>
                </form>
              </Card>
            )}
          </div>

          {/* Country Picker Modal */}
          <AnimatePresence>
            {showCountryPicker && (
              <motion.div 
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/80 backdrop-blur-md"
              >
                <motion.div 
                  initial={{ scale: 0.9, opacity: 0, y: 20 }}
                  animate={{ scale: 1, opacity: 1, y: 0 }}
                  exit={{ scale: 0.9, opacity: 0, y: 20 }}
                  className="bg-zinc-900 border border-zinc-800 w-full max-w-md rounded-[2.5rem] overflow-hidden flex flex-col max-h-[80vh] shadow-2xl"
                >
                  <div className="p-6 border-b border-zinc-800 flex items-center justify-between bg-zinc-900/50 backdrop-blur-xl sticky top-0 z-10">
                    <h3 className="text-lg font-black text-white uppercase tracking-tight">
                      {pickerType === 'phone' ? 'Indicateur Pays' : 'Sélectionner Pays'}
                    </h3>
                    <button 
                      onClick={() => {
                        setShowCountryPicker(false);
                        setPickerSearch('');
                      }}
                      className="p-2 hover:bg-zinc-800 rounded-full text-zinc-500 transition-colors"
                    >
                      <X size={20} />
                    </button>
                  </div>
                  
                  <div className="p-4 bg-zinc-900/30">
                    <div className="relative">
                      <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" size={16} />
                      <input 
                        type="text"
                        placeholder="Rechercher un pays..."
                        value={pickerSearch}
                        onChange={(e) => setPickerSearch(e.target.value)}
                        className="w-full bg-zinc-950 border border-zinc-800 rounded-xl pl-10 pr-4 py-3 text-sm text-white focus:outline-none focus:border-primary transition-all"
                        autoFocus
                      />
                    </div>
                  </div>
                  
                  <div className="flex-1 overflow-y-auto p-2 space-y-1 custom-scrollbar bg-zinc-900/20">
                    {AFRICAN_FRANCOPHONE_COUNTRIES
                      .filter(c => 
                        c.name.toLowerCase().includes(pickerSearch.toLowerCase()) || 
                        c.code.includes(pickerSearch)
                      )
                      .map((c) => (
                        <button
                          key={c.name}
                          onClick={() => {
                            setCountry(c.name);
                            const currentNumber = phone.includes(' ') ? phone.split(' ').slice(1).join(' ') : '';
                            setPhone(`${c.code} ${currentNumber}`);
                            setShowCountryPicker(false);
                            setPickerSearch('');
                          }}
                          className="w-full flex items-center justify-between p-4 rounded-2xl hover:bg-zinc-800 transition-all group"
                        >
                          <div className="flex items-center gap-3">
                            <div className="w-10 h-10 bg-zinc-950 rounded-xl flex items-center justify-center text-[10px] font-black text-primary border border-zinc-800 group-hover:border-primary/50 transition-colors">
                              {c.code}
                            </div>
                            <span className="text-sm font-bold text-white group-hover:text-primary transition-colors">{c.name}</span>
                          </div>
                          {country === c.name && (
                            <div className="w-2 h-2 bg-primary rounded-full shadow-[0_0_10px_rgba(0,174,239,0.5)]" />
                          )}
                        </button>
                      ))}
                  </div>
                </motion.div>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Bottom Action Section */}
          {!showResellerAuth && (
            <div className="w-full max-w-2xl flex flex-col items-center gap-8 pt-4">
              <div className="flex flex-col sm:flex-row justify-center items-center w-full gap-4">
                <button 
                  onClick={() => setShowResellerAuth(true)}
                  className="w-full sm:w-auto flex items-center justify-center gap-2 bg-primary hover:bg-primary-dark px-8 py-3.5 rounded-2xl text-white text-[11px] font-black uppercase tracking-widest transition-all shadow-2xl shadow-primary/30 group active:scale-95"
                >
                  Espace Revendeur
                  <div className="p-1.5 bg-white/20 rounded-lg group-hover:translate-x-1 transition-transform">
                    <ChevronRight size={16} />
                  </div>
                </button>
              </div>
              <div className="flex flex-col items-center gap-1 mt-20">
                <p className="text-[11px] text-zinc-600 tracking-[0.2em]" style={{ fontFamily: "'A770 Roman Swash', 'Playfair Display', serif", fontStyle: 'italic' }}>propulsé par Ewo Tech</p>
                <div className="w-12 h-0.5 bg-primary/10 rounded-full" />
              </div>
            </div>
          )}
        </div>
      </div>
    );
  }

  const allChannels = [...channels, ...customChannels];

  const filteredChannels = allChannels.filter(c => {
    const matchesSearch = c.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
                         c.category.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesCategory = selectedCategory === 'Tous' || c.category === selectedCategory;
    return matchesSearch && matchesCategory;
  });

  const categories = ['Tous', ...new Set(allChannels.map(c => c.category))];

  function NavItem({ icon, label, active, onClick, collapsed, id }: NavItemProps & { id: string }) {
    const handleClick = () => {
      onClick();
      if (window.innerWidth < 1024) {
        setIsSidebarOpen(false);
      }
    };

    return (
      <Focusable id={id}>
        {(isFocused) => (
          <button 
            onClick={handleClick}
            className={cn(
              "w-full flex items-center gap-4 px-4 py-3.5 rounded-2xl transition-all duration-300 group outline-none relative overflow-hidden",
              active 
                ? "bg-primary text-black shadow-xl shadow-primary/20" 
                : "text-zinc-500 hover:bg-zinc-900/50 hover:text-zinc-200",
              isFocused && "bg-zinc-800 text-white ring-2 ring-primary/50"
            )}
          >
            {active && (
              <motion.div 
                layoutId="active-pill"
                className="absolute inset-0 bg-primary"
                transition={{ type: "spring", bounce: 0.2, duration: 0.6 }}
              />
            )}
            
            <div className={cn(
              "relative z-10 transition-transform duration-300 group-hover:scale-110",
              active ? "text-black" : "group-hover:text-primary"
            )}>
              {icon}
            </div>

            {!collapsed && (
              <span className={cn(
                "relative z-10 text-xs font-black uppercase tracking-widest truncate transition-all duration-300",
                active ? "text-black" : "text-zinc-500 group-hover:text-zinc-200 group-hover:translate-x-1"
              )}>
                {label}
              </span>
            )}

            {!collapsed && active && (
              <motion.div 
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                className="relative z-10 ml-auto"
              >
                <ChevronRight size={14} className="text-black/50" />
              </motion.div>
            )}
          </button>
        )}
      </Focusable>
    );
  }

  return (
    <div className="flex h-screen bg-black text-zinc-100 font-sans overflow-hidden relative">
      {!isGuest && (
        <>
          {/* Mobile Sidebar Overlay */}
          {isSidebarOpen && (
            <div 
              className="fixed inset-0 bg-black/60 backdrop-blur-sm z-40 lg:hidden"
              onClick={() => setIsSidebarOpen(false)}
            />
          )}
          
          <aside className={`fixed inset-y-0 left-0 z-50 lg:relative ${isSidebarOpen ? 'translate-x-0 w-72' : '-translate-x-full lg:translate-x-0 lg:w-20'} bg-zinc-950 border-r border-zinc-900 transition-all duration-500 flex flex-col shadow-2xl shadow-black/50`}>
            <div className="p-6 flex items-center justify-between">
              <Logo size={32} showText={isSidebarOpen} />
              <div className="flex items-center gap-2">
                <button 
                  onClick={() => setIsSidebarOpen(!isSidebarOpen)} 
                  className="p-2 text-zinc-500 hover:text-white hover:bg-zinc-900 rounded-xl transition-all"
                >
                  {isSidebarOpen ? <ChevronRight size={18} className="rotate-180" /> : <Menu size={18} />}
                </button>
              </div>
            </div>

            <div className="flex-1 overflow-y-auto custom-scrollbar px-4 space-y-8 py-4">
              {/* Profile Section - Intelligent Info */}
              {isSidebarOpen && (
                <div className="px-2">
                  <Card className="p-4 relative overflow-hidden group border-zinc-800/50">
                    <div className="absolute -right-4 -top-4 w-20 h-20 bg-primary/5 rounded-full blur-2xl group-hover:bg-primary/10 transition-all duration-700" />
                    <div className="flex items-center gap-3 relative z-10">
                      <div className="w-12 h-12 bg-primary/20 rounded-2xl flex items-center justify-center text-primary border border-primary/10 shadow-inner">
                        <User size={24} />
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-black text-white truncate">{username}</p>
                        <div className="flex items-center gap-1.5 mt-0.5">
                          <span className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse" />
                          <p className="text-[10px] text-zinc-500 font-black uppercase tracking-widest">
                            {isReseller ? 'Partenaire Pro' : 'Membre Premium'}
                          </p>
                        </div>
                      </div>
                    </div>
                    
                    {isReseller && (
                      <div className="mt-4 pt-4 border-t border-zinc-800/50 flex items-center justify-between">
                        <div className="space-y-0.5">
                          <p className="text-[9px] font-black text-zinc-600 uppercase tracking-widest">Crédits Disponibles</p>
                          <p className="text-lg font-black text-primary tracking-tight">
                            {currentUser?.credits || 0} <span className="text-[10px] opacity-60">PTS</span>
                          </p>
                        </div>
                        <Button 
                          variant="ghost" 
                          size="sm"
                          onClick={() => setActiveTab('credits')}
                          className="p-2 bg-primary/10 hover:bg-primary/20 text-primary rounded-xl transition-all active:scale-90"
                          icon={PlusCircle}
                        />
                      </div>
                    )}
                  </Card>
                </div>
              )}

              <nav className="space-y-6">
                {/* Main Section */}
                <div className="space-y-1">
                  {isSidebarOpen && <p className="px-4 text-[10px] font-black text-zinc-600 uppercase tracking-[0.2em] mb-2">Navigation</p>}
                  {!isReseller && (
                    <NavItem id="nav-home" icon={<Tv size={20} />} label="Accueil" active={activeTab === 'home'} onClick={() => setActiveTab('home')} collapsed={!isSidebarOpen} />
                  )}
                  {isReseller && (
                    <NavItem id="nav-overview" icon={<LayoutGrid size={20} />} label="Tableau de bord" active={activeTab === 'overview'} onClick={() => setActiveTab('overview')} collapsed={!isSidebarOpen} />
                  )}
                </div>

                {/* Reseller Tools */}
                {isReseller && (
                  <div className="space-y-1">
                    {isSidebarOpen && <p className="px-4 text-[10px] font-black text-zinc-600 uppercase tracking-[0.2em] mb-2">Gestion & Outils</p>}
                    <NavItem id="nav-clients" icon={<Users size={20} />} label="Mes Clients" active={activeTab === 'clients'} onClick={() => setActiveTab('clients')} collapsed={!isSidebarOpen} />
                    <NavItem id="nav-credits" icon={<CreditCard size={20} />} label="Boutique Crédits" active={activeTab === 'credits'} onClick={() => setActiveTab('credits')} collapsed={!isSidebarOpen} />
                    <NavItem id="nav-check-mac" icon={<Search size={20} />} label="Vérifier MAC" active={activeTab === 'check_mac'} onClick={() => setActiveTab('check_mac')} collapsed={!isSidebarOpen} />
                    <NavItem id="nav-api" icon={<ZapIcon size={20} />} label="Intégration API" active={activeTab === 'api_docs'} onClick={() => setActiveTab('api_docs')} collapsed={!isSidebarOpen} />
                  </div>
                )}

                {/* System & Support */}
                <div className="space-y-1">
                  {isSidebarOpen && <p className="px-4 text-[10px] font-black text-zinc-600 uppercase tracking-[0.2em] mb-2">Configuration</p>}
                  <NavItem id="nav-store-info" icon={<Store size={20} />} label="Infos Magasin" active={activeTab === 'store_info'} onClick={() => setActiveTab('store_info')} collapsed={!isSidebarOpen} />
                  <NavItem id="nav-account" icon={<User size={20} />} label="Mon Profil" active={activeTab === 'account'} onClick={() => setActiveTab('account')} collapsed={!isSidebarOpen} />
                  <NavItem id="nav-download" icon={<List size={20} />} label="Conditions" active={activeTab === 'download_apk'} onClick={() => setActiveTab('download_apk')} collapsed={!isSidebarOpen} />
                  <NavItem id="nav-support" icon={<HelpCircle size={20} />} label="Support Client" active={activeTab === 'support'} onClick={() => setActiveTab('support')} collapsed={!isSidebarOpen} />
                </div>

                {/* Playlists for non-resellers */}
                {customPlaylists.length > 0 && !isReseller && (
                  <div className="space-y-1">
                    {isSidebarOpen && <p className="px-4 text-[10px] font-black text-zinc-600 uppercase tracking-[0.2em] mb-2">Mes Serveurs</p>}
                    {customPlaylists.map((p, i) => (
                      <NavItem 
                        key={i}
                        id={`nav-playlist-${i}`}
                        icon={<Globe size={20} className="text-blue-500" />} 
                        label={p.name} 
                        active={activeTab === `playlist-${i}`} 
                        onClick={() => setActiveTab(`playlist-${i}`)} 
                        collapsed={!isSidebarOpen} 
                      />
                    ))}
                  </div>
                )}
              </nav>
            </div>

            <div className="p-4 space-y-2 border-t border-zinc-900 bg-black/20">
              <NavItem id="nav-settings" icon={<Settings size={20} />} label="Réglages" active={activeTab === 'settings'} onClick={() => setActiveTab('settings')} collapsed={!isSidebarOpen} />
              <button 
                onClick={handleLogout}
                className={cn(
                  "w-full flex items-center gap-4 px-4 py-3.5 rounded-2xl transition-all group relative overflow-hidden",
                  "text-zinc-500 hover:bg-red-500/10 hover:text-red-500"
                )}
              >
                <div className="relative z-10 transition-transform duration-300 group-hover:scale-110 group-hover:-translate-x-1">
                  <LogOut size={20} />
                </div>
                {isSidebarOpen && (
                  <span className="relative z-10 text-xs font-black uppercase tracking-widest transition-all duration-300 group-hover:translate-x-1">
                    Déconnexion
                  </span>
                )}
              </button>
            </div>
          </aside>
      </>
    )}

      <main className="flex-1 flex flex-col min-w-0 bg-zinc-950 relative pb-16 lg:pb-0">
        {!isReseller && (
          <div className="bg-primary/10 border-b border-primary/20 py-2 px-4 flex items-center justify-between z-50">
            <div className="flex items-center gap-2">
              <ShieldCheck size={14} className="text-primary" />
              <span className="text-[10px] font-black uppercase tracking-widest text-primary">Période d'essai Premium</span>
            </div>
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-1.5">
                <span className="text-[9px] font-black text-zinc-500 uppercase tracking-widest hidden sm:inline">Temps restant:</span>
                <span className="text-[10px] font-mono font-bold text-white bg-primary px-2 py-0.5 rounded shadow-lg shadow-primary/20">{timeLeft}</span>
              </div>
              <button 
                onClick={() => setActiveTab('credits')}
                className="text-[9px] font-black uppercase tracking-widest text-zinc-400 hover:text-white transition-colors border-l border-zinc-800 pl-3"
              >
                Acheter Licence
              </button>
            </div>
          </div>
        )}
        <header className="h-14 lg:h-16 border-b border-zinc-900 flex items-center justify-between px-4 lg:px-6 bg-black/50 backdrop-blur-xl sticky top-0 z-40">
          <div className="flex items-center gap-3">
            {!isGuest && (
              <button 
                onClick={() => setIsSidebarOpen(true)}
                className="lg:hidden p-1.5 text-zinc-400 hover:text-white bg-zinc-900 rounded-lg"
              >
                <Menu size={18} />
              </button>
            )}
            <div className="flex-1 max-w-lg relative hidden md:block">
              {!isReseller && (
                <>
                  <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-zinc-500" size={16} />
                  <input 
                    type="text" 
                    placeholder="Rechercher une chaîne..." 
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-full bg-zinc-900 border border-zinc-800 rounded-xl pl-10 pr-4 py-2 text-xs focus:outline-none focus:border-primary transition-all"
                  />
                </>
              )}
            </div>
          </div>
          
          <div className="flex items-center gap-2 lg:gap-3">
            <div className="hidden md:flex flex-col items-end mr-2">
              <span className="text-[9px] font-black text-zinc-600 uppercase tracking-[0.2em]">ID Appareil</span>
              <span className="text-[11px] font-mono font-black text-primary/80 tracking-widest">{macAddress}</span>
            </div>
            <button 
              onClick={handleLogout}
              className="bg-zinc-900 hover:bg-zinc-800 text-zinc-400 hover:text-white px-3 py-1.5 rounded-xl text-[10px] font-bold transition-all border border-zinc-800 flex items-center gap-2"
            >
              <LogOut size={12} />
              <span className="hidden sm:inline">Quitter</span>
            </button>
            <div className="flex items-center gap-3 bg-zinc-900/50 border border-zinc-800 px-4 py-2 rounded-xl">
              <div className="w-8 h-8 bg-primary/20 rounded-full flex items-center justify-center text-primary">
                <User size={18} />
              </div>
              <div className="hidden sm:block">
                <p className="text-xs font-black text-white leading-none">{isGuest ? 'Invité' : username}</p>
                {!isGuest && <p className="text-[9px] text-zinc-500 font-black uppercase tracking-widest mt-1">Premium</p>}
              </div>
            </div>
          </div>
        </header>

        <div className="flex-1 overflow-y-auto custom-scrollbar">
          {activeTab === 'home' && !isReseller && (
            <div className="p-6 lg:p-12 flex flex-col items-center justify-center min-h-[80vh] text-center space-y-12">
              <div className="space-y-6 max-w-2xl">
                <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary/10 border border-primary/20 text-primary text-xs font-black uppercase tracking-widest">
                  <ZapIcon size={14} className="fill-primary" />
                  Lecteur IPTV Premium
                </div>
                <h1 className="text-5xl lg:text-7xl font-black text-white tracking-tighter leading-none">
                  Prêt pour le <span className="text-primary italic">Streaming</span> ?
                </h1>
                <p className="text-zinc-500 text-lg font-medium leading-relaxed">
                  Connectez votre serveur ou ajoutez une playlist pour commencer à regarder vos contenus favoris en Ultra HD.
                </p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 w-full max-w-4xl">
                <div className="bg-zinc-900/40 border border-zinc-800/50 rounded-[2.5rem] p-8 backdrop-blur-md flex flex-col items-center gap-6 group hover:border-primary/30 transition-all">
                  <div className="p-5 bg-primary/10 rounded-2xl text-primary group-hover:scale-110 transition-transform">
                    <Globe size={32} />
                  </div>
                  <div className="space-y-2">
                    <h3 className="text-xl font-black text-white">Ajouter un Serveur</h3>
                    <p className="text-zinc-500 text-sm">Configurez votre accès Xtream Codes ou lien M3U.</p>
                  </div>
                  <button 
                    onClick={() => setShowAddServerModal(true)}
                    className="w-full bg-white text-black py-4 rounded-2xl font-black text-sm uppercase tracking-widest hover:bg-zinc-200 transition-all"
                  >
                    Configurer
                  </button>
                </div>

                <div className="bg-zinc-900/40 border border-zinc-800/50 rounded-[2.5rem] p-8 backdrop-blur-md flex flex-col items-center gap-6 group hover:border-blue-500/30 transition-all">
                  <div className="p-5 bg-blue-500/10 rounded-2xl text-blue-500 group-hover:scale-110 transition-transform">
                    <Smartphone size={32} />
                  </div>
                  <div className="space-y-2">
                    <h3 className="text-xl font-black text-white">ID Appareil</h3>
                    <p className="text-zinc-500 text-sm">Utilisez cet ID pour l'activation à distance.</p>
                  </div>
                  <div className="w-full bg-black/40 border border-zinc-800 rounded-2xl py-4 px-6 font-mono font-black text-blue-500 tracking-widest text-lg">
                    {macAddress}
                  </div>
                </div>
              </div>
            </div>
          )}

          {customPlaylists.map((p, i) => (
            activeTab === `playlist-${i}` && (
              <div key={i} className="p-6 space-y-6">
                <div className="flex items-center justify-between">
                  <div className="space-y-1">
                    <h2 className="text-3xl font-black text-white italic tracking-tighter">{p.name}</h2>
                    <p className="text-zinc-500 text-xs font-black uppercase tracking-widest">Playlist Connectée • {p.url}</p>
                  </div>
                  <button 
                    onClick={() => {
                      const newList = customPlaylists.filter((_, idx) => idx !== i);
                      setCustomPlaylists(newList);
                      localStorage.setItem('custom_playlists', JSON.stringify(newList));
                      setActiveTab('home');
                    }}
                    className="p-3 bg-red-500/10 text-red-500 rounded-xl hover:bg-red-500 hover:text-white transition-all"
                  >
                    <X size={20} />
                  </button>
                </div>
                
                <div className="bg-zinc-900/40 border border-zinc-800/50 rounded-[2.5rem] p-12 text-center space-y-6">
                  <div className="w-20 h-20 bg-primary/10 rounded-full flex items-center justify-center mx-auto text-primary">
                    <PlayCircle size={40} />
                  </div>
                  <div className="space-y-2">
                    <h3 className="text-2xl font-black text-white">Chargement du contenu...</h3>
                    <p className="text-zinc-500 max-w-md mx-auto">Nous synchronisons les chaînes, films et séries de votre serveur. Cela peut prendre quelques instants.</p>
                  </div>
                  <div className="w-48 h-1.5 bg-zinc-800 rounded-full mx-auto overflow-hidden">
                    <div className="w-1/3 h-full bg-primary animate-progress" />
                  </div>
                </div>
              </div>
            )
          ))}

          {isReseller && ['overview', 'clients', 'credits', 'account', 'check_mac', 'store_info', 'download_apk', 'api_docs', 'support'].includes(activeTab) && (
            <ResellerPanel activeTab={activeTab as any} setActiveTab={setActiveTab} />
          )}
          
          {(activeTab === 'movies' || activeTab === 'series') && !isReseller && (
            <div className="flex flex-col items-center justify-center h-full text-zinc-500 space-y-4">
              <Film size={64} className="opacity-20" />
              <p className="text-xl font-medium">Contenu VOD bientôt disponible</p>
              <p className="text-sm max-w-md text-center">Nous intégrons actuellement les catalogues TMDb pour vous offrir la meilleure expérience cinéma.</p>
            </div>
          )}
        </div>

        {/* Bottom Navigation for Mobile & TV */}
        {!isReseller && (
          <nav className="lg:hidden fixed bottom-0 left-0 right-0 h-20 bg-zinc-950/80 backdrop-blur-2xl border-t border-zinc-900 flex items-center justify-around px-6 z-50">
            {[
              { id: 'home', icon: Tv, label: 'Accueil' },
              { id: 'settings', icon: Settings, label: 'Réglages' }
            ].map((item) => (
              <button
                key={item.id}
                onClick={() => setActiveTab(item.id)}
                className={`flex flex-col items-center gap-1.5 transition-all tv-focus p-2 rounded-xl ${activeTab === item.id ? 'text-primary' : 'text-zinc-500'}`}
              >
                <item.icon size={22} className={activeTab === item.id ? 'fill-primary/20' : ''} />
                <span className="text-[10px] font-black uppercase tracking-widest">{item.label}</span>
              </button>
            ))}
          </nav>
        )}
      </main>

      <AnimatePresence>
        {notification && (
          <Toast
            message={notification.message}
            type={notification.type}
            onClose={() => setNotification(null)}
          />
        )}
      </AnimatePresence>
    </div>
  );
}

interface NavItemProps {
  icon: React.ReactNode;
  label: string;
  active: boolean;
  onClick: () => void;
  collapsed: boolean;
  key?: React.Key;
}
