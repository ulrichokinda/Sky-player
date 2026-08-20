import 'dotenv/config';
import express from 'express';
import cors from 'cors';
import path from 'path';
import admin from 'firebase-admin';
import { getFirestore } from 'firebase-admin/firestore';
import fs from 'fs';
import crypto from 'crypto';

console.log('--- SERVER STARTING UP ---');
console.log('Node Version:', process.version);
console.log('PORT environment variable:', process.env.PORT);
console.log('CWD:', process.cwd());

process.on('uncaughtException', (err) => {
  console.error('UNCAUGHT EXCEPTION:', err);
  process.exit(1);
});

process.on('unhandledRejection', (reason, promise) => {
  console.error('UNHANDLED REJECTION:', reason);
});

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

// Initialize Firebase Admin (now lazy)
try {
  // Config reader remaining for project discovery
  console.log('Firebase configuration ready based on context, will initialize lazily.');
} catch (error) {
  console.error('CRITICAL: Firebase configuration discovery failed:', error);
}

async function startServer() {
  const app = express();
  const VERSION = '4.0.0-ULTRA';
  
  app.use(cors());
  app.use(express.json());

  // Lazy initialization of Firebase Admin
  let adminApp: admin.app.App | null = null;
  const getDb = () => {
    try {
      if (!adminApp) {
        if (admin.apps.length > 0) {
          adminApp = admin.apps[0]!;
        } else {
          const options: admin.AppOptions = {};
          if (process.env.FIREBASE_SERVICE_ACCOUNT) {
            try {
              options.credential = admin.credential.cert(JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT.trim()));
            } catch (jsonErr) {
              console.error('FIREBASE_SERVICE_ACCOUNT JSON invalid');
            }
          }
          if (projectId) options.projectId = projectId;
          adminApp = admin.initializeApp(options);
        }
      }
      return getFirestore(adminApp, databaseId);
    } catch (e) {
      console.error("Firestore not ready:", e);
      return null;
    }
  };

  // Middleware/helper de validation pour l'application Android Sky Player Pro
  const validateActivationApiKey = (req: any, res: any, next: any) => {
    const keyInEnv = process.env.ACTIVATION_API_KEY || "SKY-PRO-SECURE-V1-A7D29F4E8C1B0A3D6F5E9D8C7B6A5F4E3D2C1B0A9F8E7D6C5B4A3F2E1D0C9B8A";
    const headerKey = req.headers['x-activation-api-key'] || req.headers['x-api-key'] || req.query.api_key;
    
    // Liste des clés historiquement valides ou de production pour éviter les ruptures de communication
    const validKeys = [
      keyInEnv,
      "SKY-PRO-SECURE-V1-A7D29F4E8C1B0A3D6F5E9D8C7B6A5F4E3D2C1B0A9F8E7D6C5B4A3F2E1D0C9B8A",
      "skyplayer_promax_activation_key_2026",
      "skyplayer_promax_secure_prod_key_2026"
    ].filter(Boolean);

    const isValid = headerKey && validKeys.includes(headerKey);

    // Pour la route d'obtention de la playlist, on accepte d'être tolérant (car certains lecteurs M3U / XC standard ne peuvent pas envoyer d'en-têtes HTTP)
    const isPlaylistRoute = req.path.includes('/api/v1/playlist');

    if (!isValid && !isPlaylistRoute) {
      console.warn(`[SECURITY] REJET D'ACCÈS: Route Android ${req.baseUrl || ''}${req.path} appelée de l'IP ${req.ip} sans clé ACTIVATION_API_KEY valide.`);
      return res.status(401).json({ 
        error: "Accès refusé", 
        message: "Clé 'ACTIVATION_API_KEY' invalide ou absente dans les en-têtes (X-Activation-API-Key)." 
      });
    }

    if (isPlaylistRoute && !isValid) {
      console.log(`[PLAYLIST] Accès tolérant pour la récupération de playlist sans en-tête d'IP ${req.ip}.`);
    }

    next();
  };

  // Health check - kept extremely simple for reliability
  app.get('/api/health', (req, res) => {
    res.status(200).send('ok');
  });

  // --- YABETOO PAY API INTEGRATION ---
  

  // --- JOBOOST-CASH API INTEGRATION ---
  
  // 1. Route de Création de Paiement
  app.post("/api/v1/payments/checkout", async (req, res) => {
    const { target_mac, userId, amount, credits_purchased, phoneNumber, plan_id, email, playlist_url, xtream_host, xtream_username, xtream_password } = req.body;
    
    try {
      const firestore = getDb();
      if (!firestore) throw new Error("Base de données non accessible");
      
      const MERCHANT_ID = process.env.JOBOOST_MERCHANT_ID;
      const API_KEY = process.env.JOBOOST_API_KEY;

      console.log(`[DEBUG] MERCHANT_ID is set: ${!!MERCHANT_ID}`);
      console.log(`[DEBUG] API_KEY is set: ${!!API_KEY}`);

      if (!MERCHANT_ID || !API_KEY) {
        console.error("[API] [CRITICAL] Échec de l'appel : JOBOOST_MERCHANT_ID ou JOBOOST_API_KEY non configuré.");
        return res.status(500).json({ 
          error: "Configuration non valide", 
          message: "JOboost-Cash n'est pas configuré pour la production (Clés d'API manquantes)." 
        });
      }

      if (!amount) {
         return res.status(400).json({ error: "Le montant est requis." });
      }

      const isCreditPurchase = !!userId && !!credits_purchased;
      const isMacActivation = !!target_mac;

      if (!isCreditPurchase && !isMacActivation) {
        return res.status(400).json({ error: "L'adresse MAC ou les détails d'achat de crédits sont requis." });
      }

      const normalizedMac = isMacActivation ? normalizeMac(target_mac) : null;

      // 1. Create a payment record in Firestore to track status
      const paymentRef = await firestore.collection('payments').add({
        target_mac: normalizedMac,
        userId: userId || null,
        credits_purchased: credits_purchased || null,
        amount: parseFloat(amount),
        plan_id: plan_id || null,
        phoneNumber: phoneNumber || null,
        email: email || null,
        playlist_url: playlist_url || null,
        xtream_host: xtream_host || null,
        xtream_username: xtream_username || null,
        xtream_password: xtream_password || null,
        provider: 'joboost',
        status: 'PENDING',
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      });

      console.log(`[API] JOboost Checkout init - Type: ${isCreditPurchase ? 'Credits' : 'MAC Activation'}, Montant: ${amount}, Ref: ${paymentRef.id}`);

      // 2. Appel à l'API JOboost-Cash
      console.log(`[API] Appel réel API JOboost-Cash pour le paiement de ${amount} XAF`);
      const description = isCreditPurchase ? `Achat de ${credits_purchased} crédits revendeur` : `Abonnement Sky Player Pro pour MAC: ${normalizedMac}`;
      
      let response;
      try {
        response = await fetch('https://api.joboost-cash.com/v1/checkout', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${API_KEY}`
          },
          body: JSON.stringify({
            merchant_id: MERCHANT_ID,
            amount: parseFloat(amount),
            currency: 'XAF',
            reference: paymentRef.id,
            description: description,
            phone: phoneNumber || '',
            email: email || '',
            return_url: `https://${req.get('host')}/payment-success?ref=${paymentRef.id}`,
            cancel_url: `https://${req.get('host')}/payment-cancel?ref=${paymentRef.id}`,
            callback_url: `https://${req.get('host')}/api/v1/payments/webhook-joboost`
          })
        });
      } catch (fetchError) {
        console.error(`[API] Erreur de connexion réseau à l'API JOboost:`, fetchError);
        return res.status(500).json({ error: "Erreur de connexion au serveur de paiement." });
      }

      if (!response.ok) {
        const errorText = await response.text();
        console.error(`[API] Échec de l'appel API JOboost client (${response.status}):`, errorText);
        return res.status(response.status).json({
          error: "Échec de l'initialisation du paiement chez JOboost-Cash",
          details: errorText
        });
      }

      const data: any = await response.json();
      if (!data.success && !data.payment_url) {
        console.error('[API] Réponse JOboost valide mais sans URL de paiement ou succès', data);
        return res.status(400).json({
          error: "Réponse inattendue de JOboost-Cash (succès absent)",
          details: data
        });
      }

      const checkoutUrl = data.payment_url || `https://pay.joboost-cash.com/checkout/${paymentRef.id}`;
      const transactionId = data.transaction_id || data.reference || `JB-${new Date().getTime()}`;
      
      await paymentRef.update({
        payment_url: checkoutUrl,
        transaction_id: transactionId
      });
      
      console.log(`[API] JOboost Checkout initialisé avec succès : URL=${checkoutUrl}, TransID=${transactionId}`);

      res.json({
        success: true,
        payment_url: checkoutUrl,
        transaction_id: transactionId,
        reference: paymentRef.id
      });

    } catch (error: any) {
      console.error('[API] JOboost Checkout Error:', error);
      res.status(500).json({ error: error.message });
    }
  });

  // 2. Configuration du Webhook (Notification de paiement)
  app.post(["/api/v1/payments/webhook-joboost", "/webhook"], async (req, res) => {
    const payload = req.body;
    
    console.log('[API] JOboost Webhook payload reçu :', JSON.stringify(payload));

    try {
      const firestore = getDb();
      if (!firestore) throw new Error("Base de données non accessible");
      
      const SECRET_KEY = process.env.JOBOOST_SECRET_KEY;
      if (!SECRET_KEY) {
        console.error("[SECURITY] [CRITICAL] JOBOOST_SECRET_KEY n'est pas configuré. Webhook rejeté pour raison de sécurité.");
        return res.status(500).json({ error: "Configuration de sécurité webhook manquante sur le serveur." });
      }
      
      // Sécurité : Validation de la signature ou du token fourni par JOboost
      const signatureHeader = req.headers['x-joboost-signature'] || req.headers['x-signature'] || payload.signature;
      
      const computedSignature = crypto.createHmac('sha256', SECRET_KEY)
        .update(JSON.stringify(payload))
        .digest('hex');
        
      if (!signatureHeader || (signatureHeader !== computedSignature && signatureHeader !== SECRET_KEY)) {
        console.warn(`[SECURITY] Signature KO pour le Webhook JOboost. Reçue: ${signatureHeader || 'aucune'}, Attendue: ${computedSignature}`);
        return res.status(401).json({ error: "Signature de paiement non valide ou manquante." });
      }
      console.log(`[SECURITY] Signature Webhook JOboost validée avec succès.`);
      
      // On s'attend à ce que JOboost renvoie la référence envoyée et le statut de la transaction
      const paymentId = payload.reference || payload.external_id || payload.transaction_id;
      const status = payload.status; // e.g., 'SUCCESS', 'FAILED'

      if (!paymentId) {
         console.error('[API] Webhook JOboost ignoré : missing reference/paymentId');
         return res.status(400).json({ error: "Reference manquante" });
      }

      const paymentDoc = await firestore.collection('payments').doc(paymentId).get();
      
      if (paymentDoc.exists) {
        const paymentData = paymentDoc.data()!;
        
        if (paymentData.status !== 'SUCCESS' && status === 'SUCCESS') {
           // a. Marquer le paiement comme complété
           await paymentDoc.ref.update({ 
               status: 'SUCCESS', 
               completedAt: admin.firestore.FieldValue.serverTimestamp() 
           });
           
           if (paymentData.credits_purchased && paymentData.userId) {
              console.log(`[API] JOboost transaction validée (SUCCESS). Achat de ${paymentData.credits_purchased} crédits pour le revendeur ${paymentData.userId}`);
              
              const userRef = firestore.collection('users').doc(paymentData.userId);
              const userDoc = await userRef.get();
              if (userDoc.exists) {
                const currentCredits = userDoc.data()?.credits || 0;
                await userRef.update({
                  credits: currentCredits + paymentData.credits_purchased
                });
                console.log(`[JOBOOST-SUPPORT] ${paymentData.credits_purchased} crédits ajoutés au revendeur ${paymentData.userId}.`);
              } else {
                console.error(`[JOBOOST-SUPPORT] Revendeur ${paymentData.userId} introuvable pour l'attribution des crédits.`);
              }
           } else if (paymentData.target_mac) {
              const normalizedMac = paymentData.target_mac;
              console.log(`[API] JOboost transaction validée (SUCCESS). Activation de la MAC: ${normalizedMac}`);

              // b. Activer l'appareil lié à la MAC address reçue
              const deviceRef = firestore.collection('devices').doc(normalizedMac);
              const deviceDoc = await deviceRef.get();
              
              // Activation / Renouvellement premium (1 an / 365 jours par exemple)
              const oneYearLater = new Date();
              oneYearLater.setFullYear(oneYearLater.getFullYear() + 1);

              const updatePayload = {
                 is_active: true,
                 activated_at: admin.firestore.FieldValue.serverTimestamp(),
                 expiry_date: oneYearLater,
                 payment_ref: paymentId,
                 playlist_url: paymentData.playlist_url || (deviceDoc.exists ? deviceDoc.data()?.playlist_url : '') || '',
                 xtream_host: paymentData.xtream_host || (deviceDoc.exists ? deviceDoc.data()?.xtream_host : '') || '',
                 xtream_username: paymentData.xtream_username || (deviceDoc.exists ? deviceDoc.data()?.xtream_username : '') || '',
                 xtream_password: paymentData.xtream_password || (deviceDoc.exists ? deviceDoc.data()?.xtream_password : '') || ''
              };

              if (deviceDoc.exists) {
                 await deviceRef.update(updatePayload);
              } else {
                 await deviceRef.set({
                    mac_address: normalizedMac,
                    first_launch: admin.firestore.FieldValue.serverTimestamp(),
                    ...updatePayload
                 });
              }

              // Mettre à jour / Créer l'Activation pour les vérifications de statut (Android & Web App)
              const activationSnapshot = await firestore.collection('activations').where('target_mac', '==', normalizedMac).get();
              
              const activationData = {
                 resellerId: 'PAYMENT_GATEWAY_JOBOOST',
                 target_mac: normalizedMac,
                 credits_used: 0,
                 note: `Paiement automatique JOboost-Cash (Package: ${paymentData.plan_id || 'Premium'})`,
                 playlist_url: paymentData.playlist_url || '',
                 playlistUrl: paymentData.playlist_url || '',
                 xtream_host: paymentData.xtream_host || '',
                 xtreamServer: paymentData.xtream_host || '',
                 xtream_username: paymentData.xtream_username || '',
                 xtreamUser: paymentData.xtream_username || '',
                 xtream_password: paymentData.xtream_password || '',
                 xtreamPassword: paymentData.xtream_password || '',
                 createdAt: admin.firestore.FieldValue.serverTimestamp(),
                 expiryDate: oneYearLater.toISOString(),
                 last_connection: admin.firestore.FieldValue.serverTimestamp(),
                 system: 'N/A',
                 version: 'N/A',
                 country_code: 'N/A',
                 current_channel: 'Hors-ligne',
                 status: 'ACTIF'
              };

              if (!activationSnapshot.empty) {
                 const actDoc = activationSnapshot.docs[0];
                 await actDoc.ref.update({
                    expiryDate: oneYearLater.toISOString(),
                    status: 'ACTIF',
                    playlist_url: paymentData.playlist_url || actDoc.data()?.playlist_url || '',
                    playlistUrl: paymentData.playlist_url || actDoc.data()?.playlist_url || '',
                    xtream_host: paymentData.xtream_host || actDoc.data()?.xtream_host || '',
                    xtreamServer: paymentData.xtream_host || actDoc.data()?.xtream_host || '',
                    xtream_username: paymentData.xtream_username || actDoc.data()?.xtream_username || '',
                    xtreamUser: paymentData.xtream_username || actDoc.data()?.xtream_username || '',
                    xtream_password: paymentData.xtream_password || actDoc.data()?.xtream_password || '',
                    xtreamPassword: paymentData.xtream_password || actDoc.data()?.xtream_password || '',
                    note: `Renouvellement via JOboost-Cash`
                 });
                 console.log(`[JOBOOST-SUPPORT] Activation existante de la MAC ${normalizedMac} prolongée d'un an.`);
              } else {
                 await firestore.collection('activations').add(activationData);
                 console.log(`[JOBOOST-SUPPORT] Nouvelle activation créée pour la MAC ${normalizedMac}.`);
              }

              // Log clair pour le support client si la playlist ne s'active pas
              console.log(`[JOBOOST-SUPPORT] Target MAC ${normalizedMac} successfully fully activated via Webhook. PaymentID: ${paymentId}`);
           }
        } else if (status === 'FAILED' || status === 'CANCELLED') {
           await paymentDoc.ref.update({ status: 'FAILED' });
           console.log(`[JOBOOST-SUPPORT] Payment FAILED for MAC ${paymentData.target_mac}. PaymentID: ${paymentId}`);
        }

        // On renvoie un 200 OK à JOboost pour arrêter leurs relances
        res.status(200).json({ received: true });
      } else {
        console.error(`[API] Webhook JOboost ignoré : Document Payment ${paymentId} introuvable en bdd`);
        res.status(404).json({ error: "Paiement introuvable" });
      }
      
    } catch (error: any) {
      console.error('[API] JOboost Webhook Error:', error);
      res.status(500).json({ error: 'Erreur serveur lors du traitement du Webhook JOboost' });
    }
  });
  // --- END JOBOOST-CASH ---



  // Proxy for playlist fetching to bypass blockages
  app.get("/api/proxy/playlist", async (req, res) => {
    const targetUrl = req.query.url as string;
    if (!targetUrl) return res.status(400).json({ error: "Missing URL" });

    try {
      console.log(`[Proxy] Fetching (retryable): ${targetUrl}`);
      
      const userAgents = [
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Mozilla/5.0 (SmartHub; SMART-TV; U; SamsungBrowser; Tizen 6.0) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/4.0',
        'Dalvik/2.1.0 (Linux; U; Android 11; SM-G981B Build/RP1A.200720.012)',
        'IPTVSmartersPlayer/3.0.0 (Linux; Android 11)',
        'XCIPTV/6.0.0 (Linux; Android 11)'
      ];

      // Use a stall timeout instead of a fixed duration timeout
      const controller = new AbortController();
      let stallTimeout = setTimeout(() => controller.abort(), 60000); // 60s inactivity timeout
      
      const response = await fetch(targetUrl, {
        headers: {
          'User-Agent': userAgents[Math.floor(Math.random() * userAgents.length)],
          'Accept': '*/*',
          'Accept-Encoding': 'gzip, deflate, br',
          'Connection': 'keep-alive',
          'Cache-Control': 'no-cache'
        },
        signal: controller.signal
      });
      
      if (!response.ok) {
        clearTimeout(stallTimeout);
        return res.status(response.status).json({ error: `Serveur IPTV a renvoyé une erreur ${response.status}` });
      }

      // Stream the response back to the client
      res.setHeader('Content-Type', response.headers.get('content-type') || 'text/plain');
      res.setHeader('X-Content-Encoded', response.headers.get('content-encoding') || 'none');
      
      if (response.body) {
        // @ts-ignore - response.body is a ReadableStream in node-fetch 3.x
        for await (const chunk of response.body) {
          // Reset stall timeout on every chunk received
          clearTimeout(stallTimeout);
          stallTimeout = setTimeout(() => controller.abort(), 60000); 
          
          res.write(chunk);
        }
      }
      clearTimeout(stallTimeout);
      res.end();
    } catch (e: any) {
      if (e.name === 'AbortError') {
        console.error("[Proxy] Timeout: Server stopped sending data for 60s");
        return res.status(504).json({ error: "Le serveur IPTV a cessé d'envoyer des données (Délai d'inactivité dépassé)" });
      }
      console.error("[Proxy] Critical error:", e);
      res.status(502).json({ error: "Erreur de connexion IPTV", details: e.message });
    }
  });


  // --- UTILS ---
  const normalizeMac = (mac: string): string => {
    if (!mac) return '';
    return mac.toLowerCase().replace(/[^a-f0-9]/g, '');
  };

  // Associate Playlist from Website QR Code
  app.post("/api/playlist/associate", async (req, res) => {
    const { mac, playlist_url, xtream_host, xtream_username, xtream_password } = req.body;
    if (!mac) return res.status(400).json({ error: "MAC is required" });
    
    try {
      const firestore = getDb();
      if (!firestore) return res.status(503).json({ error: "Base de données non accessible" });

      const normalizedMac = normalizeMac(mac);
      
      let activationSnapshot = await firestore.collection('activations').where('target_mac', '==', normalizedMac).get();
      if (activationSnapshot.empty) {
         const altMac = mac.toUpperCase().trim();
         activationSnapshot = await firestore.collection('activations').where('target_mac', '==', altMac).get();
      }

      if (!activationSnapshot.empty) {
         // Update existing activation
         const actDoc = activationSnapshot.docs[0];
         await actDoc.ref.update({
             playlist_url: playlist_url || "",
             playlistUrl: playlist_url || "",
             xtream_host: xtream_host || "",
             xtreamServer: xtream_host || "",
             xtream_username: xtream_username || "",
             xtreamUser: xtream_username || "",
             xtream_password: xtream_password || "",
             xtreamPassword: xtream_password || ""
         });
      } else {
         // Create basic activation for this MAC if it doesn't exist
         // We'll mark it as a 'trial_active' internally by just setting the entry
         // The playback api decides if it is trial or not based on devices check usually
         await firestore.collection('activations').add({
            resellerId: "SELF_SERVICE",
            target_mac: normalizedMac,
            credits_used: 0,
            note: "Playlist associée depuis l'interface web",
            playlist_url: playlist_url || "",
            playlistUrl: playlist_url || "",
            xtream_host: xtream_host || "",
            xtreamServer: xtream_host || "",
            xtream_username: xtream_username || "",
            xtreamUser: xtream_username || "",
            xtream_password: xtream_password || "",
            xtreamPassword: xtream_password || "",
            createdAt: admin.firestore.FieldValue.serverTimestamp()
         });
      }

      return res.json({ success: true });
    } catch (e: any) {
       console.error('[API] Error in playlist association:', e);
       res.status(500).json({ error: 'Server error' });
    }
  });

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

      const normalizedMac = normalizeMac(target_mac);
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
      const now = new Date();
      const oneYearLater = new Date(now.getTime() + 365 * 24 * 60 * 60 * 1000);
      
      const activationData = {
        resellerId,
        target_mac: normalizedMac,
        credits_used: credits_used || 0,
        note: note || 'Client manuel',
        playlist_url: playlist_url || '',
        playlistUrl: playlist_url || '',
        xtream_host: xtream_host || '',
        xtreamServer: xtream_host || '',
        xtream_username: xtream_username || '',
        xtreamUser: xtream_username || '',
        xtream_password: xtream_password || '',
        xtreamPassword: xtream_password || '',
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        expiryDate: oneYearLater.toISOString(),
        last_connection: admin.firestore.FieldValue.serverTimestamp(),
        system: 'N/A',
        version: 'N/A',
        country_code: 'N/A',
        current_channel: 'Hors-ligne',
        status: 'ACTIF'
      };
      
      const docRef = await firestore.collection('activations').add(activationData);
      
      console.log(`[API] Activation created successfully ID: ${docRef.id} for MAC ${normalizedMac}`);
      res.json({ success: true, id: docRef.id });
    } catch (error: any) {
      console.error('[API] Activation Create Error:', error);
      res.status(500).json({ error: error.message || 'Erreur lors de la création de l\'activation' });
    }
  });

  // Check MAC Status (Server-side proxy to bypass Firestore rules for devices)
  app.get("/api/mac/check/:mac", validateActivationApiKey, async (req, res) => {
    const { mac } = req.params;
    
    try {
      const firestore = getDb();
      if (!firestore) throw new Error("Base de données non accessible");
      
      const normalizedMac = normalizeMac(mac);
      console.log(`[API] Checking status for MAC: ${normalizedMac}`);
      
      let snapshot = await firestore.collection('activations').where('target_mac', '==', normalizedMac).get();
      
      if (snapshot.empty) {
        // Fallback pour compatibilité de casse uppercase
        const altMac = mac.toUpperCase().trim();
        snapshot = await firestore.collection('activations').where('target_mac', '==', altMac).get();
      }
      
      if (snapshot.empty) {
        return res.json({ 
          active: false, 
          error: "MAC non activée. Veuillez l'ajouter dans votre panel revendeur." 
        });
      }
      
      const doc = snapshot.docs[0];
      const data = doc.data();
      
      // Check for expiry
      if (data.expiryDate) {
        const expiry = new Date(data.expiryDate);
        if (expiry < new Date()) {
          return res.json({ 
            active: false, 
            error: "Abonnement expiré. Veuillez le prolonger." 
          });
        }
      }

      res.json({ 
        active: true, 
        activation: { id: doc.id, ...data } 
      });
    } catch (error: any) {
      console.error('MAC Check Error:', error);
      res.status(500).json({ error: 'Erreur serveur lors de la vérification' });
    }
  });

  // Heartbeat / Device Info Update
  app.post("/api/activations/heartbeat", async (req, res) => {
    const { mac, system, version, country, channel } = req.body;
    
    try {
      const firestore = getDb();
      if (!firestore) throw new Error("Base de données non accessible");
      
      if (!mac) return res.status(400).json({ error: 'MAC est requise' });
      
      const normalizedMac = normalizeMac(mac);
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

  // --- TV App API Removed (No more mock endpoints) ---





  // --- Check Device & Trial Management (Sky Player Pro Android) ---
  app.post("/api/devices/check", validateActivationApiKey, async (req, res) => {
    const { mac_address, device_id } = req.body;

    if (!mac_address || !device_id) {
      return res.status(400).json({ error: "mac_address et device_id sont requis." });
    }

    try {
      const firestore = getDb();
      if (!firestore) throw new Error("Base de données non accessible");

      const normalizedMac = normalizeMac(mac_address);
      const deviceRef = firestore.collection('devices').doc(normalizedMac);
      const doc = await deviceRef.get();

      // Vérifie si un revendeur a assigné une playlist
      let activationSnapshot = await firestore.collection('activations').where('target_mac', '==', normalizedMac).get();
      if (activationSnapshot.empty) {
         // Fallback pour compatibilité de casse uppercase
         const altMac = mac_address.toUpperCase().trim();
         activationSnapshot = await firestore.collection('activations').where('target_mac', '==', altMac).get();
      }
      let activationData: any = null;
      if (!activationSnapshot.empty) {
         activationData = activationSnapshot.docs[0].data();
      }

      let payload = {
         status: "trial_active",
         days_remaining: 15,
         playlist_url: "",
         playlistUrl: "",
         xtream_host: "",
         xtreamServer: "",
         xtream_username: "",
         xtreamUser: "",
         xtream_password: "",
         xtreamPassword: "",
         message: "Période d'essai activée."
      };

      if (!doc.exists) {
        // First launch, create the trial record
        const newDevice = {
          mac_address: normalizedMac,
          device_id: device_id,
          first_launch: admin.firestore.FieldValue.serverTimestamp(),
          playlist_url: "",
          is_active: false
        };
        await deviceRef.set(newDevice);
      } else {
        const data = doc.data()!;
        if (data.device_id && data.device_id !== device_id) {
          console.warn(`[SECURITY] device_id mismatch for MAC ${normalizedMac}. Expected: ${data.device_id}, Got: ${device_id}`);
        }

        const firstLaunchDate = data.first_launch ? data.first_launch.toDate() : new Date();
        const diffTime = (new Date()).getTime() - firstLaunchDate.getTime();
        const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
        const remainingDays = 15 - diffDays;

        if (data.is_active) {
          payload.status = "premium_active";
          payload.days_remaining = 0;
          payload.playlist_url = data.playlist_url || "";
          payload.message = "Abonnement actif.";
        } else if (remainingDays <= 0) {
          payload.status = "expired";
          payload.days_remaining = 0;
          payload.playlist_url = data.playlist_url || "";
          payload.message = "La période d'essai a expiré.";
        } else {
          payload.status = "trial_active";
          payload.days_remaining = remainingDays;
          payload.playlist_url = data.playlist_url || "";
          payload.message = `Période d'essai en cours (${remainingDays} jours restants).`;
        }
      }

      // Surcharger avec les données d'activation (Priorité au Revendeur)
      if (activationData) {
         let isActivationExpired = false;
         if (activationData.expiryDate) {
           const expiry = new Date(activationData.expiryDate);
           if (expiry < new Date()) {
             isActivationExpired = true;
           }
         }
         
         if (!isActivationExpired) {
           payload.status = "premium_active";
           // Standard response format (snake_case)
           payload.playlist_url = activationData.playlist_url || "";
           payload.xtream_host = activationData.xtream_host || "";
           payload.xtream_username = activationData.xtream_username || "";
           payload.xtream_password = activationData.xtream_password || "";
           // Camel case format and duplicate keys for backwards compatibility with Android Models
           payload.playlistUrl = activationData.playlist_url || "";
           payload.xtreamServer = activationData.xtream_host || "";
           payload.xtreamUser = activationData.xtream_username || "";
           payload.xtreamPassword = activationData.xtream_password || "";
           payload.message = "Abonnement actif (via Revendeur/Web).";
         } else if (payload.status === "premium_active") {
           payload.status = "expired";
           payload.message = "Abonnement revendeur expiré.";
           payload.playlist_url = ""; 
           payload.playlistUrl = "";
         }
      }

      return res.json(payload);
    } catch (error: any) {
      console.error('[API] Device Check Error:', error);
      res.status(500).json({ error: 'Erreur interne lors de la vérification du périphérique.' });
    }
  });

  // --- Route API spécifique pour Android: Fetch Playlist (GET) ---
  app.get("/api/v1/playlist/:mac", validateActivationApiKey, async (req, res) => {
    const { mac } = req.params;
    if (!mac) return res.status(400).json({ error: "MAC address required" });

    try {
      const firestore = getDb();
      if (!firestore) throw new Error("Base de données non accessible");

      const normalizedMac = normalizeMac(mac);
      
      const payload: any = {
        playlist_url: "",
        playlistUrl: "",
        xtream_host: "",
        xtreamServer: "",
        xtream_username: "",
        xtreamUser: "",
        xtream_password: "",
        xtreamPassword: "",
        active: false,
        message: ""
      };

      let activationSnapshot = await firestore.collection('activations').where('target_mac', '==', normalizedMac).get();
      if (activationSnapshot.empty) {
         // Fallback pour compatibilité de casse uppercase
         const altMac = mac.toUpperCase().trim();
         activationSnapshot = await firestore.collection('activations').where('target_mac', '==', altMac).get();
      }

      if (!activationSnapshot.empty) {
         const data = activationSnapshot.docs[0].data();
         let isExpired = false;
         if (data.expiryDate && new Date(data.expiryDate) < new Date()) {
             isExpired = true;
         }
         
         if (!isExpired) {
           payload.active = true;
           payload.playlist_url = data.playlist_url || "";
           payload.playlistUrl = data.playlist_url || "";
           payload.xtream_host = data.xtream_host || "";
           payload.xtreamServer = data.xtream_host || "";
           payload.xtream_username = data.xtream_username || "";
           payload.xtreamUser = data.xtream_username || "";
           payload.xtream_password = data.xtream_password || "";
           payload.xtreamPassword = data.xtream_password || "";
           // Sub-object in case the native app expects nested
           payload.xtream = {
              host: data.xtream_host || "",
              username: data.xtream_username || "",
              password: data.xtream_password || ""
           };
           payload.message = "Playlist active.";
         } else {
           payload.message = "Abonnement expiré.";
         }
      } else {
         const deviceDoc = await firestore.collection('devices').doc(normalizedMac).get();
         if (deviceDoc.exists) {
            const dData = deviceDoc.data()!;
            if (dData.is_active || (dData.first_launch && (new Date().getTime() - dData.first_launch.toDate().getTime()) / 86400000 <= 15)) {
               payload.active = true;
               payload.playlist_url = dData.playlist_url || "";
               payload.playlistUrl = dData.playlist_url || "";
               payload.message = "Trial ou Appareil actif.";
            } else {
               payload.message = "Trial expiré ou appareil inactif.";
            }
         } else {
            payload.message = "Aucun appareil ou activation trouvé.";
         }
      }

      return res.json(payload);
    } catch (error: any) {
      console.error('[API] Playlist Fetch Error:', error);
      res.status(500).json({ error: 'Erreur serveur.' });
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
            paymentUrl: `https://bkapay.com/pay?amount=${amount}&ref=${externalId}`,
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
        : (provider === 'stripe' ? "Paiement par carte initié..." : `Paiement via votre fournisseur (${methodId}) initié.`);

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

  // Mount API Router fallback
  app.post("/api/ai-validation", async (req, res) => {
    try {
      const { aiImage, promptText, expectedPrice, currency, selectedPack } = req.body;
      const { GoogleGenAI } = await import("@google/genai");

      if (!process.env.GEMINI_API_KEY) {
        throw new Error("API Key is missing on the server.");
      }

      const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });
      const base64Data = aiImage.split(',')[1] || aiImage;

      const response = await ai.models.generateContent({
        model: "gemini-1.5-flash",
        contents: [{
          parts: [
            { text: promptText },
            { inlineData: { data: base64Data, mimeType: "image/jpeg" } }
          ]
        }],
        config: {
          responseMimeType: "application/json"
        }
      });

      let data;
      try {
        data = JSON.parse(response.text || "{}");
      } catch (e) {
        throw new Error("Impossible de lire la réponse de l'IA. Format JSON invalide.");
      }

      return res.json({ success: true, data });
    } catch (error: any) {
      console.error('AI Validation Error:', error);
      res.status(500).json({ error: error.message || 'Erreur lors de la validation IA' });
    }
  });

  app.post("/api/ai-receipt", async (req, res) => {
    try {
      const { base64Image, mimeType } = req.body;
      const { GoogleGenAI, Type } = await import("@google/genai");

      if (!process.env.GEMINI_API_KEY) {
        throw new Error("API Key is missing on the server.");
      }

      const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });
      const base64Data = base64Image.split(',')[1] || base64Image;

      const response = await ai.models.generateContent({
        model: "gemini-1.5-flash",
        contents: [
          {
            parts: [
              {
                inlineData: {
                  data: base64Data,
                  mimeType: mimeType || "image/jpeg",
                },
              },
              {
                text: "Analyse ce reçu de paiement Mobile Money. Identifie si c'est un reçu de transfert d'argent réussi (Airtel Money, Moov, Orange, MTN, Wave, etc.). Extrait l'ID de transaction, le montant exact, la devise, et la date. Vérifie si le reçu semble authentique et complet. Réponds au format JSON.",
              },
            ],
          },
        ],
        config: {
          responseMimeType: "application/json",
          responseSchema: {
            type: Type.OBJECT,
            properties: {
              transactionId: { type: Type.STRING, description: "L'identifiant unique de la transaction" },
              amount: { type: Type.NUMBER, description: "Le montant numérique payé" },
              currency: { type: Type.STRING, description: "La devise (ex: XAF, XOF)" },
              provider: { type: Type.STRING, description: "Le nom de l'opérateur (ex: Airtel, Moov)" },
              date: { type: Type.STRING, description: "La date du reçu au format ISO" },
              isValid: { type: Type.BOOLEAN, description: "True si le reçu est valide et n'est pas une tentative de fraude évidente" },
              reason: { type: Type.STRING, description: "Raison si non valide" },
            },
            required: ["transactionId", "amount", "currency", "provider", "isValid"],
          },
        },
      });

      const data = JSON.parse(response.text || "{}");
      return res.json({ success: true, data });
    } catch (error: any) {
      console.error('AI Receipt Validation Error:', error);
      res.status(500).json({ error: error.message || 'Erreur lors de la validation IA du reçu' });
    }
  });

  app.use('/api', (req, res) => {
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
    // In production, we assume the server is running from dist/server.cjs
    // and assets are siblings in the same dist folder.
    const distPath = path.join(process.cwd(), 'dist');
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
    app.get('*all', (req, res) => {
      res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
      res.setHeader('Pragma', 'no-cache');
      res.setHeader('Expires', '0');
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  // Respect PORT env var for Cloud Run, but maintain 3000 for internal infrastructure
  const port = parseInt(process.env.PORT || '3000', 10);
  
  // Listen on the port assigned by the platform and capture the server instance
  const server = app.listen(port, '0.0.0.0', () => {
    console.log(`>>> SERVER LISTENING ON PORT=${port} <<<`);
    console.log('Environment:', isProd ? 'PRODUCTION' : 'DEVELOPMENT');
  });

  // If the assigned port is not 3000, listen on 3000 as well for infrastructure requirements
  if (port !== 3000) {
    const internalServer = app.listen(3000, '0.0.0.0', () => {
      console.log(`>>> SERVER ALSO LISTENING ON PORT=3000 <<<`);
    });
    internalServer.on('error', (err: any) => {
      console.warn('Non-fatal: Could not bind to port 3000 for infrastructure fallback:', err.message);
    });
  }

  server.on('error', (err) => {
    console.error('SERVER BINDING ERROR:', err);
    process.exit(1);
  });
}

startServer().catch(err => {
  console.error("FAILED TO START SERVER:", err);
  process.exit(1);
});
