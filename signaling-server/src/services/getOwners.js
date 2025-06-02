const Owner = require('../schemas/owner');

async function getOwners(decoded) {
  if (!decoded.isAdmin) {
    throw new Error('Unauthorized');
  }

  const owners = await Owner.find(
    {}, 
    { _id: 0, email: 1, createdAt: 1, groupId: 1, fcmToken: 1 }
  ).lean();

  console.log(`Admin ${decoded.email} retrieved ${owners.length} owners`);
  return owners;
}

module.exports = { getOwners };