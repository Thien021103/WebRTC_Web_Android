const { deleteOwner } = require('../services/deleteOwner');

async function handleDeleteOwner(req, res) {
  try {
    const { email, groupId } = req.body;
    await deleteOwner({ email, groupId });
    res.status(200).json({ status: 'success', message: 'Owner deleted successfully' });
  } catch (error) {
    console.error(`Error in handleDeleteOwner: ${error.message}`);
    if (error.message === 'Missing required fields' || error.message === 'Group not found' || error.message === 'Owner not found or does not belong to the specified group') {
      res.status(400).json({ status: 'false', message: error.message });
    } else {
      res.status(500).json({ status: 'false', message: error.message });
    }
  }
}

module.exports = { handleDeleteOwner };