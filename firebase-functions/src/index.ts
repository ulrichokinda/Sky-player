import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

const db = admin.database();

export const checkPaymentStatus = functions.https.onRequest(async (req, res) => {
  const licenseId = req.query.licenseId as string;
  if (!licenseId) { res.status(400).json({ error: "licenseId requis" }); return; }
  try {
    const snapshot = await db.ref(`licenses/${licenseId}`).once("value");
    const data = snapshot.val();
    if (!data) { res.status(404).json({ error: "Licence non trouvée" }); return; }
    res.status(200).json({ licenseId, isActive: data.isActive || false, paymentProvider: data.paymentProvider || "unknown", paymentStatus: data.paymentStatus || "UNKNOWN", lastUpdate: data.lastPaymentUpdate || null });
  } catch (error) { console.error("❌ Erreur:", error); res.status(500).json({ error: "Erreur serveur" }); }
});

// firebase-functions v6 API: onCall receives (request) where request.data and request.auth are available
export const updateLicenseStatus = functions.https.onCall(async (request) => {
  const auth = request.auth;
  if (!auth) throw new functions.https.HttpsError("unauthenticated", "Auth requise");

  const body = request.data as { licenseId?: string; isActive?: boolean; reason?: string };
  const licenseId = body.licenseId;
  const isActive = body.isActive;
  const reason = body.reason;

  if (!licenseId || typeof isActive !== "boolean") {
    throw new functions.https.HttpsError("invalid-argument", "licenseId et isActive requis");
  }

  try {
    await db.ref(`licenses/${licenseId}`).update({
      isActive,
      manualUpdate: true,
      updatedBy: auth.uid,
      updateReason: reason || "Manual update",
      updatedAt: admin.database.ServerValue.TIMESTAMP
    });
    return { success: true, licenseId, isActive, updatedBy: auth.uid };
  } catch (error) {
    console.error("❌ Erreur:", error);
    throw new functions.https.HttpsError("internal", "Erreur mise à jour");
  }
});
