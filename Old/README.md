# SimpleTix Webhook API

Spring Boot API to receive SimpleTix webhook events and persist ticket sales to PostgreSQL.

**Multi-Module Project:** This repository contains two Spring Boot applications:
1. **SimpleTix Webhook API (main module)** - Port 8080 - Handles webhooks, scanning, feedback
2. **Payment Service** - Port 8081 - Handles payments, subscriptions, refunds

See [payment-service/subscription-docs/PROJECT_ARCHITECTURE.md](./payment-service/subscription-docs/PROJECT_ARCHITECTURE.md) for detailed module structure.

**Recent Update:** Subscription management system has been reorganized into the Payment Service module. See [payment-service/subscription-docs/](./payment-service/subscription-docs/) folder for complete documentation.

## Prerequisites
- Java 21
- Maven 3.9+
- PostgreSQL database (provide credentials via env vars)
- Docker (optional, for container build)

## Build and Run (Local)
1. Build the jar:
	- Run: `mvn clean package -DskipTests`
2. Run the app:
	- Run: `java -jar target/simpletix-webhook-0.0.1-SNAPSHOT.jar`
	- Environment variables expected:
	  - `SPRING_DATASOURCE_URL` (e.g., `jdbc:postgresql://host:5432/simpletixdb`)
	  - `SPRING_DATASOURCE_USERNAME`
	  - `SPRING_DATASOURCE_PASSWORD`
	  - `SQUARE_DATASOURCE_URL` (e.g., `jdbc:postgresql://host:5432/squaredb`)
	  - `SQUARE_DATASOURCE_USERNAME`
	  - `SQUARE_DATASOURCE_PASSWORD`
	  - `SQUARE_API_TOKEN` (Square API access token)
	  - `SQUARE_LOCATION_ID` (Square location associated with payments)
	  - `SQUARE_APPLICATION_ID` (Square Web Payments SDK application ID)
	  - `APP_ADMIN_USER` (main admin username for / and /index.html)
	  - `APP_ADMIN_PASS` (main admin password)
	  - `FEEDBACK_ADMIN_USER` (feedback admin username, default: feedbackadmin)
	  - `FEEDBACK_ADMIN_PASS` (feedback admin password, default: changeme)

## Build and Run (Docker)
1. Build image from repo root:
	- `docker build -t simpletix-webhook .`
2. Run container:
	- `docker run --rm -p 8080:8080 \
		 -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/db \
		 -e SPRING_DATASOURCE_USERNAME=postgres \
		 -e SPRING_DATASOURCE_PASSWORD=secret \
		 simpletix-webhook`

## Endpoints

### SimpleTix Webhook API (`/webhook/simpletix`)
- **POST** `/webhook/simpletix/sold` - Receive ticket sale events from SimpleTix
- **POST** `/webhook/simpletix/scanned` - Receive ticket scan events from SimpleTix
- **POST** `/webhook/simpletix/scan-notifications` - Receive scan notification events
- **POST** `/webhook/simpletix/manual-sale` - Manually create a ticket sale
- **POST** `/webhook/simpletix/**` - Catch-all webhook endpoint
- **GET** `/webhook/simpletix/sales?limit=100` - List ticket sales (default limit: 100, max: 200)
- **GET** `/webhook/simpletix/scans?limit=100` - List ticket scans (default limit: 100, max: 200)
- **GET** `/webhook/simpletix/dashboard/checkin-times` - Get check-in time distribution data
- **GET** `/webhook/simpletix/dashboard/summary` - Get dashboard summary statistics
- **GET** `/webhook/simpletix/dashboard/recent-stats` - Get recent activity statistics
- **GET** `/webhook/simpletix/dashboard/admin-scans` - Get admin scan data

### SimpleTix Legacy API (`/simpletix`)
- **GET** `/simpletix/sales?limit=100` - List ticket sales (legacy endpoint)
- **GET** `/simpletix/scans?limit=100` - List ticket scans (legacy endpoint)

