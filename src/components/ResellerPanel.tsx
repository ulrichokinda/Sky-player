import React, { useState, useEffect, useMemo } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Logo } from './Logo';
import { GoogleGenAI } from "@google/genai";
import { CreditCard, Users, History, PlusCircle, ShieldCheck, TrendingUp, ShoppingBag, User, Store, Search, LayoutGrid, Globe, Server, Download, Copy, ExternalLink, Zap, ArrowLeft, Clock, AlertCircle, X, ChevronRight, UserCheck, Lock, RotateCcw, UserMinus, List, Trash2, Edit, Settings, Smartphone, Camera, Sparkles, CheckCircle2, Filter, MoreVertical, DownloadCloud } from 'lucide-react';
import { auth } from '../firebase';
import { Focusable } from './TVFocusManager';
import { api, UserProfile, Activation as ApiActivation, Payment as ApiPayment, isTrialExpired } from '../services/api';
import { PAYMENT_METHODS } from '../constants';
import { cn, Card, Badge, Button, Input, Select, Textarea, Toast } from './ui';

interface Activation {
  id: number;
  target_mac: string;
  credits_used: number;
  created_at: string;
  note?: string;
  system?: string;
  version?: string;
  last_connection?: string;
  country_code?: string;
}

interface Purchase {
  id: number;
  amount: number;
  credits_purchased: number;
  created_at: string;
  status?: 'pending' | 'completed' | 'failed';
}

interface ResellerPanelProps {
  activeTab: 'overview' | 'clients' | 'credits' | 'account' | 'check_mac' | 'store_info' | 'download_apk' | 'api_docs' | 'support';
  setActiveTab: (tab: string) => void;
}

