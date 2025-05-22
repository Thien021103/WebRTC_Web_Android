const cloudinary = require('cloudinary').v2;

cloudinary.config({
  cloud_name: "dvarse6wk",
  api_key: "573435389774623",
  api_secret: "CZmauvR9SiOsysGNak67f9DVTjc",
});

const getVideos = async (folder) => {
  try {
    const result = await cloudinary.api.resources({
      resource_type: 'video',
      prefix: `${folder}/`,
      max_results: 100,
    });
    return result 
    ? result.resources.map(video => ({
      public_id: video.public_id,
      name: video.public_id.split('/').pop(), // Extract file name
      secure_url: video.secure_url,
    }))
    : {public_id: "",name: "", secure_url:""};
  } catch (error) {
    console.error(`Cloudinary getVideos error: ${error.message}`);
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