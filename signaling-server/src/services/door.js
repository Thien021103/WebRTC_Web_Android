const Group = require('../schemas/group');

async function getDoor(decoded) {

  const groupId = decoded.groupId;

  const dbGroup = await Group.findOne({ id: groupId });
  if (!dbGroup) {
    throw new Error('Group not found');
  }

  if (dbGroup.door) {
    return dbGroup.door;
  } else {
    let door = {
      lock: 'Unknown',
      user: 'Unknown',
      time: 'Unknown'
    };
    return door;
  }

}

module.exports = { getDoor };