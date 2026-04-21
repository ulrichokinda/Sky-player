import 'dotenv/config';
import express from 'express';
import cors from 'cors';
import { createServer as createViteServer } from 'vite';
import path from 'path';
import { fileURLToPath } from 'url';
import admin from 'firebase-admin';
import { getFirestore } from 'firebase-admin/firestore';
import fs from 'fs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Read firebase-applet-config.json
let firebaseConfig: any = {};
try {
  const configPath = path.resolve(__dirname, 'firebase-applet-config.json');
  if (fs.existsSync(configPath)) {
    firebaseConfig = JSON.parse(fs.readFileSync(configPath, 'utf8'));
  }
} catch (e) {
  console.error('Could not read firebase-applet-config.json', e);
}

const projectId = firebaseConfig.projectId || process.env.VITE_FIREBASE_PROJECT_ID || 'skyplayer-60634';
const databaseId = firebaseConfig.firestoreDatabaseId || '(default)';

// Initialize Firebase Admin
try {
  let credential;
  
  if (process.env.FIREBASE_SERVICE_ACCOUNT) {
    const saContent = process.env.FIREBASE_SERVICE_ACCOUNT.trim();
    if (saContent.startsWith('{')) {
      const serviceAccount = JSON.parse(saContent);
      // Only use the service account if it matches the target project
      if (!firebaseConfig.projectId || serviceAccount.project_id === firebaseConfig.projectId) {
        credential = admin.credential.cert(serviceAccount);
        console.log('Using Firebase Service Account JSON for authentication');
      } else {
        console.warn(`Warning: Service Account project_id (${serviceAccount.project_id}) does not match target project (${projectId}). Falling back to Application Default Credentials.`);
        credential = admin.credential.applicationDefault();
      }
    } else {
      console.error('CRITICAL: FIREBASE_SERVICE_ACCOUNT appears to be a legacy Database Secret (string).');
      credential = admin.credential.applicationDefault();
    }
  } else {
    credential = admin.credential.applicationDefault();
  }

  admin.initializeApp({
    credential,
    projectId: projectId
  });
  console.log(`Firebase Admin initialized for project: ${projectId}`);
} catch (error) {
  console.error('Error initializing Firebase Admin:', error);
  if (admin.apps.length === 0) {
    admin.initializeApp({
      projectId: projectId
    });
  }
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
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
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
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    // Serve static files from dist
    const distPath = path.resolve(__dirname, 'dist');
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

  // Cloud Run provides the PORT environment variable (usually 8080)
  const PORT = Number(process.env.PORT) || 3000;
  
  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Server running on port ${PORT}`);
  });
}

startServer().catch(err => {
  console.error("FAILED TO START SERVER:", err);
  process.exit(1);
});
