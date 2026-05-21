const express = require("express");
const admin = require("firebase-admin");
require("dotenv").config();

const PORT = Number(process.env.PORT || 8787);
const DATABASE_URL = process.env.FIREBASE_DATABASE_URL;
const ACTIVATION_API_KEY = process.env.ACTIVATION_API_KEY;
const SERVICE_ACCOUNT_JSON = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;

if (!DATABASE_URL || !ACTIVATION_API_KEY || !SERVICE_ACCOUNT_JSON) {
  throw new Error("Missing required env vars: FIREBASE_DATABASE_URL, ACTIVATION_API_KEY, FIREBASE_SERVICE_ACCOUNT_JSON");
}

let serviceAccount;
try {
  serviceAccount = JSON.parse(SERVICE_ACCOUNT_JSON);
} catch (error) {
  throw new Error("FIREBASE_SERVICE_ACCOUNT_JSON is not valid JSON");
}

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: DATABASE_URL
});

const db = admin.database();
const app = express();
app.use(express.json());

function requireApiKey(req, res, next) {
  const apiKey = req.header("x-api-key");
  if (!apiKey || apiKey !== ACTIVATION_API_KEY) {
    return res.status(401).json({ error: "unauthorized" });
  }
  return next();
}

function validateDeviceId(deviceId) {
  return typeof deviceId === "string" && deviceId.length >= 8 && deviceId.length <= 64;
}

app.get("/health", async (_req, res) => {
  try {
    const pingRef = db.ref("serverTime");
    const ping = await pingRef.get();
    res.json({
      ok: true,
      databaseReachable: ping.exists()
    });
  } catch (error) {
    res.status(500).json({
      ok: false,
      error: error.message
    });
  }
});

app.post("/activate", requireApiKey, async (req, res) => {
  try {
    const { deviceId, activatedBy } = req.body || {};
    if (!validateDeviceId(deviceId)) {
      return res.status(400).json({ error: "invalid_device_id" });
    }

    const actor = typeof activatedBy === "string" && activatedBy.trim().length > 0
      ? activatedBy.trim()
      : "skyplayerapp.xyz";

    const ref = db.ref(`licenses/${deviceId}`);
    const now = Date.now();

    await ref.update({
      isActive: true,
      activatedBy: actor,
      activationDate: now
    });

    return res.json({
      ok: true,
      deviceId,
      isActive: true,
      activatedBy: actor,
      activationDate: now
    });
  } catch (error) {
    return res.status(500).json({ error: "activation_failed", details: error.message });
  }
});

app.post("/deactivate", requireApiKey, async (req, res) => {
  try {
    const { deviceId } = req.body || {};
    if (!validateDeviceId(deviceId)) {
      return res.status(400).json({ error: "invalid_device_id" });
    }

    const ref = db.ref(`licenses/${deviceId}`);
    await ref.update({
      isActive: false
    });

    return res.json({
      ok: true,
      deviceId,
      isActive: false
    });
  } catch (error) {
    return res.status(500).json({ error: "deactivation_failed", details: error.message });
  }
});

app.get("/license/:deviceId", requireApiKey, async (req, res) => {
  try {
    const { deviceId } = req.params;
    if (!validateDeviceId(deviceId)) {
      return res.status(400).json({ error: "invalid_device_id" });
    }

    const snapshot = await db.ref(`licenses/${deviceId}`).get();
    if (!snapshot.exists()) {
      return res.status(404).json({ error: "not_found" });
    }

    return res.json({
      ok: true,
      deviceId,
      license: snapshot.val()
    });
  } catch (error) {
    return res.status(500).json({ error: "read_failed", details: error.message });
  }
});

app.listen(PORT, () => {
  console.log(`Activation service listening on :${PORT}`);
});
