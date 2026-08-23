import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

const firestore = admin.firestore();

/**
 * Vérifie le statut d'une activation via Firestore.
 * GET /checkActivationStatus?activationId=xxx
 */
export const checkActivationStatus = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  if (req.method === "OPTIONS") { res.status(204).send(""); return; }

  const activationId = req.query.activationId as string;
  if (!activationId) { res.status(400).json({ error: "activationId requis" }); return; }

  try {
    const doc = await firestore.collection("activations").doc(activationId).get();
    if (!doc.exists) { res.status(404).json({ error: "Activation non trouvée" }); return; }

    const data = doc.data()!;
    const isExpired = data.expiryDate && new Date(data.expiryDate) < new Date();

    res.status(200).json({
      activationId,
      isActive: !isExpired && (data.status === "ACTIF" || data.status === "ACTIVE"),
      status: data.status || "UNKNOWN",
      targetMac: data.target_mac || null,
      expiryDate: data.expiryDate || null,
      lastUpdate: data.last_connection || null,
    });
  } catch (error) {
    console.error("checkActivationStatus error:", error);
    res.status(500).json({ error: "Erreur serveur" });
  }
});

/**
 * Met à jour le statut d'une activation (Firestore).
 * Callable: updateActivationStatus({ activationId, isActive, reason })
 */
export const updateActivationStatus = functions.https.onCall(async (request) => {
  const auth = request.auth;
  if (!auth) throw new functions.https.HttpsError("unauthenticated", "Auth requise");

  const { activationId, isActive, reason } = request.data as {
    activationId?: string;
    isActive?: boolean;
    reason?: string;
  };

  if (!activationId || typeof isActive !== "boolean") {
    throw new functions.https.HttpsError("invalid-argument", "activationId et isActive requis");
  }

  try {
    const docRef = firestore.collection("activations").doc(activationId);
    const doc = await docRef.get();
    if (!doc.exists) {
      throw new functions.https.HttpsError("not-found", "Activation non trouvée");
    }

    await docRef.update({
      status: isActive ? "ACTIF" : "DESACTIF",
      manualUpdate: true,
      updatedBy: auth.uid,
      updateReason: reason || "Manual update",
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    return { success: true, activationId, isActive, updatedBy: auth.uid };
  } catch (error: any) {
    if (error instanceof functions.https.HttpsError) throw error;
    console.error("updateActivationStatus error:", error);
    throw new functions.https.HttpsError("internal", "Erreur mise à jour");
  }
});
