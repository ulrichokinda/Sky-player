import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import * as crypto from "crypto";
import { defineSecret } from "firebase-functions/params";

const joboostCashApiToken = defineSecret("JOBOOST_CASH_API_TOKEN");
const joboostCashWebhookSecret = defineSecret("JOBOOST_CASH_WEBHOOK_SECRET");

admin.initializeApp();

const db = admin.database();

export const joboostCashWebhook = functions
  .runWith({
    secrets: [joboostCashWebhookSecret],
  })
  .https.onRequest(async (req: functions.https.Request, res) => {
    if (req.method !== "POST") {
      console.warn("❌ Méthode non autorisée:", req.method);
      res.status(405).json({
        error: "Method Not Allowed",
        message: "Seules les requêtes POST sont acceptées",
      });
      return;
    }

    try {
      const payload = req.body;
      console.log(
        "📥 Callback Joboost Cash reçu:",
        JSON.stringify(payload, null, 2),
      );

      const signatureHeader = req.headers["x-joboost-cash-signature"];
      const signature = Array.isArray(signatureHeader)
        ? signatureHeader[0]
        : signatureHeader;
      const webhookSecret = joboostCashWebhookSecret.value();

      if (webhookSecret) {
        if (!signature) {
          console.error("❌ Signature webhook manquante");
          res.status(401).json({ error: "Missing signature" });
          return;
        }

        const isValid = verifyWebhookSignature(
          req.rawBody,
          signature,
          webhookSecret,
        );
        if (!isValid) {
          console.error("❌ Signature webhook invalide");
          res.status(401).json({ error: "Invalid signature" });
          return;
        }
      }

      const {
        transactionId,
        paymentId,
        depositId,
        status,
        phoneNumber,
        amount,
        currency,
        timestamp,
        metadata,
      } = payload;

      const providerPaymentId = transactionId || paymentId || depositId;

      if (!providerPaymentId || !status) {
        console.error("❌ Données manquantes dans le callback");
        res.status(400).json({
          error: "Bad Request",
          message: "transactionId/paymentId/depositId et status sont requis",
        });
        return;
      }

      const isActive = mapPaymentStatusToActive(status);
      const licenseId =
        metadata?.licenseId ||
        metadata?.userId ||
        extractLicenseFromPaymentId(providerPaymentId);

      if (!licenseId) {
        console.error("❌ Impossible de déterminer le licenseId");
        res.status(400).json({
          error: "Bad Request",
          message: "licenseId non trouvé dans les métadonnées",
        });
        return;
      }

      const updateData = {
        isActive,
        paymentProvider: "joboost-cash",
        paymentStatus: status,
        lastPaymentUpdate: admin.database.ServerValue.TIMESTAMP,
        providerPaymentId,
        phoneNumber: phoneNumber || null,
        amount: amount || null,
        currency: currency || null,
        paymentTimestamp: timestamp || null,
        paymentMetadata: metadata || null,
      };

      const licenseRef = db.ref(`licenses/${licenseId}`);
      await licenseRef.update(updateData);

      const paymentHistoryRef = db.ref(`payment_history/${licenseId}`).push();
      await paymentHistoryRef.set({
        ...updateData,
        createdAt: admin.database.ServerValue.TIMESTAMP,
      });

      console.log(
        `✅ Licence ${licenseId} mise à jour - isActive: ${isActive}, status: ${status}`,
      );

      res.status(200).json({
        success: true,
        message: "Callback traité avec succès",
        licenseId,
        isActive,
        status,
        providerPaymentId,
      });
    } catch (error) {
      console.error("❌ Erreur lors du traitement du callback:", error);
      res.status(500).json({
        error: "Internal Server Error",
        message: error instanceof Error ? error.message : "Erreur inconnue",
      });
    }
  });

