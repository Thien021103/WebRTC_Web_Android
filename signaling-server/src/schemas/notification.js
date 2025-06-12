const mongoose = require('mongoose');

const notificationSchema = new mongoose.Schema({
  groupId: { type: String, required: true, index: true },
  time: { type: Date, required: true, default: () => new Date(Date.now() + 7 * 60 * 60 * 1000) }, // Vietnam time (UTC+7)
}, { timestamps: true });

// Index for efficient querying by groupId and time
notificationSchema.index({ groupId: 1, time: -1 });

module.exports = mongoose.model('Notification', notificationSchema);