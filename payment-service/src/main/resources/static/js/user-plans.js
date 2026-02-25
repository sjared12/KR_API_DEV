// Check authentication on page load
async function checkAuthentication() {
    const token = localStorage.getItem('AUTH_TOKEN');
    if (!token) {
        window.location.href = '/payments/';
        return false;
    }
    
    try {
        const response = await fetch('/payments/api/auth/me', {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        if (!response.ok) {
            localStorage.removeItem('AUTH_TOKEN');
            window.location.href = '/payments/';
            return false;
        }
        return true;
    } catch (error) {
        console.error('Auth check failed:', error);
        window.location.href = '/payments/';
        return false;
    }
}

// Helper function to get auth token
function getToken() {
    return localStorage.getItem('AUTH_TOKEN');
}

// Global fetch wrapper to use JWT authentication
async function fetchWithAuth(url, options = {}) {
    const token = localStorage.getItem('AUTH_TOKEN');
    if (!token) {
        window.location.href = '/payments/';
        return null;
    }
    
    const headers = {
        ...options.headers,
        'Authorization': `Bearer ${token}`
    };
    
    const response = await fetch(url, {
        ...options,
        headers
    });
    
    if (response.status === 401 || response.status === 403) {
        localStorage.removeItem('AUTH_TOKEN');
        window.location.href = '/payments/';
        return Promise.reject('Session expired');
    }
    return response;
}

// Get current user and check if admin
async function getCurrentUser() {
    try {
        const res = await fetchWithAuth('/payments/api/auth/me');
        if (res.status === 401 || res.status === 403) {
            logout();
            return null;
        }
        if (!res.ok) {
            console.error('Failed to get user info');
            return null;
        }
        const user = await res.json();
        const fullName = `${user.firstName || ''} ${user.lastName || ''}`.trim();
        document.getElementById('userEmail').textContent = fullName || user.email;
        
        console.log('User data from /api/auth/me:', user);
        console.log('User role:', user.role);
        

        return user;
    } catch (error) {
        console.error('Error getting user:', error);
        return null;
    }
}

// Load students for dropdown
async function loadStudentsForDropdown() {
    try {
        const res = await fetchWithAuth('/payments/api/students');
        if (res.status === 401 || res.status === 403) {
            logout();
            return;
        }
        
        if (!res.ok) {
            console.error('Failed to load students');
            return;
        }
        
        const students = await res.json();
        const select = document.getElementById('planStudent');
        
        // Clear existing options except first
        while (select.options.length > 1) {
            select.remove(1);
        }
        
        // Update placeholder text based on whether students exist
        if (students.length === 0) {
            select.options[0].textContent = 'No students linked - Add students in Dashboard → My Profile';
        } else {
            select.options[0].textContent = 'Select a student (optional)...';
            
            // Add students
            students.forEach(student => {
                const option = document.createElement('option');
                option.value = student.studentId;
                option.textContent = `${student.firstName} ${student.lastName} (#${student.studentId})`;
                select.appendChild(option);
            });
        }
    } catch (error) {
        console.error('Error loading students:', error);
    }
}

// Fetch all plans for current user
async function fetchPlans() {
    try {
        document.getElementById('loadingSpinner').style.display = 'block';
        document.getElementById('plansTable').style.display = 'none';
        document.getElementById('noData').style.display = 'none';
        
        const res = await fetchWithAuth('/payments/api/plans');
        if (res.status === 401 || res.status === 403) {
            logout();
            return;
        }
        if (!res.ok) {
            console.error('Failed to fetch plans:', res.status);
            showError('Failed to load plans');
            document.getElementById('noData').style.display = 'block';
            return;
        }
        const plans = await res.json();
        displayPlans(plans);
    } catch (error) {
        console.error('Error fetching plans:', error);
        showError('An error occurred while loading plans');
        document.getElementById('noData').style.display = 'block';
    } finally {
        document.getElementById('loadingSpinner').style.display = 'none';
    }
}

// Display plans in table
function displayPlans(plans) {
    allPlans = plans;
    const tbody = document.getElementById('plansBody');
    tbody.innerHTML = '';
    
    if (plans.length === 0) {
        document.getElementById('noData').style.display = 'block';
        document.getElementById('plansTable').style.display = 'none';
        return;
    }
    
    document.getElementById('plansTable').style.display = 'table';
    document.getElementById('noData').style.display = 'none';
    
    plans.forEach(plan => {
        const row = document.createElement('tr');
        const amountCents = Number(plan.amount) || 0;
        const totalOwedCents = Number(plan.totalOwed) || amountCents;
        const amountPaidCents = Number(plan.amountPaid) || 0;
        const amount = (amountCents / 100).toFixed(2);
        const totalOwed = (totalOwedCents / 100).toFixed(2);
        const amountPaid = (amountPaidCents / 100).toFixed(2);
        const progressPercent = totalOwedCents > 0 ? Math.min(100, Math.round((amountPaidCents / totalOwedCents) * 100)) : 0;
        const startDate = plan.startDate ? new Date(plan.startDate).toLocaleDateString() : 'N/A';
        const nextChargeDate = plan.nextChargeDate && plan.frequency !== 'ONE_TIME' ? new Date(plan.nextChargeDate).toLocaleDateString() : 'N/A';
        const statusBadge = plan.paused ? '<span class="badge bg-warning text-dark">PAUSED</span>' : renderStatusBadge(plan.status);
        const studentDisplay = plan.studentName ? `${plan.studentName} (#${plan.studentId})` : 'No student assigned';
        
        row.innerHTML = `
            <td>${studentDisplay}</td>
            <td>$${amount}</td>
            <td>
                <div class="d-flex justify-content-between small"><span>$${amountPaid}</span><span>$${totalOwed}</span></div>
                <div class="progress" style="height: 6px;">
                    <div class="progress-bar" role="progressbar" style="width: ${progressPercent}%;" aria-valuenow="${progressPercent}" aria-valuemin="0" aria-valuemax="100"></div>
                </div>
            </td>
            <td>${plan.frequency}</td>
            <td>${statusBadge}</td>
            <td>${startDate}</td>
            <td>${nextChargeDate}</td>
            <td>
                <button class="btn btn-sm btn-primary" onclick="openPaymentModal('${plan.id}')" style="margin-right: 0.25rem;">
                    <i class="fas fa-eye"></i> View Details
                </button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

function renderStatusBadge(status) {
    const normalized = (status || '').toUpperCase();
    switch (normalized) {
        case 'COMPLETED':
            return '<span class="badge bg-secondary">Completed</span>';
        case 'ERROR':
            return '<span class="badge bg-warning text-dark">Error</span>';
        case 'CANCELLED':
            return '<span class="badge bg-danger">Cancelled</span>';
        default:
            return '<span class="badge bg-success">Active</span>';
    }
}

// Cancel a plan
async function cancelPlan(planId, button) {
    if (!confirm('Are you sure you want to cancel this plan?')) {
        return;
    }
    
    try {
        button.disabled = true;
        const res = await fetch(`/api/plans/${planId}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${getToken()}` }
        });
        if (res.status === 401 || res.status === 403) {
            logout();
            button.disabled = false;
            return;
        }
        if (!res.ok) {
            showError('Failed to cancel plan');
            button.disabled = false;
            return;
        }
        showSuccess('Plan cancelled successfully');
        
        // Refresh plans after 1 second
        setTimeout(() => {
            fetchPlans();
        }, 1000);
    } catch (error) {
        console.error('Error cancelling plan:', error);
        showError('An error occurred while cancelling the plan');
        button.disabled = false;
    }
}

// Create new plan
document.getElementById('createPlanBtn').addEventListener('click', async () => {
    const studentId = document.getElementById('planStudent').value.trim();
    const totalOwed = document.getElementById('planTotalOwed').value.trim();
    const amountPerInstance = document.getElementById('planAmountPerInstance').value.trim();
    const frequency = document.getElementById('planFrequency').value;
    const errorDiv = document.getElementById('createError');
    
    // Reset error
    errorDiv.style.display = 'none';
    
    // Validate form
    if (!totalOwed || parseFloat(totalOwed) < 0) {
        errorDiv.style.display = 'block';
        errorDiv.textContent = 'Valid total owed amount is required';
        return;
    }
    
    if (!amountPerInstance || parseFloat(amountPerInstance) < 0) {
        errorDiv.style.display = 'block';
        errorDiv.textContent = 'Valid amount per instance is required';
        return;
    }

    if (parseFloat(amountPerInstance) > parseFloat(totalOwed)) {
        errorDiv.style.display = 'block';
        errorDiv.textContent = 'Amount per instance cannot exceed total owed';
        return;
    }
    
    if (!frequency) {
        errorDiv.style.display = 'block';
        errorDiv.textContent = 'Please select a frequency';
        return;
    }
    
    try {
        // Convert dollars to cents for the API
        const amountInCents = Math.round(parseFloat(amountPerInstance) * 100);
        const totalOwedInCents = Math.round(parseFloat(totalOwed) * 100);
        
        const body = { 
            amount: amountInCents, 
            totalOwed: totalOwedInCents,
            frequency, 
            currency: 'USD' 
        };
        
        // Add studentId if selected
        if (studentId) {
            body.studentId = studentId;
        }
        
        const res = await fetch('/payments/api/plans', {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getToken()}`
            },
            body: JSON.stringify(body)
        });
        if (res.status === 401 || res.status === 403) {
            logout();
            return;
        }
        if (!res.ok) {
            const error = await res.text();
            errorDiv.style.display = 'block';
            errorDiv.textContent = error || 'Failed to create plan';
            return;
        }
        // Clear form
        document.getElementById('planStudent').value = '';
        document.getElementById('planTotalOwed').value = '';
        document.getElementById('planAmountPerInstance').value = '';
        document.getElementById('planFrequency').value = '';
        
        // Close modal
        const modal = bootstrap.Modal.getInstance(document.getElementById('createPlanModal'));
        modal.hide();
        
        showSuccess('Plan created successfully!');
        
        // Refresh plans
        setTimeout(() => {
            fetchPlans();
        }, 1000);
    } catch (error) {
        console.error('Error creating plan:', error);
        errorDiv.style.display = 'block';
        errorDiv.textContent = 'An error occurred while creating the plan';
    }
});

