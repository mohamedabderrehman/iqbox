const express = require('express');
const router = express.Router();
const pool = require('../config/database');
const authenticateToken = require('../middleware/auth');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const path = require('path');
const fs = require('fs');

// Middleware للتحقق من صلاحيات Admin
const authenticateAdmin = async (req, res, next) => {
  try {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];
    
    if (!token) {
      return res.status(401).json({ success: false, message: 'غير مصرح' });
    }
    
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    if (decoded.isAdmin !== true) return res.status(403).json({success:false,message:'Admin account required'});
    
    // التحقق من أن المستخدم admin
    const result = await pool.query(
      'SELECT id, username, email, role FROM admins WHERE id = $1 AND is_active = true',
      [decoded.id]
    );
    
    if (result.rows.length === 0) {
      return res.status(403).json({ success: false, message: 'غير مصرح للأدمن' });
    }
    
    req.admin = result.rows[0];
    next();
  } catch (error) {
    return res.status(403).json({ success: false, message: 'توكن غير صالح' });
  }
};

// Middleware قديم للتوافق
const isAdmin = (req, res, next) => {
  next();
};

// ===== Admin Authentication =====

// تسجيل دخول الأدمن
router.post('/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    
    if (!email || !password) {
      return res.status(400).json({
        success: false,
        message: 'البريد وكلمة المرور مطلوبان'
      });
    }
    
    const result = await pool.query(
      'SELECT id, username, email, password_hash, role FROM admins WHERE email = $1 AND is_active = true',
      [email]
    );
    
    if (result.rows.length === 0) {
      return res.status(401).json({
        success: false,
        message: 'بيانات الدخول غير صحيحة'
      });
    }
    
    const admin = result.rows[0];
    const validPassword = await bcrypt.compare(password, admin.password_hash);
    
    if (!validPassword) {
      return res.status(401).json({
        success: false,
        message: 'بيانات الدخول غير صحيحة'
      });
    }
    
    // تحديث آخر تسجيل دخول
    await pool.query(
      'UPDATE admins SET last_login = CURRENT_TIMESTAMP WHERE id = $1',
      [admin.id]
    );
    
    const token = jwt.sign(
      { id: admin.id, email: admin.email, role: admin.role, isAdmin: true },
      process.env.JWT_SECRET,
      { expiresIn: '7d' }
    );
    
    res.json({
      success: true,
      data: {
        token,
        admin: {
          id: admin.id,
          username: admin.username,
          email: admin.email,
          role: admin.role
        }
      }
    });
  } catch (error) {
    console.error('Admin login error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في تسجيل الدخول'
    });
  }
});

// ===== Users Management =====

// جلب جميع المستخدمين
router.get('/users', authenticateAdmin, async (req, res) => {
  try {
    const { page = 1, limit = 20 } = req.query;
    const offset = (page - 1) * limit;
    
    const result = await pool.query(`
      SELECT u.id, u.username, u.email, u.subscription_status, u.subscription_expiry,
             u.created_at, u.last_login,
             (SELECT COUNT(*) FROM videos WHERE user_id = u.id) as videos_count,
             (SELECT COUNT(*) FROM files WHERE user_id = u.id AND status = 'active') as files_count
      FROM users u
      ORDER BY u.created_at DESC
      LIMIT $1 OFFSET $2
    `, [limit, offset]);
    
    const countResult = await pool.query('SELECT COUNT(*) as total FROM users');
    
    res.json({
      success: true,
      data: {
        users: result.rows,
        total: parseInt(countResult.rows[0].total)
      }
    });
  } catch (error) {
    console.error('Get users error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب المستخدمين' });
  }
});

// حذف مستخدم
router.delete('/users/:id', authenticateAdmin, async (req, res) => {
  try {
    const { id } = req.params;
    await pool.query('DELETE FROM users WHERE id = $1', [id]);
    res.json({ success: true, message: 'تم حذف المستخدم' });
  } catch (error) {
    console.error('Delete user error:', error);
    res.status(500).json({ success: false, message: 'خطأ في حذف المستخدم' });
  }
});

// ===== Subscription Plans Management =====

// جلب جميع الخطط
router.get('/plans', authenticateAdmin, async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT * FROM subscription_plans ORDER BY sort_order ASC, price ASC'
    );
    res.json({ success: true, data: { plans: result.rows } });
  } catch (error) {
    console.error('Get plans error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب الخطط' });
  }
});

// إنشاء خطة جديدة
router.post('/plans', authenticateAdmin, async (req, res) => {
  try {
    const { name, name_ar, description, description_ar, price, currency, duration_days, 
            features, payment_instructions, payment_instructions_ar, is_active, sort_order } = req.body;
    
    const result = await pool.query(`
      INSERT INTO subscription_plans 
      (name, name_ar, description, description_ar, price, currency, duration_days, 
       features, payment_instructions, payment_instructions_ar, is_active, sort_order)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
      RETURNING *
    `, [name, name_ar, description, description_ar, price, currency || 'USD', duration_days,
        features ? JSON.stringify(features) : null, payment_instructions, payment_instructions_ar,
        is_active !== false, sort_order || 0]);
    
    res.status(201).json({ success: true, data: { plan: result.rows[0] } });
  } catch (error) {
    console.error('Create plan error:', error);
    res.status(500).json({ success: false, message: 'خطأ في إنشاء الخطة' });
  }
});

