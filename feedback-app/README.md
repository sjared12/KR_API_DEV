# Event Feedback App

Standalone Node.js/Express app for collecting post-event feedback. Designed to be deployed separately from the main API on Digital Ocean.

## Features

- 🎨 Beautiful, mobile-responsive survey interface
- ⭐ 5-star rating system
- 📝 Optional comments and email collection
- 🔗 Event pre-fill via URL query params (e.g., `?event=Homecoming%202025`)
- 🚀 Lightweight Express server with API proxy
- 🐳 Docker-ready for easy deployment
- ✅ XSS-safe form handling

## Quick Start (Local Development)

1. **Install dependencies:**
   ```bash
   npm install
   ```

2. **Set API endpoint** (optional, defaults to `http://localhost:8080`):
   ```bash
   export API_ENDPOINT=https://your-main-api.com
   ```

3. **Start the server:**
   ```bash
   npm start
   ```

4. **Open in browser:**
   ```
   http://localhost:3000
   ```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | Port the app listens on | `3000` |
| `API_ENDPOINT` | URL of your main Spring Boot API | `http://localhost:8080` |
| `NODE_ENV` | Environment (production/development) | `development` |

## Deploy to Digital Ocean App Platform

**IMPORTANT:** If Digital Ocean tries to create two services (Docker + Node.js), follow these steps:

### Fix Double Deployment Issue

1. **Delete the Dockerfile before deploying:**
   ```bash
   cd feedback-app
   rm Dockerfile .dockerignore
   git add -A
   git commit -m "Remove Docker, use Node.js only"
   git push
   ```

2. **Or, in Digital Ocean Apps dashboard:**
   - After connecting GitHub, you'll see detected components
   - **Delete** the Docker component (click X or trash icon)
   - **Keep only** the Node.js web service
   - Continue with deployment

### Option 1: Deploy from GitHub (Recommended)

1. **Push code to GitHub:**
   ```bash
   cd feedback-app
   git init
   git add .
   git commit -m "Initial feedback app"
   git remote add origin <your-repo-url>
   git push -u origin main
   ```

2. **Create Digital Ocean App:**
   - Go to [Digital Ocean Apps](https://cloud.digitalocean.com/apps)
   - Click **Create App**
   - Connect your GitHub repository
   - Select the `feedback-app` directory as the source
   - Digital Ocean will auto-detect it as a **Node.js** app

3. **Configure build settings:**
   - **Build Command:** Leave empty (no build needed)
   - **Run Command:** `node server.js` (should be auto-detected)
   - **HTTP Port:** `3000` (auto-detected from PORT env var)

4. **Configure environment variables:**
   - In the Digital Ocean Apps dashboard, go to **Settings → App-Level Environment Variables**
   - Add `API_ENDPOINT` = `https://your-main-api.ondigitalocean.app` (mark as secret/encrypted)
   - Digital Ocean will auto-set `PORT=3000`
   - These are stored securely and not in your code/YAML

5. **Deploy:**
   - Click **Create Resources**
   - Digital Ocean will deploy as a Node.js app (NOT Docker)

**Important:** If Digital Ocean tries to deploy both Docker and Node.js, the `.do/app.yaml` file will force it to use Node.js only.

### Option 2: Deploy with Docker (Advanced)

1. **Build the Docker image:**
   ```bash
   docker build -t feedback-app .
   ```

2. **Test locally:**
   ```bash
   docker run -p 3000:3000 \
     -e API_ENDPOINT=https://your-main-api.com \
     feedback-app
   ```

3. **Push to Digital Ocean Container Registry:**
   ```bash
   # Install doctl and authenticate
   doctl auth init
   
   # Create registry (if needed)
   doctl registry create your-registry-name
   
   # Log in to registry
   doctl registry login
   
   # Tag and push
   docker tag feedback-app registry.digitalocean.com/your-registry-name/feedback-app:latest
   docker push registry.digitalocean.com/your-registry-name/feedback-app:latest
   ```

4. **Deploy from registry in Digital Ocean Apps**

## Usage

### Survey Link (Event Required)
The feedback form **requires** an event name in the URL. Share links like:
```
https://your-feedback-app.ondigitalocean.app?event=Homecoming%202025
```

**Important:** Users cannot access the survey without a valid `?event=` parameter. If they try to visit the URL without it, they'll see an error message.

### URL Format
```
https://your-feedback-app.ondigitalocean.app?event=YOUR_EVENT_NAME
```

Replace spaces with `%20` or use URL encoding. Examples:
- `?event=Homecoming%202025`
- `?event=Basketball%20Game`
- `?event=Spring%20Concert`

### QR Code Generation
Generate QR codes pointing to your event-specific survey URL for easy access at events.

**Example QR code URL:**
```
https://your-feedback-app.ondigitalocean.app?event=Homecoming%202025
```

Recommended tools:
- [QR Code Generator](https://www.qr-code-generator.com/)
- [Canva QR Code](https://www.canva.com/qr-code-generator/)

## API Endpoints

This app proxies feedback submissions to your main API:

- **POST** `/api/feedback` - Submit feedback (proxies to main API)
- **GET** `/health` - Health check endpoint

## File Structure

```
feedback-app/
├── server.js           # Express server with proxy
├── package.json        # Node.js dependencies
├── Dockerfile          # Docker configuration
├── .dockerignore       # Docker build exclusions
├── .gitignore          # Git exclusions
├── README.md           # This file
└── public/
    └── index.html      # Survey frontend
```

## Troubleshooting

### App won't connect to main API
- Ensure `API_ENDPOINT` is set correctly in environment variables
- Check that your main API allows CORS from the feedback app domain
- Verify `/api/feedback` endpoint is publicly accessible (no auth required)

### Build fails on Digital Ocean
- Make sure `package.json` and `server.js` are in the root of your selected directory
- Verify Node.js version is >= 18.0.0
- If it tries to build both Docker and Node.js, edit the app settings and set the **Type** to "Node.js" only
- Delete the `Dockerfile` if you only want Node.js deployment (or keep it for local testing)

### Survey doesn't submit
- Check browser console for errors
- Verify API endpoint is reachable from the browser
- Ensure main API has CSRF disabled for `/api/feedback/**`

## Security Notes

- Survey submissions are public (no authentication required)
- Data is proxied through this app to your main API
- No sensitive data is stored in this app
- Consider rate limiting if deploying publicly

## Customization

### Change Colors/Branding
You can adjust the theme directly in `public/index.html` within the `<style>` block:

- Background image and overlay:
   - The page uses `public/background.png` and a readability overlay via `body::before`.
   - Replace `public/background.png` with your own image.
   - Tweak the gradient overlay stops in the `body::before { background: linear-gradient(...) }` rule if you want it lighter/darker.
- Primary palette (gold/brown):
   - Buttons and accents use a gold-to-brown gradient (`#c9b037` → `#7c5a2a`).
   - Update those color values where gradients are defined (e.g., button backgrounds, containers, rating styles) to match your brand.
- Inputs and focus states:
   - Inputs use semi-transparent dark-brown backgrounds with a gold focus ring (`#c9b037`).
   - Adjust these hex values in the input and `:focus` rules if needed.

### Add Custom Fields
1. Add HTML input in `public/index.html` form
2. Include field in JavaScript `payload` object
3. Update main API to accept new field in `FeedbackResponse` model

## License

MIT
