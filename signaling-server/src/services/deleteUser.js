const User = require('../schemas/user');

async function deleteUser(decoded, email) {
  if (!decoded.isOwner) {
    throw new Error('Unauthorized: Only owners can delete users');
  }

  const groupId = decoded.groupId;
  const user = await User.findOneAndDelete({ email: email, groupId: groupId });

  if (!user) {
    throw new Error('User not found or not in group');
  }

  console.log(`Owner ${decoded.email} deleted user ${email} from group: ${groupId}`);
  return { message: 'User deleted' };
}

module.exports = { deleteUser };