const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const { v4: uuidv4 } = require('uuid');

const Owner = require('../schemas/owner');
const User = require('../schemas/user');

const SECRET_KEY = process.env.JWT_SECRET || 'your-secret-key';

async function registerUser({ userName, password, ownerToken }) {
  if (!userName || !password || !ownerToken) {
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
  
  const existingUser = await User.findOne({ name: userName, groupId: decoded.groupId });
  if (existingUser) {
    throw new Error('Username already registered');
  }

  let userId;
  let isUnique = false;
  while (!isUnique) {
    userId = uuidv4();
    // Check uniqueness
    const existingUser = await User.findOne({ id: userId });
    if (!existingUser) {
      isUnique = true;
    }
  }

  const hashedPassword = await bcrypt.hash(password, 10);
  const user = new User({
    id: userId,
    name: userName,
    password: hashedPassword,
    groupId: decoded.groupId,
  });
  await user.save();

  console.log(`User registered: ${userId}, group: ${decoded.groupId}`);
}

module.exports = { registerUser };