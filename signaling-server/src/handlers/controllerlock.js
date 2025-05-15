const Group = require('../schemas/group');

async function handleConntrollerLock(message) {
  const match = message.match(/^LOCK (\w+) (\w+)$/);
  if (!match) {
    console.error('Invalid LOCK message format');
    return;
  }
  const [_, senderType, groupId] = match;
  console.log(`Handling lock from ${senderType}, group ${groupId}`);
  if (senderType !== 'controller') {
    console.error('Only controller can send LOCK');
    return;
  }

  try {
    const dbGroup = await Group.findOne({ id: groupId });
    if (!dbGroup) {
      console.error(`Group not found: ${groupId}`);
      return;
    }

    if (dbGroup.door?.lock === 'Locked') {
      console.error(`Group already locked: ${groupId}`);
      return;
    }

    await Group.updateOne(
      { id: groupId },
      {
        $set: {
          door: {
            lock: 'Locked',
            user: `Controller ${groupId}`,
            time: new Date()
          }
        }
      },
      { upsert: true }
    );

    console.log(`Controller ${groupId} locked group: ${groupId}`);
  } catch (error) {
    console.error(`Error in handleConntrollerLock: ${error.message}`);
  }
}

module.exports = { handleConntrollerLock };