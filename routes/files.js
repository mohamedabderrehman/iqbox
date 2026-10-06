const express = require('express');
const router = express.Router();
const pool = require('../config/database');
const authenticateToken = require('../middleware/auth');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');

// ========================================
// إعداد رفع الملفات
// ========================================
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadDir = path.join(__dirname, '..', 'uploads', 'files');
    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    const ext = path.extname(file.originalname);
    cb(null, uniqueSuffix + ext);
  }
});

const upload = multer({
  storage,
  limits: { fileSize: 500 * 1024 * 1024 }, // 500MB max
  fileFilter: (req, file, cb) => {
    const executable = /\.(php[0-9]?|phtml|phar|cgi|pl|py|sh|bat|cmd|exe|dll|html?|svg|js)$/i;
    if (executable.test(file.originalname)) return cb(new Error('Executable content is not accepted'));
    cb(null, true);
  }
});

// ========================================
// دوال مساعدة
// ========================================

// تحديد نوع الملف
function getFileType(mimeType, filename) {
  // Try MIME type first (skip generic types)
  if (mimeType && mimeType !== 'application/octet-stream') {
    if (mimeType.startsWith('image/')) return 'image';
    if (mimeType.startsWith('video/')) return 'video';
    if (mimeType.startsWith('audio/')) return 'audio';
    if (mimeType === 'application/pdf') return 'pdf';
    if (mimeType.includes('document') || mimeType.includes('word') || mimeType.includes('excel') || mimeType.includes('powerpoint')) return 'document';
    if (mimeType.includes('zip') || mimeType.includes('rar') || mimeType.includes('archive')) return 'archive';
  }
  
  // Fallback to extension (always check if MIME didn't match)
  const ext = path.extname(filename).toLowerCase();
  if (['.jpg', '.jpeg', '.png', '.gif', '.webp', '.svg', '.bmp', '.heic', '.heif'].includes(ext)) return 'image';
  if (['.mp4', '.mov', '.avi', '.mkv', '.webm', '.flv', '.wmv', '.3gp'].includes(ext)) return 'video';
  if (['.mp3', '.wav', '.ogg', '.m4a', '.flac', '.aac', '.wma'].includes(ext)) return 'audio';
  if (['.pdf'].includes(ext)) return 'pdf';
  if (['.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx', '.txt', '.rtf', '.csv'].includes(ext)) return 'document';
  if (['.zip', '.rar', '.7z', '.tar', '.gz', '.bz2'].includes(ext)) return 'archive';
  return 'other';
}

// جلب إعداد من النظام
async function getSystemSetting(key) {
  const result = await pool.query(
    'SELECT setting_value, setting_type FROM system_settings WHERE setting_key = $1',
    [key]
  );
  if (result.rows.length === 0) return null;
  
  const { setting_value, setting_type } = result.rows[0];
  switch (setting_type) {
    case 'decimal': return parseFloat(setting_value);
    case 'integer': return parseInt(setting_value);
    case 'boolean': return setting_value === 'true';
    default: return setting_value;
  }
}

// التحقق من سعة التخزين
async function checkStorageQuota(userId, fileSize) {
  const userResult = await pool.query(
    'SELECT storage_used, storage_limit, subscription_status, subscription_expiry FROM users WHERE id = $1',
    [userId]
  );
  
  if (userResult.rows.length === 0) {
    return { allowed: false, message: 'المستخدم غير موجود' };
  }
  
  const user = userResult.rows[0];
  const isPremium = user.subscription_status === 'premium' && 
                   (!user.subscription_expiry || new Date(user.subscription_expiry) > new Date());
  
  // جلب حدود التخزين من الإعدادات
  const freeLimit = await getSystemSetting('free_storage_limit_gb') || 10;
  const premiumLimit = await getSystemSetting('premium_storage_limit_gb') || 200;
  
  const storageLimit = isPremium ? premiumLimit * 1024 * 1024 * 1024 : freeLimit * 1024 * 1024 * 1024;
  const storageUsed = parseInt(user.storage_used) || 0;
  
  if (storageUsed + fileSize > storageLimit) {
    return {
      allowed: false,
      message: isPremium ? 'تجاوزت سعة التخزين القصوى' : 'تجاوزت سعة التخزين المجانية. قم بالترقية إلى Premium',
      storage_used: storageUsed,
      storage_limit: storageLimit,
      is_premium: isPremium
    };
  }
  
  return {
    allowed: true,
    storage_used: storageUsed,
    storage_limit: storageLimit,
    is_premium: isPremium
  };
}

