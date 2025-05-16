const { logoutUser, logoutOwner } = require('../services/logout');

async function handleLogout(req, res) {
  try {
    const { id, email, groupId } = req.body;
    const accessToken = req.headers.authorization.split(' ')[1]; // Bearer token

    if (email && groupId) {
      await logoutOwner({ email, groupId, accessToken });
    } else if (id) {
      await logoutUser({ id, accessToken });
    } else {
      throw new Error('Missing required fields');
    }

    res.json({ status: "success", message: 'Logged out' });
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