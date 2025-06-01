const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const { v4: uuidv4 } = require('uuid');

const Group = require('../schemas/group');
const Owner = require('../schemas/owner');

const { verifyOTP } = require('../utils/otp');

const SECRET_KEY = process.env.JWT_SECRET || 'your-secret-key';

async function registerOwner({ email, password, groupId, otp, fcmToken }) {
  if (!email || !password || !groupId || !fcmToken || !otp) {
    throw new Error('Missing required fields');
  }

  if (!verifyOTP(email, otp)) {
    throw new Error('Invalid or expired OTP');
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
  const cloudFolder = uuidv4(); // Generate UUID for cloudFolder

  const dbOwner = new Owner({
    email: email,
    password: hashedPassword,
    groupId: groupId,
    accessToken: accessToken,
    fcmToken: fcmToken,
  });
  await dbOwner.save();

  await Group.updateOne({ id: groupId }, { $set: { cloudFolder: cloudFolder } });

  console.log(`Owner registered: ${email}, group: ${groupId}, accessToken: ${accessToken}`);
  return { accessToken, cloudFolder };
}

module.exports = { registerOwner }