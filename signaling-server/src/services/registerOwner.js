const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');

const Group = require('../schemas/group');
const Owner = require('../schemas/owner');

const { verifyRegisterOTP } = require('../utils/otp');

const SECRET_KEY = process.env.JWT_SECRET || 'your-secret-key';

async function registerOwner({ email, password, groupId, groupName, otp, fcmToken }) {
  if (!email || !password || !groupId || !groupName || !otp) {
    throw new Error('Missing required fields');
  }

  const undercaseEmail = email.toLowerCase().trim();

  if (!verifyRegisterOTP(undercaseEmail, otp)) {
    throw new Error('Invalid or expired OTP');
  }

  const dbGroup = await Group.findOne({ id: groupId, ownerEmail: undercaseEmail });
  if (!dbGroup) {
    throw new Error('Invalid groupId or email not authorized');
  }

  const existingGroup = await Group.findOne({ name: groupName, ownerEmail: undercaseEmail });
  if (existingGroup) {
    throw new Error('Group name already registered');
  }

  const existingOwner = await Owner.findOne({ groupId });
  if (existingOwner) {
    throw new Error('Unauthorized');
  }

  const hashedPassword = await bcrypt.hash(password, 10);
  const accessToken = jwt.sign({ email: undercaseEmail, groupId: groupId, isOwner: true }, SECRET_KEY, { expiresIn: '7d' });
  const cloudFolder = dbGroup.cloudFolder;

  if (fcmToken) {
    const dbOwner = new Owner({
      email: undercaseEmail,
      password: hashedPassword,
      groupId: groupId,
      accessToken: accessToken,
      fcmToken: fcmToken,
    });
    await dbOwner.save();
  } else {
    const dbOwner = new Owner({
      email: undercaseEmail,
      password: hashedPassword,
      groupId: groupId,
      accessToken: accessToken,
    });
    await dbOwner.save();
  }

  await Group.updateOne(
    { id: groupId }, 
    { $set: { name: groupName }}
  );

  console.log(`Owner registered: ${undercaseEmail}, group: ${groupName}`);
  return { accessToken, cloudFolder };
}

module.exports = { registerOwner }