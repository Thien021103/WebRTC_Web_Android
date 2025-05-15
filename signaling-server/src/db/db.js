const mongoose = require('mongoose');

const mongoUrl = 'mongodb://thien:881199@mongodb:27017/mydb?authSource=admin';

async function connect(retries = 5, delay = 2000) {
  for (let i = 0; i < retries; i++) {
    try {
      await mongoose.connect(mongoUrl);
      console.log('Connected to MongoDB');
      return;
    } catch (error) {
      console.error(`MongoDB connection attempt ${i + 1} failed:`, error.message);
      if (i < retries - 1) {
        await new Promise((resolve) => setTimeout(resolve, delay));
      }
    }
  }
  console.error('Failed to connect to MongoDB after retries');
  process.exit(1);
}

module.exports = { connect, mongoose };