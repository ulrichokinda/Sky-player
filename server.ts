import 'dotenv/config';
import express from 'express';
import cors from 'cors';
import Database from 'better-sqlite3';
import { createServer as createViteServer } from 'vite';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const db = new Database('database.db');
console.log('Database initialized successfully');

// Initialize database
db.exec(`
  CREATE TABLE IF NOT EXISTS users (
    uid TEXT PRIMARY KEY,
    email TEXT UNIQUE,
    username TEXT,
    password TEXT,
    role TEXT DEFAULT 'client',
    credits INTEGER DEFAULT 0,
    phone TEXT,
    country TEXT,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP
  );

  CREATE TABLE IF NOT EXISTS activations (
    id TEXT PRIMARY KEY,
    resellerId TEXT,
    target_mac TEXT,
    credits_used INTEGER,
    note TEXT,
    system TEXT,
    version TEXT,
    last_connection DATETIME,
    country_code TEXT,
    playlist_url TEXT,
    xtream_data TEXT,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(resellerId) REFERENCES users(uid)
  );

  CREATE TABLE IF NOT EXISTS payments (
    id TEXT PRIMARY KEY,
    userId TEXT,
    amount REAL,
    credits_purchased INTEGER,
    payment_method TEXT,
    provider TEXT,
    status TEXT,
    external_id TEXT,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(userId) REFERENCES users(uid)
  );

  CREATE TABLE IF NOT EXISTS user_bouquet (
    userId TEXT PRIMARY KEY,
    countries TEXT,
    FOREIGN KEY(userId) REFERENCES users(uid)
  );
`);

import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';

const JWT_SECRET = process.env.JWT_SECRET || 'sky-player-secret-key-2026';

