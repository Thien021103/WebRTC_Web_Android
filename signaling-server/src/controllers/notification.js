const { getNotifications } = require('../services/notification');

async function handleGetNotifications(req, res) {
  try {
    const decoded = req.user; // From authMiddleware
    const notifications = await getNotifications(decoded, req.query);
    res.json({ status: 'success', notifications });
  } catch (error) {
    console.error(`Error in handleGetNotifications: ${error.message}`);
    res.status(500).json({ status: 'false', message: error.message });
  }
}

module.exports = { handleGetNotifications };