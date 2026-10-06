<?php
/**
 * IQBox Video Page - Premium Streaming Platform Design
 * صفحة فيديو احترافية بتصميم منصة بث عصرية
 */

require_once 'config.php';

// الحصول على معرف الفيديو
$videoId = getVideoId();

// جلب معلومات الفيديو
$video = null;
if (!empty($videoId)) {
    $video = fetchVideoInfo($videoId);
}

// إذا لم يوجد الفيديو
if (!isValidVideo($video)) {
    http_response_code(404);
    ?>
    <!DOCTYPE html>
    <html lang="ar" dir="rtl">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>فيديو غير موجود - <?php echo SITE_NAME; ?></title>
        <style>
            body {
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                background: linear-gradient(135deg, #0B0F1A 0%, #1a1f3a 100%);
                min-height: 100vh;
                display: flex;
                align-items: center;
                justify-content: center;
                margin: 0;
                color: #e5e7eb;
            }
            .error-container {
                text-align: center;
                padding: 40px;
            }
            h1 { font-size: 48px; margin: 0; color: #ffffff; }
            p { font-size: 20px; margin-top: 20px; color: #9ca3af; }
        </style>
    </head>
    <body>
        <div class="error-container">
            <h1>404</h1>
            <p>الفيديو غير موجود أو تم حذفه</p>
        </div>
    </body>
    </html>
    <?php
    exit;
}

// إعدادات الصفحة
$pageTitle = !empty($video['title']) ? htmlspecialchars($video['title']) : DEFAULT_VIDEO_TITLE;
$videoUrl = htmlspecialchars($video['video_url']);
$viewsCount = formatViews($video['views_count'] ?? 0);
$createdAt = formatDate($video['created_at'] ?? '');

// جلب الفيديوهات المقترحة
$recommendedVideos = fetchRecommendedVideos();
?>
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    
    <!-- SEO Meta Tags -->
    <title><?php echo $pageTitle; ?> - <?php echo SITE_NAME; ?></title>
    <meta name="description" content="شاهد الفيديو على <?php echo SITE_NAME; ?> - <?php echo $pageTitle; ?>">
    <meta name="keywords" content="فيديو, IQBox, مشاهدة فيديو">
    
    <!-- Open Graph Meta Tags -->
    <meta property="og:title" content="<?php echo $pageTitle; ?>">
    <meta property="og:description" content="شاهد الفيديو على <?php echo SITE_NAME; ?>">
    <meta property="og:type" content="video.other">
    <meta property="og:url" content="<?php echo SITE_BASE_URL . $_SERVER['REQUEST_URI']; ?>">
    <meta property="og:site_name" content="<?php echo SITE_NAME; ?>">
    
    <!-- Twitter Card -->
    <meta name="twitter:card" content="player">
    <meta name="twitter:title" content="<?php echo $pageTitle; ?>">
    <meta name="twitter:description" content="شاهد الفيديو على <?php echo SITE_NAME; ?>">
    
    <!-- Favicon -->
    <link rel="icon" type="image/x-icon" href="/favicon.ico">
    
    <!-- Google Fonts - Inter & Poppins -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Poppins:wght@400;500;600;700&display=swap" rel="stylesheet">
    
    <!-- Plyr.js CSS -->
    <link rel="stylesheet" href="https://cdn.plyr.io/3.7.8/plyr.css" />
    
    <!-- Preload Plyr SVG Sprite -->
    <link rel="preload" as="fetch" href="https://cdn.plyr.io/3.7.8/plyr.svg" crossorigin>
    
    <!-- Stylesheets -->
    <link rel="stylesheet" href="assets/css/style.css">
    
    <!-- Google AdSense -->
    <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=<?php echo ADSENSE_CLIENT_ID; ?>" crossorigin="anonymous"></script>
    
    <!-- Structured Data (JSON-LD) -->
    <script type="application/ld+json">
    {
        "@context": "https://schema.org",
        "@type": "VideoObject",
        "name": "<?php echo $pageTitle; ?>",
        "description": "شاهد الفيديو على <?php echo SITE_NAME; ?>",
        "uploadDate": "<?php echo $video['created_at'] ?? date('Y-m-d'); ?>",
        "contentUrl": "<?php echo $videoUrl; ?>",
        "embedUrl": "<?php echo $videoUrl; ?>"
    }
    </script>
</head>
<body>
    <!-- Plyr SVG Sprite (Inline for reliability) -->
    <svg style="display:none" xmlns="http://www.w3.org/2000/svg">
        <symbol id="plyr-play" viewBox="0 0 18 18"><path d="M15.562 8.1L3.87.225c-.818-.562-1.87 0-1.87.9v15.75c0 .9 1.052 1.462 1.87.9L15.563 9.9c.584-.45.584-1.35 0-1.8z"/></symbol>
        <symbol id="plyr-pause" viewBox="0 0 18 18"><path d="M6 1H3c-.6 0-1 .4-1 1v14c0 .6.4 1 1 1h3c.6 0 1-.4 1-1V2c0-.6-.4-1-1-1zm9 0h-3c-.6 0-1 .4-1 1v14c0 .6.4 1 1 1h3c.6 0 1-.4 1-1V2c0-.6-.4-1-1-1z"/></symbol>
        <symbol id="plyr-muted" viewBox="0 0 18 18"><path d="M12.4 12.5l2.1-2.1 2.1 2.1 1.4-1.4L15.9 9 18 6.9l-1.4-1.4-2.1 2.1-2.1-2.1L11 6.9 13.1 9 11 11.1zM3.786 6.008H.714C.286 6.008 0 6.31 0 6.76v4.512c0 .127.0.0.1.752h3.072l4.071 3.858c.5.3 1.143 0 1.143-.602V2.752c0-.602-.643-.9-1.143-.602L3.786 6.008z"/></symbol>
        <symbol id="plyr-volume" viewBox="0 0 18 18"><path d="M15.6 3.3c-.4-.4-1-.4-1.4 0-.4.4-.4 1 0 1.4C15.4 5.9 16 7.4 16 9c0 1.6-.6 3.1-1.8 4.3-.4.4-.4 1 0 127.0.0.1.127.0.0.1.3 0 .5-.1.7-.3C17.1 13.2 18 11.2 18 9s-.9-4.2-2.4-5.7z"/><path d="M11.282 5.282a.909.909 0 0 0 0 1.316c.735.735.995 1.458.995 2.402 0 .936-.425 1.917-.995 2.487a.909.909 0 0 0 0 1.316c.127.0.0.1 1.018.156a.725.725 0 0 0 .298-.156C13.773 11.733 14.13 10.16 14.13 9c0-.17-.002-.34-.011-.51-.053-.992-.319-2.005-1.522-3.208a.909.909 0 0 0-1.316 0zm-7.496.726H.714C.286 6.008 0 6.31 0 6.76v4.512c0 .127.0.0.1.752h3.072l4.071 3.858c.5.3 1.143 0 1.143-.602V2.752c0-.602-.643-.9-1.143-.602L3.786 6.008z"/></symbol>
        <symbol id="plyr-enter-fullscreen" viewBox="0 0 18 18"><path d="M10 3h3.6l-4 4L11 8.4l4-4V8h2V1h-7zM7 9.6l-4 4V10H1v7h7v-2H4.4l4-4z"/></symbol>
        <symbol id="plyr-exit-fullscreen" viewBox="0 0 18 18"><path d="M1 12h3.6l-4 4L2 17.4l4-4V17h2v-7H1zM16 .6l-4 4V1h-2v7h7V6h-3.6l4-4z"/></symbol>
        <symbol id="plyr-settings" viewBox="0 0 18 18"><path d="M16.135 7.784a2 2 0 0 1-1.23-2.969c.322-.536.225-.998-.094-1.316l-.31-.31c-.318-.318-.78-.415-1.316-.094a2 2 0 0 1-2.969-1.23C10.065 1.258 9.669 1 9.219 1h-.438c-.45 0-.845.258-.997.865a2 2 0 0 1-2.969 1.23c-.536-.322-.999-.225-1.317.093l-.31.31c-.318.318-.415.781-.093 1.317a2 2 0 0 1-1.23 2.969C1.26 7.935 1 8.33 1 8.781v.438c0 .127.0.0.1.997a2 2 0 0 1 1.23 2.969c-.322.536-.225.998.094 1.316l.31.31c.127.0.0.1 1.316.094a2 2 0 0 1 2.969 1.23c.127.0.0.1.997.865h.438c.45 0 .845-.258.997-.865a2 2 0 0 1 2.969-1.23c.127.0.0.1 1.317-.093l.31-.31c.318-.318.415-.781.093-1.317a2 2 0 0 1 1.23-2.969c.607-.151.865-.547.865-.997v-.438c0-.451-.26-.846-.865-.997zM9 12a3 3 0 1 1 0-6 3 3 0 0 1 0 6z"/></symbol>
        <symbol id="plyr-pip" viewBox="0 0 18 18"><path d="M13.293 3.293L7.022 9.564l1.414 1.414 6.271-6.271L17 7V1h-6z"/><path d="M13 15H3V5h5V3H2a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-6h-2v5z"/></symbol>
        <symbol id="plyr-airplay" viewBox="0 0 18 18"><path d="M16 1H2a1 1 0 0 0-1 1v10a1 1 0 0 0 1 1h3v-2H3V3h12v8h-2v2h3a1 1 0 0 0 1-1V2a1 1 0 0 0-1-1z"/><path d="M4 17h10l-5-6z"/></symbol>
        <symbol id="plyr-captions-off" viewBox="0 0 18 18"><path d="M1 1c-.6 0-1 .4-1 1v11c0 .6.4 1 1 1h4.6l2.7 2.7c.127.0.0.1.7.3.3 0 .5-.1.7-.3l2.7-2.7H17c.6 0 1-.4 1-1V2c0-.6-.4-1-1-1H1zm4.52 10.15c1.99 0 3.01-1.32 3.28-2.41l-1.29-.39c-.19.66-.78 1.45-1.99 1.45-1.14 0-2.2-.83-2.2-2.34 0-1.61 1.12-2.37 2.18-2.37 1.23 0 1.78.75 1.95 1.43l1.3-.41C8.47 4.96 7.46 3.76 5.5 3.76c-1.9 0-3.61 1.44-3.61 3.7 0 2.26 1.65 3.69 3.63 3.69zm7.57 0c1.99 0 3.01-1.32 3.28-2.41l-1.29-.39c-.19.66-.78 1.45-1.99 1.45-1.14 0-2.2-.83-2.2-2.34 0-1.61 1.12-2.37 2.18-2.37 1.23 0 1.78.75 1.95 1.43l1.3-.41c-.28-1.15-1.29-2.35-3.25-2.35-1.9 0-3.61 1.44-3.61 3.7 0 2.26 1.65 3.69 3.63 3.69z" fill-rule="evenodd" fill-opacity=".5"/></symbol>
        <symbol id="plyr-captions-on" viewBox="0 0 18 18"><path d="M1 1c-.6 0-1 .4-1 1v11c0 .6.4 1 1 1h4.6l2.7 2.7c.127.0.0.1.7.3.3 0 .5-.1.7-.3l2.7-2.7H17c.6 0 1-.4 1-1V2c0-.6-.4-1-1-1H1zm4.52 10.15c1.99 0 3.01-1.32 3.28-2.41l-1.29-.39c-.19.66-.78 1.45-1.99 1.45-1.14 0-2.2-.83-2.2-2.34 0-1.61 1.12-2.37 2.18-2.37 1.23 0 1.78.75 1.95 1.43l1.3-.41C8.47 4.96 7.46 3.76 5.5 3.76c-1.9 0-3.61 1.44-3.61 3.7 0 2.26 1.65 3.69 3.63 3.69zm7.57 0c1.99 0 3.01-1.32 3.28-2.41l-1.29-.39c-.19.66-.78 1.45-1.99 1.45-1.14 0-2.2-.83-2.2-2.34 0-1.61 1.12-2.37 2.18-2.37 1.23 0 1.78.75 1.95 1.43l1.3-.41c-.28-1.15-1.29-2.35-3.25-2.35-1.9 0-3.61 1.44-3.61 3.7 0 2.26 1.65 3.69 3.63 3.69z" fill-rule="evenodd"/></symbol>
        <symbol id="plyr-restart" viewBox="0 0 18 18"><path d="M9.7 1.2l.7 6.4-2.1-2.1c-1.9 1.9-1.9 5.1 0 7s5.1 1.9 7 0 1.9-5.1 0-7c-.2-.2-.5-.4-.7-.6L16 3.5c.127.0.0.1.9 1 3.2 3.7 2.8 9.4-1 12.6s-9.4 2.8-12.6-1c-2.8-3.3-2.8-8.3 0-11.5L1.8 3.1 8.3.6l1.4.6z"/></symbol>
        <symbol id="plyr-rewind" viewBox="0 0 18 18"><path d="M10.125 1L0 9l10.125 8v-6.171L18 17V1l-7.875 6.171z"/></symbol>
        <symbol id="plyr-fast-forward" viewBox="0 0 18 18"><path d="M7.875 7.171L0 1v16l7.875-6.171V17L18 9 7.875 1z"/></symbol>
    </svg>

    <!-- ===== HEADER ===== -->
    <header class="site-header">
        <div class="header-container">
            <!-- Logo & Brand -->
            <a href="/" class="header-brand">
                <div class="logo-placeholder">
                    <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
                        <defs>
                            <linearGradient id="logoGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                                <stop offset="0%" style="stop-color:#1D4ED8"/>
                                <stop offset="100%" style="stop-color:#3B82F6"/>
                            </linearGradient>
                        </defs>
                        <rect width="40" height="40" rx="10" fill="url(#logoGrad)"/>
                        <!-- Cloud -->
                        <path d="M28 22c1.1 0 2-.9 2-2 0-.9-.6-1.7-1.4-1.9C28.7 15.5 26.6 14 24 14c-2 0-3.8 1-4.9 2.5C18.3 16.2 17.4 16 16.5 16 13.5 16 11 18.5 11 21.5c0 .2 0 .3 0 .5H10c-1.1 0-2 .9-2 2s.9 2 2 2h18c1.1 0 2-.9 2-2s-.9-2-2-2z" fill="white"/>
                        <!-- Play -->
                        <path d="M18 19v6l5-3z" fill="#3B82F6"/>
                        <!-- Layers -->
                        <path d="M12 28h16l-2 2H14l-2-2z" fill="#06B6D4"/>
                        <path d="M14 30h12l-1 1.5H15l-1-1.5z" fill="#10B981"/>
                    </svg>
                </div>
                <span class="brand-name">IQBox</span>
            </a>

            <!-- Desktop Navigation -->
            <nav class="header-nav desktop-nav">
                <a href="#" class="nav-link">الرئيسية</a>
                <a href="#" class="nav-link">اكتشف</a>
                <a href="#" class="nav-link">المساعدة</a>
            </nav>

            <!-- Header Actions -->
            <div class="header-actions">
                <a href="#download" class="btn-download-app">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                        <path d="M12 16L12 8M12 16L8 12M12 16L16 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M12 21C16.9706 21 21 16.9706 21 12C21 7.02944 16.9706 3 12 3C7.02944 3 3 7.02944 3 12C3 16.9706 7.02944 21 12 21Z" stroke="currentColor" stroke-width="2"/>
                    </svg>
                    <span>تحميل التطبيق</span>
                </a>
                <a href="#" class="btn-signin">تسجيل الدخول</a>
            </div>

            <!-- Mobile Menu Toggle -->
            <button class="mobile-menu-toggle" aria-label="القائمة" onclick="toggleMobileMenu()">
                <span class="hamburger-line"></span>
                <span class="hamburger-line"></span>
                <span class="hamburger-line"></span>
            </button>
        </div>

        <!-- Mobile Menu -->
        <div class="mobile-menu" id="mobileMenu">
            <nav class="mobile-nav">
                <a href="#" class="mobile-nav-link">الرئيسية</a>
                <a href="#" class="mobile-nav-link">اكتشف</a>
                <a href="#" class="mobile-nav-link">المساعدة</a>
                <hr class="mobile-divider">
                <a href="#download" class="mobile-nav-link download-link">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                        <path d="M12 16L12 8M12 16L8 12M12 16L16 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M12 21C16.9706 21 21 16.9706 21 12C21 7.02944 16.9706 3 12 3C7.02944 3 3 7.02944 3 12C3 16.9706 7.02944 21 12 21Z" stroke="currentColor" stroke-width="2"/>
                    </svg>
                    تحميل التطبيق
                </a>
                <a href="#" class="mobile-nav-link signin-link">تسجيل الدخول</a>
            </nav>
        </div>
    </header>

    <!-- Loading Overlay -->
    <div id="loadingOverlay" class="loading-overlay">
        <div class="loading-spinner">
            <div class="spinner-ring"></div>
            <div class="spinner-ring"></div>
            <div class="spinner-ring"></div>
            <p>جاري تحميل الفيديو...</p>
        </div>
    </div>

    <!-- Main Container -->
    <div class="main-wrapper">
        <!-- Top Ad Banner -->
        <div class="ad-container ad-top">
            <div class="ad-placeholder">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <rect x="3" y="3" width="18" height="18" rx="2"/>
                    <path d="M3 9h18M9 3v18"/>
                </svg>
                <p>PLACE YOUR AD HERE</p>
                <span>مكان الإعلان</span>
            </div>
            <ins class="adsbygoogle"
                 style="display:none;position:absolute;top:0;left:0;width:100%;height:100%"
                 data-ad-client="<?php echo ADSENSE_CLIENT_ID; ?>"
                 data-ad-slot="<?php echo ADSENSE_SLOT_TOP; ?>"
                 data-ad-format="auto"
                 data-full-width-responsive="true"></ins>
            <script>
                (adsbygoogle = window.adsbygoogle || []).push({});
            </script>
        </div>

        <!-- Main Content -->
        <div class="content-container">
            <!-- Left Column - Video Player -->
            <div class="video-column">
                <!-- Video Player Section -->
                <div class="player-section">
                    <!-- Ad before video -->
                    <div class="ad-container ad-before-player">
                        <div class="ad-placeholder">
                            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                                <rect x="3" y="3" width="18" height="18" rx="2"/>
                                <path d="M3 9h18M9 3v18"/>
                            </svg>
                            <p>PLACE YOUR AD HERE</p>
                            <span>مكان الإعلان</span>
                        </div>
                        <ins class="adsbygoogle"
                             style="display:none;position:absolute;top:0;left:0;width:100%;height:100%"
                             data-ad-client="<?php echo ADSENSE_CLIENT_ID; ?>"
                             data-ad-slot="<?php echo ADSENSE_SLOT_TOP; ?>"
                             data-ad-format="auto"
                             data-full-width-responsive="true"></ins>
                        <script>
                            (adsbygoogle = window.adsbygoogle || []).push({});
                        </script>
                    </div>
                    
                    <div class="player-wrapper">
                        <video 
                            id="videoPlayer" 
                            class="plyr-player"
                            playsinline
                            controls
                            data-poster="">
                            <source src="<?php echo $videoUrl; ?>" type="video/mp4">
                        </video>
                    </div>
                </div>

                <!-- Video Info Card -->
                <div class="video-info-card">
                    <div class="video-header">
                        <h1 class="video-title"><?php echo $pageTitle; ?></h1>
                        <div class="video-actions">
                            <button class="action-btn like-btn" aria-label="إعجاب">
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                                    <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" fill="currentColor"/>
                                </svg>
                                <span>إعجاب</span>
                            </button>
                            <button class="action-btn save-btn" aria-label="حفظ">
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                                    <path d="M17 3H7c-1.1 0-2 .9-2 2v16l7-3 7 3V5c0-1.1-.9-2-2-2z" fill="currentColor"/>
                                </svg>
                                <span>حفظ</span>
                            </button>
                            <button class="action-btn share-btn" aria-label="مشاركة">
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                                    <path d="M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .127.0.0.1.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92 1.61 0 2.92-1.31 2.92-2.92s-1.31-2.92-2.92-2.92z" fill="currentColor"/>
                                </svg>
                                <span>مشاركة</span>
                            </button>
                        </div>
                    </div>
                    
                    <div class="video-meta">
                        <div class="meta-item">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                                <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z" fill="currentColor"/>
                            </svg>
                            <span><?php echo $viewsCount; ?> مشاهدة</span>
                        </div>
                        <div class="meta-item">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z" fill="currentColor"/>
                                <path d="M12.5 7H11v6l5.25 3.15.75-1.23-4.5-2.67z" fill="currentColor"/>
                            </svg>
                            <span><?php echo $createdAt; ?></span>
                        </div>
                    </div>
                </div>

                <!-- Description Card -->
                <div class="description-card">
                    <h3>عن الفيديو</h3>
                    <p class="video-description">
                        <?php 
                        if (!empty($video['description'])) {
                            echo nl2br(htmlspecialchars($video['description']));
                        } else {
                            echo 'شاهد هذا الفيديو المميز على ' . SITE_NAME . '. استمتع بأفضل تجربة مشاهدة مع تطبيقنا.';
                        }
                        ?>
                    </p>
                    <?php if (!empty($video['tags']) && is_array($video['tags']) && count($video['tags']) > 0): ?>
                    <div class="tags">
                        <?php foreach ($video['tags'] as $tag): ?>
                            <span class="tag"><?php echo htmlspecialchars($tag); ?></span>
                        <?php endforeach; ?>
                    </div>
                    <?php else: ?>
                    <div class="tags">
                        <span class="tag">فيديو</span>
                        <span class="tag">مشاهدة</span>
                        <span class="tag"><?php echo SITE_NAME; ?></span>
                    </div>
                    <?php endif; ?>
                </div>

                <!-- Download App Banner - Premium Conversion -->
                <div class="download-banner">
                    <div class="download-content">
                        <div class="app-icon">
                            <svg width="48" height="48" viewBox="0 0 24 24" fill="none">
                                <path d="M17 1.01L7 1c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-1.99-2-1.99zM17 19H7V5h10v14z" fill="currentColor"/>
                            </svg>
                        </div>
                        <div class="download-text">
                            <h3>شاهد أسرع وبدون إعلانات</h3>
                            <p>حمّل تطبيق <?php echo SITE_NAME; ?> لتجربة مشاهدة أفضل</p>
                        </div>
                        <button class="download-btn-premium" onclick="downloadApp()">
                            <span>حمّل التطبيق</span>
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                                <path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z" fill="currentColor"/>
                            </svg>
                        </button>
                    </div>
                </div>

                <!-- Bottom Ad -->
                <div class="ad-container ad-bottom">
                    <div class="ad-placeholder">
                        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                            <rect x="3" y="3" width="18" height="18" rx="2"/>
                            <path d="M3 9h18M9 3v18"/>
                        </svg>
                        <p>PLACE YOUR AD HERE</p>
                        <span>مكان الإعلان</span>
                    </div>
                    <ins class="adsbygoogle"
                         style="display:none;position:absolute;top:0;left:0;width:100%;height:100%"
                         data-ad-client="<?php echo ADSENSE_CLIENT_ID; ?>"
                         data-ad-slot="<?php echo ADSENSE_SLOT_BOTTOM; ?>"
                         data-ad-format="auto"
                         data-full-width-responsive="true"></ins>
                    <script>
                        (adsbygoogle = window.adsbygoogle || []).push({});
                    </script>
                </div>
            </div>

            <!-- Right Sidebar - Recommended Videos -->
            <aside class="sidebar">
                <h2 class="sidebar-title">فيديوهات مقترحة</h2>
                <div class="recommended-videos" id="recommendedVideos">
                    <?php if (!empty($recommendedVideos)): ?>
                        <?php foreach ($recommendedVideos as $recVideo): ?>
                            <div class="video-thumbnail" onclick="window.location.href='index.php?id=<?php echo $recVideo['id']; ?>'">
                                <div class="thumbnail-image">
                                    <?php if (!empty($recVideo['thumbnail_url'])): ?>
                                        <img src="<?php echo htmlspecialchars($recVideo['thumbnail_url']); ?>" alt="<?php echo htmlspecialchars($recVideo['title']); ?>">
                                    <?php endif; ?>
                                    <div class="play-overlay">
                                        <svg width="24" height="24" viewBox="0 0 24 24" fill="white">
                                            <path d="M8 5v14l11-7z"/>
                                        </svg>
                                    </div>
                                </div>
                                <div class="thumbnail-info">
                                    <h4><?php echo htmlspecialchars($recVideo['title'] ?? 'فيديو'); ?></h4>
                                    <p><?php echo formatViews($recVideo['views_count'] ?? 0); ?> مشاهدة</p>
                                </div>
                            </div>
                        <?php endforeach; ?>
                    <?php else: ?>
                        <!-- Loading skeleton -->
                        <div class="loading-skeleton">
                            <div class="skeleton-thumbnail"></div>
                            <div class="skeleton-text"></div>
                        </div>
                        <div class="loading-skeleton">
                            <div class="skeleton-thumbnail"></div>
                            <div class="skeleton-text"></div>
                        </div>
                        <div class="loading-skeleton">
                            <div class="skeleton-thumbnail"></div>
                            <div class="skeleton-text"></div>
                        </div>
                    <?php endif; ?>
                </div>
                
                <!-- Ad after Recommended Videos -->
                <div class="ad-container ad-sidebar">
                    <div class="ad-placeholder">
                        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                            <rect x="3" y="3" width="18" height="18" rx="2"/>
                            <path d="M3 9h18M9 3v18"/>
                        </svg>
                        <p>PLACE YOUR AD HERE</p>
                        <span>مكان الإعلان</span>
                    </div>
                    <ins class="adsbygoogle"
                         style="display:none;position:absolute;top:0;left:0;width:100%;height:100%"
                         data-ad-client="<?php echo ADSENSE_CLIENT_ID; ?>"
                         data-ad-slot="<?php echo ADSENSE_SLOT_BOTTOM; ?>"
                         data-ad-format="auto"
                         data-full-width-responsive="true"></ins>
                    <script>
                        (adsbygoogle = window.adsbygoogle || []).push({});
                    </script>
                </div>
            </aside>
        </div>
    </div>

    <!-- ===== FOOTER ===== -->
    <footer class="site-footer">
        <div class="footer-container">
            <!-- Footer Main Content -->
            <div class="footer-content">
                <!-- Brand Column -->
                <div class="footer-column footer-brand-column">
                    <div class="footer-brand">
                        <div class="footer-logo">
                            <svg width="48" height="48" viewBox="0 0 40 40" fill="none">
                                <defs>
                                    <linearGradient id="footerLogoGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                                        <stop offset="0%" style="stop-color:#1D4ED8"/>
                                        <stop offset="100%" style="stop-color:#3B82F6"/>
                                    </linearGradient>
                                </defs>
                                <rect width="40" height="40" rx="10" fill="url(#footerLogoGrad)"/>
                                <path d="M28 22c1.1 0 2-.9 2-2 0-.9-.6-1.7-1.4-1.9C28.7 15.5 26.6 14 24 14c-2 0-3.8 1-4.9 2.5C18.3 16.2 17.4 16 16.5 16 13.5 16 11 18.5 11 21.5c0 .2 0 .3 0 .5H10c-1.1 0-2 .9-2 2s.9 2 2 2h18c1.1 0 2-.9 2-2s-.9-2-2-2z" fill="white"/>
                                <path d="M18 19v6l5-3z" fill="#3B82F6"/>
                                <path d="M12 28h16l-2 2H14l-2-2z" fill="#06B6D4"/>
                                <path d="M14 30h12l-1 1.5H15l-1-1.5z" fill="#10B981"/>
                            </svg>
                        </div>
                        <span class="footer-brand-name">IQBox</span>
                    </div>
                    <p class="footer-tagline">تجربة مشاهدة متقدمة وآمنة. شارك فيديوهاتك مع العالم بسهولة.</p>
                    <div class="footer-social">
                        <a href="#" class="social-link" aria-label="Twitter">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                                <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/>
                            </svg>
                        </a>
                        <a href="#" class="social-link" aria-label="Instagram">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                                <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z"/>
                            </svg>
                        </a>
                        <a href="#" class="social-link" aria-label="YouTube">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                                <path d="M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 .502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z"/>
                            </svg>
                        </a>
                    </div>
                </div>

                <!-- Quick Links -->
                <div class="footer-column">
                    <h4 class="footer-heading">روابط سريعة</h4>
                    <ul class="footer-links">
                        <li><a href="#">عن المنصة</a></li>
                        <li><a href="#">تواصل معنا</a></li>
                        <li><a href="#">سياسة الخصوصية</a></li>
                        <li><a href="#">شروط الاستخدام</a></li>
                    </ul>
                </div>

                <!-- Platform -->
                <div class="footer-column">
                    <h4 class="footer-heading">المنصة</h4>
                    <ul class="footer-links">
                        <li><a href="#">تحميل التطبيق</a></li>
                        <li><a href="#">الأجهزة المدعومة</a></li>
                        <li><a href="#">مركز المساعدة</a></li>
                        <li><a href="#">الأسئلة الشائعة</a></li>
                    </ul>
                </div>

                <!-- Download App -->
                <div class="footer-column">
                    <h4 class="footer-heading">حمّل التطبيق</h4>
                    <div class="app-buttons">
                        <a href="#" class="app-store-btn">
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                                <path d="M17.05 20.28c-.98.95-2.05.8-3.08.35-1.09-.46-2.09-.48-3.24 0-1.44.62-2.2.44-3.06-.35C2.79 15.25 3.51 7.59 9.05 7.31c1.35.07 2.29.74 3.08.8 1.18-.24 2.31-.93 3.57-.84 1.51.12 2.65.72 3.4 1.8-3.12 1.87-2.38 5.98.48 7.13-.57 1.5-1.31 2.99-2.54 4.09l.01-.01zM12.03 7.25c-.15-2.23 1.66-4.07 3.74-4.25.29 2.58-2.34 4.5-3.74 4.25z"/>
                            </svg>
                            <div class="app-btn-text">
                                <span>حمّل من</span>
                                <strong>App Store</strong>
                            </div>
                        </a>
                        <a href="#" class="app-store-btn">
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                                <path d="M3.609 1.814L13.792 12 3.61 22.186a.996.996 0 0 1-.61-.92V2.734a1 1 0 0 1 .609-.92zm10.89 10.893l2.302 2.302-10.937 6.333 8.635-8.635zm3.199-3.198l2.807 1.626a1 1 0 0 1 0 1.73l-2.808 1.626L15.206 12l2.492-2.491zM5.864 2.658L16.8 8.99l-2.302 2.302-8.634-8.634z"/>
                            </svg>
                            <div class="app-btn-text">
                                <span>حمّل من</span>
                                <strong>Google Play</strong>
                            </div>
                        </a>
                    </div>
                </div>
            </div>

            <!-- Footer Bottom -->
            <div class="footer-bottom">
                <p class="copyright">© 2026 IQBox. جميع الحقوق محفوظة.</p>
                <div class="footer-bottom-links">
                    <a href="#">الخصوصية</a>
                    <span class="separator">•</span>
                    <a href="#">الشروط</a>
                    <span class="separator">•</span>
                    <a href="#">ملفات تعريف الارتباط</a>
                </div>
            </div>
        </div>
    </footer>

    <!-- Error Message -->
    <div id="errorMessage" class="error-message" style="display: none;">
        <div class="error-content">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
                <path d="M12 8V12M12 16H12.01" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
            <p id="errorText">حدث خطأ في تحميل الفيديو</p>
            <button onclick="location.reload()">إعادة المحاولة</button>
        </div>
    </div>

    <!-- Plyr.js Script -->
    <script src="https://cdn.plyr.io/3.7.8/plyr.polyfilled.js"></script>
    
    <!-- Custom Scripts -->
    <script src="assets/js/player.js"></script>
</body>
</html>