// Show success message
function showSuccess(message) {
    const div = document.getElementById('successMessage');
    div.textContent = message;
    div.style.display = 'block';
    setTimeout(() => {
        div.style.display = 'none';
    }, 5000);
}

// Show error message
function showError(message) {
    const div = document.getElementById('errorMessage');
    div.textContent = message;
    div.style.display = 'block';
    setTimeout(() => {
        div.style.display = 'none';
    }, 5000);
}

// Payment Modal Functions
async function openPaymentModal(planId) {
    currentPaymentPlanId = planId;
    const plan = allPlans.find(p => p.id === planId);
    
    if (!plan) {
        alert('Plan not found');
        return;
    }

    // Update plan summary
    const totalOwedCents = Number(plan.totalOwed) || 0;
    const amountPaidCents = Number(plan.amountPaid) || 0;
    const totalOwed = (totalOwedCents / 100).toFixed(2);
    const amountPaid = (amountPaidCents / 100).toFixed(2);
    const progressPercent = totalOwedCents > 0 ? Math.min(100, Math.round((amountPaidCents / totalOwedCents) * 100)) : 0;

    document.getElementById('modalTotalOwed').textContent = totalOwed;
    document.getElementById('modalAmountPaid').textContent = amountPaid;
    document.getElementById('modalProgress').textContent = `${progressPercent}%`;
    document.getElementById('modalStatus').className = 'badge';
    const statusClass = plan.status === 'COMPLETED' ? 'bg-secondary' : 
                       plan.status === 'ERROR' ? 'bg-warning text-dark' : 
                       plan.status === 'CANCELLED' ? 'bg-danger' : 'bg-success';
    document.getElementById('modalStatus').className = `badge ${statusClass}`;
    document.getElementById('modalStatus').textContent = plan.status || 'ACTIVE';

    // Set current frequency
    document.getElementById('planFrequencyUpdate').value = plan.frequency || 'MONTHLY';

    // Update cancel button state
    const cancelBtn = document.getElementById('cancelPlanBtn');
    if (plan.status === 'CANCELLED') {
        cancelBtn.disabled = true;
        cancelBtn.textContent = 'Already Cancelled';
    } else {
        cancelBtn.disabled = false;
        cancelBtn.innerHTML = '<i class="fas fa-times-circle"></i> Cancel Plan';
    }

    // Load payment history
    await loadPaymentHistory(planId);
    // Load card info
    await loadCardInfo(planId);

    // Show modal
    const modal = new bootstrap.Modal(document.getElementById('paymentModal'));
    modal.show();
}

