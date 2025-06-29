const { resetOwnerPassword, resetUserPassword } = require("../services/resetPassword");

async function handleResetPassword(req, res) {
  try {
    const { email, password, groupId, otp } = req.body;
    if (!email || !password || !otp) {
      throw new Error('Missing required fields');
    }
    if (groupId) {
      await resetOwnerPassword({ email, groupId, password, otp });
    } else {
      await resetUserPassword({ email, password, otp });
    }
    res.status(200).json({ status: 'success', message: 'Password updated' });
  } catch (error) {
    console.error(`Error in handleResetPassword: ${error.message}`);
    if (
      error.message === 'Missing required fields' ||
      error.message === 'Invalid email format' ||
      error.message === 'User not found' ||
      error.message === 'Owner not found for this group' ||
      error.message === 'Invalid groupId or email' ||
      error.message === 'Invalid or expired OTP'
    ) {
      res.status(400).json({ status: 'false', message: error.message });
    } else {
      res.status(500).json({ status: 'false', message: error.message });
    }
  }
}

module.exports = { handleResetPassword };