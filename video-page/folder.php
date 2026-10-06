<?php
/**
 * IQBox Folder Page - Mega-style Folder Browsing
 * Professional folder sharing with browsing, individual downloads, app CTA, 2+ ad spaces
 */

require_once 'config.php';

$shareToken = '';
if (isset($_GET['token']) && !empty($_GET['token'])) {
    $shareToken = trim($_GET['token']);
}
if (empty($shareToken)) {
    $requestUri = $_SERVER['REQUEST_URI'] ?? '';
    if (preg_match('#^/folder/([a-zA-Z0-9_-]+)#', $requestUri, $matches)) {
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

$folderData = fetchFolderInfo($shareToken);
if (!$folderData) { http_response_code(404); showError('المجلد غير موجود أو تم حذفه'); exit; }

function fetchFolderInfo($shareToken) {
    $apiUrl = API_BASE_URL . '/folders/public/' . urlencode($shareToken);
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
    return ($data && $data['success']) ? $data['data'] : null;
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

function getFileColor($fileType) {
    $c = ['video'=>'#EF4444','image'=>'#10B981','audio'=>'#8B5CF6','pdf'=>'#F59E0B','document'=>'#3B82F6','archive'=>'#6366F1','other'=>'#6B7280'];
    return $c[$fileType] ?? $c['other'];
}

$folder = $folderData['folder'];
$files = $folderData['files'] ?? [];
$subFolders = $folderData['folders'] ?? [];
$pageTitle = htmlspecialchars($folder['name']);
$totalSize = formatFileSize($folder['size'] ?? 0);
$totalFiles = count($files);
$totalFolders = count($subFolders);
$ownerName = htmlspecialchars($folder['owner_name'] ?? 'IQBox User');
$downloadAllUrl = PUBLIC_API_BASE_URL . '/folders/download/' . $shareToken;
?>
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo $pageTitle; ?> - <?php echo SITE_NAME; ?></title>
    <meta name="description" content="تصفح مجلد <?php echo $pageTitle; ?> - <?php echo $totalFiles; ?> ملف - <?php echo SITE_NAME; ?>">
    <meta property="og:type" content="website">
    <meta property="og:title" content="📁 <?php echo $pageTitle; ?> - <?php echo SITE_NAME; ?>">
    <meta property="og:description" content="<?php echo $totalFiles; ?> ملف • <?php echo $totalSize; ?>">
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
            --border-hover: rgba(255,255,255,0.15);
            --text: #f1f5f9;
            --text2: #94a3b8;
            --muted: #64748b;
            --accent: #3b82f6;
            --accent-glow: rgba(59,130,246,0.12);
            --green: #10b981;
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
        .page { position: relative; z-index: 1; }
        .container { max-width: 1100px; margin: 0 auto; padding: 16px 20px; }
        
        /* Navbar */
        .navbar {
            background: rgba(10,14,26,0.85); backdrop-filter: blur(20px);
            border-bottom: 1px solid var(--border); position: sticky; top: 0; z-index: 100;
        }
        .navbar-inner {
            max-width: 1100px; margin: 0 auto; padding: 14px 20px;
            display: flex; align-items: center; justify-content: space-between;
        }
        .logo { display: flex; align-items: center; gap: 10px; text-decoration: none; color: white; }
        .logo-icon {
            width: 38px; height: 38px; background: linear-gradient(135deg, #3b82f6, #8b5cf6);
            border-radius: 10px; display: flex; align-items: center; justify-content: center;
        }
        .logo-icon svg { width: 20px; height: 20px; stroke: white; }
        .logo-text { font-size: 22px; font-weight: 800; }
        .logo-text span { color: #3b82f6; }
        .nav-app-btn {
            display: flex; align-items: center; gap: 8px;
            background: linear-gradient(135deg, #3b82f6, #8b5cf6);
            color: white; padding: 8px 18px; border-radius: 10px;
            text-decoration: none; font-size: 13px; font-weight: 700; transition: all 0.3s;
        }
        .nav-app-btn:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(59,130,246,0.4); }
        .nav-app-btn svg { width: 16px; height: 16px; }
        
        /* Folder Hero */
        .folder-hero {
            background: var(--card); border: 1px solid var(--border);
            border-radius: 20px; padding: 28px; margin-bottom: 24px;
        }
        .hero-top {
            display: flex; align-items: center; gap: 20px; margin-bottom: 20px;
        }
        .hero-icon {
            width: 72px; height: 72px; flex-shrink: 0;
            background: linear-gradient(135deg, #3b82f6, #8b5cf6);
            border-radius: 18px; display: flex; align-items: center; justify-content: center;
        }
        .hero-icon svg { width: 36px; height: 36px; stroke: white; }
        .hero-info { flex: 1; min-width: 0; }
        .hero-info h1 { font-size: 24px; font-weight: 800; margin-bottom: 8px; word-break: break-word; }
        .hero-meta {
            display: flex; flex-wrap: wrap; gap: 12px;
        }
        .hero-chip {
            display: flex; align-items: center; gap: 5px;
            background: rgba(255,255,255,0.06); padding: 5px 12px;
            border-radius: 8px; font-size: 13px; color: var(--text2);
        }
        .hero-chip svg { width: 14px; height: 14px; stroke: var(--muted); }
        .hero-chip.blue { background: var(--accent-glow); color: var(--accent); }
        .hero-chip.blue svg { stroke: var(--accent); }
        
        .hero-actions {
            display: flex; gap: 10px; flex-wrap: wrap;
        }
        .btn {
            display: inline-flex; align-items: center; gap: 8px;
            padding: 12px 24px; border-radius: 12px; font-size: 14px; font-weight: 700;
            border: none; cursor: pointer; transition: all 0.3s; font-family: 'Cairo', sans-serif;
            text-decoration: none;
        }
        .btn svg { width: 18px; height: 18px; }
        .btn-green {
            background: linear-gradient(135deg, #10b981, #059669); color: white;
        }
        .btn-green:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(16,185,129,0.3); }
        .btn-green:disabled { opacity: 0.4; cursor: not-allowed; transform: none; }
        .btn-outline {
            background: rgba(255,255,255,0.06); color: var(--text2);
            border: 1px solid var(--border);
        }
        .btn-outline:hover { background: rgba(255,255,255,0.1); }
        
        /* Breadcrumb */
        .breadcrumb {
            display: flex; align-items: center; gap: 6px; flex-wrap: wrap;
            margin-bottom: 20px; font-size: 14px;
        }
        .breadcrumb a {
            color: var(--accent); text-decoration: none; font-weight: 600;
        }
        .breadcrumb a:hover { text-decoration: underline; }
        .breadcrumb .sep { color: var(--muted); }
        .breadcrumb .current { color: var(--text); font-weight: 700; }
        
        /* Content Table / List */
        .content-section { margin-bottom: 24px; }
        .section-header {
            display: flex; align-items: center; justify-content: space-between;
            margin-bottom: 14px;
        }
        .section-title {
            font-size: 16px; font-weight: 700; display: flex; align-items: center; gap: 8px;
        }
        .section-count {
            background: var(--accent-glow); color: var(--accent);
            padding: 3px 10px; border-radius: 8px; font-size: 12px; font-weight: 700;
        }
        
        /* File List (table-like) */
        .file-list { display: flex; flex-direction: column; gap: 2px; }
        .file-row {
            display: flex; align-items: center; gap: 14px;
            padding: 14px 18px; background: var(--card);
            border: 1px solid var(--border); border-radius: 14px;
            transition: all 0.2s; text-decoration: none; color: inherit;
        }
        .file-row:hover {
            background: rgba(255,255,255,0.07); border-color: var(--border-hover);
            transform: translateX(-4px);
        }
        .file-row-icon {
            width: 44px; height: 44px; flex-shrink: 0;
            border-radius: 12px; display: flex; align-items: center; justify-content: center;
        }
        .file-row-icon svg { width: 22px; height: 22px; }
        .file-row-info { flex: 1; min-width: 0; }
        .file-row-name {
            font-size: 14px; font-weight: 600; color: var(--text);
            white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
        }
        .file-row-detail {
            font-size: 12px; color: var(--muted); display: flex; gap: 12px; margin-top: 2px;
        }
        .file-row-actions { display: flex; gap: 6px; flex-shrink: 0; }
        .icon-btn {
            width: 36px; height: 36px; border-radius: 10px; border: none;
            display: flex; align-items: center; justify-content: center;
            cursor: pointer; transition: all 0.2s; text-decoration: none;
        }
        .icon-btn svg { width: 16px; height: 16px; }
        .icon-btn-green { background: rgba(16,185,129,0.12); color: #10b981; }
        .icon-btn-green svg { stroke: #10b981; }
        .icon-btn-green:hover { background: rgba(16,185,129,0.25); }
        .icon-btn-blue { background: var(--accent-glow); color: var(--accent); }
        .icon-btn-blue svg { stroke: var(--accent); }
        .icon-btn-blue:hover { background: rgba(59,130,246,0.25); }
        
        /* Folder row special */
        .folder-row { border-color: rgba(59,130,246,0.12); }
        .folder-row:hover { border-color: rgba(59,130,246,0.3); }
        .folder-row .file-row-name { color: var(--accent); }
        .folder-row .chevron {
            width: 28px; height: 28px; border-radius: 50%;
            background: var(--accent-glow); display: flex; align-items: center; justify-content: center;
        }
        .folder-row .chevron svg { width: 16px; height: 16px; stroke: var(--accent); }
        
        /* Empty */
        .empty {
            text-align: center; padding: 60px 20px;
            background: var(--card); border: 1px solid var(--border); border-radius: 20px;
        }
        .empty svg { width: 56px; height: 56px; stroke: var(--muted); margin-bottom: 16px; }
        .empty p { color: var(--muted); font-size: 15px; }
        
        /* Ad */
        .ad-space {
            background: var(--card); border: 1px solid var(--border);
            border-radius: 16px; padding: 20px; min-height: 100px;
            display: flex; align-items: center; justify-content: center;
            margin-bottom: 24px;
        }
        
        /* App CTA */
        .app-cta {
            background: linear-gradient(135deg, rgba(59,130,246,0.1), rgba(139,92,246,0.08));
            border: 1px solid rgba(59,130,246,0.15);
            border-radius: 20px; padding: 28px; text-align: center; margin-bottom: 24px;
        }
        .app-cta h3 { font-size: 18px; font-weight: 700; margin-bottom: 6px; }
        .app-cta p { font-size: 13px; color: var(--text2); margin-bottom: 16px; }
        .store-btn {
            display: inline-flex; align-items: center; gap: 10px;
            background: white; color: #111; padding: 10px 22px;
            border-radius: 12px; text-decoration: none; font-weight: 600;
            font-size: 14px; transition: all 0.3s;
        }
        .store-btn:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(255,255,255,0.15); }
        .store-btn svg { width: 22px; height: 22px; }
        .store-btn-sub { font-size: 10px; color: #666; display: block; font-weight: 400; }
        
        /* Gate */
        .gate-overlay {
            position: fixed; top: 0; left: 0; right: 0; bottom: 0;
            background: rgba(0,0,0,0.95); display: flex; align-items: center;
            justify-content: center; z-index: 1000; backdrop-filter: blur(8px);
        }
        .gate-content {
            background: linear-gradient(135deg, #141828, #0a0e1a);
            border: 1px solid var(--border); border-radius: 28px;
            padding: 44px; text-align: center; max-width: 480px; width: 92%;
        }
        .gate-title { font-size: 22px; font-weight: 700; margin-bottom: 8px; }
        .gate-sub { font-size: 14px; color: var(--muted); margin-bottom: 24px; }
        .gate-timer { font-size: 56px; font-weight: 800; color: var(--accent); margin: 16px 0; line-height: 1; }
        .gate-progress { width: 100%; height: 6px; background: rgba(255,255,255,0.08); border-radius: 3px; overflow: hidden; margin-bottom: 24px; }
        .gate-bar { height: 100%; background: linear-gradient(90deg, #3b82f6, #8b5cf6); border-radius: 3px; transition: width 1s linear; }
        
        .footer {
            text-align: center; padding: 32px 0; color: var(--muted);
            font-size: 13px; border-top: 1px solid var(--border); margin-top: 8px;
        }
        .footer a { color: var(--accent); text-decoration: none; }
        .hidden { display: none !important; }
        
        @media (max-width: 640px) {
            .container { padding: 12px 16px; }
            .hero-top { flex-direction: column; text-align: center; }
            .hero-meta { justify-content: center; }
            .hero-actions { justify-content: center; }
            .hero-info h1 { font-size: 20px; }
            .file-row { padding: 12px 14px; gap: 10px; }
            .file-row-name { font-size: 13px; }
            .file-row-actions { gap: 4px; }
            .icon-btn { width: 32px; height: 32px; }
        }
    </style>
</head>
<body>
    <!-- Gate Ad Overlay -->
    <div class="gate-overlay" id="gateOverlay">
        <div class="gate-content">
            <div class="gate-title">جاري تحضير المجلد...</div>
            <div class="gate-sub">يرجى الانتظار لتفعيل التحميل</div>
            <div class="gate-timer" id="gateTimer">5</div>
            <div class="gate-progress"><div class="gate-bar" id="gateBar" style="width:0%"></div></div>
            <div class="ad-space" style="margin:20px 0;border:none;">
                <ins class="adsbygoogle" style="display:block"
                     data-ad-client="<?php echo ADSENSE_CLIENT_ID; ?>"
                     data-ad-slot="<?php echo ADSENSE_SLOT_TOP; ?>" data-ad-format="auto"
                     data-full-width-responsive="true"></ins>
            </div>
        </div>
    </div>

    <div class="page">
        <nav class="navbar">
            <div class="navbar-inner">
                <a href="/" class="logo">
                    <div class="logo-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg></div>
                    <span class="logo-text">IQ<span>Box</span></span>
                </a>
                <a href="<?php echo htmlspecialchars(PLAY_STORE_URL ?: '#app-unavailable', ENT_QUOTES); ?>" class="nav-app-btn" target="_blank">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                    حمّل التطبيق
                </a>
            </div>
        </nav>

        <div class="container">
            <!-- Ad Space 1 -->
            <div class="ad-space">
                <ins class="adsbygoogle" style="display:block"
                     data-ad-client="<?php echo ADSENSE_CLIENT_ID; ?>"
                     data-ad-slot="<?php echo ADSENSE_SLOT_TOP; ?>" data-ad-format="horizontal"
                     data-full-width-responsive="true"></ins>
            </div>

            <!-- Folder Hero -->
            <div class="folder-hero">
                <div class="hero-top">
                    <div class="hero-icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
                    </div>
                    <div class="hero-info">
                        <h1><?php echo $pageTitle; ?></h1>
                        <div class="hero-meta">
                            <div class="hero-chip blue">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                                <?php echo $totalFiles; ?> ملف
                            </div>
                            <?php if ($totalFolders > 0): ?>
                            <div class="hero-chip">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
                                <?php echo $totalFolders; ?> مجلد
                            </div>
                            <?php endif; ?>
                            <div class="hero-chip">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 8v13H3V8"/><path d="M1 3h22v5H1z"/></svg>
                                <?php echo $totalSize; ?>
                            </div>
                            <div class="hero-chip">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                                <?php echo $ownerName; ?>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="hero-actions">
                    <button class="btn btn-green" id="downloadAllBtn" disabled onclick="downloadAll()">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                            <polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
                        </svg>
                        تحميل الكل (ZIP)
                    </button>
                    <button class="btn btn-outline" onclick="copyLink()">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
                            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
                        </svg>
                        نسخ الرابط
                    </button>
                </div>
            </div>

            <!-- Breadcrumb -->
            <div class="breadcrumb">
                <svg viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="2" style="width:16px;height:16px"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
                <span class="current"><?php echo $pageTitle; ?></span>
            </div>

            <?php if (!empty($subFolders)): ?>
            <!-- Sub Folders -->
            <div class="content-section">
                <div class="section-header">
                    <div class="section-title">
                        <svg viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="2" style="width:18px;height:18px"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
                        المجلدات
                        <span class="section-count"><?php echo $totalFolders; ?></span>
                    </div>
                </div>
                <div class="file-list">
                    <?php foreach ($subFolders as $sf): ?>
                    <a href="/folder/<?php echo htmlspecialchars($sf['share_token']); ?>" class="file-row folder-row">
                        <div class="file-row-icon" style="background: var(--accent-glow);">
                            <svg viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
                        </div>
                        <div class="file-row-info">
                            <div class="file-row-name"><?php echo htmlspecialchars($sf['name']); ?></div>
                            <div class="file-row-detail">
                                <span>مجلد</span>
                            </div>
                        </div>
                        <div class="chevron">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
                        </div>
                    </a>
                    <?php endforeach; ?>
                </div>
            </div>
            <?php endif; ?>

            <?php if (!empty($files)): ?>
            <!-- Files -->
            <div class="content-section">
                <div class="section-header">
                    <div class="section-title">
                        <svg viewBox="0 0 24 24" fill="none" stroke="var(--text2)" stroke-width="2" style="width:18px;height:18px"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                        الملفات
                        <span class="section-count"><?php echo $totalFiles; ?></span>
                    </div>
                </div>
                <div class="file-list">
                    <?php foreach ($files as $f):
                        $icon = getFileSVGIcon($f['file_type']);
                        $color = getFileColor($f['file_type']);
                        $fSize = formatFileSize($f['file_size']);
                        $fToken = htmlspecialchars($f['share_token']);
                    ?>
                    <div class="file-row">
                        <div class="file-row-icon" style="background: <?php echo $color; ?>15;">
                            <span style="stroke:<?php echo $color; ?>;"><?php echo $icon; ?></span>
                        </div>
                        <div class="file-row-info">
                            <div class="file-row-name"><?php echo htmlspecialchars($f['name']); ?></div>
                            <div class="file-row-detail">
                                <span><?php echo $fSize; ?></span>
                                <span style="color:<?php echo $color; ?>; text-transform:uppercase; font-weight:600;"><?php echo $f['file_type']; ?></span>
                                <span><?php echo number_format($f['downloads_count']); ?> تحميل</span>
                            </div>
                        </div>
                        <div class="file-row-actions">
                            <a href="/file/<?php echo $fToken; ?>" class="icon-btn icon-btn-blue" title="عرض">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                            </a>
                            <a href="<?php echo PUBLIC_API_BASE_URL; ?>/files/download/<?php echo $fToken; ?>" class="icon-btn icon-btn-green" title="تحميل">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                            </a>
                        </div>
                    </div>
                    <?php endforeach; ?>
                </div>
            </div>
            <?php endif; ?>

            <?php if (empty($files) && empty($subFolders)): ?>
            <div class="empty">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/><line x1="9" y1="13" x2="15" y2="13"/></svg>
                <p>هذا المجلد فارغ</p>
            </div>
            <?php endif; ?>

            <!-- Ad Space 2 -->
            <div class="ad-space">
                <ins class="adsbygoogle" style="display:block"
                     data-ad-client="<?php echo ADSENSE_CLIENT_ID; ?>"
                     data-ad-slot="<?php echo ADSENSE_SLOT_BOTTOM; ?>" data-ad-format="auto"
                     data-full-width-responsive="true"></ins>
            </div>

            <!-- App CTA -->
            <div class="app-cta">
                <h3>ارفع ملفاتك واربح مع IQBox</h3>
                <p>شارك ملفاتك واحصل على أرباح من كل تحميل. حمّل التطبيق الآن!</p>
                <a href="<?php echo htmlspecialchars(PLAY_STORE_URL ?: '#app-unavailable', ENT_QUOTES); ?>" class="store-btn" target="_blank">
                    <svg viewBox="0 0 24 24" fill="#111"><path d="M3.609 1.814L13.792 12 3.61 22.186a.996.996 0 0 1-.61-.92V2.734a1 1 0 0 1 .609-.92zm10.89 10.893l2.302 2.302-10.937 6.333 8.635-8.635zm3.199-3.199l2.807 1.626a1 1 0 0 1 0 1.732l-2.807 1.626L15.206 12l2.492-2.492zM5.864 2.658L16.8 8.99l-2.302 2.302-8.634-8.634z"/></svg>
                    <div><span class="store-btn-sub">متوفر على</span>Google Play</div>
                </a>
            </div>

            <!-- Ad Space 3 -->
            <div class="ad-space">
                <ins class="adsbygoogle" style="display:block"
                     data-ad-client="<?php echo ADSENSE_CLIENT_ID; ?>"
                     data-ad-slot="<?php echo ADSENSE_SLOT_SIDEBAR; ?>" data-ad-format="auto"
                     data-full-width-responsive="true"></ins>
            </div>

            <footer class="footer">
                <p>&copy; <?php echo date('Y'); ?> <a href="/"><?php echo SITE_NAME; ?></a> — مشاركة الملفات بسرعة وأمان</p>
            </footer>
        </div>
    </div>

    <script>
        const API_BASE = '<?php echo PUBLIC_API_BASE_URL; ?>';
        const SHARE_TOKEN = '<?php echo $shareToken; ?>';
        const DOWNLOAD_ALL_URL = '<?php echo $downloadAllUrl; ?>';
        
        let sessionToken = null, adDuration = 5, canAccess = false;
        
        function copyLink() {
            navigator.clipboard.writeText(window.location.href).then(() => {
                const btn = document.querySelector('.btn-outline');
                const orig = btn.innerHTML;
                btn.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg> تم!';
                setTimeout(() => { btn.innerHTML = orig; }, 2000);
            });
        }
        
        async function startGateAd() {
            try {
                const r = await fetch(`${API_BASE}/filegatead/folder/start`, {
                    method: 'POST', headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ share_token: SHARE_TOKEN, viewer_ip: '<?php echo $_SERVER['REMOTE_ADDR']; ?>', fingerprint: navigator.userAgent })
                });
                const d = await r.json();
                if (d.success) {
                    if (d.data.skip_ad) { hideGate(); enableAccess(); }
                    else { sessionToken = d.data.session_token; adDuration = d.data.ad_duration || 5; startCountdown(); }
                } else { hideGate(); enableAccess(); }
            } catch (e) { hideGate(); enableAccess(); }
        }
        
        function startCountdown() {
            let rem = adDuration;
            const iv = setInterval(() => {
                rem--;
                document.getElementById('gateTimer').textContent = rem;
                document.getElementById('gateBar').style.width = ((adDuration - rem) / adDuration * 100) + '%';
                if (rem <= 0) { clearInterval(iv); completeGate(); }
            }, 1000);
        }
        
        async function completeGate() {
            try {
                const r = await fetch(`${API_BASE}/filegatead/folder/complete`, {
                    method: 'POST', headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ session_token: sessionToken })
                });
                const d = await r.json();
                if (d.success) { hideGate(); enableAccess(); }
            } catch (e) { hideGate(); enableAccess(); }
        }
        
        function hideGate() { document.getElementById('gateOverlay').classList.add('hidden'); }
        function enableAccess() {
            canAccess = true;
            document.getElementById('downloadAllBtn').disabled = false;
        }
        function downloadAll() {
            if (!canAccess) return;
            const url = new URL(DOWNLOAD_ALL_URL, window.location.origin);
            if (sessionToken) url.searchParams.set('session_token', sessionToken);
            window.location.href = url.toString();
        }
        
        try { (adsbygoogle = window.adsbygoogle || []).push({}); } catch(e) {}
        try { (adsbygoogle = window.adsbygoogle || []).push({}); } catch(e) {}
        try { (adsbygoogle = window.adsbygoogle || []).push({}); } catch(e) {}
        try { (adsbygoogle = window.adsbygoogle || []).push({}); } catch(e) {}
        document.addEventListener('DOMContentLoaded', startGateAd);
    </script>
</body>
</html>
