const http = require('http');
const WebSocket = require('ws');
// const admin = require("firebase-admin");

/** FCM setup **/
// const serviceAccount = require('../firebase-admin-sdk.json');
const { handleConnect } = require('./handlers/connect');
const { handleOffer } = require('./handlers/offer');
const { handleAnswer } = require('./handlers/answer');
const { handleIce } = require('./handlers/ice');
const { connect, getDb } = require('./db/db');
const { handleLogin } = require('./handlers/login');
const { handleRegister } = require('./handlers/register');
const { groups, notifyStateUpdate } = require('./groups/groups');
// const registrationToken = 'fL9GEwT9QoKekNVV6j3BZY:APA91bGVMIObkbbROcivQ8iDtO-eDEfsL9GtRd8nKnsalNJejYG6OzmlJcZm_QoMGYOBU4oKlsQBAoDdhhnI_HlNp8LgVkwuOEywPPa-qDDEeBmZnJrHig4'; // Thay bằng FCM token từ logcat (tạm thời hardcode)

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
      } else if (message.startsWith('LOGIN')) {
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
async function handleDisconnect(client) {
  if (client && client._groupId) {

    const group = groups.get(client._groupId);

    if (group) {
      // Deleted accessToken if user disconnect
      if (client._type === 'user' && client._accessToken) {
        try {
          const db = getDb();
          await db.collection('users').updateOne(
            { accessToken: client._accessToken },
            { $unset: { accessToken: '' } }
          );
          console.log(`Deleted accessToken from users collection`);
        } catch (error) {
          console.error(`Error deleting accessToken`);
        }
      }

      // Delete from group
      group.clients[client._type] = null;
      
      // Update group state in collection and local
      if (!group.clients.camera || !group.clients.user || !group.clients.controller) {
        group.state = 'Impossible';
        try {
          const db = getDb();
          await db.collection('groups').updateOne(
            { id: client._groupId },
            { $set: { state: 'Impossible' } }
          );
          console.log(`Updated group ${client._groupId} state to Impossible`);
        } catch (error) {
          console.error(`Error updating group state: ${error.message}`);
        }
        notifyStateUpdate(client._groupId);
      }

      if (!group.clients.camera && !group.clients.user && !group.clients.controller) {
        // Delete group in collection and local
        groups.delete(client._groupId);
        try {
          const db = getDb();
          await db.collection('groups').deleteOne(
            { id: client._groupId }
          );
          console.log(`Deleted group ${client._groupId} from groups collection`);
        } catch (error) {
          console.error(`Error deleting group: ${error.message}`);
        }
      }
    }
  }
}

// Start the server
startServer().catch((error) => {
  console.error('Failed to start server:', error.message);
  process.exit(1);
});