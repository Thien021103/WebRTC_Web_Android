const { groups } = require('../signaling-server');

function handleIce(message) {
  
  // Sử dụng regex để trích xuất senderType, id, và phần còn lại
  const match = message.match(/^ICE (\w+) (\w+)([\s\S]*)$/);
  if (!match) {
    console.error('Invalid ICE message format');
    return;
  }

  const [_, senderType, id, iceData] = match; // match[0] là toàn chuỗi, bỏ qua
  
  const group = groups.get(id);

  console.log(`Handling ICE from ${senderType}, group ${id}`);

  if (!group) {
    console.error(`No group found for ID: ${id}`);
    return;
  }

  // Forward ICE

  const forwardMessage = `ICE\n${iceData.trim()}`;

  // Forward ICE
  if (senderType === 'camera' && group.clients.user && group.clients.user.readyState === WebSocket.OPEN) {
    group.clients.user.send(forwardMessage);
  } else if (senderType === 'user' && group.clients.camera && group.clients.camera.readyState === WebSocket.OPEN) {
    group.clients.camera.send(forwardMessage);
  }
}

module.exports = { handleIce };