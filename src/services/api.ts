export interface UserProfile {
  uid: string;
  email: string;
  username: string;
  role: 'admin' | 'reseller' | 'client';
  credits: number;
  phone?: string;
  country?: string;
  createdAt: string;
  trialStartedAt?: string;
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
  last_connection?: string;
  country_code?: string;
  createdAt: string;
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
  createdAt: string;
}

const STORAGE_KEYS = {
  USERS: 'ewo_users',
  ACTIVATIONS: 'ewo_activations',
  PAYMENTS: 'ewo_payments'
};

const getStorage = (key: string) => JSON.parse(localStorage.getItem(key) || '[]');
const setStorage = (key: string, data: any) => localStorage.setItem(key, JSON.stringify(data));

export const isTrialExpired = (trialStartedAt?: string) => {
  if (!trialStartedAt) return true;
  const trialDuration = 21 * 24 * 60 * 60 * 1000;
  return new Date().getTime() - new Date(trialStartedAt).getTime() > trialDuration;
};

export const api = {
  async registerUser(userData: Partial<UserProfile>) {
    const users = getStorage(STORAGE_KEYS.USERS);
    const existingIndex = users.findIndex((u: any) => u.uid === userData.uid);
    const newUser = {
      ...userData,
      credits: userData.credits || 0,
      createdAt: new Date().toISOString(),
      trialStartedAt: userData.trialStartedAt || new Date().toISOString(),
      isPremium: userData.isPremium || false,
      role: userData.role || 'client'
    };
    
    if (existingIndex > -1) {
      users[existingIndex] = { ...users[existingIndex], ...newUser };
    } else {
      users.push(newUser);
    }
    
    setStorage(STORAGE_KEYS.USERS, users);
    return newUser;
  },

  async getUser(uid: string): Promise<UserProfile> {
    const users = getStorage(STORAGE_KEYS.USERS);
    const user = users.find((u: any) => u.uid === uid);
    if (!user) throw new Error('User not found');
    return user;
  },

  async updateUser(uid: string, data: Partial<UserProfile>) {
    const users = getStorage(STORAGE_KEYS.USERS);
    const index = users.findIndex((u: any) => u.uid === uid);
    if (index === -1) throw new Error('User not found');
    
    users[index] = { ...users[index], ...data };
    setStorage(STORAGE_KEYS.USERS, users);
    return users[index];
  },

  async getActivations(resellerId: string): Promise<Activation[]> {
    const activations = getStorage(STORAGE_KEYS.ACTIVATIONS);
    return activations.filter((a: any) => a.resellerId === resellerId);
  },

  async createActivation(activation: Partial<Activation>) {
    const activations = getStorage(STORAGE_KEYS.ACTIVATIONS);
    const users = getStorage(STORAGE_KEYS.USERS);
    
    const resellerIndex = users.findIndex((u: any) => u.uid === activation.resellerId);
    if (resellerIndex === -1) throw new Error('Reseller not found');
    
    const creditsUsed = activation.credits_used || 0;
    if (users[resellerIndex].credits < creditsUsed) {
      throw new Error('Crédits insuffisants');
    }
    
    // Deduct credits
    users[resellerIndex].credits -= creditsUsed;
    setStorage(STORAGE_KEYS.USERS, users);
    
    const newActivation = {
      ...activation,
      id: Math.random().toString(36).substr(2, 9),
      createdAt: new Date().toISOString()
    };
    
    activations.push(newActivation);
    setStorage(STORAGE_KEYS.ACTIVATIONS, activations);
    return newActivation;
  },

  async getPayments(userId: string): Promise<Payment[]> {
    const payments = getStorage(STORAGE_KEYS.PAYMENTS);
    return payments.filter((p: any) => p.userId === userId);
  },

  async createPayment(payment: Partial<Payment>) {
    const payments = getStorage(STORAGE_KEYS.PAYMENTS);
    const users = getStorage(STORAGE_KEYS.USERS);
    
    const newPayment = {
      ...payment,
      id: Math.random().toString(36).substr(2, 9),
      createdAt: new Date().toISOString()
    };
    
    payments.push(newPayment);
    setStorage(STORAGE_KEYS.PAYMENTS, payments);
    
    if (newPayment.status === 'completed') {
      const userIndex = users.findIndex((u: any) => u.uid === payment.userId);
      if (userIndex > -1) {
        users[userIndex].credits += (payment.credits_purchased || 0);
        setStorage(STORAGE_KEYS.USERS, users);
      }
    }
    
    return newPayment;
  },

  async initiateMoneyFusion(data: { userId: string; amount: number; phoneNumber: string; credits_purchased: number }) {
    // Simulate MoneyFusion initiation
    const depositId = Math.random().toString(36).substr(2, 9);
    
    await this.createPayment({
      userId: data.userId,
      amount: data.amount,
      credits_purchased: data.credits_purchased,
      payment_method: 'moneyfusion',
      provider: 'moneyfusion',
      status: 'pending',
      external_id: depositId
    });
    
    return {
      success: true,
      depositId,
      message: "Paiement MoneyFusion initié. Veuillez valider sur votre téléphone."
    };
  },

  async initiateYabetooPay(data: { userId: string; amount: number; phoneNumber: string; credits_purchased: number; methodId: string }) {
    // Simulate YabetooPay initiation
    const depositId = Math.random().toString(36).substr(2, 9);
    
    await this.createPayment({
      userId: data.userId,
      amount: data.amount,
      credits_purchased: data.credits_purchased,
      payment_method: data.methodId,
      provider: 'yabetoopay',
      status: 'pending',
      external_id: depositId
    });
    
    return {
      success: true,
      depositId,
      message: `Paiement ${data.methodId} via YabetooPay initié. Veuillez valider sur votre téléphone.`
    };
  },

  async checkMacStatus(mac: string) {
    const activations = getStorage(STORAGE_KEYS.ACTIVATIONS);
    const activation = activations.find((a: any) => a.target_mac === mac);
    
    if (activation) {
      const expiryDate = new Date(new Date(activation.createdAt).getTime() + 365 * 24 * 60 * 60 * 1000);
      return {
        active: true,
        expiry: expiryDate.toLocaleDateString('fr-FR'),
        last_seen: activation.last_connection || '2024-03-09 14:22',
        version: activation.version || 'v3.2.1'
      };
    } else {
      return {
        active: false,
        error: "Adresse MAC non trouvée dans notre base d'activations."
      };
    }
  }
};
