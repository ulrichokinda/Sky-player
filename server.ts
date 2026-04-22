import 'dotenv/config';
import express from 'express';
import cors from 'cors';
import path from 'path';
import { fileURLToPath } from 'url';
import admin from 'firebase-admin';
import { getFirestore } from 'firebase-admin/firestore';
import fs from 'fs';

console.log('--- SERVER STARTING UP ---');
console.log('Node Version:', process.version);
console.log('CWD:', process.cwd());

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Read firebase-applet-config.json
let firebaseConfig: any = {};
try {
  const configPath = path.resolve(process.cwd(), 'firebase-applet-config.json');
  if (fs.existsSync(configPath)) {
    firebaseConfig = JSON.parse(fs.readFileSync(configPath, 'utf8'));
  }
} catch (e) {
  console.error('Could not read firebase-applet-config.json', e);
}

// Resolution order: Config file > Environment Variable > Cloud Run Environment
const projectId = firebaseConfig.projectId || process.env.VITE_FIREBASE_PROJECT_ID || process.env.GOOGLE_CLOUD_PROJECT || process.env.GCLOUD_PROJECT;
const databaseId = firebaseConfig.firestoreDatabaseId || '(default)';

// Initialize Firebase Admin
try {
  let credential;
  
  if (process.env.FIREBASE_SERVICE_ACCOUNT) {
    const saContent = process.env.FIREBASE_SERVICE_ACCOUNT.trim();
    if (saContent.startsWith('{')) {
      try {
        credential = admin.credential.cert(JSON.parse(saContent));
        console.log('Using FIREBASE_SERVICE_ACCOUNT from Secrets for authentication');
      } catch (jsonErr: any) {
        console.error('CRITICAL ERROR: FIREBASE_SERVICE_ACCOUNT is NOT a valid JSON.');
        console.error('Error Details:', jsonErr.message);
        console.error('Check your secret in Settings > Secrets. Ensure it starts with { and ends with } and has no extra spaces.');
      }
    } else {
      console.error('CRITICAL ERROR: FIREBASE_SERVICE_ACCOUNT secret does not start with "{". It appears to be invalid.');
    }
  }

  const options: admin.AppOptions = {};
  if (credential) {
    options.credential = credential;
  }
  
  if (projectId) {
    options.projectId = projectId;
  }

  if (admin.apps.length === 0) {
    admin.initializeApp(options);
    console.log(`Firebase Admin initialized. Target Project: ${projectId || 'detected-by-adc'}. Target DB: ${databaseId}`);
  }
} catch (error) {
  console.error('CRITICAL: Firebase Admin initialization failed:', error);
}

