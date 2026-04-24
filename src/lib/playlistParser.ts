import { Capacitor, CapacitorHttp } from '@capacitor/core';

export interface Channel {
  name: string;
  url: string;
  logo?: string;
  group?: string;
  epgId?: string;
}

export const parseM3U = (content: string): Channel[] => {
  const lines = content.split('\n');
  const channels: Channel[] = [];
  let currentChannel: Partial<Channel> = {};

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();
    if (line.startsWith('#EXTINF:')) {
      const nameMatch = line.match(/,(.*)$/);
      const logoMatch = line.match(/tvg-logo="(.*?)"/);
      const groupMatch = line.match(/group-title="(.*?)"/);
      const epgMatch = line.match(/tvg-id="(.*?)"/);

      currentChannel.name = nameMatch ? nameMatch[1].trim() : 'Sans nom';
      currentChannel.logo = logoMatch ? logoMatch[1] : undefined;
      currentChannel.group = groupMatch ? groupMatch[1] : undefined;
      currentChannel.epgId = epgMatch ? epgMatch[1] : undefined;
    } else if (line.startsWith('http') || line.startsWith('rtmp') || line.startsWith('rtsp')) {
      currentChannel.url = line;
      channels.push(currentChannel as Channel);
      currentChannel = {};
    }
  }

  return channels;
};

export const parseJSONPlaylist = (content: string): Channel[] => {
  try {
    const data = JSON.parse(content);
    
    // Support different JSON formats
    if (Array.isArray(data)) {
      return data.map(item => ({
        name: item.name || item.title || 'Sans nom',
        url: item.url || item.link || item.file,
        logo: item.logo || item.image || item.thumb,
        group: item.group || item.category
      })).filter(c => c.url);
    } else if (data.channels && Array.isArray(data.channels)) {
      return data.channels.map((item: any) => ({
        name: item.name || item.title || 'Sans nom',
        url: item.url || item.link || item.file,
        logo: item.logo || item.image || item.thumb,
        group: item.group || item.category
      })).filter((c: any) => c.url);
    }
    
    return [];
  } catch (e) {
    console.error('Failed to parse JSON playlist', e);
    return [];
  }
};

export const fetchAndParsePlaylist = async (url: string, onProgress?: (status: string) => void): Promise<Channel[]> => {
  try {
    let content = '';
    const headers = {
      'User-Agent': 'IPTVSmartersPro',
      'Accept': '*/* spielte'
    };
    
    if (Capacitor.isNativePlatform()) {
      // Use CapacitorHttp to bypass CORS on mobile
      if (onProgress) onProgress('Connexion initiale native...');
      const options = { url, headers };
      const response = await CapacitorHttp.get(options);
      
      if (response.status !== 200) {
        throw new Error(`Erreur HTTP: ${response.status}`);
      }
      
      if (onProgress) onProgress('Lecture des données natives...');
      content = typeof response.data === 'string' ? response.data : JSON.stringify(response.data);
    } else {
      // Use standard fetch for web
      if (onProgress) onProgress('Connexion au serveur cible...');
      const response = await fetch(url, { headers });
      if (!response.ok) {
        throw new Error(`Erreur HTTP: ${response.status}`);
      }
      
      // We could use Response.body reader for precise progression on web, but text() is safer for now
      if (onProgress) onProgress('Téléchargement du fichier...');
      content = await response.text();
    }
    
    if (!content) throw new Error("Contenu vide reçu du serveur");

    // Force string matching
    if (onProgress) onProgress('Analyse du document (' + Math.round(content.length / 1024) + ' KB)...');
    const contentStr = String(content).trim();

    let channels: Channel[] = [];
    if (contentStr.startsWith('{') || contentStr.startsWith('[')) {
      channels = parseJSONPlaylist(contentStr);
    } else if (contentStr.includes('#EXTM3U') || contentStr.includes('#EXTINF')) {
      channels = parseM3U(contentStr);
    } else {
      // If none matches, check if it's an API link
      if (url.includes('username=') && url.includes('password=')) {
        // Fallback: try parsing it as M3U anyway just in case it lacks headers
        if (onProgress) onProgress('Tentative de secours M3U...');
        const fallBackChannels = parseM3U(contentStr);
        if (fallBackChannels.length > 0) channels = fallBackChannels;
        else {
          console.error("Format de playlist invalide ou accès refusé par le fournisseur.");
          return [];
        }
      } else {
        // If it's a direct stream link
        channels = [{ name: 'Flux Direct', url }];
      }
    }
    
    if (onProgress) onProgress('Extraction terminée (' + channels.length + ' chaînes).');
    return channels;
  } catch (e: any) {
    console.error('Error fetching playlist (CORS, network error, or blocked by provider):', e);
    
    if (url.includes('username=') && url.includes('password=')) {
      console.error("Ceci est un lien d'API Xtream qui a échoué. Détails:", e.message);
      // Throw the error so the UI catch block can display the exact failure to the user instead of generic "empty list"
      throw e;
    }
    // Also throw for other standard failures so it hits the catch block
    throw e;
  }
};
