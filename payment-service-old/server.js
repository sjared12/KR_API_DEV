import express from 'express';
import cors from 'cors';

const app = express();
app.use(express.json());
app.use(cors());

// Dummy users data
const users = [
  { id: 1, name: 'Alice', email: 'alice@example.com' },
  { id: 2, name: 'Bob', email: 'bob@example.com' }
];

// Admin users API
app.get('/api/admin/users', (req, res) => {
  res.json(users);
});

// Health check
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok' });
});

const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
  console.log(`Payment service listening on port ${PORT}`);
});
