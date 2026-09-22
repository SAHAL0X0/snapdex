// SnapDex Global Configuration
(function () {
    // Verified Railway Production Backend URL
    const PRODUCTION_API_URL = 'https://snapdex-production-44f0.up.railway.app';
    const savedUrl = localStorage.getItem('snapdex_api_base_url');

    const API_BASE_URL = savedUrl 
        ? savedUrl.replace(/\/+$/, '') 
        : PRODUCTION_API_URL;

    window.SNAPDEX_CONFIG = {
        API_BASE_URL: API_BASE_URL,
        setApiBaseUrl: function(url) {
            if (url) {
                localStorage.setItem('snapdex_api_base_url', url.trim().replace(/\/+$/, ''));
            } else {
                localStorage.removeItem('snapdex_api_base_url');
            }
            window.location.reload();
        }
    };

    window.API_BASE_URL = API_BASE_URL;
})();
