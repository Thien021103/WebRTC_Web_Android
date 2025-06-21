const bcrypt = require('bcrypt');
const WebSocket = require('ws');

const Owner = require('../schemas/owner');
const User = require('../schemas/user');
const Group = require('../schemas/group');
const Door = require('../schemas/door');
const { groups } = require('../groups/groups');
const { mailDoorLock } = require('../utils/doorLock');

async function lock({ identifier, password, decoded }) {
  if (!identifier || !password || !decoded) {
    throw new Error('Missing required fields');
  }

  console.log(decoded);
  const groupId = decoded.groupId;
  const isOwner = decoded.isOwner;

  // Validate user/owner
  let entity;
  if (isOwner) {
    entity = await Owner.findOne({ email: identifier, groupId: groupId });
  } else {
    entity = await User.findOne({ email: identifier, groupId: groupId });
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
    throw new Error('Group not found');
  }

  if (dbGroup.door?.lock === 'Locked') {
    throw new Error('Already locked');
  }

  // Notify controller
  const group = groups.get(groupId);
  if (!group) {
    throw new Error('Group not found');
  }
  const controller = group.clients.controller;
  if (!controller) {
    throw new Error('Controller not connected');
  } else if (controller.readyState === WebSocket.OPEN) {
    controller.send(`LOCK ${dbGroup.controllerId}`);
  }

  const userIdentifier = isOwner ? `Owner ${identifier}` : `User ${identifier}`;

  // Update group state
  await Group.updateOne(
    { id: groupId },
    {
      $set: {
        door: {
          lock: 'Locked',
          user: userIdentifier,
          time: new Date(Date.now() + 7 * 60 * 60 * 1000)
        }
      }
    },
  );

  // Log door history
  await Door.create({
    groupId: groupId,
    state: 'Locked',
    user: userIdentifier,
    time: new Date(Date.now() + 7 * 60 * 60 * 1000)
  });

  console.log(`User/Owner ${identifier} locked group: ${groupId}`);
  mailDoorLock(dbGroup.ownerEmail, 'locked', identifier);
  return {};
}

module.exports = { lock };