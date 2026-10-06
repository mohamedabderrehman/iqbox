import { useState, useEffect } from 'react';
import api from '../api';

export default function Withdrawals() {
  const [requests, setRequests] = useState([]);
  const [stats, setStats] = useState({});
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all');
  const [processingId, setProcessingId] = useState(null);
  const [adminNotes, setAdminNotes] = useState('');
  const [showModal, setShowModal] = useState(null);

  useEffect(() => {
    fetchWithdrawals();
  }, [filter]);

  const fetchWithdrawals = async () => {
    try {
      setLoading(true);
      const params = filter !== 'all' ? { status: filter } : {};
      const response = await api.get('/admin/withdrawals', { params });
      setRequests(response.data.data.requests);
      setStats(response.data.data.stats);
    } catch (error) {
      console.error('Error fetching withdrawals:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (id) => {
    try {
      setProcessingId(id);
      await api.post(`/admin/withdrawals/${id}/approve`, { admin_notes: adminNotes });
      setShowModal(null);
      setAdminNotes('');
      fetchWithdrawals();
    } catch (error) {
      alert('خطأ في الموافقة على الطلب');
    } finally {
      setProcessingId(null);
    }
  };

  const handleReject = async (id) => {
    try {
      setProcessingId(id);
      await api.post(`/admin/withdrawals/${id}/reject`, { admin_notes: adminNotes });
      setShowModal(null);
      setAdminNotes('');
      fetchWithdrawals();
    } catch (error) {
      alert('خطأ في رفض الطلب');
    } finally {
      setProcessingId(null);
    }
  };

  const getMethodName = (method) => {
    const methods = {
      'qi_card': 'كي كارد (Qi Card)',
      'master_card': 'ماستر كارد (Master Card)',
      'usdt_trc20': 'USDT (TRC20)'
    };
    return methods[method] || method;
  };

  const getStatusBadge = (status) => {
    const styles = {
      pending: 'bg-yellow-100 text-yellow-800',
      approved: 'bg-green-100 text-green-800',
      rejected: 'bg-red-100 text-red-800'
    };
    const labels = {
      pending: 'قيد المراجعة',
      approved: 'تمت الموافقة',
      rejected: 'مرفوض'
    };
    return (
      <span className={`px-2 py-1 rounded-full text-xs font-medium ${styles[status]}`}>
        {labels[status]}
      </span>
    );
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">طلبات السحب</h1>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-yellow-50 rounded-lg p-4 border border-yellow-200">
          <p className="text-yellow-600 text-sm">قيد المراجعة</p>
          <p className="text-2xl font-bold text-yellow-700">{stats.pending_count || 0}</p>
          <p className="text-sm text-yellow-600">${parseFloat(stats.pending_amount || 0).toFixed(2)}</p>
        </div>
        <div className="bg-green-50 rounded-lg p-4 border border-green-200">
          <p className="text-green-600 text-sm">تمت الموافقة</p>
          <p className="text-2xl font-bold text-green-700">{stats.approved_count || 0}</p>
        </div>
        <div className="bg-red-50 rounded-lg p-4 border border-red-200">
          <p className="text-red-600 text-sm">مرفوض</p>
          <p className="text-2xl font-bold text-red-700">{stats.rejected_count || 0}</p>
        </div>
        <div className="bg-blue-50 rounded-lg p-4 border border-blue-200">
          <p className="text-blue-600 text-sm">إجمالي المدفوع</p>
          <p className="text-2xl font-bold text-blue-700">${parseFloat(stats.total_paid || 0).toFixed(2)}</p>
        </div>
      </div>

      {/* Filter */}
      <div className="flex gap-2">
        {['all', 'pending', 'approved', 'rejected'].map((f) => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition ${
              filter === f
                ? 'bg-blue-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            {f === 'all' ? 'الكل' : f === 'pending' ? 'قيد المراجعة' : f === 'approved' ? 'موافق عليها' : 'مرفوضة'}
          </button>
        ))}
      </div>

      {/* Table */}
      <div className="bg-white rounded-lg shadow overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-gray-500">جاري التحميل...</div>
        ) : requests.length === 0 ? (
          <div className="p-8 text-center text-gray-500">لا توجد طلبات</div>
        ) : (
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">المستخدم</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">المبلغ</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">طريقة السحب</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">التفاصيل</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">الحالة</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">التاريخ</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">إجراءات</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {requests.map((request) => (
                <tr key={request.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div>
                      <div className="text-sm font-medium text-gray-900">{request.username}</div>
                      <div className="text-sm text-gray-500">{request.email}</div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className="text-lg font-bold text-green-600">
                      ${parseFloat(request.amount).toFixed(2)}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {getMethodName(request.withdrawal_method)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {request.account_details && (
                      <pre className="text-xs bg-gray-100 p-2 rounded">
                        {JSON.stringify(
                          typeof request.account_details === 'string'
                            ? JSON.parse(request.account_details)
                            : request.account_details,
                          null, 2
                        )}
                      </pre>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {getStatusBadge(request.status)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {new Date(request.created_at).toLocaleDateString('ar-IQ')}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    {request.status === 'pending' && (
                      <div className="flex gap-2">
                        <button
                          onClick={() => setShowModal({ type: 'approve', id: request.id })}
                          className="bg-green-600 text-white px-3 py-1 rounded text-xs hover:bg-green-700"
                        >
                          موافقة
                        </button>
                        <button
                          onClick={() => setShowModal({ type: 'reject', id: request.id })}
                          className="bg-red-600 text-white px-3 py-1 rounded text-xs hover:bg-red-700"
                        >
                          رفض
                        </button>
                      </div>
                    )}
                    {request.admin_notes && (
                      <p className="text-xs text-gray-500 mt-1">ملاحظة: {request.admin_notes}</p>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <h3 className="text-lg font-bold mb-4">
              {showModal.type === 'approve' ? 'تأكيد الموافقة' : 'تأكيد الرفض'}
            </h3>
            <textarea
              value={adminNotes}
              onChange={(e) => setAdminNotes(e.target.value)}
              placeholder="ملاحظات (اختياري)"
              className="w-full border rounded-lg p-3 mb-4"
              rows={3}
            />
            <div className="flex gap-3 justify-end">
              <button
                onClick={() => { setShowModal(null); setAdminNotes(''); }}
                className="px-4 py-2 bg-gray-200 rounded-lg hover:bg-gray-300"
              >
                إلغاء
              </button>
              <button
                onClick={() => showModal.type === 'approve' 
                  ? handleApprove(showModal.id) 
                  : handleReject(showModal.id)
                }
                disabled={processingId === showModal.id}
                className={`px-4 py-2 text-white rounded-lg ${
                  showModal.type === 'approve' 
                    ? 'bg-green-600 hover:bg-green-700' 
                    : 'bg-red-600 hover:bg-red-700'
                }`}
              >
                {processingId === showModal.id ? 'جاري...' : 'تأكيد'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
