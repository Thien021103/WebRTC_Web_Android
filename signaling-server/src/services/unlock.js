const bcrypt = require('bcrypt');
const User = require('../schemas/user');
const Group = require('../schemas/group');
const { groups } = require('../groups/groups');

async function unlockGroup({ email, password, groupId }) {
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

  if (dbGroup.door?.lock === 'Unlocked') {
    throw new Error('Already unlocked');
  }

  await Group.updateOne(
    { id: groupId },
    {
      $set: {
        door: {
          lock: 'Unlocked',
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
    controller.send(`UNLOCK ${groupId}`);
  }

  console.log(`User ${email} unlocked group: ${groupId}`);
  return {};
}

module.exports = { unlockGroup };