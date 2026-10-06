# Deployment and troubleshooting

## Historical status

Previously deployed and tested. The source shows an evolution from videos to general files and folders; no revenue or audience figures are asserted.

نُشر واختُبر سابقاً. يبين المصدر انتقاله من الفيديو إلى الملفات والمجلدات؛ لا تُذكر أرقام إيرادات أو جمهور غير موثقة.

## Local release environment

Use fresh configuration, a disposable database/corpus and independently installed dependencies. This release never needs retired production services. Keep credentials, uploaded files, sessions, caches and signing material outside the public source. Credential removal does not revoke a provider key.

## Troubleshooting

### Database startup fails

Create the PostgreSQL database and user; compare DB_* variables with env.example.

### Android connects to itself

Use IQBOX_API_BASE_URL; emulator host is 10.0.2.2.

### Sharing page fails

Serve PHP; confirm API_BASE_URL and the token using harmless fixtures.

### Upload rejected

Check quota and extension/MIME policy; uploaded scripts are intentionally rejected.

## Current limits

External ad/payment integrations require provider configuration. Wallet records do not prove completed payouts. Storage cleanup, repeated view events and financial state transitions need database integration checks.

تحتاج الإعلانات والمدفوعات مزودين خارجيين. سجلات المحفظة لا تثبت تحويل الأموال. يلزم فحص قاعدة بيانات للتنظيف والمشاهدات والحالات المالية.
