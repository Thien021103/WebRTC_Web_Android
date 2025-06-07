const { changePassword } = require('../services/changePassword');

async function handleChangePassword(req, res) {
  try {
    const { password, newPassword } = req.body;
    const decoded = req.user; // From authMiddleware

    if (password && newPassword) {
      await changePassword({ password, newPassword, decoded });
    } else {
      throw new Error('Missing required fields');
    }
  
    res.status(201).json({ 
      status: "success", 
      message: "Password changed successfully", 
    });
  } catch (error) {
    console.error(`Error in handleChangePassword: ${error.message}`);
    if (error.message === 'Missing required fields' || error.message === 'Invalid info') {
      res.status(400).json({
        status: "false",
        message: error.message
      });
    } else if (error.message === 'Invalid password') {
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

module.exports = { handleChangePassword };