const mongoose = require('mongoose');

const groupSchema = new mongoose.Schema({

  id: { type: String, required: true, unique: true },

  ownerEmail: { type: String, required: true },

  cameraId: { type: String, unique: true, required: true },
  cameraToken: { type: String },

  controllerId: { type: String, unique: true },
  controllerToken: { type: String },

  state: { type: String, required: true, enum: ['Impossible', 'Ready'], default: 'Impossible' },
  
  cloudFolder: { type: String, unique: true, required: true },

  door: {
    lock: { type: String, enum: ['Locked', 'Unlocked'], default: 'Locked' },
    user: { type: String },
    time: { type: Date }
  }
}, { timestamps: true });

module.exports = mongoose.model('Group', groupSchema);