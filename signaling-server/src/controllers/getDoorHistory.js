const { getDoorHistory } = require("../services/getDoorHistory");

async function handleGetDoorHistory(req, res) {
  try {
    const decoded = req.user; // From authMiddleware
    const history = await getDoorHistory(decoded, req.query);
    res.json({ status: "success", history });
  } catch (error) {
    console.error(`Error in handleGetDoorHistory: ${error.message}`);
    res.status(500).json({
      status: "false",
      message: error.message
    });
  }
}

module.exports = { handleGetDoorHistory };