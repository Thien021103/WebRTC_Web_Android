const Group = require('../schemas/group');
const Owner = require('../schemas/owner');

  async function addGroup({ groupId, ownerEmail, cameraId, controllerId }) {
    if (!groupId || !ownerEmail || !cameraId || !controllerId) {
      throw new Error('Missing required fields');
    }

    // Check uniqueness
    const existingGroup = await Group.findOne({
      $or: [{ id: groupId }, { cameraId }, { controllerId }],
    });
    if (existingGroup) {
      throw new Error('Group ID, camera ID, or controller ID already exists');
    }

    // Create group
    const dbGroup = new Group({
      id: groupId,
      ownerEmail: ownerEmail,
      cameraId: cameraId,
      controllerId: controllerId,
    });
    await dbGroup.save();

    return dbGroup;
  }

  module.exports = { addGroup };