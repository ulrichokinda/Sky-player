import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import * as crypto from "crypto";
import {defineSecret} from "firebase-functions/params";

// ── Secrets ──────────────────────────────────────────────────────────────────
const joboostCashApiToken = defineSecret("JOBOOST_CASH_API_TOKEN");
const joboostCashWebhookSecret = defineSecret("JOBOOST_CASH_WEBHOOK_SECRET");
const activationApiKey = defineSecret("ACTIVATION_API_KEY");

// ── Init ─────────────────────────────────────────────────────────────────────
admin.initializeApp();
const db = admin.firestore();

// ── Constants ────────────────────────────────────────────────────────────────
const TRIAL_DAYS = 14;
const RATE_LIMIT_WINDOW_MS = 60_000; // 1 minute
const RATE_LIMIT_MAX = 100; // max requests per window per IP

// ── Rate Limiter (in-memory, per-function instance) ──────────────────────────
const rateLimitMap = new Map<string, {count: number; windowStart: number}>();

function isRateLimited(ip: string): boolean {
  const now = Date.now();
  const entry = rateLimitMap.get(ip);
  if (!entry || now - entry.windowStart > RATE_LIMIT_WINDOW_MS) {
    rateLimitMap.set(ip, {count: 1, windowStart: now});
    return false;
  }
  entry.count++;
  return entry.count > RATE_LIMIT_MAX;
}

// ── Helpers ──────────────────────────────────────────────────────────────────

/** Validate the X-Activation-API-Key header against the secret. */
function validateApiKey(req: functions.https.Request): boolean {
  const apiKey = activationApiKey.value();
  if (!apiKey) return true; // no secret configured → skip (dev mode)
  const provided =
    (req.headers["x-activation-api-key"] as string) ||
    (req.headers["x-app-key"] as string) ||
    "";
  return provided === apiKey;
}

/** Sanitize a MAC address to a canonical form (uppercase, colon-separated). */
function normalizeMac(mac: string): string {
  const cleaned = mac.replace(/[^a-fA-F0-9]/g, "").toUpperCase();
  if (cleaned.length !== 16) return mac; // return as-is if not standard
  return cleaned.match(/.{2}/g)?.join(":") ?? mac;
}

// ══════════════════════════════════════════════════════════════════════════════
// 1. POST /api/devices/check — Device status (trial / premium / expired)
// ══════════════════════════════════════════════════════════════════════════════
export const checkDeviceStatus = functions.https.onRequest(async (req, res) => {
  // CORS
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type, X-Activation-API-Key, X-App-Key");
  if (req.method === "OPTIONS") { res.status(204).send(); return; }

  if (req.method !== "POST") { res.status(405).json({error: "Method Not Allowed"}); return; }

  // Rate limit
  const ip = req.ip || "unknown";
  if (isRateLimited(ip)) { res.status(429).json({error: "Too many requests"}); return; }

  // API key
  if (!validateApiKey(req)) { res.status(401).json({error: "Invalid API key"}); return; }

  try {
    const {mac_address, android_id, app_id, brand, model, android_version} = req.body;
    if (!mac_address) { res.status(400).json({error: "mac_address required"}); return; }

    const mac = normalizeMac(mac_address);
    const now = Date.now();
    const trialEnd = now + TRIAL_DAYS * 24 * 60 * 60 * 1000;

    // Check existing activation in Firestore
    const activationsRef = db.collection("activations");
    const snapshot = await activationsRef.where("target_mac", "==", mac).limit(1).get();

    if (snapshot.empty) {
      // New device → create trial activation
      const activation = {
        target_mac: mac,
        status: "TRIAL",
        trial_start: now,
        trial_end: trialEnd,
        created_at: admin.firestore.FieldValue.serverTimestamp(),
        device_info: {brand, model, android_version, android_id, app_id},
        // Playlist fields (empty initially)
        playlist_url: null,
        playlist_name: null,
        type: "m3u",
        xtream_host: null,
        xtream_username: null,
        xtream_password: null,
      };

      await activationsRef.add(activation);

      res.status(200).json({
        status: "trial_active",
        days_remaining: TRIAL_DAYS,
        playlist_url: null,
        playlist_name: null,
        type: "m3u",
      });
      return;
    }

    // Existing activation
    const doc = snapshot.docs[0];
    const data = doc.data();
    const status = (data.status || "").toUpperCase();

    if (status === "TRIAL") {
      const trialEndTs = data.trial_end || trialEnd;
      if (now > trialEndTs) {
        // Trial expired
        await doc.ref.update({status: "TRIAL_EXPIRED", updated_at: admin.firestore.FieldValue.serverTimestamp()});
        res.status(200).json({status: "trial_expired"});
        return;
      }
      const daysRemaining = Math.ceil((trialEndTs - now) / (24 * 60 * 60 * 1000));
      res.status(200).json({
        status: "trial_active",
        days_remaining: daysRemaining,
        playlist_url: data.playlist_url || null,
        playlist_name: data.playlist_name || null,
        type: data.type || "m3u",
        xtream_username: data.xtream_username || null,
        xtream_password: data.xtream_password || null,
        xtream_server_url: data.xtream_host || null,
      });
      return;
    }

    if (status === "ACTIVE" || status === "ACTIF") {
      // Check expiry
      if (data.expires_at && now > data.expires_at) {
        await doc.ref.update({status: "EXPIRED", updated_at: admin.firestore.FieldValue.serverTimestamp()});
        res.status(200).json({status: "expired"});
        return;
      }
      res.status(200).json({
        status: "premium_active",
        playlist_url: data.playlist_url || null,
        playlist_name: data.playlist_name || null,
        type: data.type || "m3u",
        xtream_username: data.xtream_username || null,
        xtream_password: data.xtream_password || null,
        xtream_server_url: data.xtream_host || null,
      });
      return;
    }

    if (status === "EXPIRED" || status === "TRIAL_EXPIRED") {
      res.status(200).json({status: "expired"});
      return;
    }

    // Unknown status → treat as expired
    res.status(200).json({status: "expired"});
  } catch (error) {
    console.error("❌ checkDeviceStatus error:", error);
    res.status(500).json({error: "Internal Server Error"});
  }
});

