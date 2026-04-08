import React, { useState } from 'react';
import { Card, Button, Input, Select, Badge } from '../components/ui';
import { Footer } from '../components/Footer';
import { PAYMENT_METHODS } from '../constants';
import { validatePhone } from '../lib/validation';
import { api } from '../services/api';
import { Globe, Smartphone, Zap, ArrowLeft } from 'lucide-react';
import { motion } from 'motion/react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';

export const Payment = () => {
  const [searchParams] = useSearchParams();
  const [selectedPlan, setSelectedPlan] = useState<any>(null);
  const [phoneNumber, setPhoneNumber] = useState('');
  const [selectedCountry, setSelectedCountry] = useState('Côte d\'Ivoire');
  const [selectedProviderId, setSelectedProviderId] = useState('');
  const [loading, setLoading] = useState(false);

  const resellerPlans = [
    { id: '10cr', name: '10 Crédits', price: 15000, credits: 10, desc: 'Idéal pour débuter' },
    { id: '20cr', name: '20 Crédits', price: 25750, credits: 20, desc: 'Le choix populaire' },
    { id: '50cr', name: '50 Crédits', price: 45000, credits: 50, desc: 'Meilleure valeur' }
  ];

  const activationPlans = [
    { id: '1an', name: 'Activation 1 An', price: 2000, credits: 0, desc: 'Usage personnel' },
    { id: 'vie', name: 'Activation à Vie', price: 4675, credits: 0, desc: 'Usage personnel illimité' }
  ];

  const allPlans = [...resellerPlans, ...activationPlans];

  React.useEffect(() => {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    if (user.country && PAYMENT_METHODS[user.country as keyof typeof PAYMENT_METHODS]) {
      setSelectedCountry(user.country);
    }
    
    const planId = searchParams.get('plan');
    if (planId) {
      const plan = allPlans.find(p => p.id === planId);
      if (plan) setSelectedPlan(plan);
    }
  }, [searchParams]);

  const handlePayment = async () => {
    if (!selectedPlan || !phoneNumber || !selectedProviderId) {
      alert('Veuillez choisir un pack, un pays, un opérateur et entrer votre numéro');
      return;
    }

    if (!validatePhone(phoneNumber)) {
      alert('Numéro de téléphone invalide');
      return;
    }
    
    setLoading(true);
    try {
      const user = JSON.parse(localStorage.getItem('user') || '{}');
      const providerInfo = PAYMENT_METHODS[selectedCountry as keyof typeof PAYMENT_METHODS]?.find(p => p.id === selectedProviderId);

      if (!providerInfo) throw new Error('Opérateur non trouvé');

      const primaryProvider = providerInfo.provider;
      const fallbackProvider = primaryProvider === 'moneyfusion' ? 'yabetoo' : 'moneyfusion';

      let result;
      try {
        // Tentative avec le fournisseur principal
        if (primaryProvider === 'moneyfusion') {
          result = await api.initiateMoneyFusion({
            userId: user.uid || 'guest',
            amount: selectedPlan.price,
            phoneNumber,
            credits_purchased: selectedPlan.credits,
            provider: providerInfo.id
          });
        } else {
          result = await api.initiateYabetooPay({
            userId: user.uid || 'guest',
            amount: selectedPlan.price,
            phoneNumber,
            credits_purchased: selectedPlan.credits,
            methodId: providerInfo.id
          });
        }

        if (!result.success) throw new Error(result.error || 'Échec du fournisseur principal');
      } catch (primaryError) {
        console.warn(`Échec de ${primaryProvider}, tentative avec le secours (${fallbackProvider})...`);
        
        // Tentative avec le fournisseur de secours
        if (fallbackProvider === 'moneyfusion') {
          result = await api.initiateMoneyFusion({
            userId: user.uid || 'guest',
            amount: selectedPlan.price,
            phoneNumber,
            credits_purchased: selectedPlan.credits,
            provider: providerInfo.id
          });
        } else {
          result = await api.initiateYabetooPay({
            userId: user.uid || 'guest',
            amount: selectedPlan.price,
            phoneNumber,
            credits_purchased: selectedPlan.credits,
            methodId: providerInfo.id
          });
        }
      }
      
      if (result && result.success) {
        alert(result.message);
      } else {
        throw new Error(result?.error || 'Erreur lors du paiement après tentative de secours');
      }
    } catch (error: any) {
      alert(error.message || 'Erreur lors du paiement');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-black text-white font-sans selection:bg-primary/30 relative">
      <Link 
        to="/" 
        className="absolute top-8 left-8 p-3 bg-zinc-900/50 border border-zinc-800 rounded-full text-zinc-400 hover:text-white hover:bg-zinc-800 transition-all z-50 group"
        title="Retour à l'accueil"
      >
        <ArrowLeft size={20} className="group-hover:-translate-x-1 transition-transform" />
      </Link>
      <div className="max-w-6xl mx-auto px-6 py-12 md:py-24 space-y-16">
        <header className="text-center space-y-4">
          <Badge variant="primary">Boutique Officielle</Badge>
          <h1 className="text-5xl md:text-7xl font-black tracking-tighter leading-none">
            Choisir un <span className="text-primary">Pack</span>
          </h1>
          <p className="text-zinc-500 text-xl font-medium max-w-2xl mx-auto leading-relaxed">
            Rechargez vos crédits instantanément pour activer vos clients.
          </p>
        </header>
        
        <div className="grid md:grid-cols-3 gap-8">
          {allPlans.map(plan => (
            <Card 
              key={plan.id} 
              className={`flex flex-col items-center gap-6 p-10 cursor-pointer transition-all duration-500 hover:scale-105 ${selectedPlan?.id === plan.id ? 'border-primary bg-primary/5 shadow-2xl shadow-primary/10' : 'border-zinc-800 hover:border-zinc-700'}`}
              onClick={() => setSelectedPlan(plan)}
            >
              <div className="p-4 bg-primary/10 rounded-2xl text-primary">
                <Zap size={32} />
              </div>
              <div className="text-center space-y-1">
                <h3 className="text-2xl font-black tracking-tight">{plan.name}</h3>
                <p className="text-[10px] font-black text-zinc-500 uppercase tracking-widest">{plan.desc}</p>
              </div>
              <p className="text-4xl font-black tracking-tighter">{plan.price.toLocaleString()} <span className="text-lg text-zinc-500">FCFA</span></p>
            </Card>
          ))}
        </div>

        {selectedPlan && (
          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="max-w-2xl mx-auto"
          >
            <Card className="p-8 md:p-12 space-y-10 border-zinc-800/50 bg-zinc-900/30 backdrop-blur-xl">
              <div className="flex items-center gap-4">
                <div className="p-3 bg-primary/10 rounded-xl text-primary">
                  <Globe size={24} />
                </div>
                <h2 className="text-3xl font-black tracking-tighter">Paiement Mobile Money</h2>
              </div>

              <div className="space-y-8">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <Select 
                    label="Votre Pays"
                    value={selectedCountry}
                    onChange={(e: any) => {
                      setSelectedCountry(e.target.value);
                      setSelectedProviderId('');
                    }}
                  >
                    {Object.keys(PAYMENT_METHODS).map(country => (
                      <option key={country} value={country}>{country}</option>
                    ))}
                  </Select>

                  <div className="space-y-3">
                    <label className="text-[10px] font-black text-zinc-500 uppercase tracking-widest ml-2">Opérateur</label>
                    <div className="grid grid-cols-2 gap-2">
                      {PAYMENT_METHODS[selectedCountry as keyof typeof PAYMENT_METHODS]?.map((p) => (
                        <button 
                          key={p.id}
                          onClick={() => setSelectedProviderId(p.id)}
                          className={`p-3 rounded-xl border flex flex-col items-center justify-center gap-2 transition-all ${selectedProviderId === p.id ? 'bg-primary/10 border-primary text-primary' : 'bg-zinc-950 text-zinc-500 border-zinc-800 hover:border-zinc-700'}`}
                        >
                          <Smartphone size={16} />
                          <span className="font-black text-[8px] uppercase tracking-widest text-center">{p.name}</span>
                        </button>
                      ))}
                    </div>
                  </div>
                </div>

                <Input 
                  label="Numéro de téléphone Mobile Money" 
                  value={phoneNumber} 
                  onChange={(e: any) => setPhoneNumber(e.target.value)} 
                  placeholder="Ex: +242..." 
                  error={phoneNumber && !validatePhone(phoneNumber) ? "Numéro invalide" : null}
                />

                <Button 
                  fullWidth 
                  size="lg"
                  loading={loading}
                  onClick={handlePayment}
                  className="py-6 text-base"
                >
                  Payer {selectedPlan.price.toLocaleString()} FCFA
                </Button>

                <p className="text-[10px] text-zinc-500 font-medium text-center leading-relaxed">
                  En cliquant sur payer, vous acceptez nos conditions générales de vente. <br />
                  Une demande de confirmation sera envoyée sur votre téléphone.
                </p>
              </div>
            </Card>
          </motion.div>
        )}
      </div>
      <Footer />
    </div>
  );
};
