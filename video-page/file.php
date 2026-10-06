<?php
/**
 * IQBox File Page - Ad Gate + App Download Prompt
 * No web preview/download - everything goes through the app
 */

require_once 'config.php';

$shareToken = '';
if (isset($_GET['token']) && !empty($_GET['token'])) {
    $shareToken = trim($_GET['token']);
}
if (empty($shareToken)) {
    $requestUri = $_SERVER['REQUEST_URI'] ?? '';
    if (preg_match('#^/file/([a-zA-Z0-9_-]+)#', $requestUri, $matches)) {
        $shareToken = $matches[1];
    }
}
if (empty($shareToken)) {
    $queryString = $_SERVER['QUERY_STRING'] ?? '';
    if (preg_match('/token=([a-zA-Z0-9_-]+)/', $queryString, $matches)) {
        $shareToken = $matches[1];
    }
}

if (empty($shareToken)) { http_response_code(404); showError('رابط غير صالح'); exit; }

$file = fetchFileInfo($shareToken);
if (!$file) { http_response_code(404); showError('الملف غير موجود أو تم حذفه'); exit; }

function fetchFileInfo($shareToken) {
    $apiUrl = API_BASE_URL . '/files/info/' . urlencode($shareToken);
    $ch = curl_init();
    curl_setopt_array($ch, [
        CURLOPT_URL => $apiUrl, CURLOPT_RETURNTRANSFER => true,
        CURLOPT_TIMEOUT => 10, CURLOPT_HTTPHEADER => ['Content-Type: application/json']
    ]);
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    if ($httpCode !== 200) return null;
    $data = json_decode($response, true);
    return ($data && $data['success']) ? $data['data']['file'] : null;
}

