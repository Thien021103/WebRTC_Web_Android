const WebSocket = require('ws');
const { groups, notifyStateUpdate } = require('../groups/groups');
const { wsUserAuth, wsCameraAuth } = require('../middleware/auth');

async function handleIce(message, client) {
  
  const msg = message.toString().trim();
  const match = msg.match(/^ICE\s+(\w+)\s+(\S+)([\s\S]*)$/);
  if (!match) {
    console.error('Invalid ICE message format');
    client.close();
    return;
  }

  const [_, senderType, token, iceData] = match;
  
  console.log(`Handling ICE from ${senderType}`);

  // Validate sender type
  if (senderType !== 'camera' && senderType !== 'user') {
    console.error('Only camera or user can send ICE');
    client.close();
    return;
  }

  // Validate token
  let groupId;
  if (senderType === 'camera') {
    if (!(await wsCameraAuth(token, client))) {
      console.error('Invalid camera token');
      client.close();
      return;
    }
    groupId = client._camera.groupId;
  } else {
    if (!(await wsUserAuth(token, client))) {
      console.error('Invalid user token');
      client.close();
      return;
    }
    groupId = client._user.groupId;
  }
  
  const group = groups.get(groupId);

  if (!group) {
    console.error(`Group ${groupId} not found`);
    client.close();
    return;
  }

  // Forward ICE
  const forwardMessage = `ICE\n${iceData.trim()}`;
  
  let recipient;
  if (senderType === 'camera') {
    recipient = group.clients.user;
  } else {
    recipient = group.clients.camera;
  }

  if (!recipient) {
    console.error(`No ${senderType === 'camera' ? 'user' : 'camera'} in group ${groupId}`);
    client.close();
    return;
  }

  if (recipient.readyState !== WebSocket.OPEN) {
    console.error(`No ${senderType === 'camera' ? 'User' : 'Camera'} in group ${groupId}`);
    client.close();
    return;
  }
  recipient.send(forwardMessage);
  console.log(`Forwarded ICE from ${senderType} in group ${groupId}`);
}

module.exports = { handleIce };