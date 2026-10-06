const express = require('express');
const router = express.Router();
const pool = require('../config/database');
const authenticateToken = require('../middleware/auth');
const crypto = require('crypto');
const archiver = require('archiver');
const fs = require('fs');
const path = require('path');

// ========================================
// دوال مساعدة
// ========================================

function generateShareToken() {
  return crypto.randomBytes(16).toString('hex');
}

// جلب مسار المجلد (Breadcrumb)
async function getFolderPath(folderId) {
  const path = [];
  let currentId = folderId;
  
  while (currentId) {
    const result = await pool.query(
      'SELECT id, name, parent_id FROM folders WHERE id = $1',
      [currentId]
    );
    
    if (result.rows.length === 0) break;
    
    const folder = result.rows[0];
    path.unshift({ id: folder.id, name: folder.name });
    currentId = folder.parent_id;
  }
  
  return path;
}

// حساب حجم المجلد
async function getFolderSize(folderId) {
  const result = await pool.query(`
    WITH RECURSIVE folder_tree AS (
      SELECT id FROM folders WHERE id = $1
      UNION ALL
      SELECT f.id FROM folders f
      INNER JOIN folder_tree ft ON f.parent_id = ft.id
    )
    SELECT COALESCE(SUM(file_size), 0) as total_size
    FROM files
    WHERE folder_id IN (SELECT id FROM folder_tree) AND status = 'active'
  `, [folderId]);
  
  return parseInt(result.rows[0].total_size) || 0;
}

// عدد الملفات في المجلد
async function getFolderFilesCount(folderId) {
  const result = await pool.query(`
    WITH RECURSIVE folder_tree AS (
      SELECT id FROM folders WHERE id = $1
      UNION ALL
      SELECT f.id FROM folders f
      INNER JOIN folder_tree ft ON f.parent_id = ft.id
    )
    SELECT COUNT(*) as files_count
    FROM files
    WHERE folder_id IN (SELECT id FROM folder_tree) AND status = 'active'
  `, [folderId]);
  
  return parseInt(result.rows[0].files_count) || 0;
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

// ========================================
// API ENDPOINTS
// ========================================

// إنشاء مجلد جديد
router.post('/', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const { name, parent_id } = req.body;

    if (!name || name.trim() === '') {
      return res.status(400).json({
        success: false,
        message: 'اسم المجلد مطلوب'
      });
    }

    // التحقق من المجلد الأب إذا تم تحديده
    if (parent_id) {
      const parentResult = await pool.query(
        'SELECT id FROM folders WHERE id = $1 AND user_id = $2',
        [parent_id, userId]
      );
      if (parentResult.rows.length === 0) {
        return res.status(404).json({
          success: false,
          message: 'المجلد الأب غير موجود'
        });
      }
    }

    // التحقق من عدم وجود مجلد بنفس الاسم في نفس المستوى
    const existingResult = await pool.query(
      'SELECT id FROM folders WHERE user_id = $1 AND name = $2 AND (parent_id = $3 OR (parent_id IS NULL AND $3 IS NULL))',
      [userId, name.trim(), parent_id || null]
    );

    if (existingResult.rows.length > 0) {
      return res.status(400).json({
        success: false,
        message: 'يوجد مجلد بنفس الاسم'
      });
    }

    const shareToken = generateShareToken();
    const baseUrl = process.env.BASE_URL || 'http://127.0.0.1:8080';

    const result = await pool.query(
      `INSERT INTO folders (user_id, parent_id, name, share_token, is_shared)
       VALUES ($1, $2, $3, $4, true)
       RETURNING id, name, parent_id, share_token, is_shared, created_at`,
      [userId, parent_id || null, name.trim(), shareToken]
    );

    const folder = result.rows[0];

    res.status(201).json({
      success: true,
      message: 'تم إنشاء المجلد بنجاح',
      data: {
        folder: {
          id: folder.id,
          name: folder.name,
          parent_id: folder.parent_id,
          share_url: `${baseUrl}/folder/${folder.share_token}`,
          is_shared: folder.is_shared,
          created_at: folder.created_at,
          files_count: 0,
          size: 0
        }
      }
    });

  } catch (error) {
    console.error('Create folder error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في إنشاء المجلد'
    });
  }
});

