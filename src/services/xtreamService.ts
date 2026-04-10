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

  getStreamUrl(creds: XtreamCredentials, streamId: number, extension: string = 'ts') {
    return `${creds.url}/live/${creds.username}/${creds.password}/${streamId}.${extension}`;
  }
};
