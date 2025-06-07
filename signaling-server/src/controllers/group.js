const { getGroup } = require("../services/group");

async function handleGetGroup(req, res) {
  try {
    const decoded = req.user; // From authMiddleware
    const group = await getGroup(decoded);
    res.json({ status: "success", group });
  } catch (error) {
    console.error(`Error in handleGetGroup: ${error.message}`);
    res.status(500).json({
      status: "false",
      message: error.message
    });
  }
}

module.exports = { handleGetGroup };