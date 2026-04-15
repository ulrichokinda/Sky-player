import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Home } from './pages/Home';
import { Dashboard } from './pages/Dashboard';
import { Payment } from './pages/Payment';
import { Register } from './pages/Register';
import { Login } from './pages/Login';
import { About } from './pages/About';
import { Contact } from './pages/Contact';
import { Privacy } from './pages/Privacy';
import { FAQ } from './pages/FAQ';
import { Assets } from './pages/Assets';
import { Terms } from './pages/Terms';
import { SimpleUserView } from './components/SimpleUserView';
import { Capacitor } from '@capacitor/core';
import { BrandingProvider } from './components/BrandingProvider';

export default function App() {
  const isNative = Capacitor.isNativePlatform();

  return (
    <BrandingProvider>
      <BrowserRouter>
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
      </BrowserRouter>
    </BrandingProvider>
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
