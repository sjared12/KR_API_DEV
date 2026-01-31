// Global fetch wrapper to handle token expiration
async function fetchWithAuth(url, options = {}) {
    if (!options.headers) options.headers = {};
    if (typeof token !== 'undefined' && token) {
        options.headers['Authorization'] = `Bearer ${token}`;
    }
    const response = await fetch(url, options);
    if (response.status === 401 || response.status === 403) {
        // Token expired or unauthorized, redirect to login
        window.location.href = '/payments'; // or your login route
        return Promise.reject('Session expired');
    }
    return response;
}

async function refundPlan(planId) {
    await fetchWithAuth(`/api/admin/plans/${planId}/refund`, {
        method: 'POST'
    });
    fetchPlans();
}
