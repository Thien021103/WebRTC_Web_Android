const { logoutUser } = require('../services/logout');

async function handleLogout(req, res) {
  try {
    const { email, groupId, accessToken } = req.body;
    await logoutUser({ email, groupId, accessToken });
    res.json({ status: "success", message: '' });
  } catch (error) {
    console.error(`Error in handleLogout: ${error.message}`);
    if (error.message === 'Missing required fields' || error.message === 'Invalid info'){
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

module.exports = { handleLogout };