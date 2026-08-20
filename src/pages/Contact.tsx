import React from 'react';
import { Card, Input, Textarea, Button } from '../components/ui';
import { Footer } from '../components/Footer';
import { Header } from '../components/Header';
import { motion } from 'motion/react';
import { MessageSquare, Phone, Mail } from 'lucide-react';

export const Contact = () => (
  <div className="min-h-screen bg-black text-white selection:bg-primary/30">
    <Header />
    
    <main className="max-w-5xl mx-auto px-6 py-12 md:py-20">
      <div className="grid md:grid-cols-2 gap-12 items-start">
        <div className="space-y-8">
          <div className="space-y-4">
            <motion.h1 
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              className="text-5xl font-black tracking-tighter"
            >
              Parlons <span className="text-primary">Ensemble</span>
            </motion.h1>
            <p className="text-zinc-400 leading-relaxed">
              Une question technique ? Un partenariat en vue ? Notre équipe est à votre écoute pour vous accompagner dans votre expérience Sky Player.
            </p>
          </div>

          <div className="space-y-6">
            <div className="flex items-center gap-4 group">
              <div className="w-12 h-12 bg-zinc-900 rounded-2xl flex items-center justify-center text-primary group-hover:bg-primary group-hover:text-black transition-all">
                <Phone size={20} />
              </div>
              <div>
                <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">WhatsApp Support</p>
                <p className="font-bold">+242 06 583 82 96 / 05 540 40 76</p>
              </div>
            </div>
            <div className="flex items-center gap-4 group">
              <div className="w-12 h-12 bg-zinc-900 rounded-2xl flex items-center justify-center text-primary group-hover:bg-primary group-hover:text-black transition-all">
                <Mail size={20} />
              </div>
              <div>
                <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Email</p>
                <p className="font-bold">inestaulrichokinda@gmail.com</p>
              </div>
            </div>
            <div className="flex items-center gap-4 group">
              <div className="w-12 h-12 bg-zinc-900 rounded-2xl flex items-center justify-center text-primary group-hover:bg-primary group-hover:text-black transition-all">
                <MessageSquare size={20} />
              </div>
              <div>
                <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Réseaux Sociaux</p>
                <p className="font-bold">@SkyPlayerApp</p>
              </div>
            </div>
          </div>
        </div>

        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ delay: 0.1 }}
        >
          <Card className="p-8 space-y-6">
            <h2 className="text-xl font-bold">Envoyez un message</h2>
            <div className="space-y-4">
              <Input label="Nom complet" placeholder="Ex: Jean Dupont" />
              <Input label="Adresse Email" placeholder="jean@exemple.com" />
              <Textarea label="Votre Message" placeholder="Comment pouvons-nous vous aider ?" />
              <Button fullWidth size="lg">Envoyer le message</Button>
            </div>
          </Card>
        </motion.div>
      </div>
    </main>

    <Footer />
  </div>
);
