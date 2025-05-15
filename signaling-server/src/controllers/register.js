const { registerUser } = require('../services/register');

async function handleRegister(req, res) {
  try {
    const { email, password, groupId, fcmToken } = req.body;
    const { accessToken } = await registerUser({ email, password, groupId, fcmToken });
    res.status(201).json({ status: "success", message: accessToken });
  } catch (error) {
    console.error(`Error in handleRegister: ${error.message}`);
    if (error.message === 'Missing required fields' || error.message === 'Email already registered'){
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

module.exports = { handleRegister };