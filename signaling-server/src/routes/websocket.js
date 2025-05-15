const { handleConnect, handleDisconnect } = require('../handlers/connect');
const { handleOffer } = require('./handlers/offer');
const { handleAnswer } = require('./handlers/answer');
const { handleIce } = require('./handlers/ice');
const { handleConntrollerLock } = require('./handlers/lock');

module.exports = (wss) => {
  wss.on('connection', (ws) => {
    const sessionId = Math.random().toString(36).substring(2, 15);
    console.log(`Client connected with temporary ID: ${sessionId}`);

    ws.send(`STATE Impossible`);

    ws.on('message', (data) => {
      const message = data.toString().trim();
      console.log(`Message received: ${message}`);

      if (message.startsWith('CONNECT')) {
        handleConnect(message, ws);
      } else if (message.startsWith('OFFER')) {
        handleOffer(message);
      } else if (message.startsWith('ANSWER')) {
        handleAnswer(message);
      } else if (message.startsWith('ICE')) {
        handleIce(message);
      } else if (message.startsWith('LOCK')) {
        handleConntrollerLock(message);
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