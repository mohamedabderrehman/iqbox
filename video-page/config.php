<?php
/**
 * IQBox Video Page - Configuration
 * إعدادات صفحة الفيديو
 */

// إعدادات API - استخدام localhost لأن كل شيء على نفس VPS
define('API_BASE_URL', getenv('API_BASE_URL') ?: 'http://127.0.0.1:3000/api');
define('API_TIMEOUT', 10);
define('DEMO_MODE', getenv('DEMO_MODE') !== '0');

// Base URL للصفحة (للاستخدام في Open Graph)
define('SITE_BASE_URL', getenv('SITE_BASE_URL') ?: 'http://127.0.0.1:8080');
define('PUBLIC_API_BASE_URL', SITE_BASE_URL . '/api');

// إعدادات الصفحة
define('SITE_NAME', 'IQBox');
define('DEFAULT_VIDEO_TITLE', 'فيديو IQBox');

// Google AdSense (ضع كودك هنا)
define('ADSENSE_CLIENT_ID', 'ca-pub-XXXXXXXXXXXXXXXX'); // غيّر هذا
define('ADSENSE_SLOT_TOP', '1234567890'); // إعلان علوي
define('ADSENSE_SLOT_BOTTOM', '0987654321'); // إعلان سفلي
define('ADSENSE_SLOT_SIDEBAR', '1122334455'); // إعلان جانبي

// Play Store / App Store Links
define('PLAY_STORE_URL', getenv('PLAY_STORE_URL') ?: '');
define('APP_STORE_URL', getenv('APP_STORE_URL') ?: '');

/**
 * جلب معلومات الفيديو من API
 * @param string $videoId - معرف الفيديو
 * @return array|null - معلومات الفيديو أو null إذا لم يوجد
 */
function fetchVideoInfo($videoId) {
    if (empty($videoId)) {
        return null;
    }

    // استخدام API مباشرة (كل شيء على نفس VPS)
    $url = API_BASE_URL . '/videos/' . $videoId . '/info';
    
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, API_TIMEOUT);
    curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 10);
    curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
    
    // User Agent
    curl_setopt($ch, CURLOPT_USERAGENT, 'IQBox-Video-Page/1.0');
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $error = curl_error($ch);
    $errno = curl_errno($ch);
    curl_close($ch);
    
    if ($error) {
        error_log("API Error (cURL $errno): " . $error . " | URL: " . $url);
        
        // محاولة باستخدام file_get_contents كبديل
        $context = stream_context_create([
            'http' => [
                'timeout' => API_TIMEOUT,
                'method' => 'GET',
                'header' => [
                    'User-Agent: IQBox-Video-Page/1.0',
                    'Connection: close'
                ]
            ]
        ]);
        
        $response = @file_get_contents($url, false, $context);
        if ($response !== false) {
            $data = json_decode($response, true);
            if ($data && isset($data['success']) && $data['success']) {
                return $data['data']['video'] ?? null;
            }
        }
        
        return null;
    }
    
    if ($httpCode === 200) {
        $data = json_decode($response, true);
        if ($data && isset($data['success']) && $data['success']) {
            $video = $data['data']['video'] ?? null;
            // تحويل tags من string إلى array إذا لزم
            if ($video && isset($video['tags']) && is_string($video['tags'])) {
                $video['tags'] = json_decode($video['tags'], true) ?? [];
            }
            return $video;
        }
    }
    
    return null;
}

/**
 * جلب الفيديوهات المقترحة
 * @return array - قائمة الفيديوهات المقترحة
 */
function fetchRecommendedVideos() {
    $url = API_BASE_URL . '/admin/recommended';
    
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, API_TIMEOUT);
    curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 5);
    curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
    curl_setopt($ch, CURLOPT_USERAGENT, 'IQBox-Video-Page/1.0');
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $error = curl_error($ch);
    curl_close($ch);
    
    if ($error || $httpCode !== 200) {
        return [];
    }
    
    $data = json_decode($response, true);
    if ($data && isset($data['success']) && $data['success']) {
        return $data['data']['videos'] ?? [];
    }
    
    return [];
}

/**
 * الحصول على معرف الفيديو من URL
 * @return string
 */
function getVideoId() {
    return $_GET['id'] ?? '';
}

/**
 * التحقق من وجود الفيديو
 * @param array|null $video
 * @return bool
 */
function isValidVideo($video) {
    return $video !== null && isset($video['video_url']);
}

/**
 * تنسيق التاريخ
 * @param string $date
 * @return string
 */
function formatDate($date) {
    if (empty($date)) return '';
    
    $timestamp = strtotime($date);
    $now = time();
    $diff = $now - $timestamp;
    
    if ($diff < 60) {
        return 'منذ لحظات';
    } elseif ($diff < 3600) {
        $minutes = floor($diff / 60);
        return "منذ $minutes دقيقة";
    } elseif ($diff < 86400) {
        $hours = floor($diff / 3600);
        return "منذ $hours ساعة";
    } elseif ($diff < 2592000) {
        $days = floor($diff / 86400);
        return "منذ $days يوم";
    } else {
        return date('Y-m-d', $timestamp);
    }
}

/**
 * تنسيق عدد المشاهدات
 * @param int $views
 * @return string
 */
function formatViews($views) {
    if ($views < 1000) {
        return $views;
    } elseif ($views < 1000000) {
        return round($views / 1000, 1) . 'K';
    } else {
        return round($views / 1000000, 1) . 'M';
    }
}
