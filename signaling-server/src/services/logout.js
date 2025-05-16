const User = require('../schemas/user');
const Owner = require('../schemas/owner')

async function logoutOwner({ email, groupId }) {
  if ( !email || !groupId ) {
    throw new Error('Missing required fields');
  }

  const owner = await Owner.findOne({ email, groupId });
  if (!owner) {
    throw new Error('Invalid info');
  }
  await Owner.updateOne({ email, groupId }, { $unset: { accessToken: '' } });

  console.log(`Owner logged out: ${email}, group: ${groupId}`);
  return {};
}

async function logoutUser({ id }) {
  if ( !id ) {
    throw new Error('Missing required fields');
  }
  
  const user = await User.findOne({ id });
  if (!user) {
    throw new Error('Invalid info');
  }

  await User.updateOne({ id }, { $unset: { accessToken: '' } });

  console.log(`User logged out: ${id}`);
  return {};
}

module.exports = { logoutUser, logoutOwner };