const mongoose = require('mongoose');

const groupSchema = new mongoose.Schema({
  id: { type: String, required: true, unique: true },
  ownerEmail: { type: String, required: true} ,
  deviceId: { type: String, unique: true },
  state: { type: String, required: true, enum: ['Impossible', 'Ready'], default: 'Impossible' },
  fcmToken: { type: String, default: '' },
  door: {
    lock: { type: String, enum: ['Locked', 'Unlocked'], default: 'Locked' },
    user: { type: String },
    time: { type: Date }
  }
}, { timestamps: true });

module.exports = mongoose.model('Group', groupSchema);