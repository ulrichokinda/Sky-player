import React, { useState, useEffect } from 'react';
import { Card, Button, Input, Select, Badge } from './ui';
import { PAYMENT_METHODS } from '../constants';
import { validatePhone } from '../lib/validation';
import { getCurrencyInfo, getConvertedPrice } from '../lib/currency';
import { api } from '../services/api';
import { Globe, Smartphone, CreditCard, X, ShieldCheck, Fingerprint } from 'lucide-react';

const COUNTRY_FLAGS: Record<string, string> = {
  'Côte d\'Ivoire': '🇨🇮',
  'Sénégal': '🇸🇳',
  'Mali': '🇲🇱',
  'Burkina Faso': '🇧🇫',
  'Togo': '🇹🇬',
  'Bénin': '🇧🇯',
  'Guinée': '🇬🇳',
  'Niger': '🇳🇪',
  'Congo (Brazzaville)': '🇨🇬',
  'Cameroun': '🇨🇲',
  'Gabon': '🇬🇦',
  'RDC (Kinshasa)': '🇨🇩',
  'Tchad': '🇹🇩',
  'Centrafrique': '🇨🇫',
  'International (Visa / MasterCard)': '🌐'
};

const activationPlans = [
  { id: '1an', name: 'Activation 1 An', price: 2285, credits: 0, desc: 'Usage personnel' },
  { id: 'vie', name: 'Activation à Vie', price: 4675, credits: 0, desc: 'Usage personnel illimité' }
];

