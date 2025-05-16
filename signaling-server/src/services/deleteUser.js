const User = require('../schemas/user');

async function deleteUser(decoded, id) {
  if (!decoded.isOwner) {
    throw new Error('Unauthorized: Only owners can delete users');
  }

  const groupId = decoded.groupId;
  const user = await User.findOneAndDelete({ id, groupId });

  if (!user) {
    throw new Error('User not found or not in group');
  }

  console.log(`Owner ${decoded.email} deleted user ${id} from group: ${groupId}`);
  return { message: 'User deleted' };
}

module.exports = { deleteUser };