import React, { createContext, useContext, useState, useEffect } from 'react';
import { api } from '../services/api';
import { db, doc, onSnapshot, handleFirestoreError, OperationType } from '../firebase';

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
    const brandingDoc = doc(db, 'settings', 'branding');
    
    const unsubscribe = onSnapshot(brandingDoc, (snap) => {
      if (snap.exists()) {
        const data = snap.data();
        setBranding(data);
        
        // Apply app name to title
        if (data.appName) {
          document.title = data.appName;
        }

        // Apply primary color to CSS variable
        if (data.primaryColor) {
          document.documentElement.style.setProperty('--color-primary', data.primaryColor);
        }
      }
      setLoading(false);
    }, (error) => {
      console.error('Error in BrandingProvider:', error);
      handleFirestoreError(error, OperationType.GET, 'settings/branding');
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  return (
    <BrandingContext.Provider value={{ branding, loading }}>
      {children}
    </BrandingContext.Provider>
  );
};
