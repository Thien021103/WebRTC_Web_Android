const { groups, notifyStateUpdate } = require("../groups/groups");
const WebSocket = require('ws');

function handleOffer(message) {

  // Use regex to extract senderType, id, ...
  /*
  OFFER camera 123
  v=0
  ...
  */
  const match = message.match(/^OFFER (\w+) (\w+)([\s\S]*)$/);
  if (!match) {
    console.error('Invalid OFFER message format');
    return;
  }

  const [_, senderType, id, sdpData] = match; // match[0] là toàn bộ chuỗi, bỏ qua

  console.log(`Handling offer from ${senderType}, group ${id}`);

  console.log(groups)

  const group = groups.get(id);

  console.log(group)

  if (!group || group.state !== 'Ready') {
    console.error('Session must be Ready to handle offer');
    return;
  }

  if (senderType !== 'camera') {
    console.error('Only camera can send OFFER');
    return;
  }

  group.state = 'Creating';
  notifyStateUpdate(group.id);

  // Forward OFFER
  const forwardMessage = `OFFER${sdpData}`;
  console.log(forwardMessage)

  if (group.clients.user && group.clients.user.readyState === WebSocket.OPEN) {
    group.clients.user.send(forwardMessage);
  } else {
    console.error(`No user in group ${group.id}`);
  }
}

module.exports = { handleOffer };