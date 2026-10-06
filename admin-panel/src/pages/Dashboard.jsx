import { useState, useEffect } from 'react';
import { getDashboardStats } from '../api';
import { Users, FileText, CreditCard, Eye, TrendingUp, DollarSign } from 'lucide-react';

export default function Dashboard() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    try {
      const response = await getDashboardStats();
      if (response.data.success) {
        setStats(response.data.data);
      }
    } catch (err) {
      console.error('Error loading stats:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
      </div>
    );
  }

  const statCards = [
    { label: 'إجمالي المستخدمين', value: stats?.users_count || 0, icon: Users, color: 'bg-blue-500' },
    { label: 'إجمالي الملفات', value: stats?.files_count || stats?.videos_count || 0, icon: FileText, color: 'bg-green-500' },
    { label: 'إجمالي المشاهدات', value: stats?.total_views || 0, icon: Eye, color: 'bg-purple-500' },
    { label: 'المشتركين Premium', value: stats?.premium_users || 0, icon: CreditCard, color: 'bg-yellow-500' },
    { label: 'طلبات الدفع المعلقة', value: stats?.pending_payments || 0, icon: DollarSign, color: 'bg-red-500' },
    { label: 'الإيرادات الشهرية', value: `$${stats?.monthly_revenue || 0}`, icon: TrendingUp, color: 'bg-indigo-500' },
  ];

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-800">لوحة التحكم</h1>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {statCards.map((stat, index) => {
          const Icon = stat.icon;
          return (
            <div key={index} className="bg-white rounded-xl shadow-sm p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-gray-500 text-sm">{stat.label}</p>
                  <p className="text-3xl font-bold text-gray-800 mt-2">{stat.value}</p>
                </div>
                <div className={`${stat.color} p-4 rounded-xl`}>
                  <Icon className="text-white" size={24} />
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Recent Activity */}
      <div className="bg-white rounded-xl shadow-sm p-6">
        <h2 className="text-lg font-semibold text-gray-800 mb-4">النشاط الأخير</h2>
        <div className="text-gray-500 text-center py-8">
          سيتم عرض النشاطات الأخيرة هنا
        </div>
      </div>
    </div>
  );
}
