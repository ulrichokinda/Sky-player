import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import {defineSecret} from "firebase-functions/params";

// ══════════════════════════════════════════════════════════════════════════════
// INIT
// ══════════════════════════════════════════════════════════════════════════════
admin.initializeApp();
const db = admin.firestore();
const messaging = admin.messaging();

// ── Secrets ──────────────────────────────────────────────────────────────────
const activationApiKey = defineSecret("ACTIVATION_API_KEY");
const joboostCashApiToken = defineSecret("JOBOOST_CASH_API_TOKEN");
const joboostCashWebhookSecret = defineSecret("JOBOOST_CASH_WEBHOOK_SECRET");

// ── Constants ────────────────────────────────────────────────────────────────
const TRIAL_DAYS = 14;


// ══════════════════════════════════════════════════════════════════════════════
// B5: RATE LIMITING PERSISTANT (Firestore, survives cold starts)
// ══════════════════════════════════════════════════════════════════════════════
const RATE_LIMIT_WINDOW_MS = 60_000;
const RATE_LIMIT_MAX = 100;

async function isRateLimited(ip: string): Promise<boolean> {
  const now = Date.now();
  const windowStart = now - RATE_LIMIT_WINDOW_MS;
  const ref = db.collection("rate_limits").doc(ip);

  try {
    const result = await db.runTransaction(async (tx) => {
      const doc = await tx.get(ref);
      const data = doc.data();

      if (!data || data.windowStart < windowStart) {
        // New window
        tx.set(ref, {count: 1, windowStart: now});
        return false;
      }

      if (data.count >= RATE_LIMIT_MAX) {
        return true;
      }

      tx.update(ref, {count: data.count + 1});
      return false;
    });

    return result;
  } catch (error) {
    // Fallback: allow if Firestore fails
    console.warn("⚠️ Rate limit check failed, allowing:", error);
    return false;
  }
}

// ══════════════════════════════════════════════════════════════════════════════
// B6: LOGGING STRUCTURÉ + MÉTRIQUES
// ══════════════════════════════════════════════════════════════════════════════
async function logMetric(
  event: string,
  data: Record<string, unknown>
): Promise<void> {
  try {
    await db.collection("metrics").add({
      event,
      ...data,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
    });
  } catch (error) {
    console.warn("⚠️ Metric log failed:", error);
  }
}

// ══════════════════════════════════════════════════════════════════════════════
// HELPERS
// ══════════════════════════════════════════════════════════════════════════════
function validateApiKey(req: functions.https.Request): boolean {
  const apiKey = activationApiKey.value();
  if (!apiKey) return true;
  const provided =
    (req.headers["x-activation-api-key"] as string) ||
    (req.headers["x-app-key"] as string) ||
    "";
  return provided === apiKey;
}

function normalizeMac(mac: string): string {
  const cleaned = mac.replace(/[^a-fA-F0-9]/g, "").toUpperCase();
  if (cleaned.length !== 16) return mac;
  return cleaned.match(/.{2}/g)?.join(":") ?? mac;
}

function setCorsHeaders(res: functions.Response): void {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  res.set(
    "Access-Control-Allow-Headers",
    "Content-Type, X-Activation-API-Key, X-App-Key, X-API-Version"
  );
}

function sendCors(res: functions.Response): boolean {
  if (res.req.method === "OPTIONS") {
    res.status(204).send();
    return true;
  }
  return false;
}

// ══════════════════════════════════════════════════════════════════════════════
// B2: MULTI-REVENDEUR — Schéma Firestore
// ══════════════════════════════════════════════════════════════════════════════
// Les activations ont un champ optionnel `reseller_id`
// Les revendeurs ont une collection `resellers` avec leurs credentials