### Square Webhook API (`/api/square`)
- **POST** `/api/square` - Receive webhook events from Square (stores full payload)
- **POST** `/api/square/manual-entry` - Manually create a Square order entry
- **GET** `/api/square/events?limit=100` - List Square webhook events (default limit: 100, max: 200)
- **GET** `/api/square/items` - List all Square order items
- **GET** `/api/square/items/summary` - Get summary statistics for Square items
- **GET** `/api/square/items/top5` - Get top 5 best-selling items (excludes service fees and donations)
- **GET** `/api/square/items/quantity/{name}` - Get quantity sold for a specific item by name
- **GET** `/api/square/stats` - Get overall Square sales statistics
- **GET** `/api/square/stats/orders-per-minute` - Get orders per minute time series data
- **GET** `/api/square/stats/total-orders` - Get total number of orders

### Payment Plans
- One-time payments: `/pay/{studentId}` now renders an embedded Square Web Payments SDK form. The payer enters the amount, email (optional), and card details. The page calls the backend to charge the card directly (no redirect).
- Optional display: include amount due via `/pay/{studentId}/{due}` or `/pay/{studentId}?due=123.45` (does not prefill Amount to Pay).
- Automated plans: From the pay form, submit the "Start Payment Plan" section to create and publish a Square installment invoice with cadence Weekly/Bi-Weekly/Monthly. The system creates a Square Customer (or finds existing by email), creates an Order, then generates an Invoice with installment payment requests. The final installment auto-adjusts to avoid overpayment.
- Admin: View plans at `/admin/plans` (requires `APP_ADMIN_USER`/`APP_ADMIN_PASS`). Under the hood it queries `/api/plans` with pagination and filters.

#### Admin Plans API (`/api/plans`)
- GET `/api/plans?page=0&size=25&studentId=&status=` — returns a Spring Page of plans. Optional filters: `studentId`, `status` in {ACTIVE, COMPLETE, CANCELED, ERROR}.
- GET `/api/plans/{id}` — returns a single plan.

#### Payments API (`/api/payments`)
- GET `/api/payments/config` — returns Square application/location IDs for the Web Payments SDK.
- POST `/api/payments/charge` — charges a card via token produced by the Web Payments SDK. Expects JSON `{ amount, studentId, sourceId, email?, verificationToken? }` and returns `{ paymentId, status, receiptUrl }`.

### Press Box API (`/api/pressbox`)
- **POST** `/api/pressbox/announcement` - Create a new press box announcement
- **GET** `/api/pressbox/latest` - Get the latest undisplayed announcement and mark it as displayed
- **GET** `/api/pressbox/scan-announcements` - Get scan-based announcements for recent ticket scans
- **GET** `/api/pressbox/announcements` - List all press box announcements
- **GET** `/api/pressbox/pending-count` - Get count of pending (undisplayed) announcements

### Dashboard Pages (`/dashboards`)
- **GET** `/dashboards/gate` - Serve gate dashboard HTML page
- **GET** `/dashboards/subscriptions` - Serve subscription manager GUI
- **GET** `/dashboard` - Dashboard now requires admin login in-page. Use `APP_ADMIN_USER` / `APP_ADMIN_PASS`. The page presents a login overlay and sends a custom header (X-Admin-Auth) to avoid browser basic auth prompts.

### Static Resources
- **GET** `/` - Homepage with webhook event viewer (Sales, Scans, Square tabs) — now requires in-page admin login using `APP_ADMIN_USER` / `APP_ADMIN_PASS` (overlay sends `X-Admin-Auth`).
- **GET** `/index.html` - Homepage (same as `/`) — protected by the same in-page login overlay.
- **GET** `/gate`, `/gate.html`, `/dashboards/gate` - Gate dashboard page
- **GET** `/dashboard`, `/dashboard.html` - Sales & check-in dashboard
- **GET** `/subscription-manager`, `/subscription-manager.html`, `/dashboards/subscriptions` - Subscription management GUI
- **GET** `/pressbox`, `/pressbox.html`, `/dashboards/pressbox` - Press box announcement display
- **GET** `/survey`, `/survey.html` - Public feedback survey
- **GET** `/feedback-admin`, `/feedback-admin.html`, `/admin/feedback` - Feedback admin panel
- **GET** `/manual-entry`, `/manual-entry.html` - Manual ticket entry form
- **GET** `/manual-sale`, `/manual-sale.html` - Manual sale entry form
- **GET** `/analytics`, `/analytics.html`, `/dashboards/analytics` - Analytics dashboard
- **GET** `/static/**` - Static resources (CSS, JS, images, etc.)

