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
  const urlWithCacheBuster = `${url}${url.includes('?') ? '&' : '?'}t=${Date.now()}`;
  const hostUrl = 'https://ais-dev-lfwiazz5uklpv2b4uunzg7-511075437969.europe-west2.run.app';
  
  const performFetch = async (targetUrl: string, useProxy: boolean): Promise<string> => {
    const headers = {
      'User-Agent': 'IPTVSmartersPro',
      'Accept': '*/*'
    };

    if (useProxy) {
      const pUrl = Capacitor.isNativePlatform() 
        ? `${hostUrl}/api/proxy/playlist?url=${encodeURIComponent(targetUrl)}`
        : `/api/proxy/playlist?url=${encodeURIComponent(targetUrl)}`;
      
      if (onProgress) onProgress('Utilisation du tunnel sécurisé...');
      
      if (Capacitor.isNativePlatform()) {
        const response = await CapacitorHttp.get({ url: pUrl, headers: { 'Accept': '*/*' } });
        if (response.status !== 200) throw new Error(`Proxy error ${response.status}`);
        return typeof response.data === 'string' ? response.data : JSON.stringify(response.data);
      } else {
        const response = await fetch(pUrl);
        if (!response.ok) throw new Error(`Proxy error ${response.status}`);
        return await response.text();
      }
    } else {
      if (onProgress) onProgress('Connexion directe au fournisseur...');
      if (Capacitor.isNativePlatform()) {
        const response = await CapacitorHttp.get({ url: targetUrl, headers });
        if (response.status !== 200) throw new Error(`HTTP ${response.status}`);
        return typeof response.data === 'string' ? response.data : JSON.stringify(response.data);
      } else {
        // Direct fetch on web (often fails due to CORS, but we try as last resort)
        const response = await fetch(targetUrl, { headers });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return await response.text();
      }
    }
  };

  try {
    let content = '';
    try {
      // Step 1: Try Proxy
      content = await performFetch(urlWithCacheBuster, true);
    } catch (proxyError: any) {
      console.warn("Proxy attempt failed, trying direct...", proxyError);
      // Step 2: Try Direct
      content = await performFetch(urlWithCacheBuster, false);
    }
    
    if (!content) throw new Error("Réponse vide du serveur");

    // Force string matching
    if (onProgress) onProgress('Décodage du flux (' + Math.round(content.length / 1024) + ' KB)...');
    const contentStr = String(content).trim();

    let channels: Channel[] = [];
    if (contentStr.startsWith('{') || contentStr.startsWith('[')) {
      if (onProgress) onProgress('Analyse du JSON...');
      channels = parseJSONPlaylist(contentStr);
    } else if (contentStr.includes('#EXTM3U') || contentStr.includes('#EXTINF')) {
      if (onProgress) onProgress('Analyse du M3U (Lecture des lignes)...');
      channels = parseM3U(contentStr);
    } else {
      if (url.includes('username=') && url.includes('password=')) {
        if (onProgress) onProgress('Tentative de secours M3U...');
        const fallBackChannels = parseM3U(contentStr);
        if (fallBackChannels.length > 0) channels = fallBackChannels;
        else throw new Error("Format de réponse non reconnu (ni M3U ni JSON)");
      } else {
        channels = [{ name: 'Flux Direct', url }];
      }
    }
    
    if (onProgress) onProgress(`${channels.length} chaînes chargées avec succès.`);
    return channels;
  } catch (e: any) {
    console.error('Fetch error:', e);
    throw e;
  }
};
