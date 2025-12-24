# Gate Display Background Setup

## How to Add a Custom Background (Image or Video)

The gate check-in display (`/dashboards/gate`) supports both **custom background images** and **video backgrounds**.

### Steps to Upload Your Background:

#### **For Video Background (.mov, .mp4, .webm):**

1. **Prepare your video:**
   - Supported formats: `.mov`, `.mp4`, `.webm`
   - Recommended: 1920x1080 resolution or higher
   - Keep file size reasonable for loading speed
   - Video will loop automatically and play muted

2. **Name your video file:**
   - `background.mov` (or `background.mp4` or `background.webm`)

3. **Upload to the static folder:**
   - Place your video in: `/src/main/resources/static/background.mov`

4. **Rebuild and restart:**
   - Run: `mvn clean package`
   - Restart your application

#### **For Image Background (.jpg, .png):**

1. **Prepare your image:**
   - Name your image file: `background.jpg` (or `background.png`)
   - Recommended resolution: 1920x1080 or higher
   - Landscape orientation works best

2. **Upload to the static folder:**
   - Place your image in: `/src/main/resources/static/background.jpg`
   
3. **Rebuild and restart:**
   - Run: `mvn clean package`
   - Restart your application

### How It Works:

The page automatically detects your background in this priority order:
1. **Video first**: Looks for `background.mov`, `background.mp4`, or `background.webm`
2. **Image fallback**: If no video found, looks for `background.jpg` or `background.png`
3. **Default gradient**: If nothing found, displays the default purple gradient

A **40% dark overlay** is automatically applied over videos/images for text readability.

### Alternative Method (Direct Upload to Server):

If you're deploying with Docker or directly to a server, you can also place the file in the deployed static files directory:

- **Docker**: Mount a volume with your background file
- **Direct deployment**: Place in `target/classes/static/background.mov` (or `.jpg`) after building

### Supported Formats:

**Video:**
- `.mov` (QuickTime) ✅
- `.mp4` (H.264) ✅
- `.webm` ✅

**Image:**
- `.jpg` / `.jpeg` ✅
- `.png` ✅
- `.webp` ✅

### Tips:

- **For videos:**
  - Keep file size under 50MB for faster loading
  - Use a short loop (10-30 seconds) that loops smoothly
  - Videos play automatically, muted, and loop infinitely
  - Consider compressing your video for web use

- **For images:**
  - Use high-quality images for better appearance on large displays
  - Ensure the image isn't too busy - remember text will display over it

- The dark overlay helps with text readability but can be adjusted in the CSS if needed

### Testing:

Visit `/dashboards/gate` and your custom background should appear automatically!
