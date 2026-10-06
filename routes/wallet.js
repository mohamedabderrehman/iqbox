const express = require('express');
const router = express.Router();
const pool = require('../config/database');
const authenticateToken = require('../middleware/auth');

// ========================================
// دوال مساعدة
// ========================================

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

// إنشاء محفظة للمستخدم إذا لم تكن موجودة
async function ensureWallet(userId) {
  await pool.query(`
    INSERT INTO user_wallets (user_id, balance, total_earned, total_withdrawn)
    VALUES ($1, 0, 0, 0)
    ON CONFLICT (user_id) DO NOTHING
  `, [userId]);
}

// إضافة معاملة للمحفظة
async function addTransaction(userId, type, amount, description, referenceId = null, referenceType = null) {
  const walletResult = await pool.query(
    'SELECT balance FROM user_wallets WHERE user_id = $1',
    [userId]
  );
  const currentBalance = walletResult.rows[0]?.balance || 0;
  const newBalance = parseFloat(currentBalance) + parseFloat(amount);
  
  await pool.query(`
    INSERT INTO wallet_transactions (user_id, type, amount, balance_after, description, reference_id, reference_type)
    VALUES ($1, $2, $3, $4, $5, $6, $7)
  `, [userId, type, amount, newBalance, description, referenceId, referenceType]);
  
  return newBalance;
}

// ========================================
// API ENDPOINTS
// ========================================

// جلب بيانات المحفظة
router.get('/', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    await ensureWallet(userId);
    
    const walletResult = await pool.query(
      'SELECT * FROM user_wallets WHERE user_id = $1',
      [userId]
    );
    
    const wallet = walletResult.rows[0];
    
    // جلب آخر المعاملات
    const transactionsResult = await pool.query(`
      SELECT * FROM wallet_transactions 
      WHERE user_id = $1 
      ORDER BY created_at DESC 
      LIMIT 20
    `, [userId]);
    
    // جلب إحصائيات الأرباح
    const earningsResult = await pool.query(`
      SELECT 
        COALESCE(SUM(CASE WHEN type = 'view_earning' THEN amount ELSE 0 END), 0) as views_earnings,
        COALESCE(SUM(CASE WHEN type = 'referral_earning' THEN amount ELSE 0 END), 0) as referral_earnings
      FROM wallet_transactions 
      WHERE user_id = $1
    `, [userId]);
    
    res.json({
      success: true,
      data: {
        wallet: {
          balance: parseFloat(wallet.balance),
          total_earned: parseFloat(wallet.total_earned),
          total_withdrawn: parseFloat(wallet.total_withdrawn),
          pending_withdrawal: parseFloat(wallet.pending_withdrawal)
        },
        earnings_breakdown: {
          views_earnings: parseFloat(earningsResult.rows[0].views_earnings),
          referral_earnings: parseFloat(earningsResult.rows[0].referral_earnings)
        },
        recent_transactions: transactionsResult.rows
      }
    });
  } catch (error) {
    console.error('Get wallet error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب بيانات المحفظة' });
  }
});

// جلب سجل المعاملات
router.get('/transactions', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const { page = 1, limit = 20, type } = req.query;
    const offset = (page - 1) * limit;
    
    let query = 'SELECT * FROM wallet_transactions WHERE user_id = $1';
    const params = [userId];
    
    if (type) {
      query += ' AND type = $2';
      params.push(type);
    }
    
    query += ' ORDER BY created_at DESC LIMIT $' + (params.length + 1) + ' OFFSET $' + (params.length + 2);
    params.push(limit, offset);
    
    const result = await pool.query(query, params);
    
    // عدد الإجمالي
    let countQuery = 'SELECT COUNT(*) FROM wallet_transactions WHERE user_id = $1';
    const countParams = [userId];
    if (type) {
      countQuery += ' AND type = $2';
      countParams.push(type);
    }
    const countResult = await pool.query(countQuery, countParams);
    
    res.json({
      success: true,
      data: {
        transactions: result.rows,
        pagination: {
          page: parseInt(page),
          limit: parseInt(limit),
          total: parseInt(countResult.rows[0].count)
        }
      }
    });
  } catch (error) {
    console.error('Get transactions error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب المعاملات' });
  }
});

// جلب إحصائيات الأرباح التفصيلية
router.get('/earnings', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    
    // أرباح اليوم
    const todayResult = await pool.query(`
      SELECT COALESCE(SUM(amount), 0) as amount
      FROM wallet_transactions 
      WHERE user_id = $1 
      AND type IN ('view_earning', 'referral_earning')
      AND created_at >= CURRENT_DATE
    `, [userId]);
    
    // أرباح الأسبوع
    const weekResult = await pool.query(`
      SELECT COALESCE(SUM(amount), 0) as amount
      FROM wallet_transactions 
      WHERE user_id = $1 
      AND type IN ('view_earning', 'referral_earning')
      AND created_at >= CURRENT_DATE - INTERVAL '7 days'
    `, [userId]);
    
    // أرباح الشهر
    const monthResult = await pool.query(`
      SELECT COALESCE(SUM(amount), 0) as amount
      FROM wallet_transactions 
      WHERE user_id = $1 
      AND type IN ('view_earning', 'referral_earning')
      AND created_at >= CURRENT_DATE - INTERVAL '30 days'
    `, [userId]);
    
    // أرباح حسب الفيديو
    const videoEarningsResult = await pool.query(`
      SELECT v.id, v.title, v.views_count, v.paid_views_count,
             COALESCE(SUM(ve.amount), 0) as total_earned
      FROM videos v
      LEFT JOIN view_earnings ve ON v.id = ve.video_id
      WHERE v.user_id = $1
      GROUP BY v.id
      ORDER BY total_earned DESC
      LIMIT 10
    `, [userId]);
    
    // الربح لكل 1000 مشاهدة الحالي
    const earningRate = await getSystemSetting('earning_per_1000_views');
    
    res.json({
      success: true,
      data: {
        today: parseFloat(todayResult.rows[0].amount),
        this_week: parseFloat(weekResult.rows[0].amount),
        this_month: parseFloat(monthResult.rows[0].amount),
        earning_rate_per_1000: earningRate,
        top_earning_videos: videoEarningsResult.rows
      }
    });
  } catch (error) {
    console.error('Get earnings error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب الأرباح' });
  }
});