function verifyWebhookSignature(
  rawBody: Buffer,
  signature: string,
  secret: string,
): boolean {
  try {
    const normalizedSignature = signature.trim();
    const expectedSignature = crypto
      .createHmac("sha256", secret)
      .update(rawBody)
      .digest("hex");

    const providedSignature = Buffer.from(normalizedSignature, "utf8");
    const computedSignature = Buffer.from(expectedSignature, "utf8");

    if (providedSignature.length !== computedSignature.length) {
      return false;
    }

    return crypto.timingSafeEqual(providedSignature, computedSignature);
  } catch (error) {
    console.error("❌ Erreur lors de la vérification de signature:", error);
    return false;
  }
}

function mapPaymentStatusToActive(paymentStatus: string): boolean {
  const activeStatuses = [
    "COMPLETED",
    "SUCCESS",
    "CONFIRMED",
    "ACCEPTED",
    "PAID",
  ];
  const inactiveStatuses = [
    "FAILED",
    "REJECTED",
    "CANCELLED",
    "EXPIRED",
    "PENDING",
    "INITIATED",
  ];

  const status = paymentStatus.toUpperCase();

  if (activeStatuses.includes(status)) {
    return true;
  }

  if (inactiveStatuses.includes(status)) {
    return false;
  }

  console.warn(
    `⚠️ Statut paiement inconnu: ${paymentStatus}, défini comme inactif`,
  );
  return false;
}

function extractLicenseFromPaymentId(paymentId: string): string | null {
  const match = paymentId.match(/LICENSE_([^_]+)/i);
  return match ? match[1] : null;
}

export const checkPaymentStatus = functions.https.onRequest(
  async (req, res) => {
    const licenseId = req.query.licenseId as string;

    if (!licenseId) {
      res.status(400).json({ error: "licenseId requis" });
      return;
    }

    try {
      const snapshot = await db.ref(`licenses/${licenseId}`).once("value");
      const data = snapshot.val();

      if (!data) {
        res.status(404).json({ error: "Licence non trouvée" });
        return;
      }

      res.status(200).json({
        licenseId,
        isActive: data.isActive || false,
        paymentProvider: data.paymentProvider || "unknown",
        paymentStatus: data.paymentStatus || "UNKNOWN",
        lastUpdate: data.lastPaymentUpdate || null,
      });
    } catch (error) {
      console.error("❌ Erreur:", error);
      res.status(500).json({ error: "Erreur serveur" });
    }
  },
);

export const updateLicenseStatus = functions.https.onCall(
  async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Authentification requise",
      );
    }

    const { licenseId, isActive, reason } = data;

    if (!licenseId || typeof isActive !== "boolean") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "licenseId et isActive requis",
      );
    }

    try {
      const licenseRef = db.ref(`licenses/${licenseId}`);
      await licenseRef.update({
        isActive,
        manualUpdate: true,
        updatedBy: context.auth.uid,
        updateReason: reason || "Manual update",
        updatedAt: admin.database.ServerValue.TIMESTAMP,
      });

      console.log(
        `✅ Licence ${licenseId} manuellement ${isActive ? "activée" : "désactivée"}`,
      );

      return {
        success: true,
        licenseId,
        isActive,
        updatedBy: context.auth.uid,
      };
    } catch (error) {
      console.error("❌ Erreur:", error);
      throw new functions.https.HttpsError(
        "internal",
        "Erreur lors de la mise à jour",
      );
    }
  },
);

export const verifyJoboostCashPayment = functions
  .runWith({
    secrets: [joboostCashApiToken],
  })
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Authentification requise",
      );
    }

    const { paymentId } = data;

    if (!paymentId) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "paymentId requis",
      );
    }

    const apiToken = joboostCashApiToken.value();
    if (!apiToken) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Token API Joboost Cash non configuré. Contactez l'administrateur.",
      );
    }

    return {
      success: false,
      paymentId,
      provider: "joboost-cash",
      message:
        "La vérification distante Joboost Cash n'est pas encore implémentée. Fournissez l'endpoint API exact pour finaliser cette fonction.",
    };
  });
