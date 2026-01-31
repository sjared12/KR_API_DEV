#!/usr/bin/env node
/**
 * Seed or update GitHub issues from .github/backlog.json
 * - Skips creating a new issue if one with the same title already exists
 * - Updates labels and body for existing issues (optional minimal update)
 *
 * Requires: GITHUB_TOKEN, GITHUB_REPOSITORY in env (Actions provides these)
 */

const fs = require('fs');

async function run() {
  const token = process.env.GITHUB_TOKEN;
  const repoFull = process.env.GITHUB_REPOSITORY; // e.g. owner/repo
  if (!token || !repoFull) {
    console.error('Missing GITHUB_TOKEN or GITHUB_REPOSITORY.');
    process.exit(1);
  }
  const [owner, repo] = repoFull.split('/');

  const backlogPath = '.github/backlog.json';
  if (!fs.existsSync(backlogPath)) {
    console.error(`Backlog file not found: ${backlogPath}`);
    process.exit(1);
  }

  const raw = fs.readFileSync(backlogPath, 'utf8');
  /** @type {{title: string, body: string, labels?: string[]}[]} */
  const issues = JSON.parse(raw);

  for (const item of issues) {
    const title = item.title.trim();
    const body = item.body || '';
    const labels = Array.isArray(item.labels) ? item.labels : [];

    // Ensure labels exist (best-effort)
    for (const label of labels) {
      await ensureLabel(owner, repo, label, token);
    }

    const existing = await findIssueByTitle(owner, repo, title, token);
    if (existing) {
      // Update labels/body if changed (avoid noisy updates)
      const needBodyUpdate = (existing.body || '') !== body;
      const needLabelUpdate = !arrayEqualIgnoreOrder(labels, (existing.labels || []).map(l => l.name));

      if (needBodyUpdate || needLabelUpdate) {
        await updateIssue(owner, repo, existing.number, {
          body: needBodyUpdate ? body : undefined,
          labels: needLabelUpdate ? labels : undefined,
        }, token);
        console.log(`Updated issue #${existing.number}: ${title}`);
      } else {
        console.log(`No changes for existing issue #${existing.number}: ${title}`);
      }
    } else {
      const created = await createIssue(owner, repo, { title, body, labels }, token);
      console.log(`Created issue #${created.number}: ${title}`);
    }
  }
}

function arrayEqualIgnoreOrder(a, b) {
  if (a.length !== b.length) return false;
  const setA = new Set(a);
  for (const x of b) if (!setA.has(x)) return false;
  return true;
}

async function ghFetch(url, opts = {}) {
  const res = await fetch(url, {
    ...opts,
    headers: {
      'Accept': 'application/vnd.github+json',
      'Authorization': `Bearer ${process.env.GITHUB_TOKEN}`,
      'X-GitHub-Api-Version': '2022-11-28',
      ...(opts.headers || {}),
    },
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`GitHub API error ${res.status} ${res.statusText}: ${text}`);
  }
  return res;
}

async function ensureLabel(owner, repo, name, token) {
  try {
    const res = await ghFetch(`https://api.github.com/repos/${owner}/${repo}/labels/${encodeURIComponent(name)}`);
    if (res.ok) return; // exists
  } catch (_) {
    // ignore
  }
  // Create label with a default color if missing
  const color = '0e8a16'; // greenish
  try {
    await ghFetch(`https://api.github.com/repos/${owner}/${repo}/labels`, {
      method: 'POST',
      body: JSON.stringify({ name, color })
    });
    console.log(`Created label: ${name}`);
  } catch (e) {
    // Ignore if we lack perms; issue creation will still work without labels
    console.warn(`Could not create label '${name}': ${e.message}`);
  }
}

async function findIssueByTitle(owner, repo, title, token) {
  const q = encodeURIComponent(`repo:${owner}/${repo} in:title "${title}"`);
  const res = await ghFetch(`https://api.github.com/search/issues?q=${q}&per_page=5`);
  const data = await res.json();
  const match = (data.items || []).find((it) => it.title === title && it.pull_request == null);
  return match || null;
}

async function createIssue(owner, repo, issue, token) {
  const res = await ghFetch(`https://api.github.com/repos/${owner}/${repo}/issues`, {
    method: 'POST',
    body: JSON.stringify(issue),
  });
  return await res.json();
}

async function updateIssue(owner, repo, number, patch, token) {
  const res = await ghFetch(`https://api.github.com/repos/${owner}/${repo}/issues/${number}`, {
    method: 'PATCH',
    body: JSON.stringify(patch),
  });
  return await res.json();
}

run().catch((e) => {
  console.error(e);
  process.exit(1);
});