// ========================================
// طلبات السحب
// ========================================

// جلب طرق السحب المتاحة
router.get('/withdrawal-methods', authenticateToken, async (req, res) => {
  try {
    const minAmount = (await getSystemSetting('min_withdrawal_amount')) || 5;
    
    res.json({
      success: true,
      data: {
        min_withdrawal_amount: minAmount,
        methods: [
          {
            id: 'qi_card',
            name: 'كي كارد',
            name_en: 'Qi Card',
            region: 'iraq',
            fields: [
              { key: 'card_number', label: 'رقم البطاقة', type: 'text' },
              { key: 'holder_name', label: 'اسم صاحب البطاقة', type: 'text' }
            ]
          },
          {
            id: 'master_card',
            name: 'ماستر كارد',
            name_en: 'Master Card',
            region: 'iraq',
            fields: [
              { key: 'card_number', label: 'رقم البطاقة', type: 'text' },
              { key: 'holder_name', label: 'اسم صاحب البطاقة', type: 'text' }
            ]
          },
          {
            id: 'usdt_trc20',
            name: 'USDT (TRC20)',
            name_en: 'USDT (TRC20)',
            region: 'international',
            fields: [
              { key: 'wallet_address', label: 'عنوان المحفظة (TRC20)', type: 'text' }
            ]
          }
        ]
      }
    });
  } catch (error) {
    console.error('Get withdrawal methods error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب طرق السحب' });
  }
});

// إنشاء طلب سحب
router.post('/withdraw', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const { amount, method, account_details } = req.body;
    
    if (!amount || !method || !account_details) {
      return res.status(400).json({ success: false, message: 'جميع الحقول مطلوبة' });
    }
    
    const minAmount = (await getSystemSetting('min_withdrawal_amount')) || 5;
    if (amount < minAmount) {
      return res.status(400).json({ 
        success: false, 
        message: `الحد الأدنى للسحب هو $${minAmount}` 
      });
    }
    
    // التحقق من الرصيد
    await ensureWallet(userId);
    const walletResult = await pool.query(
      'SELECT balance, pending_withdrawal FROM user_wallets WHERE user_id = $1',
      [userId]
    );
    
    const wallet = walletResult.rows[0];
    const availableBalance = parseFloat(wallet.balance) - parseFloat(wallet.pending_withdrawal);
    
    if (amount > availableBalance) {
      return res.status(400).json({ success: false, message: 'الرصيد غير كافي' });
    }
    
    // التحقق من عدم وجود طلب معلق
    const pendingResult = await pool.query(
      'SELECT id FROM withdrawal_requests WHERE user_id = $1 AND status = $2',
      [userId, 'pending']
    );
    
    if (pendingResult.rows.length > 0) {
      return res.status(400).json({ success: false, message: 'لديك طلب سحب معلق بالفعل' });
    }
    
    // إنشاء طلب السحب
    const result = await pool.query(`
      INSERT INTO withdrawal_requests (user_id, amount, withdrawal_method, account_details)
      VALUES ($1, $2, $3, $4)
      RETURNING id, status, created_at
    `, [userId, amount, method, JSON.stringify(account_details)]);
    
    // تحديث pending_withdrawal
    await pool.query(
      'UPDATE user_wallets SET pending_withdrawal = pending_withdrawal + $1 WHERE user_id = $2',
      [amount, userId]
    );
    
    res.status(201).json({
      success: true,
      message: 'تم إرسال طلب السحب بنجاح',
      data: { request: result.rows[0] }
    });
  } catch (error) {
    console.error('Create withdrawal error:', error);
    res.status(500).json({ success: false, message: 'خطأ في إنشاء طلب السحب' });
  }
});

// جلب طلبات السحب للمستخدم
router.get('/withdrawals', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    
    const result = await pool.query(`
      SELECT * FROM withdrawal_requests 
      WHERE user_id = $1 
      ORDER BY created_at DESC
    `, [userId]);
    
    res.json({
      success: true,
      data: { requests: result.rows }
    });
  } catch (error) {
    console.error('Get withdrawals error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب طلبات السحب' });
  }
});

// إلغاء طلب سحب معلق
router.delete('/withdrawals/:id', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const { id } = req.params;
    
    // جلب الطلب
    const requestResult = await pool.query(
      'SELECT amount FROM withdrawal_requests WHERE id = $1 AND user_id = $2 AND status = $3',
      [id, userId, 'pending']
    );
    
    if (requestResult.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'الطلب غير موجود أو لا يمكن إلغاؤه' });
    }
    
    const amount = requestResult.rows[0].amount;
    
    // حذف الطلب
    await pool.query('DELETE FROM withdrawal_requests WHERE id = $1', [id]);
    
    // إرجاع المبلغ من pending
    await pool.query(
      'UPDATE user_wallets SET pending_withdrawal = pending_withdrawal - $1 WHERE user_id = $2',
      [amount, userId]
    );
    
    res.json({ success: true, message: 'تم إلغاء الطلب بنجاح' });
  } catch (error) {
    console.error('Cancel withdrawal error:', error);
    res.status(500).json({ success: false, message: 'خطأ في إلغاء الطلب' });
  }
});

module.exports = router;
