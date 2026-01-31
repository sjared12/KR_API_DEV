# Partial Refund Feature Documentation

## Overview
The payment portal now supports **partial refunds** with a complete audit trail. Admins can request, approve, and process refunds while maintaining a detailed history of all refund transactions.

## Key Components

### 1. RefundRecord Entity
Stores all refund request information with complete audit trail.

**Fields:**
- `id`: UUID - Unique identifier
- `plan`: ManyToOne - Associated payment plan
- `refundAmount`: BigDecimal - Amount being refunded
- `refundReason`: String - Reason for refund
- `status`: String - REQUESTED, APPROVED, PROCESSED, FAILED
- `squareRefundId`: String - Square transaction ID (if applicable)
- `refundMethod`: String - CARD, MANUAL, OTHER
- `initiatedByUser`: User - Admin who requested the refund
- `approvedByUser`: User - Admin who approved the refund
- `requestedAt`: LocalDateTime - When refund was requested
- `approvedAt`: LocalDateTime - When refund was approved
- `processedAt`: LocalDateTime - When refund was processed
- `notes`: String - Additional information

### 2. Refund Workflow

#### Step 1: Request a Refund
**Endpoint:** `POST /api/admin/plans/{planId}/refunds`

**Request Body:**
```json
{
  "refundAmount": 50.00,
  "refundReason": "Duplicate charge",
  "refundMethod": "CARD",
  "notes": "Customer called to report double billing"
}
```

**Validation:**
- Refund amount must be > $0
- Refund amount cannot exceed amount paid on the plan
- Reason is required

**Response:**
```json
{
  "id": "uuid-string",
  "planId": "uuid-string",
  "refundAmount": 50.00,
  "refundReason": "Duplicate charge",
  "status": "REQUESTED",
  "refundMethod": "CARD",
  "initiatedByUserEmail": "admin@example.com",
  "requestedAt": "2025-01-27T14:30:00",
  "notes": "Customer called to report double billing"
}
```

#### Step 2: Approve Refund (Optional)
**Endpoint:** `POST /api/admin/refunds/{refundId}/approve`

**Request Body (optional):**
```json
{
  "notes": "Approved - verified duplicate transaction in Square"
}
```

**Response:**
```json
{
  "id": "uuid-string",
  "planId": "uuid-string",
  "refundAmount": 50.00,
  "refundReason": "Duplicate charge",
  "status": "APPROVED",
  "approvedByUserEmail": "manager@example.com",
  "approvedAt": "2025-01-27T15:00:00",
  "notes": "Approved - verified duplicate transaction in Square"
}
```

#### Step 3: Process Refund
**Endpoint:** `POST /api/admin/refunds/{refundId}/process`

**Request Body (optional):**
```json
{
  "squareRefundId": "refund_123456789"
}
```

If `squareRefundId` is not provided, a unique identifier is auto-generated.

**Response:**
```json
{
  "id": "uuid-string",
  "planId": "uuid-string",
  "refundAmount": 50.00,
  "status": "PROCESSED",
  "squareRefundId": "refund_123456789",
  "processedAt": "2025-01-27T15:05:00"
}
```

**What Happens:**
- Plan's `amountPaid` is reduced by the refund amount
- Refund record status changes to PROCESSED
- Audit log entry is created
- Square transaction ID is recorded

### 3. Get Refunds

#### Get All Refunds for a Plan
**Endpoint:** `GET /api/admin/plans/{planId}/refunds`

**Response:**
```json
[
  {
    "id": "uuid-1",
    "planId": "uuid-string",
    "refundAmount": 50.00,
    "refundReason": "Duplicate charge",
    "status": "PROCESSED",
    "squareRefundId": "refund_123456789",
    "initiatedByUserEmail": "admin@example.com",
    "approvedByUserEmail": "manager@example.com",
    "requestedAt": "2025-01-27T14:30:00",
    "approvedAt": "2025-01-27T15:00:00",
    "processedAt": "2025-01-27T15:05:00"
  }
]
```

