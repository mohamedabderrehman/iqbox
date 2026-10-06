const express = require('express');
const router = express.Router();
const pool = require('../config/database');
const authenticateToken = require('../middleware/auth');

// ========================================
// نظام مشاركة الملفات والمجلدات
// إضافة ملف/مجلد مشترك لحساب المستخدم
// ========================================

// إضافة ملف مشترك لحساب المستخدم
router.post('/file/add', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const { share_token } = req.body;

    if (!share_token) {
      return res.status(400).json({
        success: false,
        message: 'رمز المشاركة مطلوب'
      });
    }

    // جلب معلومات الملف
    const fileResult = await pool.query(
      `SELECT f.id, f.user_id, f.name, f.original_name, f.file_type, f.file_size, f.share_token,
              u.username as owner_name
       FROM files f
       JOIN users u ON f.user_id = u.id
       WHERE f.share_token = $1 AND f.status = 'active'`,
      [share_token]
    );

    if (fileResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'الملف غير موجود أو تم حذفه'
      });
    }

    const file = fileResult.rows[0];

    // لا يمكن مشاركة ملفك مع نفسك
    if (file.user_id === userId) {
      return res.status(400).json({
        success: false,
        message: 'لا يمكنك إضافة ملفك الخاص'
      });
    }

    // التحقق من عدم وجود المشاركة مسبقاً
    const existingResult = await pool.query(
      'SELECT id FROM shared_items WHERE user_id = $1 AND file_id = $2',
      [userId, file.id]
    );

    if (existingResult.rows.length > 0) {
      return res.status(400).json({
        success: false,
        message: 'هذا الملف موجود بالفعل في حسابك'
      });
    }

    // إضافة المشاركة
    const result = await pool.query(
      `INSERT INTO shared_items (user_id, file_id, owner_id, item_type, share_token)
       VALUES ($1, $2, $3, 'file', $4)
       RETURNING id, created_at`,
      [userId, file.id, file.user_id, share_token]
    );

    res.status(201).json({
      success: true,
      message: 'تم إضافة الملف لحسابك بنجاح',
      data: {
        shared_item: {
          id: result.rows[0].id,
          file_id: file.id,
          file_name: file.original_name || file.name,
          file_type: file.file_type,
          file_size: file.file_size,
          owner_name: file.owner_name,
          created_at: result.rows[0].created_at
        }
      }
    });

  } catch (error) {
    console.error('Add shared file error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في إضافة الملف المشترك'
    });
  }
});

// إضافة مجلد مشترك لحساب المستخدم
router.post('/folder/add', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const { share_token } = req.body;

    if (!share_token) {
      return res.status(400).json({
        success: false,
        message: 'رمز المشاركة مطلوب'
      });
    }

    // جلب معلومات المجلد
    const folderResult = await pool.query(
      `SELECT f.id, f.user_id, f.name, f.share_token,
              u.username as owner_name
       FROM folders f
       JOIN users u ON f.user_id = u.id
       WHERE f.share_token = $1`,
      [share_token]
    );

    if (folderResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'المجلد غير موجود أو تم حذفه'
      });
    }

    const folder = folderResult.rows[0];

    // لا يمكن مشاركة مجلدك مع نفسك
    if (folder.user_id === userId) {
      return res.status(400).json({
        success: false,
        message: 'لا يمكنك إضافة مجلدك الخاص'
      });
    }

    // التحقق من عدم وجود المشاركة مسبقاً
    const existingResult = await pool.query(
      'SELECT id FROM shared_items WHERE user_id = $1 AND folder_id = $2',
      [userId, folder.id]
    );

    if (existingResult.rows.length > 0) {
      return res.status(400).json({
        success: false,
        message: 'هذا المجلد موجود بالفعل في حسابك'
      });
    }

    // إضافة المشاركة
    const result = await pool.query(
      `INSERT INTO shared_items (user_id, folder_id, owner_id, item_type, share_token)
       VALUES ($1, $2, $3, 'folder', $4)
       RETURNING id, created_at`,
      [userId, folder.id, folder.user_id, share_token]
    );

    res.status(201).json({
      success: true,
      message: 'تم إضافة المجلد لحسابك بنجاح',
      data: {
        shared_item: {
          id: result.rows[0].id,
          folder_id: folder.id,
          folder_name: folder.name,
          owner_name: folder.owner_name,
          created_at: result.rows[0].created_at
        }
      }
    });

  } catch (error) {
    console.error('Add shared folder error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في إضافة المجلد المشترك'
    });
  }
});

// جلب الملفات والمجلدات المشتركة مع المستخدم
router.get('/my-shared', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;

    // جلب الملفات المشتركة
    const filesResult = await pool.query(
      `SELECT si.id as shared_id, si.created_at as shared_at,
              f.id, f.name, f.original_name, f.file_type, f.file_size, f.share_token,
              f.views_count, f.downloads_count, f.created_at,
              u.username as owner_name
       FROM shared_items si
       JOIN files f ON si.file_id = f.id
       JOIN users u ON si.owner_id = u.id
       WHERE si.user_id = $1 AND si.item_type = 'file' AND f.status = 'active'
       ORDER BY si.created_at DESC`,
      [userId]
    );

    // جلب المجلدات المشتركة
    const foldersResult = await pool.query(
      `SELECT si.id as shared_id, si.created_at as shared_at,
              fo.id, fo.name, fo.share_token, fo.created_at,
              u.username as owner_name
       FROM shared_items si
       JOIN folders fo ON si.folder_id = fo.id
       JOIN users u ON si.owner_id = u.id
       WHERE si.user_id = $1 AND si.item_type = 'folder'
       ORDER BY si.created_at DESC`,
      [userId]
    );

    res.json({
      success: true,
      data: {
        files: filesResult.rows.map(f => ({
          ...f,
          is_shared_with_me: true
        })),
        folders: foldersResult.rows.map(f => ({
          ...f,
          is_shared_with_me: true
        })),
        files_count: filesResult.rows.length,
        folders_count: foldersResult.rows.length
      }
    });

  } catch (error) {
    console.error('Get shared items error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في جلب العناصر المشتركة'
    });
  }
});

// إزالة ملف/مجلد مشترك من حساب المستخدم
router.delete('/:id', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const sharedItemId = req.params.id;

    const result = await pool.query(
      'DELETE FROM shared_items WHERE id = $1 AND user_id = $2 RETURNING id',
      [sharedItemId, userId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'العنصر المشترك غير موجود'
      });
    }

    res.json({
      success: true,
      message: 'تم إزالة العنصر المشترك بنجاح'
    });

  } catch (error) {
    console.error('Remove shared item error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في إزالة العنصر المشترك'
    });
  }
});

module.exports = router;
