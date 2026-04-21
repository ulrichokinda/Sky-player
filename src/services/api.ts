import { 
  db, 
  storage,
  ref,
  uploadBytes,
  getDownloadURL,
  collection, 
  doc, 
  getDoc, 
  getDocs, 
  setDoc, 
  addDoc, 
  updateDoc, 
  deleteDoc,
  query, 
  where, 
  onSnapshot,
  OperationType,
  handleFirestoreError
} from '../firebase';
import { serverTimestamp, Timestamp } from 'firebase/firestore';

export interface UserProfile {
  uid: string;
  email: string;
  username: string;
  firstName?: string;
  lastName?: string;
  role: 'admin' | 'reseller' | 'client';
  credits: number;
  phone?: string;
  country?: string;
  createdAt: any;
  trialStartedAt?: any;
  isPremium?: boolean;
}

export interface Activation {
  id: string;
  resellerId: string;
  target_mac: string;
  credits_used: number;
  note?: string;
  system?: string;
  version?: string;
  last_connection?: any;
  country_code?: string;
  playlist_url?: string;
  xtream_host?: string;
  xtream_username?: string;
  xtream_password?: string;
  createdAt: any;
  current_channel?: string;
}

export interface Payment {
  id: string;
  userId: string;
  amount: number;
  credits_purchased: number;
  payment_method: string;
  provider: string;
  status: 'pending' | 'completed' | 'failed';
  external_id: string;
  createdAt: any;
}

export const isTrialExpired = (trialStartedAt?: any) => {
  if (!trialStartedAt) return true;
  const trialDate = trialStartedAt instanceof Timestamp ? trialStartedAt.toDate() : new Date(trialStartedAt);
  const trialDuration = 21 * 24 * 60 * 60 * 1000;
  return new Date().getTime() - trialDate.getTime() > trialDuration;
};