// إنشاء رابط مشاركة
function generateShareToken() {
  return crypto.randomBytes(16).toString('hex');
}

// ========================================
// API ENDPOINTS
// ========================================

// رفع ملف جديد
router.post('/upload', authenticateToken, upload.single('file'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({
        success: false,
        message: 'لم يتم رفع أي ملف'
      });
    }

    const userId = req.user.id;
    const { folder_id, name } = req.body;
    const fileSize = req.file.size;
    
    // التحقق من سعة التخزين
    const quotaCheck = await checkStorageQuota(userId, fileSize);
    if (!quotaCheck.allowed) {
      // حذف الملف المرفوع
      fs.unlinkSync(req.file.path);
      return res.status(400).json({
        success: false,
        message: quotaCheck.message,
        data: {
          storage_used: quotaCheck.storage_used,
          storage_limit: quotaCheck.storage_limit,
          is_premium: quotaCheck.is_premium
        }
      });
    }
    
    // التحقق من المجلد إذا تم تحديده
    if (folder_id) {
      const folderResult = await pool.query(
        'SELECT id FROM folders WHERE id = $1 AND user_id = $2',
        [folder_id, userId]
      );
      if (folderResult.rows.length === 0) {
        fs.unlinkSync(req.file.path);
        return res.status(404).json({
          success: false,
          message: 'المجلد غير موجود'
        });
      }
    }

    const fileType = getFileType(req.file.mimetype, req.file.originalname);
    const shareToken = generateShareToken();
    const baseUrl = process.env.BASE_URL || 'http://127.0.0.1:8080';
    const shareUrl = `${baseUrl}/file/${shareToken}`;

    // حفظ معلومات الملف
    const result = await pool.query(
      `INSERT INTO files (user_id, folder_id, name, original_name, file_type, mime_type, file_path, file_size, share_token, share_url, is_shared)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, true)
       RETURNING id, name, original_name, file_type, file_size, share_token, share_url, views_count, downloads_count, created_at`,
      [
        userId,
        folder_id || null,
        name || req.file.originalname,
        req.file.originalname,
        fileType,
        req.file.mimetype,
        req.file.path,
        fileSize,
        shareToken,
        shareUrl
      ]
    );

    const file = result.rows[0];

    // تحديث سعة التخزين المستخدمة
    await pool.query(
      'UPDATE users SET storage_used = storage_used + $1, total_uploaded = total_uploaded + $1 WHERE id = $2',
      [fileSize, userId]
    );

    // تسجيل النشاط
    await pool.query(
      'INSERT INTO user_activities (user_id, activity_type, activity_data) VALUES ($1, $2, $3)',
      [userId, 'file_upload', JSON.stringify({ file_id: file.id, file_type: fileType, file_size: fileSize })]
    );

    res.status(201).json({
      success: true,
      message: 'تم رفع الملف بنجاح',
      data: {
        file: {
          id: file.id,
          name: file.name,
          original_name: file.original_name,
          file_type: fileType,
          file_size: fileSize,
          share_token: file.share_token,
          share_url: file.share_url,
          views_count: 0,
          downloads_count: 0,
          created_at: file.created_at
        }
      }
    });

  } catch (error) {
    console.error('File upload error:', error);
    if (req.file && fs.existsSync(req.file.path)) {
      fs.unlinkSync(req.file.path);
    }
    res.status(500).json({
      success: false,
      message: 'خطأ في رفع الملف'
    });
  }
});

