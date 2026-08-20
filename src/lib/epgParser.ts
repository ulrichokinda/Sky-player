export interface EPGProgram {
  title: string;
  start: string;
  end: string;
  description?: string;
}

export const parseEPG = (data: any): EPGProgram[] => {
  if (!data || !data.epg_listings) return [];
  
  return data.epg_listings.map((item: any) => ({
    title: atob(item.title), // Xtream often base64 encodes titles in short EPG
    start: item.start,
    end: item.end,
    description: item.description ? atob(item.description) : undefined
  }));
};

export const formatTime = (dateStr: string) => {
  const date = new Date(dateStr);
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
};
