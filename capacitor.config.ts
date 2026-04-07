import { defineConfig } from '@capacitor/cli';

const config: defineConfig = {
  appId: 'com.skyplayer.app',
  appName: 'Sky Player',
  webDir: 'dist',
  bundledWebRuntime: false,
  server: {
    androidScheme: 'https'
  }
};

export default config;
