/**
 * Validates a MAC address string.
 * Supports formats like 00:1A:2B:3C:4D:5E or 00-1A-2B-3C-4D-5E
 */
export const validateMacAddress = (mac: string): boolean => {
  const regex = /^([0-9A-Fa-f]{2}[:-]){5,7}([0-9A-Fa-f]{2})$/;
  return regex.test(mac);
};

/**
 * Formats a string into a MAC address format as the user types.
 */
export const formatMacAddress = (value: string): string => {
  const cleaned = value.replace(/[^0-9A-Fa-f]/g, '').toUpperCase();
  const parts = cleaned.match(/.{1,2}/g);
  if (!parts) return cleaned;
  return parts.slice(0, 8).join(':');
};

/**
 * Validates a phone number (basic check for length and digits).
 */
export const validatePhone = (phone: string): boolean => {
  const cleaned = phone.replace(/\D/g, '');
  return cleaned.length >= 8 && cleaned.length <= 15;
};
