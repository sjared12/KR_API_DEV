# Subscription Management Documentation

Complete documentation for the subscription management system built into the Payment Service module.

## Quick Navigation

### Getting Started
- **[SUBSCRIPTION_QUICK_START.md](./SUBSCRIPTION_QUICK_START.md)** - 5-minute setup guide to get the system running locally
- **[SUBSCRIPTION_GUI_GUIDE.md](./SUBSCRIPTION_GUI_GUIDE.md)** - Web interface user guide with screenshots and workflows

### Reference
- **[SUBSCRIPTION_MANAGEMENT.md](./SUBSCRIPTION_MANAGEMENT.md)** - Complete API reference with all endpoints, request/response examples
- **[API_EXAMPLES.md](./API_EXAMPLES.md)** - 100+ curl examples for testing all endpoints

### Architecture & Implementation
- **[PROJECT_ARCHITECTURE.md](./PROJECT_ARCHITECTURE.md)** - Multi-module project structure and design
- **[IMPLEMENTATION_COMPLETE.md](./IMPLEMENTATION_COMPLETE.md)** - Implementation summary and feature list
- **[IMPLEMENTATION_SUMMARY.md](./IMPLEMENTATION_SUMMARY.md)** - Technical details and design decisions
- **[SUBSCRIPTION_REORGANIZATION.md](./SUBSCRIPTION_REORGANIZATION.md)** - How the system was reorganized into payment-service

### Deployment
- **[DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md)** - Production deployment with Docker, Kubernetes, systemd options

---

## System Overview

**Location:** `payment-service/` module  
**GUI:** `http://localhost:8081/subscription-manager`  
**API Base:** `http://localhost:8081/api/`

### Core Features
- ✅ User authentication with bcrypt password hashing
- ✅ Permission-based role access control (8 permission types)
- ✅ Subscription management (create, view, pause, cancel)
- ✅ Refund workflow with approval and automatic fee deduction
- ✅ 34+ REST API endpoints
- ✅ Web GUI with dashboard and admin panel
- ✅ Complete audit trail
- ✅ Production-ready with transactions

### Quick Start Commands

```bash
# Build
mvn clean package -DskipTests

# Run Payment Service
java -jar payment-service/target/payment-service-*.jar

# Access GUI
http://localhost:8081/subscription-manager

# Default Credentials
Username: admin
Password: changeit
```

---

## File Organization

```
payment-service/
├── src/main/java/.../
│   ├── model/
│   │   ├── User.java
│   │   ├── Permission.java
│   │   ├── Subscription.java
│   │   └── SubscriptionRefund.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── SubscriptionRepository.java
│   │   └── SubscriptionRefundRepository.java
│   ├── service/
│   │   ├── UserService.java
│   │   ├── SubscriptionService.java
│   │   └── RefundService.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── SubscriptionController.java
│   │   └── RefundController.java
│   └── dto/
│       ├── AuthDTO.java
│       ├── SubscriptionDTO.java
│       └── RefundDTO.java
└── resources/static/
    └── subscription-manager.html
```

---

## API Endpoints Summary

### Authentication (11 endpoints)
- User login and management
- Password change/reset
- Permission management

### Subscriptions (11 endpoints)
- Create and list subscriptions
- Pause and cancel subscriptions
- Search and filter
- Statistics

### Refunds (15+ endpoints)
- Request refunds
- Approve and reject
- Track refund status
- Fee calculation

---

## Permission Types

1. `VIEW_SUBSCRIPTIONS` - View subscription list and details
2. `MANAGE_SUBSCRIPTIONS` - Create subscriptions
3. `CANCEL_SUBSCRIPTIONS` - Cancel/pause subscriptions
4. `REQUEST_REFUNDS` - Request new refunds
5. `APPROVE_REFUNDS` - Approve/reject refunds
6. `VIEW_REFUNDS` - View refund list
7. `MANAGE_USERS` - Create and manage users
8. `SYSTEM_ADMIN` - Full access

---

## Common Workflows

### Login to GUI
1. Navigate to `http://localhost:8081/subscription-manager`
2. Enter credentials (default: admin/changeit)
3. Dashboard appears with statistics

### Create a Subscription
1. Click **Subscriptions** tab
2. Click **+ New Subscription**
3. Enter Square subscription ID, customer ID, email, amount
4. Click **Create**

### Request a Refund
1. Click **Refunds** tab
2. Click **+ Request Refund**
3. Select subscription and amount
4. Submit request
5. Admin approves in **Admin Panel**

### Create a New User
1. Click **Admin Panel** tab
2. Click **+ Create User**
3. Enter username, email, password
4. Select permissions
5. Click **Create User**

---

## Support & Help

- **GUI not loading?** Check that payment-service is running on port 8081
- **Login failed?** Verify credentials in `application.properties`
- **API returning 404?** Ensure full path is correct (e.g., `/api/subscriptions`)
- **Database issues?** Check PostgreSQL connection and migrations

For specific issues, see the relevant documentation file above.

---

## Technology Stack

- **Backend:** Spring Boot 3.4.10, Java 21
- **Database:** PostgreSQL
- **ORM:** JPA/Hibernate
- **Security:** Spring Security, bcrypt
- **Frontend:** HTML5, CSS3, Vanilla JavaScript
- **Build:** Maven
- **Deployment:** Docker, Docker Compose, Kubernetes

---

## Next Steps

1. Read [SUBSCRIPTION_QUICK_START.md](./SUBSCRIPTION_QUICK_START.md) for local setup
2. Explore [SUBSCRIPTION_GUI_GUIDE.md](./SUBSCRIPTION_GUI_GUIDE.md) for interface walkthrough
3. Review [SUBSCRIPTION_MANAGEMENT.md](./SUBSCRIPTION_MANAGEMENT.md) for API details
4. Check [DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md) for production setup

---

**Last Updated:** December 5, 2025  
**Status:** Complete and Production Ready
