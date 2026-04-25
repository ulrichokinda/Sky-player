import react from '@vitejs/plugin-react';
import legacy from '@vitejs/plugin-legacy';
import * as path from 'path';
import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({mode}) => {
  const env = loadEnv(mode, process.cwd(), '');
  return {
    base: '', // CRITICAL FOR TV APPS (APK, IPK, WGT) (Empty string for Capacitor)
    plugins: [
      react(),
      legacy({
        targets: ['chrome >= 49', 'edge >= 12', 'firefox >= 45', 'safari >= 10'],
        additionalLegacyPolyfills: ['regenerator-runtime/runtime']
      })
    ],
    define: {
      'process.env.GEMINI_API_KEY': JSON.stringify(env.GEMINI_API_KEY || ''),
    },
    resolve: {
      alias: {
        '@': path.resolve('./src'),
      },
    },
    server: {
      hmr: false,
      port: 3000,
      host: '0.0.0.0',
    },
    build: {
      target: ['es2015', 'edge88', 'firefox78', 'chrome87', 'safari14'], // Force compatibilité pour Smart TV
      chunkSizeWarningLimit: 1000,
      rollupOptions: {
        output: {
          manualChunks: {
            'vendor-react': ['react', 'react-dom', 'react-router-dom'],
            'vendor-firebase': ['firebase/app', 'firebase/auth', 'firebase/firestore', 'firebase/storage'],
            'vendor-player': ['video.js', 'hls.js', 'shaka-player'],
            'vendor-charts': ['recharts'],
            'vendor-ui': ['lucide-react', 'motion/react']
          }
        }
      }
    }
  };
});
