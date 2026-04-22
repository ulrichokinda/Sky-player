import React, { useMemo } from 'react';
import { 
  BarChart, Bar, LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, 
  PieChart, Pie, Cell, Legend, AreaChart, Area
} from 'recharts';
import { format, subDays, startOfDay, isWithinInterval, parseISO } from 'date-fns';
import { fr } from 'date-fns/locale';
import { motion } from 'motion/react';
import { Card, Badge } from './ui';
import { TrendingUp, Users, CreditCard, Activity, Globe, Monitor } from 'lucide-react';

interface DashboardStatsProps {
  activations: any[];
  payments: any[];
  users?: any[];
  isAdmin?: boolean;
}

const COLORS = ['#D4FF00', '#1A1A1A', '#333333', '#4ade80', '#f87171', '#60a5fa', '#fbbf24'];

export const DashboardStats: React.FC<DashboardStatsProps> = ({ activations, payments, users = [], isAdmin = false }) => {
  
  // Logic to process data for charts
  const stats = useMemo(() => {
    const last30Days = Array.from({ length: 30 }, (_, i) => {
      const date = subDays(new Date(), i);
      return format(date, 'yyyy-MM-dd');
    }).reverse();

    const dailyData = last30Days.map(date => {
      const dayActivations = activations.filter(a => {
        const d = a.createdAt?.seconds ? new Date(a.createdAt.seconds * 1000) : new Date(a.createdAt);
        return format(d, 'yyyy-MM-dd') === date;
      });

      const dayPayments = payments.filter(p => {
        const d = p.createdAt?.seconds ? new Date(p.createdAt.seconds * 1000) : new Date(p.createdAt);
        return format(d, 'yyyy-MM-dd') === date && p.status === 'completed';
      });

      return {
        date: format(parseISO(date), 'dd MMM', { locale: fr }),
        activations: dayActivations.length,
        revenue: dayPayments.reduce((acc, p) => acc + (p.amount || 0), 0),
        count: dayPayments.length
      };
    });

    // System distribution
    const systemsMap: Record<string, number> = {};
    activations.forEach(a => {
      const sys = a.system || 'Inconnu';
      systemsMap[sys] = (systemsMap[sys] || 0) + 1;
    });
    const systemData = Object.entries(systemsMap).map(([name, value]) => ({ name, value }));

    // Country distribution
    const countryMap: Record<string, number> = {};
    activations.forEach(a => {
      const country = a.country_code || a.country || 'N/A';
      countryMap[country] = (countryMap[country] || 0) + 1;
    });
    const countryData = Object.entries(countryMap)
      .map(([name, value]) => ({ name, value }))
      .sort((a, b) => b.value - a.value)
      .slice(0, 5);

    // Totals
    const totalRevenue = payments.filter(p => p.status === 'completed').reduce((acc, p) => acc + (p.amount || 0), 0);
    const mtdRevenue = payments.filter(p => {
      const d = p.createdAt?.seconds ? new Date(p.createdAt.seconds * 1000) : new Date(p.createdAt);
      return d.getMonth() === new Date().getMonth() && d.getFullYear() === new Date().getFullYear() && p.status === 'completed';
    }).reduce((acc, p) => acc + (p.amount || 0), 0);

    return {
      dailyData,
      systemData,
      countryData,
      totalRevenue,
      mtdRevenue,
      totalActivations: activations.length,
      activeUsers: users.length,
      totalCreditsUsed: activations.reduce((acc, a) => acc + (a.credits_used || 0), 0)
    };
  }, [activations, payments, users]);

  return (
    <div className="space-y-6">
      {/* Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="flex items-center gap-4 p-6 bg-zinc-900/40 border-zinc-800">
          <div className="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center text-primary">
            <TrendingUp size={24} />
          </div>
          <div>
            <p className="text-xs text-zinc-500 font-black uppercase tracking-widest">Chiffre d'Affaires</p>
            <h3 className="text-2xl font-black">{stats.totalRevenue.toLocaleString()} F</h3>
            <p className="text-[10px] text-zinc-500 mt-1">Ce mois: <span className="text-primary font-bold">{stats.mtdRevenue.toLocaleString()} F</span></p>
          </div>
        </Card>

        <Card className="flex items-center gap-4 p-6 bg-zinc-900/40 border-zinc-800">
          <div className="w-12 h-12 bg-blue-500/10 rounded-2xl flex items-center justify-center text-blue-500">
            <Activity size={24} />
          </div>
          <div>
            <p className="text-xs text-zinc-500 font-black uppercase tracking-widest">Activations</p>
            <h3 className="text-2xl font-black">{stats.totalActivations}</h3>
            <p className="text-[10px] text-zinc-500 mt-1">Total Crédits: <span className="text-blue-500 font-bold">{stats.totalCreditsUsed}</span></p>
          </div>
        </Card>

        {isAdmin && (
          <Card className="flex items-center gap-4 p-6 bg-zinc-900/40 border-zinc-800">
            <div className="w-12 h-12 bg-emerald-500/10 rounded-2xl flex items-center justify-center text-emerald-500">
              <Users size={24} />
            </div>
            <div>
              <p className="text-xs text-zinc-500 font-black uppercase tracking-widest">Revendeurs</p>
              <h3 className="text-2xl font-black">{stats.activeUsers}</h3>
              <p className="text-[10px] text-emerald-500 font-bold mt-1">Actifs</p>
            </div>
          </Card>
        )}

        <Card className="flex items-center gap-4 p-6 bg-zinc-900/40 border-zinc-800">
          <div className="w-12 h-12 bg-amber-500/10 rounded-2xl flex items-center justify-center text-amber-500">
            <CreditCard size={24} />
          </div>
          <div>
            <p className="text-xs text-zinc-500 font-black uppercase tracking-widest">Transactions</p>
            <h3 className="text-2xl font-black">{payments.length}</h3>
            <p className="text-[10px] text-zinc-500 mt-1">Réussies: <span className="text-amber-500 font-bold">{payments.filter(p => p.status === 'completed').length}</span></p>
          </div>
        </Card>
      </div>

      {/* Main Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Revenue Chart */}
        <Card className="p-6 bg-zinc-900/40 border-zinc-800">
          <div className="flex items-center justify-between mb-8">
            <h3 className="font-bold flex items-center gap-2">
              <TrendingUp size={18} className="text-primary" />
              Revenus Mobiles (CFA)
            </h3>
            <Badge variant="primary">Derniers 30 jours</Badge>
          </div>
          <div className="h-[300px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={stats.dailyData}>
                <defs>
                  <linearGradient id="colorRevenue" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#D4FF00" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#D4FF00" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#27272a" vertical={false} />
                <XAxis 
                  dataKey="date" 
                  stroke="#52525b" 
                  fontSize={10} 
                  tickLine={false} 
                  axisLine={false}
                  interval={4}
                />
                <YAxis 
                  stroke="#52525b" 
                  fontSize={10} 
                  tickLine={false} 
                  axisLine={false}
                  tickFormatter={(value) => `${value > 1000 ? value/1000 + 'k' : value}`}
                />
                <Tooltip 
                  contentStyle={{ backgroundColor: '#18181b', border: '1px solid #27272a', borderRadius: '12px' }}
                  itemStyle={{ color: '#D4FF00' }}
                />
                <Area type="monotone" dataKey="revenue" stroke="#D4FF00" fillOpacity={1} fill="url(#colorRevenue)" strokeWidth={3} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </Card>

        {/* Activations Chart */}
        <Card className="p-6 bg-zinc-900/40 border-zinc-800">
          <div className="flex items-center justify-between mb-8">
            <h3 className="font-bold flex items-center gap-2">
              <Activity size={18} className="text-blue-500" />
              Volume d'Activations
            </h3>
            <Badge variant="info">Croissance quotidienne</Badge>
          </div>
          <div className="h-[300px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={stats.dailyData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#27272a" vertical={false} />
                <XAxis 
                  dataKey="date" 
                  stroke="#52525b" 
                  fontSize={10} 
                  tickLine={false} 
                  axisLine={false}
                  interval={4}
                />
                <YAxis stroke="#52525b" fontSize={10} tickLine={false} axisLine={false} />
                <Tooltip 
                  contentStyle={{ backgroundColor: '#18181b', border: '1px solid #27272a', borderRadius: '12px' }}
                />
                <Bar dataKey="activations" fill="#3b82f6" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Systems distribution */}
        <Card className="p-6 bg-zinc-900/40 border-zinc-800">
          <h3 className="font-bold flex items-center gap-2 mb-6">
            <Monitor size={18} className="text-primary" />
            Par Système
          </h3>
          <div className="h-[250px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={stats.systemData}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={80}
                  paddingAngle={5}
                  dataKey="value"
                >
                  {stats.systemData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip 
                  contentStyle={{ backgroundColor: '#18181b', border: '1px solid #27272a', borderRadius: '12px' }}
                />
                <Legend layout="vertical" align="right" verticalAlign="middle" />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </Card>

        {/* Country distribution */}
        <Card className="lg:col-span-2 p-6 bg-zinc-900/40 border-zinc-800">
          <h3 className="font-bold flex items-center gap-2 mb-6">
            <Globe size={18} className="text-emerald-500" />
            Top 5 Pays d'Activation
          </h3>
          <div className="space-y-4">
            {stats.countryData.map((item, index) => (
              <div key={item.name} className="space-y-2">
                <div className="flex justify-between items-center text-sm">
                  <span className="font-bold">{item.name}</span>
                  <span className="text-zinc-500">{item.value} activations</span>
                </div>
                <div className="w-full h-2 bg-zinc-800 rounded-full overflow-hidden">
                  <motion.div 
                    initial={{ width: 0 }}
                    animate={{ width: `${(item.value / stats.totalActivations) * 100}%` }}
                    className="h-full bg-emerald-500"
                  />
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
};
