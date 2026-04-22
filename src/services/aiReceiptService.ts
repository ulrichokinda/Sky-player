import { GoogleGenAI, Type } from "@google/genai";

const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY as string });

export interface ReceiptData {
  transactionId: string;
  amount: number;
  currency: string;
  provider: string;
  date: string;
  isValid: boolean;
  reason?: string;
}

export const aiReceiptService = {
  async validateReceipt(base64Image: string, mimeType: string = "image/jpeg"): Promise<ReceiptData> {
    try {
      const response = await ai.models.generateContent({
        model: "gemini-1.5-flash",
        contents: [
          {
            parts: [
              {
                inlineData: {
                  data: base64Image.split(',')[1] || base64Image,
                  mimeType,
                },
              },
              {
                text: "Analyse ce reçu de paiement Mobile Money. Identifie si c'est un reçu de transfert d'argent réussi (Airtel Money, Moov, Orange, MTN, Wave, etc.). Extrait l'ID de transaction, le montant exact, la devise, et la date. Vérifie si le reçu semble authentique et complet. Réponds au format JSON.",
              },
            ],
          },
        ],
        config: {
          responseMimeType: "application/json",
          responseSchema: {
            type: Type.OBJECT,
            properties: {
              transactionId: { type: Type.STRING, description: "L'identifiant unique de la transaction" },
              amount: { type: Type.NUMBER, description: "Le montant numérique payé" },
              currency: { type: Type.STRING, description: "La devise (ex: XAF, XOF)" },
              provider: { type: Type.STRING, description: "Le nom de l'opérateur (ex: Airtel, Moov)" },
              date: { type: Type.STRING, description: "La date du reçu au format ISO" },
              isValid: { type: Type.BOOLEAN, description: "True si le reçu est valide et n'est pas une tentative de fraude évidente" },
              reason: { type: Type.STRING, description: "Raison si non valide" },
            },
            required: ["transactionId", "amount", "currency", "provider", "isValid"],
          },
        },
      });

      const result = JSON.parse(response.text || "{}");
      return result as ReceiptData;
    } catch (error) {
      console.error("AI Receipt Validation Error:", error);
      throw new Error("Impossible d'analyser le reçu via l'IA.");
    }
  }
};