async function startServer() {
  const app = express();
  const VERSION = '4.0.0-ULTRA';
  
  app.use(cors());
  app.use(express.json());

  // Safe Firestore accessor
  const getDb = () => {
    try {
      return getFirestore(admin.app(), databaseId);
    } catch (e) {
      console.error("Firestore not ready:", e);
      return null;
    }
  };

  // Health check
  app.get('/api/health', (req, res) => {
    res.json({ 
      status: 'ok', 
      version: VERSION, 
      timestamp: new Date().toISOString(),
      dbReady: !!getDb()
    });
  });

  // --- YABETOO PAY API INTEGRATION ---
  
  // Initialize Yabetoo Transaction & Request Payment
  app.post("/api/payments/yabetoo/init", async (req, res) => {
    const { userId, amount, credits, phone, network, description } = req.body;
    
    try {
      const firestore = getDb();
      if (!firestore) throw new Error("Base de données non accessible");
      
      if (!userId || !amount || !phone || !network) {
        return res.status(400).json({ error: "Paramètres manquants (userId, amount, credits, phone, network)" });
      }

      const API_KEY = process.env.YABETOO_API_KEY;
      if (!API_KEY) {
        throw new Error("Yabetoo API Key non configurée sur le serveur");
      }

      // 1. Create a payment record in Firestore to track status
      const paymentRef = await firestore.collection('payments').add({
        userId,
        amount: parseFloat(amount),
        credits_purchased: parseInt(credits) || 0,
        phone,
        network,
        status: 'PENDING',
        provider: 'YABETOO',
        externalId: null, // Will be filled with Yabetoo's ID
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        description: description || 'Achat de crédits'
      });

      // 2. Call Yabetoo API (Collection)
      // Documentation typically expects: amount, phone, network, external_id, callback_url
      const yabetooResponse = await fetch('https://api.yabetoopay.com/v1/payment/collect', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${API_KEY}`
        },
        body: JSON.stringify({
          amount: parseFloat(amount),
          phone: phone.replace('+', ''), // Remove + if present
          network: network.toUpperCase(),
          external_id: paymentRef.id,
          description: description || 'Achat de crédits Sky Player',
          callback_url: `https://${req.get('host')}/api/payments/yabetoo/webhook`
        })
      });

      const data: any = await yabetooResponse.json();

      if (!yabetooResponse.ok) {
        console.error('Yabetoo Error Output:', data);
        await paymentRef.update({ status: 'FAILED', error: data.message || 'Erreur Yabetoo' });
        throw new Error(data.message || "Erreur lors de l'appel à Yabetoo");
      }

      // Update payment with provider's transaction ID
      await paymentRef.update({ 
        externalId: data.transaction_id || data.id,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });

      res.json({ 
        success: true, 
        paymentId: paymentRef.id,
        message: "Demande de paiement envoyée. Veuillez valider sur votre téléphone." 
      });

    } catch (error: any) {
      console.error('Yabetoo Init Error:', error);
      res.status(500).json({ error: error.message || "Erreur lors de l'initialisation du paiement" });
    }
  });

  // Verify Yabetoo Payment Status
  app.get("/api/payments/yabetoo/status/:paymentId", async (req, res) => {
    const { paymentId } = req.params;
    
    try {
      const firestore = getDb();
      if (!firestore) throw new Error("Base de données non accessible");
      
      const paymentDoc = await firestore.collection('payments').doc(paymentId).get();
      if (!paymentDoc.exists) {
        return res.status(404).json({ error: "Paiement non trouvé" });
      }

      const paymentData = paymentDoc.data();
      if (paymentData?.status === 'SUCCESS') {
        return res.json({ status: 'SUCCESS', message: 'Déjà validé' });
      }

      const API_KEY = process.env.YABETOO_API_KEY;
      const externalId = paymentData?.externalId;

      if (!externalId) {
        return res.json({ status: 'PENDING', message: 'ID externe manquant' });
      }

      // Check with Yabetoo
      const yabetooCheck = await fetch(`https://api.yabetoopay.com/v1/payment/verify/${externalId}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${API_KEY}`
        }
      });

      const data: any = await yabetooCheck.json();

      if (data.status === 'SUCCESS' || data.status === 'COMPLETED') {
        // --- CRITICAL: Atomic Credit Addition ---
        const userRef = firestore.collection('users').doc(paymentData?.userId);
        
        await firestore.runTransaction(async (transaction) => {
          const userDoc = await transaction.get(userRef);
          if (!userDoc.exists) throw new Error("Utilisateur introuvable");

          const currentCredits = userDoc.data()?.credits || 0;
          const addedCredits = paymentData?.credits_purchased || 0; 

          transaction.update(userRef, {
            credits: currentCredits + addedCredits
          });

          transaction.update(paymentDoc.ref, {
            status: 'SUCCESS',
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
          });
        });

        return res.json({ status: 'SUCCESS', credits_added: true });
      }

      res.json({ status: data.status || 'PENDING' });

    } catch (error: any) {
      console.error('Yabetoo Status Check Error:', error);
      res.status(500).json({ error: error.message });
    }
  });

  // --- END YABETOO PAY ---

  // --- MONEYFUSION (FUSION PAY) ---
  app.post("/api/payments/moneyfusion/init", async (req, res) => {
    const { userId, amount, credits, phone, network, description } = req.body;
    try {
      const firestore = getDb();
      if (!firestore) throw new Error("Base de données non accessible");
      
      const MERCHANT_ID = process.env.MONEYFUSION_MERCHANT_ID;
      const API_KEY = process.env.MONEYFUSION_API_KEY;

      if (!MERCHANT_ID || !API_KEY) {
        throw new Error("MoneyFusion non configuré sur le serveur (Merchant ID ou API Key manquant)");
      }

      const paymentRef = await firestore.collection('payments').add({
        userId,
        amount: parseFloat(amount),
        credits_purchased: parseInt(credits) || 0,
        phone,
        network,
        status: 'PENDING',
        provider: 'MONEYFUSION',
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });

      // Simulation of MoneyFusion API call
      // In production, this would call fusionpay.net API
      // For now, we return a success with the payment ID to show we're ready
      res.json({
        success: true,
        paymentId: paymentRef.id,
        message: "Initialisation MoneyFusion terminée. En attente de validation."
      });
    } catch (error: any) {
      console.error('MoneyFusion Init Error:', error);
      res.status(500).json({ error: error.message });
    }
  });

  app.post("/api/payments/moneyfusion/webhook", async (req, res) => {
    const payload = req.body;
    // Typical payload: { transaction_id, status, external_reference, amount }
    try {
      const firestore = getDb();
      if (!firestore) throw new Error("Base de données non accessible");
      
      const paymentId = payload.external_reference;
      const paymentDoc = await firestore.collection('payments').doc(paymentId).get();
      
      if (paymentDoc.exists && payload.status === 'success') {
        const paymentData = paymentDoc.data();
        if (paymentData?.status !== 'SUCCESS') {
          const userRef = firestore.collection('users').doc(paymentData?.userId);
          
          await firestore.runTransaction(async (transaction) => {
            const userDoc = await transaction.get(userRef);
            const currentCredits = userDoc.data()?.credits || 0;
            const addedCredits = paymentData?.credits_purchased || 0;

            transaction.update(userRef, { credits: currentCredits + addedCredits });
            transaction.update(paymentDoc.ref, { 
              status: 'SUCCESS', 
              updatedAt: admin.firestore.FieldValue.serverTimestamp() 
            });
          });
        }
      }
      res.status(200).send('OK');
    } catch (error) {
      console.error('MoneyFusion Webhook Error:', error);
      res.status(500).send('Error');
    }
  });
  // --- END MONEYFUSION ---

  // Create Activation with Credit Deduction (Secure)
  app.post("/api/activations/create", async (req, res) => {
    const { resellerId, target_mac, credits_used, note, playlist_url, xtream_host, xtream_username, xtream_password } = req.body;
    const effectiveDbId = databaseId || '(default)';
    console.log(`[API] Creation Activation request for MAC: ${target_mac} by Reseller: ${resellerId} on DB: ${effectiveDbId}`);
    
    try {
      const firestore = getDb();
      if (!firestore) {
        console.error("[API] Firestore not initialized. Check service account secret.");
        return res.status(503).json({ error: "Base de données non accessible. Vérifiez la configuration Firebase dans les réglages." });
      }

      const normalizedMac = target_mac ? target_mac.toUpperCase().trim() : '';
      if (!normalizedMac) return res.status(400).json({ error: "L'adresse MAC est obligatoire" });
      
      // 0. Check for existing activation with this MAC to avoid duplicates
      const existingActs = await firestore.collection('activations').where('target_mac', '==', normalizedMac).get();
      if (!existingActs.empty) {
        return res.status(400).json({ error: `L'appareil avec la MAC ${normalizedMac} est déjà activé.` });
      }

      // 1. Transactional Credit Deduction...
      if (credits_used > 0 && resellerId && resellerId !== 'SYSTEM_TRIAL') {
        const userRef = firestore.collection('users').doc(resellerId);
        
        await firestore.runTransaction(async (transaction) => {
          const userDoc = await transaction.get(userRef);
          if (!userDoc.exists) throw new Error('Utilisateur non trouvé dans la base de données');
          
          const currentCredits = userDoc.data()?.credits || 0;
          if (currentCredits < credits_used) {
            throw new Error(`Crédits insuffisants (Solde: ${currentCredits}, Requis: ${credits_used})`);
          }
          
          transaction.update(userRef, {
            credits: currentCredits - credits_used
          });
        });
      }

      // 2. Create Activation
      const activationData = {
        resellerId,
        target_mac: normalizedMac,
        credits_used: credits_used || 0,
        note: note || 'Client manuel',
        playlist_url: playlist_url || '',
        xtream_host: xtream_host || '',
        xtream_username: xtream_username || '',
        xtream_password: xtream_password || '',
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        last_connection: admin.firestore.FieldValue.serverTimestamp(),
        system: 'N/A',
        version: 'N/A',
        country_code: 'N/A',
        current_channel: 'Hors-ligne'
      };
      
      const docRef = await firestore.collection('activations').add(activationData);
      
      console.log(`[API] Activation created successfully ID: ${docRef.id} for MAC ${normalizedMac}`);
      res.json({ success: true, id: docRef.id });
    } catch (error: any) {
      console.error('[API] Activation Create Error:', error);
      res.status(500).json({ error: error.message || 'Erreur lors de la création de l\'activation' });
    }
  });

  // Heartbeat / Device Info Update
  app.post("/api/activations/heartbeat", async (req, res) => {
    const { mac, system, version, country, channel } = req.body;
    
    try {
      const firestore = getDb();
      if (!firestore) throw new Error("Base de données non accessible");
      
      if (!mac) return res.status(400).json({ error: 'MAC est requise' });
      
      const normalizedMac = mac.toUpperCase().trim();
      const q = firestore.collection('activations').where('target_mac', '==', normalizedMac);
      const snapshot = await q.get();
      
      if (snapshot.empty) {
        return res.status(404).json({ error: 'Appareil non envoyé au serveur (non activé)' });
      }
      
      const docRef = snapshot.docs[0].ref;
      await docRef.update({
        system: system || 'Inconnu',
        version: version || 'Inconnu',
        country_code: country || 'N/A',
        current_channel: channel || 'Hors-ligne',
        last_connection: admin.firestore.FieldValue.serverTimestamp()
      });
      
      res.json({ success: true });
    } catch (error) {
      console.error('Heartbeat Error:', error);
      res.status(500).json({ error: 'Erreur lors de la mise à jour des infos' });
    }
  });

  // Payment Initiation Route (Server-side to protect keys)
  app.post("/api/payments/initiate", async (req, res) => {
    const { userId, amount, phoneNumber, credits_purchased, provider, methodId } = req.body;
    const depositId = Math.random().toString(36).substr(2, 9);

    try {
      const firestore = getDb();
      if (!firestore) throw new Error("Base de données non accessible");
      
      console.log(`Initiating ${provider} (${methodId}) deposit for ${phoneNumber} with amount ${amount}`);
      
      const externalId = Math.random().toString(36).substr(2, 9);
      
      // Record payment in Firestore (safe try/catch for AI Studio preview)
      try {
        const paymentData = {
          userId,
          amount,
          credits_purchased,
          payment_method: methodId,
          provider,
          status: provider === 'stripe' ? 'completed' : 'pending',
          external_id: externalId,
          createdAt: admin.firestore.FieldValue.serverTimestamp()
        };
        
        await firestore.collection('payments').add(paymentData);

        if (provider === 'bkapay') {
          // Bkapay uses a redirect flow
          return res.json({
            success: true,
            provider: 'bkapay',
            paymentUrl: `https://bkapay.com/pay?amount=${amount}&ref=${externalId}`, // Simulation du lien de paiement
            message: "Redirection vers Bkapay..."
          });
        }

        // If Stripe, update credits immediately (for demo/test)
        if (provider === 'stripe') {
          const userRef = firestore.collection('users').doc(userId);
          const userDoc = await userRef.get();
          if (userDoc.exists) {
            const currentCredits = userDoc.data()?.credits || 0;
            await userRef.update({
              credits: currentCredits + credits_purchased
            });
          }
        }
      } catch (e: any) {
        console.warn("Backend Firestore sync simulated due to missing rights (common in AI Studio):", e.message);
      }

      const message = provider === 'moneyfusion' 
        ? "Paiement MoneyFusion initié. Veuillez valider sur votre téléphone." 
        : (provider === 'stripe' ? "Paiement par carte validé (Simulation)..." : `Paiement via Yabetoo Pay (${methodId}) initié. Veuillez valider sur votre téléphone.`);

      res.json({ 
        success: true, 
        depositId,
        message,
        simulated: true
      });
    } catch (error) {
      console.error('Payment Error:', error);
      res.status(500).json({ error: 'Erreur lors de l\'initiation du paiement' });
    }
  });

  // Mount API Router fallback
  app.use('/api/*', (req, res) => {
    res.status(404).json({ error: `API route not found: ${req.originalUrl}` });
  });

  const isProd = process.env.NODE_ENV === 'production';
  console.log(`Environment: ${isProd ? 'PRODUCTION' : 'DEVELOPMENT'} (NODE_ENV: ${process.env.NODE_ENV})`);

  // Vite middleware for development
  if (!isProd) {
    console.log('Setting up Vite middleware for development...');
    const { createServer: createViteServer } = await import('vite');
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    // In production, we assume the server is running from dist/server.js
    // and assets are siblings in the same dist folder.
    const distPath = path.dirname(fileURLToPath(import.meta.url));
    console.log(`Setting up static serving for production from: ${distPath}`);
    
    app.use(express.static(distPath, {
      maxAge: '1d',
      setHeaders: (res, path) => {
        if (path.endsWith('.html')) {
          res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
          res.setHeader('Pragma', 'no-cache');
          res.setHeader('Expires', '0');
        }
      }
    }));
    app.get('*', (req, res) => {
      res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
      res.setHeader('Pragma', 'no-cache');
      res.setHeader('Expires', '0');
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  // Use process.env.PORT for external Cloud Run deployments where it dynamically assigns a port (e.g., 8080)
  // Fall back to 3000 for local development and AI Studio
  const PORT = process.env.PORT ? parseInt(process.env.PORT) : 3000;
  
  console.log(`Server starting on port: ${PORT}`);
  
  const server = app.listen(PORT, '0.0.0.0', () => {
    console.log(`Server successfully listening on 0.0.0.0:${PORT}`);
    console.log(`Ready to serve traffic.`);
  });

  server.on('error', (err) => {
    console.error('SERVER BINDING ERROR:', err);
    process.exit(1);
  });
}

startServer().catch(err => {
  console.error("FAILED TO START SERVER:", err);
  process.exit(1);
});
