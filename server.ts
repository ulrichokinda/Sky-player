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
`);

async function startServer() {
  const app = express();
  app.use(cors());
  app.use(express.json());

  // Health check
  app.get('/api/health', (req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
  });

  // API Routes
  app.post('/api/auth/register', (req, res) => {
    const { uid, email, username, phone, country, role } = req.body;
    try {
      const stmt = db.prepare('INSERT OR IGNORE INTO users (uid, email, username, phone, country, role) VALUES (?, ?, ?, ?, ?, ?)');
      stmt.run(uid, email, username, phone, country, role || 'client');
      res.json({ success: true });
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
    const { id, resellerId, target_mac, credits_used, note } = req.body;
    try {
      const user = db.prepare('SELECT credits FROM users WHERE uid = ?').get(resellerId) as any;
      if (!user || user.credits < credits_used) {
        return res.status(400).json({ error: 'Insufficient credits' });
      }

      const transaction = db.transaction(() => {
        db.prepare('INSERT INTO activations (id, resellerId, target_mac, credits_used, note) VALUES (?, ?, ?, ?, ?)').run(id, resellerId, target_mac, credits_used, note);
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
        version: activation.version || 'v3.2.1'
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

  app.post('/api/payments/moneyfusion/initiate', async (req, res) => {
    const { userId, amount, phoneNumber, credits_purchased } = req.body;
    const apiKey = process.env.MONEYFUSION_API_KEY || "dummy_key";
    const depositId = Math.random().toString(36).substr(2, 9);

    try {
      // In a real scenario, we would call MoneyFusion API here
      console.log(`Initiating MoneyFusion deposit for ${phoneNumber} with amount ${amount}`);
      
      // Record the pending payment
      const id = Math.random().toString(36).substr(2, 9);
      const transaction = db.transaction(() => {
        db.prepare('INSERT INTO payments (id, userId, amount, credits_purchased, payment_method, provider, status, external_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)').run(
          id, userId, amount, credits_purchased, 'moneyfusion', 'moneyfusion', 'pending', depositId
        );
      });
      transaction();

      res.json({ 
        success: true, 
        depositId,
        message: "Paiement MoneyFusion initié. Veuillez valider sur votre téléphone." 
      });
    } catch (error: any) {
      console.error('MoneyFusion Error:', error);
      res.status(500).json({ error: 'Erreur lors de l\'initiation du paiement MoneyFusion' });
    }
  });

  app.post('/api/payments/yabetoopay/initiate', async (req, res) => {
    const { userId, amount, phoneNumber, credits_purchased, methodId } = req.body;
    const apiKey = process.env.YABETOO_API_KEY || "dummy_key";
    const depositId = Math.random().toString(36).substr(2, 9);

    try {
      // In a real scenario, we would call YabetooPay API here
      console.log(`Initiating YabetooPay deposit for ${phoneNumber} with amount ${amount} using ${methodId}`);
      
      // Record the pending payment
      const id = Math.random().toString(36).substr(2, 9);
      const transaction = db.transaction(() => {
        db.prepare('INSERT INTO payments (id, userId, amount, credits_purchased, payment_method, provider, status, external_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)').run(
          id, userId, amount, credits_purchased, methodId, 'yabetoopay', 'pending', depositId
        );
      });
      transaction();

      res.json({ 
        success: true, 
        depositId,
        message: `Paiement ${methodId} via YabetooPay initié. Veuillez valider sur votre téléphone.` 
      });
    } catch (error: any) {
      console.error('YabetooPay Error:', error);
      res.status(500).json({ error: 'Erreur lors de l\'initiation du paiement YabetooPay' });
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
    console.log(`Server running on http://0.0.0.0:${PORT}`);
  });
}

startServer();
