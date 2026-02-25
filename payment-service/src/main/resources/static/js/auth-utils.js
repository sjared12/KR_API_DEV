// Token Auto-Refresh Utility
// Include this script in all authenticated pages

let refreshTimer = null;

async function refreshAuthToken() {
    try {
        const refreshToken = localStorage.getItem('REFRESH_TOKEN');
        if (!refreshToken) {
            console.warn('No refresh token available');
            return false;
        }

        const response = await fetch('/payments/api/auth/refresh', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken })
        });

        if (!response.ok) {
            console.error('Failed to refresh token');
            return false;
        }

        const data = await response.json();
        localStorage.setItem('AUTH_TOKEN', data.token);
        localStorage.setItem('TOKEN_EXPIRES_AT', Date.now() + data.expiresIn);
        
        console.log('Token refreshed successfully');
        setupTokenAutoRefresh(); // Schedule next refresh
        return true;
    } catch (error) {
        console.error('Token refresh error:', error);
        return false;
    }
}

function setupTokenAutoRefresh() {
    // Clear existing timer
    if (refreshTimer) {
        clearTimeout(refreshTimer);
    }

    const expiresAt = localStorage.getItem('TOKEN_EXPIRES_AT');
    if (!expiresAt) {
        return;
    }

    const now = Date.now();
    const expirationTime = parseInt(expiresAt);
    const timeUntilExpiry = expirationTime - now;
    
    // Refresh 5 minutes (300000ms) before expiration, or immediately if already expired
    const refreshTime = Math.max(0, timeUntilExpiry - 300000);
    
    console.log(`Token refresh scheduled in ${Math.round(refreshTime / 1000 / 60)} minutes`);
    
    refreshTimer = setTimeout(async () => {
        const success = await refreshAuthToken();
        if (!success) {
            console.log('Auto-refresh failed, redirecting to login');
            localStorage.clear();
            window.location.href = '/payments/login';
        }
    }, refreshTime);
}

async function performLogout() {
    const token = localStorage.getItem('AUTH_TOKEN');
    
    // Call logout API to revoke the token
    if (token) {
        try {
            await fetch('/payments/api/auth/logout', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
        } catch (error) {
            console.error('Logout API call failed:', error);
        }
    }
    
    // Clear refresh timer
    if (refreshTimer) {
        clearTimeout(refreshTimer);
    }
    
    localStorage.removeItem('AUTH_TOKEN');
    localStorage.removeItem('REFRESH_TOKEN');
    localStorage.removeItem('TOKEN_EXPIRES_AT');
    window.location.href = '/payments/login';
}

// Initialize auto-refresh on page load
function initTokenAutoRefresh() {
    const token = localStorage.getItem('AUTH_TOKEN');
    if (token) {
        setupTokenAutoRefresh();
    }
}