### Post-Event Survey
- Simple, public survey page: **GET** `/survey` (or `/survey.html`).
- Submit feedback: **POST** `/api/feedback` with JSON body `{ eventName?, rating (1-5), comments?, email? }`.
- Public summary (no PII): **GET** `/api/feedback/summary` returns count, average rating, and distribution by rating.
- Admin review (requires basic auth): **GET** `/admin/api/feedback` returns full feedback list ordered by newest.

#### Feedback Admin Panel
- Admin GUI: **GET** `/feedback-admin` (or `/feedback-admin.html`) - separate login using `FEEDBACK_ADMIN_USER` / `FEEDBACK_ADMIN_PASS`.

### Subscription Management API (`/api/subscriptions`, `/api/auth`, & `/api/refunds`)
**Module: `payment-service`**

Complete subscription management system with user authentication, permission-based access, refund workflow with automatic fee deduction, and Square integration. Part of the Payment Service module alongside payment plans and Square integration.

#### Features:
- ✅ Database-backed user authentication with bcrypt password hashing
- ✅ Permission-based role access control (7 permission types)
- ✅ Subscription creation, status tracking, and cancellation
- ✅ Refund request workflow with administrative approval
- ✅ Automatic processing fee deduction (configurable percentage)
- ✅ Complete audit trail (who approved/rejected and when)
- ✅ Square API integration for subscription and refund processing
- ✅ REST API for all operations with pagination support

#### Authentication Endpoints (`/api/auth/**`)
- **POST** `/api/auth/login` - Authenticate user (returns user profile and permissions)
- **POST** `/api/auth/users` - Create new user (admin only)
- **GET** `/api/auth/users` - List all users (admin only)
- **GET** `/api/auth/users/{id}` - Get user details
- **PUT** `/api/auth/users/{id}` - Update user profile
- **POST** `/api/auth/users/{id}/change-password` - Change own password
- **POST** `/api/auth/users/{id}/reset-password` - Reset user password (admin)
- **POST** `/api/auth/users/{id}/disable` - Disable user account (admin)
- **POST** `/api/auth/users/{id}/enable` - Enable user account (admin)
- **DELETE** `/api/auth/users/{id}` - Delete user (admin)
- **GET** `/api/auth/permissions` - List all available permissions

#### Subscription Endpoints (`/api/subscriptions/**`)
- **POST** `/api/subscriptions` - Create new subscription
- **GET** `/api/subscriptions` - List all subscriptions (paginated)
- **GET** `/api/subscriptions/{id}` - Get subscription by ID
- **GET** `/api/subscriptions/square/{squareId}` - Get subscription by Square ID
- **GET** `/api/subscriptions/customer/{email}` - Get subscriptions for customer
- **GET** `/api/subscriptions/active` - List active subscriptions
- **GET** `/api/subscriptions/search?query=...` - Search subscriptions
- **POST** `/api/subscriptions/{id}/cancel` - Cancel subscription
- **POST** `/api/subscriptions/{id}/pause` - Pause subscription
- **POST** `/api/subscriptions/{id}/resume` - Resume subscription
- **GET** `/api/subscriptions/stats/overview` - Get subscription statistics

