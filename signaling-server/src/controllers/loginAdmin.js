const { loginAdmin } = require('../services/loginAdmin');

async function handleAdminLogin(req, res) {
  try {
    const { email, password } = req.body;
    let result;
    
    if (email && password ) {
      result = await loginAdmin({ email, password });
    } else {
      throw new Error('Missing required fields');
    }
    const { accessToken } = result;
  
    res.status(201).json({ 
      status: "success", 
      message: accessToken, 
    });
  } catch (error) {
    console.error(`Error in handleAdminLogin: ${error.message}`);
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

module.exports = { handleAdminLogin };