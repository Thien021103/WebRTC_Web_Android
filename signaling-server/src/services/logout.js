const User = require('../schemas/user');
const Owner = require('../schemas/owner');
const Group = require('../schemas/group');

async function logoutOwner({ email, groupName }) {
  if ( !email || !groupName ) {
    throw new Error('Missing required fields');
  }

  const undercaseEmail = email.toLowerCase().trim();

  const dbGroup = await Group.findOne({ name: groupName, ownerEmail: undercaseEmail });
  if (!dbGroup) {
    throw new Error('Invalid groupName or email not authorized');
  }

  const owner = await Owner.findOne({ email: undercaseEmail, groupId: dbGroup.id });
  if (!owner) {
    throw new Error(`Invalid info ${undercaseEmail}, group: ${groupName}`);
  }
  await Owner.updateOne(
    { email: undercaseEmail, groupId: dbGroup.id }, 
    { $unset: 
      { accessToken: '' } 
    }
  );

  console.log(`Owner logged out: ${undercaseEmail}, group: ${groupName}`);
  return {};
}

async function logoutUser({ email }) {
  if ( !email ) {
    throw new Error('Missing required fields');
  }

  const undercaseEmail = email.toLowerCase().trim();
  
  const user = await User.findOne({ email: undercaseEmail });
  if (!user) {
    throw new Error('Invalid info');
  }

  await User.updateOne({ email: undercaseEmail }, { $unset: { accessToken: '' } });

  console.log(`User logged out: ${undercaseEmail}`);
  return {};
}

module.exports = { logoutUser, logoutOwner };