/**
 * IQBox Video Player - Premium Plyr.js Integration
 * مشغل فيديو احترافي باستخدام Plyr.js
 */

(function() {
    'use strict';

    // Elements
    const video = document.getElementById('videoPlayer');
    const loadingOverlay = document.getElementById('loadingOverlay');
    const errorMessage = document.getElementById('errorMessage');
    const errorText = document.getElementById('errorText');

    // Plyr instance
    let player = null;

    // State
    let hasError = false;

    // ===== Skeleton Loading =====
    function showPlayerSkeleton() {
        const wrapper = document.querySelector('.player-wrapper');
        if (wrapper && !wrapper.querySelector('.player-skeleton')) {
            const skeleton = document.createElement('div');
            skeleton.className = 'player-skeleton';
            skeleton.style.cssText = `
                position: absolute;
                inset: 0;
                background: var(--bg-hover);
                border-radius: var(--radius-xl);
                animation: skeleton-pulse 1.5s ease-in-out infinite;
                z-index: 1;
            `;
            wrapper.appendChild(skeleton);
        }
    }

    function hidePlayerSkeleton() {
        const skeleton = document.querySelector('.player-skeleton');
        if (skeleton) {
            skeleton.style.opacity = '0';
            skeleton.style.transition = 'opacity 0.3s';
            setTimeout(() => skeleton.remove(), 300);
        }
    }

    // ===== Initialize =====
    function init() {
        if (!video) {
            console.error('Video element not found');
            return;
        }

        // Show skeleton while loading
        showPlayerSkeleton();

        // Initialize Plyr
        player = new Plyr(video, {
            // Player settings
            controls: [
                'play-large',
                'play',
                'progress',
                'current-time',
                'mute',
                'volume',
                'settings',
                'fullscreen'
            ],
            settings: ['quality', 'speed'],
            speed: {
                selected: 1,
                options: [0.5, 0.75, 1, 1.25, 1.5, 1.75, 2]
            },
            keyboard: {
                focused: true,
                global: true
            },
            tooltips: {
                controls: true,
                seek: true
            },
            captions: {
                active: false,
                language: 'ar',
                update: false
            },
            fullscreen: {
                enabled: true,
                fallback: true,
                iosNative: true
            },
            ratio: '16:9',
            autoplay: false,
            clickToPlay: true,
            hideControls: true,
            resetOnEnd: false,
            invertTime: false,
            toggleInvert: true,
            displayDuration: true,
            loadSprite: false,
            iconPrefix: 'plyr',
            blankVideo: 'https://cdn.plyr.io/static/blank.mp4'
        });

        setupEventListeners();
        setupKeyboardControls();
    }

    // ===== Event Listeners =====
    function setupEventListeners() {
        // Plyr events
        player.on('ready', () => {
            console.log('Plyr player ready');
            hideLoading();
            hidePlayerSkeleton();
        });

        player.on('loadstart', () => {
            console.log('Video: Load start');
            showLoading();
            showPlayerSkeleton();
            hideError();
        });

        player.on('loadedmetadata', () => {
            console.log('Video: Metadata loaded');
        });

        player.on('loadeddata', () => {
            console.log('Video: Data loaded');
        });

        player.on('canplay', () => {
            console.log('Video: Can play');
        });

        player.on('canplaythrough', () => {
            console.log('Video: Can play through');
            hideLoading();
            hidePlayerSkeleton();
        });

        player.on('play', () => {
            console.log('Video: Playing');
        });

        player.on('pause', () => {
            console.log('Video: Paused');
        });

        player.on('ended', () => {
            console.log('Video: Ended');
        });

        player.on('error', (event) => {
            console.error('Video error:', event);
            hasError = true;
            hideLoading();
            showError('حدث خطأ في تحميل الفيديو. يرجى المحاولة مرة أخرى.');
        });

        player.on('waiting', () => {
            console.log('Video: Waiting for data');
        });

        player.on('playing', () => {
            console.log('Video: Playing');
            hideLoading();
            hidePlayerSkeleton();
        });

        player.on('timeupdate', () => {
            // Track progress if needed
        });

        player.on('volumechange', () => {
            // Volume changed
        });

        player.on('qualitychange', () => {
            console.log('Quality changed');
        });

        player.on('speedchange', () => {
            console.log('Speed changed');
        });

        player.on('enterfullscreen', () => {
            console.log('Entered fullscreen');
        });

        player.on('exitfullscreen', () => {
            console.log('Exited fullscreen');
        });
    }

    // ===== Keyboard Controls =====
    function setupKeyboardControls() {
        document.addEventListener('keydown', (e) => {
            // Don't trigger if user is typing
            if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') {
                return;
            }

            if (!player) return;

            switch(e.key) {
                case ' ': // Spacebar
                    e.preventDefault();
                    player.togglePlay();
                    break;
                case 'ArrowLeft':
                    e.preventDefault();
                    player.rewind(10);
                    break;
                case 'ArrowRight':
                    e.preventDefault();
                    player.forward(10);
                    break;
                case 'ArrowUp':
                    e.preventDefault();
                    player.increaseVolume(0.1);
                    break;
                case 'ArrowDown':
                    e.preventDefault();
                    player.decreaseVolume(0.1);
                    break;
                case 'f': // Fullscreen
                case 'F':
                    e.preventDefault();
                    player.fullscreen.toggle();
                    break;
                case 'm': // Mute
                case 'M':
                    e.preventDefault();
                    player.muted = !player.muted;
                    break;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    e.preventDefault();
                    const percent = parseInt(e.key) / 10;
                    player.currentTime = player.duration * percent;
                    break;
            }
        });
    }

    // ===== Download App =====
    function downloadApp() {
        const userAgent = navigator.userAgent || navigator.vendor || window.opera;
        
        // Android
        if (/android/i.test(userAgent)) {
            window.open('https://play.google.com/store/apps/details?id=com.iqbox.app', '_blank');
            return;
        }
        
        // iOS
        if (/iPad|iPhone|iPod/.test(userAgent) && !window.MSStream) {
            window.open('https://apps.apple.com/app/iqbox/id123456789', '_blank');
            return;
        }
        
        // Default
        window.open('https://play.google.com/store/apps/details?id=com.iqbox.app', '_blank');
    }

    // Make downloadApp global
    window.downloadApp = downloadApp;

    // ===== UI Helpers =====
    function showLoading() {
        if (loadingOverlay) {
            loadingOverlay.classList.remove('hidden');
        }
    }

    function hideLoading() {
        if (loadingOverlay) {
            loadingOverlay.classList.add('hidden');
        }
    }

    function showError(message) {
        if (errorMessage && errorText) {
            errorText.textContent = message || 'حدث خطأ في تحميل الفيديو';
            errorMessage.style.display = 'flex';
        }
    }

    function hideError() {
        if (errorMessage) {
            errorMessage.style.display = 'none';
        }
        hasError = false;
    }

    // ===== Load Recommended Videos =====
    function loadRecommendedVideos() {
        const container = document.getElementById('recommendedVideos');
        if (!container) return;

        fetch('http://localhost:3000/api/admin/recommended')
            .then(response => response.json())
            .then(data => {
                if (data.success && data.data.videos.length > 0) {
                    container.innerHTML = '';
                    data.data.videos.forEach(video => {
                        const videoElement = createRecommendedVideoElement(video);
                        container.appendChild(videoElement);
                    });
                }
            })
            .catch(error => {
                console.error('Error loading recommended videos:', error);
                // Keep skeleton or show fallback
            });
    }

    function createRecommendedVideoElement(video) {
        const div = document.createElement('div');
        div.className = 'video-thumbnail';
        div.onclick = () => {
            window.location.href = `index.php?id=${video.id}`;
        };

        const views = formatViews(video.views_count || 0);
        const thumbnailUrl = video.thumbnail_url || '';

        div.innerHTML = `
            <div class="thumbnail-image">
                ${thumbnailUrl ? `<img src="${thumbnailUrl}" alt="${video.title}">` : ''}
                <div class="play-overlay">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="white">
                        <path d="M8 5v14l11-7z"/>
                    </svg>
                </div>
            </div>
            <div class="thumbnail-info">
                <h4>${video.title || 'فيديو'}</h4>
                <p>${views} مشاهدة</p>
            </div>
        `;

        return div;
    }

    function formatViews(views) {
        if (!views) return '0';
        if (views < 1000) return views.toString();
        if (views < 1000000) return (views / 1000).toFixed(1) + 'K';
        return (views / 1000000).toFixed(1) + 'M';
    }

    // ===== Ad Placeholder Management =====
    function hideAdPlaceholders() {
        // Show all placeholders by default
        document.querySelectorAll('.ad-placeholder').forEach(placeholder => {
            placeholder.classList.remove('hidden');
        });
        
        // Only hide when actual ad loads
        document.querySelectorAll('.ad-container').forEach(container => {
            const adElement = container.querySelector('ins.adsbygoogle');
            const placeholder = container.querySelector('.ad-placeholder');
            
            if (!adElement || !placeholder) return;
            
            const checkAd = () => {
                const status = adElement.getAttribute('data-adsbygoogle-status');
                if (status === 'done' || status === 'filled') {
                    placeholder.classList.add('hidden');
                }
            };
            
            // Check after delay and observe changes
            setTimeout(checkAd, 2000);
            const observer = new MutationObserver(checkAd);
            observer.observe(adElement, { attributes: true, attributeFilter: ['data-adsbygoogle-status'] });
        });
    }

    // ===== Ambient Glow from Video Thumbnail =====
    function createAmbientGlow() {
        const playerSection = document.querySelector('.player-section');
        if (!playerSection) return;

        // محاولة استخراج لون من thumbnail الفيديو
        const video = document.getElementById('videoPlayer');
        if (!video) return;

        // إنشاء canvas لاستخراج الألوان
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');
        
        video.addEventListener('loadedmetadata', () => {
            try {
                canvas.width = video.videoWidth || 640;
                canvas.height = video.videoHeight || 360;
                
                ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
                
                // استخراج لون من مركز الفيديو
                const centerX = Math.floor(canvas.width / 2);
                const centerY = Math.floor(canvas.height / 2);
                const imageData = ctx.getImageData(centerX - 50, centerY - 50, 100, 100);
                
                // حساب متوسط اللون
                let r = 0, g = 0, b = 0, count = 0;
                for (let i = 0; i < imageData.data.length; i += 4) {
                    r += imageData.data[i];
                    g += imageData.data[i + 1];
                    b += imageData.data[i + 2];
                    count++;
                }
                
                r = Math.floor(r / count);
                g = Math.floor(g / count);
                b = Math.floor(b / count);
                
                // تطبيق Ambient Glow
                const glowColor = `rgba(${r}, ${g}, ${b}, 0.15)`;
                playerSection.style.setProperty('--ambient-glow-color', glowColor);
                
                // إضافة glow إضافي
                const ambientGlow = document.createElement('div');
                ambientGlow.className = 'ambient-glow-dynamic';
                ambientGlow.style.cssText = `
                    position: absolute;
                    inset: -100px;
                    background: radial-gradient(ellipse at center, ${glowColor} 0%, transparent 70%);
                    border-radius: var(--radius-2xl);
                    z-index: -1;
                    filter: blur(60px);
                    pointer-events: none;
                    opacity: 0.5;
                `;
                playerSection.appendChild(ambientGlow);
            } catch (error) {
                console.log('Ambient glow: Using default red glow');
            }
        });
    }

    // ===== Initialize when DOM is ready =====
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            init();
            loadRecommendedVideos();
            hideAdPlaceholders();
            createAmbientGlow();
        });
    } else {
        init();
        loadRecommendedVideos();
        hideAdPlaceholders();
        createAmbientGlow();
    }

    // ===== Expose API =====
    window.IQBoxPlayer = {
        play: () => player?.play(),
        pause: () => player?.pause(),
        toggle: () => player?.togglePlay(),
        setVolume: (vol) => { if (player) player.volume = Math.max(0, Math.min(1, vol)); },
        getVolume: () => player?.volume || 0,
        mute: () => { if (player) player.muted = true; },
        unmute: () => { if (player) player.muted = false; },
        seek: (time) => { if (player) player.currentTime = time; },
        getCurrentTime: () => player?.currentTime || 0,
        getDuration: () => player?.duration || 0,
        setSpeed: (speed) => { if (player) player.speed = speed; },
        getSpeed: () => player?.speed || 1,
        toggleFullscreen: () => player?.fullscreen.toggle(),
        destroy: () => { if (player) player.destroy(); }
    };

})();

