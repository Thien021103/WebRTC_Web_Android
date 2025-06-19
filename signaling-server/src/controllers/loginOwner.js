const { loginOwner } = require('../services/login');

async function handleLoginOwner(req, res) {
  try {
    const { email, password, groupName, fcmToken } = req.body;
    let result;

    if (email && groupName && password) {
      result = await loginOwner({ email, password, groupName, fcmToken });
    } else {
      throw new Error('Missing required fields');
    }
    const { accessToken, cloudFolder } = result;
  
    res.status(201).json({ 
      status: "success", 
      message: accessToken, 
      cloudFolder: cloudFolder,
      cloudName: process.env.CLOUDINARY_CLOUD_NAME
    });
  } catch (error) {
    console.error(`Error in handleLoginOwner: ${error.message}`);
    if (error.message === 'Missing required fields' || error.message === 'Invalid info') {
      res.status(400).json({
        status: "false",
        message: error.message
      });
    } else if (error.message === 'Invalid password' || error.message === 'Invalid groupName or email not authorized') {
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

module.exports = { handleLoginOwner };