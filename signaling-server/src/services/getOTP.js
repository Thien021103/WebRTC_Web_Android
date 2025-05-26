const Group = require('../schemas/group');
const Owner = require('../schemas/owner');

const { createAndSendOTP } = require("../otp/otp");

async function getOTP(email, groupId) {

  if (!email || !groupId) {
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
  await createAndSendOTP(email);
}

module.exports = { getOTP };