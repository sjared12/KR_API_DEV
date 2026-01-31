# Subscription Management System - Deployment Guide

## Pre-Deployment Checklist

- [ ] All source code committed to git
- [ ] Reviewed code for hardcoded secrets (none should exist)
- [ ] Tested locally with multiple users
- [ ] Tested complete refund workflow
- [ ] Reviewed error handling
- [ ] Set up database backups
- [ ] Configured logging
- [ ] SSL/TLS certificates ready
- [ ] Environment variables documented
- [ ] Load tested (optional)

## Development Environment

### Prerequisites
```bash
- Java 21+
- Maven 3.9+
- PostgreSQL 12+
- Git
```

### Setup
```bash
# Clone repository
git clone https://github.com/sjared12/KR_API.git
cd KR_API

# Set environment variables
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/simpletixdb
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export APP_ADMIN_USER=admin
export APP_ADMIN_PASS=changeit

# Build
mvn clean package -DskipTests

# Run
java -jar target/simpletix-webhook-0.0.1-SNAPSHOT.jar
```

## Staging Environment

### Database Setup
```bash
# Create database
createdb simpletixdb_staging

# Initialize tables (automatic via Hibernate)
# No manual schema creation needed
```

### Environment Configuration
```bash
# Set staging environment variables
export SPRING_DATASOURCE_URL=jdbc:postgresql://staging-db.internal:5432/simpletixdb_staging
export SPRING_DATASOURCE_USERNAME=staging_user
export SPRING_DATASOURCE_PASSWORD=<secure_password>
export APP_ADMIN_USER=staging_admin
export APP_ADMIN_PASS=<secure_password>
export SQUARE_API_TOKEN=<sandbox_token>
export SQUARE_LOCATION_ID=<sandbox_location>
export SQUARE_WEBHOOK_SIGNATURE_KEY=<sandbox_signature_key>
```

### Deployment Command
```bash
# Build
mvn clean package -DskipTests

# Run with nohup for background execution
nohup java -jar target/simpletix-webhook-0.0.1-SNAPSHOT.jar &

# Or with systemd service
sudo systemctl start simpletix-webhook
```

## Production Environment

### Database Setup

#### PostgreSQL Configuration
```sql
-- Create production database
CREATE DATABASE simpletixdb_prod
  WITH
    OWNER = prod_user
    ENCODING = 'UTF8'
    TEMPLATE = template0;

-- Create application user
CREATE USER simpletix_app WITH PASSWORD '<strong_password>';

-- Grant privileges
GRANT CONNECT ON DATABASE simpletixdb_prod TO simpletix_app;
GRANT USAGE ON SCHEMA public TO simpletix_app;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO simpletix_app;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO simpletix_app;

-- Enable auto-privileges
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO simpletix_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO simpletix_app;

-- Backup policy
-- Create daily backups at 2 AM UTC
-- Retention: 30 days
-- Test restore weekly
```

#### Database Backups
```bash
#!/bin/bash
# backup-db.sh - Run daily via cron

BACKUP_DIR="/backups/postgresql"
DB_NAME="simpletixdb_prod"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/$DB_NAME-$DATE.sql.gz"

pg_dump -h localhost -U simpletix_app $DB_NAME | gzip > $BACKUP_FILE

# Keep only last 30 days
find $BACKUP_DIR -name "$DB_NAME-*.sql.gz" -mtime +30 -delete

# Upload to S3 (optional)
aws s3 cp $BACKUP_FILE s3://backups-bucket/$DB_NAME/

echo "Backup completed: $BACKUP_FILE"
```

Add to crontab:
```
0 2 * * * /scripts/backup-db.sh
```

### Environment Configuration

```bash
# Production environment variables (.env.prod)
export SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db.internal:5432/simpletixdb_prod
export SPRING_DATASOURCE_USERNAME=simpletix_app
export SPRING_DATASOURCE_PASSWORD=<production_password>

# Spring configuration
export SPRING_PROFILES_ACTIVE=prod
export SERVER_PORT=8080
export SERVER_SSL_ENABLED=true
export SERVER_SSL_KEY_STORE=/etc/simpletix/keystore.jks
export SERVER_SSL_KEY_STORE_PASSWORD=<keystore_password>

# Admin credentials (must be changed immediately after deployment)
export APP_ADMIN_USER=<production_admin_username>
export APP_ADMIN_PASS=<production_admin_password>

# Square production credentials
export SQUARE_API_TOKEN=<production_token>
export SQUARE_LOCATION_ID=<production_location>
export SQUARE_WEBHOOK_SIGNATURE_KEY=<production_signature_key>
export SQUARE_APPLICATION_ID=<production_app_id>

# Refund configuration
export SUBSCRIPTION_REFUND_PROCESSING_FEE_PERCENT=2.5
export SUBSCRIPTION_ENABLE_NOTIFICATIONS=true
export REFUND_NOTIFICATION_EMAIL=ops@company.com

# Logging
export LOGGING_LEVEL_ROOT=INFO
export LOGGING_LEVEL_COM_EXAMPLE_SIMPLETIXWEBHOOK=INFO
```

