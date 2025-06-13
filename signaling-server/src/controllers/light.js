const { light } = require('../services/light');

async function handleLight(req, res) {
  try {
    const decoded = req.user; // Bearer token

    await light(decoded);

    res.json({ status: "success", message: 'Light switched' });
  } catch (error) {
    console.error(`Error in handleLight: ${error.message}`);
    if (error.message === 'Camera not connected' || error.message === 'Group not found'){
      res.status(400).json({
        status: "false",
        message: error.message
      });
    } else {
      res.status(500).json({
        status: "false",
        message: error.message
      });
    }
  }
}

module.exports = { handleLight };