const express = require("express");
const rateLimit = require("express-rate-limit");
const helmet = require("helmet");
const admin = require("firebase-admin");
require("dotenv").config();

const PORT = Number(process.env.PORT || 8787);
// Durée d'essai en jours — DOIT rester alignée sur LicenseManager.TRIAL_DAYS (app Android = 14)
// et sur config.php (backend PHP). Surchargeable via l'environnement TRIAL_DAYS.
const TRIAL_DAYS = Number(process.env.TRIAL_DAYS) || 14;
const DATABASE_URL = process.env.FIREBASE_DATABASE_URL;
const ACTIVATION_API_KEY = process.env.ACTIVATION_API_KEY;
const SERVICE_ACCOUNT_JSON = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;

// Logger helper
const log = (level, message, meta = {}) => {
  const timestamp = new Date().toISOString();
  console.log(JSON.stringify({
    timestamp,
    level,
    message,
    ...meta
  }));
};

if (!DATABASE_URL || !ACTIVATION_API_KEY || !SERVICE_ACCOUNT_JSON) {
  throw new Error(
    "Missing required env vars: FIREBASE_DATABASE_URL, ACTIVATION_API_KEY, FIREBASE_SERVICE_ACCOUNT_JSON",
  );
}

let serviceAccount;
try {
  serviceAccount = JSON.parse(SERVICE_ACCOUNT_JSON);
} catch (error) {
  throw new Error("FIREBASE_SERVICE_ACCOUNT_JSON is not valid JSON");
}

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: DATABASE_URL,
});

const db = admin.database();
const app = express();

const apiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 100,
  standardHeaders: true,
  legacyHeaders: false,
});

const sensitiveApiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 20,
  standardHeaders: true,
  legacyHeaders: false,
});

app.use(helmet());
app.use(express.json({ limit: "32kb" }));
app.use(apiLimiter);

// Request logger middleware
app.use((req, _res, next) => {
  log("info", "Incoming request", {
    method: req.method,
    url: req.url,
    ip: req.ip,
  });
  next();
});

function requireApiKey(req, res, next) {
  const apiKey = req.header("x-api-key") || req.query.apiKey;
  if (!apiKey || apiKey !== ACTIVATION_API_KEY) {
    log("warn", "Unauthorized access attempt", {
      ip: req.ip,
      url: req.url
    });
    return res.status(401).json({ error: "unauthorized" });
  }
  return next();
}

function validateDeviceId(deviceId) {
  // Accepter format MAC (XX:XX:XX:XX:XX:XX:XX:XX) et IDs standards
  return typeof deviceId === "string" && /^[A-Za-z0-9_:-]{8,64}$/.test(deviceId);
}

app.get("/api/health", async (_req, res) => {
  try {
    const pingRef = db.ref("serverTime");
    await pingRef.set(Date.now());
    log("info", "Health check successful");
    res.json({
      status: "OK",
      timestamp: new Date().toISOString(),
      service: "SkyPlayer Activation Service",
    });
  } catch (error) {
    log("error", "Health check failed", { error: error.message });
    console.error("health_check_failed", error);
    res.status(500).json({
      status: "ERROR",
      error: "health_check_failed",
    });
  }
});

app.post("/api/activate", sensitiveApiLimiter, requireApiKey, async (req, res) => {
  try {
    const { deviceId, activatedBy } = req.body || {};
    if (!validateDeviceId(deviceId)) {
      log("warn", "Invalid device ID", { deviceId });
      return res.status(400).json({ error: "invalid_device_id" });
    }

    const actor =
      typeof activatedBy === "string" && activatedBy.trim().length > 0
        ? activatedBy.trim()
        : "skyplayerapp.xyz";

    const ref = db.ref(`licenses/${deviceId}`);
    const now = Date.now();

    await ref.update({
      isActive: true,
      activatedBy: actor,
      activationDate: now,
    });

    log("info", "Device activated", { deviceId, activatedBy: actor });

    return res.json({
      ok: true,
      deviceId,
      isActive: true,
      activatedBy: actor,
      activationDate: now,
    });
  } catch (error) {
    log("error", "Activation failed", { error: error.message });
    console.error("activation_failed", error);
    return res.status(500).json({ error: "activation_failed" });
  }
});

app.post(
  "/api/deactivate",
  sensitiveApiLimiter,
  requireApiKey,
  async (req, res) => {
    try {
      const { deviceId } = req.body || {};
      if (!validateDeviceId(deviceId)) {
        log("warn", "Invalid device ID for deactivation", { deviceId });
        return res.status(400).json({ error: "invalid_device_id" });
      }

      const ref = db.ref(`licenses/${deviceId}`);
      await ref.update({
        isActive: false,
      });

      log("info", "Device deactivated", { deviceId });

      return res.json({
        ok: true,
        deviceId,
        isActive: false,
      });
    } catch (error) {
      log("error", "Deactivation failed", { error: error.message });
      console.error("deactivation_failed", error);
      return res.status(500).json({ error: "deactivation_failed" });
    }
  },
);

app.get(
  "/api/license/:deviceId",
  sensitiveApiLimiter,
  requireApiKey,
  async (req, res) => {
    try {
      const { deviceId } = req.params;
      if (!validateDeviceId(deviceId)) {
        log("warn", "Invalid device ID for license check", { deviceId });
        return res.status(400).json({ error: "invalid_device_id" });
      }

      const snapshot = await db.ref(`licenses/${deviceId}`).get();
      if (!snapshot.exists()) {
        log("info", "License not found", { deviceId });
        return res.json({
          deviceId,
          exists: false,
          isActive: false,
          isTrialExpired: false,
          trialDaysRemaining: TRIAL_DAYS,
          installDate: null,
          activatedBy: null,
          activationDate: null,
          deviceInfo: null,
        });
      }

      const data = snapshot.val();
      const installDate = data.installDate || Date.now();
      const trialDurationMs = TRIAL_DAYS * 24 * 60 * 60 * 1000;
      const expiryDate = installDate + trialDurationMs;
      const now = Date.now();

      const isTrialExpired = now > expiryDate;
      const trialDaysRemaining = Math.max(
        0,
        Math.ceil((expiryDate - now) / (1000 * 60 * 60 * 24)),
      );

      log("info", "License checked", {
        deviceId,
        isActive: !!data.isActive,
        trialDaysRemaining: data.isActive ? 0 : trialDaysRemaining
      });

      return res.json({
        deviceId,
        exists: true,
        isActive: !!data.isActive,
        isTrialExpired: isTrialExpired && !data.isActive,
        trialDaysRemaining: data.isActive ? 0 : trialDaysRemaining,
        installDate: new Date(installDate).toISOString(),
        activatedBy: data.activatedBy || null,
        activationDate: data.activationDate
          ? new Date(data.activationDate).toISOString()
          : null,
        deviceInfo: data.deviceInfo || null,
      });
    } catch (error) {
      log("error", "License read failed", { error: error.message });
      console.error("read_failed", error);
      return res.status(500).json({ error: "read_failed" });
    }
  },
);

app.listen(PORT, () => {
  log("info", `Activation service listening on :${PORT}`);
});
