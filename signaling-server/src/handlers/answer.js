const { groups, notifyStateUpdate } = require("../groups/groups");
const WebSocket = require('ws');

function handleAnswer(message) {

  // Use regex to extract senderType, id, ...
  /*
  ANSWER user 123
  v=0
  ...
  */
  const match = message.match(/^ANSWER (\w+) (\w+)([\s\S]*)$/);
  if (!match) {
    console.error('Invalid ANSWER message format');
    return;
  }

  const [_, senderType, id, sdpData] = match; // match[0] là toàn chuỗi, bỏ qua

  const group = groups.get(id);

  console.log(`Handling offer from ${senderType}, group ${id}`);

  if (!group || group.state !== 'Creating') {
    console.error('Session must be in Creating state to handle answer');
    return;
  }

  if (senderType !== 'user') {
    console.error('Only user can send ANSWER');
    return;
  }

  group.state = 'Active';
  notifyStateUpdate(id);

  // Forward ANSWER
  const forwardMessage = `ANSWER${sdpData}`;
  if (group.clients.camera && group.clients.camera.readyState === WebSocket.OPEN) {
    group.clients.camera.send(forwardMessage);
  } else {
    console.error(`No camera in group ${group.id}`);
  }
}

module.exports = { handleAnswer };