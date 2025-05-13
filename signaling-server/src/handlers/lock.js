const { getDb } = require("../db/db");
const { groups } = require("../groups/groups");
const bcrypt = require('bcrypt');

async function handleLock(req, res) {
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

  const { email, password, groupId } = req.body;

  if (!email || !password || !groupId) {
    return res.status(400).json({ status: "false", message: 'Missing required fields' });
  }

  const db = getDb();

  try {
    // Find user in db by accessToken
    const user = await db.collection('users').findOne({ 
      email: email, 
      groupId: groupId,
    });

    if (!user) {
      console.error(`Invalid email: ${email}, group: ${groupId}`);
      return res.status(401).json({ status: "false", message: 'Invalid info' });
    }
    const isPasswordValid = await bcrypt.compare(password, user.password);
    if (!isPasswordValid) {
      console.error(`Invalid password for email: ${email}`);
      return res.status(401).json({ status: "false", message: 'Invalid password' });
    }

    // Update on collection groups
    const dbGroup = await db.collection('groups').findOne({ id: groupId });
    if (!dbGroup) {
      console.error(`Group not found: ${groupId}`);
      return res.status(404).json({ status: "false", message: 'Group not found' });
    } else {
      if (dbGroup.door.lock && dbGroup.door.lock === 'Locked') {
        console.error(`Group already locked: ${groupId}`);
        return res.status(400).json({ status: "false", message: 'Already locked' });
      }
      await db.collection('groups').updateOne(
        { id: groupId },
        { $set: {
          door: { 
            lock: 'Locked',
            user: email,
            time: new Date().toISOString()
          }
        } },
        { upsert: true }
      );
    }

    // Send lock message to controller
    const group = groups.get(groupId);
    const message = `LOCK ${groupId}`;
    if (group) {
      const controller = group.clients.controller;
      if (controller && controller.readyState === controller.OPEN) {
        controller.send(message);
      }
    } else {
      console.error(`Group not found in local: ${groupId}`);
      return res.status(404).json({ status: "false", message: 'Group not found' });
    }
    
    console.log(`User ${email} locked group: ${groupId}`);
    return res.json({ status: "success", message: '' });
  } catch (error) {
    console.error(`Error in handleLock: ${error.message}`);
    return res.status(500).json({ status: "false", message: 'Server error' });
  }
}

async function handleConntrollerLock(message) {
  // Sử dụng regex để trích xuất senderType, groupId
  const match = message.match(/^LOCK (\w+) (\w+)$/);
  if (!match) {
    console.error('Invalid LOCK message format');
    return;
  }
  const [_, senderType, groupId] = match; // match[0] là toàn bộ chuỗi, bỏ qua
  console.log(`Handling offer from ${senderType}, group ${groupId}`);
  if (senderType !== 'controller') {
    console.error('Only controller can send LOCK');
    return;
  }
  try {
    // Update on collection groups
    const dbGroup = await db.collection('groups').findOne({ id: groupId });
    if (!dbGroup) {
      console.error(`Group not found: ${groupId}`);
      return;
    } else {
      if (dbGroup.door.lock && dbGroup.door.lock === 'Locked') {
        console.error(`Group already locked: ${groupId}`);
        return;
      }
      await db.collection('groups').updateOne(
        { id: groupId },
        { $set: {
          door: { 
            lock: 'Locked',
            user: `Controller ${groupId}`,
            time: new Date().toISOString()
          }
        } },
        { upsert: true }
      );
    }
    console.log(`User ${email} locked group: ${groupId}`);
    return;
  } catch (error) {
    console.error(`Error in handleControllerLock: ${error.message}`);
    return;
  }
}

module.exports = { handleLock, handleConntrollerLock };