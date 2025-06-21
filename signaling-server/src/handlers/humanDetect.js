const Group = require('../schemas/group');
const User = require('../schemas/user');
const Owner = require('../schemas/owner');
const Notification = require('../schemas/notification');

const { sendFCMNotification } = require('../utils/fcm');
const { wsCameraAuth } = require('../middleware/auth');

async function handleHumanDetect(message, client) {

  const msg = message.toString().trim();
  const match = msg.match(/^HUMAN\s+(\w+)\s+(\S+)$/);
  if (!match) {
    console.error('Invalid HUMAN message format');
    client.close();
    return;
  }

  const [_, senderType, token] = match;
  console.log(`Handling human from ${senderType}`);

  // Validate sender is controller
  if (senderType !== 'camera') {
    console.error('Only camera can send HUMAN');
    client.close();
    return;
  }

  // Validate camera token
  if (!(await wsCameraAuth(token, client))) {
    console.error('Invalid camera token');
    client.close();
    return;
  }
  const groupId = client._tmpGroupId;

  try {
    const dbGroup = await Group.findOne({ id: groupId });
    if (!dbGroup) {
      console.error(`Group not found: ${groupId}`);
      client.close();
      return;
    }

    // Send FCM notifications for controller notification
    const users = await User.find({ groupId, fcmToken: { $ne: '' } }, { fcmToken: 1 }).lean();
    const owners = await Owner.find({ groupId, fcmToken: { $ne: '' } }, { fcmToken: 1 }).lean();
    const fcmTokens = [...users, ...owners].map(entity => entity.fcmToken);
    for (const fcmToken of fcmTokens) {
      await sendFCMNotification(fcmToken, 'human');
    }
    if (fcmTokens.length === 0) {
      console.log(`No FCM tokens found for group ${groupId}`);
    }

    // Save notification if no recent one exists (within 60 seconds)
    // const recentNotification = await Notification.findOne({
    //   groupId,
    //   time: { $gte: new Date(Date.now() + 7 * 60 * 60 * 1000 - 60 * 1000) }, // Vietnam time - 60s
    // });
    // if (!recentNotification) {
    //   await Notification.create({ groupId });
    //   console.log(`Notification saved for group ${groupId}`);
    // }
  } catch (error) {
    console.error(`Error in handleHumanDetect: ${error.message}`);
    client.close();
  }
}

module.exports = { handleHumanDetect };