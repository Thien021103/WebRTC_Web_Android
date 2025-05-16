const User = require('../schemas/user');
const Owner = require('../schemas/owner')

async function logoutOwner({ email, groupId, accessToken }) {
  if (!email || !groupId || !accessToken) {
    throw new Error('Missing required fields');
  }

  const owner = await Owner.findOne({ email, groupId, accessToken });
  if (!owner) {
    throw new Error('Invalid info');
  }
  await Owner.updateOne({ email, groupId }, { $unset: { accessToken: '' } });

  console.log(`Owner logged out: ${email}, group: ${groupId}`);
  return {};
}

async function logoutUser({ id, accessToken }) {
  if (!id || !accessToken) {
    throw new Error('Missing required fields');
  }
  
  const user = await User.findOne({ id, accessToken });
  if (!user) {
    throw new Error('Invalid info');
  }

  await User.updateOne({ id }, { $unset: { accessToken: '' } });

  console.log(`User logged out: ${id}`);
  return {};
}

module.exports = { logoutUser, logoutOwner };