async function startServer() {
  const app = express();
  app.use(cors());
  app.use(express.json());

  // Health check
  app.get('/api/health', (req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
  });

  // API Routes
  app.post('/api/auth/register', async (req, res) => {
    const { uid, email, username, password, phone, country, role } = req.body;
    try {
      let hashedPassword = null;
      if (password) {
        hashedPassword = await bcrypt.hash(password, 10);
      }
      
      const id = uid || Math.random().toString(36).substr(2, 9);
      const stmt = db.prepare('INSERT OR IGNORE INTO users (uid, email, username, password, phone, country, role) VALUES (?, ?, ?, ?, ?, ?, ?)');
      stmt.run(id, email, username, hashedPassword, phone, country, role || 'client');
      
      const user = db.prepare('SELECT * FROM users WHERE uid = ?').get(id) as any;
      const token = jwt.sign({ uid: user.uid, email: user.email }, JWT_SECRET, { expiresIn: '7d' });
      
      res.json({ success: true, user, token });
    } catch (error: any) {
      res.status(400).json({ error: error.message });
    }
  });

  app.post('/api/auth/login', async (req, res) => {
    const { email, password } = req.body;
    try {
      const user = db.prepare('SELECT * FROM users WHERE email = ?').get(email) as any;
      if (!user) {
        return res.status(401).json({ error: 'Utilisateur non trouvé' });
      }
      
      if (user.password) {
        const isMatch = await bcrypt.compare(password, user.password);
        if (!isMatch) {
          return res.status(401).json({ error: 'Mot de passe incorrect' });
        }
      } else if (password) {
        // User exists but has no password (maybe registered via Google previously)
        // We could set a password here or require them to use Google
        return res.status(401).json({ error: 'Ce compte utilise une connexion sociale. Veuillez utiliser Google.' });
      }

      const token = jwt.sign({ uid: user.uid, email: user.email }, JWT_SECRET, { expiresIn: '7d' });
      res.json({ success: true, user, token });
    } catch (error: any) {
      res.status(400).json({ error: error.message });
    }
  });

  app.get('/api/users/:uid', (req, res) => {
    const user = db.prepare('SELECT * FROM users WHERE uid = ?').get(req.params.uid);
    if (user) {
      res.json(user);
    } else {
      res.status(404).json({ error: 'User not found' });
    }
  });

  app.post('/api/users/:uid/update', (req, res) => {
    const { username, phone, country, credits } = req.body;
    try {
      const stmt = db.prepare('UPDATE users SET username = COALESCE(?, username), phone = COALESCE(?, phone), country = COALESCE(?, country), credits = COALESCE(?, credits) WHERE uid = ?');
      stmt.run(username, phone, country, credits, req.params.uid);
      res.json({ success: true });
    } catch (error: any) {
      res.status(400).json({ error: error.message });
    }
  });

  app.get('/api/activations/:resellerId', (req, res) => {
    const activations = db.prepare('SELECT * FROM activations WHERE resellerId = ? ORDER BY createdAt DESC').all(req.params.resellerId);
    res.json(activations);
  });

  app.post('/api/activations', (req, res) => {
    const { id, resellerId, target_mac, credits_used, note, playlist_url, xtream_data } = req.body;
    try {
      const user = db.prepare('SELECT credits FROM users WHERE uid = ?').get(resellerId) as any;
      if (!user || user.credits < credits_used) {
        return res.status(400).json({ error: 'Insufficient credits' });
      }

      const transaction = db.transaction(() => {
        db.prepare('INSERT INTO activations (id, resellerId, target_mac, credits_used, note, playlist_url, xtream_data) VALUES (?, ?, ?, ?, ?, ?, ?)').run(id, resellerId, target_mac, credits_used, note, playlist_url, JSON.stringify(xtream_data || null));
        db.prepare('UPDATE users SET credits = credits - ? WHERE uid = ?').run(credits_used, resellerId);
      });
      transaction();
      res.json({ success: true });
    } catch (error: any) {
      res.status(400).json({ error: error.message });
    }
  });

  app.get('/api/payments/:userId', (req, res) => {
    const payments = db.prepare('SELECT * FROM payments WHERE userId = ? ORDER BY createdAt DESC').all(req.params.userId);
    res.json(payments);
  });

  app.get('/api/check-mac/:mac', (req, res) => {
    const { mac } = req.params;
    const activation = db.prepare('SELECT * FROM activations WHERE target_mac = ?').get(mac) as any;
    
    if (activation) {
      const expiryDate = new Date(new Date(activation.createdAt).getTime() + 365 * 24 * 60 * 60 * 1000);
      res.json({
        active: true,
        expiry: expiryDate.toLocaleDateString('fr-FR'),
        last_seen: activation.last_connection || '2024-03-09 14:22',
        version: activation.version || 'v3.2.1',
        playlist_url: activation.playlist_url,
        xtream_data: activation.xtream_data ? JSON.parse(activation.xtream_data) : null
      });
    } else {
      res.json({
        active: false,
        error: "Adresse MAC non trouvée dans notre base d'activations."
      });
    }
  });

  app.post('/api/payments', (req, res) => {
    const { id, userId, amount, credits_purchased, payment_method, provider, status, external_id } = req.body;
    try {
      const transaction = db.transaction(() => {
        db.prepare('INSERT INTO payments (id, userId, amount, credits_purchased, payment_method, provider, status, external_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)').run(id, userId, amount, credits_purchased, payment_method, provider, status, external_id);
        if (status === 'completed') {
          db.prepare('UPDATE users SET credits = credits + ? WHERE uid = ?').run(credits_purchased, userId);
        }
      });
      transaction();
      res.json({ success: true });
    } catch (error: any) {
      res.status(400).json({ error: error.message });
    }
  });

  app.post("/api/payments/initiate", (req, res) => {
    const { userId, amount, phoneNumber, credits_purchased, paymentMethod } = req.body;
    const depositId = Math.random().toString(36).substr(2, 9);

    try {
      console.log(`Initiating ${paymentMethod} deposit for ${phoneNumber} with amount ${amount}`);
      
      const id = Math.random().toString(36).substr(2, 9);
      const transaction = db.transaction(() => {
        db.prepare('INSERT INTO payments (id, userId, amount, credits_purchased, payment_method, provider, status, external_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)').run(
          id, userId, amount, credits_purchased, paymentMethod, paymentMethod, 'pending', depositId
        );
      });
      transaction();

      res.json({ 
        success: true, 
        depositId,
        message: `Paiement ${paymentMethod} initié. Veuillez valider sur votre téléphone.` 
      });
    } catch (error: any) {
      console.error('Payment Error:', error);
      res.status(500).json({ error: 'Erreur lors de l\'initiation du paiement' });
    }
  });

  app.post('/api/user/profile', (req, res) => {
    const { username, email, phone, country } = req.body;
    // For now, we'll just update by email or uid if provided in headers/session
    // Since we don't have full session yet, we'll expect uid in body for this demo
    const { uid } = req.body; 
    try {
      const stmt = db.prepare('UPDATE users SET username = ?, email = ?, phone = ?, country = ? WHERE uid = ?');
      stmt.run(username, email, phone, country, uid);
      res.json({ success: true, user: { username, email, phone, country } });
    } catch (error: any) {
      res.status(400).json({ error: error.message });
    }
  });

  app.post("/api/user/bouquet", (req, res) => {
    const { userId, countries } = req.body;
    try {
      const stmt = db.prepare("INSERT OR REPLACE INTO user_bouquet (userId, countries) VALUES (?, ?)");
      stmt.run(userId, JSON.stringify(countries));
      res.json({ success: true });
    } catch (error: any) {
      res.status(400).json({ error: error.message });
    }
  });

  app.get("/api/user/m3u/:userId", (req, res) => {
    const { userId } = req.params;
    const row = db.prepare("SELECT countries FROM user_bouquet WHERE userId = ?").get(userId) as any;
    const selectedCountries = row ? JSON.parse(row.countries) : [];

    // Simulate filtering a master playlist
    const masterPlaylist = [
      { name: 'Canal+', country: 'France', url: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8' },
      { name: 'RTS1', country: 'Sénégal', url: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8' },
      { name: 'CRTV', country: 'Cameroun', url: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8' },
      { name: 'TF1', country: 'France', url: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8' },
    ];

    const filteredPlaylist = masterPlaylist.filter(ch => selectedCountries.includes(ch.country));
    
    // Generate M3U content
    let m3u = "#EXTM3U\n";
    filteredPlaylist.forEach(ch => {
      m3u += `#EXTINF:-1,${ch.name}\n${ch.url}\n`;
    });

    res.setHeader('Content-Type', 'audio/x-mpegurl');
    res.send(m3u);
  });

  app.get('/api/channels', (req, res) => {
    const channels = [
      { id: '1', name: 'TF1 HD', category: 'Généraliste', url: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8' },
      { id: '2', name: 'France 2', category: 'Généraliste', url: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8' },
      { id: '3', name: 'M6', category: 'Généraliste', url: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8' },
      { id: '4', name: 'Canal+ Sport', category: 'Sports', url: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8' },
      { id: '5', name: 'beIN Sports 1', category: 'Sports', url: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8' },
      { id: '6', name: 'Disney Channel', category: 'Enfants', url: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8' }
    ];
    res.json(channels);
  });

  // Vite middleware for development
  if (process.env.NODE_ENV !== 'production') {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    app.use(express.static('dist'));
    app.get('*', (req, res) => {
      res.sendFile(path.resolve(__dirname, 'dist', 'index.html'));
    });
  }

  const PORT = 3000;
  
  // Error handling middleware
  app.use((err: any, req: any, res: any, next: any) => {
    console.error('Unhandled Error:', err);
    res.status(500).json({ error: 'Internal Server Error' });
  });

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`
🚀 Sky Player Pro est prêt !
--------------------------------------------------
💻 Accès Web (Local) : http://localhost:${PORT}
📱 Accès Réseau (Mobile/TV) : http://<VOTRE_IP_LOCALE>:${PORT}
--------------------------------------------------
🛠️ Pour compiler sur Android/TV :
1. npm run build
2. npx cap sync
3. npx cap open android
--------------------------------------------------
    `);
  });
}

startServer();
