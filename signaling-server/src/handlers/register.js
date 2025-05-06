const { getDb } = require('../db/db');
const { groups } = require("../groups/groups");
const bcrypt = require('bcrypt');
const { v4: uuidv4 } = require('uuid');

// Hàm tạo chuỗi số ngẫu nhiên dài 10 ký tự
function generateAccessToken() {
  let token = 'token';
  for (let i = 0; i < 10; i++) {
    token += Math.floor(Math.random() * 10);
  }
  token += 'token';
  return token;
}

async function handleRegister(req, res) {
  // // Sử dụng regex để bóc tách message
  // const match = message.match(/^REGISTER (\w+) (\w+)\n([\s\S]*)$/);
  // if (!match) {
  //   console.error('Invalid REGISTER message format');
  //   return;
  // }

  // const [_, type, id, rest] = match;
  // const [email, password, fcm_token] = rest.trim().split('\n');

  // if (type !== 'user') {
  //   console.error('Only user can send REGISTER');
  //   return;
  // }

  const { email, password, groupId, fcmToken } = req.body;

  if (!email || !password || !groupId || !fcmToken) {
    return res.status(400).json({ status: false, error: 'Missing required fields' });
  }

  const db = getDb();

  try {
    // Check email on users collection
    const existingUser = await db.collection('users').findOne({ 
      email: email 
    });
    if (existingUser) {
      console.error(`Email already registered: ${email}`);
      return res.status(400).json({ status: false, error: 'Email already registered' });
    }

    const hashedPassword = await bcrypt.hash(password, 10);
    const accessToken = uuidv4();

    // New user to collection users
    await db.collection('users').insertOne({
      email: email,
      password: hashedPassword,
      groupId: groupId,
      accessToken: accessToken,
    });

    // Update fcmToken on local groups and collection groups
    const group = groups.get(groupId);
    if (!group) {
      groups.set(groupId, {
        id: groupId,
        state: 'Impossible',
        clients: {
          camera: null,
          user: null,
          controller: null,
        },
        fcmToken: fcmToken,
      });
    } else {
      group.fcmToken = fcmToken;
    }

    await db.collection('groups').updateOne(
      { id: groupId },
      { $set: { fcmToken: fcmToken } },
      { upsert: true }
    );

    console.log(`User registered: ${email}, group: ${groupId}`);
    res.status(201).json({ status: true, accessToken });
  } catch (error) {
    console.error(`Error in handleRegister: ${error.message}`);
    res.status(500).json({ status: false, error: 'Server error' });
  }
}

module.exports = { handleRegister, generateAccessToken };