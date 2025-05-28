const { getGroups } = require('../services/getGroups');

async function handleGetGroups(req, res) {
  try {
    const decoded = req.user; // From authMiddleware
    const users = await getGroups(decoded);
    res.json({ status: "success", users });
  } catch (error) {
    console.error(`Error in handleGetUsers: ${error.message}`);
    res.status(error.message.includes('Unauthorized') ? 401 : 500).json({
      status: "false",
      message: error.message
    });
  }
}

module.exports = { handleGetGroups };