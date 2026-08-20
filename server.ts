import 'dotenv/config';
import express from 'express';
import cors from 'cors';
import path from 'path';
import admin from 'firebase-admin';
import { getFirestore } from 'firebase-admin/firestore';
import fs from 'fs';
import crypto from 'crypto';
import { URL } from 'url';

console.log('--- SERVER STARTING UP ---');
console.log('Node Version:', process.version);
console.log('PORT environment variable:', process.env.PORT);
console.log('CWD:', process.cwd());

process.on('uncaughtException', (err) => {
  console.error('UNCAUGHT EXCEPTION:', err);
  process.exit(1);
});
process.on('unhandledRejection', (reason) => {
  console.error('UNHANDLED REJECTION:', reason);
});

// ─── Firebase Config ────────────────────────────────────────────
let firebaseConfig: any = {};
try {
  const configPath = path.resolve(process.cwd(), 'firebase-applet-config.json');
  if (fs.existsSync(configPath)) {
    firebaseConfig = JSON.parse(fs.readFileSync(configPath, 'utf8'));
  }
} catch (e) {
  console.error('Could not read firebase-applet-config.json', e);
}
const projectId = firebaseConfig.projectId || process.env.VITE_FIREBASE_PROJECT_ID || process.env.GOOGLE_CLOUD_PROJECT || process.env.GCLOUD_PROJECT;
const databaseId = firebaseConfig.firestoreDatabaseId || '(default)';

// ─── Constants ──────────────────────────────────────────────────
const VERSION = '5.0.0-SECURE';
const ALLOWED_ORIGINS = ['https://skyplayerapp.xyz', 'http://localhost:3000'];
const SSRF_BLOCKED_RANGES = ['10.', '172.16.', '172.17.', '172.18.', '172.19.', '172.20.', '172.21.', '172.22.', '172.23.', '172.24.', '172.25.', '172.26.', '172.27.', '172.28.', '172.29.', '172.30.', '172.31.', '192.168.', '127.0.', '::1', '169.254.'];

// ─── Utils ──────────────────────────────────────────────────────
const normalizeMac = (mac: string): string => {
  if (!mac) return '';
  return mac.toLowerCase().replace(/[^a-f0-9]/g, '');
};
const isValidMac = (mac: string): boolean => {
  const n = normalizeMac(mac);
  return n.length >= 12 && n.length <= 17;
};
function isSafeUrl(urlStr: string): boolean {
  try {
    const parsed = new URL(urlStr);
    if (!['http:', 'https:'].includes(parsed.protocol)) return false;
    const h = parsed.hostname.toLowerCase();
    if (h === 'localhost' || h === '127.0.0.1' || h === '::1') return false;
    for (const range of SSRF_BLOCKED_RANGES) {
      if (h.startsWith(range)) return false;
    }
    return true;
  } catch { return false; }
}

// ─── Rate Limiter (in-memory) ───────────────────────────────────
const rateLimitStore = new Map<string, { count: number; resetAt: number }>();
const RATE_WINDOW = 60_000;
const RATE_MAX = 100;
function rateLimit(req: express.Request, res: express.Response, next: express.NextFunction) {
  const key = req.ip || 'unknown';
  const now = Date.now();
  const entry = rateLimitStore.get(key);
  if (!entry || now > entry.resetAt) {
    rateLimitStore.set(key, { count: 1, resetAt: now + RATE_WINDOW });
    return next();
  }
  entry.count++;
  if (entry.count > RATE_MAX) {
    console.warn(`[RATE-LIMIT] Blocked ${key} (${entry.count}/min)`);
    return res.status(429).json({ error: 'Trop de requêtes. Réessayez dans 1 minute.' });
  }
  next();
}
setInterval(() => {
  const now = Date.now();
  for (const [k, v] of rateLimitStore) { if (now > v.resetAt) rateLimitStore.delete(k); }
}, 300_000);

