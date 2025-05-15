  const mongoose = require('mongoose');

  const userSchema = new mongoose.Schema({
    email: { type: String, required: true, unique: true },
    password: { type: String, required: true },
    groupId: { type: String, required: true },
    accessToken: { type: String },
    fcmToken: { type: String },
  }, { timestamps: true });

  module.exports = mongoose.model('User', userSchema);