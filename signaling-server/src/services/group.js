const { WebSocket } = require('ws');
const { groups } = require('../groups/groups');
const Group = require('../schemas/group');

async function getGroup(decoded) {

  const groupId = decoded.groupId;

  const dbGroup = await Group.findOne({ id: groupId });
  if (!dbGroup) {
    throw new Error('Group not found');
  }

  const group = groups.get(groupId);
  if(!group) {
    throw new Error('Group not connected');
  }
  console.log(`User ${decoded.email} retrieved group info: ${groupId}`);
  return {
    name: dbGroup.name,
    state: group.state,
    camera: (group.clients.camera.readyState === WebSocket.OPEN)
      ? "Connected" : "Disconnected",
    controller: (group.clients.controller.readyState === WebSocket.OPEN)
      ? "Connected" : "Disconnected",
  }
}

module.exports = { getGroup };