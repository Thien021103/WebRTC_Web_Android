const http = require('http');
const WebSocket = require('ws');
// const admin = require("firebase-admin");

/** FCM setup **/
// const serviceAccount = require('../firebase-admin-sdk.json');
const { handleConnect } = require('./handlers/connect');
const { handleOffer } = require('./handlers/offer');
const { handleAnswer } = require('./handlers/answer');
const { handleIce } = require('./handlers/ice');
const { connect } = require('./db/db');
const { handleLogin } = require('./handlers/login');
const { handleRegister } = require('./handlers/register');
// const registrationToken = 'fL9GEwT9QoKekNVV6j3BZY:APA91bGVMIObkbbROcivQ8iDtO-eDEfsL9GtRd8nKnsalNJejYG6OzmlJcZm_QoMGYOBU4oKlsQBAoDdhhnI_HlNp8LgVkwuOEywPPa-qDDEeBmZnJrHig4'; // Thay bằng FCM token từ logcat (tạm thời hardcode)

/************************  
Groups are stored locally as JSON, presented as: 
{ id,
  state,
  clients: {
    camera: ws,
    user: ws,
    controller: ws
  },
  fcm_token,
}
************************/

const groups = new Map();

// Utility để thông báo trạng thái đến tất cả client trong cùng nhóm
function notifyStateUpdate(groupId) {
  const group = groups.get(groupId);
  if (group) {
    const message = `STATE ${group.state}`;
    for (const client of Object.values(group.clients)) {
      if (client.readyState === WebSocket.OPEN) {
        client.send(message);
      }
    }
  }
}

async function startServer() {

  // Connect to MongoDB
  await connect();

  // Create HTTP server
  const server = http.createServer((req, res) => {
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('Not a normal http server, this is for Websocket only');
  });

  const wss = new WebSocket.Server({ server });

  wss.on('connection', (ws) => {
    let sessionId = Math.random().toString(36).substring(2, 15); // ID tạm thời cho client
    console.log(`Client connected with temporary ID: ${sessionId}`);

    // Gửi trạng thái ban đầu (Impossible nếu chưa thuộc nhóm)
    ws.send(`STATE Impossible`);

    ws.on('message', (data) => {
      const message = data.toString();
      console.log(`Message received: ${message}`);

      if (message.startsWith('CONNECT')) {
        handleConnect(message, ws);
      } else if (message.startsWith('OFFER')) {
        handleOffer(message);
      } else if (message.startsWith('ANSWER')) {
        handleAnswer(message);
      } else if (message.startsWith('ICE')) {
        handleIce(message);
      } else if (message.startsWith('REGISTER')) {
        handleRegister(message, ws);
      } else if (message.startsWith('REGISTER')) {
        handleLogin(message, ws);
      }
    });

    ws.on('close', () => {
      handleDisconnect(ws);
      console.log(`Client ${sessionId} disconnected`);
    });

    ws.on('error', console.error);
  });

  // Start server
  const PORT = process.env.PORT || 8000;

  server.listen(PORT, () => {
    console.log(`Server listening on port ${PORT}`);
  });
}

// Handle disconnect
function handleDisconnect(client) {
  if (client && client._groupId) {
    const group = groups.get(client._groupId);
    if (group) {
      delete group.clients[client._type]; // Delete from group
      if (!group.clients.camera || !group.clients.user || !group.clients.controller) {
        group.state = 'Impossible';
        notifyStateUpdate(client._groupId);
      }
      if (Object.keys(group.clients).length === 0) {
        groups.delete(client._groupId); // Delete group
      }
    }
  }
}

// Start the server
startServer().catch((error) => {
  console.error('Failed to start server:', error.message);
  process.exit(1);
});

module.exports = 
{ 
  groups,
  notifyStateUpdate
};