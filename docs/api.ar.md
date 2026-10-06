# الواجهات ومسارات التنفيذ

رفع من أندرويد بعد الدخول ← حفظ الملف وبياناته في الخادم ← إنشاء رمز مشاركة ← عرض عام وبوابة الإعلان ← بث أو تنزيل؛ تدير اللوحة المنصة.

تحتاج المسارات المحلية للموجه إلى بادئة الخادم. تستخدم مسارات PHP الملفات الفعلية ما لم توجد إعادة كتابة. المتحكمات والوسطاء في الشيفرة مرجع الحقول والصلاحيات. فحوص tools/check-demo تمثل طلبات حقيقية ببيانات اصطناعية وليست مزوداً وهمياً.

## مراجع التنفيذ

- [server.js](../server.js)
- [routes/files.js](../routes/files.js)
- [routes/folders.js](../routes/folders.js)
- [routes/admin.js](../routes/admin.js)
- [middleware/upload.js](../middleware/upload.js)
- [IQBox-Android/app/src/main/java/com/iqbox/app/data/api/ApiClient.kt](../IQBox-Android/app/src/main/java/com/iqbox/app/data/api/ApiClient.kt)

## حدود التكامل

يلزم فحص بناء أندرويد والرفع الخلفي والروابط العميقة واحتساب المشاهدات المتكررة وكل حالات السحب والدفع. الإعلانات والدفع اعتماديات خارجية وليست دليلاً على دخل.


## جرد المسارات