// تحديث خطة
router.put('/plans/:id', authenticateAdmin, async (req, res) => {
  try {
    const { id } = req.params;
    const { name, name_ar, description, description_ar, price, currency, duration_days,
            features, payment_instructions, payment_instructions_ar, is_active, sort_order } = req.body;
    
    const result = await pool.query(`
      UPDATE subscription_plans SET
        name = COALESCE($1, name),
        name_ar = COALESCE($2, name_ar),
        description = COALESCE($3, description),
        description_ar = COALESCE($4, description_ar),
        price = COALESCE($5, price),
        currency = COALESCE($6, currency),
        duration_days = COALESCE($7, duration_days),
        features = COALESCE($8, features),
        payment_instructions = COALESCE($9, payment_instructions),
        payment_instructions_ar = COALESCE($10, payment_instructions_ar),
        is_active = COALESCE($11, is_active),
        sort_order = COALESCE($12, sort_order),
        updated_at = CURRENT_TIMESTAMP
      WHERE id = $13
      RETURNING *
    `, [name, name_ar, description, description_ar, price, currency, duration_days,
        features ? JSON.stringify(features) : null, payment_instructions, payment_instructions_ar,
        is_active, sort_order, id]);
    
    if (result.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'الخطة غير موجودة' });
    }
    
    res.json({ success: true, data: { plan: result.rows[0] } });
  } catch (error) {
    console.error('Update plan error:', error);
    res.status(500).json({ success: false, message: 'خطأ في تحديث الخطة' });
  }
});

// حذف خطة
router.delete('/plans/:id', authenticateAdmin, async (req, res) => {
  try {
    const { id } = req.params;
    await pool.query('DELETE FROM subscription_plans WHERE id = $1', [id]);
    res.json({ success: true, message: 'تم حذف الخطة' });
  } catch (error) {
    console.error('Delete plan error:', error);
    res.status(500).json({ success: false, message: 'خطأ في حذف الخطة' });
  }
});

// ===== Payment Requests Management =====

// جلب طلبات الدفع
router.get('/payments', authenticateAdmin, async (req, res) => {
  try {
    const { status } = req.query;
    
    let query = `
      SELECT pr.*, u.username, u.email, sp.name as plan_name, sp.name_ar as plan_name_ar
      FROM payment_requests pr
      LEFT JOIN users u ON pr.user_id = u.id
      LEFT JOIN subscription_plans sp ON pr.plan_id = sp.id
    `;
    
    const params = [];
    if (status && status !== 'all') {
      query += ' WHERE pr.status = $1';
      params.push(status);
    }
    
    query += ' ORDER BY pr.created_at DESC';
    
    const result = await pool.query(query, params);
    res.json({ success: true, data: { requests: result.rows } });
  } catch (error) {
    console.error('Get payments error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب طلبات الدفع' });
  }
});

// الموافقة على طلب دفع
router.post('/payments/:id/approve', authenticateAdmin, async (req, res) => {
  try {
    const { id } = req.params;
    
    // جلب معلومات الطلب مع بيانات الخطة
    const requestResult = await pool.query(
      'SELECT pr.*, sp.duration_days, sp.price FROM payment_requests pr LEFT JOIN subscription_plans sp ON pr.plan_id = sp.id WHERE pr.id = $1',
      [id]
    );
    
    if (requestResult.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'الطلب غير موجود' });
    }
    
    const request = requestResult.rows[0];
    
    // حساب تاريخ انتهاء الاشتراك
    const expiryDate = new Date();
    expiryDate.setDate(expiryDate.getDate() + (request.duration_days || 30));
    
    // تحديث اشتراك المستخدم
    await pool.query(
      'UPDATE users SET subscription_status = $1, subscription_expiry = $2 WHERE id = $3',
      ['premium', expiryDate, request.user_id]
    );
    
    // تحديث حالة الطلب
    await pool.query(
      'UPDATE payment_requests SET status = $1, processed_by = $2, processed_at = CURRENT_TIMESTAMP WHERE id = $3',
      ['approved', req.admin.id, id]
    );
    
    // ── إضافة عمولة الإحالة 10% للمُحيل ──
    try {
      const referralResult = await pool.query(
        'SELECT referrer_id FROM referrals WHERE referred_id = $1',
        [request.user_id]
      );
      
      if (referralResult.rows.length > 0) {
        const referrerId = referralResult.rows[0].referrer_id;
        const subscriptionPrice = parseFloat(request.price || request.amount || 0);
        
        // جلب نسبة الإحالة من الإعدادات (افتراضي 10%)
        let referralPercentage = 10;
        try {
          const settingResult = await pool.query(
            "SELECT setting_value FROM system_settings WHERE setting_key = 'referral_percentage'"
          );
          if (settingResult.rows.length > 0) {
            referralPercentage = parseFloat(settingResult.rows[0].setting_value) || 10;
          }
        } catch (e) { /* use default 10% */ }
        
        const referrerEarning = (subscriptionPrice * referralPercentage) / 100;
        
        if (referrerEarning > 0) {
          // تسجيل أرباح الإحالة
          await pool.query(
            `INSERT INTO referral_earnings (referrer_id, referred_id, source_earning, referrer_earning)
             VALUES ($1, $2, $3, $4)`,
            [referrerId, request.user_id, subscriptionPrice, referrerEarning]
          );
          
          // تحديث إجمالي أرباح الإحالة
          await pool.query(
            'UPDATE referrals SET total_earnings = COALESCE(total_earnings, 0) + $1 WHERE referrer_id = $2 AND referred_id = $3',
            [referrerEarning, referrerId, request.user_id]
          );
          
          // إضافة المبلغ لمحفظة المُحيل
          await pool.query(`
            INSERT INTO user_wallets (user_id, balance, total_earned, total_withdrawn, pending_withdrawal)
            VALUES ($1, $2, $2, 0, 0)
            ON CONFLICT (user_id) DO UPDATE SET 
              balance = user_wallets.balance + $2,
              total_earned = user_wallets.total_earned + $2
          `, [referrerId, referrerEarning]);
          
          // تسجيل المعاملة في المحفظة
          const walletResult = await pool.query(
            'SELECT balance FROM user_wallets WHERE user_id = $1',
            [referrerId]
          );
          const newBalance = walletResult.rows[0]?.balance || referrerEarning;
          
          await pool.query(`
            INSERT INTO wallet_transactions (user_id, type, amount, balance_after, description, reference_type)
            VALUES ($1, 'referral_earning', $2, $3, $4, 'premium_subscription')
          `, [referrerId, referrerEarning, newBalance, `عمولة إحالة - اشتراك بريميوم ($${subscriptionPrice})`]);
          
          console.log(`Referral commission: $${referrerEarning} credited to user ${referrerId} for referred user ${request.user_id} premium subscription`);
        }
      }
    } catch (referralError) {
      console.error('Referral commission error (non-blocking):', referralError);
      // لا نوقف العملية إذا فشلت الإحالة
    }
    
    res.json({ success: true, message: 'تمت الموافقة على الطلب' });
  } catch (error) {
    console.error('Approve payment error:', error);
    res.status(500).json({ success: false, message: 'خطأ في الموافقة على الطلب' });
  }
});

