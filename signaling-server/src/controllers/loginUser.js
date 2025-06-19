const { loginUser } = require('../services/login');

async function handleLoginUser(req, res) {
  try {
    const { email, password, fcmToken } = req.body;
    let result;
    
    if (email && password) {
      result = await loginUser({ email, password, fcmToken });
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
    console.error(`Error in handleLoginUser: ${error.message}`);
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

module.exports = { handleLoginUser };