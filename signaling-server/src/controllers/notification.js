const { getNotifications } = require('../services/notification');

async function handleGetNotifications(req, res) {
  try {
    const { groupId, startDate, endDate, limit, page } = req.query;
    const decoded = req.user; // From authMiddleware

    // Ensure user/owner belongs to the requested group
    if (groupId !== decoded.groupId) {
      throw new Error('Not authorized for this group');
    }

    const notifications = await getNotifications({ groupId, startDate, endDate, limit, page });
    res.json({ status: 'success', notifications });
  } catch (error) {
    console.error(`Error in handleGetNotifications: ${error.message}`);
    res.status(500).json({ status: 'false', message: error.message });
  }
}

module.exports = { handleGetNotifications };