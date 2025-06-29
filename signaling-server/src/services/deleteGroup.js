const bcrypt = require('bcrypt');
const Group = require('../schemas/group');
const User = require('../schemas/user');
const Owner = require('../schemas/owner');
const Notification = require('../schemas/notification');
const Admin = require('../schemas/admin');
const cloudinary = require('cloudinary').v2;

cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET,
});

async function deleteGroup({ groupId, password, decoded }) {
  if (!groupId || !password || !decoded) {
    throw new Error('Missing required fields');
  }

  // Validate group
  const group = await Group.findOne({ id: groupId });
  if (!group) {
    throw new Error('Group not found');
  }

  // Verify password
  const dbAdmin = await Admin.findOne({ email: decoded.email, groupId });
  if (!dbAdmin) {
    throw new Error('Admin not found for this group');
  }
  const isPasswordValid = await bcrypt.compare(password, dbAdmin.password);
  if (!isPasswordValid) {
    throw new Error('Invalid admin password');
  }

  // Delete all videos in the group's cloudFolder
  if (group.cloudFolder) {
    try {
      await cloudinary.api.delete_resources_by_prefix(`${group.cloudFolder}/`, {
        resource_type: 'video',
      });
      console.log(`Deleted videos in folder: ${group.cloudFolder}`);
    } catch (error) {
      console.error(`Failed to delete videos in ${group.cloudFolder}: ${error.message}`);
      // Continue deletion even if Cloudinary fails
    }
  }

  // Delete associated data
  await User.deleteMany({ groupId });
  await Owner.deleteMany({ groupId });
  await Notification.deleteMany({ groupId });
  await Group.deleteOne({ id: groupId });

  console.log(`Group deleted: ${groupId}`);
  return { groupId };
}

module.exports = { deleteGroup };