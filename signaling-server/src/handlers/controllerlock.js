const Group = require('../schemas/group');
const Door = require('../schemas/door');
const { wsControllerAuth } = require('../middleware/auth');

async function handleControllerLock(message, client) {

  const msg = message.toString().trim();
  const match = msg.match(/^LOCK\s+(\w+)\s+(\S+)$/);
  if (!match) {
    console.error('Invalid LOCK message format');
    client.close();
    return;
  }

  const [_, senderType, token] = match;
  console.log(`Handling lock from ${senderType}`);

  // Validate sender is controller
  if (senderType !== 'controller') {
    console.error('Only controller can send LOCK');
    client.close();
    return;
  }

  // Validate controller token
  if (!(await wsControllerAuth(token, client))) {
    console.error('Invalid controller token');
    client.close();
    return;
  }
  const groupId = client._controller.groupId;

  try {
    const dbGroup = await Group.findOne({ id: groupId });
    if (!dbGroup) {
      console.error(`Group not found: ${groupId}`);
      client.close();
      return;
    }

    if (dbGroup.door?.lock === 'Locked') {
      console.error(`Group already locked: ${groupId}`);
      return;
    }

    // Update group door state
    await Group.updateOne(
      { id: groupId },
      {
        $set: {
          door: {
            lock: 'Locked',
            user: `Controller ${dbGroup.controllerId}`,
            time: new Date()
          }
        }
      }
    );

    // Log door history
    await Door.create({
      groupId,
      state: 'Locked',
      user: `Controller ${dbGroup.controllerId}`,
      timestamp: new Date()
    });

    console.log(`Controller ${dbGroup.controllerId} locked group: ${groupId}`);
  } catch (error) {
    console.error(`Error in handleConntrollerLock: ${error.message}`);
    client.close();
  }
}

module.exports = { handleControllerLock };