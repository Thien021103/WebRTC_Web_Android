const { getVideos, deleteVideo } = require("../services/cloudVideo");

const handleGetVideos = async (req, res) => {
  try {
    const { cloudFolder } = req.body;
    const videos = await getVideos(cloudFolder);
    res.json({ status: "success", videos });
  } catch (error) {
    console.error(`Error in handleGetVideos: ${error.message}`);
    res.status(500).json({ status: "false", message: error.message });
  }
};

const handleDeleteVideo = async (req, res) => {
  try {
    const { isOwner } = req.user;
    if (!isOwner) {
      return res.status(403).json({ status: "false", message: 'Unauthorized' });
    }
    const { publicId } = req.body;
    if (!publicId) {
      return res.status(400).json({ status: "false", message: 'Missing publicId' });
    }
    await deleteVideo(publicId);
    res.json({ status: "success", message: 'Video deleted successfully' });
  } catch (error) {
    console.error(`Error in handleDeleteVideo: ${error.message}`);
    res.status(500).json({ status: "false", message: error.message });
  }
};

module.exports = { handleGetVideos, handleDeleteVideo };