export const api = {
  // Storage
  async uploadFile(file: File, path: string): Promise<string> {
    try {
      const fileRef = ref(storage, path);
      await uploadBytes(fileRef, file);
      return await getDownloadURL(fileRef);
    } catch (error) {
      console.error('Upload error:', error);
      throw error;
    }
  },

  async getBranding() {
    try {
      const brandingDoc = doc(db, 'settings', 'branding');
      const snap = await getDoc(brandingDoc);
      return snap.exists() ? snap.data() : null;
    } catch (error) {
      handleFirestoreError(error, OperationType.GET, 'settings/branding');
      return null;
    }
  },

  async updateBranding(data: any) {
    try {
      const brandingDoc = doc(db, 'settings', 'branding');
      await setDoc(brandingDoc, { ...data, updatedAt: serverTimestamp() }, { merge: true });
    } catch (error) {
      handleFirestoreError(error, OperationType.UPDATE, 'settings/branding');
    }
  },

  async registerUser(userData: Partial<UserProfile>) {
    if (!userData.uid) throw new Error('UID is required');
    try {
      const userDoc = doc(db, 'users', userData.uid);
      const existingUser = await getDoc(userDoc);
      
      const newUser = {
        ...userData,
        credits: userData.credits || 0,
        createdAt: existingUser.exists() ? existingUser.data().createdAt : serverTimestamp(),
        trialStartedAt: existingUser.exists() ? existingUser.data().trialStartedAt : serverTimestamp(),
        isPremium: userData.isPremium || false,
        role: userData.role || 'client'
      };
      
      await setDoc(userDoc, newUser, { merge: true });
      return newUser;
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, `users/${userData.uid}`);
    }
  },

  async getUser(uid: string): Promise<UserProfile> {
    try {
      const userDoc = doc(db, 'users', uid);
      const snapshot = await getDoc(userDoc);
      if (!snapshot.exists()) throw new Error('User not found');
      return snapshot.data() as UserProfile;
    } catch (error) {
      handleFirestoreError(error, OperationType.GET, `users/${uid}`);
      throw error;
    }
  },

  async updateUser(uid: string, data: Partial<UserProfile>) {
    try {
      const userDoc = doc(db, 'users', uid);
      await updateDoc(userDoc, data);
      const updated = await getDoc(userDoc);
      return updated.data() as UserProfile;
    } catch (error) {
      handleFirestoreError(error, OperationType.UPDATE, `users/${uid}`);
    }
  },

  async getActivations(resellerId: string): Promise<Activation[]> {
    try {
      const q = query(collection(db, 'activations'), where('resellerId', '==', resellerId));
      const snapshot = await getDocs(q);
      return snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Activation));
    } catch (error) {
      handleFirestoreError(error, OperationType.LIST, 'activations');
      return [];
    }
  },

  async createActivation(activation: Partial<Activation>) {
    try {
      const response = await fetch('/api/activations/create', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(activation)
      });
      
      const contentType = response.headers.get('content-type');
      if (!contentType || !contentType.includes('application/json')) {
        const text = await response.text();
        console.error('Expected JSON but got:', text.substring(0, 100));
        throw new Error('Le serveur a renvoyé une réponse invalide (HTML au lieu de JSON). Veuillez vérifier la configuration Firebase dans les réglages.');
      }
      
      const data = await response.json();
      if (!response.ok) throw new Error(data.error || 'Erreur lors de l\'activation');
      
      return data;
    } catch (error: any) {
      console.error('Create Activation Error:', error);
      throw error;
    }
  },

  async updateActivation(id: string, updates: Partial<Activation>) {
    try {
      const docRef = doc(db, 'activations', id);
      await updateDoc(docRef, updates);
    } catch (error) {
      handleFirestoreError(error, OperationType.UPDATE, 'activations');
    }
  },

  async deleteActivation(id: string) {
    try {
      const docRef = doc(db, 'activations', id);
      await deleteDoc(docRef);
    } catch (error) {
      handleFirestoreError(error, OperationType.DELETE, 'activations');
    }
  },

  async activateTrial(mac: string) {
    try {
      const normalizedMac = mac.toUpperCase().trim();
      const trialPlaylist = "https://raw.githubusercontent.com/Luka-Sky/SkyPlayer/main/trial.json"; 
      
      await this.createActivation({
        resellerId: "SYSTEM_TRIAL",
        target_mac: normalizedMac,
        credits_used: 0,
        note: "Essai Gratuit 14 Jours",
        playlist_url: trialPlaylist
      });
      return trialPlaylist;
    } catch (error) {
      console.error('Trial activation error:', error);
      throw error;
    }
  },

  async initYabetooPayment(data: { userId: string; amount: number; credits: number; phone: string; network: string; description?: string }) {
    const response = await fetch('/api/payments/yabetoo/init', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
    const result = await response.json();
    if (!response.ok) throw new Error(result.error || 'Erreur lors de l\'initialisation du paiement');
    return result;
  },

  async verifyYabetooPayment(paymentId: string) {
    const response = await fetch(`/api/payments/yabetoo/status/${paymentId}`);
    const result = await response.json();
    if (!response.ok) throw new Error(result.error || 'Erreur lors de la vérification');
    return result;
  },

  async getPayments(userId: string): Promise<Payment[]> {
    try {
      const q = query(collection(db, 'payments'), where('userId', '==', userId));
      const snapshot = await getDocs(q);
      return snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Payment));
    } catch (error) {
      handleFirestoreError(error, OperationType.LIST, 'payments');
      return [];
    }
  },

  async createPayment(payment: Partial<Payment>) {
    try {
      const docRef = await addDoc(collection(db, 'payments'), {
        ...payment,
        createdAt: serverTimestamp()
      });
      return { id: docRef.id, ...payment };
    } catch (error) {
      handleFirestoreError(error, OperationType.CREATE, 'payments');
    }
  },

  async initiatePayment(data: { userId: string; amount: number; phoneNumber: string; credits_purchased: number; provider: string; methodId: string }) {
    const response = await fetch('/api/payments/initiate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
    return response.json();
  },

  async initiateMoneyFusion(data: { userId: string; amount: number; phoneNumber: string; credits_purchased: number; provider: string }) {
    return this.initiatePayment({ ...data, methodId: data.provider, provider: 'moneyfusion' });
  },

  async initiateYabetooPay(data: { userId: string; amount: number; phoneNumber: string; credits_purchased: number; methodId: string }) {
    return this.initiatePayment({ ...data, provider: 'yabetoo' });
  },

  async initiateStripePayment(data: { userId: string; amount: number; credits_purchased: number }) {
    return this.initiatePayment({ ...data, phoneNumber: 'N/A', provider: 'stripe', methodId: 'card' });
  },

  async checkMacStatus(mac: string): Promise<{ active: boolean; activation?: any; error?: string }> {
    try {
      const normalizedMac = mac.toUpperCase().trim();
      const q = query(collection(db, 'activations'), where('target_mac', '==', normalizedMac));
      const snapshot = await getDocs(q);
      
      if (snapshot.empty) {
        return { 
          active: false, 
          error: "MAC non activée. Veuillez l'ajouter dans votre panel revendeur." 
        };
      }
      
      const data = snapshot.docs[0].data();
      return { 
        active: true, 
        activation: { id: snapshot.docs[0].id, ...data } 
      };
    } catch (error: any) {
      console.error('Check MAC error:', error);
      return { active: false, error: "Erreur de connexion au serveur." };
    }
  },

  async sendHeartbeat(data: { mac: string; system?: string; version?: string; country?: string; channel?: string }) {
    try {
      await fetch('/api/activations/heartbeat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
      });
    } catch (error) {
      console.error('Send heartbeat error:', error);
    }
  },

  async updateCurrentChannel(mac: string, channelName: string) {
    return this.sendHeartbeat({ mac, channelName });
  },

  // Admin Functions
  async generateCredits(uid: string, amount: number) {
    try {
      const userDoc = doc(db, 'users', uid);
      const snapshot = await getDoc(userDoc);
      if (!snapshot.exists()) throw new Error('User not found');
      
      const currentCredits = snapshot.data().credits || 0;
      await updateDoc(userDoc, {
        credits: currentCredits + amount
      });
      return currentCredits + amount;
    } catch (error) {
      handleFirestoreError(error, OperationType.UPDATE, `users/${uid}`);
    }
  },

  async getAllActivations(): Promise<Activation[]> {
    try {
      const snapshot = await getDocs(collection(db, 'activations'));
      return snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Activation));
    } catch (error) {
      handleFirestoreError(error, OperationType.LIST, 'activations');
      return [];
    }
  },

  async getAllPayments(): Promise<Payment[]> {
    try {
      const snapshot = await getDocs(collection(db, 'payments'));
      return snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Payment));
    } catch (error) {
      handleFirestoreError(error, OperationType.LIST, 'payments');
      return [];
    }
  },

  async getAllUsers(): Promise<UserProfile[]> {
    try {
      const snapshot = await getDocs(collection(db, 'users'));
      return snapshot.docs.map(doc => doc.data() as UserProfile);
    } catch (error) {
      handleFirestoreError(error, OperationType.LIST, 'users');
      return [];
    }
  }
};
