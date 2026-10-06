import { useState, useEffect } from 'react';
import { getAppSettings, updateAppSettings } from '../api';
import { Save, Upload } from 'lucide-react';

export default function Settings() {
  const [settings, setSettings] = useState({
    app_name: 'IQBox',
    app_logo_url: '',
    app_icon_url: '',
    primary_color: '#3B82F6',
    secondary_color: '#1E3A8A',
    support_email: '',
    support_phone: '',
    support_telegram: '',
    terms_url: '',
    privacy_url: ''
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    loadSettings();
  }, []);

  const loadSettings = async () => {
    try {
      const response = await getAppSettings();
      if (response.data.success) {
        setSettings({ ...settings, ...response.data.data.settings });
      }
    } catch (err) {
      console.error('Error loading settings:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setMessage('');

    try {
      const response = await updateAppSettings(settings);
      if (response.data.success) {
        setMessage('تم حفظ الإعدادات بنجاح');
      } else {
        setMessage('فشل حفظ الإعدادات');
      }
    } catch (err) {
      setMessage('خطأ في حفظ الإعدادات');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-800">إعدادات التطبيق</h1>

      <form onSubmit={handleSubmit} className="space-y-6">
        {message && (
          <div className={`p-4 rounded-lg ${message.includes('نجاح') ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'}`}>
            {message}
          </div>
        )}

        {/* App Identity */}
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h2 className="text-lg font-semibold text-gray-800 mb-4">هوية التطبيق</h2>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-gray-700 mb-2">اسم التطبيق</label>
              <input
                type="text"
                value={settings.app_name}
                onChange={(e) => setSettings({...settings, app_name: e.target.value})}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary outline-none"
              />
            </div>

            <div>
              <label className="block text-gray-700 mb-2">رابط اللوجو</label>
              <input
                type="url"
                value={settings.app_logo_url || ''}
                onChange={(e) => setSettings({...settings, app_logo_url: e.target.value})}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary outline-none"
                placeholder="https://example.com/logo.png"
              />
            </div>

            <div>
              <label className="block text-gray-700 mb-2">رابط الأيقونة</label>
              <input
                type="url"
                value={settings.app_icon_url || ''}
                onChange={(e) => setSettings({...settings, app_icon_url: e.target.value})}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary outline-none"
                placeholder="https://example.com/icon.png"
              />
            </div>
          </div>

          {/* Logo Preview */}
          {settings.app_logo_url && (
            <div className="mt-4">
              <label className="block text-gray-700 mb-2">معاينة اللوجو</label>
              <img 
                src={settings.app_logo_url} 
                alt="Logo Preview" 
                className="h-20 object-contain border rounded-lg p-2"
                onError={(e) => e.target.style.display = 'none'}
              />
            </div>
          )}
        </div>

        {/* Colors */}
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h2 className="text-lg font-semibold text-gray-800 mb-4">الألوان</h2>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-gray-700 mb-2">اللون الأساسي</label>
              <div className="flex gap-3">
                <input
                  type="color"
                  value={settings.primary_color}
                  onChange={(e) => setSettings({...settings, primary_color: e.target.value})}
                  className="w-12 h-12 rounded-lg cursor-pointer"
                />
                <input
                  type="text"
                  value={settings.primary_color}
                  onChange={(e) => setSettings({...settings, primary_color: e.target.value})}
                  className="flex-1 px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary outline-none"
                />
              </div>
            </div>

            <div>
              <label className="block text-gray-700 mb-2">اللون الثانوي</label>
              <div className="flex gap-3">
                <input
                  type="color"
                  value={settings.secondary_color}
                  onChange={(e) => setSettings({...settings, secondary_color: e.target.value})}
                  className="w-12 h-12 rounded-lg cursor-pointer"
                />
                <input
                  type="text"
                  value={settings.secondary_color}
                  onChange={(e) => setSettings({...settings, secondary_color: e.target.value})}
                  className="flex-1 px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary outline-none"
                />
              </div>
            </div>
          </div>
        </div>

        {/* Support */}
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h2 className="text-lg font-semibold text-gray-800 mb-4">معلومات الدعم</h2>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-gray-700 mb-2">بريد الدعم</label>
              <input
                type="email"
                value={settings.support_email || ''}
                onChange={(e) => setSettings({...settings, support_email: e.target.value})}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary outline-none"
                placeholder="support@example.com"
              />
            </div>

            <div>
              <label className="block text-gray-700 mb-2">هاتف الدعم</label>
              <input
                type="tel"
                value={settings.support_phone || ''}
                onChange={(e) => setSettings({...settings, support_phone: e.target.value})}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary outline-none"
                placeholder="+1234567890"
              />
            </div>

            <div>
              <label className="block text-gray-700 mb-2">تيليغرام الدعم</label>
              <input
                type="text"
                value={settings.support_telegram || ''}
                onChange={(e) => setSettings({...settings, support_telegram: e.target.value})}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary outline-none"
                placeholder="@username أو https://t.me/username"
              />
            </div>

            <div>
              <label className="block text-gray-700 mb-2">رابط الشروط والأحكام</label>
              <input
                type="url"
                value={settings.terms_url || ''}
                onChange={(e) => setSettings({...settings, terms_url: e.target.value})}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary outline-none"
                placeholder="https://example.com/terms"
              />
            </div>

            <div>
              <label className="block text-gray-700 mb-2">رابط سياسة الخصوصية</label>
              <input
                type="url"
                value={settings.privacy_url || ''}
                onChange={(e) => setSettings({...settings, privacy_url: e.target.value})}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary outline-none"
                placeholder="https://example.com/privacy"
              />
            </div>
          </div>
        </div>

        {/* Submit */}
        <button
          type="submit"
          disabled={saving}
          className="flex items-center gap-2 bg-primary text-white px-6 py-3 rounded-lg font-semibold hover:bg-blue-700 transition disabled:opacity-50"
        >
          <Save size={20} />
          {saving ? 'جاري الحفظ...' : 'حفظ الإعدادات'}
        </button>
      </form>
    </div>
  );
}
