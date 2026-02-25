# Keycloak setup for Payment Service

This document covers the minimum Keycloak configuration required for the payment-service app to authenticate users and authorize Admin/Refund Approver/End User roles via groups.

## 1) Realm
- Create or select a realm (example: `krhscougarband`).
- Set the realm URL to match your deployment, e.g. https://apps.dev.krhscougarband.org/auth

## 2) Client
Create a client for the payment-service UI:
- **Client ID**: `payment-service` (must match `KEYCLOAK_CLIENT_ID`)
- **Client type**: Public (SPA)
- **Standard Flow**: On
- **Direct Access Grants**: Off
- **Implicit Flow**: Off
- **PKCE**: Required (S256)

### Client URLs
- **Root URL**: `https://apps.dev.krhscougarband.org/payments`
- **Valid Redirect URIs**:
  - `https://apps.dev.krhscougarband.org/payments/*`
- **Web Origins**:
  - `https://apps.dev.krhscougarband.org`

### Logout
- **Front-channel logout**: On
- **Valid post-logout redirect URIs**:
  - `https://apps.dev.krhscougarband.org/payments`

## 3) Groups (Authorization)
Create these groups (exact names):
- **Admin**
- **Refund Approver**
- **End User**

Assign users to one of the groups. The app maps Keycloak groups to Spring roles:
- Admin → ROLE_ADMIN
- Refund Approver → ROLE_REFUND_APPROVER
- End User → ROLE_USER

## 4) Group Claims in Tokens
Ensure groups are included in tokens:
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
1. Visit https://apps.dev.krhscougarband.org/payments
2. Click **Sign in with KR Auth**
3. After login, confirm you can access:
   - User plans: /payments/plans
   - Admin dashboard (if Admin group): /payments/admin

## 7) App-side environment variables
Configure these for the payment-service app:
- `KEYCLOAK_AUTH_URL` = `https://apps.dev.krhscougarband.org/auth`
- `KEYCLOAK_REALM` = `krhscougarband`
- `KEYCLOAK_CLIENT_ID` = `payment-service`
- `KEYCLOAK_LOGOUT_REDIRECT_URI` = `https://apps.dev.krhscougarband.org/payments`

## 8) Troubleshooting
- **401 on API calls**: check client ID, issuer URL, and that access token includes groups.
- **No admin access**: confirm user is in Admin group and groups claim exists in access token.
- **CSP errors for Keycloak JS**: ensure `cdn.jsdelivr.net` is allowed in CSP (already set in app).
