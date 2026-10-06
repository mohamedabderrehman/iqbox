const express = require('express');
const router = express.Router();
const pool = require('../config/database');
const crypto = require('crypto');
const http = require('http');

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

// إضافة أرباح للمستخدم
async function addEarning(userId, amount, type, description, referenceId = null, referenceType = null) {
  await ensureWallet(userId);
  
  // تحديث المحفظة
  await pool.query(`
    UPDATE user_wallets 
    SET balance = balance + $1, 
        total_earned = total_earned + $1,
        updated_at = CURRENT_TIMESTAMP
    WHERE user_id = $2
  `, [amount, userId]);
  
  // جلب الرصيد الجديد
  const walletResult = await pool.query(
    'SELECT balance FROM user_wallets WHERE user_id = $1',
    [userId]
  );
  
  // إضافة المعاملة
  await pool.query(`
    INSERT INTO wallet_transactions (user_id, type, amount, balance_after, description, reference_id, reference_type)
    VALUES ($1, $2, $3, $4, $5, $6, $7)
  `, [userId, type, amount, walletResult.rows[0].balance, description, referenceId, referenceType]);
  
  return walletResult.rows[0].balance;
}

// جلب كود الدولة من IP
async function getCountryFromIP(ip) {
  return new Promise((resolve) => {
    // تجاهل IPs المحلية
    if (ip === '127.0.0.1' || ip === '::1' || ip.startsWith('192.168.') || ip.startsWith('10.')) {
      resolve('UNKNOWN');
      return;
    }
    
    const url = `http://ip-api.com/json/${ip}?fields=countryCode`;
    const req = http.get(url, { timeout: 3000 }, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          const json = JSON.parse(data);
          resolve(json.countryCode || 'UNKNOWN');
        } catch (e) {
          resolve('UNKNOWN');
        }
      });
    });
    req.on('error', () => resolve('UNKNOWN'));
    req.on('timeout', () => { req.destroy(); resolve('UNKNOWN'); });
  });
}

// جلب سعر CPM حسب الدولة
async function getCpmRate(countryCode) {
  // البحث عن سعر مخصص للدولة
  if (countryCode && countryCode !== 'UNKNOWN') {
    const result = await pool.query(
      'SELECT cpm_rate FROM country_cpm_rates WHERE country_code = $1 AND is_active = true',
      [countryCode]
    );
    if (result.rows.length > 0) {
      return parseFloat(result.rows[0].cpm_rate);
    }
  }
  // استخدام السعر الافتراضي
  const defaultRate = await getSystemSetting('earning_per_1000_views');
  return defaultRate || 0.50;
}

// معالجة أرباح الإحالة
async function processReferralEarning(userId, sourceEarning) {
  // جلب نسبة الإحالة
  const referralPercentage = await getSystemSetting('referral_percentage') || 10;
  
  // التحقق من وجود إحالة
  const referralResult = await pool.query(`
    SELECT r.id, r.referrer_id 
    FROM referrals r 
    WHERE r.referred_id = $1 AND r.status = 'active'
  `, [userId]);
  
  if (referralResult.rows.length === 0) return;
  
  const referral = referralResult.rows[0];
  const referrerEarning = sourceEarning * (referralPercentage / 100);
  
  if (referrerEarning <= 0) return;
  
  // إضافة الربح للمُحيل
  await addEarning(
    referral.referrer_id, 
    referrerEarning, 
    'referral_earning',
    `أرباح إحالة من مستخدم`,
    referral.id,
    'referral'
  );
  
  // تسجيل أرباح الإحالة
  await pool.query(`
    INSERT INTO referral_earnings (referral_id, referrer_id, referred_id, source_earning, referrer_earning)
    VALUES ($1, $2, $3, $4, $5)
  `, [referral.id, referral.referrer_id, userId, sourceEarning, referrerEarning]);
  
  // تحديث إجمالي أرباح الإحالة
  await pool.query(`
    UPDATE referrals SET total_earnings = total_earnings + $1 WHERE id = $2
  `, [referrerEarning, referral.id]);
}

// ========================================
// API ENDPOINTS
// ========================================

// بدء جلسة Gate Ad (يُستدعى عند فتح رابط الفيديو)
router.post('/start', async (req, res) => {
  try {
    const { video_id, viewer_ip, fingerprint, user_id } = req.body;
    
    if (!video_id || !viewer_ip) {
      return res.status(400).json({ success: false, message: 'بيانات ناقصة' });
    }
    
    // التحقق من وجود الفيديو
    const videoResult = await pool.query(
      'SELECT id, user_id FROM videos WHERE id = $1 AND status = $2',
      [video_id, 'active']
    );
    
    if (videoResult.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'الفيديو غير موجود' });
    }
    
    const video = videoResult.rows[0];
    
    // التحقق إذا كان المستخدم Premium (يتجاوز الإعلان)
    if (user_id) {
      const userResult = await pool.query(
        'SELECT subscription_status, subscription_expiry FROM users WHERE id = $1',
        [user_id]
      );
      
      if (userResult.rows.length > 0) {
        const user = userResult.rows[0];
        const isPremium = user.subscription_status === 'premium' && 
                         (!user.subscription_expiry || new Date(user.subscription_expiry) > new Date());
        
        if (isPremium) {
          return res.json({
            success: true,
            data: {
              skip_ad: true,
              is_premium: true,
              video_id: video_id
            }
          });
        }
      }
    }
    
    // التحقق من Gate Ad مفعّل
    const gateAdEnabled = await getSystemSetting('gate_ad_enabled');
    if (!gateAdEnabled) {
      return res.json({
        success: true,
        data: {
          skip_ad: true,
          gate_ad_disabled: true,
          video_id: video_id
        }
      });
    }
    
    // التحقق من cooldown (منع التكرار)
    const cooldownHours = await getSystemSetting('view_cooldown_hours') || 24;
    const existingView = await pool.query(`
      SELECT id FROM gate_ad_views 
      WHERE video_id = $1 AND viewer_ip = $2 
      AND created_at > NOW() - INTERVAL '${cooldownHours} hours'
      AND view_counted = true
    `, [video_id, viewer_ip]);
    
    const canEarn = existingView.rows.length === 0;
    
    // إنشاء session token
    const sessionToken = crypto.randomBytes(32).toString('hex');
    
    // تسجيل بداية المشاهدة
    await pool.query(`
      INSERT INTO gate_ad_views (video_id, viewer_ip, viewer_fingerprint, user_id, session_token)
      VALUES ($1, $2, $3, $4, $5)
    `, [video_id, viewer_ip, fingerprint, user_id, sessionToken]);
    
    // جلب مدة الإعلان
    const adDuration = await getSystemSetting('gate_ad_duration') || 5;
    
    res.json({
      success: true,
      data: {
        skip_ad: false,
        session_token: sessionToken,
        ad_duration: adDuration,
        can_earn: canEarn,
        video_id: video_id
      }
    });
  } catch (error) {
    console.error('Gate ad start error:', error);
    res.status(500).json({ success: false, message: 'خطأ في بدء الجلسة' });
  }
});

