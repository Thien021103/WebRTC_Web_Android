const { MongoClient } = require('mongodb');

const mongoUrl = 'mongodb://thien:881199@mongodb:27017';
const dbName = 'mydb';

let db;

async function connect(retries = 5, delay = 2000) {
  const client = new MongoClient(mongoUrl);
  for (let i = 0; i < retries; i++) {
    try {
      await client.connect();
      console.log('Connected to MongoDB');
      db = client.db(dbName);
      db.collection("users").updateOne()
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

function getDb() {
  if (!db) throw new Error('MongoDB not connected');
  return db;
}

module.exports = { connect, getDb };