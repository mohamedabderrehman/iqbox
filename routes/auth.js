const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const pool = require('../config/database');

// توليد كود إحالة فريد
function generateReferralCode() {
  return 'IQ' + crypto.randomBytes(4).toString('hex').toUpperCase();
}

// إنشاء محفظة للمستخدم
async function createWallet(userId) {
  await pool.query(`
    INSERT INTO user_wallets (user_id, balance, total_earned, total_withdrawn)
    VALUES ($1, 0, 0, 0)
    ON CONFLICT (user_id) DO NOTHING
  `, [userId]);
}

// تسجيل مستخدم جديد
router.post('/register', async (req, res) => {
  try {
    const { username, email, password, referral_code } = req.body;

    // التحقق من البيانات
    if (!username || !email || !password) {
      return res.status(400).json({
        success: false,
        message: 'جميع الحقول مطلوبة'
      });
    }

    if (password.length < 6) {
      return res.status(400).json({
        success: false,
        message: 'كلمة المرور يجب أن تكون 6 أحرف على الأقل'
      });
    }

    // التحقق من وجود المستخدم
    const userCheck = await pool.query(
      'SELECT id FROM users WHERE email = $1 OR username = $2',
      [email, username]
    );

    if (userCheck.rows.length > 0) {
      return res.status(400).json({
        success: false,
        message: 'البريد الإلكتروني أو اسم المستخدم موجود بالفعل'
      });
    }

    // التحقق من كود الإحالة إذا موجود
    let referrerId = null;
    if (referral_code) {
      const referrerResult = await pool.query(
        'SELECT id FROM users WHERE referral_code = $1',
        [referral_code.toUpperCase()]
      );
      if (referrerResult.rows.length > 0) {
        referrerId = referrerResult.rows[0].id;
      }
    }

    // تشفير كلمة المرور
    const salt = await bcrypt.genSalt(10);
    const passwordHash = await bcrypt.hash(password, salt);

    // توليد كود إحالة للمستخدم الجديد
    const newReferralCode = generateReferralCode();

    // إدخال المستخدم في قاعدة البيانات
    const result = await pool.query(
      `INSERT INTO users (username, email, password_hash, referral_code, referred_by) 
       VALUES ($1, $2, $3, $4, $5) 
       RETURNING id, username, email, subscription_status, referral_code, created_at`,
      [username, email, passwordHash, newReferralCode, referrerId]
    );

    const user = result.rows[0];

    // إنشاء محفظة للمستخدم
    await createWallet(user.id);

    // تسجيل الإحالة إذا موجودة
    if (referrerId) {
      await pool.query(`
        INSERT INTO referrals (referrer_id, referred_id, referral_code)
        VALUES ($1, $2, $3)
      `, [referrerId, user.id, referral_code.toUpperCase()]);
    }

    // إنشاء JWT Token
    const token = jwt.sign(
      { id: user.id, email: user.email },
      process.env.JWT_SECRET,
      { expiresIn: '30d' }
    );

    // تسجيل النشاط
    try {
      await pool.query(
        'INSERT INTO user_activities (user_id, activity_type, activity_data) VALUES ($1, $2, $3)',
        [user.id, 'register', JSON.stringify({ username, email, referred_by: referrerId })]
      );
    } catch (e) { /* تجاهل */ }

    res.status(201).json({
      success: true,
      message: 'تم التسجيل بنجاح',
      data: {
        user: {
          id: user.id,
          username: user.username,
          email: user.email,
          subscription_status: user.subscription_status,
          referral_code: user.referral_code
        },
        token
      }
    });

  } catch (error) {
    console.error('Register error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في السيرفر'
    });
  }
});

// تسجيل الدخول
router.post('/login', async (req, res) => {
  try {
    const { email, login, password } = req.body;
    
    // Support both 'login' (username or email) and legacy 'email' field
    const loginValue = login || email;

    // التحقق من البيانات
    if (!loginValue || !password) {
      return res.status(400).json({
        success: false,
        message: 'اسم المستخدم/البريد الإلكتروني وكلمة المرور مطلوبان'
      });
    }

    // البحث عن المستخدم بالبريد أو اسم المستخدم
    const result = await pool.query(
      'SELECT * FROM users WHERE email = $1 OR username = $1',
      [loginValue]
    );

    if (result.rows.length === 0) {
      return res.status(401).json({
        success: false,
        message: 'البريد الإلكتروني أو كلمة المرور غير صحيحة'
      });
    }

    const user = result.rows[0];

    // التحقق من كلمة المرور
    const isPasswordValid = await bcrypt.compare(password, user.password_hash);

    if (!isPasswordValid) {
      return res.status(401).json({
        success: false,
        message: 'البريد الإلكتروني أو كلمة المرور غير صحيحة'
      });
    }

    // تحديث آخر تسجيل دخول
    await pool.query(
      'UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = $1',
      [user.id]
    );

    // إنشاء JWT Token
    const token = jwt.sign(
      { id: user.id, email: user.email },
      process.env.JWT_SECRET,
      { expiresIn: '30d' }
    );

    // تسجيل النشاط
    await pool.query(
      'INSERT INTO user_activities (user_id, activity_type, activity_data) VALUES ($1, $2, $3)',
      [user.id, 'login', JSON.stringify({ timestamp: new Date() })]
    );

    res.json({
      success: true,
      message: 'تم تسجيل الدخول بنجاح',
      data: {
        user: {
          id: user.id,
          username: user.username,
          email: user.email,
          subscription_status: user.subscription_status,
          subscription_expiry: user.subscription_expiry
        },
        token
      }
    });

  } catch (error) {
    console.error('Login error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في السيرفر'
    });
  }
});

// تحديث FCM Token
router.post('/fcm-token', require('../middleware/auth'), async (req, res) => {
  try {
    const { fcm_token } = req.body;
    const userId = req.user.id;

    await pool.query(
      'UPDATE users SET fcm_token = $1 WHERE id = $2',
      [fcm_token, userId]
    );

    res.json({
      success: true,
      message: 'تم تحديث FCM Token بنجاح'
    });

  } catch (error) {
    console.error('FCM Token update error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في السيرفر'
    });
  }
});

module.exports = router;