export const ResellerPanel: React.FC<ResellerPanelProps> = ({ activeTab, setActiveTab }) => {
  const user = JSON.parse(localStorage.getItem('user') || '{}');
  const userCountry = user.country || 'Côte d\'Ivoire';

  if (!user.isPremium && isTrialExpired(user.trialStartedAt)) {
    return (
      <div className="flex-1 flex items-center justify-center p-8">
        <Card className="p-8 text-center space-y-4 max-w-md">
          <h2 className="text-2xl font-black text-white">Essai gratuit terminé</h2>
          <p className="text-zinc-500">Votre période d'essai de 3 semaines est terminée. Veuillez passer à un abonnement premium pour continuer à utiliser l'application.</p>
          <Button onClick={() => setActiveTab('credits')}>Passer à Premium</Button>
        </Card>
      </div>
    );
  }

  const getCurrencyInfo = (country: string) => {
    const map: Record<string, { symbol: string; code: string; rate: number }> = {
      'France': { symbol: '€', code: 'EUR', rate: 1 / 655.957 },
      'Belgique': { symbol: '€', code: 'EUR', rate: 1 / 655.957 },
      'Luxembourg': { symbol: '€', code: 'EUR', rate: 1 / 655.957 },
      'Monaco': { symbol: '€', code: 'EUR', rate: 1 / 655.957 },
      'Suisse': { symbol: 'CHF', code: 'CHF', rate: 1 / 700 },
      'Canada (Québec)': { symbol: 'CAD', code: 'CAD', rate: 1 / 450 },
      'RDC': { symbol: 'CDF', code: 'CDF', rate: 4.5 },
      'Guinée': { symbol: 'GNF', code: 'GNF', rate: 14.5 },
      'Madagascar': { symbol: 'MGA', code: 'MGA', rate: 7.5 },
      'Rwanda': { symbol: 'RWF', code: 'RWF', rate: 2.1 },
      'Burundi': { symbol: 'BIF', code: 'BIF', rate: 4.8 },
      'Djibouti': { symbol: 'DJF', code: 'DJF', rate: 0.27 },
      'Seychelles': { symbol: 'SCR', code: 'SCR', rate: 0.02 },
      'Comores': { symbol: 'KMF', code: 'KMF', rate: 0.75 },
      'Haïti': { symbol: 'HTG', code: 'HTG', rate: 0.2 },
      'Vanuatu': { symbol: 'VUV', code: 'VUV', rate: 0.18 }
    };
    // Default to FCFA (F) for most African francophone countries
    return map[country] || { symbol: 'F', code: 'XOF', rate: 1 };
  };

  const currencyInfo = getCurrencyInfo(userCountry);
  const currency = currencyInfo.symbol;
  const currencyCode = currencyInfo.code;
  const exchangeRate = currencyInfo.rate;

  const getConvertedPrice = (basePrice: number) => {
    return Math.round(basePrice * exchangeRate);
  };

  const getFlagEmoji = (countryCode: string) => {
    if (!countryCode) return '🌐';
    const codePoints = countryCode
      .toUpperCase()
      .split('')
      .map(char => 127397 + char.charCodeAt(0));
    return String.fromCodePoint(...codePoints);
  };

  const [stats, setStats] = useState<{ credits: number; activations: Activation[]; purchases: Purchase[] }>({ 
    credits: 0, 
    activations: [],
    purchases: []
  });
  const [targetMac, setTargetMac] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [notification, setNotification] = useState<{ type: 'success' | 'error' | 'info'; message: string } | null>(null);

  const notify = (msg: string, type: 'success' | 'error' | 'info' = 'info') => {
    setNotification({ message: msg, type });
  };

  const [activeSubTab, setActiveSubTab] = useState<'activations' | 'purchases'>('activations');
  const [activationSearch, setActivationSearch] = useState('');
  const [targetServer, setTargetServer] = useState('');
  const [checkMacInput, setCheckMacInput] = useState('');
  const [checkResult, setCheckResult] = useState<any>(null);
  const [showExportOptions, setShowExportOptions] = useState<number | null>(null);
  const [managedClient, setManagedClient] = useState<Activation | null>(null);
  const [managementType, setManagementType] = useState<'client' | 'playlist' | null>(null);

  const [showActivationModal, setShowActivationModal] = useState(false);
  const [activationStep, setActivationStep] = useState(1);
  const [activationMac, setActivationMac] = useState('');
  const [activationNote, setActivationNote] = useState('');
  const [activationSourceType, setActivationSourceType] = useState<'link' | 'xtream' | 'file'>('link');
  const [activationUrl, setActivationUrl] = useState('');
  const [activationHost, setActivationHost] = useState('');
  const [activationUsername, setActivationUsername] = useState('');
  const [activationPassword, setActivationPassword] = useState('');
  const [activationFile, setActivationFile] = useState<File | null>(null);

  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [paymentSuccess, setPaymentSuccess] = useState(false);
  const [selectedPack, setSelectedPack] = useState<{ qty: number; price: number } | null>(null);
  const [paymentMethod, setPaymentMethod] = useState<'card' | 'momo' | 'direct_momo' | 'moneyfusion'>('direct_momo');
  const [phoneNumber, setPhoneNumber] = useState(user.phone || '');
  const [momoProvider, setMomoProvider] = useState<'orange' | 'mtn' | 'moov' | 'wave' | 'airtel' | 'mtn_cg' | 'airtel_cg'>('orange');
  const [aiValidationMode, setAiValidationMode] = useState(false);
  const [aiImage, setAiImage] = useState<string | null>(null);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiResult, setAiResult] = useState<{
    success: boolean;
    transactionId?: string;
    amount?: number;
    message: string;
  } | null>(null);

  const [profileUsername, setProfileUsername] = useState(user.username || 'Ulrich Okinda');
  const [profileEmail, setProfileEmail] = useState(user.email || 'inestaulrichokinda@gmail.com');
  const [profilePhone, setProfilePhone] = useState(user.phone || '');
  const [profileCountry, setProfileCountry] = useState(user.country || '');
  const [profileCurrentPassword, setProfileCurrentPassword] = useState('');
  const [profileNewPassword, setProfileNewPassword] = useState('');
  const [profileConfirmPassword, setProfileConfirmPassword] = useState('');
  const [profileUpdateLoading, setProfileUpdateLoading] = useState(false);
  const [profileUpdateMessage, setProfileUpdateMessage] = useState({ type: '', text: '' });
  const [isEditingProfile, setIsEditingProfile] = useState(false);
  const [is2FAEnabled, setIs2FAEnabled] = useState(true);

  const [massOldHost, setMassOldHost] = useState('');
  const [massNewHost, setMassNewHost] = useState('');

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    if (profileNewPassword && profileNewPassword !== profileConfirmPassword) {
      setProfileUpdateMessage({ type: 'error', text: 'Les mots de passe ne correspondent pas' });
      return;
    }

    setProfileUpdateLoading(true);
    setProfileUpdateMessage({ type: '', text: '' });

    try {
      if (!auth.currentUser) return;
      const updatedUser = await api.updateUser(auth.currentUser.uid, {
        username: profileUsername,
        email: profileEmail,
        phone: profilePhone,
        country: profileCountry
      });

      localStorage.setItem('user', JSON.stringify({ ...user, ...updatedUser }));
      notify('Profil mis à jour avec succès', 'success');
      setProfileNewPassword('');
      setProfileConfirmPassword('');
      // Force refresh local user data
      setTimeout(() => window.location.reload(), 1500); 
    } catch (error: any) {
      notify('Erreur: ' + error.message, 'error');
    } finally {
      setProfileUpdateLoading(false);
    }
  };

  useEffect(() => {
    if (!auth.currentUser) return;

    const resellerId = auth.currentUser.uid;

    const fetchData = async () => {
      try {
        const userData = await api.getUser(resellerId);
        const activations = await api.getActivations(resellerId);
        const payments = await api.getPayments(resellerId);
        
        setStats({
          credits: userData.credits || 0,
          activations: activations as any,
          purchases: payments as any
        });
      } catch (error) {
        console.error("Error fetching data from local API:", error);
      }
    };

    fetchData();
    const interval = setInterval(fetchData, 5000); // Polling every 5 seconds

    return () => clearInterval(interval);
  }, []);

  const handleActivate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!targetMac || !auth.currentUser) return;
    setLoading(true);
    try {
      const resellerId = auth.currentUser.uid;
      const id = Math.random().toString(36).substr(2, 9);
      
      await api.createActivation({
        id,
        resellerId,
        target_mac: targetMac,
        credits_used: 1,
        note: ''
      });

      setTargetMac('');
      setTargetServer('');
      notify('Activation réussie !', 'success');
      
      // Refresh stats
      const userData = await api.getUser(resellerId);
      const activations = await api.getActivations(resellerId);
      setStats(prev => ({ ...prev, credits: userData.credits, activations: activations as any }));
    } catch (error: any) {
      notify("Erreur d'activation: " + error.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleFullActivation = async () => {
    if (!activationMac || !auth.currentUser) return;
    setLoading(true);
    try {
      const resellerId = auth.currentUser.uid;
      await api.createActivation({
        resellerId,
        target_mac: activationMac,
        credits_used: 1,
        note: activationNote,
        system: 'Android TV',
        version: '4.2.0'
      });

      setActivationMac('');
      setActivationNote('');
      setShowActivationModal(false);
      notify('Activation réussie !', 'success');
      
      // Refresh stats
      const userData = await api.getUser(resellerId);
      const activations = await api.getActivations(resellerId);
      setStats(prev => ({ ...prev, credits: userData.credits, activations: activations as any }));
    } catch (error: any) {
      notify("Erreur d'activation: " + error.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleConfirmPayment = async () => {
    if (!selectedPack || !auth.currentUser) return;
    setLoading(true);
    try {
      const userId = auth.currentUser.uid;
      const id = Math.random().toString(36).substr(2, 9);
      const externalId = (paymentMethod === 'direct_momo' ? "DIR-" : paymentMethod === 'moneyfusion' ? "MF-" : "FLW-") + Math.random().toString(36).substr(2, 9);
      
      if (paymentMethod === 'moneyfusion') {
        await api.initiateMoneyFusion({
          userId,
          amount: selectedPack.price,
          phoneNumber,
          credits_purchased: selectedPack.qty
        });
      } else if (paymentMethod === 'yabetoopay') {
        await api.initiateYabetooPay({
          userId,
          amount: selectedPack.price,
          phoneNumber,
          credits_purchased: selectedPack.qty,
          methodId: momoProvider
        });
      } else {
        await api.createPayment({
          id,
          userId,
          amount: selectedPack.price,
          credits_purchased: selectedPack.qty,
          payment_method: paymentMethod,
          provider: momoProvider,
          status: 'pending',
          external_id: externalId
        });
      }

      setPaymentSuccess(true);
      setTimeout(() => {
        setShowPaymentModal(false);
        setPaymentSuccess(false);
      }, 3000);
    } catch (error: any) {
      alert("Erreur de paiement: " + error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleCheckMac = async () => {
    if (!checkMacInput) return;
    setLoading(true);
    try {
      // Mock check for now, could be a Firestore query or cloud function
      setCheckResult({
        mac: checkMacInput,
        status: 'available',
        last_server: 'S1',
        expiry: 'N/A'
      });
    } finally {
      setLoading(false);
    }
  };

  const handleBuyCredits = (pack: { qty: number; price: number }) => {
    setSelectedPack(pack);
    setShowPaymentModal(true);
  };

  const handleAIVerification = async () => {
    if (!aiImage || !selectedPack || !auth.currentUser) return;
    setAiLoading(true);
    setAiResult(null);

    try {
      const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });
      const base64Data = aiImage.split(',')[1];
      
      const prompt = `
        ANALYSE DE PAIEMENT MOBILE MONEY (CONGO-BRAZZAVILLE)
        
        Tu es un expert en vérification de transactions MTN Mobile Money et Airtel Money.
        Analyse cette image (reçu ou SMS) et extrais les informations suivantes au format JSON strict:
        
        {
          "transactionId": "ID de la transaction",
          "amount": 15000,
          "status": "SUCCESS" ou "FAILED",
          "provider": "MTN" ou "AIRTEL",
          "confidence": 0.95
        }
        
        IMPORTANT:
        1. Si c'est un transfert vers "Julda Ulrich Okinda", c'est valide.
        2. Le montant attendu est d'environ ${selectedPack.price} ${currency}.
        3. Ne réponds QUE par le JSON.
      `;

      const response = await ai.models.generateContent({
        model: "gemini-3-flash-preview",
        contents: [{
          parts: [
            { text: prompt },
            { inlineData: { data: base64Data, mimeType: "image/jpeg" } }
          ]
        }],
        config: {
          responseMimeType: "application/json"
        }
      });

      let data;
      try {
        data = JSON.parse(response.text);
      } catch (e) {
        throw new Error("Impossible de lire la réponse de l'IA. Format JSON invalide.");
      }
      
      if (data.status === 'SUCCESS' && data.confidence > 0.7) {
        const expectedPrice = selectedPack.price;
        const receivedAmount = data.amount;
        
        // Allow 5% tolerance
        const isAmountValid = Math.abs(receivedAmount - expectedPrice) / expectedPrice < 0.05;

        if (isAmountValid) {
          setAiResult({
            success: true,
            transactionId: data.transactionId,
            amount: receivedAmount,
            message: "Validation IA réussie ! Transaction confirmée instantanément."
          });
          notify("Paiement validé par l'IA !", "success");
          
          // Auto-confirm payment
          const userId = auth.currentUser.uid;
          const id = Math.random().toString(36).substr(2, 9);
          
          await api.createPayment({
            id,
            userId,
            amount: receivedAmount,
            credits_purchased: selectedPack.qty,
            payment_method: 'direct_momo_ai',
            provider: data.provider,
            status: 'completed',
            external_id: data.transactionId
          });

          setTimeout(async () => {
            const userData = await api.getUser(userId);
            setStats(prev => ({ ...prev, credits: userData.credits }));
            setPaymentSuccess(true);
            setTimeout(() => {
              setShowPaymentModal(false);
              setPaymentSuccess(false);
              setAiValidationMode(false);
              setAiImage(null);
              setAiResult(null);
            }, 3000);
          }, 1500);

        } else {
          setAiResult({
            success: false,
            message: `Montant incorrect. Détecté: ${receivedAmount} ${currency}, Attendu: ${expectedPrice} ${currency}.`
          });
        }
      } else {
        setAiResult({
          success: false,
          message: "L'IA n'a pas pu confirmer le paiement. Assurez-vous que l'image est bien lisible."
        });
      }
    } catch (error: any) {
      console.error("AI Error:", error);
      setAiResult({
        success: false,
        message: "Erreur lors de l'analyse IA: " + error.message
      });
      notify("Erreur d'analyse IA", "error");
    } finally {
      setAiLoading(false);
    }
  };

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setAiImage(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  return (
    <div className="flex flex-col min-h-full bg-black text-white font-sans selection:bg-primary/30 min-w-0">
      {/* Main Content Area */}
      <div className="flex-1 p-3 lg:p-6 bg-[radial-gradient(circle_at_top_right,_var(--tw-gradient-stops))] from-primary/5 via-transparent to-transparent min-w-0">
        <AnimatePresence mode="wait">
          {activeTab === 'overview' && (
            <motion.div 
              key="overview"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="space-y-6 lg:space-y-10 pb-16"
            >
              <header className="space-y-2">
                <Badge variant="primary">
                  <LayoutGrid size={10} />
                  Tableau de Bord
                </Badge>
                <h1 className="text-3xl lg:text-5xl font-black tracking-tighter leading-none">Bienvenue, <span className="text-primary">{user.username}</span></h1>
                <p className="text-zinc-500 text-sm lg:text-base font-medium max-w-xl leading-relaxed">Gérez vos activations et suivez vos performances en toute simplicité.</p>
              </header>

              {/* Bento Grid Stats */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3 lg:gap-4">
                <Card className="md:col-span-2 p-6 lg:p-8 flex flex-col justify-between h-48 lg:h-56 group hover:border-primary/30 relative overflow-hidden">
                  <div className="absolute -right-10 -top-10 w-32 h-32 bg-primary/10 rounded-full blur-3xl group-hover:scale-150 transition-all duration-700" />
                  <div className="flex justify-between items-start relative z-10">
                    <div className="p-3 lg:p-4 bg-primary/10 rounded-xl text-primary border border-primary/20 group-hover:scale-110 transition-transform duration-500">
                      <TrendingUp size={24} />
                    </div>
                    <span className="text-[11px] font-black uppercase tracking-[0.4em] text-zinc-600">Balance Live</span>
                  </div>
                  <div className="relative z-10">
                    <p className="text-4xl lg:text-6xl font-black tracking-tighter text-white mb-1 leading-none">{stats?.credits ?? 0}</p>
                    <p className="text-zinc-500 font-bold uppercase text-[11px] lg:text-xs tracking-[0.2em]">Crédits Premium Disponibles</p>
                  </div>
                </Card>

                <Card className="p-8 md:p-12 flex flex-col justify-between h-64 md:h-72 group hover:border-blue-500/30 relative overflow-hidden">
                  <div className="absolute -right-10 -top-10 w-40 h-40 bg-blue-500/10 rounded-full blur-3xl group-hover:scale-150 transition-all duration-700" />
                  <div className="p-4 md:p-5 bg-blue-500/10 rounded-2xl text-blue-500 w-fit border border-blue-500/20 group-hover:scale-110 transition-transform duration-500 relative z-10">
                    <Users size={28} className="md:w-9 md:h-9" />
                  </div>
                  <div className="relative z-10">
                    <p className="text-5xl md:text-6xl font-black tracking-tighter text-white mb-2 leading-none">{stats?.activations?.length ?? 0}</p>
                    <p className="text-zinc-500 font-bold uppercase text-xs tracking-[0.2em]">Clients Actifs</p>
                  </div>
                </Card>
              </div>

              {/* Quick Activation Section */}
              <div className="relative group">
                <div className="absolute inset-0 bg-primary/20 blur-[100px] opacity-0 group-hover:opacity-100 transition-opacity duration-1000" />
                <div className="relative bg-white text-black rounded-[2rem] md:rounded-[3rem] p-8 md:p-16 flex flex-col md:flex-row items-center gap-8 md:gap-16 shadow-2xl shadow-white/5 overflow-hidden">
                  <div className="absolute -right-20 -bottom-20 w-80 h-80 bg-zinc-100 rounded-full opacity-50" />
                  <div className="flex-1 space-y-4 md:space-y-6 relative z-10 text-center md:text-left">
                    <Badge variant="default" className="bg-black/5 text-black/40 border-black/5">
                      <Zap size={12} className="fill-black/40" />
                      Action Rapide
                    </Badge>
                    <h2 className="text-3xl md:text-5xl font-black tracking-tighter leading-tight">Activation <span className="text-primary">Express</span></h2>
                    <p className="text-zinc-600 font-medium text-base md:text-lg leading-relaxed max-w-md">Activez un nouvel appareil en quelques secondes. Entrez simplement l'adresse MAC.</p>
                  </div>
                  <form onSubmit={handleActivate} className="w-full md:w-auto flex flex-col lg:flex-row gap-4 relative z-10">
                    <div className="flex flex-col sm:flex-row gap-4 flex-1">
                      <Input
                        type="text"
                        value={targetMac}
                        onChange={(e: any) => setTargetMac(e.target.value)}
                        placeholder="MAC (00:1A:...)"
                        className="bg-zinc-100 border-2 border-transparent text-black placeholder:text-zinc-400 focus:border-primary focus:bg-white md:min-w-[250px] py-4"
                        required
                      />
                      <Select
                        value={targetServer}
                        onChange={(e: any) => setTargetServer(e.target.value)}
                        className="bg-zinc-100 border-2 border-transparent text-black focus:border-primary focus:bg-white py-4"
                        required
                      >
                        <option value="">Choisir Serveur</option>
                        <option value="S1">WO SERVER 01</option>
                        <option value="S2">WO SERVER 02 (Backup)</option>
                      </Select>
                    </div>
                    <Button
                      type="submit"
                      loading={loading}
                      size="lg"
                      variant="white"
                      className="bg-black text-white hover:bg-zinc-900 px-12 py-5"
                      icon={Zap}
                    >
                      Activer
                    </Button>
                  </form>
                </div>
              </div>
            </motion.div>
          )}

        {activeTab === 'clients' && (
          <motion.div 
            key="clients"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="space-y-8 pb-20"
          >
            <div className="flex flex-col xl:flex-row xl:items-center justify-between gap-6">
              <div className="space-y-1">
                <Badge variant="info">
                  <Users size={10} />
                  Base de Données
                </Badge>
                <h1 className="text-4xl md:text-5xl font-black tracking-tighter">Gestion <span className="text-blue-500">Clients</span></h1>
                <p className="text-zinc-500 text-sm font-medium">Suivez vos activations et gérez votre parc client.</p>
              </div>
              <div className="flex flex-wrap items-center gap-3">
                <div className="relative group flex-1 md:flex-none">
                  <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-600 group-focus-within:text-blue-500 transition-colors" size={18} />
                  <input 
                    type="text"
                    placeholder="Rechercher..."
                    value={activationSearch}
                    onChange={(e) => setActivationSearch(e.target.value)}
                    className="bg-zinc-900/50 border border-zinc-800 rounded-2xl pl-12 pr-6 py-3 text-sm font-bold focus:border-blue-500 outline-none transition-all w-full md:w-64 backdrop-blur-sm"
                  />
                </div>
                <Button variant="secondary" size="md" icon={Filter}>Filtres</Button>
                <Button 
                  variant="primary" 
                  size="md" 
                  icon={PlusCircle}
                  onClick={() => {
                    setActivationStep(1);
                    setShowActivationModal(true);
                  }}
                  className="bg-blue-500 text-black shadow-blue-500/20"
                >
                  Nouveau
                </Button>
              </div>
            </div>

            <Card className="p-0 overflow-hidden border-zinc-800/50">
              <div className="overflow-x-auto custom-scrollbar">
                <table className="w-full text-left border-collapse min-w-[1000px]">
                  <thead>
                    <tr className="bg-zinc-950/50 text-zinc-500 text-[10px] uppercase tracking-[0.2em] font-black border-b border-zinc-800/50">
                      <th className="px-8 py-5">Appareil (MAC)</th>
                      <th className="px-8 py-5">Note / Client</th>
                      <th className="px-8 py-5">Système</th>
                      <th className="px-8 py-5">Statut</th>
                      <th className="px-8 py-5">Expiration</th>
                      <th className="px-8 py-5 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-800/30">
                    {(stats?.activations || []).length > 0 ? (
                      (stats?.activations || [])
                        .filter(a => {
                          const search = activationSearch.toLowerCase();
                          return (
                            a.target_mac.toLowerCase().includes(search) ||
                            (a.note && a.note.toLowerCase().includes(search)) ||
                            (a.system && a.system.toLowerCase().includes(search))
                          );
                        })
                        .map((act) => {
                          const expiryDate = new Date(act.created_at);
                          expiryDate.setFullYear(expiryDate.getFullYear() + 1);
                          const isExpired = expiryDate < new Date();

                          return (
                            <motion.tr 
                              layout
                              initial={{ opacity: 0 }}
                              animate={{ opacity: 1 }}
                              key={act.id} 
                              className="group hover:bg-white/[0.02] transition-colors"
                            >
                              <td className="px-8 py-6">
                                <div className="flex items-center gap-3">
                                  <div className="relative">
                                    <div className={cn(
                                      "w-2.5 h-2.5 rounded-full",
                                      isExpired ? "bg-red-500 shadow-[0_0_10px_rgba(239,68,68,0.4)]" : "bg-emerald-500 shadow-[0_0_10px_rgba(16,185,129,0.4)]"
                                    )} />
                                  </div>
                                  <div className="flex flex-col">
                                    <span className="font-mono text-sm font-black text-white tracking-wider">{act.target_mac}</span>
                                    <span className="text-[10px] text-zinc-600 font-bold uppercase tracking-widest">
                                      {getFlagEmoji(act.country_code || 'FR')} {act.country_code || 'France'}
                                    </span>
                                  </div>
                                </div>
                              </td>
                              <td className="px-8 py-6">
                                <span className="text-xs text-zinc-400 font-bold">{act.note || '—'}</span>
                              </td>
                              <td className="px-8 py-6">
                                <div className="flex flex-col gap-1">
                                  <span className="text-[10px] font-black text-zinc-300 uppercase tracking-widest">{act.system || 'Android TV'}</span>
                                  <span className="text-[9px] font-mono text-zinc-600 uppercase">v{act.version || '4.2.0'}</span>
                                </div>
                              </td>
                              <td className="px-8 py-6">
                                <Badge variant={isExpired ? 'error' : 'success'}>
                                  {isExpired ? 'Expiré' : 'Actif'}
                                </Badge>
                              </td>
                              <td className="px-8 py-6">
                                <div className="flex flex-col">
                                  <span className={cn("text-xs font-black tracking-tight", isExpired ? "text-red-500" : "text-zinc-300")}>
                                    {expiryDate.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' })}
                                  </span>
                                  <span className="text-[9px] text-zinc-600 font-black uppercase tracking-widest">Abonnement Annuel</span>
                                </div>
                              </td>
                              <td className="px-8 py-6">
                                <div className="flex justify-end gap-2">
                                  <Button 
                                    variant="secondary" 
                                    size="sm" 
                                    icon={ShieldCheck}
                                    onClick={() => {
                                      setManagedClient(act);
                                      setManagementType('client');
                                    }}
                                  >
                                    Gérer
                                  </Button>
                                  <Button 
                                    variant="outline" 
                                    size="sm" 
                                    icon={List}
                                    onClick={() => {
                                      setManagedClient(act);
                                      setManagementType('playlist');
                                    }}
                                  >
                                    Playlist
                                  </Button>
                                </div>
                              </td>
                            </motion.tr>
                          );
                        })
                    ) : (
                      <tr>
                        <td colSpan={6} className="px-8 py-24 text-center">
                          <div className="flex flex-col items-center gap-4 opacity-20">
                            <Users size={48} />
                            <p className="text-xs font-black uppercase tracking-[0.2em]">Aucun client trouvé</p>
                          </div>
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </Card>

            {/* Quick Check Tool */}
            <Card variant="glass" className="p-8 border-blue-500/10">
              <div className="flex flex-col lg:flex-row items-center gap-8">
                <div className="p-5 bg-blue-500/10 rounded-2xl text-blue-500 border border-blue-500/20">
                  <Search size={32} />
                </div>
                <div className="flex-1 space-y-1 text-center lg:text-left">
                  <h3 className="text-2xl font-black tracking-tight">Vérificateur de MAC</h3>
                  <p className="text-zinc-500 text-sm font-medium">Vérifiez le statut d'une adresse MAC avant activation.</p>
                </div>
                <div className="flex flex-col sm:flex-row gap-3 w-full lg:w-auto">
                  <Input 
                    placeholder="MAC (ex: 00:1A:...)"
                    value={checkMacInput}
                    onChange={(e: any) => setCheckMacInput(e.target.value.toUpperCase())}
                    className="lg:w-64"
                  />
                  <Button variant="white" size="md" onClick={handleCheckMac} loading={loading}>Vérifier</Button>
                </div>
              </div>
            </Card>
          </motion.div>
        )}

        {activeTab === 'credits' && (
          <motion.div 
            key="credits"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="max-w-6xl space-y-16 pb-20"
          >
            <header className="flex flex-col md:flex-row md:items-end justify-between gap-6">
              <div className="space-y-4">
                <Badge variant="primary">
                  <ShoppingBag size={12} />
                  Boutique Officielle
                </Badge>
                <h1 className="text-5xl md:text-6xl font-black tracking-tighter leading-none">Boutique & <span className="text-primary">Crédits</span></h1>
                <p className="text-zinc-500 text-lg max-w-xl font-medium">Rechargez votre stock de crédits instantanément via nos méthodes de paiement sécurisées.</p>
              </div>
              <div className="flex items-center gap-4 bg-zinc-900/50 border border-zinc-800 p-4 rounded-3xl backdrop-blur-sm">
                <div className="p-3 bg-primary/10 rounded-2xl text-primary">
                  <CreditCard size={24} />
                </div>
                <div>
                  <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Solde Actuel</p>
                  <p className="text-2xl font-black text-white">{stats?.credits ?? 0} <span className="text-xs text-zinc-500">Crédits</span></p>
                </div>
              </div>
            </header>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
              {[
                { qty: 10, basePrice: 15000, popular: false, desc: '1 crédit = 1 an de validité' },
                { qty: 20, basePrice: 25750, popular: false, desc: '1 crédit = 1 an de validité' },
                { qty: 50, basePrice: 45000, popular: true, desc: '1 crédit = 1 an de validité' },
              ].map((pack) => {
                const convertedPrice = getConvertedPrice(pack.basePrice);
                const pricePerCredit = (convertedPrice / pack.qty).toFixed(0);
                return (
                  <Card 
                    key={pack.qty}
                    className={cn(
                      "group p-8 flex flex-col justify-between h-[26rem] overflow-hidden transition-all duration-700",
                      pack.popular 
                        ? 'bg-primary border-primary/80 text-black scale-105 shadow-[0_20px_50px_rgba(242,125,38,0.2)] z-10' 
                        : 'hover:border-primary/30'
                    )}
                  >
                    <div className={cn(
                      "absolute -right-10 -top-10 w-40 h-40 rounded-full blur-3xl opacity-20 transition-all duration-700 group-hover:scale-150",
                      pack.popular ? 'bg-black' : 'bg-primary'
                    )} />
                    
                    {pack.popular && (
                      <div className="absolute top-6 right-6">
                        <Badge variant="default" className="bg-black text-white border-black/10">
                          <Zap size={10} className="fill-primary text-primary" />
                          Best Seller
                        </Badge>
                      </div>
                    )}

                    <div className="relative space-y-4">
                      <div className={cn(
                        "w-14 h-14 rounded-2xl flex items-center justify-center border transition-transform duration-500 group-hover:scale-110",
                        pack.popular ? 'bg-black/10 border-black/10 text-black' : 'bg-primary/10 border-primary/20 text-primary'
                      )}>
                        <ShoppingBag size={28} />
                      </div>
                      <div>
                        <div className="flex items-baseline gap-2">
                          <p className="text-6xl font-black tracking-tighter leading-none">{pack.qty}</p>
                          <div className="flex flex-col">
                            <p className={cn("text-[10px] font-black uppercase tracking-widest", pack.popular ? 'text-black/60' : 'text-zinc-500')}>Crédits</p>
                            <p className={cn("text-[10px] font-bold", pack.popular ? 'text-black/40' : 'text-zinc-600')}>{pricePerCredit} {currency}/u</p>
                          </div>
                        </div>
                        <p className={cn("mt-4 text-xs font-medium leading-relaxed max-w-[80%]", pack.popular ? 'text-black/70' : 'text-zinc-400')}>{pack.desc}</p>
                      </div>
                    </div>

                    <div className="relative space-y-6">
                      <div className="flex flex-col">
                        <span className={cn("text-[9px] font-black uppercase tracking-[0.2em] mb-1", pack.popular ? 'text-black/50' : 'text-zinc-600')}>Investissement Total</span>
                        <p className="text-3xl font-black tracking-tight">{convertedPrice.toLocaleString()} <span className="text-lg opacity-60">{currency}</span></p>
                      </div>
                      <Button 
                        variant={pack.popular ? 'white' : 'secondary'}
                        fullWidth
                        size="lg"
                        className={cn(pack.popular ? 'bg-black text-white hover:bg-zinc-900' : 'bg-white text-black hover:bg-zinc-200')}
                        onClick={() => handleBuyCredits({ qty: pack.qty, price: convertedPrice })}
                      >
                        Sélectionner ce Pack
                      </Button>
                    </div>
                  </Card>
                );
              })}
            </div>

            {/* Payment Methods Section */}
            <Card className="p-8 md:p-12 space-y-8 border-zinc-800/50">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
                <div className="space-y-2">
                  <h3 className="text-3xl font-black tracking-tighter">Réception des Paiements</h3>
                  <p className="text-zinc-500 text-sm font-medium">Veuillez effectuer votre transfert sur l'un des numéros ci-dessous.</p>
                </div>
                <div className="px-6 py-3 bg-primary/10 border border-primary/20 rounded-2xl">
                  <p className="text-primary text-xs font-black uppercase tracking-widest">Nom de confirmation</p>
                  <p className="text-white font-black text-lg">Julda Ulrich Okinda</p>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="p-8 bg-zinc-950 border border-zinc-800 rounded-3xl flex items-center justify-between group hover:border-yellow-500/30 transition-all">
                  <div className="flex items-center gap-6">
                    <div className="w-16 h-16 bg-yellow-500/10 rounded-2xl flex items-center justify-center text-yellow-500 font-black text-xl">MTN</div>
                    <div>
                      <p className="text-[10px] font-black text-zinc-500 uppercase tracking-widest mb-1">MTN Congo</p>
                      <p className="text-2xl font-black text-white tracking-widest">06 583 82 96</p>
                    </div>
                  </div>
                  <button 
                    onClick={() => {
                      navigator.clipboard.writeText('065838296');
                    }}
                    className="p-3 bg-zinc-900 rounded-xl text-zinc-500 hover:text-yellow-500 transition-colors"
                  >
                    <Copy size={20} />
                  </button>
                </div>

                <div className="p-8 bg-zinc-950 border border-zinc-800 rounded-3xl flex items-center justify-between group hover:border-red-500/30 transition-all">
                  <div className="flex items-center gap-6">
                    <div className="w-16 h-16 bg-red-500/10 rounded-2xl flex items-center justify-center text-red-500 font-black text-xl">AIR</div>
                    <div>
                      <p className="text-[10px] font-black text-zinc-500 uppercase tracking-widest mb-1">Airtel Money</p>
                      <p className="text-2xl font-black text-white tracking-widest">05 540 40 76</p>
                    </div>
                  </div>
                  <button 
                    onClick={() => {
                      navigator.clipboard.writeText('055404076');
                    }}
                    className="p-3 bg-zinc-900 rounded-xl text-zinc-500 hover:text-red-500 transition-colors"
                  >
                    <Copy size={20} />
                  </button>
                </div>
              </div>
              
              <div className="p-6 bg-blue-500/5 border border-blue-500/10 rounded-2xl flex items-center gap-4">
                <div className="p-2 bg-blue-500/20 rounded-lg text-blue-500">
                  <Zap size={16} />
                </div>
                <p className="text-xs text-zinc-400 leading-relaxed">
                  Après votre transfert, veuillez envoyer une capture d'écran de la transaction via le support WhatsApp pour une validation immédiate de vos crédits.
                </p>
              </div>
            </Card>

            {/* History Table Section */}
            <div className="space-y-8 pt-8">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className="p-3 bg-zinc-900 rounded-2xl text-zinc-400">
                    <History size={24} />
                  </div>
                  <div>
                    <h3 className="text-3xl font-black tracking-tighter">Historique des Achats</h3>
                    <p className="text-zinc-500 text-sm font-medium">Consultez vos factures et l'état de vos commandes.</p>
                  </div>
                </div>
                <button className="text-xs font-black uppercase tracking-widest text-primary hover:text-primary/80 transition-colors">Voir tout</button>
              </div>

              <Card className="p-0 overflow-hidden border-zinc-800/50">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="text-zinc-600 text-[10px] uppercase tracking-[0.3em] font-black border-b border-zinc-800/50 bg-zinc-950/50">
                      <th className="px-10 py-8">Pack de Crédits</th>
                      <th className="px-10 py-8">Montant Payé</th>
                      <th className="px-10 py-8">Date de Transaction</th>
                      <th className="px-10 py-8 text-right">État</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-800/30">
                    {(stats?.purchases || []).length > 0 ? (
                      (stats?.purchases || []).map((pur) => (
                        <tr key={pur.id} className="group hover:bg-white/[0.02] transition-all duration-300">
                          <td className="px-10 py-8">
                            <div className="flex items-center gap-4">
                              <div className="w-10 h-10 rounded-xl bg-zinc-800/50 flex items-center justify-center text-zinc-400 group-hover:bg-primary/10 group-hover:text-primary transition-colors">
                                <ShoppingBag size={18} />
                              </div>
                              <span className="text-white font-black text-lg tracking-tight">{pur.credits_purchased} Crédits Premium</span>
                            </div>
                          </td>
                          <td className="px-10 py-8">
                            <span className="font-mono text-xl font-bold text-zinc-300">{pur.amount.toLocaleString()} <span className="text-sm text-zinc-600">{currency}</span></span>
                          </td>
                          <td className="px-10 py-8">
                            <div className="flex flex-col">
                              <span className="text-zinc-400 font-bold">{new Date(pur.created_at).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' })}</span>
                              <span className="text-[10px] text-zinc-600 font-black uppercase tracking-widest">{new Date(pur.created_at).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}</span>
                            </div>
                          </td>
                          <td className="px-10 py-8 text-right">
                            {pur.status === 'pending' ? (
                              <Badge variant="warning">
                                <Clock size={14} />
                                En attente
                              </Badge>
                            ) : pur.status === 'failed' ? (
                              <Badge variant="error">
                                <AlertCircle size={14} />
                                Échec
                              </Badge>
                            ) : (
                              <Badge variant="success">
                                <ShieldCheck size={14} />
                                Confirmé
                              </Badge>
                            )}
                          </td>
                        </tr>
                      ))
                    ) : (
                      <tr>
                        <td colSpan={4} className="px-10 py-20 text-center">
                          <div className="flex flex-col items-center gap-4 opacity-30">
                            <History size={48} />
                            <p className="text-sm font-black uppercase tracking-widest">Aucun achat enregistré</p>
                          </div>
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </Card>
            </div>
          </motion.div>
        )}

        {activeTab === 'account' && (
          <motion.div 
            key="account"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="max-w-5xl space-y-16 pb-20"
          >
            <header className="space-y-4">
              <Badge variant="default" className="bg-zinc-800 border-zinc-700">
                <User size={12} />
                Profil Revendeur
              </Badge>
              <h1 className="text-5xl md:text-7xl font-black tracking-tighter leading-none">Mon <span className="text-zinc-500">Compte</span></h1>
              <p className="text-zinc-500 text-xl font-medium max-w-2xl leading-relaxed">Gérez vos informations personnelles, vos paramètres de sécurité et accédez aux outils avancés.</p>
            </header>

            <div className="grid grid-cols-1 lg:grid-cols-5 gap-4 lg:gap-6">
              <Card className="lg:col-span-3 p-6 lg:p-8 relative overflow-hidden group">
                <div className="absolute -right-20 -top-20 w-64 h-64 bg-primary/5 rounded-full blur-3xl group-hover:scale-110 transition-transform duration-1000" />
                
                <div className="flex flex-col md:flex-row items-center justify-between gap-6 relative z-10 mb-12">
                  <div className="flex flex-col md:flex-row items-center gap-6">
                    <div className="relative">
                      <div className="w-24 h-24 bg-primary/10 rounded-2xl flex items-center justify-center text-primary border border-primary/20 shadow-2xl">
                        <User size={48} />
                      </div>
                      <div className="absolute -bottom-1 -right-1 w-8 h-8 bg-primary rounded-xl flex items-center justify-center text-black border-2 border-zinc-900 shadow-xl">
                        <ShieldCheck size={16} />
                      </div>
                    </div>
                    <div className="text-center md:text-left space-y-1">
                      <h2 className="text-3xl font-black tracking-tighter">{user.username}</h2>
                      <p className="text-zinc-500 font-medium text-base">{user.email}</p>
                      <Badge variant="primary" className="mt-2">
                        <div className="w-1.5 h-1.5 rounded-full bg-primary animate-pulse" />
                        Revendeur Certifié WO
                      </Badge>
                    </div>
                  </div>
                  <Button 
                    variant="secondary" 
                    size="sm"
                    onClick={() => setIsEditingProfile(!isEditingProfile)}
                    icon={isEditingProfile ? X : Edit}
                  >
                    {isEditingProfile ? 'Annuler' : 'Modifier le Profil'}
                  </Button>
                </div>

                {!isEditingProfile ? (
                  <div className="space-y-8 relative z-10">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div className="p-5 bg-zinc-950/50 border border-zinc-800/50 rounded-xl">
                        <p className="text-[9px] text-zinc-600 uppercase font-black tracking-widest mb-1.5">Nom et prénom</p>
                        <p className="text-lg font-bold text-white">{user.username || 'Ulrich Okinda'}</p>
                      </div>
                      <div className="p-5 bg-zinc-950/50 border border-zinc-800/50 rounded-xl">
                        <p className="text-[9px] text-zinc-600 uppercase font-black tracking-widest mb-1.5">E-mail</p>
                        <p className="text-lg font-bold text-white">{user.email || 'inestaulrichokinda@gmail.com'}</p>
                      </div>
                    </div>

                    <div className="pt-8 border-t border-zinc-800/50 space-y-6">
                      <div className="flex items-center justify-between">
                        <h3 className="text-lg font-black text-white uppercase tracking-tight">Two-Factor Authentication</h3>
                        <Badge variant="default">Paramètres</Badge>
                      </div>
                      
                      <div className="flex items-center justify-between p-4 bg-zinc-950/30 border border-zinc-800/50 rounded-xl">
                        <div className="flex items-center gap-3">
                          <div className={`w-2 h-2 rounded-full ${is2FAEnabled ? 'bg-green-500 shadow-[0_0_10px_rgba(34,197,94,0.5)]' : 'bg-zinc-600'}`} />
                          <span className="text-sm font-bold text-white">2FA activé</span>
                        </div>
                        <button className="text-[10px] font-black text-primary uppercase tracking-widest hover:underline">Régénérer codes de secours</button>
                      </div>
                    </div>

                    <div className="pt-8 border-t border-zinc-800/50 space-y-6">
                      <h3 className="text-lg font-black text-white uppercase tracking-tight">Changer le mot de passe</h3>
                      <div className="p-4 bg-amber-500/5 border border-amber-500/20 rounded-xl">
                        <p className="text-xs text-amber-500/80 leading-relaxed">
                          Si vous soupçonnez que votre compte a été compromis, n'oubliez pas de régénérer également votre jeton API pour protéger vos intégrations.
                        </p>
                      </div>

                      <div className="space-y-4">
                        <Input 
                          label="Mot de passe actuel"
                          type="password"
                          placeholder="••••••••"
                        />
                        
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          <div className="space-y-4">
                            <Input 
                              label="Nouveau mot de passe"
                              type="password"
                              value={profileNewPassword}
                              onChange={(e: any) => setProfileNewPassword(e.target.value)}
                              placeholder="••••••••"
                            />
                            <Input 
                              label="Confirmation mot de passe"
                              type="password"
                              value={profileConfirmPassword}
                              onChange={(e: any) => setProfileConfirmPassword(e.target.value)}
                              placeholder="••••••••"
                            />
                          </div>
                          
                          <div className="p-4 bg-zinc-950/50 border border-zinc-800/50 rounded-xl space-y-2">
                            <p className="text-[9px] font-black text-zinc-500 uppercase tracking-widest mb-2">Exigences :</p>
                            <ul className="space-y-1">
                              {[
                                'Un caractère spécial',
                                'Un nombre',
                                'Une minuscule',
                                'Une majuscule',
                                'Au moins 8 caractères'
                              ].map((req) => (
                                <li key={req} className="text-[10px] text-zinc-400 flex items-center gap-2">
                                  <div className="w-1 h-1 rounded-full bg-zinc-600" />
                                  {req}
                                </li>
                              ))}
                            </ul>
                          </div>
                        </div>

                        <Button variant="secondary" className="w-full">
                          Changer le mot de passe
                        </Button>
                      </div>
                    </div>

                    <div className="pt-8 border-t border-zinc-800/50">
                      <button className="text-xs font-black text-red-500 uppercase tracking-widest hover:underline">
                        Supprimer mon compte
                      </button>
                    </div>
                  </div>
                ) : (
                  <form onSubmit={handleUpdateProfile} className="space-y-6 relative z-10">
                    <div className="flex items-center justify-between mb-4">
                      <h2 className="text-2xl font-black text-white uppercase tracking-tight">Modifier le Profil</h2>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <Input 
                        label="Nom et prénom"
                        value={profileUsername}
                        onChange={(e: any) => setProfileUsername(e.target.value)}
                        placeholder="Nom et prénom"
                      />
                      <Input 
                        label="E-mail"
                        type="email"
                        value={profileEmail}
                        onChange={(e: any) => setProfileEmail(e.target.value)}
                        placeholder="E-mail"
                      />
                    </div>

                    {profileUpdateMessage.text && (
                      <div className={cn(
                        "p-4 rounded-xl text-xs font-bold flex items-center gap-2 border",
                        profileUpdateMessage.type === 'success' ? 'bg-emerald-500/10 text-emerald-500 border-emerald-500/20' : 'bg-red-500/10 text-red-500 border-red-500/20'
                      )}>
                        {profileUpdateMessage.type === 'success' ? <ShieldCheck size={14} /> : <AlertCircle size={14} />}
                        {profileUpdateMessage.text}
                      </div>
                    )}

                    <Button 
                      type="submit"
                      className="w-full"
                      loading={profileUpdateLoading}
                      icon={ShieldCheck}
                    >
                      {profileUpdateLoading ? 'Mise à jour...' : 'Enregistrer les modifications'}
                    </Button>
                  </form>
                )}
              </Card>

              <div className="lg:col-span-2 bg-white text-black rounded-[2rem] p-8 lg:p-10 space-y-8 flex flex-col justify-between shadow-2xl shadow-white/5 relative overflow-hidden group">
                <div className="absolute -right-20 -bottom-20 w-48 h-48 bg-zinc-100 rounded-full opacity-50 group-hover:scale-110 transition-transform duration-1000" />
                
                <div className="space-y-4 relative z-10">
                  <div className="p-2 bg-black rounded-xl text-white w-fit">
                    <Logo size={48} showText={false} />
                  </div>
                  <h3 className="text-2xl lg:text-3xl font-black tracking-tighter leading-none">WO PLAYER <br/>Infrastructure</h3>
                  <p className="text-zinc-600 font-medium text-sm leading-relaxed">
                    Votre partenaire technologique pour une diffusion IPTV de classe mondiale.
                  </p>
                </div>
                
                <div className="pt-6 border-t border-zinc-100 flex justify-between relative z-10">
                  {[
                    { label: 'Support', value: '24/7' },
                    { label: 'Uptime', value: '99.9%' },
                    { label: 'Livraison', value: 'Instant' }
                  ].map((stat) => (
                    <div key={stat.label} className="text-center">
                      <p className="text-2xl font-black">{stat.value}</p>
                      <p className="text-[9px] text-zinc-400 uppercase font-black tracking-widest">{stat.label}</p>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* Advanced Tools */}
            <div className="relative group">
              <div className="absolute inset-0 bg-primary/5 blur-[100px] opacity-0 group-hover:opacity-100 transition-opacity duration-1000" />
              <Card className="p-12 space-y-12 overflow-hidden rounded-[3rem]">
                <div className="flex items-center gap-4">
                  <div className="p-3 bg-primary/10 rounded-xl text-primary">
                    <Globe size={24} />
                  </div>
                  <h3 className="text-3xl font-black tracking-tighter">Outils de Migration</h3>
                </div>
                
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
                  <div className="space-y-6">
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      <Input 
                        label="Mass Host Change" 
                        placeholder="Ancien Host (http://...)" 
                        value={massOldHost}
                        onChange={(e: any) => setMassOldHost(e.target.value)}
                      />
                      <Input 
                        label="&nbsp;" 
                        placeholder="Nouveau Host (http://...)" 
                        value={massNewHost}
                        onChange={(e: any) => setMassNewHost(e.target.value)}
                      />
                    </div>
                    <Button 
                      variant="white" 
                      size="lg" 
                      className="w-full"
                      onClick={() => {
                        if (!massOldHost || !massNewHost) {
                          notify("Veuillez remplir les deux champs", "error");
                          return;
                        }
                        notify("Migration lancée avec succès !", "success");
                        setMassOldHost('');
                        setMassNewHost('');
                      }}
                    >
                      Lancer la Migration
                    </Button>
                  </div>
                  
                  <div className="p-10 bg-amber-500/5 border border-amber-500/10 rounded-[2.5rem] flex flex-col justify-center gap-6 relative overflow-hidden">
                    <div className="absolute -right-10 -bottom-10 w-40 h-40 bg-amber-500/5 rounded-full blur-2xl" />
                    <div className="flex items-center gap-3 text-amber-500 relative z-10">
                      <History size={20} className="rotate-45" />
                      <p className="text-[10px] font-black uppercase tracking-widest">Avertissement Critique</p>
                    </div>
                    <p className="text-zinc-500 leading-relaxed font-medium relative z-10">
                      Le changement de masse d'hôte est une opération <span className="text-white font-bold">irréversible</span>. Assurez-vous que le nouveau serveur est totalement opérationnel avant de valider. Toutes les MACs seront migrées instantanément.
                    </p>
                  </div>
                </div>
              </Card>
            </div>
          </motion.div>
        )}

        {activeTab === 'check_mac' && (
          <motion.div 
            key="check_mac"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="max-w-4xl space-y-12 pb-20"
          >
            <header className="space-y-4">
              <Badge variant="default" className="bg-zinc-800 border-zinc-700 text-zinc-400">
                <Search size={12} />
                Outil de Diagnostic
              </Badge>
              <h1 className="text-5xl md:text-7xl font-black tracking-tighter leading-none">Vérifier <span className="text-zinc-500">MAC</span></h1>
              <p className="text-zinc-500 text-xl font-medium max-w-2xl leading-relaxed">Vérifiez instantanément le statut d'une adresse MAC sur nos serveurs.</p>
            </header>

            <Card className="p-8 space-y-8 border-zinc-800/50">
              <div className="space-y-4">
                <Input 
                  label="Adresse MAC de l'appareil"
                  value={checkMacInput}
                  onChange={(e: any) => setCheckMacInput(e.target.value.toUpperCase())}
                  placeholder="00:11:22:33:44:55"
                  className="text-lg font-mono"
                  rightElement={
                    <Button 
                      onClick={async () => {
                        if (!checkMacInput) return;
                        setLoading(true);
                        try {
                          const result = await api.checkMacStatus(checkMacInput);
                          setCheckResult(result);
                          notify("Vérification terminée", "success");
                        } catch (err) {
                          setCheckResult({ error: "Erreur lors de la vérification" });
                          notify("Erreur de vérification", "error");
                        } finally {
                          setLoading(false);
                        }
                      }}
                      disabled={loading}
                      loading={loading}
                      icon={Search}
                      size="sm"
                    >
                      Vérifier
                    </Button>
                  }
                />
              </div>

              {checkResult && (
                <motion.div 
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  className="p-8 bg-zinc-950/50 border border-zinc-800 rounded-2xl space-y-6"
                >
                  {checkResult.error ? (
                    <div className="flex items-center gap-4 text-red-500">
                      <AlertCircle size={32} />
                      <div>
                        <p className="font-black text-xl tracking-tight">Erreur</p>
                        <p className="text-zinc-500 font-medium">{checkResult.error}</p>
                      </div>
                    </div>
                  ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                      <div className="space-y-1">
                        <p className="text-[10px] font-black text-zinc-600 uppercase tracking-widest">Statut de l'appareil</p>
                        <div className="flex items-center gap-3">
                          <div className={`w-3 h-3 rounded-full ${checkResult.active ? 'bg-primary shadow-[0_0_15px_rgba(0,255,0,0.5)]' : 'bg-zinc-700'}`} />
                          <p className="text-2xl font-black tracking-tight">{checkResult.active ? 'ACTIF' : 'INACTIF'}</p>
                        </div>
                      </div>
                      <div className="space-y-1">
                        <p className="text-[10px] font-black text-zinc-600 uppercase tracking-widest">Date d'expiration</p>
                        <p className="text-2xl font-black tracking-tight text-zinc-300">{checkResult.expiry || 'N/A'}</p>
                      </div>
                      <div className="space-y-1">
                        <p className="text-[10px] font-black text-zinc-600 uppercase tracking-widest">Dernière Connexion</p>
                        <p className="text-lg font-bold text-zinc-400">{checkResult.last_seen || 'Jamais'}</p>
                      </div>
                      <div className="space-y-1">
                        <p className="text-[10px] font-black text-zinc-600 uppercase tracking-widest">Version APK</p>
                        <p className="text-lg font-bold text-zinc-400">{checkResult.version || 'Inconnue'}</p>
                      </div>
                    </div>
                  )}
                </motion.div>
              )}
            </Card>
          </motion.div>
        )}

        {activeTab === 'store_info' && (
          <motion.div 
            key="store_info"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="max-w-5xl space-y-12 pb-20"
          >
            <header className="space-y-4">
              <Badge variant="default" className="bg-zinc-800 border-zinc-700 text-zinc-400">
                <Store size={12} />
                Informations
              </Badge>
              <h1 className="text-5xl md:text-7xl font-black tracking-tighter leading-none">Infos <span className="text-zinc-500">Magasin</span></h1>
              <p className="text-zinc-500 text-xl font-medium max-w-2xl leading-relaxed">Découvrez les détails officiels de l'application WOPlayer.</p>
            </header>

            <Card className="p-8 md:p-12 border-zinc-800/50 space-y-8">
              <div className="space-y-6">
                <div className="inline-block">
                  <Badge variant="error" className="bg-red-500/10 border-red-500/20 text-red-500">
                    LECTEUR MÉDIA, PAS DE CANAUX INCLUS
                  </Badge>
                </div>
                
                <div className="space-y-2">
                  <h2 className="text-3xl font-black text-white tracking-tight">Votre meilleur lecteur de media</h2>
                  <p className="text-primary text-4xl font-black italic tracking-tighter">WoPlayer</p>
                </div>

                <div className="prose prose-invert max-w-3xl">
                  <p className="text-zinc-400 text-lg leading-relaxed">
                    Vivez le divertissement ultime avec <span className="text-white font-bold">WoPlayer</span>, votre application incontournable pour profiter des playlists et regarder vos contenus préférés. 
                    Plongez dans l'innovation des lecteurs multimédias avec <span className="text-white font-bold">WoPlayer</span>, un leader des solutions de divertissement.
                  </p>
                </div>

                <div className="pt-8 border-t border-zinc-800">
                  <p className="text-zinc-300 text-lg font-medium leading-relaxed">
                    Téléchargez <span className="text-primary font-bold">WoPlayer</span> dès maintenant depuis le <span className="text-white">Roku Store</span>, le <span className="text-white">LG TV Store</span>, le <span className="text-white">Samsung TV Store</span> et le <span className="text-white">Google Play Store</span>, et plongez dans une nouvelle ère de divertissement !
                  </p>
                </div>

                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 pt-8">
                  {['Roku Store', 'LG Content Store', 'Samsung Apps', 'Google Play'].map((store) => (
                    <Card key={store} className="p-4 bg-zinc-950 border-zinc-800 text-center">
                      <p className="text-[10px] font-black text-zinc-600 uppercase tracking-widest mb-1">Disponible sur</p>
                      <p className="text-xs font-bold text-zinc-300">{store}</p>
                    </Card>
                  ))}
                </div>
              </div>
            </Card>
          </motion.div>
        )}

        {activeTab === 'download_apk' && (
          <motion.div 
            key="download_apk"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="max-w-5xl space-y-12 pb-20"
          >
            <header className="space-y-4">
              <Badge variant="default" className="bg-zinc-800 border-zinc-700 text-zinc-400">
                <List size={12} />
                Légal
              </Badge>
              <h1 className="text-5xl md:text-7xl font-black tracking-tighter leading-none">Termes et <span className="text-zinc-500">Conditions</span></h1>
              <p className="text-zinc-500 text-xl font-medium max-w-2xl leading-relaxed">Conditions générales de vente en ligne du service WOPlayer.</p>
            </header>

            <Card className="p-8 md:p-12 border-zinc-800/50">
              <div className="prose prose-invert max-w-none space-y-6 text-zinc-400 text-sm leading-relaxed">
                <h2 className="text-2xl font-black text-white tracking-tight">Conditions générales de vente en ligne</h2>
                <p>Nous vous invitons à lire les présentes conditions générales de vente en ligne du site web WOPlayer.</p>
                <p className="font-bold text-white">La validation de votre formulaire de transaction en ligne vaut acceptation irrévocable des présentes conditions.</p>

                <div className="space-y-8">
                  <section className="space-y-3">
                    <h3 className="text-lg font-bold text-white">1. Préambule</h3>
                    <p>Les présentes conditions générales régissent tous les achats et abonnements effectués via l'application WOPlayer ou le site web WOPlayer.app. Toute transaction réalisée via notre application ou notre site web implique l'acceptation pleine et entière des présentes conditions par le client.</p>
                  </section>

                  <section className="space-y-3">
                    <h3 className="text-lg font-bold text-white">2. Objectif</h3>
                    <p>Ces conditions définissent les droits et obligations des deux parties concernant la vente de licences ou d'abonnements WOPlayer, qu'ils soient achetés via notre site web, l'App Store ou Google Play.</p>
                  </section>

                  <section className="space-y-3">
                    <h3 className="text-lg font-bold text-white">3. Processus de vente</h3>
                    <p>Les clients peuvent acheter une licence ou un abonnement :</p>
                    <ul className="list-disc pl-5 space-y-2">
                      <li>Sur notre site web ( https://woplayer.app ) en sélectionnant le forfait souhaité, en saisissant leurs informations et en effectuant le paiement via une plateforme sécurisée.</li>
                      <li>Par le biais d’achats intégrés sur l’ App Store (Apple) ou Google Play (Android).</li>
                    </ul>
                    <p>Les prix et les conditions de paiement peuvent varier selon la plateforme et la région. Chaque magasin fixe ses propres prix, taxes et modalités de facturation.</p>
                  </section>

                  <section className="space-y-3">
                    <h3 className="text-lg font-bold text-white">4. Modes de paiement</h3>
                    <div className="space-y-4">
                      <div>
                        <h4 className="font-bold text-zinc-200">a. Achats sur le site Web</h4>
                        <p>Les paiements sur le site web sont traités de manière sécurisée via la plateforme du Centre Monétique Interbancaire. Vous pouvez payer par carte bancaire. Une fois la transaction confirmée par la banque, le montant est débité le jour ouvrable suivant.</p>
                      </div>
                      <div>
                        <h4 className="font-bold text-zinc-200">b. Achats sur l'App Store et Google Play</h4>
                        <p>Pour les achats effectués directement dans l'application :</p>
                        <ul className="list-disc pl-5 space-y-2">
                          <li>Le paiement est traité par Apple Inc. ou Google LLC conformément à leurs conditions d'utilisation respectives.</li>
                          <li>WOPlayer ne stocke ni ne traite vos informations de paiement.</li>
                          <li>Le renouvellement des abonnements est géré par la boutique correspondante (App Store / Google Play) et peut être annulé à tout moment depuis les paramètres de votre compte.</li>
                        </ul>
                      </div>
                    </div>
                  </section>

                  <section className="space-y-3">
                    <h3 className="text-lg font-bold text-white">5. Renouvellement et gestion des abonnements</h3>
                    <p>Pour les abonnements achetés via l'App Store ou Google Play :</p>
                    <ul className="list-disc pl-5 space-y-2">
                      <li>Les abonnements sont automatiquement renouvelés sauf s'ils sont annulés au moins 24 heures avant la date de renouvellement.</li>
                      <li>Les frais de renouvellement sont traités via votre compte Apple ou Google.</li>
                      <li>Vous pouvez gérer ou annuler votre abonnement à tout moment depuis les paramètres de votre compte.</li>
                    </ul>
                    <p>Pour les activations de site web, les renouvellements peuvent être effectués manuellement via votre site web WOPlayer.</p>
                  </section>

                  <section className="space-y-3">
                    <h3 className="text-lg font-bold text-white">6. Période d'essai</h3>
                    <p>WOPlayer peut proposer un essai gratuit de 30 jours pour permettre aux utilisateurs de tester l'application. Passé ce délai, vous pourrez acheter une licence ou un abonnement pour continuer à utiliser les fonctionnalités premium.</p>
                  </section>

                  <section className="space-y-3">
                    <h3 className="text-lg font-bold text-white">7. Tarification</h3>
                    <p>Les tarifs du site web sont actuellement les suivants :</p>
                    <ul className="list-disc pl-5 space-y-2">
                      <li>65 MAD (environ 5,99 EUR) par appareil pour un an</li>
                      <li>162 MAD (~14,99 EUR) par appareil pour une activation à vie (« FOREVER »), qui reste valable tant que WOPlayer continue de fonctionner et de fournir ses services.</li>
                    </ul>
                    <p>Les prix des achats intégrés sur l'App Store ou Google Play peuvent varier en fonction des frais de plateforme, des taxes régionales ou des taux de change. Tous les prix sont clairement affichés avant la confirmation du paiement.</p>
                  </section>

                  <section className="space-y-3">
                    <h3 className="text-lg font-bold text-white">8. Politique de remboursement</h3>
                    <p>Vous disposez d'un droit de rétractation de 7 jours à compter de la date de la transaction pour annuler un achat effectué sur notre site web. Pour exercer ce droit, veuillez nous contacter à l'adresse support@woplayer.app en indiquant votre adresse MAC et votre numéro de transaction.</p>
                    <p>Une fois l'annulation traitée, votre activation sera supprimée de notre système et l'appareil ne sera plus autorisé à être utilisé.</p>
                    <p>Les demandes de remboursement pour les achats effectués sur l'App Store or Google Play doivent être adressées directement à Apple ou Google, car les paiements sont traités par ces entreprises conformément à leurs politiques de remboursement.</p>
                  </section>

                  <section className="space-y-3">
                    <h3 className="text-lg font-bold text-white">9. Protection des données</h3>
                    <p>Les données personnelles collectées lors du paiement et de l'activation sont utilisées uniquement pour le traitement de votre commande. Nous traitons vos informations de manière confidentielle, conformément à notre Politique de confidentialité . Vous pouvez demander l'accès à vos données personnelles ou leur rectification en contactant support@woplayer.app .</p>
                  </section>

                  <section className="space-y-3">
                    <h3 className="text-lg font-bold text-white">10. Preuve de paiement</h3>
                    <p>Les données de paiement enregistrées par nos partenaires de paiement autorisés (Centre Monétique Interbancaire, Apple ou Google) constituent une preuve valable de la transaction.</p>
                  </section>

                  <section className="space-y-3">
                    <h3 className="text-lg font-bold text-white">11. Contact</h3>
                    <p>Pour toute question concernant votre commande, votre abonnement ou votre compte, veuillez contacter notre équipe d'assistance :</p>
                    <p className="font-bold text-primary">📧 support@woplayer.app</p>
                  </section>
                </div>
              </div>
            </Card>
          </motion.div>
        )}

        {activeTab === 'api_docs' && (
          <motion.div 
            key="api_docs"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="max-w-5xl space-y-12 pb-20"
          >
            <header className="space-y-4">
              <Badge variant="primary">
                <Zap size={12} />
                Nouveauté Développeur
              </Badge>
              <h1 className="text-5xl md:text-7xl font-black tracking-tighter leading-none">Accès <span className="text-zinc-500">API</span></h1>
              <p className="text-zinc-500 text-xl font-medium max-w-2xl leading-relaxed">Intégrez nos services d'activation directement dans votre propre système ou site web.</p>
            </header>

            <Card className="p-8 border-zinc-800/50 space-y-8">
              <div className="flex flex-col md:flex-row items-center justify-between gap-6 p-8 bg-zinc-950/50 border border-zinc-800 rounded-2xl">
                <div className="space-y-2">
                  <p className="text-[10px] font-black text-zinc-600 uppercase tracking-widest">Votre Clé API Secrète</p>
                  <div className="flex items-center gap-4">
                    <code className="text-xl font-mono font-bold text-primary tracking-wider">wo_live_************************</code>
                    <button className="p-2 text-zinc-500 hover:text-white transition-colors">
                      <Copy size={20} />
                    </button>
                  </div>
                </div>
                <Button variant="secondary">
                  Régénérer la Clé
                </Button>
              </div>

              <div className="space-y-6">
                <h3 className="text-2xl font-black tracking-tight">Documentation Rapide</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <Card className="p-6 bg-zinc-950/30 border-zinc-800/50 space-y-3">
                    <div className="flex items-center gap-3 text-blue-500">
                      <Server size={20} />
                      <span className="font-black text-xs uppercase tracking-widest">Endpoint d'Activation</span>
                    </div>
                    <code className="block text-[10px] font-mono text-zinc-500 bg-black/50 p-3 rounded-lg">POST /api/v1/activate</code>
                    <p className="text-xs text-zinc-400">Permet d'activer une adresse MAC en utilisant vos crédits.</p>
                  </Card>
                  <Card className="p-6 bg-zinc-950/30 border-zinc-800/50 space-y-3">
                    <div className="flex items-center gap-3 text-emerald-500">
                      <TrendingUp size={20} />
                      <span className="font-black text-xs uppercase tracking-widest">Endpoint Statut</span>
                    </div>
                    <code className="block text-[10px] font-mono text-zinc-500 bg-black/50 p-3 rounded-lg">GET /api/v1/status/:mac</code>
                    <p className="text-xs text-zinc-400">Vérifie le statut et l'expiration d'un client.</p>
                  </Card>
                </div>
              </div>
            </Card>
          </motion.div>
        )}

        {activeTab === 'support' && (
          <motion.div 
            key="support"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="max-w-5xl space-y-12 pb-20"
          >
            <header className="space-y-4">
              <Badge variant="default" className="bg-zinc-800 border-zinc-700 text-zinc-400">
                <Globe size={12} />
                Assistance Technique
              </Badge>
              <h1 className="text-5xl md:text-7xl font-black tracking-tighter leading-none">Centre de <span className="text-zinc-500">Soutien</span></h1>
              <p className="text-zinc-500 text-xl font-medium max-w-2xl leading-relaxed">Besoin d'aide ? Notre équipe d'experts est disponible 24/7 pour vous accompagner.</p>
            </header>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <Card className="p-8 border-zinc-800/50 space-y-6 group hover:border-primary/30 transition-all cursor-pointer">
                <div className="w-16 h-16 bg-emerald-500/10 rounded-2xl flex items-center justify-center text-emerald-500 group-hover:scale-110 transition-transform">
                  <ExternalLink size={32} />
                </div>
                <div>
                  <h3 className="text-xl font-black tracking-tight">WhatsApp Pro</h3>
                  <p className="text-zinc-500 text-sm font-medium mt-2">Réponse instantanée pour les urgences techniques.</p>
                </div>
              </Card>
              <Card className="p-8 border-zinc-800/50 space-y-6 group hover:border-primary/30 transition-all cursor-pointer">
                <div className="w-16 h-16 bg-blue-500/10 rounded-2xl flex items-center justify-center text-blue-500 group-hover:scale-110 transition-transform">
                  <Globe size={32} />
                </div>
                <div>
                  <h3 className="text-xl font-black tracking-tight">Canal Telegram</h3>
                  <p className="text-zinc-500 text-sm font-medium mt-2">Restez informé des mises à jour et de l'état des serveurs.</p>
                </div>
              </Card>
              <Card className="p-8 border-zinc-800/50 space-y-6 group hover:border-primary/30 transition-all cursor-pointer">
                <div className="w-16 h-16 bg-purple-500/10 rounded-2xl flex items-center justify-center text-purple-500 group-hover:scale-110 transition-transform">
                  <Edit size={32} />
                </div>
                <div>
                  <h3 className="text-xl font-black tracking-tight">Ticket Support</h3>
                  <p className="text-zinc-500 text-sm font-medium mt-2">Ouvrez un ticket pour un suivi détaillé de votre demande.</p>
                </div>
              </Card>
            </div>
          </motion.div>
        )}
        </AnimatePresence>

        {/* Activation Modal */}
        <AnimatePresence>
          {showActivationModal && (
            <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
              <motion.div 
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={() => setShowActivationModal(false)}
                className="absolute inset-0 bg-black/90 backdrop-blur-md"
              />
              <motion.div 
                initial={{ opacity: 0, scale: 0.95, y: 20 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.95, y: 20 }}
                className="relative w-full max-w-md bg-zinc-950 border border-zinc-800 rounded-[2.5rem] shadow-2xl overflow-hidden p-8"
              >
                <div className="space-y-6">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="p-2.5 bg-blue-500/10 rounded-xl text-blue-500">
                        <PlusCircle size={24} />
                      </div>
                      <h2 className="text-2xl font-black tracking-tighter">
                        {activationStep === 1 ? 'Nouvelle Activation' : 'Configuration Source'}
                      </h2>
                    </div>
                    <button onClick={() => setShowActivationModal(false)} className="p-2 text-zinc-500 hover:text-white transition-colors">
                      <X size={24} />
                    </button>
                  </div>

                  {activationStep === 1 ? (
                    <div className="space-y-6">
                      <Input 
                        label="Votre adresse MAC"
                        value={activationMac}
                        onChange={(e: any) => setActivationMac(e.target.value.toUpperCase())}
                        placeholder="00:1A:79:..."
                        className="text-lg font-mono"
                      />
                      <Textarea 
                        label="Note"
                        value={activationNote}
                        onChange={(e: any) => setActivationNote(e.target.value)}
                        placeholder="Note pour ce client..."
                        className="h-24"
                      />
                      <Button 
                        onClick={() => setActivationStep(2)}
                        className="w-full"
                        size="lg"
                      >
                        Suivant
                      </Button>
                    </div>
                  ) : (
                    <div className="space-y-6">
                      <div className="space-y-2">
                        <label className="text-[10px] font-black text-zinc-600 uppercase tracking-widest ml-1">Type de Source</label>
                        <div className="grid grid-cols-3 gap-2">
                          {(['link', 'xtream', 'file'] as const).map((type) => (
                            <button
                              key={type}
                              onClick={() => setActivationSourceType(type)}
                              className={cn(
                                "py-3 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all border",
                                activationSourceType === type
                                  ? "bg-blue-500 border-blue-500 text-white shadow-lg shadow-blue-500/20"
                                  : "bg-zinc-900 border-zinc-800 text-zinc-500 hover:border-zinc-700"
                              )}
                            >
                              {type === 'link' ? 'Lien' : type === 'xtream' ? 'Xtream' : 'Fichier'}
                            </button>
                          ))}
                        </div>
                      </div>

                      {activationSourceType === 'link' && (
                        <Input 
                          label="Lien M3U"
                          value={activationUrl}
                          onChange={(e: any) => setActivationUrl(e.target.value)}
                          placeholder="http://..."
                        />
                      )}

                      {activationSourceType === 'xtream' && (
                        <div className="space-y-4">
                          <Input 
                            label="Host"
                            value={activationHost}
                            onChange={(e: any) => setActivationHost(e.target.value)}
                            placeholder="http://server.com:8080"
                          />
                          <div className="grid grid-cols-2 gap-4">
                            <Input 
                              label="Utilisateur"
                              value={activationUsername}
                              onChange={(e: any) => setActivationUsername(e.target.value)}
                            />
                            <Input 
                              label="Mot de passe"
                              type="password"
                              value={activationPassword}
                              onChange={(e: any) => setActivationPassword(e.target.value)}
                            />
                          </div>
                        </div>
                      )}

                      {activationSourceType === 'file' && (
                        <div className="space-y-2">
                          <label className="text-[10px] font-black text-zinc-600 uppercase tracking-widest ml-1">Fichier M3U</label>
                          <div className="relative group">
                            <input 
                              type="file" 
                              onChange={(e) => setActivationFile(e.target.files?.[0] || null)}
                              className="absolute inset-0 w-full h-full opacity-0 cursor-pointer z-10"
                            />
                            <div className="w-full bg-zinc-900 border-2 border-dashed border-zinc-800 rounded-2xl px-5 py-8 text-center group-hover:border-blue-500 transition-all">
                              <Download size={32} className="mx-auto text-zinc-600 mb-2 group-hover:text-blue-500" />
                              <p className="text-xs font-bold text-zinc-500">
                                {activationFile ? activationFile.name : 'Cliquez ou glissez votre fichier ici'}
                              </p>
                            </div>
                          </div>
                        </div>
                      )}

                      <div className="flex gap-4 pt-4">
                        <Button 
                          variant="outline"
                          onClick={() => setActivationStep(1)}
                          className="flex-1"
                        >
                          Retour
                        </Button>
                        <Button 
                          onClick={handleFullActivation}
                          className="flex-1"
                          loading={loading}
                          disabled={loading}
                        >
                          Enregistrer
                        </Button>
                      </div>
                    </div>
                  )}
                </div>
              </motion.div>
            </div>
          )}
        </AnimatePresence>

        {/* Payment Modal Refactored */}
        <AnimatePresence>
          {showPaymentModal && (
            <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 sm:p-6">
              <motion.div 
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={() => !loading && setShowPaymentModal(false)}
                className="absolute inset-0 bg-black/90 backdrop-blur-md"
              />
              <motion.div 
                initial={{ opacity: 0, scale: 0.95, y: 20 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.95, y: 20 }}
                className="relative w-full max-w-lg bg-zinc-950 border border-zinc-800 rounded-[2.5rem] shadow-2xl overflow-hidden flex flex-col"
              >
                {/* Header */}
                <div className="p-6 border-b border-zinc-900 flex items-center justify-between bg-zinc-900/50">
                  <div className="flex items-center gap-3">
                    <div className="p-2 bg-primary/10 rounded-lg text-primary">
                      <ShoppingBag size={20} />
                    </div>
                    <h2 className="text-lg font-black tracking-tight">Finaliser la commande</h2>
                  </div>
                  <button 
                    disabled={loading}
                    onClick={() => setShowPaymentModal(false)} 
                    className="p-2 text-zinc-500 hover:text-white hover:bg-zinc-800 rounded-xl transition-all disabled:opacity-0"
                  >
                    <X size={20} />
                  </button>
                </div>

                <div className="flex-1 overflow-y-auto custom-scrollbar p-6 space-y-8">
                  {paymentSuccess ? (
                    <div className="py-12 text-center space-y-6 animate-in zoom-in-95 duration-500">
                      <div className="w-20 h-20 bg-primary rounded-full flex items-center justify-center mx-auto shadow-[0_0_40px_rgba(242,125,38,0.3)]">
                        <ShieldCheck size={40} className="text-black" />
                      </div>
                      <div className="space-y-2">
                        <h3 className="text-2xl font-black tracking-tighter">Paiement Confirmé !</h3>
                        <p className="text-zinc-400 font-medium">Vos crédits ont été ajoutés à votre compte.</p>
                      </div>
                      <p className="text-[10px] font-black uppercase tracking-[0.2em] text-primary animate-pulse">Redirection en cours...</p>
                    </div>
                  ) : (
                    <>
                      {/* Order Summary Card */}
                      <div className="bg-zinc-900/50 border border-zinc-800 rounded-3xl p-5 flex items-center justify-between">
                        <div className="space-y-1">
                          <p className="text-[10px] font-black text-zinc-500 uppercase tracking-widest">Produit sélectionné</p>
                          <p className="text-xl font-black text-white">Pack {selectedPack?.qty} Crédits</p>
                        </div>
                        <div className="text-right">
                          <p className="text-[10px] font-black text-zinc-500 uppercase tracking-widest">Total à payer</p>
                          <p className="text-2xl font-black text-primary">{selectedPack?.price.toLocaleString()} {currency}</p>
                        </div>
                      </div>

                      {/* Payment Method Selection */}
                      <div className="space-y-4">
                        <p className="text-[10px] font-black text-zinc-500 uppercase tracking-widest ml-1">Mode de paiement</p>
                        <div className="grid grid-cols-2 gap-3">
                          <button 
                            onClick={() => setPaymentMethod('direct_momo')}
                            className={`flex flex-col items-center gap-3 p-4 rounded-2xl border-2 transition-all relative overflow-hidden group ${paymentMethod === 'direct_momo' ? 'border-primary bg-primary/5 text-primary' : 'border-zinc-800 text-zinc-500 hover:border-zinc-700'}`}
                          >
                            <div className="flex items-center gap-2">
                              <div className="flex -space-x-2">
                                <div className="w-8 h-8 rounded-lg bg-yellow-400 flex items-center justify-center text-[10px] font-black text-black border-2 border-zinc-950 shadow-lg">MTN</div>
                                <div className="w-8 h-8 rounded-lg bg-red-600 flex items-center justify-center text-[10px] font-black text-white border-2 border-zinc-950 shadow-lg">AIR</div>
                              </div>
                            </div>
                            <span className="font-black text-[10px] uppercase tracking-widest">Direct MoMo</span>
                            <span className="text-[7px] font-black uppercase tracking-[0.2em] opacity-40">Congo Brazzaville</span>
                          </button>
                          
                          {PAYMENT_METHODS[userCountry as keyof typeof PAYMENT_METHODS] && (
                            <button 
                              onClick={() => setPaymentMethod('yabetoopay')}
                              className={`flex flex-col items-center gap-3 p-4 rounded-2xl border-2 transition-all relative overflow-hidden group ${paymentMethod === 'yabetoopay' ? 'border-primary bg-primary/5 text-primary' : 'border-zinc-800 text-zinc-500 hover:border-zinc-700'}`}
                            >
                              <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-600 to-primary flex items-center justify-center text-white shadow-lg group-hover:scale-110 transition-transform">
                                <Globe size={20} />
                              </div>
                              <span className="font-black text-[10px] uppercase tracking-widest">YabetooPay</span>
                            </button>
                          )}

                          <button 
                            onClick={() => setPaymentMethod('moneyfusion')}
                            className={`flex flex-col items-center gap-3 p-4 rounded-2xl border-2 transition-all relative overflow-hidden group ${paymentMethod === 'moneyfusion' ? 'border-primary bg-primary/5 text-primary' : 'border-zinc-800 text-zinc-500 hover:border-zinc-700'}`}
                          >
                            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-600 to-primary flex items-center justify-center text-white shadow-lg group-hover:scale-110 transition-transform">
                              <Globe size={20} />
                            </div>
                            <span className="text-[10px] font-black uppercase tracking-widest">MoneyFusion</span>
                            <span className="text-[7px] font-black uppercase tracking-[0.2em] opacity-40">Afrique Francophone</span>
                          </button>
                          <button 
                            onClick={() => setPaymentMethod('card')}
                            className={`flex flex-col items-center gap-3 p-4 rounded-2xl border-2 transition-all relative overflow-hidden group ${paymentMethod === 'card' ? 'border-primary bg-primary/5 text-primary' : 'border-zinc-800 text-zinc-500 hover:border-zinc-700'}`}
                          >
                            <div className="flex gap-1">
                              <div className="w-8 h-5 bg-zinc-800 rounded-sm border border-zinc-700 flex items-center justify-center">
                                <div className="w-2 h-2 rounded-full bg-orange-500 -mr-1" />
                                <div className="w-2 h-2 rounded-full bg-red-500" />
                              </div>
                              <div className="w-8 h-5 bg-zinc-800 rounded-sm border border-zinc-700 flex items-center justify-center">
                                <span className="text-[6px] font-black text-blue-400 italic">VISA</span>
                              </div>
                            </div>
                            <span className="text-[10px] font-black uppercase tracking-widest">Carte Bancaire</span>
                            <span className="text-[7px] font-black uppercase tracking-[0.2em] opacity-40">Visa / Mastercard</span>
                          </button>
                        </div>
                      </div>

                      {/* Direct MoMo Details */}
                      {paymentMethod === 'direct_momo' && (
                        <div className="space-y-6 animate-in fade-in slide-in-from-top-2 duration-300">
                          <div className="flex items-center justify-between bg-zinc-900 rounded-2xl p-1 border border-zinc-800">
                            <button 
                              onClick={() => setAiValidationMode(false)}
                              className={`flex-1 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all ${!aiValidationMode ? 'bg-zinc-800 text-white' : 'text-zinc-500 hover:text-zinc-300'}`}
                            >
                              Manuel
                            </button>
                            <button 
                              onClick={() => setAiValidationMode(true)}
                              className={`flex-1 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all flex items-center justify-center gap-2 relative overflow-hidden ${aiValidationMode ? 'bg-primary text-black' : 'text-zinc-500 hover:text-zinc-300'}`}
                            >
                              <Sparkles size={12} />
                              Validation IA
                              <span className="absolute -right-2 -top-2 bg-white text-black text-[6px] px-2 py-1 rounded-full font-black rotate-12 shadow-lg">FAST</span>
                            </button>
                          </div>

                          {!aiValidationMode ? (
                            <>
                              <div className="space-y-3">
                                <p className="text-[10px] font-black text-zinc-500 uppercase tracking-widest ml-1">Opérateur Congo Brazzaville</p>
                                <div className="grid grid-cols-2 gap-3">
                                  {[
                                    { id: 'mtn_cg', label: 'MTN Money', color: 'bg-yellow-400', logo: 'MTN', textColor: 'text-black' },
                                    { id: 'airtel_cg', label: 'Airtel Money', color: 'bg-red-600', logo: 'Airtel', textColor: 'text-white' }
                                  ].map((p) => (
                                    <button 
                                      key={p.id}
                                      onClick={() => setMomoProvider(p.id as any)}
                                      className={`group relative p-6 rounded-2xl border flex flex-col items-center justify-center gap-3 transition-all ${momoProvider === p.id ? 'bg-white text-black border-white shadow-xl scale-[1.02]' : 'bg-zinc-900 text-zinc-500 border-zinc-800 hover:border-zinc-700'}`}
                                    >
                                      <div className={`w-12 h-12 rounded-xl ${p.color} flex items-center justify-center shadow-lg transform group-hover:rotate-6 transition-transform`}>
                                        <span className={`font-black text-[10px] ${p.textColor}`}>{p.logo}</span>
                                      </div>
                                      <span className="font-black text-[10px] uppercase tracking-widest">{p.label}</span>
                                      {momoProvider === p.id && (
                                        <div className="absolute -top-2 -right-2 w-6 h-6 bg-primary rounded-full flex items-center justify-center text-black border-4 border-zinc-950">
                                          <ShieldCheck size={12} />
                                        </div>
                                      )}
                                    </button>
                                  ))}
                                </div>
                              </div>

                              {momoProvider && (
                                <div className="bg-primary/5 border border-primary/20 rounded-2xl p-4 space-y-3">
                                  <p className="text-[10px] font-black text-primary uppercase tracking-widest">Instructions de paiement</p>
                                  <div className="space-y-2">
                                    <p className="text-xs text-zinc-300 leading-relaxed">
                                      {momoProvider === 'mtn_cg' ? (
                                        <>Composez le <span className="text-white font-bold">*105#</span>, choisissez 'Paiement', puis entrez le numéro marchand <span className="text-white font-bold">06 583 82 96</span>.</>
                                      ) : (
                                        <>Composez le <span className="text-white font-bold">*128#</span>, choisissez 'Achat', puis entrez le numéro marchand <span className="text-white font-bold">05 540 40 76</span>.</>
                                      )}
                                    </p>
                                    <div className="pt-2 border-t border-primary/10">
                                      <label className="text-[9px] font-black text-zinc-500 uppercase tracking-widest block mb-2">ID de Transaction (après envoi)</label>
                                      <input 
                                        type="text"
                                        value={phoneNumber}
                                        onChange={(e) => setPhoneNumber(e.target.value)}
                                        placeholder="Ex: TXN123456789"
                                        className="w-full bg-black/40 border border-zinc-800 rounded-xl px-4 py-3 text-sm font-mono text-white focus:border-primary outline-none transition-all"
                                      />
                                    </div>
                                  </div>
                                </div>
                              )}
                            </>
                          ) : (
                            <div className="space-y-4 animate-in zoom-in-95 duration-300">
                              <div className="bg-primary/10 border border-primary/30 rounded-2xl p-5 space-y-3 relative overflow-hidden">
                                <div className="absolute -right-4 -top-4 w-20 h-20 bg-primary/10 rounded-full blur-2xl" />
                                <div className="flex items-center gap-2 text-primary">
                                  <div className="p-1.5 bg-primary/20 rounded-lg">
                                    <Sparkles size={16} />
                                  </div>
                                  <p className="text-[11px] font-black uppercase tracking-widest">Validation Instantanée par IA</p>
                                </div>
                                <p className="text-[11px] text-zinc-300 leading-relaxed">
                                  Notre IA analyse votre capture d'écran en quelques secondes pour confirmer le transfert. <span className="text-primary font-bold">Plus besoin d'attendre la vérification manuelle !</span>
                                </p>
                              </div>

                              <div className="relative group">
                                <input 
                                  type="file" 
                                  accept="image/*"
                                  onChange={handleImageUpload}
                                  className="absolute inset-0 w-full h-full opacity-0 cursor-pointer z-10"
                                />
                                <div className={`w-full border-2 border-dashed rounded-3xl p-10 text-center transition-all duration-500 ${aiImage ? 'border-primary bg-primary/5' : 'border-zinc-800 bg-zinc-900/50 group-hover:border-primary/50'}`}>
                                  {aiImage ? (
                                    <div className="space-y-4">
                                      <div className="relative inline-block">
                                        <img src={aiImage} alt="Preview" className="w-32 h-32 object-cover mx-auto rounded-2xl border-2 border-primary shadow-2xl shadow-primary/20" />
                                        <div className="absolute -bottom-2 -right-2 bg-primary text-black p-1.5 rounded-lg shadow-lg">
                                          <ShieldCheck size={14} />
                                        </div>
                                      </div>
                                      <p className="text-[10px] font-black text-primary uppercase tracking-widest">Capture d'écran chargée</p>
                                    </div>
                                  ) : (
                                    <div className="space-y-4">
                                      <div className="w-16 h-16 bg-zinc-800 rounded-2xl flex items-center justify-center mx-auto group-hover:scale-110 group-hover:bg-primary/10 transition-all duration-500">
                                        <Camera size={32} className="text-zinc-600 group-hover:text-primary transition-colors" />
                                      </div>
                                      <div className="space-y-1">
                                        <p className="text-sm font-black text-white">Importer le reçu</p>
                                        <p className="text-[10px] font-medium text-zinc-500 uppercase tracking-widest">Format JPG, PNG ou Capture d'écran</p>
                                      </div>
                                    </div>
                                  )}
                                </div>
                              </div>

                              {aiResult && (
                                <div className={`p-4 rounded-2xl border flex items-start gap-3 animate-in slide-in-from-bottom-2 ${aiResult.success ? 'bg-primary/10 border-primary/20 text-primary' : 'bg-red-500/10 border-red-500/20 text-red-500'}`}>
                                  {aiResult.success ? <CheckCircle2 size={18} /> : <AlertCircle size={18} />}
                                  <div className="space-y-1">
                                    <p className="text-xs font-black leading-none">{aiResult.success ? 'Succès' : 'Échec'}</p>
                                    <p className="text-[10px] font-medium opacity-80">{aiResult.message}</p>
                                  </div>
                                </div>
                              )}

                              <button 
                                onClick={handleAIVerification}
                                disabled={!aiImage || aiLoading}
                                className="w-full bg-white text-black py-4 rounded-2xl font-black text-sm uppercase tracking-widest hover:scale-[1.02] active:scale-[0.98] transition-all disabled:opacity-20 flex items-center justify-center gap-3"
                              >
                                {aiLoading ? (
                                  <div className="w-5 h-5 border-2 border-black/20 border-t-black rounded-full animate-spin" />
                                ) : (
                                  <>
                                    <Sparkles size={18} />
                                    <span>Analyser & Valider</span>
                                  </>
                                )}
                              </button>
                            </div>
                          )}
                        </div>
                      )}

                      {paymentMethod === 'moneyfusion' && (
                        <div className="space-y-6 animate-in fade-in slide-in-from-top-2 duration-300">
                          <div className="p-6 bg-primary/5 border border-primary/10 rounded-3xl space-y-4">
                            <div className="flex items-center gap-3 text-primary">
                              <Globe size={20} />
                              <p className="text-xs font-black uppercase tracking-widest">PawaPay Global</p>
                            </div>
                            <p className="text-sm text-zinc-400 leading-relaxed">
                              PawaPay vous permet de payer via Mobile Money dans plusieurs pays (Sénégal, Mali, Burkina Faso, etc.).
                            </p>
                            <div className="space-y-2">
                              <label className="text-[10px] font-black text-zinc-500 uppercase tracking-widest ml-1">Numéro de téléphone</label>
                              <input 
                                type="tel" 
                                value={phoneNumber}
                                onChange={(e) => setPhoneNumber(e.target.value)}
                                placeholder="Ex: +221..."
                                className="w-full bg-zinc-950 border border-zinc-800 rounded-2xl px-5 py-4 text-white font-bold focus:border-primary outline-none transition-all"
                              />
                            </div>
                          </div>
                        </div>
                      )}

                      {paymentMethod === 'card' && (
                        <div className="py-8 text-center space-y-4 animate-in fade-in slide-in-from-top-2 duration-300">
                          <div className="w-16 h-16 bg-zinc-900 rounded-full flex items-center justify-center mx-auto text-zinc-700">
                            <CreditCard size={32} />
                          </div>
                          <p className="text-sm text-zinc-500 font-medium">Le paiement par carte sera bientôt disponible.</p>
                        </div>
                      )}
                    </>
                  )}
                </div>

                {/* Footer Action */}
                {!paymentSuccess && (
                  <div className="p-6 border-t border-zinc-900 bg-zinc-900/30 space-y-4">
                    <button 
                      disabled={loading || (paymentMethod === 'direct_momo' && (!phoneNumber || phoneNumber.length < 5)) || (paymentMethod === 'moneyfusion' && (!phoneNumber || phoneNumber.length < 5)) || paymentMethod === 'card'}
                      onClick={handleConfirmPayment}
                      className="w-full bg-primary text-black py-4 rounded-2xl font-black text-sm uppercase tracking-widest hover:scale-[1.02] active:scale-[0.98] transition-all disabled:opacity-20 disabled:grayscale shadow-xl shadow-primary/10 flex items-center justify-center gap-3"
                    >
                      {loading ? (
                        <div className="w-5 h-5 border-2 border-black/20 border-t-black rounded-full animate-spin" />
                      ) : (
                        <>
                          <Zap size={18} fill="currentColor" />
                          <span>Payer {selectedPack?.price.toLocaleString()} {currency}</span>
                        </>
                      )}
                    </button>
                    <div className="flex items-center justify-center gap-2 text-[9px] font-black text-zinc-600 uppercase tracking-widest">
                      <ShieldCheck size={12} />
                      Transaction 100% Sécurisée
                    </div>
                  </div>
                )}
              </motion.div>
            </div>
          )}
        </AnimatePresence>
      </div>

      {/* Management Floating Page (Modal) */}
      <AnimatePresence>
        {managedClient && managementType && (
          <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 md:p-8">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => { setManagedClient(null); setManagementType(null); }}
              className="absolute inset-0 bg-black/80 backdrop-blur-md"
            />
            <motion.div
              initial={{ opacity: 0, scale: 0.9, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.9, y: 20 }}
              className="relative w-full max-w-lg bg-zinc-900 border border-zinc-800 rounded-[2rem] overflow-hidden shadow-[0_50px_100px_rgba(0,0,0,0.8)]"
            >
              <div className="p-6 md:p-10 space-y-6 md:space-y-8">
                <header className="flex items-center justify-between">
                  <div className="space-y-1">
                    <div className={`inline-flex items-center gap-2 px-3 py-1 rounded-full text-[9px] font-black uppercase tracking-widest ${managementType === 'client' ? 'bg-primary/10 text-primary' : 'bg-blue-500/10 text-blue-500'}`}>
                      {managementType === 'client' ? <ShieldCheck size={10} /> : <List size={10} />}
                      Gestion {managementType === 'client' ? 'Client' : 'Playlist'}
                    </div>
                    <h2 className="text-2xl md:text-3xl font-black tracking-tighter leading-none">
                      {managedClient.target_mac}
                    </h2>
                    {managedClient.note && (
                      <p className="text-zinc-500 font-bold text-xs">Note: {managedClient.note}</p>
                    )}
                  </div>
                  <button 
                    onClick={() => { setManagedClient(null); setManagementType(null); }}
                    className="p-3 bg-zinc-800 text-zinc-400 hover:text-white rounded-full transition-all"
                  >
                    <X size={20} />
                  </button>
                </header>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 md:gap-4">
                  {managementType === 'client' ? (
                    <>
                      <button onClick={() => { alert('Activé'); setManagedClient(null); }} className="group p-4 md:p-5 bg-zinc-800/50 hover:bg-primary rounded-2xl text-left transition-all duration-300">
                        <UserCheck size={24} className="text-primary group-hover:text-white mb-3 transition-colors" />
                        <p className="text-white font-black text-base mb-0.5">Activer</p>
                        <p className="text-zinc-500 group-hover:text-white/70 text-[10px] font-bold leading-tight">Prolonger l'abonnement.</p>
                      </button>
                      <button onClick={() => { alert('Verrouillé'); setManagedClient(null); }} className="group p-4 md:p-5 bg-zinc-800/50 hover:bg-blue-500 rounded-2xl text-left transition-all duration-300">
                        <Lock size={24} className="text-blue-500 group-hover:text-white mb-3 transition-colors" />
                        <p className="text-white font-black text-base mb-0.5">Verrouillage</p>
                        <p className="text-zinc-500 group-hover:text-white/70 text-[10px] font-bold leading-tight">Sécuriser l'accès MAC.</p>
                      </button>
                      <button onClick={() => { alert('Rétracté'); setManagedClient(null); }} className="group p-4 md:p-5 bg-zinc-800/50 hover:bg-amber-500 rounded-2xl text-left transition-all duration-300">
                        <RotateCcw size={24} className="text-amber-500 group-hover:text-white mb-3 transition-colors" />
                        <p className="text-white font-black text-base mb-0.5">Rétracter</p>
                        <p className="text-zinc-500 group-hover:text-white/70 text-[10px] font-bold leading-tight">Annuler l'opération.</p>
                      </button>
                      <button onClick={() => { alert('Transfert Activé'); setManagedClient(null); }} className="group p-4 md:p-5 bg-zinc-800/50 hover:bg-purple-500 rounded-2xl text-left transition-all duration-300">
                        <Zap size={24} className="text-purple-500 group-hover:text-white mb-3 transition-colors" />
                        <p className="text-white font-black text-base mb-0.5">Transfert</p>
                        <p className="text-zinc-500 group-hover:text-white/70 text-[10px] font-bold leading-tight">Activer transfert ME2U.</p>
                      </button>
                      <button onClick={() => { alert('Détaché'); setManagedClient(null); }} className="sm:col-span-2 group p-4 md:p-5 bg-red-500/5 hover:bg-red-500 rounded-2xl text-left transition-all duration-300 border border-red-500/10">
                        <UserMinus size={24} className="text-red-500 group-hover:text-white mb-3 transition-colors" />
                        <p className="text-white font-black text-base mb-0.5">Détacher le client</p>
                        <p className="text-zinc-500 group-hover:text-white/70 text-[10px] font-bold leading-tight">Supprimer définitivement ce client.</p>
                      </button>
                    </>
                  ) : (
                    <>
                      <button onClick={() => { alert('Gérer'); setManagedClient(null); }} className="group p-4 md:p-5 bg-zinc-800/50 hover:bg-blue-500 rounded-2xl text-left transition-all duration-300">
                        <Settings size={24} className="text-blue-500 group-hover:text-white mb-3 transition-colors" />
                        <p className="text-white font-black text-base mb-0.5">Gérer la liste</p>
                        <p className="text-zinc-500 group-hover:text-white/70 text-[10px] font-bold leading-tight">Configuration des chaînes.</p>
                      </button>
                      <button onClick={() => { alert('Modifier'); setManagedClient(null); }} className="group p-4 md:p-5 bg-zinc-800/50 hover:bg-amber-500 rounded-2xl text-left transition-all duration-300">
                        <Edit size={24} className="text-amber-500 group-hover:text-white mb-3 transition-colors" />
                        <p className="text-white font-black text-base mb-0.5">Modifier</p>
                        <p className="text-zinc-500 group-hover:text-white/70 text-[10px] font-bold leading-tight">Mettre à jour les liens.</p>
                      </button>
                      <button onClick={() => { alert('Supprimé'); setManagedClient(null); }} className="sm:col-span-2 group p-4 md:p-5 bg-red-500/5 hover:bg-red-500 rounded-2xl text-left transition-all duration-300 border border-red-500/10">
                        <Trash2 size={24} className="text-red-500 group-hover:text-white mb-3 transition-colors" />
                        <p className="text-white font-black text-base mb-0.5">Supprimer tous les liens</p>
                        <p className="text-zinc-500 group-hover:text-white/70 text-[10px] font-bold leading-tight">Réinitialisation de la playlist.</p>
                      </button>
                    </>
                  )}
                </div>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

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
};
