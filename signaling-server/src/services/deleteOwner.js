const Owner = require('../schemas/owner');
const Group = require('../schemas/group');
const User = require('../schemas/user');

async function deleteOwner({ email, groupId }) {
  if (!email || !groupId) {
    throw new Error('Missing required fields');
  }

  // Validate groupId
  const group = await Group.findOne({ id: groupId });
  if (!group) {
    throw new Error('Group not found');
  }

  // Delete all users in the group
  await User.deleteMany({ groupId });

  // Find and delete owner
  const owner = await Owner.findOneAndDelete({ email, groupId });
  if (!owner) {
    throw new Error('Owner not found or does not belong to the specified group');
  }
  await Group.updateOne({ id: groupId }, { $unset: { name: '' } });

  return { email, groupId };
}

module.exports = { deleteOwner };