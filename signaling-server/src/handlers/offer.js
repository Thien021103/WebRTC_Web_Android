const { groups, notifyStateUpdate } = require("../groups/groups");
const WebSocket = require('ws');
const { wsCameraAuth } = require("../middleware/auth");

async function handleOffer(message, client) {

  // Use regex to extract senderType, id, ...
  /*
  OFFER camera 123
  v=0
  ...
  */
  const msg = message.toString().trim();
  const match = msg.match(/^OFFER\s+(\w+)\s+(\S+)([\s\S]*)$/);
  if (!match) {
    console.error('Invalid OFFER message format');
    client.close();
    return;
  }
  const [_, senderType, token, sdpData] = match;

  console.log(`Handling offer from ${senderType}`);

  // Validate sender is camera
  if (senderType !== 'camera') {
    console.error('Only camera can send OFFER');
    client.close();
    return;
  }

  // Validate camera token
  console.log(`Validating camera token: ${token}`);
  if (!(await wsCameraAuth(token, client))) {
    console.error('Invalid camera token');
    client.close();
    return;
  }

  const groupId = client._tmpGroupId;
  const group = groups.get(groupId);

  if (!group || group.state !== 'Ready') {
    console.error(`Group ${groupId} not Ready or missing`);
    client.close();
    return;
  }

  group.state = 'Creating';
  notifyStateUpdate(group.id);

  // Forward OFFER
  const forwardMessage = `OFFER${sdpData}`;

  if (!group.clients.user) {
    console.error(`No user in group ${groupId}`);
    client.close();
    return;
  }

  if (group.clients.user.readyState !== WebSocket.OPEN) {
    console.error(`User in group ${groupId} is not connected`);
    client.close();
    return;
  }
  
  group.clients.user.send(forwardMessage);
  console.log(`Forwarded OFFER to user in group ${groupId}`);
}

module.exports = { handleOffer };