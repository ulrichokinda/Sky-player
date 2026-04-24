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

export const fetchAndParsePlaylist = async (url: string): Promise<Channel[]> => {
  try {
    let content = '';
    const headers = {
      'User-Agent': 'IPTVSmartersPro',
      'Accept': '*/* spielte'
    };
    
    if (Capacitor.isNativePlatform()) {
      // Use CapacitorHttp to bypass CORS on mobile
      const options = { url, headers };
      const response = await CapacitorHttp.get(options);
      
      if (response.status !== 200) {
        throw new Error(`Erreur HTTP: ${response.status}`);
      }
      
      content = typeof response.data === 'string' ? response.data : JSON.stringify(response.data);
    } else {
      // Use standard fetch for web
      const response = await fetch(url, { headers });
      if (!response.ok) {
        throw new Error(`Erreur HTTP: ${response.status}`);
      }
      content = await response.text();
    }
    
    if (!content) throw new Error("Contenu vide reçu du serveur");

    // Force string matching
    const contentStr = String(content).trim();

    if (contentStr.startsWith('{') || contentStr.startsWith('[')) {
      return parseJSONPlaylist(contentStr);
    } else if (contentStr.includes('#EXTM3U') || contentStr.includes('#EXTINF')) {
      return parseM3U(contentStr);
    } else {
      // If none matches, check if it's an API link
      if (url.includes('username=') && url.includes('password=')) {
        // Fallback: try parsing it as M3U anyway just in case it lacks headers
        const fallBackChannels = parseM3U(contentStr);
        if (fallBackChannels.length > 0) return fallBackChannels;
        
        console.error("Format de playlist invalide ou accès refusé par le fournisseur.");
        return [];
      }
      // If it's a direct stream link
      return [{ name: 'Flux Direct', url }];
    }
  } catch (e: any) {
    console.error('Error fetching playlist (CORS, network error, or blocked by provider):', e);
    
    if (url.includes('username=') && url.includes('password=')) {
      console.error("Ceci est un lien d'API Xtream qui a échoué. Détails:", e.message);
      return []; // Return empty array to trigger actual error in SimpleUserView
    }
    return [{ name: 'Flux Direct', url }];
  }
};