// جلب ملفات المستخدم
router.get('/my-files', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const { folder_id, type, sort = 'date', order = 'desc', search } = req.query;

    let query = `
      SELECT id, name, original_name, file_type, mime_type, file_size, share_token, share_url, 
             views_count, downloads_count, folder_id, created_at, updated_at
      FROM files 
      WHERE user_id = $1 AND status = 'active'
    `;
    const params = [userId];
    let paramIndex = 2;

    // فلترة حسب المجلد
    if (folder_id === 'null' || folder_id === 'root') {
      query += ` AND folder_id IS NULL`;
    } else if (folder_id) {
      query += ` AND folder_id = $${paramIndex}`;
      params.push(folder_id);
      paramIndex++;
    }

    // فلترة حسب النوع
    if (type && type !== 'all') {
      query += ` AND file_type = $${paramIndex}`;
      params.push(type);
      paramIndex++;
    }

    // البحث
    if (search) {
      query += ` AND (name ILIKE $${paramIndex} OR original_name ILIKE $${paramIndex})`;
      params.push(`%${search}%`);
      paramIndex++;
    }

    // الترتيب
    const sortColumn = sort === 'name' ? 'name' : sort === 'size' ? 'file_size' : 'created_at';
    const sortOrder = order === 'asc' ? 'ASC' : 'DESC';
    query += ` ORDER BY ${sortColumn} ${sortOrder}`;

    const result = await pool.query(query, params);

    // جلب المجلدات أيضًا
    let foldersQuery = `
      SELECT f.id, f.name, f.parent_id, f.share_token, f.is_shared, f.created_at,
             (SELECT COUNT(*) FROM files fi WHERE fi.folder_id = f.id AND fi.status = 'active') as files_count
      FROM folders f
      WHERE f.user_id = $1
    `;
    const folderParams = [userId];

    if (folder_id === 'null' || folder_id === 'root') {
      foldersQuery += ` AND f.parent_id IS NULL`;
    } else if (folder_id) {
      foldersQuery += ` AND f.parent_id = $2`;
      folderParams.push(folder_id);
    } else {
      foldersQuery += ` AND f.parent_id IS NULL`;
    }

    foldersQuery += ` ORDER BY f.name ASC`;

    const foldersResult = await pool.query(foldersQuery, folderParams);

    // Auto-correct file_type for files stored as 'other'
    const files = result.rows.map(f => {
      if (f.file_type === 'other') {
        const corrected = getFileType(f.mime_type, f.original_name || f.name);
        if (corrected !== 'other') {
          f.file_type = corrected;
          // Fire-and-forget DB update
          pool.query('UPDATE files SET file_type = $1 WHERE id = $2', [corrected, f.id]).catch(() => {});
        }
      }
      return f;
    });

    const baseUrl = process.env.BASE_URL || 'http://127.0.0.1:8080';
    const foldersWithUrls = foldersResult.rows.map(f => ({
      ...f,
      files_count: parseInt(f.files_count) || 0,
      share_url: f.share_token ? `${baseUrl}/folder/${f.share_token}` : null
    }));

    res.json({
      success: true,
      data: {
        files: files,
        folders: foldersWithUrls,
        count: files.length,
        folders_count: foldersWithUrls.length
      }
    });

  } catch (error) {
    console.error('Get files error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في جلب الملفات'
    });
  }
});

// جلب معلومات التخزين
router.get('/storage', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;

    const userResult = await pool.query(
      'SELECT storage_used, storage_limit, subscription_status, subscription_expiry, total_uploaded, total_downloaded FROM users WHERE id = $1',
      [userId]
    );

    if (userResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'المستخدم غير موجود'
      });
    }

    const user = userResult.rows[0];
    const isPremium = user.subscription_status === 'premium' && 
                     (!user.subscription_expiry || new Date(user.subscription_expiry) > new Date());

    const freeLimit = await getSystemSetting('free_storage_limit_gb') || 10;
    const premiumLimit = await getSystemSetting('premium_storage_limit_gb') || 200;
    const storageLimit = isPremium ? premiumLimit * 1024 * 1024 * 1024 : freeLimit * 1024 * 1024 * 1024;

    // إحصائيات الملفات
    const statsResult = await pool.query(`
      SELECT 
        COUNT(*) as total_files,
        COUNT(*) FILTER (WHERE file_type = 'video') as videos_count,
        COUNT(*) FILTER (WHERE file_type = 'image') as images_count,
        COUNT(*) FILTER (WHERE file_type = 'document' OR file_type = 'pdf') as documents_count,
        COUNT(*) FILTER (WHERE file_type NOT IN ('video', 'image', 'document', 'pdf')) as other_count,
        COALESCE(SUM(file_size), 0) as total_size
      FROM files WHERE user_id = $1 AND status = 'active'
    `, [userId]);

    const stats = statsResult.rows[0];

    res.json({
      success: true,
      data: {
        storage: {
          used: parseInt(user.storage_used) || 0,
          limit: storageLimit,
          percentage: Math.round(((parseInt(user.storage_used) || 0) / storageLimit) * 100),
          is_premium: isPremium,
          total_uploaded: parseInt(user.total_uploaded) || 0,
          total_downloaded: parseInt(user.total_downloaded) || 0
        },
        stats: {
          total_files: parseInt(stats.total_files),
          videos_count: parseInt(stats.videos_count),
          images_count: parseInt(stats.images_count),
          documents_count: parseInt(stats.documents_count),
          other_count: parseInt(stats.other_count)
        }
      }
    });

  } catch (error) {
    console.error('Get storage error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في جلب معلومات التخزين'
    });
  }
});

