// SnapDex Global Configuration
(function () {
    const isLocalhost = 
        window.location.hostname === 'localhost' ||
        window.location.hostname === '127.0.0.1' ||
        window.location.protocol === 'file:';

    // Configurable production backend URL for Railway deployment
    // Can be updated or overridden in localStorage via snapdex_api_base_url
    const DEFAULT_PROD_URL = 'https://snapdex-production.up.railway.app';
    const savedUrl = localStorage.getItem('snapdex_api_base_url');

    const API_BASE_URL = savedUrl 
        ? savedUrl.replace(/\/+$/, '') 
        : (isLocalhost ? 'http://localhost:8080' : DEFAULT_PROD_URL);

    window.SNAPDEX_CONFIG = {
        API_BASE_URL: API_BASE_URL,
        IS_LOCAL: isLocalhost,
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