// جلب مجلدات المستخدم
router.get('/', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const { parent_id } = req.query;

    let query = `
      SELECT id, name, parent_id, share_token, is_shared, created_at
      FROM folders 
      WHERE user_id = $1
    `;
    const params = [userId];

    if (parent_id === 'null' || parent_id === 'root' || !parent_id) {
      query += ` AND parent_id IS NULL`;
    } else {
      query += ` AND parent_id = $2`;
      params.push(parent_id);
    }

    query += ` ORDER BY name ASC`;

    const result = await pool.query(query, params);
    const baseUrl = process.env.BASE_URL || 'http://127.0.0.1:8080';

    // إضافة معلومات إضافية لكل مجلد
    const foldersWithInfo = await Promise.all(result.rows.map(async (folder) => {
      const size = await getFolderSize(folder.id);
      const filesCount = await getFolderFilesCount(folder.id);
      return {
        ...folder,
        share_url: `${baseUrl}/folder/${folder.share_token}`,
        size,
        files_count: filesCount
      };
    }));

    res.json({
      success: true,
      data: {
        folders: foldersWithInfo,
        count: foldersWithInfo.length
      }
    });

  } catch (error) {
    console.error('Get folders error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في جلب المجلدات'
    });
  }
});

// جلب محتويات مجلد
router.get('/:id/contents', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const folderId = req.params.id;

    // التحقق من ملكية المجلد
    const folderResult = await pool.query(
      'SELECT id, name, parent_id, share_token FROM folders WHERE id = $1 AND user_id = $2',
      [folderId, userId]
    );

    if (folderResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'المجلد غير موجود'
      });
    }

    const folder = folderResult.rows[0];
    const baseUrl = process.env.BASE_URL || 'http://127.0.0.1:8080';

    // جلب المجلدات الفرعية
    const subFoldersResult = await pool.query(
      'SELECT id, name, parent_id, share_token, is_shared, created_at FROM folders WHERE parent_id = $1 ORDER BY name ASC',
      [folderId]
    );

    // جلب الملفات
    const filesResult = await pool.query(
      `SELECT id, name, original_name, file_type, file_size, share_token, share_url, views_count, downloads_count, created_at
       FROM files WHERE folder_id = $1 AND status = 'active' ORDER BY created_at DESC`,
      [folderId]
    );

    // جلب مسار المجلد
    const folderPath = await getFolderPath(folderId);

    res.json({
      success: true,
      data: {
        folder: {
          id: folder.id,
          name: folder.name,
          parent_id: folder.parent_id,
          share_url: `${baseUrl}/folder/${folder.share_token}`,
          path: folderPath
        },
        folders: subFoldersResult.rows.map(f => ({
          ...f,
          share_url: `${baseUrl}/folder/${f.share_token}`
        })),
        files: filesResult.rows,
        folders_count: subFoldersResult.rows.length,
        files_count: filesResult.rows.length
      }
    });

  } catch (error) {
    console.error('Get folder contents error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في جلب محتويات المجلد'
    });
  }
});

// إعادة تسمية مجلد
router.put('/:id', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const folderId = req.params.id;
    const { name } = req.body;

    if (!name || name.trim() === '') {
      return res.status(400).json({
        success: false,
        message: 'اسم المجلد مطلوب'
      });
    }

    const result = await pool.query(
      'UPDATE folders SET name = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2 AND user_id = $3 RETURNING id, name',
      [name.trim(), folderId, userId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'المجلد غير موجود'
      });
    }

    res.json({
      success: true,
      message: 'تم تحديث المجلد بنجاح',
      data: { folder: result.rows[0] }
    });

  } catch (error) {
    console.error('Update folder error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في تحديث المجلد'
    });
  }
});

// حذف مجلد
router.delete('/:id', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const folderId = req.params.id;

    // جلب جميع الملفات في المجلد وفروعه
    const filesResult = await pool.query(`
      WITH RECURSIVE folder_tree AS (
        SELECT id FROM folders WHERE id = $1 AND user_id = $2
        UNION ALL
        SELECT f.id FROM folders f
        INNER JOIN folder_tree ft ON f.parent_id = ft.id
      )
      SELECT file_path, file_size FROM files
      WHERE folder_id IN (SELECT id FROM folder_tree)
    `, [folderId, userId]);

    // حذف الملفات من السيرفر
    let totalSize = 0;
    for (const file of filesResult.rows) {
      totalSize += parseInt(file.file_size) || 0;
      if (fs.existsSync(file.file_path)) {
        fs.unlinkSync(file.file_path);
      }
    }

    // حذف المجلد (سيحذف الملفات والمجلدات الفرعية تلقائيًا بسبب CASCADE)
    const deleteResult = await pool.query(
      'DELETE FROM folders WHERE id = $1 AND user_id = $2 RETURNING id',
      [folderId, userId]
    );

    if (deleteResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'المجلد غير موجود'
      });
    }

    // تحديث سعة التخزين
    if (totalSize > 0) {
      await pool.query(
        'UPDATE users SET storage_used = storage_used - $1 WHERE id = $2',
        [totalSize, userId]
      );
    }

    res.json({
      success: true,
      message: 'تم حذف المجلد بنجاح'
    });

  } catch (error) {
    console.error('Delete folder error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في حذف المجلد'
    });
  }
});

