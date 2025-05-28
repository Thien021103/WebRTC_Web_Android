const mongoose = require('mongoose');

const adminSchema = new mongoose.Schema({
  email: { type: String, required: true, unique: true },
  password: { type: String, required: true },
  accessToken: { type: String },
}, { timestamps: true });

module.exports = mongoose.model('Admin', adminSchema);