const http = require('http');
const WebSocket = require('ws');

const clients = {};
let sessionState = 'Impossible';  // Default state (similar to Impossible in Kotlin)

// Utility to notify state updates to all connected clients
function notifyStateUpdate() {
  const message = `STATE ${sessionState}`;
  for (const client of Object.values(clients)) {
    client.send(message);
  }
}

// Handling connection and message routing
const server = http.createServer((req, res) => {
  res.writeHead(404, { 'Content-Type': 'text/plain' });
  res.end('Not Found');
});

const wss = new WebSocket.Server({ server });

wss.on('connection', (ws) => {
  let sessionId = Math.random().toString(36).substring(2, 15); // Generate a random session ID
  console.log(`Client connected with ID: ${sessionId}`);
  clients[sessionId] = ws;

  // Send initial STATE to the connected client
  ws.send(`STATE ${sessionState}`);

  ws.on('message', (data) => {
    const message = data.toString();  // Access the utf8Data property
    console.log(`Message received: ${message}`);

    if (message.startsWith('STATE')) {
      handleState(sessionId);
    } else if (message.startsWith('OFFER')) {
      handleOffer(sessionId, message);
    } else if (message.startsWith('ANSWER')) {
      handleAnswer(sessionId, message);
    } else if (message.startsWith('ICE')) {
      handleIce(sessionId, message);
    }
  });

  ws.on('close', () => {
    delete clients[sessionId];
    if (Object.keys(clients).length < 2) {
      sessionState = 'Impossible';  // Reset to Impossible if less than 2 clients
      notifyStateUpdate();
    }
    console.log(`Client ${sessionId} disconnected`);
  });

  // Update session state when both clients are ready
  if (Object.keys(clients).length === 2) {
    sessionState = 'Ready';
    notifyStateUpdate();
  }
});

// Handle state-related message
function handleState(sessionId) {
  const client = clients[sessionId];
  if (client) {
    client.send(`STATE ${sessionState}`);
  }
}

// Handle OFFER message
function handleOffer(sessionId, message) {
  if (sessionState !== 'Ready') {
    console.error('Session should be in Ready state to handle offer');
    return;
  }

  sessionState = 'Creating';
  console.log(`Handling offer from ${sessionId}`);
  notifyStateUpdate();

  // Forward the offer to the other client
  for (const [otherSessionId, otherClient] of Object.entries(clients)) {
    if (otherSessionId !== sessionId) {
      otherClient.send(message);
    }
  }
}

// Handle ANSWER message
function handleAnswer(sessionId, message) {
  if (sessionState !== 'Creating') {
    console.error('Session should be in Creating state to handle answer');
    return;
  }

  console.log(`Handling answer from ${sessionId}`);
  sessionState = 'Active';
  notifyStateUpdate();

  // Forward the answer to the other client
  for (const [otherSessionId, otherClient] of Object.entries(clients)) {
    if (otherSessionId !== sessionId) {
      otherClient.send(message);
    }
  }
}

// Handle ICE message
function handleIce(sessionId, message) {
  console.log(`Handling ICE from ${sessionId}`);

  // Forward the ICE message to the other client
  for (const [otherSessionId, otherClient] of Object.entries(clients)) {
    if (otherSessionId !== sessionId) {
      otherClient.send(message);
    }
  }
}

// Start the HTTP server
const port = process.env.PORT || 8000;
server.listen(port, () => {
  console.log(`Server listening on port ${port}`);
});
