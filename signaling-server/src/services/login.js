const bcrypt = require('bcrypt');
const { v4: uuidv4 } = require('uuid');
const User = require('../schemas/user');
const Group = require('../schemas/group');
const { groups } = require('../groups/groups');

async function loginUser({ email, password, groupId, fcmToken }) {
  if (!email || !password || !groupId || !fcmToken) {
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

  const accessToken = uuidv4();
  await User.updateOne(
    { email, groupId }, 
    { $set: { accessToken, fcmToken } }
  );

  const group = groups.get(groupId);
  if (!group) {
    groups.set(groupId, {
      id: groupId,
      state: 'Impossible',
      clients: { camera: null, user: null, controller: null },
      fcmToken,
    });
  } else {
    group.fcmToken = fcmToken;
  }

  await Group.updateOne(
    { id: groupId },
    { $set: { fcmToken } },
    { upsert: true }
  );

  console.log(`User logged in: ${email}, group: ${groupId}`);
  return { accessToken };
}

module.exports = { loginUser };