// ══════════════════════════════════════════════════════════════════════════════
// 1. POST /api/v2/devices/check — Device status (B8: versionné)
// ══════════════════════════════════════════════════════════════════════════════
export const checkDeviceStatus = functions.https.onRequest(async (req, res) => {
  setCorsHeaders(res);
  if (sendCors(res)) return;

  if (req.method !== "POST") {
    res.status(405).json({error: "Method Not Allowed"});
    return;
  }

  const ip = req.ip || "unknown";
  if (await isRateLimited(ip)) {
    res.status(429).json({error: "Too many requests"});
    return;
  }

  if (!validateApiKey(req)) {
    res.status(401).json({error: "Invalid API key"});
    return;
  }

  try {
    const {
      mac_address, android_id, app_id,
      hardware_fingerprint, brand, model, android_version,
      reseller_id, // B2: multi-revendeur
    } = req.body;

    if (!mac_address) {
      res.status(400).json({error: "mac_address required"});
      return;
    }

    const mac = normalizeMac(mac_address);
    const now = Date.now();
    const trialEnd = now + TRIAL_DAYS * 24 * 60 * 60 * 1000;

    const activationsRef = db.collection("activations");
    const snapshot = await activationsRef
      .where("target_mac", "==", mac)
      .limit(1)
      .get();

    if (snapshot.empty) {
      // ── B10: IDEMPOTENCE — vérifier si une activation récente existe pour ce MAC ──
      const recentCheck = await activationsRef
        .where("target_mac", "==", mac)
        .orderBy("created_at", "desc")
        .limit(1)
        .get();

      if (!recentCheck.empty) {
        const recent = recentCheck.docs[0];
        const recentData = recent.data();
        const recentCreated = recentData.created_at?.toMillis?.() || 0;
        if (now - recentCreated < 300_000) {
          // 5 min idempotence window
          console.log(`ℹ️ Idempotent checkDeviceStatus for ${mac} (created ${now - recentCreated}ms ago)`);
          res.status(200).json({
            status: mapStatusToResponse(recentData.status),
            days_remaining: recentData.trial_end
              ? Math.ceil((recentData.trial_end - now) / 86400000)
              : undefined,
            playlist_url: recentData.playlist_url || null,
            playlist_name: recentData.playlist_name || null,
            type: recentData.type || "m3u",
            xtream_username: recentData.xtream_username || null,
            xtream_password: recentData.xtream_password || null,
            xtream_server_url: recentData.xtream_host || null,
          });
          return;
        }
      }

      // ── ANTI-REINSTALL ──
      if (hardware_fingerprint) {
        const hwSnapshot = await activationsRef
          .where("hardware_fingerprint", "==", hardware_fingerprint)
          .limit(1)
          .get();

        if (!hwSnapshot.empty) {
          const existingDoc = hwSnapshot.docs[0];
          const existingData = existingDoc.data();
          const existingStatus = (existingData.status || "").toUpperCase();

          console.log(
            `🔒 Hardware fingerprint match: MAC ${mac} → existing MAC ${existingData.target_mac}`
          );

          await existingDoc.ref.update({
            target_mac: mac,
            hardware_fingerprint,
            device_info: {brand, model, android_version, android_id, app_id},
            remapped_at: admin.firestore.FieldValue.serverTimestamp(),
          });

          const existingTrialEnd = existingData.trial_end || trialEnd;
          if (existingStatus === "TRIAL" && now <= existingTrialEnd) {
            const daysRemaining = Math.ceil(
              (existingTrialEnd - now) / 86400000
            );
            res.status(200).json({
              status: "trial_active",
              days_remaining: daysRemaining,
              playlist_url: existingData.playlist_url || null,
              playlist_name: existingData.playlist_name || null,
              type: existingData.type || "m3u",
            });
            return;
          }
          if (existingStatus === "ACTIVE" || existingStatus === "ACTIF") {
            res.status(200).json({
              status: "premium_active",
              playlist_url: existingData.playlist_url || null,
              playlist_name: existingData.playlist_name || null,
              type: existingData.type || "m3u",
              xtream_username: existingData.xtream_username || null,
              xtream_password: existingData.xtream_password || null,
              xtream_server_url: existingData.xtream_host || null,
            });
            return;
          }
          res.status(200).json({status: "expired"});
          return;
        }
      }

      // Genuinely new device → create trial activation
      const activation = {
        target_mac: mac,
        hardware_fingerprint: hardware_fingerprint || null,
        status: "TRIAL",
        trial_start: now,
        trial_end: trialEnd,
        reseller_id: reseller_id || null, // B2
        created_at: admin.firestore.FieldValue.serverTimestamp(),
        device_info: {brand, model, android_version, android_id, app_id},
        playlist_url: null,
        playlist_name: null,
        type: "m3u",
        xtream_host: null,
        xtream_username: null,
        xtream_password: null,
      };

      await activationsRef.add(activation);

      // B6: Métriques
      await logMetric("device_new", {mac, brand, model, reseller_id});

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
        await doc.ref.update({
          status: "TRIAL_EXPIRED",
          updated_at: admin.firestore.FieldValue.serverTimestamp(),
        });

        // B3: Notification trial expiré
        await sendNotification(data.device_info?.android_id, {
          title: "Essai terminé",
          body: "Votre période d'essai est terminée. Contactez votre revendeur pour activer.",
          type: "trial_expired",
        });

        // B6: Métriques
        await logMetric("trial_expired", {mac});

        res.status(200).json({status: "trial_expired"});
        return;
      }
      const daysRemaining = Math.ceil((trialEndTs - now) / 86400000);
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
      if (data.expires_at && now > data.expires_at) {
        await doc.ref.update({
          status: "EXPIRED",
          updated_at: admin.firestore.FieldValue.serverTimestamp(),
        });
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

    res.status(200).json({status: "expired"});
  } catch (error) {
    console.error("❌ checkDeviceStatus error:", error);
    res.status(500).json({error: "Internal Server Error"});
  }
});

// ══════════════════════════════════════════════════════════════════════════════
// 2. GET /api/v2/playlist/:mac — Playlist info
// ══════════════════════════════════════════════════════════════════════════════
export const getMacPlaylist = functions.https.onRequest(async (req, res) => {
  setCorsHeaders(res);
  if (sendCors(res)) return;

  const ip = req.ip || "unknown";
  if (await isRateLimited(ip)) {
    res.status(429).json({error: "Too many requests"});
    return;
  }

  if (!validateApiKey(req)) {
    res.status(401).json({error: "Invalid API key"});
    return;
  }

  try {
    const macRaw = req.params.mac;
    const mac = normalizeMac(Array.isArray(macRaw) ? macRaw[0] : macRaw);
    if (!mac) {
      res.status(400).json({error: "MAC address required"});
      return;
    }

    const snapshot = await db
      .collection("activations")
      .where("target_mac", "==", mac)
      .limit(1)
      .get();

    if (snapshot.empty) {
      res.status(200).json({active: false, message: "No activation found"});
      return;
    }

    const doc = snapshot.docs[0];
    const data = doc.data();
    const status = (data.status || "").toUpperCase();
    const now = Date.now();

    const isActive =
      (status === "ACTIVE" || status === "ACTIF") &&
      (!data.expires_at || data.expires_at > now);

    if (!isActive) {
      res.status(200).json({active: false, message: "Activation inactive or expired"});
      return;
    }

    // B6: Métriques
    await logMetric("playlist_accessed", {mac, playlist_name: data.playlist_name});

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
// 3. GET /api/mac/check/:mac — MAC activation check
// ══════════════════════════════════════════════════════════════════════════════
export const checkMacActivation = functions.https.onRequest(async (req, res) => {
  setCorsHeaders(res);
  if (sendCors(res)) return;

  const ip = req.ip || "unknown";
  if (await isRateLimited(ip)) {
    res.status(429).json({error: "Too many requests"});
    return;
  }

  if (!validateApiKey(req)) {
    res.status(401).json({error: "Invalid API key"});
    return;
  }

  try {
    const macRaw = req.params.mac;
    const mac = normalizeMac(Array.isArray(macRaw) ? macRaw[0] : macRaw);
    if (!mac) {
      res.status(400).json({error: "MAC address required"});
      return;
    }

    const snapshot = await db
      .collection("activations")
      .where("target_mac", "==", mac)
      .limit(1)
      .get();

    if (snapshot.empty) {
      res.status(200).json({active: false, error: "No activation found"});
      return;
    }

    const doc = snapshot.docs[0];
    const data = doc.data();
    const status = (data.status || "").toUpperCase();
    const now = Date.now();

    const isActive =
      (status === "ACTIVE" || status === "ACTIF") &&
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
// B3: NOTIFICATIONS FCM
// ══════════════════════════════════════════════════════════════════════════════
async function sendNotification(
  androidId: string | undefined,
  payload: {title: string; body: string; type: string}
): Promise<void> {
  if (!androidId) return;

  try {
    // Chercher le token FCM associé à ce device
    const tokenDoc = await db
      .collection("fcm_tokens")
      .doc(androidId)
      .get();

    if (!tokenDoc.exists) {
      console.log(`ℹ️ No FCM token for device ${androidId}`);
      return;
    }

    const token = tokenDoc.data()?.token;
    if (!token) return;

    await messaging.send({
      token,
      notification: {
        title: payload.title,
        body: payload.body,
      },
      data: {
        type: payload.type,
        timestamp: Date.now().toString(),
      },
      android: {
        priority: "high",
        notification: {
          channelId: "skyplayer_notifications",
        },
      },
    });

    console.log(`✅ Notification sent to ${androidId}: ${payload.type}`);
  } catch (error) {
    console.warn(`⚠️ Failed to send notification to ${androidId}:`, error);
  }
}

// ══════════════════════════════════════════════════════════════════════════════
// B4: WEBHOOK PAIEMENT MODULAIRE (Provider Registry)
// ══════════════════════════════════════════════════════════════════════════════
interface PaymentProvider {
  name: string;
  verifySignature: (rawBody: Buffer, signature: string, secret: string) => boolean;
  extractPaymentId: (payload: Record<string, unknown>) => string | null;
  mapStatus: (status: string) => boolean;
}

const paymentProviders: Record<string, PaymentProvider> = {
  "joboost-cash": {
    name: "Joboost Cash",
    verifySignature: (rawBody, signature, secret) => {
      try {
        const crypto = require("crypto");
        const expected = crypto.createHmac("sha256", secret).update(rawBody).digest("hex");
        const provided = Buffer.from(signature.trim(), "utf8");
        const computed = Buffer.from(expected, "utf8");
        if (provided.length !== computed.length) return false;
        return crypto.timingSafeEqual(provided, computed);
      } catch {
        return false;
      }
    },
    extractPaymentId: (payload) =>
      (payload.transactionId as string) ||
      (payload.paymentId as string) ||
      (payload.depositId as string) ||
      null,
    mapStatus: (status) => {
      const active = ["COMPLETED", "SUCCESS", "CONFIRMED", "ACCEPTED", "PAID"];
      return active.includes(status.toUpperCase());
    },
  },

  // Ajouter d'autres providers ici :
  // "stripe": { ... },
  // "paypal": { ... },
  // "orange-money": { ... },
};

// ══════════════════════════════════════════════════════════════════════════════
// 4. POST /api/joboost-cash — Payment webhook (B9: générique)
// ══════════════════════════════════════════════════════════════════════════════
export const joboostCashWebhook = functions
  .runWith({secrets: [joboostCashWebhookSecret]})
  .https.onRequest(async (req, res) => {
    setCorsHeaders(res);
    if (sendCors(res)) return;

    if (req.method !== "POST") {
      res.status(405).json({error: "Method Not Allowed"});
      return;
    }

    const ip = req.ip || "unknown";
    if (await isRateLimited(ip)) {
      res.status(429).json({error: "Too many requests"});
      return;
    }

    try {
      const payload = req.body;
      console.log("📥 Webhook Joboost Cash:", JSON.stringify(payload, null, 2));

      // B9: Signature verification via provider registry
      const provider = paymentProviders["joboost-cash"];
      const webhookSecret = joboostCashWebhookSecret.value();

      if (webhookSecret) {
        const signatureHeader = req.headers["x-joboost-cash-signature"];
        const signature = Array.isArray(signatureHeader)
          ? signatureHeader[0]
          : signatureHeader;

        if (!signature) {
          console.error("❌ Missing webhook signature");
          res.status(401).json({error: "Missing signature"});
          return;
        }

        if (!provider.verifySignature(req.rawBody, signature, webhookSecret)) {
          console.error("❌ Invalid webhook signature");
          res.status(401).json({error: "Invalid signature"});
          return;
        }
      }

      const {status, phoneNumber, amount, currency, timestamp, metadata} =
        payload;

      const providerPaymentId = provider.extractPaymentId(payload);
      if (!providerPaymentId || !status) {
        res.status(400).json({
          error: "transactionId/paymentId/depositId and status required",
        });
        return;
      }

      // Idempotency
      const existingPayment = await db
        .collection("payments")
        .where("provider_payment_id", "==", providerPaymentId)
        .limit(1)
        .get();

      if (!existingPayment.empty) {
        console.log(
          `ℹ️ Transaction ${providerPaymentId} already processed, skipping`
        );
        res.status(200).json({
          success: true,
          message: "Already processed",
          duplicate: true,
        });
        return;
      }

      const activationId =
        metadata?.licenseId ||
        metadata?.userId ||
        extractActivationFromPaymentId(providerPaymentId);

      if (!activationId) {
        res.status(400).json({error: "activationId not found in metadata"});
        return;
      }

      const isActive = provider.mapStatus(status);
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

      const activationRef = db.collection("activations").doc(activationId);
      await activationRef.update(updateData);

      // Record payment
      await db.collection("payments").add({
        activation_id: activationId,
        provider_payment_id: providerPaymentId,
        provider: "joboost-cash",
        status,
        amount: amount || null,
        currency: currency || null,
        phone_number: phoneNumber || null,
        metadata: metadata || null,
        created_at: admin.firestore.FieldValue.serverTimestamp(),
      });

      // B3: Notification activation
      if (isActive) {
        const activationDoc = await activationRef.get();
        const activationData = activationDoc.data();
        await sendNotification(activationData?.device_info?.android_id, {
          title: "✅ Activation réussie",
          body: "Votre application est maintenant activée. Profitez de SkyPlayer !",
          type: "activated",
        });
      }

      // B6: Métriques
      await logMetric("payment_received", {
        provider: "joboost-cash",
        providerPaymentId,
        isActive,
        amount,
        currency,
      });

      console.log(
        `✅ Activation ${activationId} updated — isActive: ${isActive}`
      );

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
// 5. GET /api/check-status?activationId=xxx
// ══════════════════════════════════════════════════════════════════════════════
export const checkPaymentStatus = functions.https.onRequest(async (req, res) => {
  setCorsHeaders(res);
  if (sendCors(res)) return;

  const activationId = req.query.activationId as string;
  if (!activationId) {
    res.status(400).json({error: "activationId required"});
    return;
  }

  try {
    const doc = await db.collection("activations").doc(activationId).get();
    if (!doc.exists) {
      res.status(404).json({error: "Activation not found"});
      return;
    }

    const data = doc.data()!;
    res.status(200).json({
      activationId,
      isActive: data.status === "ACTIVE" || data.status === "ACTIF",
      paymentProvider: data.payment_provider || "unknown",
      paymentStatus: data.payment_status || "UNKNOWN",
      lastUpdate:
        data.last_payment_update?.toDate?.()?.toISOString() || null,
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
    throw new functions.https.HttpsError(
      "invalid-argument",
      "activationId and isActive required"
    );
  }

  try {
    const updateData: Record<string, unknown> = {
      status: isActive ? "ACTIVE" : "INACTIVE",
      manual_update: true,
      updated_by: context.auth.uid,
      update_reason: reason || "Manual update",
      updated_at: admin.firestore.FieldValue.serverTimestamp(),
    };

    const activationRef = db.collection("activations").doc(activationId);
    await activationRef.update(updateData);

    // B3: Notification activation
    if (isActive) {
      const activationDoc = await activationRef.get();
      const activationData = activationDoc.data();
      await sendNotification(activationData?.device_info?.android_id, {
        title: "✅ Activation réussie",
        body: "Votre application est maintenant activée.",
        type: "activated",
      });
    }

    // B6: Métriques
    await logMetric("license_updated", {
      activationId,
      isActive,
      updatedBy: context.auth.uid,
    });

    return {
      success: true,
      activationId,
      isActive,
      updatedBy: context.auth.uid,
    };
  } catch (error) {
    console.error("❌ updateLicenseStatus error:", error);
    throw new functions.https.HttpsError("internal", "Update failed");
  }
});

// ══════════════════════════════════════════════════════════════════════════════
// 7. verifyJoboostCashPayment — Callable
// ══════════════════════════════════════════════════════════════════════════════
export const verifyJoboostCashPayment = functions
  .runWith({secrets: [joboostCashApiToken]})
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "Auth required");
    }

    const {paymentId} = data;
    if (!paymentId) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "paymentId required"
      );
    }

    const apiToken = joboostCashApiToken.value();
    if (!apiToken) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Joboost Cash API token not configured"
      );
    }

    try {
      const response = await fetch(
        `https://api.joboost-cash.com/v1/payments/${paymentId}`,
        {
          headers: {
            Authorization: `Bearer ${apiToken}`,
            "Content-Type": "application/json",
          },
        }
      );

      if (!response.ok) {
        console.error(`❌ Joboost Cash API error: ${response.status}`);
        return {
          success: false,
          paymentId,
          provider: "joboost-cash",
          error: `API returned ${response.status}`,
        };
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
      return {
        success: false,
        paymentId,
        provider: "joboost-cash",
        error: "Verification failed",
      };
    }
  });

// ══════════════════════════════════════════════════════════════════════════════
// B2: ENDPOINTS MULTI-REVENDEUR
// ══════════════════════════════════════════════════════════════════════════════

// GET /api/v2/reseller/:id/clients — Liste les clients d'un revendeur
export const getResellerClients = functions.https.onRequest(async (req, res) => {
  setCorsHeaders(res);
  if (sendCors(res)) return;

  if (!validateApiKey(req)) {
    res.status(401).json({error: "Invalid API key"});
    return;
  }

  const ip = req.ip || "unknown";
  if (await isRateLimited(ip)) {
    res.status(429).json({error: "Too many requests"});
    return;
  }

  try {
    const resellerId = req.params.id;
    if (!resellerId) {
      res.status(400).json({error: "reseller_id required"});
      return;
    }

    const snapshot = await db
      .collection("activations")
      .where("reseller_id", "==", resellerId)
      .orderBy("created_at", "desc")
      .limit(100)
      .get();

    const clients = snapshot.docs.map((doc) => ({
      id: doc.id,
      ...doc.data(),
    }));

    // B6: Métriques
    await logMetric("reseller_clients_viewed", {
      resellerId,
      count: clients.length,
    });

    res.status(200).json({reseller_id: resellerId, count: clients.length, clients});
  } catch (error) {
    console.error("❌ getResellerClients error:", error);
    res.status(500).json({error: "Internal Server Error"});
  }
});

// POST /api/v2/reseller/create — Créer un compte revendeur
export const createReseller = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Auth required");
  }

  const {name, email, phone} = data;
  if (!name || !email) {
    throw new functions.https.HttpsError("invalid-argument", "name and email required");
  }

  try {
    const resellerId = `RSL_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`;

    await db.collection("resellers").doc(resellerId).set({
      name,
      email,
      phone: phone || null,
      status: "ACTIVE",
      created_by: context.auth.uid,
      created_at: admin.firestore.FieldValue.serverTimestamp(),
      updated_at: admin.firestore.FieldValue.serverTimestamp(),
    });

    // B6: Métriques
    await logMetric("reseller_created", {resellerId, name});

    return {success: true, reseller_id: resellerId};
  } catch (error) {
    console.error("❌ createReseller error:", error);
    throw new functions.https.HttpsError("internal", "Create failed");
  }
});

// ══════════════════════════════════════════════════════════════════════════════
// B7: EXPIRATION AUTOMATIQUE DES PLAYLISTS (Scheduled Function)
// ══════════════════════════════════════════════════════════════════════════════
export const cleanupExpiredActivations = functions.pubsub
  .schedule("every 24 hours")
  .onRun(async () => {
    const now = Date.now();
    let cleaned = 0;

    try {
      // 1. Expire les trials dépassés
      const expiredTrials = await db
        .collection("activations")
        .where("status", "==", "TRIAL")
        .where("trial_end", "<", now)
        .get();

      const batch1 = db.batch();
      for (const doc of expiredTrials.docs) {
        batch1.update(doc.ref, {
          status: "TRIAL_EXPIRED",
          updated_at: admin.firestore.FieldValue.serverTimestamp(),
        });
        cleaned++;
      }
      if (expiredTrials.size > 0) await batch1.commit();

      // 2. Expire les premium dépassés
      const expiredPremium = await db
        .collection("activations")
        .where("status", "in", ["ACTIVE", "ACTIF"])
        .where("expires_at", "<", now)
        .get();

      const batch2 = db.batch();
      for (const doc of expiredPremium.docs) {
        batch2.update(doc.ref, {
          status: "EXPIRED",
          updated_at: admin.firestore.FieldValue.serverTimestamp(),
        });
        cleaned++;
      }
      if (expiredPremium.size > 0) await batch2.commit();

      // 3. Nettoie les tokens FCM invalides ( older than 30 days)
      const thirtyDaysAgo = new Date(now - 30 * 24 * 60 * 60 * 1000);
      const oldTokens = await db
        .collection("fcm_tokens")
        .where("updated_at", "<", thirtyDaysAgo)
        .get();

      const batch3 = db.batch();
      for (const doc of oldTokens.docs) {
        batch3.delete(doc.ref);
      }
      if (oldTokens.size > 0) await batch3.commit();

      console.log(`🧹 Cleanup: ${cleaned} activations expired, ${oldTokens.size} old tokens removed`);
      await logMetric("cleanup_completed", {
        expiredActivations: cleaned,
        oldTokensRemoved: oldTokens.size,
      });
    } catch (error) {
      console.error("❌ Cleanup error:", error);
    }
  });

// ══════════════════════════════════════════════════════════════════════════════
// B3: FCM TOKEN REGISTRATION
// ══════════════════════════════════════════════════════════════════════════════
export const registerFcmToken = functions.https.onCall(async (data, context) => {
  const {android_id, fcm_token} = data;
  if (!android_id || !fcm_token) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "android_id and fcm_token required"
    );
  }

  try {
    await db.collection("fcm_tokens").doc(android_id).set({
      token: fcm_token,
      updated_at: admin.firestore.FieldValue.serverTimestamp(),
    });

    return {success: true};
  } catch (error) {
    console.error("❌ registerFcmToken error:", error);
    throw new functions.https.HttpsError("internal", "Registration failed");
  }
});

