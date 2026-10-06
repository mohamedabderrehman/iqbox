import React, { useState, useEffect, useCallback } from 'react';
import {
  FileVideo,
  FileImage,
  FileAudio,
  FileText,
  FileArchive,
  File,
  Folder,
  FolderOpen,
  HardDrive,
  CheckCircle,
  Lock,
  Eye,
  Download,
  ExternalLink,
  Copy,
  Trash2,
  AlertTriangle
} from 'lucide-react';
import api from '../api';

export default function FilesManagement() {
  // Tab state
  const [activeTab, setActiveTab] = useState('files');
  
  // Files state
  const [files, setFiles] = useState([]);
  const [filesTotal, setFilesTotal] = useState(0);
  const [filesPage, setFilesPage] = useState(1);
  const [filesPerPage] = useState(20);
  
  // Folders state
  const [folders, setFolders] = useState([]);
  const [foldersTotal, setFoldersTotal] = useState(0);
  const [foldersPage, setFoldersPage] = useState(1);
  const [foldersPerPage] = useState(20);
  
  // Common state
  const [users, setUsers] = useState([]);
  const [selectedUser, setSelectedUser] = useState('');
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState(null);
  const [message, setMessage] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [fileTypeFilter, setFileTypeFilter] = useState('all');
  const [sortBy, setSortBy] = useState('created_at');
  const [sortOrder, setSortOrder] = useState('desc');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  
  // Delete confirmation modal
  const [deleteModal, setDeleteModal] = useState({ show: false, type: null, item: null });

  useEffect(() => {
    fetchUsers();
    fetchStats();
  }, []);

  useEffect(() => {
    if (activeTab === 'files') {
      fetchFiles();
    } else {
      fetchFolders();
    }
  }, [activeTab, selectedUser, fileTypeFilter, sortBy, sortOrder, filesPage, foldersPage]);

  const fetchUsers = async () => {
    try {
      const response = await api.get('/admin/users');
      setUsers(response.data.data.users || []);
    } catch (error) {
      console.error('Error fetching users:', error);
    }
  };

  const fetchStats = async () => {
    try {
      const response = await api.get('/admin/files/stats');
      setStats(response.data.data);
    } catch (error) {
      console.error('Error fetching stats:', error);
    }
  };

  const fetchFiles = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      params.append('page', filesPage);
      params.append('limit', filesPerPage);
      params.append('sort', sortBy);
      params.append('order', sortOrder);
      if (selectedUser) params.append('user_id', selectedUser);
      if (fileTypeFilter !== 'all') params.append('type', fileTypeFilter);
      if (searchTerm) params.append('search', searchTerm);
      if (dateFrom) params.append('date_from', dateFrom);
      if (dateTo) params.append('date_to', dateTo);
      
      const response = await api.get(`/admin/files?${params}`);
      setFiles(response.data.data.files || []);
      setFilesTotal(response.data.data.total || 0);
    } catch (error) {
      console.error('Error fetching files:', error);
      setFiles([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchFolders = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      params.append('page', foldersPage);
      params.append('limit', foldersPerPage);
      params.append('sort', sortBy);
      params.append('order', sortOrder);
      if (selectedUser) params.append('user_id', selectedUser);
      if (searchTerm) params.append('search', searchTerm);
      if (dateFrom) params.append('date_from', dateFrom);
      if (dateTo) params.append('date_to', dateTo);
      
      const response = await api.get(`/admin/folders?${params}`);
      setFolders(response.data.data.folders || []);
      setFoldersTotal(response.data.data.total || 0);
    } catch (error) {
      console.error('Error fetching folders:', error);
      setFolders([]);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    if (activeTab === 'files') {
      setFilesPage(1);
      fetchFiles();
    } else {
      setFoldersPage(1);
      fetchFolders();
    }
  };

  const handleDeleteFile = async () => {
    if (!deleteModal.item) return;
    
    try {
      await api.delete(`/admin/files/${deleteModal.item.id}`);
      setMessage({ type: 'success', text: 'تم حذف الملف بنجاح' });
      setDeleteModal({ show: false, type: null, item: null });
      fetchFiles();
      fetchStats();
      setTimeout(() => setMessage(null), 3000);
    } catch (error) {
      setMessage({ type: 'error', text: 'خطأ في حذف الملف' });
    }
  };

  const handleDeleteFolder = async () => {
    if (!deleteModal.item) return;
    
    try {
      await api.delete(`/admin/folders/${deleteModal.item.id}`);
      setMessage({ type: 'success', text: 'تم حذف المجلد بنجاح' });
      setDeleteModal({ show: false, type: null, item: null });
      fetchFolders();
      fetchStats();
      setTimeout(() => setMessage(null), 3000);
    } catch (error) {
      setMessage({ type: 'error', text: 'خطأ في حذف المجلد' });
    }
  };

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text);
    setMessage({ type: 'success', text: 'تم نسخ الرابط' });
    setTimeout(() => setMessage(null), 2000);
  };

  const openDeleteModal = (type, item) => {
    setDeleteModal({ show: true, type, item });
  };

  const formatFileSize = (bytes) => {
    if (bytes >= 1073741824) return (bytes / 1073741824).toFixed(2) + ' GB';
    if (bytes >= 1048576) return (bytes / 1048576).toFixed(2) + ' MB';
    if (bytes >= 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return bytes + ' bytes';
  };

  const getFileIcon = (fileType) => {
    const icons = {
      video: FileVideo,
      image: FileImage,
      audio: FileAudio,
      pdf: FileText,
      document: FileText,
      archive: FileArchive,
      other: File
    };
    const Icon = icons[fileType] || icons.other;
    return <Icon className="w-5 h-5 text-gray-500" />;
  };

  const getFileTypeColor = (fileType) => {
    const colors = {
      video: 'bg-red-100 text-red-800',
      image: 'bg-green-100 text-green-800',
      audio: 'bg-purple-100 text-purple-800',
      pdf: 'bg-yellow-100 text-yellow-800',
      document: 'bg-blue-100 text-blue-800',
      archive: 'bg-indigo-100 text-indigo-800',
      other: 'bg-gray-100 text-gray-800'
    };
    return colors[fileType] || colors.other;
  };

  const totalFilesPages = Math.ceil(filesTotal / filesPerPage);
  const totalFoldersPages = Math.ceil(foldersTotal / foldersPerPage);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-900">إدارة الملفات والمجلدات</h1>
      </div>

      {/* Toast Message */}
      {message && (
        <div className={`fixed top-4 left-4 right-4 md:left-auto md:right-4 md:w-96 p-4 rounded-lg shadow-lg z-50 ${
          message.type === 'success' ? 'bg-green-500 text-white' : 'bg-red-500 text-white'
        }`}>
          {message.text}
        </div>
      )}

      {/* Stats Cards */}
      {stats && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center">
              <div className="p-3 bg-blue-100 rounded-lg">
                <File className="w-6 h-6 text-blue-600" />
              </div>
              <div className="mr-4">
                <p className="text-sm text-gray-500">إجمالي الملفات</p>
                <p className="text-2xl font-bold text-gray-900">{stats.total_files || 0}</p>
              </div>
            </div>
          </div>
          
          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center">
              <div className="p-3 bg-green-100 rounded-lg">
                <FolderOpen className="w-6 h-6 text-green-600" />
              </div>
              <div className="mr-4">
                <p className="text-sm text-gray-500">إجمالي المجلدات</p>
                <p className="text-2xl font-bold text-gray-900">{stats.total_folders || 0}</p>
              </div>
            </div>
          </div>
          
          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center">
              <div className="p-3 bg-purple-100 rounded-lg">
                <HardDrive className="w-6 h-6 text-purple-600" />
              </div>
              <div className="mr-4">
                <p className="text-sm text-gray-500">إجمالي التخزين</p>
                <p className="text-2xl font-bold text-gray-900">{formatFileSize(stats.total_storage || 0)}</p>
              </div>
            </div>
          </div>
          
          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center">
              <div className="p-3 bg-yellow-100 rounded-lg">
                <Download className="w-6 h-6 text-yellow-600" />
              </div>
              <div className="mr-4">
                <p className="text-sm text-gray-500">إجمالي التحميلات</p>
                <p className="text-2xl font-bold text-gray-900">{stats.total_downloads || 0}</p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Tabs */}
      <div className="bg-white rounded-lg shadow">
        <div className="border-b">
          <nav className="flex">
            <button
              onClick={() => setActiveTab('files')}
              className={`px-6 py-4 text-sm font-medium border-b-2 transition-colors ${
                activeTab === 'files'
                  ? 'border-blue-600 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              <span className="inline-flex items-center gap-2">
                <FileText className="w-4 h-4" />
                الملفات ({filesTotal})
              </span>
            </button>
            <button
              onClick={() => setActiveTab('folders')}
              className={`px-6 py-4 text-sm font-medium border-b-2 transition-colors ${
                activeTab === 'folders'
                  ? 'border-blue-600 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              <span className="inline-flex items-center gap-2">
                <Folder className="w-4 h-4" />
                المجلدات ({activeTab === 'folders' ? foldersTotal : (stats?.total_folders || 0)})
              </span>
            </button>
          </nav>
        </div>

        {/* Filters */}
        <div className="p-4 border-b bg-gray-50">
          <div className="grid grid-cols-1 md:grid-cols-6 gap-4">
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">المستخدم</label>
              <select
                value={selectedUser}
                onChange={(e) => setSelectedUser(e.target.value)}
                className="w-full border rounded-lg p-2 text-sm"
              >
                <option value="">جميع المستخدمين</option>
                {users.map(user => (
                  <option key={user.id} value={user.id}>
                    {user.username}
                  </option>
                ))}
              </select>
            </div>
            
            {activeTab === 'files' && (
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">نوع الملف</label>
                <select
                  value={fileTypeFilter}
                  onChange={(e) => setFileTypeFilter(e.target.value)}
                  className="w-full border rounded-lg p-2 text-sm"
                >
                  <option value="all">جميع الأنواع</option>
                  <option value="video">فيديو</option>
                  <option value="image">صورة</option>
                  <option value="audio">صوت</option>
                  <option value="document">مستند</option>
                  <option value="pdf">PDF</option>
                  <option value="archive">أرشيف</option>
                  <option value="other">أخرى</option>
                </select>
              </div>
            )}
            
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">البحث</label>
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                placeholder="اسم الملف..."
                className="w-full border rounded-lg p-2 text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">من تاريخ</label>
              <input
                type="date"
                value={dateFrom}
                onChange={(e) => setDateFrom(e.target.value)}
                className="w-full border rounded-lg p-2 text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">إلى تاريخ</label>
              <input
                type="date"
                value={dateTo}
                onChange={(e) => setDateTo(e.target.value)}
                className="w-full border rounded-lg p-2 text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">ترتيب حسب</label>
              <div className="flex gap-2">
                <select
                  value={sortBy}
                  onChange={(e) => setSortBy(e.target.value)}
                  className="flex-1 border rounded-lg p-2 text-sm"
                >
                  <option value="created_at">التاريخ</option>
                  <option value="file_size">الحجم</option>
                  <option value="views_count">المشاهدات</option>
                  <option value="downloads_count">التحميلات</option>
                  <option value="name">الاسم</option>
                </select>
                <button
                  onClick={() => setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')}
                  className="px-3 border rounded-lg hover:bg-gray-100"
                  title={sortOrder === 'asc' ? 'تصاعدي' : 'تنازلي'}
                >
                  {sortOrder === 'asc' ? '↑' : '↓'}
                </button>
              </div>
            </div>
          </div>
          
          <div className="mt-4 flex justify-end">
            <button
              onClick={handleSearch}
              className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 text-sm"
            >
              بحث
            </button>
          </div>
        </div>

        {/* Content */}
        {loading ? (
          <div className="p-12 text-center">
            <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-blue-600 border-t-transparent"></div>
            <p className="mt-4 text-gray-500">جاري التحميل...</p>
          </div>
        ) : activeTab === 'files' ? (
          /* Files Table */
          files.length === 0 ? (
            <div className="p-12 text-center">
              <FolderOpen className="w-14 h-14 text-gray-300 mx-auto" />
              <p className="mt-4 text-gray-500">لا توجد ملفات</p>
              <p className="text-sm text-gray-400">جرب تغيير معايير البحث</p>
            </div>
          ) : (
            <>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">الملف</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">النوع</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">المالك</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">المجلد</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">الحجم</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                        <div className="flex items-center justify-center">
                          <Eye className="w-4 h-4" />
                          <span className="sr-only">المشاهدات</span>
                        </div>
                      </th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                        <div className="flex items-center justify-center">
                          <Download className="w-4 h-4" />
                          <span className="sr-only">التحميلات</span>
                        </div>
                      </th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">التاريخ</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">الإجراءات</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {files.map(file => (
                      <tr key={file.id} className="hover:bg-gray-50">
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-2">
                            <span className="flex items-center">{getFileIcon(file.file_type)}</span>
                            <span className="font-medium text-sm truncate max-w-[200px]" title={file.name}>
                              {file.name}
                            </span>
                          </div>
                        </td>
                        <td className="px-4 py-3">
                          <span className={`px-2 py-1 rounded-full text-xs ${getFileTypeColor(file.file_type)}`}>
                            {file.file_type}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <div className="text-sm">
                            <p className="font-medium">{file.owner_name || 'غير معروف'}</p>
                            <p className="text-xs text-gray-400">ID: {file.user_id}</p>
                          </div>
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-500">
                          {file.folder_name ? (
                            <span className="inline-flex items-center gap-1 text-gray-500">
                              <Folder className="w-4 h-4" />
                              {file.folder_name}
                            </span>
                          ) : '—'}
                        </td>
                        <td className="px-4 py-3 text-sm">{formatFileSize(file.file_size)}</td>
                        <td className="px-4 py-3 text-sm text-center">{file.views_count || 0}</td>
                        <td className="px-4 py-3 text-sm text-center">{file.downloads_count || 0}</td>
                        <td className="px-4 py-3 text-sm text-gray-500">
                          {new Date(file.created_at).toLocaleDateString('ar-IQ')}
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex gap-1">
                            {file.share_url && (
                              <>
                                <a
                                  href={file.share_url}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  className="p-1.5 text-blue-600 hover:bg-blue-50 rounded"
                                  title="فتح"
                                >
                                  <ExternalLink className="w-4 h-4" />
                                </a>
                                <button
                                  onClick={() => copyToClipboard(file.share_url)}
                                  className="p-1.5 text-gray-600 hover:bg-gray-100 rounded"
                                  title="نسخ الرابط"
                                >
                                  <Copy className="w-4 h-4" />
                                </button>
                              </>
                            )}
                            <button
                              onClick={() => openDeleteModal('file', file)}
                              className="p-1.5 text-red-600 hover:bg-red-50 rounded"
                              title="حذف"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Files Pagination */}
              {totalFilesPages > 1 && (
                <div className="p-4 border-t flex items-center justify-between">
                  <p className="text-sm text-gray-500">
                    عرض {((filesPage - 1) * filesPerPage) + 1} - {Math.min(filesPage * filesPerPage, filesTotal)} من {filesTotal}
                  </p>
                  <div className="flex gap-2">
                    <button
                      onClick={() => setFilesPage(p => Math.max(1, p - 1))}
                      disabled={filesPage === 1}
                      className="px-3 py-1 border rounded hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      السابق
                    </button>
                    <span className="px-3 py-1 bg-blue-600 text-white rounded">
                      {filesPage} / {totalFilesPages}
                    </span>
                    <button
                      onClick={() => setFilesPage(p => Math.min(totalFilesPages, p + 1))}
                      disabled={filesPage === totalFilesPages}
                      className="px-3 py-1 border rounded hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      التالي
                    </button>
                  </div>
                </div>
              )}
            </>
          )
        ) : (
          /* Folders Table */
          folders.length === 0 ? (
            <div className="p-12 text-center">
              <FolderOpen className="w-14 h-14 text-gray-300 mx-auto" />
              <p className="mt-4 text-gray-500">لا توجد مجلدات</p>
              <p className="text-sm text-gray-400">جرب تغيير معايير البحث</p>
            </div>
          ) : (
            <>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">المجلد</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">المالك</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">المسار</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">الملفات</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">الحجم</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">مشارك</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">التاريخ</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">الإجراءات</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {folders.map(folder => (
                      <tr key={folder.id} className="hover:bg-gray-50">
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-2">
                            <span className="flex items-center">
                              <Folder className="w-5 h-5 text-gray-500" />
                            </span>
                            <span className="font-medium">{folder.name}</span>
                          </div>
                        </td>
                        <td className="px-4 py-3">
                          <div className="text-sm">
                            <p className="font-medium">{folder.owner_name || 'غير معروف'}</p>
                            <p className="text-xs text-gray-400">ID: {folder.user_id}</p>
                          </div>
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-500">
                          {folder.parent_name ? (
                            <span className="inline-flex items-center gap-1 text-gray-500">
                              <Folder className="w-4 h-4" />
                              {folder.parent_name}
                            </span>
                          ) : '/ (الجذر)'}
                        </td>
                        <td className="px-4 py-3 text-sm text-center">{folder.files_count || 0}</td>
                        <td className="px-4 py-3 text-sm">{formatFileSize(folder.size || 0)}</td>
                        <td className="px-4 py-3">
                          {folder.is_shared ? (
                            <span className="inline-flex items-center gap-1 px-2 py-1 bg-green-100 text-green-800 rounded-full text-xs">
                              <CheckCircle className="w-3 h-3" />
                              مشارك
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 px-2 py-1 bg-gray-100 text-gray-600 rounded-full text-xs">
                              <Lock className="w-3 h-3" />
                              خاص
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-500">
                          {new Date(folder.created_at).toLocaleDateString('ar-IQ')}
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex gap-1">
                            {folder.share_url && (
                              <>
                                <a
                                  href={folder.share_url}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  className="p-1.5 text-blue-600 hover:bg-blue-50 rounded"
                                  title="فتح"
                                >
                                  <ExternalLink className="w-4 h-4" />
                                </a>
                                <button
                                  onClick={() => copyToClipboard(folder.share_url)}
                                  className="p-1.5 text-gray-600 hover:bg-gray-100 rounded"
                                  title="نسخ الرابط"
                                >
                                  <Copy className="w-4 h-4" />
                                </button>
                              </>
                            )}
                            <a
                              href={`/api/folders/download/${folder.share_token}`}
                              className="p-1.5 text-green-600 hover:bg-green-50 rounded"
                              title="تحميل ZIP"
                            >
                              <Download className="w-4 h-4" />
                            </a>
                            <button
                              onClick={() => openDeleteModal('folder', folder)}
                              className="p-1.5 text-red-600 hover:bg-red-50 rounded"
                              title="حذف"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Folders Pagination */}
              {totalFoldersPages > 1 && (
                <div className="p-4 border-t flex items-center justify-between">
                  <p className="text-sm text-gray-500">
                    عرض {((foldersPage - 1) * foldersPerPage) + 1} - {Math.min(foldersPage * foldersPerPage, foldersTotal)} من {foldersTotal}
                  </p>
                  <div className="flex gap-2">
                    <button
                      onClick={() => setFoldersPage(p => Math.max(1, p - 1))}
                      disabled={foldersPage === 1}
                      className="px-3 py-1 border rounded hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      السابق
                    </button>
                    <span className="px-3 py-1 bg-blue-600 text-white rounded">
                      {foldersPage} / {totalFoldersPages}
                    </span>
                    <button
                      onClick={() => setFoldersPage(p => Math.min(totalFoldersPages, p + 1))}
                      disabled={foldersPage === totalFoldersPages}
                      className="px-3 py-1 border rounded hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      التالي
                    </button>
                  </div>
                </div>
              )}
            </>
          )
        )}
      </div>

      {/* Delete Confirmation Modal */}
      {deleteModal.show && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
            <div className="text-center">
              <AlertTriangle className="w-12 h-12 text-yellow-500 mx-auto" />
              <h3 className="mt-4 text-lg font-bold text-gray-900">
                تأكيد الحذف
              </h3>
              <p className="mt-2 text-gray-600">
                {deleteModal.type === 'file' 
                  ? `هل أنت متأكد من حذف الملف "${deleteModal.item?.name}"؟`
                  : `هل أنت متأكد من حذف المجلد "${deleteModal.item?.name}" وجميع محتوياته؟`
                }
              </p>
              <p className="mt-1 text-sm text-red-500">
                هذا الإجراء لا يمكن التراجع عنه
              </p>
            </div>
            <div className="mt-6 flex gap-3 justify-center">
              <button
                onClick={() => setDeleteModal({ show: false, type: null, item: null })}
                className="px-6 py-2 border rounded-lg hover:bg-gray-100"
              >
                إلغاء
              </button>
              <button
                onClick={deleteModal.type === 'file' ? handleDeleteFile : handleDeleteFolder}
                className="px-6 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700"
              >
                حذف
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
