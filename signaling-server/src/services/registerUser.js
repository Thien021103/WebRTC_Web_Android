const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');

const Owner = require('../schemas/owner');
const User = require('../schemas/user');

const SECRET_KEY = process.env.JWT_SECRET || 'your-secret-key';

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

module.exports = { registerUser };