async function loadPaymentHistory(planId) {
    try {
        const res = await fetch(`/payments/api/payments/plan/${planId}`, {
            headers: { 'Authorization': `Bearer ${getToken()}` }
        });
        if (res.status === 401 || res.status === 403) {
            logout();
            document.getElementById('paymentHistoryList').innerHTML = '<p class="text-danger">Session expired. Please log in again.</p>';
            return;
        }
        if (!res.ok) {
            console.error('Failed to load payments');
            document.getElementById('paymentHistoryList').innerHTML = '<p class="text-danger">Failed to load payments</p>';
            return;
        }
        const payments = await res.json();
        const historyDiv = document.getElementById('paymentHistoryList');

        if (payments.length === 0) {
            historyDiv.innerHTML = '<p class="text-muted">No payments recorded yet</p>';
            return;
        }

        historyDiv.innerHTML = payments.map(payment => {
            let extraInfo = '';
            if (payment.paymentMethod === 'CHECK' && payment.checkNumber) {
                extraInfo = `<span class="ms-2 text-info">Check #: ${payment.checkNumber}</span>`;
            } else if ((payment.paymentMethod === 'CARD' || payment.paymentMethod === 'Credit Card') && payment.cardLast4) {
                extraInfo = `<span class="ms-2 text-info">Card: ****${payment.cardLast4}</span>`;
            }
            return `
            <div class="list-group-item">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <p class="mb-0"><strong>$${(payment.amount / 100).toFixed(2)}</strong></p>
                        <small class="text-muted">${payment.paymentMethod || 'Card'}${extraInfo}</small>
                    </div>
                    <div class="text-end">
                        <span class="badge ${payment.status === 'PAID' ? 'bg-success' : payment.status === 'FAILED' ? 'bg-danger' : 'bg-warning'}">${payment.status}</span>
                        <br><small class="text-muted">${new Date(payment.timestamp).toLocaleDateString()}</small>
                    </div>
                </div>
            </div>
            `;
        }).join('');
    } catch (error) {
        console.error('Error loading payments:', error);
        document.getElementById('paymentHistoryList').innerHTML = '<p class="text-danger">Error loading payments</p>';
    }
}

