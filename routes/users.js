const express = require('express');
const router = express.Router();
const pool = require('../config/database');
const authenticateToken = require('../middleware/auth');

// جلب معلومات المستخدم
router.get('/profile', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;

    const result = await pool.query(
      `SELECT id, username, email, subscription_status, subscription_expiry, created_at, last_login 
       FROM users 
       WHERE id = $1`,
      [userId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'المستخدم غير موجود'
      });
    }

    // جلب عدد الفيديوهات
    const videosCount = await pool.query(
      'SELECT COUNT(*) as count FROM videos WHERE user_id = $1',
      [userId]
    );

    // جلب عدد الفيديوهات المحفوظة
    const libraryCount = await pool.query(
      'SELECT COUNT(*) as count FROM user_library WHERE user_id = $1',
      [userId]
    );

    res.json({
      success: true,
      data: {
        user: {
          ...result.rows[0],
          videos_count: parseInt(videosCount.rows[0].count),
          library_count: parseInt(libraryCount.rows[0].count)
        }
      }
    });

  } catch (error) {
    console.error('Get profile error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في جلب معلومات المستخدم'
    });
  }
});

// تحديث معلومات المستخدم
router.put('/profile', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const { username } = req.body;

    if (!username) {
      return res.status(400).json({
        success: false,
        message: 'اسم المستخدم مطلوب'
      });
    }

    // التحقق من عدم تكرار اسم المستخدم
    const checkUser = await pool.query(
      'SELECT id FROM users WHERE username = $1 AND id != $2',
      [username, userId]
    );

    if (checkUser.rows.length > 0) {
      return res.status(400).json({
        success: false,
        message: 'اسم المستخدم موجود بالفعل'
      });
    }

    await pool.query(
      'UPDATE users SET username = $1 WHERE id = $2',
      [username, userId]
    );

    res.json({
      success: true,
      message: 'تم تحديث المعلومات بنجاح'
    });

  } catch (error) {
    console.error('Update profile error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في تحديث المعلومات'
    });
  }
});

// جلب مكتبة المستخدم (الفيديوهات المحفوظة)
router.get('/library', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;

    const result = await pool.query(
      `SELECT v.id, v.title, v.filename, v.video_url, v.views_count, v.created_at, ul.saved_at 
       FROM user_library ul 
       JOIN videos v ON ul.video_id = v.id 
       WHERE ul.user_id = $1 
       ORDER BY ul.saved_at DESC`,
      [userId]
    );

    res.json({
      success: true,
      data: {
        videos: result.rows,
        count: result.rows.length
      }
    });

  } catch (error) {
    console.error('Get library error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في جلب المكتبة'
    });
  }
});

// إضافة فيديو إلى المكتبة
router.post('/library/:videoId', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const videoId = req.params.videoId;

    // التحقق من وجود الفيديو
    const videoCheck = await pool.query(
      'SELECT id FROM videos WHERE id = $1',
      [videoId]
    );

    if (videoCheck.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'الفيديو غير موجود'
      });
    }

    // إضافة إلى المكتبة (مع تجنب التكرار)
    try {
      await pool.query(
        'INSERT INTO user_library (user_id, video_id) VALUES ($1, $2)',
        [userId, videoId]
      );

      res.json({
        success: true,
        message: 'تم إضافة الفيديو إلى المكتبة بنجاح'
      });
    } catch (error) {
      if (error.code === '23505') { // Unique constraint violation
        return res.status(400).json({
          success: false,
          message: 'الفيديو موجود بالفعل في المكتبة'
        });
      }
      throw error;
    }

  } catch (error) {
    console.error('Add to library error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في إضافة الفيديو إلى المكتبة'
    });
  }
});

// حذف فيديو من المكتبة
router.delete('/library/:videoId', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const videoId = req.params.videoId;

    await pool.query(
      'DELETE FROM user_library WHERE user_id = $1 AND video_id = $2',
      [userId, videoId]
    );

    res.json({
      success: true,
      message: 'تم حذف الفيديو من المكتبة بنجاح'
    });

  } catch (error) {
    console.error('Remove from library error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في حذف الفيديو من المكتبة'
    });
  }
});

