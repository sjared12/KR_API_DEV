// Get token from localStorage
function getToken() {
    return localStorage.getItem('AUTH_TOKEN');
}

// Global fetch wrapper to handle token expiration
async function fetchWithAuth(url, options = {}) {
    if (!options.headers) options.headers = {};
    const token = getToken();
    if (token) {
        options.headers['Authorization'] = `Bearer ${token}`;
    }
    const response = await fetch(url, options);
    if (response.status === 401 || response.status === 403) {
        logout();
        return Promise.reject('Session expired');
    }
    return response;
}

async function refundPlan(planId) {
    await fetchWithAuth(`/payments/api/admin/plans/${planId}/refund`, {
        method: 'POST'
    });
    fetchAllPlans();
}
