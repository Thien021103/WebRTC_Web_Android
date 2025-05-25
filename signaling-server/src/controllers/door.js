const { getDoor } = require("../services/door");

async function handleGetDoor(req, res) {
  try {
    const decoded = req.user; // From authMiddleware
    const door = await getDoor(decoded);
    res.json({ status: "success", door });
  } catch (error) {
    console.error(`Error in handleGetDoor: ${error.message}`);
    res.status(500).json({
      status: "false",
      message: error.message
    });
  }
}

module.exports = { handleGetDoor };