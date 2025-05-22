const cloudinary = require('cloudinary').v2;

cloudinary.config({
  cloud_name: "dvarse6wk",
  api_key: "573435389774623",
  api_secret: "CZmauvR9SiOsysGNak67f9DVTjc",
});

const getVideos = async (folder) => {
  try {
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