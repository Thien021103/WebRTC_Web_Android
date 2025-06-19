const { v4: uuidv4 } = require('uuid');
const Group = require('../schemas/group');
const Owner = require('../schemas/owner');

async function addGroup({ ownerEmail }) {
  if (!ownerEmail) {
    throw new Error('Missing required fields');
  }

  // Generate unique IDs and secrets
  let groupId, cameraId, controllerId, cloudFolder;
  let isUnique = false;

  while (!isUnique) {
    groupId = uuidv4();
    cameraId = uuidv4();
    controllerId = uuidv4();
    cloudFolder = uuidv4(); // Generate UUID for cloudFolder

    // Check uniqueness
    const existingGroup = await Group.findOne({
      $or: [{ id: groupId }, { cameraId }, { controllerId }],
    });
    if (!existingGroup) {
      isUnique = true;
    }
  }

  // Create group
  const dbGroup = new Group({
    id: groupId,
    ownerEmail: ownerEmail,
    cameraId: cameraId,
    controllerId: controllerId,
    cloudFolder: cloudFolder,
  });
  await dbGroup.save();

  return dbGroup;
}

module.exports = { addGroup };