### Docker Deployment

#### Dockerfile (create in project root)
```dockerfile
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache tzdata

WORKDIR /app

COPY target/simpletix-webhook-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Docker Compose (for multi-container setup)
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: simpletix-db
    environment:
      POSTGRES_DB: simpletixdb_prod
      POSTGRES_USER: simpletix_app
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-db.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U simpletix_app"]
      interval: 10s
      timeout: 5s
      retries: 5

  simpletix-app:
    build: .
    container_name: simpletix-webhook
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/simpletixdb_prod
      SPRING_DATASOURCE_USERNAME: simpletix_app
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      APP_ADMIN_USER: ${ADMIN_USER}
      APP_ADMIN_PASS: ${ADMIN_PASS}
      SQUARE_API_TOKEN: ${SQUARE_API_TOKEN}
      SQUARE_LOCATION_ID: ${SQUARE_LOCATION_ID}
      SQUARE_WEBHOOK_SIGNATURE_KEY: ${SQUARE_WEBHOOK_SIGNATURE_KEY}
    ports:
      - "8080:8080"
    volumes:
      - ./logs:/app/logs
    restart: unless-stopped

volumes:
  postgres_data:
```

#### Deploy Docker
```bash
# Build image
docker build -t simpletix-webhook:latest .

# Run container
docker run -d \
  --name simpletix-webhook \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/simpletixdb_prod \
  -e SPRING_DATASOURCE_USERNAME=simpletix_app \
  -e SPRING_DATASOURCE_PASSWORD=$DB_PASSWORD \
  -e APP_ADMIN_USER=$ADMIN_USER \
  -e APP_ADMIN_PASS=$ADMIN_PASS \
  -e SQUARE_API_TOKEN=$SQUARE_TOKEN \
  -e SQUARE_LOCATION_ID=$SQUARE_LOCATION \
  -p 8080:8080 \
  simpletix-webhook:latest

# Or use Docker Compose
docker-compose -f docker-compose.yml up -d
```

### Kubernetes Deployment (Optional)

#### Deployment manifest (k8s-deployment.yaml)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: simpletix-webhook
  namespace: production
spec:
  replicas: 3
  selector:
    matchLabels:
      app: simpletix-webhook
  template:
    metadata:
      labels:
        app: simpletix-webhook
    spec:
      containers:
      - name: simpletix-webhook
        image: registry.company.com/simpletix-webhook:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: simpletix-secrets
              key: db-url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: simpletix-secrets
              key: db-user
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: simpletix-secrets
              key: db-pass
        - name: SQUARE_API_TOKEN
          valueFrom:
            secretKeyRef:
              name: simpletix-secrets
              key: square-token
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1024Mi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /api/auth/permissions
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /api/auth/permissions
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: simpletix-webhook-service
  namespace: production
spec:
  type: LoadBalancer
  selector:
    app: simpletix-webhook
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
```

#### Deploy to Kubernetes
```bash
# Create secrets
kubectl create secret generic simpletix-secrets \
  --from-literal=db-url='jdbc:postgresql://db:5432/simpletixdb_prod' \
  --from-literal=db-user='simpletix_app' \
  --from-literal=db-pass='<password>' \
  --from-literal=square-token='<token>' \
  -n production

# Deploy
kubectl apply -f k8s-deployment.yaml

# Check status
kubectl get pods -n production
kubectl logs -n production -l app=simpletix-webhook
```

### Systemd Service (Traditional VPS)

#### Create service file (/etc/systemd/system/simpletix-webhook.service)
```ini
[Unit]
Description=SimpleTix Webhook API
After=network.target postgresql.service

[Service]
Type=simple
User=simpletix
WorkingDirectory=/opt/simpletix-webhook
Environment="SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/simpletixdb_prod"
Environment="SPRING_DATASOURCE_USERNAME=simpletix_app"
Environment="SPRING_DATASOURCE_PASSWORD=<password>"
ExecStart=/usr/bin/java -jar /opt/simpletix-webhook/app.jar
Restart=on-failure
RestartSec=10s
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

#### Deploy with Systemd
```bash
# Copy JAR
sudo cp target/simpletix-webhook-0.0.1-SNAPSHOT.jar /opt/simpletix-webhook/app.jar

# Set permissions
sudo chown simpletix:simpletix /opt/simpletix-webhook/app.jar

# Enable service
sudo systemctl daemon-reload
sudo systemctl enable simpletix-webhook
sudo systemctl start simpletix-webhook

# Check status
sudo systemctl status simpletix-webhook

# View logs
sudo journalctl -u simpletix-webhook -f
```

## Post-Deployment Verification

### Health Checks

```bash
# Check API availability
curl -s http://localhost:8080/api/auth/permissions | jq '.length'

# Check database connection
curl -s http://localhost:8080/api/auth/users | jq '.length'

# Check subscription endpoints
curl -s http://localhost:8080/api/subscriptions?page=0&size=1 | jq '.totalElements'
```

### Monitoring & Logging

