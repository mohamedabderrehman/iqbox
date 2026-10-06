const express = require('express');
const router = express.Router();
const pool = require('../config/database');

// ========================================
// PUBLIC ENDPOINTS (للتطبيق)
// ========================================

// جلب إعدادات التطبيق (اسم التطبيق، اللوجو، الألوان، إلخ)
router.get('/app', async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT app_name, app_logo_url, app_icon_url, primary_color, secondary_color,
              support_email, support_phone, support_telegram, terms_url, privacy_url
       FROM app_settings 
       WHERE id = 1`
    );

    // جلب إعدادات التخزين من system_settings
    const storageSettings = await pool.query(`
      SELECT setting_key, setting_value FROM system_settings 
      WHERE setting_key IN ('free_storage_limit_gb', 'premium_storage_limit_gb', 'earning_per_1000_views', 'referral_percentage', 'gate_ad_enabled')
    `);
    
    const systemSettings = {};
    storageSettings.rows.forEach(row => {
      systemSettings[row.setting_key] = row.setting_value;
    });

    if (result.rows.length === 0) {
      return res.json({
        success: true,
        data: {
          settings: {
            app_name: 'IQBox',
            app_logo_url: null,
            app_icon_url: null,
            primary_color: '#3B82F6',
            secondary_color: '#1E3A8A',
            support_email: null,
            support_phone: null,
            support_telegram: null,
            terms_url: null,
            privacy_url: null,
            free_storage_limit_gb: parseInt(systemSettings.free_storage_limit_gb) || 10,
            premium_storage_limit_gb: parseInt(systemSettings.premium_storage_limit_gb) || 200,
            earning_per_1000_views: parseFloat(systemSettings.earning_per_1000_views) || 0.50,
            referral_percentage: parseFloat(systemSettings.referral_percentage) || 10,
            gate_ad_enabled: systemSettings.gate_ad_enabled === 'true'
          }
        }
      });
    }

    res.json({
      success: true,
      data: {
        settings: {
          ...result.rows[0],
          free_storage_limit_gb: parseInt(systemSettings.free_storage_limit_gb) || 10,
          premium_storage_limit_gb: parseInt(systemSettings.premium_storage_limit_gb) || 200,
          earning_per_1000_views: parseFloat(systemSettings.earning_per_1000_views) || 0.50,
          referral_percentage: parseFloat(systemSettings.referral_percentage) || 10,
          gate_ad_enabled: systemSettings.gate_ad_enabled === 'true'
        }
      }
    });
  } catch (error) {
    console.error('Get app settings error:', error);
    res.status(500).json({
      success: false,
      message: 'خطأ في جلب إعدادات التطبيق'
    });
  }
});

module.exports = router;
