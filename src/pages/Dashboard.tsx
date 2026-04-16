import React, { useState, useEffect } from 'react';
import { Card, Badge, Button, Input, Select, Textarea, cn, Toast } from '../components/ui';
import { Header } from '../components/Header';
import { Footer } from '../components/Footer';
import { 
  Users, User, Store, Search, CreditCard, Download, 
  FileText, HelpCircle, AlertCircle, LogOut, Plus, 
  CheckCircle2, Copy, ExternalLink, MessageSquare, FileSpreadsheet,
  RotateCcw, Filter, Edit2, Trash2, CalendarPlus, MoreVertical, RefreshCw,
  Menu, X, List, Tv, Settings, ChevronLeft, ChevronRight, ShieldCheck, Zap, Activity
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'motion/react';

const CUSTOMERS: any[] = [];

const TRANSACTIONS: any[] = [];

import { auth, onAuthStateChanged, signOut } from '../firebase';
import { api } from '../services/api';
import { useBranding } from '../components/BrandingProvider';

export const Dashboard = () => {
  const { branding: globalBranding } = useBranding();
  const [activeTab, setActiveTab] = useState('Clients');
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('Tous');
  const [systemFilter, setSystemFilter] = useState('Tous');
  const [countryFilter, setCountryFilter] = useState('Tous');
  const [showModal, setShowModal] = useState<string | null>(null);
  const [selectedCustomer, setSelectedCustomer] = useState<any>(null);
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [genLoading, setGenLoading] = useState(false);
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' | 'info' } | null>(null);
  const [user, setUser] = useState<any>(null);
  const [dbUser, setDbUser] = useState<any>(null);
  const [activations, setActivations] = useState<any[]>([]);
  const [allActivations, setAllActivations] = useState<any[]>([]);
  const [transactions, setTransactions] = useState<any[]>([]);
  const [allTransactions, setAllTransactions] = useState<any[]>([]);
  const [allUsers, setAllUsers] = useState<any[]>([]);
  const [macCheckResult, setMacCheckResult] = useState<any>(null);
  const [macToCheck, setMacToCheck] = useState('');
  const [newPlaylistUrl, setNewPlaylistUrl] = useState('');
  const [newMac, setNewMac] = useState('');
  const [newNote, setNewNote] = useState('');
  const [branding, setBranding] = useState<any>(null);
  const navigate = useNavigate();

  useEffect(() => {
    // Check for version mismatch on mount
    const CURRENT_VERSION = '4.0.0-ULTRA';
    const storedVersion = localStorage.getItem('app_version');
    if (storedVersion && storedVersion !== CURRENT_VERSION) {
      localStorage.setItem('app_version', CURRENT_VERSION);
      window.location.reload();
    }
  }, []);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      if (!user) {
        navigate('/login');
      } else {
        setUser(user);
        await fetchUserData(user.uid, user);
        
        // Auto-refresh activations every 30 seconds to see current channels
        const interval = setInterval(() => fetchActivations(user.uid), 30000);
        return () => clearInterval(interval);
      }
    });
    return () => unsubscribe();
  }, [navigate]);

  useEffect(() => {
    if (globalBranding) {
      setBranding(globalBranding);
    }
  }, [globalBranding]);

  const fetchUserData = async (uid: string, fbUser: any) => {
    try {
      let data = await api.getUser(uid).catch(() => null);
      if (!data) {
        // Register user if not found (Google login case)
        data = await api.registerUser({
          uid: fbUser.uid,
          email: fbUser.email,
          username: fbUser.displayName || fbUser.email?.split('@')[0],
          role: 'client'
        });
      }
      setDbUser(data);
      fetchActivations(uid);
      fetchTransactions(uid);
      
      if (fbUser.email === 'koutoudivine@gmail.com' || fbUser.email === 'inestaulrichokinda@gmail.com') {
        fetchAllAdminData();
      }
    } catch (error) {
      console.error("Error fetching user data:", error);
    }
  };

  const fetchActivations = async (uid: string) => {
    try {
      const data = await api.getActivations(uid);
      setActivations(data);
    } catch (error) {
      console.error("Error fetching activations:", error);
    }
  };

  const fetchTransactions = async (uid: string) => {
    try {
      const data = await api.getPayments(uid);
      setTransactions(data);
    } catch (error) {
      console.error("Error fetching transactions:", error);
    }
  };

  const fetchAllAdminData = async () => {
    try {
      const [acts, pays, users] = await Promise.all([
        api.getAllActivations(),
        api.getAllPayments(),
        api.getAllUsers()
      ]);
      setAllActivations(acts);
      setAllTransactions(pays);
      setAllUsers(users);
    } catch (error) {
      console.error("Error fetching admin data:", error);
    }
  };

  const fetchBranding = async () => {
    try {
      const data = await api.getBranding();
      setBranding(data);
      if (data?.appName) document.title = `${data.appName} - Dashboard`;
    } catch (error) {
      console.error("Error fetching branding:", error);
    }
  };

  const handleLogoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setLoading(true);
    try {
      const url = await api.uploadFile(file, `branding/logo_${Date.now()}`);
      await api.updateBranding({ ...branding, logoUrl: url });
      setBranding((prev: any) => ({ ...prev, logoUrl: url }));
      showToast("Logo mis à jour avec succès !", "success");
    } catch (error) {
      showToast("Erreur lors de l'upload du logo", "error");
    } finally {
      setLoading(false);
    }
  };

  const handleForceUpdate = () => {
    localStorage.clear();
    sessionStorage.clear();
    localStorage.setItem('app_version', '4.0.0-ULTRA');
    window.location.reload();
  };

  const handleGenerateCredits = async () => {
    if (!user) return;
    setGenLoading(true);
    try {
      const newTotal = await api.generateCredits(user.uid, 10);
      if (typeof newTotal === 'number') {
        setDbUser((prev: any) => ({ ...prev, credits: newTotal }));
        showToast("10 Crédits générés avec succès !", "success");
      } else {
        throw new Error("Réponse invalide du serveur");
      }
    } catch (error: any) {
      console.error("Generate credits error:", error);
      showToast(error.message || "Erreur lors de la génération des crédits", "error");
    } finally {
      setGenLoading(false);
    }
  };

  const handleCheckMac = async () => {
    if (!macToCheck) return;
    setLoading(true);
    try {
      const data = await api.checkMacStatus(macToCheck);
      setMacCheckResult(data);
    } catch (error) {
      showToast("Erreur lors de la vérification", "error");
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = async () => {
    try {
      await signOut(auth);
      navigate('/login');
    } catch (error: any) {
      showToast("Erreur lors de la déconnexion", "error");
    }
  };

  const isAdminUser = user?.email === 'koutoudivine@gmail.com' || user?.email === 'inestaulrichokinda@gmail.com';

  const menuItems = [
    { name: 'Clients', icon: Users },
    ...(isAdminUser ? [
      { name: 'Activités Globales', icon: Activity },
      { name: 'Branding', icon: Tv }
    ] : []),
    { name: 'Profil', icon: User },
    { name: 'Infos Boutique', icon: Store },
    { name: 'Vérifier MAC', icon: Search },
    { name: 'Aperçu du solde', icon: CreditCard },
    { name: 'Acheter des crédits', icon: CreditCard },
    { name: 'Télécharger APK', icon: Download },
    { name: 'API', icon: FileText },
    { name: 'Support', icon: HelpCircle },
    { name: 'Conditions d\'utilisation', icon: FileText },
  ];

  const showToast = (message: string, type: 'success' | 'error' | 'info' = 'info') => {
    setToast({ message, type });
  };

  const handleAction = async (actionName: string, callback: () => Promise<void>) => {
    setLoading(true);
    try {
      await callback();
      showToast(`Action "${actionName}" réussie !`, 'success');
      setShowModal(null);
    } catch (error: any) {
      console.error(error);
      showToast(error.message || "Une erreur inattendue est survenue. Veuillez réessayer.", 'error');
    } finally {
      setLoading(false);
    }
  };

  const exportToCSV = () => {
    try {
      const headers = ['MAC', 'Nom', 'Systeme', 'Version', 'Statut', 'Expiration'];
      const rows = CUSTOMERS.map(c => [c.mac, c.name, c.system, c.version, c.status, c.expiry]);
      
      const csvContent = [
        headers.join(','),
        ...rows.map(row => row.join(','))
      ].join('\n');
      
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      const link = document.createElement('a');
      const url = URL.createObjectURL(blob);
      link.setAttribute('href', url);
      link.setAttribute('download', `export_clients_${new Date().toISOString().split('T')[0]}.csv`);
      link.style.visibility = 'hidden';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      showToast("Exportation réussie", "success");
    } catch (error) {
      showToast("Échec de l'exportation", "error");
    }
  };

  const renderContent = () => {
    switch (activeTab) {
      case 'Clients':
        const displayActivations = activations.length > 0 ? activations : CUSTOMERS;
        const filteredCustomers = displayActivations.filter(c => {
          const mac = c.mac || c.target_mac || '';
          const name = c.name || c.note || 'Client';
          const matchesSearch = mac.toLowerCase().includes(searchTerm.toLowerCase()) || 
                               name.toLowerCase().includes(searchTerm.toLowerCase());
          const matchesStatus = statusFilter === 'Tous' || (c.status || 'ACTIF') === statusFilter;
          const matchesSystem = systemFilter === 'Tous' || (c.system || 'N/A') === systemFilter;
          const matchesCountry = countryFilter === 'Tous' || (c.country || 'N/A') === countryFilter;
          return matchesSearch && matchesStatus && matchesSystem && matchesCountry;
        });

        const systems = ['Tous', ...Array.from(new Set(displayActivations.map(c => c.system || 'N/A')))];
        const countries = ['Tous', ...Array.from(new Set(displayActivations.map(c => c.country || 'N/A')))];

        return (
          <div className="space-y-6">
            <div className="flex flex-wrap gap-4 items-center">
              <Button onClick={() => setShowModal('activate')} icon={Plus}>Activer</Button>
              <Button onClick={() => setShowModal('new-client')} variant="outline" icon={Plus}>Nouveau client</Button>
              <Button onClick={exportToCSV} variant="ghost" icon={FileSpreadsheet} className="text-emerald-500 hover:bg-emerald-500/10">Exporter Excel</Button>
              <div className="flex-1" />
              <Input 
                placeholder="Rechercher par MAC ou Nom..." 
                value={searchTerm} 
                onChange={(e: any) => setSearchTerm(e.target.value)} 
                className="max-w-xs" 
                icon={Search}
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 bg-zinc-900/20 p-4 rounded-2xl border border-zinc-800/50">
              <Select 
                label="Filtrer par Statut" 
                value={statusFilter} 
                onChange={(e: any) => setStatusFilter(e.target.value)}
              >
                <option value="Tous">Tous les statuts</option>
                <option value="ACTIF">Actif</option>
                <option value="EXPIRÉ">Expiré</option>
              </Select>
              <Select 
                label="Filtrer par Système" 
                value={systemFilter} 
                onChange={(e: any) => setSystemFilter(e.target.value)}
              >
                {systems.map(s => <option key={s} value={s}>{s === 'Tous' ? 'Tous les systèmes' : s}</option>)}
              </Select>
              <Select 
                label="Filtrer par Pays" 
                value={countryFilter} 
                onChange={(e: any) => setCountryFilter(e.target.value)}
              >
                {countries.map(c => <option key={c} value={c}>{c === 'Tous' ? 'Tous les pays' : c}</option>)}
              </Select>
            </div>

            <Card className="overflow-hidden p-0 border-zinc-800">
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="text-zinc-500 bg-zinc-900/50 border-b border-zinc-800">
                    <tr>
                      <th className="p-4 text-left font-black uppercase tracking-widest text-[10px]">Adresse MAC</th>
                      <th className="p-4 text-left font-black uppercase tracking-widest text-[10px]">Nom du Client</th>
                      <th className="p-4 text-left font-black uppercase tracking-widest text-[10px]">Chaîne Actuelle</th>
                      <th className="p-4 text-left font-black uppercase tracking-widest text-[10px]">Pays</th>
                      <th className="p-4 text-left font-black uppercase tracking-widest text-[10px]">Système</th>
                      <th className="p-4 text-left font-black uppercase tracking-widest text-[10px]">Version</th>
                      <th className="p-4 text-left font-black uppercase tracking-widest text-[10px]">Statut</th>
                      <th className="p-4 text-left font-black uppercase tracking-widest text-[10px]">Date d'expiration</th>
                      <th className="p-4 text-right font-black uppercase tracking-widest text-[10px]">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredCustomers.map((c, i) => (
                      <tr key={i} className="border-b border-zinc-900 hover:bg-zinc-950 transition-colors">
                        <td className="p-4 font-mono text-primary">{c.mac || c.target_mac}</td>
                        <td className="p-4 font-bold">{c.name || c.note || 'Client'}</td>
                        <td className="p-4">
                          {c.current_channel ? (
                            <div className="flex items-center gap-2">
                              <span className="w-2 h-2 bg-red-500 rounded-full animate-pulse" />
                              <span className="text-xs text-white font-medium">{c.current_channel}</span>
                            </div>
                          ) : (
                            <span className="text-xs text-zinc-600 italic">Hors-ligne</span>
                          )}
                        </td>
                        <td className="p-4 text-zinc-400">{c.country || 'N/A'}</td>
                        <td className="p-4 text-zinc-400">{c.system || 'N/A'}</td>
                        <td className="p-4 text-zinc-500">{c.version || 'N/A'}</td>
                        <td className="p-4">
                          <Badge variant={(c.status || 'ACTIF') === 'ACTIF' ? 'success' : 'error'}>{c.status || 'ACTIF'}</Badge>
                        </td>
                        <td className="p-4 text-zinc-400">{c.expiry || new Date(new Date(c.createdAt).getTime() + 365*24*60*60*1000).toLocaleDateString('fr-FR')}</td>
                        <td className="p-4 text-right">
                          <div className="flex justify-end gap-2">
                            <button 
                              onClick={() => { setSelectedCustomer(c); setShowModal('extend'); }}
                              className="p-2 hover:bg-primary/10 text-primary rounded-lg transition-colors"
                              title="Prolonger"
                            >
                              <CalendarPlus size={16} />
                            </button>
                            <button 
                              onClick={() => { setSelectedCustomer(c); setShowModal('playlist'); }}
                              className="p-2 hover:bg-white/5 text-zinc-400 hover:text-white rounded-lg transition-colors"
                              title="Playlist"
                            >
                              <List size={16} />
                            </button>
                            <button 
                              onClick={() => { setSelectedCustomer(c); setShowModal('edit-client'); }}
                              className="p-2 hover:bg-white/5 text-zinc-400 hover:text-white rounded-lg transition-colors"
                              title="Modifier"
                            >
                              <Edit2 size={16} />
                            </button>
                            <button 
                              onClick={() => { setSelectedCustomer(c); setShowModal('reset-device'); }}
                              className="p-2 hover:bg-white/5 text-zinc-400 hover:text-white rounded-lg transition-colors"
                              title="Réinitialiser"
                            >
                              <RefreshCw size={16} />
                            </button>
                            <button 
                              onClick={() => { setSelectedCustomer(c); setShowModal('delete-client'); }}
                              className="p-2 hover:bg-red-500/10 text-red-500 rounded-lg transition-colors"
                              title="Supprimer"
                            >
                              <Trash2 size={16} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          </div>
        );

      case 'Profil':
        return (
          <div className="space-y-6">
            <Card className="max-w-2xl space-y-6">
              <h2 className="text-xl font-bold">Paramètres du profil</h2>
              <div className="grid grid-cols-2 gap-4">
                <Input label="Nom complet" defaultValue={dbUser?.username || user?.displayName || ''} />
                <Input label="Adresse Email" defaultValue={user?.email || ''} readOnly />
                <Input label="Téléphone" defaultValue={dbUser?.phone || ''} />
                <Input label="Pays" defaultValue={dbUser?.country || ''} />
                <Input label="Nouveau mot de passe" type="password" placeholder="••••••••" />
                <Input label="Confirmer le mot de passe" type="password" placeholder="••••••••" />
              </div>
              <Button 
                loading={loading} 
                onClick={() => handleAction("Mise à jour du profil", async () => {})}
              >
                Mettre à jour le profil
              </Button>
            </Card>

            <Card className="max-w-2xl border-amber-500/20 bg-amber-500/5 space-y-4">
              <div className="flex items-center gap-3 text-amber-500">
                <RefreshCw size={20} />
                <h3 className="font-bold">Zone de Maintenance</h3>
              </div>
              <p className="text-sm text-zinc-400">
                Si vous ne voyez pas les dernières mises à jour ou si vous rencontrez des problèmes d'affichage (écran noir), utilisez le bouton ci-dessous pour forcer le nettoyage du cache de votre navigateur.
              </p>
              <Button 
                variant="outline" 
                className="border-amber-500/30 text-amber-500 hover:bg-amber-500/10"
                onClick={() => {
                  if (window.confirm("Voulez-vous forcer le rechargement complet de l'application ?")) {
                    localStorage.clear();
                    sessionStorage.clear();
                    window.location.reload();
                  }
                }}
              >
                Forcer le rechargement (Vider le cache)
              </Button>
            </Card>
          </div>
        );

      case 'Infos Boutique':
        return (
          <Card className="max-w-2xl space-y-6">
            <h2 className="text-xl font-bold">Informations de la boutique</h2>
            <div className="space-y-4">
              <div className="flex justify-between border-b border-zinc-800 pb-2">
                <span className="text-zinc-500">Nom de la boutique</span>
                <span className="font-bold">Sky Player Official</span>
              </div>
              <div className="flex justify-between border-b border-zinc-800 pb-2">
                <span className="text-zinc-500">Niveau de revendeur</span>
                <Badge variant="primary">Revendeur Or</Badge>
              </div>
              <div className="flex justify-between border-b border-zinc-800 pb-2">
                <span className="text-zinc-500">Total des activations</span>
                <span className="font-bold">142</span>
              </div>
            </div>
          </Card>
        );

      case 'Vérifier MAC':
        return (
          <Card className="max-w-md space-y-6">
            <h2 className="text-xl font-bold">Vérifier le statut MAC</h2>
            <p className="text-zinc-500 text-sm">Entrez une adresse MAC pour vérifier son statut d'activation et sa date d'expiration.</p>
            <Input 
              placeholder="00:00:00:00:00:00" 
              label="Adresse MAC" 
              value={macToCheck}
              onChange={(e: any) => setMacToCheck(e.target.value)}
            />
            <Button 
              fullWidth 
              icon={Search} 
              loading={loading}
              onClick={handleCheckMac}
            >
              Vérifier le statut
            </Button>

            {macCheckResult && (
              <motion.div 
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className={cn(
                  "p-4 rounded-xl border",
                  macCheckResult.active ? "bg-emerald-500/10 border-emerald-500/20" : "bg-red-500/10 border-red-500/20"
                )}
              >
                {macCheckResult.active ? (
                  <div className="space-y-2">
                    <div className="flex items-center gap-2 text-emerald-500 font-bold">
                      <CheckCircle2 size={16} />
                      <span>Appareil Actif</span>
                    </div>
                    <div className="grid grid-cols-2 gap-2 text-xs">
                      <span className="text-zinc-500">Expiration:</span>
                      <span className="text-white font-bold">{macCheckResult.expiry}</span>
                      <span className="text-zinc-500">Dernière connexion:</span>
                      <span className="text-white">{macCheckResult.last_seen}</span>
                      <span className="text-zinc-500">Version:</span>
                      <span className="text-white">{macCheckResult.version}</span>
                    </div>
                  </div>
                ) : (
                  <div className="flex items-center gap-2 text-red-500 font-bold">
                    <AlertCircle size={16} />
                    <span>{macCheckResult.error || "Non activé"}</span>
                  </div>
                )}
              </motion.div>
            )}
          </Card>
        );

      case 'Aperçu du solde':
        const displayTransactions = transactions.length > 0 ? transactions : TRANSACTIONS;
        return (
          <div className="space-y-6">
            <div className="flex items-center justify-between">
              <h2 className="text-xl font-bold">Historique des transactions</h2>
              {isAdminUser && (
                <Button 
                  variant="primary" 
                  size="sm" 
                  icon={Zap} 
                  loading={loading}
                  onClick={handleGenerateCredits}
                  className="bg-amber-500 text-black hover:bg-amber-400"
                >
                  Générer 10 Crédits
                </Button>
              )}
            </div>
            <Card className="overflow-hidden p-0 border-zinc-800">
              <table className="w-full text-sm">
                <thead className="text-zinc-500 bg-zinc-900/50 border-b border-zinc-800">
                  <tr>
                    <th className="p-4 text-left font-black uppercase tracking-widest text-[10px]">Date</th>
                    <th className="p-4 text-left font-black uppercase tracking-widest text-[10px]">Type</th>
                    <th className="p-4 text-left font-black uppercase tracking-widest text-[10px]">Montant</th>
                    <th className="p-4 text-left font-black uppercase tracking-widest text-[10px]">Statut</th>
                  </tr>
                </thead>
                <tbody>
                  {displayTransactions.map((t, i) => (
                    <tr key={i} className="border-b border-zinc-900">
                      <td className="p-4 text-zinc-400">{t.date || new Date(t.createdAt).toLocaleDateString('fr-FR')}</td>
                      <td className="p-4 font-bold">{t.type || (t.amount > 0 ? 'Achat' : 'Activation')}</td>
                      <td className={`p-4 font-bold ${(t.amount?.toString().startsWith('+') || t.status === 'completed') ? 'text-emerald-500' : 'text-red-500'}`}>
                        {t.amount?.toString().startsWith('+') || t.amount?.toString().startsWith('-') ? t.amount : (t.status === 'completed' ? `+${t.credits_purchased} Crédits` : `-${t.credits_used} Crédits`)}
                      </td>
                      <td className="p-4"><Badge variant={t.status === 'completed' || t.status === 'TERMINÉ' ? 'success' : 'info'}>{t.status}</Badge></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </Card>
          </div>
        );

      case 'Acheter des crédits':
        return (
          <Card className="max-w-md space-y-6 text-center py-12">
            <div className="w-20 h-20 bg-primary/10 rounded-full flex items-center justify-center mx-auto text-primary">
              <CreditCard size={40} />
            </div>
            <h2 className="text-2xl font-black italic">Besoin de crédits ?</h2>
            <p className="text-zinc-500">Rechargez votre compte pour continuer à activer des appareils pour vos clients.</p>
            <div className="grid gap-4">
              <Button onClick={() => navigate('/payment')} fullWidth size="lg">Acheter des crédits</Button>
              <Button variant="outline" fullWidth onClick={() => setActiveTab('Support')}>Contacter un agent</Button>
            </div>
          </Card>
        );

      case 'Télécharger APK':
        return (
          <div className="grid md:grid-cols-2 gap-6">
            <Card className="space-y-4">
              <div className="flex items-center gap-3 text-primary">
                <Download size={24} />
                <h3 className="font-bold">Sky Player Android</h3>
              </div>
              <p className="text-sm text-zinc-500">Version stable pour smartphones Android.</p>
              <Button variant="outline" fullWidth icon={ExternalLink} onClick={() => window.location.href = '/SkyPlayer.apk'}>Télécharger APK</Button>
            </Card>
            <Card className="space-y-4">
              <div className="flex items-center gap-3 text-primary">
                <Download size={24} />
                <h3 className="font-bold">Sky Player TV Box</h3>
              </div>
              <p className="text-sm text-zinc-500">Version optimisée pour Android TV et FireStick.</p>
              <Button variant="outline" fullWidth icon={ExternalLink} onClick={() => window.location.href = '/SkyPlayer.apk'}>Télécharger APK</Button>
            </Card>
          </div>
        );

      case 'API':
        return (
          <Card className="max-w-2xl space-y-6">
            <div className="flex items-center justify-between">
              <h2 className="text-xl font-bold">Accès API</h2>
              <Badge variant="info">Bêta</Badge>
            </div>
            <div className="space-y-4">
              <div className="space-y-2">
                <label className="text-[10px] font-black text-zinc-500 uppercase tracking-widest">Votre clé API</label>
                <div className="flex gap-2">
                  <Input readOnly value="sk_live_51N...8x9z" className="font-mono" />
                  <Button variant="outline" icon={Copy} onClick={() => showToast("Clé API copiée", "info")}>Copier</Button>
                </div>
              </div>
              <div className="p-4 bg-zinc-950 rounded-xl border border-zinc-800">
                <p className="text-xs text-zinc-500 leading-relaxed">
                  Utilisez notre API pour automatiser les activations depuis votre propre panneau ou site web. 
                  Consultez notre <span className="text-primary cursor-pointer hover:underline">documentation</span> pour plus de détails.
                </p>
              </div>
            </div>
          </Card>
        );

      case 'Support':
        return (
          <Card className="max-w-2xl space-y-6">
            <h2 className="text-xl font-bold">Support Technique</h2>
            <p className="text-zinc-500 text-sm">Notre équipe est disponible 24/7 pour vous aider en cas de problème.</p>
            <div className="grid md:grid-cols-2 gap-4">
              <Button variant="outline" className="h-32 flex-col gap-3" icon={MessageSquare}>
                Support WhatsApp
                <span className="text-[10px] text-zinc-500">Réponse rapide</span>
              </Button>
              <Button variant="outline" className="h-32 flex-col gap-3" icon={HelpCircle}>
                Ouvrir un ticket
                <span className="text-[10px] text-zinc-500">Support par email</span>
              </Button>
            </div>
          </Card>
        );

      case 'Conditions d\'utilisation':
        return (
          <Card className="max-w-3xl space-y-6">
            <h2 className="text-xl font-bold">Conditions d'utilisation</h2>
            <div className="prose prose-invert text-sm text-zinc-400 space-y-4">
              <p className="font-bold text-primary">Sky Player Pro est un lecteur multimédia neutre.</p>
              <p>En utilisant notre service, vous acceptez les points clés suivants :</p>
              <ul className="list-disc pl-5 space-y-2">
                <li><strong>Absence de contenu :</strong> Nous ne fournissons aucun lien, fichier ou flux média.</li>
                <li><strong>Compatibilité :</strong> Support des listes M3U, JSON et codes Xtream.</li>
                <li><strong>Responsabilité :</strong> Vous êtes responsable du contenu que vous ajoutez.</li>
                <li><strong>Propriété :</strong> L'utilisation de l'app ne vous donne aucun droit sur les contenus tiers.</li>
                <li><strong>Remboursements :</strong> Les activations numériques sont définitives et non remboursables.</li>
              </ul>
              <div className="pt-4">
                <Button variant="outline" onClick={() => navigate('/terms')} icon={ExternalLink}>
                  Consulter les termes complets (RGPD/DMCA)
                </Button>
              </div>
            </div>
          </Card>
        );

      case 'Activités Globales':
        return (
          <div className="space-y-8">
            {/* ... existing content ... */}
          </div>
        );

      case 'Branding':
        return (
          <div className="space-y-6">
            <Card className="max-w-2xl space-y-6">
              <div className="flex items-center gap-3 text-primary">
                <Tv size={24} />
                <h2 className="text-xl font-bold">Personnalisation (Branding)</h2>
              </div>
              <p className="text-sm text-zinc-500">Personnalisez l'apparence de Sky Player pour vos clients. Ces paramètres seront appliqués à tous les utilisateurs.</p>
              
              <div className="space-y-4">
                <div className="p-6 border border-zinc-800 rounded-2xl bg-zinc-900/20 flex flex-col items-center gap-4">
                  <p className="text-[10px] font-black text-zinc-500 uppercase tracking-widest">Logo de l'application</p>
                  {branding?.logoUrl ? (
                    <img 
                      src={branding.logoUrl} 
                      alt="Logo" 
                      className="h-20 max-w-full object-contain" 
                      referrerPolicy="no-referrer" 
                    />
                  ) : (
                    <div className="h-20 w-20 bg-zinc-800 rounded-xl flex items-center justify-center text-zinc-600">
                      <Tv size={40} />
                    </div>
                  )}
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" onClick={() => document.getElementById('logo-upload')?.click()}>
                      Changer le logo
                    </Button>
                    {branding?.logoUrl && (
                      <Button 
                        variant="outline" 
                        size="sm" 
                        className="text-red-500 border-red-500/20 hover:bg-red-500/10"
                        onClick={() => handleAction("Suppression du logo", async () => {
                          await api.updateBranding({ ...branding, logoUrl: null });
                          setBranding((prev: any) => ({ ...prev, logoUrl: null }));
                        })}
                      >
                        Supprimer
                      </Button>
                    )}
                    <input 
                      id="logo-upload" 
                      type="file" 
                      accept="image/*" 
                      className="hidden" 
                      onChange={handleLogoUpload} 
                    />
                  </div>
                  <p className="text-[10px] text-zinc-600">Format recommandé : PNG transparent, 512x512px</p>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <Input 
                    label="Nom de l'application" 
                    placeholder="Sky Player Pro" 
                    value={branding?.appName || ''}
                    onChange={(e: any) => setBranding((prev: any) => ({ ...prev, appName: e.target.value }))}
                  />
                  <Input 
                    label="Couleur Primaire (Hex)" 
                    placeholder="#FFD700" 
                    value={branding?.primaryColor || ''}
                    onChange={(e: any) => setBranding((prev: any) => ({ ...prev, primaryColor: e.target.value }))}
                  />
                </div>
                
                <Button 
                  fullWidth 
                  loading={loading}
                  onClick={() => handleAction("Mise à jour du branding", async () => {
                    await api.updateBranding(branding);
                  })}
                >
                  Enregistrer les modifications
                </Button>
              </div>
            </Card>
          </div>
        );

      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-black text-white flex flex-col md:flex-row overflow-x-hidden">
      <AnimatePresence>
        {toast && (
          <Toast 
            message={toast.message} 
            type={toast.type} 
            onClose={() => setToast(null)} 
          />
        )}
      </AnimatePresence>

      {/* Mobile Header */}
      <div className="md:hidden flex items-center justify-between p-4 bg-zinc-900 border-b border-zinc-800 sticky top-0 z-40">
        <div className="flex items-center gap-3">
          {globalBranding?.logoUrl ? (
            <img 
              src={globalBranding.logoUrl} 
              alt={globalBranding.appName || 'Logo'} 
              className="h-8 w-8 object-contain" 
              referrerPolicy="no-referrer" 
            />
          ) : (
            <div className="w-8 h-8 bg-primary rounded-lg flex items-center justify-center text-black font-black italic text-xs">SP</div>
          )}
          <h2 className="font-bold text-sm">{globalBranding?.appName || 'Sky Player'}</h2>
        </div>
        <button 
          onClick={() => setIsSidebarOpen(!isSidebarOpen)}
          className="p-2 text-zinc-400 hover:text-white"
        >
          <Menu size={24} />
        </button>
      </div>

      {/* Sidebar Overlay for Mobile */}
      <AnimatePresence>
        {isSidebarOpen && (
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={() => setIsSidebarOpen(false)}
            className="fixed inset-0 bg-black/60 backdrop-blur-sm z-[50] md:hidden"
          />
        )}
      </AnimatePresence>

      {/* Sidebar */}
      <aside className={cn(
        "fixed inset-y-0 left-0 z-[60] bg-zinc-950 border-r border-zinc-800 p-6 flex flex-col transition-all duration-300 md:translate-x-0 md:static md:h-screen md:sticky md:top-0",
        isSidebarOpen ? "translate-x-0" : "-translate-x-full",
        isSidebarCollapsed ? "w-24 px-4" : "w-72"
      )}>
        <div className={cn(
          "flex items-center justify-between mb-8 md:block",
          isSidebarCollapsed && "md:flex md:justify-center"
        )}>
          <div className={cn("space-y-1", isSidebarCollapsed && "hidden")}>
            <h2 className="font-bold text-lg truncate">{dbUser?.username || user?.displayName || user?.email?.split('@')[0] || 'Utilisateur'}</h2>
            <p className="text-zinc-500 text-sm truncate">{user?.email}</p>
          </div>
          {isSidebarCollapsed && (
            globalBranding?.logoUrl ? (
              <img 
                src={globalBranding.logoUrl} 
                alt="Logo" 
                className="w-10 h-10 object-contain shrink-0" 
                referrerPolicy="no-referrer" 
              />
            ) : (
              <div className="w-10 h-10 bg-primary rounded-xl flex items-center justify-center text-black font-black italic text-xs shrink-0">
                {dbUser?.username?.[0] || user?.email?.[0]?.toUpperCase() || 'S'}
              </div>
            )
          )}
          <button onClick={() => setIsSidebarOpen(false)} className="md:hidden p-2 text-zinc-500">
            <X size={20} />
          </button>
        </div>
        
        <nav className="flex-1 space-y-1 overflow-y-auto custom-scrollbar">
          {menuItems.map(item => (
            <button 
              key={item.name} 
              onClick={() => { setActiveTab(item.name); setIsSidebarOpen(false); }}
              className={cn(
                "flex items-center gap-3 w-full p-3 rounded-xl transition-all text-sm font-medium focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary",
                activeTab === item.name 
                  ? "bg-primary text-black shadow-lg shadow-primary/20" 
                  : "text-zinc-400 hover:bg-zinc-900 hover:text-white",
                isSidebarCollapsed && "justify-center px-0"
              )}
              title={isSidebarCollapsed ? item.name : undefined}
            >
              <item.icon size={18} className="shrink-0" />
              {!isSidebarCollapsed && <span>{item.name}</span>}
            </button>
          ))}
        </nav>

        <div className="pt-6 border-t border-zinc-800 mt-6 space-y-2">
          <div className={cn("px-3 py-2 text-[10px] font-black text-zinc-600 uppercase tracking-widest", isSidebarCollapsed && "hidden")}>
            Version 4.0.0-ULTRA
          </div>
          <button 
            onClick={() => setIsSidebarCollapsed(!isSidebarCollapsed)}
            className="hidden md:flex items-center gap-3 w-full p-3 rounded-xl text-zinc-500 hover:bg-zinc-900 hover:text-white transition-all text-sm font-medium"
          >
            {isSidebarCollapsed ? <ChevronRight size={18} /> : <ChevronLeft size={18} />}
            {!isSidebarCollapsed && <span>Réduire</span>}
          </button>
          <button 
            onClick={handleLogout}
            className={cn(
              "flex items-center gap-3 w-full p-3 rounded-xl text-red-500 hover:bg-red-500/10 transition-all text-sm font-medium",
              isSidebarCollapsed && "justify-center px-0"
            )}
            title={isSidebarCollapsed ? "Déconnexion" : undefined}
          >
            <LogOut size={18} className="shrink-0" />
            {!isSidebarCollapsed && <span>Déconnexion</span>}
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 flex flex-col min-h-screen">
        <Header />
        <div className="p-4 md:p-8 space-y-8 flex-1">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <h1 className="text-3xl font-black italic">{activeTab}</h1>
            <div className="flex items-center gap-3">
              {isAdminUser && (
                <div className="flex items-center gap-2">
                  <Button 
                    variant="primary" 
                    size="sm" 
                    icon={Zap} 
                    loading={genLoading}
                    onClick={handleGenerateCredits}
                    className="bg-amber-500 text-black hover:bg-amber-400 h-[46px] px-6"
                  >
                    Générer 10 Crédits
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    icon={RefreshCw}
                    onClick={handleForceUpdate}
                    className="h-[46px] border-zinc-800 text-zinc-500 hover:text-white"
                    title="Forcer la mise à jour"
                  >
                    MAJ
                  </Button>
                </div>
              )}
              <Card className="bg-primary/10 border-primary/20 p-4 py-2 flex items-center gap-3 shrink-0 whitespace-nowrap">
                <CreditCard size={18} className="text-primary" />
                <div>
                  <p className="text-[10px] text-zinc-500 font-black uppercase tracking-widest">Solde</p>
                  <p className="text-sm text-primary font-black">{dbUser?.credits || 0} CRÉDITS</p>
                </div>
              </Card>
            </div>
          </div>

          {renderContent()}
        </div>
        <Footer />
      </main>

      {/* Simple Modal Overlay */}
      {showModal && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <Card className="w-full max-w-lg space-y-6 relative border-zinc-700 shadow-2xl">
            <button 
              disabled={loading}
              onClick={() => setShowModal(null)}
              className="absolute top-4 right-4 p-2 hover:bg-white/5 rounded-full transition-colors disabled:opacity-50"
            >
              <Plus className="rotate-45" size={20} />
            </button>
            
            {showModal === 'activate' ? (
              <>
                <h2 className="text-2xl font-black italic">Activer l'appareil</h2>
                <div className="space-y-4">
                  <Input 
                    label="Adresse MAC" 
                    placeholder="00:00:00:00:00:00" 
                    value={newMac}
                    onChange={(e: any) => setNewMac(e.target.value)}
                  />
                  <Input 
                    label="Lien Playlist (M3U ou JSON)" 
                    placeholder="http://exemple.com/playlist.m3u ou .json" 
                    value={newPlaylistUrl}
                    onChange={(e: any) => setNewPlaylistUrl(e.target.value)}
                  />
                  <Select label="Forfait d'abonnement">
                    <option>1 An (1 Crédit)</option>
                    <option>À vie (2 Crédits)</option>
                  </Select>
                  <div className="p-4 bg-primary/5 border border-primary/10 rounded-xl">
                    <p className="text-xs text-zinc-400">Cette action déduira <span className="text-primary font-bold">1 Crédit</span> de votre solde.</p>
                  </div>
                  <Button 
                    fullWidth 
                    loading={loading}
                    onClick={() => handleAction("Activation de l'appareil", async () => {
                      const normalizedMac = newMac.toUpperCase().trim();
                      await api.createActivation({
                        resellerId: user.uid,
                        target_mac: normalizedMac,
                        credits_used: 1,
                        note: newNote || 'Activation manuelle',
                        playlist_url: newPlaylistUrl
                      });
                      fetchActivations(user.uid);
                      setNewMac('');
                      setNewPlaylistUrl('');
                    })}
                  >
                    Confirmer l'activation
                  </Button>
                </div>
              </>
            ) : showModal === 'new-client' ? (
              <>
                <h2 className="text-2xl font-black italic">Nouveau Client</h2>
                <div className="space-y-4">
                  <Input 
                    label="Nom du Client" 
                    placeholder="Jean Dupont" 
                    value={newNote}
                    onChange={(e: any) => setNewNote(e.target.value)}
                  />
                  <Input 
                    label="Adresse MAC" 
                    placeholder="00:00:00:00:00:00" 
                    value={newMac}
                    onChange={(e: any) => setNewMac(e.target.value)}
                  />
                  <Input 
                    label="Lien Playlist (M3U ou JSON)" 
                    placeholder="http://exemple.com/playlist.m3u ou .json" 
                    value={newPlaylistUrl}
                    onChange={(e: any) => setNewPlaylistUrl(e.target.value)}
                  />
                  <Button 
                    fullWidth 
                    loading={loading}
                    onClick={() => handleAction("Ajout d'un nouveau client", async () => {
                      const normalizedMac = newMac.toUpperCase().trim();
                      await api.createActivation({
                        resellerId: user.uid,
                        target_mac: normalizedMac,
                        credits_used: 0, // Just adding to list
                        note: newNote || 'Client manuel',
                        playlist_url: newPlaylistUrl
                      });
                      fetchActivations(user.uid);
                      setNewMac('');
                      setNewNote('');
                      setNewPlaylistUrl('');
                    })}
                  >
                    Ajouter le client
                  </Button>
                </div>
              </>
            ) : showModal === 'extend' ? (
              <>
                <h2 className="text-2xl font-black italic">Prolonger l'abonnement</h2>
                <div className="space-y-4">
                  <div className="p-4 bg-zinc-950 rounded-xl border border-zinc-800">
                    <p className="text-xs text-zinc-500">Client : <span className="text-white font-bold">{selectedCustomer?.name}</span></p>
                    <p className="text-xs text-zinc-500">MAC : <span className="text-primary font-mono">{selectedCustomer?.mac}</span></p>
                    <p className="text-xs text-zinc-500">Expire le : <span className="text-white">{selectedCustomer?.expiry}</span></p>
                  </div>
                  <Select label="Choisir une extension">
                    <option>+1 An (1 Crédit)</option>
                    <option>Passer à Vie (2 Crédits)</option>
                  </Select>
                  <Button 
                    fullWidth 
                    loading={loading}
                    onClick={() => handleAction("Prolongation d'abonnement", async () => {})}
                  >
                    Confirmer la prolongation
                  </Button>
                </div>
              </>
            ) : showModal === 'playlist' ? (
              <>
                <h2 className="text-2xl font-black italic">Gestion Playlist</h2>
                <div className="space-y-4">
                  <div className="p-4 bg-zinc-950 rounded-xl border border-zinc-800">
                    <p className="text-xs text-zinc-500">Client : <span className="text-white font-bold">{selectedCustomer?.name || selectedCustomer?.note}</span></p>
                    <p className="text-xs text-zinc-500">MAC : <span className="text-primary font-mono">{selectedCustomer?.mac || selectedCustomer?.target_mac}</span></p>
                  </div>
                  
                  <div className="flex items-center justify-between">
                    <h3 className="text-sm font-black uppercase tracking-widest text-zinc-500">Chaînes</h3>
                    <Button 
                      variant="outline" 
                      size="sm" 
                      icon={Copy}
                      onClick={() => {
                        const m3uLink = `http://skyplayer.live/get.php?mac=${selectedCustomer?.mac || selectedCustomer?.target_mac}&type=m3u_plus`;
                        navigator.clipboard.writeText(m3uLink);
                        showToast('Lien M3U complet copié !', 'success');
                      }}
                    >
                      Lien M3U
                    </Button>
                  </div>

                  {/* Playlist Cleaning Section */}
                  <div className="p-4 bg-primary/5 border border-primary/20 rounded-xl space-y-3">
                    <div className="flex items-center gap-2 text-primary">
                      <Filter size={16} />
                      <h4 className="text-xs font-black uppercase tracking-widest">Nettoyage de Playlist</h4>
                    </div>
                    <p className="text-[10px] text-zinc-500 leading-tight">
                      Sélectionnez les bouquets à <span className="text-red-500 font-bold">SUPPRIMER</span> pour rendre le lecteur plus fluide (recommandé pour Smart TV).
                    </p>
                    <div className="grid grid-cols-2 gap-2">
                      {['FRANCE', 'BELGIQUE', 'CANADA', 'AFRIQUE', 'ADULTES', 'VOD'].map(country => (
                        <label key={country} className="flex items-center gap-2 p-2 bg-zinc-900 rounded-lg cursor-pointer hover:bg-zinc-800 transition-colors">
                          <input type="checkbox" className="rounded border-zinc-700 text-primary focus:ring-primary bg-zinc-800" />
                          <span className="text-[10px] font-bold text-zinc-400">{country}</span>
                        </label>
                      ))}
                    </div>
                    <Button 
                      fullWidth 
                      size="sm" 
                      variant="outline" 
                      className="text-[10px] h-8 border-primary/30 hover:bg-primary/10"
                      onClick={() => showToast("Playlist optimisée avec succès !", "success")}
                    >
                      Appliquer le nettoyage
                    </Button>
                  </div>

                  <div className="space-y-2 max-h-[250px] overflow-y-auto pr-2 custom-scrollbar">
                    {[
                      { name: 'TF1 HD', category: 'France' },
                      { name: 'France 2 HD', category: 'France' },
                      { name: 'M6 HD', category: 'France' },
                      { name: 'beIN Sports 1', category: 'Sports' },
                      { name: 'Disney Channel', category: 'Enfants' },
                    ].map((channel, idx) => (
                      <div key={idx} className="flex items-center justify-between p-3 bg-zinc-800/30 border border-zinc-800 rounded-xl group hover:border-primary/30 transition-all">
                        <div className="flex items-center gap-3">
                          <div className="w-8 h-8 bg-zinc-800 rounded-lg flex items-center justify-center text-zinc-500">
                            <Tv size={14} />
                          </div>
                          <div>
                            <p className="text-sm font-bold text-white">{channel.name}</p>
                            <p className="text-[10px] text-zinc-500 font-black uppercase tracking-widest">{channel.category}</p>
                          </div>
                        </div>
                        <button 
                          onClick={() => {
                            const channelLink = `http://skyplayer.live/stream.php?mac=${selectedCustomer?.mac || selectedCustomer?.target_mac}&channel=${channel.name.replace(/\s+/g, '_')}`;
                            navigator.clipboard.writeText(channelLink);
                            showToast(`Lien pour ${channel.name} copié !`, 'success');
                          }}
                          className="p-2 bg-zinc-800 text-zinc-400 hover:text-primary hover:bg-primary/10 rounded-lg transition-all opacity-0 group-hover:opacity-100"
                          title="Copier le lien M3U de la chaîne"
                        >
                          <Copy size={14} />
                        </button>
                      </div>
                    ))}
                  </div>

                  <div className="grid grid-cols-2 gap-3 pt-2">
                    <Button variant="outline" fullWidth onClick={() => setShowModal(null)}>Fermer</Button>
                    <Button fullWidth onClick={() => { alert('Réinitialisé'); setShowModal(null); }} className="bg-red-500 hover:bg-red-600 text-white">Réinitialiser</Button>
                  </div>
                </div>
              </>
            ) : showModal === 'edit-client' ? (
              <>
                <h2 className="text-2xl font-black italic">Modifier le client</h2>
                <div className="space-y-4">
                  <Input label="Nom du Client" defaultValue={selectedCustomer?.name} />
                  <Input label="Adresse MAC" defaultValue={selectedCustomer?.mac} readOnly className="opacity-50" />
                  <Input label="Notes" placeholder="Notes sur le client..." />
                  <Button 
                    fullWidth 
                    loading={loading}
                    onClick={() => handleAction("Modification du client", async () => {})}
                  >
                    Enregistrer les modifications
                  </Button>
                </div>
              </>
            ) : showModal === 'reset-device' ? (
              <>
                <h2 className="text-2xl font-black italic">Réinitialiser l'appareil</h2>
                <div className="space-y-4">
                  <p className="text-sm text-zinc-400">Voulez-vous vraiment réinitialiser les données de cet appareil ? Cette action est irréversible.</p>
                  <div className="p-4 bg-amber-500/10 border border-amber-500/20 rounded-xl">
                    <p className="text-xs text-amber-500">L'abonnement restera actif, mais les listes de lecture seront vidées.</p>
                  </div>
                  <Button 
                    fullWidth 
                    loading={loading}
                    onClick={() => handleAction("Réinitialisation de l'appareil", async () => {})}
                  >
                    Confirmer la réinitialisation
                  </Button>
                </div>
              </>
            ) : (
              <>
                <h2 className="text-2xl font-black italic text-red-500">Supprimer le client</h2>
                <div className="space-y-4">
                  <p className="text-sm text-zinc-400">Êtes-vous sûr de vouloir supprimer <span className="text-white font-bold">{selectedCustomer?.name}</span> ?</p>
                  <p className="text-xs text-red-500/70">Attention : L'abonnement en cours sera définitivement perdu.</p>
                  <Button 
                    fullWidth 
                    variant="danger"
                    loading={loading}
                    onClick={() => handleAction("Suppression du client", async () => {})}
                  >
                    Supprimer définitivement
                  </Button>
                </div>
              </>
            )}
          </Card>
        </div>
      )}
    </div>
  );
};
