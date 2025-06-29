const Group = require('../schemas/group');
const Owner = require('../schemas/owner');
const User = require('../schemas/user');

const { createForgetOTP } = require("../utils/otp");

async function forgetUserOTP(email) {

  if (!email) {
    throw new Error('Missing required fields');
  }

  const undercaseEmail = email.toLowerCase().trim();

  const existingUser = await User.findOne({ email: undercaseEmail });
  if (!existingUser) {
    throw new Error('Invalid email');
  }
  await createForgetOTP(undercaseEmail);
}

async function forgetOwnerOTP({ email, groupId }) {

  if (!email || !groupId) {
    throw new Error('Missing required fields');
  }

  const undercaseEmail = email.toLowerCase().trim();

  const dbGroup = await Group.findOne({ id: groupId, ownerEmail: undercaseEmail });
  if (!dbGroup) {
    throw new Error('Invalid groupId or email');
  }

  const existingOwner = await Owner.findOne({ groupId: groupId, email: undercaseEmail });
  if (!existingOwner) {
    throw new Error('Invalid groupId or email');
  }
  await createForgetOTP(undercaseEmail);
}

module.exports = { forgetUserOTP, forgetOwnerOTP };