const Group = require('../schemas/group');

async function getGroups(decoded) {
  if (!decoded.isAdmin) {
    throw new Error('Unauthorized');
  }

  const groups = await Group.find(
    {}, 
    { _id: 0, id: 1, createdAt: 1 }
  ).lean();

  console.log(`Admin ${decoded.email} retrieved ${groups.length} groups`);
  return groups;
}

module.exports = { getGroups };