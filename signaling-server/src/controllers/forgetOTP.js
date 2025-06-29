const { forgetUserOTP, forgetOwnerOTP } = require('../services/forgetOTP');

  async function handleForgetOTP(req, res) {
    try {
      const { email, groupId } = req.body;
      if (!email) {
        throw new Error('Missing required fields');
      }
      if (groupId) {
        await forgetOwnerOTP({ email, groupId });
      }
      else {
        await forgetUserOTP(email);
      }
      res.status(200).json({ status: "success", message: "OTP sent to your email" });
    } catch (error) {
      console.error(`Error in handleRegisterOTP: ${error.message}`);

      if (error.message === 'Missing required fields' || error.message === 'Invalid groupId or email' || error.message === 'Invalid email') {
        res.status(400).json({ status: 'false', message: error.message });
      } else {
        res.status(500).json({ status: 'false', message: error.message });
      }
    }
  }

module.exports = { handleForgetOTP };