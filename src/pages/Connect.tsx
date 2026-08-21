import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Card, Button, Input } from '../components/ui';
import { Wifi, Tv, Link as LinkIcon, AlertCircle, CheckCircle2 } from 'lucide-react';
import { motion } from 'motion/react';

export function Connect() {
  const [searchParams] = useSearchParams();
  const [macAddress, setMacAddress] = useState<string | null>(null);
  const [isValidMac, setIsValidMac] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<boolean>(false);
  
  // Form states
  const [playlistType, setPlaylistType] = useState<'m3u' | 'xtream'>('m3u');
  const [m3uUrl, setM3uUrl] = useState('');
  const [xtreamServer, setXtreamServer] = useState('');
  const [xtreamUser, setXtreamUser] = useState('');
  const [xtreamPass, setXtreamPass] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    // 1. Récupère proprement le paramètre 'mac' depuis l'URL
    const macParam = searchParams.get('mac');
    
    if (!macParam) {
      setError("Erreur : Adresse MAC manquante. Veuillez scanner le QR code à nouveau.");
      return;
    }

    // 2. Nettoie et valide l'adresse MAC
    // Remove all non-hex characters (spaces, dashes, colons) to normalize
    const cleanedMac = macParam.replace(/[^a-fA-F0-9]/g, '').toUpperCase();
    
    // Regex pour vérifier le format MAC standard à 12 ou 16 caractères hexadécimaux
    const macRegex = /^([A-F0-9]{12}|[A-F0-9]{16})$/;

    if (!macRegex.test(cleanedMac)) {
      setError("Erreur : Adresse MAC invalide. Le format n'est pas reconnu.");
      return;
    }

    // Reformat for display (e.g. 00:11:22:33:44:55)
    const formattedMac = cleanedMac.match(/.{1,2}/g)?.join(':') || cleanedMac;
    
    setMacAddress(formattedMac);
    setIsValidMac(true);
  }, [searchParams]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const payload = {
        mac: macAddress,
        playlist_url: playlistType === 'm3u' ? m3uUrl : '',
        xtream_host: playlistType === 'xtream' ? xtreamServer : '',
        xtream_username: playlistType === 'xtream' ? xtreamUser : '',
        xtream_password: playlistType === 'xtream' ? xtreamPass : ''
      };

      const response = await fetch('/api/playlist/associate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (!response.ok) {
        throw new Error("Erreur de communication avec le serveur.");
      }
      
      setSuccess(true);
    } catch (err: any) {
      alert("Une erreur est survenue lors de l'association.");
    } finally {
      setLoading(false);
    }
  };

  if (error) {
    return (
      <div className="min-h-screen bg-black text-white flex items-center justify-center p-6">
        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="max-w-md w-full bg-red-950/20 border border-red-500/50 rounded-2xl p-8 text-center"
        >
          <div className="w-16 h-16 bg-red-500/20 rounded-full flex items-center justify-center mx-auto mb-6">
            <AlertCircle className="text-red-500" size={32} />
          </div>
          <h2 className="text-2xl font-bold text-white mb-2">Erreur de Connexion</h2>
          <p className="text-red-200">{error}</p>
        </motion.div>
      </div>
    );
  }

  if (success) {
    return (
      <div className="min-h-screen bg-black text-white flex items-center justify-center p-6">
        <motion.div 
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          className="max-w-md w-full bg-green-950/20 border border-green-500/50 rounded-2xl p-8 text-center"
        >
          <div className="w-16 h-16 bg-green-500/20 rounded-full flex items-center justify-center mx-auto mb-6">
            <CheckCircle2 className="text-green-500" size={32} />
          </div>
          <h2 className="text-2xl font-bold text-white mb-2">Appareil Connecté !</h2>
          <p className="text-green-200 mb-6">
            Votre playlist a été associée avec succès à l'appareil <strong>{macAddress}</strong>.
          </p>
          <p className="text-sm text-zinc-400">
            Vous pouvez maintenant redémarrer l'application sur votre TV / Appareil.
          </p>
        </motion.div>
      </div>
    );
  }

  if (!isValidMac) {
    return (
      <div className="min-h-screen bg-black flex items-center justify-center">
        <div className="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-black text-white p-6 py-12 flex flex-col items-center justify-center bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-primary/10 via-black to-black">
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="max-w-xl w-full space-y-8"
      >
        <div className="text-center space-y-4">
          <div className="w-20 h-20 bg-primary/20 text-primary rounded-full flex items-center justify-center mx-auto shadow-[0_0_50px_rgba(var(--color-primary-rgb),0.3)]">
            <Tv size={40} />
          </div>
          <h1 className="text-3xl font-bold font-display tracking-tight">Connecter un Appareil</h1>
          <p className="text-zinc-400">
            Liez votre contenu à l'appareil TV via son adresse MAC.
          </p>
          <div className="inline-block bg-zinc-900 border border-zinc-800 rounded-lg px-4 py-2 font-mono text-primary font-bold tracking-widest text-lg">
            {macAddress}
          </div>
        </div>

        <Card className="bg-zinc-950 border-zinc-800 p-6 md:p-8">
          <div className="flex bg-zinc-900 p-1 rounded-xl mb-8">
            <button
              type="button"
              onClick={() => setPlaylistType('m3u')}
              className={`flex-1 py-2 text-sm font-bold rounded-lg transition-all ${
                playlistType === 'm3u' ? 'bg-zinc-800 text-white shadow-sm' : 'text-zinc-500 hover:text-white'
              }`}
            >
              Lien M3U
            </button>
            <button
              type="button"
              onClick={() => setPlaylistType('xtream')}
              className={`flex-1 py-2 text-sm font-bold rounded-lg transition-all ${
                playlistType === 'xtream' ? 'bg-zinc-800 text-white shadow-sm' : 'text-zinc-500 hover:text-white'
              }`}
            >
              API Xtream
            </button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            {playlistType === 'm3u' ? (
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-zinc-400 mb-2">Lien de la Playlist (URL M3U)</label>
                  <Input 
                    type="url" 
                    placeholder="http://exemple.com/get.php?username=...&password=...&type=m3u_plus" 
                    value={m3uUrl}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => setM3uUrl(e.target.value)}
                    required
                    className="bg-black border-zinc-800"
                  />
                </div>
              </div>
            ) : (
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-zinc-400 mb-2">URL du Serveur</label>
                  <Input 
                    type="url" 
                    placeholder="http://serveur-xtream.com:8080" 
                    value={xtreamServer}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => setXtreamServer(e.target.value)}
                    required
                    className="bg-black border-zinc-800"
                  />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-zinc-400 mb-2">Nom d'utilisateur</label>
                    <Input 
                      type="text" 
                      placeholder="Username" 
                      value={xtreamUser}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) => setXtreamUser(e.target.value)}
                      required
                      className="bg-black border-zinc-800"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-zinc-400 mb-2">Mot de passe</label>
                    <Input 
                      type="password" 
                      placeholder="••••••••" 
                      value={xtreamPass}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) => setXtreamPass(e.target.value)}
                      required
                      className="bg-black border-zinc-800"
                    />
                  </div>
                </div>
              </div>
            )}

            <Button 
              type="submit" 
              fullWidth 
              size="lg" 
              loading={loading}
              icon={LinkIcon}
              className="text-black font-bold mt-8"
            >
              Associer la Playlist
            </Button>
          </form>
        </Card>
      </motion.div>
    </div>
  );
}
