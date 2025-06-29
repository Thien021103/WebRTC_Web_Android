const { registerOTP } = require('../services/registerOTP');

  async function handleRegisterOTP(req, res) {
    try {
      const { email, groupId } = req.body;
      if (!email || !groupId) {
        throw new Error('Missing required fields');
      }
      await registerOTP(email, groupId);

      res.status(200).json({ status: "success", message: "OTP sent to your email" });
    } catch (error) {
      console.error(`Error in handleRegisterOTP: ${error.message}`);

      if (error.message === 'Missing required fields' || error.message === 'Invalid groupId or email' || error.message === 'Unauthorized') {
        res.status(400).json({ status: 'false', message: error.message });
      } else {
        res.status(500).json({ status: 'false', message: error.message });
      }
    }
  }

module.exports = { handleRegisterOTP };