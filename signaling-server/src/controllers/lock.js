const { lock } = require('../services/lock');

async function handleLock(req, res) {
  try {
    const { email, password } = req.body;
    const decoded = req.user; // Bearer token

    if (email) {
      await lock({ identifier: email, password: password, decoded: decoded });
    } else {
      throw new Error('Missing required fields');
    }

    res.json({ status: "success", message: 'Locked' });
  } catch (error) {
    console.error(`Error in handleLock: ${error.message}`);
    if (error.message === 'Missing required fields' || error.message === 'Invalid info' || error.message === 'Group not found'){
      res.status(400).json({
        status: "false",
        message: error.message
      });
    } else if (error.message === 'Invalid password' || error.message === 'Already locked'){
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

module.exports = { handleLock };