const { handleConnect } = require('../handlers/connect');
const { handleOffer } = require('../handlers/offer');
const { handleAnswer } = require('../handlers/answer');
const { handleIce } = require('../handlers/ice');
const { handleControllerLock } = require('../handlers/controllerlock');
const { handleDisconnect } = require('../handlers/disconnect');
const { handleControllerNotify } = require('../handlers/controllernotify');

module.exports = (wss) => {
  wss.on('connection', (ws) => {
    const sessionId = Math.random().toString(36).substring(2, 15);
    // console.log(`Client connected with temporary ID: ${sessionId}`);

    ws.send(`STATE Impossible`);

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