const mongoose = require('mongoose');

const doorSchema = new mongoose.Schema({
  groupId: { type: String, required: true, index: true },
  state: { type: String, enum: ['Locked', 'Unlocked'], required: true },
  user: { type: String, default: null },
  time: { type: Date, required: true, default: () => new Date(Date.now() + 7 * 60 * 60 * 1000) }
}, { timestamps: true });

// Compound index for efficient querying by groupId and timestamp
doorSchema.index({ groupId: 1, timestamp: -1 });

module.exports = mongoose.model('Door', doorSchema);