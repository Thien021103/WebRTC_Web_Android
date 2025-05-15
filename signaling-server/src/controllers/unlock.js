const { unlockGroup } = require('../services/unlock');

async function handleUnlock(req, res) {
  try {
    const { email, password, groupId } = req.body;
    await unlockGroup({ email, password, groupId });
    res.json({ status: "success", message: 'Unlocked' });
  } catch (error) {
    console.error(`Error in handleUnlock: ${error.message}`);
    if (error.message === 'Missing required fields' || error.message === 'Invalid info' || error.message === 'Group not found'){
      res.status(400).json({
        status: "false",
        message: error.message
      });
    } else if (error.message === 'Invalid password' || error.message === 'Already unlocked'){
      res.status(401).json({
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

module.exports = { handleUnlock };