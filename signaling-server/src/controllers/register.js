const { registerUser } = require('../services/registerUser');
const { registerOwner } = require('../services/registerOwner');

async function handleRegister(req, res) {
  try {
    const { userName, email, password, groupId, groupName, otp, fcmToken, ownerToken } = req.body;

    if ( email && groupId && groupName && otp && password && !ownerToken ) {
      const { accessToken, cloudFolder } = await registerOwner({ email, password, groupId, groupName, otp, fcmToken });
      res.status(201).json({ 
        status: "success", 
        message: accessToken,
        cloudFolder: cloudFolder,
        cloudName: process.env.CLOUDINARY_CLOUD_NAME,
      });
    } else if ( email && userName && ownerToken ) {
      await registerUser({ userName, email, password, ownerToken });
      res.status(201).json({ 
        status: "success",
        message: "User registered" 
      });
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
    } else if (error.message === 'Invalid or expired token' || error.message === 'Unauthorized'){
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