// ══════════════════════════════════════════════════════════════════════════════
// B6: MÉTRIQUES DASHBOARD (Cloud Function callable)
// ══════════════════════════════════════════════════════════════════════════════
export const getDashboardStats = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Auth required");
  }

  try {
    const now = Date.now();
    const oneDayAgo = now - 24 * 60 * 60 * 1000;
    const oneWeekAgo = now - 7 * 24 * 60 * 60 * 1000;

    // Total activations
    const totalActivations = await db.collection("activations").count().get();
    const activeCount = await db
      .collection("activations")
      .where("status", "in", ["ACTIVE", "ACTIF"])
      .count()
      .get();
    const trialCount = await db
      .collection("activations")
      .where("status", "==", "TRIAL")
      .count()
      .get();
    const expiredCount = await db
      .collection("activations")
      .where("status", "in", ["EXPIRED", "TRIAL_EXPIRED"])
      .count()
      .get();

    // Payments today
    const paymentsToday = await db
      .collection("payments")
      .where("created_at", ">=", new Date(oneDayAgo))
      .count()
      .get();

    // Payments this week
    const paymentsWeek = await db
      .collection("payments")
      .where("created_at", ">=", new Date(oneWeekAgo))
      .count()
      .get();

    // Active devices (last 24h)
    const activeDevices = await db
      .collection("metrics")
      .where("event", "==", "device_new")
      .where("timestamp", ">=", new Date(oneDayAgo))
      .count()
      .get();

    return {
      activations: {
        total: totalActivations.data().count,
        active: activeCount.data().count,
        trial: trialCount.data().count,
        expired: expiredCount.data().count,
      },
      payments: {
        today: paymentsToday.data().count,
        thisWeek: paymentsWeek.data().count,
      },
      devices: {
        newToday: activeDevices.data().count,
      },
    };
  } catch (error) {
    console.error("❌ getDashboardStats error:", error);
    throw new functions.https.HttpsError("internal", "Stats failed");
  }
});

