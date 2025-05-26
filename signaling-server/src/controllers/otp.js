const { getOTP } = require('../services/getOTP');

  async function handleSendOTP(req, res) {
    try {
      const { email, groupId } = req.body;
      if (!email || !groupId) {
        throw new Error('Missing required fields');
      }
      await getOTP(email, groupId);

      res.status(200).json({ status: "success", message: "OTP sent to your email" });
    } catch (error) {
      console.error(`Error in handleSendOTP: ${error.message}`);

      res.status(500).json({ status: "false", message: error.message });
    }
  }

module.exports = { handleSendOTP };