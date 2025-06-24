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

  const undercaseEmail = email.toLowerCase().trim();

  const dbGroup = await Group.findOne({ name: groupName, ownerEmail: undercaseEmail });
  if (!dbGroup) {
    throw new Error('Invalid groupName or email not authorized');
  }
  const cloudFolder = dbGroup.cloudFolder;

  const owner = await Owner.findOne({ email: undercaseEmail, groupId: dbGroup.id });
  if (!owner) {
    throw new Error('Invalid info');
  }

  const isPasswordValid = await bcrypt.compare(password, owner.password);
  if (!isPasswordValid) {
    throw new Error('Invalid password');
  }

  const accessToken = jwt.sign({ email: undercaseEmail, groupId: dbGroup.id, isOwner: true }, SECRET_KEY, { expiresIn: '7d' });
  if (fcmToken) {
    await Owner.updateOne(
      { email: undercaseEmail, groupId: dbGroup.id }, 
      { $set: { fcmToken: fcmToken, accessToken: accessToken } }
    );
  } else {
    await Owner.updateOne(
      { email: undercaseEmail, groupId: dbGroup.id }, 
      { $set: { accessToken: accessToken } }
    );
  }

  console.log(`Owner logged in: ${undercaseEmail}, group: ${groupName}`);
  return { accessToken, cloudFolder };
}

async function loginUser({ email, password, fcmToken }) {
  if (!email || !password) {
    throw new Error('Missing required fields');
  }

  const undercaseEmail = email.toLowerCase().trim();

  const user = await User.findOne({ email: undercaseEmail });
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

  const accessToken = jwt.sign({ email: undercaseEmail, groupId: user.groupId, isOwner: false }, SECRET_KEY, { expiresIn: '7d' });
  
  if (fcmToken) {
    await User.updateOne(
      { email: undercaseEmail }, 
      { $set: { fcmToken: fcmToken, accessToken: accessToken } }
    );
  } else {
    await User.updateOne(
      { email: undercaseEmail }, 
      { $set: { accessToken: accessToken } }
    );
  }

  console.log(`User logged in: ${undercaseEmail}, group: ${user.groupId}`);
  return { accessToken, cloudFolder };
}

module.exports = { loginOwner, loginUser };