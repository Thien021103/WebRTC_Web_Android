const Group = require('../schemas/group');
const { groups, notifyStateUpdate } = require('../groups/groups');

// Handle disconnect
async function handleDisconnect(client) {
  if (!client?._groupId) return;
  const group = groups.get(client._groupId);
  if (!group) return;

  const groupId = client._groupId;
  const type = client._type;

  group.clients[type] = null;

  // Clear tokens for camera/controller
  if (type === 'camera') {
    await Group.updateOne({ id: groupId }, { $unset: { cameraToken: '' } });
  } else if (type === 'controller') {
    await Group.updateOne({ id: groupId }, { $unset: { controllerToken: '' } });
  }

  const { camera, user, controller } = group.clients;
  if (!camera || !user || !controller) {
    group.state = 'Impossible';
    try {
      await Group.updateOne({ id: groupId }, { $set: { state: 'Impossible' } });
      console.log(`Updated group ${groupId} state to Impossible`);
    } catch (error) {
      console.error(`Error updating group state: ${error.message}`);
    }
    notifyStateUpdate(groupId);
  }

  if (!camera && !user && !controller) {
    groups.delete(groupId);
    console.log(`Deleted group ${groupId} from local groups`);
  }
}

module.exports = { handleConnect, handleDisconnect };