// رفض طلب دفع
router.post('/payments/:id/reject', authenticateAdmin, async (req, res) => {
  try {
    const { id } = req.params;
    const { reason } = req.body;
    
    await pool.query(
      'UPDATE payment_requests SET status = $1, admin_notes = $2, processed_by = $3, processed_at = CURRENT_TIMESTAMP WHERE id = $4',
      ['rejected', reason, req.admin.id, id]
    );
    
    res.json({ success: true, message: 'تم رفض الطلب' });
  } catch (error) {
    console.error('Reject payment error:', error);
    res.status(500).json({ success: false, message: 'خطأ في رفض الطلب' });
  }
});

// ===== App Settings Management =====

// جلب إعدادات التطبيق
router.get('/settings', authenticateAdmin, async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM app_settings WHERE id = 1');
    
    if (result.rows.length === 0) {
      return res.json({
        success: true,
        data: {
          settings: {
            app_name: 'IQBox',
            app_logo_url: null,
            primary_color: '#3B82F6',
            secondary_color: '#1E3A8A'
          }
        }
      });
    }
    
    res.json({ success: true, data: { settings: result.rows[0] } });
  } catch (error) {
    console.error('Get app settings error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب الإعدادات' });
  }
});

// تحديث إعدادات التطبيق
router.put('/settings', authenticateAdmin, async (req, res) => {
  try {
    const { app_name, app_logo_url, app_icon_url, primary_color, secondary_color,
            support_email, support_phone, terms_url, privacy_url } = req.body;
    
    await pool.query(`
      UPDATE app_settings SET
        app_name = COALESCE($1, app_name),
        app_logo_url = $2,
        app_icon_url = $3,
        primary_color = COALESCE($4, primary_color),
        secondary_color = COALESCE($5, secondary_color),
        support_email = $6,
        support_phone = $7,
        terms_url = $8,
        privacy_url = $9,
        updated_at = CURRENT_TIMESTAMP
      WHERE id = 1
    `, [app_name, app_logo_url, app_icon_url, primary_color, secondary_color,
        support_email, support_phone, terms_url, privacy_url]);
    
    res.json({ success: true, message: 'تم تحديث الإعدادات' });
  } catch (error) {
    console.error('Update app settings error:', error);
    res.status(500).json({ success: false, message: 'خطأ في تحديث الإعدادات' });
  }
});

// ===== Dashboard Stats =====

router.get('/stats', authenticateAdmin, async (req, res) => {
  try {
    const [users, videos, files, videoViews, fileViews, premium, pending] = await Promise.all([
      pool.query('SELECT COUNT(*) as count FROM users'),
      pool.query('SELECT COUNT(*) as count FROM videos WHERE status = $1', ['active']),
      pool.query('SELECT COUNT(*) as count FROM files WHERE status = $1', ['active']),
      pool.query('SELECT COALESCE(SUM(views_count), 0) as total FROM videos'),
      pool.query("SELECT COALESCE(SUM(views_count), 0) as total FROM files WHERE status = 'active'"),
      pool.query('SELECT COUNT(*) as count FROM users WHERE subscription_status = $1', ['premium']),
      pool.query('SELECT COUNT(*) as count FROM payment_requests WHERE status = $1', ['pending'])
    ]);
    
    res.json({
      success: true,
      data: {
        users_count: parseInt(users.rows[0].count),
        videos_count: parseInt(videos.rows[0].count),
        files_count: parseInt(files.rows[0].count),
        total_views: parseInt(videoViews.rows[0].total) + parseInt(fileViews.rows[0].total),
        premium_users: parseInt(premium.rows[0].count),
        pending_payments: parseInt(pending.rows[0].count),
        monthly_revenue: 0 // TODO: حساب الإيرادات
      }
    });
  } catch (error) {
    console.error('Get stats error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب الإحصائيات' });
  }
});

// ===== Video Management =====

