const express = require('express');
const router = express.Router();
const pool = require('../config/database');
const crypto = require('crypto');
const http = require('http');

// ========================================
// دوال مساعدة
// ========================================

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

async function ensureWallet(userId) {
  await pool.query(`
    INSERT INTO user_wallets (user_id, balance, total_earned, total_withdrawn)
    VALUES ($1, 0, 0, 0)
    ON CONFLICT (user_id) DO NOTHING
  `, [userId]);
}

async function addEarning(userId, amount, type, description, referenceId = null, referenceType = null) {
  await ensureWallet(userId);
  
  await pool.query(`
    UPDATE user_wallets 
    SET balance = balance + $1, 
        total_earned = total_earned + $1,
        updated_at = CURRENT_TIMESTAMP
    WHERE user_id = $2
  `, [amount, userId]);
  
  const walletResult = await pool.query(
    'SELECT balance FROM user_wallets WHERE user_id = $1',
    [userId]
  );
  
  await pool.query(`
    INSERT INTO wallet_transactions (user_id, type, amount, balance_after, description, reference_id, reference_type)
    VALUES ($1, $2, $3, $4, $5, $6, $7)
  `, [userId, type, amount, walletResult.rows[0].balance, description, referenceId, referenceType]);
  
  return walletResult.rows[0].balance;
}

async function getCountryFromIP(ip) {
  return new Promise((resolve) => {
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

async function getCpmRate(countryCode) {
  if (countryCode && countryCode !== 'UNKNOWN') {
    const result = await pool.query(
      'SELECT cpm_rate FROM country_cpm_rates WHERE country_code = $1 AND is_active = true',
      [countryCode]
    );
    if (result.rows.length > 0) {
      return parseFloat(result.rows[0].cpm_rate);
    }
  }
  const defaultRate = await getSystemSetting('earning_per_1000_views');
  return defaultRate || 0.50;
}

async function processReferralEarning(userId, sourceEarning) {
  const referralPercentage = await getSystemSetting('referral_percentage') || 10;
  
  const referralResult = await pool.query(`
    SELECT r.id, r.referrer_id 
    FROM referrals r 
    WHERE r.referred_id = $1 AND r.status = 'active'
  `, [userId]);
  
  if (referralResult.rows.length === 0) return;
  
  const referral = referralResult.rows[0];
  const referrerEarning = sourceEarning * (referralPercentage / 100);
  
  if (referrerEarning <= 0) return;
  
  await addEarning(
    referral.referrer_id, 
    referrerEarning, 
    'referral_earning',
    `أرباح إحالة من ملف`,
    referral.id,
    'referral'
  );
  
  await pool.query(`
    INSERT INTO referral_earnings (referral_id, referrer_id, referred_id, source_earning, referrer_earning)
    VALUES ($1, $2, $3, $4, $5)
  `, [referral.id, referral.referrer_id, userId, sourceEarning, referrerEarning]);
  
  await pool.query(`
    UPDATE referrals SET total_earnings = total_earnings + $1 WHERE id = $2
  `, [referrerEarning, referral.id]);
}

// ========================================
// API ENDPOINTS - File Gate Ad
// ========================================

// بدء جلسة Gate Ad للملف
router.post('/file/start', async (req, res) => {
  try {
    const { share_token, viewer_ip, fingerprint, user_id } = req.body;
    
    if (!share_token || !viewer_ip) {
      return res.status(400).json({ success: false, message: 'بيانات ناقصة' });
    }
    
    // جلب معلومات الملف
    const fileResult = await pool.query(
      'SELECT id, user_id, file_type FROM files WHERE share_token = $1 AND status = $2',
      [share_token, 'active']
    );
    
    if (fileResult.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'الملف غير موجود' });
    }
    
    const file = fileResult.rows[0];
    
    // التحقق إذا كان المستخدم Premium
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
              file_id: file.id
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
          file_id: file.id
        }
      });
    }
    
    // التحقق من cooldown
    const cooldownHours = await getSystemSetting('view_cooldown_hours') || 24;
    const existingView = await pool.query(`
      SELECT id FROM file_views 
      WHERE file_id = $1 AND viewer_ip = $2 
      AND created_at > NOW() - INTERVAL '${cooldownHours} hours'
      AND view_counted = true
    `, [file.id, viewer_ip]);
    
    const canEarn = existingView.rows.length === 0;
    
    const sessionToken = crypto.randomBytes(32).toString('hex');
    const countryCode = await getCountryFromIP(viewer_ip);
    
    await pool.query(`
      INSERT INTO file_views (file_id, viewer_ip, viewer_fingerprint, user_id, session_token, country_code, action_type)
      VALUES ($1, $2, $3, $4, $5, $6, 'view')
    `, [file.id, viewer_ip, fingerprint, user_id, sessionToken, countryCode]);
    
    const adDuration = await getSystemSetting('gate_ad_duration') || 5;
    
    res.json({
      success: true,
      data: {
        skip_ad: false,
        session_token: sessionToken,
        ad_duration: adDuration,
        can_earn: canEarn,
        file_id: file.id,
        file_type: file.file_type
      }
    });
  } catch (error) {
    console.error('File gate ad start error:', error);
    res.status(500).json({ success: false, message: 'خطأ في بدء الجلسة' });
  }
});

