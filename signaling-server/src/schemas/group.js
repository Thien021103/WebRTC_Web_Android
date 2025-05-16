const mongoose = require('mongoose');

const groupSchema = new mongoose.Schema({

  id: { type: String, required: true, unique: true },

  ownerEmail: { type: String, required: true },

  cameraId: { type: String, unique: true, required: true },
  cameraToken: { type: String },
  cameraSecret: { type: String, required: true },

  controllerId: { type: String, unique: true, required: true },
  controllerToken: { type: String },
  controllerSecret: { type: String, required: true },

  state: { type: String, required: true, enum: ['Impossible', 'Ready'], default: 'Impossible' },
  
  door: {
    lock: { type: String, enum: ['Locked', 'Unlocked'], default: 'Locked' },
    user: { type: String },
    time: { type: Date }
  }
}, { timestamps: true });

module.exports = mongoose.model('Group', groupSchema);