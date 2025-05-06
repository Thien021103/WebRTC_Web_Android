const { getDb } = require("../db/db");
const { groups } = require("../groups/groups");
const { generateAccessToken } = require("./register");
const { v4: uuidv4 } = require('uuid');
const bcrypt = require('bcrypt');

async function handleLogin(req, res) {
  // // Sử dụng regex để bóc tách message
  // /*
  // LOGIN user 123
  // email
  // password
  // fcmToken
  // */
  // const match = message.match(/^LOGIN (\w+) (\w+)\n([\s\S]*)$/);
  // if (!match) {
  //   console.error('Invalid LOGIN message format');
  //   return;
  // }

  // const [_, type, id, rest] = match;
  // const [email, password, fcmToken] = rest.trim().split('\n');

  // if (type !== 'user') {
  //   console.error('Only user can send LOGIN');
  //   return;
  // }

  const { email, password, groupId, fcmToken } = req.body;

  if (!email || !password || !groupId || !fcmToken) {
    return res.status(400).json({ status: false, message: 'Missing required fields' });
  }

  const db = getDb();

  try {
    // User from database
    const user = await db.collection('users').findOne({ 
      email: email, 
      groupId: groupId 
    });

    if (!user) {
      console.error(`Login failed for email: ${email}, group: ${groupId}`);
      return res.status(401).json({ status: false, error: 'Invalid email or groupId' });
    }

    const isPasswordValid = await bcrypt.compare(password, user.password);
    if (!isPasswordValid) {
      console.error(`Invalid password for email: ${email}`);
      return res.status(401).json({ status: false, error: 'Invalid password' });
    }

    const accessToken = uuidv4();

    // Update users collection
    await db.collection('users').updateOne(
      { email: email, groupId: groupId },
      { $set: { accessToken: accessToken, fcmToken: fcmToken } }
    );

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

    console.log(`User logged in: ${email}, group: ${groupId}`);
    res.json({ status: true, accessToken });
  } catch (error) {
    console.error(`Error in handleLogin: ${error.message}`);
    res.status(500).json({ status: false, error: 'Server error' });
  }
}

module.exports = { handleLogin };