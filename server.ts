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
      credential = admin.credential.cert(JSON.parse(saContent));
      console.log('Using FIREBASE_SERVICE_ACCOUNT from Secrets for authentication');
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
  const firestore = getFirestore(admin.app(), databaseId);
  const app = express();
  const VERSION = '4.0.0-ULTRA';
  console.log(`Starting Sky Player Server v${VERSION}...`);
  console.log(`Environment: ${process.env.NODE_ENV || 'development'}`);
  
  app.use(cors());
  app.use(express.json());

  // Health check
  app.get('/api/health', (req, res) => {
    res.json({ status: 'ok', version: VERSION, timestamp: new Date().toISOString() });
  });

  // --- YABETOO PAY API INTEGRATION ---
  
  // Initialize Yabetoo Transaction & Request Payment
  app.post("/api/payments/yabetoo/init", async (req, res) => {
    const { userId, amount, credits, phone, network, description } = req.body;
    
    try {
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
    
    try {
      const normalizedMac = target_mac.toUpperCase().trim();
      
      // 1. Transactional Credit Deduction (Only if not a trial and credits > 0)
      if (credits_used > 0 && resellerId && resellerId !== 'SYSTEM_TRIAL') {
        const userRef = firestore.collection('users').doc(resellerId);
        
        await firestore.runTransaction(async (transaction) => {
          const userDoc = await transaction.get(userRef);
          if (!userDoc.exists) throw new Error('Utilisateur non trouvé');
          
          const currentCredits = userDoc.data()?.credits || 0;
          if (currentCredits < credits_used) {
            throw new Error('Crédits insuffisants');
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
        credits_used,
        note: note || 'Activation manuelle',
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
      
      console.log(`Successfully created activation for MAC ${normalizedMac}. Client: ${note}`);
      res.json({ success: true, id: docRef.id });
    } catch (error: any) {
      console.error('Activation Create Error:', error);
      res.status(500).json({ error: error.message || 'Erreur lors de la création de l\'activation' });
    }
  });

  // Heartbeat / Device Info Update
  app.post("/api/activations/heartbeat", async (req, res) => {
    const { mac, system, version, country, channel } = req.body;
    
    try {
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
      console.log(`Initiating ${provider} (${methodId}) deposit for ${phoneNumber} with amount ${amount}`);
      
      const externalId = Math.random().toString(36).substr(2, 9);
      
      // Record payment in Firestore
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

      const message = provider === 'moneyfusion' 
        ? "Paiement MoneyFusion initié. Veuillez valider sur votre téléphone." 
        : (provider === 'stripe' ? "Redirection vers la passerelle de paiement par carte (Stripe)..." : `Paiement via Yabetoo Pay (${methodId}) initié. Veuillez valider sur votre téléphone.`);

      res.json({ 
        success: true, 
        depositId,
        message 
      });
    } catch (error) {
      console.error('Payment Error:', error);
      res.status(500).json({ error: 'Erreur lors de l\'initiation du paiement' });
    }
  });

  // Vite middleware for development
  if (process.env.NODE_ENV !== 'production') {
    const { createServer } = await import('vite');
    const vite = await createServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    // In production, we assume the server is running from dist/server.js
    // and assets are siblings in the same dist folder.
    const __currentDir = path.dirname(fileURLToPath(import.meta.url));
    const distPath = __currentDir;
    
    console.log(`Production assets path: ${distPath}`);
    
    app.use(express.static(distPath, {
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

  // Cloud Run provides the PORT environment variable. AI Studio proxy uses 3000.
  let PORT = 3000;
  if (process.env.PORT) {
    const parsed = parseInt(process.env.PORT);
    if (!isNaN(parsed)) {
      PORT = parsed;
    }
  }
  
  console.log(`Port resolved to: ${PORT}`);
  
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
