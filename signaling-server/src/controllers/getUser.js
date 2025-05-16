const { getUsersInGroup } = require('../services/getUser');

async function handleGetUsers(req, res) {
  try {
    const decoded = req.user; // From authMiddleware
    const users = await getUsersInGroup(decoded);
    res.json({ status: "success", users });
  } catch (error) {
    console.error(`Error in handleGetUsers: ${error.message}`);
    res.status(error.message.includes('Unauthorized') ? 401 : 500).json({
      status: "false",
      message: error.message
    });
  }
}

module.exports = { handleGetUsers };