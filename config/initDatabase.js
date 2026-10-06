const pool = require('./database');

// إنشاء الجداول الأساسية
const initDatabase = async () => {
  try {
    // جدول المستخدمين
    await pool.query(`
      CREATE TABLE IF NOT EXISTS users (
        id SERIAL PRIMARY KEY,
        username VARCHAR(100) UNIQUE NOT NULL,
        email VARCHAR(255) UNIQUE NOT NULL,
        password_hash VARCHAR(255) NOT NULL,
        subscription_status VARCHAR(50) DEFAULT 'free',
        subscription_expiry DATE,
        fcm_token TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        last_login TIMESTAMP
      )
    `);

    // جدول الفيديوهات
    await pool.query(`
      CREATE TABLE IF NOT EXISTS videos (
        id SERIAL PRIMARY KEY,
        user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        title VARCHAR(255),
        description TEXT,
        tags TEXT[],
        filename VARCHAR(255) NOT NULL,
        file_path TEXT NOT NULL,
        file_size BIGINT,
        video_url TEXT,
        thumbnail_url TEXT,
        views_count INTEGER DEFAULT 0,
        likes_count INTEGER DEFAULT 0,
        is_featured BOOLEAN DEFAULT FALSE,
        is_recommended BOOLEAN DEFAULT FALSE,
        status VARCHAR(50) DEFAULT 'active',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // جدول مكتبة المستخدم
    await pool.query(`
      CREATE TABLE IF NOT EXISTS user_library (
        id SERIAL PRIMARY KEY,
        user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        video_id INTEGER REFERENCES videos(id) ON DELETE CASCADE,
        saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE(user_id, video_id)
      )
    `);

    // جدول نشاطات المستخدم
    await pool.query(`
      CREATE TABLE IF NOT EXISTS user_activities (
        id SERIAL PRIMARY KEY,
        user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        activity_type VARCHAR(50) NOT NULL,
        activity_data JSONB,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // جدول الفيديوهات المقترحة (لإدارة Admin)
    await pool.query(`
      CREATE TABLE IF NOT EXISTS recommended_videos (
        id SERIAL PRIMARY KEY,
        video_id INTEGER REFERENCES videos(id) ON DELETE CASCADE,
        position INTEGER DEFAULT 0,
        is_active BOOLEAN DEFAULT TRUE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE(video_id)
      )
    `);

    // جدول إعدادات النظام (لإدارة Admin)
    await pool.query(`
      CREATE TABLE IF NOT EXISTS system_settings (
        id SERIAL PRIMARY KEY,
        setting_key VARCHAR(100) UNIQUE NOT NULL,
        setting_value TEXT,
        setting_type VARCHAR(50) DEFAULT 'string',
        description TEXT,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // جدول إعدادات التطبيق (اسم التطبيق، اللوجو، إلخ)
    await pool.query(`
      CREATE TABLE IF NOT EXISTS app_settings (
        id SERIAL PRIMARY KEY,
        app_name VARCHAR(100) DEFAULT 'IQBox',
        app_logo_url TEXT,
        app_icon_url TEXT,
        primary_color VARCHAR(20) DEFAULT '#3B82F6',
        secondary_color VARCHAR(20) DEFAULT '#1E3A8A',
        support_email VARCHAR(255),
        support_phone VARCHAR(50),
        terms_url TEXT,
        privacy_url TEXT,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // جدول خطط الاشتراك (ديناميكي من الأدمن)
    await pool.query(`
      CREATE TABLE IF NOT EXISTS subscription_plans (
        id SERIAL PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        name_ar VARCHAR(100),
        description TEXT,
        description_ar TEXT,
        price DECIMAL(10, 2) NOT NULL,
        currency VARCHAR(10) DEFAULT 'USD',
        duration_days INTEGER NOT NULL,
        features JSONB,
        payment_instructions TEXT,
        payment_instructions_ar TEXT,
        is_active BOOLEAN DEFAULT TRUE,
        sort_order INTEGER DEFAULT 0,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // جدول طلبات الدفع (المستخدم يطلب، الأدمن يوافق)
    await pool.query(`
      CREATE TABLE IF NOT EXISTS payment_requests (
        id SERIAL PRIMARY KEY,
        user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        plan_id INTEGER REFERENCES subscription_plans(id) ON DELETE SET NULL,
        amount DECIMAL(10, 2) NOT NULL,
        currency VARCHAR(10) DEFAULT 'USD',
        payment_method VARCHAR(50),
        payment_proof TEXT,
        notes TEXT,
        status VARCHAR(20) DEFAULT 'pending',
        admin_notes TEXT,
        processed_by INTEGER REFERENCES users(id),
        processed_at TIMESTAMP,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // جدول الأدمن
    await pool.query(`
      CREATE TABLE IF NOT EXISTS admins (
        id SERIAL PRIMARY KEY,
        username VARCHAR(100) UNIQUE NOT NULL,
        email VARCHAR(255) UNIQUE NOT NULL,
        password_hash VARCHAR(255) NOT NULL,
        role VARCHAR(50) DEFAULT 'admin',
        is_active BOOLEAN DEFAULT TRUE,
        last_login TIMESTAMP,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // ========================================
    // جداول نظام الربح والمحفظة
    // ========================================

    // جدول محفظة المستخدم
    await pool.query(`
      CREATE TABLE IF NOT EXISTS user_wallets (
        id SERIAL PRIMARY KEY,
        user_id INTEGER REFERENCES users(id) ON DELETE CASCADE UNIQUE,
        balance DECIMAL(15, 6) DEFAULT 0,
        total_earned DECIMAL(15, 6) DEFAULT 0,
        total_withdrawn DECIMAL(15, 6) DEFAULT 0,
        pending_withdrawal DECIMAL(15, 6) DEFAULT 0,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // جدول المعاملات المالية
    await pool.query(`
      CREATE TABLE IF NOT EXISTS wallet_transactions (
        id SERIAL PRIMARY KEY,
        user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        type VARCHAR(50) NOT NULL,
        amount DECIMAL(15, 6) NOT NULL,
        balance_after DECIMAL(15, 6),
        description TEXT,
        reference_id INTEGER,
        reference_type VARCHAR(50),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // جدول طلبات السحب
    await pool.query(`
      CREATE TABLE IF NOT EXISTS withdrawal_requests (
        id SERIAL PRIMARY KEY,
        user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        amount DECIMAL(15, 6) NOT NULL,
        withdrawal_method VARCHAR(50) NOT NULL,
        account_details JSONB NOT NULL,
        status VARCHAR(20) DEFAULT 'pending',
        admin_notes TEXT,
        processed_by INTEGER,
        processed_at TIMESTAMP,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // جدول نظام الإحالات
    await pool.query(`
      CREATE TABLE IF NOT EXISTS referrals (
        id SERIAL PRIMARY KEY,
        referrer_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        referred_id INTEGER REFERENCES users(id) ON DELETE CASCADE UNIQUE,
        referral_code VARCHAR(20) NOT NULL,
        total_earnings DECIMAL(15, 6) DEFAULT 0,
        status VARCHAR(20) DEFAULT 'active',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // جدول أرباح الإحالات
    await pool.query(`
      CREATE TABLE IF NOT EXISTS referral_earnings (
        id SERIAL PRIMARY KEY,
        referral_id INTEGER REFERENCES referrals(id) ON DELETE CASCADE,
        referrer_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        referred_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        source_earning DECIMAL(15, 6) NOT NULL,
        referrer_earning DECIMAL(15, 6) NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // جدول مشاهدات Gate Ad (لمنع التكرار واحتساب المشاهدات الصحيحة)
    await pool.query(`
      CREATE TABLE IF NOT EXISTS gate_ad_views (
        id SERIAL PRIMARY KEY,
        video_id INTEGER REFERENCES videos(id) ON DELETE CASCADE,
        viewer_ip VARCHAR(45) NOT NULL,
        viewer_fingerprint VARCHAR(255),
        user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
        ad_completed BOOLEAN DEFAULT FALSE,
        view_counted BOOLEAN DEFAULT FALSE,
        earning_calculated BOOLEAN DEFAULT FALSE,
        session_token VARCHAR(100) UNIQUE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        completed_at TIMESTAMP
      )
    `);

    // جدول أرباح المشاهدات
    await pool.query(`
      CREATE TABLE IF NOT EXISTS view_earnings (
        id SERIAL PRIMARY KEY,
        user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        video_id INTEGER REFERENCES videos(id) ON DELETE CASCADE,
        views_count INTEGER NOT NULL,
        earning_rate DECIMAL(10, 6) NOT NULL,
        amount DECIMAL(15, 6) NOT NULL,
        period_start TIMESTAMP,
        period_end TIMESTAMP,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // جدول أسعار CPM حسب الدولة
    await pool.query(`
      CREATE TABLE IF NOT EXISTS country_cpm_rates (
        id SERIAL PRIMARY KEY,
        country_code VARCHAR(2) NOT NULL UNIQUE,
        country_name VARCHAR(100) NOT NULL,
        country_name_ar VARCHAR(100),
        cpm_rate DECIMAL(10, 4) NOT NULL DEFAULT 0.10,
        is_active BOOLEAN DEFAULT TRUE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // إدراج أسعار CPM الافتراضية لبعض الدول
    const defaultCpmRates = [
      { code: 'US', name: 'United States', name_ar: 'الولايات المتحدة', rate: 0.50 },
      { code: 'GB', name: 'United Kingdom', name_ar: 'المملكة المتحدة', rate: 0.40 },
      { code: 'CA', name: 'Canada', name_ar: 'كندا', rate: 0.40 },
      { code: 'DE', name: 'Germany', name_ar: 'ألمانيا', rate: 0.35 },
      { code: 'FR', name: 'France', name_ar: 'فرنسا', rate: 0.30 },
      { code: 'AU', name: 'Australia', name_ar: 'أستراليا', rate: 0.35 },
      { code: 'AE', name: 'United Arab Emirates', name_ar: 'الإمارات', rate: 0.30 },
      { code: 'SA', name: 'Saudi Arabia', name_ar: 'السعودية', rate: 0.25 },
      { code: 'KW', name: 'Kuwait', name_ar: 'الكويت', rate: 0.25 },
      { code: 'QA', name: 'Qatar', name_ar: 'قطر', rate: 0.25 },
      { code: 'IQ', name: 'Iraq', name_ar: 'العراق', rate: 0.10 },
      { code: 'DZ', name: 'Algeria', name_ar: 'الجزائر', rate: 0.10 },
      { code: 'EG', name: 'Egypt', name_ar: 'مصر', rate: 0.10 },
      { code: 'MA', name: 'Morocco', name_ar: 'المغرب', rate: 0.10 },
      { code: 'TN', name: 'Tunisia', name_ar: 'تونس', rate: 0.10 },
      { code: 'JO', name: 'Jordan', name_ar: 'الأردن', rate: 0.12 },
      { code: 'LB', name: 'Lebanon', name_ar: 'لبنان', rate: 0.10 },
      { code: 'SY', name: 'Syria', name_ar: 'سوريا', rate: 0.05 },
      { code: 'LY', name: 'Libya', name_ar: 'ليبيا', rate: 0.08 },
      { code: 'SD', name: 'Sudan', name_ar: 'السودان', rate: 0.05 },
      { code: 'TR', name: 'Turkey', name_ar: 'تركيا', rate: 0.15 },
      { code: 'IN', name: 'India', name_ar: 'الهند', rate: 0.05 },
      { code: 'BR', name: 'Brazil', name_ar: 'البرازيل', rate: 0.10 },
    ];

    for (const rate of defaultCpmRates) {
      await pool.query(`
        INSERT INTO country_cpm_rates (country_code, country_name, country_name_ar, cpm_rate)
        VALUES ($1, $2, $3, $4)
        ON CONFLICT (country_code) DO NOTHING
      `, [rate.code, rate.name, rate.name_ar, rate.rate]);
    }

    // إضافة عمود referral_code للمستخدمين
    await pool.query(`
      ALTER TABLE users 
      ADD COLUMN IF NOT EXISTS referral_code VARCHAR(20) UNIQUE,
      ADD COLUMN IF NOT EXISTS referred_by INTEGER REFERENCES users(id)
    `);

    // إضافة عمود paid_views للفيديوهات (المشاهدات المحتسبة للربح)
    await pool.query(`
      ALTER TABLE videos 
      ADD COLUMN IF NOT EXISTS paid_views_count INTEGER DEFAULT 0
    `);

    // إدراج إعدادات التطبيق الافتراضية إذا لم تكن موجودة
    await pool.query(`
      INSERT INTO app_settings (id, app_name, app_logo_url)
      VALUES (1, 'IQBox', NULL)
      ON CONFLICT (id) DO NOTHING
    `);

    // إنشاء فهارس لتحسين الأداء
    await pool.query(`
      CREATE INDEX IF NOT EXISTS idx_videos_user_id ON videos(user_id);
      CREATE INDEX IF NOT EXISTS idx_videos_status ON videos(status);
      CREATE INDEX IF NOT EXISTS idx_videos_featured ON videos(is_featured);
      CREATE INDEX IF NOT EXISTS idx_videos_recommended ON videos(is_recommended);
      CREATE INDEX IF NOT EXISTS idx_user_library_user_id ON user_library(user_id);
      CREATE INDEX IF NOT EXISTS idx_user_activities_user_id ON user_activities(user_id);
      CREATE INDEX IF NOT EXISTS idx_recommended_videos_position ON recommended_videos(position);
      CREATE INDEX IF NOT EXISTS idx_subscription_plans_active ON subscription_plans(is_active);
      CREATE INDEX IF NOT EXISTS idx_payment_requests_user_id ON payment_requests(user_id);
      CREATE INDEX IF NOT EXISTS idx_payment_requests_status ON payment_requests(status);
      CREATE INDEX IF NOT EXISTS idx_wallet_transactions_user_id ON wallet_transactions(user_id);
      CREATE INDEX IF NOT EXISTS idx_withdrawal_requests_user_id ON withdrawal_requests(user_id);
      CREATE INDEX IF NOT EXISTS idx_withdrawal_requests_status ON withdrawal_requests(status);
      CREATE INDEX IF NOT EXISTS idx_referrals_referrer_id ON referrals(referrer_id);
      CREATE INDEX IF NOT EXISTS idx_referrals_referred_id ON referrals(referred_id);
      CREATE INDEX IF NOT EXISTS idx_gate_ad_views_video_id ON gate_ad_views(video_id);
      CREATE INDEX IF NOT EXISTS idx_gate_ad_views_session ON gate_ad_views(session_token);
      CREATE INDEX IF NOT EXISTS idx_view_earnings_user_id ON view_earnings(user_id);
    `);

    // ========================================
    // جداول نظام الملفات والمجلدات (Files Platform)
    // ========================================

    // جدول المجلدات
    await pool.query(`
      CREATE TABLE IF NOT EXISTS folders (
        id SERIAL PRIMARY KEY,
        user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        parent_id INTEGER REFERENCES folders(id) ON DELETE CASCADE,
        name VARCHAR(255) NOT NULL,
        share_token VARCHAR(100) UNIQUE,
        is_shared BOOLEAN DEFAULT FALSE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // جدول الملفات (يشمل الفيديوهات والصور والمستندات)
    await pool.query(`
      CREATE TABLE IF NOT EXISTS files (
        id SERIAL PRIMARY KEY,
        user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        folder_id INTEGER REFERENCES folders(id) ON DELETE SET NULL,
        name VARCHAR(255) NOT NULL,
        original_name VARCHAR(255),
        file_type VARCHAR(50) NOT NULL,
        mime_type VARCHAR(100),
        file_path TEXT NOT NULL,
        file_size BIGINT DEFAULT 0,
        share_token VARCHAR(100) UNIQUE,
        share_url TEXT,
        is_shared BOOLEAN DEFAULT FALSE,
        views_count INTEGER DEFAULT 0,
        downloads_count INTEGER DEFAULT 0,
        paid_views_count INTEGER DEFAULT 0,
        thumbnail_url TEXT,
        status VARCHAR(50) DEFAULT 'active',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // جدول مشاهدات/تنزيلات الملفات (Gate Ad)
    await pool.query(`
      CREATE TABLE IF NOT EXISTS file_views (
        id SERIAL PRIMARY KEY,
        file_id INTEGER REFERENCES files(id) ON DELETE CASCADE,
        folder_id INTEGER REFERENCES folders(id) ON DELETE CASCADE,
        viewer_ip VARCHAR(45) NOT NULL,
        viewer_fingerprint VARCHAR(255),
        user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
        action_type VARCHAR(20) DEFAULT 'view',
        ad_completed BOOLEAN DEFAULT FALSE,
        view_counted BOOLEAN DEFAULT FALSE,
        earning_calculated BOOLEAN DEFAULT FALSE,
        session_token VARCHAR(100) UNIQUE,
        country_code VARCHAR(5),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        completed_at TIMESTAMP
      )
    `);

    // جدول أرباح الملفات
    await pool.query(`
      CREATE TABLE IF NOT EXISTS file_earnings (
        id SERIAL PRIMARY KEY,
        user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        file_id INTEGER REFERENCES files(id) ON DELETE CASCADE,
        views_count INTEGER NOT NULL,
        downloads_count INTEGER DEFAULT 0,
        earning_rate DECIMAL(10, 6) NOT NULL,
        amount DECIMAL(15, 6) NOT NULL,
        period_start TIMESTAMP,
        period_end TIMESTAMP,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // إضافة أعمدة التخزين للمستخدمين
    await pool.query(`
      ALTER TABLE users 
      ADD COLUMN IF NOT EXISTS storage_used BIGINT DEFAULT 0,
      ADD COLUMN IF NOT EXISTS storage_limit BIGINT DEFAULT 10737418240,
      ADD COLUMN IF NOT EXISTS total_uploaded BIGINT DEFAULT 0,
      ADD COLUMN IF NOT EXISTS total_downloaded BIGINT DEFAULT 0
    `);

    // إضافة عمود Telegram للدعم
    await pool.query(`
      ALTER TABLE app_settings 
      ADD COLUMN IF NOT EXISTS support_telegram VARCHAR(255)
    `);

    // إنشاء فهارس للملفات والمجلدات
    await pool.query(`
      CREATE INDEX IF NOT EXISTS idx_folders_user_id ON folders(user_id);
      CREATE INDEX IF NOT EXISTS idx_folders_parent_id ON folders(parent_id);
      CREATE INDEX IF NOT EXISTS idx_folders_share_token ON folders(share_token);
      CREATE INDEX IF NOT EXISTS idx_files_user_id ON files(user_id);
      CREATE INDEX IF NOT EXISTS idx_files_folder_id ON files(folder_id);
      CREATE INDEX IF NOT EXISTS idx_files_share_token ON files(share_token);
      CREATE INDEX IF NOT EXISTS idx_files_file_type ON files(file_type);
      CREATE INDEX IF NOT EXISTS idx_files_status ON files(status);
      CREATE INDEX IF NOT EXISTS idx_file_views_file_id ON file_views(file_id);
      CREATE INDEX IF NOT EXISTS idx_file_views_session ON file_views(session_token);
      CREATE INDEX IF NOT EXISTS idx_file_earnings_user_id ON file_earnings(user_id);
    `);

    // إدراج إعدادات النظام الافتراضية للأرباح
    const defaultSettings = [
      { key: 'earning_per_1000_views', value: '0.50', type: 'decimal', desc: 'الربح لكل 1000 مشاهدة بالدولار' },
      { key: 'referral_percentage', value: '10', type: 'decimal', desc: 'نسبة أرباح الإحالة %' },
      { key: 'min_withdrawal_amount', value: '5.00', type: 'decimal', desc: 'الحد الأدنى للسحب بالدولار' },
      { key: 'gate_ad_enabled', value: 'true', type: 'boolean', desc: 'تفعيل بوابة الإعلان' },
      { key: 'gate_ad_duration', value: '5', type: 'integer', desc: 'مدة الإعلان بالثواني' },
      { key: 'view_cooldown_hours', value: '24', type: 'integer', desc: 'ساعات الانتظار قبل احتساب مشاهدة جديدة من نفس IP' },
      { key: 'weekly_subscription_price', value: '1.99', type: 'decimal', desc: 'سعر الاشتراك الأسبوعي' },
      { key: 'monthly_subscription_price', value: '4.99', type: 'decimal', desc: 'سعر الاشتراك الشهري' },
      { key: 'free_storage_limit_gb', value: '10', type: 'integer', desc: 'سعة التخزين للمستخدم المجاني (GB)' },
      { key: 'premium_storage_limit_gb', value: '200', type: 'integer', desc: 'سعة التخزين للمستخدم Premium (GB)' }
    ];

    for (const setting of defaultSettings) {
      await pool.query(`
        INSERT INTO system_settings (setting_key, setting_value, setting_type, description)
        VALUES ($1, $2, $3, $4)
        ON CONFLICT (setting_key) DO NOTHING
      `, [setting.key, setting.value, setting.type, setting.desc]);
    }

    console.log('✅ Database tables created successfully');
  } catch (error) {
    console.error('❌ Error initializing database:', error);
    throw error;
  }
};

module.exports = initDatabase;
