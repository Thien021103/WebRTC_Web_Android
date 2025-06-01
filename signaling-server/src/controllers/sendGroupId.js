const { sendGroupId } = require('../services/sendGroupId');

async function handleSendGroupId(req, res) {
  try {
    const { email, groupId } = req.body;
    if (!email || !groupId) {
      throw new Error('Missing required fields');
    }
    await sendGroupId(email, groupId);
    res.status(200).json({ status: "success", message: "Group ID sent to your email" });
  } catch (error) {
    console.error(`Error in handleSendGroupId: ${error.message}`);
    res.status(500).json({ status: "false", message: error.message });
  }
}

module.exports = { handleSendGroupId };