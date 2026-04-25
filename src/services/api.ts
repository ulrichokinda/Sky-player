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
  OperationType,
  handleFirestoreError
} from '../firebase';
import { serverTimestamp, Timestamp } from 'firebase/firestore';
import { Capacitor, CapacitorHttp } from '@capacitor/core';

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
  expiryDate?: any;
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

const nativeFetch = async (url: string, options: any = {}) => {
  if (Capacitor.isNativePlatform()) {
    try {
      const response = await CapacitorHttp.request({
        url: url.startsWith('/') ? `${window.location.origin}${url}` : url,
        method: options.method || 'GET',
        headers: {
          'Content-Type': 'application/json',
          ...(options.headers || {})
        },
        data: options.body ? JSON.parse(options.body) : undefined,
        connectTimeout: 30000,
        readTimeout: 30000
      });
      
      return {
        ok: response.status >= 200 && response.status < 300,
        status: response.status,
        headers: {
          get: (name: string) => response.headers[name] || response.headers[name.toLowerCase()]
        },
        json: async () => response.data,
        text: async () => typeof response.data === 'string' ? response.data : JSON.stringify(response.data)
      };
    } catch (e) {
      console.error('Native fetch error:', e);
      throw e;
    }
  }
  return fetch(url, options);
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
      const response = await nativeFetch('/api/activations/create', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(activation)
      });
      
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
    const response = await nativeFetch('/api/payments/initiate', {
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

  async initiateBkaPay(data: { userId: string; amount: number; phoneNumber: string; credits_purchased: number; methodId: string }) {
    return this.initiatePayment({ ...data, provider: 'bkapay' });
  },

  async initYabetooPayment(data: any) {
    return this.nativeApiCall('/api/payments/yabetoo/init', 'POST', data);
  },

  async verifyYabetooPayment(paymentId: string) {
    return this.nativeApiCall(`/api/payments/yabetoo/status/${paymentId}`, 'GET');
  },

  async nativeApiCall(url: string, method: string, body?: any) {
    const response = await nativeFetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: body ? JSON.stringify(body) : undefined
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'API Error');
    return data;
  },

  async updateUserPassword(user: any, newPassword: string) {
    const { updatePassword } = await import('../firebase');
    return updatePassword(user, newPassword);
  },

  async generateCredits(uid: string, amount: number) {
    try {
      const userDoc = doc(db, 'users', uid);
      const snapshot = await getDoc(userDoc);
      if (!snapshot.exists()) throw new Error('User not found');
      const currentCredits = snapshot.data().credits || 0;
      await updateDoc(userDoc, { credits: currentCredits + amount });
      return currentCredits + amount;
    } catch (error) {
      handleFirestoreError(error, OperationType.UPDATE, `users/${uid}`);
    }
  },

  async checkMacStatus(mac: string): Promise<{ active: boolean; activation?: any; error?: string }> {
    try {
      const normalizedMac = mac.toUpperCase().trim();
      const activationsRef = collection(db, 'activations');
      const q = query(activationsRef, where('target_mac', '==', normalizedMac));
      
      let snapshot;
      try {
        snapshot = await getDocs(q);
      } catch (e) {
        console.warn("getDocs failed, falling back to REST");
      }
      
      let foundData: any = null;
      let foundId: string | null = null;

      if (snapshot && !snapshot.empty) {
        foundData = snapshot.docs[0].data();
        foundId = snapshot.docs[0].id;
      } else {
        // REST Fallback for extreme cases
        try {
          const restUrl = `https://firestore.googleapis.com/v1/projects/skyplayer-60634/databases/(default)/documents:runQuery`;
          const restBody = {
            structuredQuery: {
              from: [{ collectionId: 'activations' }],
              where: {
                fieldFilter: {
                  field: { fieldPath: 'target_mac' },
                  op: 'EQUAL',
                  value: { stringValue: normalizedMac }
                }
              }
            }
          };
          
          const restResponse = await nativeFetch(restUrl, {
            method: 'POST',
            body: JSON.stringify(restBody)
          });
          
          if (restResponse.ok) {
            const restData: any = await restResponse.json();
            if (restData && restData.length > 0 && restData[0].document) {
              const doc = restData[0].document;
              foundId = doc.name.split('/').pop();
              
              const mapFields = (fields: any) => {
                const res: any = {};
                for (const key in fields) {
                  const valObj = fields[key];
                  if ('stringValue' in valObj) res[key] = valObj.stringValue;
                  else if ('integerValue' in valObj) res[key] = parseInt(valObj.integerValue);
                  else if ('booleanValue' in valObj) res[key] = valObj.booleanValue;
                  else if ('timestampValue' in valObj) res[key] = valObj.timestampValue;
                }
                return res;
              };
              foundData = mapFields(doc.fields);
            }
          }
        } catch (restErr) {
          console.error("REST API fallback failed:", restErr);
        }
      }

      if (!foundData) {
        return { active: false, error: "MAC non activée. Veuillez l'ajouter." };
      }
      
      if (foundData.expiryDate) {
        const expiry = new Date(foundData.expiryDate);
        if (expiry < new Date()) {
          return { active: false, error: "Abonnement expiré. Veuillez le prolonger." };
        }
      }
      
      return { active: true, activation: { id: foundId, ...foundData } };
    } catch (error: any) {
      console.error('Check MAC error:', error);
      return { active: false, error: "Erreur de connexion Firebase." };
    }
  },

  async sendHeartbeat(data: { mac: string; system?: string; version?: string; country?: string; channel?: string }) {
    try {
      await nativeFetch('/api/activations/heartbeat', {
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