// ══════════════════════════════════════════════════════════════════════════════
// B1: DASHBOARD ADMIN WEB (Minimal HTML served from Firebase Hosting)
// ══════════════════════════════════════════════════════════════════════════════
export const adminDashboard = functions.https.onRequest(async (req, res) => {
  res.set("Content-Type", "text/html");
  res.status(200).send(`
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>SkyPlayer Admin</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #0f172a; color: #e2e8f0; }
    .header { background: #1e293b; padding: 20px; border-bottom: 1px solid #334155; }
    .header h1 { color: #38bdf8; font-size: 24px; }
    .container { max-width: 1200px; margin: 0 auto; padding: 20px; }
    .stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 24px; }
    .stat-card { background: #1e293b; border-radius: 12px; padding: 20px; border: 1px solid #334155; }
    .stat-card h3 { color: #94a3b8; font-size: 14px; margin-bottom: 8px; }
    .stat-card .value { font-size: 32px; font-weight: 700; color: #38bdf8; }
    .stat-card.active .value { color: #22c55e; }
    .stat-card.trial .value { color: #f59e0b; }
    .stat-card.expired .value { color: #ef4444; }
    table { width: 100%; border-collapse: collapse; background: #1e293b; border-radius: 12px; overflow: hidden; }
    th { background: #334155; padding: 12px 16px; text-align: left; font-size: 13px; color: #94a3b8; text-transform: uppercase; }
    td { padding: 12px 16px; border-top: 1px solid #334155; font-size: 14px; }
    tr:hover td { background: #1a2332; }
    .badge { display: inline-block; padding: 2px 8px; border-radius: 12px; font-size: 12px; font-weight: 600; }
    .badge-active { background: #166534; color: #22c55e; }
    .badge-trial { background: #713f12; color: #f59e0b; }
    .badge-expired { background: #7f1d1d; color: #ef4444; }
    .search { margin-bottom: 20px; }
    .search input { width: 100%; padding: 12px 16px; border-radius: 8px; border: 1px solid #334155; background: #0f172a; color: #e2e8f0; font-size: 16px; }
    .search input:focus { outline: none; border-color: #38bdf8; }
    .loading { text-align: center; padding: 40px; color: #64748b; }
  </style>
</head>
<body>
  <div class="header"><h1>🎮 SkyPlayer Admin Dashboard</h1></div>
  <div class="container">
    <div class="stats" id="stats"></div>
    <div class="search"><input type="text" id="searchInput" placeholder="Rechercher par MAC, email ou reseller..." oninput="filterTable()"></div>
    <table><thead><tr><th>MAC</th><th>Statut</th><th>Playlist</th><th>Revendeur</th><th>Créé le</th><th>Expiré le</th></tr></thead><tbody id="activations"></tbody></table>
    <div class="loading" id="loading">Chargement...</div>
  </div>
  <script>
    // Admin dashboard - requires Firebase Auth token
    // For production: add Firebase Auth + Firestore SDK
    document.getElementById('loading').textContent = 'Connectez-vous via Firebase Console pour accéder aux données.';
  </script>
</body>
</html>
  `);
});

// ══════════════════════════════════════════════════════════════════════════════
// HELPERS
// ══════════════════════════════════════════════════════════════════════════════
function mapStatusToResponse(status: string | undefined): string {
  switch ((status || "").toUpperCase()) {
    case "TRIAL":
      return "trial_active";
    case "ACTIVE":
    case "ACTIF":
      return "premium_active";
    case "EXPIRED":
    case "TRIAL_EXPIRED":
      return "expired";
    default:
      return "expired";
  }
}

function extractActivationFromPaymentId(paymentId: string): string | null {
  const match =
    paymentId.match(/LICENSE_([^_]+)/i) ||
    paymentId.match(/ACTIVATION_([^_]+)/i);
  return match ? match[1] : null;
}
