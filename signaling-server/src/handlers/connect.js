const jwt = require('jsonwebtoken');
const Owner = require('../schemas/owner');
const User = require('../schemas/user');
const Group = require('../schemas/group');
const { groups, notifyStateUpdate } = require('../groups/groups');
const { sendFCMNotification } = require('../fcm/fcm');
const { wsUserAuth } = require('../middleware/auth');

const SECRET_KEY = process.env.JWT_SECRET || 'your-secret-key';

async function handleConnect(message, client) {
  const msg = message.toString().trim();
  const match = msg.match(/^CONNECT\s+(\w+)\s+(\S+)$/);
  if (!match) {
    console.error('Invalid CONNECT message format');
    client.close();
    return;
  }
  const [_, type, idOrToken] = match;

  let groupId, jwtToken;

  if (type === 'user') {
    // Validate user/owner token
    if (!wsUserAuth(idOrToken, client)) {
      client.close();
      return;
    }
    let decoded = client._user;
    groupId = decoded.groupId;
    // client._accessToken = idOrToken;

  } else if (type === 'camera' || type === 'controller') {
    // Validate cameraId or controllerId
    const field = type === 'camera' ? 'cameraId' : 'controllerId';
    const dbGroup = await Group.findOne({ [field]: idOrToken });
    if (!dbGroup) {
      console.error(`Invalid ${field}`);
      client.close();
      return;
    }
    groupId = dbGroup.id;

    // Generate JWT for camera/controller
    jwtToken = jwt.sign({ [field]: idOrToken, groupId: groupId }, SECRET_KEY, { expiresIn: '7d' });
  } else {
    console.error(`Invalid connection type: ${type}`);
    client.close();
    return;
  }

  // Initialize local group if new
  if (!groups.has(groupId)) {
    groups.set(groupId, {
      id: groupId,
      state: 'Impossible',
      clients: { camera: null, user: null, controller: null }
    });
    console.log(`New group added: ${JSON.stringify(groups.get(groupId))}`);
  }

  const group = groups.get(groupId);

  // Check for existing client of the same type
  if (['camera', 'user', 'controller'].includes(type) && group.clients[type]) {
    console.error(`Group ${groupId} already has a ${type} connected`);
    client.send(`ERROR Group is busy - ${type} already connected`);
    client.close();
    return;
  }

  // Assign client to group
  if (['camera', 'user', 'controller'].includes(type)) {
    group.clients[type] = client;
    client._groupId = groupId;
    client._type = type;
    console.log(`${type} connected with group ID: ${groupId}`);
  }

  // Send JWT for camera/controller after client assignment
  if (type === 'camera') {
    client.send(`TOKEN ${jwtToken}`);
    await Group.updateOne({ id: groupId }, { $set: { cameraToken: jwtToken } });
  } else if (type === 'controller') {
    client.send(`TOKEN ${jwtToken}`);
    await Group.updateOne({ id: groupId }, { $set: { controllerToken: jwtToken } });
  }

  try {
    // Send FCM notifications for controller connection
    if (type === 'controller') {
      const users = await User.find({ groupId, fcmToken: { $ne: '' } }, { fcmToken: 1 }).lean();
      const owners = await Owner.find({ groupId, fcmToken: { $ne: '' } }, { fcmToken: 1 }).lean();
      const fcmTokens = [...users, ...owners].map(entity => entity.fcmToken);
      for (const fcmToken of fcmTokens) {
        await sendFCMNotification(fcmToken);
      }
      if (fcmTokens.length === 0) {
        console.log(`No FCM tokens found for group ${groupId}`);
      }
    }

    // Update group state
    const { camera, user, controller } = group.clients;
    const newState = camera && user && controller ? 'Ready' : 'Impossible';
    group.state = newState;

    await Group.updateOne({ id: groupId }, { $set: { state: newState } });
    notifyStateUpdate(group.id);
  } catch (error) {
    console.error('Error in handleConnect:', error.message);
  }
}

module.exports = { handleConnect };