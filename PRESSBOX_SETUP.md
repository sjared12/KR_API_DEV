# Press Box Announcer System

## Overview
The Press Box Announcer system displays names when specific ticket types (e.g., VIP, Premium) are scanned, and allows manual announcements to be sent from the dashboard.

## Features
- **Automatic Announcements**: When VIP/Premium tickets are scanned, names automatically appear on the press box display
- **Manual Announcements**: Send custom announcements from the dashboard with a name and optional message
- **Real-time Display**: Press box display polls every 2 seconds for new announcements
- **Manual Acknowledgment**: Announcements stay on screen until the "Acknowledged" button is pressed

## Access Points

### Press Box Display (Public)
- URL: `/pressbox` or `/pressbox.html` or `/dashboards/pressbox`
- Full-screen display for the press box announcer
- Shows standby screen when no announcements are pending
- Auto-fetches and displays announcements

### Dashboard Control (Public)
- URL: `/dashboard` or `/dashboard.html`
- Click the **"📢 Press Box Announcement"** button (bottom-right)
- Enter name (required) and optional message
- Click "Send Announcement" to display on press box

## API Endpoints

### Get Latest Announcement
```bash
GET /api/pressbox/latest
```
Returns the next undisplayed announcement and marks it as displayed.

Response (200 OK):
```json
{
  "id": 1,
  "name": "John Smith",
  "message": "Special guest tonight!",
  "ticketType": "VIP Access Pass",
  "source": "SCAN",
  "createdAt": "2025-10-20T12:34:56Z"
}
```

Response (204 No Content): No pending announcements

### Create Manual Announcement
```bash
POST /api/pressbox/announcement
Content-Type: application/json

{
  "name": "Jane Doe",
  "message": "Thank you for your support!"
}
```

Response (200 OK):
```json
{
  "id": 2,
  "name": "Jane Doe",
  "message": "Thank you for your support!",
  "success": true
}
```

### Get Recent Scan Announcements
```bash
GET /api/pressbox/scan-announcements?ticketTypes=VIP,Premium&limit=20
```
Returns recent scans filtered by ticket types (useful for debugging).

### Get All Announcements (Admin)
```bash
GET /api/pressbox/announcements?limit=50
```
Returns all announcements for debugging/admin purposes.

### Get Pending Count
```bash
GET /api/pressbox/pending-count
```
Returns the number of pending (undisplayed) announcements.

## Configuration

### Environment Variables

Set these in Digital Ocean App Platform:

```bash
# Ticket types that trigger automatic press box announcements
# Comma-separated, case-insensitive, partial match
PRESSBOX_TRIGGER_TICKET_TYPES=VIP,Premium,Backstage Pass,Meet and Greet

# Default if not set: VIP,Premium
```

### How Ticket Type Matching Works

The system uses **case-insensitive partial matching**:

- Configured: `VIP`
- Will match: "VIP", "vip", "VIP Access Pass", "Super VIP Ticket"

- Configured: `Premium,VIP`
- Will match any ticket containing "premium" OR "vip"

## Database Schema

### Table: `press_box_announcement`

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| name | VARCHAR(255) | Name to display (required) |
| message | TEXT | Optional message |
| ticket_type | VARCHAR(255) | Ticket type that triggered (if scan) |
| source | VARCHAR(20) | "SCAN" or "MANUAL" |
| created_at | TIMESTAMP | When announcement was created |
| displayed_at | TIMESTAMP | When announcement was shown |
| displayed | BOOLEAN | Whether announcement has been shown |

## Workflow

### Automatic Scan Announcements

1. Ticket is scanned via SimpleTix webhook
2. System checks if `OrderItemTitle` matches configured trigger types
3. If match: creates `PressBoxAnnouncement` record
4. Press box display polls `/api/pressbox/latest` every 2 seconds
5. API returns announcement and marks it as displayed
6. Display shows name with "Acknowledged" button
7. Announcer clicks "Acknowledged" to return to standby screen

### Manual Announcements

1. User opens dashboard at `/dashboard`
2. Clicks "📢 Press Box Announcement" button
3. Modal opens with form
4. User enters name (required) and message (optional)
5. Submits form → creates announcement via API
6. Press box display picks it up on next poll
7. Shows until announcer clicks "Acknowledged" button

## Display Behavior

- **Standby Mode**: "Press Box Announcer / Waiting for announcements..."
- **Announcement Mode**: Large gold text with name, optional message/ticket type, and green "Acknowledged" button
- **Animation**: Smooth scale-up transition with shimmer effect
- **Duration**: Stays on screen until "Acknowledged" button is clicked
- **Queue**: Announcements are queued and displayed one at a time

## Styling & Branding

The press box display (`/pressbox.html`) uses:
- Dark gradient background
- Gold shimmer text effect for names
- Large, readable fonts optimized for distance viewing
- Smooth animations for professional appearance
- Status indicator (only shows on connection errors)

## Testing

### Test Manual Announcement
1. Go to `/dashboard`
2. Click "📢 Press Box Announcement"
3. Enter "Test Name" and click Send
4. Open `/pressbox` in another window
5. Should see "Test Name" appear within 2 seconds
6. Click "Acknowledged" button to dismiss

### Test Scan Announcement
```bash
curl -X POST http://localhost:8080/webhook/simpletix/scanned \
  -H "Content-Type: application/json" \
  -d '{
    "OrderNumber": "TEST-001",
    "OrderItemTitle": "VIP Access Pass",
    "ParticipantBillingName": "John Smith"
  }'
```

Then check `/pressbox` display.

### Check Logs
```bash
# On startup, you should see:
"Press Box trigger ticket types: [vip, premium]"

# When VIP ticket is scanned:
"Created press box announcement for: John Smith (VIP Access Pass)"
```

## Troubleshooting

### Announcements not appearing on display
1. Check browser console for errors
2. Verify `/api/pressbox/latest` returns data
3. Check `/api/pressbox/pending-count` to see if announcements are queued
4. Ensure press box display is polling (check Network tab in DevTools)

### Scans not creating announcements
1. Check `PRESSBOX_TRIGGER_TICKET_TYPES` environment variable
2. Verify ticket type matches (case-insensitive partial match)
3. Check application logs for "Created press box announcement" messages
4. Verify SimpleTix webhook is working (`OrderItemTitle` field present)

### Manual announcements not working
1. Check browser console for API errors
2. Verify `/api/pressbox/announcement` endpoint is accessible
3. Check that name field is not empty
4. Verify CSRF token is not blocking (should be disabled for `/api/pressbox/**`)

## Security

All press box endpoints are publicly accessible (no authentication required):
- `/pressbox.html` - Display page
- `/api/pressbox/**` - All API endpoints

This allows the press box display to run without login credentials.

## Future Enhancements

Potential improvements:
- Admin page to manage/delete announcements
- Priority levels for announcements
- Sound effects when announcements appear
- Multiple display templates/themes
- Statistics on announcement views
- Integration with other ticketing systems
