const { loginUser, loginOwner } = require('../services/login');

async function handleLogin(req, res) {
  try {
    const { email, id, password, groupId, fcmToken } = req.body;
    let result;
    
    if (email && groupId && fcmToken) {
      result = await loginOwner({ email, password, groupId, fcmToken });
    } else if (id && fcmToken) {
      result = await loginUser({ id, password, fcmToken });
    } else {
      throw new Error('Missing required fields');
    }
    const { accessToken } = result;
    
    res.json({ status: "success", message: accessToken });
  } catch (error) {
    console.error(`Error in handleLogin: ${error.message}`);
    if (error.message === 'Missing required fields' || error.message === 'Invalid info') {
      res.status(400).json({
        status: "false",
        message: error.message
      });
    } else if (error.message === 'Invalid password' || error.message === 'Invalid groupId or email not authorized') {
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

module.exports = { handleLogin };