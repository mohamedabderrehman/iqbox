const multer = require('multer');
const path = require('path');
const fs = require('fs');

// إنشاء مجلد الرفع إذا لم يكن موجوداً
const uploadDir = process.env.UPLOAD_DIR || './uploads/videos';
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

// إعداد multer لرفع الفيديو
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    // اسم الملف: timestamp-userId-originalname
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    const ext = path.extname(file.originalname);
    const name = path.basename(file.originalname, ext);
    cb(null, `${uniqueSuffix}-${name}${ext}`);
  }
});

// فلترة الملفات - فقط فيديو
const fileFilter = (req, file, cb) => {
  const allowedExtensions = /\.(mp4|avi|mov|wmv|flv|webm|mkv|m4v|3gp)$/i;
  const extname = allowedExtensions.test(path.extname(file.originalname));
  
  // التحقق من mimetype (يجب أن يبدأ بـ video/)
  const isVideoMimeType = file.mimetype && file.mimetype.startsWith('video/');

  if (extname && isVideoMimeType) {
    cb(null, true);
  } else {
    cb(new Error('يُسمح فقط بملفات الفيديو!'), false);
  }
};

const upload = multer({
  storage: storage,
  limits: {
    fileSize: parseInt(process.env.MAX_FILE_SIZE) || 500 * 1024 * 1024 // 500MB
  },
  fileFilter: fileFilter
});

module.exports = upload;
