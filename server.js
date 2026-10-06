const express = require('express');
const cors = require('cors');
const dotenv = require('dotenv');
const path = require('path');
const fs = require('fs');

// تحميل متغيرات البيئة
dotenv.config();
if (!process.env.JWT_SECRET || process.env.JWT_SECRET.length < 32) {
  throw new Error('JWT_SECRET must contain at least 32 characters');
}

// استيراد Routes
const authRoutes = require('./routes/auth');
const videoRoutes = require('./routes/videos');
const userRoutes = require('./routes/users');
const subscriptionRoutes = require('./routes/subscription');
const settingsRoutes = require('./routes/settings');
const adminRoutes = require('./routes/admin');
const walletRoutes = require('./routes/wallet');
const referralRoutes = require('./routes/referral');
const gateadRoutes = require('./routes/gatead');
const filesRoutes = require('./routes/files');
const foldersRoutes = require('./routes/folders');
const fileGateadRoutes = require('./routes/filegatead');

// استيراد إعداد قاعدة البيانات
const initDatabase = require('./config/initDatabase');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// إنشاء مجلد الرفع إذا لم يكن موجوداً
const uploadDir = process.env.UPLOAD_DIR || './uploads/videos';
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

// Routes
app.use('/api/auth', authRoutes);
app.use('/api/videos', videoRoutes);
app.use('/api/users', userRoutes);
app.use('/api/subscription', subscriptionRoutes);
app.use('/api/settings', settingsRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/wallet', walletRoutes);
app.use('/api/referral', referralRoutes);
app.use('/api/gatead', gateadRoutes);
app.use('/api/files', filesRoutes);
app.use('/api/folders', foldersRoutes);
app.use('/api/filegatead', fileGateadRoutes);

// Route للتحقق من حالة السيرفر
app.get('/api/health', (req, res) => {
  res.json({
    success: true,
    message: 'IQBox API is running',
    timestamp: new Date().toISOString()
  });
});

// Route للجذر
app.get('/', (req, res) => {
  res.json({
    success: true,
    message: 'Welcome to IQBox API',
    version: '1.0.0',
    endpoints: {
      health: '/api/health',
      auth: '/api/auth',
      videos: '/api/videos',
      users: '/api/users',
      subscription: '/api/subscription'
    }
  });
});

// معالج الأخطاء
app.use((err, req, res, next) => {
  console.error('Error:', err);
  res.status(err.status || 500).json({
    success: false,
    message: err.message || 'خطأ في السيرفر',
    ...(process.env.NODE_ENV === 'development' && { stack: err.stack })
  });
});

// بدء السيرفر
const startServer = async () => {
  try {
    // تهيئة قاعدة البيانات
    await initDatabase();
    
    // بدء السيرفر
    app.listen(PORT, '0.0.0.0', () => {
      console.log(`
╔════════════════════════════════════════╗
║     IQBox API Server Started          ║
╠════════════════════════════════════════╣
║  Server: http://0.0.0.0:${PORT}          ║
║  Environment: ${process.env.NODE_ENV || 'development'}              ║
║  Upload Directory: ${uploadDir}       ║
╚════════════════════════════════════════╝
      `);
    });
  } catch (error) {
    console.error('❌ Failed to start server:', error);
    process.exit(1);
  }
};

startServer();

module.exports = app;
