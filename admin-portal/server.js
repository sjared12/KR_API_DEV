import express from 'express';
import cors from 'cors';
import fs from 'fs';
import path from 'path';
import fetch from 'node-fetch';

const app = express();
app.use(express.json());
app.use(cors());

// Auth removed by request: all endpoints are open

// Digital Ocean API Configuration
const DO_API_TOKEN = process.env.DO_API_TOKEN || '';
const DO_APP_ID = process.env.DO_APP_ID || '';
const DO_API_BASE = 'https://api.digitalocean.com/v2';

if (!DO_API_TOKEN) {
  console.warn('Warning: DO_API_TOKEN not set - App Platform features will not work');
}
if (!DO_APP_ID) {
  console.warn('Warning: DO_APP_ID not set - App Platform features will not work');
}

async function doAPI(endpoint, options = {}) {
  const url = `${DO_API_BASE}${endpoint}`;
  const response = await fetch(url, {
    ...options,
    headers: {
      'Authorization': `Bearer ${DO_API_TOKEN}`,
      'Content-Type': 'application/json',
      ...options.headers
    }
  });
  if (!response.ok) {
    const error = await response.text();
    throw new Error(`DO API Error: ${response.status} - ${error}`);
  }
  return response.json();
}

// Health
app.get('/api/health', (req, res) => {
  const masked = DO_APP_ID ? `${DO_APP_ID.slice(0, 6)}…${DO_APP_ID.slice(-4)}` : '';
  res.json({
    status: 'ok',
    doConfigured: !!DO_API_TOKEN,
    appIdConfigured: !!DO_APP_ID,
    appIdPreview: masked
  });
});

// Config store
const CONFIG_PATH = process.env.CONFIG_PATH || '/data/config.json';
function ensureConfig() {
  const dir = path.dirname(CONFIG_PATH);
  try { fs.mkdirSync(dir, { recursive: true }); } catch {}
  if (!fs.existsSync(CONFIG_PATH)) {
    const seed = {
      api: {
        SPRING_DATASOURCE_URL: 'jdbc:postgresql://db:5432/simpletixdb',
        SPRING_DATASOURCE_USERNAME: 'postgres',
        SPRING_DATASOURCE_PASSWORD: 'postgres'
      },
      payment: {
        SPRING_DATASOURCE_URL: 'jdbc:postgresql://db:5432/payments',
        SPRING_DATASOURCE_USERNAME: 'postgres',
        SPRING_DATASOURCE_PASSWORD: 'postgres'
      },
      logger: {},
      feedback: {
        API_ENDPOINT: 'http://api:8080'
      },
      proxy: {}
    };
    fs.writeFileSync(CONFIG_PATH, JSON.stringify(seed, null, 2));
  }
}
function readConfig() {
  ensureConfig();
  return JSON.parse(fs.readFileSync(CONFIG_PATH, 'utf8'));
}
function writeConfig(cfg) {
  fs.writeFileSync(CONFIG_PATH, JSON.stringify(cfg, null, 2));
}

app.get('/api/config', (req, res) => {
  try { res.json(readConfig()); } catch (e) { res.status(500).json({ error: e.message }); }
});
app.get('/api/config/:service', (req, res) => {
  try {
    const cfg = readConfig();
    const svc = cfg[req.params.service];
    if (!svc) return res.status(404).json({ error: 'Unknown service' });
    res.json(svc);
  } catch (e) { res.status(500).json({ error: e.message }); }
});
app.put('/api/config/:service', (req, res) => {
  try {
    const cfg = readConfig();
    if (!cfg[req.params.service]) return res.status(404).json({ error: 'Unknown service' });
    cfg[req.params.service] = { ...cfg[req.params.service], ...req.body };
    writeConfig(cfg);
    res.json({ ok: true, config: cfg[req.params.service] });
  } catch (e) { res.status(500).json({ error: e.message }); }
});