export const DirectCheckoutModal = ({ isOpen, onClose, initialPlanId }: { isOpen: boolean, onClose: () => void, initialPlanId: string }) => {
  const [selectedPlan, setSelectedPlan] = useState<any>(null);
  const [macAddress, setMacAddress] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [selectedCountry, setSelectedCountry] = useState('Côte d\'Ivoire');
  const [selectedProviderId, setSelectedProviderId] = useState('');
  const [loading, setLoading] = useState(false);
  const [paymentStatus, setPaymentStatus] = useState<string | null>(null);

  useEffect(() => {
    if (initialPlanId) {
      const plan = activationPlans.find(p => p.id === initialPlanId);
      if (plan) setSelectedPlan(plan);
    }
  }, [initialPlanId]);

  if (!isOpen) return null;

  const validateMac = (mac: string) => {
    const macRegex = /^([0-9A-Fa-f]{2}[:-]?){5}([0-9A-Fa-f]{2})$/;
    return macRegex.test(mac);
  };

  const handlePayment = async () => {
    if (!selectedPlan || !selectedProviderId || !macAddress) {
      alert('Veuillez remplir tous les champs (adresse MAC, pack et opérateur)');
      return;
    }

    if (!validateMac(macAddress)) {
      alert('Format d\'adresse MAC invalide (ex: AA:BB:CC:DD:EE:FF)');
      return;
    }

    const providerInfo = PAYMENT_METHODS[selectedCountry as keyof typeof PAYMENT_METHODS]?.find(p => p.id === selectedProviderId);
    if (!providerInfo) throw new Error('Opérateur non trouvé');

    const isCard = providerInfo.id === 'card';

    if (!isCard && !phoneNumber) {
      alert('Veuillez entrer votre numéro de téléphone');
      return;
    }

    if (!isCard && !validatePhone(phoneNumber)) {
      alert('Numéro de téléphone invalide');
      return;
    }
    
    setLoading(true);
    setPaymentStatus(null);
    try {
      const result = await api.initiateJOboostCheckout({
        userId: 'GUEST',
        amount: getConvertedPrice(selectedPlan.price, selectedCountry),
        phoneNumber: phoneNumber || 'N/A',
        credits_purchased: selectedPlan.credits,
        target_mac: macAddress,
        plan_id: selectedPlan.id
      });
      
      if (result && result.payment_url) {
        window.location.href = result.payment_url;
      } else if (result && result.success) {
        setPaymentStatus(result.message || "Demande de paiement envoyée. Suivez les instructions sur votre téléphone.");
      } else {
        throw new Error(result?.error || 'Erreur lors de l\'initialisation du paiement');
      }
    } catch (error: any) {
      alert(error.message || 'Erreur lors du paiement');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-[100] flex items-center justify-center p-4">
      <div className="bg-zinc-950 border border-zinc-800 rounded-3xl w-full max-w-2xl overflow-hidden relative flex flex-col max-h-[90vh]">
        <button 
          onClick={onClose}
          className="absolute top-4 right-4 p-2 bg-zinc-900 rounded-full hover:bg-zinc-800 text-zinc-400 hover:text-white z-10"
        >
          <X size={20} />
        </button>

        <div className="p-6 md:p-8 overflow-y-auto custom-scrollbar flex-1">
          <div className="text-center space-y-2 mb-8 mt-4">
            <Badge variant="primary">Sans Inscription</Badge>
            <h2 className="text-3xl font-black mt-2">Activer <span className="text-primary">{macAddress || 'votre appareil'}</span></h2>
            <p className="text-zinc-400">Paiement direct et activation instantanée</p>
          </div>

          {paymentStatus ? (
            <div className="text-center space-y-6 py-8">
              <div className="w-20 h-20 bg-primary/20 text-primary rounded-full flex items-center justify-center mx-auto">
                <Smartphone size={40} />
              </div>
              <h3 className="text-xl font-bold">{paymentStatus}</h3>
              <p className="text-zinc-400 max-w-sm mx-auto">
                Veuillez valider le paiement sur votre téléphone. L'appareil sera activé automatiquement une fois le paiement confirmé.
              </p>
              <Button onClick={onClose} className="mt-4">Fermer la fenêtre</Button>
            </div>
          ) : (
            <div className="space-y-8">
              {/* Étape 1: MAC et Plan */}
              <div className="space-y-4">
                <div className="flex items-center gap-3 border-b border-zinc-800 pb-2">
                  <Fingerprint className="text-primary" size={20} />
                  <h3 className="text-lg font-bold">1. Identifiants & Choix</h3>
                </div>
                
                <Input 
                  label="Adresse MAC de l'appareil" 
                  value={macAddress} 
                  onChange={(e: any) => setMacAddress(e.target.value.toUpperCase())} 
                  placeholder="Ex: A1:B2:C3:D4:E5:F6" 
                  error={macAddress && !validateMac(macAddress) ? "L'adresse MAC doit contenir 6 groupes de 2 caractères" : null}
                />

                <div className="grid grid-cols-2 gap-3 pt-2">
                  {activationPlans.map(plan => (
                    <button 
                      key={plan.id}
                      onClick={() => setSelectedPlan(plan)}
                      className={`p-4 rounded-xl border text-left transition-all ${selectedPlan?.id === plan.id ? 'bg-primary/10 border-primary shadow-[0_0_15px_rgba(var(--primary),0.2)]' : 'bg-zinc-900 border-zinc-800 hover:border-zinc-700'}`}
                    >
                      <h4 className="font-bold whitespace-nowrap">{plan.name}</h4>
                      <p className="text-xl font-black text-primary mt-1">{getConvertedPrice(plan.price, selectedCountry)} {getCurrencyInfo(selectedCountry).symbol}</p>
                    </button>
                  ))}
                </div>
              </div>

              {/* Étape 2: Sélection du Pays */}
              <div className="space-y-4">
                <div className="flex items-center gap-3 border-b border-zinc-800 pb-2">
                  <Globe className="text-primary" size={20} />
                  <h3 className="text-lg font-bold">2. Localisation</h3>
                </div>
                <div className="grid grid-cols-2 lg:grid-cols-3 gap-2 max-h-[200px] overflow-y-auto custom-scrollbar pr-2">
                  {Object.keys(PAYMENT_METHODS).map(country => (
                    <button 
                      key={country}
                      onClick={() => {
                        setSelectedCountry(country);
                        setSelectedProviderId('');
                      }}
                      className={`p-2 rounded-lg border flex items-center gap-2 transition-all ${selectedCountry === country ? 'bg-primary/10 border-primary text-primary' : 'bg-zinc-900 text-zinc-400 border-zinc-800 hover:border-zinc-700'}`}
                    >
                      <span>{COUNTRY_FLAGS[country] || '🌍'}</span>
                      <span className="font-bold text-[10px] uppercase truncate text-left">{country}</span>
                    </button>
                  ))}
                </div>
              </div>

              {/* Étape 3: Paiement */}
              <div className="space-y-4">
                <div className="flex items-center gap-3 border-b border-zinc-800 pb-2">
                  <CreditCard className="text-primary" size={20} />
                  <h3 className="text-lg font-bold">3. Règlement</h3>
                </div>

                <div className="grid grid-cols-2 lg:grid-cols-3 gap-2">
                  {PAYMENT_METHODS[selectedCountry as keyof typeof PAYMENT_METHODS]?.map((p) => (
                    <button 
                      key={p.id}
                      onClick={() => setSelectedProviderId(p.id)}
                      className={`p-3 rounded-lg border flex flex-col items-center justify-center gap-2 transition-all ${selectedProviderId === p.id ? 'bg-primary/10 border-primary text-primary' : 'bg-zinc-900 text-zinc-500 border-zinc-800 hover:border-zinc-700'}`}
                    >
                      {p.id === 'card' ? <CreditCard size={18} /> : <Smartphone size={18} />}
                      <span className="font-bold text-[9px] uppercase text-center">{p.name}</span>
                    </button>
                  ))}
                </div>

                {selectedProviderId && (
                  <div className="pt-4 pb-4">
                    {PAYMENT_METHODS[selectedCountry as keyof typeof PAYMENT_METHODS]?.find(p => p.id === selectedProviderId)?.id !== 'card' ? (
                      <Input 
                        label="Numéro de téléphone (sans indicatif)" 
                        value={phoneNumber} 
                        onChange={(e: any) => setPhoneNumber(e.target.value)} 
                        placeholder="Ex: 061234567" 
                        error={phoneNumber && !validatePhone(phoneNumber) ? "Numéro invalide" : null}
                      />
                    ) : (
                      <div className="p-3 bg-zinc-900 rounded-lg border border-zinc-800 flex items-center gap-3">
                        <ShieldCheck className="text-primary shrink-0" size={20} />
                        <p className="text-[10px] text-zinc-400">Vous serez redirigé vers l'interface sécurisée.</p>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {!paymentStatus && (
          <div className="p-6 border-t border-zinc-800 bg-zinc-950 shrink-0">
            <Button 
              fullWidth 
              size="lg"
              loading={loading}
              disabled={!selectedPlan || !selectedProviderId || !macAddress || !validateMac(macAddress)}
              onClick={handlePayment}
            >
              Payer {selectedPlan ? getConvertedPrice(selectedPlan.price, selectedCountry).toLocaleString() : 0} {getCurrencyInfo(selectedCountry).symbol}
            </Button>
          </div>
        )}
      </div>
    </div>
  );
};
