const bcrypt = require('bcrypt');
const Group = require('../schemas/group');
const Owner = require('../schemas/owner');
const User = require('../schemas/user');
const { mailNewPassword } = require('../utils/changePassword');

async function changePassword({ password, newPassword, decoded }) {

  const groupId = decoded.groupId;
  const email = decoded.email;

  const dbGroup = await Group.findOne({ id: groupId });
  if (!dbGroup) {
    throw new Error('Group not found');
  }
  
  if(decoded.isOwner) {
    const existingOwner = await Owner.findOne({ groupId: groupId, email: email });
    if (!existingOwner) {
      throw new Error('Owner not found');
    }
    const isPasswordValid = await bcrypt.compare(password, existingOwner.password);
    if (!isPasswordValid) {
      throw new Error('Invalid password');
    }

    const newHashedPassword = await bcrypt.hash(newPassword, 10);
    await Owner.findOneAndUpdate(
      { email: email, groupId: groupId },
      { password: newHashedPassword },
      { new: true }
    );
  } else {
    const existingUser = await User.findOne({ email: email });
    if (!existingUser) {
      throw new Error('User not found');
    }
    const isPasswordValid = await bcrypt.compare(password, existingUser.password);
    if (!isPasswordValid) {
      throw new Error('Invalid password');
    }

    const newHashedPassword = await bcrypt.hash(newPassword, 10);
    await User.findOneAndUpdate(
      { email: email, groupId: groupId },
      { password: newHashedPassword },
      { new: true }
    );
  }
  // Send new password via email
  await mailNewPassword(email);
  console.log(`Password changed: ${email}`);
}

module.exports = { changePassword };