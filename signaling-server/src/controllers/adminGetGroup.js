const { adminGetGroup } = require("../services/adminGetGroup");

async function handleAdminGetGroup(req, res) {
  try {
    const { groupId } = req.body;
    const decoded = req.user; // From authMiddleware
    if (groupId) {
      const group = await adminGetGroup(groupId, decoded);
      res.json({ status: "success", group });
    } else {
      throw new Error('Missing required fields');
    }
  } catch (error) {
    console.error(`Error in handleAdminGetGroup: ${error.message}`);
    res.status(500).json({
      status: "false",
      message: error.message
    });
  }
}

module.exports = { handleAdminGetGroup };