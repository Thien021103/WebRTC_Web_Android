const bcrypt = require('bcrypt');
const Group = require('../schemas/group');
const Owner = require('../schemas/owner');
const User = require('../schemas/user');
const { verifyForgetOTP } = require('../utils/otp');

async function resetUserPassword({ email, password, otp }) {
  if (!email || !password || !otp) {
    throw new Error('Missing required fields');
  }

  const lowercaseEmail = email.toLowerCase().trim();

  const existingUser = await User.findOne({ email: lowercaseEmail });
  if (!existingUser) {
    throw new Error('User not found');
  }

  if (!verifyForgetOTP(lowercaseEmail, otp)) {
    throw new Error('Invalid or expired OTP');
  }

  const hashedPassword = await bcrypt.hash(password, 10);
  await User.updateOne({ email: lowercaseEmail }, { $set: { password: hashedPassword } });

  console.log(`User password updated: ${lowercaseEmail}`);
}

async function resetOwnerPassword({ email, groupId, password, otp }) {
  if (!email || !groupId || !password || !otp) {
    throw new Error('Missing required fields');
  }

  const lowercaseEmail = email.toLowerCase().trim();

  const dbGroup = await Group.findOne({ id: groupId, ownerEmail: lowercaseEmail });
  if (!dbGroup) {
    throw new Error('Invalid groupId or email');
  }

  const existingOwner = await Owner.findOne({ email: lowercaseEmail, groupId });
  if (!existingOwner) {
    throw new Error('Owner not found for this group');
  }

  if (!verifyForgetOTP(lowercaseEmail, otp)) {
    throw new Error('Invalid or expired OTP');
  }

  const hashedPassword = await bcrypt.hash(password, 10);
  await Owner.updateOne({ email: lowercaseEmail, groupId: groupId }, { $set: { password: hashedPassword } });

  console.log(`Owner password updated: ${lowercaseEmail}, group: ${groupId}`);
}

module.exports = { resetUserPassword, resetOwnerPassword };