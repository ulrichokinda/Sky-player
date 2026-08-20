export interface XtreamCredentials {
  url: string;
  username: string;
  password: string;
}

export interface XtreamChannel {
  num: number;
  name: string;
  stream_type: string;
  stream_id: number;
  stream_icon: string;
  epg_channel_id: string;
  added: string;
  category_id: string;
  custom_sid: string;
  tv_archive: number;
  direct_source: string;
  tv_archive_duration: number;
}

export const xtreamService = {
  async login(creds: XtreamCredentials) {
    const url = `${creds.url}/player_api.php?username=${creds.username}&password=${creds.password}`;
    const response = await fetch(url);
    return response.json();
  },

  async getLiveChannels(creds: XtreamCredentials): Promise<XtreamChannel[]> {
    const url = `${creds.url}/player_api.php?username=${creds.username}&password=${creds.password}&action=get_live_streams`;
    const response = await fetch(url);
    return response.json();
  },

  async getCategories(creds: XtreamCredentials, type: 'live' | 'vod' | 'series' = 'live') {
    const action = type === 'live' ? 'get_live_categories' : type === 'vod' ? 'get_vod_categories' : 'get_series_categories';
    const url = `${creds.url}/player_api.php?username=${creds.username}&password=${creds.password}&action=${action}`;
    const response = await fetch(url);
    return response.json();
  },

  async getShortEPG(creds: XtreamCredentials, streamId: number) {
    const url = `${creds.url}/player_api.php?username=${creds.username}&password=${creds.password}&action=get_short_epg&stream_id=${streamId}`;
    const response = await fetch(url);
    return response.json();
  },

  async getFullEPG(creds: XtreamCredentials, streamId: number) {
    const url = `${creds.url}/player_api.php?username=${creds.username}&password=${creds.password}&action=get_simple_data_table&stream_id=${streamId}`;
    const response = await fetch(url);
    return response.json();
  },

  async getXMLTV(creds: XtreamCredentials) {
    const url = `${creds.url}/xmltv.php?username=${creds.username}&password=${creds.password}`;
    const response = await fetch(url);
    return response.text();
  },

  getStreamUrl(creds: XtreamCredentials, streamId: number, extension: string = 'ts') {
    // Basic protection: we could obfuscate this or use a proxy, but for now we ensure it's built correctly
    return `${creds.url}/live/${creds.username}/${creds.password}/${streamId}.${extension}`;
  },

  getCatchupUrl(creds: XtreamCredentials, streamId: number, start: string, duration: number) {
    // Format: /timeshift/username/password/duration/start_time/stream_id.ts
    return `${creds.url}/timeshift/${creds.username}/${creds.password}/${duration}/${start}/${streamId}.ts`;
  }
};
