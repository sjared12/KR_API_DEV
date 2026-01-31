# Subscription Manager GUI Guide

## Access the GUI

**Module:** Payment Service (`payment-service`)

**Pretty URLs (Recommended):**
- Local: `http://localhost:8081/subscription-manager`
- Remote: `https://your-domain.com/subscription-manager`

**Alternative URLs:**
- `http://localhost:8081/dashboards/subscriptions`
- `http://localhost:8081/subscription-manager.html`

**Default Test Credentials:**
- Username: `admin`
- Password: `changeit`

*(Create additional users via the Admin Panel)*

**Note:** The payment service runs on port 8081 by default (separate from main SimpleTix app on 8080)

---

## Features

### 1. Dashboard
The landing page after login displays:
- **Active Subscriptions Count** - Number of currently active subscriptions
- **Pending Approvals** - Refunds awaiting admin approval
- **Total Amount Paid** - Sum of all subscription payments
- **Total Refunded** - Sum of all approved refunds
- **Recent Activity** - List of most recent subscriptions

### 2. Subscriptions Tab
Complete subscription management interface:

**Actions:**
- **View All** - Browse all subscriptions with pagination
- **Filter by Status** - ACTIVE, PAUSED, PENDING, CANCELED
- **Create New** - Add new subscription (Square IDs required)
- **View Details** - See full subscription information
- **Pause** - Temporarily suspend a subscription
- **Cancel** - Permanently cancel with reason

**Information Displayed:**
- Subscription ID
- Customer Email
- Amount Paid
- Current Status
- Square Subscription ID
- Creation Date

### 3. Refunds Tab
Comprehensive refund workflow management:

**Refund Request Process:**
1. Click "+ Request Refund"
2. Select subscription from dropdown
3. Enter requested refund amount
4. Add optional reason
5. Submit for approval

**Refund Status Tracking:**
- **REQUESTED** - Initial request submitted
- **PENDING_APPROVAL** - Awaiting admin review
- **APPROVED** - Approved with fee deducted
- **REJECTED** - Request denied
- **PROCESSING** - Ready for Square
- **COMPLETED** - Successfully processed

**Automatic Fee Calculation:**
- Processing fee: 2.5% (configurable)
- Formula: `Net Refund = Requested Amount - (Requested Amount × 2.5%)`
- Example: $100 request = $2.50 fee, $97.50 refund

### 4. Admin Panel (Admin Users Only)
Requires `MANAGE_USERS` or `APPROVE_REFUNDS` permissions.

**User Management Section:**
- View all users with their permissions
- Create new users with selected permissions
- View last login time
- Manage active/inactive status

**Pending Refund Approvals Section:**
- Shows all refunds awaiting approval
- Displays automatic fee calculation
- One-click approval or rejection
- View who requested the refund

---

## Permission System

### Available Permissions:
1. **VIEW_SUBSCRIPTIONS** - View subscription list and details
2. **MANAGE_SUBSCRIPTIONS** - Create subscriptions
3. **CANCEL_SUBSCRIPTIONS** - Cancel/pause subscriptions
4. **REQUEST_REFUNDS** - Request new refunds
5. **APPROVE_REFUNDS** - Approve/reject refunds
6. **VIEW_REFUNDS** - View refund list
7. **MANAGE_USERS** - Create and manage users
8. **SYSTEM_ADMIN** - Full access (includes all above)

### Default Users:
- **admin** - SYSTEM_ADMIN (full access)
- Create additional users with specific permission sets as needed

---

## Common Workflows

### Workflow 1: New Subscription
1. Navigate to **Subscriptions** tab
2. Click **+ New Subscription**
3. Enter Square IDs and customer info
4. Click **Create**
5. View in subscriptions list

### Workflow 2: Request & Approve Refund
1. Navigate to **Refunds** tab
2. Click **+ Request Refund**
3. Select subscription and amount
4. Submit request
5. Admin reviews in **Pending Approvals** section
6. Click **Review** to see fee calculation
7. Click **Approve** or **Reject**
8. Refund shows as COMPLETED

### Workflow 3: Create New User with Limited Permissions
1. Navigate to **Admin Panel**
2. Click **+ Create User**
3. Enter username, email, password
4. Select specific permissions (e.g., REQUEST_REFUNDS only)
5. Click **Create User**
6. New user can log in with limited access

---

## Interface Elements

### Status Badges
- 🟢 **ACTIVE/COMPLETED** - Green (successful/active)
- 🟡 **PENDING/PAUSED** - Yellow (in progress/waiting)
- 🔴 **CANCELED/REJECTED** - Red (failed/denied)
- 🔵 **PROCESSING** - Blue (being processed)

### Buttons
- **Blue (Primary)** - Main actions (create, submit)
- **Green (Success)** - Positive confirmation (approve)
- **Yellow (Warning)** - Caution actions (pause, review)
- **Red (Danger)** - Destructive actions (cancel, reject)
- **Gray (Secondary)** - Secondary options

### Real-Time Updates
- Stats refresh automatically every 5 seconds
- Click **Refresh** button for immediate update
- Alerts appear for all actions (success/error)

---

## Error Handling

**Common Issues:**

1. **"Unauthorized" on login**
   - Check username/password
   - Verify user exists in system
   - Check if user is active (not disabled)

2. **"Permission denied" message**
   - Log in as admin to grant permissions
   - Go to Admin Panel → Create User
   - Select appropriate permissions

3. **"Failed to load subscriptions"**
   - Check network connection
   - Verify API is running on localhost:8080
   - Check browser console for detailed errors

4. **Fee calculation doesn't match**
   - Fee is always 2.5% of requested amount
   - Calculate: `fee = requested * 0.025`
   - Net = `requested - fee`

---

## Best Practices

1. **For Refund Approvals:**
   - Review pending approvals daily in Admin Panel
   - Check refund reason if provided
   - Verify subscription was active when requested

2. **For User Management:**
   - Create limited permission users when possible
   - Only grant SYSTEM_ADMIN to trusted admins
   - Disable inactive users instead of deleting

3. **For Subscriptions:**
   - Pause instead of cancel when possible (easier to resume)
   - Document cancellation reasons
   - Track refunded amounts for reconciliation

4. **For Auditing:**
   - All actions show who performed them and when
   - Refund approvals show who approved and notes
   - Last login tracked for user activity

---

## Tips & Tricks

- **Search & Filter** - Use status filters to find specific refunds
- **Keyboard Navigation** - Tab through form fields, Enter to submit
- **Pagination** - Navigate through pages at bottom of tables
- **Modal Windows** - Close any popup with × button or Cancel
- **Alerts** - Success/error messages auto-dismiss after 5 seconds
- **Responsive Design** - Works on desktop, tablet, and mobile

---

## Technical Notes

**Architecture:**
- Pure HTML/CSS/JavaScript (no external frameworks)
- RESTful API communication
- Basic authentication headers (username:password in base64)
- Real-time data updates via AJAX

**Browser Support:**
- Chrome/Edge/Firefox 90+
- Safari 14+
- Mobile browsers (iOS Safari, Chrome Android)

**Performance:**
- Pagination limits (default 10 items per page)
- Lazy loading of modals
- Efficient API calls
- Client-side caching where applicable

---

## Support

For API details, see **SUBSCRIPTION_MANAGEMENT.md**  
For deployment, see **DEPLOYMENT_GUIDE.md**  
For quick start, see **SUBSCRIPTION_QUICK_START.md**
