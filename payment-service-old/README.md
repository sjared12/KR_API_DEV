# KR Payment Service

A dedicated Spring Boot microservice that manages student payments, Square payment links, and installment plans for the Kenton Ridge band program. This code was extracted from the main `KR_API` project so the payment domain can evolve independently while the rest of the application continues to focus on analytics and webhooks.

## Features
- Square Online Checkout payment link generation with fee estimates.
- Installment plan creation that issues Square invoices (installments or fallback balance invoices).
- **Automatic payment plan completion tracking** — when students make payments (via `/charge` or Square recurring invoices), the system automatically applies payments to their active plan and marks it COMPLETE when fully paid.
- Payment plan admin REST API with pagination, filtering, and a lightweight HTML dashboard.
- Square webhook handlers that apply payments to existing plans.
- Shared DTOs and utilities for the `pay.html` form and admin tools.

## Technology Stack
- Java 21
- Spring Boot 3.4.x
- Spring MVC, Spring Data JPA, Spring Security
- PostgreSQL (production) / H2 (tests)
- Thymeleaf templates for server-rendered views
- Square REST APIs (Invoices, Orders, Online Checkout)

## Repository Layout
```
payment-service/
├── pom.xml
├── README.md
├── src
│   ├── main
│   │   ├── java/com/example/simpletixwebhook
│   │   │   ├── PaymentServiceApplication.java
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   └── util/
│   │   └── resources
│   │       ├── application.properties (or application.yml)
│   │       ├── templates/
│   │       └── static/
│   └── test/java/com/example/simpletixwebhook
└── ...
```

## Prerequisites
- Java 21 SDK
- Maven 3.9+
- PostgreSQL database for production (or another JDBC-compatible database)
- Square Developer account with an access token and location ID

## Configuration
Set the following environment variables for the service (e.g., in DigitalOcean App Platform or `.env` files):

| Variable | Purpose |
|----------|---------|
| `SPRING_DATASOURCE_URL` | JDBC URL for the primary database |
| `SPRING_DATASOURCE_USERNAME` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
| `SQUARE_DATASOURCE_URL` *(optional)* | Separate JDBC URL for Square/order storage; defaults to the primary datasource |
| `SQUARE_DATASOURCE_USERNAME` *(optional)* | Separate DB user for Square data |
| `SQUARE_DATASOURCE_PASSWORD` *(optional)* | Separate DB password for Square data |
| `square.api.token` | Square access token (sandbox or production) |
| `SQUARE_LOCATION_ID` | Square location identifier |
| `SQUARE_APPLICATION_ID` | Square Web Payments SDK application ID |
| `SQUARE_WEBHOOK_SIGNATURE_KEY` | Square webhook signature secret |
| `SQUARE_WEBHOOK_NOTIFICATION_URL` | The externally visible URL Square posts webhooks to |
| `APP_ADMIN_USER` / `APP_ADMIN_PASS` | Admin credentials for the HTML dashboards |

If `SQUARE_DATASOURCE_*` variables are omitted, the service uses the primary datasource to store Square orders, items, and plans.

### application.properties sample
```properties
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}

square.datasource.url=${SQUARE_DATASOURCE_URL:${SPRING_DATASOURCE_URL}}
square.datasource.username=${SQUARE_DATASOURCE_USERNAME:${SPRING_DATASOURCE_USERNAME}}
square.datasource.password=${SQUARE_DATASOURCE_PASSWORD:${SPRING_DATASOURCE_PASSWORD}}

square.api.token=${SQUARE_API_TOKEN}
SQUARE_LOCATION_ID=${SQUARE_LOCATION_ID}
square.application.id=${SQUARE_APPLICATION_ID:}
SQUARE_WEBHOOK_SIGNATURE_KEY=${SQUARE_WEBHOOK_SIGNATURE_KEY}
SQUARE_WEBHOOK_NOTIFICATION_URL=${SQUARE_WEBHOOK_NOTIFICATION_URL:}
```

## Building & Running
```bash
mvn clean package
java -jar target/payment-service-0.0.1-SNAPSHOT.jar
```

During development you can run:
```bash
mvn spring-boot:run
```

### Running Tests
```bash
mvn test
```

## Key Endpoints
| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/payments/config` | Returns Square application/location IDs for Web Payments SDK |
| `POST` | `/api/payments/charge` | Charges a card tokenized by the Web Payments SDK; auto-applies to active payment plan |
| `POST` | `/charge` | Alias for `/api/payments/charge` (public, no authentication required) |
| `POST` | `/pay/{studentId}/plan` | Creates a payment plan and issues Square invoices |
| `GET` | `/api/plans` | Admin listing of payment plans (paged, filters by studentId/status) |
| `GET` | `/api/plans/{id}` | Retrieve a specific payment plan |
| `POST` | `/api/plans` | Create a new payment plan (admin) |
| `PUT` | `/api/plans/{id}/complete` | Manually mark a payment plan as complete (admin override) |
| `PUT` | `/api/plans/{id}/cancel` | Cancel a payment plan with optional reason |
| `POST` | `/api/square` | Receives Square webhooks (orders, payments, invoices) |
| `GET` | `/api/square/stats` | Aggregate item and revenue statistics |
| `GET` | `/api/square/items/page` | Paginated Square order items |

Static assets exposed via `/pay.html` and `/admin/plans.html` continue to work with the controllers above.

## Payment Plan Flow

### Overview
The payment service automatically tracks student payment plans and marks them complete when the total amount due is paid. Plans can be tracked by:
1. **Invoice ID** — for Square recurring invoice payments (WEEKLY/BIWEEKLY/MONTHLY)
2. **Student ID** — for direct payments via `/charge` endpoint (one-time or manual installments)

### Creating a Payment Plan
**Admin API:**
```bash
POST /api/plans
Content-Type: application/json

