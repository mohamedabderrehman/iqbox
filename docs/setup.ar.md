# الإعداد الكامل

أنشئ PostgreSQL فارغة وانسخ env.example إلى .env واضبط DB_HOST وDB_PORT وDB_USER وDB_PASSWORD وDB_NAME وJWT_SECRET وDEMO_PASSWORD. تنشئ التعبئة admin@example.test وdemo-user@example.test وdemo-other@example.test. عنوان إدارة VITE_API_URL هو http://127.0.0.1:3000/api ويشير API_BASE_URL لعارض PHP إلى الخادم نفسه. يستخدم إعداد أندرويد IQBOX_API_BASE_URL العنوان 10.0.2.2 للوصول للمضيف من المحاكي.

## الأوامر

```sh
npm ci
node tools/bootstrap-demo.js
npm run dev
# Separate terminal:
node tools/check-demo.js
cd admin-panel
npm ci
npm run dev
# PHP viewer, separate terminal from root:
cd video-page
php -S 127.0.0.1:8088 router.php
```

## جرد الإعداد

| المتغير | موضع الاستخدام | قاعدة الإعداد |
|---|---|---|
| `API_BASE_URL` | `routes/videos.js` | استخدم المثال المحلي أو افتراضي الشيفرة واضبطه للبيئة المؤقتة. |
| `BASE_URL` | `routes/files.js` | استخدم المثال المحلي أو افتراضي الشيفرة واضبطه للبيئة المؤقتة. |
| `DB_HOST` | `config/database.js` | استخدم المثال المحلي أو افتراضي الشيفرة واضبطه للبيئة المؤقتة. |
| `DB_NAME` | `config/database.js` | استخدم المثال المحلي أو افتراضي الشيفرة واضبطه للبيئة المؤقتة. |
| `DB_PASSWORD` | `config/database.js` | قدم القيمة بصورة خاصة عند تفعيل التكامل، دون سر افتراضي. |
| `DB_PORT` | `config/database.js` | استخدم المثال المحلي أو افتراضي الشيفرة واضبطه للبيئة المؤقتة. |
| `DB_USER` | `config/database.js` | استخدم المثال المحلي أو افتراضي الشيفرة واضبطه للبيئة المؤقتة. |
| `JWT_SECRET` | `middleware/auth.js` | قدم القيمة بصورة خاصة عند تفعيل التكامل، دون سر افتراضي. |
| `MAX_FILE_SIZE` | `middleware/upload.js` | استخدم المثال المحلي أو افتراضي الشيفرة واضبطه للبيئة المؤقتة. |
| `NODE_ENV` | `server.js` | استخدم المثال المحلي أو افتراضي الشيفرة واضبطه للبيئة المؤقتة. |
| `PORT` | `server.js` | استخدم المثال المحلي أو افتراضي الشيفرة واضبطه للبيئة المؤقتة. |
| `SITE_BASE_URL` | `video-page/config.php` | استخدم المثال المحلي أو افتراضي الشيفرة واضبطه للبيئة المؤقتة. |
| `UPLOAD_DIR` | `server.js` | استخدم المثال المحلي أو افتراضي الشيفرة واضبطه للبيئة المؤقتة. |

ليست كل متغيرات الجرد إلزامية. تحدد الفقرة الأولى قيم التشغيل الأساسية، وتلزم قيم المزود للتكامل الحي المفعل فقط. تتجاوز DEMO_API_URL هدف الفحص المحلي عند دعمه. لا توجه أوامر التعبئة والاستعادة والفحص لقاعدة إنتاج. لا تُحمّل أمثلة البيئة نفسها تلقائياً؛ جهز بيئة العملية أو dotenv حيث يستخدمه المكون.

## المكونات

| المكون | المسؤولية |
|---|---|
| `IQBox-Android/` | تطبيق أندرويد Compose |
| `routes/` | واجهات المصادقة والرفع والمشاركة وبوابات الإعلان والحسابات المالية |
| `config/` | اتصال PostgreSQL وتهيئة الجداول |
| `admin-panel/` | إدارة React |
| `video-page/` | عارض المشاركة العام PHP |


## مسارات المشاركة المحلية

يشغل router.php مساري /file/<token> و/folder/<token> دون خادم سابق. ضع BASE_URL وSITE_BASE_URL على عنوان العارض 8088 وAPI_BASE_URL على API مع /api. تُضبط روابط المتجر الحالية اختيارياً ولا نضع رابط متجر غير موثق.
