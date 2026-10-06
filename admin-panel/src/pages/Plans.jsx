import { useState, useEffect } from 'react';
import { getPlans, createPlan, updatePlan, deletePlan } from '../api';
import { Plus, Edit2, Trash2, X } from 'lucide-react';

export default function Plans() {
  const [plans, setPlans] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingPlan, setEditingPlan] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    name_ar: '',
    description: '',
    description_ar: '',
    price: '',
    currency: 'USD',
    duration_days: 30,
    payment_instructions: '',
    payment_instructions_ar: '',
    is_active: true
  });

  useEffect(() => {
    loadPlans();
  }, []);

  const loadPlans = async () => {
    try {
      const response = await getPlans();
      if (response.data.success) {
        setPlans(response.data.data.plans);
      }
    } catch (err) {
      console.error('Error loading plans:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingPlan) {
        await updatePlan(editingPlan.id, formData);
      } else {
        await createPlan(formData);
      }
      loadPlans();
      closeModal();
    } catch (err) {
      alert('حدث خطأ');
    }
  };

  const handleDelete = async (id) => {
    if (!confirm('هل أنت متأكد من حذف هذه الخطة؟')) return;
    try {
      await deletePlan(id);
      setPlans(plans.filter(p => p.id !== id));
    } catch (err) {
      alert('فشل حذف الخطة');
    }
  };

  const openModal = (plan = null) => {
    if (plan) {
      setEditingPlan(plan);
      setFormData({
        name: plan.name || '',
        name_ar: plan.name_ar || '',
        description: plan.description || '',
        description_ar: plan.description_ar || '',
        price: plan.price || '',
        currency: plan.currency || 'USD',
        duration_days: plan.duration_days || 30,
        payment_instructions: plan.payment_instructions || '',
        payment_instructions_ar: plan.payment_instructions_ar || '',
        is_active: plan.is_active !== false
      });
    } else {
      setEditingPlan(null);
      setFormData({
        name: '',
        name_ar: '',
        description: '',
        description_ar: '',
        price: '',
        currency: 'USD',
        duration_days: 30,
        payment_instructions: '',
        payment_instructions_ar: '',
        is_active: true
      });
    }
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setEditingPlan(null);
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
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-800">خطط الاشتراك</h1>
        <button
          onClick={() => openModal()}
          className="flex items-center gap-2 bg-primary text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition"
        >
          <Plus size={20} />
          إضافة خطة
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {plans.map((plan) => (
          <div key={plan.id} className={`bg-white rounded-xl shadow-sm p-6 ${!plan.is_active ? 'opacity-60' : ''}`}>
            <div className="flex items-start justify-between mb-4">
              <div>
                <h3 className="text-lg font-bold text-gray-800">{plan.name_ar || plan.name}</h3>
                <p className="text-sm text-gray-500">{plan.name}</p>
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => openModal(plan)}
                  className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg"
                >
                  <Edit2 size={18} />
                </button>
                <button
                  onClick={() => handleDelete(plan.id)}
                  className="p-2 text-red-600 hover:bg-red-50 rounded-lg"
                >
                  <Trash2 size={18} />
                </button>
              </div>
            </div>

            <div className="text-3xl font-bold text-primary mb-2">
              {plan.price} <span className="text-lg text-gray-500">{plan.currency}</span>
            </div>
            <p className="text-gray-600 mb-4">{plan.duration_days} يوم</p>
            
            {plan.description_ar && (
              <p className="text-gray-500 text-sm">{plan.description_ar}</p>
            )}

            <div className="mt-4 pt-4 border-t">
              <span className={`px-3 py-1 rounded-full text-sm ${plan.is_active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>
                {plan.is_active ? 'نشط' : 'غير نشط'}
              </span>
            </div>
          </div>
        ))}

        {plans.length === 0 && (
          <div className="col-span-full text-center py-12 text-gray-500">
            لا توجد خطط. أضف خطة جديدة.
          </div>
        )}
      </div>

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between p-6 border-b">
              <h2 className="text-xl font-bold">{editingPlan ? 'تعديل الخطة' : 'إضافة خطة جديدة'}</h2>
              <button onClick={closeModal} className="p-2 hover:bg-gray-100 rounded-lg">
                <X size={24} />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-gray-700 mb-2">الاسم (English)</label>
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) => setFormData({...formData, name: e.target.value})}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary outline-none"
                    required
                  />
                </div>
                <div>
                  <label className="block text-gray-700 mb-2">الاسم (عربي)</label>
                  <input
                    type="text"
                    value={formData.name_ar}
                    onChange={(e) => setFormData({...formData, name_ar: e.target.value})}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary outline-none"
                  />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-4">
                <div>
                  <label className="block text-gray-700 mb-2">السعر</label>
                  <input
                    type="number"
                    step="0.01"
                    value={formData.price}
                    onChange={(e) => setFormData({...formData, price: e.target.value})}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary outline-none"
                    required
                  />
                </div>
                <div>
                  <label className="block text-gray-700 mb-2">العملة</label>
                  <select
                    value={formData.currency}
                    onChange={(e) => setFormData({...formData, currency: e.target.value})}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary outline-none"
                  >
                    <option value="USD">USD</option>
                    <option value="EUR">EUR</option>
                    <option value="SAR">SAR</option>
                    <option value="AED">AED</option>
                  </select>
                </div>
                <div>
                  <label className="block text-gray-700 mb-2">المدة (أيام)</label>
                  <input
                    type="number"
                    value={formData.duration_days}
                    onChange={(e) => setFormData({...formData, duration_days: parseInt(e.target.value)})}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary outline-none"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block text-gray-700 mb-2">الوصف (عربي)</label>
                <textarea
                  value={formData.description_ar}
                  onChange={(e) => setFormData({...formData, description_ar: e.target.value})}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary outline-none"
                  rows={2}
                />
              </div>

              <div>
                <label className="block text-gray-700 mb-2">تعليمات الدفع (عربي)</label>
                <textarea
                  value={formData.payment_instructions_ar}
                  onChange={(e) => setFormData({...formData, payment_instructions_ar: e.target.value})}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary outline-none"
                  rows={3}
                  placeholder="مثال: قم بالتحويل إلى الحساب التالي..."
                />
              </div>

              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="is_active"
                  checked={formData.is_active}
                  onChange={(e) => setFormData({...formData, is_active: e.target.checked})}
                  className="w-4 h-4"
                />
                <label htmlFor="is_active" className="text-gray-700">خطة نشطة</label>
              </div>

              <div className="flex gap-4 pt-4">
                <button
                  type="submit"
                  className="flex-1 bg-primary text-white py-3 rounded-lg font-semibold hover:bg-blue-700 transition"
                >
                  {editingPlan ? 'حفظ التغييرات' : 'إضافة الخطة'}
                </button>
                <button
                  type="button"
                  onClick={closeModal}
                  className="px-6 py-3 border border-gray-300 rounded-lg hover:bg-gray-50 transition"
                >
                  إلغاء
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
