import React, { lazy, Suspense } from 'react';
import { HashRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Home } from './pages/Home';
import { SimpleUserView } from './components/SimpleUserView';
import { Capacitor } from '@capacitor/core';
import { BrandingProvider } from './components/BrandingProvider';

// Lazy load heavy pages
const Dashboard = lazy(() => import('./pages/Dashboard').then(module => ({ default: module.Dashboard })));
const Payment = lazy(() => import('./pages/Payment').then(module => ({ default: module.Payment })));
const Register = lazy(() => import('./pages/Register').then(module => ({ default: module.Register })));
const Login = lazy(() => import('./pages/Login').then(module => ({ default: module.Login })));
const About = lazy(() => import('./pages/About').then(module => ({ default: module.About })));
const Contact = lazy(() => import('./pages/Contact').then(module => ({ default: module.Contact })));
const Privacy = lazy(() => import('./pages/Privacy').then(module => ({ default: module.Privacy })));
const FAQ = lazy(() => import('./pages/FAQ').then(module => ({ default: module.FAQ })));
const Assets = lazy(() => import('./pages/Assets').then(module => ({ default: module.Assets })));
const Terms = lazy(() => import('./pages/Terms').then(module => ({ default: module.Terms })));

const LoadingFallback = () => (
  <div className="min-h-screen bg-black flex items-center justify-center">
    <div className="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
  </div>
);

export default function App() {
  const isNative = Capacitor.isNativePlatform();

  return (
    <ErrorBoundary>
      <BrandingProvider>
        <HashRouter>
          <Suspense fallback={<LoadingFallback />}>
            <Routes>
              {/* If native app (APK/TV), show the MAC screen as home. Otherwise show the landing page. */}
              <Route path="/" element={isNative ? <Navigate to="/app" replace /> : <Home />} />
              
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/payment" element={<Payment />} />
              <Route path="/register" element={<Register />} />
              <Route path="/login" element={<Login />} />
              <Route path="/about" element={<About />} />
              <Route path="/contact" element={<Contact />} />
              <Route path="/privacy" element={<Privacy />} />
              <Route path="/faq" element={<FAQ />} />
              <Route path="/assets" element={<Assets />} />
              <Route path="/terms" element={<Terms />} />
              
              {/* The specialized view for the APK/TV app */}
              <Route path="/app" element={<SimpleUserView channels={[]} onNotify={() => {}} />} />
            </Routes>
          </Suspense>
        </HashRouter>
      </BrandingProvider>
    </ErrorBoundary>
  );
}

class ErrorBoundary extends React.Component<{ children: React.ReactNode }, { hasError: boolean, error: any }> {
  constructor(props: any) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: any) {
    return { hasError: true, error };
  }

  componentDidCatch(error: any, errorInfo: any) {
    console.error("APP CRASHED:", error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-black text-white flex flex-col items-center justify-center p-8 text-center">
          <h1 className="text-2xl font-bold mb-4">Une erreur est survenue lors du chargement.</h1>
          <p className="text-zinc-500 mb-8 max-w-md">{this.state.error?.message || "Erreur de rendu sur ce modèle de TV."}</p>
          <button 
            onClick={() => window.location.reload()}
            className="px-6 py-3 bg-primary text-black rounded-full font-bold"
          >
            Redémarrer l'application
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}

interface NavItemProps {
  icon: React.ReactNode;
  label: string;
  active: boolean;
  onClick: () => void;
  collapsed: boolean;
  key?: React.Key;
}