// إكمال Gate Ad للملف
router.post('/file/complete', async (req, res) => {
  try {
    const { session_token } = req.body;
    
    if (!session_token) {
      return res.status(400).json({ success: false, message: 'session_token مطلوب' });
    }
    
    const sessionResult = await pool.query(`
      SELECT fv.*, f.user_id as file_owner_id
      FROM file_views fv
      JOIN files f ON fv.file_id = f.id
      WHERE fv.session_token = $1 AND fv.ad_completed = false
    `, [session_token]);
    
    if (sessionResult.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'الجلسة غير موجودة أو منتهية' });
    }
    
    const session = sessionResult.rows[0];
    
    const adDuration = await getSystemSetting('gate_ad_duration') || 5;
    const sessionStart = new Date(session.created_at);
    const now = new Date();
    const elapsedSeconds = (now - sessionStart) / 1000;
    
    if (elapsedSeconds < adDuration - 1) {
      return res.status(400).json({ success: false, message: 'لم يكتمل وقت الإعلان' });
    }
    
    await pool.query(`
      UPDATE file_views 
      SET ad_completed = true, completed_at = CURRENT_TIMESTAMP
      WHERE id = $1
    `, [session.id]);
    
    const cooldownHours = await getSystemSetting('view_cooldown_hours') || 24;
    const existingView = await pool.query(`
      SELECT id FROM file_views 
      WHERE file_id = $1 AND viewer_ip = $2 
      AND id != $3
      AND created_at > NOW() - INTERVAL '${cooldownHours} hours'
      AND view_counted = true
    `, [session.file_id, session.viewer_ip, session.id]);
    
    let viewCounted = false;
    let earningAdded = false;
    
    if (existingView.rows.length === 0) {
      viewCounted = true;
      
      await pool.query(`
        UPDATE file_views SET view_counted = true WHERE id = $1
      `, [session.id]);
      
      await pool.query(`
        UPDATE files 
        SET views_count = views_count + 1, 
            paid_views_count = paid_views_count + 1 
        WHERE id = $1
      `, [session.file_id]);
      
      const earningPer1000 = await getCpmRate(session.country_code);
      const earningPerView = earningPer1000 / 1000;
      
      if (earningPerView > 0 && session.file_owner_id) {
        earningAdded = true;
        
        await addEarning(
          session.file_owner_id,
          earningPerView,
          'file_view_earning',
          `ربح من مشاهدة/تنزيل ملف`,
          session.file_id,
          'file'
        );
        
        await pool.query(`
          INSERT INTO file_earnings (user_id, file_id, views_count, earning_rate, amount)
          VALUES ($1, $2, 1, $3, $4)
        `, [session.file_owner_id, session.file_id, earningPer1000, earningPerView]);
        
        await processReferralEarning(session.file_owner_id, earningPerView);
        
        await pool.query(`
          UPDATE file_views SET earning_calculated = true WHERE id = $1
        `, [session.id]);
      }
    }
    
    res.json({
      success: true,
      data: {
        view_counted: viewCounted,
        earning_added: earningAdded,
        file_id: session.file_id
      }
    });
  } catch (error) {
    console.error('File gate ad complete error:', error);
    res.status(500).json({ success: false, message: 'خطأ في إكمال الجلسة' });
  }
});

