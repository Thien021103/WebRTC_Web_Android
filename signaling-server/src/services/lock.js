const bcrypt = require('bcrypt');
const User = require('../schemas/user');
const Group = require('../schemas/group');
const { groups } = require('../groups/groups');

async function lockGroup({ email, password, groupId }) {
  if (!email || !password || !groupId) {
    throw new Error('Missing required fields');
  }

  const user = await User.findOne({ email, groupId });
  if (!user) {
    throw new Error('Invalid info');
  }

  const isPasswordValid = await bcrypt.compare(password, user.password);
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

  await Group.updateOne(
    { id: groupId },
    {
      $set: {
        door: {
          lock: 'Locked',
          user: `User ${email}`,
          time: new Date()
        }
      }
    },
    { upsert: true }
  );

  const group = groups.get(groupId);
  if (!group) {
    throw new Error('Group not found');
  }

  const controller = group.clients.controller;
  if (controller && controller.readyState === controller.OPEN) {
    controller.send(`LOCK ${groupId}`);
  }

  console.log(`User ${email} locked group: ${groupId}`);
  return {};
}

module.exports = { lockGroup };