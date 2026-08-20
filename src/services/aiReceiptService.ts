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
      const response = await fetch('/api/ai-receipt', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ base64Image, mimeType })
      });

      const jsonResponse = await response.json();
      
      if (!response.ok) {
        throw new Error(jsonResponse.error || "Erreur de validation du reçu.");
      }

      return jsonResponse.data as ReceiptData;
    } catch (error) {
      console.error("AI Receipt Validation Error:", error);
      throw new Error("Impossible d'analyser le reçu via l'IA.");
    }
  }
};
