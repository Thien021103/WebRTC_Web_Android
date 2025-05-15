const User = require('../schemas/user');
const Group = require('../schemas/group');
const { groups, notifyStateUpdate } = require('../groups/groups');
const { sendFCMNotification } = require('../fcm/fcm');

async function handleConnect(message, client) {
  const msg = message.toString().trim();
  const [_, type, token] = msg.split(' ');

  let groupId;

  if (type === 'user') {
    try {
      const user = await User.findOne({ accessToken: token });
      if (!user || !user.accessToken) {
        console.error('Invalid access token');
        client.close();
        return;
      }
      groupId = user.groupId;
      client._accessToken = token;
    } catch (error) {
      console.error('Error validating user accessToken:', error.message);
      client.close();
      return;
    }
  } else {
    groupId = token;
  }

  // New group if received a new id
  if (!groups.has(groupId)) {
    groups.set(groupId, {
      id: groupId,
      state: 'Impossible',
      clients: { camera: null, user: null, controller: null },
      fcmToken: null,
    });
    console.log(`New group added:\n${JSON.stringify(groups.get(groupId))}`);
  }

  console.log(groups);

  const group = groups.get(groupId);

  if (['camera', 'user', 'controller'].includes(type)) {
    group.clients[type] = client;
    client._groupId = groupId;  // groupId for WebSocket instance
    client._type = type;        // type of WebSocket instance
    console.log(`${type} connected with group ID: ${groupId}`);
  }

  try {
    const dbGroup = await Group.findOne({ id: groupId });
    if (!dbGroup) {
      await Group.create({
        id: groupId,
        state: 'Impossible',
        fcmToken: '',
      });
    } else if (type === 'user' && group.fcmToken) {
      await Group.updateOne(
        { id: groupId }, 
        { $set: { state: 'Impossible' } }
      );
    }

    // Gửi FCM notification khi controller kết nối
    if (type === 'controller') {
      if (dbGroup && dbGroup.fcmToken) {
        await sendFCMNotification(dbGroup.fcmToken);
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

    await Group.updateOne(
      { id: groupId }, 
      { $set: { state: newState } }
    );

    notifyStateUpdate(groupId);
  } catch (error) {
    console.error('Error in handleConnect:', error.message);
  }
}

// Handle disconnect
async function handleDisconnect(client) {
  if (client && client._groupId) {

    const group = groups.get(client._groupId);

    if (group) {

      // Delete from group
      group.clients[client._type] = null;
      
      // Update group state in collection and local
      if (!group.clients.camera || !group.clients.user || !group.clients.controller) {
        group.state = 'Impossible';
        try {
          await Group.updateOne(
            { id: client._groupId },
            { $set: { state: 'Impossible' } }
          );
          console.log(`Updated group ${client._groupId} state to Impossible`);
        } catch (error) {
          console.error(`Error updating group state: ${error.message}`);
        }
        notifyStateUpdate(client._groupId);
      }

      if (!group.clients.camera && !group.clients.user && !group.clients.controller) {
        // Delete group in local
        groups.delete(client._groupId);
      }
    }
  }
}

module.exports = { handleConnect, handleDisconnect };