// ══════════════════════════════════════════════════════════════════════════════
// 2. GET /api/v1/playlist/:mac — Playlist info for a MAC
// ══════════════════════════════════════════════════════════════════════════════
export const getMacPlaylist = functions.https.onRequest(async (req, res) => {
  // CORS
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "GET, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type, X-Activation-API-Key, X-App-Key");
  if (req.method === "OPTIONS") { res.status(204).send(); return; }

  // Rate limit
  const ip = req.ip || "unknown";
  if (isRateLimited(ip)) { res.status(429).json({error: "Too many requests"}); return; }

  // API key
  if (!validateApiKey(req)) { res.status(401).json({error: "Invalid API key"}); return; }

  try {
    const macRaw = req.params.mac;
    const mac = normalizeMac(Array.isArray(macRaw) ? macRaw[0] : macRaw);
    if (!mac) { res.status(400).json({error: "MAC address required"}); return; }

    const snapshot = await db.collection("activations")
      .where("target_mac", "==", mac).limit(1).get();

    if (snapshot.empty) {
      res.status(200).json({active: false, message: "No activation found"});
      return;
    }

    const doc = snapshot.docs[0];
    const data = doc.data();
    const status = (data.status || "").toUpperCase();
    const now = Date.now();

    const isActive = (status === "ACTIVE" || status === "ACTIF") &&
      (!data.expires_at || data.expires_at > now);

    if (!isActive) {
      res.status(200).json({active: false, message: "Activation inactive or expired"});
      return;
    }

    res.status(200).json({
      active: true,
      name: data.playlist_name || "Ma Playlist",
      playlist_url: data.playlist_url || "",
      xtream_host: data.xtream_host || null,
      xtream_username: data.xtream_username || "",
      xtream_password: data.xtream_password || "",
      expire: data.expires_at ? new Date(data.expires_at).toISOString() : null,
      message: "Playlist active",
    });
  } catch (error) {
    console.error("❌ getMacPlaylist error:", error);
    res.status(500).json({error: "Internal Server Error"});
  }
});

// ══════════════════════════════════════════════════════════════════════════════
// 3. GET /api/mac/check/:mac — MAC activation check (boolean)
// ══════════════════════════════════════════════════════════════════════════════
export const checkMacActivation = functions.https.onRequest(async (req, res) => {
  // CORS
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "GET, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type, X-Activation-API-Key");
  if (req.method === "OPTIONS") { res.status(204).send(); return; }

  // Rate limit
  const ip = req.ip || "unknown";
  if (isRateLimited(ip)) { res.status(429).json({error: "Too many requests"}); return; }

  // API key
  if (!validateApiKey(req)) { res.status(401).json({error: "Invalid API key"}); return; }

  try {
    const macRaw = req.params.mac;
    const mac = normalizeMac(Array.isArray(macRaw) ? macRaw[0] : macRaw);
    if (!mac) { res.status(400).json({error: "MAC address required"}); return; }

    const snapshot = await db.collection("activations")
      .where("target_mac", "==", mac).limit(1).get();

    if (snapshot.empty) {
      res.status(200).json({active: false, error: "No activation found"});
      return;
    }

    const doc = snapshot.docs[0];
    const data = doc.data();
    const status = (data.status || "").toUpperCase();
    const now = Date.now();

    const isActive = (status === "ACTIVE" || status === "ACTIF") &&
      (!data.expires_at || data.expires_at > now);

    res.status(200).json({
      active: isActive,
      activation: isActive ? data : null,
      error: isActive ? null : "Activation inactive or expired",
    });
  } catch (error) {
    console.error("❌ checkMacActivation error:", error);
    res.status(500).json({error: "Internal Server Error"});
  }
});

