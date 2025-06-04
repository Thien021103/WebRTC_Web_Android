const User = require('../schemas/user');
const Owner = require('../schemas/owner');
const Group = require('../schemas/group');

async function logoutOwner({ email, groupName }) {
  if ( !email || !groupName ) {
    throw new Error('Missing required fields');
  }
  const dbGroup = await Group.findOne({ name: groupName, ownerEmail: email });
  if (!dbGroup) {
    throw new Error('Invalid groupName or email not authorized');
  }

  const owner = await Owner.findOne({ email: email, groupId: dbGroup.id });
  if (!owner) {
    throw new Error(`Invalid info ${email}, group: ${groupName}`);
  }
  await Owner.updateOne(
    { email: email, groupId: dbGroup.id }, 
    { $unset: 
      { accessToken: '' } 
    }
  );

  console.log(`Owner logged out: ${email}, group: ${groupName}`);
  return {};
}

async function logoutUser({ email }) {
  if ( !email ) {
    throw new Error('Missing required fields');
  }
  
  const user = await User.findOne({ email });
  if (!user) {
    throw new Error('Invalid info');
  }

  await User.updateOne({ email }, { $unset: { accessToken: '' } });

  console.log(`User logged out: ${email}`);
  return {};
}

module.exports = { logoutUser, logoutOwner };