{
  "studentId": "12345",
  "email": "student@example.com",
  "cadence": "MONTHLY",
  "totalDue": 500.00,
  "installmentAmount": 100.00,
  "invoiceId": "inv_abc123"  // optional, for Square recurring invoices
}
```

**Response:**
```json
{
  "id": 1,
  "studentId": "12345",
  "email": "student@example.com",
  "cadence": "MONTHLY",
  "totalDue": 500.00,
  "installmentAmount": 100.00,
  "remaining": 500.00,
  "status": "ACTIVE",
  "invoiceId": "inv_abc123",
  "createdAt": "2025-01-15T10:00:00Z",
  "updatedAt": "2025-01-15T10:00:00Z"
}
```

### How Payments Auto-Apply

#### 1. Direct Payments via `/charge` or `/api/payments/charge` Endpoint
When a student makes a payment through the payment form (`/pay.html`):

```bash
POST /charge
{
  "amount": 100.00,
  "studentId": "12345",
  "sourceId": "cnon_...",  // Square payment token
  "email": "student@example.com"
}
```
or
```bash
POST /api/payments/charge
{
  "amount": 100.00,
  "studentId": "12345",
  "sourceId": "cnon_...",  // Square payment token
  "email": "student@example.com"
}
```

**What happens:**
1. Square Payments API charges the card
2. `SquarePaymentApiController.charge()` receives the response
3. Controller calls `PaymentPlanService.applyPaymentByStudentId("12345", 100.00, paymentId)`
4. Service finds the first ACTIVE plan for student `12345`
5. Service subtracts $100 from `remaining` balance: `500.00 - 100.00 = 400.00`
6. Service saves the updated plan
7. If `remaining <= 0`, service sets `status = COMPLETE`

**Logs:**
```
Payment sqpay_xyz123 for $100.00 applied to payment plan for student 12345
Payment sqpay_xyz123 applied to plan 1 for student 12345: $100.00 paid, remaining balance now $400.00
```

#### 2. Square Recurring Invoice Payments (Webhook)
When Square processes a recurring invoice payment:

```
Square sends webhook: payment.created event
{
  "type": "payment.created",
  "data": {
    "object": {
      "payment": {
        "id": "sqpay_abc456",
        "invoice_id": "inv_abc123",
        "total_money": { "amount": 10000, "currency": "USD" },
        "reference_id": "12345"
      }
    }
  }
}
```

**What happens:**
1. `SquareWebhookController.receiveSquareEvent()` receives webhook
2. Controller calls `PaymentPlanService.handleSquareWebhook(payload, "payment.created")`
3. Service extracts `invoice_id = "inv_abc123"`, `amount = 10000` (cents), `studentId = "12345"`
4. Service first tries invoice-based tracking:
   - Finds plan with `invoiceId = "inv_abc123"`
   - Subtracts $100 from remaining
   - Sets `status = COMPLETE` if `remaining <= 0`
5. If no invoice plan found, falls back to student ID:
   - Calls `applyPaymentByStudentId("12345", 100.00, "webhook-payment.created")`
   - Finds ACTIVE plan for student `12345`
   - Applies payment same as direct charge

**Logs:**
```
Payment applied to plan 1 invoice inv_abc123: paid=100.00, remaining=400.00, status=ACTIVE
```
Or (if using student ID fallback):
```
Webhook payment.created applied payment $100.00 to student 12345 plan via student ID tracking
```

### Auto-Completion Example

**Initial State:**
```json
{
  "id": 1,
  "studentId": "12345",
  "totalDue": 500.00,
  "remaining": 100.00,
  "status": "ACTIVE"
}
```

**Student pays final $150 via /charge:**
```bash
POST /api/payments/charge
{ "amount": 150.00, "studentId": "12345", ... }
```

**Updated State:**
```json
{
  "id": 1,
  "studentId": "12345",
  "totalDue": 500.00,
  "remaining": -50.00,  // overpaid
  "status": "COMPLETE"  // auto-marked complete!
}
```

**Logs:**
```
Payment plan 1 for student 12345 marked COMPLETE. Payment sqpay_xyz789 applied: $150.00 paid, balance was $100.00, now $-50.00
```

### Admin Management

**List all plans:**
```bash
GET /api/plans?page=0&size=20&status=ACTIVE
```

**Get plan for specific student:**
```bash
GET /api/plans?studentId=12345
```

**Manually complete a plan:**
```bash
PUT /api/plans/1/complete
```
Response: `{ "message": "Plan marked complete" }`

**Cancel a plan:**
```bash
PUT /api/plans/1/cancel
Content-Type: application/json

