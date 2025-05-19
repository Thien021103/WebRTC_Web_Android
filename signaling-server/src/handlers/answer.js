const WebSocket = require('ws');
const { groups, notifyStateUpdate } = require('../groups/groups');
const { wsUserAuth } = require('../middleware/auth');

async function handleAnswer(message, client) {

  // Use regex to extract senderType, id, ...
  /*
  ANSWER user 123
  v=0
  ...
  */
  const msg = message.toString().trim();
  const match = msg.match(/^ANSWER\s+(\w+)\s+(\S+)([\s\S]*)$/);
  if (!match) {
    console.error('Invalid ANSWER message format');
    client.close();
    return;
  }

  const [_, senderType, token, sdpData] = match;

  console.log(`Handling answer from ${senderType}`);

  // Validate sender is user
  if (senderType !== 'user') {
    console.error('Only user can send ANSWER');
    client.close();
    return;
  }

  // Validate user/owner token
  if (!(await wsUserAuth(token, client))) {
    console.error('Invalid user token');
    client.close();
    return;
  }

  const groupId = client._decoded.groupId;
  const group = groups.get(groupId);

  if (!group || group.state !== 'Creating') {
    console.error(`Group ${groupId} not in Creating state or missing`);
    client.close();
    return;
  }

  group.state = 'Active';
  notifyStateUpdate(group.id);

  // Forward ANSWER
  const forwardMessage = `ANSWER${sdpData}`;

  if (!group.clients.camera) {
    console.error(`No camera in group ${groupId}`);
    client.close();
    return;
  }

  if (group.clients.camera.readyState !== WebSocket.OPEN) {
    console.error(`Camera in group ${groupId} is not connected`);
    client.close();
    return;
  }

  group.clients.camera.send(forwardMessage);
  console.log(`Forwarded ANSWER to camera in group ${groupId}`);
}

module.exports = { handleAnswer };