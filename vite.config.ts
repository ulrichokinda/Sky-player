import react from '@vitejs/plugin-react';
import path from 'path';
import {defineConfig, loadEnv} from 'vite';

export default defineConfig(({mode}) => {
  const env = loadEnv(mode, '.', '');
  return {
    base: '', // CRITICAL FOR TV APPS (APK, IPK, WGT) (Empty string for Capacitor)
    plugins: [
      react()
    ],
    define: {
      'process.env.GEMINI_API_KEY': JSON.stringify(env.GEMINI_API_KEY),
    },
    resolve: {
      alias: {
        '@': path.resolve(__dirname, '.'),
      },
    },
    server: {
      hmr: false,
      port: 3000,
      host: '0.0.0.0',
    },
    build: {
      target: ['es2015', 'edge88', 'firefox78', 'chrome87', 'safari14'], // Force compatibilité pour Smart TV
      chunkSizeWarningLimit: 2000,
      rollupOptions: {
        output: {
          manualChunks: undefined
        }
      }
    }
  };
});
