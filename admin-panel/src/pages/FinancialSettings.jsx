import { useState, useEffect } from 'react';
import api from '../api';

export default function FinancialSettings() {
  const [settings, setSettings] = useState({
    earning_per_1000_views: 0.50,
    referral_percentage: 10,
    min_withdrawal_amount: 5.00,
    gate_ad_enabled: true,
    gate_ad_duration: 5,
    view_cooldown_hours: 24,
    weekly_subscription_price: 1.99,
    monthly_subscription_price: 4.99,
    free_storage_limit_gb: 10,
    premium_storage_limit_gb: 200
  });
  const [cpmRates, setCpmRates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState(null);
  const [editingRate, setEditingRate] = useState(null);
  const [showAddForm, setShowAddForm] = useState(false);
  const [newRate, setNewRate] = useState({ country_code: '', country_name: '', country_name_ar: '', cpm_rate: 0.10 });

  useEffect(() => {
    fetchSettings();
    fetchCpmRates();
  }, []);

  const fetchSettings = async () => {
    try {
      setLoading(true);
      const response = await api.get('/admin/financial-settings');
      const data = response.data.data.settings;
      
      const newSettings = {};
      Object.keys(data).forEach(key => {
        newSettings[key] = data[key].value;
      });
      setSettings(prev => ({ ...prev, ...newSettings }));
    } catch (error) {
      console.error('Error fetching settings:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchCpmRates = async () => {
    try {
      const response = await api.get('/admin/cpm-rates');
      setCpmRates(response.data.data.rates || []);
    } catch (error) {
      console.error('Error fetching CPM rates:', error);
    }
  };

  const handleSave = async () => {
    try {
      setSaving(true);
      await api.put('/admin/financial-settings', { settings });
      setMessage({ type: 'success', text: 'تم حفظ الإعدادات بنجاح' });
      setTimeout(() => setMessage(null), 3000);
    } catch (error) {
      setMessage({ type: 'error', text: 'خطأ في حفظ الإعدادات' });
    } finally {
      setSaving(false);
    }
  };

  const handleChange = (key, value) => {
    setSettings(prev => ({ ...prev, [key]: value }));
  };

  const handleAddCpmRate = async () => {
    try {
      if (!newRate.country_code || !newRate.country_name) {
        setMessage({ type: 'error', text: 'يرجى ملء كود الدولة والاسم' });
        return;
      }
      await api.post('/admin/cpm-rates', newRate);
      setMessage({ type: 'success', text: 'تم إضافة الدولة بنجاح' });
      setShowAddForm(false);
      setNewRate({ country_code: '', country_name: '', country_name_ar: '', cpm_rate: 0.10 });
      fetchCpmRates();
      setTimeout(() => setMessage(null), 3000);
    } catch (error) {
      setMessage({ type: 'error', text: 'خطأ في إضافة الدولة' });
    }
  };

  const handleUpdateCpmRate = async (id, cpm_rate) => {
    try {
      await api.put(`/admin/cpm-rates/${id}`, { cpm_rate: parseFloat(cpm_rate) });
      setEditingRate(null);
      fetchCpmRates();
      setMessage({ type: 'success', text: 'تم تحديث السعر' });
      setTimeout(() => setMessage(null), 3000);
    } catch (error) {
      setMessage({ type: 'error', text: 'خطأ في تحديث السعر' });
    }
  };

  const handleToggleCpmRate = async (id, is_active) => {
    try {
      await api.put(`/admin/cpm-rates/${id}`, { is_active: !is_active });
      fetchCpmRates();
    } catch (error) {
      setMessage({ type: 'error', text: 'خطأ في تحديث الحالة' });
    }
  };

  const handleDeleteCpmRate = async (id) => {
    if (!window.confirm('هل أنت متأكد من حذف هذه الدولة؟')) return;
    try {
      await api.delete(`/admin/cpm-rates/${id}`);
      fetchCpmRates();
      setMessage({ type: 'success', text: 'تم الحذف' });
      setTimeout(() => setMessage(null), 3000);
    } catch (error) {
      setMessage({ type: 'error', text: 'خطأ في الحذف' });
    }
  };

  if (loading) {
    return <div className="p-8 text-center text-gray-500">جاري التحميل...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-900">الإعدادات المالية</h1>
        <button
          onClick={handleSave}
          disabled={saving}
          className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50"
        >
          {saving ? 'جاري الحفظ...' : 'حفظ الإعدادات العامة'}
        </button>
      </div>

      {message && (
        <div className={`p-4 rounded-lg ${
          message.type === 'success' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
        }`}>
          {message.text}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Earnings Settings */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-lg font-semibold mb-4 text-gray-900 border-b pb-2">
            إعدادات الأرباح العامة
          </h2>
          
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                CPM الافتراضي (للدول غير المحددة) ($)
              </label>
              <input
                type="number"
                step="0.01"
                value={settings.earning_per_1000_views}
                onChange={(e) => handleChange('earning_per_1000_views', parseFloat(e.target.value))}
                className="w-full border rounded-lg p-3 focus:ring-2 focus:ring-blue-500"
              />
              <p className="text-xs text-gray-500 mt-1">
                يُستخدم هذا السعر للدول التي لم يتم تحديد سعر خاص لها
              </p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                نسبة أرباح الإحالة (%)
              </label>
              <input
                type="number"
                step="1"
                min="0"
                max="100"
                value={settings.referral_percentage}
                onChange={(e) => handleChange('referral_percentage', parseInt(e.target.value))}
                className="w-full border rounded-lg p-3 focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                الحد الأدنى للسحب ($)
              </label>
              <input
                type="number"
                step="0.01"
                value={settings.min_withdrawal_amount}
                onChange={(e) => handleChange('min_withdrawal_amount', parseFloat(e.target.value))}
                className="w-full border rounded-lg p-3 focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>
        </div>

        {/* Gate Ad Settings */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-lg font-semibold mb-4 text-gray-900 border-b pb-2">
            إعدادات بوابة الإعلان (Gate Ad)
          </h2>
          
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <label className="block text-sm font-medium text-gray-700">
                  تفعيل بوابة الإعلان
                </label>
                <p className="text-xs text-gray-500">
                  عند التعطيل، لن يتم احتساب أي مشاهدات للربح
                </p>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={settings.gate_ad_enabled}
                  onChange={(e) => handleChange('gate_ad_enabled', e.target.checked)}
                  className="sr-only peer"
                />
                <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
              </label>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                مدة الإعلان (ثواني)
              </label>
              <input
                type="number"
                step="1"
                min="3"
                max="30"
                value={settings.gate_ad_duration}
                onChange={(e) => handleChange('gate_ad_duration', parseInt(e.target.value))}
                className="w-full border rounded-lg p-3 focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                فترة الانتظار بين المشاهدات (ساعات)
              </label>
              <input
                type="number"
                step="1"
                min="1"
                value={settings.view_cooldown_hours}
                onChange={(e) => handleChange('view_cooldown_hours', parseInt(e.target.value))}
                className="w-full border rounded-lg p-3 focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>
        </div>

        {/* Subscription Settings */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-lg font-semibold mb-4 text-gray-900 border-b pb-2">
            ⭐ إعدادات الاشتراك Premium
          </h2>
          
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                سعر الاشتراك الأسبوعي ($)
              </label>
              <input
                type="number"
                step="0.01"
                value={settings.weekly_subscription_price}
                onChange={(e) => handleChange('weekly_subscription_price', parseFloat(e.target.value))}
                className="w-full border rounded-lg p-3 focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                سعر الاشتراك الشهري ($)
              </label>
              <input
                type="number"
                step="0.01"
                value={settings.monthly_subscription_price}
                onChange={(e) => handleChange('monthly_subscription_price', parseFloat(e.target.value))}
                className="w-full border rounded-lg p-3 focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>
        </div>

        {/* Storage Quota Settings */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-lg font-semibold mb-4 text-gray-900 border-b pb-2">
            إعدادات سعة التخزين
          </h2>
          
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                سعة التخزين للمستخدم المجاني (GB)
              </label>
              <input
                type="number"
                step="1"
                min="1"
                value={settings.free_storage_limit_gb}
                onChange={(e) => handleChange('free_storage_limit_gb', parseInt(e.target.value))}
                className="w-full border rounded-lg p-3 focus:ring-2 focus:ring-blue-500"
              />
              <p className="text-xs text-gray-500 mt-1">
                الحد الأقصى للتخزين للمستخدمين المجانيين
              </p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                سعة التخزين لمستخدم Premium (GB)
              </label>
              <input
                type="number"
                step="1"
                min="1"
                value={settings.premium_storage_limit_gb}
                onChange={(e) => handleChange('premium_storage_limit_gb', parseInt(e.target.value))}
                className="w-full border rounded-lg p-3 focus:ring-2 focus:ring-blue-500"
              />
              <p className="text-xs text-gray-500 mt-1">
                الحد الأقصى للتخزين للمستخدمين المشتركين
              </p>
            </div>
          </div>
        </div>

        {/* Info Card */}
        <div className="bg-blue-50 rounded-lg shadow p-6 border border-blue-200">
          <h2 className="text-lg font-semibold mb-4 text-blue-900">
            معلومات مهمة
          </h2>
          
          <ul className="space-y-2 text-sm text-blue-800">
            <li>• أسعار CPM تُحدد حسب دولة المشاهد (IP)</li>
            <li>• الدول غير المحددة تستخدم السعر الافتراضي</li>
            <li>• المشاهدات تُحتسب فقط عند إكمال Gate Ad</li>
            <li>• Premium يتجاوزون الإعلانات ولا تُحتسب مشاهداتهم للربح</li>
            <li>• أرباح الإحالة تُضاف تلقائياً</li>
          </ul>
        </div>
      </div>

      {/* CPM by Country Section */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex justify-between items-center mb-4 border-b pb-4">
          <h2 className="text-lg font-semibold text-gray-900">
            أسعار CPM حسب الدولة (لكل 1000 مشاهدة)
          </h2>
          <button
            onClick={() => setShowAddForm(!showAddForm)}
            className="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 text-sm"
          >
            {showAddForm ? 'إلغاء' : '+ إضافة دولة'}
          </button>
        </div>

        {/* Add New Country Form */}
        {showAddForm && (
          <div className="bg-gray-50 rounded-lg p-4 mb-4 border">
            <div className="grid grid-cols-1 md:grid-cols-5 gap-3 items-end">
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">كود الدولة</label>
                <input
                  type="text"
                  maxLength={2}
                  placeholder="US"
                  value={newRate.country_code}
                  onChange={(e) => setNewRate({...newRate, country_code: e.target.value.toUpperCase()})}
                  className="w-full border rounded-lg p-2 text-sm uppercase"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">اسم الدولة (EN)</label>
                <input
                  type="text"
                  placeholder="United States"
                  value={newRate.country_name}
                  onChange={(e) => setNewRate({...newRate, country_name: e.target.value})}
                  className="w-full border rounded-lg p-2 text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">اسم الدولة (AR)</label>
                <input
                  type="text"
                  placeholder="الولايات المتحدة"
                  value={newRate.country_name_ar}
                  onChange={(e) => setNewRate({...newRate, country_name_ar: e.target.value})}
                  className="w-full border rounded-lg p-2 text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">CPM ($)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  value={newRate.cpm_rate}
                  onChange={(e) => setNewRate({...newRate, cpm_rate: parseFloat(e.target.value)})}
                  className="w-full border rounded-lg p-2 text-sm"
                />
              </div>
              <button
                onClick={handleAddCpmRate}
                className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 text-sm h-[38px]"
              >
                إضافة
              </button>
            </div>
          </div>
        )}

        {/* CPM Rates Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 text-gray-600">
                <th className="p-3 text-right">الكود</th>
                <th className="p-3 text-right">الدولة</th>
                <th className="p-3 text-right">بالعربية</th>
                <th className="p-3 text-right">CPM ($)</th>
                <th className="p-3 text-center">الحالة</th>
                <th className="p-3 text-center">إجراءات</th>
              </tr>
            </thead>
            <tbody>
              {cpmRates.map((rate) => (
                <tr key={rate.id} className={`border-t hover:bg-gray-50 ${!rate.is_active ? 'opacity-50' : ''}`}>
                  <td className="p-3 font-mono font-bold">{rate.country_code}</td>
                  <td className="p-3">{rate.country_name}</td>
                  <td className="p-3">{rate.country_name_ar || '-'}</td>
                  <td className="p-3">
                    {editingRate === rate.id ? (
                      <input
                        type="number"
                        step="0.01"
                        min="0"
                        defaultValue={rate.cpm_rate}
                        onBlur={(e) => handleUpdateCpmRate(rate.id, e.target.value)}
                        onKeyDown={(e) => { if (e.key === 'Enter') handleUpdateCpmRate(rate.id, e.target.value); }}
                        className="w-24 border rounded p-1 text-sm"
                        autoFocus
                      />
                    ) : (
                      <span
                        className="cursor-pointer text-blue-600 hover:underline font-semibold"
                        onClick={() => setEditingRate(rate.id)}
                      >
                        ${parseFloat(rate.cpm_rate).toFixed(2)}
                      </span>
                    )}
                  </td>
                  <td className="p-3 text-center">
                    <button
                      onClick={() => handleToggleCpmRate(rate.id, rate.is_active)}
                      className={`px-2 py-1 rounded text-xs font-medium ${
                        rate.is_active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
                      }`}
                    >
                      {rate.is_active ? 'مفعّل' : 'معطّل'}
                    </button>
                  </td>
                  <td className="p-3 text-center">
                    <button
                      onClick={() => handleDeleteCpmRate(rate.id)}
                      className="text-red-500 hover:text-red-700 text-xs"
                    >
                      حذف
                    </button>
                  </td>
                </tr>
              ))}
              {cpmRates.length === 0 && (
                <tr>
                  <td colSpan="6" className="p-8 text-center text-gray-400">
                    لا توجد أسعار مخصصة. سيتم استخدام السعر الافتراضي لجميع الدول.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
