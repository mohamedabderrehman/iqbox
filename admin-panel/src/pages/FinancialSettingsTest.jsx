import { useState, useEffect } from 'react';

export default function FinancialSettings() {
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(false);
  }, []);

  if (loading) {
    return <div className="p-8 text-center text-gray-500">جاري التحميل...</div>;
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">الإعدادات المالية</h1>
      
      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-lg font-semibold mb-4">اختبار الصفحة</h2>
        <p className="text-gray-600">هذه صفحة اختبار بسيطة للتأكد من أن المسار يعمل.</p>
      </div>
    </div>
  );
}
