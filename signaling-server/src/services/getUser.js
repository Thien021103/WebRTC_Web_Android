const User = require('../schemas/user');

async function getUsersInGroup(decoded) {
  if (!decoded.isOwner) {
    throw new Error('Unauthorized');
  }

  const groupId = decoded.groupId;
  const users = await User.find(
    { groupId }, 
    { _id: 0, name: 1, email: 1, groupId: 1, createdAt: 1 }
  ).lean();

  console.log(`Owner ${decoded.email} retrieved ${users.length} users for group: ${groupId}`);
  return users;
}

module.exports = { getUsersInGroup };