import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import * as crypto from 'crypto';
import { defineSecret, defineString } from 'firebase-functions/params';

// Définir les secrets et paramètres
const pawapayApiToken = defineSecret('PAWAPAY_API_TOKEN');
const pawapayWebhookSecret = defineSecret('PAWAPAY_WEBHOOK_SECRET');

// Initialiser Firebase Admin
admin.initializeApp();

const db = admin.database();

/**
 * Cloud Function HTTP pour recevoir les callbacks PawaPay
 * Endpoint: https://<region>-<project-id>.cloudfunctions.net/pawapayWebhook
 */
export const pawapayWebhook = functions.https.onRequest(async (req, res) => {
  // Vérifier que c'est une requête POST
  if (req.method !== 'POST') {
    console.warn('❌ Méthode non autorisée:', req.method);
    res.status(405).json({ 
      error: 'Method Not Allowed',
      message: 'Seules les requêtes POST sont acceptées' 
    });
    return;
  }

  try {
    const payload = req.body;
    console.log('📥 Callback PawaPay reçu:', JSON.stringify(payload, null, 2));

    // Vérifier la signature webhook (sécurité)
    const signature = req.headers['x-pawapay-signature'] as string;
    const webhookSecret = pawapayWebhookSecret.value();
    
    if (webhookSecret && signature) {
      const isValid = verifyPawaPaySignature(payload, signature, webhookSecret);
      if (!isValid) {
        console.error('❌ Signature webhook invalide');
        res.status(401).json({ error: 'Invalid signature' });
        return;
      }
    }

    // Extraire les données du callback
    const {
      depositId,
      status,
      phoneNumber,
      amount,
      currency,
      timestamp,
      metadata
    } = payload;

    // Vérifier que les données essentielles sont présentes
    if (!depositId || !status) {
      console.error('❌ Données manquantes dans le callback');
      res.status(400).json({ 
        error: 'Bad Request',
        message: 'depositId et status sont requis' 
      });
      return;
    }

    // Déterminer le statut isActive basé sur le statut PawaPay
    const isActive = mapPawaPayStatusToActive(status);
    
    // Extraire l'ID utilisateur/licence des métadonnées
    const licenseId = metadata?.licenseId || metadata?.userId || extractLicenseFromDeposit(depositId);
    
    if (!licenseId) {
      console.error('❌ Impossible de déterminer le licenseId');
      res.status(400).json({ 
        error: 'Bad Request',
        message: 'licenseId non trouvé dans les métadonnées' 
      });
      return;
    }

    // Mettre à jour la Realtime Database
    const updateData = {
      isActive: isActive,
      paymentStatus: status,
      lastPaymentUpdate: admin.database.ServerValue.TIMESTAMP,
      pawapayDepositId: depositId,
      pawapayPhoneNumber: phoneNumber || null,
      pawapayAmount: amount || null,
      pawapayCurrency: currency || null,
      pawapayTimestamp: timestamp || null,
      pawapayMetadata: metadata || null
    };

    // Mettre à jour le nœud de licence
    const licenseRef = db.ref(`licenses/${licenseId}`);
    await licenseRef.update(updateData);

    // Créer un historique des paiements
    const paymentHistoryRef = db.ref(`payment_history/${licenseId}`).push();
    await paymentHistoryRef.set({
      ...updateData,
      createdAt: admin.database.ServerValue.TIMESTAMP
    });

    console.log(`✅ Licence ${licenseId} mise à jour - isActive: ${isActive}, status: ${status}`);

    // Répondre à PawaPay avec succès
    res.status(200).json({
      success: true,
      message: 'Callback traité avec succès',
      licenseId: licenseId,
      isActive: isActive,
      status: status
    });

  } catch (error) {
    console.error('❌ Erreur lors du traitement du callback:', error);
    res.status(500).json({
      error: 'Internal Server Error',
      message: error instanceof Error ? error.message : 'Erreur inconnue'
    });
  }
});

/**
 * Vérifie la signature du webhook PawaPay
 */
function verifyPawaPaySignature(
  payload: any,
  signature: string,
  secret: string
): boolean {
  try {
    const payloadString = JSON.stringify(payload);
    const expectedSignature = crypto
      .createHmac('sha256', secret)
      .update(payloadString)
      .digest('hex');
    
    return crypto.timingSafeEqual(
      Buffer.from(signature),
      Buffer.from(expectedSignature)
    );
  } catch (error) {
    console.error('❌ Erreur lors de la vérification de signature:', error);
    return false;
  }
}

/**
 * Mappe le statut PawaPay vers isActive
 */
