const { addGroup } = require('../services/addGroup');

async function handleAddGroup(req, res) {
  try {
    const { ownerEmail } = req.body;
    await addGroup({ ownerEmail });
    res.status(201).json({ status: 'success', message: 'Group created successfully' });
  } catch (error) {
    console.error(`Error in handleAddGroup: ${error.message}`);
    if (error.message === 'Missing required fields' || error.message === 'Owner not found' || error.message === 'Group ID, camera ID, or controller ID already exists') {
      res.status(400).json({ status: 'false', message: error.message });
    } else {
      res.status(500).json({ status: 'false', message: error.message });
    }
  }
}

module.exports = { handleAddGroup };