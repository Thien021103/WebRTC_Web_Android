const { loginUser } = require('../services/login');

async function handleLogin(req, res) {
  try {
    const { email, password, groupId, fcmToken } = req.body;
    const { accessToken } = await loginUser({ email, password, groupId, fcmToken });
    res.json({ status: "success", message: accessToken });
  } catch (error) {
    console.error(`Error in handleLogin: ${error.message}`);
    if (error.message === 'Missing required fields' || error.message === 'Invalid info'){
      res.status(400).json({
        status: "false",
        message: error.message
      });
    } else if (error.message === 'Invalid password'){
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