// ─── Server Start ──────────────────────────────────────────────
async function startServer() {
  const app = express();

  // CORS whitelist
  app.use(cors({
    origin: (origin, cb) => {
      if (!origin || ALLOWED_ORIGINS.includes(origin)) cb(null, true);
      else { console.warn(`[CORS] Blocked: ${origin}`); cb(null, false); }
    },
    credentials: true,
  }));

  // Body parser 1MB limit
  app.use(express.json({ limit: '1mb' }));

  // Rate limiter
  app.use(rateLimit);

  // Security headers
  app.use((_req, res, next) => {
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
    next();
  });

  // ─── Firebase Admin (lazy) ──────────────────────────────────
  let adminApp: admin.app.App | null = null;
  const getDb = () => {
    try {
      if (!adminApp) {
        if (admin.apps.length > 0) { adminApp = admin.apps[0]!; }
        else {
          const options: admin.AppOptions = {};
          if (process.env.FIREBASE_SERVICE_ACCOUNT) {
            try { options.credential = admin.credential.cert(JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT.trim())); }
            catch { console.error('FIREBASE_SERVICE_ACCOUNT JSON invalid'); }
          }
          if (projectId) options.projectId = projectId;
          adminApp = admin.initializeApp(options);
        }
      }
      return getFirestore(adminApp, databaseId);
    } catch (e) { console.error('Firestore not ready:', e); return null; }
  };

  // ─── API Key Auth (NO hardcoded fallback!) ──────────────────
  const validateActivationApiKey = (req: express.Request, res: express.Response, next: express.NextFunction) => {
    const apiKey = process.env.ACTIVATION_API_KEY;
    if (!apiKey) {
      console.error('[SECURITY] ACTIVATION_API_KEY not set!');
      return res.status(500).json({ error: 'Server configuration error' });
    }
    const headerKey = (req.headers['x-activation-api-key'] || req.headers['x-api-key'] || req.query.api_key) as string | undefined;
    const isPlaylistRoute = req.path.includes('/api/v1/playlist');
    if (headerKey === apiKey) return next();
    if (isPlaylistRoute) { console.log(`[PLAYLIST] Tolerant access from IP ${req.ip}`); return next(); }
    console.warn(`[SECURITY] DENIED: ${req.method} ${req.path} from ${req.ip}`);
    return res.status(401).json({ error: 'Clé API invalide ou absente.' });
  };

  const validateAiAuth = (req: express.Request, res: express.Response, next: express.NextFunction) => {
    const apiKey = process.env.ACTIVATION_API_KEY;
    if (!apiKey) return res.status(500).json({ error: 'Server configuration error' });
    const headerKey = (req.headers['x-activation-api-key'] || req.headers['x-api-key']) as string | undefined;
    if (headerKey === apiKey) return next();
    console.warn(`[SECURITY] AI DENIED from ${req.ip}`);
    return res.status(401).json({ error: 'Accès non autorisé.' });
  };

  // ═══════════════════════════════════════════════════════════
  //  ROUTES
  // ═══════════════════════════════════════════════════════════

  app.get('/api/health', (_req, res) => {
    res.status(200).json({ status: 'ok', version: VERSION, uptime: process.uptime() });
  });

  // ─── Proxy (anti-SSRF + retry) ─────────────────────────────
  app.get('/api/proxy/playlist', async (req, res) => {
    const targetUrl = String(req.query.url || '');
    if (!targetUrl) return res.status(400).json({ error: 'Missing URL' });
    if (!isSafeUrl(targetUrl)) { console.warn(`[SECURITY] SSRF blocked: ${targetUrl}`); return res.status(403).json({ error: 'URL non autorisée.' }); }

    const userAgents = [
      'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36',
      'Dalvik/2.1.0 (Linux; Android 11; SM-G981B)',
      'IPTVSmartersPlayer/3.0.0 (Linux; Android 11)',
    ];

    for (let attempt = 0; attempt <= 2; attempt++) {
      try {
        if (attempt > 0) console.log(`[Proxy] Retry ${attempt}/2: ${targetUrl}`);
        else console.log(`[Proxy] Fetching: ${targetUrl}`);

        const controller = new AbortController();
        const stallTimeout = setTimeout(() => controller.abort(), 60_000);
        const response = await fetch(targetUrl, {
          headers: { 'User-Agent': userAgents[Math.floor(Math.random() * userAgents.length)], 'Accept': '*/*' },
          signal: controller.signal,
        });
        if (!response.ok) { clearTimeout(stallTimeout); if (attempt < 2) continue; return res.status(response.status).json({ error: `Erreur serveur IPTV: ${response.status}` }); }

        res.setHeader('Content-Type', response.headers.get('content-type') || 'text/plain');
        if (response.body) { for await (const chunk of response.body) { clearTimeout(stallTimeout); res.write(chunk); } }
        clearTimeout(stallTimeout);
        res.end();
        return;
      } catch (e: any) {
        if (e.name === 'AbortError' && attempt >= 2) return res.status(504).json({ error: 'Délai dépassé' });
        if (attempt < 2) continue;
        console.error('[Proxy] Error:', e);
        res.status(502).json({ error: 'Erreur connexion IPTV', details: e.message });
        return;
      }
    }
    res.status(502).json({ error: 'Proxy error' });
  });

  // ─── Playlist Association ──────────────────────────────────
  app.post('/api/playlist/associate', validateActivationApiKey, async (req, res) => {
    const { mac, playlist_url, xtream_host, xtream_username, xtream_password } = req.body;
    if (!mac) return res.status(400).json({ error: 'MAC required' });
    if (!isValidMac(mac)) return res.status(400).json({ error: 'Format MAC invalide' });

    try {
      const firestore = getDb();
      if (!firestore) return res.status(503).json({ error: 'DB non accessible' });
      const normalizedMac = normalizeMac(mac);
      let snap = await firestore.collection('activations').where('target_mac', '==', normalizedMac).get();
      if (snap.empty) snap = await firestore.collection('activations').where('target_mac', '==', mac.toUpperCase().trim()).get();

      if (!snap.empty) {
        await snap.docs[0].ref.update({ playlist_url: playlist_url || '', xtream_host: xtream_host || '', xtream_username: xtream_username || '', xtream_password: xtream_password || '' });
      } else {
        await firestore.collection('activations').add({ resellerId: 'SELF_SERVICE', target_mac: normalizedMac, credits_used: 0, note: 'Playlist associée depuis web', playlist_url: playlist_url || '', xtream_host: xtream_host || '', xtream_username: xtream_username || '', xtream_password: xtream_password || '', createdAt: admin.firestore.FieldValue.serverTimestamp() });
      }
      return res.json({ success: true });
    } catch (e: any) { console.error('[API] associate error:', e); res.status(500).json({ error: 'Server error' }); }
  });

  // ─── Create Activation ─────────────────────────────────────
  app.post('/api/activations/create', validateActivationApiKey, async (req, res) => {
    const { resellerId, target_mac, credits_used, note, playlist_url, xtream_host, xtream_username, xtream_password } = req.body;
    if (!target_mac || !isValidMac(target_mac)) return res.status(400).json({ error: 'MAC invalide ou manquante' });
    if (!resellerId) return res.status(400).json({ error: 'resellerId requis' });

    try {
      const firestore = getDb();
      if (!firestore) return res.status(503).json({ error: 'DB non accessible' });
      const normalizedMac = normalizeMac(target_mac);
      const existing = await firestore.collection('activations').where('target_mac', '==', normalizedMac).get();
      if (!existing.empty) return res.status(400).json({ error: `MAC ${normalizedMac} déjà activée.` });

      if (credits_used > 0 && resellerId !== 'SYSTEM_TRIAL') {
        const userRef = firestore.collection('users').doc(resellerId);
        await firestore.runTransaction(async (t) => {
          const u = await t.get(userRef);
          if (!u.exists) throw new Error('Utilisateur non trouvé');
          const cur = u.data()?.credits || 0;
          if (cur < credits_used) throw new Error(`Crédits insuffisants (${cur} < ${credits_used})`);
          t.update(userRef, { credits: cur - credits_used });
        });
      }

      const oneYear = new Date(); oneYear.setFullYear(oneYear.getFullYear() + 1);
      const docRef = await firestore.collection('activations').add({
        resellerId, target_mac: normalizedMac, credits_used: credits_used || 0, note: note || 'Client manuel',
        playlist_url: playlist_url || '', xtream_host: xtream_host || '', xtream_username: xtream_username || '', xtream_password: xtream_password || '',
        createdAt: admin.firestore.FieldValue.serverTimestamp(), expiryDate: oneYear.toISOString(),
        last_connection: admin.firestore.FieldValue.serverTimestamp(), status: 'ACTIF',
      });
      console.log(`[API] Activation created: ${docRef.id} for MAC ${normalizedMac}`);
      res.json({ success: true, id: docRef.id });
    } catch (error: any) { console.error('[API] Activation Error:', error); res.status(500).json({ error: error.message }); }
  });

  // ─── Check MAC Status ──────────────────────────────────────
  app.get('/api/mac/check/:mac', validateActivationApiKey, async (req, res) => {
    const mac = String(req.params.mac || '');
    if (!isValidMac(mac)) return res.status(400).json({ error: 'MAC invalide' });

    try {
      const firestore = getDb();
      if (!firestore) throw new Error('DB non accessible');
      const normalizedMac = normalizeMac(mac);
      let snap = await firestore.collection('activations').where('target_mac', '==', normalizedMac).get();
      if (snap.empty) snap = await firestore.collection('activations').where('target_mac', '==', mac.toUpperCase().trim()).get();
      if (snap.empty) return res.json({ active: false, error: 'MAC non activée.' });

      const doc = snap.docs[0];
      const data = doc.data();
      if (data.expiryDate && new Date(data.expiryDate) < new Date()) return res.json({ active: false, error: 'Abonnement expiré.' });
      res.json({ active: true, activation: { id: doc.id, ...data } });
    } catch (error: any) { console.error('MAC Check Error:', error); res.status(500).json({ error: 'Erreur serveur' }); }
  });

  // ─── Heartbeat ─────────────────────────────────────────────
  app.post('/api/activations/heartbeat', validateActivationApiKey, async (req, res) => {
    const { mac, system, version, country, channel } = req.body;
    if (!mac) return res.status(400).json({ error: 'MAC requise' });
    if (!isValidMac(mac)) return res.status(400).json({ error: 'MAC invalide' });

    try {
      const firestore = getDb();
      if (!firestore) throw new Error('DB non accessible');
      const normalizedMac = normalizeMac(mac);
      const snap = await firestore.collection('activations').where('target_mac', '==', normalizedMac).get();
      if (snap.empty) return res.status(404).json({ error: 'Appareil non trouvé' });
      await snap.docs[0].ref.update({ system: system || 'Inconnu', version: version || 'Inconnu', country_code: country || 'N/A', current_channel: channel || 'Hors-ligne', last_connection: admin.firestore.FieldValue.serverTimestamp() });
      res.json({ success: true });
    } catch (error) { console.error('Heartbeat Error:', error); res.status(500).json({ error: 'Erreur serveur' }); }
  });

  // ─── Device Check (Android) ────────────────────────────────
  app.post('/api/devices/check', validateActivationApiKey, async (req, res) => {
    const { mac_address, device_id } = req.body;
    if (!mac_address || !device_id) return res.status(400).json({ error: 'mac_address et device_id requis.' });
    if (!isValidMac(mac_address)) return res.status(400).json({ error: 'MAC invalide' });

    try {
      const firestore = getDb();
      if (!firestore) throw new Error('DB non accessible');
      const normalizedMac = normalizeMac(mac_address);
      const deviceRef = firestore.collection('devices').doc(normalizedMac);
      const doc = await deviceRef.get();

      let actSnap = await firestore.collection('activations').where('target_mac', '==', normalizedMac).get();
      if (actSnap.empty) actSnap = await firestore.collection('activations').where('target_mac', '==', mac_address.toUpperCase().trim()).get();
      const actData = actSnap.empty ? null : actSnap.docs[0].data();

      let payload: any = { status: 'trial_active', days_remaining: 15, playlist_url: '', xtream_host: '', xtream_username: '', xtream_password: '', message: "Période d'essai activée." };

      if (!doc.exists) {
        await deviceRef.set({ mac_address: normalizedMac, device_id, first_launch: admin.firestore.FieldValue.serverTimestamp(), playlist_url: '', is_active: false });
      } else {
        const data = doc.data()!;
        const firstLaunch = data.first_launch?.toDate?.() || new Date();
        const diffDays = Math.floor((Date.now() - firstLaunch.getTime()) / 86_400_000);
        const remaining = 15 - diffDays;
        if (data.is_active) payload = { ...payload, status: 'premium_active', days_remaining: 0, playlist_url: data.playlist_url || '', message: 'Abonnement actif.' };
        else if (remaining <= 0) payload = { ...payload, status: 'expired', days_remaining: 0, playlist_url: data.playlist_url || '', message: 'Essai expiré.' };
        else payload = { ...payload, days_remaining: remaining, playlist_url: data.playlist_url || '', message: `Essai en cours (${remaining}j).` };
      }

      if (actData) {
        const isExpired = actData.expiryDate && new Date(actData.expiryDate) < new Date();
        if (!isExpired) payload = { ...payload, status: 'premium_active', playlist_url: actData.playlist_url || '', xtream_host: actData.xtream_host || '', xtream_username: actData.xtream_username || '', xtream_password: actData.xtream_password || '', message: 'Abonnement actif (Revendeur).' };
        else if (payload.status === 'premium_active') payload = { ...payload, status: 'expired', playlist_url: '', message: 'Abonnement revendeur expiré.' };
      }
      return res.json(payload);
    } catch (error: any) { console.error('[API] Device Check Error:', error); res.status(500).json({ error: 'Erreur serveur.' }); }
  });

  // ─── Playlist Fetch (Android) ──────────────────────────────
  app.get('/api/v1/playlist/:mac', validateActivationApiKey, async (req, res) => {
    const mac = String(req.params.mac || '');
    if (!mac) return res.status(400).json({ error: 'MAC required' });

    try {
      const firestore = getDb();
      if (!firestore) throw new Error('DB non accessible');
      const normalizedMac = normalizeMac(mac);
      const payload: any = { playlist_url: '', xtream_host: '', xtream_username: '', xtream_password: '', active: false, message: '' };

      let actSnap = await firestore.collection('activations').where('target_mac', '==', normalizedMac).get();
      if (actSnap.empty) actSnap = await firestore.collection('activations').where('target_mac', '==', mac.toUpperCase().trim()).get();

      if (!actSnap.empty) {
        const data = actSnap.docs[0].data();
        const isExpired = data.expiryDate && new Date(data.expiryDate) < new Date();
        if (!isExpired) { payload.active = true; payload.playlist_url = data.playlist_url || ''; payload.xtream_host = data.xtream_host || ''; payload.xtream_username = data.xtream_username || ''; payload.xtream_password = data.xtream_password || ''; payload.xtream = { host: data.xtream_host || '', username: data.xtream_username || '', password: data.xtream_password || '' }; payload.message = 'Playlist active.'; }
        else payload.message = 'Abonnement expiré.';
      } else {
        const deviceDoc = await firestore.collection('devices').doc(normalizedMac).get();
        if (deviceDoc.exists) { const d = deviceDoc.data()!; const fl = d.first_launch?.toDate?.() || new Date(); const ds = (Date.now() - fl.getTime()) / 86_400_000; if (d.is_active || ds <= 15) { payload.active = true; payload.playlist_url = d.playlist_url || ''; payload.message = 'Trial ou appareil actif.'; } else payload.message = 'Trial expiré.'; }
        else payload.message = 'Aucun appareil trouvé.';
      }
      return res.json(payload);
    } catch (error: any) { console.error('[API] Playlist Error:', error); res.status(500).json({ error: 'Erreur serveur.' }); }
  });

  // ─── Payment Init (placeholder) ────────────────────────────
  app.post('/api/payments/initiate', validateActivationApiKey, async (req, res) => {
    const { userId, amount, phoneNumber, credits_purchased, provider, methodId } = req.body;
    if (!userId || !amount || !provider) return res.status(400).json({ error: 'userId, amount et provider requis.' });
    try {
      const firestore = getDb();
      if (!firestore) throw new Error('DB non accessible');
      const depositId = crypto.randomBytes(9).toString('hex');
      await firestore.collection('payments').add({ userId, amount: parseFloat(amount), credits_purchased: credits_purchased || 0, payment_method: methodId || null, provider, status: 'pending', external_id: depositId, createdAt: admin.firestore.FieldValue.serverTimestamp() });
      console.log(`[API] Payment initiated: ${provider} (${methodId}) for ${phoneNumber}, ${amount}`);
      res.json({ success: true, depositId, message: `Paiement via ${provider} initié.` });
    } catch (error) { console.error('Payment Error:', error); res.status(500).json({ error: 'Erreur paiement' }); }
  });

  // ─── AI Validation (secured) ───────────────────────────────
  app.post('/api/ai-validation', validateAiAuth, async (req, res) => {
    if (!process.env.GEMINI_API_KEY) return res.status(500).json({ error: 'Gemini API key not configured.' });
    try {
      const { aiImage, promptText } = req.body;
      if (!aiImage || !promptText) return res.status(400).json({ error: 'aiImage et promptText requis.' });
      const { GoogleGenAI } = await import('@google/genai');
      const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });
      const base64Data = aiImage.split(',')[1] || aiImage;
      const response = await ai.models.generateContent({ model: 'gemini-1.5-flash', contents: [{ parts: [{ text: promptText }, { inlineData: { data: base64Data, mimeType: 'image/jpeg' } }] }], config: { responseMimeType: 'application/json' } });
      let data; try { data = JSON.parse(response.text || '{}'); } catch { throw new Error('Réponse IA invalide'); }
      return res.json({ success: true, data });
    } catch (error: any) { console.error('AI Error:', error); res.status(500).json({ error: error.message || 'Erreur IA' }); }
  });

  app.post('/api/ai-receipt', validateAiAuth, async (req, res) => {
    if (!process.env.GEMINI_API_KEY) return res.status(500).json({ error: 'Gemini API key not configured.' });
    try {
      const { base64Image, mimeType } = req.body;
      if (!base64Image) return res.status(400).json({ error: 'base64Image requis.' });
      const { GoogleGenAI, Type } = await import('@google/genai');
      const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });
      const base64Data = base64Image.split(',')[1] || base64Image;
      const response = await ai.models.generateContent({ model: 'gemini-1.5-flash', contents: [{ parts: [{ inlineData: { data: base64Data, mimeType: mimeType || 'image/jpeg' } }, { text: "Analyse ce reçu de paiement Mobile Money. ID, montant, devise, date. JSON." }] }], config: { responseMimeType: 'application/json', responseSchema: { type: Type.OBJECT, properties: { transactionId: { type: Type.STRING }, amount: { type: Type.NUMBER }, currency: { type: Type.STRING }, provider: { type: Type.STRING }, date: { type: Type.STRING }, isValid: { type: Type.BOOLEAN }, reason: { type: Type.STRING } }, required: ['transactionId', 'amount', 'currency', 'provider', 'isValid'] } } });
      const data = JSON.parse(response.text || '{}');
      return res.json({ success: true, data });
    } catch (error: any) { console.error('AI Receipt Error:', error); res.status(500).json({ error: error.message || 'Erreur IA receipt' }); }
  });

  // ─── 404 API fallback ──────────────────────────────────────
  app.use('/api', (_req, res) => { res.status(404).json({ error: 'API route not found' }); });

  // ─── Vite dev / Static prod ────────────────────────────────
  const isProd = process.env.NODE_ENV === 'production';
  console.log(`Environment: ${isProd ? 'PRODUCTION' : 'DEVELOPMENT'}`);

  if (!isProd) {
    const { createServer: createViteServer } = await import('vite');
    const vite = await createViteServer({ server: { middlewareMode: true }, appType: 'spa' });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), 'dist');
    app.use(express.static(distPath, { maxAge: '1d' }));
    app.get('*all', (_req, res) => { res.sendFile(path.join(distPath, 'index.html')); });
  }

  // ─── Start ─────────────────────────────────────────────────
  const port = parseInt(process.env.PORT || '3000', 10);
  const server = app.listen(port, '0.0.0.0', () => {
    console.log(`>>> SERVER v${VERSION} LISTENING ON PORT=${port} <<<`);
  });
  if (port !== 3000) {
    const internal = app.listen(3000, '0.0.0.0', () => console.log('>>> ALSO LISTENING ON PORT=3000 <<<'));
    internal.on('error', (err: any) => console.warn('Port 3000 fallback failed:', err.message));
  }
  server.on('error', (err) => { console.error('SERVER ERROR:', err); process.exit(1); });
}

startServer().catch(err => { console.error('FAILED TO START:', err); process.exit(1); });
