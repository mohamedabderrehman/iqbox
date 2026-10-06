import { useState, useEffect } from 'react';
import { getPaymentRequests, approvePayment, rejectPayment } from '../api';
import { Check, X, Clock, CheckCircle, XCircle } from 'lucide-react';

export default function Payments() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all');

  useEffect(() => {
    loadRequests();
  }, [filter]);

  const loadRequests = async () => {
    try {
      const response = await getPaymentRequests(filter);
      if (response.data.success) {
        setRequests(response.data.data.requests);
      }
    } catch (err) {
      console.error('Error loading requests:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (id) => {
    if (!confirm('هل أنت متأكد من الموافقة على هذا الطلب؟')) return;
    try {
      await approvePayment(id);
      loadRequests();
    } catch (err) {
      alert('فشل الموافقة على الطلب');
    }
  };

  const handleReject = async (id) => {
    const reason = prompt('سبب الرفض (اختياري):');
    try {
      await rejectPayment(id, reason);
      loadRequests();
    } catch (err) {
      alert('فشل رفض الطلب');
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'pending':
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 bg-yellow-100 text-yellow-700 rounded-full text-sm">
            <Clock size={14} />
            قيد المراجعة
          </span>
        );
      case 'approved':
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 bg-green-100 text-green-700 rounded-full text-sm">
            <CheckCircle size={14} />
            تمت الموافقة
          </span>
        );
      case 'rejected':
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 bg-red-100 text-red-700 rounded-full text-sm">
            <XCircle size={14} />
            مرفوض
          </span>
        );
      default:
        return status;
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
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-800">طلبات الدفع</h1>
        <div className="flex gap-2">
          {['all', 'pending', 'approved', 'rejected'].map((f) => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              className={`px-4 py-2 rounded-lg transition ${
                filter === f 
                  ? 'bg-primary text-white' 
                  : 'bg-white text-gray-600 hover:bg-gray-50'
              }`}
            >
              {f === 'all' ? 'الكل' : f === 'pending' ? 'معلق' : f === 'approved' ? 'موافق' : 'مرفوض'}
            </button>
          ))}
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm overflow-hidden">
        <table className="w-full">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-4 text-right text-sm font-semibold text-gray-600">المستخدم</th>
              <th className="px-6 py-4 text-right text-sm font-semibold text-gray-600">الخطة</th>
              <th className="px-6 py-4 text-right text-sm font-semibold text-gray-600">المبلغ</th>
              <th className="px-6 py-4 text-right text-sm font-semibold text-gray-600">الحالة</th>
              <th className="px-6 py-4 text-right text-sm font-semibold text-gray-600">التاريخ</th>
              <th className="px-6 py-4 text-right text-sm font-semibold text-gray-600">إجراءات</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {requests.map((request) => (
              <tr key={request.id} className="hover:bg-gray-50">
                <td className="px-6 py-4">
                  <div>
                    <span className="font-medium text-gray-800">{request.username}</span>
                    <p className="text-sm text-gray-500">{request.email}</p>
                  </div>
                </td>
                <td className="px-6 py-4 text-gray-600">
                  {request.plan_name_ar || request.plan_name || '-'}
                </td>
                <td className="px-6 py-4">
                  <span className="font-semibold text-gray-800">
                    {request.amount} {request.currency}
                  </span>
                </td>
                <td className="px-6 py-4">{getStatusBadge(request.status)}</td>
                <td className="px-6 py-4 text-gray-600">
                  {new Date(request.created_at).toLocaleDateString('ar')}
                </td>
                <td className="px-6 py-4">
                  {request.status === 'pending' && (
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => handleApprove(request.id)}
                        className="p-2 text-green-600 hover:bg-green-50 rounded-lg"
                        title="موافقة"
                      >
                        <Check size={18} />
                      </button>
                      <button
                        onClick={() => handleReject(request.id)}
                        className="p-2 text-red-600 hover:bg-red-50 rounded-lg"
                        title="رفض"
                      >
                        <X size={18} />
                      </button>
                    </div>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {requests.length === 0 && (
          <div className="text-center py-12 text-gray-500">
            لا توجد طلبات
          </div>
        )}
      </div>
    </div>
  );
}