{ "reason": "Student withdrew from program" }
```
Response: `{ "message": "Plan canceled" }`

### Payment Plan Status Enum
- `ACTIVE` — plan is active and accepting payments
- `COMPLETE` — total amount paid, no more payments needed
- `CANCELED` — plan canceled by admin
- `ERROR` — payment processing error (reserved for future use)

### Cadence Enum
Defines installment frequency for Square recurring invoices:
- `WEEKLY` — every 7 days
- `BIWEEKLY` — every 14 days
- `MONTHLY` — monthly on invoice creation day

### Implementation Details
- **Service:** `PaymentPlanService` (`src/main/java/.../service/PaymentPlanService.java`)
- **Repository:** `PaymentPlanRepository` (JPA with custom queries)
- **Entity:** `PaymentPlan` with fields: `studentId`, `email`, `cadence`, `totalDue`, `installmentAmount`, `remaining`, `status`, `invoiceId`
- **Controllers:** `SquarePaymentApiController` (charge endpoint), `PaymentPlanAdminController` (admin API), `SquareWebhookController` (webhooks)

### Key Methods
| Method | Purpose |
|--------|---------|
| `applyPaymentByStudentId(studentId, amount, paymentId)` | Apply payment to student's ACTIVE plan by student ID |
| `handleSquareWebhook(payload, eventType)` | Process Square payment/invoice webhooks and apply to plans |
| `completePlan(planId)` | Admin override to mark plan complete |
| `cancelPlan(planId, reason)` | Admin cancellation with audit log |
| `findActivePaymentPlan(studentId)` | Get the active plan for a student |

### Monitoring & Troubleshooting
**Check if payment applied:**
```bash
GET /api/plans?studentId=12345
```
Look at `remaining` field and `status`.

**View webhook logs:**
```bash
# In server logs, search for:
grep "Payment applied to plan" application.log
grep "payment plan" application.log
```

**Common issues:**
- **Payment not applied:** Check that `studentId` matches exactly between payment and plan
- **Multiple plans:** If student has >1 ACTIVE plan, payment applies to first one only (warning logged)
- **Webhook failures:** Verify Square signature key configured, check for JSON parse errors in logs

## Security
**Public endpoints:**
- `/pay`, `/pay.html`, `/charge`, `/api/payments/charge` and all static assets are public (no authentication required)

 | `SPRING_DATASOURCE_URL` | JDBC URL for the primary database (e.g. `jdbc:postgresql://payment-db:5432/payment_db` or managed DB host) |
 | `SPRING_DATASOURCE_USERNAME` | Database user (e.g. `payment_user`) |
 | `SPRING_DATASOURCE_PASSWORD` | Database password (ensure it is set; missing value causes startup failure) |
 | `SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT` | Hibernate dialect (e.g. `org.hibernate.dialect.PostgreSQLDialect`) |
**Webhooks:**
- Webhook controller validates Square signatures using the configured secret.

| `SQUARE_API_TOKEN` | Square access token (sandbox or production) |
1. Configure the environment variables listed above wherever you deploy (DigitalOcean, Heroku, etc.).
| `SQUARE_APPLICATION_ID` | Square Web Payments SDK application ID |
3. Schedule database backups for the payment plan repository.
4. Monitor logs for Square webhook failures or invoice creation errors.

## Migration from `KR_API`
This module was extracted from the monolithic `KR_API`. When migrating:
- Remove the payment controllers/services/entities from the original project.
- Replace direct method calls with REST or messaging calls into this service.
- Update deployment manifests to deploy both services and point UI links (`/pay`, `/admin/plans.html`) to the new host if necessary.

## Roadmap Ideas
- Publish events when payment plans change for downstream consumers.
- Replace manual HTTP clients with Square SDK and WebClient.
- Add comprehensive integration tests using MockWebServer or WireMock.
- Containerize with Docker for independent deployment.
square.api.token=${SQUARE_API_TOKEN}
square.location.id=${SQUARE_LOCATION_ID}
square.application.id=${SQUARE_APPLICATION_ID:}
square.webhook.signature.key=${SQUARE_WEBHOOK_SIGNATURE_KEY}
square.webhook.notification.url=${SQUARE_WEBHOOK_NOTIFICATION_URL:}

# JPA
spring.jpa.hibernate.ddl-auto=${SPRING_JPA_HIBERNATE_DDL_AUTO:update}
spring.jpa.properties.hibernate.dialect=${SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT:org.hibernate.dialect.PostgreSQLDialect}

### DigitalOcean App Platform Notes
- Set env vars on the payment-service component: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT`, and Square vars.
- Verify the DB user/password matches your PostgreSQL instance. A missing `SPRING_DATASOURCE_PASSWORD` results in: “The server requested password-based authentication, but no password was provided”.
- If using the internal Postgres container (compose), use host `payment-db` and port `5432`. For Managed Databases, use the DO-provided hostname, port, db name, user, and password.
- After updating env vars, redeploy the app.