// List App Platform services across all apps accessible by the token
app.get('/api/containers', async (req, res) => {
  try {
    if (!DO_API_TOKEN) {
      console.warn('DO API not configured, returning empty list');
      return res.json([]);
    }

    // Allow optional filtering to a single app via query, otherwise aggregate all apps
    const requestedAppId = req.query.appId;
    let apps = [];

    try {
      if (requestedAppId) {
        const single = await doAPI(`/apps/${requestedAppId}`);
        apps = [single.app];
      } else {
        const list = await doAPI('/apps');
        apps = list.apps || [];
      }
    } catch (err) {
      // If list fails (RBAC / older token), fall back to DO_APP_ID when available
      if (DO_APP_ID) {
        console.warn('Falling back to DO_APP_ID after list failure:', err.message);
        const single = await doAPI(`/apps/${DO_APP_ID}`);
        apps = [single.app];
      } else {
        throw err;
      }
    }

    // Hydrate each app with current details (spec + active deployment)
    const appDetails = await Promise.all(apps.map(async app => {
      if (app.spec && app.active_deployment) return app;
      const fresh = await doAPI(`/apps/${app.id}`);
      return fresh.app || app;
    }));

    const items = appDetails.flatMap(app => {
      const spec = app.spec || {};
      const active = app.active_deployment || {};
      const activeServices = active.services || [];

      // Build a map of component statuses from the deployment progress tree
      const componentStatus = {};
      const progressSteps = active.progress?.steps || [];
      const walkSteps = step => {
        if (!step) return;
        if (step.component_name) {
          componentStatus[step.component_name] = step.status || step.name;
        }
        if (Array.isArray(step.steps)) {
          step.steps.forEach(walkSteps);
        }
      };
      progressSteps.forEach(walkSteps);

      const services = [
        ...((spec.services || []).map(s => ({ ...s, __kind: 'service' }))),
        ...((spec.workers || []).map(s => ({ ...s, __kind: 'worker' }))),
        ...((spec.jobs || []).map(s => ({ ...s, __kind: 'job' }))),
        // In case spec is empty for some reason, include active deployment shapes
        ...((spec.services?.length ? [] : activeServices) || [])
      ];

      return services.map(svc => {
        const svcName = svc.name || svc.spec?.name || 'unknown';
        const svcStatus = activeServices.find(s => (s.name || s.spec?.name) === svcName) || {};
        const progressStatus = componentStatus[svcName];
        const phase = active.phase || app.phase;
        const health = svcStatus.health_check?.status || svcStatus.status || progressStatus;
        // Maintenance/Archive flags
        const maintenanceSpec = spec.maintenance || active.spec?.maintenance || {};
        const isArchived = !!maintenanceSpec.archive;
        const isMxMode = !!maintenanceSpec.enabled; // MX mode (maintenance enabled)
        const isHealthy = (phase === 'ACTIVE') && (!progressStatus || progressStatus === 'SUCCESS') && !isArchived && !isMxMode;
        const image = svc.github?.repo ? `${svc.github.repo}@${svc.github.branch || 'main'}` : (svc.image?.repository || svc.image?.registry_type || svc.source_dir || 'N/A');

        return {
          id: `${app.id}:${svcName}`,
          appId: app.id,
          appName: spec.name || app.id,
          name: `${spec.name || app.id} / ${svcName}`,
          shortName: svcName,
          image,
          state: isArchived ? 'archived' : (isMxMode ? 'mx' : (isHealthy ? 'running' : (phase?.toLowerCase() || 'unknown'))),
          status: isArchived ? 'archived' : (isMxMode ? 'mx' : (health || phase || 'unknown')),
          running: isHealthy,
          archived: isArchived,
          mxMode: isMxMode,
          type: svc.__kind || svc.kind || 'service',
          updatedAt: active.updated_at || app.updated_at || app.last_deployment_active_at,
          url: app.live_url || app.default_ingress,
          instances: svc.instance_count,
          size: svc.instance_size_slug
        };
      });
    });

    res.json(items);
  } catch (e) {
    console.error('Error fetching containers:', e.message);
    if (String(e.message).includes('404')) {
      console.error('Tip: 404 usually means the DO_APP_ID is wrong or the token cannot access that app (team/account mismatch).');
    }
    res.json([]);
  }
});