#### Application Logs
```bash
# Real-time logs
tail -f /var/log/simpletix-webhook/app.log

# Search for errors
grep ERROR /var/log/simpletix-webhook/app.log

# Count by level
grep -c "ERROR" /var/log/simpletix-webhook/app.log
grep -c "WARN" /var/log/simpletix-webhook/app.log
grep -c "INFO" /var/log/simpletix-webhook/app.log
```

#### Database Monitoring
```sql
-- Check table sizes
SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename))
FROM pg_tables
WHERE schemaname != 'pg_catalog'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Check user activity
SELECT * FROM pg_stat_statements ORDER BY total_time DESC LIMIT 10;

-- Check connection count
SELECT count(*) FROM pg_stat_activity;
```

### Performance Tuning

#### PostgreSQL Configuration
```
# In postgresql.conf

# Buffer configuration
shared_buffers = 256MB
effective_cache_size = 1GB

# Connection pooling
max_connections = 200

# Query performance
work_mem = 16MB
maintenance_work_mem = 64MB

# Index usage
random_page_cost = 1.1
```

#### Application Configuration
```properties
# In application-prod.properties

# Database pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000

# JPA/Hibernate
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

## Monitoring & Alerting

### Prometheus Metrics (Optional)

Add to pom.xml:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Configure in application-prod.properties:
```properties
management.endpoints.web.exposure.include=health,metrics,prometheus
management.metrics.enable.jvm=true
management.metrics.enable.process=true
```

### Datadog Integration (Optional)

```bash
# Add Datadog agent
dd_trace_redis_port=6379
dd_trace_analytics_enabled=true

# Environment
export DD_AGENT_HOST=localhost
export DD_TRACE_ENABLED=true
```

## Security Hardening

### SSL/TLS Configuration

```bash
# Generate self-signed certificate (for testing only)
keytool -genkey -alias tomcat -storetype PKCS12 \
  -keyalg RSA -keysize 2048 \
  -keystore keystore.p12 -validity 365

# Or use Let's Encrypt
certbot certonly --standalone -d webhook.company.com
```

Configure in application.properties:
```properties
server.ssl.enabled=true
server.ssl.key-store=/path/to/keystore.p12
server.ssl.key-store-password=${SSL_PASSWORD}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=tomcat
```

### Firewall Configuration

```bash
# Allow only necessary ports
sudo ufw allow 22/tcp   # SSH
sudo ufw allow 8080/tcp # Application
sudo ufw allow 443/tcp  # HTTPS
sudo ufw enable
```

### Database Security

```bash
# Create restricted user (PostgreSQL)
CREATE ROLE app_readonly WITH LOGIN PASSWORD '<password>';
GRANT USAGE ON SCHEMA public TO app_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO app_readonly;

# Regular user with full access
CREATE ROLE app_readwrite WITH LOGIN PASSWORD '<password>';
GRANT USAGE ON SCHEMA public TO app_readwrite;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO app_readwrite;
```

## Disaster Recovery

### Recovery Procedures

```bash
# 1. Database failure - restore from backup
pg_restore -d simpletixdb_prod /backups/latest-backup.sql

# 2. Application failure - restart service
sudo systemctl restart simpletix-webhook

# 3. Complete system failure - rebuild from scratch
# - Provision new server
# - Install PostgreSQL
# - Deploy application
# - Restore database backup
```

### Maintenance Windows

```bash
# Schedule maintenance (e.g., 2 AM Sunday)
30 2 * * 0 /scripts/maintenance.sh

# Send user notification before maintenance
curl -X POST http://localhost:8080/api/announcements \
  -H "Content-Type: application/json" \
  -d '{"message":"Maintenance scheduled 2-3 AM UTC Sunday","severity":"info"}'
```

## Rollback Plan

```bash
# Keep previous version
cp /opt/simpletix-webhook/app.jar /opt/simpletix-webhook/app.jar.backup-v1.0.0

# Quick rollback
sudo systemctl stop simpletix-webhook
cp /opt/simpletix-webhook/app.jar.backup-v1.0.0 /opt/simpletix-webhook/app.jar
sudo systemctl start simpletix-webhook

# Database rollback (if needed)
pg_restore -d simpletixdb_prod /backups/before-upgrade.sql
```

## Support & Documentation

- Application logs: `/var/log/simpletix-webhook/app.log`
- Database backup location: `/backups/postgresql/`
- Configuration: `/opt/simpletix-webhook/.env.prod`
- Documentation: See README.md and SUBSCRIPTION_MANAGEMENT.md

---

## Deployment Checklist (Final)

- [ ] Database backups configured and tested
- [ ] SSL/TLS certificates installed
- [ ] Firewalls configured
- [ ] Admin password changed
- [ ] Database credentials rotated
- [ ] Square credentials verified (production)
- [ ] Monitoring and alerting configured
- [ ] Support documentation available
- [ ] Rollback plan documented
- [ ] Initial load test completed
- [ ] Security scan completed
- [ ] Compliance checklist reviewed

---

**Deployment Complete! Monitor application health and logs for any issues.**
