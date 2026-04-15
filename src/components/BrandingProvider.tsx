import React, { createContext, useContext, useState, useEffect } from 'react';
import { api } from '../services/api';

interface BrandingContextType {
  branding: any;
  loading: boolean;
}

const BrandingContext = createContext<BrandingContextType>({ branding: null, loading: true });

export const useBranding = () => useContext(BrandingContext);

export const BrandingProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [branding, setBranding] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchBranding = async () => {
      try {
        const data = await api.getBranding();
        if (data) {
          setBranding(data);
          
          // Apply app name to title
          if (data.appName) {
            document.title = data.appName;
          }

          // Apply primary color to CSS variable
          if (data.primaryColor) {
            document.documentElement.style.setProperty('--color-primary', data.primaryColor);
            // Also update primary-dark if it's not set, or derive it
            // For now just set primary
          }
        }
      } catch (error) {
        console.error('Error in BrandingProvider:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchBranding();
  }, []);

  return (
    <BrandingContext.Provider value={{ branding, loading }}>
      {children}
    </BrandingContext.Provider>
  );
};
