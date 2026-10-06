const express = require('express');
const router = express.Router();
const pool = require('../config/database');
const authenticateToken = require('../middleware/auth');
const upload = require('../middleware/upload');
const path = require('path');
const fs = require('fs');

// رفع فيديو جديد
router.post('/upload', authenticateToken, upload.single('video'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({
        success: false,
        message: 'لم يتم رفع أي ملف'
      });
    }

    const userId = req.user.id;
    const { title, description, tags } = req.body;
    const filename = req.file.filename;
    const filePath = req.file.path;
    const fileSize = req.file.size;
    
    // معالجة tags
    let tagsArray = [];
    if (tags) {
      tagsArray = Array.isArray(tags) ? tags : tags.split(',').map(t => t.trim()).filter(t => t);
    }

    // إنشاء رابط الفيديو
    const videoId = Date.now() + '-' + Math.round(Math.random() * 1E9);
    const baseUrl = process.env.BASE_URL || 'http://127.0.0.1:8080';
    const videoUrl = `${baseUrl}/index.php?id=${videoId}`;

    // حفظ معلومات الفيديو في قاعدة البيانات
    const result = await pool.query(
      `INSERT INTO videos (user_id, title, description, tags, filename, file_path, file_size, video_url) 
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8) 
       RETURNING id, title, description, tags, filename, video_url, views_count, created_at`,
      [userId, title || filename, description || null, tagsArray.length > 0 ? tagsArray : null, filename, filePath, fileSize, videoUrl]
    );

    const video = result.rows[0];

    // تسجيل النشاط
    await pool.query(
      'INSERT INTO user_activities (user_id, activity_type, activity_data) VALUES ($1, $2, $3)',
      [userId, 'video_upload', JSON.stringify({ video_id: video.id, title: video.title })]
    );

    res.status(201).json({
      success: true,
      message: 'تم رفع الفيديو بنجاح',
      data: {
        video: {
          id: video.id,
          title: video.title,
          video_url: video.video_url,
          views_count: video.views_count,
          created_at: video.created_at
        }
      }
    });

  } catch (error) {
    console.error('Upload error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في رفع الفيديو'
    });
  }
});

// جلب جميع فيديوهات المستخدم
router.get('/user', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;

    const result = await pool.query(
      `SELECT id, title, filename, video_url, views_count, file_size, created_at 
       FROM videos 
       WHERE user_id = $1 
       ORDER BY created_at DESC`,
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
    console.error('Get videos error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في جلب الفيديوهات'
    });
  }
});

// جلب رابط فيديو معين
router.get('/:id/link', authenticateToken, async (req, res) => {
  try {
    const videoId = req.params.id;
    const userId = req.user.id;

    const result = await pool.query(
      'SELECT id, title, video_url, file_path FROM videos WHERE id = $1 AND user_id = $2',
      [videoId, userId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'الفيديو غير موجود'
      });
    }

    res.json({
      success: true,
      data: {
        video: result.rows[0]
      }
    });

  } catch (error) {
    console.error('Get video link error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في جلب رابط الفيديو'
    });
  }
});

// جلب معلومات فيديو (لصفحة الويب)
router.get('/:id/info', async (req, res) => {
  try {
    const videoId = req.params.id;

    // البحث عن الفيديو باستخدام video_url الذي يحتوي على id
    // نبحث بعدة طرق لضمان إيجاد الفيديو
    let result = await pool.query(
      `SELECT id, title, description, tags, filename, file_path, views_count, likes_count, created_at 
       FROM videos 
       WHERE (video_url LIKE $1 OR video_url LIKE $2 OR video_url LIKE $3) AND status = 'active'`,
      [`%id=${videoId}%`, `%?id=${videoId}%`, `%id=${videoId}&%`]
    );

    // إذا لم نجد، جرب البحث بالـ id الفعلي (إذا كان videoId رقم)
    if (result.rows.length === 0 && !isNaN(videoId)) {
      result = await pool.query(
        `SELECT id, title, description, tags, filename, file_path, views_count, likes_count, created_at 
         FROM videos 
         WHERE id = $1 AND status = 'active'`,
        [parseInt(videoId)]
      );
    }

    // إذا لم نجد بعد، جرب البحث في video_url بشكل أكثر مرونة
    if (result.rows.length === 0) {
      result = await pool.query(
        `SELECT id, title, description, tags, filename, file_path, views_count, likes_count, created_at 
         FROM videos 
         WHERE (video_url LIKE $1 OR video_url LIKE $2) AND status = 'active'`,
        [`%${videoId}%`, `%${encodeURIComponent(videoId)}%`]
      );
    }

    if (result.rows.length === 0) {
      console.log('Video not found for ID:', videoId);
      // للتصحيح: عرض جميع الفيديوهات
      const allVideos = await pool.query('SELECT id, video_url FROM videos LIMIT 5');
      console.log('Available videos:', allVideos.rows);
      
      return res.status(404).json({
        success: false,
        message: 'الفيديو غير موجود',
        debug: {
          searchedId: videoId,
          availableVideos: allVideos.rows.map(v => ({ id: v.id, url: v.video_url }))
        }
      });
    }

    const video = result.rows[0];

    // زيادة عدد المشاهدات
    await pool.query(
      'UPDATE videos SET views_count = views_count + 1 WHERE id = $1',
      [video.id]
    );

    // إنشاء رابط الفيديو المباشر (يستخدم Nginx على 8080)
    const apiBaseUrl = process.env.API_BASE_URL || process.env.BASE_URL || 'http://127.0.0.1:8080';
    const videoDirectUrl = `${apiBaseUrl}/api/videos/${video.id}/stream`;

    res.json({
      success: true,
      data: {
        video: {
          id: video.id,
          title: video.title,
          description: video.description || null,
          tags: video.tags || [],
          video_url: videoDirectUrl,
          views_count: video.views_count + 1,
          likes_count: video.likes_count || 0,
          created_at: video.created_at
        }
      }
    });

  } catch (error) {
    console.error('Get video info error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في جلب معلومات الفيديو'
    });
  }
});

