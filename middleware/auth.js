const jwt = require('jsonwebtoken');

// التحقق من JWT Token
const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1]; // Bearer TOKEN

  if (!token) {
    return res.status(401).json({ 
      success: false, 
      message: 'الوصول مرفوض. لا يوجد token.' 
    });
  }

  jwt.verify(token, process.env.JWT_SECRET, (err, user) => {
    if (err || user.isAdmin === true) {
      return res.status(403).json({ 
        success: false, 
        message: 'Token غير صالح أو منتهي الصلاحية.' 
      });
    }
    req.user = user;
    next();
  });
};

module.exports = authenticateToken;