// بدء جلسة Gate Ad للمجلد
router.post('/folder/start', async (req, res) => {
  try {
    const { share_token, viewer_ip, fingerprint, user_id } = req.body;
    
    if (!share_token || !viewer_ip) {
      return res.status(400).json({ success: false, message: 'بيانات ناقصة' });
    }
    
    const folderResult = await pool.query(
      'SELECT id, user_id FROM folders WHERE share_token = $1',
      [share_token]
    );
    
    if (folderResult.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'المجلد غير موجود' });
    }
    
    const folder = folderResult.rows[0];
    
    // التحقق إذا كان المستخدم Premium
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
              folder_id: folder.id
            }
          });
        }
      }
    }
    
    const gateAdEnabled = await getSystemSetting('gate_ad_enabled');
    if (!gateAdEnabled) {
      return res.json({
        success: true,
        data: {
          skip_ad: true,
          gate_ad_disabled: true,
          folder_id: folder.id
        }
      });
    }
    
    const sessionToken = crypto.randomBytes(32).toString('hex');
    const countryCode = await getCountryFromIP(viewer_ip);
    
    await pool.query(`
      INSERT INTO file_views (folder_id, viewer_ip, viewer_fingerprint, user_id, session_token, country_code, action_type)
      VALUES ($1, $2, $3, $4, $5, $6, 'folder_view')
    `, [folder.id, viewer_ip, fingerprint, user_id, sessionToken, countryCode]);
    
    const adDuration = await getSystemSetting('gate_ad_duration') || 5;
    
    res.json({
      success: true,
      data: {
        skip_ad: false,
        session_token: sessionToken,
        ad_duration: adDuration,
        folder_id: folder.id
      }
    });
  } catch (error) {
    console.error('Folder gate ad start error:', error);
    res.status(500).json({ success: false, message: 'خطأ في بدء الجلسة' });
  }
});

// إكمال Gate Ad للمجلد
router.post('/folder/complete', async (req, res) => {
  try {
    const { session_token } = req.body;
    
    if (!session_token) {
      return res.status(400).json({ success: false, message: 'session_token مطلوب' });
    }
    
    const sessionResult = await pool.query(`
      SELECT fv.*, fo.user_id as folder_owner_id
      FROM file_views fv
      JOIN folders fo ON fv.folder_id = fo.id
      WHERE fv.session_token = $1 AND fv.ad_completed = false
    `, [session_token]);
    
    if (sessionResult.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'الجلسة غير موجودة أو منتهية' });
    }
    
    const session = sessionResult.rows[0];
    
    const adDuration = await getSystemSetting('gate_ad_duration') || 5;
    const sessionStart = new Date(session.created_at);
    const now = new Date();
    const elapsedSeconds = (now - sessionStart) / 1000;
    
    if (elapsedSeconds < adDuration - 1) {
      return res.status(400).json({ success: false, message: 'لم يكتمل وقت الإعلان' });
    }
    
    await pool.query(`
      UPDATE file_views 
      SET ad_completed = true, completed_at = CURRENT_TIMESTAMP, view_counted = true
      WHERE id = $1
    `, [session.id]);
    
    res.json({
      success: true,
      data: {
        folder_id: session.folder_id,
        access_granted: true
      }
    });
  } catch (error) {
    console.error('Folder gate ad complete error:', error);
    res.status(500).json({ success: false, message: 'خطأ في إكمال الجلسة' });
  }
});

module.exports = router;
