const { deleteUser } = require("../services/deleteUser");

async function handleDeleteUser(req, res) {
  try {
    const decoded = req.user; // From authMiddleware
    const { email } = req.body;
    if (!email) {
      throw new Error('Missing user ID');
    }
    const result = await deleteUser(decoded, email);
    res.json({ status: "success", message: result.message });
  } catch (error) {
    console.error(`Error in handleDeleteUser: ${error.message}`);
    res.status(error.message.includes('Unauthorized') || error.message.includes('User not found') ? 401 : 500).json({
      status: "false",
      message: error.message
    });
  }
}

module.exports = { handleDeleteUser };