// إكمال Gate Ad (يُستدعى بعد انتهاء الإعلان)
router.post('/complete', async (req, res) => {
  try {
    const { session_token } = req.body;
    
    if (!session_token) {
      return res.status(400).json({ success: false, message: 'session_token مطلوب' });
    }
    
    // جلب جلسة المشاهدة
    const sessionResult = await pool.query(`
      SELECT gav.*, v.user_id as video_owner_id
      FROM gate_ad_views gav
      JOIN videos v ON gav.video_id = v.id
      WHERE gav.session_token = $1 AND gav.ad_completed = false
    `, [session_token]);
    
    if (sessionResult.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'الجلسة غير موجودة أو منتهية' });
    }
    
    const session = sessionResult.rows[0];
    
    // التحقق من الوقت (يجب أن يمر وقت الإعلان على الأقل)
    const adDuration = await getSystemSetting('gate_ad_duration') || 5;
    const sessionStart = new Date(session.created_at);
    const now = new Date();
    const elapsedSeconds = (now - sessionStart) / 1000;
    
    if (elapsedSeconds < adDuration - 1) { // -1 للتسامح
      return res.status(400).json({ success: false, message: 'لم يكتمل وقت الإعلان' });
    }
    
    // تحديث الجلسة
    await pool.query(`
      UPDATE gate_ad_views 
      SET ad_completed = true, completed_at = CURRENT_TIMESTAMP
      WHERE id = $1
    `, [session.id]);
    
    // التحقق من cooldown للاحتساب
    const cooldownHours = await getSystemSetting('view_cooldown_hours') || 24;
    const existingView = await pool.query(`
      SELECT id FROM gate_ad_views 
      WHERE video_id = $1 AND viewer_ip = $2 
      AND id != $3
      AND created_at > NOW() - INTERVAL '${cooldownHours} hours'
      AND view_counted = true
    `, [session.video_id, session.viewer_ip, session.id]);
    
    let viewCounted = false;
    let earningAdded = false;
    
    if (existingView.rows.length === 0) {
      // احتساب المشاهدة
      viewCounted = true;
      
      await pool.query(`
        UPDATE gate_ad_views SET view_counted = true WHERE id = $1
      `, [session.id]);
      
      // زيادة عداد المشاهدات المدفوعة
      await pool.query(`
        UPDATE videos 
        SET views_count = views_count + 1, 
            paid_views_count = paid_views_count + 1 
        WHERE id = $1
      `, [session.video_id]);
      
      // حساب الأرباح حسب دولة المشاهد
      const countryCode = await getCountryFromIP(session.viewer_ip);
      const earningPer1000 = await getCpmRate(countryCode);
      const earningPerView = earningPer1000 / 1000;
      
      if (earningPerView > 0 && session.video_owner_id) {
        earningAdded = true;
        
        // إضافة الربح لصاحب الفيديو
        await addEarning(
          session.video_owner_id,
          earningPerView,
          'view_earning',
          `ربح من مشاهدة فيديو`,
          session.video_id,
          'video'
        );
        
        // تسجيل الربح
        await pool.query(`
          INSERT INTO view_earnings (user_id, video_id, views_count, earning_rate, amount)
          VALUES ($1, $2, 1, $3, $4)
        `, [session.video_owner_id, session.video_id, earningPer1000, earningPerView]);
        
        // معالجة أرباح الإحالة
        await processReferralEarning(session.video_owner_id, earningPerView);
        
        await pool.query(`
          UPDATE gate_ad_views SET earning_calculated = true WHERE id = $1
        `, [session.id]);
      }
    }
    
    res.json({
      success: true,
      data: {
        view_counted: viewCounted,
        earning_added: earningAdded,
        video_id: session.video_id
      }
    });
  } catch (error) {
    console.error('Gate ad complete error:', error);
    res.status(500).json({ success: false, message: 'خطأ في إكمال الجلسة' });
  }
});

// جلب إعدادات Gate Ad (للصفحة)
router.get('/settings', async (req, res) => {
  try {
    const enabled = await getSystemSetting('gate_ad_enabled');
    const duration = await getSystemSetting('gate_ad_duration');
    
    res.json({
      success: true,
      data: {
        enabled: enabled,
        duration: duration
      }
    });
  } catch (error) {
    console.error('Get gate ad settings error:', error);
    res.status(500).json({ success: false, message: 'خطأ في جلب الإعدادات' });
  }
});

module.exports = router;
