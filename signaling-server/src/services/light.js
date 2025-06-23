const WebSocket = require('ws');

const Group = require('../schemas/group');
const { groups } = require('../groups/groups');

async function light(decoded) {

  const groupId = decoded.groupId;
  const identifier = decoded.email;


  const dbGroup = await Group.findOne({ id: groupId });
  if (!dbGroup) {
    throw new Error('Camera not connected');
  }

  // Notify camera
  const group = groups.get(groupId);
  if (!group) {
    throw new Error('Camera not connected');
  }
  const camera = group.clients.camera;
  if (!camera) {
    throw new Error('Camera not connected');
  } else if (camera.readyState === WebSocket.OPEN) {
    camera.send(`LIGHT`);
  }

  console.log(`User/Owner ${identifier} lighted group: ${dbGroup.name}`);
  return {};
}

module.exports = { light };