// جلب جميع الفيديوهات (لـ Admin)
router.get('/videos', authenticateToken, isAdmin, async (req, res) => {
  try {
    const { page = 1, limit = 50, status, search } = req.query;
    const offset = (page - 1) * limit;

    let query = `
      SELECT 
        v.id, v.title, v.description, v.tags, v.views_count, v.likes_count,
        v.is_featured, v.is_recommended, v.status, v.created_at, v.updated_at,
        u.username, u.email
      FROM videos v
      JOIN users u ON v.user_id = u.id
      WHERE 1=1
    `;
    const params = [];
    let paramCount = 1;

    if (status) {
      query += ` AND v.status = $${paramCount}`;
      params.push(status);
      paramCount++;
    }

    if (search) {
      query += ` AND (v.title ILIKE $${paramCount} OR v.description ILIKE $${paramCount})`;
      params.push(`%${search}%`);
      paramCount++;
    }

    query += ` ORDER BY v.created_at DESC LIMIT $${paramCount} OFFSET $${paramCount + 1}`;
    params.push(limit, offset);

    const result = await pool.query(query, params);

    // جلب العدد الكلي
    const countResult = await pool.query(
      'SELECT COUNT(*) as total FROM videos WHERE status = $1 OR $1 IS NULL',
      [status || null]
    );

    res.json({
      success: true,
      data: {
        videos: result.rows,
        pagination: {
          page: parseInt(page),
          limit: parseInt(limit),
          total: parseInt(countResult.rows[0].total),
          pages: Math.ceil(countResult.rows[0].total / limit)
        }
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

// تحديث معلومات فيديو (Admin)
router.put('/videos/:id', authenticateToken, isAdmin, async (req, res) => {
  try {
    const videoId = req.params.id;
    const { title, description, tags, is_featured, is_recommended, status } = req.body;

    const updates = [];
    const values = [];
    let paramCount = 1;

    if (title !== undefined) {
      updates.push(`title = $${paramCount}`);
      values.push(title);
      paramCount++;
    }

    if (description !== undefined) {
      updates.push(`description = $${paramCount}`);
      values.push(description);
      paramCount++;
    }

    if (tags !== undefined) {
      updates.push(`tags = $${paramCount}`);
      values.push(Array.isArray(tags) ? tags : [tags]);
      paramCount++;
    }

    if (is_featured !== undefined) {
      updates.push(`is_featured = $${paramCount}`);
      values.push(is_featured);
      paramCount++;
    }

    if (is_recommended !== undefined) {
      updates.push(`is_recommended = $${paramCount}`);
      values.push(is_recommended);
      paramCount++;
    }

    if (status !== undefined) {
      updates.push(`status = $${paramCount}`);
      values.push(status);
      paramCount++;
    }

    if (updates.length === 0) {
      return res.status(400).json({
        success: false,
        message: 'لا توجد بيانات للتحديث'
      });
    }

    updates.push(`updated_at = CURRENT_TIMESTAMP`);
    values.push(videoId);

    const query = `
      UPDATE videos 
      SET ${updates.join(', ')}
      WHERE id = $${paramCount}
      RETURNING id, title, description, tags, is_featured, is_recommended, status, updated_at
    `;

    const result = await pool.query(query, values);

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'الفيديو غير موجود'
      });
    }

    res.json({
      success: true,
      message: 'تم تحديث الفيديو بنجاح',
      data: {
        video: result.rows[0]
      }
    });

  } catch (error) {
    console.error('Update video error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في تحديث الفيديو'
    });
  }
});

// حذف فيديو (Admin)
router.delete('/videos/:id', authenticateToken, isAdmin, async (req, res) => {
  try {
    const videoId = req.params.id;

    // جلب معلومات الفيديو
    const videoResult = await pool.query(
      'SELECT file_path FROM videos WHERE id = $1',
      [videoId]
    );

    if (videoResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'الفيديو غير موجود'
      });
    }

    // حذف الملف
    const filePath = path.join(__dirname, '..', videoResult.rows[0].file_path);
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
    }

    // حذف من قاعدة البيانات
    await pool.query('DELETE FROM videos WHERE id = $1', [videoId]);

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

// ===== Recommended Videos Management =====

// جلب الفيديوهات المقترحة
router.get('/recommended', async (req, res) => {
  try {
    const result = await pool.query(`
      SELECT 
        v.id, v.title, v.thumbnail_url, v.views_count, v.created_at,
        rv.position
      FROM recommended_videos rv
      JOIN videos v ON rv.video_id = v.id
      WHERE rv.is_active = TRUE AND v.status = 'active'
      ORDER BY rv.position ASC, v.created_at DESC
      LIMIT 20
    `);

    res.json({
      success: true,
      data: {
        videos: result.rows
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

// إضافة فيديو للقائمة المقترحة (Admin)
router.post('/recommended', authenticateToken, isAdmin, async (req, res) => {
  try {
    const { video_id, position } = req.body;

    if (!video_id) {
      return res.status(400).json({
        success: false,
        message: 'معرف الفيديو مطلوب'
      });
    }

    // التحقق من وجود الفيديو
    const videoCheck = await pool.query(
      'SELECT id FROM videos WHERE id = $1',
      [video_id]
    );

    if (videoCheck.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'الفيديو غير موجود'
      });
    }

    // إضافة أو تحديث
    await pool.query(`
      INSERT INTO recommended_videos (video_id, position, is_active)
      VALUES ($1, $2, TRUE)
      ON CONFLICT (video_id) 
      DO UPDATE SET position = $2, is_active = TRUE
    `, [video_id, position || 0]);

    res.json({
      success: true,
      message: 'تم إضافة الفيديو للقائمة المقترحة بنجاح'
    });

  } catch (error) {
    console.error('Add recommended video error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في إضافة الفيديو'
    });
  }
});

// حذف فيديو من القائمة المقترحة (Admin)
router.delete('/recommended/:videoId', authenticateToken, isAdmin, async (req, res) => {
  try {
    const videoId = req.params.videoId;

    await pool.query(
      'DELETE FROM recommended_videos WHERE video_id = $1',
      [videoId]
    );

    res.json({
      success: true,
      message: 'تم حذف الفيديو من القائمة المقترحة'
    });

  } catch (error) {
    console.error('Remove recommended video error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في حذف الفيديو'
    });
  }
});

// Duplicate legacy settings/stats handlers removed.
// ===== Withdrawal Management =====

// جلب جميع طلبات السحب
router.get('/withdrawals', authenticateAdmin, async (req, res) => {
  try {
    const { status, page = 1, limit = 20 } = req.query;
    const offset = (page - 1) * limit;
    
    let query = `
      SELECT wr.*, u.username, u.email,
             uw.balance as user_balance
      FROM withdrawal_requests wr
      JOIN users u ON wr.user_id = u.id
      LEFT JOIN user_wallets uw ON wr.user_id = uw.user_id
      WHERE 1=1
    `;
    const params = [];
    let paramCount = 1;
    
    if (status) {
      query += ` AND wr.status = $${paramCount}`;
      params.push(status);
      paramCount++;
    }
    
    query += ` ORDER BY wr.created_at DESC LIMIT $${paramCount} OFFSET $${paramCount + 1}`;
    params.push(limit, offset);
    
    const result = await pool.query(query, params);
    
    // إحصائيات
    const statsResult = await pool.query(`
      SELECT 
        COUNT(*) FILTER (WHERE status = 'pending') as pending_count,
        COUNT(*) FILTER (WHERE status = 'approved') as approved_count,
        COUNT(*) FILTER (WHERE status = 'rejected') as rejected_count,
        COALESCE(SUM(amount) FILTER (WHERE status = 'pending'), 0) as pending_amount,
        COALESCE(SUM(amount) FILTER (WHERE status = 'approved'), 0) as total_paid
      FROM withdrawal_requests
    `);
    
    res.json({
      success: true,
      data: {
        requests: result.rows,
        stats: statsResult.rows[0]
      }
    });
  } catch (error) {
    console.error('Get withdrawals error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب طلبات السحب' });
  }
});

// الموافقة على طلب سحب
router.post('/withdrawals/:id/approve', authenticateAdmin, async (req, res) => {
  try {
    const { id } = req.params;
    const { admin_notes } = req.body;
    
    // جلب الطلب
    const requestResult = await pool.query(
      'SELECT * FROM withdrawal_requests WHERE id = $1 AND status = $2',
      [id, 'pending']
    );
    
    if (requestResult.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'الطلب غير موجود أو تمت معالجته' });
    }
    
    const request = requestResult.rows[0];
    
    // تحديث الطلب
    await pool.query(`
      UPDATE withdrawal_requests 
      SET status = 'approved', admin_notes = $1, processed_by = $2, processed_at = CURRENT_TIMESTAMP
      WHERE id = $3
    `, [admin_notes, req.admin.id, id]);
    
    // تحديث محفظة المستخدم
    await pool.query(`
      UPDATE user_wallets 
      SET balance = balance - $1, 
          total_withdrawn = total_withdrawn + $1,
          pending_withdrawal = pending_withdrawal - $1,
          updated_at = CURRENT_TIMESTAMP
      WHERE user_id = $2
    `, [request.amount, request.user_id]);
    
    // إضافة معاملة
    const walletResult = await pool.query(
      'SELECT balance FROM user_wallets WHERE user_id = $1',
      [request.user_id]
    );
    
    await pool.query(`
      INSERT INTO wallet_transactions (user_id, type, amount, balance_after, description, reference_id, reference_type)
      VALUES ($1, 'withdrawal', $2, $3, $4, $5, 'withdrawal')
    `, [request.user_id, -request.amount, walletResult.rows[0].balance, 'سحب أرباح', id]);
    
    res.json({ success: true, message: 'تمت الموافقة على طلب السحب' });
  } catch (error) {
    console.error('Approve withdrawal error:', error);
    res.status(500).json({ success: false, message: 'خطأ في الموافقة على الطلب' });
  }
});

// رفض طلب سحب
router.post('/withdrawals/:id/reject', authenticateAdmin, async (req, res) => {
  try {
    const { id } = req.params;
    const { admin_notes } = req.body;
    
    // جلب الطلب
    const requestResult = await pool.query(
      'SELECT * FROM withdrawal_requests WHERE id = $1 AND status = $2',
      [id, 'pending']
    );
    
    if (requestResult.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'الطلب غير موجود أو تمت معالجته' });
    }
    
    const request = requestResult.rows[0];
    
    // تحديث الطلب
    await pool.query(`
      UPDATE withdrawal_requests 
      SET status = 'rejected', admin_notes = $1, processed_by = $2, processed_at = CURRENT_TIMESTAMP
      WHERE id = $3
    `, [admin_notes, req.admin.id, id]);
    
    // إرجاع المبلغ من pending
    await pool.query(`
      UPDATE user_wallets 
      SET pending_withdrawal = pending_withdrawal - $1,
          updated_at = CURRENT_TIMESTAMP
      WHERE user_id = $2
    `, [request.amount, request.user_id]);
    
    res.json({ success: true, message: 'تم رفض طلب السحب' });
  } catch (error) {
    console.error('Reject withdrawal error:', error);
    res.status(500).json({ success: false, message: 'خطأ في رفض الطلب' });
  }
});

