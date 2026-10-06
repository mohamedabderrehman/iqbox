const express = require('express');
const router = express.Router();
const pool = require('../config/database');
const authenticateToken = require('../middleware/auth');
const crypto = require('crypto');

// ========================================
// دوال مساعدة
// ========================================

// توليد كود إحالة فريد
function generateReferralCode() {
  return 'IQ' + crypto.randomBytes(4).toString('hex').toUpperCase();
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

// جلب كود الإحالة الخاص بالمستخدم
router.get('/my-code', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    
    // جلب أو إنشاء كود الإحالة
    let result = await pool.query(
      'SELECT referral_code FROM users WHERE id = $1',
      [userId]
    );
    
    let referralCode = result.rows[0]?.referral_code;
    
    if (!referralCode) {
      // إنشاء كود جديد
      referralCode = generateReferralCode();
      await pool.query(
        'UPDATE users SET referral_code = $1 WHERE id = $2',
        [referralCode, userId]
      );
    }
    
    // جلب نسبة الإحالة
    const referralPercentage = await getSystemSetting('referral_percentage');
    
    res.json({
      success: true,
      data: {
        referral_code: referralCode,
        referral_link: `https://iqbox.app/register?ref=${referralCode}`,
        referral_percentage: referralPercentage
      }
    });
  } catch (error) {
    console.error('Get referral code error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب كود الإحالة' });
  }
});

// جلب إحصائيات الإحالات
router.get('/stats', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    
    // عدد المستخدمين المُحالين
    const referralsResult = await pool.query(`
      SELECT COUNT(*) as total_referrals
      FROM referrals 
      WHERE referrer_id = $1
    `, [userId]);
    
    // إجمالي أرباح الإحالات
    const earningsResult = await pool.query(`
      SELECT COALESCE(SUM(referrer_earning), 0) as total_earnings
      FROM referral_earnings 
      WHERE referrer_id = $1
    `, [userId]);
    
    // أرباح هذا الشهر
    const monthEarningsResult = await pool.query(`
      SELECT COALESCE(SUM(referrer_earning), 0) as month_earnings
      FROM referral_earnings 
      WHERE referrer_id = $1
      AND created_at >= CURRENT_DATE - INTERVAL '30 days'
    `, [userId]);
    
    // نسبة الإحالة الحالية
    const referralPercentage = await getSystemSetting('referral_percentage');
    
    res.json({
      success: true,
      data: {
        total_referrals: parseInt(referralsResult.rows[0].total_referrals),
        total_earnings: parseFloat(earningsResult.rows[0].total_earnings),
        month_earnings: parseFloat(monthEarningsResult.rows[0].month_earnings),
        referral_percentage: referralPercentage
      }
    });
  } catch (error) {
    console.error('Get referral stats error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب إحصائيات الإحالات' });
  }
});

// جلب قائمة المستخدمين المُحالين
router.get('/referred-users', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const { page = 1, limit = 20 } = req.query;
    const offset = (page - 1) * limit;
    
    const result = await pool.query(`
      SELECT 
        r.id,
        u.username,
        u.created_at as joined_at,
        r.total_earnings,
        r.status
      FROM referrals r
      JOIN users u ON r.referred_id = u.id
      WHERE r.referrer_id = $1
      ORDER BY r.created_at DESC
      LIMIT $2 OFFSET $3
    `, [userId, limit, offset]);
    
    const countResult = await pool.query(
      'SELECT COUNT(*) FROM referrals WHERE referrer_id = $1',
      [userId]
    );
    
    res.json({
      success: true,
      data: {
        referred_users: result.rows,
        pagination: {
          page: parseInt(page),
          limit: parseInt(limit),
          total: parseInt(countResult.rows[0].count)
        }
      }
    });
  } catch (error) {
    console.error('Get referred users error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب المستخدمين المُحالين' });
  }
});

// جلب سجل أرباح الإحالات
router.get('/earnings', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const { page = 1, limit = 20 } = req.query;
    const offset = (page - 1) * limit;
    
    const result = await pool.query(`
      SELECT 
        re.id,
        re.source_earning,
        re.referrer_earning,
        re.created_at,
        u.username as referred_username
      FROM referral_earnings re
      JOIN users u ON re.referred_id = u.id
      WHERE re.referrer_id = $1
      ORDER BY re.created_at DESC
      LIMIT $2 OFFSET $3
    `, [userId, limit, offset]);
    
    res.json({
      success: true,
      data: { earnings: result.rows }
    });
  } catch (error) {
    console.error('Get referral earnings error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب أرباح الإحالات' });
  }
});

// التحقق من صحة كود الإحالة (للتسجيل)
router.get('/validate/:code', async (req, res) => {
  try {
    const { code } = req.params;
    
    const result = await pool.query(
      'SELECT id, username FROM users WHERE referral_code = $1',
      [code.toUpperCase()]
    );
    
    if (result.rows.length === 0) {
      return res.json({ success: false, valid: false, message: 'كود الإحالة غير صالح' });
    }
    
    res.json({
      success: true,
      valid: true,
      data: {
        referrer_username: result.rows[0].username
      }
    });
  } catch (error) {
    console.error('Validate referral code error:', error);
    res.status(500).json({ success: false, message: 'خطأ في التحقق من الكود' });
  }
});

module.exports = router;
