# JWT Token Management & Security Features

## Overview
This document describes the enhanced JWT token security features implemented in the payment service, including token expiration, automatic refresh, remember-me functionality, and token revocation.

## Features Implemented

### 1. Shorter Token Expiration (1 hour default)
- **Access tokens** now expire after **1 hour** (configurable via `JWT_EXPIRATION`)
- **Refresh tokens** expire after **7 days** (configurable via `JWT_EXPIRATION_REMEMBER`)
- Reduces security risk by limiting the lifespan of compromised tokens

### 2. Automatic Token Refresh
- Tokens are automatically refreshed **5 minutes before expiration**
- No user interruption - seamless background refresh
- Uses refresh tokens to obtain new access tokens
- Falls back to login page if refresh fails

### 3. Remember Me Option
- Users can choose session duration at login
- **Without "Remember Me"**: 1-hour access token + 7-day refresh token
- **With "Remember Me"**: 7-day access token + 7-day refresh token
- Provides flexibility between security and convenience

### 4. Token Revocation (Logout)
- Tokens are actively revoked on logout
- Revoked tokens stored in database (`revoked_tokens` table)
- JWT filter checks blacklist before authentication
- Prevents use of stolen tokens after logout
- Daily cleanup job removes expired tokens at 3 AM

## Configuration

Add these environment variables to customize token expiration:

```bash
# Access token expiration (default: 1 hour = 3600000ms)
JWT_EXPIRATION=3600000

# Remember-me/refresh token expiration (default: 7 days = 604800000ms)
JWT_EXPIRATION_REMEMBER=604800000

# JWT secret key (must be at least 32 characters)
JWT_SECRET=your-secure-secret-key-min-32-characters-long
```

## API Endpoints

### Login
```http
POST /payments/api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "rememberMe": false
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "user@example.com",
  "expiresIn": 3600000
}
```

### Refresh Token
```http
POST /payments/api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "user@example.com",
  "expiresIn": 3600000
}
```

### Logout (Current Device)
```http
POST /payments/api/auth/logout
Authorization: Bearer {token}
```

**Response:**
```json
{
  "message": "Logged out successfully"
}
```

### Logout All Devices
```http
POST /payments/api/auth/logout-all
Authorization: Bearer {token}
```

**Response:**
```json
{
  "message": "Logged out from all devices successfully",
  "note": "Please re-login on all devices"
}
```

## Frontend Implementation

### Login Page
- Added "Remember Me" checkbox
- Stores both access token and refresh token
- Tracks token expiration time in localStorage

### Dashboard & Protected Pages
- Automatic token refresh 5 minutes before expiration
- Logout now revokes the token server-side
- Clears all auth-related data from localStorage

### Shared Auth Utilities
Created `/static/js/auth-utils.js` with reusable functions:
- `refreshAuthToken()` - Refresh the access token
- `setupTokenAutoRefresh()` - Schedule automatic refresh
- `performLogout()` - Logout with token revocation
- `initTokenAutoRefresh()` - Initialize on page load

To use in other pages:
```html
<script src="/payments/js/auth-utils.js"></script>
<script>
  document.addEventListener('DOMContentLoaded', initTokenAutoRefresh);
</script>
```

## Database Schema

### New Table: `revoked_tokens`
```sql
CREATE TABLE revoked_tokens (
  id UUID PRIMARY KEY,
  token_hash VARCHAR(64) NOT NULL UNIQUE,
  user_email VARCHAR(255) NOT NULL,
  revoked_at TIMESTAMP NOT NULL,
  expiry_time TIMESTAMP NOT NULL,
  reason VARCHAR(50)
);

CREATE INDEX idx_token_hash ON revoked_tokens(token_hash);
CREATE INDEX idx_expiry_time ON revoked_tokens(expiry_time);
```

## Security Improvements

1. **Reduced Attack Window**: 1-hour tokens limit exposure time
2. **Token Revocation**: Logout invalidates tokens immediately
3. **Blacklist Check**: Revoked tokens cannot be reused
4. **Automatic Cleanup**: Expired tokens removed daily
5. **Refresh Token Rotation**: New access tokens issued regularly
6. **User Control**: Remember-me option for flexibility

## Token Lifecycle

1. **Login**: User receives access token (1h) + refresh token (7d)
2. **Auto-Refresh**: 5 minutes before expiry, client requests new access token
3. **Continued Use**: Process repeats every hour until refresh token expires
4. **Logout**: Both tokens are revoked and stored in blacklist
5. **Re-Login Required**: After 7 days (or manual logout)

## Maintenance

### Automatic Token Cleanup
The `TokenCleanupService` runs daily at 3 AM to remove expired revoked tokens:
- Prevents database bloat
- Removes tokens that are already expired naturally
- No manual intervention required

### Monitoring
Monitor these metrics:
- Token refresh success/failure rates
- Number of revoked tokens in database
- Token cleanup job execution

## Testing

### Test Token Expiration
```bash
# Set short expiration for testing (5 minutes)
JWT_EXPIRATION=300000
```

### Test Auto-Refresh
1. Login and observe console logs
2. Wait ~4-5 minutes (for 5-min tokens) or 55 minutes (for 1-hour tokens)
3. Verify automatic refresh occurs
4. Check new token is stored

### Test Revocation
1. Login and copy the token
2. Logout
3. Try to use the copied token - should fail with 401

## Migration Guide

### For Existing Users
Existing users with old tokens will:
1. Continue using their current token until expiration
2. Be redirected to login when token expires
3. Get new token format on next login

### For Developers
Update frontend pages to include auto-refresh:
1. Add `<script src="/payments/js/auth-utils.js"></script>`
2. Call `initTokenAutoRefresh()` on page load
3. Update logout functions to use `performLogout()`

## Troubleshooting

### Token Refresh Fails
- Check `REFRESH_TOKEN` exists in localStorage
- Verify refresh token hasn't expired (7 days)
- Check network console for 401 errors
- User will be redirected to login automatically

### User Logged Out Unexpectedly
- Check token expiration time in localStorage
- Verify auto-refresh is set up correctly
- Check browser console for errors
- May indicate refresh token expired (7 days)

### Tokens Not Being Revoked
- Verify `RevokedToken` entity is created in database
- Check database for `revoked_tokens` table
- Verify JwtFilter has RevokedTokenRepository injected
- Check logout endpoint is being called

## Future Enhancements

Potential improvements:
1. **Active Session Management**: Track all active sessions per user
2. **Device Fingerprinting**: Link tokens to specific devices
3. **Suspicious Activity Detection**: Revoke tokens on anomalies
4. **Token Versioning**: Invalidate all tokens on password change
5. **Rate Limiting**: Prevent token refresh abuse
