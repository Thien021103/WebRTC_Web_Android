const http = require('http');
const WebSocket = require('ws');

/**  Groups are stored as JSON: 
{ id,
  state,
  clients: {
    camera: ws,
    user: ws,
    controller: ws
  }
}
**/
const groups = new Map();

// Utility để thông báo trạng thái đến tất cả client trong cùng nhóm
function notifyStateUpdate(groupId) {
  const group = groups.get(groupId);
  if (group) {
    const message = `STATE ${group.state}`;
    for (const client of Object.values(group.clients)) {
      if (client.readyState === WebSocket.OPEN) {
        client.send(message);
      }
    }
  }
}

// Create HTTP server
const server = http.createServer((req, res) => {
  res.writeHead(404, { 'Content-Type': 'text/plain' });
  res.end('Not Found');
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
    }
  });

  ws.on('close', () => {
    handleDisconnect(ws);
    console.log(`Client ${sessionId} disconnected`);
  });

  ws.on('error', console.error);
});

// Xử lý kết nối từ client (Camera, User, Controller)
function handleConnect(message, client) {
  const [_, type, id] = message.split(' '); // "CONNECT camera 123" -> ["CONNECT", "camera", "123"]

  // New group if received a new id
  if (!groups.has(id)) {
    groups.set(id, {
      id: id,
      state: 'Impossible',
      clients: {}
    });
  }

  const group = groups.get(id);

  // Gán client vào nhóm theo loại
  if (type === 'camera' || type === 'user' || type === 'controller') {
    group.clients[type] = client;
    client._groupId = id; // groupId for WebSocket instance
    client._type = type;  // type of WebSocket instance
    console.log(`${type} connected with ID: ${id}`);
  }

  // Check for Readiness
  if (group.clients.camera && group.clients.user && group.clients.controller) {
    group.state = 'Ready';
    notifyStateUpdate(id);
    console.log(`Group ${id} is Ready with Camera, User, Controller`);
  } else {
    group.state = 'Impossible';
    notifyStateUpdate(id);
  }
}

// Handle disconnect
function handleDisconnect(client) {
  if (client && client._groupId) {
    const group = groups.get(client._groupId);
    if (group) {
      delete group.clients[client._type]; // Delete from group
      if (!group.clients.camera || !group.clients.user || !group.clients.controller) {
        group.state = 'Impossible';
        notifyStateUpdate(client._groupId);
      }
      if (Object.keys(group.clients).length === 0) {
        groups.delete(client._groupId); // Delete group
      }
    }
  }
}

// Xử lý OFFER message
function handleOffer(message) {
  // Sử dụng regex để trích xuất senderType, id, và phần còn lại
  const match = message.match(/^OFFER (\w+) (\w+)([\s\S]*)$/);
  if (!match) {
    console.error('Invalid OFFER message format');
    return;
  }

  const [_, senderType, id, sdpData] = match; // match[0] là toàn bộ chuỗi, bỏ qua
  const group = groups.get(id);

  if (!group || group.state !== 'Ready') {
    console.error('Session must be Ready to handle offer');
    return;
  }

  if (senderType !== 'camera') {
    console.error('Only camera can send OFFER');
    return;
  }

  group.state = 'Creating';
  console.log(`Handling offer from ${senderType} ${id}`);
  notifyStateUpdate(id);

  // Tạo message mới bằng cách thay thế phần đầu
  const forwardMessage = `OFFER${sdpData}`;

  // Forward OFFER đến User
  if (group.clients.user && group.clients.user.readyState === WebSocket.OPEN) {
    group.clients.user.send(forwardMessage);
  }
}

// Xử lý ANSWER message
function handleAnswer(message) {
  // Sử dụng regex để trích xuất senderType, id, và phần còn lại
  const match = message.match(/^ANSWER (\w+) (\w+)([\s\S]*)$/);
  if (!match) {
    console.error('Invalid ANSWER message format');
    return;
  }

  const [_, senderType, id, sdpData] = match; // match[0] là toàn chuỗi, bỏ qua
  const group = groups.get(id);

  if (!group || group.state !== 'Creating') {
    console.error('Session must be in Creating state to handle answer');
    return;
  }

  if (senderType !== 'user') {
    console.error('Only user can send ANSWER');
    return;
  }

  group.state = 'Active';
  console.log(`Handling answer from ${senderType} ${id}`);
  notifyStateUpdate(id);

  // Tạo message mới bằng cách thay thế phần đầu
  const forwardMessage = `ANSWER${sdpData}`;

  // Forward ANSWER đến Camera
  if (group.clients.camera && group.clients.camera.readyState === WebSocket.OPEN) {
    group.clients.camera.send(forwardMessage);
  }
}

// Xử lý ICE message
function handleIce(message) {
  // Sử dụng regex để trích xuất senderType, id, và phần còn lại
  const match = message.match(/^ICE (\w+) (\w+)([\s\S]*)$/);
  if (!match) {
    console.error('Invalid ICE message format');
    return;
  }

  const [_, senderType, id, iceData] = match; // match[0] là toàn chuỗi, bỏ qua
  const group = groups.get(id);

  if (!group) {
    console.error(`No group found for ID: ${id}`);
    return;
  }

  console.log(`Handling ICE from ${senderType} ${id}`);

  // Tạo message mới với "ICE" trên dòng riêng
  const forwardMessage = `ICE\n${iceData.trim()}`; // trim() để loại bỏ dấu cách đầu dòng nếu có

  // Chuyển tiếp ICE giữa Camera và User
  if (senderType === 'camera' && group.clients.user && group.clients.user.readyState === WebSocket.OPEN) {
    group.clients.user.send(forwardMessage);
  } else if (senderType === 'user' && group.clients.camera && group.clients.camera.readyState === WebSocket.OPEN) {
    group.clients.camera.send(forwardMessage);
  }
}

// Khởi động server
const port = process.env.PORT || 8000;
server.listen(port, () => {
  console.log(`Server listening on port ${port}`);
});
