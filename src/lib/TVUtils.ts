/**
 * Utility functions for Smart TV platforms (Tizen, webOS, Android TV)
 */

export const isTV = () => {
  const ua = navigator.userAgent.toLowerCase();
  return (
    ua.includes('tizen') ||
    ua.includes('webos') ||
    ua.includes('smart-tv') ||
    ua.includes('androidtv') ||
    ua.includes('googletv') ||
    ua.includes('appletv') ||
    ua.includes('hbbtv')
  );
};

export const getTVPlatform = () => {
  const ua = navigator.userAgent.toLowerCase();
  if (ua.includes('tizen')) return 'Tizen';
  if (ua.includes('webos')) return 'webOS';
  if (ua.includes('androidtv') || ua.includes('googletv')) return 'AndroidTV';
  return 'Web';
};

/**
 * Key mapping for different TV platforms
 */
export const TVKeys = {
  ENTER: [13],
  BACK: [8, 27, 461, 10009], // Backspace, Escape, webOS Back, Tizen Back
  UP: [38],
  DOWN: [40],
  LEFT: [37],
  RIGHT: [39],
  PLAY: [415, 250],
  PAUSE: [19, 102],
  STOP: [413],
  FAST_FORWARD: [417, 108],
  REWIND: [412, 109],
};

export const isKey = (e: KeyboardEvent, keyType: keyof typeof TVKeys) => {
  const keyCode = e.keyCode || e.which;
  return TVKeys[keyType].includes(keyCode) || e.key === keyType;
};
