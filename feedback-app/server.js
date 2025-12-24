const express = require('express');
const axios = require('axios');
const cors = require('cors');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

// Admin Portal base (internal network) and dynamic config fetch
const ADMIN_PORTAL_URL = process.env.ADMIN_PORTAL_URL || 'http://admin-portal:4000';
let cachedConfig = { apiEndpoint: process.env.API_ENDPOINT || 'http://api:8080' };
let lastFetch = 0;
const TTL_MS = 30_000; // refresh config every 30s

async function loadConfig(force = false) {
  const now = Date.now();
  if (!force && now - lastFetch < TTL_MS) return cachedConfig;
  try {
    const resp = await axios.get(`${ADMIN_PORTAL_URL}/api/public-config/feedback`, { timeout: 3000 });
    const cfg = resp.data || {};
    const endpoint = cfg.API_ENDPOINT || cfg.apiEndpoint || cachedConfig.apiEndpoint;
    cachedConfig = { apiEndpoint: endpoint };
    lastFetch = now;
  } catch (e) {
    // keep previous cached value on failure
    lastFetch = now;
  }
  return cachedConfig;
}

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Proxy feedback submission to main API
app.post('/api/feedback', async (req, res) => {
  try {
    const { apiEndpoint } = await loadConfig();
    console.log('Received feedback submission:', req.body);
    
    // Forward to main API
    const response = await axios.post(`${apiEndpoint}/api/feedback`, req.body, {
      headers: { 'Content-Type': 'application/json' },
      timeout: 10000
    });
    
    console.log('Feedback submitted successfully');
    res.json(response.data);
  } catch (error) {
    console.error('Error submitting feedback:', error.message);
    
    if (error.response) {
      // API returned an error
      res.status(error.response.status).json(error.response.data);
    } else {
      // Network or other error
      res.status(500).json({ 
        error: 'Failed to submit feedback. Please try again later.' 
      });
    }
  }
});

// Expose current config for the client if needed
app.get('/config', async (req, res) => {
  const cfg = await loadConfig();
  res.json({ API_ENDPOINT: cfg.apiEndpoint });
});

// Serve index.html for root and /survey
app.get(['/', '/survey'], (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Start server
app.listen(PORT, async () => {
  await loadConfig(true);
  console.log(`Feedback app running on port ${PORT}`);
  console.log(`Admin portal: ${ADMIN_PORTAL_URL}`);
  console.log(`API endpoint (dynamic): ${cachedConfig.apiEndpoint}`);
  console.log(`Environment: ${process.env.NODE_ENV || 'development'}`);
});
