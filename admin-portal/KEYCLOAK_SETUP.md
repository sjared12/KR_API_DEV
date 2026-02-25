# Keycloak setup for Admin Portal

This document covers the minimum Keycloak configuration required for the admin-portal app to authenticate users and authorize Admin/Refund Approver/End User roles via groups.

## 1) Realm
- Create or select a realm (example: `krhscougarband`).
- Set the realm URL to match your deployment, e.g. https://apps.dev.krhscougarband.org/auth
- **Note**: This should be the same realm used for payment-service and other apps.

## 2) Client
Create a client for the admin-portal UI:
- **Client ID**: `admin-portal` (must match `KEYCLOAK_CLIENT_ID`)
- **Client type**: Public (SPA)
- **Standard Flow**: On
- **Direct Access Grants**: Off
- **Implicit Flow**: Off
- **PKCE**: Required (S256)

### Client URLs
- **Root URL**: `https://apps.dev.krhscougarband.org/admin`
- **Valid Redirect URIs**:
  - `https://apps.dev.krhscougarband.org/admin/*`
- **Web Origins**:
  - `https://apps.dev.krhscougarband.org`

### Logout
- **Front-channel logout**: On
- **Valid post-logout redirect URIs**:
  - `https://apps.dev.krhscougarband.org/admin`

## 3) Groups (Authorization)
Use the same groups created for payment-service:
- **Admin**
- **Refund Approver**
- **End User**

Assign users to one of the groups. The app maps Keycloak groups to Spring roles:
- Admin → ROLE_ADMIN
- Refund Approver → ROLE_REFUND_APPROVER
- End User → ROLE_USER

## 4) Group Claims in Tokens
Ensure groups are included in tokens (should already be configured if payment-service is set up):
- **Client Scopes** → `groups` scope enabled (default in most setups)
- OR add a **Protocol Mapper** on the client:
  - Mapper Type: Group Membership
  - Token Claim Name: `groups`
  - Full group path: On or Off (both supported)
  - Add to ID token + Access token: On

## 5) User Attributes (optional but recommended)
Keycloak should include these standard claims so the app can display names:
- `email`
- `given_name`
- `family_name`

These are included by default if the user profile fields are populated.

## 6) Test Login
1. Visit https://apps.dev.krhscougarband.org/admin
2. Click **Sign in with Keycloak**
3. After login, confirm you can access:
   - Dashboard: /admin (container management)
   - Configuration: /admin (service configuration section)
   - Admin features (if Admin group): Full access to all controls

## 7) App-side environment variables
Configure these for the admin-portal app:
- `KEYCLOAK_AUTH_URL` = `https://apps.dev.krhscougarband.org/auth`
- `KEYCLOAK_REALM` = `krhscougarband`
- `KEYCLOAK_CLIENT_ID` = `admin-portal`
- `KEYCLOAK_LOGOUT_REDIRECT_URI` = `https://apps.dev.krhscougarband.org/admin`

## 8) API Authentication
The admin-portal API endpoints (`/admin/api/**`) require Bearer token authentication:
- Include `Authorization: Bearer <access_token>` header in all API requests
- Tokens are automatically managed by the browser via Keycloak JS adapter
- Token refresh occurs every 60 seconds automatically

### Protected Endpoints
All endpoints under `/admin/api/**` require authentication:
- `GET /admin/api/auth/config` - Returns Keycloak configuration (unauthenticated)
- `GET /admin/api/health` - Health check (unauthenticated)
- `GET /admin/api/containers` - List services
- `POST /admin/api/containers` - Create service (requires ROLE_ADMIN)
- `GET /admin/api/containers/{id}` - Get service details
- `POST /admin/api/containers/{id}/{action}` - Control service (start/stop/restart)
- `DELETE /admin/api/containers/{id}` - Delete service (requires ROLE_ADMIN)
- `GET /admin/api/config/{service}` - Get service configuration
- `PUT /admin/api/config/{service}` - Update service configuration (requires ROLE_ADMIN)

## 9) Role-Based Access Control (RBAC)
Admin-only operations can be protected with Spring Security annotations:
- `@PreAuthorize("hasRole('ADMIN')")` on controller methods
- Users in Admin group automatically get `ROLE_ADMIN`
- Unauthenticated requests receive 401 Unauthorized
- Unauthorized role access receives 403 Forbidden

## 10) Troubleshooting
- **Login page instead of dashboard**: User is not authenticated. Click sign-in button to redirect to Keycloak.
- **401 on API calls**: Check that token is valid, client ID matches, and issuer URL is correct.
- **No access to admin features**: Confirm user is in Admin group and groups claim exists in access token.
- **CSP errors for Keycloak JS**: Ensure `cdn.jsdelivr.net` and `https://apps.dev.krhscougarband.org` are allowed in CSP (already set in app).
- **CORS errors**: Verify Keycloak server has CORS enabled for the admin-portal domain.
- **Token refresh fails**: Check that refresh token is valid and Keycloak token endpoint is accessible.

## 11) Migration from No Auth
If migrating from a previous version without authentication:
1. Ensure all users are created in Keycloak with proper group assignments
2. Assign users to appropriate groups (Admin, Refund Approver, End User)
3. Deploy updated admin-portal with environment variables configured
4. Users will be redirected to Keycloak login on first access
5. After login, they will have access based on their group membership