// DigitalOcean Apps diagnostics endpoints
app.get('/api/do/apps', async (req, res) => {
  try {
    if (!DO_API_TOKEN) return res.status(503).json({ error: 'DO_API_TOKEN not configured' });
    const list = await doAPI('/apps');
    res.json(list.apps || []);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.get('/api/do/validate', async (req, res) => {
  try {
    if (!DO_API_TOKEN || !DO_APP_ID) return res.status(400).json({ error: 'Set DO_API_TOKEN and DO_APP_ID' });
    const data = await doAPI(`/apps/${DO_APP_ID}`);
    res.json({ ok: true, name: data.app?.spec?.name || data.app?.id, id: data.app?.id });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Service details
app.get('/api/containers/:name', async (req, res) => {
  try {
    if (!DO_API_TOKEN) return res.status(503).json({ error: 'Digital Ocean API not configured' });

    const raw = decodeURIComponent(req.params.name || '');
    let targetAppId = DO_APP_ID;
    let targetService = raw;
    if (raw.includes(':')) {
      const parts = raw.split(':');
      targetAppId = parts[0];
      targetService = parts.slice(1).join(':');
    } else if (raw.includes(' / ')) {
      const parts = raw.split(' / ');
      targetAppId = parts[0];
      targetService = parts.slice(1).join(' / ');
    }
    if (!targetAppId) return res.status(400).json({ error: 'App id missing for details' });

    const appData = await doAPI(`/apps/${targetAppId}`);
    const app = appData.app;
    const spec = app.spec || {};
    const active = app.active_deployment || {};
    const activeServices = active.services || [];

    const svc = (spec.services || []).find(s => s.name === targetService)
      || (spec.workers || []).find(s => s.name === targetService)
      || (spec.jobs || []).find(s => s.name === targetService);
    if (!svc) return res.status(404).json({ error: `Service ${targetService} not found in app ${targetAppId}` });

    const svcStatus = activeServices.find(s => (s.name || s.spec?.name) === targetService) || {};
    const progressSteps = active.progress?.steps || [];
    const componentEvents = [];
    const walkSteps = step => {
      if (!step) return;
      if (step.component_name === targetService) {
        componentEvents.push({ name: step.name, status: step.status });
      }
      if (Array.isArray(step.steps)) step.steps.forEach(walkSteps);
    };
    progressSteps.forEach(walkSteps);

    const maintenanceSpec = spec.maintenance || active.spec?.maintenance || {};
    const isArchived = !!maintenanceSpec.archive;
    const isMxMode = !!maintenanceSpec.enabled;
    const phase = active.phase || app.phase;
    const health = svcStatus.health_check?.status || svcStatus.status;
    const running = (phase === 'ACTIVE') && !isArchived && !isMxMode && (!componentEvents.length || componentEvents.every(e => e.status === 'SUCCESS'));
    const state = isArchived ? 'archived' : (isMxMode ? 'mx' : (running ? 'running' : (phase?.toLowerCase() || 'unknown')));

    const ingressRules = Array.isArray(spec.ingress?.rules) ? spec.ingress.rules.filter(r => r?.component?.name === targetService) : [];

    const source = svc.github ? { type: 'github', ...svc.github, dockerfile_path: svc.dockerfile_path }
      : svc.image ? { type: 'image', ...svc.image }
      : { type: 'unknown' };

    const commit = activeServices.find(s => s.name === targetService)?.source_commit_hash;

    res.json({
      app: {
        id: app.id,
        name: spec.name || app.id,
        region: app.region?.slug || app.region,
        tier: app.tier_slug,
        live_url: app.live_url || app.default_ingress,
        live_domain: app.live_domain,
        default_ingress: app.default_ingress
      },
      service: {
        name: targetService,
        type: (spec.services || []).some(s => s.name === targetService) ? 'service' : ( (spec.workers || []).some(s => s.name === targetService) ? 'worker' : 'job'),
        instance_count: svc.instance_count,
        instance_size_slug: svc.instance_size_slug,
        http_port: svc.http_port,
        routes: svc.routes || [],
        envs: svc.envs || [],
        source
      },
      ingress: ingressRules,
      deployment: {
        id: active.id,
        phase,
        created_at: active.created_at,
        updated_at: active.updated_at,
        commit,
        events: componentEvents
      },
      status: {
        state,
        running,
        mxMode: isMxMode,
        archived: isArchived,
        health: health || 'unknown'
      }
    });
  } catch (e) {
    const msg = String(e.message || '');
    const match = msg.match(/DO API Error:\s(\d+)/);
    if (match) return res.status(parseInt(match[1], 10)).json({ error: msg });
    res.status(500).json({ error: msg || 'Unknown error' });
  }
});

// Update a service (instances, size, http_port, envs, routes)
app.put('/api/containers/:name', async (req, res) => {
  try {
    if (!DO_API_TOKEN) return res.status(503).json({ error: 'Digital Ocean API not configured' });

    const raw = decodeURIComponent(req.params.name || '');
    let targetAppId = DO_APP_ID;
    let targetService = raw;
    if (raw.includes(':')) {
      const parts = raw.split(':');
      targetAppId = parts[0];
      targetService = parts.slice(1).join(':');
    } else if (raw.includes(' / ')) {
      const parts = raw.split(' / ');
      targetAppId = parts[0];
      targetService = parts.slice(1).join(' / ');
    }
    if (!targetAppId) return res.status(400).json({ error: 'App id missing for update' });

    const appData = await doAPI(`/apps/${targetAppId}`);
    const spec = appData.app.spec || {};

    // Block edits on archived apps
    const maintenanceSpec = spec.maintenance || appData.app.active_deployment?.spec?.maintenance || {};
    if (maintenanceSpec.archive) {
      return res.status(409).json({ error: 'App is archived; edits are not permitted.' });
    }

    const findRef = () => {
      const i = (spec.services || []).findIndex(s => s.name === targetService);
      if (i >= 0) return { group: 'services', idx: i };
      const w = (spec.workers || []).findIndex(s => s.name === targetService);
      if (w >= 0) return { group: 'workers', idx: w };
      const j = (spec.jobs || []).findIndex(s => s.name === targetService);
      if (j >= 0) return { group: 'jobs', idx: j };
      return null;
    };
    const ref = findRef();
    if (!ref) return res.status(404).json({ error: `Service ${targetService} not found` });

    const svc = spec[ref.group][ref.idx];
    const { instance_count, instance_size_slug, http_port, routes, envs } = req.body || {};

    if (typeof instance_count === 'number' && instance_count >= 0) svc.instance_count = instance_count;
    if (typeof instance_size_slug === 'string' && instance_size_slug) svc.instance_size_slug = instance_size_slug;
    if (http_port === null || http_port === undefined) {
      // leave as-is
    } else if (typeof http_port === 'number') {
      svc.http_port = http_port;
    }
    if (Array.isArray(routes)) {
      svc.routes = routes;
    }
    if (Array.isArray(envs)) {
      svc.envs = envs.map(ev => ({ key: ev.key, value: ev.value, scope: ev.scope || 'RUN_TIME', type: ev.type }));
    }

    await doAPI(`/apps/${targetAppId}`, { method: 'PUT', body: JSON.stringify({ spec }) });
    res.json({ ok: true, service: svc });
  } catch (e) {
    const msg = String(e.message || '');
    const match = msg.match(/DO API Error:\s(\d+)/);
    if (match) return res.status(parseInt(match[1], 10)).json({ error: msg });
    res.status(500).json({ error: msg || 'Unknown error' });
  }
});

// App Platform service control - restart triggers a new deployment
app.post('/api/containers/:name/restart', async (req, res) => {
  try {
    if (!DO_API_TOKEN) {
      return res.status(503).json({ error: 'Digital Ocean API not configured' });
    }

    const raw = decodeURIComponent(req.params.name || '');
    const [targetAppId] = raw.includes(':') ? raw.split(':') : raw.includes(' / ') ? raw.split(' / ') : [DO_APP_ID];
    if (!targetAppId) {
      return res.status(400).json({ error: 'App id missing for restart' });
    }

    // Create a new deployment to restart the service
    await doAPI(`/apps/${targetAppId}/deployments`, {
      method: 'POST',
      body: JSON.stringify({ force_build: false })
    });
    
    res.json({ ok: true, message: 'Deployment triggered' });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// App Platform doesn't support stop/start - only restart via redeployment
app.post('/api/containers/:name/stop', async (req, res) => {
  res.status(501).json({ error: 'Stop not supported on App Platform. Use scale to 0 instances instead.' });
});

app.post('/api/containers/:name/start', async (req, res) => {
  res.status(501).json({ error: 'Start not supported on App Platform. Use scale > 0 instances instead.' });
});

// Delete a service from the app
app.delete('/api/containers/:name', async (req, res) => {
  try {
    if (!DO_API_TOKEN) {
      return res.status(503).json({ error: 'Digital Ocean API not configured' });
    }
    const raw = decodeURIComponent(req.params.name || '');
    let targetAppId = DO_APP_ID;
    let targetService = raw;

    if (raw.includes(':')) {
      const parts = raw.split(':');
      targetAppId = parts[0];
      targetService = parts.slice(1).join(':');
    } else if (raw.includes(' / ')) {
      const parts = raw.split(' / ');
      targetAppId = parts[0];
      targetService = parts.slice(1).join(' / ');
    }

    if (!targetAppId) {
      return res.status(400).json({ error: 'App id missing for delete' });
    }
    
    // Get current app spec
    const appData = await doAPI(`/apps/${targetAppId}`);
    const spec = appData.app.spec || {};

    // Prevent modifications when archived
    const maintenanceSpec = spec.maintenance || appData.app.active_deployment?.spec?.maintenance || {};
    if (maintenanceSpec.archive) {
      return res.status(409).json({ error: 'App is archived; delete is not permitted.' });
    }
    
    // Remove the service from the spec
    const originalLength = (spec.services || []).length + (spec.workers || []).length + (spec.jobs || []).length;
    spec.services = (spec.services || []).filter(s => s.name !== targetService);
    spec.workers = (spec.workers || []).filter(w => w.name !== targetService);
    spec.jobs = (spec.jobs || []).filter(j => j.name !== targetService);

    // Remove any ingress rules that reference the deleted component
    if (spec.ingress && Array.isArray(spec.ingress.rules)) {
      spec.ingress.rules = spec.ingress.rules.filter(rule => {
        const compName = rule?.component?.name;
        return compName !== targetService;
      });
    }
    
    const newLength = spec.services.length + spec.workers.length + spec.jobs.length;
    
    if (originalLength === newLength) {
      return res.status(404).json({ error: `Service ${targetService} not found` });
    }
    
    // Update the app with new spec
    await doAPI(`/apps/${targetAppId}`, {
      method: 'PUT',
      body: JSON.stringify({ spec })
    });
    
    res.json({ ok: true, message: `Service ${targetService} deleted` });
  } catch (e) {
    // Map DO API error codes to HTTP status when possible
    const msg = String(e.message || '');
    const match = msg.match(/DO API Error:\s(\d+)/);
    if (match) {
      return res.status(parseInt(match[1], 10)).json({ error: msg });
    }
    res.status(500).json({ error: msg || 'Unknown error' });
  }
});

// Create a new service in the app
app.post('/api/containers', async (req, res) => {
  try {
    if (!DO_API_TOKEN || !DO_APP_ID) {
      return res.status(503).json({ error: 'Digital Ocean API not configured' });
    }

    const { name, source_type, image_registry, image_repository, image_tag, github_repo, github_branch, dockerfile_path, instance_size, instance_count, http_port, env_vars } = req.body;
    
    if (!name) {
      return res.status(400).json({ error: 'Service name is required' });
    }
    
    // Get current app spec
    const appData = await doAPI(`/apps/${DO_APP_ID}`);
    const spec = appData.app.spec;
    
    // Build the new service spec
    const newService = {
      name: name,
      instance_count: instance_count || 1,
      instance_size_slug: instance_size || 'basic-xxs'
    };
    
    // Configure source
    if (source_type === 'image' && image_repository) {
      newService.image = {
        registry_type: image_registry || 'DOCKER_HUB',
        repository: image_repository,
        tag: image_tag || 'latest'
      };
    } else if (source_type === 'github' && github_repo) {
      newService.github = {
        repo: github_repo,
        branch: github_branch || 'main',
        deploy_on_push: true
      };
      if (dockerfile_path) {
        newService.dockerfile_path = dockerfile_path;
      }
    } else {
      return res.status(400).json({ error: 'Invalid source configuration' });
    }
    
    // Add HTTP port if specified
    if (http_port) {
      newService.http_port = parseInt(http_port);
      newService.routes = [{
        path: `/${name}`
      }];
    }
    
    // Add environment variables
    if (env_vars && Array.isArray(env_vars)) {
      newService.envs = env_vars.map(ev => ({
        key: ev.key,
        value: ev.value,
        scope: 'RUN_TIME'
      }));
    }
    
    // Add to services array
    if (!spec.services) {
      spec.services = [];
    }
    spec.services.push(newService);
    
    // Update the app with new spec
    await doAPI(`/apps/${DO_APP_ID}`, {
      method: 'PUT',
      body: JSON.stringify({ spec })
    });
    
    res.json({ ok: true, message: `Service ${name} created`, service: newService });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Optionally, expose current config for feedback app client-side
app.get('/api/public-config/:service', async (req, res) => {
  try {
    const cfg = readConfig();
    const svc = cfg[req.params.service];
    if (!svc) return res.status(404).json({ error: 'Unknown service' });
    res.json(svc);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

// Static UI
app.use('/', express.static('/usr/share/admin-portal'));

const port = process.env.PORT || 4000;
app.listen(port, () => {
  console.log(`Admin portal listening on :${port}`);
});
