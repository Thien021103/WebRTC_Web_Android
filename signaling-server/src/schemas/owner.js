const mongoose = require('mongoose');

const ownerSchema = new mongoose.Schema({
  email: { type: String, required: true },
  password: { type: String, required: true },
  groupId: { type: String, required: true, unique: true },
  accessToken: { type: String },
  fcmToken: { type: String },
}, { timestamps: true });

module.exports = mongoose.model('Owner', ownerSchema);