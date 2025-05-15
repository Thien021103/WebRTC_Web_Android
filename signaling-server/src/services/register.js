const bcrypt = require('bcrypt');
const { v4: uuidv4 } = require('uuid');
const User = require('../schemas/user');
const { groups } = require('../groups/groups');
const Group = require('../schemas/group');

async function registerUser({ email, password, groupId, fcmToken }) {
  if (!email || !password || !groupId || !fcmToken) {
    throw new Error('Missing required fields');
  }

  const existingUser = await User.findOne({ email });
  if (existingUser) {
    throw new Error('Email already registered');
  }

  const hashedPassword = await bcrypt.hash(password, 10);
  const accessToken = uuidv4();

  const user = new User({
    email,
    password: hashedPassword,
    groupId,
    accessToken,
    fcmToken,
  });
  await user.save();

  // Update groups
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

  await Group.udateOne(
    { id: groupId },
    { $set: { fcmToken } },
    { upsert: true }
  );

  console.log(`User registered: ${email}, group: ${groupId}, accessToken: ${accessToken}`);
  return { accessToken };
}

module.exports = { registerUser };