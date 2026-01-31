# Subscription Management System

This document describes the subscription management system integrated with Square POS software. The system allows management of subscriptions, with features for cancellation, refund requests with approval workflow, and automatic fee deduction.

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [User Authentication](#user-authentication)
4. [Subscription Management](#subscription-management)
5. [Refund Management](#refund-management)
6. [API Endpoints](#api-endpoints)
7. [Database Schema](#database-schema)
8. [Configuration](#configuration)
9. [Integration with Square](#integration-with-square)
10. [Error Handling](#error-handling)

---

## Overview

The Subscription Management System provides:

- **User Management**: Database-backed user authentication with role-based permissions
- **Subscription Tracking**: Manage Square subscriptions with status tracking
- **Refund Workflow**: Request refunds with automatic fee deduction and approval process
- **Permission System**: Granular control over who can perform specific actions
- **Audit Trail**: Track who approved/rejected refunds and when

### Key Features

- ✅ Username/password authentication with bcrypt hashing
- ✅ Permission-based access control
- ✅ Refund approval workflow with automatic notifications
- ✅ Processing fee deduction (configurable percentage)
- ✅ Integration with Square API for refund processing
- ✅ Complete audit trail for compliance
- ✅ REST API for all operations
- ✅ Pagination support for list endpoints
- ✅ Search functionality

---

## Architecture

### Entity Relationships

```
User (1:many)
  ├─ Subscription (created_by)
  ├─ SubscriptionRefund (requested_by, approved_by)
  └─ Permissions (many:many)

Subscription (1:many)
  └─ SubscriptionRefund (subscription_id)
```

### Service Layer

- **UserService**: Authentication, user management, permission checking
- **SubscriptionService**: CRUD operations, status management, payment tracking
- **RefundService**: Refund request/approval workflow, fee calculation, Square integration

### Controllers

- **AuthController**: `/api/auth/**` - User login, creation, management
- **SubscriptionController**: `/api/subscriptions/**` - Subscription CRUD and operations
- **RefundController**: `/api/refunds/**` - Refund request and approval

---

## User Authentication

### Permissions

The system supports the following permissions:

| Permission | Code | Description |
|-----------|------|-------------|
| VIEW_SUBSCRIPTIONS | `view_subscriptions` | Can view subscription details |
| CANCEL_SUBSCRIPTIONS | `cancel_subscriptions` | Can cancel subscriptions |
| REQUEST_REFUNDS | `request_refunds` | Can request refunds |
| APPROVE_REFUNDS | `approve_refunds` | Can approve refunds for processing |
| VIEW_REFUNDS | `view_refunds` | Can view refund requests and history |
| MANAGE_USERS | `manage_users` | Can create, update, and manage users |
| VIEW_USERS | `view_users` | Can view user list and details |
| SYSTEM_ADMIN | `system_admin` | Full system administrator access |

### Password Hashing

Passwords are hashed using **BCrypt** with strength 10. Raw passwords are never stored.

### User States

- **Active**: User can login
- **Inactive**: User cannot login (soft delete)

---

## Subscription Management

### Subscription Status

| Status | Description |
|--------|-------------|
| PENDING | Subscription created but not active in Square |
| ACTIVE | Active subscription in Square |
| PAUSED | Paused subscription |
| CANCELED | Canceled subscription |
| EXPIRED | Subscription expired |
| ERROR | Error during processing |

### Subscription Lifecycle

1. **Creation**: Subscription created from Square webhook or API
2. **Activation**: Subscription becomes ACTIVE in Square
3. **Payment**: Payments recorded, tracked against total amount
4. **Cancellation**: Can be canceled with reason
5. **Completion**: Auto-completes when fully paid

### Key Fields

- `squareSubscriptionId`: Unique ID from Square
- `squareCustomerId`: Square customer reference
- `customerEmail`: Customer email address
- `amount`: Total subscription amount
- `amountPaid`: Amount paid to date
- `status`: Current subscription status
- `billingCycle`: WEEKLY, BIWEEKLY, MONTHLY
- `cancellationReason`: Reason for cancellation (if canceled)

---

## Refund Management

### Refund Status Workflow

```
REQUESTED
    ↓
PENDING_APPROVAL
    ↓
    ├─→ APPROVED (with fee deduction)
    │      ↓
    │   PROCESSING (sending to Square)
    │      ↓
    │   COMPLETED (with Square refund ID)
    │
    └─→ REJECTED
```

### Fee Deduction Process

1. **Request Phase**: User requests refund with amount
2. **Approval Phase**: Administrator reviews and approves
   - Processing fee is deducted (default 2.5%)
   - Formula: `approvedAmount = requestedAmount - (requestedAmount * feePercent / 100)`
   - Fee is stored and tracked
3. **Processing Phase**: Refund marked ready for Square API
4. **Completion Phase**: Square processes refund, confirmaton recorded

### Example: Refund with Fee Deduction

```
Requested Amount: $100.00
Processing Fee: 2.5%
Processing Fee Deduction: $2.50
Approved/Net Refund Amount: $97.50
```

### Refund Statuses

| Status | Description |
|--------|-------------|
| REQUESTED | Refund request submitted |
| PENDING_APPROVAL | Waiting for administrator approval |
| APPROVED | Approved with fees deducted, ready for Square |
| PROCESSING | Being sent to Square API |
| COMPLETED | Successfully refunded in Square |
| REJECTED | Refund request was rejected |
| FAILED | Refund failed during processing |
| ERROR | Error occurred during processing |

### Audit Trail

Each refund tracks:
- Who requested the refund (`requestedByUser`)
- When it was requested (`createdAt`)
- Who approved it (`approvedByUser`)
- When it was approved (`approvedAt`)
- Processing fee deducted
- Square refund ID (once processed)
- Notes and approval/rejection reasons

---

## API Endpoints

### Authentication Endpoints

#### Login
```
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}

Response: 200 OK
{
  "id": 1,
  "username": "admin",
  "email": "admin@example.com",
  "fullName": "Administrator",
  "active": true,
  "permissions": ["SYSTEM_ADMIN"],
  "createdAt": "2025-01-01T10:00:00",
  "lastLogin": "2025-12-05T14:30:00"
}
```

#### Create User (Admin)
```
POST /api/auth/users
Content-Type: application/json

{
  "username": "john.smith",
  "password": "securePassword123",
  "email": "john@example.com",
  "fullName": "John Smith",
  "permissions": ["VIEW_SUBSCRIPTIONS", "REQUEST_REFUNDS"]
}

Response: 201 Created
```

#### Get All Users
```
GET /api/auth/users

Response: 200 OK
[
  {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "fullName": "Administrator",
    "active": true,
    "permissions": ["SYSTEM_ADMIN"],
    "createdAt": "2025-01-01T10:00:00",
    "lastLogin": "2025-12-05T14:30:00"
  },
  ...
]
```

#### Change User Password
```
POST /api/auth/users/{id}/change-password
Content-Type: application/json

{
  "oldPassword": "currentPassword",
  "newPassword": "newPassword123"
}

Response: 200 OK
{ "message": "Password changed successfully" }
```

#### Available Permissions
```
GET /api/auth/permissions

Response: 200 OK
[
  {
    "code": "view_subscriptions",
    "name": "VIEW_SUBSCRIPTIONS",
    "description": "Can view subscription details"
  },
  ...
]
```

### Subscription Endpoints

#### Create Subscription
```
POST /api/subscriptions
Content-Type: application/json

{
  "squareSubscriptionId": "sub_123abc",
  "squareCustomerId": "cust_456def",
  "squarePlanId": "plan_789ghi",
  "customerEmail": "customer@example.com",
  "amount": 99.99,
  "currency": "USD",
  "description": "Monthly Subscription",
  "billingCycle": "MONTHLY"
}

Response: 201 Created
```

#### Get Subscription
```
GET /api/subscriptions/{id}
Response: 200 OK
{
  "id": 1,
  "squareSubscriptionId": "sub_123abc",
  "squareCustomerId": "cust_456def",
  "customerEmail": "customer@example.com",
  "amount": 99.99,
  "amountPaid": 0.00,
  "status": "ACTIVE",
  "currency": "USD",
  "billingCycle": "MONTHLY",
  "createdAt": "2025-12-05T10:00:00",
  "updatedAt": "2025-12-05T10:00:00",
  "canceledAt": null,
  "cancellationReason": null
}
```

#### Get All Subscriptions (Paginated)
```
GET /api/subscriptions?page=0&size=20

Response: 200 OK
{
  "content": [...],
  "totalElements": 150,
  "totalPages": 8,
  "number": 0,
  "size": 20
}
```

#### Get Active Subscriptions
```
GET /api/subscriptions/active?page=0&size=20
Response: 200 OK
```

#### Get Subscriptions by Customer
```
GET /api/subscriptions/customer/customer@example.com?page=0&size=10
Response: 200 OK
```

#### Search Subscriptions
```
GET /api/subscriptions/search?query=customer@example.com&page=0&size=20
Response: 200 OK
```

#### Cancel Subscription
```
POST /api/subscriptions/{id}/cancel
Content-Type: application/json

{
  "reason": "Customer requested cancellation"
}

Response: 200 OK
```

#### Pause Subscription
```
POST /api/subscriptions/{id}/pause
Response: 200 OK
```

#### Resume Subscription
```
POST /api/subscriptions/{id}/resume
Response: 200 OK
```

#### Subscription Statistics
```
GET /api/subscriptions/stats/overview

Response: 200 OK
{
  "total": 150,
  "active": 120,
  "canceled": 30
}
```

### Refund Endpoints

#### Request Refund
```
POST /api/refunds/request
Content-Type: application/json
X-User-Id: 1

{
  "subscriptionId": 1,
  "requestedAmount": 50.00,
  "reason": "Customer not satisfied with service"
}

Response: 201 Created
{
  "id": 1,
  "subscriptionId": 1,
  "squareSubscriptionId": "sub_123abc",
  "requestedAmount": 50.00,
  "processingFee": 0.00,
  "approvedAmount": 0.00,
  "refundedAmount": 0.00,
  "status": "REQUESTED",
  "refundReason": "Customer not satisfied with service",
  "rejectionReason": null,
  "squareRefundId": null,
  "requestedByUser": "john.smith",
  "approvedByUser": null,
  "createdAt": "2025-12-05T15:30:00",
  "approvedAt": null,
  "completedAt": null,
  "notes": null
}
```

#### Get Refund by ID
```
GET /api/refunds/{id}
Response: 200 OK
```

#### Get All Refunds
```
GET /api/refunds?page=0&size=20
Response: 200 OK
```

#### Get Pending Approvals
```
GET /api/refunds/pending-approvals?page=0&size=20
Response: 200 OK
```

#### Count Pending Approvals
```
GET /api/refunds/pending-approvals/count

Response: 200 OK
{
  "pendingCount": 5
}
```

#### Get Refunds by Status
```
GET /api/refunds/status/APPROVED?page=0&size=20
Response: 200 OK
```

#### Get Refunds for Subscription
```
GET /api/refunds/subscription/{subscriptionId}?page=0&size=10
Response: 200 OK
```

#### Approve Refund (with fee deduction)
```
POST /api/refunds/{id}/approve
Content-Type: application/json
X-User-Id: 1

{
  "processingFeePercent": 2.5,
  "notes": "Approved - customer complaint justified"
}

Response: 200 OK
{
  "id": 1,
  "status": "APPROVED",
  "requestedAmount": 50.00,
  "processingFee": 1.25,
  "approvedAmount": 48.75,
  "approvedByUser": "admin",
  "approvedAt": "2025-12-05T16:00:00",
  "notes": "Approved - customer complaint justified"
}
```

#### Reject Refund
```
POST /api/refunds/{id}/reject
Content-Type: application/json
X-User-Id: 1

{
  "rejectionReason": "Refund policy does not apply - within first 30 days"
}

Response: 200 OK
```

#### Mark Refund as Processing
```
POST /api/refunds/{id}/mark-processing
Response: 200 OK
```

#### Complete Refund (Square Processed)
```
POST /api/refunds/{id}/complete
Content-Type: application/json

{
  "squareRefundId": "refund_abc123",
  "refundedAmount": 48.75
}

Response: 200 OK
```

#### Get Refunds Pending Square Processing
```
GET /api/refunds/pending-square-processing?page=0&size=20
Response: 200 OK
```

#### Refund Statistics
```
GET /api/refunds/stats/overview

Response: 200 OK
{
  "pendingApproval": 3,
  "completed": 12
}
```

---

## Database Schema

### Users Table
```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(100) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  email VARCHAR(255),
  full_name VARCHAR(255),
  active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  last_login TIMESTAMP
);
```

### User Permissions Table
```sql
CREATE TABLE user_permissions (
  user_id BIGINT NOT NULL,
  permission VARCHAR(50) NOT NULL,
  PRIMARY KEY (user_id, permission),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### Subscriptions Table
```sql
CREATE TABLE subscriptions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  square_subscription_id VARCHAR(100) NOT NULL UNIQUE,
  square_customer_id VARCHAR(100) NOT NULL,
  square_plan_id VARCHAR(100) NOT NULL,
  customer_email VARCHAR(255) NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  amount_paid DECIMAL(10,2) DEFAULT 0,
  status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
  currency VARCHAR(50) DEFAULT 'USD',
  description VARCHAR(500),
  cancellation_reason VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  canceled_at TIMESTAMP,
  billing_cycle VARCHAR(100) DEFAULT 'MONTHLY'
);
```

### Subscription Refunds Table
```sql
CREATE TABLE subscription_refunds (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  subscription_id BIGINT NOT NULL,
  requested_amount DECIMAL(10,2) NOT NULL,
  processing_fee DECIMAL(10,2) DEFAULT 0,
  approved_amount DECIMAL(10,2) DEFAULT 0,
  refunded_amount DECIMAL(10,2) DEFAULT 0,
  status VARCHAR(50) NOT NULL DEFAULT 'REQUESTED',
  refund_reason VARCHAR(500),
  rejection_reason VARCHAR(500),
  square_refund_id VARCHAR(100),
  requested_by_user_id BIGINT,
  approved_by_user_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  approved_at TIMESTAMP,
  completed_at TIMESTAMP,
  notes VARCHAR(500),
  FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE,
  FOREIGN KEY (requested_by_user_id) REFERENCES users(id),
  FOREIGN KEY (approved_by_user_id) REFERENCES users(id)
);
```

---

## Configuration

### Environment Variables

```bash
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/subscriptions
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# Admin Credentials (initial setup)
APP_ADMIN_USER=admin
APP_ADMIN_PASS=changeme123

# Square Configuration
SQUARE_API_TOKEN=your_square_token
SQUARE_LOCATION_ID=your_location_id
SQUARE_APPLICATION_ID=your_app_id
SQUARE_WEBHOOK_SIGNATURE_KEY=your_webhook_key

# Refund Configuration
REFUND_NOTIFICATION_EMAIL=admin@example.com
SUBSCRIPTION_ENABLE_NOTIFICATIONS=true
```

### Application Properties

```properties
# In application.properties
subscription.refund.processing-fee-percent=2.5
subscription.refund.notification-email=admin@example.com
subscription.enable-email-notifications=false
```

---

## Integration with Square

### Square Webhook Events

The system can handle the following Square events:

1. **subscription.created**: New subscription created
2. **subscription.updated**: Subscription details updated
3. **subscription.cancelled**: Subscription cancelled in Square
4. **payment.created**: Payment received on subscription
5. **refund.created**: Refund processed in Square

### Webhook Handler

Update your `SquareWebhookController` to sync subscriptions and process refunds:

```java
@PostMapping("/square/subscriptions")
public ResponseEntity<?> handleSubscriptionWebhook(@RequestBody SquareWebhookEvent event) {
    // Parse event
    String eventType = event.getType();
    
    if ("subscription.created".equals(eventType)) {
        // Create/sync subscription
    } else if ("payment.created".equals(eventType)) {
        // Record payment, check if fully paid
    } else if ("refund.created".equals(eventType)) {
        // Mark refund as completed in our system
    }
    
    return ResponseEntity.ok(Map.of("success", true));
}
```

### Refund Processing Flow

1. Administrator approves refund in the system
2. System deducts fees and stores approval
3. Integration code calls Square Refund API with approvedAmount
4. Square processes refund and sends webhook event
5. Webhook handler marks refund as COMPLETED with Square refund ID

---

## Error Handling

### Common Error Responses

#### 400 Bad Request
```json
{
  "error": "Refund amount cannot exceed subscription amount"
}
```

#### 401 Unauthorized
```json
{
  "error": "Invalid username or password"
}
```

#### 403 Forbidden
```json
{
  "error": "User does not have permission to approve refunds"
}
```

#### 404 Not Found
```json
{
  "error": "Subscription not found"
}
```

#### 409 Conflict
```json
{
  "error": "Subscription is already canceled"
}
```

#### 500 Internal Server Error
```json
{
  "error": "Error processing refund: database connection failed"
}
```

### Validation Rules

- **Refund Amount**: Must be > 0 and ≤ subscription amount
- **Processing Fee**: Must be 0-100%
- **Username**: Required, must be unique
- **Password**: Minimum 6 characters
- **Email**: Must be valid format (if provided)

---

## Usage Examples

### Complete Refund Workflow

```bash
# 1. Customer requests refund of $50 from $100 subscription
curl -X POST http://localhost:8080/api/refunds/request \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 2" \
  -d '{
    "subscriptionId": 1,
    "requestedAmount": 50.00,
    "reason": "Not satisfied with service quality"
  }'

# Response: Refund created with status REQUESTED

# 2. Admin checks pending refunds
curl http://localhost:8080/api/refunds/pending-approvals?page=0&size=10

# 3. Admin approves refund with 2.5% fee deduction
# Approved Amount = $50 - ($50 * 2.5%) = $48.75
curl -X POST http://localhost:8080/api/refunds/1/approve \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "processingFeePercent": 2.5,
    "notes": "Approved due to service quality issues"
  }'

# Response: Refund status changed to APPROVED, fee deducted

# 4. Integration code marks as processing
curl -X POST http://localhost:8080/api/refunds/1/mark-processing

# 5. Call Square Refund API with approved amount ($48.75)
# (Implementation in your Square integration)

# 6. After Square processes, mark as completed
curl -X POST http://localhost:8080/api/refunds/1/complete \
  -H "Content-Type: application/json" \
  -d '{
    "squareRefundId": "refund_abc123xyz",
    "refundedAmount": 48.75
  }'

# Response: Refund status changed to COMPLETED, Square refund ID stored
```

### User Management Workflow

```bash
# 1. Create new user
curl -X POST http://localhost:8080/api/auth/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jane.doe",
    "password": "securePassword123",
    "email": "jane@example.com",
    "fullName": "Jane Doe",
    "permissions": ["VIEW_SUBSCRIPTIONS", "REQUEST_REFUNDS", "APPROVE_REFUNDS"]
  }'

# 2. User logs in
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jane.doe",
    "password": "securePassword123"
  }'

# 3. Change password
curl -X POST http://localhost:8080/api/auth/users/3/change-password \
  -H "Content-Type: application/json" \
  -d '{
    "oldPassword": "securePassword123",
    "newPassword": "newSecurePassword456"
  }'
```

---

## Security Considerations

1. **Passwords**: Always use HTTPS in production. Passwords are hashed with BCrypt.
2. **Permissions**: Implement role-based access control. Check permissions before allowing operations.
3. **Audit Trail**: All refund approvals/rejections are logged with user information.
4. **Token Management**: Implement JWT tokens for stateless authentication (future enhancement).
5. **CORS**: Configure CORS appropriately for your frontend.
6. **Input Validation**: All inputs are validated before processing.

---

## Future Enhancements

- [ ] JWT Token-based authentication
- [ ] Email notifications for refund requests/approvals
- [ ] Two-factor authentication
- [ ] Advanced analytics and reporting
- [ ] Bulk refund processing
- [ ] Customizable refund fees per customer
- [ ] Refund scheduling (process on specific dates)
- [ ] API key authentication for third-party integrations
- [ ] WebSocket notifications for real-time updates

---

## Support & Troubleshooting

For issues or questions about the Subscription Management System, please refer to the main README.md or contact your system administrator.