// ===== Financial Settings =====

// جلب الإعدادات المالية
router.get('/financial-settings', authenticateAdmin, async (req, res) => {
  try {
    const result = await pool.query(`
      SELECT setting_key, setting_value, setting_type, description
      FROM system_settings
      WHERE setting_key IN (
        'earning_per_1000_views', 'referral_percentage', 'min_withdrawal_amount',
        'gate_ad_enabled', 'gate_ad_duration', 'view_cooldown_hours',
        'weekly_subscription_price', 'monthly_subscription_price'
      )
    `);
    
    const settings = {};
    result.rows.forEach(row => {
      let value = row.setting_value;
      if (row.setting_type === 'decimal') value = parseFloat(value);
      else if (row.setting_type === 'integer') value = parseInt(value);
      else if (row.setting_type === 'boolean') value = value === 'true';
      settings[row.setting_key] = { value, description: row.description };
    });
    
    res.json({ success: true, data: { settings } });
  } catch (error) {
    console.error('Get financial settings error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب الإعدادات' });
  }
});

// تحديث الإعدادات المالية
router.put('/financial-settings', authenticateAdmin, async (req, res) => {
  try {
    const { settings } = req.body;
    
    const allowedKeys = [
      'earning_per_1000_views', 'referral_percentage', 'min_withdrawal_amount',
      'gate_ad_enabled', 'gate_ad_duration', 'view_cooldown_hours',
      'weekly_subscription_price', 'monthly_subscription_price'
    ];
    
    for (const [key, value] of Object.entries(settings)) {
      if (!allowedKeys.includes(key)) continue;
      
      const type = typeof value === 'boolean' ? 'boolean' : 
                   Number.isInteger(value) ? 'integer' : 'decimal';
      
      await pool.query(`
        UPDATE system_settings 
        SET setting_value = $1, setting_type = $2, updated_at = CURRENT_TIMESTAMP
        WHERE setting_key = $3
      `, [String(value), type, key]);
    }
    
    res.json({ success: true, message: 'تم تحديث الإعدادات المالية' });
  } catch (error) {
    console.error('Update financial settings error:', error);
    res.status(500).json({ success: false, message: 'خطأ في تحديث الإعدادات' });
  }
});

