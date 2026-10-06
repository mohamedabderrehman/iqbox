const express = require('express');
const router = express.Router();
const pool = require('../config/database');
const authenticateToken = require('../middleware/auth');

// ========================================
// PUBLIC ENDPOINTS (للتطبيق)
// ========================================

// جلب جميع خطط الاشتراك النشطة
router.get('/plans', async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT id, name, name_ar, description, description_ar, price, currency, 
              duration_days, features, payment_instructions, payment_instructions_ar
       FROM subscription_plans 
       WHERE is_active = true 
       ORDER BY sort_order ASC, price ASC`
    );

    res.json({
      success: true,
      data: {
        plans: result.rows
      }
    });
  } catch (error) {
    console.error('Get plans error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في جلب خطط الاشتراك'
    });
  }
});

// جلب خطة معينة
router.get('/plans/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const result = await pool.query(
      `SELECT id, name, name_ar, description, description_ar, price, currency, 
              duration_days, features, payment_instructions, payment_instructions_ar
       FROM subscription_plans 
       WHERE id = $1 AND is_active = true`,
      [id]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'الخطة غير موجودة'
      });
    }

    res.json({
      success: true,
      data: {
        plan: result.rows[0]
      }
    });
  } catch (error) {
    console.error('Get plan error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في جلب الخطة'
    });
  }
});

// جلب حالة الاشتراك للمستخدم
router.get('/status', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;

    const result = await pool.query(
      'SELECT subscription_status, subscription_expiry FROM users WHERE id = $1',
      [userId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'المستخدم غير موجود'
      });
    }

    const user = result.rows[0];

    // التحقق من انتهاء الاشتراك
    if (user.subscription_status === 'premium' && user.subscription_expiry) {
      const expiryDate = new Date(user.subscription_expiry);
      const now = new Date();
      
      if (now > expiryDate) {
        await pool.query(
          'UPDATE users SET subscription_status = $1, subscription_expiry = NULL WHERE id = $2',
          ['free', userId]
        );
        user.subscription_status = 'free';
        user.subscription_expiry = null;
      }
    }

    res.json({
      success: true,
      data: {
        subscription_status: user.subscription_status,
        subscription_expiry: user.subscription_expiry,
        is_premium: user.subscription_status === 'premium'
      }
    });
  } catch (error) {
    console.error('Get subscription status error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في جلب حالة الاشتراك'
    });
  }
});

// إنشاء طلب دفع (المستخدم يختار خطة ويضغط "تم الدفع")
router.post('/request', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const { plan_id, payment_method, notes } = req.body;

    if (!plan_id) {
      return res.status(400).json({
        success: false,
        message: 'يرجى اختيار خطة'
      });
    }

    // التحقق من وجود الخطة
    const planResult = await pool.query(
      'SELECT id, price, currency FROM subscription_plans WHERE id = $1 AND is_active = true',
      [plan_id]
    );

    if (planResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'الخطة غير موجودة'
      });
    }

    const plan = planResult.rows[0];

    // التحقق من عدم وجود طلب معلق
    const pendingRequest = await pool.query(
      'SELECT id FROM payment_requests WHERE user_id = $1 AND status = $2',
      [userId, 'pending']
    );

    if (pendingRequest.rows.length > 0) {
      return res.status(400).json({
        success: false,
        message: 'لديك طلب دفع معلق بالفعل'
      });
    }

    // إنشاء طلب الدفع
    const result = await pool.query(
      `INSERT INTO payment_requests (user_id, plan_id, amount, currency, payment_method, user_notes)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING id, status, created_at`,
      [userId, plan_id, plan.price, plan.currency, payment_method || 'manual', notes || null]
    );

    // تسجيل النشاط (تجاهل الخطأ إذا الجدول غير موجود)
    try {
      await pool.query(
        'INSERT INTO user_activities (user_id, activity_type, activity_data) VALUES ($1, $2, $3)',
        [userId, 'payment_request', JSON.stringify({ plan_id, amount: plan.price })]
      );
    } catch (activityError) {
      console.log('Activity logging skipped:', activityError.message);
    }

    res.status(201).json({
      success: true,
      message: 'تم إرسال طلب الدفع بنجاح. سيتم مراجعته قريباً.',
      data: {
        request: result.rows[0]
      }
    });
  } catch (error) {
    console.error('Create payment request error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في إنشاء طلب الدفع'
    });
  }
});

// جلب طلبات الدفع للمستخدم
router.get('/requests', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;

    const result = await pool.query(
      `SELECT pr.id, pr.amount, pr.currency, pr.payment_method, pr.status, 
              pr.admin_notes, pr.created_at, pr.processed_at,
              sp.name as plan_name, sp.name_ar as plan_name_ar, sp.duration_days
       FROM payment_requests pr
       LEFT JOIN subscription_plans sp ON pr.plan_id = sp.id
       WHERE pr.user_id = $1
       ORDER BY pr.created_at DESC`,
      [userId]
    );

    res.json({
      success: true,
      data: {
        requests: result.rows
      }
    });
  } catch (error) {
    console.error('Get payment requests error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في جلب طلبات الدفع'
    });
  }
});

// إلغاء طلب دفع معلق
router.delete('/requests/:id', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const { id } = req.params;

    const result = await pool.query(
      'DELETE FROM payment_requests WHERE id = $1 AND user_id = $2 AND status = $3 RETURNING id',
      [id, userId, 'pending']
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'الطلب غير موجود أو لا يمكن إلغاؤه'
      });
    }

    res.json({
      success: true,
      message: 'تم إلغاء الطلب بنجاح'
    });
  } catch (error) {
    console.error('Cancel payment request error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في إلغاء الطلب'
    });
  }
});

module.exports = router;