function mapPawaPayStatusToActive(pawaPayStatus: string): boolean {
  const activeStatuses = ['COMPLETED', 'SUCCESS', 'CONFIRMED', 'ACCEPTED'];
  const inactiveStatuses = ['FAILED', 'REJECTED', 'CANCELLED', 'EXPIRED', 'PENDING'];
  
  const status = pawaPayStatus.toUpperCase();
  
  if (activeStatuses.includes(status)) {
    return true;
  }
  
  if (inactiveStatuses.includes(status)) {
    return false;
  }
  
  // Par défaut, considérer comme inactif si statut inconnu
  console.warn(`⚠️ Statut PawaPay inconnu: ${pawaPayStatus}, défini comme inactif`);
  return false;
}

/**
 * Extrait le licenseId du depositId si nécessaire
 */
function extractLicenseFromDeposit(depositId: string): string | null {
  // Format attendu: "LICENSE_xxx_TIMESTAMP" ou similaire
  const match = depositId.match(/LICENSE_([^_]+)/i);
  return match ? match[1] : null;
}

/**
 * Fonction pour vérifier manuellement le statut d'un paiement
 * Endpoint: https://<region>-<project-id>.cloudfunctions.net/checkPaymentStatus?licenseId=xxx
 */
export const checkPaymentStatus = functions.https.onRequest(async (req, res) => {
  const licenseId = req.query.licenseId as string;
  
  if (!licenseId) {
    res.status(400).json({ error: 'licenseId requis' });
    return;
  }

  try {
    const snapshot = await db.ref(`licenses/${licenseId}`).once('value');
    const data = snapshot.val();
    
    if (!data) {
      res.status(404).json({ error: 'Licence non trouvée' });
      return;
    }

    res.status(200).json({
      licenseId: licenseId,
      isActive: data.isActive || false,
      paymentStatus: data.paymentStatus || 'UNKNOWN',
      lastUpdate: data.lastPaymentUpdate || null
    });
  } catch (error) {
    console.error('❌ Erreur:', error);
    res.status(500).json({ error: 'Erreur serveur' });
  }
});

/**
 * Fonction pour activer/désactiver manuellement une licence (admin)
 * Nécessite l'authentification
 */
export const updateLicenseStatus = functions.https.onCall(async (data, context) => {
  // Vérifier l'authentification
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Authentification requise');
  }

  const { licenseId, isActive, reason } = data;
  
  if (!licenseId || typeof isActive !== 'boolean') {
    throw new functions.https.HttpsError('invalid-argument', 'licenseId et isActive requis');
  }

  try {
    const licenseRef = db.ref(`licenses/${licenseId}`);
    await licenseRef.update({
      isActive: isActive,
      manualUpdate: true,
      updatedBy: context.auth.uid,
      updateReason: reason || 'Manual update',
      updatedAt: admin.database.ServerValue.TIMESTAMP
    });

    console.log(`✅ Licence ${licenseId} manuellement ${isActive ? 'activée' : 'désactivée'}`);
    
    return {
      success: true,
      licenseId: licenseId,
      isActive: isActive,
      updatedBy: context.auth.uid
    };
  } catch (error) {
    console.error('❌ Erreur:', error);
    throw new functions.https.HttpsError('internal', 'Erreur lors de la mise à jour');
  }
});

/**
 * Fonction pour vérifier le statut d'un paiement via l'API PawaPay
 * Utilise le JWT token pour l'authentification
 */
export const verifyPawaPayPayment = functions.https.onCall(async (data, context) => {
  // Vérifier l'authentification
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Authentification requise');
  }

  const { depositId } = data;
  
  if (!depositId) {
    throw new functions.https.HttpsError('invalid-argument', 'depositId requis');
  }

  try {
    // Récupérer le token API depuis les secrets
    const apiToken = pawapayApiToken.value();
    
    if (!apiToken) {
      throw new functions.https.HttpsError('failed-precondition', 
        'Token API PawaPay non configuré. Contactez l\'administrateur.');
    }

    // Appel API PawaPay pour vérifier le statut
    const response = await fetch(`https://api.pawapay.io/v1/deposits/${depositId}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${apiToken}`,
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      }
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error('❌ Erreur API PawaPay:', response.status, errorText);
      throw new functions.https.HttpsError('internal', 
        `Erreur API PawaPay: ${response.status}`);
    }

    const paymentData = await response.json();
    
    console.log('✅ Statut PawaPay vérifié:', paymentData);

    // Mettre à jour la base de données si le statut a changé
    const isActive = mapPawaPayStatusToActive(paymentData.status);
    
    return {
      success: true,
      depositId: depositId,
      pawapayStatus: paymentData.status,
      isActive: isActive,
      amount: paymentData.amount,
      currency: paymentData.currency,
      phoneNumber: paymentData.phoneNumber,
      createdAt: paymentData.createdAt,
      updatedAt: paymentData.updatedAt
    };

  } catch (error) {
    console.error('❌ Erreur vérification paiement:', error);
    throw new functions.https.HttpsError('internal', 
      error instanceof Error ? error.message : 'Erreur inconnue');
  }
});