// حذف ملف
router.delete('/:id', authenticateToken, async (req, res) => {
  try {
    const fileId = req.params.id;
    const userId = req.user.id;

    const result = await pool.query(
      'SELECT file_path, file_size FROM files WHERE id = $1 AND user_id = $2',
      [fileId, userId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'الملف غير موجود'
      });
    }

    const file = result.rows[0];

    // حذف الملف من السيرفر
    if (fs.existsSync(file.file_path)) {
      fs.unlinkSync(file.file_path);
    }

    // حذف من قاعدة البيانات
    await pool.query('DELETE FROM files WHERE id = $1', [fileId]);

    // تحديث سعة التخزين
    await pool.query(
      'UPDATE users SET storage_used = storage_used - $1 WHERE id = $2',
      [file.file_size, userId]
    );

    res.json({
      success: true,
      message: 'تم حذف الملف بنجاح'
    });

  } catch (error) {
    console.error('Delete file error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في حذف الملف'
    });
  }
});

// جلب معلومات ملف (للصفحة العامة)
router.get('/info/:shareToken', async (req, res) => {
  try {
    const { shareToken } = req.params;

    const result = await pool.query(`
      SELECT f.id, f.name, f.original_name, f.file_type, f.file_size, f.mime_type,
             f.views_count, f.downloads_count, f.created_at, f.user_id, u.username as owner_name
      FROM files f
      JOIN users u ON f.user_id = u.id
      WHERE f.share_token = $1 AND f.status = 'active'
    `, [shareToken]);

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'الملف غير موجود'
      });
    }

    const file = result.rows[0];

    // Re-detect file_type if it was stored as 'other' but has a known extension/mime
    let fileType = file.file_type;
    if (fileType === 'other') {
      const correctedType = getFileType(file.mime_type, file.original_name || file.name);
      if (correctedType !== 'other') {
        fileType = correctedType;
        // Also fix it in the database for future requests
        await pool.query('UPDATE files SET file_type = $1 WHERE id = $2', [correctedType, file.id]);
      }
    }

    // Increment view count
    await pool.query('UPDATE files SET views_count = views_count + 1 WHERE id = $1', [file.id]);

    res.json({
      success: true,
      data: {
        file: {
          id: file.id,
          name: file.name,
          original_name: file.original_name,
          file_type: fileType,
          file_size: file.file_size,
          mime_type: file.mime_type || null,
          share_token: shareToken,
          views_count: file.views_count + 1,
          downloads_count: file.downloads_count,
          owner_name: file.owner_name,
          created_at: file.created_at
        }
      }
    });

  } catch (error) {
    console.error('Get file info error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في جلب معلومات الملف'
    });
  }
});

