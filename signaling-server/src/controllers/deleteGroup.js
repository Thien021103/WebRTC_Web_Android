const { deleteGroup } = require('../services/deleteGroup');

async function handleDeleteGroup(req, res) {
  try {
    const { groupId, password } = req.body;
    const decoded = req.user; // From authMiddleware
    await deleteGroup({ groupId, password, decoded });
    res.json({ status: 'success', message: 'Group successfully deleted' });
  } catch (error) {
    console.error(`Error in handleDeleteGroup: ${error.message}`);
    if (
      error.message === 'Missing required fields' || error.message === 'Group not found' || 
      error.message === 'Invalid admin password' || error.message === 'Admin not found') {
      res.status(400).json({ status: 'false', message: error.message });
    } else {
      res.status(500).json({ status: 'false', message: error.message });
    }
  }
}

module.exports = { handleDeleteGroup };