// ===== User Wallets Management =====

// جلب محافظ المستخدمين
router.get('/user-wallets', authenticateAdmin, async (req, res) => {
  try {
    const { page = 1, limit = 20, search } = req.query;
    const offset = (page - 1) * limit;
    
    let query = `
      SELECT uw.*, u.username, u.email,
             (SELECT COUNT(*) FROM videos WHERE user_id = u.id) as videos_count
      FROM user_wallets uw
      JOIN users u ON uw.user_id = u.id
      WHERE 1=1
    `;
    const params = [];
    let paramCount = 1;
    
    if (search) {
      query += ` AND (u.username ILIKE $${paramCount} OR u.email ILIKE $${paramCount})`;
      params.push(`%${search}%`);
      paramCount++;
    }
    
    query += ` ORDER BY uw.balance DESC LIMIT $${paramCount} OFFSET $${paramCount + 1}`;
    params.push(limit, offset);
    
    const result = await pool.query(query, params);
    
    // إحصائيات عامة
    const statsResult = await pool.query(`
      SELECT 
        COALESCE(SUM(balance), 0) as total_balance,
        COALESCE(SUM(total_earned), 0) as total_earned,
        COALESCE(SUM(total_withdrawn), 0) as total_withdrawn
      FROM user_wallets
    `);
    
    res.json({
      success: true,
      data: {
        wallets: result.rows,
        stats: statsResult.rows[0]
      }
    });
  } catch (error) {
    console.error('Get user wallets error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب المحافظ' });
  }
});

// جلب تفاصيل محفظة مستخدم
router.get('/user-wallets/:userId', authenticateAdmin, async (req, res) => {
  try {
    const { userId } = req.params;
    
    const walletResult = await pool.query(`
      SELECT uw.*, u.username, u.email
      FROM user_wallets uw
      JOIN users u ON uw.user_id = u.id
      WHERE uw.user_id = $1
    `, [userId]);
    
    if (walletResult.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'المحفظة غير موجودة' });
    }
    
    const transactionsResult = await pool.query(`
      SELECT * FROM wallet_transactions 
      WHERE user_id = $1 
      ORDER BY created_at DESC 
      LIMIT 50
    `, [userId]);
    
    res.json({
      success: true,
      data: {
        wallet: walletResult.rows[0],
        transactions: transactionsResult.rows
      }
    });
  } catch (error) {
    console.error('Get user wallet error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب المحفظة' });
  }
});

// ===== Referrals Management =====

// جلب إحصائيات الإحالات
router.get('/referrals-stats', authenticateAdmin, async (req, res) => {
  try {
    const statsResult = await pool.query(`
      SELECT 
        COUNT(DISTINCT referrer_id) as total_referrers,
        COUNT(*) as total_referrals,
        COALESCE(SUM(total_earnings), 0) as total_referral_earnings
      FROM referrals
    `);
    
    const topReferrersResult = await pool.query(`
      SELECT u.id, u.username, u.email,
             COUNT(r.id) as referrals_count,
             COALESCE(SUM(r.total_earnings), 0) as total_earnings
      FROM users u
      JOIN referrals r ON u.id = r.referrer_id
      GROUP BY u.id
      ORDER BY referrals_count DESC
      LIMIT 10
    `);
    
    res.json({
      success: true,
      data: {
        stats: statsResult.rows[0],
        top_referrers: topReferrersResult.rows
      }
    });
  } catch (error) {
    console.error('Get referrals stats error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب إحصائيات الإحالات' });
  }
});

// ===== Earnings Overview =====

// جلب نظرة عامة على الأرباح
router.get('/earnings-overview', authenticateAdmin, async (req, res) => {
  try {
    // إجمالي الأرباح الموزعة
    const totalEarningsResult = await pool.query(`
      SELECT 
        COALESCE(SUM(CASE WHEN type = 'view_earning' THEN amount ELSE 0 END), 0) as view_earnings,
        COALESCE(SUM(CASE WHEN type = 'referral_earning' THEN amount ELSE 0 END), 0) as referral_earnings
      FROM wallet_transactions
      WHERE amount > 0
    `);
    
    // أرباح اليوم
    const todayResult = await pool.query(`
      SELECT COALESCE(SUM(amount), 0) as amount
      FROM wallet_transactions
      WHERE type IN ('view_earning', 'referral_earning')
      AND created_at >= CURRENT_DATE
    `);
    
    // أرباح الأسبوع
    const weekResult = await pool.query(`
      SELECT COALESCE(SUM(amount), 0) as amount
      FROM wallet_transactions
      WHERE type IN ('view_earning', 'referral_earning')
      AND created_at >= CURRENT_DATE - INTERVAL '7 days'
    `);
    
    // المشاهدات المدفوعة
    const paidViewsResult = await pool.query(`
      SELECT COALESCE(SUM(paid_views_count), 0) as total
      FROM videos
    `);
    
    res.json({
      success: true,
      data: {
        total_view_earnings: parseFloat(totalEarningsResult.rows[0].view_earnings),
        total_referral_earnings: parseFloat(totalEarningsResult.rows[0].referral_earnings),
        today_earnings: parseFloat(todayResult.rows[0].amount),
        week_earnings: parseFloat(weekResult.rows[0].amount),
        total_paid_views: parseInt(paidViewsResult.rows[0].total)
      }
    });
  } catch (error) {
    console.error('Get earnings overview error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب نظرة الأرباح' });
  }
});

