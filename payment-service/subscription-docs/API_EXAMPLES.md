# Subscription Management API - Usage Examples

This document provides practical curl examples for common operations.

## Table of Contents
1. [Authentication](#authentication)
2. [User Management](#user-management)
3. [Subscription Management](#subscription-management)
4. [Refund Management](#refund-management)
5. [Batch Operations](#batch-operations)

---

## Authentication

### Login as Administrator

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "changeit"
  }'
```

**Response:**
```json
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

### Get Available Permissions

```bash
curl http://localhost:8080/api/auth/permissions
```

**Response:**
```json
[
  {
    "code": "view_subscriptions",
    "name": "VIEW_SUBSCRIPTIONS",
    "description": "Can view subscription details"
  },
  {
    "code": "cancel_subscriptions",
    "name": "CANCEL_SUBSCRIPTIONS",
    "description": "Can cancel subscriptions"
  },
  ...
]
```

---

## User Management

### Create New User with Specific Permissions

```bash
curl -X POST http://localhost:8080/api/auth/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jane.smith",
    "password": "SecurePassword123",
    "email": "jane.smith@company.com",
    "fullName": "Jane Smith",
    "permissions": [
      "VIEW_SUBSCRIPTIONS",
      "REQUEST_REFUNDS",
      "APPROVE_REFUNDS",
      "VIEW_REFUNDS"
    ]
  }'
```

### Create Refund-Only User

```bash
curl -X POST http://localhost:8080/api/auth/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "refund.processor",
    "password": "Password123",
    "email": "refund@company.com",
    "fullName": "Refund Processor",
    "permissions": [
      "VIEW_SUBSCRIPTIONS",
      "REQUEST_REFUNDS",
      "VIEW_REFUNDS"
    ]
  }'
```

### Create Approval-Only User

```bash
curl -X POST http://localhost:8080/api/auth/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "refund.approver",
    "password": "Password456",
    "email": "approver@company.com",
    "fullName": "Refund Approver",
    "permissions": [
      "APPROVE_REFUNDS",
      "VIEW_REFUNDS",
      "VIEW_SUBSCRIPTIONS"
    ]
  }'
```

### List All Users

```bash
curl http://localhost:8080/api/auth/users
```

### Get Specific User

```bash
curl http://localhost:8080/api/auth/users/2
```

### Update User Profile

```bash
curl -X PUT http://localhost:8080/api/auth/users/2 \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newemail@company.com",
    "fullName": "Jane Marie Smith",
    "permissions": [
      "VIEW_SUBSCRIPTIONS",
      "REQUEST_REFUNDS"
    ]
  }'
```

### Change Own Password

```bash
curl -X POST http://localhost:8080/api/auth/users/2/change-password \
  -H "Content-Type: application/json" \
  -d '{
    "oldPassword": "SecurePassword123",
    "newPassword": "NewSecurePassword456"
  }'
```

### Reset User Password (Admin)

```bash
curl -X POST http://localhost:8080/api/auth/users/2/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "newPassword": "TempPassword123"
  }'
```

### Disable User

```bash
curl -X POST http://localhost:8080/api/auth/users/2/disable
```

### Enable User

```bash
curl -X POST http://localhost:8080/api/auth/users/2/enable
```

### Delete User

```bash
curl -X DELETE http://localhost:8080/api/auth/users/2
```

---

## Subscription Management

### Create Subscription

```bash
curl -X POST http://localhost:8080/api/subscriptions \
  -H "Content-Type: application/json" \
  -d '{
    "squareSubscriptionId": "sub_monthly_001",
    "squareCustomerId": "cust_12345abc",
    "squarePlanId": "plan_monthly_100",
    "customerEmail": "customer@example.com",
    "amount": 99.99,
    "currency": "USD",
    "description": "Monthly Premium Subscription",
    "billingCycle": "MONTHLY"
  }'
```

### Get Subscription by ID

```bash
curl http://localhost:8080/api/subscriptions/1
```

### Get Subscription by Square ID

```bash
curl http://localhost:8080/api/subscriptions/square/sub_monthly_001
```

### List All Subscriptions (Paginated)

```bash
# Get first page (20 items per page, default)
curl http://localhost:8080/api/subscriptions?page=0&size=20

# Get second page with custom size
curl http://localhost:8080/api/subscriptions?page=1&size=50
```

### Get Active Subscriptions

```bash
curl http://localhost:8080/api/subscriptions/active?page=0&size=20
```

### Get Customer's Subscriptions

```bash
curl http://localhost:8080/api/subscriptions/customer/customer@example.com?page=0&size=10
```

### Search Subscriptions

```bash
# Search by email or ID
curl "http://localhost:8080/api/subscriptions/search?query=customer&page=0&size=20"

# Search by Square subscription ID
curl "http://localhost:8080/api/subscriptions/search?query=sub_monthly_001"
```

### Cancel Subscription

```bash
curl -X POST http://localhost:8080/api/subscriptions/1/cancel \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Customer requested cancellation due to budget constraints"
  }'
```

### Pause Subscription

```bash
curl -X POST http://localhost:8080/api/subscriptions/1/pause
```

### Resume Subscription

```bash
curl -X POST http://localhost:8080/api/subscriptions/1/resume
```

### Update Subscription Amount

```bash
curl -X PUT http://localhost:8080/api/subscriptions/1/amount \
  -H "Content-Type: application/json" \
  -d '{
    "newAmount": 149.99
  }'
```

### Get Subscription Statistics

```bash
curl http://localhost:8080/api/subscriptions/stats/overview
```

**Response:**
```json
{
  "total": 150,
  "active": 120,
  "canceled": 30
}
```

---

## Refund Management

### Request Refund

```bash
curl -X POST http://localhost:8080/api/refunds/request \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 2" \
  -d '{
    "subscriptionId": 1,
    "requestedAmount": 50.00,
    "reason": "Customer not satisfied with service quality"
  }'
```

**Note:** `X-User-Id` header identifies who is requesting the refund (optional, tracked in audit trail)

### Request Full Refund

```bash
curl -X POST http://localhost:8080/api/refunds/request \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 2" \
  -d '{
    "subscriptionId": 1,
    "requestedAmount": 99.99,
    "reason": "Full refund - customer cancellation"
  }'
```

### Get Refund by ID

```bash
curl http://localhost:8080/api/refunds/1
```

### List All Refunds

```bash
curl http://localhost:8080/api/refunds?page=0&size=20
```

### Get Pending Approvals

```bash
curl http://localhost:8080/api/refunds/pending-approvals?page=0&size=10
```

### Count Pending Approvals

```bash
curl http://localhost:8080/api/refunds/pending-approvals/count
```

**Response:**
```json
{
  "pendingCount": 5
}
```

### Get Refunds by Status

```bash
# Get all requested refunds
curl http://localhost:8080/api/refunds/status/REQUESTED?page=0&size=20

# Get all approved refunds
curl http://localhost:8080/api/refunds/status/APPROVED?page=0&size=20

# Get completed refunds
curl http://localhost:8080/api/refunds/status/COMPLETED?page=0&size=20

# Get rejected refunds
curl http://localhost:8080/api/refunds/status/REJECTED?page=0&size=20
```

### Get Refunds for Specific Subscription

```bash
curl http://localhost:8080/api/refunds/subscription/1?page=0&size=10
```

### Approve Refund (with Default Fee 2.5%)

```bash
curl -X POST http://localhost:8080/api/refunds/1/approve \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "processingFeePercent": 2.5,
    "notes": "Approved - valid service complaint. Customer will be contacted within 24 hours."
  }'
```

### Approve Refund with Custom Fee

```bash
curl -X POST http://localhost:8080/api/refunds/2/approve \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "processingFeePercent": 3.5,
    "notes": "Approved with 3.5% fee due to international processing"
  }'
```

### Approve Refund with No Fee

```bash
curl -X POST http://localhost:8080/api/refunds/3/approve \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "processingFeePercent": 0,
    "notes": "Full refund approved - company error"
  }'
```

### Reject Refund

```bash
curl -X POST http://localhost:8080/api/refunds/4/reject \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "rejectionReason": "Refund window expired - subscription active for 120 days"
  }'
```

### Mark Refund as Processing (Ready for Square)

```bash
curl -X POST http://localhost:8080/api/refunds/1/mark-processing
```

### Complete Refund (After Square Processes)

```bash
curl -X POST http://localhost:8080/api/refunds/1/complete \
  -H "Content-Type: application/json" \
  -d '{
    "squareRefundId": "refund_wpsAjnqzxW4qgZz",
    "refundedAmount": 48.75
  }'
```

### Get Refunds Awaiting Square Processing

```bash
curl http://localhost:8080/api/refunds/pending-square-processing?page=0&size=10
```

### Get Refund Statistics

```bash
curl http://localhost:8080/api/refunds/stats/overview
```

**Response:**
```json
{
  "pendingApproval": 3,
  "completed": 12
}
```

---

## Batch Operations

### Complete Refund Workflow (Scenario 1: Approve and Process)

```bash
#!/bin/bash

# Step 1: Create subscription
SUBSCRIPTION=$(curl -s -X POST http://localhost:8080/api/subscriptions \
  -H "Content-Type: application/json" \
  -d '{
    "squareSubscriptionId": "sub_batch_001",
    "squareCustomerId": "cust_batch_001",
    "squarePlanId": "plan_test",
    "customerEmail": "batch@example.com",
    "amount": 100.00,
    "currency": "USD",
    "billingCycle": "MONTHLY"
  }')

SUB_ID=$(echo $SUBSCRIPTION | jq -r '.id')
echo "Created subscription: $SUB_ID"

# Step 2: Request refund
REFUND=$(curl -s -X POST http://localhost:8080/api/refunds/request \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 2" \
  -d "{
    \"subscriptionId\": $SUB_ID,
    \"requestedAmount\": 50.00,
    \"reason\": \"Test refund\"
  }")

REFUND_ID=$(echo $REFUND | jq -r '.id')
echo "Created refund request: $REFUND_ID"

# Step 3: Approve refund
curl -s -X POST http://localhost:8080/api/refunds/$REFUND_ID/approve \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "processingFeePercent": 2.5,
    "notes": "Approved in batch"
  }' | jq '.'

echo "Refund approved. Net amount: 48.75"

# Step 4: Mark as processing
curl -s -X POST http://localhost:8080/api/refunds/$REFUND_ID/mark-processing | jq '.'

echo "Marked as processing"

# Step 5: Complete after Square processes
curl -s -X POST http://localhost:8080/api/refunds/$REFUND_ID/complete \
  -H "Content-Type: application/json" \
  -d '{
    "squareRefundId": "refund_test_123",
    "refundedAmount": 48.75
  }' | jq '.'

echo "Refund workflow complete"
```

### Bulk User Creation

```bash
#!/bin/bash

USERS=("john.doe" "jane.smith" "bob.johnson" "alice.williams")
PERMISSIONS="[\"VIEW_SUBSCRIPTIONS\",\"REQUEST_REFUNDS\"]"

for user in "${USERS[@]}"
do
  curl -s -X POST http://localhost:8080/api/auth/users \
    -H "Content-Type: application/json" \
    -d "{
      \"username\": \"$user\",
      \"password\": \"Password123\",
      \"email\": \"$user@company.com\",
      \"fullName\": \"$user\",
      \"permissions\": $PERMISSIONS
    }" | jq '.username'
  echo "Created user: $user"
done
```

### Generate Refund Report

```bash
#!/bin/bash

echo "Pending Refunds:"
curl -s http://localhost:8080/api/refunds/status/REQUESTED?page=0&size=100 \
  | jq '.content[] | {id, requestedAmount, status, createdAt}'

echo -e "\nApproved Refunds:"
curl -s http://localhost:8080/api/refunds/status/APPROVED?page=0&size=100 \
  | jq '.content[] | {id, approvedAmount, processingFee, approvedAt}'

echo -e "\nCompleted Refunds:"
curl -s http://localhost:8080/api/refunds/status/COMPLETED?page=0&size=100 \
  | jq '.content[] | {id, refundedAmount, squareRefundId, completedAt}'
```

### Monitor Dashboard

```bash
#!/bin/bash

while true
do
  clear
  echo "=== Subscription & Refund Dashboard ==="
  echo ""
  
  echo "Subscription Stats:"
  curl -s http://localhost:8080/api/subscriptions/stats/overview | jq '.'
  
  echo ""
  echo "Refund Stats:"
  curl -s http://localhost:8080/api/refunds/stats/overview | jq '.'
  
  echo ""
  echo "Pending Approvals:"
  curl -s http://localhost:8080/api/refunds/pending-approvals/count | jq '.'
  
  echo ""
  echo "--- Refreshing in 5 seconds ---"
  sleep 5
done
```

---

## Common Patterns

### Complete Refund Workflow Summary

```
1. Request Refund
   POST /api/refunds/request
   Status: REQUESTED

2. Approve with Fee Deduction
   POST /api/refunds/{id}/approve
   Formula: approvedAmount = requested - (requested × fee%)
   Status: APPROVED

3. Mark for Square Processing
   POST /api/refunds/{id}/mark-processing
   Status: PROCESSING

4. Call Square Refund API
   Use: approvedAmount (not requested)

5. Mark Complete
   POST /api/refunds/{id}/complete
   Store: squareRefundId
   Status: COMPLETED
```

### Fee Examples

**Example 1: 2.5% Fee (Default)**
```
Requested: $100.00
Fee: $100 × 2.5% = $2.50
Approved: $100 - $2.50 = $97.50
```

**Example 2: 3% Fee**
```
Requested: $50.00
Fee: $50 × 3% = $1.50
Approved: $50 - $1.50 = $48.50
```

**Example 3: No Fee**
```
Requested: $75.00
Fee: $75 × 0% = $0.00
Approved: $75 - $0 = $75.00
```

---

## Error Handling

### Invalid Amount

```bash
curl -X POST http://localhost:8080/api/refunds/request \
  -H "Content-Type: application/json" \
  -d '{
    "subscriptionId": 1,
    "requestedAmount": 1000.00,
    "reason": "Too much"
  }'
```

**Response (400):**
```json
{
  "error": "Refund amount cannot exceed subscription amount"
}
```

### Invalid Status Transition

```bash
curl -X POST http://localhost:8080/api/refunds/5/approve \
  -H "Content-Type: application/json" \
  -d '{
    "processingFeePercent": 2.5,
    "notes": "Already approved"
  }'
```

**Response (400):**
```json
{
  "error": "Refund cannot be approved in current status: COMPLETED"
}
```

### Resource Not Found

```bash
curl http://localhost:8080/api/refunds/99999
```

**Response (404):**
```json
{
  "error": "Not Found"
}
```

---

## Tips & Best Practices

1. **Always include `X-User-Id` header** when requesting/approving refunds for proper audit trails
2. **Store refund IDs** after creation for tracking
3. **Use pagination** for large result sets (page=0&size=20)
4. **Save Square refund IDs** after completion for reconciliation
5. **Monitor pending-approvals count** regularly for timely processing
6. **Test fee percentages** before deploying to production
7. **Keep detailed notes** in approval for audit trail

---

## Support

For more details on specific endpoints, see [SUBSCRIPTION_MANAGEMENT.md](./SUBSCRIPTION_MANAGEMENT.md)
