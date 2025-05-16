const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const Owner = require('../schemas/owner');
const User = require('../schemas/user');
const Group = require('../schemas/group');
const { groups } = require('../groups/groups');

const SECRET_KEY = process.env.JWT_SECRET || 'your-secret-key';

async function unlock({ identifier, password, accessToken }) {
  if (!identifier || !password || !accessToken) {
    throw new Error('Missing required fields');
  }

  // Verify JWT
  let decoded;
  try {
    decoded = jwt.verify(accessToken, SECRET_KEY);
    if ((decoded.isOwner && decoded.email !== identifier) || (!decoded.isOwner && decoded.id !== identifier)) {
      throw new Error('Unauthorized');
    }
  } catch (error) {
    console.error(error.message);
    throw new Error('Invalid or expired token');
  }

  const groupId = decoded.groupId;
  const isOwner = decoded.isOwner;

  // Validate user/owner
  let entity;
  if (isOwner) {
    entity = await Owner.findOne({ email: identifier, groupId, accessToken });
  } else {
    entity = await User.findOne({ id: identifier, groupId, accessToken });
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

  if (dbGroup.door?.lock === 'Unlocked') {
    throw new Error('Already unlocked');
  }

  // Update group state
  await Group.updateOne(
    { id: groupId },
    {
      $set: {
        door: {
          lock: 'Unlocked',
          user: isOwner ? `Owner ${identifier}` : `User ${identifier}`,
          time: new Date()
        }
      }
    },
  );

  // Notify controller
  const group = groups.get(groupId);
  if (!group) {
    throw new Error('Group not found');
  }
  const controller = group.clients.controller;
  if (controller && controller.readyState === controller.OPEN) {
    controller.send(`UNLOCK ${groupId}`);
  }

  console.log(`User/Owner ${identifier} unlocked group: ${groupId}`);
  return {};
}

module.exports = { unlock };