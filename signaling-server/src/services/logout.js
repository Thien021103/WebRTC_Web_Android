const User = require('../schemas/user');
const Group = require('../schemas/group');
const { groups, notifyStateUpdate } = require('../groups/groups');

async function logoutUser({ email, groupId, accessToken }) {
  if (!email || !groupId || !accessToken) {
    throw new Error('Missing required fields');
  }

  const user = await User.findOne({ email, groupId, accessToken });
  if (!user) {
    throw new Error('Invalid info');
  }

  await User.updateOne({ email, groupId }, { $unset: { accessToken: '' } });

  const group = groups.get(groupId);
  if (group) {
    group.clients.user = null;
    group.state = 'Impossible';
    notifyStateUpdate(groupId);
    if (!group.clients.camera && !group.clients.user && !group.clients.controller) {
      groups.delete(groupId);
      await Group.deleteOne({ id: groupId });
    }
  }

  await Group.updateOne({ id: groupId }, { $set: { state: 'Impossible' } });

  console.log(`User logged out: ${email}, group: ${groupId}`);
  return {};
}

module.exports = { logoutUser };