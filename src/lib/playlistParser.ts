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

export const parseJSONPlaylist = (content: string, xtreamHost?: string, xtreamUser?: string, xtreamPass?: string): Channel[] => {
  try {
    const data = JSON.parse(content);
    
    // Support different JSON formats
    if (Array.isArray(data)) {
      return data.map(item => {
        // Xtream Codes API formatting
        let finalUrl = item.url || item.link || item.file;
        if (!finalUrl && item.stream_id && xtreamHost) {
          const type = item.stream_type === 'movie' ? 'movie' : (item.stream_type === 'series' ? 'series' : 'live');
          let ext = 'ts';
          if (type === 'movie' || type === 'series') ext = item.container_extension || 'mp4';
          
          if (type === 'live') {
            finalUrl = `${xtreamHost}/${xtreamUser}/${xtreamPass}/${item.stream_id}`;
          } else {
            finalUrl = `${xtreamHost}/${type}/${xtreamUser}/${xtreamPass}/${item.stream_id}.${ext}`;
          }
        }

        return {
          name: item.name || item.title || 'Sans nom',
          url: finalUrl,
          logo: item.logo || item.image || item.thumb || item.stream_icon,
          group: item.group || item.category || (item.category_id ? `Catégorie ${item.category_id}` : undefined),
          epgId: item.epg_channel_id
        };
      }).filter(c => c.url);
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

const fetchWithProxy = async (urlWithCacheBuster: string, onProgress?: (msg: string) => void): Promise<string> => {
  const proxyUrl = `/api/proxy/playlist?url=${encodeURIComponent(urlWithCacheBuster)}`;
  
  const response = await fetch(proxyUrl);
  if (!response.ok) throw new Error(`Tunnel instable (${response.status})`);
  
  const reader = response.body?.getReader();
  if (!reader) {
    return await response.text();
  } else {
    const decoder = new TextDecoder();
    let receivedBytes = 0;
    let chunks: string[] = [];
    
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      
      receivedBytes += value.length;
      const mb = (receivedBytes / (1024 * 1024)).toFixed(2);
      if (onProgress) onProgress(`Chargement : ${mb} MB reçus...`);
      
      chunks.push(decoder.decode(value, { stream: true }));
    }
    return chunks.join('');
  }
};

export const fetchAndParsePlaylist = async (url: string, onProgress?: (status: string) => void): Promise<Channel[]> => {
  const isNative = Capacitor.isNativePlatform();
  let urlToFetch = url;
  
  // Xtream Codes API optimization:
  // Instead of downloading a massive M3U file, use the JSON API which is 10x smaller and faster
  let isXtream = false;
  let xtreamHost = '';
  let xtreamUser = '';
  let xtreamPass = '';
  
  if (url.includes('get.php?username=') && url.includes('password=')) {
    isXtream = true;
    try {
      const urlObj = new URL(url);
      xtreamUser = urlObj.searchParams.get('username') || '';
      xtreamPass = urlObj.searchParams.get('password') || '';
      xtreamHost = urlObj.origin;
      // We are going to fetch live streams via JSON first
      urlToFetch = `${xtreamHost}/player_api.php?username=${xtreamUser}&password=${xtreamPass}&action=get_live_streams`;
      if (onProgress) onProgress('Mode Optimisé Xtream activé...');
    } catch(e) {
      isXtream = false;
    }
  }

  const urlWithCacheBuster = `${urlToFetch}${urlToFetch.includes('?') ? '&' : '?'}t=${Date.now()}`;
  
  const NATIVE_FINGERPRINTS = [
    {
      'User-Agent': 'Mozilla/5.0 (Linux; Android 10; SM-G981B) SmartersPlayer/3.0.0',
      'X-Requested-With': 'com.nst.iptvsmarterstvbox'
    },
    {
      'User-Agent': 'IPTVSmartersPlayer/1.0.0 (iPhone; iOS 15.0; Scale/3.00)',
      'X-Requested-With': 'com.nst.iptvsmarters'
    },
    {
      'User-Agent': 'VLC/3.0.18 LibVLC/3.0.18',
      'X-Requested-With': 'org.videolan.vlc'
    }
  ];

  try {
    let content = '';

    if (isNative) {
      if (onProgress) onProgress('Optimisation du tunnel réseau...');
      
      let lastError = null;
      
      for (let i = 0; i < NATIVE_FINGERPRINTS.length; i++) {
        const fingerprint = NATIVE_FINGERPRINTS[i];
        if (onProgress) onProgress(`Tentative de connexion ${i + 1}/${NATIVE_FINGERPRINTS.length}...`);
        
        try {
          const response = await CapacitorHttp.get({ 
            url: urlWithCacheBuster, 
            headers: {
              ...fingerprint,
              'Accept': '*/*',
              'Accept-Language': 'fr-FR',
              'Connection': 'keep-alive',
              'Cache-Control': 'no-cache'
            },
            connectTimeout: 30000,
            readTimeout: 60000
          });
          
          if (response.status === 200) {
            content = typeof response.data === 'string' ? response.data : JSON.stringify(response.data);
            if (content && content.length > 50) break; 
          } else {
            console.warn(`Native fingerprint ${i} failed with status ${response.status}`);
            lastError = `Statut ${response.status}`;
            continue;
          }
        } catch (e: any) {
          lastError = e.message;
          console.warn(`Native attempt ${i} failed:`, e);
        }
      }

      if (!content) {
        if (onProgress) onProgress('Passage au mode de secours stable...');
        // Fallback to Proxy even on Native if CapacitorHttp fails
        content = await fetchWithProxy(urlWithCacheBuster, onProgress);
      }
    } else {
      content = await fetchWithProxy(urlWithCacheBuster, onProgress);
    }
    
    if (!content) throw new Error("Réponse vide du serveur");

    // Force string matching
    if (onProgress) onProgress('Décodage du flux (' + Math.round(content.length / 1024) + ' KB)...');
    const contentStr = String(content).trim();

    let channels: Channel[] = [];
    if (contentStr.startsWith('{') || contentStr.startsWith('[')) {
      if (onProgress) onProgress('Analyse du JSON...');
      channels = parseJSONPlaylist(contentStr, isXtream ? xtreamHost : undefined, xtreamUser, xtreamPass);
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
