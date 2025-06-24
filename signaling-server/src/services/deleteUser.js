const User = require('../schemas/user');

async function deleteUser(decoded, email) {
  if (!decoded.isOwner) {
    throw new Error('Unauthorized: Only owners can delete users');
  }

  undercaseEmail = email.toLowerCase().trim();

  const groupId = decoded.groupId;
  const user = await User.findOneAndDelete({ email: undercaseEmail, groupId: groupId });

  if (!user) {
    throw new Error('User not found or not in group');
  }

  console.log(`Owner ${decoded.email} deleted user ${undercaseEmail} from group: ${groupId}`);
  return { message: 'User deleted' };
}

module.exports = { deleteUser };