// تنزيل/بث ملف
router.get('/download/:shareToken', async (req, res) => {
  try {
    const { shareToken } = req.params;
    const { session_token: sessionToken } = req.query;

    const result = await pool.query(
      'SELECT id, user_id, file_path, original_name, mime_type, file_size FROM files WHERE share_token = $1 AND status = $2',
      [shareToken, 'active']
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'الملف غير موجود'
      });
    }

    const file = result.rows[0];
    const downloadSize = parseInt(file.file_size) || 0;

    let gateAdEnabled = false;
    try {
      gateAdEnabled = await getSystemSetting('gate_ad_enabled');
    } catch (e) {
      // If setting doesn't exist, allow download
    }
    if (gateAdEnabled && sessionToken) {
      const sessionResult = await pool.query(
        'SELECT id FROM file_views WHERE session_token = $1 AND file_id = $2 AND ad_completed = true',
        [sessionToken, file.id]
      );
      // Session token provided but invalid - still allow download (don't block users)
      if (sessionResult.rows.length === 0) {
        console.log('Invalid gate ad session for download, allowing anyway');
      }
    }

    if (!fs.existsSync(file.file_path)) {
      return res.status(404).json({
        success: false,
        message: 'ملف غير موجود على السيرفر'
      });
    }

    // زيادة عداد التنزيلات
    await pool.query(
      'UPDATE files SET downloads_count = downloads_count + 1 WHERE id = $1',
      [file.id]
    );

    if (file.user_id && downloadSize > 0) {
      await pool.query(
        'UPDATE users SET total_downloaded = total_downloaded + $1 WHERE id = $2',
        [downloadSize, file.user_id]
      );
    }

    res.setHeader('Content-Disposition', `attachment; filename="${encodeURIComponent(file.original_name)}"`);
    res.setHeader('Content-Type', file.mime_type || 'application/octet-stream');
    res.setHeader('Content-Length', file.file_size);

    const fileStream = fs.createReadStream(file.file_path);
    fileStream.pipe(res);

  } catch (error) {
    console.error('Download file error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في تنزيل الملف'
    });
  }
});

// بث ملف (للمعاينة)
router.get('/stream/:shareToken', async (req, res) => {
  try {
    const { shareToken } = req.params;

    const result = await pool.query(
      'SELECT file_path, original_name, mime_type, file_size, file_type FROM files WHERE share_token = $1 AND status = $2',
      [shareToken, 'active']
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'الملف غير موجود'
      });
    }

    const file = result.rows[0];

    if (!fs.existsSync(file.file_path)) {
      return res.status(404).json({
        success: false,
        message: 'ملف غير موجود على السيرفر'
      });
    }

    const stat = fs.statSync(file.file_path);
    const fileSize = stat.size;
    const range = req.headers.range;

    // Auto-detect correct mime_type if stored as generic
    let mimeType = file.mime_type;
    if (!mimeType || mimeType === 'application/octet-stream') {
      const ext = path.extname(file.original_name || file.file_path).toLowerCase();
      const mimeMap = {'.mp4':'video/mp4','.mov':'video/quicktime','.avi':'video/x-msvideo','.mkv':'video/x-matroska','.webm':'video/webm','.flv':'video/x-flv',
        '.mp3':'audio/mpeg','.wav':'audio/wav','.ogg':'audio/ogg','.m4a':'audio/mp4','.flac':'audio/flac',
        '.jpg':'image/jpeg','.jpeg':'image/jpeg','.png':'image/png','.gif':'image/gif','.webp':'image/webp','.svg':'image/svg+xml',
        '.pdf':'application/pdf','.json':'application/json','.txt':'text/plain','.html':'text/html','.css':'text/css','.js':'application/javascript'};
      mimeType = mimeMap[ext] || 'application/octet-stream';
    }

    // دعم Range requests للفيديو والصوت
    const isStreamable = file.file_type === 'video' || file.file_type === 'audio' ||
      mimeType.startsWith('video/') || mimeType.startsWith('audio/') ||
      ['.mp4','.mov','.avi','.mkv','.webm','.mp3','.wav','.ogg','.m4a'].includes(path.extname(file.original_name || '').toLowerCase());
    
    // Always send Accept-Ranges header so browser knows it can request ranges
    res.setHeader('Accept-Ranges', 'bytes');
    
    if (range) {
      const parts = range.replace(/bytes=/, "").split("-");
      const start = parseInt(parts[0], 10);
      const end = parts[1] ? parseInt(parts[1], 10) : fileSize - 1;
      const chunksize = (end - start) + 1;
      const fileStream = fs.createReadStream(file.file_path, { start, end });
      const head = {
        'Content-Range': `bytes ${start}-${end}/${fileSize}`,
        'Accept-Ranges': 'bytes',
        'Content-Length': chunksize,
        'Content-Type': mimeType,
      };
      res.writeHead(206, head);
      fileStream.pipe(res);
    } else {
      const head = {
        'Content-Length': fileSize,
        'Content-Type': mimeType,
      };
      res.writeHead(200, head);
      fs.createReadStream(file.file_path).pipe(res);
    }

  } catch (error) {
    console.error('Stream file error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في بث الملف'
    });
  }
});

module.exports = router;
