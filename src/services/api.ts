import { 
  db, 
  collection, 
  doc, 
  getDoc, 
  getDocs, 
  setDoc, 
  addDoc, 
  updateDoc, 
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
      if (!activation.resellerId) throw new Error('Reseller ID is required');
      
      const resellerDoc = doc(db, 'users', activation.resellerId);
      const resellerSnap = await getDoc(resellerDoc);
      if (!resellerSnap.exists()) throw new Error('Reseller not found');
      
      const resellerData = resellerSnap.data();
      const creditsUsed = activation.credits_used || 0;
      
      if (resellerData.credits < creditsUsed) {
        throw new Error('Crédits insuffisants');
      }
      
      // Atomic-ish update (should be a transaction in real app)
      await updateDoc(resellerDoc, {
        credits: resellerData.credits - creditsUsed
      });
      
      const newActivation = {
        ...activation,
        createdAt: serverTimestamp(),
        last_connection: serverTimestamp()
      };
      
      const docRef = await addDoc(collection(db, 'activations'), newActivation);
      return { id: docRef.id, ...newActivation };
    } catch (error) {
      handleFirestoreError(error, OperationType.CREATE, 'activations');
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
      const q = query(collection(db, 'activations'), where('target_mac', '==', mac));
      const snapshot = await getDocs(q);
      if (snapshot.empty) return { active: false, error: "MAC non trouvée" };
      
      const data = snapshot.docs[0].data();
      return { 
        active: true, 
        activation: { id: snapshot.docs[0].id, ...data } 
      };
    } catch (error: any) {
      console.error('Check MAC error:', error);
      return { active: false, error: error.message };
    }
  },

  async updateCurrentChannel(mac: string, channelName: string) {
    try {
      const q = query(collection(db, 'activations'), where('target_mac', '==', mac));
      const snapshot = await getDocs(q);
      if (!snapshot.empty) {
        const docRef = doc(db, 'activations', snapshot.docs[0].id);
        await updateDoc(docRef, {
          current_channel: channelName,
          last_connection: serverTimestamp()
        });
      }
    } catch (error) {
      console.error('Update channel error:', error);
    }
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
