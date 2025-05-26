const http = require('http');
const express = require('express');
const cors = require('cors');
const WebSocket = require('ws');

require('dotenv').config(); // Load .env

const { connect } = require('./db/db');
const apiRoutes = require('./routes/api');
const { websocketHandler, startHeartbeat } = require('./routes/websocket');
const { requestLogger, errorHandler } = require('./middleware/middleware');

async function startServer() {
  console.log('Starting server...');

  try {
    await connect();
    console.log('MongoDB connection established');
  } catch (error) {
    console.error('Failed to connect to MongoDB:', error.message);
    process.exit(1);
  }

  // Create Express app
  const app = express();

  // Middleware
  app.use(express.json());
  app.use(cors());
  app.use(requestLogger);

  // Routes
  app.use('/api', apiRoutes);

  // Fallback
  app.use((req, res) => {
    res.status(404).json({ error: 'Not a normal HTTP server, this is for WebSocket and API only' });
  });

  // Error handling
  app.use(errorHandler);

  const server = http.createServer(app);

  // Create Websocket server
  const wss = new WebSocket.Server({ server });
  websocketHandler(wss);
  startHeartbeat(wss);

  const PORT = process.env.PORT || 8000;

  server.listen(PORT, () => {
    console.log(`Server listening on port ${PORT}`);
  });
}

// Start the server
startServer().catch((error) => {
  console.error('Failed to start server:', error.message);
  process.exit(1);
});