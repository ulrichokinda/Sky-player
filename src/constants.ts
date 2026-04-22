export const PAYMENT_METHODS = {
  // --- AFRIQUE CENTRALE (Priorité Yabetoo Pay) ---
  'Congo (Brazzaville)': [
    { id: 'mtn_cg', name: 'MTN Mobile Money', provider: 'yabetoo' },
    { id: 'airtel_cg', name: 'Airtel Money', provider: 'yabetoo' }
  ],
  'Cameroun': [
    { id: 'mtn_cm', name: 'MTN Mobile Money', provider: 'yabetoo' },
    { id: 'orange_cm', name: 'Orange Money', provider: 'yabetoo' }
  ],
  'Gabon': [
    { id: 'airtel_ga', name: 'Airtel Money', provider: 'yabetoo' },
    { id: 'moov_ga', name: 'Moov Money', provider: 'yabetoo' }
  ],
  'Tchad': [
    { id: 'airtel_td', name: 'Airtel Money', provider: 'yabetoo' },
    { id: 'moov_td', name: 'Moov Money', provider: 'yabetoo' }
  ],
  'Centrafrique': [
    { id: 'orange_cf', name: 'Orange Money', provider: 'yabetoo' },
    { id: 'telecel_cf', name: 'Telecel Cash', provider: 'yabetoo' }
  ],

  // --- AUTRES PAYS AFRIQUE (BkaPay) ---
  'Côte d\'Ivoire': [
    { id: 'orange_ci', name: 'Orange Money', provider: 'bkapay' },
    { id: 'mtn_ci', name: 'MTN Mobile Money', provider: 'bkapay' },
    { id: 'moov_ci', name: 'Moov Money', provider: 'bkapay' },
    { id: 'wave_ci', name: 'Wave', provider: 'bkapay' }
  ],
  'Sénégal': [
    { id: 'orange_sn', name: 'Orange Money', provider: 'bkapay' },
    { id: 'wave_sn', name: 'Wave', provider: 'bkapay' },
    { id: 'free_sn', name: 'Free Money', provider: 'bkapay' }
  ],
  'Bénin': [
    { id: 'mtn_bj', name: 'MTN Mobile Money', provider: 'bkapay' },
    { id: 'moov_bj', name: 'Moov Money', provider: 'bkapay' }
  ],
  'Burkina Faso': [
    { id: 'orange_bf', name: 'Orange Money', provider: 'bkapay' },
    { id: 'moov_bf', name: 'Moov Money', provider: 'bkapay' }
  ],
  'Mali': [
    { id: 'orange_ml', name: 'Orange Money', provider: 'bkapay' },
    { id: 'moov_ml', name: 'Moov Money', provider: 'bkapay' }
  ],
  'Togo': [
    { id: 'tmoney_tg', name: 'TMoney', provider: 'bkapay' },
    { id: 'moov_tg', name: 'Moov Money', provider: 'bkapay' }
  ],
  'RDC (Kinshasa)': [
    { id: 'mpesa_cd', name: 'M-Pesa', provider: 'bkapay' },
    { id: 'airtel_cd', name: 'Airtel Money', provider: 'bkapay' },
    { id: 'orange_cd', name: 'Orange Money', provider: 'bkapay' }
  ],
  'Niger': [
    { id: 'airtel_ne', name: 'Airtel Money', provider: 'bkapay' },
    { id: 'moov_ne', name: 'Moov Money', provider: 'bkapay' }
  ],
  'Guinée': [
    { id: 'orange_gn', name: 'Orange Money', provider: 'bkapay' },
    { id: 'mtn_gn', name: 'MTN Mobile Money', provider: 'bkapay' }
  ],

  // --- INTERNATIONAL (BkaPay Cards) ---
  'International (Visa / MasterCard)': [
    { id: 'card', name: 'Payer par Carte', provider: 'bkapay' }
  ]
};
