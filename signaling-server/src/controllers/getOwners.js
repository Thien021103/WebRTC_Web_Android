const { getOwners } = require("../services/getOwners");

async function handleGetOwners(req, res) {
  try {
    const decoded = req.user; // From authMiddleware
    const owners = await getOwners(decoded);
    res.json({ status: "success", owners });
  } catch (error) {
    console.error(`Error in handleGetOwners: ${error.message}`);
    res.status(error.message.includes('Unauthorized') ? 401 : 500).json({
      status: "false",
      message: error.message
    });
  }
}

module.exports = { handleGetOwners };