const bcrypt = require('bcrypt');
const WebSocket = require('ws');
const Owner = require('../schemas/owner');
const User = require('../schemas/user');
const Group = require('../schemas/group');
const Door = require('../schemas/door');
const { groups } = require('../groups/groups');
const { mailDoorLock } = require('../utils/doorLock');

async function unlock({ identifier, password, decoded }) {
  if (!identifier || !password || !decoded) {
    throw new Error('Missing required fields');
  }

  const undercaseEmail = identifier.toLowerCase().trim();

  console.log(decoded);
  const groupId = decoded.groupId;
  const isOwner = decoded.isOwner;

  // Validate user/owner
  let entity;
  if (isOwner) {
    entity = await Owner.findOne({ email: undercaseEmail, groupId: groupId });
  } else {
    entity = await User.findOne({ email: undercaseEmail, groupId: groupId });
  }
  if (!entity) {
    throw new Error('Invalid info');
  }

  const isPasswordValid = await bcrypt.compare(password, entity.password);
  if (!isPasswordValid) {
    throw new Error('Invalid password');
  }

  const dbGroup = await Group.findOne({ id: groupId });
  if (!dbGroup) {
    throw new Error('Controller not connected');
  }

  // if (dbGroup.door?.lock === 'Unlocked') {
  //   throw new Error('Already unlocked');
  // }

  // Notify controller
  const group = groups.get(groupId);
  if (!group) {
    throw new Error('Controller not connected');
  }
  const controller = group.clients.controller;
  if (!controller) {
    throw new Error('Controller not connected');
  } else if (controller.readyState === WebSocket.OPEN) {
    controller.send(`UNLOCK ${dbGroup.controllerId}`);
  }

  const userIdentifier = isOwner ? `Owner ${undercaseEmail}` : `User ${undercaseEmail}`;

  // Update group state
  await Group.updateOne(
    { id: groupId },
    {
      $set: {
        door: {
          lock: 'Unlocked',
          user: userIdentifier,
          time: new Date(Date.now() + 7 * 60 * 60 * 1000)
        }
      }
    },
  );

  // Log door history
  await Door.create({
    groupId: groupId, 
    state: 'Unlocked',
    user: userIdentifier,
    time: new Date(Date.now() + 7 * 60 * 60 * 1000)
  });

  console.log(`User/Owner ${undercaseEmail} unlocked group: ${groupId}`);
  mailDoorLock(dbGroup.ownerEmail, 'unlocked', undercaseEmail)
  return {};
}

module.exports = { unlock };