const { getDb } = require('../db/db');
const { sendFCMNotification } = require('../fcm/fcm');
const { groups, notifyStateUpdate } = require('../groups/groups');

async function handleConnect(message, client) {

  const db = getDb();
  let groupId;

  const msg = message.toString().trim();
  const [_, type, token] = msg.split(' '); // "CONNECT camera 123" -> ["CONNECT", "camera", "123"]

  // Handle user type with accessToken validation
  if (type === 'user') {
    try {
      // Find user in db by accessToken
      const user = await db.collection('users').findOne({ accessToken: token });
      if (!user) {
        console.error('Invalid access token');
        client.close();
        return;
      }
      groupId = user.groupId; // Get groupId from user document
    } catch (error) {
      console.error('Error validating user accessToken:', error.message);
      client.close();
      return;
    }
  } else {
    // For non-user types (camera, controller), use token as id
    groupId = token;
  }

  // New group if received a new id
  if (!groups.has(groupId)) {
    groups.set(groupId, {
      groupId,
      state: 'Impossible',
      clients: {
        camera: null,
        user: null,
        controller: null,
      },
      fcm_token: null,
    });
    console.log(`New group added:\n${groups.get(groupId)}`)
  }

  const group = groups.get(groupId);

  // Assign client to group based on type
  if (type === 'camera' || type === 'user' || type === 'controller') {
    group.clients[type] = client;
    client._groupId = groupId;  // groupId for WebSocket instance
    client._type = type;        // type of WebSocket instance
    console.log(`${type} connected with group ID: ${groupId}`);
  }

  try {
    // Save or update group in MongoDB
    const dbGroup = await db.collection('groups').findOne({ id: groupId });
    if (!dbGroup) {
      await db.collection('groups').insertOne({
        id: groupId,
        state: 'Impossible',
        fcm_token: '',
      });
    } else if (type === 'user' && group.fcm_token) {
      await db.collection('groups').updateOne(
        { id: groupId },
        { $set: { state: "Impossible" } }
      );
    }

    // Gửi FCM notification khi controller kết nối
    if (type === 'controller') {
      // const dbGroup = await db.collection('groups').findOne({ id: groupId });
      if (dbGroup && dbGroup.fcm_token) {
        await sendFCMNotification(dbGroup.fcm_token);
      } else {
        console.log(`No user token found for group ${groupId}`);
      }
    }

    // Cập nhật trạng thái group
    const camera = group.clients.camera;
    const user = group.clients.user;
    const controller = group.clients.controller;

    const newState = camera && user && controller ? 'Ready' : 'Impossible';
    group.state = newState;

    await db.collection('groups').updateOne(
      { id: groupId },
      { $set: { state: newState } }
    );

    notifyStateUpdate(groupId);
  } catch (error) {
    console.error('Error in handleConnect:', error.message);
  }
}

module.exports = { handleConnect };