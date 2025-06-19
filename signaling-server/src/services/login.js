const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const Owner = require('../schemas/owner');
const User = require('../schemas/user');
const Group = require('../schemas/group');

const SECRET_KEY = process.env.JWT_SECRET || 'your-secret-key';

async function loginOwner({ email, password, groupName, fcmToken }) {
  if (!email || !password || !groupName) {
    throw new Error('Missing required fields');
  }

  const dbGroup = await Group.findOne({ name: groupName, ownerEmail: email });
  if (!dbGroup) {
    throw new Error('Invalid groupName or email not authorized');
  }
  const cloudFolder = dbGroup.cloudFolder;

  const owner = await Owner.findOne({ email: email, groupId: dbGroup.id });
  if (!owner) {
    throw new Error('Invalid info');
  }

  const isPasswordValid = await bcrypt.compare(password, owner.password);
  if (!isPasswordValid) {
    throw new Error('Invalid password');
  }

  const accessToken = jwt.sign({ email: email, groupId: dbGroup.id, isOwner: true }, SECRET_KEY, { expiresIn: '7d' });
  if (fcmToken) {
    await Owner.updateOne(
      { email: email, groupId: dbGroup.id }, 
      { $set: { fcmToken: fcmToken, accessToken: accessToken } }
    );
  } else {
    await Owner.updateOne(
      { email: email, groupId: dbGroup.id }, 
      { $set: { accessToken: accessToken } }
    );
  }

  console.log(`Owner logged in: ${email}, group: ${groupName}`);
  return { accessToken, cloudFolder };
}

async function loginUser({ email, password, fcmToken }) {
  if (!email || !password) {
    throw new Error('Missing required fields');
  }

  const user = await User.findOne({ email });
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

  const accessToken = jwt.sign({ email: email, groupId: user.groupId, isOwner: false }, SECRET_KEY, { expiresIn: '7d' });
  
  if (fcmToken) {
    await User.updateOne(
      { email }, 
      { $set: { fcmToken: fcmToken, accessToken: accessToken } }
    );
  } else {
    await User.updateOne(
      { email }, 
      { $set: { accessToken: accessToken } }
    );
  }

  console.log(`User logged in: ${email}, group: ${user.groupId}`);
  return { accessToken, cloudFolder };
}

module.exports = { loginOwner, loginUser };