function showError($message) {
    ?><!DOCTYPE html><html lang="ar" dir="rtl"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>خطأ - <?php echo SITE_NAME; ?></title>
    <style>*{margin:0;padding:0;box-sizing:border-box}body{font-family:'Segoe UI',sans-serif;background:#0a0e1a;min-height:100vh;display:flex;align-items:center;justify-content:center;color:#e5e7eb}.e{text-align:center;padding:48px;background:rgba(255,255,255,.04);border-radius:24px;border:1px solid rgba(255,255,255,.08);max-width:400px}.e svg{width:64px;height:64px;color:#EF4444;margin-bottom:20px}.e p{font-size:17px;color:#9ca3af}</style></head><body>
<div id="app-unavailable" style="padding:12px;text-align:center;background:#e8edf8;color:#203054">عرض ببيانات اصطناعية. روابط المتجر تتطلب رابطاً حالياً يضبطه المشغل؛ لا نفترض توفر التطبيق في المتجر.</div><div class="e"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg><p><?php echo htmlspecialchars($message); ?></p></div></body></html><?php
}

function formatFileSize($bytes) {
    if ($bytes >= 1073741824) return number_format($bytes / 1073741824, 2) . ' GB';
    if ($bytes >= 1048576) return number_format($bytes / 1048576, 2) . ' MB';
    if ($bytes >= 1024) return number_format($bytes / 1024, 2) . ' KB';
    return $bytes . ' bytes';
}

function getFileColor($fileType) {
    $c = ['video'=>'#EF4444','image'=>'#10B981','audio'=>'#8B5CF6','pdf'=>'#F59E0B','document'=>'#3B82F6','archive'=>'#6366F1','other'=>'#6B7280'];
    return $c[$fileType] ?? $c['other'];
}

function getFileSVGIcon($fileType) {
    switch ($fileType) {
        case 'video': return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="23 7 16 12 23 17 23 7"/><rect x="1" y="5" width="15" height="14" rx="2" ry="2"/></svg>';
        case 'image': return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>';
        case 'audio': return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg>';
        case 'pdf': return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>';
        case 'document': return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>';
        case 'archive': return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 8v13H3V8"/><path d="M1 3h22v5H1z"/><path d="M10 12h4"/></svg>';
        default: return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"/><polyline points="13 2 13 9 20 9"/></svg>';
    }
}

function formatTimeAgo($date) {
    $ts = strtotime($date);
    $diff = time() - $ts;
    if ($diff < 60) return 'الآن';
    if ($diff < 3600) return floor($diff/60) . ' دقيقة';
    if ($diff < 86400) return floor($diff/3600) . ' ساعة';
    if ($diff < 2592000) return floor($diff/86400) . ' يوم';
    return date('Y-m-d', $ts);
}

$pageTitle = htmlspecialchars($file['original_name'] ?? $file['name']);
$fileSize = formatFileSize($file['file_size']);
$fileColor = getFileColor($file['file_type']);
$fileSVG = getFileSVGIcon($file['file_type']);
$fileType = $file['file_type'];
$ownerName = htmlspecialchars($file['owner_name'] ?? 'IQBox User');
$uploadDate = formatTimeAgo($file['created_at'] ?? '');
$deepLinkUrl = "iqbox://file/" . $shareToken;
$pageUrl = SITE_BASE_URL . "/file/" . $shareToken;

// File type label in Arabic
$typeLabels = ['video'=>'فيديو','image'=>'صورة','audio'=>'صوت','pdf'=>'PDF','document'=>'مستند','archive'=>'أرشيف','other'=>'ملف'];
$typeLabel = $typeLabels[$fileType] ?? 'ملف';

// Action text based on file type
$actionTexts = ['video'=>'شاهد الفيديو','image'=>'شاهد الصورة','audio'=>'استمع للصوت'];
$actionText = $actionTexts[$fileType] ?? 'افتح الملف';
?>
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo $pageTitle; ?> - <?php echo SITE_NAME; ?></title>
    <meta name="description" content="<?php echo $actionText; ?> <?php echo $pageTitle; ?> عبر تطبيق <?php echo SITE_NAME; ?>">
    <meta property="og:type" content="website">
    <meta property="og:title" content="<?php echo $pageTitle; ?>">
    <meta property="og:description" content="<?php echo $typeLabel; ?> - <?php echo $fileSize; ?> | <?php echo $actionText; ?> عبر تطبيق <?php echo SITE_NAME; ?>">
    <meta property="og:site_name" content="<?php echo SITE_NAME; ?>">
    
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Cairo:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    
    <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=<?php echo ADSENSE_CLIENT_ID; ?>" crossorigin="anonymous"></script>
    
    <style>
        :root {
            --bg: #0a0e1a;
            --card: rgba(255,255,255,0.04);
            --border: rgba(255,255,255,0.08);
            --text: #f1f5f9;
            --text2: #94a3b8;
            --muted: #64748b;
            --accent: #3b82f6;
            --file-color: <?php echo $fileColor; ?>;
        }
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: 'Cairo', 'Segoe UI', sans-serif;
            background: var(--bg); min-height: 100vh;
            color: var(--text); line-height: 1.7;
        }
        body::before {
            content: ''; position: fixed; top: 0; left: 0; right: 0; bottom: 0;
            background: radial-gradient(circle at 20% 20%, rgba(59,130,246,0.06) 0%, transparent 50%),
                        radial-gradient(circle at 80% 80%, rgba(139,92,246,0.04) 0%, transparent 50%);
            pointer-events: none; z-index: 0;
        }
        .wrap { position: relative; z-index: 1; }
        
        /* Navbar */
        .nav { background: rgba(10,14,26,0.85); backdrop-filter: blur(20px); border-bottom: 1px solid var(--border); position: sticky; top: 0; z-index: 100; }
        .nav-inner { max-width: 600px; margin: 0 auto; padding: 14px 20px; display: flex; align-items: center; justify-content: space-between; }
        .logo { display: flex; align-items: center; gap: 10px; text-decoration: none; color: white; }
        .logo-icon { width: 38px; height: 38px; background: linear-gradient(135deg, #3b82f6, #8b5cf6); border-radius: 10px; display: flex; align-items: center; justify-content: center; }
        .logo-icon svg { width: 20px; height: 20px; stroke: white; }
        .logo-text { font-size: 22px; font-weight: 800; } .logo-text span { color: #3b82f6; }
        
        /* Main Content */
        .main { max-width: 600px; margin: 0 auto; padding: 24px 20px; }
        
        /* File Card */
        .file-card { background: var(--card); border: 1px solid var(--border); border-radius: 24px; overflow: hidden; margin-bottom: 24px; }
        .file-icon-area { padding: 48px 40px; text-align: center; background: linear-gradient(135deg, rgba(255,255,255,0.02), rgba(255,255,255,0.05)); }
        .icon-wrap { width: 100px; height: 100px; background: linear-gradient(135deg, <?php echo $fileColor; ?>20, <?php echo $fileColor; ?>08); border: 2px solid <?php echo $fileColor; ?>30; border-radius: 28px; display: flex; align-items: center; justify-content: center; margin: 0 auto 20px; }
        .icon-wrap svg { width: 48px; height: 48px; stroke: <?php echo $fileColor; ?>; }
        .type-badge { display: inline-block; background: <?php echo $fileColor; ?>20; color: <?php echo $fileColor; ?>; padding: 5px 16px; border-radius: 16px; font-size: 13px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; }
        
        .file-details { padding: 28px; }
        .file-title { font-size: 20px; font-weight: 700; margin-bottom: 12px; word-break: break-word; line-height: 1.4; }
        .file-meta { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 20px; }
        .chip { display: flex; align-items: center; gap: 6px; background: rgba(255,255,255,0.05); padding: 5px 12px; border-radius: 8px; font-size: 12px; color: var(--text2); }
        .chip svg { width: 13px; height: 13px; stroke: var(--muted); }
        
        .uploader { display: flex; align-items: center; gap: 10px; padding-bottom: 20px; border-bottom: 1px solid var(--border); margin-bottom: 20px; }
        .avatar { width: 36px; height: 36px; border-radius: 50%; background: linear-gradient(135deg, #3b82f6, #8b5cf6); display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 700; color: white; }
        .uname { font-size: 14px; font-weight: 600; } .udate { font-size: 12px; color: var(--muted); }
        
        .stats { display: flex; gap: 0; background: rgba(255,255,255,0.03); border-radius: 12px; overflow: hidden; border: 1px solid var(--border); }
        .stat { flex: 1; text-align: center; padding: 14px 8px; border-left: 1px solid var(--border); }
        .stat:last-child { border-left: none; }
        .stat-val { font-size: 18px; font-weight: 800; color: var(--accent); }
        .stat-lbl { font-size: 11px; color: var(--muted); }
        
        /* App CTA - Main focus */
        .app-cta { background: linear-gradient(135deg, rgba(59,130,246,0.12), rgba(139,92,246,0.1)); border: 1px solid rgba(59,130,246,0.2); border-radius: 24px; padding: 40px 28px; text-align: center; margin-bottom: 24px; }
        .app-cta-icon { width: 72px; height: 72px; margin: 0 auto 20px; background: linear-gradient(135deg, #3b82f6, #8b5cf6); border-radius: 20px; display: flex; align-items: center; justify-content: center; box-shadow: 0 8px 32px rgba(59,130,246,0.3); }
        .app-cta-icon svg { width: 36px; height: 36px; stroke: white; }
        .app-cta h2 { font-size: 24px; font-weight: 800; margin-bottom: 8px; }
        .app-cta p { font-size: 15px; color: var(--text2); margin-bottom: 24px; max-width: 380px; margin-left: auto; margin-right: auto; }
        
        .btn-open-app { display: flex; align-items: center; justify-content: center; gap: 10px; width: 100%; padding: 16px; background: linear-gradient(135deg, #3b82f6, #6366f1); color: white; border: none; border-radius: 14px; font-size: 17px; font-weight: 700; cursor: pointer; font-family: 'Cairo', sans-serif; transition: all 0.3s; text-decoration: none; margin-bottom: 12px; }
        .btn-open-app:hover { transform: translateY(-2px); box-shadow: 0 8px 28px rgba(59,130,246,0.4); }
        .btn-open-app svg { width: 22px; height: 22px; }
        
        .btn-store { display: flex; align-items: center; justify-content: center; gap: 10px; width: 100%; padding: 14px; background: white; color: #111; border: none; border-radius: 14px; font-size: 15px; font-weight: 700; cursor: pointer; font-family: 'Cairo', sans-serif; transition: all 0.3s; text-decoration: none; }
        .btn-store:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(255,255,255,0.15); }
        .btn-store svg { width: 22px; height: 22px; }
        .btn-store-sub { font-size: 11px; color: #666; display: block; font-weight: 400; }
        
        .share-row { display: flex; gap: 10px; margin-top: 16px; }
        .btn-sm { flex: 1; display: flex; align-items: center; justify-content: center; gap: 6px; padding: 10px; border-radius: 10px; font-size: 13px; font-weight: 600; border: none; cursor: pointer; font-family: 'Cairo', sans-serif; transition: all 0.2s; }
        .btn-sm svg { width: 16px; height: 16px; }
        .btn-copy { background: rgba(59,130,246,0.12); color: #60a5fa; border: 1px solid rgba(59,130,246,0.2); }
        .btn-share { background: rgba(139,92,246,0.12); color: #a78bfa; border: 1px solid rgba(139,92,246,0.2); }
        
        /* Ad */
        .ad-space { background: var(--card); border: 1px solid var(--border); border-radius: 16px; padding: 16px; min-height: 50px; display: flex; align-items: center; justify-content: center; margin-bottom: 24px; }
        .ad-wrapper { display: none; } .ad-wrapper.ad-loaded { display: block; }
        
        /* Gate */
        .gate { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.95); display: flex; align-items: center; justify-content: center; z-index: 1000; backdrop-filter: blur(8px); }
        .gate-box { background: linear-gradient(135deg, #141828, #0a0e1a); border: 1px solid var(--border); border-radius: 28px; padding: 44px; text-align: center; max-width: 480px; width: 92%; }
        .gate-title { font-size: 22px; font-weight: 700; margin-bottom: 8px; }
        .gate-sub { font-size: 14px; color: var(--muted); margin-bottom: 24px; }
        .gate-timer { font-size: 56px; font-weight: 800; color: var(--accent); margin: 16px 0; line-height: 1; }
        .gate-bar { width: 100%; height: 6px; background: rgba(255,255,255,0.08); border-radius: 3px; overflow: hidden; margin-bottom: 24px; }
        .gate-fill { height: 100%; background: linear-gradient(90deg, #3b82f6, #8b5cf6); border-radius: 3px; transition: width 1s linear; }
        
        .footer { text-align: center; padding: 32px 0; color: var(--muted); font-size: 13px; border-top: 1px solid var(--border); }
        .footer a { color: var(--accent); text-decoration: none; }
        .hidden { display: none !important; }
        
        @media (max-width: 640px) {
            .main { padding: 16px; }
            .file-details { padding: 20px; }
            .file-title { font-size: 17px; }
            .app-cta { padding: 28px 20px; }
            .app-cta h2 { font-size: 20px; }
        }
    </style>
</head>
<body>
    <!-- Gate Ad Overlay -->
    <div class="gate" id="gateOverlay">
        <div class="gate-box">
            <div class="gate-title">جاري تحضير الملف...</div>
            <div class="gate-sub">يرجى الانتظار</div>
            <div class="gate-timer" id="gateTimer">5</div>
            <div class="gate-bar"><div class="gate-fill" id="gateProgress" style="width:0%"></div></div>
            <div class="ad-space" style="margin:20px 0;">
                <ins class="adsbygoogle" style="display:block"
                     data-ad-client="<?php echo ADSENSE_CLIENT_ID; ?>"
                     data-ad-slot="<?php echo ADSENSE_SLOT_TOP; ?>" data-ad-format="auto"
                     data-full-width-responsive="true"></ins>
            </div>
        </div>
    </div>

    <div class="wrap">
        <nav class="nav">
            <div class="nav-inner">
                <a href="/" class="logo">
                    <div class="logo-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg></div>
                    <span class="logo-text">IQ<span>Box</span></span>
                </a>
            </div>
        </nav>

        <div class="main">
            <!-- File Info Card -->
            <div class="file-card">
                <div class="file-icon-area">
                    <div class="icon-wrap"><?php echo $fileSVG; ?></div>
                    <div class="type-badge"><?php echo $typeLabel; ?></div>
                </div>
                <div class="file-details">
                    <h1 class="file-title"><?php echo $pageTitle; ?></h1>
                    <div class="file-meta">
                        <div class="chip"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 8v13H3V8"/><path d="M1 3h22v5H1z"/></svg><?php echo $fileSize; ?></div>
                        <div class="chip"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>منذ <?php echo $uploadDate; ?></div>
                    </div>
                    <div class="uploader">
                        <div class="avatar"><?php echo mb_substr($ownerName, 0, 1); ?></div>
                        <div><div class="uname"><?php echo $ownerName; ?></div><div class="udate">عبر <?php echo SITE_NAME; ?></div></div>
                    </div>
                    <div class="stats">
                        <div class="stat"><div class="stat-val"><?php echo number_format($file['views_count']); ?></div><div class="stat-lbl">مشاهدة</div></div>
                        <div class="stat"><div class="stat-val"><?php echo number_format($file['downloads_count']); ?></div><div class="stat-lbl">تحميل</div></div>
                        <div class="stat"><div class="stat-val"><?php echo $fileSize; ?></div><div class="stat-lbl">الحجم</div></div>
                    </div>
                </div>
            </div>

            <!-- Ad Space -->
            <div class="ad-wrapper">
                <div class="ad-space">
                    <ins class="adsbygoogle" style="display:block"
                         data-ad-client="<?php echo ADSENSE_CLIENT_ID; ?>"
                         data-ad-slot="<?php echo ADSENSE_SLOT_BOTTOM; ?>" data-ad-format="auto"
                         data-full-width-responsive="true"></ins>
                </div>
            </div>

            <!-- App Download CTA - Main Focus -->
            <div class="app-cta" id="appCta" style="display:none;">
                <div class="app-cta-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg>
                </div>
                <h2><?php echo $actionText; ?> عبر التطبيق</h2>
                <p>حمّل تطبيق IQBox لمشاهدة وتحميل هذا الملف. ارفع ملفاتك واربح من كل مشاهدة!</p>
                
                <a href="<?php echo $deepLinkUrl; ?>" class="btn-open-app" id="btnOpenApp">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
                    فتح في التطبيق
                </a>
                
                <a href="<?php echo htmlspecialchars(PLAY_STORE_URL ?: '#app-unavailable', ENT_QUOTES); ?>" class="btn-store" target="_blank">
                    <svg viewBox="0 0 24 24" fill="#111"><path d="M3.609 1.814L13.792 12 3.61 22.186a.996.996 0 0 1-.61-.92V2.734a1 1 0 0 1 .609-.92zm10.89 10.893l2.302 2.302-10.937 6.333 8.635-8.635zm3.199-3.199l2.807 1.626a1 1 0 0 1 0 1.732l-2.807 1.626L15.206 12l2.492-2.492zM5.864 2.658L16.8 8.99l-2.302 2.302-8.634-8.634z"/></svg>
                    <div><span class="btn-store-sub">ليس لديك التطبيق؟</span>حمّل من Google Play</div>
                </a>
                
                <div class="share-row">
                    <button class="btn-sm btn-copy" onclick="copyLink()">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
                        نسخ الرابط
                    </button>
                    <button class="btn-sm btn-share" onclick="shareFile()">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/></svg>
                        مشاركة
                    </button>
                </div>
            </div>

            <!-- Ad Space Bottom -->
            <div class="ad-wrapper">
                <div class="ad-space">
                    <ins class="adsbygoogle" style="display:block"
                         data-ad-client="<?php echo ADSENSE_CLIENT_ID; ?>"
                         data-ad-slot="<?php echo ADSENSE_SLOT_SIDEBAR; ?>" data-ad-format="auto"
                         data-full-width-responsive="true"></ins>
                </div>
            </div>
            
            <footer class="footer">
                <p>&copy; <?php echo date('Y'); ?> <a href="/"><?php echo SITE_NAME; ?></a> — مشاركة الملفات بسرعة وأمان</p>
            </footer>
        </div>
    </div>

    <script>
        const API_BASE = '<?php echo PUBLIC_API_BASE_URL; ?>';
        const SHARE_TOKEN = '<?php echo $shareToken; ?>';
        const PAGE_URL = window.location.href;
        
        function copyLink() {
            navigator.clipboard.writeText(PAGE_URL).then(function() {
                const btn = document.querySelector('.btn-copy');
                const orig = btn.innerHTML;
                btn.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg> تم!';
                setTimeout(function() { btn.innerHTML = orig; }, 2000);
            });
        }
        
        function shareFile() {
            if (navigator.share) {
                navigator.share({ title: '<?php echo addslashes($pageTitle); ?>', url: PAGE_URL });
            } else { copyLink(); }
        }
        
        let sessionToken = null;
        let adDuration = 5;
        
        async function startGateAd() {
            try {
                const response = await fetch(`${API_BASE}/filegatead/file/start`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        share_token: SHARE_TOKEN,
                        viewer_ip: '<?php echo $_SERVER['REMOTE_ADDR']; ?>',
                        fingerprint: navigator.userAgent
                    })
                });
                const data = await response.json();
                if (data.success) {
                    if (data.data.skip_ad) { hideGate(); showApp(); }
                    else { sessionToken = data.data.session_token; adDuration = data.data.ad_duration || 5; startCountdown(); }
                } else { hideGate(); showApp(); }
            } catch (e) { console.error('Gate error:', e); hideGate(); showApp(); }
        }
        
        function startCountdown() {
            let remaining = adDuration;
            const timer = document.getElementById('gateTimer');
            const progress = document.getElementById('gateProgress');
            const iv = setInterval(function() {
                remaining--;
                timer.textContent = remaining;
                progress.style.width = ((adDuration - remaining) / adDuration * 100) + '%';
                if (remaining <= 0) { clearInterval(iv); completeGate(); }
            }, 1000);
        }
        
        async function completeGate() {
            try {
                await fetch(`${API_BASE}/filegatead/file/complete`, {
                    method: 'POST', headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ session_token: sessionToken })
                });
            } catch (e) {}
            hideGate();
            showApp();
        }
        
        function hideGate() { document.getElementById('gateOverlay').classList.add('hidden'); }
        function showApp() { document.getElementById('appCta').style.display = 'block'; }
        
        // Show ad wrappers when ads load
        try { (adsbygoogle = window.adsbygoogle || []).push({}); document.querySelectorAll('.ad-wrapper').forEach(w => w.classList.add('ad-loaded')); } catch(e) {}
        
        document.addEventListener('DOMContentLoaded', startGateAd);
    </script>
</body>
</html>