async function loadCardInfo(planId) {
    try {
        const res = await fetch(`/payments/api/payments/plan/${planId}/card`, {
            headers: { 'Authorization': `Bearer ${getToken()}` }
        });
        if (res.status === 401 || res.status === 403) {
            logout();
            return;
        }
        if (res.ok) {
            const card = await res.json();
            document.getElementById('cardHolderName').value = card.cardHolderName || '';
            document.getElementById('cardBrand').value = card.cardBrand || '';
            document.getElementById('cardLast4').value = card.cardLast4 || '';
        }
    } catch (error) {
        console.error('Error loading card info:', error);
    }
}

async function updateCardInfo() {
    const planId = currentPaymentPlanId;

    const cardData = {
        cardHolderName: document.getElementById('cardHolderName').value,
        cardBrand: document.getElementById('cardBrand').value,
        cardLast4: document.getElementById('cardLast4').value
    };

    try {
        const res = await fetch(`/payments/api/payments/plan/${planId}/card`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${getToken()}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(cardData)
        });
        if (res.status === 401 || res.status === 403) {
            logout();
            return;
        }
        if (res.ok) {
            showPaymentSuccess('Card information updated successfully');
            // Refresh plan data
            await fetchPlans();
        } else {
            showPaymentError('Failed to update card information');
        }
    } catch (error) {
        console.error('Error updating card info:', error);
        showPaymentError('Error updating card information');
    }
}

async function updatePlanFrequency() {
    const planId = currentPaymentPlanId;
    const frequency = document.getElementById('planFrequencyUpdate').value;

    try {
        const res = await fetch(`/payments/api/plans/${planId}/frequency`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${getToken()}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ frequency: frequency })
        });
        if (res.status === 401 || res.status === 403) {
            logout();
            return;
        }
        if (res.ok) {
            showPaymentSuccess('Payment frequency updated successfully');
            // Refresh plan data
            await fetchPlans();
        } else {
            showPaymentError('Failed to update payment frequency');
        }
    } catch (error) {
        console.error('Error updating frequency:', error);
        showPaymentError('Error updating payment frequency');
    }
}

function showPaymentSuccess(message) {
    const successDiv = document.getElementById('paymentSuccess');
    successDiv.textContent = message;
    successDiv.style.display = 'block';
    document.getElementById('paymentError').style.display = 'none';
    setTimeout(() => { successDiv.style.display = 'none'; }, 3000);
}

function showPaymentError(message) {
    const errorDiv = document.getElementById('paymentError');
    errorDiv.textContent = message;
    errorDiv.style.display = 'block';
    document.getElementById('paymentSuccess').style.display = 'none';
}

async function cancelPlanFromModal() {
    const planId = currentPaymentPlanId;
    const plan = allPlans.find(p => p.id === planId);

    if (!plan) {
        alert('Plan not found');
        return;
    }

    if (plan.status === 'CANCELLED') {
        showPaymentError('This plan is already cancelled');
        return;
    }

    if (!confirm('Are you sure you want to cancel this payment plan? This action cannot be undone.')) {
        return;
    }

    try {
        const res = await fetch(`/payments/api/plans/${planId}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${getToken()}` }
        });
        if (res.status === 401 || res.status === 403) {
            logout();
            return;
        }
        if (res.ok) {
            showPaymentSuccess('Plan cancelled successfully');
            // Close modal and refresh
            setTimeout(() => {
                bootstrap.Modal.getInstance(document.getElementById('paymentModal')).hide();
                fetchPlans();
            }, 1500);
        } else {
            showPaymentError('Failed to cancel plan');
        }
    } catch (error) {
        console.error('Error cancelling plan:', error);
        showPaymentError('Error cancelling plan');
    }
}

// Logout
function logout() {
    window.location.href = '/payments/api/auth/logout';
}

// Initialize on page load
checkAuthentication().then(async (isAuthenticated) => {
    if (!isAuthenticated) return;
    
    await getCurrentUser();
    await loadStudentsForDropdown();
    await fetchPlans();
});