#### Refund Endpoints (`/api/refunds/**`)
- **POST** `/api/refunds/request` - Request a refund
- **GET** `/api/refunds` - List all refunds (paginated)
- **GET** `/api/refunds/{id}` - Get refund by ID
- **GET** `/api/refunds/pending-approvals` - List pending approvals
- **GET** `/api/refunds/pending-approvals/count` - Count pending approvals
- **GET** `/api/refunds/status/{status}` - Filter refunds by status
- **GET** `/api/refunds/subscription/{subscriptionId}` - Get refunds for subscription
- **POST** `/api/refunds/{id}/approve` - Approve refund with fee deduction
- **POST** `/api/refunds/{id}/reject` - Reject refund request
- **POST** `/api/refunds/{id}/mark-processing` - Mark ready for Square processing
- **POST** `/api/refunds/{id}/complete` - Mark as completed in Square
- **GET** `/api/refunds/pending-square-processing` - Get refunds awaiting Square processing
- **GET** `/api/refunds/stats/overview` - Get refund statistics

#### Subscription Management GUI
- **GET** `/subscription-manager`, `/subscription-manager.html`, `/dashboards/subscriptions` - Complete web interface for subscription management
  - User authentication with login page
  - Dashboard with real-time statistics
  - Subscription management (create, view, pause, cancel)
  - Refund workflow (request, approve, reject)
  - Admin panel for user management and refund approvals
  - Responsive design with modern UI

See [payment-service/subscription-docs/SUBSCRIPTION_MANAGEMENT.md](./payment-service/subscription-docs/SUBSCRIPTION_MANAGEMENT.md) for complete API documentation, database schema, configuration, and usage examples.
- API endpoints (require custom basic auth):
  - **GET** `/api/feedback-admin/responses` - Get all feedback responses with separate admin credentials.
  - **DELETE** `/api/feedback-admin/responses/{id}` - Delete a specific feedback response.

Notes:
- CSRF is disabled for `/api/feedback/**` and `/api/feedback-admin/**` so you can POST from the survey page and admin panel without a token.
- To share the survey, use a QR code or link pointing to your domain + `/survey`.
- The feedback admin panel uses separate credentials from the main admin (APP_ADMIN_USER/PASS).
- Feedback data is stored in the `feedback_response` table in the default datasource (SimpleTix DB).

## Security
- Spring Security is enabled. We avoid browser Basic Auth prompts by using in-page login overlays with a custom header for admin areas.
- CSRF protection is enabled for user-facing endpoints. It is disabled for webhook and feedback/admin APIs that accept POSTs from pages.
 - Admin-protected pages: `/`, `/index.html`, and `/dashboard` require in-page login. The client stores a Basic token in `sessionStorage` and sends it in the `X-Admin-Auth` header with requests.

### Square Webhook Signature Verification
To validate incoming Square webhooks, set the following:

- `SQUARE_WEBHOOK_SIGNATURE_KEY` — from Square Developer Dashboard > Webhooks > Your Subscriptions > Signature Key
- `SQUARE_WEBHOOK_NOTIFICATION_URL` — EXACT URL configured for the subscription (recommended, especially if behind a proxy)

How it works:
- The app verifies `x-square-hmacsha256-signature` (HMAC-SHA256) and also supports legacy `x-square-signature` (HMAC-SHA1). The signature is computed over `notification_url + request_body` using the signature key.
- If the signature key is set and verification fails, the webhook returns HTTP 401.
- If the signature key is not set, verification is skipped (a warning is logged). Set the key in production.

## Database
- SimpleTix events are stored in the default datasource (SimpleTix DB).
- Square events are stored in the Square DB, using a separate datasource and repository.

## Viewer
- The homepage (`/` or `/index.html`) displays tabs for Sales, Scans, and Square events and is gated by an in-page admin login overlay.
- All event data is rendered securely to prevent XSS.

## Notes
- Project follows standard Maven layout under `src/main/*`.
- Dockerfile uses multi-stage build with Maven and Eclipse Temurin JDK 21.

## Frontend/Backend Split

### Frontend (SPA)
- Located in `feedback-app/` (Vite + React)
- SPA routes: `/survey`, `/feedback-admin`, `/gate.html`, `/dashboard`, `/`
- Uses environment variable `VITE_BACKEND_URL` to connect to backend API
- Redirects: `app.frontend.base-url` property controls where SPA redirects after login
- Access `/admin` for feedback admin panel (separate credentials)

