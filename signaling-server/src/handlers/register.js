const { getDb } = require('../db/db');
const { groups } = require("../groups/groups");

// Hàm tạo chuỗi số ngẫu nhiên dài 10 ký tự
function generateAccessToken() {
  let token = 'token';
  for (let i = 0; i < 10; i++) {
    token += Math.floor(Math.random() * 10);
  }
  token += 'token';
  return token;
}

async function handleRegister(message, client) {
  // Sử dụng regex để bóc tách message
  const match = message.match(/^REGISTER (\w+) (\w+)\n([\s\S]*)$/);
  if (!match) {
    console.error('Invalid REGISTER message format');
    return;
  }

  const [_, type, id, rest] = match;
  const [email, password, fcm_token] = rest.trim().split('\n');

  if (type !== 'user') {
    console.error('Only user can send REGISTER');
    return;
  }

  const db = getDb();

  try {
    // Kiểm tra email đã tồn tại
    const existingUser = await db.collection('users').findOne({ email });
    if (existingUser) {
      console.error(`Email already registered: ${email}`);
      client.send('REGISTER failed');
      return;
    }

    // Tạo accessToken
    const accessToken = generateAccessToken();

    // Lưu user vào collection users
    await db.collection('users').insertOne({
      email,
      password,
      groupId: id,
      accessToken,
    });

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

    await db.collection('groups').updateOne(
      { id },
      { $set: { fcm_token: fcm_token } },
      { upsert: true }
    );

    // Gửi accessToken về client
    client.send(`LOGIN ${accessToken}`);
    console.log(`User registered: ${email}, group: ${id}`);
  } catch (error) {
    console.error('Error in handleRegister:', error.message);
    client.send('REGISTER failed');
  }
}

module.exports = { handleRegister };