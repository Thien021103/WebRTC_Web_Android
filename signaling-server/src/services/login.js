const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const Owner = require('../schemas/owner');
const User = require('../schemas/user');
const Group = require('../schemas/group');

const SECRET_KEY = process.env.JWT_SECRET || 'your-secret-key';

async function loginOwner({ email, password, groupId, fcmToken }) {
  if (!email || !password || !groupId || !fcmToken) {
    throw new Error('Missing required fields');
  }

  const dbGroup = await Group.findOne({ id: groupId, ownerEmail: email });
  if (!dbGroup) {
    throw new Error('Invalid groupId or email not authorized');
  }
  const cloudFolder = dbGroup.cloudFolder;

  const owner = await Owner.findOne({ email: email, groupId: groupId });
  if (!owner) {
    throw new Error('Invalid info');
  }

  const isPasswordValid = await bcrypt.compare(password, owner.password);
  if (!isPasswordValid) {
    throw new Error('Invalid password');
  }

  const accessToken = jwt.sign({ email: email, groupId: groupId, isOwner: true }, SECRET_KEY, { expiresIn: '7d' });
  await Owner.updateOne(
    { email, groupId }, 
    { $set: { fcmToken: fcmToken, accessToken: accessToken } }
  );

  console.log(`Owner logged in: ${email}, group: ${groupId}`);
  return { accessToken, cloudFolder };
}

async function loginUser({ id, password, fcmToken }) {
  if (!id || !password || !fcmToken) {
    throw new Error('Missing required fields');
  }

  const user = await User.findOne({ id });
  if (!user) {
    throw new Error('Invalid info');
  }

  const dbGroup = await Group.findOne({ id: user.groupId });
  if (!dbGroup) {
    throw new Error('Invalid info');
  }
  const cloudFolder = dbGroup.cloudFolder;

  const isPasswordValid = await bcrypt.compare(password, user.password);
  if (!isPasswordValid) {
    throw new Error('Invalid password');
  }

  const accessToken = jwt.sign({ id: id, groupId: user.groupId, isOwner: false }, SECRET_KEY, { expiresIn: '7d' });
  await User.updateOne(
    { id }, 
    { $set: { fcmToken: fcmToken, accessToken: accessToken } }
  );

  console.log(`User logged in: ${id}, group: ${user.groupId}`);
  return { accessToken, cloudFolder };
}

module.exports = { loginOwner, loginUser };