// بث الفيديو (Streaming)
router.get('/:id/stream', async (req, res) => {
  try {
    const videoId = req.params.id;

    const result = await pool.query(
      'SELECT file_path, filename FROM videos WHERE id = $1',
      [videoId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'الفيديو غير موجود'
      });
    }

    const video = result.rows[0];
    const videoPath = path.join(__dirname, '..', video.file_path);

    if (!fs.existsSync(videoPath)) {
      return res.status(404).json({
        success: false,
        message: 'ملف الفيديو غير موجود'
      });
    }

    const stat = fs.statSync(videoPath);
    const fileSize = stat.size;
    const range = req.headers.range;

    if (range) {
      // دعم Range requests للبث
      const parts = range.replace(/bytes=/, "").split("-");
      const start = parseInt(parts[0], 10);
      const end = parts[1] ? parseInt(parts[1], 10) : fileSize - 1;
      const chunksize = (end - start) + 1;
      const file = fs.createReadStream(videoPath, { start, end });
      const head = {
        'Content-Range': `bytes ${start}-${end}/${fileSize}`,
        'Accept-Ranges': 'bytes',
        'Content-Length': chunksize,
        'Content-Type': 'video/mp4',
      };
      res.writeHead(206, head);
      file.pipe(res);
    } else {
      const head = {
        'Content-Length': fileSize,
        'Content-Type': 'video/mp4',
      };
      res.writeHead(200, head);
      fs.createReadStream(videoPath).pipe(res);
    }

  } catch (error) {
    console.error('Stream video error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في بث الفيديو'
    });
  }
});

// حذف فيديو
router.delete('/:id', authenticateToken, async (req, res) => {
  try {
    const videoId = req.params.id;
    const userId = req.user.id;

    // التحقق من ملكية الفيديو
    const result = await pool.query(
      'SELECT file_path FROM videos WHERE id = $1 AND user_id = $2',
      [videoId, userId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'الفيديو غير موجود أو ليس لديك صلاحية لحذفه'
      });
    }

    const video = result.rows[0];

    // حذف الملف من السيرفر
    const filePath = path.join(__dirname, '..', video.file_path);
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
    }

    // حذف من قاعدة البيانات
    await pool.query('DELETE FROM videos WHERE id = $1', [videoId]);

    // تسجيل النشاط
    await pool.query(
      'INSERT INTO user_activities (user_id, activity_type, activity_data) VALUES ($1, $2, $3)',
      [userId, 'video_delete', JSON.stringify({ video_id: videoId })]
    );

    res.json({
      success: true,
      message: 'تم حذف الفيديو بنجاح'
    });

  } catch (error) {
    console.error('Delete video error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في حذف الفيديو'
    });
  }
});

// جلب الفيديوهات المقترحة (أحدث الفيديوهات)
router.get('/recommended', async (req, res) => {
  try {
    const limit = parseInt(req.query.limit) || 20;
    
    const result = await pool.query(
      `SELECT id, title, description, tags, video_url, views_count, likes_count, created_at 
       FROM videos 
       WHERE status = 'active' 
       ORDER BY created_at DESC 
       LIMIT $1`,
      [limit]
    );

    res.json({
      success: true,
      data: {
        videos: result.rows,
        count: result.rows.length
      }
    });

  } catch (error) {
    console.error('Get recommended videos error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في جلب الفيديوهات المقترحة'
    });
  }
});

// البحث عن فيديوهات
router.get('/search', async (req, res) => {
  try {
    const { q, limit = 20 } = req.query;
    
    if (!q || q.trim() === '') {
      return res.status(400).json({
        success: false,
        message: 'يرجى إدخال كلمة البحث'
      });
    }

    const searchTerm = `%${q.trim()}%`;
    
    const result = await pool.query(
      `SELECT id, title, description, tags, video_url, views_count, likes_count, created_at 
       FROM videos 
       WHERE status = 'active' 
         AND (title ILIKE $1 OR description ILIKE $1 OR $2 = ANY(tags))
       ORDER BY views_count DESC, created_at DESC 
       LIMIT $3`,
      [searchTerm, q.trim().toLowerCase(), parseInt(limit)]
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
    console.error('Search videos error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في البحث'
    });
  }
});

// الفيديوهات الأكثر مشاهدة (Trending)
router.get('/trending', async (req, res) => {
  try {
    const limit = parseInt(req.query.limit) || 10;
    
    const result = await pool.query(
      `SELECT id, title, description, tags, video_url, views_count, likes_count, created_at 
       FROM videos 
       WHERE status = 'active' 
       ORDER BY views_count DESC, likes_count DESC 
       LIMIT $1`,
      [limit]
    );

    res.json({
      success: true,
      data: {
        videos: result.rows,
        count: result.rows.length
      }
    });

  } catch (error) {
    console.error('Get trending videos error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في جلب الفيديوهات الرائجة'
    });
  }
});

// Debug endpoint - عرض جميع الفيديوهات (للتطوير فقط)
router.get('/debug/all', async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT id, title, video_url, created_at FROM videos ORDER BY id DESC LIMIT 10'
    );
    
    res.json({
      success: true,
      data: {
        videos: result.rows,
        count: result.rows.length
      }
    });
  } catch (error) {
    console.error('Debug error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في جلب البيانات'
    });
  }
});

module.exports = router;
