const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const Group = require('../schemas/group');
const Owner = require('../schemas/owner');
const User = require('../schemas/user');

const SECRET_KEY = process.env.JWT_SECRET || 'your-secret-key';

async function registerOwner({ email, password, groupId, fcmToken }) {
  if (!email || !password || !groupId || !fcmToken) {
    throw new Error('Missing required fields');
  }

  const dbGroup = await Group.findOne({ id: groupId, ownerEmail: email });
  if (!dbGroup) {
    throw new Error('Invalid groupId or email not authorized');
  }

  const existingOwner = await Owner.findOne({ groupId });
  if (existingOwner) {
    throw new Error('Unauthorized');
  }

  const hashedPassword = await bcrypt.hash(password, 10);
  const accessToken = jwt.sign({ email: email, groupId: groupId, isOwner: true }, SECRET_KEY, { expiresIn: '7d' });

  const dbOwner = new Owner({
    email: email,
    password: hashedPassword,
    groupId: groupId,
    accessToken: accessToken,
    fcmToken: fcmToken,
  });
  await dbOwner.save();

  console.log(`Owner registered: ${email}, group: ${groupId}, accessToken: ${accessToken}`);
  return { accessToken };
}

async function registerUser({ id, password, ownerToken }) {
  if (!id || !password || !ownerToken) {
    throw new Error('Missing required fields');
  }

  // Verify JWT
  let decoded;
  try {
    decoded = jwt.verify(ownerToken, SECRET_KEY);
    if (!decoded.isOwner) {
      throw new Error('Unauthorized');
    }
  } catch (error) {
    throw new Error('Invalid or expired token');
  }

  // Verify owner
  const owner = await Owner.findOne({ email: decoded.email, groupId: decoded.groupId, accessToken: ownerToken });
  if (!owner) {
    throw new Error('Unauthorized');
  }

  const existingUser = await User.findOne({ id });
  if (existingUser) {
    throw new Error('User ID already registered');
  }

  const hashedPassword = await bcrypt.hash(password, 10);
  const user = new User({
    id: id,
    password: hashedPassword,
    groupId: decoded.groupId,
  });
  await user.save();

  console.log(`User registered: ${id}, group: ${decoded.groupId}`);
}

module.exports = { registerOwner, registerUser };