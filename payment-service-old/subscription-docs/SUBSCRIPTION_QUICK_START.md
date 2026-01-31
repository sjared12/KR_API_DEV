# Subscription Management System - Quick Start Guide

## 5-Minute Setup

### 1. Database Setup

The system automatically creates tables on first run. Ensure your PostgreSQL is running:

```bash
# Create database (if not exists)
createdb simpletixdb
```

Tables will be created automatically by Hibernate when the application starts.

### 2. Environment Variables

Add these to your `.env` file or Docker environment:

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/simpletixdb
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=password

# Admin (creates root user)
APP_ADMIN_USER=admin
APP_ADMIN_PASS=changeit

# Optional: Refund notifications
SUBSCRIPTION_ENABLE_NOTIFICATIONS=false
REFUND_NOTIFICATION_EMAIL=admin@example.com
```

### 3. Start Application

```bash
mvn spring-boot:run
```

Or run the built jar:

```bash
java -jar target/simpletix-webhook-0.0.1-SNAPSHOT.jar
```

### 4. Test the System

#### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"changeme123"}'
```

#### Create a Subscription
```bash
curl -X POST http://localhost:8080/api/subscriptions \
  -H "Content-Type: application/json" \
  -d '{
    "squareSubscriptionId":"sub_test123",
    "squareCustomerId":"cust_test456",
    "squarePlanId":"plan_test789",
    "customerEmail":"customer@example.com",
    "amount":99.99,
    "currency":"USD",
    "billingCycle":"MONTHLY"
  }'
```

#### Request a Refund
```bash
curl -X POST http://localhost:8080/api/refunds/request \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "subscriptionId":1,
    "requestedAmount":50.00,
    "reason":"Customer requested"
  }'
```

#### Approve Refund (with 2.5% fee)
```bash
curl -X POST http://localhost:8080/api/refunds/1/approve \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "processingFeePercent":2.5,
    "notes":"Approved"
  }'
```

## Key Concepts

### Permissions

Users need specific permissions to perform actions:

| Permission | Allows |
|-----------|--------|
| `VIEW_SUBSCRIPTIONS` | View any subscription |
| `CANCEL_SUBSCRIPTIONS` | Cancel subscriptions |
| `REQUEST_REFUNDS` | Request refunds |
| `APPROVE_REFUNDS` | Approve/reject refunds |
| `VIEW_REFUNDS` | View all refunds |

### Refund Fee Calculation

```
Requested Amount: $100
Fee Percentage: 2.5%
Fee Deducted: $100 × 0.025 = $2.50
Net Refund Amount: $100 - $2.50 = $97.50
```

The system automatically:
1. Calculates the fee
2. Deducts from the approved amount
3. Stores audit trail (who approved, when, fee amount)
4. Marks ready for Square processing

### Refund Workflow

```
User requests refund
    ↓
Admin reviews request
    ↓
Admin approves (fee is deducted)
    ↓
System marks for Square processing
    ↓
Integration code calls Square API with net amount
    ↓
Square processes refund
    ↓
System marks complete with Square refund ID
```

## Common Tasks

### Create a New User

```bash
curl -X POST http://localhost:8080/api/auth/users \
  -H "Content-Type: application/json" \
  -d '{
    "username":"john.doe",
    "password":"SecurePass123",
    "email":"john@example.com",
    "fullName":"John Doe",
    "permissions":["VIEW_SUBSCRIPTIONS","REQUEST_REFUNDS"]
  }'
```

### View Pending Refunds

```bash
curl http://localhost:8080/api/refunds/pending-approvals?page=0&size=10
```

### Get Refund Stats

```bash
curl http://localhost:8080/api/refunds/stats/overview
```

### Disable User

```bash
curl -X POST http://localhost:8080/api/auth/users/2/disable
```

## Troubleshooting

### Users Table Not Created

**Issue**: "Table users doesn't exist" error

**Solution**: Ensure `spring.jpa.hibernate.ddl-auto=update` is set in `application.properties`. The table will be created on first startup.

### Authentication Failing

**Issue**: Login always returns 401 Unauthorized

**Solution**: 
1. Check username/password match what's in the database
2. Ensure user `active` flag is set to `true`
3. Verify bcrypt password hashing is working: `new BCryptPasswordEncoder().matches("password", hash)`

### Refund Fee Calculation Wrong

**Issue**: Approved amount doesn't match expected

**Solution**: Verify `processingFeePercent` in the approval request. Default is 2.5%.

## Integration with Square

### Processing Approved Refunds

After a refund is approved, your integration code should:

1. Get approved refunds pending Square processing:
   ```
   GET /api/refunds/pending-square-processing
   ```

2. Call Square Refund API with `approvedAmount` (not requested amount):
   ```java
   // approvedAmount already has fee deducted
   squareClient.refundsApi.createRefund(
       transactionId,
       new CreateRefundRequest.Builder(approvedAmount).build()
   );
   ```

3. Mark refund complete in system:
   ```
   POST /api/refunds/{id}/complete
   {
     "squareRefundId": "refund_abc123",
     "refundedAmount": 97.50
   }
   ```

## Next Steps

- Read [SUBSCRIPTION_MANAGEMENT.md](./SUBSCRIPTION_MANAGEMENT.md) for complete API documentation
- Set up email notifications (see configuration section)
- Implement Square webhook integration (see Square integration section)
- Deploy to production with proper SSL certificates

## Support

For detailed information, see:
- [SUBSCRIPTION_MANAGEMENT.md](./SUBSCRIPTION_MANAGEMENT.md) - Complete API docs
- [README.md](./README.md) - Full project documentation
- Swagger UI: http://localhost:8080/swagger-ui.html (if enabled)
