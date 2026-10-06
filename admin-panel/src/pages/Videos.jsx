import { useState, useEffect } from 'react';
import { getVideos, deleteVideo } from '../api';
import { Search, Trash2, Eye, ExternalLink } from 'lucide-react';

export default function Videos() {
  const [videos, setVideos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  useEffect(() => {
    loadVideos();
  }, []);

  const loadVideos = async () => {
    try {
      const response = await getVideos();
      if (response.data.success) {
        setVideos(response.data.data.videos);
      }
    } catch (err) {
      console.error('Error loading videos:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (!confirm('هل أنت متأكد من حذف هذا الفيديو؟')) return;
    try {
      await deleteVideo(id);
      setVideos(videos.filter(v => v.id !== id));
    } catch (err) {
      alert('فشل حذف الفيديو');
    }
  };

  const filteredVideos = videos.filter(video => 
    video.title?.toLowerCase().includes(search.toLowerCase())
  );

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
        <h1 className="text-2xl font-bold text-gray-800">الفيديوهات</h1>
        <div className="relative">
          <Search className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
          <input
            type="text"
            placeholder="بحث..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pr-10 pl-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
          />
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm overflow-hidden">
        <table className="w-full">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-4 text-right text-sm font-semibold text-gray-600">العنوان</th>
              <th className="px-6 py-4 text-right text-sm font-semibold text-gray-600">المالك</th>
              <th className="px-6 py-4 text-right text-sm font-semibold text-gray-600">المشاهدات</th>
              <th className="px-6 py-4 text-right text-sm font-semibold text-gray-600">الحجم</th>
              <th className="px-6 py-4 text-right text-sm font-semibold text-gray-600">التاريخ</th>
              <th className="px-6 py-4 text-right text-sm font-semibold text-gray-600">إجراءات</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {filteredVideos.map((video) => (
              <tr key={video.id} className="hover:bg-gray-50">
                <td className="px-6 py-4">
                  <span className="font-medium text-gray-800">{video.title}</span>
                </td>
                <td className="px-6 py-4 text-gray-600">{video.username || 'غير معروف'}</td>
                <td className="px-6 py-4 text-gray-600">{video.views_count || 0}</td>
                <td className="px-6 py-4 text-gray-600">
                  {video.file_size ? `${(video.file_size / 1024 / 1024).toFixed(2)} MB` : '-'}
                </td>
                <td className="px-6 py-4 text-gray-600">
                  {new Date(video.created_at).toLocaleDateString('ar')}
                </td>
                <td className="px-6 py-4">
                  <div className="flex items-center gap-2">
                    {video.video_url && (
                      <a 
                        href={video.video_url} 
                        target="_blank" 
                        rel="noopener noreferrer"
                        className="p-2 text-green-600 hover:bg-green-50 rounded-lg"
                      >
                        <ExternalLink size={18} />
                      </a>
                    )}
                    <button 
                      onClick={() => handleDelete(video.id)}
                      className="p-2 text-red-600 hover:bg-red-50 rounded-lg"
                    >
                      <Trash2 size={18} />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {filteredVideos.length === 0 && (
          <div className="text-center py-12 text-gray-500">
            لا يوجد فيديوهات
          </div>
        )}
      </div>
    </div>
  );
}