// ══════════════════════════════════════════════════════════════════════════════
// 4. POST /api/joboost-cash — Payment webhook (with idempotency)
// ══════════════════════════════════════════════════════════════════════════════
export const joboostCashWebhook = functions
  .runWith({secrets: [joboostCashWebhookSecret]})
  .https.onRequest(async (req, res) => {
    // CORS — allow Joboost Cash domain + fallback
    res.set("Access-Control-Allow-Origin", "*");
    res.set("Access-Control-Allow-Methods", "POST, OPTIONS");
    res.set("Access-Control-Allow-Headers", "Content-Type, X-Joboost-Cash-Signature");
    if (req.method === "OPTIONS") { res.status(204).send(); return; }

    if (req.method !== "POST") { res.status(405).json({error: "Method Not Allowed"}); return; }

    // Rate limit
    const ip = req.ip || "unknown";
    if (isRateLimited(ip)) { res.status(429).json({error: "Too many requests"}); return; }

    try {
      const payload = req.body;
      console.log("📥 Webhook Joboost Cash:", JSON.stringify(payload, null, 2));

      // Signature verification
      const signatureHeader = req.headers["x-joboost-cash-signature"];
      const signature = Array.isArray(signatureHeader) ? signatureHeader[0] : signatureHeader;
      const webhookSecret = joboostCashWebhookSecret.value();

      if (webhookSecret) {
        if (!signature) {
          console.error("❌ Missing webhook signature");
          res.status(401).json({error: "Missing signature"});
          return;
        }
        if (!verifyWebhookSignature(req.rawBody, signature, webhookSecret)) {
          console.error("❌ Invalid webhook signature");
          res.status(401).json({error: "Invalid signature"});
          return;
        }
      }

      const {
        transactionId, paymentId, depositId,
        status, phoneNumber, amount, currency,
        timestamp, metadata,
      } = payload;

      const providerPaymentId = transactionId || paymentId || depositId;
      if (!providerPaymentId || !status) {
        res.status(400).json({error: "transactionId/paymentId/depositId and status required"});
        return;
      }

      // ── IDEMPOTENCY: check if this transaction was already processed ──
      const existingPayment = await db.collection("payments")
        .where("provider_payment_id", "==", providerPaymentId).limit(1).get();
      if (!existingPayment.empty) {
        console.log(`ℹ️ Transaction ${providerPaymentId} already processed, skipping`);
        res.status(200).json({success: true, message: "Already processed", duplicate: true});
        return;
      }

      // Find activation by licenseId from metadata or payment ID
      const activationId = metadata?.licenseId || metadata?.userId || extractActivationFromPaymentId(providerPaymentId);
      if (!activationId) {
        res.status(400).json({error: "activationId not found in metadata"});
        return;
      }

      const isActive = mapPaymentStatusToActive(status);
      const updateData: Record<string, unknown> = {
        status: isActive ? "ACTIVE" : "INACTIVE",
        payment_provider: "joboost-cash",
        payment_status: status,
        last_payment_update: admin.firestore.FieldValue.serverTimestamp(),
        updated_at: admin.firestore.FieldValue.serverTimestamp(),
      };
      if (phoneNumber) updateData.phone_number = phoneNumber;
      if (amount) updateData.amount = amount;
      if (currency) updateData.currency = currency;
      if (timestamp) updateData.payment_timestamp = timestamp;

      // Update activation document
      const activationRef = db.collection("activations").doc(activationId);
      await activationRef.update(updateData);

      // Record payment (idempotent — new document per transaction)
      await db.collection("payments").add({
        activation_id: activationId,
        provider_payment_id: providerPaymentId,
        status,
        amount: amount || null,
        currency: currency || null,
        phone_number: phoneNumber || null,
        metadata: metadata || null,
        created_at: admin.firestore.FieldValue.serverTimestamp(),
      });

      console.log(`✅ Activation ${activationId} updated — isActive: ${isActive}, status: ${status}`);

      res.status(200).json({
        success: true,
        message: "Webhook processed",
        activationId,
        isActive,
        status,
        providerPaymentId,
      });
    } catch (error) {
      console.error("❌ Webhook error:", error);
      res.status(500).json({error: "Internal Server Error"});
    }
  });

