const Group = require('../schemas/group');
const Owner = require('../schemas/owner');

const { createRegisterOTP } = require("../utils/otp");

async function registerOTP(email, groupId) {

  if (!email || !groupId) {
    throw new Error('Missing required fields');
  }

  const undercaseEmail = email.toLowerCase().trim();

  const dbGroup = await Group.findOne({ id: groupId, ownerEmail: undercaseEmail });
  if (!dbGroup) {
    throw new Error('Invalid groupId or email');
  }

  const existingOwner = await Owner.findOne({ groupId });
  if (existingOwner) {
    throw new Error('Unauthorized');
  }
  await createRegisterOTP(undercaseEmail);
}

module.exports = { registerOTP };