// ===== Mobile Menu Toggle =====
function toggleMobileMenu() {
    const mobileMenu = document.getElementById('mobileMenu');
    const menuToggle = document.querySelector('.mobile-menu-toggle');
    
    if (mobileMenu && menuToggle) {
        mobileMenu.classList.toggle('active');
        menuToggle.classList.toggle('active');
        
        // Animate hamburger to X
        const lines = menuToggle.querySelectorAll('.hamburger-line');
        if (menuToggle.classList.contains('active')) {
            lines[0].style.transform = 'rotate(45deg) translate(5px, 5px)';
            lines[1].style.opacity = '0';
            lines[2].style.transform = 'rotate(-45deg) translate(5px, -5px)';
        } else {
            lines[0].style.transform = 'none';
            lines[1].style.opacity = '1';
            lines[2].style.transform = 'none';
        }
    }
}

// Close mobile menu when clicking outside
document.addEventListener('click', function(event) {
    const mobileMenu = document.getElementById('mobileMenu');
    const menuToggle = document.querySelector('.mobile-menu-toggle');
    
    if (mobileMenu && menuToggle) {
        if (!mobileMenu.contains(event.target) && !menuToggle.contains(event.target)) {
            if (mobileMenu.classList.contains('active')) {
                toggleMobileMenu();
            }
        }
    }
});