// جلب إحصائيات المستخدم
router.get('/stats', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;

    // إجمالي الفيديوهات
    const videosCount = await pool.query(
      'SELECT COUNT(*) as count FROM videos WHERE user_id = $1',
      [userId]
    );

    // إجمالي المشاهدات (videos + files)
    const totalViews = await pool.query(
      `SELECT 
        (SELECT COALESCE(SUM(views_count), 0) FROM videos WHERE user_id = $1) +
        (SELECT COALESCE(SUM(views_count), 0) FROM files WHERE user_id = $1 AND status = 'active')
       as total`,
      [userId]
    );

    // إجمالي حجم الملفات (فيديوهات)
    const videoStorage = await pool.query(
      'SELECT COALESCE(SUM(file_size), 0) as total FROM videos WHERE user_id = $1',
      [userId]
    );

    // إجمالي حجم الملفات (files)
    const fileStorage = await pool.query(
      'SELECT COALESCE(SUM(file_size), 0) as total FROM files WHERE user_id = $1 AND status = $2',
      [userId, 'active']
    );

    // إجمالي الإعجابات
    const totalLikes = await pool.query(
      'SELECT COALESCE(SUM(likes_count), 0) as total FROM videos WHERE user_id = $1',
      [userId]
    );

    // إجمالي الملفات
    const filesCount = await pool.query(
      'SELECT COUNT(*) as count FROM files WHERE user_id = $1 AND status = $2',
      [userId, 'active']
    );

    // إجمالي التنزيلات
    const totalDownloads = await pool.query(
      'SELECT COALESCE(SUM(downloads_count), 0) as total FROM files WHERE user_id = $1 AND status = $2',
      [userId, 'active']
    );

    // جلب معلومات التخزين من جدول المستخدمين
    const userStorage = await pool.query(
      'SELECT storage_used, storage_limit, total_uploaded, total_downloaded FROM users WHERE id = $1',
      [userId]
    );

    const user = userStorage.rows[0] || {};
    const totalStorage = parseInt(videoStorage.rows[0].total) + parseInt(fileStorage.rows[0].total);

    res.json({
      success: true,
      data: {
        stats: {
          videos_count: parseInt(videosCount.rows[0].count),
          files_count: parseInt(filesCount.rows[0].count),
          total_views: parseInt(totalViews.rows[0].total),
          total_downloads: parseInt(totalDownloads.rows[0].total),
          total_storage: totalStorage,
          total_likes: parseInt(totalLikes.rows[0].total),
          transaction_volume: {
            total_uploaded: parseInt(user.total_uploaded) || 0,
            total_downloaded: parseInt(user.total_downloaded) || 0,
            storage_used: parseInt(user.storage_used) || 0,
            storage_limit: parseInt(user.storage_limit) || 10737418240
          }
        }
      }
    });

  } catch (error) {
    console.error('Get stats error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في جلب الإحصائيات'
    });
  }
});

// جلب فيديوهات المستخدم (مع limit اختياري)
router.get('/videos', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const limit = req.query.limit ? parseInt(req.query.limit) : null;

    let query = `SELECT id, title, description, filename, video_url, views_count, likes_count, file_size, created_at 
       FROM videos 
       WHERE user_id = $1 
       ORDER BY created_at DESC`;
    
    const params = [userId];
    
    if (limit) {
      query += ' LIMIT $2';
      params.push(limit);
    }

    const result = await pool.query(query, params);

    res.json({
      success: true,
      data: {
        videos: result.rows,
        count: result.rows.length
      }
    });

  } catch (error) {
    console.error('Get user videos error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في جلب الفيديوهات'
    });
  }
});

// البحث في فيديوهات المستخدم
router.get('/videos/search', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const { q, limit = 20 } = req.query;
    
    if (!q || q.trim() === '') {
      return res.status(400).json({
        success: false,
        message: 'يرجى إدخال كلمة البحث'
      });
    }

    const searchTerm = `%${q.trim()}%`;
    
    const result = await pool.query(
      `SELECT id, title, description, filename, video_url, views_count, likes_count, file_size, created_at 
       FROM videos 
       WHERE user_id = $1 
         AND (title ILIKE $2 OR description ILIKE $2)
       ORDER BY created_at DESC 
       LIMIT $3`,
      [userId, searchTerm, parseInt(limit)]
    );

    res.json({
      success: true,
      data: {
        videos: result.rows,
        count: result.rows.length,
        query: q
      }
    });

  } catch (error) {
    console.error('Search user videos error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في البحث'
    });
  }
});

// جلب نشاطات المستخدم
router.get('/activities', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const limit = parseInt(req.query.limit) || 50;

    const result = await pool.query(
      `SELECT activity_type, activity_data, created_at 
       FROM user_activities 
       WHERE user_id = $1 
       ORDER BY created_at DESC 
       LIMIT $2`,
      [userId, limit]
    );

    res.json({
      success: true,
      data: {
        activities: result.rows,
        count: result.rows.length
      }
    });

  } catch (error) {
    console.error('Get activities error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في جلب النشاطات'
    });
  }
});

// تغيير كلمة المرور
router.put('/password', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const { current_password, new_password } = req.body;

    if (!current_password || !new_password) {
      return res.status(400).json({
        success: false,
        message: 'كلمة المرور الحالية والجديدة مطلوبتان'
      });
    }

    if (new_password.length < 6) {
      return res.status(400).json({
        success: false,
        message: 'كلمة المرور الجديدة يجب أن تكون 6 أحرف على الأقل'
      });
    }

    // جلب كلمة المرور الحالية
    const userResult = await pool.query(
      'SELECT password_hash FROM users WHERE id = $1',
      [userId]
    );

    if (userResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'المستخدم غير موجود'
      });
    }

    // التحقق من كلمة المرور الحالية
    const bcrypt = require('bcryptjs');
    const isValid = await bcrypt.compare(current_password, userResult.rows[0].password_hash);

    if (!isValid) {
      return res.status(400).json({
        success: false,
        message: 'كلمة المرور الحالية غير صحيحة'
      });
    }

    // تشفير كلمة المرور الجديدة
    const salt = await bcrypt.genSalt(10);
    const newPasswordHash = await bcrypt.hash(new_password, salt);

    // تحديث كلمة المرور
    await pool.query(
      'UPDATE users SET password_hash = $1 WHERE id = $2',
      [newPasswordHash, userId]
    );

    res.json({
      success: true,
      message: 'تم تغيير كلمة المرور بنجاح'
    });

  } catch (error) {
    console.error('Change password error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في تغيير كلمة المرور'
    });
  }
});

module.exports = router;
