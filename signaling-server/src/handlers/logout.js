const { getDb } = require("../db/db");
const { groups, notifyStateUpdate } = require("../groups/groups");
const { v4: uuidv4 } = require('uuid');
const bcrypt = require('bcrypt');

async function handleLogout(req, res) {
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

  const { email, groupId, accessToken } = req.body;

  if (!email || !groupId || !accessToken) {
    return res.status(400).json({ status: "false", message: 'Missing required fields' });
  }

  const db = getDb();

  try {
    // User from database
    const user = await db.collection('users').findOne({ 
      email: email, 
      groupId: groupId,
      accessToken: accessToken, 
    });

    if (!user) {
      console.error(`Logout failed for email: ${email}, group: ${groupId}`);
      return res.status(401).json({ status: "false", message: 'Invalid email or groupId' });
    }

    // Update users collection
    await db.collection('users').updateOne(
      { email: email, groupId: groupId },
      { $unset: { accessToken: '' } }
    );

    // Update on local groups 
    const group = groups.get(groupId);
    if (group) {
      group.clients.user = null;
      group.state = 'Impossible';
      notifyStateUpdate(groupId);
    }
    // Update on collection groups
    const db = getDb();
    await db.collection('groups').updateOne(
      { id: groupId },
      { $set: { state: 'Impossible' } }
    );
    console.log(`Updated db group ${groupId} state to Impossible`);

    console.log(`User logged out: ${email}, group: ${groupId}`);
    return res.json({ status: "success", message: '' });
  } catch (error) {
    console.error(`Error in handleLogout: ${error.message}`);
    return res.status(500).json({ status: "false", message: 'Server error' });
  }
}

module.exports = { handleLogout };