// جلب معلومات مجلد (للصفحة العامة)
router.get('/public/:shareToken', async (req, res) => {
  try {
    const { shareToken } = req.params;

    const result = await pool.query(`
      SELECT f.id, f.name, f.user_id, u.username as owner_name, f.created_at
      FROM folders f
      JOIN users u ON f.user_id = u.id
      WHERE f.share_token = $1
    `, [shareToken]);

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'المجلد غير موجود'
      });
    }

    const folder = result.rows[0];

    // جلب الملفات في المجلد
    const filesResult = await pool.query(`
      SELECT id, name, original_name, file_type, file_size, share_token, views_count, downloads_count, created_at
      FROM files WHERE folder_id = $1 AND status = 'active' ORDER BY name ASC
    `, [folder.id]);

    // جلب المجلدات الفرعية
    const subFoldersResult = await pool.query(
      'SELECT id, name, share_token, created_at FROM folders WHERE parent_id = $1 ORDER BY name ASC',
      [folder.id]
    );

    const size = await getFolderSize(folder.id);
    const baseUrl = process.env.BASE_URL || 'http://127.0.0.1:8080';

    res.json({
      success: true,
      data: {
        folder: {
          id: folder.id,
          name: folder.name,
          owner_name: folder.owner_name,
          created_at: folder.created_at,
          size,
          files_count: filesResult.rows.length,
          folders_count: subFoldersResult.rows.length
        },
        files: filesResult.rows.map(f => ({
          ...f,
          share_url: `${baseUrl}/file/${f.share_token}`
        })),
        folders: subFoldersResult.rows.map(f => ({
          ...f,
          share_url: `${baseUrl}/folder/${f.share_token}`
        }))
      }
    });

  } catch (error) {
    console.error('Get public folder error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في جلب معلومات المجلد'
    });
  }
});

// تنزيل مجلد كـ ZIP
router.get('/download/:shareToken', async (req, res) => {
  try {
    const { shareToken } = req.params;
    const { session_token: sessionToken } = req.query;

    const folderResult = await pool.query(
      'SELECT id, name, user_id FROM folders WHERE share_token = $1',
      [shareToken]
    );

    if (folderResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'المجلد غير موجود'
      });
    }

    const folder = folderResult.rows[0];

    const gateAdEnabled = await getSystemSetting('gate_ad_enabled');
    if (gateAdEnabled) {
      if (!sessionToken) {
        return res.status(403).json({
          success: false,
          message: 'يجب إكمال بوابة الإعلان قبل التحميل'
        });
      }

      const sessionResult = await pool.query(
        'SELECT id FROM file_views WHERE session_token = $1 AND folder_id = $2 AND ad_completed = true',
        [sessionToken, folder.id]
      );

      if (sessionResult.rows.length === 0) {
        return res.status(403).json({
          success: false,
          message: 'جلسة بوابة الإعلان غير صالحة'
        });
      }
    }

    // جلب جميع الملفات في المجلد
    const filesResult = await pool.query(`
      SELECT file_path, original_name, file_size FROM files 
      WHERE folder_id = $1 AND status = 'active'
    `, [folder.id]);

    if (filesResult.rows.length === 0) {
      return res.status(400).json({
        success: false,
        message: 'المجلد فارغ'
      });
    }

    const totalDownloadSize = filesResult.rows.reduce((sum, file) => sum + (parseInt(file.file_size) || 0), 0);

    await pool.query(
      'UPDATE folders SET downloads_count = downloads_count + 1 WHERE id = $1',
      [folder.id]
    );

    if (folder.user_id && totalDownloadSize > 0) {
      await pool.query(
        'UPDATE users SET total_downloaded = total_downloaded + $1 WHERE id = $2',
        [totalDownloadSize, folder.user_id]
      );
    }

    res.setHeader('Content-Type', 'application/zip');
    res.setHeader('Content-Disposition', `attachment; filename="${encodeURIComponent(folder.name)}.zip"`);

    const archive = archiver('zip', { zlib: { level: 5 } });
    archive.pipe(res);

    for (const file of filesResult.rows) {
      if (fs.existsSync(file.file_path)) {
        archive.file(file.file_path, { name: file.original_name });
      }
    }

    await archive.finalize();

  } catch (error) {
    console.error('Download folder error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في تنزيل المجلد'
    });
  }
});

module.exports = router;
