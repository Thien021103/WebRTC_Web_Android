const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const { v4: uuidv4 } = require('uuid');

const Owner = require('../schemas/owner');
const User = require('../schemas/user');
const { mailNewUser } = require('../utils/newUser');

const SECRET_KEY = process.env.JWT_SECRET || 'your-secret-key';

async function registerUser({ userName, email, password, ownerToken }) {
  if (!userName || !password || !ownerToken || !email) {
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
  
  const existingUser = await User.findOne({ email: email });
  if (existingUser) {
    throw new Error('Email already registered');
  }

  const existingUserName = await User.findOne({ name: userName, groupId: decoded.groupId });
  if (existingUserName) {
    throw new Error('Username already registered');
  }

  const hashedPassword = await bcrypt.hash(password, 10);
  const user = new User({
    email: email,
    name: userName,
    password: hashedPassword,
    groupId: decoded.groupId,
  });
  await user.save();

  await mailNewUser(email, userName, decoded.email);
  console.log(`User registered: ${email}, group: ${decoded.groupId}`);
}

module.exports = { registerUser };