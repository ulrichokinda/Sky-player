export const getCurrencyInfo = (country: string) => {
  const map: Record<string, { symbol: string; code: string; rate: number }> = {
    'France': { symbol: '€', code: 'EUR', rate: 1 / 655.957 },
    'Belgique': { symbol: '€', code: 'EUR', rate: 1 / 655.957 },
    'Luxembourg': { symbol: '€', code: 'EUR', rate: 1 / 655.957 },
    'Monaco': { symbol: '€', code: 'EUR', rate: 1 / 655.957 },
    'Suisse': { symbol: 'CHF', code: 'CHF', rate: 1 / 700 },
    'Canada (Québec)': { symbol: 'CAD', code: 'CAD', rate: 1 / 450 },
    'RDC (Kinshasa)': { symbol: 'CDF', code: 'CDF', rate: 4.5 },
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
  // Default to FCFA for most African francophone countries
  return map[country] || { symbol: 'FCFA', code: 'XOF', rate: 1 };
};

export const getConvertedPrice = (basePrice: number, country: string) => {
  const { rate } = getCurrencyInfo(country);
  return Math.round(basePrice * rate);
};
