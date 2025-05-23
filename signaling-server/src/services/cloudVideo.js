const Group = require('../schemas/group');
const cloudinary = require('cloudinary').v2;

console.log('Cloudinary config:', {
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET,
});

cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET,
});

const getVideos = async (groupId) => {
  
  try {

    const dbGroup = await Group.findOne({ id: groupId });
    if (!dbGroup) {
      throw new Error('Group not found');
    }
    const folder = dbGroup.cloudFolder;

    console.log(`getVideos: Fetching videos for folder: ${folder}`);
    const result = await cloudinary.api.resources({
      resource_type: 'video',
      type: 'upload',
      prefix: `${folder}/`, // Ensure exact folder
      max_results: 100,
    });
    const videos = result.resources
      .filter(video => video.public_id.startsWith(`${folder}/`))
      .map(video => ({
        public_id: video.public_id,
        name: video.public_id.split('/').pop(),
        secure_url: video.secure_url,
      }));
    console.log(`getVideos: Found ${videos.length} videos`);
    return videos;
  } catch (error) {
    console.error(`getVideos: Error: ${error.error.message}`);
    throw new Error('Failed to fetch videos');
  }
};

const deleteVideo = async (publicId) => {
  try {
    await cloudinary.uploader.destroy(publicId, { resource_type: 'video' });
    return true;
  } catch (error) {
    console.error(`Cloudinary deleteVideo error: ${error.message}`);
    throw new Error('Failed to delete video');
  }
};

module.exports = { getVideos, deleteVideo };