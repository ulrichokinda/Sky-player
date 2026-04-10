export const runSpeedTest = async (onProgress: (speed: number) => void) => {
  const testUrl = 'https://picsum.photos/seed/speedtest/2000/2000'; // Large image for testing
  const startTime = Date.now();
  
  try {
    const response = await fetch(testUrl, { cache: 'no-store' });
    const reader = response.body?.getReader();
    if (!reader) throw new Error('Reader not available');

    let receivedLength = 0;
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      receivedLength += value.length;
      
      const currentTime = Date.now();
      const duration = (currentTime - startTime) / 1000;
      const speedBps = (receivedLength * 8) / duration;
      const speedMbps = speedBps / (1024 * 1024);
      onProgress(speedMbps);
    }
    
    return true;
  } catch (e) {
    console.error('Speed test failed', e);
    return false;
  }
};