// ==================== إدارة أسعار CPM حسب الدولة ====================

// جلب جميع أسعار CPM
router.get('/cpm-rates', authenticateAdmin, async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT * FROM country_cpm_rates ORDER BY country_name ASC'
    );
    
    res.json({
      success: true,
      data: { rates: result.rows }
    });
  } catch (error) {
    console.error('Get CPM rates error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب الأسعار' });
  }
});

// إضافة أو تحديث سعر CPM لدولة
router.post('/cpm-rates', authenticateAdmin, async (req, res) => {
  try {
    const { country_code, country_name, country_name_ar, cpm_rate } = req.body;
    
    if (!country_code || !country_name || cpm_rate === undefined) {
      return res.status(400).json({ success: false, message: 'البيانات غير مكتملة' });
    }
    
    const result = await pool.query(`
      INSERT INTO country_cpm_rates (country_code, country_name, country_name_ar, cpm_rate)
      VALUES ($1, $2, $3, $4)
      ON CONFLICT (country_code) DO UPDATE SET
        country_name = EXCLUDED.country_name,
        country_name_ar = EXCLUDED.country_name_ar,
        cpm_rate = EXCLUDED.cpm_rate,
        updated_at = CURRENT_TIMESTAMP
      RETURNING *
    `, [country_code.toUpperCase(), country_name, country_name_ar || null, parseFloat(cpm_rate)]);
    
    res.json({
      success: true,
      message: 'تم حفظ السعر بنجاح',
      data: { rate: result.rows[0] }
    });
  } catch (error) {
    console.error('Save CPM rate error:', error);
    res.status(500).json({ success: false, message: 'خطأ في حفظ السعر' });
  }
});

// تحديث سعر CPM لدولة
router.put('/cpm-rates/:id', authenticateAdmin, async (req, res) => {
  try {
    const { id } = req.params;
    const { cpm_rate, is_active } = req.body;
    
    const updates = [];
    const values = [];
    let idx = 1;
    
    if (cpm_rate !== undefined) {
      updates.push(`cpm_rate = $${idx++}`);
      values.push(parseFloat(cpm_rate));
    }
    if (is_active !== undefined) {
      updates.push(`is_active = $${idx++}`);
      values.push(is_active);
    }
    updates.push(`updated_at = CURRENT_TIMESTAMP`);
    
    values.push(parseInt(id));
    
    const result = await pool.query(
      `UPDATE country_cpm_rates SET ${updates.join(', ')} WHERE id = $${idx} RETURNING *`,
      values
    );
    
    if (result.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'الدولة غير موجودة' });
    }
    
    res.json({
      success: true,
      message: 'تم تحديث السعر',
      data: { rate: result.rows[0] }
    });
  } catch (error) {
    console.error('Update CPM rate error:', error);
    res.status(500).json({ success: false, message: 'خطأ في تحديث السعر' });
  }
});

// حذف سعر CPM لدولة
router.delete('/cpm-rates/:id', authenticateAdmin, async (req, res) => {
  try {
    const { id } = req.params;
    await pool.query('DELETE FROM country_cpm_rates WHERE id = $1', [parseInt(id)]);
    res.json({ success: true, message: 'تم حذف الدولة' });
  } catch (error) {
    console.error('Delete CPM rate error:', error);
    res.status(500).json({ success: false, message: 'خطأ في الحذف' });
  }
});

// ===== Files Management =====

// إحصائيات الملفات
router.get('/files/stats', authenticateAdmin, async (req, res) => {
  try {
    const stats = await pool.query(`
      SELECT 
        (SELECT COUNT(*) FROM files) as total_files,
        (SELECT COUNT(*) FROM folders) as total_folders,
        (SELECT COALESCE(SUM(file_size), 0) FROM files) as total_storage,
        (SELECT COALESCE(SUM(downloads_count), 0) FROM files) as total_downloads
    `);
    
    res.json({
      success: true,
      data: stats.rows[0]
    });
  } catch (error) {
    console.error('Files stats error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب الإحصائيات' });
  }
});

// جلب جميع الملفات مع pagination
router.get('/files', authenticateAdmin, async (req, res) => {
  try {
    const { 
      user_id, type, search, 
      page = 1, limit = 20, 
      sort = 'created_at', order = 'desc',
      date_from, date_to 
    } = req.query;
    
    const offset = (parseInt(page) - 1) * parseInt(limit);
    const validSorts = ['created_at', 'file_size', 'views_count', 'downloads_count', 'name'];
    const sortColumn = validSorts.includes(sort) ? sort : 'created_at';
    const sortOrder = order === 'asc' ? 'ASC' : 'DESC';
    
    let whereClause = '1=1';
    const params = [];
    let idx = 1;
    
    if (user_id) {
      whereClause += ` AND f.user_id = $${idx++}`;
      params.push(parseInt(user_id));
    }
    
    if (type && type !== 'all') {
      whereClause += ` AND f.file_type = $${idx++}`;
      params.push(type);
    }
    
    if (search) {
      whereClause += ` AND f.name ILIKE $${idx++}`;
      params.push(`%${search}%`);
    }
    
    if (date_from) {
      whereClause += ` AND f.created_at >= $${idx++}`;
      params.push(date_from);
    }
    
    if (date_to) {
      whereClause += ` AND f.created_at <= $${idx++}`;
      params.push(date_to + ' 23:59:59');
    }
    
    // Count total
    const countResult = await pool.query(
      `SELECT COUNT(*) FROM files f WHERE ${whereClause}`,
      params
    );
    const total = parseInt(countResult.rows[0].count);
    
    // Get files with pagination
    const filesQuery = `
      SELECT f.*, u.username as owner_name, u.email as owner_email, fld.name as folder_name
      FROM files f
      LEFT JOIN users u ON f.user_id = u.id
      LEFT JOIN folders fld ON f.folder_id = fld.id
      WHERE ${whereClause}
      ORDER BY f.${sortColumn} ${sortOrder}
      LIMIT $${idx++} OFFSET $${idx++}
    `;
    params.push(parseInt(limit), offset);
    
    const filesResult = await pool.query(filesQuery, params);
    
    res.json({
      success: true,
      data: {
        files: filesResult.rows,
        total,
        page: parseInt(page),
        limit: parseInt(limit),
        pages: Math.ceil(total / parseInt(limit))
      }
    });
  } catch (error) {
    console.error('Get files error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب الملفات' });
  }
});