#### Get Specific Refund
**Endpoint:** `GET /api/admin/refunds/{refundId}`

**Response:** Single refund record (same structure as above)

### 4. Reject Refund
**Endpoint:** `POST /api/admin/refunds/{refundId}/reject`

**Request Body (optional):**
```json
{
  "reason": "Transaction verified as correct - no duplicate"
}
```

**Response:**
```json
{
  "id": "uuid-string",
  "status": "FAILED",
  "notes": "Rejected: Transaction verified as correct - no duplicate",
  "approvedByUserEmail": "manager@example.com"
}
```

## Refund Statuses

| Status | Meaning | Can Change To |
|--------|---------|---------------|
| REQUESTED | Initial refund request submitted | APPROVED, FAILED |
| APPROVED | Admin approved the refund | PROCESSED, FAILED |
| PROCESSED | Refund sent to payment processor | (Final) |
| FAILED | Refund was rejected | (Final) |

## Database Schema

### refund_records Table
```sql
CREATE TABLE refund_records (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  plan_id UUID NOT NULL REFERENCES plans(id),
  refund_amount DECIMAL(12, 2) NOT NULL,
  refund_reason VARCHAR(255) NOT NULL,
  status VARCHAR(50) NOT NULL DEFAULT 'REQUESTED',
  square_refund_id VARCHAR(255),
  refund_method VARCHAR(50) DEFAULT 'CARD',
  initiated_by_user_id UUID REFERENCES users(id),
  approved_by_user_id UUID REFERENCES users(id),
  requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  approved_at TIMESTAMP,
  processed_at TIMESTAMP,
  notes TEXT
);

CREATE INDEX idx_refund_records_plan_id ON refund_records(plan_id);
CREATE INDEX idx_refund_records_status ON refund_records(status);
```

## Audit Trail

Every refund action is tracked with:
- User email who initiated/approved action
- Timestamps for each state change
- Notes/comments from admins
- Square transaction ID (if applicable)

This enables complete accountability and tracking of all refunds.

## Integration Points

### PlanService Methods
- `requestPartialRefund()` - Creates refund request with validation
- `approveRefund()` - Marks refund as approved
- `rejectRefund()` - Rejects a refund request
- `processRefund()` - Processes approved refund and updates plan
- `getRefundsForPlan()` - Retrieves refund history
- `getRefund()` - Gets specific refund record

### Relationship
- Plan has @OneToMany relationship with RefundRecord
- Cascade type: ALL (deleting plan cascades to refunds)
- Fetch type: LAZY (refunds loaded on demand)

## Example Flow

1. **Request:** Admin sees customer was overcharged
   ```
   POST /api/admin/plans/uuid-123/refunds
   { "refundAmount": 25.00, "refundReason": "Overcharge" }
   → Status: REQUESTED
   ```

2. **Approve:** Manager reviews and approves
   ```
   POST /api/admin/refunds/uuid-456/approve
   { "notes": "Verified in Square - duplicate charge" }
   → Status: APPROVED
   ```

3. **Process:** Send refund to Square
   ```
   POST /api/admin/refunds/uuid-456/process
   { "squareRefundId": "refund_abc123" }
   → Status: PROCESSED
   → Plan.amountPaid reduced by $25
   ```

4. **Verify:** Check refund history
   ```
   GET /api/admin/plans/uuid-123/refunds
   → Returns all refunds for that plan with full audit trail
   ```

## Error Handling

| Error | Cause | Response |
|-------|-------|----------|
| "Plan not found" | Invalid planId | 400 Bad Request |
| "Refund amount must be > 0" | Invalid amount | 400 Bad Request |
| "Cannot exceed amount paid" | Refund too large | 400 Bad Request |
| "Only REQUESTED refunds can be approved" | Wrong status | 400 Bad Request |
| "Refund not found" | Invalid refundId | 400 Bad Request |
