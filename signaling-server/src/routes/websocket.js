const WebSocket = require('ws');
const { v4: uuidv4 } = require('uuid');

const { handleConnect } = require('../handlers/connect');
const { handleOffer } = require('../handlers/offer');
const { handleAnswer } = require('../handlers/answer');
const { handleIce } = require('../handlers/ice');
const { handleControllerLock } = require('../handlers/controllerlock');
const { handleDisconnect } = require('../handlers/disconnect');
const { handleControllerNotify } = require('../handlers/controllernotify');

const { groups } = require('../groups/groups')

// Heartbeat mechanism
function startHeartbeat(wss) {
  setInterval(() => {
    console.log('Heartbeat: Sending PING to wss.clients');
    const now = Date.now();
    wss.clients.forEach(ws => {

      // Check state
      if (ws.readyState === WebSocket.CLOSED || ws.readyState === WebSocket.CLOSING) {
        console.log(`Client ${ws.id} is closed, disconnecting`);
        ws.close();
        return;
      }

      // 30s timeout
      if (!ws._isAlive && ws._lastPong && now - ws._lastPong > 30000) { 
        console.log(`Client ${ws.id} is unresponsive`);
        ws.close();
        return; 
      }

      // Mark as not responding
      ws._isAlive = false;

      try {
        ws.send('PING');
      } catch (error) {
        console.error(`Client ${ws.id} PING error: ${error.message}`);
        ws._isAlive = false;
        ws.close();
        return;
      }
    });
  }, 10000); // Check every 10 seconds

  setInterval(() => {
    console.log('Cleaning up stale clients in groups');
    groups.forEach((group, groupId) => {
      ['camera', 'user', 'controller'].forEach(type => {
        const client = group.clients[type];
        if (client && (!client._isAlive || client.readyState !== WebSocket.OPEN)) {
          client.close();
        }
      });
    });
  }, 300000); // Check every 300 seconds
}

function websocketHandler(wss) {
  wss.on('connection', (ws) => {
    const sessionId = uuidv4();
    // console.log(`Client connected with temporary ID: ${sessionId}`);

    ws.send(`STATE Impossible`);
    
    // Initialize state, last pong
    ws._isAlive = true;
    ws._lastPong = Date.now(); 

    ws.on('message', (data) => {
      const message = data.toString().trim();
      console.log(`Message received: ${message}`);

      if (message.startsWith('CONNECT')) {
        handleConnect(message, ws);
      } else if (message.startsWith('OFFER')) {
        handleOffer(message, ws);
      } else if (message.startsWith('ANSWER')) {
        handleAnswer(message, ws);
      } else if (message.startsWith('ICE')) {
        handleIce(message, ws);
      } else if (message.startsWith('LOCK')) {
        handleControllerLock(message, ws);
      } else if (message.startsWith('NOTIFY')) {
        handleControllerNotify(message, ws);
      } else if (message.startsWith('PONG')) {
        ws._isAlive = true;
        ws._lastPong = Date.now(); // Update on PONG
      }
    });

    ws.on('close', () => {
      console.log(`Client ${sessionId} disconnected`);
      handleDisconnect(ws);
    });

    ws.on('error', (error) => {
      console.error(`WebSocket error: ${error.message}`);
      handleDisconnect(ws);
    });
  });
};

module.exports = { websocketHandler, startHeartbeat }