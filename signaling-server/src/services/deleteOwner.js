const Owner = require('../schemas/owner');
const Group = require('../schemas/group');
const User = require('../schemas/user');

async function deleteOwner({ email, groupId }) {
  if (!email || !groupId) {
    throw new Error('Missing required fields');
  }

  const undercaseEmail = email.toLowerCase().trim();

  // Validate groupId
  const group = await Group.findOne({ id: groupId });
  if (!group) {
    throw new Error('Group not found');
  }

  // Find and delete owner
  const owner = await Owner.findOneAndDelete({ email: undercaseEmail, groupId: groupId });
  if (!owner) {
    throw new Error('Owner not found or does not belong to the specified group');
  }
  
  // Delete all users in the group
  await User.deleteMany({ groupId });

  await Group.updateOne({ id: groupId }, { $unset: { name: '' } });

  return { undercaseEmail, groupId };
}

module.exports = { deleteOwner };