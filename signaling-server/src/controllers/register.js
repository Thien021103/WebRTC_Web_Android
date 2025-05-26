const { registerUser } = require('../services/registerUser');
const { registerOwner } = require('../services/registerOwner');

async function handleRegister(req, res) {
  try {
    const { id, email, password, groupId, otp, fcmToken, ownerToken } = req.body;

    if ( email && groupId && otp && fcmToken ) {
      const { accessToken, cloudFolder } = await registerOwner({ email, password, groupId, otp, fcmToken });
      res.status(201).json({ 
        status: "success", 
        message: accessToken,
        cloudFolder: cloudFolder,
        cloudName: process.env.CLOUDINARY_CLOUD_NAME,
        cloudKey: process.env.CLOUDINARY_API_KEY,
        cloudSec: process.env.CLOUDINARY_API_SECRET
      });
    } else if ( id && ownerToken ) {
      await registerUser({ id, password, ownerToken });
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