| الطريقة | المسار المحلي للموجه | الشيفرة |
|---|---|---|
| GET | `/api/health` | `server.js` |
| GET | `/` | `server.js` |
| POST | `/login` | `routes/admin.js` |
| GET | `/users` | `routes/admin.js` |
| DELETE | `/users/:id` | `routes/admin.js` |
| GET | `/plans` | `routes/admin.js` |
| POST | `/plans` | `routes/admin.js` |
| PUT | `/plans/:id` | `routes/admin.js` |
| DELETE | `/plans/:id` | `routes/admin.js` |
| GET | `/payments` | `routes/admin.js` |
| POST | `/payments/:id/approve` | `routes/admin.js` |
| POST | `/payments/:id/reject` | `routes/admin.js` |
| GET | `/settings` | `routes/admin.js` |
| PUT | `/settings` | `routes/admin.js` |
| GET | `/stats` | `routes/admin.js` |
| GET | `/videos` | `routes/admin.js` |
| PUT | `/videos/:id` | `routes/admin.js` |
| DELETE | `/videos/:id` | `routes/admin.js` |
| GET | `/recommended` | `routes/admin.js` |
| POST | `/recommended` | `routes/admin.js` |
| DELETE | `/recommended/:videoId` | `routes/admin.js` |
| GET | `/withdrawals` | `routes/admin.js` |
| POST | `/withdrawals/:id/approve` | `routes/admin.js` |
| POST | `/withdrawals/:id/reject` | `routes/admin.js` |
| GET | `/financial-settings` | `routes/admin.js` |
| PUT | `/financial-settings` | `routes/admin.js` |
| GET | `/user-wallets` | `routes/admin.js` |
| GET | `/user-wallets/:userId` | `routes/admin.js` |
| GET | `/referrals-stats` | `routes/admin.js` |
| GET | `/earnings-overview` | `routes/admin.js` |
| GET | `/cpm-rates` | `routes/admin.js` |
| POST | `/cpm-rates` | `routes/admin.js` |
| PUT | `/cpm-rates/:id` | `routes/admin.js` |
| DELETE | `/cpm-rates/:id` | `routes/admin.js` |
| GET | `/files/stats` | `routes/admin.js` |
| GET | `/files` | `routes/admin.js` |
| GET | `/folders` | `routes/admin.js` |
| DELETE | `/files/:id` | `routes/admin.js` |
| DELETE | `/folders/:id` | `routes/admin.js` |
| POST | `/register` | `routes/auth.js` |
| POST | `/login` | `routes/auth.js` |
| POST | `/fcm-token` | `routes/auth.js` |
| POST | `/file/start` | `routes/filegatead.js` |
| POST | `/file/complete` | `routes/filegatead.js` |
| POST | `/folder/start` | `routes/filegatead.js` |
| POST | `/folder/complete` | `routes/filegatead.js` |
| POST | `/upload` | `routes/files.js` |
| GET | `/my-files` | `routes/files.js` |
| GET | `/storage` | `routes/files.js` |
| DELETE | `/:id` | `routes/files.js` |
| GET | `/info/:shareToken` | `routes/files.js` |
| GET | `/download/:shareToken` | `routes/files.js` |
| GET | `/stream/:shareToken` | `routes/files.js` |
| POST | `/` | `routes/folders.js` |
| GET | `/` | `routes/folders.js` |
| GET | `/:id/contents` | `routes/folders.js` |
| PUT | `/:id` | `routes/folders.js` |
| DELETE | `/:id` | `routes/folders.js` |
| GET | `/public/:shareToken` | `routes/folders.js` |
| GET | `/download/:shareToken` | `routes/folders.js` |
| POST | `/start` | `routes/gatead.js` |
| POST | `/complete` | `routes/gatead.js` |
| GET | `/settings` | `routes/gatead.js` |
| GET | `/my-code` | `routes/referral.js` |
| GET | `/stats` | `routes/referral.js` |
| GET | `/referred-users` | `routes/referral.js` |
| GET | `/earnings` | `routes/referral.js` |
| GET | `/validate/:code` | `routes/referral.js` |
| GET | `/app` | `routes/settings.js` |
| POST | `/file/add` | `routes/sharing.js` |
| POST | `/folder/add` | `routes/sharing.js` |
| GET | `/my-shared` | `routes/sharing.js` |
| DELETE | `/:id` | `routes/sharing.js` |
| GET | `/plans` | `routes/subscription.js` |
| GET | `/plans/:id` | `routes/subscription.js` |
| GET | `/status` | `routes/subscription.js` |
| POST | `/request` | `routes/subscription.js` |
| GET | `/requests` | `routes/subscription.js` |
| DELETE | `/requests/:id` | `routes/subscription.js` |
| GET | `/profile` | `routes/users.js` |
| PUT | `/profile` | `routes/users.js` |
| GET | `/library` | `routes/users.js` |
| POST | `/library/:videoId` | `routes/users.js` |
| DELETE | `/library/:videoId` | `routes/users.js` |
| GET | `/stats` | `routes/users.js` |
| GET | `/videos` | `routes/users.js` |
| GET | `/videos/search` | `routes/users.js` |
| GET | `/activities` | `routes/users.js` |
| PUT | `/password` | `routes/users.js` |
| POST | `/upload` | `routes/videos.js` |
| GET | `/user` | `routes/videos.js` |
| GET | `/:id/link` | `routes/videos.js` |
| GET | `/:id/info` | `routes/videos.js` |
| GET | `/:id/stream` | `routes/videos.js` |
| DELETE | `/:id` | `routes/videos.js` |
| GET | `/recommended` | `routes/videos.js` |
| GET | `/search` | `routes/videos.js` |
| GET | `/trending` | `routes/videos.js` |
| GET | `/debug/all` | `routes/videos.js` |
| GET | `/` | `routes/wallet.js` |
| GET | `/transactions` | `routes/wallet.js` |
| GET | `/earnings` | `routes/wallet.js` |
| GET | `/withdrawal-methods` | `routes/wallet.js` |
| POST | `/withdraw` | `routes/wallet.js` |
| GET | `/withdrawals` | `routes/wallet.js` |
| DELETE | `/withdrawals/:id` | `routes/wallet.js` |

## مثال الاستخدام

تُربط API تحت /api. رموز المستخدم والمدير سياقان مختلفان. يمنح رمز المشاركة الوصول للعارض المطبق دون حذف أو فحص مجلد خاص لمستخدم آخر.

```sh
curl -X POST http://localhost:3000/api/auth/login -H 'Content-Type: application/json' -d '{"email":"demo-user@example.test","password":"YOUR_DEMO_PASSWORD"}'
curl -X POST http://localhost:3000/api/folders -H 'Content-Type: application/json' -H 'Authorization: Bearer YOUR_DEMO_TOKEN' -d '{"name":"Synthetic folder"}'
curl -X POST http://localhost:3000/api/files/upload -H 'Authorization: Bearer YOUR_DEMO_TOKEN' -F 'file=@synthetic.txt'
curl http://localhost:3000/api/files/info/YOUR_SHARE_TOKEN
```
