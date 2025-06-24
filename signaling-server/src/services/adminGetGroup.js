const { WebSocket } = require('ws');
const { groups } = require('../groups/groups');
const Group = require('../schemas/group');

async function adminGetGroup(groupId, decoded) {

  const dbGroup = await Group.findOne({ id: groupId });
  if (!dbGroup) {
    throw new Error('Group not found');
  }

  const group = groups.get(groupId);
  console.log(`Admin ${decoded.email} retrieved group info: ${groupId}`);

  if(!group) {
    return {
      owner: dbGroup.ownerEmail,
      state: "Impossible",
      camera: "Disconnected",
      controller: "Disconnected",
    }
  }
  return {
    owner: dbGroup.ownerEmail,
    state: group.state,
    camera: (group.clients.camera?.readyState === WebSocket.OPEN)
      ? "Connected" : "Disconnected",
    controller: (group.clients.controller?.readyState === WebSocket.OPEN)
      ? "Connected" : "Disconnected",
  }
}

module.exports = { adminGetGroup };