const { registerOwner, registerUser } = require('../services/register');

async function handleRegister(req, res) {
  try {
    const { id, email, password, groupId, fcmToken, ownerToken } = req.body;

    if (email && groupId && fcmToken) {
      const { accessToken } = await registerOwner({ email, password, groupId, fcmToken });
      res.status(201).json({ status: "success", message: accessToken });
    } else if (id && groupId && ownerToken) {
      await registerUser({ id, password, ownerToken });
      res.status(201).json({ status: "success", message: "User registered" });
    } else {
      throw new Error('Missing required fields');
    }
    
  } catch (error) {
    console.error(`Error in handleRegister: ${error.message}`);
    if (error.message === 'Missing required fields' || error.message === 'Email already registered' || error.message === 'User ID already registered'){
      res.status(400).json({
        status: "false",
        message: error.message
      });
    } else if (error.message === 'Invalid or expired token' || error.message === 'Unauthorized' || error.message === 'GroupId already owned'){
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

module.exports = { handleRegister };