### Backend (API)
- Located in `src/main/java/com/example/simpletixwebhook/`
- API endpoints documented above
- Handles webhook events, ticket sales, Square payments, admin authentication

## Containers and Orchestration

The repo includes per-app Dockerfiles and a reverse proxy with a simple landing page. You can run each app independently or together with Docker Compose.

**Services**
- Core API: see [Dockerfile](Dockerfile)
- Payment Service: see [payment-service/Dockerfile](payment-service/Dockerfile)
- Logger Service: see [Logger/Dockerfile](Logger/Dockerfile)
- Feedback App: see [feedback-app/Dockerfile](feedback-app/Dockerfile)
- Reverse Proxy: see [docker/nginx/default.conf](docker/nginx/default.conf) and landing [docker/landing/index.html](docker/landing/index.html)
- Compose: see [compose.yaml](compose.yaml)

### Run All (Compose)

Access the landing page at http://localhost:8088 after startup.

```bash
docker compose up --build -d
```

Optional: provide a `.env` file next to [compose.yaml](compose.yaml) to override defaults.

Example `.env`:

```bash
PAYMENT_DB_URL=jdbc:postgresql://db:5432/payments
PAYMENT_DB_USER=postgres
PAYMENT_DB_PASSWORD=postgres
```

### Run Each App Independently

Core API (Spring Boot):

```bash
docker build -t krapi/api .
docker run --rm -p 8080:8080 \
	-e JAVA_OPTS="-Xms256m -Xmx512m" \
	-e SPRING_DATASOURCE_URL="jdbc:postgresql://host:5432/simpletixdb" \
	-e SPRING_DATASOURCE_USERNAME=postgres \
	-e SPRING_DATASOURCE_PASSWORD=secret \
	krapi/api
```

Payment Service:

```bash
docker build -t krapi/payment-service payment-service
docker run --rm -p 8081:8080 \
	-e JAVA_OPTS="-Xms256m -Xmx512m" \
	-e SPRING_PROFILES_ACTIVE=prod \
	-e SPRING_DATASOURCE_URL="jdbc:postgresql://host:5432/payments" \
	-e SPRING_DATASOURCE_USERNAME=postgres \
	-e SPRING_DATASOURCE_PASSWORD=secret \
	krapi/payment-service
```

Logger Service:

```bash
docker build -t krapi/logger Logger
docker run --rm -p 8090:8080 \
	-e JAVA_OPTS="-Xms256m -Xmx512m" \
	krapi/logger
```

Feedback App (Node.js):

```bash
docker build -t krapi/feedback feedback-app
docker run --rm -p 3000:3000 \
	-e PORT=3000 \
	-e API_ENDPOINT=http://localhost:8080 \
	krapi/feedback
```

Reverse Proxy + Landing:

```bash
docker build -t krapi/reverse-proxy docker/nginx
docker run --rm -p 8088:80 krapi/reverse-proxy
```

### Proxy Routes
- `/` → landing page
- `/api/` → Core API
- `/payment/` → Payment Service
- `/logger/` → Logger Service
- `/feedback/` → Feedback App

If you run a service standalone, access it directly on the published port (e.g., Core API on http://localhost:8080).

### Using the Admin Portal with DigitalOcean App Platform
- Set the Admin Portal env vars in your DO App: `DO_API_TOKEN` (Personal Access Token with write access) and `DO_APP_ID` (the target App Platform app ID). Both are already wired in [compose.yaml](compose.yaml) as optional envs.
- Start the Admin Portal from the DO deploy (Docker socket features are not available on App Platform; only the DO API actions work there).
- In the Admin Portal UI, choose a service, click **Set env in DO**, and enter key/value pairs. This calls the DO Apps API to update env vars for that service. Repeat per service as needed.
- Click **Trigger Deploy** to apply the updated env vars. DO will redeploy the app with the new configuration.
- For production, move the portal config store off the container filesystem (e.g., managed DB or DO DB) since the App Platform filesystem is ephemeral.