// جلب جميع المجلدات مع pagination
router.get('/folders', authenticateAdmin, async (req, res) => {
  try {
    const { 
      user_id, search, 
      page = 1, limit = 20, 
      sort = 'created_at', order = 'desc',
      date_from, date_to 
    } = req.query;
    
    const offset = (parseInt(page) - 1) * parseInt(limit);
    const validSorts = ['created_at', 'name'];
    const sortColumn = validSorts.includes(sort) ? sort : 'created_at';
    const sortOrder = order === 'asc' ? 'ASC' : 'DESC';
    
    let whereClause = '1=1';
    const params = [];
    let idx = 1;
    
    if (user_id) {
      whereClause += ` AND fld.user_id = $${idx++}`;
      params.push(parseInt(user_id));
    }
    
    if (search) {
      whereClause += ` AND fld.name ILIKE $${idx++}`;
      params.push(`%${search}%`);
    }
    
    if (date_from) {
      whereClause += ` AND fld.created_at >= $${idx++}`;
      params.push(date_from);
    }
    
    if (date_to) {
      whereClause += ` AND fld.created_at <= $${idx++}`;
      params.push(date_to + ' 23:59:59');
    }
    
    // Count total
    const countResult = await pool.query(
      `SELECT COUNT(*) FROM folders fld WHERE ${whereClause}`,
      params
    );
    const total = parseInt(countResult.rows[0].count);
    
    // Get folders with pagination
    const foldersQuery = `
      SELECT fld.*, u.username as owner_name, u.email as owner_email,
        parent.name as parent_name,
        (SELECT COUNT(*) FROM files WHERE folder_id = fld.id) as files_count,
        (SELECT COALESCE(SUM(file_size), 0) FROM files WHERE folder_id = fld.id) as size
      FROM folders fld
      LEFT JOIN users u ON fld.user_id = u.id
      LEFT JOIN folders parent ON fld.parent_id = parent.id
      WHERE ${whereClause}
      ORDER BY fld.${sortColumn} ${sortOrder}
      LIMIT $${idx++} OFFSET $${idx++}
    `;
    params.push(parseInt(limit), offset);
    
    const foldersResult = await pool.query(foldersQuery, params);
    
    res.json({
      success: true,
      data: {
        folders: foldersResult.rows,
        total,
        page: parseInt(page),
        limit: parseInt(limit),
        pages: Math.ceil(total / parseInt(limit))
      }
    });
  } catch (error) {
    console.error('Get folders error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب المجلدات' });
  }
});

// حذف ملف (Admin)
router.delete('/files/:id', authenticateAdmin, async (req, res) => {
  try {
    const { id } = req.params;
    
    // Get file info first
    const fileResult = await pool.query('SELECT * FROM files WHERE id = $1', [parseInt(id)]);
    if (fileResult.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'الملف غير موجود' });
    }
    
    const file = fileResult.rows[0];
    
    // Delete physical file
    const filePath = path.join(__dirname, '..', file.file_path);
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
    }
    
    // Update user storage
    await pool.query(
      'UPDATE users SET storage_used = GREATEST(0, storage_used - $1) WHERE id = $2',
      [file.file_size, file.user_id]
    );
    
    // Delete from database
    await pool.query('DELETE FROM files WHERE id = $1', [parseInt(id)]);
    
    res.json({ success: true, message: 'تم حذف الملف' });
  } catch (error) {
    console.error('Delete file error:', error);
    res.status(500).json({ success: false, message: 'خطأ في حذف الملف' });
  }
});

// حذف مجلد (Admin)
router.delete('/folders/:id', authenticateAdmin, async (req, res) => {
  try {
    const { id } = req.params;
    
    // Get all files in folder recursively
    const filesResult = await pool.query(
      'SELECT * FROM files WHERE folder_id = $1',
      [parseInt(id)]
    );
    
    // Delete physical files
    for (const file of filesResult.rows) {
      const filePath = path.join(__dirname, '..', file.file_path);
      if (fs.existsSync(filePath)) {
        fs.unlinkSync(filePath);
      }
      
      // Update user storage
      await pool.query(
        'UPDATE users SET storage_used = GREATEST(0, storage_used - $1) WHERE id = $2',
        [file.file_size, file.user_id]
      );
    }
    
    // Delete files from database
    await pool.query('DELETE FROM files WHERE folder_id = $1', [parseInt(id)]);
    
    // Delete sub-folders recursively
    await pool.query('DELETE FROM folders WHERE parent_id = $1', [parseInt(id)]);
    
    // Delete folder
    await pool.query('DELETE FROM folders WHERE id = $1', [parseInt(id)]);
    
    res.json({ success: true, message: 'تم حذف المجلد' });
  } catch (error) {
    console.error('Delete folder error:', error);
    res.status(500).json({ success: false, message: 'خطأ في حذف المجلد' });
  }
});

module.exports = router;

