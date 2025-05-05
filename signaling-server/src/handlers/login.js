const { getDb } = require("../db/db");
const { groups } = require("../groups/groups");
const { generateAccessToken } = require("./register");

async function handleLogin(message, client) {
  // Sử dụng regex để bóc tách message
  /*
  LOGIN user 123
  email
  password
  fcm_token
  */
  const match = message.match(/^LOGIN (\w+) (\w+)\n([\s\S]*)$/);
  if (!match) {
    console.error('Invalid LOGIN message format');
    return;
  }

  const [_, type, id, rest] = match;
  const [email, password, fcm_token] = rest.trim().split('\n');

  if (type !== 'user') {
    console.error('Only user can send LOGIN');
    return;
  }

  const db = getDb();

  try {
    // Tra cứu user trong collection users
    const user = await db.collection('users').findOne({
      email,
      password,
      groupId: id,
    });

    if (!user) {
      console.error(`Login failed for email: ${email}, group: ${id}`);
      client.send('LOGIN failed');
      return;
    }
    // New accessToken in to database
    const accessToken = generateAccessToken();
    await db.collection('users').updateOne(
      { groupId: id },
      { $set: { accessToken: accessToken } },
      { upsert: true }
    );
    
    // Lưu hoặc cập nhật fcm_token trong collection groups
    const group = groups.get(id);
    if (!group) {
      groups.set(id, {
        id,
        state: 'Impossible',
        clients: {
          camera: null,
          user: null,
          controller: null,
        },
        fcm_token: fcm_token,
      });
    } else {
      group.fcm_token = fcm_token;
    }

    // Database
    await db.collection('groups').updateOne(
      { id },
      { $set: { fcm_token: fcm_token } },
      { upsert: true }
    );

    // Send accessToken to client
    client.send(`LOGIN ${accessToken}`);
    console.log(`User logged in: ${email}, group: ${id}`);
  } catch (error) {
    console.error('Error in handleLogin:', error.message);
    client.send('LOGIN failed');
  }
}

module.exports = { handleLogin };