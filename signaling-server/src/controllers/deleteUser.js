const { deleteUser } = require("../services/deleteUser");

async function handleDeleteUser(req, res) {
  try {
    const decoded = req.user; // From authMiddleware
    const { id } = req.body;
    if (!id) {
      throw new Error('Missing user ID');
    }
    const result = await deleteUser(decoded, id);
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