// ══════════════════════════════════════════════════════════════════════════════
// 5. GET /api/check-status?activationId=xxx — Check payment status
// ══════════════════════════════════════════════════════════════════════════════
export const checkPaymentStatus = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  if (req.method === "OPTIONS") { res.status(204).send(); return; }

  const activationId = req.query.activationId as string;
  if (!activationId) { res.status(400).json({error: "activationId required"}); return; }

  try {
    const doc = await db.collection("activations").doc(activationId).get();
    if (!doc.exists) { res.status(404).json({error: "Activation not found"}); return; }

    const data = doc.data()!;
    res.status(200).json({
      activationId,
      isActive: data.status === "ACTIVE" || data.status === "ACTIF",
      paymentProvider: data.payment_provider || "unknown",
      paymentStatus: data.payment_status || "UNKNOWN",
      lastUpdate: data.last_payment_update?.toDate?.()?.toISOString() || null,
    });
  } catch (error) {
    console.error("❌ checkPaymentStatus error:", error);
    res.status(500).json({error: "Internal Server Error"});
  }
});

// ══════════════════════════════════════════════════════════════════════════════
// 6. updateLicenseStatus — Callable (admin only)
// ══════════════════════════════════════════════════════════════════════════════
export const updateLicenseStatus = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Auth required");
  }

  const {activationId, isActive, reason} = data;
  if (!activationId || typeof isActive !== "boolean") {
    throw new functions.https.HttpsError("invalid-argument", "activationId and isActive required");
  }

  try {
    await db.collection("activations").doc(activationId).update({
      status: isActive ? "ACTIVE" : "INACTIVE",
      manual_update: true,
      updated_by: context.auth.uid,
      update_reason: reason || "Manual update",
      updated_at: admin.firestore.FieldValue.serverTimestamp(),
    });

    return {success: true, activationId, isActive, updatedBy: context.auth.uid};
  } catch (error) {
    console.error("❌ updateLicenseStatus error:", error);
    throw new functions.https.HttpsError("internal", "Update failed");
  }
});

// ══════════════════════════════════════════════════════════════════════════════
// 7. verifyJoboostCashPayment — Callable (now implemented)
// ══════════════════════════════════════════════════════════════════════════════
export const verifyJoboostCashPayment = functions
  .runWith({secrets: [joboostCashApiToken]})
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "Auth required");
    }

    const {paymentId} = data;
    if (!paymentId) {
      throw new functions.https.HttpsError("invalid-argument", "paymentId required");
    }

    const apiToken = joboostCashApiToken.value();
    if (!apiToken) {
      throw new functions.https.HttpsError("failed-precondition", "Joboost Cash API token not configured");
    }

    try {
      // Call Joboost Cash API to verify payment status
      const response = await fetch(`https://api.joboost-cash.com/v1/payments/${paymentId}`, {
        headers: {
          "Authorization": `Bearer ${apiToken}`,
          "Content-Type": "application/json",
        },
      });

      if (!response.ok) {
        console.error(`❌ Joboost Cash API error: ${response.status}`);
        return {success: false, paymentId, provider: "joboost-cash", error: `API returned ${response.status}`};
      }

      const result = await response.json();
      return {
        success: true,
        paymentId,
        provider: "joboost-cash",
        status: result.status || "UNKNOWN",
        details: result,
      };
    } catch (error) {
      console.error("❌ verifyJoboostCashPayment error:", error);
      return {success: false, paymentId, provider: "joboost-cash", error: "Verification failed"};
    }
  });

// ══════════════════════════════════════════════════════════════════════════════
// Internal helpers
// ══════════════════════════════════════════════════════════════════════════════

function verifyWebhookSignature(rawBody: Buffer, signature: string, secret: string): boolean {
  try {
    const normalizedSignature = signature.trim();
    const expectedSignature = crypto.createHmac("sha256", secret).update(rawBody).digest("hex");
    const provided = Buffer.from(normalizedSignature, "utf8");
    const computed = Buffer.from(expectedSignature, "utf8");
    if (provided.length !== computed.length) return false;
    return crypto.timingSafeEqual(provided, computed);
  } catch {
    return false;
  }
}

function mapPaymentStatusToActive(paymentStatus: string): boolean {
  const active = ["COMPLETED", "SUCCESS", "CONFIRMED", "ACCEPTED", "PAID"];
  const inactive = ["FAILED", "REJECTED", "CANCELLED", "EXPIRED", "PENDING", "INITIATED"];
  const s = paymentStatus.toUpperCase();
  if (active.includes(s)) return true;
  if (inactive.includes(s)) return false;
  console.warn(`⚠️ Unknown payment status: ${paymentStatus}`);
  return false;
}

function extractActivationFromPaymentId(paymentId: string): string | null {
  const match = paymentId.match(/LICENSE_([^_]+)/i) || paymentId.match(/ACTIVATION_([^_]+)/i);
  return match ? match[1] : null;
}
