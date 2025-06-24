const { WebSocket } = require('ws');
const { groups } = require('../groups/groups');
const Group = require('../schemas/group');
const Owner = require('../schemas/owner');
const e = require('express');

async function adminGetGroup(groupId, decoded) {

  let registered = "No"

  const dbGroup = await Group.findOne({ id: groupId });
  if (!dbGroup) {
    throw new Error('Group not found');
  }

  const owner = await Owner.findOne({ groupId: groupId });
  if (owner) {
    registered = "Yes";
  } else {
    registered = "No";
  }

  const group = groups.get(groupId);
  console.log(`Admin ${decoded.email} retrieved group info: ${groupId}`);

  if(!group) {
    return {
      owner: dbGroup.ownerEmail,
      registered: registered,
      state: "Impossible",
      camera: "Disconnected",
      controller: "Disconnected",
    }
  }
  return {
    owner: dbGroup.ownerEmail,
    registered: registered,
    state: group.state,
    camera: (group.clients.camera?.readyState === WebSocket.OPEN)
      ? "Connected" : "Disconnected",
    controller: (group.clients.controller?.readyState === WebSocket.OPEN)
      ? "Connected" : "Disconnected",
  }
}

module.exports = { adminGetGroup };